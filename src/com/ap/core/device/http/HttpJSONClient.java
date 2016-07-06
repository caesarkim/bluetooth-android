/**
 * AgingPlaceMobile
 */
package com.ap.core.device.http;

import java.io.IOException;

import com.loopj.android.http.RequestParams;

/* Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */
public interface HttpJSONClient {
	/**
	 * 
	 * @param restPath
	 * @param jsonRequest
	 * @param clientReceiver
	 * @throws JSONClientException
	 */
	public void asyncExecutePost(String restPath, String jsonRequest, HttpClientReceiver clientReceiver) throws JSONClientException;
	
	/**
	 * 
	 * @param restPath
	 * @param params
	 * @param clientReceiver
	 * @throws JSONClientException
	 */
	public void asyncExecuteGet(String restPath, RequestParams params, HttpClientReceiver clientReceiver) throws JSONClientException;
	
	/**
	 * 
	 * @param GET_URL
	 * @return
	 * @throws IOException
	 */
	public String executeGet(String getUrl) throws IOException;
	
	/**
	 * 
	 * @param POST_URL
	 * @param requestBody
	 * @return
	 * @throws IOException
	 */
	public String executePost(String postUrl, String requestBody) throws IOException;
	
	/**
	 * 
	 * @param postUrl
	 * @param requestBody
	 * @return
	 * @throws IOException
	 */
	public String executePut(String postUrl, String requestBody) throws IOException;
	
	/**
	 * 
	 * @param postUrl
	 * @param requestBody
	 * @return
	 * @throws IOException
	 */
	public String executeDelete(String postUrl, String requestBody) throws IOException;
}
