/**
 * ReportRequest.java
 */
package com.lightspeedeps.object;

import com.lightspeedeps.model.User;
import com.lightspeedeps.type.ReportType;

/**
 * A class which represents a report that is scheduled for asynchronous
 * execution.
 */
public class ReportRequest {

	/** Typically this is the object that is needed to generate the report, i.e.,
	 * the source of data for the report.  For example, for a call sheet report,
	 * it is a Callsheet object. */
	private final Object reportSource;

	/** The type of report being requested/scheduled. */
	private final ReportType type;

	/** A User who is related to the report request.  In cases where the report will
	 * be archived in the File Repository, this User will be made the "owner" of that
	 * document. */
	private final User user;

	/**
	 * The normal constructor.
	 * @param t The type of report.
	 * @param rept Typically the Object needed to generate the report.
	 * @param u The User to whom this report is related.
	 */
	public ReportRequest(ReportType t, Object rept, User u) {
		type = t;
		reportSource = rept;
		user = u;
	}

	/**See {@link #reportSource}. */
	public Object getReportSource() {
		return reportSource;
	}

	/**See {@link #type}. */
	public ReportType getType() {
		return type;
	}

	/**See {@link #user}. */
	public User getUser() {
		return user;
	}

}
