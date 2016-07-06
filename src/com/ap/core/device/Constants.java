/**
 * AgingPlaceMobile
 */
package com.ap.core.device;

/**
 * @author jungwhan
 * Constants.java
 * 11:00:19 PM Feb 6, 2016 2016
 */
public class Constants {
	public final static int MAX_TIMEOUT = 1000 * 3;
	
	public final static String DEVICE_PREFIX = "ada";
	
	public final static String PLATFORM = "Android";
	
	public final static String APPLICATION_NAME = "SafeSenior";
	
	public final static String PREFERENCE_SHARED_KEY = "com.ap.safesenior.mobile";
	
	public final static String DISTANCE_ERROR_PREFIX = "ERROR:";
	
	public final static String DISTANCE_SUCCESS_PREFIX = "DISTANCE:";
	
	public final static String MESSAGE_DELIMITER = "|";
	
	public final static String PREFERENCE_KEY_USER_PREFERENCE = "USER_PREFERENCE";
	
	public final static String PREFERENCE_KEY_SELECTED_CARE_RECEIVER = "SELECTED_CARE_RECEIVER";
	
	public final static String PREFERENCE_KEY_ALL_SENSORS = "ALL_SENSORS";
	
	public final static String PREFERENCE_KEY_NOTIFICATION_OPTION = "NOTIFICATION_OPTION";

	public final static String USER_REGISTRATION_ID = "USER_REGISTRATION_ID";
	
	public final static String MESSAGE_QUEUE = "messageQueue";
	
	public final static String MESSAGE_SERVICE_QUEUE = "messageServiceQueue";
	
	public final static String MESSAGE_COMMAND_QUEUE = "messageCommandQueue";
	
	public final static String MESSAGE_REGISTRATION_QUEUE = "messageRegistrationQueue";
	
	public final static String MESSAGE_QUEUE_KEY = "messageQueueKey";
	
	public final static String MESSAGE_REGISTRATION_QUEUE_KEY = "messageRegistrationQueueKey";
	
	public final static String MESSAGE_COMMAND = "COMMAND";
	
	public final static String ERROR_BUNDLE = "ERROR_BUNDLE";
	
	public final static String MESSAGE_PROGRESS_STATUS = "PROGRESS_STATUS";
	
	public final static String MESSAGE_PROGRESS_FINISH = "FINISH";
	
	public final static String MESSAGE_PROGRESS_ONGOING = "ONGOING";
	
	public final static String MESSAGE_PROGRESS_ERROR = "ERROR";
	
	public final static String MESSAGE_SENSOR_DISCONNECTED = "MESSAGE_SENSOR_DISCONNECTED";
	
	public final static String senderID = "193315944318";
	
	public final static String HANDLER_MESSAGE_ERROR = "ERROR";
	
	public final static String HANDLER_MESSAGE_KEY = "MESSAGE";
	
	public final static String CARE_GIVER_KEY = "CARE_GIVER_KEY";
	
	public final static String BUNDLE_CARE_RECEIVER_KEY = "CARE_RECEIVER_KEY";
	
	public final static String BUNDLE_DOCTOR_INFO = "DOCTOR_INFO";
	
	public final static String BUNDLE_TRACK_PAGE = "trackPage";
	
	public final static String BUNDLE_TRACK_PAGE_VALUE_FROM_MAIN = "fromMain";
	
	
	// Defined in the AndroidManifest.xml
	public final static String BROADCAST_RECEIVER_SENSOR_RESET = "com.ap.mobile.services.receivers.SensorResetRequestBroadcastReceiver";
}
