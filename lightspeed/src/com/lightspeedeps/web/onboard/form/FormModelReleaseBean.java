package com.lightspeedeps.web.onboard.form;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.*;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.*;
import org.json.*;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.model.*;
import com.lightspeedeps.service.*;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.*;
import com.lightspeedeps.web.onboard.ContactFormBean;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.validator.PhoneNumberValidator;

@ManagedBean
@ViewScoped
public class FormModelReleaseBean extends StandardFormBean<FormModelRelease> implements Serializable {

	/**
	 * default constructor
	 */
	public FormModelReleaseBean() {
		super("FormModelReleaseBean");
	}
	private static final Log LOG = LogFactory.getLog(FormModelReleaseBean.class);

	public static FormModelReleaseBean getInstance() {
		return (FormModelReleaseBean) ServiceFinder.findBean("formModelReleaseBean");
	}
	/**
	 *
	 */
	private static final long serialVersionUID = -6825029144953574102L;

	private static final int ACT_WARNING_POPUP = 20;

	private String JOB_STATE = "CA";

	private static final String OTHER_CITY_FIELD = "Other";

	// DAO classes
	private transient FormModelReleaseDAO formModelReleaseDAO;

	/** Instance of current Production. */
	private Production production;

	private List<SelectItem> agencyList;
	private static String AGENCY_LOOKUP_PATH = null;
	private static final String AGENCY_LOOKUP_PARAM_PATTERN = "agents?namecontains=";

	public  FormModelReleaseDAO getFormModelReleaseDAO() {
		if( formModelReleaseDAO == null) {
			formModelReleaseDAO = FormModelReleaseDAO.getInstance();
		}
		return formModelReleaseDAO;
	}


	@Override
	public void rowClicked(ContactDocument contactDocument) {
		// resetting this to avoid rendering other city input box for incomplete or empty forms 
		setShowOtherCities(false);
		setContactDoc(contactDocument);
		setUpForm();
	}

	/**
	 * Global class variable used to render cities drop down
	 */
	private List<SelectItem> cities = null;
	
	/**
	 * Boolean field used to manage the cities drop down, set to false if no cities are available for a state
	 */
	private boolean displayCities = false;
	
	/**
	 * Boolean field used to render other city field when <b>Other</b> is selected in cities drop down 
	 */
	private boolean showOtherCities = false;

	public List<SelectItem> getCities() {
		return cities;
	}

	public void setCities(List<SelectItem> cities) {
		this.cities = cities;
	}

	public boolean getDisplayCities() {
		return displayCities;
	}

	public void setDisplayCities(boolean displayCities) {
		this.displayCities = displayCities;
	}
    
	public boolean isShowOtherCities() {
		return showOtherCities;
	}

	public void setShowOtherCities(boolean showOtherCities) {
		this.showOtherCities = showOtherCities;
	}
	
	/**
	 * Method used to fetch the saved data for the selected form and to set that
	 * data in the Form instance.
	 */
	private void setUpForm() {
		Integer relatedFormId = contactDoc.getRelatedFormId();
		if (relatedFormId != null) {
			form = getFormById(relatedFormId);
			form.getProducerAddress().getAddrLine1();
			form.getModelAddress().getAddrLine1();
			resetAttachmentDisclaimer();
			if(form.getJobCity() != null && form.getJobCity().equals(OTHER_CITY_FIELD)) {
				setShowOtherCities(true);
			}
			setForm(form);
		}
		else {
			form = getBlankForm();
			populateForm(false);
		}

		if (form.getProducerAddress() == null) {
			form.setProducerAddress(new Address());
		}
		if (form.getModelAddress() == null) {
			form.setModelAddress(new Address());
		}

		if (relatedFormId == null) {
			// LS-3105 Call api to create new form.
			form =  (FormModelRelease)FormService.getInstance().persistForm(form, PayrollFormType.MODEL_RELEASE.getApiSaveUrl(), FormModelRelease.class);
			contactDoc.setRelatedFormId(form.getId());
			ContactDocumentDAO.getInstance().merge(contactDoc);
		}
	}

	/**
	 * Method checks validates if the attachment uploaded against a document
	 * are by employer or not.
	 * If the attachment is uploaded by an employer then the disclaimer is turned ON else
	 * if not uploaded not any attachment exists after delete then the disclaimer is turned OFF
	 */
	private void resetAttachmentDisclaimer() {
		if (! contactDoc.getAttachments().isEmpty()) {
			Integer userId = contactDoc.getContact().getUser().getId();
			boolean isEmployerAttach = false;
			for (Attachment attachment : contactDoc.getAttachments()) {
				if (attachment.getUser().getId().compareTo(userId) != 0) {
					isEmployerAttach = true;
					break;
				}
			}
			form.setEmployerAttachDoc(isEmployerAttach);
			FormService.getInstance().update(form, PayrollFormType.MODEL_RELEASE.getApiUpdateUrl(), FormModelRelease.class);
		}
	}


	@Override
	public String actionEdit() {
		if(form.getTermDate()!=null) {
	       	Date termDate=java.util.Date.from(form.getTermDate().atStartOfDay()
	  		      .atZone(ZoneId.systemDefault()).toInstant());
	        form.setTermRefDate(termDate);
        }

		if(form.getTermYear()!=null) {
        	Date termYear=java.util.Date.from(form.getTermYear().atStartOfDay()
        		      .atZone(ZoneId.systemDefault())
        		      .toInstant());
        	form.setTermRefYear(termYear);
        }
		calculateEditFlags(false);
		return super.actionEdit();
	}

	@Override
	public String actionCancel() {
		refreshForm();

		return super.actionCancel();
	}
	/**
	 * Retrieve the form from the id associated with the form.
	 * Use micro-service to retrieve the form.
	 *
	 * LS-3060
	 */
	@Override
	public FormModelRelease getFormById(Integer id) {
		return (FormModelRelease)FormService.getInstance().findById(id, PayrollFormType.MODEL_RELEASE.getApiFindUrl(), FormModelRelease.class);
	}

	@Override
	public FormModelRelease getBlankForm() {
		FormModelRelease formModelRelease = new FormModelRelease();
		formModelRelease.setProducerAddress(new Address());
		formModelRelease.setModelAddress(new Address());
		formModelRelease.setProducer(false);
		formModelRelease.setUsageNo(true);
		formModelRelease.setCompensationYes(true);

		formModelRelease.setVersion(FormModelRelease.DEFAULT_MR_VERSION);
		Production production = getProduction();
		//LS-3187 add production name in ttc client
		if (production.getTitle() != null) {
			formModelRelease.setTtcClient(production.getTitle());
		}
		//LS-3219 add job name
		Project project = SessionUtils.getCurrentProject();
		if (project!=null && project.getEpisode() != null) {
			formModelRelease.setJob(project.getEpisode());
		}
		//LS-3709 add default age
		formModelRelease.setAge("18");
		formModelRelease.setUnderAge("18");

		// LS-4449 add default value
		formModelRelease.setModelAgencyNo(true);
		formModelRelease.setModelAgencyText("NA");

		return formModelRelease;
	}

	@Override
	public void refreshForm() {
		if (form != null) {
			form = (FormModelRelease)FormService.getInstance().findById(form.getId(), PayrollFormType.MODEL_RELEASE.getApiFindUrl(), FormModelRelease.class);
		}
	}

	@Override
	public String actionSave() {
		try {
			LocalDate termDate = null;
			if (form.getTermRefDate() != null) {
				Instant instant = Instant.ofEpochMilli(form.getTermRefDate().getTime());
				LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
				termDate = localDateTime.toLocalDate();

			}
			form.setTermDate(termDate);

			LocalDate termYear = null;
			if (form.getTermRefYear() != null) {
				Instant instant = Instant.ofEpochMilli(form.getTermRefYear().getTime());
				LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
				termYear = localDateTime.toLocalDate();
			}
			form.setTermYear(termYear);

			PayrollPreference pr = getProduction().getPayrollPref();
			pr = PayrollPreferenceDAO.getInstance().refresh(pr);
			if (pr.getUseModelRelease() && FF4JUtils.useFeature(FeatureFlagType.TTCO_MRF_STARTS_AND_TIMECARDS)) {
				Employment emp = getContactDoc().getEmployment();
				StartForm sf = form.getStartForm();
				if (sf == null) {
					Contact ct = getContactDoc().getContact();
					ct = ContactDAO.getInstance().refresh(ct);
					sf = StartFormService.createFirstStartForm(ct, emp, getContactDoc().getProject());
					StartFormDAO.getInstance().createContactDocument(sf, getProduction());
					form.setStartForm(sf);
				}
				sf = StartFormService.updateStartForm(sf, form);
			}

			FormService.getInstance().update(form, PayrollFormType.MODEL_RELEASE.getApiUpdateUrl(), FormModelRelease.class);

			return super.actionSave();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
			return Constants.ERROR_RETURN;
		}
	}

	/**
	 * Specific processing for Model Release upon submit:
	 * <ul>
	 * <li>Set Model Release approved if employer previously signed</li>
	 * <li>issue info message about attachments</li>
	 * </ul>
	 */
	@Override
	public void submitted() {
		//LS-3170 Add special Submit processing for model release
		if (contactDoc.getEmployerSignature() != null) {
			contactDoc.setStatus(ApprovalStatus.APPROVED);
			contactDoc.setApproverId(null);
			getContactDocumentDAO().attachDirty(contactDoc);
		}
	}

	@Override
	public boolean checkSaveValid() {
		boolean isValid = true;
		if (!StringUtils.isEmpty(form.getProducerPhone())) {
			if (!PhoneNumberValidator.isValid((form.getProducerPhone()))) {
				 isValid = false;
				 MsgUtils.addFacesMessage("Form.getProducerPhone",FacesMessage.SEVERITY_ERROR);
			}
		}
		if (!StringUtils.isEmpty(form.getModelAgencyPhone())) {
			if (!PhoneNumberValidator.isValid(form.getModelAgencyPhone())) {
				 isValid = false;
				 MsgUtils.addFacesMessage("Form.getModelAgencyPhone",FacesMessage.SEVERITY_ERROR);
			}
		}
		if (!StringUtils.isEmpty(form.getModelPhone())) {
			if (!PhoneNumberValidator.isValid(form.getModelPhone())) {
				 isValid = false;
				 MsgUtils.addFacesMessage("Form.getModelPhone",FacesMessage.SEVERITY_ERROR);
			}
		}
		if (form.getCorporationFederalId() !=null && !form.getCorporationFederalId().equals("N/A")) {
			if (!StringUtils.isEmpty(form.getCorporationFederalId())) {
				if (StringUtils.cleanTaxId(form.getCorporationFederalId()).isEmpty()) {
					isValid = false;
					MsgUtils.addFacesMessage("Form.FedidNumber", FacesMessage.SEVERITY_ERROR);
				}
			}
			else {
				isValid = false;
				MsgUtils.addFacesMessage("Form.FedidNumber", FacesMessage.SEVERITY_ERROR);
			}
		}
		if (StringUtils.isEmpty(form.getModelSSN())) {
			isValid = issueErrorMessage("Model SSN", false, "");
		}

		if (form.getPerdiemAmount() != null) {
			if (form.getPerdiemDays()==null) {
				isValid = issueErrorMessage("Per Diem day", false, "");
			}
			if (!form.getLodging() &&  !form.getMealsIncidentals()) {
				isValid = false;
				MsgUtils.addFacesMessage("MandatoryCheckBox", FacesMessage.SEVERITY_ERROR);
			}
		}
        //LS-3220 Validation Checks
		if (form.getMediaOther() && StringUtils.isEmpty(form.getMediaOtherText())) {
			isValid = issueErrorMessage("Media Other", false, "");
		}

		if (form.getTermOther() && StringUtils.isEmpty(form.getTermOtherText())) {
			isValid = issueErrorMessage("Term Other", false, "");
		}

		if (form.getTerritoryOther() && StringUtils.isEmpty(form.getTerritoryOtherText())) {
			isValid = issueErrorMessage("Territory Other", false, "");
		}

		if (form.getPerDay() && form.getServiceHours() == null) {
			isValid = issueErrorMessage("Per Day Hours", false, "");
		}

		//LS-3228 Add checks for Taxable and Non Taxable field
		if (form.getTaxable()) {
			if (form.getTaxableTotal()==null) {
				isValid = issueErrorMessage("Taxable Total", false, "");
			}
		}

		if (form.getNonTaxable()) {
			if (form.getNonTaxableTotal()==null) {
				isValid = issueErrorMessage("Non Taxable Total", false, "");
			}
		}
		//3289 Add checks for Usage Yes
		if (form.getUsageYes()) {
			if (!form.getUnlimited() && !form.getUnlimitedExceptOutdoor() && !form.getElectricMedia()
					&& !form.getBroll() && !form.getDirectMail() && !form.getCollateral() && !form.getIsm()
					&& !form.getPr() && !form.getPrint() && !form.getCircular() && !form.getIndustrial()
					&& !form.getMediaOther()) {
				isValid = issueErrorMessage("Media", false, "");
			}

			if (!form.getTerm() && !form.getUnlimitedTime()  && !form.getTermOther()) {
				isValid = issueErrorMessage("Term", false, "");
			}
			if (!form.getNorthAmerica() && !form.getEuropeanUnion() && !form.getSouthAmerica() && !form.getAsia()
					&& !form.getWorldWide() && !form.getWorldWideElectronicMedia() && !form.getTerritoryOther()) {
				isValid = issueErrorMessage("Territory", false, "");
			}
		}

		//Add validation for Compensation
		if (form.getCompensationYes()) {
			if (!(form.getRateForService() != null) && !form.getPerDay() && !form.getPerHour() && !form.getRightTerm()
					&& !form.getNotrightTerm() && !(form.getUsage() != null)) {
				if (form.getAddlYes()) {
					if (!form.getAddlOverTime() && !form.getShootDay() && !form.getWeatherDay()
							&& !form.getIntimatesDay()) {
						isValid = false;
						MsgUtils.addFacesMessage("Form.AddlValidation", FacesMessage.SEVERITY_ERROR);
						MsgUtils.addFacesMessage("Form.CompensationValidation", FacesMessage.SEVERITY_ERROR);
					}
				}
				else {
					isValid = false;
					MsgUtils.addFacesMessage("Form.CompensationValidation", FacesMessage.SEVERITY_ERROR);
				}
			}
			else if (form.getAddlYes()) {
				if (!form.getAddlOverTime() && !form.getShootDay() && !form.getWeatherDay()
						&& !form.getIntimatesDay()) {
					isValid = false;
					MsgUtils.addFacesMessage("Form.AddlValidation", FacesMessage.SEVERITY_ERROR);
				}
			}
		}

		setSaveValid(isValid);
		return super.checkSaveValid();
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
		if (StringUtils.isEmpty(form.getModelName())) {
			isValid = issueErrorMessage("Model", false, "");
		}
		if (StringUtils.isEmpty(form.getModelSSN())) {
			isValid = issueErrorMessage("Model SSN", false, "");
		}
		if (StringUtils.isEmpty(form.getProjectName())) {
			isValid = issueErrorMessage("Project Name", false, "");
		}
		if (StringUtils.isEmpty(form.getShootDates())) {
			isValid = issueErrorMessage("Shoot Dates", false, "");
		}
		if (StringUtils.isEmpty(form.getJobState())) {
			isValid = issueErrorMessage("Job State", false, "");
		}
		if (StringUtils.isEmpty(form.getJobCity())) {
			isValid = issueErrorMessage("Job City", false, "");
		} else if (! LocationUtils.validateCityState(form.getJobCity(), form.getJobState())) {
			String formatMessage = MsgUtils.formatMessage("Address.CityStateMisMatched", form.getJobCity(), form.getJobState());
			isValid = issueErrorMessage(formatMessage, false, "");
		} else if (form.getJobCity().equals(OTHER_CITY_FIELD) && StringUtils.isEmpty(form.getOtherCityName())) {
			isValid = issueErrorMessage("Other City", false, "");
		}
		if (form.getUsageYes()) {
			if (!form.getUnlimited() && !form.getUnlimitedExceptOutdoor() && !form.getElectricMedia()
					&& !form.getBroll() && !form.getDirectMail() && !form.getCollateral() && !form.getIsm()
					&& !form.getPr() && !form.getPrint() && !form.getCircular() && !form.getIndustrial()
					&& !form.getMediaOther()) {
				isValid = issueErrorMessage("Media", false, "");
			}

			if (!form.getTerm() && !form.getUnlimitedTime()  && !form.getTermOther()) {
				isValid = issueErrorMessage("Term", false, "");
			}
			if (!form.getNorthAmerica() && !form.getEuropeanUnion() && !form.getSouthAmerica() && !form.getAsia()
					&& !form.getWorldWide() && !form.getWorldWideElectronicMedia() && !form.getTerritoryOther()) {
				isValid = issueErrorMessage("Territory", false, "");
			}
		}

		if (StringUtils.isEmpty(form.getProducerPrintName())) {
			isValid = issueErrorMessage("Print Name", false, "");
		}
		if (StringUtils.isEmpty(form.getModelAgencyCompany())) {
			isValid = issueErrorMessage("Company", false, "");
		}

		if (!StringUtils.isEmpty(form.getProducerPhone())) {
			if (!PhoneNumberValidator.isValid(form.getProducerPhone())) {
				isValid = false;
				MsgUtils.addFacesMessage("Form.getProducerPhone", FacesMessage.SEVERITY_ERROR);
			}
		}
		else {
			isValid = issueErrorMessage("Producer Phone", false, "");
		}

		if (StringUtils.isEmpty(form.getProducerEmail())) {
			isValid = issueErrorMessage("Email", false, "");
		}

		if (form.getPerdiemAmount() != null) {
			if (form.getPerdiemDays()==null) {
				isValid = issueErrorMessage("Per Diem day", false, "");
			}
			if (!form.getLodging() &&  !form.getMealsIncidentals()) {
				isValid = false;
				MsgUtils.addFacesMessage("MandatoryCheckBox", FacesMessage.SEVERITY_ERROR);
			}
		}
		// LS-3220 Validation Checks
		if (form.getMediaOther() && StringUtils.isEmpty(form.getMediaOtherText())) {
			isValid = issueErrorMessage("Media Other", false, "");
		}

		if (form.getTermOther() && StringUtils.isEmpty(form.getTermOtherText())) {
			isValid = issueErrorMessage("Term Other", false, "");
		}

		if (form.getTerritoryOther() && StringUtils.isEmpty(form.getTerritoryOtherText())) {
			isValid = issueErrorMessage("Territory Other", false, "");
		}

		if (form.getPerDay() && form.getServiceHours() == null) {
			isValid = issueErrorMessage("Per Day Hours", false, "");
		}

		//LS-3228 Add checks for Taxable and Non Taxable field
		if (form.getTaxable()) {
			if (form.getTaxableTotal()==null) {
				isValid = issueErrorMessage("Taxable Total", false, "");
			}
		}
		if (form.getNonTaxable()) {
			if (form.getNonTaxableTotal()==null) {
				isValid = issueErrorMessage("Non Taxable Total", false, "");
			}
			if (contactDoc.getAttachments()!=null && contactDoc.getAttachments().size()>0) {
				MsgUtils.addFacesMessage("Form.attachmentExist", FacesMessage.SEVERITY_INFO);
			}
		}
		//Add validation for Compensation
		if (form.getCompensationYes()) {
			if (!(form.getRateForService() != null) && !form.getPerDay() && !form.getPerHour() && !form.getRightTerm()
					&& !form.getNotrightTerm() && !(form.getUsage() != null)) {
				if (form.getAddlYes()) {
					if (!form.getAddlOverTime() && !form.getShootDay() && !form.getPrepDay() && !form.getWeatherDay()
							&& !form.getIntimatesDay() && !form.getTravelDayRate()) {
						isValid = false;
						MsgUtils.addFacesMessage("Form.AddlValidation", FacesMessage.SEVERITY_ERROR);
						MsgUtils.addFacesMessage("Form.CompensationValidation", FacesMessage.SEVERITY_ERROR);
					}
				}
				else {
					isValid = false;
					MsgUtils.addFacesMessage("Form.CompensationValidation", FacesMessage.SEVERITY_ERROR);
				}
			}
			else if (form.getAddlYes()) {
				if (!form.getAddlOverTime() && !form.getShootDay() && !form.getPrepDay() && !form.getWeatherDay()
						&& !form.getIntimatesDay() && !form.getTravelDayRate()) {
					isValid = false;
					MsgUtils.addFacesMessage("Form.AddlValidation", FacesMessage.SEVERITY_ERROR);
				}
			}

		}
		if (form.getAddlOverTime() && form.getAddlPerHour() == null) {
			isValid = issueErrorMessage("Overtime per hour", false, "");
		}

		if (form.getOptionalReuseYes()) {
			PopupBean bean = PopupBean.getInstance();
			String prefix = "ModelReleaseListBean.SendPendingDocs.";
			if ((form.getReuseMedia().isEmpty())){
				isValid = openPopupWarning(bean, prefix);
			}
			else if ((form.getReuseTerm().isEmpty())){
				isValid = openPopupWarning(bean, prefix);
			}
			else if ((form.getReuseMedia().isEmpty())){
				isValid = openPopupWarning(bean, prefix);
			}
			else if ((form.getReuseFee().isEmpty())){
				isValid = openPopupWarning(bean, prefix);
			}
		}
		//LS-4603 Add validation for Occupation
		if (StringUtils.isEmpty(form.getOccupation())) {
			isValid = issueErrorMessage("Occupation", false, "");
		}

		//LS-4819 added validation for Rate for service when shoot use and Compensation Yes is selected
		if (form.isShootUse() && form.getCompensationYes() && form.getRateForService() == null) {
			isValid = issueErrorMessage("Rate for services", false, "");
		}

        setEmployerSignValid(isValid);
		return super.checkEmployerSignValid();
	}


	private boolean openPopupWarning(PopupBean bean, String prefix) {
		bean.show(this, ACT_WARNING_POPUP, prefix + "Title", prefix + "Text", prefix + "Ok", prefix + "Cancel");
		return false;
	}


	/** Method used to check the validity of fields in the form.
	 * @return isValid
	 */
	@Override
	public boolean checkSubmitValid() {
		boolean isValid = true;
		if (StringUtils.isEmpty(form.getModelName())) {
			isValid = issueErrorMessage("Model", false, "");
		}
		if (StringUtils.isEmpty(form.getModelPrintName())) {
			isValid = issueErrorMessage("Print Name", false, "");
		}
		if (form.getModelEmail()==null) {
			isValid = issueErrorMessage("Email", false, "");
		}
		if (StringUtils.isEmpty(form.getModelSSN())) {
			isValid = issueErrorMessage("Model SSN", false, "");
		}

		if (form.getPerdiemAmount() != null) {
			if (form.getPerdiemDays()==null) {
				isValid = issueErrorMessage("Per Diem day", false, "");
			}
			if (!form.getLodging() &&  !form.getMealsIncidentals()) {
				isValid = false;
				MsgUtils.addFacesMessage("MandatoryCheckBox", FacesMessage.SEVERITY_ERROR);
			}
		}
		//LS-3228 Add checks for Taxable and Non Taxable field
		if (form.getTaxable()) {
			if (form.getTaxableTotal()==null) {
				isValid = issueErrorMessage("Taxable Total", false, "");
			}
		}
		if (form.getNonTaxable()) {
			if (form.getNonTaxableTotal() == null) {
				isValid = issueErrorMessage("Non Taxable Total", false, "");
			}
			if (contactDoc.getAttachments().isEmpty()) {
				isValid = false;
				MsgUtils.addFacesMessage("Form.modelreleaseUploadFile", FacesMessage.SEVERITY_ERROR);
			}
		}

		if (!form.isAcknowledged()) {
			isValid = false;
			MsgUtils.addFacesMessage("Form.Acknowledged", FacesMessage.SEVERITY_ERROR);
		}

		// LS-4449 Add validation for Model Agency
		if (form.getModelAgencyYes()) {
			if (StringUtils.isEmpty(form.getModelAgencyStreet())) {
				isValid = issueErrorMessage("Model Agency Street", false, "");
			}
			if (StringUtils.isEmpty(form.getModelAgencyPhone())) {
				isValid = issueErrorMessage("Model Agency Phone", false, "");
			}
			if (StringUtils.isEmpty(form.getModelAddress().getAddrLine1())) {
				isValid = issueErrorMessage("Model Agency City/State/Zip", false, "");
			}
		}

		//LS-4447 Add validation check for model Email
		if (StringUtils.isEmpty(form.getModelEmail())) {
			isValid = issueErrorMessage("Model Email", false, "");
		}

		setSubmitValid(isValid);
		return super.checkSubmitValid();
	}

	/**
	 * Auto-fill the ModelRelease form.
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

	/** Method to pre-populate the form on creation. */
	@Override
	public void populateForm(boolean prompted) {
		LOG.debug(" ");
		Production production = getProduction();
		PayrollPreference payrollPref;
		payrollPref = production.getPayrollPref();
		payrollPref = PayrollPreferenceDAO.getInstance().refresh(payrollPref);
		User user = getContactDoc().getContact().getUser();
		user = UserDAO.getInstance().refresh(user);
		form.setModelName(user.getFirstNameLastName());
		form.setModelPrintName(user.getFirstNameLastName());
		form.setModelSSN(user.getSocialSecurity());
		// LS-3287 set check boxes depending on whether Loan fields are set on my account tab
		if(! StringUtils.isEmpty(user.getLoanOutCorpName())) {
			form.setModelCorporationText(user.getLoanOutCorpName());
			form.setModelCorporationYes(true);
			form.setModelCorporationNo(false);
		}
		else {
			form.setModelCorporationText("N/A");
			form.setModelCorporationYes(false);
			form.setModelCorporationNo(true);
		}
		if(! StringUtils.isEmpty(user.getFederalTaxId())) {
			form.setCorporationFederalId(user.getFederalTaxId());
			form.setCorporationFederalYes(true);
			form.setCorporationFederalNo(false);
		}
		else {
			form.setCorporationFederalId("N/A");
			form.setCorporationFederalYes(false);
			form.setCorporationFederalNo(true);
		}

		//LS-3287  Add model Phone number
		if (user.getCellPhone() != null) {
			form.setModelPhone(user.getPrimaryPhone());
		}

		form.setModelEmail(user.getEmailAddress());

		//LS-3287  Add Producer Print Name
		User curUser = SessionUtils.getCurrentUser();
		form.setProducerPrintName(curUser.getFirstNameLastName());

		//LS-3219 add job name
		Project project = SessionUtils.getCurrentProject();
		if (project != null && project.getEpisode() != null) {
			form.setJob(project.getEpisode());
			form.setProjectName(project.getTitle());
			// LS-3287 Concatenate project start and end dates for shoot dates
			Date projectStartDate = project.getUnit(1).getProjectSchedule().getStartDate();
			String startDate = (projectStartDate != null) ? CalendarUtils.formatDate(projectStartDate, "MM/dd/yyyy") : "";
			Date projectOrigEndDate = project.getOriginalEndDate();
			String endDate = (projectOrigEndDate != null) ? CalendarUtils.formatDate(projectOrigEndDate, "MM/dd/yyyy") : "";
			form.setShootDates(startDate + ((startDate.length() > 1) ? " " : "") + endDate);
		}
		
		//LS-4719  Populate company Name
		form.setModelAgencyCompany(production.getTitle());
	}

	public void listenTerm(ValueChangeEvent event) {
		form.setUnlimitedTime(false);
		form.setTermOther(false);
		form.setTermOtherText(null);
	}

	public void listenUnlimitedTime(ValueChangeEvent event) {
		form.setTerm(false);
		form.setTermOther(false);
		form.setTermMonth(null);
		form.setTermDate(null);
		form.setTermYear(null);
		form.setTermOtherText(null);
	}

	public void listenTermOther(ValueChangeEvent event) {
		form.setUnlimitedTime(false);
		form.setTerm(false);
		form.setTermMonth(null);
		form.setTermDate(null);
		form.setTermYear(null);
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
				ContactDocEventDAO.getInstance().createEvent(contactDoc, TimedEventType.APPROVE);
			}
			else if (contactDoc.getStatus() == ApprovalStatus.SUBMITTED) {
				contactDoc.setApproverId(null);
				contactDoc.setStatus(ApprovalStatus.APPROVED);
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

		//LS-4539 update EmployerAttachDoc
		resetAttachmentDisclaimer();

		// LS-3834
		notifyEmployee();
	}

	private void notifyEmployee() {
		boolean isNotificationRequired = false;
		for (ContactDocEvent evt : contactDoc.getContactDocEvents()) {
			if (evt.getType() == TimedEventType.RECALL) {
				isNotificationRequired = true;
				break;
			}
		}
		if (isNotificationRequired) {
			LOG.debug("Sending notification to employee since a recall event was found for the current form");
			List<String> distributedDocumentList = new ArrayList<>();
			distributedDocumentList.add(contactDoc.getDocument().getNormalizedName());
			// send notification to employee
			DoNotification.getInstance().documentDistributed(contactDoc.getEmployment().getContact(), contactDoc.getContact(), 1, distributedDocumentList);
		}
	}


	public void listenModelAgencyYes(ValueChangeEvent event) {
		form.setModelAgencyYes(true);
		form.setModelAgencyNo(false);
		form.setModelAgencyText("");
	}

	public void listenModelAgencyNo(ValueChangeEvent event) {
		form.setModelAgencyYes(false);
		form.setModelAgencyNo(true);
		form.setModelAgencyText("N/A");
	}

	public void listenCorporationFederalYes(ValueChangeEvent event) {
		form.setCorporationFederalYes(true);
		form.setCorporationFederalNo(false);
		form.setCorporationFederalId("");
	}

	public void listenCorporationFederalNo(ValueChangeEvent event) {
		form.setCorporationFederalYes(false);
		form.setCorporationFederalNo(true);
		form.setCorporationFederalId("N/A");
	}

	public void listenModelCorporationYes(ValueChangeEvent event) {
		form.setModelCorporationYes(true);
		form.setModelCorporationNo(false);
		form.setModelCorporationText("");
	}

	public void listenModelCorporationNo(ValueChangeEvent event) {
		form.setModelCorporationYes(false);
		form.setModelCorporationNo(true);
		form.setModelCorporationText("N/A");
	}

	public void listenUsageYes(ValueChangeEvent event) {
		if (form.getUsageNo() == form.getUsageYes()) {
			form.setUsageNo(false);
			form.setUsageYes(true);
		}
		else {
			form.setUsageYes(! form.getUsageYes());
			form.setUsageNo(! form.getUsageNo());
		}
	}

	public void listenUsageNo(ValueChangeEvent event) {
		if (! form.getUsageNo() && ! form.getUsageYes()) {
			form.setUsageYes(false);
			form.setUsageNo(true);
		}
		else if (form.getUsageYes() && form.getUsageNo()) {
			form.setUsageNo(false);
			form.setUsageYes(true);
		}
		else {
			form.setUsageYes(! form.getUsageYes());
			form.setUsageNo(! form.getUsageNo());
		}
		form.setUnlimited(false);
		form.setUnlimitedExceptOutdoor(false);
		form.setElectricMedia(false);
		form.setBroll(false);
		form.setDirectMail(false);
		form.setCollateral(false);
		form.setIsm(false);
		form.setPr(false);
		form.setPrint(false);
		form.setCircular(false);
		form.setIndustrial(false);
		form.setMediaOther(false);
		form.setMediaOtherText("");
		form.setTerm(false);
		form.setTermMonth(null);
		form.setTermDate(null);
		form.setTermYear(null);
		form.setUnlimitedTime(false);
		form.setTermOther(false);
		form.setNorthAmerica(false);
		form.setEuropeanUnion(false);
		form.setSouthAmerica(false);
		form.setAsia(false);
		form.setWorldWide(false);
		form.setWorldWideElectronicMedia(false);
		form.setTerritoryOther(false);
		form.setTerritoryOtherText("");
		form.setOptionalReuseYes(false);
		form.setOptionalReuseNo(false);
		form.setReuseMedia("");
		form.setReuseTerm("");
		form.setReuseTerritory("");
		form.setReuseFee("");
	}

	public void listenOptionalReuseYes(ValueChangeEvent event) {
		form.setOptionalReuseYes(true);
		form.setOptionalReuseNo(false);
	}

	public void listenOptionalReuseNo(ValueChangeEvent event) {
		form.setOptionalReuseYes(false);
		form.setOptionalReuseNo(true);
		form.setReuseMedia("");
		form.setReuseTerm("");
		form.setReuseTerritory("");
		form.setReuseFee("");
	}

	public void listenCompensationYes(ValueChangeEvent event) {
		if (form.getCompensationNo() == form.getCompensationYes()) {
			form.setCompensationNo(false);
			form.setCompensationYes(true);
		}
		else {
			form.setCompensationYes(! form.getCompensationYes());
			form.setCompensationNo(! form.getCompensationNo());
		}
	}

	public void listenCompensationNo(ValueChangeEvent event) {
		if (! form.getCompensationNo() && ! form.getCompensationYes()) {
			form.setCompensationYes(false);
			form.setCompensationNo(true);
		}
		else if (form.getCompensationYes() && form.getCompensationNo()) {
			form.setCompensationNo(false);
			form.setCompensationYes(true);
		}
		else {
			form.setCompensationYes(! form.getCompensationYes());
			form.setCompensationNo(! form.getCompensationNo());
		}

		form.setRateForService(new BigDecimal("0.0"));
		form.setPerDay(false);
		form.setPerHour(false);
		form.setServiceHours(new BigDecimal("0.0"));
		form.setRightTerm(false);
		form.setNotrightTerm(false);
		form.setUsage(new BigDecimal("0.0"));
		form.setAddlYes(false);
		form.setAddlNo(false);
		form.setAddlOverTime(false);
		form.setAddlPerHour(new BigDecimal("0.0"));
		form.setShootDay(false);
		form.setPrepDay(false);
		form.setShootPerDay(new BigDecimal("0.0"));
		form.setShootPerHours(null);
		form.setPrepPerDay(new BigDecimal("0.0"));
		form.setPrepPerHours(null);
		form.setWeatherDay(false);
		form.setWeatherPerDay(new BigDecimal("0.0"));
		form.setWeatherHours(null);
		form.setIntimatesDay(false);
		form.setIntimatesPerDay(new BigDecimal("0.0"));
		form.setIntimatesPerHours(null);
		form.setTravelDayRate(false);
		form.setTravelDayPerDay(new BigDecimal("0.0"));
		form.setTravelDayPerHours(null);
	}

	public void listenAddlYes(ValueChangeEvent event) {
		form.setAddlYes(true);
		form.setAddlNo(false);
	}

	public void listenAddlNo(ValueChangeEvent event) {
		form.setAddlYes(false);
		form.setAddlNo(true);
		form.setAddlOverTime(false);
		form.setAddlPerHour(new BigDecimal("0.0"));
		form.setShootDay(false);
		form.setShootPerDay(new BigDecimal("0.0"));
		form.setShootPerHours(null);
		form.setWeatherDay(false);
		form.setWeatherPerDay(new BigDecimal("0.0"));
		form.setWeatherHours(null);
		form.setIntimatesDay(false);
		form.setIntimatesPerDay(new BigDecimal("0.0"));
		form.setIntimatesPerHours(null);
	}

	public void listenRightTerm(ValueChangeEvent event) {
		form.setNotrightTerm(false);
		form.setRightTerm(true);
	}

	public void listenNotRightTerm(ValueChangeEvent event) {
		form.setNotrightTerm(true);
		form.setRightTerm(false);
	}

	public void listenModelAgent(ValueChangeEvent event) {
		form.setModel(false);
		form.setGuardian(false);
		form.setModelAgency(true);
	}

	public void listenModel(ValueChangeEvent event) {
		form.setModel(true);
		form.setGuardian(false);
		form.setModelAgency(false);
	}

	public void listenGuardian(ValueChangeEvent event) {
		form.setModel(false);
		form.setGuardian(true);
		form.setModelAgency(false);
	}

	public void listenMediaOther(ValueChangeEvent event) {
		form.setMediaOtherText("");
	}

	public void listenTerritoryOther(ValueChangeEvent event) {
		form.setTerritoryOtherText("");
	}

	public void listenPerHour(ValueChangeEvent event) {
		form.setPerHour(true);
		form.setPerDay(false);
		form.setServiceHours(new BigDecimal("0.0"));
	}

	public void listenPerDay(ValueChangeEvent event) {
		boolean perDay = (boolean)event.getNewValue();
		if (perDay) {
			String jobState = form.getJobState();
			form.setPerHour(! perDay);
			form.setPerDay(perDay);
			if (! StringUtils.isEmpty(jobState) && jobState.equalsIgnoreCase(JOB_STATE)) {
				form.setServiceHours(Constants.DECIMAL_EIGHT);
			}
			else {
				form.setServiceHours(new BigDecimal("0.0"));
			}
		}
		else {
			form.setPerHour(perDay);
			form.setPerDay(perDay);
			form.setServiceHours(new BigDecimal("0.0"));
		}
	}


	// LS-3125 Add listener REIMBURSEMENTS type

	public void listenTaxable(ValueChangeEvent event) {
		form.setTaxableTotal(null);
	}

	public void listenNonTaxable(ValueChangeEvent event) {
		form.setNonTaxableTotal(null);
	}

    //LS-3126 Add listener for rate amount
	public void listenRateAmount(ValueChangeEvent event) {
		form.setPerdiemDays(null);
		form.setLodging(false);
		form.setMealsIncidentals(false);
	}

	//LS-3520 warning popup

	@Override
	public String confirmOk(int action) {
		String res = null;
		switch (action) {
			case ACT_WARNING_POPUP:
				res = actionCreateFormOk();
				break;
		}

		return res;
	}

	@Override
	public String confirmCancel(int action) {
		return null;
	}


	private String actionCreateFormOk() {
		String unknown="Unknown";

		if(form.getReuseMedia().isEmpty()) {
		form.setReuseMedia(unknown);
		}
		if(form.getReuseTerm().isEmpty()) {
		form.setReuseTerm(unknown);
		}
		if(form.getReuseTerritory().isEmpty()) {
		form.setReuseTerritory(unknown);
		}
		if(form.getReuseFee().isEmpty()) {
		form.setReuseFee(unknown);
		}
		FormService.getInstance().update(form, PayrollFormType.MODEL_RELEASE.getApiUpdateUrl(), FormModelRelease.class);
		return null;
	}

	/**
	 * Method used to check whether the employer signature fields are empty or not in the
	 * form on action send. This will issue error messages for any required
	 * fields.
	 *
	 * @return False if there is no  employer signature.
	 * @return True if there is employer signature.
	 */
	@Override
	public boolean checkSendValid() {
		boolean isSendValid = true;
		if(contactDoc.getEmployerSignature() == null) {
			isSendValid = false;
		}

		return isSendValid;
	}

	/*
	 * True if the listener set Use only checkbox checked
	 * changes on LS-4813 and LS-4814
	 */
	public void listenModelUseOnlyChange(ValueChangeEvent event) {
		boolean newValue = (boolean)event.getNewValue();
		form.setCompensationYes(true);
		form.setCompensationNo(false);
		if (newValue) {
			form.setUsageNo(true);
			form.setUsageYes(false);

		}
		else {
			form.setUsageYes(true);
			form.setUsageNo(false);
			form.setRateForService(null);
			form.setPerHour(false);
			form.setPerDay(false);
			form.setServiceHours(null);
			form.setRightTerm(false);
			form.setNotrightTerm(false);
			form.setAddlYes(false);
			form.setAddlNo(false);
			form.setAddlOverTime(false);
			form.setAddlPerHour(null);
			form.setShootDay(false);
			form.setShootPerDay(null);
			form.setShootPerHours(null);
			form.setPrepDay(false);
			form.setPrepPerDay(null);
			form.setPrepPerHours(null);
			form.setWeatherPerDay(null);
			form.setWeatherDay(false);
			form.setWeatherHours(null);
			form.setIntimatesDay(false);
			form.setIntimatesPerDay(null);
			form.setIntimatesPerHours(null);
			form.setTravelDayRate(false);
			form.setTravelDayPerDay(null);
			form.setTravelDayPerHours(null);

		}
	}

	/** Listener invoked by the value change event in the state drop down
	 * It internally invokes a rest API call to lookup API for fetching
	 * cities based on selected state.
	 * It also sets the rendering flag based on the API output.
	 * @param event
	 */
	public void listenStateChange(ValueChangeEvent event) {
		try {
			String state = event.getNewValue().toString();
			getCityByState(state);
			// clearing the city fields on state change also hiding the other city input box
			form.setJobCity(null);
			form.setOtherCityName(null);
			setShowOtherCities(false);
			boolean perDay = form.getPerDay();
			if (perDay && ! StringUtils.isEmpty(state) && state.equals(JOB_STATE)) {
				form.setServiceHours(Constants.DECIMAL_EIGHT);
			}
			else {
				form.setServiceHours(null);
			}
		} catch (JSONException e) {
			EventUtils.logError(e);
		}
	}


	private void getCityByState(String state) throws JSONException {
		JSONArray cityByState = LocationUtils.getCityByState(state);
		setDisplayCities(false);
		if (null != cityByState) {
			List<SelectItem> citiesList = LocationUtils.getCityListBystate(cityByState);
			citiesList.add(new SelectItem(OTHER_CITY_FIELD));
			if (citiesList != null) {
				setCities(citiesList);
				setDisplayCities(true);
				return;
			}
		}
	}
	
	/** Listener invoked by the value change event in the city drop down
	 * User select other option then other cities input box is visible
	 * @param event
	 */
	public void listenCityChange(ValueChangeEvent event) {
		String city = event.getNewValue().toString();
		if (city != null && city.equals(OTHER_CITY_FIELD)) {
			setShowOtherCities(true);
		} 
		else {
			setShowOtherCities(false);
		}
	}

	public List<SelectItem> getAgencyList() {
		if (agencyList == null) {
			agencyList = createAgencyList();
		}
		return agencyList;
	}

	@Override
	public Production getProduction() {
		if (production == null) {
			production = SessionUtils.getCurrentOrViewedProduction();
		}
		return production;
	}

	/**
	 * Create model agency list based on user entered text
	 *
	 * @return
	 */
	private List<SelectItem> createAgencyList() {
		agencyList = new ArrayList<>();
		if (AGENCY_LOOKUP_PATH == null) {
			AGENCY_LOOKUP_PATH = ApplicationUtils
					.getInitParameterString(Constants.INIT_PARAM_API_SERVER_DOMAIN) + "lookup/";
		}
		String apiUrl = AGENCY_LOOKUP_PATH + AGENCY_LOOKUP_PARAM_PATTERN;
		try {
			JSONArray jsonArray = ApiUtils.callRestApiWithJSONArray(apiUrl);
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject agencyObject = jsonArray.getJSONObject(i);
				if (agencyObject != null) {
					String agencyDesc = (String)agencyObject.get("description");
					if (agencyDesc != null)
						agencyList.add(new SelectItem(agencyDesc));
				}
			}
			if (agencyList.isEmpty()) {
				agencyList.add(new SelectItem("  ", ""));
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return agencyList;
	}
	
	/** Listener for prep day selection under compensation section.
	 * Clears out values when it is unchecked. LS-4759
	 * @param event
	 */
	public void listenPrepDay(ValueChangeEvent event) {
		boolean newValue = (boolean)event.getNewValue();
		if (! newValue) {
			form.setPrepPerDay(null);
			form.setPrepPerHours(null);
		}
	}
	
	/** Listener for Travel day rate selection under compensation section.
	 * Clears out values when it is unchecked. LS-4759
	 * @param event
	 */
	public void listenTravelDayRate(ValueChangeEvent event) {
		boolean newValue = (boolean)event.getNewValue();
		if (! newValue) {
			form.setTravelDayPerDay(null);
			form.setTravelDayPerHours(null);
		}
	}

}
