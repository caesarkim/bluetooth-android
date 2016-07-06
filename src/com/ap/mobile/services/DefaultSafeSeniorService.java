/**
 * AgingPlaceMobile
 */
package com.ap.mobile.services;

import im.delight.android.location.SimpleLocation;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.ap.common.models.http.HttpCareReceiver;
import com.ap.common.models.http.HttpConnectTime;
import com.ap.common.models.http.HttpResponseMessage;
import com.ap.common.models.http.HttpStatusConstants;
import com.ap.core.device.CareType;
import com.ap.core.device.ConnectType;
import com.ap.core.device.Constants;
import com.ap.core.device.CoreConfig;
import com.ap.core.device.ErrorCodes;
import com.ap.core.device.components.SensorDiscoveryException;
import com.ap.core.device.components.analyzer.DefaultRetryConnectionDistanceAnalyzer;
import com.ap.core.device.components.analyzer.DefaultSensorDataAnalyzer;
import com.ap.core.device.context.SensorContext;
import com.ap.core.device.context.UserPreference;
import com.ap.core.device.exceptions.DeviceErrorException;
import com.ap.core.device.exceptions.DeviceException;
import com.ap.core.device.http.DefaultHttpJSONClient;
import com.ap.core.device.http.HttpJSONClient;
import com.ap.core.device.utils.JSONUtils;
import com.ap.mobile.components.SensorDiscoveryConnectionManager;
import com.ap.mobile.model.UISensorDevice;
import com.ap.mobile.services.DefaultSafeSeniorMonitorService.DefaultSafeSeniorMonitorServiceBinder;
import com.ap.mobile.ui.adapter.DefaultBluetoothDiscoveryConnectorProviderAdapter;
import com.ap.mobile.util.UIUtils;
import com.ap.mobile.util.Utils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */

/**
 * @author jungwhan DefaultSafeMonitorService.java 11:46:06 PM Feb 26, 2016 2016
 */
public class DefaultSafeSeniorService extends AbstractService implements SafeSeniorService {
	protected static final String TAG = DefaultSafeSeniorService.class.getSimpleName();

	private List<DeviceErrorException> exceptions = new ArrayList<DeviceErrorException>();
	
	private boolean isBound = false;
	
	private boolean isRunning = false;
	
	private Thread bluetoothConnectionThread;
	
	private DefaultSafeSeniorMonitorService safeSeniorMonitorService;
	
	private Thread connectionStatusUploader;
	
	private SensorContext sensorContext = new SensorContext();
	
	/**
	 * Constructor
	 */
	public DefaultSafeSeniorService() {
		//
	}

	public class DefaultSafeSeniorServiceBinder extends Binder {
		public DefaultSafeSeniorService getService() {
			return DefaultSafeSeniorService.this;
		}
	}
	
	/**
	 * 
	 */
    @Override
    public void onDestroy() {
        super.onDestroy();
        log(TAG, TAG + "==DefaultSafeSeniorService.onDestroy() is called.");
        if (isBound) {
        	this.unbindService(serviceConnection);
        }
		isRunning = false;
        Log.i(TAG, "DefaultSafeSeniorService.onDestroy() is called.");
    }
    
	/*
	 * 
	 */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "DefaultSafeSeniorService.onCreate() is called.");
        LocalBroadcastManager.getInstance(this.getApplicationContext()).registerReceiver(mMessageReceiver, new IntentFilter(Constants.MESSAGE_SERVICE_QUEUE));
    }

	private final IBinder safeSeniorServiceBinder = new DefaultSafeSeniorServiceBinder();

	@Override
	public IBinder onBind(Intent arg0) {
		return safeSeniorServiceBinder;
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.IntentService#onHandleIntent(android.content.Intent)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		UserPreference userPreference = UserPreference.getCurrentUserPreference(this.getApplicationContext());
		if (userPreference != null && userPreference.getCareType() != null && userPreference.getCareType().equals(CareType.CareGiver.name())) {
			Log.i(TAG, "This is a care giver's mode.");
			return Service.START_NOT_STICKY; 
		}
		
		Log.i(TAG, "onHandleIntent is called at "+ new java.util.Date());
		final Context context = this;
		if (userPreference == null) {
			userPreference = new UserPreference();
		}
		final String pinNumber = userPreference.getPinNumber();
		final String phoneNumber = userPreference.getPhoneNumber();
		Log.i(TAG, "pinNumber=" + pinNumber);
		Log.i(TAG, "phoneNumber=" + phoneNumber);
		if (pinNumber == null || pinNumber.isEmpty()) {
			return Service.START_STICKY;
		}
		if (!isBound) {
			Intent safeSeniorMonitorServiceIntent = new Intent(context, DefaultSafeSeniorMonitorService.class);
			isBound = true;
			try {
				bindService(safeSeniorMonitorServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
			} catch (Throwable t) {
				//
			}
		}
		
		// add data
		sensorContext.setUiContext(context);
		final SensorDiscoveryConnectionManager sensorDiscoveryConnectionManager = SensorDiscoveryConnectionManager.getInstance();
		sensorDiscoveryConnectionManager.setSensorContext(sensorContext);
		DefaultBluetoothDiscoveryConnectorProviderAdapter bluetoothDiscoveryConnectionAdapter = new DefaultBluetoothDiscoveryConnectorProviderAdapter();
		try {
			bluetoothDiscoveryConnectionAdapter.initialize(context, sensorDiscoveryConnectionManager);
			sensorDiscoveryConnectionManager.init();
		} catch (SensorDiscoveryException e) {
			Log.e(TAG, e.getMessage());
		}
		exceptions.clear();
		bluetoothConnectionThread = new Thread() {
			@Override
			public void run() {
				final Intent messageQueueIntent = new Intent(Constants.MESSAGE_QUEUE);
				Log.i(TAG, "onHandleIntent.thread is starting at "+ new java.util.Date());
				String message = Utils.getPreferenceValue(context, Constants.PREFERENCE_KEY_ALL_SENSORS);
				try {
					Log.i(TAG, "cached message=" + message);
					if (message == null || message.isEmpty()) {
						String url = CoreConfig.getUrl("/sensors/" + pinNumber);
						HttpJSONClient client = new DefaultHttpJSONClient();
						message = client.executeGet(url);
					}
					Utils.savePreference(context, Constants.PREFERENCE_KEY_ALL_SENSORS, message);

					Log.i(TAG, "responseMessage=" + message);
				} catch (Exception e1) {
					e1.printStackTrace();
					exceptions.add(new DeviceErrorException(ErrorCodes.CODE_CANNOT_GET_SENSORS, "Server Error"));
					Bundle args = new Bundle();
					args.putSerializable(Constants.ERROR_BUNDLE, (Serializable) exceptions);
					messageQueueIntent.putExtra(Constants.MESSAGE_QUEUE_KEY, args);
					messageQueueIntent.putExtra(Constants.MESSAGE_PROGRESS_STATUS, Constants.MESSAGE_PROGRESS_ERROR);
					LocalBroadcastManager.getInstance(context).sendBroadcast(messageQueueIntent);
					return;
				}
				if (exceptions != null && !exceptions.isEmpty()) {
					Bundle args = new Bundle();
					args.putSerializable(Constants.ERROR_BUNDLE, (Serializable) exceptions);
					messageQueueIntent.putExtra(Constants.MESSAGE_QUEUE_KEY, args);
					messageQueueIntent.putExtra(Constants.MESSAGE_PROGRESS_STATUS, Constants.MESSAGE_PROGRESS_ERROR);
					LocalBroadcastManager.getInstance(context).sendBroadcast(messageQueueIntent);
					return;
				}
				Type responseMessageType = new TypeToken<HttpResponseMessage<List<UISensorDevice>>>() {
				}.getType();
				HttpResponseMessage<List<UISensorDevice>> responseMessage = JSONUtils.getTypeResponseMessage(message, responseMessageType);
				if (responseMessage.getStatusCode() == HttpStatusConstants.STATUS_WRONG_PIN) {
					exceptions.add(new DeviceErrorException(ErrorCodes.CODE_ACTIVATION_FAILURE, "Pin Number does not exist."));
					Bundle args = new Bundle();
					args.putSerializable(Constants.ERROR_BUNDLE, (Serializable) exceptions);
					messageQueueIntent.putExtra(Constants.MESSAGE_QUEUE_KEY, args);
					messageQueueIntent.putExtra(Constants.MESSAGE_PROGRESS_STATUS, Constants.MESSAGE_PROGRESS_ERROR);
					LocalBroadcastManager.getInstance(context).sendBroadcast(messageQueueIntent);
					return;
				}
				List<UISensorDevice> sensorDevices = responseMessage.getObject();
				if (sensorDevices == null || sensorDevices.isEmpty()) {
					exceptions.add(new DeviceErrorException(ErrorCodes.CODE_NO_PURCHASE_SENSORS, "You haven't purchased sensors."));
					Bundle args = new Bundle();
					args.putSerializable(Constants.ERROR_BUNDLE, (Serializable) exceptions);
					messageQueueIntent.putExtra(Constants.MESSAGE_QUEUE_KEY, args);
					messageQueueIntent.putExtra(Constants.MESSAGE_PROGRESS_STATUS, Constants.MESSAGE_PROGRESS_ERROR);
					LocalBroadcastManager.getInstance(context).sendBroadcast(messageQueueIntent);
					return;
				}
				if (exceptions != null && !exceptions.isEmpty()) {
					Bundle args = new Bundle();
					args.putSerializable(Constants.ERROR_BUNDLE, (Serializable) exceptions);
					messageQueueIntent.putExtra(Constants.MESSAGE_QUEUE_KEY, args);
					messageQueueIntent.putExtra(Constants.MESSAGE_PROGRESS_STATUS, Constants.MESSAGE_PROGRESS_ERROR);
					LocalBroadcastManager.getInstance(context).sendBroadcast(messageQueueIntent);
					return;
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					//
				}
				startConnections(sensorDevices, sensorDiscoveryConnectionManager, sensorContext, messageQueueIntent);
				Log.i(TAG, "Sensor connections are finished.");
				if (exceptions == null || exceptions.isEmpty()) {
					uploadConnectionStatus(sensorDevices);
					safeSeniorMonitorService.setResetRequested(false);
					messageQueueIntent.putExtra(Constants.MESSAGE_PROGRESS_STATUS, Constants.MESSAGE_PROGRESS_FINISH);
					LocalBroadcastManager.getInstance(context).sendBroadcast(messageQueueIntent);
					updateUserInformation(pinNumber, phoneNumber);
					try {
						updateConnectionTime(sensorDevices);
					} catch (IOException e) {
						//
					}
					isRunning = true;
					return;
				} else {
					Bundle args = new Bundle();
					args.putSerializable(Constants.ERROR_BUNDLE, (Serializable) exceptions);
					messageQueueIntent.putExtra(Constants.MESSAGE_QUEUE_KEY, args);
					messageQueueIntent.putExtra(Constants.MESSAGE_PROGRESS_STATUS, Constants.MESSAGE_PROGRESS_ERROR);
					LocalBroadcastManager.getInstance(context).sendBroadcast(messageQueueIntent);
					try {
						sensorDiscoveryConnectionManager.cancelDiscovery();
					} catch (Exception e) {
						//
					}
					return;
				}
			}
		};
		bluetoothConnectionThread.start();
		Log.i(TAG, "OnStartCommand has finished.");
		Log.i(TAG, "DefaultSafeSeniorService is finished.");
		return Service.START_STICKY;
	}
	
	private void retryConnectToDevices(Context context) {
		log(TAG, TAG + ".retryConnectToDevices started at "+ new java.util.Date());
		Log.i(TAG, "retryConnectToDevices started");
		if (sensorContext.isSensorsConnected()) {
			isRunning = true;
			return;
		}
		sensorContext.setUiContext(DefaultSafeSeniorService.this);
		sensorContext.setInRetryWork(true);
		try {
			String message = Utils.getPreferenceValue(context, Constants.PREFERENCE_KEY_ALL_SENSORS);
			Type responseMessageType = new TypeToken<HttpResponseMessage<List<UISensorDevice>>>() {
			}.getType();
			HttpResponseMessage<List<UISensorDevice>> responseMessage = JSONUtils.getTypeResponseMessage(message, responseMessageType);
			List<UISensorDevice> sensorDevices = responseMessage.getObject();
			DefaultRetryConnectionDistanceAnalyzer retryConnectionDistanceAnalyzer = new DefaultRetryConnectionDistanceAnalyzer(sensorContext);
			SensorDiscoveryConnectionManager sensorDiscoveryConnectionProvider = SensorDiscoveryConnectionManager.getInstance();
			while (true) {
				try {
					retryConnectionDistanceAnalyzer.analyze();
					long interval = retryConnectionDistanceAnalyzer.getInterval();
					log(TAG, TAG + ".retryConnectToDevices is called at "+ new java.util.Date());
					Log.i(TAG, "Interval is "+ interval);
					try {
						Thread.sleep(interval);
					} catch (InterruptedException e) {
						//
					}

					// Need to fix. It picks one of the devices at random and keeps trying to connect. 
					// Need to create another method just to see if it can connect.
					if (sensorContext.isSensorsConnected()) {
						isRunning = true;
						break;
					} else {
						sensorDiscoveryConnectionProvider.checkConnection();
						startConnections(sensorDevices, sensorDiscoveryConnectionProvider, sensorContext, null);
						sensorContext.setSensorsConnected(true);
						Log.i(TAG, "All the sensor reconnections are successful.....");
						sensorContext.getCareReceiverConnectionInformation().addConnectTime(new HttpConnectTime(ConnectType.CONNECTED.getConnectType(), System.currentTimeMillis()));
						int connectTimeInfoCount = sensorContext.getCareReceiverConnectionInformation().getConnectTime().size();
						Log.i(TAG, "connectTimeInfoCount="+ connectTimeInfoCount);
						Log.i(TAG, "careReceiverConnectionInformation="+ sensorContext.getCareReceiverConnectionInformation());
						if (connectTimeInfoCount >= 10) {
							// Upload care receiver's connect/disconnect time.
//							uploadCareReceiverConnectionTimesInfo(sensorContext.getCareReceiverConnectionInformation());
							sensorContext.getCareReceiverConnectionInformation().getConnectTime().clear();
						}
						break;
					}
				} catch (DeviceException e) {
					Log.i(TAG, "tryReconnection.Reconnection failed with "+ e.getMessage());
					sensorContext.setSensorsConnected(false);
				} catch (Exception e) {
					Log.i(TAG, "tryReconnection.Reconnection failed with "+ e.getMessage());
					sensorContext.setSensorsConnected(false);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sensorContext.setInRetryWork(false);				
		}
		Log.i(TAG, "retryConnectToDevices is finished.");
		log(TAG, TAG + ".retryConnectToDevices finished at "+ new java.util.Date());
	}
	
	/**
	 * message receiver
	 */
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, Intent intent) {
			// Extract data included in the Intent
			String commandName = intent.getStringExtra(Constants.MESSAGE_COMMAND_QUEUE);
			Log.i(TAG, "commandName is "+ commandName);
			if (commandName == null || commandName.isEmpty()) {
				return;
			}
			if (commandName.equals(Constants.MESSAGE_SENSOR_DISCONNECTED)) {
				if (sensorContext.isInRetryWork()) {
					return;
				}
				new Thread() {
					@Override
					public void run() {
						retryConnectToDevices(context);
					}
				}.start();
			}

			Log.i(TAG, "onReceive is finished.");
		}
	};
	
	private void uploadConnectionStatus(final List<UISensorDevice> sensorDevices) {
		if (connectionStatusUploader != null) {
			try {
				connectionStatusUploader.interrupt();
			} catch (Exception e) {
				//
			}
		}
		connectionStatusUploader = new Thread() {
			@Override
			public void run() {
				int CONNECTION_STATUS_CHECK_INTERVAL = (1000 * 60 * 60 * 2);
				while (true) {
					Log.i(TAG, "uploadConnectionStatus has started.");
					try {
						Thread.sleep(CONNECTION_STATUS_CHECK_INTERVAL);
					} catch (InterruptedException e) {
						Log.i(TAG, "error="+ e.getMessage());
					}
					try {
						updateConnectionTime(sensorDevices);
						CONNECTION_STATUS_CHECK_INTERVAL = (1000 * 60 * 60 * 2);
					} catch (Exception e) {
						CONNECTION_STATUS_CHECK_INTERVAL = (1000 * 60 * 30);
						//
					}
				}
			}
		};
		connectionStatusUploader.start();
		Log.i(TAG, "uploadConnectionStatus has started.");
	}
	
	private void updateConnectionTime(final List<UISensorDevice> sensorDevices) throws IOException {
		HttpConnectTime connectionTime = null;
		List<HttpConnectTime> connectTimes = new ArrayList<HttpConnectTime>();
		for (UISensorDevice sensorDevice : sensorDevices) {
			if (sensorDevice.isConnected()) {
				connectionTime = new HttpConnectTime(ConnectType.CONNECTED.getConnectType(), System.currentTimeMillis());
			} else {
				connectionTime = new HttpConnectTime(ConnectType.DISCONNECTED.getConnectType(), System.currentTimeMillis());
			}
			connectionTime.setSensorId(sensorDevice.getSensorId());
			connectTimes.add(connectionTime);
		}
		String url = CoreConfig.getUrl("/user/carereceiver/connected_time");
		DefaultHttpJSONClient client = new DefaultHttpJSONClient();
		String requestBody = JSONUtils.toJSON(connectTimes);
		try {
			client.executePost(url, requestBody);
		} catch (IOException e) {
			throw e;
		}
	}
	
	private void updateUserInformation(String pinNumber, String phoneNumber) {
		if (pinNumber == null) {
			throw new IllegalArgumentException("pinNumber can't be null.");
		}
		if (phoneNumber == null) {
			throw new IllegalArgumentException("phoneNumber can't be null.");
		}
		String registrationId = Utils.getPreferenceValue(this.getApplicationContext(), Constants.USER_REGISTRATION_ID);
		pinNumber = pinNumber.trim();
		phoneNumber = phoneNumber.trim();
		UserPreference currentUserPreference = UserPreference.getCurrentUserPreference(this.getApplicationContext());
		String userUUID = null;
		if (currentUserPreference != null) {
			userUUID = currentUserPreference.getUuid();
		}
		if (userUUID == null || userUUID.isEmpty()) {
			userUUID = UUID.randomUUID().toString() + "-" + UIUtils.genCharacters();
		}
		UserPreference userPreference = new UserPreference();
		userPreference.setCareType(String.valueOf(CareType.CareRecipient.name()));
		userPreference.setPinNumber(pinNumber.trim());
		userPreference.setPhoneNumber(phoneNumber.trim());
		userPreference.setUuid(userUUID);
		userPreference.setActivated(true);
		userPreference.setRegistrationId(registrationId);

		HttpCareReceiver user = new HttpCareReceiver();
		user.setPhoneNumber(phoneNumber);
		user.setPinNumber(pinNumber);
		user.setUuid(userUUID);
		user.setRegistrationId(registrationId);
		user.setDeviceBrand(Build.BRAND);
		user.setDeviceManufacturer(Build.MANUFACTURER);
		user.setDeviceModel(Build.MODEL);
		user.setPlatform(Constants.PLATFORM);
		TimeZone tz = TimeZone.getDefault();
		String timezone = tz.getDisplayName(false, TimeZone.SHORT) +" Timezon id :: " +tz.getID();
		user.setTimezone(timezone);
		user.setActivated(1);
		user.setActivateTime(System.currentTimeMillis());
		user.setConnected(1);
		user.setConnectTime(System.currentTimeMillis());

		SimpleLocation location = new SimpleLocation(this.getApplicationContext());
		if (location != null) {
			final double latitude = location.getLatitude();
			final double longitude = location.getLongitude();
			Log.i(TAG, "$$$$$$$$ location is " + latitude + "==" + longitude);
			userPreference.setLatitude(latitude);
			userPreference.setLongitude(longitude);
			user.setLatitude(latitude);
			user.setLongitude(longitude);
		}
		Log.i(TAG, "userPreference.getJson()=" + userPreference.getJson());
		Utils.writePreference(this.getApplicationContext(), Constants.PREFERENCE_KEY_USER_PREFERENCE, userPreference.getJson());

		String url = CoreConfig.getUrl("/user/carereceiver");
		HttpJSONClient client = new DefaultHttpJSONClient();
		try {
			Gson gson = new Gson();
			String jsonRequest = gson.toJson(user);
			Log.i(TAG, "jsonRequest=" + jsonRequest);
			String response = client.executePost(url, jsonRequest);
			Log.i(TAG, "response=" + response);
		} catch (Exception e1) {
			Log.i(TAG, "errorMessage="+ e1.getMessage());
		}
	}
	
	public void startConnections(List<UISensorDevice> sensorDevices, SensorDiscoveryConnectionManager sensorDiscoveryConnectionManager, SensorContext sensorContext, Intent messageQueueIntent) {
		for (UISensorDevice uiSensorDevice : sensorDevices) {
			Log.i(TAG, "starting a sensor("+ uiSensorDevice.getSensorName()  +") at " + new java.util.Date());
			BluetoothDevice bluetoothDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(uiSensorDevice.getMacAddress());
			try {
				if (messageQueueIntent != null) {
					messageQueueIntent.putExtra(Constants.MESSAGE_QUEUE_KEY, "Pairing " + uiSensorDevice.getSensorName() + "...");
					LocalBroadcastManager.getInstance(sensorContext.getUiContext()).sendBroadcast(messageQueueIntent);					
				}
				pairDevice(bluetoothDevice, uiSensorDevice);
				Thread.sleep(1000);
			} catch (Exception e) {
				DeviceErrorException errorException = new DeviceErrorException(ErrorCodes.CODE_ACTIVATION_FAILURE, uiSensorDevice.getSensorName() + ": Pairing failure.");
				sensorDiscoveryConnectionManager.cancelDiscovery();
				exceptions.add(errorException);
				return;
			}
		}
		
		DefaultSensorDataAnalyzer dataAnaylzer = new DefaultSensorDataAnalyzer(sensorContext);
		sensorDiscoveryConnectionManager.setRetryConnectionDistanceAnalyzer(new DefaultRetryConnectionDistanceAnalyzer(sensorContext));
		sensorDiscoveryConnectionManager.addPacketReceiver(dataAnaylzer);
		for (UISensorDevice uiSensorDevice : sensorDevices) {
			Log.i(TAG, "sensorDeviceTemp=" + uiSensorDevice);
			Object LOCK = new Object();
			try {
				sensorDiscoveryConnectionManager.startConnections(uiSensorDevice, LOCK, exceptions);
			} catch (SensorDiscoveryException e) {
				e.printStackTrace();
				// pd.dismiss();
				exceptions.clear();
				DeviceErrorException errorException = new DeviceErrorException(ErrorCodes.CODE_ACTIVATION_FAILURE, uiSensorDevice.getSensorName() + ": Connection failure.");
				sensorDiscoveryConnectionManager.cancelDiscovery();
				exceptions.add(errorException);
				if (messageQueueIntent != null) {
					Bundle args = new Bundle();
					args.putSerializable(Constants.ERROR_BUNDLE, (Serializable) exceptions);
					messageQueueIntent.putExtra(Constants.MESSAGE_QUEUE_KEY, args);
					messageQueueIntent.putExtra(Constants.MESSAGE_PROGRESS_STATUS, Constants.MESSAGE_PROGRESS_ERROR);
					LocalBroadcastManager.getInstance(sensorContext.getUiContext()).sendBroadcast(messageQueueIntent);					
				}

				break;
			} finally {
				synchronized (LOCK) {
					try {
						LOCK.wait(1000 * 60 * 1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			if (messageQueueIntent != null) {
				messageQueueIntent.putExtra(Constants.MESSAGE_PROGRESS_STATUS, Constants.MESSAGE_PROGRESS_ONGOING);
				messageQueueIntent.putExtra(Constants.MESSAGE_QUEUE_KEY, "Detected " + uiSensorDevice.getSensorName() + "...");
				LocalBroadcastManager.getInstance(sensorContext.getUiContext()).sendBroadcast(messageQueueIntent);				
			}
			sensorContext.setSensorsConnected(true);
			if (!sensorDiscoveryConnectionManager.isDeviceConnected(uiSensorDevice.getSensorName())) {
				Log.i(TAG, "Connection Failure: " + uiSensorDevice.getSensorName() + " is not connected.");
				if (messageQueueIntent != null) {
					messageQueueIntent.putExtra(Constants.MESSAGE_QUEUE_KEY, uiSensorDevice.getSensorName() + " is not connected.");
				}
				uiSensorDevice.setConnected(false);
				sensorContext.setSensorsConnected(false);
				break;
			}
		}
//		DeviceConnection deviceConnection = SensorConnectionFactory.getDeviceConnection(ModuleType.BLE.name(), BluetoothAdapter.getDefaultAdapter(), null, sensorContext.getUiContext());
//		try {
////			((SensorDeviceBLEConnection))deviceConnection).setSensorDiscoveryConnectionMonitor();
//			deviceConnection.discovery();
//		} catch (DeviceException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		if (exceptions == null || exceptions.isEmpty()) {
			dataAnaylzer.initialize();
			dataAnaylzer.start();			
		}
	}
	
	public Map<String, Boolean> getSensorConnections() {
		SensorDiscoveryConnectionManager sensorDiscoveryConnectionProvider = SensorDiscoveryConnectionManager.getInstance();
		return sensorDiscoveryConnectionProvider.getSensorConnectionStatus();
	}
	
	protected ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			DefaultSafeSeniorMonitorServiceBinder binder = (DefaultSafeSeniorMonitorServiceBinder) service;
			Log.i(TAG, "onServiceConnected is connected.");
			safeSeniorMonitorService = binder.getService();
			isBound = true;
		}

		public void onServiceDisconnected(ComponentName arg0) {
			isBound = false;
		}
	};
	
	/*
	 * (non-Javadoc)
	 * @see com.ap.mobile.services.SafeSeniorService#resetSensors()
	 */
	@Override
	public synchronized void resetSensors() {
		Log.i(TAG, "resetSensors is called.");
		Utils.delete(this.getApplicationContext(), Constants.PREFERENCE_KEY_ALL_SENSORS);
		SensorDiscoveryConnectionManager sensorDiscoveryConnectionManager = SensorDiscoveryConnectionManager.getInstance();
		try {
			if (safeSeniorMonitorService == null) {
				// It's not activated yet.
				Intent safeSeniorMonitorServiceIntent = new Intent(this.getApplicationContext(), DefaultSafeSeniorMonitorService.class);
				isBound = true;
				try {
					bindService(safeSeniorMonitorServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
				} catch (Throwable t) {
					//
				}
				Thread.sleep(1000 * 5);
				if (safeSeniorMonitorService == null) {
					log(TAG, TAG + ".resetSensors() is called, but null. Checked at "+ new java.util.Date());
					return;
				}
				return;
			}
			safeSeniorMonitorService.setResetRequested(true);
			sensorDiscoveryConnectionManager.resetSensors();
		} catch (Exception e) {
			e.printStackTrace();
			safeSeniorMonitorService.setResetRequested(false);
		}
		try {
			if (bluetoothConnectionThread != null) {
				bluetoothConnectionThread.interrupt();
			}			
		} catch (Exception e) {
			//
		}
		Log.i(TAG, "resetSensors is finished.");
	}

	protected void pairDevice(BluetoothDevice device, UISensorDevice sensorDevice) throws DeviceException {
		try {
			if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
				return;
			}
			Method method = device.getClass().getMethod("createBond", (Class[]) null);
			method.invoke(device, (Object[]) null);
		} catch (Exception e) {
			throw new DeviceException("Can't pair with " + sensorDevice.getSensorType() + "==" + sensorDevice.getSensorName() + "==" + sensorDevice.getMacAddress());
		}
	}

	/**
	 * @return the sensorContext
	 */
	public SensorContext getSensorContext() {
		return sensorContext;
	}

	/**
	 * @return the isRunning
	 */
	public boolean isRunning() {
		return isRunning;
	}
}
