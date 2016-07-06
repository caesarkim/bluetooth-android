/**
 * AgingPlaceMobile
 */
package com.ap.core.device.utils;

import com.ap.common.models.http.HttpDataPacket;
import com.ap.core.device.SensorData;

/**
 * Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */
public class MessageUtils {

	/**
	 * Convert a message string to DataPacket
	 * @param message
	 * @return
	 */
	public static HttpDataPacket toDataPacket(String message) {
		String[] messages = message.split("\\|");
		if (messages == null || messages.length < 5) {
			return null;
		}
		HttpDataPacket packet = new HttpDataPacket();
		packet.setSensorType(messages[0]);
		packet.setSensorName(messages[1]);
		packet.setPacketType(messages[2]);
		packet.setPacketValue(messages[3]);
		packet.setBatteryLevel(messages[4]);
		packet.setTimestamp(System.currentTimeMillis());
		return packet;
	}
	
	/**
	 * Convert a message string to SensorData
	 * @param message
	 * @return
	 */
	public static SensorData toSensorData(String message) {
		String[] messages = message.split("\\|");
		if (messages == null || messages.length < 5) {
			return null;
		}
		SensorData sensorData = new SensorData();
		sensorData.setSensorType(messages[0]);
		sensorData.setSensorName(messages[1]);
		sensorData.setPacketType(messages[2]);
		sensorData.setPacketValue(messages[3]);
		sensorData.setBatteryLevel(messages[4]);
		sensorData.setTimestamp(System.currentTimeMillis());
		return sensorData;
	}
}
