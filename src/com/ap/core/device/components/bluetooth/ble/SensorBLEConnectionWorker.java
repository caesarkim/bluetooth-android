/**
 * AgingSafeSeniorMobile
 */
package com.ap.core.device.components.bluetooth.ble;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.ap.core.device.components.DeviceConnectionListener;
import com.ap.core.device.thread.Worker;


/**
 *  Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by jungwhan Projecteria LLC, May 18, 2016
 */

/**
 * @author jungwhan
 * SensorBLEConnectionWorker.java
 * 4:15:02 PM May 18, 2016 2016
 */
public class SensorBLEConnectionWorker extends Worker {
	protected static final String TAG = SensorBLEConnectionWorker.class.getSimpleName();
	
	public final static String ACTION_GATT_CONNECTED = "com.nordicsemi.nrfUART.ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_DISCONNECTED = "com.nordicsemi.nrfUART.ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.nordicsemi.nrfUART.ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_DATA_AVAILABLE = "com.nordicsemi.nrfUART.ACTION_DATA_AVAILABLE";
	public final static String EXTRA_DATA = "com.nordicsemi.nrfUART.EXTRA_DATA";
	public final static String DEVICE_DOES_NOT_SUPPORT_UART = "com.nordicsemi.nrfUART.DEVICE_DOES_NOT_SUPPORT_UART";
	
	public static final UUID RX_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
	public static final UUID RX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
	public static final UUID TX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
	public static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
	
	private BluetoothDevice bluetoothDevice;
	
	private BluetoothGatt mBluetoothGatt;
	
	private boolean isConnected = false;
	
	private Context context;
	
	private boolean canStop = false;
	
	private List<DeviceConnectionListener> blueDeviceConnectionListeners = new ArrayList<DeviceConnectionListener>();
	
	/**
	 * 
	 * @param bluetoothDevice
	 */
	public SensorBLEConnectionWorker(BluetoothDevice bluetoothDevice) {
		this.bluetoothDevice = bluetoothDevice;
		reRegisterIntentFilters();
	}
	
	public void setContext(Context context) {
		this.context = context;
	}
	
	public void initialize() throws IOException {
		Log.i(TAG, "initialize is called.");
		init();
		if (bluetoothDevice == null) {
			throw new IOException("bluetoothDevice can't be null.");
		}
		mBluetoothGatt = bluetoothDevice.connectGatt(context, true, mGattCallback);
		if (mBluetoothGatt == null) {
			throw new IOException("can't connect");
		}
	}
	
	public void init() {
		if (mBluetoothGatt != null) {
			Log.i(TAG, "Trying to use an existing mBluetoothGatt for connection.");
			if (mBluetoothGatt.connect()) {
				return;
			}
		}
	}
	
	public void reRegisterIntentFilters() {
		unregisterReceiver();
		LocalBroadcastManager.getInstance(context).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
	}
	
	public void unregisterReceiver() {
        try {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
	}

	/**
	 * @param deviceConnectionListener
	 */
	public void addBluetoothDeviceConnectionListener(DeviceConnectionListener deviceConnectionListener) {
		blueDeviceConnectionListeners.add(deviceConnectionListener);
	}
	
	@Override
	public void run() {
		try {
			initialize();
		} catch (IOException e) {
			e.printStackTrace();
		}
		setBLEMode();
	}
	
	public void setBLEMode() {
		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(1000 * 20);
				} catch (InterruptedException e) {
					//
				}
				writeRXCharacteristic("D1".getBytes());
			}
		}.start();
	}
	
	/**
	 * @param canStop the canStop to set
	 */
	public void setCanStop(boolean canStop) {
		this.canStop = canStop;
	}
	
	
	private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
		final Intent intent = new Intent(action);
		if (TX_CHAR_UUID.equals(characteristic.getUuid())) {
			intent.putExtra(EXTRA_DATA, characteristic.getValue());
		} else {

		}
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}
	
	private void broadcastUpdate(final String action) {
		final Intent intent = new Intent(action);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}
	
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			String intentAction;

			if (newState == BluetoothProfile.STATE_CONNECTED) {
				intentAction = ACTION_GATT_CONNECTED;
				broadcastUpdate(intentAction);
				Log.i(TAG, "Connected to GATT server.");
				Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt);
				isConnected = true;
				mBluetoothGatt.discoverServices();
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				intentAction = ACTION_GATT_DISCONNECTED;
//				mConnectionState = STATE_DISCONNECTED;
				Log.i(TAG, "Disconnected from GATT server.");
				broadcastUpdate(intentAction);
				isConnected = false;
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Log.w(TAG, "mBluetoothGatt = " + mBluetoothGatt);
				broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
			} else {
				Log.w(TAG, "onServicesDiscovered received: " + status);
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
		}
	};
	
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_GATT_CONNECTED);
        intentFilter.addAction(ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ACTION_DATA_AVAILABLE);
        intentFilter.addAction(DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }
    
	/**
	 * Retry the connection
	 */
	private void retryConnect() {
		for (DeviceConnectionListener blueDeviceConnectionListener : blueDeviceConnectionListeners) {
			blueDeviceConnectionListener.retryConnect();
		}
	}
	
	private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_GATT_SERVICES_DISCOVERED)) {
                enableTXNotification();
                setBLEMode();
            }
            //*********************//
            if (action.equals(ACTION_DATA_AVAILABLE)) {
                final byte[] txValue = intent.getByteArrayExtra(EXTRA_DATA);
                try {
                    String text = new String(txValue, "UTF-8");
                    String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
	                Log.i(TAG, "text=currentDateTimeString::::"+ currentDateTimeString + "=="+ text);
                } catch (Exception e) {
                	e.printStackTrace();
                }
            }
            if (action.equals(ACTION_GATT_DISCONNECTED)) {
//            	cleanupStream();
//            	retryConnect();
            }
		}
	};
	
	/**
	 * Retrieves a list of supported GATT services on the connected device. This
	 * should be invoked only after {@code BluetoothGatt#discoverServices()}
	 * completes successfully.
	 * 
	 * @return A {@code List} of supported services.
	 */
	public List<BluetoothGattService> getSupportedGattServices() {
		if (mBluetoothGatt == null)
			return null;

		return mBluetoothGatt.getServices();
	}
	
	public void enableTXNotification() {
		/*
		 * if (mBluetoothGatt == null) { showMessage("mBluetoothGatt null" +
		 * mBluetoothGatt); broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
		 * return; }
		 */
		if (mBluetoothGatt == null) {
			return;
		}
		BluetoothGattService RxService = mBluetoothGatt.getService(RX_SERVICE_UUID);
		if (RxService == null) {
			showMessage("Rx service not found!");
			broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
			return;
		}
		BluetoothGattCharacteristic TxChar = RxService.getCharacteristic(TX_CHAR_UUID);
		if (TxChar == null) {
			showMessage("Tx charateristic not found!");
			broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
			return;
		}
		mBluetoothGatt.setCharacteristicNotification(TxChar, true);

		BluetoothGattDescriptor descriptor = TxChar.getDescriptor(CCCD);
		descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		mBluetoothGatt.writeDescriptor(descriptor);

	}
	
	public void cleanupStream() {
		if (mBluetoothGatt != null) {
			mBluetoothGatt.disconnect();
			mBluetoothGatt.close();
		}
	}
	
	private void showMessage(String msg) {
		Log.e(TAG, msg);
	}
	
	public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
		mBluetoothGatt.readCharacteristic(characteristic);
	}
	
	public void writeRXCharacteristic(byte[] value) {
		if (mBluetoothGatt == null) {
			try {
				initialize();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			BluetoothGattService RxService = mBluetoothGatt.getService(RX_SERVICE_UUID);
			showMessage("mBluetoothGatt null" + mBluetoothGatt);
			if (RxService == null) {
				showMessage("Rx service not found!");
//				broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
				return;
			}
			BluetoothGattCharacteristic RxChar = RxService.getCharacteristic(RX_CHAR_UUID);
			if (RxChar == null) {
				showMessage("Rx charateristic not found!");
//				broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
				return;
			}
			RxChar.setValue(value);
			boolean status = mBluetoothGatt.writeCharacteristic(RxChar);

			Log.d(TAG, "write TXchar - status=" + status);			
		} catch (Exception e) {
			Log.e(TAG, "Error="+ e.getMessage());
		}
	}

	/**
	 * @return the isConnected
	 */
	public boolean isConnected() {
		return isConnected;
	}
}
