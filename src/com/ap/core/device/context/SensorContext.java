/**
 * AgingPlaceMobile
 */
package com.ap.core.device.context;

import android.content.Context;

import com.ap.common.models.http.HttpCareReceiverConnectInfo;
import com.ap.common.models.http.HttpConnectTime;

/* Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */
public class SensorContext {
	private boolean isSensorsConnected;
	
	private boolean isInRetryWork = false;
	
	private Context uiContext;
	
	private HttpCareReceiverConnectInfo connectInfo = new HttpCareReceiverConnectInfo();
	
	public HttpCareReceiverConnectInfo getCareReceiverConnectionInformation() {
		return this.connectInfo;
	}
	
	/**
	 * 
	 * @param currentConnectTime
	 */
	public void addConnectionInformation(HttpConnectTime currentConnectTime) {
		connectInfo.addConnectTime(currentConnectTime);
	}

	/**
	 * @return the uiContext
	 */
	public Context getUiContext() {
		return uiContext;
	}

	/**
	 * @param uiContext the uiContext to set
	 */
	public void setUiContext(Context uiContext) {
		this.uiContext = uiContext;
	}

	/**
	 * @return the isSensorsConnected
	 */
	public boolean isSensorsConnected() {
		return isSensorsConnected;
	}

	/**
	 * @param isSensorsConnected the isSensorsConnected to set
	 */
	public void setSensorsConnected(boolean isSensorsConnected) {
		this.isSensorsConnected = isSensorsConnected;
	}

	/**
	 * @return the isInRetryWork
	 */
	public boolean isInRetryWork() {
		return isInRetryWork;
	}

	/**
	 * @param isInRetryWork the isInRetryWork to set
	 */
	public void setInRetryWork(boolean isInRetryWork) {
		this.isInRetryWork = isInRetryWork;
	}
}
