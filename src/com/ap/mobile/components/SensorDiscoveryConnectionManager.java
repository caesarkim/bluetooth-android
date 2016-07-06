/**
 * AgingPlaceMobile
 */
package com.ap.mobile.components;

/**
 * Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.ap.common.models.http.HttpCareReceiverConnectInfo;
import com.ap.common.models.http.HttpConnectTime;
import com.ap.common.models.http.HttpDataPacket;
import com.ap.core.device.ConnectType;
import com.ap.core.device.Constants;
import com.ap.core.device.CoreConfig;
import com.ap.core.device.ErrorCodes;
import com.ap.core.device.components.DeviceConnection;
import com.ap.core.device.components.PacketReceiver;
import com.ap.core.device.components.SensorDiscoveryException;
import com.ap.core.device.components.analyzer.DefaultRetryConnectionDistanceAnalyzer;
import com.ap.core.device.components.analyzer.DefaultSensorDataAnalyzer;
import com.ap.core.device.components.analyzer.SensorDataAnalyzer;
import com.ap.core.device.components.bluetooth.ModuleType;
import com.ap.core.device.components.bluetooth.SensorConnectionFactory;
import com.ap.core.device.components.bluetooth.classic.SensorDeviceConnection;
import com.ap.core.device.context.SensorContext;
import com.ap.core.device.exceptions.DataAnalyzeException;
import com.ap.core.device.exceptions.DeviceErrorException;
import com.ap.core.device.exceptions.DeviceException;
import com.ap.core.device.http.DefaultHttpJSONClient;
import com.ap.mobile.model.UISensorDevice;
import com.google.gson.Gson;

/**
 * @author jungwhan SensorDiscoveryConnectionManager.java 2:41:07 PM Feb 6,
 *         2016 2016
 */
public class SensorDiscoveryConnectionManager implements SensorDiscoveryConnectionMonitor {
	protected static final String TAG = SensorDiscoveryConnectionManager.class.getSimpleName();

	private static SensorDiscoveryConnectionManager sensorDiscoveryConnectionManager = SensorDiscoveryConnectionManager.getInstance();

	private BluetoothAdapter bluetoothAdapter;

	private Map<String, Map<String, DeviceConnection>> deviceConnections = new ConcurrentHashMap<String, Map<String, DeviceConnection>>();

	private List<PacketReceiver> packetReceivers = new ArrayList<PacketReceiver>();
	
	private SensorDiscoveryConnectionMonitor seniorDiscoveryConnectionMonitor;
	
	private DefaultRetryConnectionDistanceAnalyzer retryConnectionDistanceAnalyzer;
	
	private ReentrantLock connectionCountLock = new ReentrantLock();
	
	private SensorContext sensorContext;
	
	private List<UISensorDevice> sensorDevices = new ArrayList<UISensorDevice>();
	
	private int deviceConnectionCount;

	/**
	 * 
	 */
	private SensorDiscoveryConnectionManager() {
		super();
		seniorDiscoveryConnectionMonitor = this;
	}

	public static SensorDiscoveryConnectionManager getInstance() {
		if (sensorDiscoveryConnectionManager == null) {
			sensorDiscoveryConnectionManager = new SensorDiscoveryConnectionManager();
		}
		return sensorDiscoveryConnectionManager;
	}

	/**
	 * Initialize
	 * @throws SensorDiscoveryException
	 */
	public void init() throws SensorDiscoveryException {
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter == null) {
			Log.i(TAG, "Bluetooth does not support.");
			throw new SensorDiscoveryException("bluetoothAdapter can't be null.");
		}
		bluetoothAdapter.cancelDiscovery();
		if (bluetoothAdapter.isDiscovering()) {
			return;
		}
	}

	public void checkBluetoothConnections() {
		Set<String> set = deviceConnections.keySet();
		Iterator<String> iterator = set.iterator();
		while (iterator.hasNext()) {
			try {
				String deviceType = iterator.next();
				HashMap<String, DeviceConnection> deviceConnectionMap = (HashMap<String, DeviceConnection>)deviceConnections.get(deviceType);
				for (DeviceConnection deviceConnection : deviceConnectionMap.values()) {
					deviceConnection.validateConnection();
				}
			} catch (DeviceException e) {
				Log.e(TAG, e.getMessage());
			}
		}
	}
	
	public Map<String, Boolean> getSensorConnectionStatus() {
		Map<String, Boolean> connectionStatuses = new HashMap<String, Boolean>();
		Set<String> set = deviceConnections.keySet();
		Iterator<String> iterator = set.iterator();
		while (iterator.hasNext()) {
			String deviceType = iterator.next();
			HashMap<String, DeviceConnection> deviceConnectionMap = (HashMap<String, DeviceConnection>)deviceConnections.get(deviceType);
			for (DeviceConnection deviceConnection : deviceConnectionMap.values()) {
				connectionStatuses.put(deviceConnection.getSensorDeviceName(), deviceConnection.isConnected());
			}
		}
		return connectionStatuses;
	}
	
	/**
	 * 
	 * @param deviceName
	 * @return
	 */
	public boolean isDeviceConnected(String deviceName) {
		Set<String> set = deviceConnections.keySet();
		Iterator<String> iterator = set.iterator();
		while (iterator.hasNext()) {
			String deviceType = iterator.next();
			HashMap<String, DeviceConnection> deviceConnectionMap = (HashMap<String, DeviceConnection>)deviceConnections.get(deviceType);
			for (DeviceConnection deviceConnection : deviceConnectionMap.values()) {
				if (deviceName.equals(deviceConnection.getSensorDeviceName())) {
					return deviceConnection.isConnected();
				}
			}
		}
		return false;
	}

	/**
	 * Start connecting to sensors
	 * @param sensorDevices
	 * @throws SensorDiscoveryException
	 */
	public void startConnections(final UISensorDevice sensorDevice, final Object lock, final List<DeviceErrorException> exceptions) throws SensorDiscoveryException {
		new Thread() {
			public void run() {
				Log.i(TAG, "sensorDevice=" + sensorDevice);
				DeviceConnection deviceConnection = null;
				try {
					Map<String, DeviceConnection> deviceConnectionList = deviceConnections.get(sensorDevice.getSensorType());
					if (deviceConnectionList == null || deviceConnectionList.isEmpty()) {
						deviceConnectionList = new HashMap<String, DeviceConnection>();
						deviceConnection = SensorConnectionFactory.getDeviceConnection(ModuleType.CLASSIC.name(), bluetoothAdapter, sensorDevice, sensorContext.getUiContext());//new SensorDeviceConnection(bluetoothAdapter, sensorDevice);
					} else {
						deviceConnection = deviceConnectionList.get(sensorDevice.getSensorName());
						if (deviceConnection == null) {
							deviceConnection = SensorConnectionFactory.getDeviceConnection(ModuleType.CLASSIC.name(), bluetoothAdapter, sensorDevice, sensorContext.getUiContext());//new SensorDeviceConnection(bluetoothAdapter, sensorDevice);
						}
					}
					((SensorDeviceConnection)deviceConnection).setSensorDiscoveryConnectionMonitor(seniorDiscoveryConnectionMonitor);
					for (PacketReceiver packetReceiver : packetReceivers) {
						deviceConnection.registerPacketReceiver(packetReceiver);
					}
					deviceConnectionList.put(sensorDevice.getSensorName(), deviceConnection);
					if (!deviceConnection.isConnected()) {
						try {
							Log.d(TAG, sensorDevice.getSensorName() + " is being discovered and being connected...");
							deviceConnection.discovery();
							sensorDevice.setConnected(true);
							deviceConnectionCount++;
						} catch (Exception e) {
							sensorDevice.setConnected(false);
							if (exceptions != null) {
								exceptions.add(new DeviceErrorException(ErrorCodes.CODE_CANNOT_DETECT, "Can not detect a sensor." + sensorDevice.getSensorName()));
							}
							throw new SensorDiscoveryException(e);
						}
					}
					sensorDevices.add(sensorDevice);
					sensorContext.getCareReceiverConnectionInformation().addConnectTime(new HttpConnectTime(ConnectType.CONNECTED.getConnectType(), System.currentTimeMillis()));
					deviceConnections.put(sensorDevice.getSensorType(), deviceConnectionList);
				} catch (Exception e) {
					Log.e(TAG, e.getMessage());
					e.printStackTrace();
				} finally {
					if (lock != null) {
						synchronized (lock) {
							lock.notify();
						}						
					}
				}
			}
		}.start();
	}
	
	/**
	 * Start discovering
	 * @param sensorDevices
	 * @throws SensorDiscoveryException
	 */
	public void handleReconnection() throws Exception {
		Set<String> set = deviceConnections.keySet();
		Iterator<String> iterator = set.iterator();
		while (iterator.hasNext()) {
			try {
				String deviceType = iterator.next();
				Map<String, DeviceConnection> deviceConnectionList = deviceConnections.get(deviceType);
				for (DeviceConnection deviceConnection : deviceConnectionList.values()) {
					deviceConnection.reConnect();
				}
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
				throw e;
			}
		}
	}

	/**
	 * Handle reset
	 * @throws DeviceException
	 */
	public void resetSensors() throws DeviceException {
		Log.i(TAG, "resetSensors is called at "+ new java.util.Date());
		for (PacketReceiver packetReceiver : packetReceivers) {
			if (packetReceiver instanceof SensorDataAnalyzer) {
				((SensorDataAnalyzer)packetReceiver).stopAnalyze();
			}
		}
		packetReceivers.clear(); // clear the packet receivers
		cancelDiscovery();
		deviceConnections.clear();
		sensorDevices.clear();
		Log.i(TAG, "resetSensors is finished at "+ new java.util.Date());
	}

	/**
	 * Add packet receiver
	 * @param packetReceiver
	 */
	public void addPacketReceiver(PacketReceiver packetReceiver) {
		packetReceivers.add(packetReceiver);
	}

	/**
	 * Send data packet to a sensor
	 * @param sensorType
	 * @param dataPacket
	 */
	protected void writeToSensor(String sensorType, HttpDataPacket dataPacket) throws DeviceException {
		if (sensorType.equalsIgnoreCase("all")) {
			Log.i(TAG, "Sending a message to all sensors.");
			writeToAllSensors(dataPacket);
		} else {
			Map<String, DeviceConnection> deviceConnectionList = deviceConnections.get(sensorType);
			for (DeviceConnection deviceConnection : deviceConnectionList.values()) {
				try {
					Log.i(TAG, "Sending a message to " + sensorType);
					deviceConnection.sendMessageEvent(dataPacket);
				} catch (DeviceException e) {
					throw e;
				}
			}
		}
	}

	/**
	 * Write to all the sensors.
	 * @param dataPacket
	 * @throws DeviceException
	 */
	protected void writeToAllSensors(HttpDataPacket dataPacket) throws DeviceException {
		Set<String> set = deviceConnections.keySet();
		Iterator<String> iterator = set.iterator();
		while (iterator.hasNext()) {
			try {
				String deviceType = iterator.next();
				Map<String, DeviceConnection> deviceConnectionList = deviceConnections.get(deviceType);
				for (DeviceConnection deviceConnection : deviceConnectionList.values()) {
					deviceConnection.sendMessageEvent(dataPacket);
				}
			} catch (DeviceException e) {
				Log.e(TAG, e.getMessage());
				throw e;
			}
		}
	}

	/**
	 * Stop sensor discovery
	 */
	public void cancelDiscovery() {
		Log.i(TAG, "cancelDiscovery is called at "+ new java.util.Date());
		Set<String> set = deviceConnections.keySet();
		Iterator<String> iterator = set.iterator();
		while (iterator.hasNext()) {
			try {
				String deviceType = iterator.next();
				Map<String, DeviceConnection> deviceConnectionList = deviceConnections.get(deviceType);
				for (DeviceConnection deviceConnection : deviceConnectionList.values()) {
					deviceConnection.disconnect();
				}
			} catch (DeviceException e) {
				Log.e(TAG, e.getMessage());
				e.printStackTrace();
			}
		}
		if (bluetoothAdapter != null) {
			bluetoothAdapter.cancelDiscovery();
		}
		sensorContext.setSensorsConnected(false);
		Log.i(TAG, "cancelDiscovery is finished at "+ new java.util.Date());
	}

	/**
	 * @return the deviceConnections
	 */
	public Map<String, Map<String, DeviceConnection>> getDeviceConnections() {
		return deviceConnections;
	}
	
	/**
	 * 
	 * @param sensorDevice
	 * @return
	 */
	private DeviceConnection getDeviceConnection(UISensorDevice sensorDevice) {
		Map<String, DeviceConnection> deviceConnectionList = null;
		DeviceConnection deviceConnection = null;
		Set<String> set = deviceConnections.keySet();
		Iterator<String> iterator = set.iterator();
		while (iterator.hasNext()) {
			String deviceType = iterator.next();
			if (sensorDevice.getSensorType().equalsIgnoreCase(deviceType)) {
				deviceConnectionList = deviceConnections.get(deviceType);
				for (DeviceConnection deviceConnectionTemp : deviceConnectionList.values()) {
					if (deviceConnectionTemp.getSensorDeviceName().equalsIgnoreCase(sensorDevice.getSensorName())) {
						deviceConnection = deviceConnectionTemp;
						break;
					}
				}
				if (deviceConnection != null) {
					break;
				}
			}
		}
		return deviceConnection;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ap.mobile.components.SensorDiscoveryConnectionMonitor#updateSensorDeviceConnection(com.ap.mobile.model.UISensorDevice)
	 */
	@Override
	public void updateSensorDeviceConnection(UISensorDevice sensorDevice) {
		try {
			if (connectionCountLock.tryLock()) {
				Log.i(TAG, "Sensor="+ sensorDevice.getSensorName() + " has acquired the lock.");
				DeviceConnection deviceConnection = getDeviceConnection(sensorDevice);
				if (deviceConnection != null) {
					try {
						deviceConnection.disconnect();
						Log.i(TAG, "connection for "+ deviceConnection.getSensorDeviceName() + " has been closed.");
						Log.i(TAG, "sensorDevice="+ sensorDevice + " is disconnected.");
						sensorContext.setSensorsConnected(false);
					} catch (DeviceException e) {
						e.printStackTrace();
					}
				}
					// Deadlock took place in here.
//					this.cancelDiscovery();
				if (retryConnectionDistanceAnalyzer != null) {
					try {
						while (true) {
							retryConnectionDistanceAnalyzer.analyze();
							long interval = retryConnectionDistanceAnalyzer.getInterval();
							Log.i(TAG, "Interval is "+ getTime(interval));
							try {
								Thread.sleep(interval);
							} catch (InterruptedException e) {
								//
							}
							try {
								// Need to fix. It picks one of the devices at random and keeps trying to connect. 
								// Need to create another method just to see if it can connect.
								checkConnection();
//								retryConnection();
								for (PacketReceiver receiver : packetReceivers) {
									if (receiver instanceof DefaultSensorDataAnalyzer) {
										((DefaultSensorDataAnalyzer)receiver).initialize();
									}
								}
								sensorContext.setSensorsConnected(true);
								Log.i(TAG, "All the sensor reconnections are successful.....");
								sensorContext.getCareReceiverConnectionInformation().addConnectTime(new HttpConnectTime(ConnectType.CONNECTED.getConnectType(), System.currentTimeMillis()));
								int connectTimeInfoCount = sensorContext.getCareReceiverConnectionInformation().getConnectTime().size();
								Log.i(TAG, "connectTimeInfoCount="+ connectTimeInfoCount);
								Log.i(TAG, "careReceiverConnectionInformation="+ sensorContext.getCareReceiverConnectionInformation());
								if (connectTimeInfoCount >= 10) {
									// Upload care receiver's connect/disconnect time.
									uploadCareReceiverConnectionTimesInfo(sensorContext.getCareReceiverConnectionInformation());
									sensorContext.getCareReceiverConnectionInformation().getConnectTime().clear();
								}
								// Update the log
								break;
							} catch (DeviceException e) {
								Log.i(TAG, sensorDevice.getSensorName() + "tryReconnection.Reconnection failed with "+ e.getMessage());
								sensorContext.setSensorsConnected(false);
							} catch (Exception e) {
								Log.i(TAG, sensorDevice.getSensorName() + ".tryReconnection.Reconnection failed with "+ e.getMessage());
								sensorContext.setSensorsConnected(false);
							}
						}
					} catch (DataAnalyzeException e) {
						e.printStackTrace();
					}
				}
			} else {
				Log.i(TAG, "Sensor="+ sensorDevice.getSensorName() + " can not acquire the lock.");
			}
		} finally {
			try {
				if (connectionCountLock != null) {
					connectionCountLock.unlock();
				}				
			} catch (Exception e) {
				//
			}
		}
		Log.i(TAG, "updateSensorDeviceConnection ended.");
	}
	
	/**
	 * 
	 * @param httpCareReceiverConnectInfo
	 */
	private void uploadCareReceiverConnectionTimesInfo(HttpCareReceiverConnectInfo httpCareReceiverConnectInfo) {
		String url = CoreConfig.getUrl("/user/carereceiver/connect_stats");
		Gson gson = new Gson();
		String jsonRequest = gson.toJson(httpCareReceiverConnectInfo);
		Log.i(TAG, "url="+ url);
		Log.i(TAG, "jsonRequest="+ jsonRequest);
		DefaultHttpJSONClient httpClient = new DefaultHttpJSONClient();
		try {
			httpClient.executePost(url, jsonRequest);
		} catch (IOException e) {
			Log.i(TAG, e.getMessage());
		}
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public void checkConnection() throws DeviceException, IOException {
		Map<String, DeviceConnection> deviceConnectionList = null;
		Set<String> set = deviceConnections.keySet();
		Iterator<String> iterator = set.iterator();
		while (iterator.hasNext()) {
			String deviceType = iterator.next();
			deviceConnectionList = deviceConnections.get(deviceType);
			for (DeviceConnection deviceConnection : deviceConnectionList.values()) {
				if (deviceConnection.isConnected()) {
					continue;
				}
				deviceConnection.checkConnection();
				break; // Need to fix, because if one of the device is on low battery or no battery, only this device will fail, but other devices will be ok.
			}
		}
		sensorContext.setSensorsConnected(true);
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public void retryConnection() throws DeviceException, IOException {
		Map<String, DeviceConnection> deviceConnectionList = null;
		Set<String> set = deviceConnections.keySet();
		Iterator<String> iterator = set.iterator();
		while (iterator.hasNext()) {
			String deviceType = iterator.next();
			deviceConnectionList = deviceConnections.get(deviceType);
			for (DeviceConnection deviceConnection : deviceConnectionList.values()) {
				if (deviceConnection.isConnected()) {
					continue;
				}
				deviceConnection.discovery();
			}
		}
		sensorContext.setSensorsConnected(true);
	}
	
	private String getTime(long time) {
		String msg = "";
		int hour = 0;
		if (time > (1000 * 60 * 60)) {
			hour = (int)time /(1000 * 60 * 60);
			msg += hour + " hour(s)";
		}
		long remaining = time - (1000 * 60 * 60 * hour);
		if (remaining > (1000 * 60 * 1)) {
			long minutes = remaining / (1000 * 60 * 1);
			msg += " " + (remaining /(1000 * 60 * 1)) + " minute(s)";
			remaining = remaining - (1000 * 60 * 1 * minutes);
		}
		if (remaining > 0) {
			remaining = remaining / 1000;
			msg += " " + remaining + " second(s)";
		}
		return msg;
	}

	/**
	 * @param retryConnectionDistanceAnalyzer the retryConnectionDistanceAnalyzer to set
	 */
	public void setRetryConnectionDistanceAnalyzer(DefaultRetryConnectionDistanceAnalyzer retryConnectionDistanceAnalyzer) {
		this.retryConnectionDistanceAnalyzer = retryConnectionDistanceAnalyzer;
	}

	/**
	 * @param sensorContext the sensorContext to set
	 */
	public void setSensorContext(SensorContext sensorContext) {
		this.sensorContext = sensorContext;
	}

	/**
	 * @return the sensorContext
	 */
	public SensorContext getSensorContext() {
		return sensorContext;
	}

	/* (non-Javadoc)
	 * @see com.ap.mobile.components.SensorDiscoveryConnectionMonitor#manageSensorDeviceConnection(com.ap.mobile.model.UISensorDevice)
	 */
	@Override
	public synchronized void manageSensorDeviceConnection(UISensorDevice sensorDevice) {
		Log.i(TAG, "Sensor="+ sensorDevice.getSensorName() + " has acquired the lock.");
		DeviceConnection deviceConnection = getDeviceConnection(sensorDevice);
		if (deviceConnection != null) {
			try {
				deviceConnection.disconnect();
				Log.i(TAG, "connection for "+ deviceConnection.getSensorDeviceName() + " has been closed.");
				Log.i(TAG, "sensorDevice="+ sensorDevice.getSensorName() + " is disconnected.");
				sensorContext.setSensorsConnected(false);
			} catch (DeviceException e) {
				e.printStackTrace();
			}
		}
		deviceConnectionCount--;
		Log.i(TAG, "deviceConnectionCount="+ deviceConnectionCount);
		if (deviceConnectionCount > 0) {
			return;
		}
		if (retryConnectionDistanceAnalyzer != null) {
			try {
				for (PacketReceiver receiver : packetReceivers) {
					if (receiver instanceof DefaultSensorDataAnalyzer) {
						((DefaultSensorDataAnalyzer)receiver).stopAnalyze();
					}
				}
				Log.i(TAG, "sensorDevice="+ sensorDevice + " is sending a message to the SafeSeniorService for retry logic...");
				Intent messageQueueIntent = new Intent(Constants.MESSAGE_SERVICE_QUEUE);
				messageQueueIntent.putExtra(Constants.MESSAGE_COMMAND_QUEUE, Constants.MESSAGE_SENSOR_DISCONNECTED);
				LocalBroadcastManager.getInstance(sensorContext.getUiContext()).sendBroadcast(messageQueueIntent);
//				while (true) {
//					retryConnectionDistanceAnalyzer.analyze();
//					long interval = retryConnectionDistanceAnalyzer.getInterval();
//					Log.i(TAG, "Interval is "+ getTime(interval));
//					try {
//						Thread.sleep(interval);
//					} catch (InterruptedException e) {
//						//
//					}
//					try {
//						// Need to fix. It picks one of the devices at random and keeps trying to connect. 
//						// Need to create another method just to see if it can connect.
//						checkConnection();
//						retryConnection();
//						for (PacketReceiver receiver : packetReceivers) {
//							if (receiver instanceof DefaultSensorDataAnalyzer) {
//								((DefaultSensorDataAnalyzer)receiver).initialize();
//							}
//						}
//						sensorContext.setSensorsConnected(true);
//						Log.i(TAG, "All the sensor reconnections are successful.....");
//						sensorContext.getCareReceiverConnectionInformation().addConnectTime(new HttpConnectTime(ConnectType.CONNECTED.getConnectType(), System.currentTimeMillis()));
//						int connectTimeInfoCount = sensorContext.getCareReceiverConnectionInformation().getConnectTime().size();
//						Log.i(TAG, "connectTimeInfoCount="+ connectTimeInfoCount);
//						Log.i(TAG, "careReceiverConnectionInformation="+ sensorContext.getCareReceiverConnectionInformation());
//						if (connectTimeInfoCount >= 10) {
//							// Upload care receiver's connect/disconnect time.
//							uploadCareReceiverConnectionTimesInfo(sensorContext.getCareReceiverConnectionInformation());
//							sensorContext.getCareReceiverConnectionInformation().getConnectTime().clear();
//						}
//						deviceConnectionCount = deviceConnections.size();
//						// Update the log
//						break;
//					} catch (DeviceException e) {
//						Log.i(TAG, sensorDevice.getSensorName() + "tryReconnection.Reconnection failed with "+ e.getMessage());
//						sensorContext.setSensorsConnected(false);
//					} catch (Exception e) {
//						Log.i(TAG, sensorDevice.getSensorName() + ".tryReconnection.Reconnection failed with "+ e.getMessage());
//						sensorContext.setSensorsConnected(false);
//					}
//				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		Log.i(TAG, sensorDevice.getSensorName() + ".manageSensorDeviceConnection ended.");
	}
}
