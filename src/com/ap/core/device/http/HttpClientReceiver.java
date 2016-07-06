/**
 * AgingPlaceMobile
 */
package com.ap.core.device.http;

/* Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */
public interface HttpClientReceiver {
	/**
	 * 
	 * @param message
	 */
	public void notifyInvoker(String message);
}
