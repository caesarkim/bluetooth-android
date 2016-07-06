/**
 * AgingPlaceMobile
 */
package com.ap.core.device.components.analyzer;

/* Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */


import com.ap.core.device.exceptions.DataAnalyzeException;


/**
 * @author jungwhan
 * SensorDataAnalyzer.java
 * 1:31:25 AM Feb 17, 2016 2016
 */
public interface SensorDataAnalyzer {
	/**
	 * 
	 * @throws DataAnalyzeException
	 */
	public void analyze() throws DataAnalyzeException;
	
	/**
	 * 
	 */
	public void stopAnalyze();
}
