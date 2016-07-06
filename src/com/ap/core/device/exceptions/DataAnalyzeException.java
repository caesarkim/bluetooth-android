/**
 * AgingPlaceMobile
 */
package com.ap.core.device.exceptions;

/* Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */
public class DataAnalyzeException extends Exception {
	private static final long serialVersionUID = -5651334548500172044L;

	/**
	 * 
	 * @param message
	 */
	public DataAnalyzeException(String message) {
		super(message);
	}
	
	/**
	 * 
	 * @param t
	 */
	public DataAnalyzeException(Throwable t) {
		super(t);
	}
}
