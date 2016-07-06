/**
 * AgingPlace
 */
package com.ap.core.device.components.bluetooth.classic;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.ap.common.models.http.HttpDataPacket;
import com.ap.core.device.Constants;
import com.ap.core.device.components.DeviceConnection;
import com.ap.core.device.components.DeviceConnectionListener;
import com.ap.core.device.components.PacketReceiver;
import com.ap.core.device.exceptions.DeviceException;
import com.ap.mobile.components.SensorDiscoveryConnectionMonitor;
import com.ap.mobile.model.UISensorDevice;
import com.ap.mobile.util.Utils;

/* Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */
public class SensorDeviceConnection implements DeviceConnection, DeviceConnectionListener {
	protected static final String TAG = SensorDeviceConnection.class.getSimpleName();

	private BluetoothAdapter bluetoothAdapter;

	private UISensorDevice sensorDevice;

	private SensorConnectionWorker connectionWorker;

	private List<PacketReceiver> packetReceivers = new ArrayList<PacketReceiver>();
	
	private Object workerObject = new Object();
	
	private boolean isConnected = false;
	
	private boolean isDisconnectRequested = false;
	
	private int retryCount = 0;
	
	private int retryLimit = 3;
	
	private long retryInterval = 1000 * 10;
	
	private SensorDiscoveryConnectionMonitor sensorDiscoveryConnectionMonitor;

	public SensorDeviceConnection(BluetoothAdapter bluetoothAdapter, UISensorDevice sensorDevice) {
		super();
		this.bluetoothAdapter = bluetoothAdapter;
		this.sensorDevice = sensorDevice;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ap.common.device.components.DeviceConnection#discovery()
	 */
	@Override
	public void discovery() throws DeviceException, IOException {
		Log.d(TAG, "discovery is called.");
		if (isConnected) {
			Log.i(TAG, sensorDevice.getSensorName() + "=="+ sensorDevice.getSensorName() + " is already running.");
			return;
		}
		startSensorCommunication();
		notifyInvoker();
	}
	
	private void startSensorCommunication()  throws DeviceException, IOException {
		Log.d(TAG, "startSensorCommunication is called.");
		BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(sensorDevice.getMacAddress());
		if (bluetoothDevice == null) {
			throw new DeviceException("Sensor " + sensorDevice.getSensorType() + "==" + sensorDevice.getSensorName() + "==" + sensorDevice.getMacAddress() + " does not exist.");
		}
		Log.d(TAG, "bluetoothDevice="+ bluetoothDevice.toString());
		connectionWorker = new SensorConnectionWorker(bluetoothDevice);
		connectionWorker.setSensorDevice(sensorDevice);
		connectionWorker.addPacketReceivers(packetReceivers);
		connectionWorker.addBluetoothDeviceConnectionListener(this);
		connectionWorker.initialize();
		connectionWorker.start();
		sensorDevice.setConnected(true);
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
	
	/**
	 * 
	 * @param message
	 */
	private void writeToSensor(String message) {
		connectionWorker.sendToSensor(message);
	}

	/**
	 * Pair device
	 * @param device
	 */
	protected void pairDevice(BluetoothDevice device) throws DeviceException {
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
	 * Pair device
	 * @param device
	 */
	protected void unpairDevice(BluetoothDevice device) throws DeviceException {
		try {
			Method m = device.getClass().getMethod("removeBond", (Class[]) null);
			m.invoke(device, (Object[]) null);
		} catch (Exception e) {
			throw new DeviceException("Can't pair with " + sensorDevice.getSensorType() + "==" + sensorDevice.getSensorName() + "==" + sensorDevice.getMacAddress());
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.ap.common.device.components.DeviceConnection#disconnect()
	 */
	@Override
	public synchronized void disconnect() throws DeviceException {
		isDisconnectRequested = true;
		if (connectionWorker != null) {
			connectionWorker.notifyLock();
			connectionWorker.setCanStop(true);
			connectionWorker.stopWorker();
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
		sensorDevice.setConnected(false);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.ap.common.device.components.DeviceConnection#registerDataReceiver
	 * (com.ap.common.device.components.PacketReceiver)
	 */
	@Override
	public void registerPacketReceiver(PacketReceiver receiver) {
		if (!packetReceivers.contains(receiver)) {
			packetReceivers.add(receiver);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.ap.common.device.components.DeviceConnection#unregisterDataReceiver
	 * (com.ap.common.device.components.PacketReceiver)
	 */
	@Override
	public void unregisterPacketReceiver(PacketReceiver receiver) {
		packetReceivers.remove(receiver);
	}

	/* (non-Javadoc)
	 * @see com.ap.common.device.components.DeviceConnectionListener#connected()
	 */
	@Override
	public void connected() {
		synchronized(workerObject) {
			workerObject.notifyAll();
			isConnected = true;
		}
		retryCount = 0;
		sensorDevice.setConnected(true);
		isDisconnectRequested = false;
		Log.i(TAG, "connected() is called to "+ sensorDevice.getSensorName() + "=="+ sensorDevice.getSensorType());
	}

	/* (non-Javadoc)
	 * @see com.ap.common.device.components.DeviceConnectionListener#retryConnect()
	 */
	@Override
	public synchronized void retryConnect() {
		if (isDisconnectRequested) {
			Log.i(TAG, "disconnection is requested. No retry.... "+ sensorDevice.getSensorName() + "=="+ sensorDevice.getSensorType());
			return;
		}
		isConnected = false;
		while (true) {
			if (retryCount >= retryLimit) {
				if (sensorDiscoveryConnectionMonitor != null) {
					if (connectionWorker == null) {
						break;
					}
					connectionWorker.stopWorker();
					try {
						connectionWorker.interrupt();
					} catch (Exception e) {
						//
					}
					connectionWorker.notifyLock();
					sensorDevice.setConnected(false);
					sensorDiscoveryConnectionMonitor.manageSensorDeviceConnection(sensorDevice);
				}
				Log.i(TAG, "Stop retrying...");
				isConnected = false;
//				retryCount = 0;
				break;
			}
			Log.i(TAG, "retryConnect is called for "+ sensorDevice.getSensorName() + "=="+ sensorDevice.getSensorType());
			Log.i(TAG, "retryCount="+ retryCount + " is tried for "+ sensorDevice.getSensorName() + "=="+ sensorDevice.getSensorType());
			try {
				try {
					retryInterval = 1000 * Utils.randInt(10, 30);
					Log.i(TAG, "retryInterval is " + (retryInterval/1000) + " seconds for "+ sensorDevice.getSensorName() + "=="+ sensorDevice.getSensorType());
					Thread.sleep(retryInterval);
				} catch (InterruptedException e) {
					// 
				}
				if (connectionWorker == null) {
					break;
				}
				retryCount++;
				connectionWorker.cleanupStream();
				connectionWorker.initiateCommunication();
				connectionWorker.notifyLock();
				Log.i(TAG, "RECONNECTION is successful for "+ sensorDevice.getSensorName() + "=="+ sensorDevice.getSensorType());
				break;
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
			}	
		}
		
	}

	/* (non-Javadoc)
	 * @see com.ap.core.device.components.DeviceConnection#sendMessageEvent(com.ap.core.device.components.DataPacket)
	 */
	@Override
	public void sendMessageEvent(HttpDataPacket dataPacket) throws DeviceException {
		StringBuffer b = new StringBuffer();
		b.append(" ");
		b.append(dataPacket.getSensorType()).append(Constants.MESSAGE_DELIMITER);
		b.append(dataPacket.getSensorName()).append(Constants.MESSAGE_DELIMITER);
		b.append(dataPacket.getPacketType()).append(Constants.MESSAGE_DELIMITER);
		b.append(dataPacket.getPacketValue()).append(Constants.MESSAGE_DELIMITER);
		b.append(System.currentTimeMillis()).append(Constants.MESSAGE_DELIMITER);
		b.append(dataPacket.getBatteryLevel()).append("\n");
		String message = b.toString();
		Log.i(TAG, "sensorMessage="+ message);
		writeToSensor(message);
	}

	/**
	 * 
	 */
	public boolean isConnected() {
		if (connectionWorker == null) {
			return false;
		}
		return connectionWorker.isConnected();
	}

	/* (non-Javadoc)
	 * @see com.ap.core.device.components.DeviceConnection#reConnect()
	 */
	@Override
	public void reConnect() throws DeviceException {
		isDisconnectRequested = false;
		retryCount = 0;
		retryConnect();
	}

	/* (non-Javadoc)
	 * @see com.ap.core.device.components.DeviceConnection#validateConnection()
	 */
	@Override
	public void validateConnection() throws DeviceException {
		String pingMessage = "PING";
		writeToSensor(pingMessage);
	}

	/**
	 * @param sensorDiscoveryConnectionMonitor the sensorDiscoveryConnectionMonitor to set
	 */
	public void setSensorDiscoveryConnectionMonitor(SensorDiscoveryConnectionMonitor sensorDiscoveryConnectionMonitor) {
		this.sensorDiscoveryConnectionMonitor = sensorDiscoveryConnectionMonitor;
	}

	/* (non-Javadoc)
	 * @see com.ap.core.device.components.DeviceConnection#getSensorDeviceName()
	 */
	@Override
	public String getSensorDeviceName() {
		return sensorDevice.getSensorName();
	}

	/* (non-Javadoc)
	 * @see com.ap.core.device.components.DeviceConnectionListener#failConnected()
	 */
	@Override
	public void failConnected() {
		sensorDevice.setConnected(false);
	}

	/* (non-Javadoc)
	 * @see com.ap.core.device.components.DeviceConnection#cancel()
	 */
	@Override
	public void cancel() throws DeviceException {
		disconnect();
		if (packetReceivers != null) {
			packetReceivers.clear();
		}
	}

	/* (non-Javadoc)
	 * @see com.ap.core.device.components.DeviceConnection#checkConnection()
	 */
	@Override
	public void checkConnection() throws DeviceException, IOException {
		Log.d(TAG, "startSensorCommunication is called.");
		BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(sensorDevice.getMacAddress());
		if (bluetoothDevice == null) {
			throw new DeviceException("Sensor " + sensorDevice.getSensorType() + "==" + sensorDevice.getSensorName() + "==" + sensorDevice.getMacAddress() + " does not exist.");
		}
		Log.d(TAG, "bluetoothDevice="+ bluetoothDevice.toString());
		connectionWorker = new SensorConnectionWorker(bluetoothDevice);
		connectionWorker.setSensorDevice(sensorDevice);
		connectionWorker.addPacketReceivers(packetReceivers);
		connectionWorker.addBluetoothDeviceConnectionListener(this);
		connectionWorker.checkConnection();
	}
}
