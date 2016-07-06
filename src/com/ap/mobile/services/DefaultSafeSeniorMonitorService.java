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

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.ap.common.models.http.HttpDeviceHealth;
import com.ap.common.models.http.HttpResponseMessage;
import com.ap.core.device.CareType;
import com.ap.core.device.Constants;
import com.ap.core.device.CoreConfig;
import com.ap.core.device.context.UserPreference;
import com.ap.core.device.http.DefaultHttpJSONClient;
import com.ap.core.device.http.HttpJSONClient;
import com.ap.core.device.utils.JSONUtils;
import com.ap.mobile.model.CommandInfo;
import com.ap.mobile.model.CommandType;
import com.ap.mobile.model.UISensorDevice;
import com.ap.mobile.services.DefaultSafeSeniorService.DefaultSafeSeniorServiceBinder;
import com.ap.mobile.services.RegistrationIntentService.RegistrationIntentServiceBinder;
import com.ap.mobile.util.Utils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * @author jungwhan 
 * DefaultSafeSeniorMonitorService.java 12:49:44 AM Mar 2, 2016
 * 
 */
public class DefaultSafeSeniorMonitorService extends AbstractService {
	protected static final String TAG = DefaultSafeSeniorMonitorService.class.getSimpleName();

	protected DefaultSafeSeniorService safeSeniorService;
	
	protected RegistrationIntentService registrationIdService;

	protected boolean isResetRequested = false;

	protected boolean isBound = false;
	
	private static boolean isRunning = false;
	
	private boolean canContinue = true;
	
	private long timestamp;
	
	private int batteryLevel;
	
	private final static int monitorInterval = 1000 * 60 * 30;
	
	private BluetoothAdapter mBluetoothAdapter;

	public class DefaultSafeSeniorMonitorServiceBinder extends Binder {
		public DefaultSafeSeniorMonitorService getService() {
			return DefaultSafeSeniorMonitorService.this;
		}
	}

	private final IBinder safeSeniorMonitorServiceBinder = new DefaultSafeSeniorMonitorServiceBinder();

	@Override
	public IBinder onBind(Intent arg0) {
		return safeSeniorMonitorServiceBinder;
	}

	@Override
	public void onCreate() {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();  
		try {
			unregisterReceiver(this.mBatInfoReceiver);
		} catch (Exception e){
			//
		}
		final Context context = this;
		if (!isBound) {
			Intent registrationServiceIntent = new Intent(context, RegistrationIntentService.class);
			Intent safeSeniorServiceIntent = new Intent(context, DefaultSafeSeniorService.class);
			try {
				context.bindService(safeSeniorServiceIntent, safeSeniorServiceConnection, Context.BIND_AUTO_CREATE);
				isBound = true;
			} catch (Throwable t) {
				//
			}
			try {
				context.bindService(registrationServiceIntent, registrationServiceConnection, Context.BIND_AUTO_CREATE);
			} catch (Throwable t) {
				//
			}
		}
		registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		try {
			if (serviceCommandReceiver != null) {
				LocalBroadcastManager.getInstance(this.getApplicationContext()).unregisterReceiver(serviceCommandReceiver);
			}
		} catch (Throwable t) {
			//
		}
		LocalBroadcastManager.getInstance(this.getApplicationContext()).registerReceiver(serviceCommandReceiver, new IntentFilter(Constants.MESSAGE_COMMAND_QUEUE));
	}

	private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context arg0, Intent intent) {
			batteryLevel = intent.getIntExtra("level", 0);

//			Log.i(TAG, String.valueOf(batteryLevel) + "%");

			int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
			boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
			int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
			boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;

			boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

			if (usbCharge || acCharge || isCharging) {
				canContinue = true;
			} else {
				if (batteryLevel < 20) {
					canContinue = false;
				}
			}
		}
	};
	
	private BroadcastReceiver serviceCommandReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context arg0, Intent intent) {
			CommandInfo comamndInfo = (CommandInfo)intent.getSerializableExtra(Constants.MESSAGE_COMMAND);
			Log.i(TAG, "comamndInfo="+ comamndInfo);
			if (comamndInfo == null) {
				return;
			}
			CommandType commandType = CommandType.getCommandType(comamndInfo.getCommand());
			Log.i(TAG, "commandType="+ commandType);
			if (commandType == null) {
				return;
			}
			log(TAG, TAG + ".serviceCommandReceiver received message("+ commandType.getCommand() +" at "+ new java.util.Date());
			UserPreference userPreference = UserPreference.getCurrentUserPreference(DefaultSafeSeniorMonitorService.this.getApplicationContext());
			Log.i(TAG, "userPreference="+ userPreference);
			if (userPreference == null) {
				return;
			}
			if (!comamndInfo.getUuid().equals(userPreference.getUuid())) {
				Log.i(TAG, "uuid is different.");
				Log.i(TAG, "comamndInfo.getUuid()="+ comamndInfo.getUuid());
				Log.i(TAG, "userPreference.getUuid()="+ userPreference.getUuid());
				return;
			}
			long timelapse = System.currentTimeMillis() - timestamp;
			if (timelapse != 0 && timelapse < (1000 * 60 * 5)) {
				return;
			}
			timestamp = System.currentTimeMillis();
			String command = commandType.getCommand();
			if (command.equals(CommandType.RESET.name())) {
				if (safeSeniorService == null) {
					Log.i(TAG, "safeSeniorService is null.");
					return;
				}
				safeSeniorService.resetSensors();
				try {
					Thread.sleep(1000 * 10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				Intent safeSeniorServiceIntent = new Intent(DefaultSafeSeniorMonitorService.this.getApplicationContext(), DefaultSafeSeniorService.class);
				Log.i(TAG, "Restarting safeSeniorServiceIntent @ " + SystemClock.elapsedRealtime());
				DefaultSafeSeniorMonitorService.this.getApplicationContext().startService(safeSeniorServiceIntent);
			}
			if (command.equals(CommandType.CHECKSTATUS.name())) {
				executeCheckStatus();
			}
			if (command.equals(CommandType.RENEW_REGISTRATION_ID.name())) {
				renewRegistrationId();
			}
		}
	};
	
	private void renewRegistrationId() {
		Intent RegistrationIntentServiceIntent = new Intent(this, RegistrationIntentService.class);
		Log.i(TAG, "Starting RegistrationIntentService @ " + SystemClock.elapsedRealtime());
		startService(RegistrationIntentServiceIntent);
	}
	
	private void executeCheckStatus() {
		try {
			UserPreference userPreference = UserPreference.getCurrentUserPreference(DefaultSafeSeniorMonitorService.this.getApplicationContext());
			if (userPreference == null) {
				return;
			}
			String pinNumber = userPreference.getPinNumber();
			String uuid = userPreference.getUuid();
			HttpDeviceHealth deviceHealth = new HttpDeviceHealth();
			deviceHealth.setPinNumber(pinNumber);
			deviceHealth.setUuid(uuid);
			String deviceInfo = Utils.getPreferenceValue(DefaultSafeSeniorMonitorService.this.getApplicationContext(), Constants.PREFERENCE_KEY_ALL_SENSORS);
			Log.i(TAG, "deviceInfo="+ deviceInfo);
			if (deviceInfo == null) {
				return;
			}
			Type responseMessageType = new TypeToken<HttpResponseMessage<List<UISensorDevice>>>() {
			}.getType();
			HttpResponseMessage<List<UISensorDevice>> responseMessage = JSONUtils.getTypeResponseMessage(deviceInfo, responseMessageType);
			List<UISensorDevice> sensorDevices = responseMessage.getObject();
			Map<String, Boolean> connections = safeSeniorService.getSensorConnections();
			for (UISensorDevice sensorDevice : sensorDevices) {
				Boolean status = connections.get(sensorDevice.getSensorName());
				if (status == null) {
					status = Boolean.FALSE;
				}
				deviceHealth.addDeviceHealth(sensorDevice.getMacAddress(), status);
			}
			Log.i(TAG, "deviceHealth="+ deviceHealth);
			Gson gson = new Gson();
			final String requestBody = gson.toJson(deviceHealth);
			new Thread() {
				@Override
				public void run() {
					String url = CoreConfig.getUrl("/sensor/health");
					HttpJSONClient client = new DefaultHttpJSONClient();
					try {
						Log.i(TAG, "requestBody=" + requestBody);
						String response = client.executePost(url, requestBody);
						Log.i(TAG, "response=" + response);
					} catch (Exception e1) {
						Log.i(TAG, "errorMessage="+ e1.getMessage());
					}
				}
			}.start();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		log(TAG, TAG + ".DefaultSafeSeniorMonitorService.onDestroy() is called at "+ new java.util.Date());
		if (isBound) {
			this.unbindService(safeSeniorServiceConnection);
			this.unbindService(registrationServiceConnection);
		}
//		this.unregisterReceiver(mBatInfoReceiver);
		isRunning = false;
		mBluetoothAdapter = null;
		Log.i(TAG, "DefaultSafeSeniorMonitorService.onDestroy() is called.");
	}
	

	/*
	 * (non-Javadoc)
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		UserPreference currentUserPreference = UserPreference.getCurrentUserPreference(DefaultSafeSeniorMonitorService.this.getApplicationContext());
		if (currentUserPreference != null && currentUserPreference.getCareType() != null && currentUserPreference.getCareType().equals(CareType.CareGiver.name())) {
			Log.i(TAG, "This is a care giver's mode.");
			return Service.START_NOT_STICKY; 
		}
		Log.i(TAG, "DefaultSafeSeniorMonitorService is starting... ");
		final Context context = this;
		final DefaultSafeSeniorMonitorService thisInstance = this;
		new Thread() {
			@Override
			public void run() {
				isRunning = true;
				try {
					Thread.sleep(monitorInterval);
				} catch (InterruptedException e) {
					//
				}
				log(TAG, TAG + ".checked "+ RegistrationIntentService.isRunning());
				if (!RegistrationIntentService.isRunning()) {
					log(TAG, TAG + ".checked registrationService. it stopped. so restarting...");
					Intent registrationServiceIntent = new Intent(context, RegistrationIntentService.class);
					Log.i(TAG, "Starting RegistrationService @ " + new java.util.Date());
					startService(registrationServiceIntent);
				}
				log(TAG, TAG + ".DefaultSafeSeniorMonitorService woke up at "+ new java.util.Date());
				long ct = System.currentTimeMillis();
				AlarmManager mgr = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
				Intent safeSeniorIntent = new Intent(getApplicationContext(), DefaultSafeSeniorMonitorService.class);
				PendingIntent pi = PendingIntent.getService(getApplicationContext(), 0, safeSeniorIntent, 0);

				mgr.set(AlarmManager.RTC_WAKEUP, ct + (1000 * 60 * 10), pi);
				
				synchronized (thisInstance) {
					if (isResetRequested) {
						Log.i(TAG, "Reset has been requested, so skip this time. " + new java.util.Date());
						return;
					}
					Log.i(TAG, "canContinue=" + canContinue);
					if (!canContinue) {
						if (safeSeniorService != null) {
							safeSeniorService.resetSensors();
						}
						Log.i(TAG, "batter is low. ");
						return;
					}
				}
				if (safeSeniorService == null) {
					log(TAG, TAG + ".safeSeniorService is null.");
					Log.i(TAG, "safeSeniorService is null.");
					return;
				}
				// Check if a user's bluetooth is turned on.
				if (!mBluetoothAdapter.isEnabled()) {
				    mBluetoothAdapter.enable(); 
				}
				int limit = 0;
				while (true) {
					if (mBluetoothAdapter.isEnabled()) {
						limit = 0;
						break;
					}
					try {
						limit++;
						Thread.sleep(1000 * 60 * 2);	// 2 minutes sleep
					} catch (InterruptedException e) {
						//
					}
					if (limit >= 10) {
						break;
					}
				}
				if (limit > 0) {
					return;
				}
				if (!mBluetoothAdapter.isEnabled()) {
					// Update a care receiver's status
					return;
				}
				if (safeSeniorService.getSensorContext().isInRetryWork()) {
					Log.i(TAG, "The service is in retry logic.");
					return;
				}
				
				Log.i(TAG, "DefaultSafeSeniorMonitorService woke up and checked... at " + new java.util.Date());
				Map<String, Boolean> sensorStatuses = safeSeniorService.getSensorConnections();
				Log.i(TAG, "DefaultSafeSeniorMonitorService.sensorStatuses=" + sensorStatuses + "==" + new java.util.Date());
				if (sensorStatuses == null || sensorStatuses.isEmpty()) {
					Intent safeSeniorServiceIntent = new Intent(context, DefaultSafeSeniorService.class);
					Log.i(TAG, "Starting service @ " + new java.util.Date());
					startService(safeSeniorServiceIntent);
				} else {
					log(TAG, TAG + ". sensor statuses is not empty, but all set to false. checked at "+ new java.util.Date());
				}
			}
		}.start();
		Log.i(TAG, "DefaultSafeSeniorMonitorService has started... ");
		return Service.START_STICKY;
	}

	protected ServiceConnection safeSeniorServiceConnection = new ServiceConnection() {
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
	
	protected ServiceConnection registrationServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			RegistrationIntentServiceBinder binder = (RegistrationIntentServiceBinder) service;
			Log.i(TAG, "onServiceConnected is connected.");
			registrationIdService = binder.getService();
			isBound = true;
		}

		public void onServiceDisconnected(ComponentName arg0) {
			isBound = false;
		}
	};
	

	/**
	 * @param isResetRequested the isResetRequested to set
	 */
	public synchronized void setResetRequested(boolean isResetRequestedLocal) {
		Log.i(TAG, "setResetRequested is set to "+ isResetRequestedLocal);
		this.isResetRequested = isResetRequestedLocal;
	}

	/**
	 * @return the isRunning
	 */
	public static boolean isRunning() {
		return isRunning;
	}
}
