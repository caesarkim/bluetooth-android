/**
 * AgingPlaceMobile
 */
package com.ap.core.device.components.analyzer;

/* Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */
 

import im.delight.android.location.SimpleLocation;
import android.content.Context;
import android.location.LocationManager;
import android.util.Log;

import com.ap.core.device.context.SensorContext;
import com.ap.core.device.context.UserPreference;
import com.ap.core.device.exceptions.DataAnalyzeException;

/**
 * @author jungwhan DefaultRetryConnectionDistanceAnalyzer.java 4:26:05 PM Feb 27, 2016 2016
 */
public class DefaultRetryConnectionDistanceAnalyzer extends AbstractDistanceAnalyzer implements SensorDataAnalyzer {
	protected static final String TAG = DefaultRetryConnectionDistanceAnalyzer.class.getSimpleName();

	protected SensorContext context;
	
	private long interval = 1000 * 60 * 5;
	
	/**
	 * Constructor
	 */
	public DefaultRetryConnectionDistanceAnalyzer(SensorContext context) {
		super();
		this.context = context;
	}
	
	private String getTime(long time) {
		String msg = "";
		if (time > (1000 * 60 * 60)) {
			msg += (time /(1000 * 60 * 60)) + " hour(s)";
		}
		long remaining = time - (time /(1000 * 60 * 60));
		if (remaining > (1000 * 60 * 1)) {
			msg += (remaining /(1000 * 60 * 1)) + " minute(s)";
		}
		remaining = remaining - (remaining /(1000 * 60 * 1));
		if (remaining > 0) {
			msg += remaining + " second(s)";
		}
		return msg;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ap.core.device.components.analyzer.SensorDataAnalyzer#analyze()
	 */
	@Override
	public void analyze() throws DataAnalyzeException {
		UserPreference currentUserPreference = UserPreference.getCurrentUserPreference(context.getUiContext());
		Log.i(TAG, "currentUserPreference="+ currentUserPreference);
		final LocationManager manager = (LocationManager) context.getUiContext().getSystemService(Context.LOCATION_SERVICE);
		if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			Log.i(TAG, "GPS is not enabled.");
			return;
		}
		double originalUserLatitude = currentUserPreference.getLatitude();
		double originalUserLongitude = currentUserPreference.getLongitude();

		SimpleLocation location = new SimpleLocation(context.getUiContext());
		final double latitude = location.getLatitude();
		final double longitude = location.getLongitude();
		Log.i(TAG, "$$$$$$$$ location is " + latitude + "==" + longitude);

		double distance = getDistance(originalUserLatitude, originalUserLongitude, latitude, longitude, 'M');
		Log.i(TAG, "distance is "+ distance);
		if (distance <= 1) {
			interval = 1000 * 60 * 10;
		} else {
			interval = (long)(distance * (1000 * 60 * 2));
		}
		interval = 1000 * 60 * 10;
		Log.i(TAG, "interval="+ getTime(interval));
		Log.i(TAG, "calculateIntervalThread ended.");
	}

	/**
	 * @return the interval
	 */
	public long getInterval() {
		return interval;
	}

	/**
	 * @param interval the interval to set
	 */
	public void setInterval(long interval) {
		this.interval = interval;
	}

	/* (non-Javadoc)
	 * @see com.ap.core.device.components.analyzer.SensorDataAnalyzer#stopAnalyze()
	 */
	@Override
	public void stopAnalyze() {
		// TODO Auto-generated method stub
		
	}
}
