/**
 * AgingPlaceMobile
 */
package com.ap.core.device;

/**
 * @author jungwhan
 * ConnectType.java
 * 11:03:11 PM Mar 7, 2016 2016
 */
public enum ConnectType {
	CONNECTED (1),
	
	DISCONNECTED(2);
	
	private int connectType;
	
	private ConnectType(int connectType) {
		this.connectType = connectType;
	}
	
	public int getConnectType() {
		return this.connectType;
	}
}
