/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ap.mobile.services;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.ap.common.models.http.HttpNotification;
import com.ap.common.models.http.HttpStatusConstants;
import com.ap.common.models.http.NotificationType;
import com.ap.core.device.CareType;
import com.ap.core.device.Constants;
import com.ap.core.device.CoreConfig;
import com.ap.core.device.DataStatusOptions;
import com.ap.core.device.components.analyzer.DefaultDistanceAnalyzer;
import com.ap.core.device.context.UserPreference;
import com.ap.core.device.exceptions.DataAnalyzeException;
import com.ap.core.device.http.DefaultHttpJSONClient;
import com.ap.mobile.activities.fragment.caretaker.CareGiverNotificationActivity;
import com.ap.mobile.model.CommandInfo;
import com.ap.mobile.model.CommandType;
import com.ap.mobile.services.notifications.CareGiverNotificationHelper;
import com.google.android.gms.gcm.GcmListenerService;
import com.google.gson.Gson;

public class GCMMessageListenerService extends GcmListenerService {
	private static final String TAG = GCMMessageListenerService.class.getSimpleName();
	
	private static boolean isNotificationReceived = false;

	/**
	 * Called when message is received.
	 * 
	 * @param from SenderID of the sender.
	 * @param data Data bundle containing message data as key/value pairs. For
	 *        Set of keys use data.keySet().
	 */
	// [START receive_message]
	@Override
	public void onMessageReceived(String from, Bundle data) {
		Log.i(TAG, "from=" + from);
		Log.i(TAG, "data=" + data.toString());
		String notificationType = data.getString(com.ap.common.models.http.HttpStatusConstants.NOTIFICATION_PROPERTY_NOTIFICATION_TYPE);
		String uuid = data.getString(com.ap.common.models.http.HttpStatusConstants.NOTIFICATION_PROPERTY_UUID);
		String careGiverUUID = data.getString(com.ap.common.models.http.HttpStatusConstants.NOTIFICATION_PROPERTY_CARE_GIVER_UUID);
		String careType = data.getString(com.ap.common.models.http.HttpStatusConstants.NOTIFICATION_PROPERTY_CARETYPE);
		Log.d(TAG, "notificationType: " + notificationType);
		Log.d(TAG, "uuid: " + uuid);
		Log.d(TAG, "careGiverUUID: " + careGiverUUID);
		Log.d(TAG, "careType: " + careType);
		if (careType != null && careType.equals(String.valueOf(CareType.CareRecipient.getCareTypeId()))) {
			CommandInfo commandInfo = new CommandInfo();
			commandInfo.setCommand(notificationType);
			commandInfo.setUuid(uuid);
			final Intent messageQueueIntent = new Intent(Constants.MESSAGE_COMMAND_QUEUE);
			messageQueueIntent.putExtra(Constants.MESSAGE_COMMAND, commandInfo);
			LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(messageQueueIntent);
			
			UserPreference userPreference = UserPreference.getCurrentUserPreference(this.getApplicationContext());
			if (userPreference == null) {
				return;
			}
			Log.i(TAG, "userPreference.uuid=" + userPreference.getUuid());
			Log.i(TAG, "uuid=" + uuid);
			if (notificationType.equalsIgnoreCase(NotificationType.START_SERVICE.name()) && uuid.equals(userPreference.getUuid())) {
				if (!DefaultSafeSeniorMonitorService.isRunning()) {
					Log.i(TAG, "DefaultSafeSeniorMonitorService is not running.");
				}
				Intent safeSeniorMonitorServiceIntent = new Intent(this.getApplicationContext(), DefaultSafeSeniorMonitorService.class);
				Log.i(TAG, "Starting safeSeniorServiceIntent @ " + SystemClock.elapsedRealtime());
				this.getApplicationContext().startService(safeSeniorMonitorServiceIntent);				
			}
			if (notificationType.equalsIgnoreCase(CommandType.CHECK_POSITION.name()) && uuid.equals(userPreference.getUuid())) {
				Log.i(TAG, "Checking position...");
				DefaultDistanceAnalyzer distanceAnalyzer = new DefaultDistanceAnalyzer(this.getApplicationContext());
				try {
					distanceAnalyzer.analyze();
					String result = distanceAnalyzer.getDistance();
					Log.i(TAG, "result="+ result);
					if (result == null || result.isEmpty()) {
						return;
					}
					if (result.startsWith(Constants.DISTANCE_SUCCESS_PREFIX)) {
						result = result.substring(result.indexOf(":")+1).trim();
					}
					UserPreference currentUserPreference = UserPreference.getCurrentUserPreference(this.getApplicationContext());
					HttpNotification notification = new HttpNotification();
					notification.setCreated(System.currentTimeMillis());
					notification.setMessageType(NotificationType.CARE_RECEIVER_POSITION.getTypeId());
					notification.setCareReceiverUUID(currentUserPreference.getUuid());
					notification.setIntervalType(0);
					notification.setExtraInfo(result);
					final String url = CoreConfig.getUrl("/user/carereceiver/check_position/"+ careGiverUUID);
					Gson gson = new Gson();
					String request = gson.toJson(notification);
					Log.i(TAG, "distance notification="+ request);
					final DefaultHttpJSONClient httpClient = new DefaultHttpJSONClient();
					try {
						httpClient.executePost(url, request);
					} catch (Exception e) {
						Log.i(TAG, "error="+ e.getMessage());
					}
				} catch (DataAnalyzeException e) {
					e.printStackTrace();
				}
			}
		} else {
			if (notificationType == null || notificationType.isEmpty()) {
				return;
			}
			Context context = getApplicationContext();
			String notificationMainId = data.getString(HttpStatusConstants.NOTIFICATION_PROPERTY_NOTIFICATION_MAIN_ID);
			String created = data.getString(com.ap.common.models.http.HttpStatusConstants.NOTIFICATION_PROPERTY_CREATED);
			Log.i(TAG, "created="+ created);
			Log.i(TAG, "notificationMainId="+ notificationMainId);
			
			if (notificationType.equals(NotificationType.NOTIFICATION_DISPLAY.name())) {
				DataStatusOptions.isCareReceiverNotificationDirty = true;
				CareGiverNotificationHelper.handleInactivityNotification(data, context, created, notificationMainId);
				isNotificationReceived = true;
			} else if (notificationType.equals(NotificationType.NOTIFICATION_CLEAR.name())) {
//				notificationManager.cancel(NOTIFICATION_ID);
				// comment out this for live
				isNotificationReceived = false;
				if (!isNotificationReceived) {
					return;
				}
				isNotificationReceived = false;
				Log.i(TAG, "isNotificationOpen in CareGiverNotificationActivity="+ CareGiverNotificationActivity.isNotificationOpen);
				DataStatusOptions.isCareReceiverNotificationDirty = true;
	
				CareGiverNotificationHelper.handleNotificationClear(data, context, notificationMainId);

			} else if (notificationType.equals(NotificationType.BATTERY_LOW.name())) {
				Log.i(TAG, "isNotificationOpen in CareGiverNotificationActivity="+ CareGiverNotificationActivity.isNotificationOpen);
				DataStatusOptions.isCareReceiverNotificationDirty = true;
				CareGiverNotificationHelper.handleBatteryLowNotification(data, context, notificationMainId);

			} else if (notificationType.equals(NotificationType.CARE_RECEIVER_POSITION.name())) {
				DataStatusOptions.isCareReceiverNotificationDirty = true;
				CareGiverNotificationHelper.handleCheckPositionNotification(data, context, notificationMainId);
			}
		}
	}
}
