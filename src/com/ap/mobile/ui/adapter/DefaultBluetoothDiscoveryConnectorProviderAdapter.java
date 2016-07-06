/**
 * AgingPlaceMobile
 */
package com.ap.mobile.ui.adapter;

/**
 * Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.ap.common.models.http.HttpConnectTime;
import com.ap.core.device.ConnectType;
import com.ap.mobile.components.SensorDiscoveryConnectionManager;

/**
 * @author jungwhan
 * BluetoothDiscoveryConnectorProviderAdapter.java
 * 1:23:09 AM Feb 13, 2016 2016
 */
public class DefaultBluetoothDiscoveryConnectorProviderAdapter implements BluetoothDiscoveryConnectorProviderAdapter {
	protected static final String TAG = DefaultBluetoothDiscoveryConnectorProviderAdapter.class.getSimpleName();

	private SensorDiscoveryConnectionManager sensorDiscoveryConnectionProvider;
	
	private Context context;
	
	private static boolean isRegistered = false;
	
	public DefaultBluetoothDiscoveryConnectorProviderAdapter() {
		super();
	}
	
	public DefaultBluetoothDiscoveryConnectorProviderAdapter(Activity activity) {
		this.context = activity;
	}
	
	public void initialize(Context activity, SensorDiscoveryConnectionManager sensorDiscoveryConnectionProvider) {
		this.context = activity;
		this.sensorDiscoveryConnectionProvider = sensorDiscoveryConnectionProvider;
		registerBluetoothConnectionEvent();
	}
	
	/**
	 * Register bluetooth filters
	 */
	private void registerBluetoothConnectionEvent() {
		if (isRegistered) {
			Log.i(TAG, "broadcastReceiver is already registered.");
			return;
		}
		IntentFilter connectedFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
		IntentFilter disconnectedRequestFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
		IntentFilter disconnectedFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		try {
			context.registerReceiver(broadcastReceiver, connectedFilter);
			context.registerReceiver(broadcastReceiver, disconnectedRequestFilter);
			context.registerReceiver(broadcastReceiver, disconnectedFilter);
		} catch (Exception e) {
			//
		}
		isRegistered = true;
	}
	
	public void unregisterBluetoothReceiver() {
		try {
			context.unregisterReceiver(broadcastReceiver);
		} catch (Exception e) {
			e.printStackTrace();
		}
		isRegistered = false;
	}
	
	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
		    String action = intent.getAction();
		    Log.i(TAG, "action=="+  action);
		    //Finding devices
		    if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
		        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		        Log.i(TAG, "device found : "+  device.getName() + "\n" + device.getAddress());
		    } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
				Log.i(TAG, "Bluetooth is disconnected.");
				sensorDiscoveryConnectionProvider.checkBluetoothConnections();
				sensorDiscoveryConnectionProvider.getSensorContext().getCareReceiverConnectionInformation().addConnectTime(new HttpConnectTime(ConnectType.DISCONNECTED.getConnectType(), System.currentTimeMillis()));
			}
		}
	};
}
