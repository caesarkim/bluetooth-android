/**
 * AgingPlaceMobile
 */
package com.ap.mobile.components.tasks;

/**
 * Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.ap.common.models.http.HttpResponseMessage;
import com.ap.common.models.http.HttpStatusConstants;
import com.ap.core.device.CareType;
import com.ap.core.device.Constants;
import com.ap.core.device.CoreConfig;
import com.ap.core.device.ErrorCodes;
import com.ap.core.device.context.UserPreference;
import com.ap.core.device.exceptions.DeviceErrorException;
import com.ap.core.device.exceptions.DeviceException;
import com.ap.core.device.http.DefaultHttpJSONClient;
import com.ap.core.device.http.HttpJSONClient;
import com.ap.core.device.utils.JSONUtils;
import com.ap.mobile.activities.fragment.aged.SensorActivationListener;
import com.ap.mobile.model.UISensorDevice;
import com.ap.mobile.services.DefaultSafeSeniorMonitorService;
import com.ap.mobile.services.DefaultSafeSeniorService;
import com.ap.mobile.util.Utils;
import com.google.gson.reflect.TypeToken;

/**
 * @author jungwhan SensorDeviceConnectionTask.java 2:33:31 PM Feb 13, 2016 2016
 */
public class SensorDeviceConnectionTask extends AsyncTask<Object, String, Boolean> {
	protected static final String TAG = SensorDeviceConnectionTask.class.getSimpleName();

	private ProgressDialog pd;

	private SensorActivationListener sensorActivationListener;

	private Activity activity;
	
	private final Object LOCK = new Object();

	public SensorDeviceConnectionTask(Activity activity) {
		this.activity = activity;
	}

	@Override
	protected void onPreExecute() {
		pd = ProgressDialog.show(activity, "Activating...", "Preparing sensors...");
	}

	private List<DeviceErrorException> exceptions = new ArrayList<DeviceErrorException>();

	@Override
	protected Boolean doInBackground(Object... params) {
		String pinNumber = ((String) params[0]).trim();
		String phoneNumber = ((String) params[1]).trim();
		sensorActivationListener = (SensorActivationListener)params[2];
		Log.i(TAG, "pinNumber=" + pinNumber);
		String url = CoreConfig.getUrl("/sensors/authenticate/" + pinNumber);
		HttpJSONClient client = new DefaultHttpJSONClient();
		String message = null;
		try {
			message = client.executeGet(url);
			Log.i(TAG, "message=" + message);
		} catch (Exception e) {
			message = HttpStatusConstants.STATUS_FAILURE;
		}
		if (message == null || message.equalsIgnoreCase(HttpStatusConstants.STATUS_FAILURE)) {
			exceptions.add(new DeviceErrorException(ErrorCodes.CODE_CANNOT_DETECT, "Can not detect a sensor ID."));
			return false;
		} else {
			Type responseMessageType = new TypeToken<HttpResponseMessage<Boolean>>() {
			}.getType();
			HttpResponseMessage<Boolean> responseMessage = JSONUtils.getTypeResponseMessage(message, responseMessageType);
			if (responseMessage.getMessage().equalsIgnoreCase(HttpStatusConstants.STATUS_FAILURE)) {
				exceptions.add(new DeviceErrorException(ErrorCodes.CODE_CANNOT_DETECT, "Can not detect a sensor ID."));
				return false;
			}
			
			if (responseMessage != null) {
				if (responseMessage.getObject() != null && !responseMessage.getObject().booleanValue()) {
					exceptions.add(new DeviceErrorException(ErrorCodes.CODE_CANNOT_DETECT, "Sensor ID does not exist."));
					return false;
				}
				if (responseMessage.getMessage().equals(HttpStatusConstants.STATUS_SUCCESS) && !responseMessage.getObject().booleanValue()) {
					exceptions.add(new DeviceErrorException(ErrorCodes.CODE_CANNOT_DETECT, "Sensor ID does not exist."));
					return false;
				}
			}
		}
		
		
		
		UserPreference currentUserPreference = UserPreference.getCurrentUserPreference(activity);
		if (currentUserPreference == null) {
			currentUserPreference = new UserPreference();
		}
		currentUserPreference.setCareType(String.valueOf(CareType.CareRecipient.name()));
		currentUserPreference.setPinNumber(pinNumber);
		currentUserPreference.setPhoneNumber(phoneNumber);
		Utils.writePreference(activity, Constants.PREFERENCE_KEY_USER_PREFERENCE, currentUserPreference.getJson());
		
		Intent safeSeniorServiceIntent = new Intent(activity, DefaultSafeSeniorService.class);
		Log.i(TAG, "Starting safeSeniorServiceIntent @ " + SystemClock.elapsedRealtime());
		activity.startService(safeSeniorServiceIntent);
		LocalBroadcastManager.getInstance(activity).registerReceiver(mMessageReceiver, new IntentFilter(Constants.MESSAGE_QUEUE));
		publishProgress("Scanning...");
		synchronized (LOCK) {
			try {
				LOCK.wait(1000 * 60 * 2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return true;
	}

	/**
	 * message receiver
	 */
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@SuppressWarnings("unchecked")
		@Override
		public void onReceive(Context context, Intent intent) {
			// Extract data included in the Intent
			String progressStatus = intent.getStringExtra(Constants.MESSAGE_PROGRESS_STATUS);
			Log.i(TAG, "progressStatus="+ progressStatus);
			if (progressStatus == null || progressStatus.isEmpty()) {
				return;
			}
			if (progressStatus.equals(Constants.MESSAGE_PROGRESS_FINISH)) {
				synchronized (LOCK) {
					LOCK.notifyAll();
				}
				pd.dismiss();
			} else if (progressStatus.equals(Constants.MESSAGE_PROGRESS_ERROR)) {
				Log.i(TAG, "progressStatus="+ progressStatus);
				Intent messageIntent = intent;
				Bundle args = messageIntent.getBundleExtra(Constants.MESSAGE_QUEUE_KEY);
				if (args != null) {
					Object object = args.getSerializable(Constants.ERROR_BUNDLE);
					if (object != null) {
						exceptions = (List<DeviceErrorException>)args.getSerializable(Constants.ERROR_BUNDLE);
					}
				}
				synchronized (LOCK) {
					LOCK.notifyAll();
				}
				pd.dismiss();
				Intent safeSeniorServiceIntent = new Intent(activity, DefaultSafeSeniorService.class);
				activity.stopService(safeSeniorServiceIntent);
			} else if (progressStatus.equals(Constants.MESSAGE_PROGRESS_ONGOING)) {
				String message = intent.getStringExtra(Constants.MESSAGE_QUEUE_KEY);
				Log.i(TAG, "message="+ message);

				publishProgress(message);
			}

		}
	};

	/**
	 * 
	 * @param device
	 * @param sensorDevice
	 * @throws DeviceException
	 */
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

	@Override
	protected void onProgressUpdate(String... values) {
		pd.setMessage(values[0]);
	}

	@Override
	protected void onPostExecute(final Boolean success) {
		try {
			if (pd != null && pd.isShowing()) {
				pd.dismiss();
			}
		} catch (Exception e) {

		} finally {
			pd = null;
		}
		super.onPostExecute(success);
		if (sensorActivationListener != null) {
			if (exceptions == null || exceptions.isEmpty()) {
				sensorActivationListener.sensorDetectionEvent(0, null);
				Intent safeSeniorMonitorServiceIntent = new Intent(activity, DefaultSafeSeniorMonitorService.class);
				Log.i(TAG, "Starting safeSeniorServiceIntent @ " + SystemClock.elapsedRealtime());
				activity.startService(safeSeniorMonitorServiceIntent);

			}
			for (DeviceErrorException exception : exceptions) {
				sensorActivationListener.sensorDetectionEvent(exception.getErrorCode(), exception.getMessage());
			}
			sensorActivationListener = null;
		}
		exceptions.clear();
		LocalBroadcastManager.getInstance(activity).unregisterReceiver(mMessageReceiver);
	}

}
