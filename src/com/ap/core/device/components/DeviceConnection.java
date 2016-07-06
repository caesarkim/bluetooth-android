
package com.ap.core.device.components;

/* Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */

import java.io.IOException;

import com.ap.common.models.http.HttpDataPacket;
import com.ap.core.device.exceptions.DeviceException;


/**
 * @author jungwhan
 * Copyright (c) 2016, Jungwhan Kim. All rights reserved.
 * DeviceConnection.java
 * 8:51:58 PM Feb 2, 2016 2016
 */
public interface DeviceConnection {
	/**
	 * Register a data receiver
	 * @param receiver
	 */
	public void registerPacketReceiver(PacketReceiver receiver);
	
	/**
	 * 
	 * @return
	 */
	public String getSensorDeviceName();
	
	/**
	 * 
	 * @throws DeviceException
	 */
	public void reConnect() throws DeviceException;
	
	/**
	 * 
	 * @return
	 */
	public boolean isConnected();
	
	/**
	 * Unregister a data register
	 * @param receiver
	 */
	public void unregisterPacketReceiver(PacketReceiver receiver);
	
	/**
	 * Start a discovery
	 * @throws DeviceException
	 */
	public void discovery() throws DeviceException, IOException;

	/**
	 * Check connection
	 * @throws DeviceException
	 */
	public void checkConnection() throws DeviceException, IOException;
	
	/**
	 * 
	 * @throws DeviceException
	 */
	public void validateConnection() throws DeviceException;
	
	/**
	 * Disconnect from a device
	 * @throws DeviceException
	 */
	public void disconnect() throws DeviceException;
	
	/**
	 * Disconnect from a device
	 * @throws DeviceException
	 */
	public void cancel() throws DeviceException;
	
	/**
	 * 
	 * @param dataPacket
	 * @throws DeviceException
	 */
	public void sendMessageEvent(HttpDataPacket dataPacket) throws DeviceException;
}
