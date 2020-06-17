/**
 * File: UserCallInfo.java
 */
package com.lightspeedeps.object;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.lightspeedeps.model.Unit;

/**
 * This object holds calculated information about a user's call-times
 * in all the Units in which he has a role.  It is kept in the
 * user's session, and recalculated periodically, as needed.
 *  (This is not a persisted object -- there is no SQL
 * table backing it.)
 */
public class UserCallInfo implements Serializable {
	/** */
	private static final long serialVersionUID = - 7660714636568438813L;

	/** The date & time at which the information in this object was last evaluated. */
	private Date lastEvaluated = new Date();

	/** The id of the Project that this call-time information applies to. */
	private Integer projectId;

	/** The database id of the user's "current" Callsheet.  This will be null if
	 * there is no such Callsheet. */
	private Integer currentCsId;

	/** A Map from Unit number to the UserCallUnit that holds the call-time information
	 * corresponding to that Unit. */
	private final Map<Integer,UserCallUnit> callInfo = new HashMap<Integer,UserCallUnit>();

	/** The default constructor. */
	public UserCallInfo() {
	}

	/**
	 * Store the user's call-time information related to a given Unit.
	 *
	 * @param unit The Unit this information applies to.
	 * @param callSheetId The database id of the Callsheet from which the
	 *            information was taken.
	 * @param usercall The User's specific call-time from the Callsheet.
	 * @param crewcall The general crew call-time from the Callsheet.
	 */
	public void put(Unit unit, Integer callSheetId, Date usercall, Date crewcall) {
		callInfo.put(unit.getNumber(), new UserCallUnit(callSheetId, usercall, crewcall));
	}

	/**
	 * Get the {@link com.lightspeedeps.object.UserCallUnit UserCallUnit}
	 * corresponding to the specified Unit.
	 *
	 * @param unit The requested {@link com.lightspeedeps.model.Unit Unit}.
	 * @return The UserCallUnit for the unit given, or null if the user has no
	 *         role in that Unit.
	 */
	public UserCallUnit getCallUnit(Unit unit) {
		return callInfo.get(unit.getNumber());
	}

	/** See {@link #lastEvaluated}. */
	public Date getLastEvaluated() {
		return lastEvaluated;
	}
	/** See {@link #lastEvaluated}. */
	public void setLastEvaluated(Date lastEvaluated) {
		this.lastEvaluated = lastEvaluated;
	}

	/** See {@link #projectId}. */
	public Integer getProjectId() {
		return projectId;
	}
	/** See {@link #projectId}. */
	public void setProjectId(Integer projectId) {
		this.projectId = projectId;
	}

	/** See {@link #currentCsId}. */
	public Integer getCurrentCsId() {
		return currentCsId;
	}
	/** See {@link #currentCsId}. */
	public void setCurrentCsId(Integer currentCsId) {
		this.currentCsId = currentCsId;
	}

}
