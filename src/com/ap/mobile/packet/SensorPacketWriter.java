/**
 * AgingPlaceMobile
 */
package com.ap.mobile.packet;

/**
 * Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */

import java.io.IOException;
import java.io.OutputStream;

import android.util.Log;

import com.ap.core.device.components.PacketWriter;

/**
 * @author jungwhan
 * SensorPacketWriter.java
 * 3:28:27 PM Feb 6, 2016 2016
 */
public class SensorPacketWriter implements PacketWriter {
	protected static final String TAG = "SensorPacketWriter";
	
	private OutputStream outputStream;
	
	public SensorPacketWriter(OutputStream outputStream) {
		this.outputStream = outputStream;
	}

	/* (non-Javadoc)
	 * @see com.ap.common.device.components.PacketWriter#write(java.lang.String)
	 */
	@Override
	public void write(String message) {
		try {
			this.outputStream.write(message.getBytes());
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
		}
	}

}
