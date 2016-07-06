/**
 * AgingSafeSeniorMobile
 */
package com.ap.mobile.services;

import android.app.Service;
import android.util.Log;

import com.ap.common.models.http.HttpGeneric;
import com.ap.core.device.CoreConfig;
import com.ap.core.device.http.DefaultHttpJSONClient;
import com.google.gson.Gson;

/**
 *  Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by jungwhan Projecteria LLC, May 9, 2016
 */

/**
 * @author jungwhan
 * AbstractService.java
 * 11:45:32 PM May 9, 2016 2016
 */
public abstract class AbstractService extends Service {
	
	protected void log(final String tag, final String message) {
		Log.i(tag, message);
		new Thread() {
			@Override
			public void run() {
				HttpGeneric logRequest = new HttpGeneric();
				logRequest.setData(message);
				String url = CoreConfig.getUrl("/sensor/save");
				Gson gson = new Gson();
				String jsonRequest = gson.toJson(logRequest);
				DefaultHttpJSONClient httpClient = new DefaultHttpJSONClient();
				try {
					httpClient.executePost(url, jsonRequest);
				} catch (Exception e) {
//					log(TAG, e.getMessage());
				}
			}
		}.start();
	}

}
