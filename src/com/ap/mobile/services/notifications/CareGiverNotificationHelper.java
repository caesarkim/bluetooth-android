/**
 * AgingSafeSeniorMobile
 */
package com.ap.mobile.services.notifications;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.ap.common.models.http.HttpCareGiverSetting;
import com.ap.common.models.http.HttpNotification;
import com.ap.common.models.http.HttpStatusConstants;
import com.ap.common.models.http.NotificationType;
import com.ap.core.device.Constants;
import com.ap.core.device.CoreConfig;
import com.ap.core.device.context.UserPreference;
import com.ap.core.device.http.DefaultHttpJSONClient;
import com.ap.core.device.http.HttpJSONClient;
import com.ap.mobile.activities.fragment.caretaker.CareGiverBatteryNotificationActivity;
import com.ap.mobile.activities.fragment.caretaker.CareGiverCheckPositionNotificationActivity;
import com.ap.mobile.activities.fragment.caretaker.CareGiverNotificationActivity;
import com.ap.mobile.model.NotificationOption;
import com.ap.mobile.util.Utils;
import com.ap.safesenior.mobile.R;
import com.google.gson.Gson;

/**
 *  Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by jungwhan Projecteria LLC, May 14, 2016
 */

/**
 * @author jungwhan
 * CareGiverNotificationHelper.java
 * 2:50:27 PM May 14, 2016 2016
 */
public class CareGiverNotificationHelper {
	private static final String TAG = CareGiverNotificationHelper.class.getSimpleName();

	private final static int NOTIFICATION_ID = 72823;
	
	private final static int BATTERY_NOTIFICATION_ID = 22392;
	
	public static void handleCheckPositionNotification(Bundle data, Context context, String notificationMainId) {
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Intent launchIntent = new Intent(context, CareGiverCheckPositionNotificationActivity.class);
		String notificationDetailId = data.getString(HttpStatusConstants.NOTIFICATION_PROPERTY_NOTIFICATION_DETAIL_ID);
		String notificationMessage = data.getString(HttpStatusConstants.NOTIFICATION_PROPERTY_NOTIFICATION_MESSAGE);
		String careReceiverName = data.getString(HttpStatusConstants.NOTIFICATION_PROPERTY_CARE_RECEIVER_NAME);
		String careReceiverIDStr = data.getString(HttpStatusConstants.NOTIFICATION_PROPERTY_CARE_RECEIVER_ID);
		long careReceiverId = Long.parseLong(careReceiverIDStr);
		int notificationId = Utils.notificationId();
		Log.i(TAG, "careReceiverName="+ careReceiverName);
		Log.i(TAG, "careReceiverId="+ careReceiverId);
		
		launchIntent.putExtra(com.ap.common.models.http.HttpStatusConstants.NOTIFICATION_PROPERTY_NOTIFICATION_MESSAGE, notificationMessage);
		launchIntent.putExtra(com.ap.common.models.http.HttpStatusConstants.NOTIFICATION_PROPERTY_NOTIFICATION_DETAIL_ID, notificationDetailId);
		launchIntent.putExtra(com.ap.common.models.http.HttpStatusConstants.NOTIFICATION_PROPERTY_NOTIFICATION_ID, String.valueOf(notificationId));
		launchIntent.putExtra(com.ap.common.models.http.HttpStatusConstants.NOTIFICATION_PROPERTY_CARE_RECEIVER_NAME, careReceiverName);
		launchIntent.putExtra(com.ap.common.models.http.HttpStatusConstants.NOTIFICATION_PROPERTY_NOTIFICATION_MAIN_ID, notificationMainId);
		
		sendNotificationDeliveryConfirmation(context, careReceiverId, notificationDetailId, NotificationType.CARE_RECEIVER_POSITION.getTypeId());
		
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context).setContentTitle("Checking Position").setContentText("Checking position").setSmallIcon(R.drawable.logo_front);
		builder.setAutoCancel(true);
		builder.setContentIntent(pendingIntent);

		notificationManager.notify(BATTERY_NOTIFICATION_ID, builder.build());
	}
	
	public static void handleBatteryLowNotification(Bundle data, Context context, String notificationMainId) {
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		Intent launchIntent = new Intent(context, CareGiverBatteryNotificationActivity.class);
		String careReceiverJSON = data.getString(HttpStatusConstants.NOTIFICATION_PROPERTY_CARE_RECEIVER_JSON);
		String notificationDetailId = data.getString(HttpStatusConstants.NOTIFICATION_PROPERTY_NOTIFICATION_DETAIL_ID);
		String notificationMessage = data.getString(HttpStatusConstants.NOTIFICATION_PROPERTY_NOTIFICATION_MESSAGE);
		String careReceiverName = data.getString(HttpStatusConstants.NOTIFICATION_PROPERTY_CARE_RECEIVER_NAME);
		String careReceiverIDStr = data.getString(HttpStatusConstants.NOTIFICATION_PROPERTY_CARE_RECEIVER_ID);
		long careReceiverId = Long.parseLong(careReceiverIDStr);
		int notificationId = Utils.notificationId();
		Log.i(TAG, "careReceiverName="+ careReceiverName);
		Log.i(TAG, "careReceiverId="+ careReceiverId);
		
		launchIntent.putExtra(com.ap.common.models.http.HttpStatusConstants.NOTIFICATION_PROPERTY_NOTIFICATION_MESSAGE, notificationMessage);
		launchIntent.putExtra(com.ap.common.models.http.HttpStatusConstants.NOTIFICATION_PROPERTY_NOTIFICATION_DETAIL_ID, notificationDetailId);
		launchIntent.putExtra(com.ap.common.models.http.HttpStatusConstants.NOTIFICATION_PROPERTY_NOTIFICATION_ID, String.valueOf(notificationId));
		launchIntent.putExtra(com.ap.common.models.http.HttpStatusConstants.NOTIFICATION_PROPERTY_CARE_RECEIVER_NAME, careReceiverName);
		launchIntent.putExtra(com.ap.common.models.http.HttpStatusConstants.NOTIFICATION_PROPERTY_CARE_RECEIVER_JSON, careReceiverJSON);
		launchIntent.putExtra(com.ap.common.models.http.HttpStatusConstants.NOTIFICATION_PROPERTY_NOTIFICATION_MAIN_ID, notificationMainId);
		
		sendNotificationDeliveryConfirmation(context, careReceiverId, notificationDetailId, NotificationType.BATTERY_LOW.getTypeId());
		
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context).setContentTitle("Battery Low Alert").setContentText("Battery Low Alert").setSmallIcon(R.drawable.logo_front);
		builder.setAutoCancel(true);
		builder.setContentIntent(pendingIntent);

		notificationManager.notify(BATTERY_NOTIFICATION_ID, builder.build());
	}
	
	public static void handleNotificationClear(Bundle data, Context context, String notificationMainId) {
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Intent launchIntent = new Intent(context, CareGiverNotificationActivity.class);

		launchIntent.putExtra(com.ap.common.models.http.HttpStatusConstants.NOTIFICATION_PROPERTY_NOTIFICATION_TYPE, NotificationType.NOTIFICATION_CLEAR.name());
		launchIntent.putExtra(com.ap.common.models.http.HttpStatusConstants.NOTIFICATION_PROPERTY_NOTIFICATION_MAIN_ID, notificationMainId);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context).setContentTitle("Alert Clear").setContentText("Alert Clear").setSmallIcon(R.drawable.logo_front);
		builder.setAutoCancel(true);
		builder.setContentIntent(pendingIntent);

//		notificationManager.cancel(NOTIFICATION_ID);
		notificationManager.notify(NOTIFICATION_ID, builder.build());
	}
	
	public static void handleInactivityNotification(Bundle data, Context context, String created, String notificationMainId) {
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		String careReceiverJSON = data.getString(HttpStatusConstants.NOTIFICATION_PROPERTY_CARE_RECEIVER_JSON);
		String notificationDetailId = data.getString(HttpStatusConstants.NOTIFICATION_PROPERTY_NOTIFICATION_DETAIL_ID);
		String notificationMessage = data.getString(HttpStatusConstants.NOTIFICATION_PROPERTY_NOTIFICATION_MESSAGE);
		String notificationMessageTemplate = data.getString(HttpStatusConstants.NOTIFICATION_PROPERTY_NOTIFICATION_MESSAGE_TEMPLATE);
		String careReceiverName = data.getString(HttpStatusConstants.NOTIFICATION_PROPERTY_CARE_RECEIVER_NAME);
		String careReceiverPhoneNumber = data.getString(HttpStatusConstants.NOTIFICATION_PROPERTY_CARE_RECEIVER_PHONE_NUMBER);
		String careReceiverIDStr = data.getString(HttpStatusConstants.NOTIFICATION_PROPERTY_CARE_RECEIVER_ID);
		long careReceiverId = Long.parseLong(careReceiverIDStr);
		int notificationId = Utils.notificationId();
		Log.i(TAG, "careReceiverName="+ careReceiverName);
		Log.i(TAG, "careReceiverPhoneNumber="+ careReceiverPhoneNumber);
		Log.i(TAG, "careReceiverId="+ careReceiverId);
		
		Intent launchIntent = new Intent(context, CareGiverNotificationActivity.class);

		launchIntent.putExtra(com.ap.common.models.http.HttpStatusConstants.NOTIFICATION_PROPERTY_NOTIFICATION_TYPE, NotificationType.NOTIFICATION_DISPLAY.name());
		launchIntent.putExtra(com.ap.common.models.http.HttpStatusConstants.NOTIFICATION_PROPERTY_CREATED, created);
		launchIntent.putExtra(com.ap.common.models.http.HttpStatusConstants.NOTIFICATION_PROPERTY_NOTIFICATION_MESSAGE, notificationMessage);
		launchIntent.putExtra(com.ap.common.models.http.HttpStatusConstants.NOTIFICATION_PROPERTY_NOTIFICATION_MESSAGE_TEMPLATE, notificationMessageTemplate);
		launchIntent.putExtra(com.ap.common.models.http.HttpStatusConstants.NOTIFICATION_PROPERTY_NOTIFICATION_DETAIL_ID, notificationDetailId);
		launchIntent.putExtra(com.ap.common.models.http.HttpStatusConstants.NOTIFICATION_PROPERTY_NOTIFICATION_ID, String.valueOf(notificationId));
		launchIntent.putExtra(com.ap.common.models.http.HttpStatusConstants.NOTIFICATION_PROPERTY_CARE_RECEIVER_NAME, careReceiverName);
		launchIntent.putExtra(com.ap.common.models.http.HttpStatusConstants.NOTIFICATION_PROPERTY_CARE_RECEIVER_PHONE_NUMBER, careReceiverPhoneNumber);
		launchIntent.putExtra(com.ap.common.models.http.HttpStatusConstants.NOTIFICATION_PROPERTY_NOTIFICATION_MAIN_ID, notificationMainId);
		launchIntent.putExtra(com.ap.common.models.http.HttpStatusConstants.NOTIFICATION_PROPERTY_CARE_RECEIVER_JSON, careReceiverJSON);
		Log.i(TAG, "context.getPackageName()=" + context.getPackageName());
		// Intent launchIntent =
		// context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
		// launchIntent.setAction(action);
		
		Gson gson = new Gson();
		String notificationPreference = Utils.getPreferenceValue(context, Constants.PREFERENCE_KEY_NOTIFICATION_OPTION);
		HttpCareGiverSetting setting = gson.fromJson(notificationPreference, HttpCareGiverSetting.class);
		
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context).setContentTitle("Notification Alert").setContentText("Inactivity Notification Alert").setSmallIcon(R.drawable.logo_front);
		builder.setAutoCancel(true);
		builder.setContentIntent(pendingIntent);

//		notificationManager.cancel(NOTIFICATION_ID);
		notificationManager.notify(NOTIFICATION_ID, builder.build());
		int defaultOption = NotificationOption.VIBRATION.getOptionId();
		if (setting != null) {
			int userSettingVaue = Integer.parseInt(setting.getSettingValue());
			if (userSettingVaue == NotificationOption.SOUND.getOptionId()) {
			    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			    Ringtone r = RingtoneManager.getRingtone(context, notification);
			    r.play();
			} else if (userSettingVaue == defaultOption) {
				Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
				vibrator.vibrate(500);
			}
		}
		CareGiverNotificationHelper.sendNotificationDeliveryConfirmation(context, careReceiverId, notificationDetailId, NotificationType.NOTIFICATION_DISPLAY.getTypeId());
	}
	
	public static void sendNotificationDeliveryConfirmation(Context context, long careReceiverId, String notificationId, int messageType) {
		Log.i(TAG, "sendNotificationDeliveryConfirmation is called.");
		Log.i(TAG, "notificationId="+ notificationId);
		Log.i(TAG, "careReceiverId="+ careReceiverId);
		if (notificationId == null || notificationId.isEmpty()) {
			return;
		}
		UserPreference userPreference = UserPreference.getCurrentUserPreference(context);
		if (userPreference == null) {
			return;
		}
		String url = CoreConfig.getUrl("/user/caregiver/"+ userPreference.getUuid() +"/notification_delivry_confirmation");
		Log.i(TAG, "url="+ url);
		HttpJSONClient client = new DefaultHttpJSONClient();
		try {
			HttpNotification notification = new HttpNotification();
			if (messageType == NotificationType.BATTERY_LOW.getTypeId()) {
				notification.setIntervalType(0);
			}
			notification.setStatus(1);
			notification.setMessageType(messageType);
			notification.setCareReceiverId(careReceiverId);
			notification.setId(Long.parseLong(notificationId));
			Gson gson = new Gson();
			String requestBody = gson.toJson(notification);
			Log.i(TAG, "requestBody=" + requestBody);
			String response = client.executePost(url, requestBody);
			Log.i(TAG, "response=" + response);
		} catch (Exception e1) {
			Log.i(TAG, "errorMessage="+ e1.getMessage());
		}
	}
}
