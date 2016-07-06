/**
 * AgingSafeSeniorMobile
 */
package com.ap.core.device.components.bluetooth.ble;

import java.io.IOException;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.ap.common.models.http.HttpDataPacket;
import com.ap.core.device.components.DeviceConnection;
import com.ap.core.device.components.DeviceConnectionListener;
import com.ap.core.device.components.PacketReceiver;
import com.ap.core.device.exceptions.DeviceException;
import com.ap.mobile.components.SensorDiscoveryConnectionMonitor;
import com.ap.mobile.model.UISensorDevice;
import com.ap.mobile.util.Utils;

/**
 *  Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by jungwhan Projecteria LLC, May 18, 2016
 */

/**
 * @author jungwhan
 * SensorDeviceBLEConnection.java
 * 4:14:47 PM May 18, 2016 2016
 */
public class SensorDeviceBLEConnection implements DeviceConnection, DeviceConnectionListener {
	protected static final String TAG = SensorDeviceBLEConnection.class.getSimpleName();
	
	private BluetoothAdapter bluetoothAdapter;
	
	private Context context;
	
	private SensorBLEConnectionWorker connectionWorker;
	
	private Object workerObject = new Object();
	
	private boolean isConnected = false;

	private boolean isDisconnectRequested = false;
	
	private int retryCount = 0;
	
	private int retryLimit = 3;
	
	private long retryInterval = 1000 * 10;
	
	private SensorDiscoveryConnectionMonitor sensorDiscoveryConnectionMonitor;
	
	public SensorDeviceBLEConnection(BluetoothAdapter bluetoothAdapter, UISensorDevice sensorDevice, Context context) {
		super();
		this.bluetoothAdapter = bluetoothAdapter;
		this.context = context;
//		this.sensorDevice = sensorDevice;
	}

	/* (non-Javadoc)
	 * @see com.ap.core.device.components.DeviceConnectionListener#connected()
	 */
	@Override
	public void connected() {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.ap.core.device.components.DeviceConnectionListener#retryConnect()
	 */
	@Override
	public void retryConnect() {
		if (isDisconnectRequested) {
//			Log.i(TAG, "disconnection is requested. No retry.... "+ sensorDevice.getSensorName() + "=="+ sensorDevice.getSensorType());
			return;
		}
		isConnected = false;
		while (true) {
			if (retryCount >= retryLimit) {
//				if (sensorDiscoveryConnectionMonitor != null) {
					if (connectionWorker == null) {
						break;
					}
					connectionWorker.cleanupStream();
					connectionWorker.stopWorker();
					try {
						connectionWorker.interrupt();
					} catch (Exception e) {
						//
					}
//					sensorDevice.setConnected(false);
//					sensorDiscoveryConnectionMonitor.manageSensorDeviceConnection(sensorDevice);
//				}
				Log.i(TAG, "Stop retrying...");
				isConnected = false;
//				retryCount = 0;
				break;
			}
//			Log.i(TAG, "retryConnect is called for "+ sensorDevice.getSensorName() + "=="+ sensorDevice.getSensorType());
//			Log.i(TAG, "retryCount="+ retryCount + " is tried for "+ sensorDevice.getSensorName() + "=="+ sensorDevice.getSensorType());
			try {
				try {
					retryInterval = 1000 * Utils.randInt(10, 30);
//					Log.i(TAG, "retryInterval is " + (retryInterval/1000) + " seconds for "+ sensorDevice.getSensorName() + "=="+ sensorDevice.getSensorType());
					Thread.sleep(retryInterval);
				} catch (InterruptedException e) {
					// 
				}
				if (connectionWorker == null) {
					break;
				}
				retryCount++;
				connectionWorker.reRegisterIntentFilters();
				connectionWorker.init();
				Thread.sleep(1000 * 10);
				if (connectionWorker.isConnected()) {
//					Log.i(TAG, "RECONNECTION is successful for "+ sensorDevice.getSensorName() + "=="+ sensorDevice.getSensorType());
					connectionWorker.setBLEMode();
					break;
				}
				connectionWorker.unregisterReceiver();
				connectionWorker.cleanupStream();
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
			}	
		}
	}

	/* (non-Javadoc)
	 * @see com.ap.core.device.components.DeviceConnectionListener#failConnected()
	 */
	@Override
	public void failConnected() {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.ap.core.device.components.DeviceConnection#registerPacketReceiver(com.ap.core.device.components.PacketReceiver)
	 */
	@Override
	public void registerPacketReceiver(PacketReceiver receiver) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.ap.core.device.components.DeviceConnection#getSensorDeviceName()
	 */
	@Override
	public String getSensorDeviceName() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.ap.core.device.components.DeviceConnection#reConnect()
	 */
	@Override
	public void reConnect() throws DeviceException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.ap.core.device.components.DeviceConnection#isConnected()
	 */
	@Override
	public boolean isConnected() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.ap.core.device.components.DeviceConnection#unregisterPacketReceiver(com.ap.core.device.components.PacketReceiver)
	 */
	@Override
	public void unregisterPacketReceiver(PacketReceiver receiver) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.ap.core.device.components.DeviceConnection#discovery()
	 */
	@Override
	public void discovery() throws DeviceException, IOException {
		if (isConnected) {
//			Log.i(TAG, sensorDevice.getSensorName() + "=="+ sensorDevice.getSensorName() + " is already running.");
			return;
		}
		BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice("E8:77:C3:20:29:71");
		connectionWorker = new SensorBLEConnectionWorker(bluetoothDevice);
		connectionWorker.addBluetoothDeviceConnectionListener(this);
		connectionWorker.setContext(context);
		connectionWorker.initialize();
		connectionWorker.start();
		notifyInvoker();
	}

	/* (non-Javadoc)
	 * @see com.ap.core.device.components.DeviceConnection#checkConnection()
	 */
	@Override
	public void checkConnection() throws DeviceException, IOException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.ap.core.device.components.DeviceConnection#validateConnection()
	 */
	@Override
	public void validateConnection() throws DeviceException {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Notify the class which calls this class.
	 */
	private void notifyInvoker() {
		synchronized(workerObject) {
			try {
				workerObject.wait(1000 * 10);
			} catch (InterruptedException e) {
				//
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see com.ap.core.device.components.DeviceConnection#disconnect()
	 */
	@Override
	public void disconnect() throws DeviceException {
		isDisconnectRequested = true;
		if (connectionWorker != null) {
			connectionWorker.setCanStop(true);
			connectionWorker.cleanupStream();
			if (connectionWorker.isAlive()) {
				try {
					connectionWorker.interrupt();
				} catch (Exception e) {
					
				}
			}
//			packetReceivers.clear();   Need to review
			isConnected = false;
			retryCount = 0;
		}
//		sensorDevice.setConnected(false);
	}

	/* (non-Javadoc)
	 * @see com.ap.core.device.components.DeviceConnection#cancel()
	 */
	@Override
	public void cancel() throws DeviceException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.ap.core.device.components.DeviceConnection#sendMessageEvent(com.ap.common.models.http.HttpDataPacket)
	 */
	@Override
	public void sendMessageEvent(HttpDataPacket dataPacket) throws DeviceException {
		// TODO Auto-generated method stub
		
	}

}
