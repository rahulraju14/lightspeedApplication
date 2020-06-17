/**
 * File: UserCallInfo.java
 */
package com.lightspeedeps.object;

import java.io.Serializable;
import java.util.Date;

/**
 * This object holds calculated information about a user's call-times in a
 * single Unit in which he has a role. A collection of these are kept in the
 * UserCallInfo object. (This is not a persisted object -- there is no SQL table
 * backing it.)
 */
public class UserCallUnit implements Serializable {
	/** */
	private static final long serialVersionUID = 3575632209594225516L;

	/** The database id of the Callsheet that was the source of this
	 * information. */
	private Integer callsheetId;

	/** The user's call time, or null if not found in the corresponding
	 * Callsheet. */
	private Date userCall;

	/** The crew-call time from the corresponding Callsheet. */
	private Date crewCall;

	/**
	 * Construct an instance with the given values.
	 *
	 * @param csId The database id of the Callsheet that was the source of this
	 *            information.
	 * @param userCall The user's call time, or null if not found in the
	 *            corresponding Callsheet.
	 * @param crewCall The crew-call time from the corresponding Callsheet.
	 */
	public UserCallUnit(Integer csId, Date userCall, Date crewCall) {
		callsheetId = csId;
		this.userCall = userCall;
		this.crewCall = crewCall;
	}

	/** See {@link #callsheetId}. */
	public Integer getCallsheetId() {
		return callsheetId;
	}
	/** See {@link #callsheetId}. */
	public void setCallsheetId(Integer callsheetId) {
		this.callsheetId = callsheetId;
	}

	/** See {@link #userCall}. */
	public Date getUserCall() {
		return userCall;
	}
	/** See {@link #userCall}. */
	public void setUserCall(Date userCall) {
		this.userCall = userCall;
	}

	/** See {@link #crewCall}. */
	public Date getCrewCall() {
		return crewCall;
	}
	/** See {@link #crewCall}. */
	public void setCrewCall(Date crewCall) {
		this.crewCall = crewCall;
	}

}
