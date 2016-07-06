/**
 * AgingPlaceMobile
 */
package com.ap.core.device.exceptions;

import com.ap.core.device.components.SensorDiscoveryException;

/* Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */
public class DeviceErrorException extends SensorDiscoveryException {
	private static final long serialVersionUID = 955139640114276100L;
	
	private int errorCode;
	
	private String message;

	public DeviceErrorException(int errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
		this.message = message;
	}
	
	public DeviceErrorException(Throwable t) {
		super(t);
	}

	/**
	 * @return the errorCode
	 */
	public int getErrorCode() {
		return errorCode;
	}

	/**
	 * @param errorCode the errorCode to set
	 */
	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}
}
