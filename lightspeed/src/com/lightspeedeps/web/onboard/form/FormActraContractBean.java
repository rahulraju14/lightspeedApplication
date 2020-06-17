/**
 * File: FormActraContractBean.java
 */
package com.lightspeedeps.web.onboard.form;

import java.util.*;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.ActraContractTimesheet;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.payroll.TimecardUtils;
import com.lightspeedeps.web.onboard.ContactFormBean;
import com.lightspeedeps.web.popup.PopupBean;

/**
 * Backing bean for the ACTRA Commercial Engagement Contract form on the Payroll / Start Forms page.
 * @see com.lightspeedeps.model.FormActraContract
 * @see StandardFormBean
 * @see com.lightspeedeps.web.onboard.ContactFormBean
 */
@ManagedBean
@ViewScoped
public class FormActraContractBean extends StandardFormBean<FormActraContract> {
	private static final long serialVersionUID = - 2203713022780468663L;

	private static final Log LOG = LogFactory.getLog(FormActraContractBean.class);

	// DAO classes
	private transient FormActraContractDAO formActraContractDAO;
//	private transient EmploymentDAO employmentDAO;
//	private transient UserDAO userDAO;

//	private String callType = "";

	/** backing field for the "Add more info" checkbox on the form. */
	private boolean addMoreInfo = false;

	/** List of selections for the call-type information in the work-time section. */
	private List<SelectItem> callTypeList;

	/** Array for Substantial Snack Yes & No radio buttons LS-2476 */
	private Boolean[] ndm = new Boolean[2];

	/** */
	private List<ActraContractTimesheet> actraTimesheetList;

	/** */
	public static FormActraContractBean getInstance() {
		return (FormActraContractBean)ServiceFinder.findBean("formActraContractBean");
	}
	/** Select item list of office to send documents to. Current just used for Canada */
	private List<SelectItem>officeListDL;

	private transient OfficeDAO officeDAO;

	/**
	 * default constructor
	 */
	public FormActraContractBean() {
		super("FormActraContractBean");
		resetBooleans();
		// form = new FormActraContract();
	}

	/**
	 * Set all our backing boolean arrays to all False.
	 */
	private void resetBooleans() {
		Arrays.fill(ndm, Boolean.FALSE);
	}

	/**
	 * Set the boolean array for ndm (Substantial Snack) radio buttons
	 * LS-2476
	 */
	private void setUpBooleans() {
		if (form != null) {
			fillArray(ndm, form.getNdm());
		}
	}

	/** Sets the value of boolean fields chosen from their given radio buttons on the form.
	 * @param radioButton Boolean Array used for the field. LS-2476
	 * @return
	 */
	private Boolean setChosenValue(Boolean[] radioButton) {
		if (radioButton != null) {
			if (radioButton[0]) {
				return true;
			}
			else if (radioButton[1]) {
				return false;
			}
		}
		// If none of them is selected.
		return null;
	}

	/** Utility method used to set the value of radio button for the form field.
	 * @param booleanArray Array of Boolean used for the Form field. LS-2476
	 * @param formField
	 */
	private void fillArray(Boolean[] booleanArray, Boolean formField) {
		if (formField == null) {
			booleanArray[0] = false;
			booleanArray[1] = false;
		}
		else if (formField) {
			booleanArray[0] = true;
			booleanArray[1] = false;
		}
		else if (! formField) {
			booleanArray[0] = false;
			booleanArray[1] = true;
		}
	}

	@Override
	public String actionCancel() {
		refreshForm();
		setUpBooleans();
		super.actionCancel();

		return null;
	}

	@Override
	public String actionEdit() {
		if (form.getAgencyAddress() == null) {
			form.setAgencyAddress(new Address(true));
		}
		if (form.getProdHouseAddress() == null) {
			form.setProdHouseAddress(new Address(true));
		}
		if (form.getTalentAddress() == null) {
			form.setTalentAddress(new Address(true));
		}
		forceLazyInit();
		calculateEditFlags(true);
		return super.actionEdit();
	}

	/**
	 *
	 */
	private void forceLazyInit() {
		WeeklyTimecard wtc = form.getWeeklyTimecard();
		wtc.getDailyTimes().size();
		form.getAgencyAddress().getAddrLine1();
		form.getProdHouseAddress().getAddrLine1();
		form.getTalentAddress().getAddrLine1();
		if(form.getOffice() != null) {
			form.getOffice().getEmailAddress();
		}
	}

	@Override
	public String actionSave() {
		// If this is a new form, create the timecard for the contract.
		Date todaysDate = CalendarUtils.todaysDate();
		//contactDoc = getContactDocumentDAO().refresh(contactDoc);
		//contactDoc.getEmpSignature();
		//contactDoc.getEmployerSignature();
		if(form.getId() == null) {
			form.setCreatedBy(getvUser());
			form.setCreatedDate(todaysDate);
			form.setUpdatedBy(getvUser());
			form.setUpdatedDate(todaysDate);
		}
		else {
			// Update form
			form.setUpdatedBy(getvUser());
			form.setUpdatedDate(todaysDate);
		}

		form.setNdm(setChosenValue(ndm));
		WeeklyTimecard wtc = form.getWeeklyTimecard();
		Iterator<ActraContractTimesheet> iter = actraTimesheetList.iterator();
		// LS-2092
		boolean bRet = true;

		for (DailyTime dt : wtc.getDailyTimes()) {
			ActraContractTimesheet entry;
			if (iter.hasNext()) {
				entry = iter.next();
				// LS-2092 validate date1 or date2
				bRet = validateTimeCardEntry(entry, bRet);
			}
			else {
				entry = null;
			}
			if (entry != null) { // LS-2213. Remove date validation since it was keeping the time entries from being removed.
				dt.setDate(entry.getDate1());
				dt.setTrvlToLocFrom(entry.getTravelFrom1());
				dt.setTrvlToLocTo(entry.getTravelTo1());
				dt.setCallTime(entry.getCallTime1());
				dt.setM1Out(entry.getM1Out());
				dt.setM1In(entry.getM1In());
				dt.setWrap(entry.getFinishTime1());
				dt.setTrvlFromLocTo(entry.getTravelTo2());
				dt.setTrvlFromLocFrom(entry.getTravelFrom2());
				dt.setCallType(entry.getCallType());
				dt.setDate2(entry.getDate2());
				dt.setCallTime2(entry.getCallTime2());
				dt.setWrap2(entry.getFinishTime2());
			}
			if (form.hasMoreInfo() || addMoreInfo) {
				form.setVersion(FormActraContract.ACTRA_CONTRACT_LONG_VERSION_2018);
			}
			else {
				form.setVersion(FormActraContract.DEFAULT_ACTRA_CONTRACT_VERSION);
			}
	     }
		if (! bRet) {
			// LS-2092 returns invalid if date1 or date 2 is missing
			return Constants.ERROR_RETURN;
		}
		else {
			WeeklyTimecardDAO.getInstance().merge(wtc);
			getFormActraContractDAO().merge(form);
			return super.actionSave();
		}
	}

	/**
	 * Method to validate date1 and date2 on Actra TimeSheet - LS-2092
	 *
	 * @param entry
	 * @param bRet
	 * @return
	 */
	public Boolean validateTimeCardEntry(ActraContractTimesheet entry, boolean bRet) {
		if (entry.getDate1() == null && (entry.getCallTime1() != null ||
				entry.getTravelFrom1() != null || entry.getTravelTo1() != null ||
				entry.getTravelFrom2() != null || entry.getFinishTime1() != null ||
				entry.getTravelTo2() != null ||
				entry.getM1In() != null || entry.getM1Out() != null)) {
			PopupBean.getInstance().show(this, 0,
					"FormActraContractBean.MissingDate.Title",
					"FormActraContractBean.MissingDate.Text", "Confirm.OK", null);
			bRet = false;
		}
		else if (entry.getDate2() == null &&
				(entry.getCallTime2() != null || entry.getFinishTime2() != null)) {
			PopupBean.getInstance().show(this, 0,
					"FormActraContractBean.MissingDate.Title",
					"FormActraContractBean.MissingDate.Text", "Confirm.OK", null);
			bRet = false;
		}
		return bRet;
	}
	/**
	 * @return The AGREE or DISAGREE event types, based on the currentAction.
	 *
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#calculateInitialEvent(int)
	 */
	@Override
	public TimedEventType calculateInitialEvent(int currentAction) {
		TimedEventType event = null;
		if (currentAction == ContactFormBean.ACT_EMPLOYEE_INITIAL) {
			event = TimedEventType.AGREE;
		}
		else if (currentAction == ContactFormBean.ACT_EMPLOYEE_INITIAL_DISAGREE) {
			event = TimedEventType.DISAGREE;
		}
		else {
			event = super.calculateInitialEvent(currentAction);
		}
		return event;
	}

	/**
	 * Called to verify that a form is ready to be signed by the employer.
	 *
	 * @return True if the signature operation should be allowed to continue (by
	 *         prompting for password & PIN). False if the operation should not
	 *         continue -- in this case an error message should have been
	 *         issued.
	 */
	@Override
	public boolean checkEmployerSignValid() {
		boolean isValid = true;
		//Contract # field is no longer mandatory LS-1939
		/*if (StringUtils.isEmpty(form.getContractNum())){
			isValid = issueErrorMessage("Contract Number", false, "");
		}*/
		if (form.getOffice() == null || StringUtils.isEmpty(form.getOffice().getBranchCode())) {
			isValid = issueErrorMessage("Branch Code", false, "");
		}
		if (StringUtils.isEmpty(form.getAgencyName())) {
			isValid = issueErrorMessage("Agency Name", false, "");
		}
		if (StringUtils.isEmpty(form.getAgencyProducer())) {
			isValid = issueErrorMessage("Agency Producer", false, "");
		}

		isValid &= checkAddressValidMsg(form.getAgencyAddress());

		if (StringUtils.isEmpty(form.getAdvertiserName())) {
			isValid = issueErrorMessage("Advertiser Name", false, "");
		}
		if (StringUtils.isEmpty(form.getProductName())) {
			isValid = issueErrorMessage("Product Name", false, "");
		}
		if (StringUtils.isEmpty(form.getProdHouseName())) {
			isValid = issueErrorMessage("Production House Name", false, "");
		}
		//Production address # field is no longer mandatory LS-1939
		//isValid &= checkAddressValidMsg(form.getProdHouseAddress());

		//No longer used for Producer's signature - LS-1998
		//isValid &= checkAddressValidMsg(form.getTalentAddress());

		//performer telephone number # field is no longer mandatory LS-1939
		/*if (StringUtils.isEmpty(form.getTalentPhoneNum())) {
			isValid = issueErrorMessage("Talent Phone Number", false, "");
		}*/
		if (StringUtils.isEmpty(form.getTalentEmailAddress())) {
			isValid = issueErrorMessage("Talent Email Address", false, "");
		}
		//GST/HST & QST # field is no longer mandatory LS-1939
		/*if (StringUtils.isEmpty(form.getGstHst())) {
			isValid = issueErrorMessage("GST/HST#", false, "");
		}
		if (StringUtils.isEmpty(form.getQst())) {
			isValid = issueErrorMessage("QST#", false, "");
		}*/
		if (StringUtils.isEmpty(form.getPerformanceCategory())) {
			isValid = issueErrorMessage("Performance Category", false, "");
		}
		if (StringUtils.isEmpty(form.getCommercialName())) {
			isValid = issueErrorMessage("Commercial Names/ Numbers/ Length", false, "");
		}
		if (StringUtils.isEmpty(form.getSessionFees())) {
			isValid = issueErrorMessage("Session Fees", false, "");
		}
		if (StringUtils.isEmpty(form.getResidualFees())) {
			isValid = issueErrorMessage("Residual Fees", false, "");
		}
		if (StringUtils.isEmpty(form.getOtherFees())) {
			isValid = issueErrorMessage("Other Fees", false, "");
		}
		setEmployerSignValid(isValid);
		return super.checkEmployerSignValid();
	}

	/** Method used to check the validity of fields in the form.
	 * @return isValid
	 */
	@Override
	public boolean checkSubmitValid() {
		boolean isValid = true;
		if (StringUtils.isEmpty(form.getSocialInsuranceNum())){
			isValid = issueErrorMessage("Social Insurance Number", false, "");
		}

		//#full member number #apprenticenumber #permit number # field is no longer mandatory
		/*if (StringUtils.isEmpty(form.getFullMemberNum())) {
			isValid = issueErrorMessage("Full Member Num", false, "");
		}
		if (StringUtils.isEmpty(form.getApprenticeNum())) {
			isValid = issueErrorMessage("Apprentice Num", false, "");
		}
		if (StringUtils.isEmpty(form.getWorkPermitNum())) {
			isValid = issueErrorMessage("Work Permit Num", false, "");
		}*/
		isValid &= isValidSsn(form.getSocialInsuranceNum());
		//LS-1998 The Performer's Address fields is require for Performer's Signature
		isValid &= checkAddressValidMsg(form.getTalentAddress());
		setSubmitValid(isValid);
		return super.checkSubmitValid();
	}

	/**
	 * Handle the employer signature on the ACTRA form, which may happen either
	 * before or after the employee signs ("Submits") the form.
	 *
	 * @param action The dialog box action currently being processed. This may
	 *            be used to distinguish, for example, between a signature and
	 *            an initial event.
	 *
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#employerSign()
	 */
	@Override
	public void employerSign(int action) {
		if (action == ContactFormBean.ACT_EMPLOYER_SIGN) {
			if (contactDoc.getStatus() == ApprovalStatus.PENDING || contactDoc.getSubmitable()) {
				LOG.debug("producer/employer is signing before the talent");
				ContactDocEventDAO.getInstance().createEvent(contactDoc, TimedEventType.SIGN);
			}
			else if (contactDoc.getStatus() == ApprovalStatus.SUBMITTED) {
				ContactDocEventDAO.getInstance().createEvent(contactDoc, TimedEventType.SIGN);
			}
		}
		else if (action == ContactFormBean.ACT_EMPLOYER_INITIAL) {
			// this is the producer/approver/employer initialing the bottom of Section B, which
			// completes the contract.
			contactDoc.setApproverId(null);
			contactDoc.setStatus(ApprovalStatus.APPROVED);
			ContactDocEventDAO.getInstance().createEvent(contactDoc, TimedEventType.APPROVE);
		}
	}

	@Override
	public void rowClicked(ContactDocument contactDocument) {
		LOG.debug("FormActractContractBean rowClicked");
		setContactDoc(contactDocument);
		setUpForm();
	}

	/** Method used to fetch the saved data for the selected form and to set that data in the Form instance. */
	private void setUpForm() {
		try {
			LOG.debug("");
			SessionUtils.put(Constants.ATTR_TIME_ROUNDING_TYPE, HourRoundingType.N_A); // LS-2116 Don't round times.
			addMoreInfo = false;
			//contactDoc = getContactDocumentDAO().refresh(contactDoc);
			Integer relatedFormId = contactDoc.getRelatedFormId();
			if (relatedFormId != null) {
				LOG.debug("");
				setForm(getFormActraContractDAO().findById(relatedFormId));
				if (form.getWeeklyTimecard().getDailyTimes().get(4).getDate() != null) {
					addMoreInfo = true;
				}
			}
			else {
				LOG.debug("");
				setForm(getBlankForm());
			}
			callTypeList = null; // so it will be refreshed; only needed if list is dynamic

			setUpBooleans(); // LS-2476

			if (form.getSpecialProvisions() != null && ! form.getSpecialProvisions().isEmpty() ||
					form.getVersion().equals(FormActraContract.ACTRA_CONTRACT_LONG_VERSION_2018)) {
				addMoreInfo = true;
			}
			actraTimesheetList = createTimesheet(addMoreInfo, false);

			if (form.getAgencyAddress() == null) {
				form.setAgencyAddress(new Address(true));
			}
			if (form.getProdHouseAddress() == null) {
				form.setProdHouseAddress(new Address(true));
			}
			if (form.getTalentAddress() == null) {
				form.setTalentAddress(new Address(true));
			}

			// Fix LIE switching between mini-tabs. r9127
			form.getAgencyAddress().getAddrLine1();
			form.getProdHouseAddress().getAddrLine1();
			form.getTalentAddress().getAddrLine1();
			if (relatedFormId == null) {
				LOG.debug("");
				getFormActraContractDAO().save(form);
				contactDoc.setRelatedFormId(form.getId());
				ContactDocumentDAO.getInstance().merge(contactDoc);
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Creates Timecard entry list for the ACTRA Contract form.
	 * @param moreInfo used to indicate the type of form(Long ACTRA form or Short ACTRA form),
	 * which is used to create the specific number of rows.
	 * @param isUpdated, indicates whether to update the Timecard rows or not.
	 * @return the updated or new list of Actra Timecard entries.
	 */
	private List<ActraContractTimesheet> createTimesheet(Boolean moreInfo, boolean isUpdated) {
		List<ActraContractTimesheet> list = new ArrayList<>();
		if (isUpdated) {
			LOG.debug("");
			list.addAll(actraTimesheetList);
		}
		boolean changedToLong = (isUpdated && moreInfo);
		int i = 1;
		if ((! isUpdated) || changedToLong) {
			LOG.debug("");
			ActraCallType callType;
			if (form.getWeeklyTimecard() != null && form.getWeeklyTimecard().getDailyTimes() != null) {
				for (DailyTime dt : form.getWeeklyTimecard().getDailyTimes()) {
					// Changed to Long ACTRA form.
					if (changedToLong && (i <= 4)) {
						i++;
						continue;
					}
					callType = ActraCallType.getTypeByIndex(i);
					ActraContractTimesheet item = new ActraContractTimesheet(dt.getDate(), dt.getTrvlToLocFrom(), dt.getTrvlToLocTo(),
						dt.getCallTime(), dt.getM1Out(), dt.getM1In(), dt.getWrap(), dt.getTrvlFromLocFrom(), dt.getTrvlFromLocTo(),
						dt.getDate2(), dt.getCallTime2(), dt.getWrap2(), (dt.getCallType() == null ? callType : dt.getCallType()) , i);
					list.add(item);
					if (i == 4 && ! moreInfo) {
						break;
					}
					i++;
				}
			}
		}
		// Changed to Short ACTRA form.
		else if (isUpdated && (! moreInfo)) {
			LOG.debug("");
			Iterator<ActraContractTimesheet> itr = list.iterator();
			while (itr.hasNext()) {
				/* Need this initialization of next() method, otherwise will have IllegalStateException
				if user checks/unchecks the checkbox multiple times(for multiple remove actions).*/
				itr.next();
				if (i > 4) {
					itr.remove();
				}
				i++;
			}
		}
		return list;
	}

	@Override
	public FormActraContract getFormById(Integer id) {
		return getFormActraContractDAO().findById(id);
	}

	/**
	 * Create a blank copy of the ACTRA contract.
	 *
	 * @return A new FormActraContract, pre-populated with the current project
	 *         preferences and the employee's information (from the User record
	 *         or elsewhere). The form is NOT persisted.
	 */
	@Override
	public FormActraContract getBlankForm() {
		//contactDoc = getContactDocumentDAO().refresh(contactDoc);
		form = new FormActraContract();
		try {
			LOG.debug("");
			Date todaysDate = CalendarUtils.todaysDate();
			User user = getvUser(); // currently logged-in user
			form.setVersion(FormActraContract.DEFAULT_ACTRA_CONTRACT_VERSION);
			form.setCreatedBy(user);
			form.setCreatedDate(todaysDate);
			form.setUpdatedBy(user);
			form.setUpdatedDate(todaysDate);
			if (contactDoc != null) {
				Contact ct = contactDoc.getContact();
				Employment emp = contactDoc.getEmployment();
                WeeklyTimecard wtc = TimecardUtils.createActraTimecard(ct.getUser(),
                        getProduction(), todaysDate, emp.getStartForm());
                form.setWeeklyTimecard(wtc);
			}
			populateForm(false);
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return form;
	}

	/** Method to pre-populate the form on creation. */
	@Override
	public void populateForm(boolean prompted) {
		LOG.debug(" ");
		Production production = getProduction();
		Project proj = SessionUtils.getCurrentOrViewedProject();

		// Onboarding Preferences data
		PayrollPreference payrollPref;
		payrollPref = production.getPayrollPref();
		payrollPref = PayrollPreferenceDAO.getInstance().refresh(payrollPref);

		CanadaProjectDetail cproj = proj.getCanadaProjectDetail();
		if(cproj == null) {
			LOG.debug("cproj == null");
			return;
		}
		cproj = CanadaProjectDetailDAO.getInstance().refresh(cproj);
		form.setAdditionalTitles(cproj.getAdditionalTitles());
		form.setAdvertiserName(cproj.getAdvertiserName());
		form.setAgencyName(cproj.getAgencyName());
		form.setAgencyProducer(cproj.getAgencyProducer());
		//form.setBranchCode(cproj.getBranchCode()); //no longer used in LS-2194
		form.setCommercialName(cproj.getCommercialName());
		form.setDirectorName(cproj.getDirectorName());
		form.setProductName(cproj.getBrandName());
		form.setProdHouseName(cproj.getProdHouseName());
		form.setNationalTv(cproj.getNationalTv());
		form.setNationalRadio(cproj.getNationalRadio());
		form.setNationalDigitalMediaVideo(cproj.getNationalDigitalMediaVideo());
		form.setNationalDigitalMediaAudio(cproj.getNationalDigitalMediaAudio());
		form.setTagsTv(cproj.getTagsTv());
		form.setTagsRadio(cproj.getTagsRadio());
		form.setTagsDigitalMedia(cproj.getTagsDigitalMedia());
		form.setRegionalChangesTv(cproj.getRegionalChangesTv());
		form.setRegionalChangesRadio(cproj.getRegionalChangesRadio());
		form.setRegionalChangesDigitalMedia(cproj.getRegionalChangesDigitalMedia());
		form.setPsaTv(cproj.getPsaTv());
		form.setPsaRadio(cproj.getPsaRadio());
		form.setPsaDigitalMedia(cproj.getPsaDigitalMedia());
		form.setDemoTv(cproj.getDemoTv());
		form.setDemoRadio(cproj.getDemoRadio());
		form.setDemoDigital(cproj.getDemoDigital());
		form.setDemoPresentation(cproj.getDemoPresentation());
		form.setDemoInfomercial(cproj.getDemoInfomercial());
		form.setSeasonalTv(cproj.getSeasonalTv());
		form.setSeasonalRadio(cproj.getSeasonalRadio());
		form.setSeasonalDealer(cproj.getSeasonalDealer());
		form.setSeasonalDealerTv(cproj.getSeasonalDealerTv());
		form.setSeasonalDealerRadio(cproj.getSeasonalDealerRadio());
		form.setSeasonalDoubleShoot(cproj.getSeasonalDoubleShoot());
		form.setSeasonalJointPromo(cproj.getSeasonalJointPromo());
		form.setLocalRegionalCategoryNum(cproj.getLocalRegionalCategoryNum());
		form.setLocalRegionalTv(cproj.getLocalRegionalTv());
		form.setLocalRegionalRadio(cproj.getLocalRegionalRadio());
		form.setLocalRegionalDigitalMedia(cproj.getLocalRegionalDigitalMedia());
		form.setLocalRegionalDemo(cproj.getLocalRegionalDemo());
		form.setLocalRegionalDigitalMediaBroadcast(cproj.getLocalRegionalDigitalMediaBroadcast());
		form.setLocalRegionalBroadcastDigitalMedia(cproj.getLocalRegionalBroadcastDigitalMedia());
		form.setLocalRegionalOther(cproj.getLocalRegionalOther());
		form.setLocalRegionalPilotProject(cproj.getLocalRegionalPilotProject());
		form.setShortLifeTv7Days(cproj.getShortLifeTv7Days());
		form.setShortLifeTv14Days(cproj.getShortLifeTv14Days());
		form.setShortLifeTv31Days(cproj.getShortLifeTv31Days());
		form.setShortLifeTv45Days(cproj.getShortLifeTv45Days());
		form.setShortLifeRadio7Days(cproj.getShortLifeRadio7Days());
		form.setShortLifeRadio14Days(cproj.getShortLifeRadio14Days());
		form.setShortLifeRadio31Days(cproj.getShortLifeRadio31Days());
		form.setShortLifeRadio45Days(cproj.getShortLifeRadio45Days());
		form.setTvBroadcastDigitalMedia(cproj.getTvBroadcastDigitalMedia());
		form.setDigitalMediaBroadcastTv(cproj.getDigitalMediaBroadcastTv());
		form.setDigitalMediaOther(cproj.getDigitalMediaOther());
		form.setRadioDigitalMedia(cproj.getRadioDigitalMedia());
		form.setActraOnline(cproj.getActraOnline());
		form.setArticle2403(cproj.getArticle2403());
		form.setArticle2404(cproj.getArticle2404());
		form.setArticle2405(cproj.getArticle2405());
		form.setArticle2406(cproj.getArticle2406());
		form.setDocket(cproj.getDocket());
		/**
		 * LS-2194 Branch code droptown for project details and Actra contract
		 */
		if (cproj.getOffice() != null) {
			form.setOffice(cproj.getOffice());
		}
		if (form.getAgencyAddress() == null) {
			getForm().setAgencyAddress(new Address(true));
		}
		if (cproj.getAgencyAddress() != null) { // Agency Address
			form.getAgencyAddress().copyFrom(cproj.getAgencyAddress());
		}

		if (form.getProdHouseAddress() == null) {
			getForm().setProdHouseAddress(new Address(true));
		}
		if (cproj.getProdHouseAddress() != null) {
			form.getProdHouseAddress().copyFrom(cproj.getProdHouseAddress());
		}

		if (form.getTalentAddress() == null) {
			getForm().setTalentAddress(new Address(true));
		}

		if (getContactDoc() != null) {
			User cdUser = getContactDoc().getContact().getUser();
			form.setTalentName(cdUser.getFirstNameLastName());
			//LS-1867 Pre-populate fields from My Account tab to ACTRA Contract
			form.setLoanOutName(cdUser.getLoanOutCorpName());
			form.setApprenticeNum(cdUser.getApprentice());
			form.setTalentPhoneNum(cdUser.getPrimaryPhone());
			form.setTalentEmailAddress(cdUser.getEmailAddress());
			if (cdUser.getHomeAddress() != null) {
				form.getTalentAddress().copyFrom(cdUser.getHomeAddress());
			}
			// For the agency name and contact, we need to find the selected
			// agent from the list of possible agents for this user.
			form.setAgencyContact(null);
			form.setTalentAgencyName(null);

			List<Agent> agents = cdUser.getAgentsList();
			if(agents != null && !agents.isEmpty()) {
				// find the selected default agent.
				for(Agent agent : agents) {
					if(agent.getSelected()) {
						form.setAgencyContact(agent.getDisplayName());
						form.setTalentAgencyName(agent.getAgencyName());
						break;
					}
				}
			}
			if (cdUser.getBirthdate() != null) {
				Date today = CalendarUtils.todaysDate();
				Calendar cal = CalendarUtils.getInstance(today);
				cal.setTime(cdUser.getBirthdate());
				int years = CalendarUtils.yearsUntilDate(cal, today);
				LOG.debug("years = " + years);
				if (years >= 0 && years < 18) {
					form.setDob(cdUser.getBirthdate());
				}
				else {
					LOG.debug("NOT A MINOR");
				}
			}
			form.setSocialInsuranceNum(cdUser.getSocialSecurity());
			form.setGstHst(cdUser.getGstNumber());
			form.setQst(cdUser.getQstNumber());
			form.setFullMemberNum(cdUser.getFullMemberNum());


		}

//		Employment employment = getContactDoc().getEmployment();
//		if (employment != null) {
//			LOG.debug("Employment:" + employment.getId());
//			employment = EmploymentDAO.getInstance().refresh(employment);
//			StartForm startForm = employment.getStartForm();
//			if (startForm != null) {
//				LOG.debug("Startform =" + startForm.getId());
//			}
//		}

	}

	@Override
	public String autoFillForm(boolean prompted) {
		try {
			populateForm(prompted);
			return super.autoFillForm(prompted);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	private FormActraContractDAO getFormActraContractDAO() {
		if(formActraContractDAO == null) {
			formActraContractDAO = FormActraContractDAO.getInstance();
		}

		return formActraContractDAO;
	}

	/*private EmploymentDAO getEmploymentDAO() {
		if(employmentDAO == null) {
			employmentDAO = EmploymentDAO.getInstance();
		}

		return employmentDAO;
	}

	private UserDAO getUserDAO() {
		if(userDAO == null) {
			userDAO = UserDAO.getInstance();
		}

		return userDAO;
	}*/

	@Override
	public void refreshForm() {
		if (form != null) {
			form = getFormActraContractDAO().refresh(form);
		}
		if (form.getVersion().equals(FormActraContract.ACTRA_CONTRACT_VERSION_2018)) {
			setAddMoreInfo(false);
		}
		if (form.getVersion().equals(FormActraContract.ACTRA_CONTRACT_LONG_VERSION_2018)) {
			setAddMoreInfo(true);
		}
		setActraTimesheetList(null);
	}

	public List<SelectItem> getCallTypeList() {
		if (callTypeList == null ) {
			callTypeList = new ArrayList<>();
			callTypeList.add(new SelectItem("Wardrobe Call"));
			callTypeList.add(new SelectItem("Rehearsal"));
			callTypeList.add(new SelectItem("Holding Call"));
//			if (form != null && form.getAdvertiserName() != null && (! form.getAdvertiserName().isEmpty())) {
//				callTypeList.add(new SelectItem(form.getAdvertiserName()));
//			}
		}
		return callTypeList;
	}

//	public String actionAdd() {
//		getActraTimesheetList().add(new ActraContractTimesheet(true));
//		LOG.debug("Size = " + getActraTimesheetList().size());
//		for (ActraContractTimesheet wrp : getActraTimesheetList()) {
//			LOG.debug("Add Other = " + wrp.getAddOther());
//		}
//		return null;
//	}

//	public void callTypeChange(ValueChangeEvent event) {
//		Object value = event.getNewValue();
//		if (value != null) {
//			LOG.debug("NEW VALUE = " + value);
//		}
//	}

//	/**
//	 * Action method for the "Print" button on the Start Forms mini-tab. Called
//	 * via ContactFormBean.
//	 *
//	 * @return null navigation string
//	 */
//	public String actionPrint_JASPER_NOT_USED() {
//		try {
//			if (contactDoc != null) {
//				LOG.debug("");
//				Integer id = contactDoc.getRelatedFormId();
//				FormActraContract formActra = null;
//				if (id != null) {
//					formActra = getFormActraContractDAO().findById(id);
//				}
//				else {
//					formActra = getBlankForm();
//					formActra.setId(0); // this retrieves blank I-9 record in database!
//				}
//				if (formActra != null) {
//					ReportBean report = ReportBean.getInstance();
//					formActra = getFormActraContractDAO().refresh(formActra);
//					report.generateFormActraContract(formActra, null, contactDoc.getId(), contactDoc.getStatus().name());
//				}
//			}
//		}
//		catch (Exception e) {
//			EventUtils.logError(e);
//			MsgUtils.addGenericErrorMessage();
//		}
//		return null;
//	}

	/**
	 * Listener for "Add more info" checkbox; CURRENTLY NOT USED
	 * @param event change event created by framework
	 */
	public void listenAddMoreInfo(ValueChangeEvent event) {
		Boolean value = (Boolean)event.getNewValue();
		if (value != null) {
			LOG.debug("NEW VALUE = " + value);
			actraTimesheetList = createTimesheet(value, true);
		}
	}

	/**
	 *
	 */
	public void listenNdmChange(ValueChangeEvent event) {
		Boolean value = (Boolean)event.getNewValue();

		if(value != null) {
			if(value) {
				ndm[0] = true;
				ndm[1] = false;
			}
			else {
				ndm[0] = true;
				ndm[1] = false;
			}
		}
	}
	/**
	 * Listener for "Substantial snack=yes" checkbox;
	 *
	 * @param event change event created by framework
	 */
	public void listenSnackYes(ValueChangeEvent event) {
		Boolean value = (Boolean)event.getNewValue();
		if(value != null) {
			if(value) {
				ndm[0] = true;
				ndm[1] = false;
			}
			else {
				ndm[0] = false;
				ndm[1] = true;
			}
		}
	}

	/**
	 * Listener for "Substantial snack=No" checkbox;
	 *
	 * @param event change event created by framework
	 */
	public void listenSnackNo(ValueChangeEvent event) {
		Boolean value = (Boolean)event.getNewValue();
		if(value != null) {
			if(value) {
				ndm[0] = false;
				ndm[1] = true;
			}
			else {
				ndm[0] = true;
				ndm[1] = false;
			}
		}
	}

	/** See {@link #addMoreInfo}. */
	public boolean isAddMoreInfo() {
		return addMoreInfo;
	}
	public void setAddMoreInfo(boolean addMoreInfo) {
		this.addMoreInfo = addMoreInfo;
	}

	/**
	 * @return effective value of the FormActraContract.ndm field, where null is
	 *         treated as false. So when 'ndm' is null, both the "Yes" and "No"
	 *         boxes will be unchecked.
	 */
	public Boolean[] getNdm() {
		return ndm;
	}

	public void setNdm(Boolean[] ndm) {
		this.ndm = ndm;
	}

	public List<ActraContractTimesheet> getActraTimesheetList() {
		if (actraTimesheetList == null) {
			if (form == null) {
				return new ArrayList<>();
			}
			actraTimesheetList = createTimesheet(addMoreInfo, false);
		}
		return actraTimesheetList;
	}

	public void setActraTimesheetList(List<ActraContractTimesheet> actraTimesheetList) {
		this.actraTimesheetList = actraTimesheetList;
	}

	/**
	 * Method to check if the timesheet is empty or have entries - LS-1963
	 *
	 * @return
	 */
	public boolean isTimesheetEmpty() {
		for (Iterator<ActraContractTimesheet> iter = actraTimesheetList.iterator(); iter
				.hasNext();) {
			ActraContractTimesheet entry = iter.next();
			if (entry.getDate1() != null || entry.getDate2() != null ||
					entry.getTravelFrom1() != null || entry.getTravelTo1() != null ||
					entry.getTravelFrom2() != null || entry.getTravelTo2() != null ||
					entry.getCallTime1() != null || entry.getFinishTime1() != null ||
					entry.getCallTime2() != null || entry.getFinishTime2() != null ||
					entry.getM1In() != null || entry.getM1Out() != null) {
				return false;
			}
		}
		return true;
	}
	/**
	 * LS-2194 Branch code droptown for project details and Actra contract
	 */
	/** See {@link #officeListDL}. */
	public List<SelectItem> getOfficeListDL() {
		if (officeListDL == null) {
			createOfficeDL();
		}
		return officeListDL;
	}

	/** See {@link #officeListDL}. */
	public void setOfficeListDL(List<SelectItem> officeListDL) {
		this.officeListDL = officeListDL;
	}

	/**
	 * Create a list of Offices. will use for branch code selected office.
	 * Currently only being used for Canada
	 */
	private void createOfficeDL() {
		officeListDL = new ArrayList<>();
		List<Office> officeList = getOfficeDAO().findOffices(OfficeType.ACTRA, "sortOrder");
		officeListDL.add(Constants.EMPTY_SELECT_ITEM);
		for (Office office : officeList) {
			officeListDL.add(new SelectItem(office, office.getBranchCode()));
		}
	}

	/**
	 * Set the select office to set BranchCode.
	 *
	 * @param event
	 */
	public void listenOfficeListChange(ValueChangeEvent event) {
		Office office = (Office)event.getNewValue();
		if (office != null) {
			refreshForm();
			form.setOffice(office);
		}
	}

	private OfficeDAO getOfficeDAO() {
		if (officeDAO == null) {
			officeDAO = OfficeDAO.getInstance();
		}
		return officeDAO;
	}

}
