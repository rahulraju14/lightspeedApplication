/**
 * File: DeleteHotCostsBean.java
 */
package com.lightspeedeps.web.approver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIData;
import javax.faces.component.html.HtmlSelectBooleanCheckbox;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.WeeklyHotCostsDAO;
import com.lightspeedeps.model.DailyHotCost;
import com.lightspeedeps.model.WeeklyHotCosts;
import com.lightspeedeps.object.DepartmentHotCostsEntry;
import com.lightspeedeps.object.HotCostRecipient;
import com.lightspeedeps.object.WeeklyHotCostsEntry;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupHolder;

/**
 * Present a list of Daily Hot Cost objects to delete. If OK selected
 * then delete the selected recipients.
 */
@ManagedBean(name = "deleteHotCostsBean")
@ViewScoped
public class DeleteHotCostsBean extends PopupBean {
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(DeleteHotCostsBean.class);

	/** List that contains the Weekly Hot Costs data for a department */
	private Map<Integer, DepartmentHotCostsEntry> deptWeeklyHotCostsList = new HashMap<>();
	/** The number of the day of the week that we are updating */
	private Byte dayOfWeekNum;
	/** Whether or not we are updating all of the HotCostRecipient with the changes */
	private boolean selectAllRecipients;
	/** List of possible recipients */
	private List<HotCostRecipient> recipients;
	/** */
	private boolean errors;

	public static DeleteHotCostsBean getInstance() {
		return (DeleteHotCostsBean)ServiceFinder.findBean("deleteHotCostsBean");
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

	/** See {@link #dayOfWeekNum}. */
	public Byte getDayOfWeekNum() {
		return dayOfWeekNum;
	}

	/** See {@link #dayOfWeekNum}. */
	public void setDayOfWeekNum(Byte dayOfWeekNum) {
		this.dayOfWeekNum = dayOfWeekNum;
	}

	/** See {@link #selectAllRecipients}. */
	public boolean getSelectAllRecipients() {
		return selectAllRecipients;
	}

	/** See {@link #selectAllRecipients}. */
	public void setSelectAllRecipients(boolean selectAllRecipients) {
		this.selectAllRecipients = selectAllRecipients;
	}

	/** See {@link #recipients}. */
	public List<HotCostRecipient> getRecipients() {
		if (recipients == null) {
			recipients = createRecipients();
		}
		return recipients;
	}

	/** See {@link #recipients}. */
	public void setRecipients(List<HotCostRecipient> recipients) {
		this.recipients = recipients;
	}

	/** See {@link #errors}. */
	public boolean getErrors() {
		return errors;
	}

	/** See {@link #errors}. */
	public void setErrors(boolean errors) {
		this.errors = errors;
	}

	/**
	 * Create a list of recipients from the collection of entries in the hot
	 * cost by department entries.
	 *
	 * @return
	 */
	private List<HotCostRecipient> createRecipients() {
		recipients = new ArrayList<>();
		Collection<DepartmentHotCostsEntry> dhcs = deptWeeklyHotCostsList.values();

		for (DepartmentHotCostsEntry dhc : dhcs) {
			List<WeeklyHotCostsEntry> whcs = dhc.getWeeklyHotCostsWrappers();

			for (WeeklyHotCostsEntry whce : whcs) {
				// If the weekly hot cost entry does not have a
				// daily hot cost for this person, it means
				// it has already been deleted so skip this one.
				if(whce.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum) == null) {
					continue;
				}
				HotCostRecipient recip = new HotCostRecipient();

				recip.setChecked(false);
				recip.setWeeklyHotCostsEntry(whce);

				recipients.add(recip);
			}
		}

		return recipients;
	}

	@Override
	public String actionOk() {
		for (HotCostRecipient recip : recipients) {
			if (recip.getChecked()) {
				WeeklyHotCosts whc = recip.getWeeklyHotCostsEntry().getWeeklyHotCosts();
				DailyHotCost dailyHotCost = whc.getDailyHotCosts().get(dayOfWeekNum - 1);

				// Reset the values and mark this Daily Hot Cost display flag
				// false so it will not show up in displayed list on the
				// Hot Costs Daily Entry page.
				dailyHotCost.reset();
				WeeklyHotCostsDAO.getInstance().attachDirty(whc);
			}
		}

		return super.actionOk();
	}

	/**
	 * ValueChangeListener method for the "Select All Recipients" checkbox on
	 * the Clone Hot Costs page.
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
			UIData recipients = (UIData)event.getComponent().findComponent("recipientsTable");
			if (recipients != null) {
				HtmlSelectBooleanCheckbox cb = (HtmlSelectBooleanCheckbox)event.getComponent()
						.findComponent("recipChecked");

				HotCostRecipient recip = (HotCostRecipient)recipients.getRowData();

				recip.setChecked(checked);
				cb.setValue(checked);
			}
		}
	}

	@Override
	public void show(PopupHolder holder, int act, String titleId) {
		this.show(holder, act, titleId, "Confirm.OK", "Confirm.Cancel");
	}
}
