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
 * PacketWriter.java
 * 3:26:39 PM Feb 6, 2016 2016
 */
public interface PacketWriter {
	/**
	 * Deliver a message
	 * @param message
	 */
	public void write(String message);
}
