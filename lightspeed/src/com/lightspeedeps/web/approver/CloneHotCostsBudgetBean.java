/**
 * File: CopyBudgetBean.java
 */
package com.lightspeedeps.web.approver;

import java.util.Date;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupHolder;

/**
 * Pop up that allows a user to select a date from which to copy the budgeted
 * values and apply them to the current Daily Hot Costs
 */
@ManagedBean
@ViewScoped
public class CloneHotCostsBudgetBean extends PopupBean {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(CloneHotCostsBudgetBean.class);
	private static final long serialVersionUID = 1L;

	/** The date from which the budget should be copied from */
	private Date copyBudgetFromDate;
	/** First day of work */
	private Date workStartDate;

	public static CloneHotCostsBudgetBean getInstance() {
		return (CloneHotCostsBudgetBean)ServiceFinder.findBean("cloneHotCostsBudgetBean");
	}

	public CloneHotCostsBudgetBean() {

	}

	/** See {@link #copyBudgetFromDate}. */
	public Date getCopyBudgetFromDate() {
		return copyBudgetFromDate;
	}

	/** See {@link #copyBudgetFromDate}. */
	public void setCopyBudgetFromDate(Date copyBudgetFromDate) {
		this.copyBudgetFromDate = copyBudgetFromDate;
	}

	/** See {@link #workStartDate}. */
	public Date getWorkStartDate() {
		return workStartDate;
	}

	/** See {@link #workStartDate}. */
	public void setWorkStartDate(Date workStartDate) {
		this.workStartDate = workStartDate;
	}

	@Override
	public String actionOk() {
		HotCostsDataEntryBean bean = (HotCostsDataEntryBean)getConfirmationHolder();
		bean.setCopyBudgetFromDate(copyBudgetFromDate);

		return super.actionOk();
	}
	/**
	 * Show the Copy Budget pop up. The workStartDate is the last date the user
	 * can go back to copy a budget.
	 *
	 * @param popupHolder
	 * @param workStartDate - First day of work for production/project.
	 * @param copyBudgetFromDate - Date from which to copy the budgeted values to use in the current Daily Hot Costs
	 * @param action
	 */
	public void show(PopupHolder popupHolder, Date workStartDate, Date copyBudgetFromDate, int action) {
		this.workStartDate = workStartDate;
		this.copyBudgetFromDate = copyBudgetFromDate;

		show(popupHolder, action, "HotCosts.CloneBudgetedValues.Title", "HotCosts.CloneBudgetedValues.Ok", "Confirm.Cancel");
	}
}
