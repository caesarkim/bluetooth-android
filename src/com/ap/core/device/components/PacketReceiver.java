/**
 * AgingPlace
 */
package com.ap.core.device.components;

/* Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */

/**
 * @author jungwhan
 * PacketReceiver.java
 * 9:59:07 PM Feb 2, 2016 2016
 */
public interface PacketReceiver {
	/**
	 * Deliver a message
	 * @param message
	 */
	public void receive(String message);
}
