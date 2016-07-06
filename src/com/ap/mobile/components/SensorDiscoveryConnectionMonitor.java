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

import com.ap.mobile.model.UISensorDevice;

/**
 * @author jungwhan
 * SensorDiscoveryConnectionMonitor.java
 * 1:56:51 AM Feb 13, 2016 2016
 */
public interface SensorDiscoveryConnectionMonitor {
	/**
	 * 
	 * @param sensorDevice
	 */
	public void updateSensorDeviceConnection(UISensorDevice sensorDevice);
	
	/**
	 * Need to update the connection flag and have logic for retry the connection in here.
	 * @param sensorDevice
	 */
	public void manageSensorDeviceConnection(UISensorDevice sensorDevice);
}
