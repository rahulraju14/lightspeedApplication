package com.lightspeedeps.object;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseId;

import com.lightspeedeps.model.Department;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.common.NumberUtils;

/**
 * Class to hold collection of DailyHotCostEntries to display on Hot Costs
 * Daily and Summary pages.
 */
public class DepartmentHotCostsEntry implements Serializable {
	private static final long serialVersionUID = 1L;

	/** List of WeeklyHotCost wrapper class for display purposed */
	private List<WeeklyHotCostsEntry>weeklyHotCostsWrappers;
	/** Department associated with the DailyHotCost collection */
	private Department dept;
	/** Whether to expand or collapse the table for this department*/
	private boolean expand;
	/** Whether to expand or collapse the table for this department in the Summary view */
	private boolean summaryExpand;

	/** Default Constructor*/
	public DepartmentHotCostsEntry() {
		weeklyHotCostsWrappers = new ArrayList<>(0);
		expand = true;
		summaryExpand = true;
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

	/** See {@link #summaryExpand}. */
	public boolean getSummaryExpand() {
		return summaryExpand;
	}

	/** See {@link #summaryExpand}. */
	public void setSummaryExpand(boolean summaryExpand) {
		this.summaryExpand = summaryExpand;
	}

	/** See {@link #weeklyHotCostsWrappers}. */
	public List<WeeklyHotCostsEntry> getWeeklyHotCostsWrappers() {
		return weeklyHotCostsWrappers;
	}

	/** See {@link #weeklyHotCostsWrappers}. */
	public void setWeeklyHotCostsWrappers(List<WeeklyHotCostsEntry> weeklyHotCostsWrappers) {
		this.weeklyHotCostsWrappers = weeklyHotCostsWrappers;
	}

	/**
	 * Listener for whether to collapse or expand the department table
	 * @param event
	 */
	public void listenToggleDept(ActionEvent event) {
		if (!event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// simpler to schedule event for later - after "setXxxx()" are called from framework
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		expand = !expand;
	}

	//
	/**
	 * Calculate Total Sunday Cost for the Summary row
	 */
	public BigDecimal getTotalSundayCost() {
		BigDecimal totalCost = Constants.DECIMAL_ZERO;

		if (weeklyHotCostsWrappers != null && !weeklyHotCostsWrappers.isEmpty()) {
			for (WeeklyHotCostsEntry whcWrapper : weeklyHotCostsWrappers) {
				totalCost = NumberUtils.safeAdd(totalCost, whcWrapper.getWeeklyHotCosts().getDailyHotCosts().get(0).getActualCost());
			}
		}
		return totalCost;
	}

	/**
	 * Calculate Total Monday Cost for the Summary row
	 */
	public BigDecimal getTotalMondayCost() {
		BigDecimal totalCost = Constants.DECIMAL_ZERO;

		if (weeklyHotCostsWrappers != null && !weeklyHotCostsWrappers.isEmpty()) {
			for (WeeklyHotCostsEntry whcWrapper : weeklyHotCostsWrappers) {
				totalCost = NumberUtils.safeAdd(totalCost, whcWrapper.getWeeklyHotCosts().getDailyHotCosts().get(1).getActualCost());
			}
		}
		return totalCost;
	}

	/**
	 * Calculate Total Tuesday Cost for the Summary row
	 */
	public BigDecimal getTotalTuesdayCost() {
		BigDecimal totalCost = Constants.DECIMAL_ZERO;

		if (weeklyHotCostsWrappers != null && !weeklyHotCostsWrappers.isEmpty()) {
			for (WeeklyHotCostsEntry whcWrapper : weeklyHotCostsWrappers) {
				totalCost = NumberUtils.safeAdd(totalCost, whcWrapper.getWeeklyHotCosts().getDailyHotCosts().get(2).getActualCost());
			}
		}
		return totalCost;
	}

	/**
	 * Calculate Total Wednesday Cost for the Summary row
	 */
	public BigDecimal getTotalWednesdayCost() {
		BigDecimal totalCost = Constants.DECIMAL_ZERO;

		if (weeklyHotCostsWrappers != null && !weeklyHotCostsWrappers.isEmpty()) {
			for (WeeklyHotCostsEntry whcWrapper : weeklyHotCostsWrappers) {
				totalCost = NumberUtils.safeAdd(totalCost, whcWrapper.getWeeklyHotCosts().getDailyHotCosts().get(3).getActualCost());
			}
		}
		return totalCost;
	}

	/**
	 * Calculate Total Thursday Cost for the Summary row
	 */
	public BigDecimal getTotalThursdayCost() {
		BigDecimal totalCost = Constants.DECIMAL_ZERO;

		if (weeklyHotCostsWrappers != null && !weeklyHotCostsWrappers.isEmpty()) {
			for (WeeklyHotCostsEntry whcWrapper : weeklyHotCostsWrappers) {
				totalCost = NumberUtils.safeAdd(totalCost, whcWrapper.getWeeklyHotCosts().getDailyHotCosts().get(4).getActualCost());
			}
		}
		return totalCost;
	}

	/**
	 * Calculate Total Friday Cost for the Summary row
	 */
	public BigDecimal getTotalFridayCost() {
		BigDecimal totalCost = Constants.DECIMAL_ZERO;

		if (weeklyHotCostsWrappers != null && !weeklyHotCostsWrappers.isEmpty()) {
			for (WeeklyHotCostsEntry whcWrapper : weeklyHotCostsWrappers) {
				totalCost = NumberUtils.safeAdd(totalCost, whcWrapper.getWeeklyHotCosts().getDailyHotCosts().get(5).getActualCost());
			}
		}
		return totalCost;
	}

	/**
	 * Calculate Total Saturday Cost for the Summary row
	 */
	public BigDecimal getTotalSaturdayCost() {
		BigDecimal totalCost = Constants.DECIMAL_ZERO;

		if (weeklyHotCostsWrappers != null && !weeklyHotCostsWrappers.isEmpty()) {
			for (WeeklyHotCostsEntry whcWrapper : weeklyHotCostsWrappers) {
				totalCost = NumberUtils.safeAdd(totalCost, whcWrapper.getWeeklyHotCosts().getDailyHotCosts().get(6).getActualCost());
			}
		}
		return totalCost;
	}

	/**
	 * Calculate Total Hours for the Summary row
	 */
	public BigDecimal getTotalHours() {
		BigDecimal totalHours = Constants.DECIMAL_ZERO;

		if (weeklyHotCostsWrappers != null && !weeklyHotCostsWrappers.isEmpty()) {
			for (WeeklyHotCostsEntry whcWrapper : weeklyHotCostsWrappers) {
				totalHours = NumberUtils.safeAdd(totalHours, whcWrapper.getWeeklyHotCosts().getWeeklyWorkedHours());
			}
		}

		return totalHours;
	}

	/**
	 * Calculate Total Budgeted Hours for the Summary row
	 */
	public BigDecimal getTotalBudgetedHours() {
		BigDecimal totalHours = Constants.DECIMAL_ZERO;

		if (weeklyHotCostsWrappers != null && !weeklyHotCostsWrappers.isEmpty()) {
			for (WeeklyHotCostsEntry whcWrapper : weeklyHotCostsWrappers) {
				totalHours = NumberUtils.safeAdd(totalHours, whcWrapper.getWeeklyHotCosts().getBudgetedWeeklyHours());
			}
		}
		return totalHours;
	}

	/**
	 * Calculate Total Hours Variance for the Summary row
	 */
	public BigDecimal getTotalHoursVariance() {
		BigDecimal variance = Constants.DECIMAL_ZERO;

		if (weeklyHotCostsWrappers != null && !weeklyHotCostsWrappers.isEmpty()) {
			for (WeeklyHotCostsEntry whcWrapper : weeklyHotCostsWrappers) {
				variance = NumberUtils.safeAdd(variance, whcWrapper.getWeeklyHotCosts().getHoursVariance());
			}
		}

		return variance;
	}

	/**
	 * Calculate Total Costs for the Summary row
	 */
	public BigDecimal getTotalCost() {
		BigDecimal totalCost = Constants.DECIMAL_ZERO;

		if (weeklyHotCostsWrappers != null && !weeklyHotCostsWrappers.isEmpty()) {
			for (WeeklyHotCostsEntry whcWrapper : weeklyHotCostsWrappers) {
				totalCost = NumberUtils.safeAdd(totalCost, whcWrapper.getWeeklyHotCosts().getWeeklyCost());
			}
		}

		return totalCost;
	}

	/**
	 * Calculate Total Budgeted Costs Variance for the Summary row
	 */
	public BigDecimal getTotalBudgetedCost() {
		BigDecimal totalCost = Constants.DECIMAL_ZERO;

		if (weeklyHotCostsWrappers != null && !weeklyHotCostsWrappers.isEmpty()) {
			for (WeeklyHotCostsEntry whcWrapper : weeklyHotCostsWrappers) {
				totalCost = NumberUtils.safeAdd(totalCost, whcWrapper.getWeeklyHotCosts().getBudgetedWeeklyCost());
			}
		}

		return totalCost;
	}

	/**
	 * Calculate Total Costs Variance for the Summary row
	 */
	public BigDecimal getTotalCostVariance() {
		BigDecimal variance = Constants.DECIMAL_ZERO;

		if (weeklyHotCostsWrappers != null && !weeklyHotCostsWrappers.isEmpty()) {
			for (WeeklyHotCostsEntry whcWrapper : weeklyHotCostsWrappers) {
				variance = NumberUtils.safeAdd(variance, whcWrapper.getWeeklyHotCosts().getCostVariance());
			}
		}

		return variance;
	}
}
