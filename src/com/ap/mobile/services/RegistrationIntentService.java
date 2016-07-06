/**
 * AgingPlaceMobile
 */
package com.ap.mobile.services;

/**
 * Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */

import im.delight.android.location.SimpleLocation;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.ap.common.models.http.HttpCareGiver;
import com.ap.common.models.http.HttpCareReceiver;
import com.ap.core.device.CareType;
import com.ap.core.device.Constants;
import com.ap.core.device.CoreConfig;
import com.ap.core.device.context.UserPreference;
import com.ap.core.device.http.DefaultHttpJSONClient;
import com.ap.core.device.http.HttpJSONClient;
import com.ap.mobile.services.DefaultSafeSeniorService.DefaultSafeSeniorServiceBinder;
import com.ap.mobile.util.Utils;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.google.gson.Gson;

/**
 * @author jungwhan RegistrationIntentService.java 10:01:50 PM Mar 11, 2016 2016
 */
public class RegistrationIntentService extends AbstractService {
	private static final String TAG = RegistrationIntentService.class.getSimpleName();
	
	private final IBinder registrationServiceBinder = new RegistrationIntentServiceBinder();

	protected DefaultSafeSeniorService safeSeniorService;
	
	private static boolean isRunning = false;
	
	private boolean running = true;
	
	protected boolean isBound = false;
	
	private Thread updateRegistrationThread;

	@Override
	public IBinder onBind(Intent arg0) {
		return registrationServiceBinder;
	}
	
	/**
	 * 
	 * @author jungwhan
	 * RegistrationIntentService.java
	 * 11:12:07 PM Mar 12, 2016 2016
	 */
	public class RegistrationIntentServiceBinder extends Binder {
		public RegistrationIntentService getService() {
			return RegistrationIntentService.this;
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		log(TAG, TAG + ".onDestroy is called at "+ new java.util.Date());
		if (isBound) {
			this.unbindService(serviceConnection);
		}
//		this.unregisterReceiver(mBatInfoReceiver);
		Log.i(TAG, "RegistrationIntentService.onDestroy() is called.");
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		log(TAG, TAG + ".onStartCommand for RegistrationIntentService is starting..." + new java.util.Date());
		Log.i(TAG, "onStartCommand for RegistrationIntentService is starting..." + new java.util.Date());
		readRegistrationID();
		
		long ct = System.currentTimeMillis();
		AlarmManager mgr = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
		Intent safeSeniorIntent = new Intent(getApplicationContext(), DefaultSafeSeniorMonitorService.class);
		PendingIntent pi = PendingIntent.getService(getApplicationContext(), 0, safeSeniorIntent, 0);

		mgr.set(AlarmManager.RTC_WAKEUP, ct + (1000 * 60 * 60 * 12), pi);
		Log.i(TAG, "onStartCommand for RegistrationIntentService ended..." + new java.util.Date());
		return Service.START_STICKY;
	}


	private void readRegistrationID() {
		if (isRunning) {
			return;
		}
		try {
			final Context context = this;
			if (!isBound) {
				Intent safeSeniorServiceIntent = new Intent(context, DefaultSafeSeniorService.class);
				try {
					context.bindService(safeSeniorServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
					isBound = true;
				} catch (Throwable t) {
					//
				}
			}
			updateRegistrationID();

		} catch (Exception e) {
			Log.d(TAG, "Failed to complete token refresh", e);
		}
		Intent registrationComplete = new Intent("registrationComplete");
		LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
	}
	
	private void updateRegistrationID() {
		if (updateRegistrationThread != null && updateRegistrationThread.isAlive()) {
			Log.i(TAG, TAG + ".updateRegistrationThread is not null. Skipping ... Checked at " + new java.util.Date());
			return;
		}
		updateRegistrationThread = new Thread() {
			@Override
			public void run() {
				while (running) {
					try {
						isRunning = true;
						InstanceID instanceID = InstanceID.getInstance(RegistrationIntentService.this);
						String token = instanceID.getToken(Constants.senderID, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
						Log.i(TAG, "GCM Registration Token: " + token);
						if (token == null || token.isEmpty() || token.length() < 55) {
							return;
						}
						UserPreference userPreference = UserPreference.getCurrentUserPreference(getApplicationContext());
						Log.i(TAG, "userPreference: " + userPreference);
						if (userPreference != null) {
							Log.i(TAG, "Updating user preference in the backend.");
							userPreference.setRegistrationId(token);
							sendRegistrationToServer(userPreference);
							Utils.writePreference(getApplicationContext(), Constants.PREFERENCE_KEY_USER_PREFERENCE, userPreference.getJson());
						}
						Utils.writePreference(getApplicationContext(), Constants.USER_REGISTRATION_ID, token);					
					} catch (Exception e) {
						e.printStackTrace();
						//
					}
					try {
						Thread.sleep(1000 * 60 * 60 * 12);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				isRunning = false;
			}
		};
		updateRegistrationThread.start();
	}
	
	protected ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			DefaultSafeSeniorServiceBinder binder = (DefaultSafeSeniorServiceBinder) service;
			Log.i(TAG, "onServiceConnected is connected.");
			safeSeniorService = binder.getService();
			isBound = true;
		}

		public void onServiceDisconnected(ComponentName arg0) {
			isBound = false;
		}
	};
	
	/**
	 * 
	 * @param userPreference
	 */
	private void sendRegistrationToServer(UserPreference userPreference) {
		if (userPreference == null) {
			return;
		}
		if (userPreference.getCareType().equalsIgnoreCase(String.valueOf(CareType.CareRecipient.name()))) {
			Log.i(TAG, "Updating care recipient...");
			final HttpCareReceiver user = new HttpCareReceiver();
			user.setPhoneNumber(userPreference.getPhoneNumber());
			user.setPinNumber(userPreference.getPinNumber());
			user.setUuid(userPreference.getUuid());
			user.setRegistrationId(userPreference.getRegistrationId());

			user.setActivated(1);
			user.setActivateTime(System.currentTimeMillis());
			user.setConnected(1);
			user.setConnectTime(System.currentTimeMillis());
			SimpleLocation location = new SimpleLocation(this.getApplicationContext());
			if (location != null) {
				final double latitude = location.getLatitude();
				final double longitude = location.getLongitude();
				user.setLatitude(latitude);
				user.setLongitude(longitude);
			}
			new Thread() {
				@Override
				public void run() {
					String url = CoreConfig.getUrl("/user/carereceiver/registrationId");
					Log.i(TAG, "url=" + url);
					HttpJSONClient client = new DefaultHttpJSONClient();
					try {
						Gson gson = new Gson();
						String jsonRequest = gson.toJson(user);
						Log.i(TAG, "jsonRequest=" + jsonRequest);
						String response = client.executePost(url, jsonRequest);
						Log.i(TAG, "response=" + response);
						log(TAG, TAG + ".CareRecipient's registration id was updated at  " + new java.util.Date());
					} catch (Exception e1) {
						Log.i(TAG, "Error="+ e1.getMessage());
					}
				}
			}.start();
		} else if (userPreference.getCareType().equalsIgnoreCase(String.valueOf(CareType.CareGiver.name()))) {
			Log.i(TAG, "Updating care giver...");
			final HttpCareGiver user = new HttpCareGiver();
			user.setRegistrationId(userPreference.getRegistrationId());
			user.setEmailAddress(userPreference.getEmailAddress());
			new Thread() {
				@Override
				public void run() {
					String url = CoreConfig.getUrl("/user/caregiver/registrationId");
					Log.i(TAG, "url=" + url);
					HttpJSONClient client = new DefaultHttpJSONClient();
					try {
						Gson gson = new Gson();
						String jsonRequest = gson.toJson(user);
						Log.i(TAG, "jsonRequest=" + jsonRequest);
						String response = client.executePut(url, jsonRequest);
						Log.i(TAG, "response=" + response);
						log(TAG, TAG + ".CareGiver's registration id was updated at  " + new java.util.Date());
					} catch (Exception e1) {
						Log.i(TAG, "Error="+ e1.getMessage());
					}
				}
			}.start();
		}
	}

	/**
	 * @return the isRunning
	 */
	public static boolean isRunning() {
		return isRunning;
	}
}
