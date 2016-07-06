/**
 * AgingPlaceMobile
 */
package com.ap.core.device.components;

/* Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */

/**
 * @author jungwhan
 * DeviceConnectionListener.java
 * 11:51:25 PM Feb 6, 2016 2016
 */
public interface DeviceConnectionListener {
	/**
	 * Callback method when the sensor is connected.
	 */
	public void connected();

	/**
	 * Callback method when the sensor is connected.
	 */
	public void retryConnect();
	
	/**
	 * 
	 */
	public void failConnected();

}
