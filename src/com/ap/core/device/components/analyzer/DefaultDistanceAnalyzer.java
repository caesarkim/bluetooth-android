/**
 * AgingSafeSeniorMobile
 */
package com.ap.core.device.components.analyzer;

import im.delight.android.location.SimpleLocation;
import android.content.Context;
import android.location.LocationManager;
import android.util.Log;

import com.ap.core.device.Constants;
import com.ap.core.device.context.UserPreference;
import com.ap.core.device.exceptions.DataAnalyzeException;

/**
 *  Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by jungwhan Projecteria LLC, May 13, 2016
 */

/**
 * @author jungwhan
 * DefaultDistanceAnalyzer.java
 * 2:31:44 PM May 13, 2016 2016
 */
public class DefaultDistanceAnalyzer  extends AbstractDistanceAnalyzer implements SensorDataAnalyzer {
	private static final String TAG = DefaultDistanceAnalyzer.class.getSimpleName();
	
	private Context context;
	
	private String distance;
	
	/**
	 * constructor
	 * @param sensorContext
	 */
	public DefaultDistanceAnalyzer(Context context) {
		this.context = context;
	}

	/* (non-Javadoc)
	 * @see com.ap.core.device.components.analyzer.SensorDataAnalyzer#analyze()
	 */
	@Override
	public void analyze() throws DataAnalyzeException {
		Log.i(TAG, "analyze() is called.");
		final LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			Log.i(TAG, "GPS is not enabled.");
			distance = Constants.DISTANCE_ERROR_PREFIX + " ({0})''s GPS is disabled";
			return;
		}
		SimpleLocation location = new SimpleLocation(context);
		final double latitude = location.getLatitude();
		final double longitude = location.getLongitude();
		Log.i(TAG, "$$$$$$$$ location is " + latitude + "==" + longitude);
		
		UserPreference currentUserPreference = UserPreference.getCurrentUserPreference(context);
		Log.i(TAG, "currentUserPreference="+ currentUserPreference);

		double originalUserLatitude = currentUserPreference.getLatitude();
		double originalUserLongitude = currentUserPreference.getLongitude();
		if (originalUserLatitude == 0.0 && originalUserLongitude == 0.0) {
//			distance = Constants.DISTANCE_ERROR_PREFIX + " Care receiver's GPS is disabled.";
			// Get it from the server
			distance = Constants.DISTANCE_ERROR_PREFIX + " {0}''s original location was not logged.";
			return;
		}
		double distanceResult = getDistance(originalUserLatitude, originalUserLongitude, latitude, longitude, 'M');
		if (distanceResult == 0.0 || distanceResult == Double.NaN || (latitude == originalUserLatitude && longitude == originalUserLongitude)) {
			distance = Constants.DISTANCE_SUCCESS_PREFIX + " near the sensors";	
		} else {
			distance = Constants.DISTANCE_SUCCESS_PREFIX + " "+ distanceResult + " miles away";
		}
	}

	/* (non-Javadoc)
	 * @see com.ap.core.device.components.analyzer.SensorDataAnalyzer#stopAnalyze()
	 */
	@Override
	public void stopAnalyze() {
		// TODO Auto-generated method stub

	}

	/**
	 * @return the distance
	 */
	public String getDistance() {
		return distance;
	}

}
