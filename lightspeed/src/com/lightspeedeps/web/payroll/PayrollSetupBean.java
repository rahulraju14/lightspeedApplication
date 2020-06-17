package com.lightspeedeps.web.payroll;

import java.io.Serializable;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.payroll.ContractUtils;
import com.lightspeedeps.web.util.ApplicationScopeBean;
import com.lightspeedeps.web.util.EnumList;
import com.lightspeedeps.web.validator.EmailValidator;
import com.lightspeedeps.web.view.View;

/**
 * Backing bean for the Payroll Preference page's "Setup" mini-tab. This tab
 * allows the user (typically a payroll or production accountant) to set
 * Production values and preferences related to the payroll system. These are
 * usually settings that are entered once, when a Production is first set up,
 * and not changed after that.
 */
@ManagedBean
@ViewScoped
public class PayrollSetupBean extends View implements Serializable {
	/** */
	private static final long serialVersionUID = - 6190739835038434764L;

	private static final Log log = LogFactory.getLog(PayrollSetupBean.class);

	/** The current Production. */
	private Production production;

	/** The PayrollPreference being updated; this will be project-specific
	 * for Commercial productions. */
	private PayrollPreference payrollPref;

	/** The list of Production Types to choose from, currently only used for
	 * ASA unions.  This replaces the Occ Code drop-down when an ASA union
	 * is selected. */
	private List<SelectItem> productionAsaTypeDL;

	/** List of StudioType values for selection drop-down. */
	private final List<SelectItem> studioTypeDL;

	/** List of MediumType values for selection drop-down. */
	private static final List<SelectItem> mediumTypeDL = EnumList.createEnumValueSelectList(MediumType.class);


	/** The drop-down list for the detail/contract type of the production. */
	private List<SelectItem> productionDetailTypeDL;

	/** Drop-down list for selecting the season of the television series
	 *  (1st, 2nd, or 3rd & later. */
	private List<SelectItem> televisionSeasonDL;

	/** Drop-down list for selecting which range of dates a television series started in. */
	private List<SelectItem> televisionEraDL;

	/** Drop-down list for selecting within which east-coast area the production is being shot. */
	private List<SelectItem> nyRegionDL;

	/** Drop-down for choosing which state wage laws to apply. */
	private List<SelectItem> wageRuleStateDL;

	/** value of weBatchUse set in JSP for Prefix usage */
	private static final String WE_BATCH_USE_PREFIX = "p";

	/** value of weBatchUse set in JSP for Suffix usage */
	private static final String WE_BATCH_USE_SUFFIX = "s";

	/** Value selected by the user by choosing either the 'prefix' or 'suffix' radio button. */
	private String weBatchUse = WE_BATCH_USE_PREFIX;

	/** Drop-down selection list for the "create timecard in advance" number of days. */
	private static final List<SelectItem> ADVANCE_SELECT_DL = Arrays.asList(
			new SelectItem(0, "0"),
			new SelectItem(1, "1"),
			new SelectItem(2, "2"),
			new SelectItem(3, "3"),
			new SelectItem(4, "4"),
			new SelectItem(5, "5"),
			new SelectItem(6, "6")
			);

	/** Drop-down selection list for the "create timecard max weeks in advance" number of weeks. */
	private static final List<SelectItem> WEEKS_ADVANCE_SELECT_DL = Arrays.asList(
			new SelectItem(1, "1"),
			new SelectItem(2, "2"),
			new SelectItem(3, "3"),
			new SelectItem(4, "4")
			);

	/** The List of Contract`s that have been added to the current Production. */
	private List<Contract> activeContractList;

//	/** The List of Contract`s that have NOT been added to the current Production. */
//	private List<Contract> inactiveContractList;

	/** List of HourRoundingType values for selection drop-down. */
	private final static List<SelectItem> hourRoundingTypeDL = EnumList.createEnumValueSelectList(HourRoundingType.class);

	/** True iff the Video Tape contract is assigned to this production. */
	private Boolean usesVideoTape;

	/** True iff the ASA (Area Standards Agreement) contract is assigned to this production. */
	private Boolean usesASA;

	/** True iff at least one timecard already exists for the current production (and project,
	 * if Commercial).  Used to enable/disable the "first day on timecard" selection. */
	private Boolean timecardExists;

	/** Field to save value of firstWorkWeekDay at beginning of Edit, to see if it
	 * has changed if user changes firstWeekDay. */
	private Integer oldFirstWorkWeekDay;

	/** Backing field for radio buttons (Notice Given) */
	private String noticeGivenType = DO_NOT_SPECIFY;

	public static final String DO_NOT_SPECIFY = "a";
	public static final String AT_TIME_OF_HIRE = "b";
	public static final String WITHIN_7_DAYS = "c";

	/**
	 * default constructor
	 */
	public PayrollSetupBean() {
		super("Payroll.Setup.");
		log.debug("");

		if (getProduction().getType().hasPayrollByProject()) {
			studioTypeDL = Constants.AICP_CONTRACT_DL;
		}
		else {
			studioTypeDL = EnumList.createEnumValueSelectList(StudioType.class);
		}

		if (production.getPayrollPref().getMailingAddress() == null) {
			// prevent error trying to display address fields
			production.getPayrollPref().setMailingAddress(new Address());
		}

		initBatchUse();

		forceLazyInit();
	}

	/**
	 * The action method for the "Save" button. Saves the current production
	 * preference settings.
	 *
	 * @see com.lightspeedeps.web.view.View#actionSave()
	 * @return null navigation string
	 */
	@Override
	public String actionSave() {
		try {
			SessionUtils.put(Constants.ATTR_TC_WEEK_END_DAY, null);
			if (payrollPref != null) {
				if (payrollPref.getMileageRate() != null) {
					payrollPref.setMileageRate(payrollPref.getMileageRate().setScale(3, RoundingMode.HALF_UP));
					if (payrollPref.getMileageRate().compareTo(Constants.DECIMAL_10) >= 0) {
						MsgUtils.addFacesMessage("Payroll.MileageRate.TooHigh", FacesMessage.SEVERITY_ERROR);
						return null;
					}
					if (payrollPref.getMileageRate().signum() < 0) {
						MsgUtils.addFacesMessage("Payroll.MileageRate.Negative", FacesMessage.SEVERITY_ERROR);
						return null;
					}
				}
				if (payrollPref.getAccountMajor() != null) {
					payrollPref.setAccountMajor(payrollPref.getAccountMajor().trim());
				}
				if (payrollPref.getWorkCity() != null) {
					payrollPref.setWorkCity(payrollPref.getWorkCity().trim());
				}
				if (payrollPref.getWorkState() != null) {
					payrollPref.setWorkState(payrollPref.getWorkState().trim());
				}
				if (payrollPref.getWorkZip() != null) { // LS-2343
					payrollPref.setWorkZip(payrollPref.getWorkZip().trim());
				}
				// LS-2159  Add Work Country in Payroll Preference
				if (payrollPref.getWorkCountry() != null) {
					payrollPref.setWorkCountry(payrollPref.getWorkCountry().trim());
				}
				if (payrollPref.getOvertimeRule() != null) {
					payrollPref.setOvertimeRule(payrollPref.getOvertimeRule().trim());
				}
				if (StringUtils.isEmpty(payrollPref.getBatchEmailAddress())) {
					payrollPref.setUseEmail(false);
					payrollPref.setBatchEmailAddress(null);
				}
				else {
					payrollPref.setBatchEmailAddress((payrollPref.getBatchEmailAddress().trim()));
					if (! EmailValidator.isValidEmail(payrollPref.getBatchEmailAddress())) {
						MsgUtils.addFacesMessage("Payroll.BatchEmail.Invalid", FacesMessage.SEVERITY_ERROR);
						return null;
					}
				}

				payrollPref.setUseWeAsPrefix(weBatchUse.equals(WE_BATCH_USE_PREFIX));

				switch (noticeGivenType) {
					case DO_NOT_SPECIFY:
						payrollPref.setNoticeGivenAtHire(null);
						production.getPayrollPref().setNoticeGivenAtHire(null);
						break;
					case AT_TIME_OF_HIRE:
						payrollPref.setNoticeGivenAtHire(true);
						production.getPayrollPref().setNoticeGivenAtHire(true);
						break;
					case WITHIN_7_DAYS:
						payrollPref.setNoticeGivenAtHire(false);
						production.getPayrollPref().setNoticeGivenAtHire(false);
						break;
				}

				// Clear the paydayFreqOther field if not relevant.
				if (production.getPayrollPref().getPaydayFreq() != PaydayFrequency.O) {
					production.getPayrollPref().setPaydayFreqOther(null);
				}

				// Clear the SickLeaveReason field if not exempt.
				if (production.getPayrollPref().getSickLeaveType() != CalifSickLeaveType.EXEMPT) {
					production.getPayrollPref().setSickLeaveReason(null);
				}

				// Save both project and production payroll preference instances,
				// as some fields in JSP reference the production instance of payroll preferences.
				payrollPref = PayrollPreferenceDAO.getInstance().merge(payrollPref);
				PayrollPreferenceDAO.getInstance().merge(production.getPayrollPref());
				production = ProductionDAO.getInstance().refresh(production);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}

		return super.actionSave();
	}

	/**
	 * The action method for the "Edit" button. Used to initialize mailing address if it is null.
	 *
	 * @see com.lightspeedeps.web.view.View#actionEdit()
	 * @return null navigation string
	 */
	@Override
	public String actionEdit() {
		try {
			production = ProductionDAO.getInstance().refresh(getProduction());
			payrollPref = PayrollPreferenceDAO.getInstance().refresh(payrollPref);
			if (production.getPayrollPref().getMailingAddress() == null) {
				production.getPayrollPref().setMailingAddress(new Address());
			}
			oldFirstWorkWeekDay = payrollPref.getFirstWorkWeekDay();
			forceLazyInit();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return super.actionEdit();
	}

	/**
	 * The Action method for Cancel button while in Edit mode. Cleans up the
	 * state of the Production and WeeklyTimecard, and calls our superclass'
	 * actionCancel() method.
	 *
	 * @return null navigation string
	 */
	@Override
	public String actionCancel() {
		log.debug("");
		try {
			super.actionCancel();
			if (getProduction() != null) {
				production = ProductionDAO.getInstance().refresh(production);
				payrollPref = null; // force refresh
				payrollPref = getPayrollPref();
				initBatchUse();
			}
			forceLazyInit();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Set our W/E batch field for the radio buttons.
	 */
	private void initBatchUse() {
		if (getProduction() != null) {
			if (getPayrollPref().getUseWeAsPrefix()) {
				setWeBatchUse(WE_BATCH_USE_PREFIX);
			}
			else {
				setWeBatchUse(WE_BATCH_USE_SUFFIX);
			}
		}
	}

	/**
	 * Ensure any fields necessary for re-rendering later are loaded
	 * while our entities are still in the Hibernate session.
	 */
	private void forceLazyInit() {
		if (getPayrollService() != null) {
			getPayrollService().getName();
			getPayrollService().getBatchEmailAddress();
		}
		if (getProduction().getType().hasPayrollByProject()) {
			production.getPayrollPref().getFedidNumber();
		}
	}

	/**
	 * valueChangeListener for the "email batch data" check-box.
	 *
	 * @param event supplied by the framework.
	 */
	public void listenBatchEmail(ValueChangeEvent event) {
		if (event != null) {
			boolean check = (Boolean)event.getNewValue();
			if (check && StringUtils.isEmpty(payrollPref.getBatchEmailAddress())) {
				if (getPayrollService() != null) {
					payrollPref.setBatchEmailAddress(getPayrollService().getBatchEmailAddress());
				}
			}
		}
	}

	/**
	 * valueChangeListener for the "email Onboard data" check-box.
	 *
	 * @param event supplied by the framework.
	 */
	public void listenOnboardEmail(ValueChangeEvent event) {
		if (event != null) {
			boolean check = (Boolean)event.getNewValue();
			if (check && StringUtils.isEmpty(payrollPref.getOnboardEmailAddress())) {
				if (getPayrollService() != null) {
					payrollPref.setOnboardEmailAddress(getPayrollService().getBatchEmailAddress());
				}
			}
		}
	}

	/**
	 * valueChangeListener for the studio-type radio-button
	 * selection.
	 *
	 * @param event supplied by the framework.
	 */
	public void listenChangeStudio(ValueChangeEvent event) {
		productionDetailTypeDL = null; // force it to refresh
	}

	/**
	 * valueChangeListener for the Medium (production type)
	 * radio button selection.
	 *
	 * @param event supplied by the framework.
	 */
	public void listenChangeMedium(ValueChangeEvent event) {
		productionDetailTypeDL = null; // force it to refresh
		if (payrollPref.getMediumType() == MediumType.FT) {
			payrollPref.setDetailType(PayrollPreference.DETAIL_TYPE_BASIC);
			if (payrollPref.getAsaContract() == null) {
				payrollPref.setAsaContract(PayrollProductionType.ASA_THEATRICAL);
			}
		}
		else {
			payrollPref.setDetailType(PayrollPreference.DETAIL_TYPE_ONE_HOUR_SERIES);
		}
	}

	/**
	 * valueChangeListener for the production detail (contract) drop-down
	 * selection list.
	 *
	 * @param event supplied by the framework.
	 */
	public void listenChangeDetailType(ValueChangeEvent event) {
		if (payrollPref.getMediumType() == MediumType.TV) {
			if (payrollPref.getDetailType() != null) {
				if (payrollPref.getDetailType().equals(PayrollPreference.DETAIL_TYPE_LONG_FORM) ||
						payrollPref.getDetailType().equals(PayrollPreference.DETAIL_TYPE_PILOT)) {
					if (payrollPref.getAsaContract() == null) {
						// ASA uses same for Pilot, Long-form, MOW, or 1st year of 1-hour series
						payrollPref.setAsaContract(PayrollProductionType.ASA_PILOT);
					}
				}
				else if (payrollPref.getDetailType().equals(PayrollPreference.DETAIL_TYPE_OTHER)) {
					if (payrollPref.getAsaContract() == null) {
						payrollPref.setAsaContract(PayrollProductionType.ASA_OTHER);
					}
				}
				else { // 1-hour series
					if (payrollPref.getTvSeason() == null) {
						payrollPref.setTvSeason(PayrollPreference.SEASON_1);
					}
				}
			}
		}
	}

	/**
	 * valueChangeListener for the television season drop-down
	 * selection list.
	 *
	 * @param event supplied by the framework.
	 */
	public void listenChangeSeason(ValueChangeEvent event) {
		if (payrollPref.getMediumType() == MediumType.TV) {
			if (payrollPref.getDetailType().equals(PayrollPreference.DETAIL_TYPE_ONE_HOUR_SERIES)) {
				if (payrollPref.getTvSeason() != null &&
						payrollPref.getTvSeason().equals(PayrollPreference.SEASON_1)) {
					if (payrollPref.getAsaContract() == null) {
						// ASA uses same for Pilot, Long-form, MOW, or 1st year of 1-hour series
						payrollPref.setAsaContract(PayrollProductionType.ASA_PILOT);
					}
				}
				else {
					if (payrollPref.getAsaContract() == null) {
						payrollPref.setAsaContract(PayrollProductionType.ASA_OTHER);
					}
				}
			}
		}
	}

	/**
	 * Called when user changes the drop-down selection for the 'First day on
	 * timecard'. We will adjust the 'First day of production week' to match if
	 * it's different. Should be setup in xhtml to be called during the Invoke
	 * Application phase.
	 *
	 * @param event supplied by the framework.
	 */
	public void listenChangeWeekFirstDay(ValueChangeEvent event) {
		if (event != null) {
			if (payrollPref.getFirstWorkWeekDay() != payrollPref.getWeekFirstDay() &&
					oldFirstWorkWeekDay == payrollPref.getFirstWorkWeekDay()) {
				payrollPref.setFirstWorkWeekDay(payrollPref.getWeekFirstDay());
				oldFirstWorkWeekDay = payrollPref.getFirstWorkWeekDay();
				MsgUtils.addFacesMessage("Payroll.ProdWeekChanged", FacesMessage.SEVERITY_INFO);
			}
		}
	}

	/**
	 * @return True if this production has the ASA contract assigned.
	 */
	private Boolean calculateUsesASA() {
		List<Contract> list = ContractUtils.getActiveContractList(production);
		boolean res = false;
		for (Contract c : list) {
			if (c.getUnionKey().equals("ASA")) {
				res = true;
				break;
			}
		}
		return res;
	}

//	/**
//	 * Create sorted list of Contract`s that have NOT been assigned to the
//	 * current Production. Sets {@link #inactiveContractList}.
//	 */
//	private void createInactiveContractList() {
//		ContractType type;
//		if (production.getType().isAicp()) {
//			type = ContractType.AGC;
//		}
//		else {
//			type = ContractType.AGTF;
//		}
//		List<Contract> list = ContractDAO.getInstance().findByType(type);
//		for (Contract c : getActiveContractList()) {
//			list.remove(c);
//		}
//		inactiveContractList = new ArrayList<>();
//		inactiveContractList.addAll(list);
//	}


	/**
	 * Creates the appropriate drop-down list for selecting the
	 * detailed production type or contract type.
	 *
	 * @return A non-null (and non-empty!) selection list.
	 */
	private List<SelectItem> createProductionDetailTypeDL() {
		List<SelectItem> list;
		if (payrollPref.getMediumType() == MediumType.FT) {
			list =  SelectionItemDAO.getInstance().createDLbyType(
					SelectionItem.FEATURE_PRODUCTION_TYPE);
//			if (production.getStudioType() == StudioType.IN) {
//				list =  SelectionItemDAO.getInstance().createDLbyType(
//						SelectionItem.INDIE_FEATURE_PRODUCTION_TYPE);
//			}
//			else {
//				list =  SelectionItemDAO.getInstance().createDLbyType(
//						SelectionItem.MAJOR_FEATURE_PRODUCTION_TYPE);
//			}
		}
		else {
			list =  SelectionItemDAO.getInstance().createDLbyType(
					SelectionItem.TV_PRODUCTION_TYPE);
		}
		return list;
	}

	/**
	 * Create a list of PayrollProductionType for the user to choose
	 * from.  This is currently only displayed if the user has selected
	 * an ASA-type union.
	 * @return The list for the "Type" drop-down, which replaces the
	 * OccCode drop-down.
	 */
	private List<SelectItem> createProductionAsaTypeDL() {
		List<SelectItem> list = EnumList.createEnumSelectList(PayrollProductionType.class);
		list.add(0, new SelectItem(null, "(select contract)"));
		return list;
	}

	// accessors and mutators

	/**See {@link #production}. */
	public Production getProduction() {
		if (production == null) {
			production = SessionUtils.getProduction();
		}
		// TODO what is this for?
		if (production.getPayrollPref().getMailingAddress() == null) {
			production.getPayrollPref().setMailingAddress(new Address());
		}
		return production;
	}

	/** See {@link #payrollPref}. */
	public PayrollPreference getPayrollPref() {
		if (payrollPref == null) {
			if (getProduction().getType().hasPayrollByProject()) {
				payrollPref = SessionUtils.getCurrentProject().getPayrollPref();
			}
			else {
				payrollPref = getProduction().getPayrollPref();
			}

			if (getProduction().getPayrollPref().getNoticeGivenAtHire() == null) {
				noticeGivenType = DO_NOT_SPECIFY;
			}
			else if (getProduction().getPayrollPref().getNoticeGivenAtHire()) {
				noticeGivenType = AT_TIME_OF_HIRE;
			}
			else if (! getProduction().getPayrollPref().getNoticeGivenAtHire()) {
				noticeGivenType = WITHIN_7_DAYS;
			}
		}
		return payrollPref;
	}

	/**
	 * @return the PayrollService specified in the current production's
	 *         PayrollPreference object.
	 */
	public PayrollService getPayrollService() {
		return getPayrollPref().getPayrollService();
	}

	/** List of states for the selected country. */
	public List<SelectItem> getStateCodePreferencesDL() {
		ApplicationScopeBean bean = ApplicationScopeBean.getInstance();
		List<SelectItem> stateList = new ArrayList<>();
		if (getPayrollPref() != null && getPayrollPref().getWorkCountry() != null) {
			stateList = bean.getStateCodeDL(getPayrollPref().getWorkCountry());
		}
		else {
			stateList = bean.getStateCodeProdDL();
		}
		return stateList;
	}
	/** See {@link #timecardExists}. */
	public Boolean getTimecardExists() {
		if (timecardExists == null) {
			timecardExists = true; // the safer default
			if (getProduction() != null) {
				Project proj = null;
				if (getProduction().getType().hasPayrollByProject()) {
					proj = SessionUtils.getCurrentProject();
				}
				timecardExists = WeeklyTimecardDAO.getInstance().existsForProduction(getProduction(), proj);
			}
		}
		return timecardExists;
	}

	/** See {@link #timecardExists}. */
	public void setTimecardExists(Boolean timecardExists) {
		this.timecardExists = timecardExists;
	}

	/**See {@link #weBatchUse}. */
	public String getWeBatchUse() {
		return weBatchUse;
	}
	/**See {@link #weBatchUse}. */
	public void setWeBatchUse(String weBatchUse) {
		this.weBatchUse = weBatchUse;
	}

	/** See {@link #usesVideoTape}. */
	public Boolean getUsesVideoTape() {
		if (usesVideoTape == null) {
			usesVideoTape = ContractUtils.calculateUsesVideoTape(production);
		}
		return usesVideoTape;
	}

	/** See {@link #usesVideoTape}. */
	public void setUsesVideoTape(Boolean usesVideoTape) {
		this.usesVideoTape = usesVideoTape;
	}

	/** See {@link #usesASA}. */
	public Boolean getUsesASA() {
		if (usesASA == null) {
			usesASA = calculateUsesASA();
		}
		return usesASA;
	}
	/** See {@link #usesASA}. */
	public void setUsesASA(Boolean usesASA) {
		this.usesASA = usesASA;
	}

	/**See {@link #studioTypeDL}. */
	public List<SelectItem> getStudioTypeDL() {
		return studioTypeDL;
	}

	/**See {@link #mediumTypeDL}. */
	public List<SelectItem> getMediumTypeDL() {
		return mediumTypeDL;
	}

	/**See {@link #productionDetailTypeDL}. */
	public List<SelectItem> getProductionDetailTypeDL() {
		if (productionDetailTypeDL == null) {
			productionDetailTypeDL =  createProductionDetailTypeDL();
		}
		return productionDetailTypeDL;
	}

	/**See {@link #televisionSeasonDL}. */
	public List<SelectItem> getTelevisionSeasonDL() {
		if (televisionSeasonDL == null) {
			televisionSeasonDL = SelectionItemDAO.getInstance().createDLbyType(
					SelectionItem.TV_PRODUCTION_SEASON);
		}
		return televisionSeasonDL;
	}

	/**See {@link #televisionEraDL}. */
	public List<SelectItem> getTelevisionEraDL() {
		if (televisionEraDL == null) {
			televisionEraDL = SelectionItemDAO.getInstance().createDLbyType(
					SelectionItem.TV_PRODUCTION_START_DATE);
		}
		return televisionEraDL;
	}

	/**See {@link #nyRegionDL}. */
	public List<SelectItem> getNyRegionDL() {
		if (nyRegionDL == null) {
			nyRegionDL = SelectionItemDAO.getInstance().createDLbyType(SelectionItem.NEW_YORK_REGION);
		}
		return nyRegionDL;
	}

	/**See {@link #productionAsaTypeDL}. */
	public List<SelectItem> getProductionAsaTypeDL() {
		if (productionAsaTypeDL == null) {
			productionAsaTypeDL = createProductionAsaTypeDL();
		}
		return productionAsaTypeDL;
	}

	/**See {@link #hourRoundingTypeDL}. */
	public List<SelectItem> getHourRoundingTypeDL() {
		return hourRoundingTypeDL;
	}

	/**See {@link #ADVANCE_SELECT_DL}. */
	public List<SelectItem> getAdvanceSelectDL() {
		return ADVANCE_SELECT_DL;
	}

	/**See {@link #ADVANCE_SELECT_DL}. */
	public List<SelectItem> getWeeksAdvanceSelectDL() {
		return WEEKS_ADVANCE_SELECT_DL;
	}

	/** See {@link #activeContractList}. */
	public List<Contract> getActiveContractList() {
		if (activeContractList == null) {
			activeContractList = ContractUtils.createActiveContractList(production);
		}
		return activeContractList;
	}

//	/** See {@link #inactiveContractList}. */
//	public List<Contract> getInactiveContractList() {
//		if (inactiveContractList == null) {
//			createInactiveContractList();
//		}
//		return inactiveContractList;
//	}

	/**
	 * @return the drop-down selection list of week-day names, used for picking:
	 *         <ul>
	 *         <li>the first work-day of the production week.</li>
	 *         <li>the regular pay-day day of the week (WTPA)</li>
	 *         <li>the end-of-week week day for Tours productions</li>
	 *         </ul>
	 */
	public List<SelectItem> getWeekDayNameDl() {
		return Constants.WEEKDAY_NAMES_DL;
	}

	/** See {@link #wageRuleStateDL}. */
	public List<SelectItem> getWageRuleStateDL() {
		if (wageRuleStateDL == null) {
			if (! editMode) {
				production = ProductionDAO.getInstance().refresh(getProduction());
			}
			wageRuleStateDL = ApplicationScopeBean.getInstance().getStateCodeWorkedDL(getProduction());
		}
		return wageRuleStateDL;
	}

	/**See {@link #regularPayDayWeekDayList}. */
	public List<SelectItem> getRegularPayDayWeekDayList() {
		return Constants.WEEKDAY_NAMES_PLUS_BLANK_DL;
	}

	/**See {@link #noticeGivenType}. */
	public String getNoticeGivenType() {
		return noticeGivenType;
	}
	/**See {@link #noticeGivenType}. */
	public void setNoticeGivenType(String noticeGivenType) {
		this.noticeGivenType = noticeGivenType;
	}

}
