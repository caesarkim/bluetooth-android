/**
 * AgingPlaceMobile
 */
package com.ap.core.device.components.analyzer;

/* Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import android.util.Log;

import com.ap.common.models.http.HttpBatteryNotification;
import com.ap.common.models.http.HttpGeneric;
import com.ap.common.models.http.HttpNotification;
import com.ap.common.models.http.NotificationType;
import com.ap.core.device.CoreConfig;
import com.ap.core.device.PresenceStatus;
import com.ap.core.device.SensorData;
import com.ap.core.device.UserStatus;
import com.ap.core.device.components.PacketReceiver;
import com.ap.core.device.components.analyzer.log.CareReceiverLogUploader;
import com.ap.core.device.context.SensorContext;
import com.ap.core.device.context.UserPreference;
import com.ap.core.device.exceptions.DataAnalyzeException;
import com.ap.core.device.http.DefaultHttpJSONClient;
import com.ap.core.device.thread.Worker;
import com.ap.core.device.utils.MessageUtils;
import com.ap.mobile.model.SensorType;
import com.ap.mobile.util.Utils;
import com.google.gson.Gson;

/**
 * @author jungwhan DefaultSensorDataAnalyzer.java 1:37:30 AM Feb 17, 2016 2016
 */
public class DefaultSensorDataAnalyzer extends Worker implements SensorDataAnalyzer, PacketReceiver {
	protected static final String TAG = DefaultSensorDataAnalyzer.class.getSimpleName();

	/**
	 * sensorDataCollections which contains sensor data
	 */
	private List<SensorData> sensorDataCollection = new ArrayList<SensorData>();
	
	private SensorContext context;

	/**
	 * User status instance variable
	 */
	private UserStatus userStatus = new UserStatus();
	
	/**
	 * Flag indicating if this analyzer is running or not.
	 */
	private boolean isAnalyzerRunning = true;

	/**
	 * Variable representing how often this analyzer should sleep.
	 */
	private long analyzeInterval = 1000 * 60 * 1;
	
	private final static long defaultAnalyzeInterval = 1000 * 60 * 1;
	
	private long notificationSentInterval = 1000 * 60 * 10;
	
	/**
	 * Interval for check if there is any motion.
	 */
	private long awayDetectInterval = 1000 * 60 * 2;
	
	private final static int EXACT_10_MINUTES = 1000 * 60 * 10;
	
	/**
	 * Variable indicating if there is any inactivity.
	 */
	private long inactivityNotificationTimeUp = 1000 * 60 * 9;
	
	private final static long inactivityMinimum = 1000 * 60 * 10;
	
	private final static long inactivityMaximum = 1000 * 60 * 60 * 6;
	
	private int bedFrom = 23;
	
	private int bedTo = 5;
	
	private long recentInactivityCreated = 0;
	
	private long lastReceivedEventTimestamp = 0;
	
	private int intervalType;
	
	private ReentrantLock lock = new ReentrantLock();
	
	private final Object sleepLock = new Object();
	
	private final Object controlLock = new Object();
	
	private UserAwayUpdater userStatusUpdater;
	
	private CareReceiverLogUploader careReceiverLogUploader;
	
	private long doorSensorDetected = 0;
	
	private static boolean isNotificationSent = false;
	
	private Map<String, String> batteryLowTrack = new HashMap<String, String>();
	
	private long batteryLowSentTime;
	
	/**
	 * Constructor
	 */
	public DefaultSensorDataAnalyzer(SensorContext context) {
		super();
		this.context = context;
		careReceiverLogUploader = new CareReceiverLogUploader(context);
	}
	
	public void initialize() {
		Log.i(TAG, "initialize() is called.");
		synchronized(this) {
			userStatus = new UserStatus();
			if (userStatusUpdater != null) {
				try {
					userStatusUpdater.interrupt();
				} catch (Exception e) {
					//
				}
			}
			intervalType = 0;
			userStatusUpdater = new UserAwayUpdater();
			userStatusUpdater.start();
			isAnalyzerRunning = true;
			lastReceivedEventTimestamp = 0;
			inactivityNotificationTimeUp = 1000 * 60 * 10;
			careReceiverLogUploader = new CareReceiverLogUploader(context);
		}
		synchronized (sensorDataCollection) {
			sensorDataCollection.clear();
		}
	}
	
	
	private void log(final String tag, final String message) {
		Log.i(tag, message);
		new Thread() {
			@Override
			public void run() {
				HttpGeneric logRequest = new HttpGeneric();
				logRequest.setData(message);
				String url = CoreConfig.getUrl("/sensor/save");
				Gson gson = new Gson();
				String jsonRequest = gson.toJson(logRequest);
				DefaultHttpJSONClient httpClient = new DefaultHttpJSONClient();
//				try {
//					httpClient.executePost(url, jsonRequest);
//				} catch (IOException e) {
////					log(TAG, e.getMessage());
//				}
			}
		}.start();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.ap.core.device.components.PacketReceiver#receive(java.lang.String)
	 */
	@Override
	public void receive(String message) {
		SensorData sensorData = MessageUtils.toSensorData(message);
		if (sensorData == null) {
			return;
		}
		synchronized (sensorDataCollection) {
			sensorDataCollection.add(sensorData);

			String batteryLevelValue = sensorData.getBatteryLevel();
			if (batteryLevelValue != null) {
				batteryLevelValue = batteryLevelValue.trim();
				if (batteryLevelValue != null && !batteryLevelValue.isEmpty() && Integer.parseInt(batteryLevelValue) < 20) {
					Log.i(TAG, new java.util.Date() + "=battery level is " + batteryLevelValue + ". Please send a notification.");
					// Send a low-battery notification
					if (!batteryLowTrack.containsKey(sensorData.getSensorName())) {
						fireLowBatteryNotification(sensorData.getSensorType(), sensorData.getSensorName(), batteryLevelValue);
						batteryLowSentTime = System.currentTimeMillis();
					}
					batteryLowTrack.put(sensorData.getSensorName(), "SENT");
					long difference = System.currentTimeMillis() - batteryLowSentTime;
					if (difference > 0 && (difference < (1000 * 60 * 60 * 24))) {
						batteryLowTrack.clear();
					}
				}
			}
			if (sensorData.getSensorType().equalsIgnoreCase(SensorType.TEMPERATURE_SENSOR.name())) {
				Log.i(TAG, "Temperature sensor data is received. "+ sensorData);
				return;
			}
			inactivityNotificationTimeUp = inactivityMinimum;
			if (sensorData.getSensorType().equalsIgnoreCase(SensorType.MOTION_SENSOR.name())) {
//				Log.i(TAG, "MOTION_SENSOR.userStatus.isInactivityNotificationSent()="+ isNotificationSent);
				intervalType = 0;
				if (isNotificationSent) {
					// send a notification clear message
					try {
						fireInactivityNotificationClear();
						log(TAG, "fireInactivityNotificationClear sent a message.");
					} catch (IOException e) {
						log(TAG, "fireInactivityNotificationClear.error="+ e.getMessage());
					}
				}

				long difference = System.currentTimeMillis() - doorSensorDetected;
				if (difference < (1000 * 10)) {
					return;
				}
				isNotificationSent = false;
				careReceiverLogUploader.incrementLog();
//				Log.i(TAG, "motionSensor received=" + sensorData + " at "+ new java.util.Date());
				userStatus.setStatusChangeTime(System.currentTimeMillis());
				userStatus.setInactivityNotificationSent(false);
				lock.lock();
				try {
					if (userStatus.getUserStatus() != null && userStatus.getUserStatus().name().equalsIgnoreCase(PresenceStatus.AWAY.name())) {
						userStatus.setUserStatus(PresenceStatus.ACTIVE, PresenceStatus.BACK);
					} else {
						userStatus.setUserStatus(PresenceStatus.ACTIVE);
					}
					sensorDataCollection.clear();
					if (!userStatusUpdater.isSleeping()) {
						synchronized(controlLock) {
							userStatusUpdater.setPutToSleep(true);
							controlLock.notifyAll();
						}
					}
				} finally {
					lock.unlock();
				}
				lastReceivedEventTimestamp = System.currentTimeMillis();
				notificationSentInterval = EXACT_10_MINUTES;
				return;
			}
			if (sensorData.getSensorType().equalsIgnoreCase(SensorType.DOOR_SENSOR.name())) {
				intervalType = 0;
				Log.i(TAG, " = Door sensor is detected."+ sensorData + " at "+ new java.util.Date());
				userStatus.setStatusChangeTime(System.currentTimeMillis());
				lock.lock();
				sensorDataCollection.clear();
				try {
					synchronized(controlLock) {
						userStatusUpdater.setPutToSleep(false);
						controlLock.notifyAll();
					}
					synchronized(sleepLock) {
						sleepLock.notifyAll();
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					lock.unlock();
				}
				doorSensorDetected = System.currentTimeMillis();
				lastReceivedEventTimestamp = System.currentTimeMillis();
			}
		}
	}
	
	public void stopAnalyze() {
		isAnalyzerRunning = false;
		Log.i(TAG, "stopAnalize() is called.");
		careReceiverLogUploader.cleanUp();
		if (userStatusUpdater != null && userStatusUpdater.isRunning() && userStatusUpdater.isAlive()) {
			try {
				synchronized (sleepLock) {
					sleepLock.notifyAll();
				}
				synchronized (controlLock) {
					controlLock.notifyAll();
				}
				sensorDataCollection.clear();
				userStatusUpdater.setRunning(false);
				userStatusUpdater.setPutToSleep(false);
				userStatusUpdater.interrupt();
			} catch (Exception e) {
				//
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.ap.core.device.components.analyzer.SensorDataAnalyzer#analyze()
	 */
	@Override
	public void analyze() throws DataAnalyzeException {
		boolean wasUserInBed = false;
		while (isAnalyzerRunning) {
			try {
				Thread.sleep(analyzeInterval);
			} catch (InterruptedException e) {
				e.printStackTrace();
				break;
			}
			if (userStatus.getUserStatus() == null) {
				Log.i(TAG, "user status is null.");
				continue;
			}
			if (userStatus.isInactivityNotificationSent()) {
				log(TAG, "Notification has been already sent.");
				continue;
			}
			// Need to comment out
			long currentMilliseconds = System.currentTimeMillis();
			Date currentTime = new Date(currentMilliseconds);
			Calendar calendar = GregorianCalendar.getInstance();
			calendar.setTime(currentTime);
			int hour = calendar.get(Calendar.HOUR_OF_DAY);
			if (hour < bedTo || hour > bedFrom) {
				wasUserInBed = true;
				log(TAG, "a care receiver is sleeping... "+ new java.util.Date());
				// It will slow down the interval.
				analyzeInterval = (1000 * 60 * 60 * 1);
				log(TAG, "analayzeInterval="+ analyzeInterval);
				continue;
			}
			if (wasUserInBed) {
				log(TAG, "Getting out of a bed and going back to normal "+ new java.util.Date());
				userStatus.setStatusChangeTime(System.currentTimeMillis());
				analyzeInterval = defaultAnalyzeInterval;
				wasUserInBed = false;
				continue;
			}
			// after the bed time, it will go back to default.
			analyzeInterval = defaultAnalyzeInterval;
			synchronized (sensorDataCollection) {
//				If this is used, it can't analyze the data.
				if (PresenceStatus.AWAY.name().equalsIgnoreCase(userStatus.getUserStatus().name())) {
					Log.i(TAG, "A care receiver is "+ userStatus.getUserStatus().name() + " at "+ new java.util.Date());
					continue;
				}
				long difference = System.currentTimeMillis() - (userStatus.getStatusChangeTime());
				log(TAG, "difference=userStatusChangeTime is "+ difference + "=="+ userStatus.getStatusChangeTime());
				log(TAG, "inactivityNotificationTimeUp="+ inactivityNotificationTimeUp);
				if (difference >= inactivityNotificationTimeUp && difference <= inactivityMaximum) {
					// Send an inactivity notification
					recentInactivityCreated = userStatus.getStatusChangeTime();
					log(TAG, "Time is up. "+ recentInactivityCreated + "=="+ new java.util.Date(recentInactivityCreated));
					try {
						userStatus.setUserStatus(PresenceStatus.INACTIVE);
						log(TAG, "intervalType is "+ intervalType);
						intervalType++;
						if (intervalType == 1) {
							long remainingDifference = (System.currentTimeMillis() - recentInactivityCreated);
							log(TAG, "1. remainingDifference="+ remainingDifference + "=="+  Utils.getFriendlyTime(recentInactivityCreated));
							if (EXACT_10_MINUTES > remainingDifference) {
								log(TAG, "not 10 minutes yet.");
								continue;
							}
						}
						try {
							// Check if user's status has changed while it was in sleep mode.
							if (PresenceStatus.ACTIVE.name().equalsIgnoreCase(userStatus.getUserStatus().name()) || PresenceStatus.BACK.name().equalsIgnoreCase(userStatus.getUserStatus().name())) {
								log(TAG, "2. user is "+ userStatus.getUserStatus().name());
								continue;
							}
							fireInactivityNotification(intervalType);
							isNotificationSent = true;
						} catch (Exception e) {
							e.printStackTrace();
						}
						if (intervalType < 4) {
							notificationSentInterval = 1000 * 60 * 10;
							inactivityNotificationTimeUp = notificationSentInterval + inactivityNotificationTimeUp;
						}
						if (intervalType == 4) {
							inactivityNotificationTimeUp = (1000 * 60 * 20) + inactivityNotificationTimeUp;
						} else if (intervalType >= 5) {
							inactivityNotificationTimeUp = (1000 * 60 * 60 * 1) + inactivityNotificationTimeUp;
						}
						log(TAG, "intervalType=notificationSentInterval="+ intervalType + "=="+ inactivityNotificationTimeUp);
					} catch (Exception e) {
						log(TAG, "error=sending an inactivity notification.");
					}
				}
				log(TAG, "it exceeded the number of notification delivery="+ intervalType);
				if (difference >= inactivityMaximum) {
					log(TAG, "Setting true to setInactivityNotificationSent");
					userStatus.setInactivityNotificationSent(true);
					userStatus.setStatusChangeTime(System.currentTimeMillis());
				}
			}
		}
		Log.i(TAG, "analyze is out of the loop.");
		synchronized (sleepLock) {
			userStatusUpdater.setRunning(false);
			userStatusUpdater.setPutToSleep(false);
			sleepLock.notifyAll();
		}
		synchronized (controlLock) {
			controlLock.notifyAll();
		}
		Log.i(TAG, "analyze is finished.");
	}
	
	/**
	 * 
	 * @throws IOException
	 */
	private void fireInactivityNotificationClear() throws IOException {
		log(TAG, "fireInactivityNotificationClear is called.");
		UserPreference currentUserPreference = UserPreference.getCurrentUserPreference(context.getUiContext());
		if (currentUserPreference == null) {
			return;
		}
		HttpNotification notification = new HttpNotification();
		notification.setCreated(recentInactivityCreated);
		notification.setMessageType(NotificationType.NOTIFICATION_CLEAR.getTypeId());
		notification.setIntervalType(NotificationType.BATTERY_LOW.getDefaultInterval());
		notification.setMessageType(1);
		notification.setCareReceiverUUID(currentUserPreference.getUuid());
		final String url = CoreConfig.getUrl("/user/caregiver/clear_notification");
		Gson gson = new Gson();
		String request = gson.toJson(notification);
		log(TAG, "fireInactivityNotificationClear.inactivity notification="+ request);
		final DefaultHttpJSONClient httpClient = new DefaultHttpJSONClient();
		try {
			httpClient.executePost(url, request);
		} catch (IOException e) {
			log(TAG, e.getMessage());
		}
	}
	
	private void fireInactivityNotification(int intervalType) throws IOException {
		log(TAG, "sendInactivityNotification is called.");
		if (!context.isSensorsConnected()) {
			log(TAG, "sensor is not connected.");
			return;
		}
		UserPreference currentUserPreference = UserPreference.getCurrentUserPreference(context.getUiContext());
		HttpNotification notification = new HttpNotification();
		notification.setCreated(userStatus.getStatusChangeTime());
		notification.setMessageType(NotificationType.NOTIFICATION_DISPLAY.getTypeId());
		notification.setIntervalType(intervalType);
		notification.setCareReceiverUUID(currentUserPreference.getUuid());
		final String url = CoreConfig.getUrl("/user/carereceiver/create_notification");
		Gson gson = new Gson();
		String request = gson.toJson(notification);
		log(TAG, "inactivity notification="+ request);
		final DefaultHttpJSONClient httpClient = new DefaultHttpJSONClient();
		try {
			httpClient.executePost(url, request);
		} catch (IOException e) {
			throw e;
		}
	}
	
	private void fireLowBatteryNotification(String sensorType, String sensorName, String batteryLevel) {
		log(TAG, "fireLowBatteryNotification is called.");
		if (!context.isSensorsConnected()) {
			log(TAG, "sensor is not connected.");
			return;
		}
		UserPreference currentUserPreference = UserPreference.getCurrentUserPreference(context.getUiContext());
		HttpBatteryNotification batteryNotification = new HttpBatteryNotification();
		batteryNotification.setCreated(System.currentTimeMillis());
		batteryNotification.setMessageType(NotificationType.BATTERY_LOW.getTypeId());
		batteryNotification.setIntervalType(NotificationType.BATTERY_LOW.getDefaultInterval());
		batteryNotification.setCareReceiverUUID(currentUserPreference.getUuid());
		batteryNotification.setBatteryLevel(batteryLevel);
		batteryNotification.setSensorName(sensorName);
		batteryNotification.setSensorType(sensorType);
		final String url = CoreConfig.getUrl("/user/carereceiver/battery_low");
		Gson gson = new Gson();
		String request = gson.toJson(batteryNotification);
		log(TAG, "battery notification="+ request);
		final DefaultHttpJSONClient httpClient = new DefaultHttpJSONClient();
		try {
			httpClient.executePost(url, request);
		} catch (Exception e) {
			e.printStackTrace();
			Log.i(TAG, "error="+ e.getMessage());
		}
	}

	@Override
	public void run() {
		try {
			analyze();
		} catch (DataAnalyzeException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	class UserAwayUpdater extends Thread {
		private boolean isRunning = true;
		
		private boolean putToSleep = true;

		@Override
		public void run() {
			while (isRunning) {
				synchronized (sleepLock) {
					try {
//						putToSleep = true;
						Log.i(TAG, "sleeping now" + " at "+ new java.util.Date());
						sleepLock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				log(TAG, "sleepLock woke up." + " at "+ new java.util.Date());
				synchronized (controlLock) {
					try {
//						putToSleep = false;
						Log.i(TAG, "controlLock is waiting " + " at "+ new java.util.Date());
						controlLock.wait(awayDetectInterval);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				Log.i(TAG, "controlLock woke up " + " at "+ new java.util.Date());
				Log.i(TAG, "putToSleep="+ putToSleep + " at "+ new java.util.Date());
				if (putToSleep) {
					continue;
				}
				if (!isRunning) {
					break;
				}
				
				synchronized (sensorDataCollection) {
					if (sensorDataCollection.isEmpty()) {
						log(TAG, "Care recipient is set to AWAY at "+ new java.util.Date());
						userStatus.setUserStatus(PresenceStatus.AWAY);
						userStatus.setStatusChangeTime(System.currentTimeMillis());
					}
				}
			}
			Log.i(TAG, "UserAwayUpdater is out of the loop.");
		}

		/**
		 * @return the isRunning
		 */
		public boolean isRunning() {
			return isRunning;
		}

		/**
		 * @param isRunning the isRunning to set
		 */
		public void setRunning(boolean isRunning) {
			this.isRunning = isRunning;
		}

		/**
		 * @return the putTosleep
		 */
		public boolean isSleeping() {
			return putToSleep;
		}

		/**
		 * @param putTosleep the putTosleep to set
		 */
		public void setPutToSleep(boolean putToSleep) {
			this.putToSleep = putToSleep;
		}
	}

	/**
	 * @return the isAnalyzerRunning
	 */
	public boolean isAnalyzerRunning() {
		return isAnalyzerRunning;
	}

	/**
	 * @param isAnalyzerRunning the isAnalyzerRunning to set
	 */
	public void setAnalyzerRunning(boolean isAnalyzerRunning) {
		this.isAnalyzerRunning = isAnalyzerRunning;
	}

	/**
	 * @return the analyzeInterval
	 */
	public long getAnalyzeInterval() {
		return analyzeInterval;
	}

	/**
	 * @param analyzeInterval the analyzeInterval to set
	 */
	public void setAnalyzeInterval(long analyzeInterval) {
		this.analyzeInterval = analyzeInterval;
	}

	/**
	 * @return the awayDetectInterval
	 */
	public long getAwayDetectInterval() {
		return awayDetectInterval;
	}

	/**
	 * @param awayDetectInterval the awayDetectInterval to set
	 */
	public void setAwayDetectInterval(long awayDetectInterval) {
		this.awayDetectInterval = awayDetectInterval;
	}

	/**
	 * @return the inactivityCutOff
	 */
	public long getInactivityNotificationTimeUp() {
		return inactivityNotificationTimeUp;
	}

	/**
	 * @param inactivityCutOff the inactivityCutOff to set
	 */
	public void setInactivityNotificationTimeUp(long inactivityCutOff) {
		this.inactivityNotificationTimeUp = inactivityCutOff;
	}

	/**
	 * @return the userStatus
	 */
	public UserStatus getUserStatus() {
		return userStatus;
	}

	/**
	 * @return the lastReceivedEventTimestamp
	 */
	public long getLastReceivedEventTimestamp() {
		return lastReceivedEventTimestamp;
	}

	/**
	 * @param context the context to set
	 */
	public void setContext(SensorContext context) {
		this.context = context;
	}

	/**
	 * @return the notificationSentInterval
	 */
	public long getNotificationSentInterval() {
		return notificationSentInterval;
	}

	/**
	 * @param notificationSentInterval the notificationSentInterval to set
	 */
	public void setNotificationSentInterval(long notificationSentInterval) {
		this.notificationSentInterval = notificationSentInterval;
	}

	/**
	 * @return the bedFrom
	 */
	public int getBedFrom() {
		return bedFrom;
	}

	/**
	 * @param bedFrom the bedFrom to set
	 */
	public void setBedFrom(int bedFrom) {
		this.bedFrom = bedFrom;
	}

	/**
	 * @return the bedTo
	 */
	public int getBedTo() {
		return bedTo;
	}

	/**
	 * @param bedTo the bedTo to set
	 */
	public void setBedTo(int bedTo) {
		this.bedTo = bedTo;
	}

	/**
	 * @return the intervalType
	 */
	public int getIntervalType() {
		return intervalType;
	}

	/**
	 * @param intervalType the intervalType to set
	 */
	public void setIntervalType(int intervalType) {
		this.intervalType = intervalType;
	}

}
