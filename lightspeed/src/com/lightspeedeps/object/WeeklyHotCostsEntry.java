package com.lightspeedeps.object;

import java.io.Serializable;

import com.lightspeedeps.model.Department;
import com.lightspeedeps.model.WeeklyHotCosts;
import com.lightspeedeps.model.WeeklyTimecard;

/**
 * Display collection of the weekly hot costs. Will be used on the Hot
 * Costs Summary dashboard.
 */
public class WeeklyHotCostsEntry implements Serializable {

	private static final long serialVersionUID = 1L;

	/** Department associated with the DailyHotCost collection */
	private Department dept;

	/** Current WeeklyHotCosts */
	private WeeklyHotCosts weeklyHotCosts;
	/** Current WeeklyTimecard for this employee.
	 * This will be used to put in values for daily hot costs if there are not
	 * and associated WeeklyHotCosts objects
	 */
	private WeeklyTimecard weeklyTimecard;

	/** Whether to expand or collapse the table for this department*/
	private boolean expand;

	/** Default Constructor*/
	public WeeklyHotCostsEntry() {
		expand = true;
	}

	/** See {@link #weeklyTimecard}. */
	public WeeklyTimecard getWeeklyTimecard() {
		return weeklyTimecard;
	}

	/** See {@link #weeklyTimecard}. */
	public void setWeeklyTimecard(WeeklyTimecard weeklyTimecard) {
		this.weeklyTimecard = weeklyTimecard;
	}

	/** See {@link #dept}. */
	public Department getDept()  {
		return dept;
	}

	/** See {@link #dept}. */
	public void setDept(Department dept) {
		this.dept = dept;
	}

	/** See {@link #expand}. */
	public boolean getExpand() {
		return expand;
	}

	/** See {@link #expand}. */
	public void setExpand(boolean expand) {
		this.expand = expand;
	}

	/** See {@link #weeklyHotCost}. */
	public WeeklyHotCosts getWeeklyHotCosts() {
		return weeklyHotCosts;
	}

	/** See {@link #weeklyHotCost}. */
	public void setWeeklyHotCosts(WeeklyHotCosts weeklyHotCosts) {
		this.weeklyHotCosts = weeklyHotCosts;
	}
}
