/**
 * AgingPlaceMobile
 */
package com.ap.core.device.components;

/* Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */

import java.io.Serializable;

/**
 * @author jungwhan
 * BluetoothDiscoveryException.java
 * 3:36:45 PM Feb 6, 2016 2016
 */
public class SensorDiscoveryException extends Exception implements Serializable {

	private static final long serialVersionUID = 9176489635165851451L;

	public SensorDiscoveryException(String message) {
		super(message);
	}
	
	public SensorDiscoveryException(Throwable t) {
		super(t);
	}
}
