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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.ap.mobile.activities.fragment.caretaker.adapter.SensorStatusArrayAdapter;
import com.ap.mobile.model.UISensorDevice;
import com.ap.mobile.services.DefaultSafeSeniorService;
import com.ap.safesenior.mobile.R;

/**
 * @author jungwhan
 * SensorStatusCheckTask.java
 * 1:39:06 AM Mar 1, 2016 2016
 */
public class SensorStatusCheckTask extends AsyncTask<Object, String, Map<String, Boolean>> {
	protected static final String TAG = SensorStatusCheckTask.class.getSimpleName();
	
	private ProgressDialog pd;
	
	private Activity activity;
	
	private Object LOCK = new Object();
	
	private Map<String, Boolean> results = new HashMap<String, Boolean>();
	
	private List<UISensorDevice> sensorDevices;
	
	public SensorStatusCheckTask(Activity activity) {
		this.activity = activity;
	}
	

	@Override
	protected void onPreExecute() {
		pd = ProgressDialog.show(activity, "Checking status", "Connecting...");
	}

	/* (non-Javadoc)
	 * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected Map<String, Boolean> doInBackground(Object... params) {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			//
		}
		final DefaultSafeSeniorService safeSeniorService = (DefaultSafeSeniorService)params[0];
		sensorDevices = (List<UISensorDevice>)params[1];
		Thread thread1 = new Thread() {
			@Override
			public void run() {
				try {
					if (safeSeniorService == null) {
						return;
					}
					Map<String, Boolean> sensorStatuses = safeSeniorService.getSensorConnections();
					for (UISensorDevice sensorDevice : sensorDevices) {
						Boolean status = sensorStatuses.get(sensorDevice.getSensorName());
						if (status == null) {
							sensorDevice.setConnected(false);
						}
						sensorDevice.setConnected(status.booleanValue());
					}
					final ListView sensorDeviceListView = (ListView) activity.findViewById(R.id.sensorDeviceListView);
					ArrayAdapter<UISensorDevice> sensorDeviceAdapter = new SensorStatusArrayAdapter(activity, R.layout.template_sensor_status, sensorDevices);
					sensorDeviceListView.setAdapter(sensorDeviceAdapter);

				} catch (Exception e) {
					Log.e(TAG, "error="+ e.getMessage());
				} finally {
					synchronized(LOCK) {
						LOCK.notifyAll();
					}
				}
			}
		};
		thread1.start();
		synchronized(LOCK) {
			try {
				LOCK.wait(1000 * 20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		return results;
	}

	
	@Override
	protected void onProgressUpdate(String... values) {
		pd.setMessage(values[0]);
	}


	@Override
	protected void onPostExecute(final Map<String, Boolean> success) {
		super.onPostExecute(success);
		if (pd != null && pd.isShowing()) {
			pd.dismiss();
		}
	}
}
