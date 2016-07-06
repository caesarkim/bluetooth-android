/**
 * Storyty
 */
package com.ap.core.device.utils;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/* Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */
public class JSONUtils {
	
	public static String toJSON(Object obj) {
		Gson gson = new Gson();
		return gson.toJson(obj);
	}

	public static <T> T getResponseMessage(String responseMessageJSON, Class<T> clazz) {
		Gson gson = new GsonBuilder().create();
		return gson.fromJson(responseMessageJSON, clazz);
	}
	
	public static <T> T getTypeResponseMessage(String responseMessageJSON, Type responseMessageType) {
		Gson gson = new GsonBuilder().create();
		return gson.fromJson(responseMessageJSON, responseMessageType);
	}
}
