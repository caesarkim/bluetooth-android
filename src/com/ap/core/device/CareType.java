/**
 * AgingSafeSenior
 */
package com.ap.core.device;

/**
 * @author jungwhan
 * CareType.java
 * 3:34:54 PM Feb 15, 2016 2016
 */
public enum CareType {
	CareRecipient(1, "CareRecipient"),
	
	CareGiver(2, "CareGiver");
	
	private int careTypeId;
	
	private String careTypeName;
	
	private CareType(int careType, String typeName) {
		this.careTypeId = careType;
		this.careTypeName = typeName;
	}

	/**
	 * @return the careTypeId
	 */
	public int getCareTypeId() {
		return careTypeId;
	}

	/**
	 * @return the careTypeName
	 */
	public String getCareTypeName() {
		return careTypeName;
	}
}
