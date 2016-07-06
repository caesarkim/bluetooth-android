/**
 * AgingSafeSeniorMobile
 */
package com.ap.core.device.components.analyzer;

/**
 * Copyright (C) Projecteria LLC - All Rights Reserved Unauthorized copying of
 * this file, via any medium is strictly prohibited Proprietary and confidential
 * Written by jungwhan Projecteria LLC, May 13, 2016
 */

/**
 * @author jungwhan AbstractDistanceAnalyzer.java 2:35:37 PM May 13, 2016 2016
 */
public abstract class AbstractDistanceAnalyzer {

	private double deg2rad(double deg) {
		return (deg * Math.PI / 180.0);
	}

	private double rad2deg(double rad) {
		return (rad * 180.0 / Math.PI);
	}

	protected double getDistance(double lat1, double lon1, double lat2, double lon2, char unit) {
		double theta = lon1 - lon2;
		double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
		dist = Math.acos(dist);
		dist = rad2deg(dist);
		dist = dist * 60 * 1.1515;
		if (unit == 'K') {
			dist = dist * 1.609344;
		} else if (unit == 'N') {
			dist = dist * 0.8684;
		}
		return (dist);
	}
}
