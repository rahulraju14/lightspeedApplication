package com.lightspeedeps.web.onboard.form;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.web.onboard.ContactFormBean;
import com.lightspeedeps.web.popup.PopupBean;

/**
 * Backing bean for the CA and NY WTPA forms on the Payroll / Start Forms page.
 * Note that these forms have different UIs, but share the same persisted model
 * class (FormWTPA).
 * @see com.lightspeedeps.model.FormWTPA
 * @see StandardFormBean
 * @see com.lightspeedeps.web.onboard.ContactFormBean
 */
@ManagedBean
@ViewScoped
public class FormWtpaBean extends StandardFormBean<FormWTPA> implements Serializable {

	/**
	 */
	private static final long serialVersionUID = -2200705940697558365L;

	private static final Log LOG = LogFactory.getLog(FormWtpaBean.class);

	private boolean isActionCancel = false;

	/** Week day list for regular pay day drop down on form. */
	private static List<SelectItem> regularPayDayWeekDayList = null;

	/** "Decline to sign" action code for popup confirmation/prompt dialog. */
	private static final int ACT_DECLINE = 42;

	/** Boolean array used for the selection of CA WTPA's Regular rate of pay radio buttons. */
	private Boolean caRegularFreq[] = new Boolean[3];

	/** Boolean array used for the selection of CA WTPA's Overtime rate of pay & NY WTPA's Overtime method radio buttons. */
	private Boolean overtimeFreq[] = new Boolean[2];

	/** Boolean array used for the selection of WTPA's Regular Payday radio buttons. */
	private Boolean regularPayday[] = new Boolean[3];

	/** Boolean array used for the selection of CA WTPA's Sick Leave Type radio buttons. */
	private Boolean caSickLeaveType[] = new Boolean[4];

	/** Boolean array used for the selection of NY WTPA's Exempt pay rate radio buttons. */
	private Boolean nyExemptPay[] = new Boolean[5];

	/** "Auto-fill" action code for popup confirmation/prompt dialog. */
//	private static final int ACT_AUTO_FILL = 43;

	static {
		String[] weekdays = new DateFormatSymbols().getWeekdays();
		regularPayDayWeekDayList = new ArrayList<>();
		int count = 0;
		for (String weekDay : weekdays) {
			regularPayDayWeekDayList.add(new SelectItem(count, weekDay));
			count++;
		}
	}

	public FormWtpaBean() {
		super("FormWtpaBean");
		resetBooleans();
	}

	/**
	 * Set all our backing boolean arrays to all False.
	 */
	private void resetBooleans() {
		Arrays.fill(caRegularFreq, Boolean.FALSE);
		Arrays.fill(overtimeFreq, Boolean.FALSE);
		Arrays.fill(regularPayday, Boolean.FALSE);
		Arrays.fill(caSickLeaveType, Boolean.FALSE);
		Arrays.fill(nyExemptPay, Boolean.FALSE);
	}

	public static FormWtpaBean getInstance() {
		return (FormWtpaBean) ServiceFinder.findBean("formWtpaBean");
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#getFormById(Integer)
	 */
	@Override
	public FormWTPA getFormById(Integer id) {
		return FormWtpaDAO.getInstance().findById(id);
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#getBlankForm()
	 */
	@Override
	public FormWTPA getBlankForm() {
		FormWTPA formWTPA = new FormWTPA();
		formWTPA.setAddress(new Address());
		formWTPA.setMailingAddress(new Address());
		return formWTPA;
	}

	@Override
	public String actionSave() {
		LOG.debug("Form Wtpa");
		try {
			if (contactDoc.getFormType() == PayrollFormType.NY_WTPA) {
				if (overtimeFreq[0]) {
					form.setOvertimeMethod(PayFrequency.W);
				}
				else if (overtimeFreq[1]) {
					form.setOvertimeMethod(PayFrequency.D);
				}
				for (ExemptPayType type : ExemptPayType.values()) {
					if (nyExemptPay[type.getIndex()]) {
						form.setExemptPayType(type);
						break;
					}
				}
			}
			for (PaydayFrequency type : PaydayFrequency.values()) {
				if (regularPayday[type.getIndex()]) {
					form.setPaydayFreq(type);
					break;
				}
			}
			if (contactDoc.getFormType() == PayrollFormType.CA_WTPA) {
				if (caRegularFreq[0]) {
					form.setRegularFreq(PayFrequency.H);
				}
				else if (caRegularFreq[1]) {
					form.setRegularFreq(PayFrequency.D);
				}
				else if (caRegularFreq[2]) {
					form.setRegularFreq(PayFrequency.O);
				}
				if (overtimeFreq[0]) {
					form.setOvertimeFreq(PayFrequency.H);
				}
				else if (overtimeFreq[1]) {
					form.setOvertimeFreq(PayFrequency.O);
				}
				for (CalifSickLeaveType type : CalifSickLeaveType.values()) {
					if (caSickLeaveType[type.getIndex()]) {
						form.setSickLeaveType(type);
						break;
					}
				}
			}
			FormWtpaDAO.getInstance().attachDirty(form);
			return super.actionSave();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
			return Constants.ERROR_RETURN;
		}
	}
	/** LS-926 Save of CA WTPA fails if "regular rate" is over $99,999 All Decimal Fields validation */
	public boolean checkDecimalAmounts() {
		if (form.getRegularRate() != null && Constants.DECIMAL_100K.compareTo(form.getRegularRate()) != 1) {
			MsgUtils.addFacesMessage("FormWtpaBean.ValidateAllBigDecimal", FacesMessage.SEVERITY_ERROR);
			return false;
		}
		else if (form.getOvertimeRate() != null && Constants.DECIMAL_100K.compareTo(form.getOvertimeRate()) != 1) {
			MsgUtils.addFacesMessage("FormWtpaBean.ValidateAllBigDecimal", FacesMessage.SEVERITY_ERROR);
			return false;
		}
		else if (form.getAllowTipsAmt() != null && Constants.DECIMAL_100K.compareTo(form.getAllowTipsAmt()) != 1) {
			MsgUtils.addFacesMessage("FormWtpaBean.ValidateAllBigDecimal", FacesMessage.SEVERITY_ERROR);
			return false;
		}
		else if (form.getAllowMealAmt() != null && Constants.DECIMAL_100K.compareTo(form.getAllowMealAmt()) != 1) {
			MsgUtils.addFacesMessage("FormWtpaBean.ValidateAllBigDecimal",FacesMessage.SEVERITY_ERROR);
			return false;
		}
		else if (form.getAllowLodgingAmt() != null && Constants.DECIMAL_100K.compareTo(form.getAllowLodgingAmt()) != 1) {
			MsgUtils.addFacesMessage("FormWtpaBean.ValidateAllBigDecimal",FacesMessage.SEVERITY_ERROR);
			return false;
		}
		else if (form.getAllowOtherAmt() != null && Constants.DECIMAL_100K.compareTo(form.getAllowOtherAmt()) != 1) {
			MsgUtils.addFacesMessage("FormWtpaBean.ValidateAllBigDecimal", FacesMessage.SEVERITY_ERROR);
			return false;
		}
		else if (form.getOvertimeTier1Amt() != null && Constants.DECIMAL_100K.compareTo(form.getOvertimeTier1Amt()) != 1) {
			MsgUtils.addFacesMessage("FormWtpaBean.ValidateAllBigDecimal", FacesMessage.SEVERITY_ERROR);
			return false;
		}
		else if (form.getOvertimeTier2Amt() != null && Constants.DECIMAL_100K.compareTo(form.getOvertimeTier2Amt()) != 1) {
			MsgUtils.addFacesMessage("FormWtpaBean.ValidateAllBigDecimal", FacesMessage.SEVERITY_ERROR);
			return false;
		}
		else if (form.getExemptPayAmt() != null && Constants.DECIMAL_100K.compareTo(form.getExemptPayAmt()) != 1) { // Fix for npe LS-2284
			MsgUtils.addFacesMessage("FormWtpaBean.ValidateAllBigDecimal", FacesMessage.SEVERITY_ERROR);
			return false;
		}
		else if (form.getOvertimeTier1() != null && Constants.HOURS_IN_A_DAY.compareTo(form.getOvertimeTier1()) != 1) {
			MsgUtils.addFacesMessage("FormWtpaBean.ValidateHoursBigDecimal", FacesMessage.SEVERITY_ERROR);
			return false;
		}
		else if (form.getOvertimeTier2() != null && Constants.HOURS_IN_A_DAY.compareTo(form.getOvertimeTier2()) != 1) {
			MsgUtils.addFacesMessage("FormWtpaBean.ValidateHoursBigDecimal", FacesMessage.SEVERITY_ERROR);
			return false;
		}
		return true;
	}

	/** Action method called when user agrees to decline to sign by clicking "Decline" on decline popup.
	 * @return
	 */
	private String actionDeclineOk() {
		try {
			ContactDocEventDAO.getInstance().createEvent(getContactDoc(), TimedEventType.DECLINE);
			// Check if employer already signed & mark APPROVED
			setContactDoc(ContactDocumentDAO.getInstance().refresh(getContactDoc()));
			if (getContactDoc().getEmployerSignature() != null) {
				getContactDoc().setStatus(ApprovalStatus.APPROVED);
				ContactDocEventDAO.getInstance().createEvent(getContactDoc(), TimedEventType.APPROVE);
			}
			else {
				setContactDoc(ContactFormBean.getInstance().submitContactDocument(getContactDoc(), false, null));
				getContactDoc().setStatus(ApprovalStatus.SUBMITTED);
			}
			getContactDoc().setLockedBy(null);
			ContactDocumentDAO.getInstance().attachDirty(getContactDoc());
			//FormWtpaDAO.getInstance().attachDirty(form);
			actionSave();
			ContactFormBean bean = ContactFormBean.getInstance();
			bean.setEditMode(false);
			bean.clearEmpTable();
			bean.calculateAuthFlags();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	@Override
	public String actionEdit() {
		calculateEditFlags(false);
		return super.actionEdit();
	}

	@Override
	public void rowClicked(ContactDocument contactDocument) {
		LOG.debug("");
		setContactDoc(contactDocument);
		isActionCancel = false;
		setUpForm();
	}

	/** Method used to fetch the saved data for the selected form and to set that data in the Form instance. */
	private void setUpForm() {
		Integer relatedFormId = contactDoc.getRelatedFormId();
		resetBooleans();
		if (relatedFormId != null) {
			setForm(FormWtpaDAO.getInstance().findById(relatedFormId));
			if (contactDoc.getFormType() == PayrollFormType.CA_WTPA) {
				if (getForm().getRegularFreq() != null) {
					if (getForm().getRegularFreq() == PayFrequency.H) {
						caRegularFreq[0] = true;
					}
					else if (getForm().getRegularFreq() == PayFrequency.D) {
						caRegularFreq[1] = true;
					}
					else if (getForm().getRegularFreq() == PayFrequency.O) {
						caRegularFreq[2] = true;
					}
				}
				if (getForm().getOvertimeFreq() != null) {
					if (getForm().getOvertimeFreq() == PayFrequency.H) {
						overtimeFreq[0] = true;
					}
					else if (getForm().getOvertimeFreq() == PayFrequency.O) {
						overtimeFreq[1] = true;
					}
				}
				if (getForm().getSickLeaveType() != null) {
					int index = form.getSickLeaveType().getIndex();
					caSickLeaveType[index] = true;
				}
			}
			else if (contactDoc.getFormType() == PayrollFormType.NY_WTPA) {
				if (getForm().getOvertimeMethod() != null) {
					if (getForm().getOvertimeMethod() == PayFrequency.W) {
						overtimeFreq[0] = true;
					}
					else if (getForm().getOvertimeMethod() == PayFrequency.D) {
						overtimeFreq[1] = true;
					}
				}
				if (getForm().getExemptPayType() != null) {
					int index = form.getExemptPayType().getIndex();
					nyExemptPay[index] = true;
				}
			}
			// For both the WTPA forms.
			if (getForm().getPaydayFreq() != null) {
				int index = form.getPaydayFreq().getIndex();
				regularPayday[index] = true;
			}
		}
		else {
			setForm(new FormWTPA());
		}
		if (getForm().getAddress() == null) {
			getForm().setAddress(new Address());
		}
		if (getForm().getMailingAddress() == null) {
			getForm().setMailingAddress(new Address());
		}
		form.getAddress().getAddrLine1(); // Prevents LIE when returning to form mini-tab
		form.getMailingAddress().getAddrLine1(); // Prevents LIE when returning to form mini-tab
		if (form.getName() == null && (! isActionCancel)) {
			populateForm(true);
			FormWtpaDAO.getInstance().attachDirty(form);
			contactDoc.setRelatedFormId(form.getId());
			ContactDocumentDAO.getInstance().attachDirty(contactDoc);
		}
	}

	@Override
	public String actionCancel() {
		isActionCancel = true;
		setUpForm();
		return super.actionCancel();
	}

	/** Method to pre-populate the form on creation. */
	@Override
	public void populateForm(boolean prompted) {
		LOG.debug(" ");
		Production production = getProduction();

		// Onboarding Preferences data
		PayrollPreference payrollPref;
		payrollPref = production.getPayrollPref();
		if (production.getType().isAicp()) {
			Project proj = SessionUtils.getCurrentOrViewedProject();
			form.setProjectName(proj.getTitle());
			form.setProjectNumber(proj.getEpisode());
		}
		else {
			form.setProjectName(production.getTitle()); // may be overridden by StartForm (below)
		}
		if (payrollPref != null) {
			payrollPref = PayrollPreferenceDAO.getInstance().refresh(payrollPref);
			form.setFedidNumber(payrollPref.getFedidNumber());
			form.setDba(payrollPref.getDba());
			form.setRegularPayDay(payrollPref.getRegularPayDay());
			form.setPaydayFreq(payrollPref.getPaydayFreq());
			if (payrollPref.getPaydayFreq() != null) {
				regularPayday[payrollPref.getPaydayFreq().getIndex()] = true;
			}

			form.setPaydayFreqOther(payrollPref.getPaydayFreqOther());
			if (payrollPref.getNoticeGivenAtHire() != null) {
				form.setNoticeGivenAtHire(payrollPref.getNoticeGivenAtHire());
			}
			else {
				form.setNoticeGivenAtHire(true);
			}
			if (getContactDoc().getFormType() == PayrollFormType.CA_WTPA && payrollPref.getSickLeaveType() != null) {
				form.setSickLeaveType(payrollPref.getSickLeaveType());
				caSickLeaveType[payrollPref.getSickLeaveType().getIndex()] = true;
				if (payrollPref.getSickLeaveType() == CalifSickLeaveType.EXEMPT) {
					form.setSickLeaveReason(payrollPref.getSickLeaveReason());
				}
			}
		}

		form.setAllowNone(true);
		form.setEmployerName(production.getStudio());
		form.setEmployerPhone(production.getPhone());
		if (form.getAddress() == null) {
			getForm().setAddress(new Address());
		}
		if (production.getAddress() != null) { // Employer's Address
			form.getAddress().copyFrom(production.getAddress());
			if (! StringUtils.isEmpty(form.getAddress().getAddrLine2())) {
				form.getAddress().setAddrLine1(
						form.getAddress().getAddrLine1() + ", " + form.getAddress().getAddrLine2());
				form.getAddress().setAddrLine2(null);
			}
		}

		if (form.getMailingAddress() == null) {
			getForm().setMailingAddress(new Address());
		}
		if (payrollPref != null && payrollPref.getMailingAddress() != null) {
			form.getMailingAddress().copyFrom(payrollPref.getMailingAddress());
		}
		else if (production.getAddress() != null) {
			form.getMailingAddress().copyFrom(production.getAddress());
		}
		if (! StringUtils.isEmpty(form.getMailingAddress().getAddrLine2())) {
			form.getMailingAddress().setAddrLine1(
					form.getMailingAddress().getAddrLine1() + ", " + form.getMailingAddress().getAddrLine2());
			form.getMailingAddress().setAddrLine2(null);
		}

		Employment employment = getContactDoc().getEmployment();
		if (employment != null) {
			LOG.debug("Employment:" + employment.getId());
			employment = EmploymentDAO.getInstance().refresh(employment);
			StartForm startForm = employment.getStartForm();
			if (startForm != null) {
				LOG.debug("Startform =" + startForm.getId());
				String name = startForm.getFirstName() + " " + startForm.getLastName();
				form.setName(name);
				form.setStartDate(startForm.getWorkStartDate());
				form.setEmail(startForm.getContact().getEmailAddress());
				form.setPhone(startForm.getPhone());
				form.setOccupation(startForm.getJobClass());
				if (production.getType().hasPayrollByProject()) {
					form.setProjectName(startForm.getJobName());
					form.setProjectNumber(startForm.getJobNumber());
				}
				else {
					form.setProjectName(startForm.getProdTitle());
				}
				form.setExempt(startForm.getAllowWorked());
				StartRateSet srs = startForm.getProd();
				if (form.getExempt()) {
					form.setRegularRate(null);
					form.setOvertimeRate(null);
				}
				else {
					BigDecimal otRate = null;
					form.setRegularRate(startForm.getRate());
					if (srs.getOt1Rate() != null && srs.getOt1Rate().signum() != 0) {
						otRate = srs.getOt1Rate();
					}
					else {
						if (startForm.isStudioRate()) {
							if (srs.getX15StudioRate() != null && srs.getX15StudioRate().signum() != 0) {
								otRate = srs.getX15StudioRate();
							}
						}
						else {
							if (srs.getX15LocRate() != null && srs.getX15LocRate().signum() != 0) {
								otRate = srs.getX15LocRate();
							}
						}
					}
					form.setOvertimeRate(otRate);
				}
				BigDecimal exemptRate = null;
				if (startForm.getRateType() == EmployeeRateType.WEEKLY) {
					form.setRegularFreq(PayFrequency.W);
					form.setOvertimeFreq(PayFrequency.W);
					exemptRate = startForm.getRate();
					form.setExemptPayType(ExemptPayType.O);
					nyExemptPay[ExemptPayType.O.getIndex()] = true;
					form.setRegularFreqOther("Week"/*PayFrequency.W.getLabel()*/);
					caRegularFreq[2] = true;
					overtimeFreq[0] = true;
				}
				else if (startForm.getRateType() == EmployeeRateType.DAILY) {
					form.setRegularFreq(PayFrequency.D);
					form.setOvertimeFreq(PayFrequency.H);
					exemptRate = startForm.getRate();
					form.setExemptPayType(ExemptPayType.D);
					nyExemptPay[ExemptPayType.D.getIndex()] = true;
					caRegularFreq[1] = true;
					overtimeFreq[0] = true;
				}
				else { // Hourly
					form.setRegularFreq(PayFrequency.H);
					form.setOvertimeFreq(PayFrequency.H);
					caRegularFreq[0] = true;
					overtimeFreq[0] = true;
					if (srs.getOt1AfterHours() != null) {
						form.setOvertimeMethod(PayFrequency.D); // For NY WTPA
						form.setOvertimeTier1(srs.getOt1AfterHours());
						form.setOvertimeTier1Amt(srs.getOt1Rate());
						form.setOvertimeTier2(srs.getOt2AfterHours());
						form.setOvertimeTier2Amt(srs.getOt2Rate());
					}
					else {
						form.setOvertimeMethod(PayFrequency.W); // For NY WTPA
					}
				}
				if (form.getExempt()) {
					form.setExemptPayAmt(exemptRate);
					if (getContactDoc().getFormType() == PayrollFormType.CA_WTPA) {
						form.setRegularRate(null);
						if (form.getRegularFreq() == PayFrequency.W) {
							form.setRegularFreq(PayFrequency.O);
						}
					}
				}
			}
			else {
				User cdUser = getContactDoc().getContact().getUser();
				form.setName(cdUser.getFirstNameLastName());
				form.setEmail(cdUser.getEmailAddress());
				form.setPhone(cdUser.getPrimaryPhone());
				form.setStartDate(CalendarUtils.todaysDate());
				form.setOccupation(employment.getOccupation());
				if (production.getType().isAicp()) {
					form.setProjectName(SessionUtils.getCurrentOrViewedProject().getTitle());
					form.setProjectNumber(SessionUtils.getCurrentOrViewedProject().getEpisode());
				}
			}
		}
		else {
			User cdUser = getContactDoc().getContact().getUser();
			form.setName(cdUser.getFirstNameLastName());
			form.setEmail(cdUser.getEmailAddress());
			form.setPhone(cdUser.getPrimaryPhone());
			MsgUtils.addFacesMessage("FormWtpaBean.PopulateFormError", FacesMessage.SEVERITY_ERROR);
		}
		super.populateForm(prompted);
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#refreshForm()
	 */
	@Override
	public void refreshForm() {
		if (form != null) {
			form = FormWtpaDAO.getInstance().refresh(form);
		}
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
		if (! checkSendValid()) {
			MsgUtils.addFacesMessage("ContactFormBean.EmployerSignatureInfo", FacesMessage.SEVERITY_ERROR);
			return false;
		}
		boolean isNotValid = (StringUtils.isEmpty(form.getEmployeeRepName()) ||
				StringUtils.isEmpty(form.getEmployeeRepTitle()) ||
				StringUtils.isEmpty(form.getEmployeeRepEmail()));

		if (contactDoc.getFormType() == PayrollFormType.CA_WTPA) {
			isNotValid = isNotValid || StringUtils.isEmpty(form.getEmployeeRepPhone());
		}

		if (isNotValid) {
			MsgUtils.addFacesMessage("ContactFormBean.EmployerSignatureInfo", FacesMessage.SEVERITY_ERROR);
			return false;
		}

		return true;
	}

	/**
	 * Handle the employer signature on the WTPA form, which may happen either
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
		if (contactDoc.getStatus() == ApprovalStatus.PENDING || contactDoc.getSubmitable()) {
			LOG.debug("");
			ContactDocEventDAO.getInstance().createEvent(contactDoc, TimedEventType.SIGN);
		}
		else if (contactDoc.getStatus() == ApprovalStatus.SUBMITTED) {
			contactDoc.setApproverId(null);
			contactDoc.setStatus(ApprovalStatus.APPROVED);
			ContactDocEventDAO.getInstance().createEvent(contactDoc, TimedEventType.SIGN);
			ContactDocEventDAO.getInstance().createEvent(contactDoc, TimedEventType.APPROVE);
		}
	}

	/** Method used to check the validity of fields in the form on Save action.
	 * @return isValid
	 */
	@Override
	public boolean checkSaveValid() {
		LOG.debug("");
		boolean isValid = true;
		isValid &= checkAddressValid(form.getAddress(), "EmployerZipCode");
		isValid &= checkAddressValid(form.getAddress(), "MailingZipCode");
		isValid &= checkDecimalAmounts(); //LS-926 Decimal Fields validation

		/*if (form.getAddress() != null && (! form.getAddress().isZipValidOrEmpty())) {
			isValid = false;
			MsgUtils.addFacesMessage("Form.Address.EmployerZipCode", FacesMessage.SEVERITY_ERROR);
		}
		if (form.getMailingAddress() != null && (! form.getMailingAddress().isZipValidOrEmpty())) {
			isValid = false;
			MsgUtils.addFacesMessage("Form.Address.MailingZipCode", FacesMessage.SEVERITY_ERROR);
		}*/
		if ((!StringUtils.isEmpty(form.getFedidNumber())) && (StringUtils.cleanTaxId(form.getFedidNumber()) == null ||
				StringUtils.cleanTaxId(form.getFedidNumber()).isEmpty())) {
			isValid = false;
			MsgUtils.addFacesMessage("Form.FedidNumber", FacesMessage.SEVERITY_ERROR);
		}
		if (form.getEmployeeDeclinesSign() && (getContactDoc().getSubmitable() || getContactDoc().getStatus() == ApprovalStatus.PENDING)) {
			LOG.debug("declined to sign");
			setContactDoc(ContactDocumentDAO.getInstance().refresh(getContactDoc()));
			PopupBean bean = PopupBean.getInstance();
			if (getvContact().equals(getContactDoc().getContact())) {
				if (! checkSubmitValid()) {
					return false;
				}
				bean.show(this, ACT_DECLINE, "FormW4Bean.EmployeeDeclineSign.");
			}
			else {
				bean.show(this, ACT_DECLINE, "FormW4Bean.EmployerDeclineSign.");
			}
			return false;
		}
		setSaveValid(isValid);
		return super.checkSaveValid();
	}

	/**
	 * Checks the WTPA form to see if the necessary fields have been filled in
	 * before it may be sent to the employee.
	 *
	 * @return True if the necessary fields are filled in, otherwise false.
	 */
	@Override
	public boolean checkSendValid() {
		if (form != null) {
			return form.getHasRequiredFields(contactDoc.getFormType());
		}
		return false;
	}

	/**
	 * Method used to check the validity of fields in the form on Submit action.
	 *
	 * @return isValid
	 */
	@Override
	public boolean checkSubmitValid() {
		LOG.debug("");
		boolean isValid = true;
		String blankField = null;

		//NY WTPA
		if (getContactDoc().getFormType() == PayrollFormType.NY_WTPA) {
			if (! form.getPayNoticeEnglish() && (form.getPayNoticePrimaryLang() == null || form.getPayNoticePrimaryLang().isEmpty())) {
				isValid = false;
				blankField = "Pay Notice Primary Language";
			}
		}
		LOG.debug("valid fields : " + isValid);
		setSubmitValid(isValid);
		if (! isValid) {
			MsgUtils.addFacesMessage("Form.ValidationMessage", FacesMessage.SEVERITY_ERROR, blankField);
			return false;
		}
		return super.checkSubmitValid();
	}

	@Override
	public String confirmOk(int action) {
		String res = null;
		switch(action) {
			case ACT_DECLINE:
				actionDeclineOk();
				break;
//			case ACT_AUTO_FILL:
//				actionAutoFillRepresentativeFieldsOk();
//				break;
			default:
				res = super.confirmOk(action);
				break;
		}
		return res;
	}

	/**
	 * Action method for the "Auto-fill" button for
	 * representative fields of WTPA. This checks the user's
	 * preference, and will either prompt them to OK the auto-fill, or just do
	 * it immediately. In the case of a prompt, we'll be called back via either
	 * the {@link #confirmOk(int)} or {@link #confirmCancel(int)} methods.
	 *
	 * @return null navigation string
	 */
//	public String actionAutoFillRepresentativeFieldsPrompt() {
//		try {
//			LOG.debug("");
//			if (getContactDoc() != null) {
//				boolean hidePrompt = UserPrefBean.getInstance().getBoolean(Constants.ATTR_AUTO_FILL_PROMPT, false);
//				if (! hidePrompt) { // If hidePrompt is false, pop up appears.
//					LOG.debug("");
//					PopupCheckboxBean bean = PopupCheckboxBean.getInstance();
//					bean.prompt(this, ACT_AUTO_FILL, "ContactFormBean.AutoFillWtpaRepresentative.", true, null);
//				}
//				else {
//					LOG.debug("");
//					actionAutoFillRepresentativeFieldsOk();
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
	 * Method called when user clicks Auto-fill button for representative fields
	 * of WTPA. This will populate/fill the representative fields in the form
	 * with the information of the currently logged in approver.
	 *
	 * @return null navigation String
	 */
	public void actionAutoFillRepresentativeFields() {
		try {
			// (not used) save "never show" option in User Preferences.
//			boolean show = PopupCheckboxBean.getInstance().getCheck();
//			UserPrefBean.getInstance().put(Constants.ATTR_AUTO_FILL_PROMPT, show);
			Production production = getProduction();
			Contact currcontact = ContactDAO.getInstance().findByUserProduction(getvUser(), production);
			form.setEmployeeRepName(currcontact.getDisplayName());
			form.setEmployeeRepTitle(currcontact.getRoleName());
			form.setEmployeeRepEmail(currcontact.getEmailAddress());
			if (contactDoc.getFormType() == PayrollFormType.CA_WTPA) {
				form.setEmployeeRepPhone(currcontact.getUser().getPrimaryPhone());
			}
			ContactFormBean bean = ContactFormBean.getInstance();
			if (! bean.getEditMode()) {
				actionSave();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Method used to create string of payroll company and worker's company info for WTPA form.
	 * @return form's payroll information string .
	 */
	public String getPayrollInfo() {
		String payroll = "";
		Production prod = getProduction();
		if (prod != null) {
			PayrollService service = prod.getPayrollPref().getPayrollService();
			if (service != null) {
				payroll = service.getWtpaPayrollCompany();
				if (payroll == null) {
					payroll = "&#160;";
				}
				if(contactDoc != null && contactDoc.getFormType() == PayrollFormType.CA_WTPA &&
						service.getWtpaWorkersComp() != null) {
					payroll += "<br/>" + service.getWtpaWorkersComp();
				}
			}
		}
		return payroll;
	}

	// Value change listeners for the NY WTPA form.

	/** Value change listener for NY and CA WTPA's regular rate text field.
	 * @param evt
	 */
	public void listenChangeRegularRate (ValueChangeEvent evt) {
		try {
			LOG.debug("");
			if (evt.getNewValue() != null) {
				LOG.debug("");
				BigDecimal regRate = (BigDecimal) evt.getNewValue();
				LOG.debug("regular rate : " + regRate);
				if (form.getOvertimeMethod() == PayFrequency.W) {
					BigDecimal otRate = NumberUtils.safeMultiply(regRate, Constants.DECIMAL_ONE_FIVE);
					form.setOvertimeRate(otRate);
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Listener for the Radio buttons for "Overtime Method" in the
	 * “Non-exempt employees section" of Form NY WTPA,
	 *  used to clear the fields for the unselected button.
	 * @param event
	 */
	public void listenChangeOvertime(ValueChangeEvent event) {
		LOG.debug("");
		refreshRadioButtons(event);
		Boolean newValue = (Boolean) event.getNewValue();
		Integer index = null;
		if (event.getComponent() != null) {
			LOG.debug("overtimeFreq[0] = " + overtimeFreq[0]);
			LOG.debug("overtimeFreq[1] = " + overtimeFreq[1]);
			if (overtimeFreq[0] && newValue) {
				form.setOvertimeMethod(PayFrequency.W);
				form.setOvertimeTier1(null);
				form.setOvertimeTier1Amt(null);
				form.setOvertimeTier2(null);
				form.setOvertimeTier2Amt(null);
				if (form.getRegularRate() != null && form.getOvertimeRate() == null) {
					BigDecimal otRate = NumberUtils.safeMultiply(form.getRegularRate(), Constants.DECIMAL_ONE_FIVE);
					form.setOvertimeRate(otRate);
				}
				index = 0;
			}
			else if (overtimeFreq[1] && newValue) {
				form.setOvertimeMethod(PayFrequency.D);
				form.setOvertimeRate(null);
				if (form.getRegularRate() != null && form.getOvertimeTier1Amt() == null) {
					BigDecimal otRate = NumberUtils.safeMultiply(form.getRegularRate(), Constants.DECIMAL_ONE_FIVE);
					form.setOvertimeTier1Amt(otRate);
				}
				index = 1;
			}
			for (int i = 0; i < overtimeFreq.length; i++) {
				if (index != null && i != index) {
					overtimeFreq[i] = false;
				}
			}
		}
	}

	/** Listener for the Radio buttons for "Employee Status" in the
	 * “Employee Info section" of Form NY WTPA,
	 *  used to clear the fields for the unselected button.
	 * @param event
	 */
	public void listenChangeExemptType(ValueChangeEvent event) {
		LOG.debug("");
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// Need to schedule event for later - after "setXxxx()" are called from framework.
			// Simpler to do it once here then for every field on the I9
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		if (event.getComponent() != null) {
			if (form.getExempt()) {
				form.setRegularRate(null);
				form.setOvertimeMethod(null);
				form.setOvertimeRate(null);
				form.setOvertimeTier1(null);
				form.setOvertimeTier1Amt(null);
				form.setOvertimeTier2(null);
				form.setOvertimeTier2Amt(null);
				form.setCbaAgreement(null);
				Arrays.fill(overtimeFreq, Boolean.FALSE);
			}
			else {
				Arrays.fill(nyExemptPay, Boolean.FALSE);
				form.setExemptPayType(null);
				form.setExemptPayAmt(null);
				form.setExemptReason(null);
				form.setCbaAgreement(null);
			}
		}
	}

	/** Value change listener for NY's Exempt Pay Type radio buttons.
	 * @param evt
	 */
	public void listenChangeExemptPayType(ValueChangeEvent evt) {
		try {
			refreshRadioButtons(evt);
			LOG.debug("");
			for (ExemptPayType type : ExemptPayType.values()) {
				LOG.debug("TYPE = " + type.name());
				if (nyExemptPay[type.getIndex()]) {
					form.setExemptPayType(type);
					break;
				}
			}
			for (ExemptPayType type : ExemptPayType.values()) {
				if (type != form.getExemptPayType()) {
					nyExemptPay[type.getIndex()] = false;
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Value change listener for WTPA's Regular Payday radio buttons.
	 * @param evt
	 */
	public void listenPaydayFrequency(ValueChangeEvent evt) {
		try {
			refreshRadioButtons(evt);
			// Due to an apparent bug in ace:buttonGroup, sometimes more than one button is true!
			LOG.debug("regularPayday: " + regularPayday[0] + "," + regularPayday[1] + "," + regularPayday[2] );
			for (PaydayFrequency type : PaydayFrequency.values()) {
				LOG.debug("TYPE = " + type.name());
				if (regularPayday[type.getIndex()]) {
					form.setPaydayFreq(type);
					if (type != PaydayFrequency.O) {
						form.setPaydayFreqOther(null);
					}
					break;
				}
			}
			// The following may help in some cases, by ensuring only one boolean in group
			// is true. However, we still sometimes see 2 radio buttons "on" in UI.
			for (PaydayFrequency type : PaydayFrequency.values()) {
				if (type != form.getPaydayFreq()) {
					regularPayday[type.getIndex()] = false;
				}
			}
			//LOG.debug("regularPayday" + regularPayday[0] + "," + regularPayday[1] + "," + regularPayday[2] );
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	// Value change listeners for the CA WTPA form.

	/** Value change listener for CA's Regular Frequency radio buttons.
	 * @param evt
	 */
	public void listenChangeRegularFrequency(ValueChangeEvent evt) {
		try {
			refreshRadioButtons(evt);
			Boolean newVal = (Boolean) evt.getNewValue();
			LOG.debug("Value = " + newVal);
			PayFrequency payFrequency = null;
			Integer index = null;
			LOG.debug("caRegularFreq: " + caRegularFreq[0] + "," + caRegularFreq[1] + "," + caRegularFreq[2] );
			if (caRegularFreq[0] && newVal) {
				LOG.debug("");
				payFrequency = PayFrequency.H;
				index = 0;
			}
			else if (caRegularFreq[1] && newVal) {
				LOG.debug("");
				payFrequency = PayFrequency.D;
				index = 1;
			}
			else if (caRegularFreq[2] && newVal) {
				LOG.debug("");
				payFrequency = PayFrequency.O;
				index = 2;
			}
			if (payFrequency != PayFrequency.O) {
				form.setRegularFreqOther(null);
			}
			if (payFrequency != null) {
				form.setRegularFreq(payFrequency);
			}

			for (int i = 0; i < caRegularFreq.length; i++) {
				if (index != null && i != index) {
					caRegularFreq[i] = false;
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Value change listener for CA's Overtime Frequency radio buttons.
	 * @param evt
	 */
	public void listenChangOvertimeFrequency(ValueChangeEvent evt) {
		try {
			refreshRadioButtons(evt);
			Boolean newVal = (Boolean) evt.getNewValue();
			LOG.debug(" " + newVal);
			Integer index = null;
			PayFrequency payFrequency = null;
			if (overtimeFreq[0] && newVal) {
				LOG.debug("");
				payFrequency = PayFrequency.H;
				index = 0;
			}
			else if (overtimeFreq[1] && newVal) {
				LOG.debug("");
				payFrequency = PayFrequency.O;
				index = 1;
			}
			if (payFrequency != PayFrequency.O) {
				form.setOvertimeFreqOther(null);
			}
			if (payFrequency != null) {
				form.setOvertimeFreq(payFrequency);
			}
			for (int i = 0; i < overtimeFreq.length; i++) {
				if (index != null && i != index) {
					overtimeFreq[i] = false;
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Value change listener for CA's Sick Leave Type radio buttons.
	 * @param evt
	 */
	public void listenChangSickLeaveType(ValueChangeEvent evt) {
		try {
			refreshRadioButtons(evt);
			LOG.debug("");
			for (CalifSickLeaveType type : CalifSickLeaveType.values()) {
				LOG.debug("TYPE = " + type.name());
				if (caSickLeaveType[type.getIndex()]) {
					form.setSickLeaveType(type);
					if (type != CalifSickLeaveType.EXEMPT) {
						form.setSickLeaveReason(null);
					}
					break;
				}
			}
			for (CalifSickLeaveType type : CalifSickLeaveType.values()) {
				if (type != form.getSickLeaveType()) {
					caSickLeaveType[type.getIndex()] = false;
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**See {@link #regularPayDayWeekDayList}. */
	public List<SelectItem> getRegularPayDayWeekDayList() {
		return Constants.WEEKDAY_NAMES_PLUS_BLANK_DL;
	}

	/**See {@link #caRegularFreq}. */
	public Boolean[] getCaRegularFreq() {
		return caRegularFreq;
	}
	/**See {@link #caRegularFreq}. */
	public void setCaRegularFreq(Boolean[] caRegularFreq) {
		this.caRegularFreq = caRegularFreq;
	}

	/**See {@link #overtimeFreq}. */
	public Boolean[] getOvertimeFreq() {
		return overtimeFreq;
	}
	/**See {@link #overtimeFreq}. */
	public void setOvertimeFreq(Boolean[] overtimeFreq) {
		this.overtimeFreq = overtimeFreq;
	}

	/**See {@link #regularPayday}. */
	public Boolean[] getRegularPayday() {
		return regularPayday;
	}
	/**See {@link #regularPayday}. */
	public void setRegularPayday(Boolean[] regularPayday) {
		this.regularPayday = regularPayday;
	}

	/**See {@link #caSickLeaveType}. */
	public Boolean[] getCaSickLeaveType() {
		return caSickLeaveType;
	}
	/**See {@link #caSickLeaveType}. */
	public void setCaSickLeaveType(Boolean[] caSickLeaveType) {
		this.caSickLeaveType = caSickLeaveType;
	}

	/**See {@link #nyExemptPay}. */
	public Boolean[] getNyExemptPay() {
		return nyExemptPay;
	}
	/**See {@link #nyExemptPay}. */
	public void setNyExemptPay(Boolean[] nyExemptPay) {
		this.nyExemptPay = nyExemptPay;
	}

}
