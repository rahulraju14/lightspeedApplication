package com.lightspeedeps.web.approver;

import java.math.BigDecimal;
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

import com.lightspeedeps.dao.HotCostsInputDAO;
import com.lightspeedeps.model.DailyHotCost;
import com.lightspeedeps.model.HotCostsInput;
import com.lightspeedeps.model.WeeklyHotCosts;
import com.lightspeedeps.object.DepartmentHotCostsEntry;
import com.lightspeedeps.object.HotCostRecipient;
import com.lightspeedeps.object.WeeklyHotCostsEntry;
import com.lightspeedeps.type.DayType;
import com.lightspeedeps.type.WorkZone;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupHolder;

/**
 * Update the different Hot Cost section on the Hot Costs daily entry page
 * independently. This section are used to push the associated values of the
 * section down to the daily hot cost entries. The section are the daily time
 * entry, mpv and budgeted amounts section. There will be a check in place to
 * allow the user to overwrite existing data or not. If there is a need to update
 * weekly hot costs or individual daily hot costs, that will be done in this
 * class as well.
 */
@ManagedBean
@ViewScoped
public class UpdateHotCostsBean extends PopupBean {
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(UpdateHotCostsBean.class);

	/** Which section we are updating from */
//	private int sectionToUpdate;
	/** Boolean for overwriting or preserving existing data. */
	private boolean overwriteData;
	/** Data that is being pushed to the hot cost entries */
	private HotCostsInput hotCostsInput;
	/** List that contains the Weekly Hot Costs data for a department */
	private Map<Integer, DepartmentHotCostsEntry> deptWeeklyHotCostsMap = new HashMap<>();
	/** The number of the day of the week that we are updating */
	private Byte dayOfWeekNum;
	/** Whether or not we are updating all of the HotCostRecipient with the changes */
	private boolean selectAllRecipients;
	/** Popup title based off of the section being cloned */
	private String title;
	/** Popup text based off of the section being cloned */
	private String text;
	/** Popup overwrite text based off of the section being cloned. */
	private String overwriteText;
	/** List of possible recipients */
	private List<HotCostRecipient> recipients;

	/** */
	private boolean errors;

	public static UpdateHotCostsBean getInstance() {
		return (UpdateHotCostsBean)ServiceFinder.findBean("updateHotCostsBean");
	}

	public UpdateHotCostsBean() {
		overwriteData = false;
		selectAllRecipients = false;
		recipients = null;
	}
//
//	/** See {@link #sectionToUpdate}. */
//	public void setSectionToUpdate(int sectionToUpdate) {
//		this.sectionToUpdate = sectionToUpdate;
//	}

	/** See {@link #overwriteData}. */
	public boolean getOverwriteData() {
		return overwriteData;
	}

	/** See {@link #overwriteData}. */
	public void setOverwriteData(boolean overwriteData) {
		this.overwriteData = overwriteData;
	}

	/** See {@link #hotCostsInput}. */
	public void setHotCostsInput(HotCostsInput hotCostsInput) {
		this.hotCostsInput = hotCostsInput;
	}

	/** See {@link #dayOfWeekNum}. */
	public void setDayOfWeekNum(Byte dayOfWeekNum) {
		this.dayOfWeekNum = dayOfWeekNum;
	}

	/** See {@link #errors}. */
	public boolean getErrors() {
		return errors;
	}

	/** See {@link #errors}. */
	public void setErrors(boolean errors) {
		this.errors = errors;
	}

	/** See {@link #deptWeeklyHotCostsMap}. */
	public void setDeptWeeklyHotCostsMap(
			Map<Integer, DepartmentHotCostsEntry> deptWeeklyHotCostsMap) {
		this.deptWeeklyHotCostsMap = deptWeeklyHotCostsMap;
	}

	/** See {@link #selectAllRecipients}. */
	public boolean getSelectAllRecipients() {
		return selectAllRecipients;
	}

	/** See {@link #selectAllRecipients}. */
	public void setSelectAllRecipients(boolean selectAllRecipients) {
		this.selectAllRecipients = selectAllRecipients;
	}

	/** See {@link #title}. */
	@Override
	public String getTitle() {
		return title;
	}

	/** See {@link #title}. */
	@Override
	public void setTitle(String title) {
		this.title = title;
	}

	/** See {@link #text}. */
	public String getText() {
		return text;
	}

	/** See {@link #text}. */
	public void setText(String text) {
		this.text = text;
	}

	/** See {@link #overwriteText}. */
	public String getOverwriteText() {
		return overwriteText;
	}

	/** See {@link #overwriteText}. */
	public void setOverwriteText(String overwriteText) {
		this.overwriteText = overwriteText;
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

	@Override
	public String actionOk() {
		switch (getAction()) {
			case HotCostsDataEntryBean.ACT_DAILY_TIME_ENTRY_SECTION:
				updateDailyTimeValues();
				break;
			case HotCostsDataEntryBean.ACT_MPV_ENTRY_SECTION:
				updateMpvValues();
				break;
			case HotCostsDataEntryBean.ACT_BUDGETED_AMOUNTS_ENTRY_SECTION:
				updatedBudgetedValues();
				break;
		}

		return super.actionOk();
	}

	@Override
	public String actionCancel() {
		hotCostsInput = HotCostsInputDAO.getInstance().refresh(hotCostsInput);
		return super.actionCancel();
	}
	/**
	 * Update the daily time entries from the global push entries to the Daily
	 * Hot Costs entries below. Skip if the fields are empty.
	 *
	 * @return
	 */
	private void updateDailyTimeValues() {
		if (recipients != null && ! recipients.isEmpty()) {
			DayType dayType = hotCostsInput.getDayType();
			BigDecimal callTime = hotCostsInput.getCallTime();
			BigDecimal m1Out = hotCostsInput.getM1Out();
			BigDecimal m1In = hotCostsInput.getM1In();
			BigDecimal m2Out = hotCostsInput.getM2Out();
			BigDecimal m2In = hotCostsInput.getM2In();
			BigDecimal wrap = hotCostsInput.getWrap();
			WorkZone workZone = hotCostsInput.getWorkZone();
			Boolean offProduction = hotCostsInput.getOffProduction();
			Boolean forcedCall = hotCostsInput.getForcedCall();

			for (HotCostRecipient recip : recipients) {
				if (recip.getChecked()) {
					WeeklyHotCostsEntry whc = recip.getWeeklyHotCostsEntry();
					DailyHotCost dailyHotCost = whc.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1);

					// Day Type
					if (overwriteData || dailyHotCost.getDayType() == null) {
						dailyHotCost.setDayType(dayType);
					}
					// Call
					if (overwriteData || dailyHotCost.getCallTime() == null) {
						dailyHotCost.setCallTime(callTime);
					}
					// M1 Out
					if (overwriteData || dailyHotCost.getM1Out() == null) {
						dailyHotCost.setM1Out(m1Out);
					}
					// M1 In
					if (overwriteData || dailyHotCost.getM1In() == null) {
						dailyHotCost.setM1In(m1In);
					}
					// M2 Out
					if (overwriteData || dailyHotCost.getM2Out() == null) {
						dailyHotCost.setM2Out(m2Out);
					}
					// M2 In
					if (overwriteData || dailyHotCost.getM2In() == null) {
						dailyHotCost.setM2Out(m2In);
					}
					// Wrap
					if (overwriteData || dailyHotCost.getWrap() == null) {
						dailyHotCost.setWrap(wrap);
					}

					// Work Zone
					if (overwriteData || dailyHotCost.getWorkZone() == null) {
						dailyHotCost.setWorkZone(workZone);
					}
					// Off Production
					if (!dailyHotCost.getOffProduction()) {
						dailyHotCost.setOffProduction(offProduction);
					}
					// Forced Call
					if (overwriteData) {
						dailyHotCost.setForcedCall(forcedCall);
					}
					HotCostsUtils.updateHours(dailyHotCost, overwriteData);
				}
			}
		}
	}

	/**
	 * Update the MPV values of the individual Daily Hot Cost entries. Skip if
	 * the fields are empty.
	 */
	private void updateMpvValues() {
		if (recipients != null && ! recipients.isEmpty()) {
			BigDecimal ndmStart = hotCostsInput.getNdmStart();
			BigDecimal ndmEnd = hotCostsInput.getNdmEnd();
			BigDecimal ndbEnd = hotCostsInput.getNdbEnd();
			BigDecimal grace1 = hotCostsInput.getGrace1();
			BigDecimal grace2 = hotCostsInput.getGrace2();
			Byte mpv1Payroll = hotCostsInput.getMpv1Payroll();
			Byte mpv2Payroll = hotCostsInput.getMpv2Payroll();
			BigDecimal lastManIn = hotCostsInput.getLastManIn();
			Boolean cameraWrap = hotCostsInput.getCameraWrap();
//			Boolean frenchHours = hotCostsInput.getFrenchHours();

			for (HotCostRecipient recip : recipients) {
				if (recip.getChecked()) {
					WeeklyHotCostsEntry whc = recip.getWeeklyHotCostsEntry();
					DailyHotCost dailyHotCost = whc.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum - 1);

					// NDB End
					if (overwriteData || dailyHotCost.getNdbEnd() == null) {
						dailyHotCost.setNdbEnd(ndbEnd);
					}
					// NDM Start
					if (overwriteData || dailyHotCost.getNdmStart() == null) {
						dailyHotCost.setNdmStart(ndmStart);
					}
					// NDM End
					if (overwriteData || dailyHotCost.getNdmEnd() == null) {
						dailyHotCost.setNdmEnd(ndmEnd);
					}
					// Grace 1
					if (overwriteData || dailyHotCost.getGrace1() == null) {
						dailyHotCost.setGrace1(grace1);
					}
					// Grace 2
					if (overwriteData || dailyHotCost.getGrace2() == null) {
						dailyHotCost.setGrace2(grace2);
					}
					// Camera Wrap
					if (!dailyHotCost.getCameraWrap()) {
						dailyHotCost.setCameraWrap(cameraWrap);
					}
					// Last Man In
					if (overwriteData || dailyHotCost.getLastManIn() == null) {
						dailyHotCost.setLastManIn(lastManIn);
					}

					// MPV 1 Payroll
					if (overwriteData || dailyHotCost.getMpv1Payroll() == null) {
						dailyHotCost.setMpv1Payroll(mpv1Payroll);
					}
					// MPV 2 Payroll
					if (overwriteData || dailyHotCost.getMpv2Payroll() == null) {
						dailyHotCost.setMpv2Payroll(mpv2Payroll);
					}
				}
			}
		}
	}

	/**
	 * Update the Budgeted values of the individual Daily Hot Cost entries. Skip
	 * if the fields are empty.
	 */
	private void updatedBudgetedValues() {
		if (recipients != null && ! recipients.isEmpty()) {
			BigDecimal budgetedHours = hotCostsInput.getBudgetedHours();
			Byte budgetedMpv = hotCostsInput.getBudgetedMpv();
			BigDecimal budgetedMpvCost = hotCostsInput.getBudgetedMpvCost();

			for (HotCostRecipient recip : recipients) {
				if (recip.getChecked()) {
					WeeklyHotCostsEntry whcWrapper = recip.getWeeklyHotCostsEntry();
					WeeklyHotCosts whc = whcWrapper.getWeeklyHotCosts();
					DailyHotCost dailyHotCost = whc.getDailyHotCosts().get(dayOfWeekNum - 1);

					// Budgeted Hours
					if (overwriteData || dailyHotCost.getBudgetedHours() == null) {
						dailyHotCost.setBudgetedHours(budgetedHours);
					}
					// Budgeted Cost
					if(budgetedHours != null) {
						HotCostsUtils.updateBudgetedAmount(whcWrapper, dailyHotCost, budgetedHours);
					}

//					if (overwriteData || dailyHotCost.getBudgetedCost() == null) {
//						dailyHotCost.setBudgetedCost(budgetedCost);
//					}
					// Budgeted MPVs
					if (overwriteData || dailyHotCost.getBudgetedMpv() == null) {
						dailyHotCost.setBudgetedMpv(budgetedMpv);
					}
					// Budgeted MPV Cost
					if (overwriteData || dailyHotCost.getBudgetedMpvCost() == null) {
						dailyHotCost.setBudgetedMpvCost(budgetedMpvCost);
					}
				}
			}
		}
	}

	/**
	 * Create a list of recipients from the collection of entries in the hot
	 * cost by department entries.
	 *
	 * @return
	 */
	private List<HotCostRecipient> createRecipients() {
		recipients = new ArrayList<>();
		Collection<DepartmentHotCostsEntry> dhcs = deptWeeklyHotCostsMap.values();

		for (DepartmentHotCostsEntry dhc : dhcs) {
			List<WeeklyHotCostsEntry> whcWrappers = dhc.getWeeklyHotCostsWrappers();

			for (WeeklyHotCostsEntry whcWrapper : whcWrappers) {
				// If the weekly hot cost entry does not have a
				// daily hot cost for this person, it means
				// it has already been deleted so skip this one.
				if(whcWrapper.getWeeklyHotCosts().getDailyHotCosts().get(dayOfWeekNum) == null) {
					continue;
				}
				HotCostRecipient recip = new HotCostRecipient();

				recip.setChecked(false);
				recip.setWeeklyHotCostsEntry(whcWrapper);

				recipients.add(recip);
			}
		}

		return recipients;
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
