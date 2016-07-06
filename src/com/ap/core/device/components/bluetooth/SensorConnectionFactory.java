/**
 * AgingSafeSeniorMobile
 */
package com.ap.core.device.components.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import com.ap.core.device.components.DeviceConnection;
import com.ap.core.device.components.bluetooth.ble.SensorDeviceBLEConnection;
import com.ap.core.device.components.bluetooth.classic.SensorDeviceConnection;
import com.ap.mobile.model.UISensorDevice;

/**
 *  Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by jungwhan Projecteria LLC, May 18, 2016
 */

/**
 * @author jungwhan
 * SensorConnectionFactory.java
 * 4:14:08 PM May 18, 2016 2016
 */
public class SensorConnectionFactory {

	public static DeviceConnection getDeviceConnection(String type, BluetoothAdapter bluetoothAdapter, UISensorDevice sensorDevice, Context context) {
		if (type.equalsIgnoreCase(ModuleType.CLASSIC.name())) {
			return new SensorDeviceConnection(bluetoothAdapter, sensorDevice);
		} else if (type.equalsIgnoreCase(ModuleType.BLE.name())) {
			return new SensorDeviceBLEConnection(bluetoothAdapter, sensorDevice, context);
		}
		return null;
	}
}
