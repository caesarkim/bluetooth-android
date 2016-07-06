/**
 * AgingPlace
 */
package com.ap.core.device.exceptions;

/* Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */
public class DeviceException extends Exception {

	private static final long serialVersionUID = 1370044601424418261L;

	public DeviceException(String message) {
		super(message);
	}
	
	public DeviceException(Throwable t) {
		super(t);
	}
}
