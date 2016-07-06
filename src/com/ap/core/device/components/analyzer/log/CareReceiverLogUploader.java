/**
 * AgingPlaceMobile
 */
package com.ap.core.device.components.analyzer.log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import android.annotation.SuppressLint;
import android.os.Environment;
import android.util.Log;

import com.ap.common.models.http.HttpCareReceiverStat;
import com.ap.core.device.Constants;
import com.ap.core.device.CoreConfig;
import com.ap.core.device.context.LogUtils;
import com.ap.core.device.context.SensorContext;
import com.ap.core.device.context.UserPreference;
import com.ap.core.device.http.DefaultHttpJSONClient;
import com.google.gson.Gson;

/*
 * Copyright (C) Projecteria LLC - All Rights Reserved Unauthorized copying of
 * this file, via any medium is strictly prohibited Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */
@SuppressLint("UseSparseArrays")
public class CareReceiverLogUploader {
	protected static final String TAG = CareReceiverLogUploader.class.getSimpleName();

	private SensorContext context;

	private final static String ACTIVITY_LOG_PREFIX = "activity_log_";

	private Map<Integer, Integer> logs = new ConcurrentHashMap<Integer, Integer>();

	private Stack<File> files = new Stack<File>();

	private static String timestamp;

	private static String dateTimestamp;

	private static String APPLICATION_BASE_DIR;
	static {
		APPLICATION_BASE_DIR = Environment.getExternalStorageDirectory().getPath() + "/" + Constants.APPLICATION_NAME;
	}

	static {
		File file = new File(APPLICATION_BASE_DIR);
		if (!file.exists()) {
			file.mkdirs();
		}
	}

	public CareReceiverLogUploader(SensorContext context) {
		this.context = context;
		timestamp = "";
	}

	public void incrementLog() {
		Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		int currentDate = calendar.get(Calendar.DATE);
		int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
		String currentTimestamp = year + "_" + month + "_" + currentDate + "_" + currentHour;
		String currentTimeId = new StringBuffer().append(year).append(month).append(currentDate).toString();

		if (dateTimestamp != null && !dateTimestamp.isEmpty() && !currentTimeId.equals(dateTimestamp)) {
			// upload log files
			uploadLogFiles();
		}

		UserPreference userPreference = UserPreference.getCurrentUserPreference(context.getUiContext());
		if (userPreference == null) {
			return;
		}
		final String uuid = userPreference.getUuid();
		if ((timestamp != null && !timestamp.isEmpty()) && !currentTimestamp.equalsIgnoreCase(timestamp)) {
			// upload stat information to the server
			if (uuid == null) {
				Log.e(TAG, "care receiver uuid can't be null.");
			} else {
				if (!logs.isEmpty()) {
					final HttpCareReceiverStat stat = new HttpCareReceiverStat();
					stat.setTimestamp(timestamp);
					stat.setDateStamp(currentTimeId);
					stat.setUuid(uuid);
					stat.setTimeId(Integer.parseInt(currentTimeId));
					stat.setStat(new HashMap<Integer, Integer>(logs));
					Gson gson = new Gson();
					final String jsonRequest = gson.toJson(stat);
					new Thread() {
						@Override
						public void run() {
							synchronized (files) {
								uploadAndSave(jsonRequest);
								logs.clear();
							}
						}
					}.start();
				} else {
					Log.i(TAG, "logs is empty.");
				}
			}
		}

		dateTimestamp = currentTimeId;
		timestamp = currentTimestamp;

		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		if (!logs.containsKey(hour)) {
			logs.put(hour, 1);
		} else {
			Integer stat = logs.get(hour);
			if (stat != null) {
				stat++;
			} else {
				stat = 1;
			}
			Log.i(TAG, "hour==stat=" + hour + "==" + stat);
			logs.put(hour, stat);
		}
	}

	public void cleanUp() {
		Log.i(TAG, "cleanUp is called.");
		uploadLogFiles();
	}

	public void uploadLogFiles() {
		Log.i(TAG, "uploadLogFiles is called.");
		File applicationDir = new File(APPLICATION_BASE_DIR);
		final File[] logFiles = applicationDir.listFiles();
		new Thread() {
			@Override
			public void run() {
				synchronized (files) {
					boolean successful = false;
					for (File file : logFiles) {
						if (!file.exists()) {
							continue;
						}
						if (!file.getName().startsWith(ACTIVITY_LOG_PREFIX)) {
							continue;
						}
						String content = LogUtils.readFile(file.getAbsolutePath());
						try {
							uploadLog(content);
							if (file.exists()) {
								file.delete();
							}
							successful = true;
						} catch (Exception e) {
							Log.e(TAG, "error=" + e.getMessage());
							successful = false;
						}
					}
					if (successful) {
						files.clear();
					}
				}
			}
		}.start();
	}

	private void uploadAndSave(String newJsonLog) {
		Log.i(TAG, "uploadAndSave is called.");
		try {
			uploadLog(newJsonLog);
		} catch (IOException e) {
			e.printStackTrace();
			saveLog(newJsonLog, ACTIVITY_LOG_PREFIX + System.currentTimeMillis() + ".txt");
		}

		File applicationDir = new File(APPLICATION_BASE_DIR);
		final File[] logFiles = applicationDir.listFiles();
		if (logFiles != null && logFiles.length > 0) {
			for (File logFile : logFiles) {
				if (!logFile.exists()) {
					continue;
				}
				if (!logFile.getName().startsWith(ACTIVITY_LOG_PREFIX)) {
					continue;
				}
				String logContent = LogUtils.readFile(logFile.getAbsolutePath());
				try {
					uploadLog(logContent);
					if (logFile.exists()) {
						logFile.delete();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void saveLog(String content, String fileName) {
		File file = new File(APPLICATION_BASE_DIR, fileName);
		Log.i(TAG, "file=" + file.getAbsolutePath());
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			LogUtils.saveLog(content, fos);
		} catch (FileNotFoundException fe) {
			fe.printStackTrace();
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException ioe) {
					Log.i(TAG, "error=" + ioe.getMessage());
				}
			}
		}
		synchronized (files) {
			files.add(file);
		}
	}

	private void uploadLog(final String jsonRequest) throws IOException {
		final String url = CoreConfig.getUrl("/user/carereceiver/stat");
		Log.i(TAG, "jsonRequest=" + jsonRequest);
		final DefaultHttpJSONClient httpClient = new DefaultHttpJSONClient();
		try {
			httpClient.executePost(url, jsonRequest);
		} catch (IOException e) {
			throw e;
		}
	}
}
