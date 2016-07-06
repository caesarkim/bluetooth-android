/**
 * AgingPlaceMobile
 */
package com.ap.core.device;

/**
 * @author jungwhan
 * DataStatusOptions.java
 * 11:27:44 PM Apr 8, 2016 2016
 */
public class DataStatusOptions {
	/**
	 * It indicates if care receiver instance is dirty.
	 */
	public static boolean isCareReceiverListDirty = false;
	
	/**
	 * It indicates if a care receiver notification data is dirty.
	 */
	public static boolean isCareReceiverNotificationDirty = false;
	
	/**
	 * It indicates if a doctor info is dirty.
	 */
	public static boolean isDoctorInfoDirty = false;
	
	/**
	 * It indicates if a sensor data is dirty.
	 */
	public static boolean isSensorDataDirty = false;
}
