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

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.ap.core.device.Constants;
import com.ap.core.device.ErrorCodes;
import com.ap.core.device.exceptions.DeviceErrorException;
import com.ap.mobile.services.DefaultSafeSeniorService;
import com.ap.mobile.services.receivers.SensorResetRequestReceiver;

/**
 * @author jungwhan
 * SensorResetTask.java
 * 11:10:22 PM Feb 15, 2016 2016
 */
public class SensorResetTask extends AsyncTask<Object, String, Boolean> {
	protected static final String TAG = SensorResetTask.class.getSimpleName();
	
	private ProgressDialog pd;
	
	private Activity activity;
	
	private List<DeviceErrorException> exceptions = new ArrayList<DeviceErrorException>();
	
	private final Object LOCK = new Object();
	
	public SensorResetTask(Activity activity) {
		this.activity = activity;
	}
	
	@Override
	protected void onPreExecute() {
		pd = ProgressDialog.show(activity, "Resetting...", "Reset in progress...");
	}

	/* (non-Javadoc)
	 * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
	 */
	@Override
	protected Boolean doInBackground(Object... params) {
		DefaultSafeSeniorService safeSeniorService = ((DefaultSafeSeniorService) params[0]);
		try {
			publishProgress("Resetting sensors. Please wait...");
			
			if (safeSeniorService == null) {
				throw new Exception("Service is not connected.");
			}
			safeSeniorService.resetSensors();

			
			Intent safeSeniorServiceIntent = new Intent(activity, DefaultSafeSeniorService.class);
			Log.i(TAG, "Restarting safeSeniorServiceIntent @ " + SystemClock.elapsedRealtime());
			activity.startService(safeSeniorServiceIntent);
			LocalBroadcastManager.getInstance(activity).registerReceiver(mMessageReceiver, new IntentFilter(Constants.MESSAGE_QUEUE));
			publishProgress("Restarting...");
			synchronized (LOCK) {
				try {
					LOCK.wait(1000 * 60 * 2);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			exceptions.add(new DeviceErrorException(ErrorCodes.CODE_RESET_FAILURE, "Reset failed."));
		}
		Log.i(TAG, "doInBackground is finished.");
		if (exceptions == null || exceptions.isEmpty()) {
			return true;
		} else {
			return false;
		}
	}
	
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
	
	@Override
	protected void onPostExecute(final Boolean success) {
		if (exceptions == null || exceptions.isEmpty()) {
			Toast.makeText(getActivity(), "Reset is successful.", Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(getActivity(), "Reset failed.", Toast.LENGTH_LONG).show();
		}
		if (pd != null) {
			pd.dismiss();
		}
		exceptions.clear();
	}
	
	@Override
	protected void onProgressUpdate(String... values) {
		pd.setMessage(values[0]);
	}


	/**
	 * @return the activity
	 */
	public Activity getActivity() {
		return activity;
	}

}
