/**
 * File: HotCostRecipient.java
 */
package com.lightspeedeps.object;

import com.lightspeedeps.model.Employment;

/**
 * Recipient who can be added, updated or deleted from the Hot Costs
 * Data Entry page. 
 */
public class HotCostRecipient {
	/** Weekly Hot Cost entry associated with this Recipient */
	private WeeklyHotCostsEntry weeklyHotCostsEntry;
	/** Whether or not this recipient's is checked for updates */
	private boolean checked;
	/** Employment Record for the recipient */
	private Employment employment;
	
	/** See {@link #checked}. */
	public boolean getChecked() {
		return checked;
	}

	/** See {@link #checked}. */
	public void setChecked(boolean checked) {
		this.checked = checked;
	}

	/** See {@link #weeklyHotCostsEntry}. */
	public WeeklyHotCostsEntry getWeeklyHotCostsEntry() {
		return weeklyHotCostsEntry;
	}

	/** See {@link #weeklyHotCostsEntry}. */
	public void setWeeklyHotCostsEntry(WeeklyHotCostsEntry weeklyHotCostsEntry) {
		this.weeklyHotCostsEntry = weeklyHotCostsEntry;
	}

	/** See {@link #employment}. */
	public Employment getEmployment() {
		return employment;
	}

	/** See {@link #employment}. */
	public void setEmployment(Employment employment) {
		this.employment = employment;
	}
}
