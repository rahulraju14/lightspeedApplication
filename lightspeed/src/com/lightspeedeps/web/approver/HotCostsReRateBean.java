package com.lightspeedeps.web.approver;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.component.radiobutton.RadioButton;

import com.lightspeedeps.dao.DailyHotCostDAO;
import com.lightspeedeps.dao.OccupationDAO;
import com.lightspeedeps.dao.PayRateDAO;
import com.lightspeedeps.dao.UnionsDAO;
import com.lightspeedeps.dao.WeeklyHotCostsDAO;
import com.lightspeedeps.model.DailyHotCost;
import com.lightspeedeps.model.Occupation;
import com.lightspeedeps.model.PayRate;
import com.lightspeedeps.model.StartForm;
import com.lightspeedeps.model.Unions;
import com.lightspeedeps.model.WeeklyHotCosts;
import com.lightspeedeps.type.EmployeeRateType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.popup.PopupInputBean;

/**
 * Popup for re-rating a member or setting them back to their original rate.
 */
@ManagedBean
@ViewScoped
public class HotCostsReRateBean extends PopupInputBean {
	private static final Log log = LogFactory.getLog(HotCostsReRateBean.class);
	private static final long serialVersionUID = 1L;

	/** Rate type to use */
	/** Current rate */
	public static final String CURRENT_RATE = "CR";
	/** Re-rate */
	public static final String RE_RATE = "RR";

	/** Re-rate Types */
	/** Hourly re-rate type */
	public static final String HOURLY = "HOURLY_RATE";
	/** Daily re-rate type */
	public static final String DAILY = "DAILY_RATE";
	/** Weekly re-rate type */
	public static final String WEEKLY = "WEEKLY_RATE";

	/** Whether the employee rate type is Hourly, Daily Exempt or
	 * Weekly Exempt.
	 */
	private EmployeeRateType rateType;
	/** Rate to use either current or re-rate */
	private String rateToUse;
	/** Rate from the start form */
	private BigDecimal rate;
	/** Daily Rate **/
	private BigDecimal dailyRate;
	/** Weekly Rate */
	private BigDecimal weeklyRate;
	/** Rate to apply for a Re-rate */
	private BigDecimal reRateAmount;
	/** Whether we are doing a Re-rate or using the current rate */
	private Boolean reRate = false;
	/** The DailyHotCost item to apply the change in rate to */
	private DailyHotCost dailyHotCost;
	/** WeeklyHotCosts item associated with the DailyHotCost */
	private WeeklyHotCosts weeklyHotCosts;
	/** Whether this is a Union employee */
	private boolean isUnion;
	/** Union associated with this employee */
	private Unions union;
	/** The database id of the selected job class (occupation) entry. */
	private Integer jobClassId;
	/** The list of job classifications (occupations) to choose from. */
	private List<SelectItem> jobClassDL;
	/** The database id of the Occupation entry selected via the Occ Code drop-down. */
	private Integer occCodeId;
	/** The list of occupation codes to choose from, based on the
	 * currently selected union and job classification (occupation name). */
	private List<SelectItem> occCodeDL;
	/** Map keyed by occCode that contains associated PayRate object */
	private Map<String, PayRate> occPayRate;
	/** Ls Occ Code associated with this employee. */
	private String lsOccCode;
	/** If not in edit mode save the changes immediately */
	private boolean editMode;
	/** Using rate according to the agreement this employee is under. */
	private boolean useStandardRate;
	/**  Using re-rate amount */
	private boolean useRerate;

	private transient PayRateDAO payRateDAO;

	/**
	 * Returns the current instance of our bean. Note that this may not be available in a batch
	 * environment, in which case null is returned.
	 */
	public static HotCostsReRateBean getInstance() {
		HotCostsReRateBean bean = null;

		bean = (HotCostsReRateBean) ServiceFinder.findBean("hotCostsReRateBean");

		return bean;
	}

	/** Default Constructor */
	public HotCostsReRateBean() {
		rateToUse = CURRENT_RATE;
		useStandardRate = true;
	}

	/** See {@link #reRateAmount}. */
	public BigDecimal getReRateAmount() {
		return reRateAmount;
	}

	/** See {@link #reRateAmount}. */
	public void setReRateAmount(BigDecimal reRateAmount) {
		this.reRateAmount = reRateAmount;
	}

	/** See {@link #reRate}. */
	public Boolean getReRate() {
		return reRate;
	}

	/** See {@link #reRate}. */
	public void setReRate(Boolean reRate) {
		this.reRate = reRate;
	}

	/** See {@link #rate}. */
	public BigDecimal getRate() {
		return rate;
	}

	/** See {@link #rate}. */
	public void setRate(BigDecimal rate) {
		this.rate = rate;
	}

	/** See {@link #dailyRate}. */
	public BigDecimal getDailyRate() {
		return dailyRate;
	}

	/** See {@link #dailyRate}. */
	public void setDailyRate(BigDecimal dailyRate) {
		this.dailyRate = dailyRate;
	}

	/** See {@link #weeklyRate}. */
	public BigDecimal getWeeklyRate() {
		return weeklyRate;
	}

	/** See {@link #weeklyRate}. */
	public void setWeeklyRate(BigDecimal weeklyRate) {
		this.weeklyRate = weeklyRate;
	}

	/** See {@link #rateToUse}. */
	public String getRateToUse() {
		return rateToUse;
	}

	/** See {@link #rateToUse}. */
	public void setRateToUse(String rateToUse) {
		this.rateToUse = rateToUse;
	}

	/** See {@link #rateType}. */
	public EmployeeRateType getRateType() {
		return rateType;
	}

	/** See {@link #rateType}. */
	public void setRateType(EmployeeRateType rateType) {
		this.rateType = rateType;
	}

	/** See {@link #dailyHotCost}. */
	public DailyHotCost getDailyHotCost() {
		return dailyHotCost;
	}

	/** See {@link #dailyHotCost}. */
	public void setDailyHotCost(DailyHotCost dailyHotCost) {
		this.dailyHotCost = dailyHotCost;
	}

	/** See {@link #isUnion}. */
	public boolean getIsUnion() {
		return isUnion;
	}

	/** See {@link #isUnion}. */
	public void setIsUnion(boolean isUnion) {
		this.isUnion = isUnion;
	}

	/** See {@link #union}. */
	public Unions getUnion() {
		return union;
	}

	/** See {@link #union}. */
	public void setUnion(Unions union) {
		this.union = union;
	}

	/** See {@link #jobClassId}. */
	public Integer getJobClassId() {
		return jobClassId;
	}

	/** See {@link #jobClassId}. */
	public void setJobClassId(Integer jobClassId) {
		this.jobClassId = jobClassId;
	}

	/** See {@link #occCodeId}. */
	public Integer getOccCodeId() {
		return occCodeId;
	}

	/** See {@link #occCodeId}. */
	public void setOccCodeId(Integer occCodeId) {
		this.occCodeId = occCodeId;
	}

	/** See {@link #lsOccCode}. */
	public String getLsOccCode() {
		return lsOccCode;
	}

	/** See {@link #lsOccCode}. */
	public void setLsOccCode(String lsOccCode) {
		this.lsOccCode = lsOccCode;
	}

	/** See {@link #weeklyHotCosts}. */
	public WeeklyHotCosts getWeeklyHotCosts() {
		return weeklyHotCosts;
	}

	/** See {@link #weeklyHotCosts}. */
	public void setWeeklyHotCosts(WeeklyHotCosts weeklyHotCosts) {
		this.weeklyHotCosts = weeklyHotCosts;
	}

	/** See {@link #editMode}. */
	public void setEditMode(boolean editMode) {
		this.editMode = editMode;
	}

	/** See {@link #useStandardRate}. */
	public boolean getUseStandardRate() {
		return useStandardRate;
	}

	/** See {@link #useStandardRate}. */
	public void setUseStandardRate(boolean useStandardRate) {
		this.useStandardRate = useStandardRate;
	}

	/** See {@link #useRerate}. */
	public boolean getUseRerate() {
		return useRerate;
	}

	/** See {@link #useRerate}. */
	public void setUseRerate(boolean useRerate) {
		this.useRerate = useRerate;
	}

	/** See {@link #jobClassDL}. */
	public List<SelectItem> getJobClassDL() {
		return jobClassDL;
	}

	/** See {@link #jobClassDL}. */
	public void setJobClassDL(List<SelectItem> jobClassDL) {
		this.jobClassDL = jobClassDL;
	}

	/** See {@link #occCodeDL}. */
	public List<SelectItem> getOccCodeDL() {
		return occCodeDL;
	}

	/** See {@link #occCodeDL}. */
	public void setOccCodeDL(List<SelectItem> occCodeDL) {
		this.occCodeDL = occCodeDL;
	}

	@Override
	public String actionOk() {
		dailyHotCost.setReRate(useRerate);

		if(reRate) {
			dailyHotCost.setRate(reRateAmount);
		}
		else {
			dailyHotCost.setRate(rate);
		}

		if(!editMode) {
			// If not in edit mode, save changes.
			if(weeklyHotCosts.getId() == null) {
				WeeklyHotCostsDAO.getInstance().save(weeklyHotCosts);
			}

			dailyHotCost.setWeeklyHotCosts(weeklyHotCosts);
			if(dailyHotCost.getId() == null) {
				DailyHotCostDAO.getInstance().save(dailyHotCost);
			}
			else {
				DailyHotCostDAO.getInstance().attachDirty(dailyHotCost);
			}
		}

		return super.actionOk();
	}

	@Override
	public String actionCancel() {
		return super.actionCancel();
	}

	/**
	 * Set the appropriate rate for the job class selected.
	 *
	 * @param event
	 */
	public void listenJobClassChange(ValueChangeEvent event) {
		lsOccCode = (String)event.getNewValue();

		reRateAmount = getRateByEmpRateType(lsOccCode);
	}

	/**
	 * Set the appropriate rate for the occ code selected.
	 *
	 * @param event
	 */
	public void listenOccCodeChange(ValueChangeEvent event) {
		lsOccCode = (String)event.getNewValue();

		reRateAmount = getRateByEmpRateType(lsOccCode);
	}

	/**
	 * Listen for change of radio buttons for which rate to use, the standard rate
	 * or the re-rate amount.
	 * @param event
	 */
	public void listenRateToUseChange(ValueChangeEvent event) {
		RadioButton bttn = (RadioButton)event.getSource();
		String componentId = bttn.getId();

		log.debug(componentId);
		if(componentId.equals(RE_RATE)) {
			rateToUse = RE_RATE;
			useStandardRate = false;
		}
		else {
			rateToUse = CURRENT_RATE;
			useRerate = false;
		}
	}


	/**
	 * Show the re-rate popup
	 * @param popupHolder - calling bean
	 * @param action - action to take
	 */
	public void show(PopupHolder popupHolder, StartForm sf, int action) {
		reRate = dailyHotCost.getReRate();
		isUnion = false;
		union = null;

		if (sf.getUnionKey() != null) {
			union = UnionsDAO.getInstance().findOneByProperty(UnionsDAO.UNION_KEY, sf.getUnionKey());
			if (union != null) {
				isUnion = ! union.getUnionKey().equals(Unions.NON_UNION);
			}
		}

		switch(rateType) {
			case HOURLY:
				rate = weeklyHotCosts.getHourlyRate();
				break;
			case DAILY:
				rate = weeklyHotCosts.getDailyRate();
				break;
			case WEEKLY:
				rate = weeklyHotCosts.getWeeklyRate();
				break;
			default:
				rate = Constants.DECIMAL_ZERO;
		}

		rateToUse = HotCostsReRateBean.CURRENT_RATE;
		// If this is currently a reRate set the radio button to default to reRate;
		if(reRate) {
			rateToUse = HotCostsReRateBean.RE_RATE;
		}
		createJobClassOccCodeDL(sf);
		this.show(popupHolder, action, "HotCosts.ReRate.Title", "Confirm.OK", "Confirm.Cancel");
	}

	/**
	 * Build the jot class list based on the employee's current schedule.
	 */
	private void createJobClassOccCodeDL(StartForm startForm) {
		if(isUnion) {
			occPayRate = new HashMap<>();
			jobClassDL = new ArrayList<>();
			occCodeDL = new ArrayList<>();
			String useStudioOrLoc = startForm.getUseStudioOrLoc();
			char locality = PayRate.LOCALITY_ALL;
			lsOccCode = startForm.getLsOccCode();
			reRate = dailyHotCost.getReRate();

			OccupationDAO occDAO = OccupationDAO.getInstance();

			if(useStudioOrLoc.equals(StartForm.USE_STUDIO_RATE)) {
				locality = PayRate.LOCALITY_STUDIO;
			}
			else if(useStudioOrLoc.equals(StartForm.USE_LOCATION_RATE)) {
				locality = PayRate.LOCALITY_DISTANT;
			}

			Date reRateDate = startForm.getWorkStartDate();

			if(reRateDate == null) {
				reRateDate = startForm.getHireDate();
			}
			List<PayRate> payRates = getPayRateDAO().findByContractDateScheduleLocality(startForm.getContractCode(), startForm.getWorkStartDate(), startForm.getContractSchedule(), locality);

			for(PayRate payRate : payRates) {
				occPayRate.put(payRate.getOccCode(), payRate);
				// Get the Occupation associated with this PayRate object to get the occ code.
				Occupation occ = occDAO.findOne("from Occupation where lsOccCode = ?", payRate.getOccCode());
				occCodeDL.add(new SelectItem(occ.getLsOccCode(), occ.getOccCode()));
				jobClassDL.add(new SelectItem(payRate.getOccCode(), occ.getName()));

			}
		}
	}

	private PayRateDAO getPayRateDAO() {
		if(payRateDAO == null) {
			payRateDAO = PayRateDAO.getInstance();
		}

		return payRateDAO;
	}

	/**
	 * Get the pay rate amount based on Occ Code and
	 * Employee Rate Type.
	 * @param lsOccCode
	 * @return
	 */
	public BigDecimal getRateByEmpRateType(String lsOccCode) {
		PayRate payRate = occPayRate.get(lsOccCode);
		BigDecimal empRate = Constants.DECIMAL_ZERO;

		if(rateType == EmployeeRateType.HOURLY) {
			empRate = payRate.getHourlyRate();
		}
		else if(rateType == EmployeeRateType.DAILY) {
			empRate = payRate.getDailyRate();
		}
		else if(rateType == EmployeeRateType.WEEKLY) {
			empRate = payRate.getWeeklyRate();
		}

		return empRate;
	}
}
