/**
 * AgingPlace
 */
package com.ap.core.device.components.bluetooth.classic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.ap.common.models.http.HttpEventType;
import com.ap.common.models.http.HttpPacketValue;
import com.ap.common.models.http.HttpSensorDevice;
import com.ap.core.device.CoreConfig;
import com.ap.core.device.SensorData;
import com.ap.core.device.components.DeviceConnectionListener;
import com.ap.core.device.components.PacketReceiver;
import com.ap.core.device.http.DefaultHttpJSONClient;
import com.ap.core.device.http.HttpJSONClient;
import com.ap.core.device.thread.Worker;
import com.ap.core.device.utils.MessageUtils;
import com.ap.mobile.model.UISensorDevice;
import com.google.gson.Gson;

/* Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */
public class SensorConnectionWorker extends Worker {
	protected static final String TAG = SensorConnectionWorker.class.getSimpleName();
	
	private BluetoothSocket bluetoothSocket;

	private List<PacketReceiver> packetReceivers = new ArrayList<PacketReceiver>();
	
	private boolean canKeepReading = true;
	
	private boolean canStop = false;

	private InputStream inputStream;
	
	private OutputStream outputStream;
	
	private UISensorDevice sensorDevice;
	
	private BluetoothDevice bluetoothDevice;
	
	private Object LOCK = new Object();
	
	private List<DeviceConnectionListener> blueDeviceConnectionListeners = new ArrayList<DeviceConnectionListener>();
	
	public SensorConnectionWorker(BluetoothDevice bluetoothDevice) {
		this.bluetoothDevice = bluetoothDevice;
	}
	
	public void initialize() throws IOException {
		Log.d(TAG, "initialize is called for "+ sensorDevice.getSensorName());
		initializeBluetoothSocket();
		try {
			initiateCommunication();
		} catch (IOException e) {
			notifyFailedConnection();
			throw e;
		}
	}
	
	/**
	 * Just to check if it can connect to the bluetooth
	 * @throws IOException
	 */
	public void checkConnection() throws IOException {
		initialize();
		if (bluetoothSocket == null) {
			Log.i(TAG, "bluetoothSocket is null.");
			throw new IOException("BluetoothSocket is not available.");
		}
		inputStream = bluetoothSocket.getInputStream();
		if (inputStream == null) {
			Log.i(TAG, "inputStream can't be null.");
			throw new IOException("inputStream is not available.");

		}
		outputStream = bluetoothSocket.getOutputStream();
		if (outputStream == null) {
			Log.i(TAG, "outputStream can't be null.");
			throw new IOException("BluetoothSocket is not available.");
		}
		cleanupStream();
	}
		
	/**
	 * Initialize bluetooth socket
	 */
	protected void initializeBluetoothSocket() throws IOException {
		Log.d(TAG, "initializeBluetoothSocket is called for "+ sensorDevice.getSensorName());
		UUID bluetoothUUID = UUID.fromString(sensorDevice.getSensorUUID());
		bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(bluetoothUUID);
	}
	
	/**
	 * 
	 * @param packetReceivers
	 */
	public void addPacketReceivers(List<PacketReceiver> packetReceivers) {
		this.packetReceivers.addAll(packetReceivers);
	}

	/**
	 * @param deviceConnectionListener
	 */
	public void addBluetoothDeviceConnectionListener(DeviceConnectionListener deviceConnectionListener) {
		blueDeviceConnectionListeners.add(deviceConnectionListener);
	}
	
	@Override
	public void stopWorker() {
		canStop = true;
		canKeepReading = false;
		cleanupStream();
		clearListeners();
		notifyLock();
		this.cancel();
	}
	
	public boolean isConnected() {
		synchronized (this) {
			if (bluetoothSocket == null) {
				return false;
			}
			return bluetoothSocket.isConnected();			
		}
	}
	
	/**
	 * Clean up streams
	 */
	public synchronized void cleanupStream() {
		canKeepReading = false;
		// Cause dead lock
//		try {
//			Thread.sleep(2000);
//		} catch (InterruptedException e1) {
//			// 
//		}
		if (inputStream != null) {
			try {
				inputStream.close();
			} catch (IOException e) {
				//
			}
		}
		inputStream = null;
		if (outputStream != null) {
			try {
				outputStream.close();
			} catch (IOException e) {
				//
			}
		}
		outputStream = null;
		if (bluetoothSocket != null) {
			try {
				bluetoothSocket.close();
			} catch (IOException e) {
				//
			}
		}
		bluetoothSocket = null;
	}
	
	protected void clearListeners() {
		packetReceivers.clear();
		blueDeviceConnectionListeners.clear();
	}
	
	public void initiateCommunication() throws IOException {
		Log.d(TAG, "initiateCommunication is called for "+ sensorDevice.getSensorName());
		if (bluetoothSocket == null) {
			initializeBluetoothSocket();
		}
		try {
			if (bluetoothSocket.isConnected()) {
				bluetoothSocket.close();
			}
			bluetoothSocket.connect();
			canKeepReading = true;
			Log.i(TAG, "initiateCommunication() is called and connection is successful for "+ sensorDevice.getSensorName());
		} catch (IOException connectException) {
			notifyFailedConnection();
			Log.e(TAG, connectException.getMessage());
			Log.e(TAG, "SensorDevice Failed...="+ sensorDevice);
			try {
				if (bluetoothSocket != null) {
					bluetoothSocket.close();
				}
				bluetoothSocket = null;
			} catch (IOException closeException) {
				//
			}
			canKeepReading = false;
			throw connectException;
		}
	}
	
	/**
	 * Notify if the connection is successful.
	 */
	public void notifyLock() {
		synchronized(LOCK) {
			LOCK.notifyAll();
		}
	}
	
	/**
	 * Retry the connection
	 */
	private void retryConnect() {
		cleanupStream();
		for (DeviceConnectionListener blueDeviceConnectionListener : blueDeviceConnectionListeners) {
			blueDeviceConnectionListener.retryConnect();
		}
	}
	
	/**
	 * Confirm the connection
	 */
	private void confirmConnection() {
		sensorDevice.setConnected(true);
		sensorDevice.setConnectedTime(System.currentTimeMillis());
		for (DeviceConnectionListener blueDeviceConnectionListener : blueDeviceConnectionListeners) {
			blueDeviceConnectionListener.connected();
		}
	}
	
	/**
	 * Confirm the connection
	 */
	private void notifyFailedConnection() {
		sensorDevice.setConnected(false);
		sensorDevice.setConnectedTime(System.currentTimeMillis());
		for (DeviceConnectionListener blueDeviceConnectionListener : blueDeviceConnectionListeners) {
			blueDeviceConnectionListener.failConnected();
		}
	}
	
	/* (non-Javadoc)
	 * @see com.ap.common.device.thread.Worker#run()
	 */
	@Override
	public void run() {
		sensorCommunication();
	}
	
	private void sensorCommunication() {
		Thread thread1 = null;
		while (!canStop) {
			try {
				readDataPacket();
			} catch (Exception connectException) {
				Log.i(TAG, "sensorCommunication.error="+ connectException.getMessage());
				Log.e(TAG, connectException.getMessage());
				sensorDevice.setConnected(false);
				sensorDevice.setConnectedTime(System.currentTimeMillis());

				try {
					cleanupStream();
				} catch (Exception closeException) {
					closeException.printStackTrace();
				}
				thread1 = new Thread() {
					@Override
					public void run() {
						retryConnect();
					}
				};
				thread1.start();
				try {
					synchronized(LOCK) {
						Log.i(TAG, "Waiting for LOCK to be released.");
						LOCK.wait(1000 * 10);
					}
					Log.i(TAG, "LOCK is released.");
				} catch (InterruptedException e) {
					// 
				}
			}
			try {
				if (thread1 != null && thread1.isAlive()) {
					thread1.join();
				}
			} catch (InterruptedException e) {
				// 
			}
		}
		try {
			if (thread1 != null) {
				thread1.interrupt();
			}
		} catch (Exception e) {
			//
		}
		thread1 = null;
		Log.i(TAG, sensorDevice.getSensorName() + ".sensorCommunication is finished.");
	}
	
	private void validateSocketStream() throws IOException {
		if (bluetoothSocket == null) {
			Log.i(TAG, "bluetoothSocket is null.");
			throw new IOException("BluetoothSocket is not available.");
		}
		inputStream = bluetoothSocket.getInputStream();
		if (inputStream == null) {
			Log.i(TAG, "inputStream can't be null.");
			throw new IOException("inputStream is not available.");

		}
		outputStream = bluetoothSocket.getOutputStream();
		if (outputStream == null) {
			Log.i(TAG, "outputStream can't be null.");
			throw new IOException("BluetoothSocket is not available.");
		}
	}
	
	/**
	 * Read data from sensors
	 * @throws IOException
	 */
	protected void readDataPacket() throws IOException {
		validateSocketStream();
		Log.i(TAG, "readDataPacket() is called for "+ sensorDevice.getSensorName());
		confirmConnection();
		final byte delimiter = 10; // This is the ASCII code for a newline
		long pingTime = 0;
		long idleTimePause = 500;
		boolean isRegistered = false;
		while (canKeepReading) {
			if (!canKeepReading) {
				break;
			}
			if (bluetoothSocket == null || !bluetoothSocket.isConnected()) {
				throw new IOException("BluetoothSocket is disconnected.");
			}
			try {
				int readBufferPosition = 0;
				byte[] readBuffer = new byte[1024];
				int bytesAvailable = inputStream.available();
				if (bytesAvailable > 0) {
					// Need to wait for a little bit to wait for packets coming from sensors.
					Thread.sleep(800);
					bytesAvailable = inputStream.available();
					byte[] packetBytes = new byte[bytesAvailable];
					inputStream.read(packetBytes);
					for (int i = 0; i < bytesAvailable; i++) {
						pingTime = 0;
						byte b = packetBytes[i];
						if (b == delimiter) {
							byte[] encodedBytes = new byte[readBufferPosition];
							System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
							final String data = new String(encodedBytes, "UTF-8");
//							Log.i(TAG, "received=" + data + "=="+ new java.util.Date());
							try {
								SensorData sensorData = MessageUtils.toSensorData(data);
								if (sensorData != null) {
									Log.i(TAG, "dataPacket="+ sensorData);
									if (sensorData.getPacketType().equalsIgnoreCase(HttpEventType.REGISTER.name())) {
										sendToSensor(HttpPacketValue.REGISTER_SUCCESSFUL.name() + "|" + sensorDevice.getMacAddress());
										if (!isRegistered) {
											activateDevice();											
										}
										isRegistered = true;
									} else if (sensorData.getPacketType().equalsIgnoreCase(HttpEventType.PING.name())) {
										Log.i(TAG, "PING.dataPacket="+ sensorData);	
									} else {
//										Log.i(TAG, "motionEvent="+ sensorData.toString());
										if (packetReceivers != null) {
											for (PacketReceiver packetReceiver : packetReceivers) {
												packetReceiver.receive(data);
											}
										}
									}
								} else {
									break;
								}
							} catch (Exception e) {
								break;
							}
							readBufferPosition = 0;
						} else {
							readBuffer[readBufferPosition++] = b;
						}
					}
				} else {
					Thread.sleep(idleTimePause);
					if (pingTime >= (1000 * 60 * 5)) {
						pingTime = 0;
						sendToSensor(HttpPacketValue.PING.name(), true);
					}
					pingTime = idleTimePause + pingTime;
				}
			} catch (IOException e) {
				e.printStackTrace();
				Log.e(TAG, "IOException="+ sensorDevice.getSensorName() + "=="+ e.getMessage());
				throw new IOException("BluetoothSocket is disconnected.");
			} catch (Exception e) {
				e.printStackTrace();
				Log.e(TAG, "Exception="+ sensorDevice.getSensorName() + "=="+ e.getMessage());
				throw new IOException("BluetoothSocket is disconnected.");
			}
		}
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			//
		}
		Log.i(TAG, "readDataPacket ended.");
	}
	
	/**
	 * Activate the sensor
	 */
	private void activateDevice() {
		new Thread() {
			@Override
			public void run() {
				String url = CoreConfig.getUrl("/sensor");
				HttpJSONClient client = new DefaultHttpJSONClient();
				final Gson gson = new Gson();
				HttpSensorDevice httpSensorDevice = new HttpSensorDevice(sensorDevice);
				String jsonRequest = gson.toJson(httpSensorDevice);
				Log.i(TAG, "jsonRequest=" + jsonRequest);
				String response = null;
				try {
					response = client.executePost(url, jsonRequest);
					Log.i(TAG, "response=" + response);
					sensorDevice.setActivated(true);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
	
	public void sendToSensor(final String message) {
		sendToSensor(message, false);
	}

	/**
	 * Write to sensor
	 * @param message
	 */
	private void sendToSensor(final String message, final boolean noRetry) {
		if (outputStream == null) {
			return;
		}
		new Thread() {
			public void run() {
				try {
					if (outputStream == null) {
						return;
					}
					outputStream.write(message.getBytes());
					outputStream.flush();
				} catch (IOException e) {
					e.printStackTrace();
					if (noRetry) {
						return;
					}
					Log.e(TAG, e.getMessage());
					try {
						if (outputStream != null) {
							outputStream.close();
						}
						outputStream = null;
						if (bluetoothSocket != null) {
							bluetoothSocket.close();
						}
					} catch (IOException closeException) {
						closeException.printStackTrace();
					}
					cleanupStream();
				}				
			}
		}.start();
	}

	/**
	 * @param sensorDevice the sensorDevice to set
	 */
	public void setSensorDevice(UISensorDevice sensorDevice) {
		this.sensorDevice = sensorDevice;
	}

	/**
	 * @param canStop the canStop to set
	 */
	public void setCanStop(boolean canStop) {
		this.canStop = canStop;
	}
}
