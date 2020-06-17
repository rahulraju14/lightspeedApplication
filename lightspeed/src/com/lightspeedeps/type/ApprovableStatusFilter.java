//	File Name:	ApprovableStatusFilter.java
package com.lightspeedeps.type;

/**
 * An enumeration used in filtering WeeklyTimecard`s based on their {@link ApprovalStatus} value.
 * <p>
 * Each enum has a unique mask value which is a power of 2.  Each ApprovalStatus enum has
 * a filter-matching value which is the sum of the masks of the ApprovableStatusFilter
 * enums which should include that ApprovalStatus enum.
 * <p>
 * Note that the order of declaration of the values controls the order of presentation
 * in a drop-down list of choices.
 */
public enum ApprovableStatusFilter {

	/** This filter value includes all status values. */
	ALL					(0, "All"),

	/** This filter value includes all timecards at the employee level -- either Open
	 * (never submitted), or rejected back to the employee. */
	NOT_SUBMITTED		(2, "Not Submitted"),

	/** This filter value includes all timecards not at the employee level, and which have not
	 * yet received final approval; i.e., anything waiting for approval. */
	SUBMITTED			(4, "Submitted"),

	/** This filter value includes "SUBMITTED" timecards that are in the current viewer's queue.
	 * This is the only filter whose results will vary relative to the viewer. */
	READY  				(8, "Ready for Your Review"),

	/** This filter value includes all timecards that have been rejected or recalled. */
	REJECTED_RECALLED	(16, "Rejected or Recalled"),

	/** This filter value includes all timecards that have received final approval
	 * (regardless of whether they are locked or not). */
	APPROVED			(32, "Approved (Final)"),

	/** This filter value includes only timecards with the Void status. */
	VOID				(64, "Void"),

	/** This serves only as a delimiter between "normal" WeeklyStatusFilter values for
	 * timecards, and the entries used for StartForm filtering on the Approval Dashboard. */
	N_A					(0, ""),

	/** StartForm`s that are complete -- may be used for generating timecards. */
	COMPLETE			(0, "Complete"),

	/** StartForm`s that are not complete -- timecards cannot be generated from these. */
	INCOMPLETE			(0, "Incomplete"),
	;

	/** The usual mixed-case heading to display for this value. */
	private final String heading;

	/** A unique mask value which is a power of 2. Each {@link ApprovalStatus} enum has a
	 * filter-matching value which is the sum of the masks of the
	 * WeeklyStatusFilter enums which should include that WeeklyStatus enum. */
	protected final int mask;

	private ApprovableStatusFilter(int pMask, String head) {
		heading = head;
		mask = pMask;
	}

	/**
	 * @return a comma-delimited list of  {@link ApprovalStatus} values which should be
	 *         selected for in an SQL query to get the (super)set of timecards
	 *         that match this filter. The query result may be a superset
	 *         (including extraneous timecards) if the filter is one which is
	 *         relative to the viewer, e.g., the "READY" filter.
	 */
	public String sqlFilter() {
		String filter = "";
		for ( ApprovalStatus stat : ApprovalStatus.values()) {
			if (stat.isIncludedInFilter(this)) {
				filter += ", '" + stat.name() + "'";
			}
		}
		if (filter.length() > 2) {
			filter = filter.substring(2);
		}
		return filter;
	}

	/** See {@link #mask}. */
	public int getMask() {
		return mask;
	}

	/**
	 * @return The "pretty" mixed-case version of the enumerated value.
	 * This could be enhanced to use the current locale setting for i18n purposes.
	 * <p>
	 * NOTE: This overrides the default toString(), which returns
	 * the same value as name(), which is the exact text of the enum name.
	 */
	@Override
	public String toString() {
		return heading;
	}

	/**
	 * @return The "pretty" printable version of this enumerated type. It is the same as toString,
	 * but can be used from jsp pages since it follows the bean accessor (getter) naming convention.
	 */
	public String getLabel() {
		return toString();
	}

}

