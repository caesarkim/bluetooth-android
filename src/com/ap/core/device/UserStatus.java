/**
 * AgingPlaceMobile
 */
package com.ap.core.device;

/**
 * @author jungwhan
 * CareRecipient.java
 * 2:14:44 AM Feb 17, 2016 2016
 */
public class UserStatus {
	private PresenceStatus[] userStatus;
	
	private boolean inactivityNotificationSent = false;
	
	private long statusChangeTime;
	
	@Override
	public String toString() {
		StringBuffer b = new StringBuffer();
		if (userStatus != null) {
			for (PresenceStatus status : userStatus) {
				b.append("userStatus=").append(status.name());
			}			
		}
		return b.toString();
	}
	
	/**
	 * @return the userStatus
	 */
	public PresenceStatus[] getUserStatuss() {
		return userStatus;
	}

	/**
	 * @return the userStatus
	 */
	public PresenceStatus getUserStatus() {
		if (userStatus == null) {
			return null;
		}
		return userStatus[0];
	}

	/**
	 * @param userStatus the userStatus to set
	 */
	public void setUserStatus(PresenceStatus... userStatus) {
		this.userStatus = userStatus;
	}

	/**
	 * @return the awayDuration
	 */
	public long getStatusChangeTime() {
		return statusChangeTime;
	}

	/**
	 * @param awayDuration the awayDuration to set
	 */
	public void setStatusChangeTime(long awayDuration) {
		this.statusChangeTime = awayDuration;
	}

	/**
	 * @return the isNotificationSent
	 */
	public boolean isInactivityNotificationSent() {
		return inactivityNotificationSent;
	}

	/**
	 * @param isNotificationSent the isNotificationSent to set
	 */
	public void setInactivityNotificationSent(boolean isNotificationSent) {
		this.inactivityNotificationSent = isNotificationSent;
	}
}
