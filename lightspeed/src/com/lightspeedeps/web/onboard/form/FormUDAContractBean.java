package com.lightspeedeps.web.onboard.form;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.html.HtmlInputText;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.UdaContractTimeSheet;
import com.lightspeedeps.type.ApprovalStatus;
import com.lightspeedeps.type.OfficeType;
import com.lightspeedeps.type.TimedEventType;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.payroll.TimecardUtils;
import com.lightspeedeps.web.onboard.ContactFormBean;

/**
 * Backing bean for the UDA INM Contract form on the Payroll / Start Forms page.
 *
 * @see com.lightspeedeps.model.FormUDAContract
 * @see StandardFormBean
 */
@ManagedBean
@ViewScoped
public class FormUDAContractBean extends StandardFormBean<FormUDAContract> implements Serializable {

	private static final long serialVersionUID = -2104414734423424308L;

	private static final Log LOG = LogFactory.getLog(FormUDAContractBean.class);

	// DAO classes
	private transient FormUDAContractDAO formUdaContractDAO;

	private transient OfficeDAO officeDAO;

	/** */
	List<UdaContractTimeSheet> udaTimesheetList;

	/** backing field for the "Add more info" checkbox on the form. */
	private boolean addMoreInfo = false;



	public List<UdaContractTimeSheet> getUdaTimesheetList() {
		if (udaTimesheetList == null) {
			if (form == null) {
				return new ArrayList<>();
			}
			udaTimesheetList = createTimesheet(addMoreInfo, false);
		}
		return udaTimesheetList;
	}

	public void setUdaTimesheetList(List<UdaContractTimeSheet> udaTimesheetList) {
		this.udaTimesheetList = udaTimesheetList;
	}

	public static FormUDAContractBean getInstance() {
		return (FormUDAContractBean) ServiceFinder.findBean("formUDAContractBean");
	}

	/**
	 * default constructor
	 */
	public FormUDAContractBean() {
		super("FormUDAContractBean");
	}

	@Override
	public String actionCancel() {
		refreshForm();
		super.actionCancel();

		return null;
	}

	@Override
	public String actionEdit() {
		if (form != null && form.getArtistAddress() == null) {
			form.setArtistAddress(new Address());
		}
		if (form != null && form.getProducerAddress() == null) {
			form.setProducerAddress(new Address());
		}
		if (form != null && form.getRecording1() == null) {
			form.setRecording1(new Recording());
		}
		if (form != null && form.getRecording2() == null) {
			form.setRecording2(new Recording());
		}
		if (form != null && form.getRecording3() == null) {
			form.setRecording3(new Recording());
		}
		forceLazyInit();
		calculateEditFlags(false);
		return super.actionEdit();
	}

	private void forceLazyInit() {
		WeeklyTimecard wtc = form.getWeeklyTimecard();
		if(wtc!=null)
		wtc.getDailyTimes().size();
		form.getArtistAddress().getAddrLine1();
		form.getProducerAddress().getAddrLine1();

	}


	@Override
	public String actionSave() {
		WeeklyTimecard wtc = form.getWeeklyTimecard();
		Iterator<UdaContractTimeSheet> iter = udaTimesheetList.iterator();
		if (wtc != null) {
			for (DailyTime dt : wtc.getDailyTimes()) {
				UdaContractTimeSheet entry;
				if (iter.hasNext()) {
					entry = iter.next();
				} else {
					entry = null;
				}
				if (entry != null && (entry.getDateOfRecording() != null || entry.getDateOfRecording() != null)) {
					dt.setDate(entry.getDateOfRecording());
					dt.setCallTime2(entry.getTimeOfArrival());
					dt.setTrvlToLocFrom(entry.getTravelFrom1());
					dt.setTrvlToLocTo(entry.getTravelTo1());
					dt.setCallTime(entry.getCallTime1());
					dt.setM1Out(entry.getM1Out());
					dt.setM1In(entry.getM1In());
					dt.setWrap(entry.getFinishTime1());
				} else { // If date field is blank, clear data
					dt.erase();
					dt.setDate(null);
				}
			}
		}
		if (wtc != null) {
			WeeklyTimecardDAO.getInstance().merge(wtc);
		}
		getFormUdaContractDAO().merge(form);
		return super.actionSave();
	}

	@Override
	public void rowClicked(ContactDocument contactDocument) {
		setContactDoc(contactDocument);
		setUpForm();
	}

	/** Method used to fetch the saved data for the selected form and to set that data in the Form instance. */
	private void setUpForm() {
		try {
			LOG.debug("");
			addMoreInfo = false;
			Integer relatedFormId = contactDoc.getRelatedFormId();
			if (relatedFormId != null) {
				LOG.debug("");
				setForm(getFormUdaContractDAO().findById(relatedFormId));
				if (form !=null && form.getWeeklyTimecard() !=null && form.getWeeklyTimecard().getDailyTimes() !=null && form.getWeeklyTimecard().getDailyTimes().get(3)!=null &&  form.getWeeklyTimecard().getDailyTimes().get(3).getDate() != null) {
					addMoreInfo = true;
				}
			}
			else {
				LOG.debug("");
				setForm(getBlankForm());
			}
			udaTimesheetList = createTimesheet(addMoreInfo, false);

			Office office = getOfficeDAO().findOffice(OfficeType.UDA);
			if(office!=null) {
				form.setOffice(office);
			}

			if (form != null && form.getArtistAddress() == null) {
				form.setArtistAddress(new Address(true));
			}
			if (form != null && form.getProducerAddress() == null) {
				form.setProducerAddress(new Address(true));
			}

			if (relatedFormId == null) {
				LOG.debug("");
				getFormUdaContractDAO().save(form);
				contactDoc.setRelatedFormId(form.getId());
				ContactDocumentDAO.getInstance().merge(contactDoc);
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	@Override
	public FormUDAContract getFormById(Integer id) {
		return FormUDAContractDAO.getInstance().findById(id);
	}

	@Override
	public FormUDAContract getBlankForm() {
		form = new FormUDAContract();
		try {
			LOG.debug("");
			Date todaysDate = CalendarUtils.todaysDate();
			form.setVersion(FormUDAContract.DEFAULT_UDA_CONTRACT_VERSION);
			if (contactDoc != null) {
				Contact ct = contactDoc.getContact();
				Employment emp = contactDoc.getEmployment();
				WeeklyTimecard wtc = TimecardUtils.createUdaTimecard(ct.getUser(),
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

	@Override
	public void refreshForm() {
		if (form != null) {
			form = FormUDAContractDAO.getInstance().refresh(form);
		}
	}

	/**
	 * Auto-fill the UDA form.
	 */
	@Override
	public String autoFillForm(boolean prompted) {
		try {
			populateForm(prompted);
			return super.autoFillForm(prompted);
		} catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** Method used to check the validity of fields in the form.
	 * @return isValid
	 */
	@Override
	public boolean checkSubmitValid() {
		boolean isValid = true;
		User currentUser = SessionUtils.getCurrentUser();
		if (StringUtils.isEmpty(form.getArtistName())) {
			isValid = issueErrorMessage("Artist Name", false, "");
		}
		if (StringUtils.isEmpty(form.getArtistAddress().getAddrLine1())){
			isValid = issueErrorMessage("Artist Address", false, "");
		}
		if (StringUtils.isEmpty(form.getArtistPhone())){
			isValid = issueErrorMessage("Artist Phone", false, "");
		}
		if (StringUtils.isEmpty(form.getArtistEmail())){
			isValid = issueErrorMessage("Artist Email", false, "");
		}
		if (StringUtils.isEmpty(form.getArtistGst())){
			isValid = issueErrorMessage("GST", false, "");
		}
		if (StringUtils.isEmpty(form.getArtistQst())){
			isValid = issueErrorMessage("QST", false, "");
		}
		if (! StringUtils.isEmpty(form.getSocialInsuranceNumber()) && currentUser != null &&
				(! StringUtils.isEmpty(currentUser.getSocialSecurity())) &&
				(! form.getSocialInsuranceNumber().equals(currentUser.getSocialSecurity()))) {
			isValid = issueErrorMessage("", true, ".SocialSecurity");
		}

		if (! (form.isActiveMember() || form.isApprenticeMember() || form.isPermitHolder())) {
			isValid = issueErrorMessage("", false, ".UDAStatus"); // one of the 3 checkboxes must be checked.
		}

		if(form.isLessThan18Years() && form.getDob() == null) {
			isValid = issueErrorMessage("", false, ".MinorWithoutDob");
		}
		setSubmitValid(isValid);
		return super.checkSubmitValid();
	}

	public FormUDAContractDAO getFormUdaContractDAO() {
		if(formUdaContractDAO == null) {
			formUdaContractDAO = FormUDAContractDAO.getInstance();
		}
		return formUdaContractDAO;
	}

	/** Method to pre-populate the form on creation. */
	@Override
	public void populateForm(boolean prompted) {
		LOG.debug(" ");
		Production production = getProduction();
		Project proj = SessionUtils.getCurrentOrViewedProject();
		UdaProjectDetail uproj = proj.getUdaProjectDetail();
		if (uproj == null) {
			LOG.debug("uproj == null");
			return;
		}
		form.setProducerName(uproj.getProducerName());
		/* form.setProducerAddress(uproj.getProducerAddress()); */
		Address producerAddress = createProducerAddress(uproj);
		form.setProducerAddress(producerAddress);
		form.setProducerPhone(uproj.getProducerPhone());
		form.setProducerEmail(uproj.getProducerEmail());
		form.setProdNumber(uproj.getProdNumber());
		form.setResponsibleName(uproj.getResponsibleName());
		form.setAdvertiserName1(uproj.getAdvertiserName());
		form.setCommercialTitle(uproj.getCommercialTitle());
		form.setCommercialDescription(uproj.getCommercialDescription());
		form.setCommercialVersion(uproj.getCommercialVersion());
		form.setProductName1(uproj.getProductName());
		form.setProducerUDA(uproj.getProducerUDA());

		if (getContactDoc() != null) {
			PayrollPreference payrollPref;
			payrollPref = production.getPayrollPref();
			payrollPref = PayrollPreferenceDAO.getInstance().refresh(payrollPref);
			User user = getContactDoc().getContact().getUser();
			user = UserDAO.getInstance().refresh(user);
			form.setArtistName(user.getFirstNameLastName());
			form.setCompanyName(null);
			Address artistAddress = new Address();
			if (user.getHomeAddress() != null) {
				artistAddress = createArtistAddress(user);
			}
			form.setArtistAddress(artistAddress);
			form.setArtistPhone(user.getPrimaryPhone());
			form.setArtistEmail(user.getEmailAddress());
			form.setArtistGst(user.getGstNumber());
			form.setArtistQst(user.getQstNumber());
			form.setSocialInsuranceNumber(user.getSocialSecurity());
			form.setCompanyName(user.getLoanOutCorpName());
			form.setNoUDAMember(user.getUdaMember());
			if (form.getDob() == null) {
				form.setDob(user.getBirthdate());
			}
			form.setLessThan18Years(user.getMinor());
			if (form != null && form.getArtistAddress() == null) {
				form.setArtistAddress(new Address());
			}
			if (form != null && form.getProducerAddress() == null) {
				form.setProducerAddress(new Address());
			}
			if (form != null && form.getRecording1() == null) {
				form.setRecording1(new Recording());
			}
			if (form != null && form.getRecording2() == null) {
				form.setRecording2(new Recording());
			}
			if (form != null && form.getRecording3() == null) {
				form.setRecording3(new Recording());
			}
		}

	}


	/**
	 * Create the Producer's address to be displayed on contract, from
	 * information in the UDA project detail record.
	 *
	 * @param uproj The UdaProjectDetail instance associated with this contract.
	 * @return a new Address object which has not been persisted.
	 */
	private Address createProducerAddress(UdaProjectDetail uproj) {
		Address producerAddress = new Address();
		Address projAddr = uproj.getProducerAddress();

		producerAddress.setAddrLine1(projAddr.getAddrLine1Line2());

		String addressLine2 = projAddr.getCityStateZipCountry();
		producerAddress.setAddrLine2(addressLine2);
		return producerAddress;
	}

	/**
	 * Create the Artist's address to be displayed on the contract, from
	 * information in the User object. LS-2583
	 *
	 * @param user The User instance for the performer/artist this contract is
	 *            associated with.
	 * @return a new Address object which has not been persisted.
	 */
	private Address createArtistAddress(User user) {
		Address artistAddress = new Address();
		Address userAddr = user.getHomeAddress();
		if (userAddr != null) {
			artistAddress.setAddrLine1(userAddr.getAddrLine1Line2());
			artistAddress.setAddrLine2(userAddr.getCityStateZipCountry());
		}
		return artistAddress;
	}

	/** Creates Timecard entry list for the UDA Contract form.
	 * @param moreInfo used to indicate the type of form(Long Uda form or Short Uda form),
	 * which is used to create the specific number of rows.
	 * @param isUpdated, indicates whether to update the Timecard rows or not.
	 * @return the updated or new list of Uda Timecard entries.
	 */
	private List<UdaContractTimeSheet> createTimesheet(Boolean moreInfo, boolean isUpdated) {
		List<UdaContractTimeSheet> list = new ArrayList<>();
		if (isUpdated) {
			LOG.debug("");
			list.addAll(udaTimesheetList);
		}
		boolean changedToLong = (isUpdated && moreInfo);
		int i = 1;
		if ((! isUpdated) || changedToLong) {
			LOG.debug("");
			if (form !=null && form.getWeeklyTimecard() != null && form.getWeeklyTimecard().getDailyTimes() != null) {
				for (DailyTime dt : form.getWeeklyTimecard().getDailyTimes()) {
					// Changed to Long UDA form.
					if (changedToLong && (i <= 4)) {
						i++;
						continue;
					}
					UdaContractTimeSheet item = new UdaContractTimeSheet(dt.getDate(),dt.getCallTime2(),dt.getTrvlToLocFrom(),dt.getTrvlToLocTo(),dt.getCallTime(),dt.getM1Out(),dt.getM1In(),dt.getWrap());
					list.add(item);
					if (i == 4 && ! moreInfo) {
						break;
					}
					i++;
				}
			}
		}

		// Changed to Short UDA form.
		else if (isUpdated && (! moreInfo)) {
			LOG.debug("");
			Iterator<UdaContractTimeSheet> itr = list.iterator();
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
		if (StringUtils.isEmpty(form.getProducerName())) {
			isValid = issueErrorMessage("Producer Name", false, "");
		}
		if (StringUtils.isEmpty(form.getProducerEmail())) {
			isValid = issueErrorMessage("Producer Email", false, "");
		}
		if (StringUtils.isEmpty(form.getResponsibleName())) {
			isValid = issueErrorMessage("Responsible Name", false, "");
		}
		if (StringUtils.isEmpty(form.getProducerAddress().getAddrLine1())){
			isValid = issueErrorMessage("Artist Address Line 1", false, "");
		}
		if (StringUtils.isEmpty(form.getProducerAddress().getAddrLine2())){
			isValid = issueErrorMessage("Artist Address Line 2", false, "");
		}
		if (StringUtils.isEmpty(form.getAdvertiserName1())) {
			isValid = issueErrorMessage("Advertiser Name", false, "");
		}
		if (StringUtils.isEmpty(form.getProductName1())) {
			isValid = issueErrorMessage("Product Name", false, "");
		}
		if (StringUtils.isEmpty(form.getCommercialTitle())) {
			isValid = issueErrorMessage("Commercial Title", false, "");
		}
		if (!form.isFunctionPrincipalPerformer() && !form.isFunctionChorist() && !form.isFunctionSOCPerformer()
				&& !form.isFunctionDemonstrator() && !form.isFunctionVoiceOver() && !form.isFunctionPrincipalExtra()
				&& !form.isFunctionExtra() && !form.isFunctionOther()) {
			isValid = issueErrorMessage("FONCTION/FUNCTION", false, "");
		}
		if (StringUtils.isEmpty(form.getFeeRate())) {
			isValid = issueErrorMessage("Tarrif/Rate", false, "");
		}
		if (form.getNegotiableDateA()==null) {
			isValid = issueErrorMessage("Date A", false, "");
		}
		if (form.getNegotiableDateB()==null) {
			isValid = issueErrorMessage("Date B", false, "");
		}
		setEmployerSignValid(isValid);
		return super.checkEmployerSignValid();
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#checkApproveValid()
	 */
	@Override
	public boolean checkApproveValid() {
		boolean isValid = true;
		if (form.getSignatureDate() == null) { // Timesheet date field must be filled
			isValid = issueErrorMessage("Date", false, "");
		}
		return isValid && super.checkApproveValid();
	}

	/**
	 * Handle the employer signature on the UDA form, which may happen either
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

	public void listenActiveMember(ValueChangeEvent event) {
		form.setApprenticeMember(false);
		form.setActiveMember(true);
		form.setPermitHolder(false);
	}

	public void listenApprenticeMember(ValueChangeEvent event) {
		form.setApprenticeMember(true);
		form.setActiveMember(false);
		form.setPermitHolder(false);
	}

	public void listenPermitHolder(ValueChangeEvent event) {
		form.setApprenticeMember(false);
		form.setActiveMember(false);
		form.setPermitHolder(true);
	}

	public void listenPeriod3month(ValueChangeEvent event) {
		form.setPeriod3month(true);
		form.setPeriod6month(false);
		form.setPeriod9month(false);
	}

	public void listenPeriod6month(ValueChangeEvent event) {
		form.setPeriod3month(false);
		form.setPeriod6month(true);
		form.setPeriod9month(false);
	}

	public void listenPeriod9month(ValueChangeEvent event) {
		form.setPeriod3month(false);
		form.setPeriod6month(false);
		form.setPeriod9month(true);
	}
/*
	public void listenFunctionPrincipalPerformer(ValueChangeEvent event) {
		form.setFunctionPrincipalPerformer(true);
		form.setFunctionChorist(false);
		form.setFunctionSOCPerformer(false);
		form.setFunctionDemonstrator(false);
		form.setFunctionVoiceOver(false);
		form.setFunctionPrincipalExtra(false);
		form.setFunctionExtra(false);
		form.setFunctionOther(false);
		form.setFunctionOtherText("");
		form.setFunctionName("Acteur Principal/Principal Performer");
	}

	public void listenFunctionChorist(ValueChangeEvent event) {
		form.setFunctionPrincipalPerformer(false);
		form.setFunctionChorist(true);
		form.setFunctionSOCPerformer(false);
		form.setFunctionDemonstrator(false);
		form.setFunctionVoiceOver(false);
		form.setFunctionPrincipalExtra(false);
		form.setFunctionExtra(false);
		form.setFunctionOther(false);
		form.setFunctionOtherText("");
		form.setFunctionName("Rôle muet/SOC܆ Performer");
	}

	public void listenFunctionSOCPerformer(ValueChangeEvent event) {
		form.setFunctionPrincipalPerformer(false);
		form.setFunctionChorist(false);
		form.setFunctionSOCPerformer(true);
		form.setFunctionDemonstrator(false);
		form.setFunctionVoiceOver(false);
		form.setFunctionPrincipalExtra(false);
		form.setFunctionExtra(false);
		form.setFunctionOther(false);
		form.setFunctionOtherText("");
		form.setFunctionName("Voix hors champ/ Voice over");
	}

	public void listenFunctionDemonstrator(ValueChangeEvent event) {
		form.setFunctionPrincipalPerformer(false);
		form.setFunctionChorist(false);
		form.setFunctionSOCPerformer(false);
		form.setFunctionDemonstrator(true);
		form.setFunctionVoiceOver(false);
		form.setFunctionPrincipalExtra(false);
		form.setFunctionExtra(false);
		form.setFunctionOther(false);
		form.setFunctionOtherText("");
		form.setFunctionName("Figurant principal/Principal extra");
	}

	public void listenFunctionVoiceOver(ValueChangeEvent event) {
		form.setFunctionPrincipalPerformer(false);
		form.setFunctionChorist(false);
		form.setFunctionSOCPerformer(false);
		form.setFunctionDemonstrator(false);
		form.setFunctionVoiceOver(true);
		form.setFunctionPrincipalExtra(false);
		form.setFunctionExtra(false);
		form.setFunctionOther(false);
		form.setFunctionOtherText("");
		form.setFunctionName("Figurant/Extra");
	}

	public void listenFunctionPrincipalExtra(ValueChangeEvent event) {
		form.setFunctionPrincipalPerformer(false);
		form.setFunctionChorist(false);
		form.setFunctionSOCPerformer(false);
		form.setFunctionDemonstrator(false);
		form.setFunctionVoiceOver(false);
		form.setFunctionPrincipalExtra(true);
		form.setFunctionExtra(false);
		form.setFunctionOther(false);
		form.setFunctionOtherText("");
		form.setFunctionName("Choriste/Chorist");
	}

	public void listenFunctionExtra(ValueChangeEvent event) {
		form.setFunctionPrincipalPerformer(false);
		form.setFunctionChorist(false);
		form.setFunctionSOCPerformer(false);
		form.setFunctionDemonstrator(false);
		form.setFunctionVoiceOver(false);
		form.setFunctionPrincipalExtra(false);
		form.setFunctionExtra(true);
		form.setFunctionOther(false);
		form.setFunctionOtherText("");
		form.setFunctionName("Démonstrateur/܆ Demonstrator");
	}
*/
	public void listenFunctionOther(ValueChangeEvent event) {
		Boolean checked = (Boolean)event.getNewValue();

		if(!checked) {
			HtmlInputText otherText = (HtmlInputText)event.getComponent().findComponent("functionOtherText");
			if(otherText != null) {
				otherText.setValue("");
			}
		}
	}

	public void listenLodgingAndMeal(ValueChangeEvent event) {
		form.setLodgingAndMealsNumber(null);
		form.setLodgingAndMealsAmount(null);
		form.setLodgingAndMealsTotal(BigDecimal.ZERO);
		form.setMealOnlyNumber(null);
		form.setMealOnlyAmount(null);
		form.setMealOnlyTotal(BigDecimal.ZERO);
	}

	private OfficeDAO getOfficeDAO() {
		if (officeDAO == null) {
			officeDAO = OfficeDAO.getInstance();
		}
		return officeDAO;
	}
	
	//Add listner for empty text box LogementRepas / RepasMeals
	public void listenLogementRepas(ValueChangeEvent event) {
		form.setLodgingAndMealsNumber(null);
		form.setLodgingAndMealsAmount(null);
		form.setLodgingAndMealsTotal(null);
	}
	
	public void listenRepasMealsOnly(ValueChangeEvent event) {
		form.setMealOnlyNumber(null);
		form.setMealOnlyAmount(null);
		form.setMealOnlyTotal(null);
	}
}
