/**
 * AgingPlaceMobile
 */
package com.ap.mobile.services.receivers;

/**
 * Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ap.mobile.services.SafeSeniorService;

/**
 * @author jungwhan
 * SensorResetRequestReceiver.java
 * 10:37:03 PM Mar 5, 2016 2016
 */
public class SensorResetRequestReceiver extends BroadcastReceiver {
	protected static final String TAG = SensorResetRequestReceiver.class.getSimpleName();
	
	private SafeSeniorService safeSeniorService;
	
	public SensorResetRequestReceiver() {
		super();
	}

	/* (non-Javadoc)
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(TAG, "SensorResetRequestReceiver.onReceive is called.");
		if (safeSeniorService != null) {
			safeSeniorService.resetSensors();
		}

	}
}
