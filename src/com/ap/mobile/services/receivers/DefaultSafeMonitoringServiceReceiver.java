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
import android.os.SystemClock;
import android.util.Log;

import com.ap.mobile.services.DefaultSafeSeniorMonitorService;
import com.ap.mobile.services.DefaultSafeSeniorService;

/**
 * @author jungwhan
 * DefaultSafeMonitoringServiceReceiver.java
 * 12:49:47 AM Feb 27, 2016 2016
 */
public class DefaultSafeMonitoringServiceReceiver extends BroadcastReceiver {
	protected static final String TAG = DefaultSafeMonitoringServiceReceiver.class.getSimpleName();

	/* (non-Javadoc)
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
	     Intent safeSeniorService = new Intent(context, DefaultSafeSeniorService.class);
	     Log.i("DefaultSafeMonitoringServiceReceiver", "Starting DefaultSafeSeniorService @ " + SystemClock.elapsedRealtime());
	     context.startService(safeSeniorService);
	     
	     Intent safeSeniorMonitorService = new Intent(context, DefaultSafeSeniorMonitorService.class);
	     Log.i("DefaultSafeMonitoringServiceReceiver", "Starting DefaultSafeSeniorMonitorService @ " + SystemClock.elapsedRealtime());
	     context.startService(safeSeniorMonitorService);
	}

}