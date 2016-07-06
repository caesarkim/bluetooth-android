/**
 * AgingPlaceMobile
 */
package com.ap.core.device.http;

/* Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */
public class JSONClientException extends Exception {

	private static final long serialVersionUID = 4028290696506852310L;

	public JSONClientException(String message) {
		super(message);
	}
	
	public JSONClientException(Throwable t) {
		super(t);
	}
}
