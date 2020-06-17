/**
 * File: AddHotCostsBean.java
 */
package com.lightspeedeps.web.approver;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIData;
import javax.faces.component.html.HtmlSelectBooleanCheckbox;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.EmploymentDAO;
import com.lightspeedeps.dao.WeeklyHotCostsDAO;
import com.lightspeedeps.model.Department;
import com.lightspeedeps.model.Employment;
import com.lightspeedeps.model.StartForm;
import com.lightspeedeps.model.WeeklyHotCosts;
import com.lightspeedeps.object.DepartmentHotCostsEntry;
import com.lightspeedeps.object.HotCostRecipient;
import com.lightspeedeps.object.WeeklyHotCostsEntry;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupHolder;

/**
 * Popup that allows a user to add a person to the daily hot cost entry view.
 */
@ManagedBean
@ViewScoped
public class AddHotCostsBean extends PopupBean {
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(AddHotCostsBean.class);

	/**
	 * Date of the day that we are adding Hot Costs for. Do not add people whose
	 * start date is after this date or their end date is before this date.
	 */
	private Date dayDate;
	/**
	 * Week End Date for the data being pulled from the timecards and the week
	 * end date for the hot costs being processed
	 */
	private Date weekEndDate;
	/**
	 * Map of Employment Records for eligible people who can be added. If the
	 * person is added the checked field on the Employment Record will be true;
	 */
	private Map<Employment, WeeklyHotCosts> eligibleEmployments = new HashMap<>();
	/** List of eligible employment records to display */
	private List<HotCostRecipient> recipients;
	/** List that contains the Weekly Hot Costs data for a department */
	private Map<Integer, DepartmentHotCostsEntry> deptWeeklyHotCostsList;
	/**
	 * Whether or not we are updating all of the HotCostRecipient with the
	 * changes
	 */
	private boolean selectAllRecipients;

	public static AddHotCostsBean getInstance() {
		return (AddHotCostsBean)ServiceFinder.findBean("addHotCostsBean");
	}

	public AddHotCostsBean() {

	}

	/** See {@link #weekEndDate}. */
	public Date getWeekEndDate() {
		return weekEndDate;
	}

	/** See {@link #weekEndDate}. */
	public void setWeekEndDate(Date weekEndDate) {
		this.weekEndDate = weekEndDate;
	}

	/** See {@link #dayDate}. */
	public Date getDayDate() {
		return dayDate;
	}

	/** See {@link #dayDate}. */
	public void setDayDate(Date dayDate) {
		this.dayDate = dayDate;
	}

	/** See {@link #eligibleEmployments}. */
	public Map<Employment, WeeklyHotCosts> getEligibleEmployments() {
		return eligibleEmployments;
	}

	/** See {@link #eligibleEmployments}. */
	public void setEligibleEmployments(Map<Employment, WeeklyHotCosts> eligibleEmployments) {
		this.eligibleEmployments = eligibleEmployments;
	}

	/** See {@link #recipients}. */
	public List<HotCostRecipient> getRecipients() {
		if (recipients == null) {
			createRecipients();
		}
		return recipients;
	}

	/** See {@link #recipients}. */
	public void setRecipients(List<HotCostRecipient> recipients) {
		this.recipients = recipients;
	}

	/** See {@link #deptWeeklyHotCostsList}. */
	public Map<Integer, DepartmentHotCostsEntry> getDeptWeeklyHotCostsList() {
		return deptWeeklyHotCostsList;
	}

	/** See {@link #deptWeeklyHotCostsList}. */
	public void setDeptWeeklyHotCostsList(
			Map<Integer, DepartmentHotCostsEntry> deptWeeklyHotCostsList) {
		this.deptWeeklyHotCostsList = deptWeeklyHotCostsList;
	}

	/** See {@link #selectAllRecipients}. */
	public boolean getSelectAllRecipients() {
		return selectAllRecipients;
	}

	/** See {@link #selectAllRecipients}. */
	public void setSelectAllRecipients(boolean selectAllRecipients) {
		this.selectAllRecipients = selectAllRecipients;
	}

	private void createRecipients() {
		recipients = new ArrayList<>();
		Iterator<Entry<Employment, WeeklyHotCosts>> itr = eligibleEmployments.entrySet().iterator();

		while (itr.hasNext()) {
			Entry<Employment, WeeklyHotCosts> entry = itr.next();
			HotCostRecipient recipient = new HotCostRecipient();

			recipient.setEmployment(entry.getKey());
			WeeklyHotCostsEntry whce = new WeeklyHotCostsEntry();
			whce.setWeeklyHotCosts(entry.getValue());
			recipient.setWeeklyHotCostsEntry(whce);

			recipients.add(recipient);
		}
	}

	@Override
	public String actionOk() {
		// Add the checked recipients with new WeeklyHotCosts to the department.
		for (HotCostRecipient recip : recipients) {
			if (recip.getChecked()) {
				WeeklyHotCosts whc = recip.getWeeklyHotCostsEntry().getWeeklyHotCosts();
				Employment emp = recip.getEmployment();
				emp = EmploymentDAO.getInstance().refresh(emp);
				emp.getStartForms().size();
				emp.getContact();
				Department dept = emp.getRole().getDepartment();

				if (whc == null) {
					StartForm sf;
					StartForm existingSf = emp.getStartForm();

					if (existingSf == null) {
						sf = new StartForm();
						sf.setEmployment(emp);
						sf.setContact(emp.getContact());
						sf.setProd(null);
					}
					else {
						sf = existingSf.deepCopy();
					}
					whc = HotCostsUtils.createNewWeeklyHotCosts(emp, null, weekEndDate, sf);
				}
				DepartmentHotCostsEntry deptWhc = deptWeeklyHotCostsList.get(dept.getId());

				if (deptWhc == null) {
					// This is the first member of this department so create new Department
					// Hot Costs entry.
					deptWhc = new DepartmentHotCostsEntry();
				}

				WeeklyHotCostsEntry whce = new WeeklyHotCostsEntry();

				// Save the new Weekly Hot Costs record.
				if(whc.getId() == null) {
					WeeklyHotCostsDAO.getInstance().save(whc);
				}
				else {
					WeeklyHotCostsDAO.getInstance().attachDirty(whc);
				}

				whce.setDept(dept);
				whce.setWeeklyHotCosts(whc);
				deptWhc.getWeeklyHotCostsWrappers().add(whce);

			}
		}

		deptWeeklyHotCostsList = null;
		return super.actionOk();
	}

	/**
	 * ValueChangeListener method for the "Select All Recipients" checkbox on
	 * the Add Hot Costs page.
	 *
	 * @param event contains old and new values
	 */
	public void listenSelectAllRecipients(ValueChangeEvent event) {
		if (event.getNewValue() != null) {
			boolean checked = (Boolean)event.getNewValue();
			for (HotCostRecipient recip : recipients) {
				recip.setChecked(checked);
			}
		}
	}

	/**
	 * Check/uncheck individual recipients
	 *
	 * @param event
	 */
	public void listenSelectRecipient(ValueChangeEvent event) {
		Boolean checked = (Boolean)event.getNewValue();

		if (checked != null) {
			UIData recipients = (UIData)event.getComponent().findComponent("addHotCostsTable");
			if (recipients != null) {
				HtmlSelectBooleanCheckbox cb = (HtmlSelectBooleanCheckbox)event.getComponent()
						.findComponent("recipChecked");

				HotCostRecipient recip = (HotCostRecipient)recipients.getRowData();

				recip.setChecked(checked);
				cb.setValue(checked);
			}
		}
	}

	public void show(PopupHolder popupHolder, int action) {
		this.show(popupHolder, action, null, "Confirm.OK", "Confirm.Cancel");
	}
}
