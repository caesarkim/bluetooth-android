/**
 * AgingPlaceMobile
 */
package com.ap.core.device.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.json.JSONObject;

import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

/* Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */
public class DefaultHttpJSONClient implements HttpJSONClient {
	protected static final String TAG = DefaultHttpJSONClient.class.getSimpleName();

	/*
	 * (non-Javadoc)
	 * @see
	 * com.ap.core.device.http.HttpJSONClient#asyncExecutePost(java.lang.String,
	 * java.lang.String, com.ap.core.device.http.HttpClientReceiver)
	 */
	@Override
	public void asyncExecutePost(String restPath, String jsonRequest, final HttpClientReceiver clientReceiver) throws JSONClientException {
		StringEntity stringEntity = null;
		try {
			stringEntity = new StringEntity(jsonRequest);
			stringEntity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		if (stringEntity == null) {
			throw new JSONClientException("");
		}
		AsyncHttpClient client = new AsyncHttpClient();
		client.post(null, restPath, stringEntity, "application/json", new JsonHttpResponseHandler() {
			@Override
			public void onFailure(Throwable t, JSONObject jsonObject) {
				t.printStackTrace();
			}

			@Override
			public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
				Log.i(TAG, "statusCode: " + statusCode + " == response:" + response);
				String message = null;
				try {
					Log.i(TAG, "response=" + response);
					if (response != null) {
						String responseMessageJSON = response.toString();
						Log.i(TAG, "response=" + response);
						if (clientReceiver != null) {
							clientReceiver.notifyInvoker(responseMessageJSON);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					Log.e(TAG, e.getMessage());
					if (clientReceiver != null) {
						clientReceiver.notifyInvoker(message);
					}
				}
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.ap.core.device.http.HttpJSONClient#asyncExecuteGet(java.lang.String,
	 * com.loopj.android.http.RequestParams,
	 * com.ap.core.device.http.HttpClientReceiver)
	 */
	@Override
	public void asyncExecuteGet(String restPath, RequestParams params, final HttpClientReceiver clientReceiver) throws JSONClientException {
		AsyncHttpClient client = new AsyncHttpClient();

		client.get(restPath, params, new JsonHttpResponseHandler() {
			@Override
			public void onFailure(Throwable t, JSONObject jsonObject) {
				t.printStackTrace();
			}

			@Override
			public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
				try {
					String responseMessageJSON = response.toString();
					if (clientReceiver != null) {
						clientReceiver.notifyInvoker(responseMessageJSON);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see com.ap.core.device.http.HttpJSONClient#executeGet(java.lang.String)
	 */
	public String executeGet(String getUrl) throws IOException {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(getUrl);
		HttpResponse httpResponse = httpClient.execute(httpGet);
		String inputLine;
		StringBuffer response = new StringBuffer();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
			while ((inputLine = reader.readLine()) != null) {
				response.append(inputLine);
			}
		} catch (Exception e) {
			throw new IOException(e);
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
		return response.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see com.ap.core.device.http.HttpJSONClient#executePost(java.lang.String,
	 * java.util.List)
	 */
	@Override
	public String executePost(String postUrl, String requestBody) throws IOException {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(postUrl);

		StringEntity input = new StringEntity(requestBody);
		input.setContentType("application/json");
		httpPost.setEntity(input);

		httpClient.addRequestInterceptor(new HttpRequestInterceptor() {
			public void process(HttpRequest request, HttpContext context) {
				// Add header to accept gzip content
				if (!request.containsHeader("Accept-Encoding")) {
					request.addHeader("Accept-Encoding", "gzip");
				}
			}
		});

		String inputLine;
		StringBuffer response = new StringBuffer();
		BufferedReader reader = null;
		try {
			HttpResponse httpResponse = httpClient.execute(httpPost);
			InputStream instream = httpResponse.getEntity().getContent();
			Header contentEncoding = httpResponse.getFirstHeader("Content-Encoding");
			if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
				instream = new GZIPInputStream(instream);
			}
			reader = new BufferedReader(new InputStreamReader(instream));
			while ((inputLine = reader.readLine()) != null) {
				response.append(inputLine);
			}
		} catch (Exception e) {
			throw new IOException(e);
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
		return response.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see com.ap.core.device.http.HttpJSONClient#executePost(java.lang.String,
	 * java.util.List)
	 */
	@Override
	public String executePut(String postUrl, String requestBody) throws IOException {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpPut httpPost = new HttpPut(postUrl);

		StringEntity input = new StringEntity(requestBody);
		input.setContentType("application/json");
		httpPost.setEntity(input);

		HttpResponse httpResponse = httpClient.execute(httpPost);

		String inputLine;
		StringBuffer response = new StringBuffer();
		Log.i(TAG, "POST Response Status:: " + httpResponse.getStatusLine().getStatusCode());

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
			while ((inputLine = reader.readLine()) != null) {
				response.append(inputLine);
			}
		} catch (Exception e) {
			throw new IOException(e);
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
		return response.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.ap.core.device.http.HttpJSONClient#executeDelete(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public String executeDelete(String postUrl, String requestBody) throws IOException {
		HttpResponse httpResponse = null;
		String inputLine;
		StringBuffer response = new StringBuffer();
		if (requestBody == null || requestBody.isEmpty()) {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpDelete httpDelete = new HttpDelete(postUrl);
			httpResponse = httpClient.execute(httpDelete);
			Log.i(TAG, "POST Response Status:: " + httpResponse.getStatusLine().getStatusCode());
		} else {
	        HttpEntity entity = new StringEntity(requestBody);
	        HttpClient httpClient = new DefaultHttpClient();
	        HttpDeleteWithBody httpDeleteWithBody = new HttpDeleteWithBody(postUrl);
	        httpDeleteWithBody.setHeader("Content-Type", "application/json");
	        httpDeleteWithBody.setEntity(entity);
	        httpResponse = httpClient.execute(httpDeleteWithBody);
		}
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
			while ((inputLine = reader.readLine()) != null) {
				response.append(inputLine);
			}
		} catch (Exception e) {
			throw new IOException(e);
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
		return response.toString();
	}
}
