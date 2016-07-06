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

/**
 * @author jungwhan
 * GCMBroadcastReceiver.java
 * 1:01:58 AM Mar 11, 2016 2016
 */
public class NotificationReceiver extends BroadcastReceiver {
	/* (non-Javadoc)
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("GCMBroadcastReceiver", "&&&&&&&&&&&&&&&&&NotificationReceiver is called." + intent);
	}
}
