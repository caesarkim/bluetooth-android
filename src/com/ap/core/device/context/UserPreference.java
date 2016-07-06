/**
 * AgingPlaceMobile
 */
package com.ap.core.device.context;

import android.content.Context;

import com.ap.core.device.Constants;
import com.ap.mobile.util.Utils;
import com.google.gson.Gson;

/* Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */
public class UserPreference {
	private boolean isActivated;
	
	private String pinNumber;
	
	private String phoneNumber;
	
	private String emailAddress;
	
	private String uuid;
	
	private String careType;
	
	private double latitude;
	
	private double longitude;
	
	private String registrationId;
	
	@Override
	public String toString() {
		StringBuffer b = new StringBuffer();
		b.append("isActivated=").append(isActivated).append(", ");
		b.append("emailAddress=").append(emailAddress).append(", ");
		b.append("pinNumber=").append(pinNumber).append(", ");
		b.append("phoneNumber=").append(phoneNumber).append(", ");
		b.append("careType=").append(careType).append(", ");
		b.append("latitude=").append(latitude).append(", ");
		b.append("longitude=").append(longitude).append(", ");
		b.append("registrationId=").append(registrationId).append(", ");
		return b.toString();
	}
	
	public UserPreference() {
		super();
	}
	
	public String toJson() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}
	
	public static UserPreference getCurrentUserPreference(Context context) {
		String currentJson = Utils.getPreferenceValue(context, Constants.PREFERENCE_KEY_USER_PREFERENCE);
		if (currentJson == null || currentJson.isEmpty()) {
			return null;
		}
		Gson gson = new Gson();
		return gson.fromJson(currentJson, UserPreference.class);
	}
	
	public String getJson() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}

	/**
	 * @return the pinNumber
	 */
	public String getPinNumber() {
		return pinNumber;
	}

	/**
	 * @param pinNumber the pinNumber to set
	 */
	public void setPinNumber(String pinNumber) {
		this.pinNumber = pinNumber;
	}

	/**
	 * @return the phoneNumber
	 */
	public String getPhoneNumber() {
		return phoneNumber;
	}

	/**
	 * @param phoneNumber the phoneNumber to set
	 */
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	/**
	 * @return the uuid
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * @param uuid the uuid to set
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	/**
	 * @return the careType
	 */
	public String getCareType() {
		return careType;
	}

	/**
	 * @param careType the careType to set
	 */
	public void setCareType(String careType) {
		this.careType = careType;
	}

	/**
	 * @return the latitude
	 */
	public double getLatitude() {
		return latitude;
	}

	/**
	 * @param latitude the latitude to set
	 */
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	/**
	 * @return the longitude
	 */
	public double getLongitude() {
		return longitude;
	}

	/**
	 * @param longitude the longitude to set
	 */
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	/**
	 * @return the isActivated
	 */
	public boolean isActivated() {
		return isActivated;
	}

	/**
	 * @param isActivated the isActivated to set
	 */
	public void setActivated(boolean isActivated) {
		this.isActivated = isActivated;
	}

	/**
	 * @return the registrationId
	 */
	public String getRegistrationId() {
		return registrationId;
	}

	/**
	 * @param registrationId the registrationId to set
	 */
	public void setRegistrationId(String registrationId) {
		this.registrationId = registrationId;
	}

	/**
	 * @return the emailAddress
	 */
	public String getEmailAddress() {
		return emailAddress;
	}

	/**
	 * @param emailAddress the emailAddress to set
	 */
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}
}
