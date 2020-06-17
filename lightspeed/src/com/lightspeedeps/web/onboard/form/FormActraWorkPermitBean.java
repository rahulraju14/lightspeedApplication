package com.lightspeedeps.web.onboard.form;

import java.io.Serializable;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.type.ApprovalStatus;
import com.lightspeedeps.type.GenderType;
import com.lightspeedeps.type.TimedEventType;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.web.onboard.ContactFormBean;

/**
 * Backing bean for the FormActraPermit form on the Payroll / Start Forms page.
 *
 * @see com.lightspeedeps.model.FormActraWorkPermit
 * @see StandardFormBean
 */
@ManagedBean
@ViewScoped
public class FormActraWorkPermitBean extends StandardFormBean<FormActraWorkPermit> implements Serializable  {

	private static final long serialVersionUID = -8243307250526410569L;

	private static final Log LOG = LogFactory.getLog(FormActraWorkPermitBean.class);

	private transient FormActraWorkPermitDAO formActraWorkPermitDAO;
	/** Select item list of office to send documents to. Current just used for Canada */
	private List<SelectItem> officeListDL;

	public FormActraWorkPermitBean() {
		super("FormActraWorkPermitBean.");
	}

	public static FormActraWorkPermitBean getInstance() {
		return (FormActraWorkPermitBean) ServiceFinder.findBean("formActraWorkPermitBean");
	}

	/** Method used to fetch the saved data for the selected form and to set that data in the Form instance. */
	private void setUpForm() {
		try {
			Integer relatedFormId = contactDoc.getRelatedFormId();
			if(relatedFormId == null) {
				setForm(getBlankForm());
			}
			else {
				setForm(getFormActraWorkPermitDAO().findById(relatedFormId));
			}
			if (form.getTalentAddress() == null) {
				form.setTalentAddress(new Address(true));
			}
			// Fix LIE switching between mini-tabs. r9127
			form.getTalentAddress().getAddrLine1();
			if (relatedFormId == null) {
				LOG.debug("");
				getFormActraWorkPermitDAO().save(form);
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
	public String actionEdit() {
		if (form.getTalentAddress() == null) {
			form.setTalentAddress(new Address(true));
		}
		if (form.getOffice() == null) {
			form.setOffice(new Office());
		}
		forceLazyInit();
		calculateEditFlags(true);
		return super.actionEdit();
	}

	private void forceLazyInit() {
		if (form.getOffice() != null) {
			form.getOffice().getEmailAddress();
		}
	}

	@Override
	public String actionSave() {
		getFormActraWorkPermitDAO().merge(form);
		return super.actionSave();
	}

	private FormActraWorkPermitDAO getFormActraWorkPermitDAO() {
		if(formActraWorkPermitDAO == null) {
			formActraWorkPermitDAO = FormActraWorkPermitDAO.getInstance();
		}

		return formActraWorkPermitDAO;
	}

	@Override
	public void rowClicked(ContactDocument contactDocument) {
		LOG.debug("FormActraWorkPermitBean contactDocument rowClicked");
		rowClicked(contactDocument, ContactFormBean.getInstance());
	}

	/**
	 * Override to allow setting 'show auto fill' on ContactFormBean.
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#rowClicked(com.lightspeedeps.model.ContactDocument, com.lightspeedeps.web.onboard.ContactFormBean)
	 */
	@Override
	public void rowClicked(ContactDocument contactDocument, ContactFormBean contactFormBean) {
		// LS-2318 Show work permit auto-fill button for specific roles
		LOG.debug("FormActraWorkPermitBean contactDocument,contactFormBean rowClicked");
		if (contactDocument != null) {
			Role role = getvContact().getRole();
			if (role != null) {
				Integer roleId = role.getId();
				if (roleId.equals(Constants.ROLE_ID_ACTRA_BUSINESS_AFFAIRS_MANAGER) || roleId.equals(Constants.ROLE_ID_ACTRA_AGENCY_PRODUCER)) {
					contactFormBean.setShowAutoFill(true);
				}
				else {
					contactFormBean.setShowAutoFill(false);
				}
			}
		}
		setContactDoc(contactDocument);
		setUpForm();
	}

	@Override
	public FormActraWorkPermit getFormById(Integer id) {
		return getFormActraWorkPermitDAO().findById(id);
	}

	/**
	 * @return a blank form, pre-populated, but not persisted.
	 */
	@Override
	public FormActraWorkPermit getBlankForm() {
		form = new FormActraWorkPermit();
		LOG.debug("");
//		Date todaysDate = CalendarUtils.todaysDate();
//		Contact ct = contactDoc.getContact();
//		User user = getvUser(); // currently logged-in user
//		Employment emp = contactDoc.getEmployment();
		form.setVersion(FormActraWorkPermit.DEFAULT_ACTRA_PERMIT_VERSION_2018);
		populateForm(false);
		return form;
	}

	@Override
	public String autoFillForm(boolean prompted) {
		try {
			LOG.debug("");
			populateForm(prompted);
			return super.autoFillForm(prompted);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** Method to pre-populate the form on creation. */
	@Override
	public void populateForm(boolean prompted) {
		LOG.debug("");
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

		if (form.getTalentAddress() == null) {
			getForm().setTalentAddress(new Address(true));
		}

		if (getContactDoc() != null) {
			// fields from user
			User cdUser = getContactDoc().getContact().getUser();
			form.setProfessionalName(cdUser.getFirstNameLastName());
			form.setLegalName(cdUser.getFullName());
			form.setHomePhone(cdUser.getHomePhone());
			form.setCellPhone(cdUser.getCellPhone());
			form.setTalentEmailAddress(cdUser.getEmailAddress());
			form.setCitizenship(cdUser.getCitizenStatus());
			if (cdUser.getHomeAddress() != null) {
				form.getTalentAddress().copyFrom(cdUser.getHomeAddress());
				// LS-2462
				if (form.getTalentAddress().getAddrLine1() != null &&
						form.getTalentAddress().getAddrLine2() != null) {
					String address = form.getTalentAddress().getAddrLine1() + " " +
							form.getTalentAddress().getAddrLine2();
					form.getTalentAddress().setAddrLine1(address);
				}
			}
			//LS-2461
			List<Agent> agents = cdUser.getAgentsList();
			if (agents != null && ! agents.isEmpty()) {
					for (Agent agent : agents) {
						if (agent.getSelected()) {
							form.setTalentAgencyName(agent.getAgencyName());
							form.setTalentAgencyPhone(agent.getOfficePhone());
							break;
						}
					}
			}
			form.setTalentDob(cdUser.getBirthdate());
			form.setGuardianName(cdUser.getGuardianName());
			//LS-2502 set gender and other from user model as it is
			if (cdUser.getGender() != null) {
				form.setTalentGender(cdUser.getGender());
			}
			form.setTalentGenderOther(cdUser.getOtherGenderDesc());
			form.setSinNum(cdUser.getSocialSecurity());
		}
		// fetch from ProjectDetails
		form.setAdvertiser(cproj.getAdvertiserName());
		form.setAdheredEngager(cproj.getAgencyName());
		form.setCommercialName(cproj.getCommercialName());
		form.setProductName(cproj.getBrandName());
		form.setProductionHouse(cproj.getProdHouseName());
		if (cproj.getProdHouseAddress() != null) {
			form.setComLocation(cproj.getProdHouseAddress().getCity());
		}
		if (cproj.getOffice() != null) {
			form.setOffice(cproj.getOffice());
			form.getOffice().getOfficeName(); // Fix LIE. LS-2587
		}
	}

	/**
	 * Handle the employer signature on the ACTRA WORK PERMIT form.
	 * @param action The dialog box action currently being processed.
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#employerSign()
	 */
	@Override
	public void employerSign(int action) {
		if (action == ContactFormBean.ACT_EMPLOYER_SIGN) {
			LOG.debug("");
			contactDoc.setApproverId(null);
			contactDoc.setStatus(ApprovalStatus.APPROVED);
			ContactDocEventDAO.getInstance().createEvent(contactDoc, TimedEventType.APPROVE);
		}
	}
	/**
	 * LS-2260
	 * Called to verify that a form is ready to be signed by the employer.
	 * @return True if the signature operation should be allowed to continue (by
	 *         prompting for password & PIN). False if the operation should not
	 *         continue -- in this case an error message should have been
	 *         issued.
	 */
	@Override
	public boolean checkEmployerSignValid() {
		boolean isValid = true;
		//LS-2488
		Role role = getvContact().getRole();
		// This will allow only the BA, Producer, or Performer to sign the document
		if (role.isProductionWide()) {
			isValid = issueErrorMessage("Work Permit Signature", false, ".UnauthorizedSignature");
		}

		if (form.getWorkPermitFee() == null || form.getWorkPermitFee() == 0) {
			if (form.getWorkPermitFee() == null) {
				isValid = issueErrorMessage("Work Permit Fee", false, "");
			}
			else {
				isValid = issueErrorMessage("Work Permit Fee", false, ".FieldIsZero");
			}

		}
		// LS-2488 & LS-4173
		if (form.getOffice() == null || StringUtils.isEmpty(form.getOffice().getBranchCode())) {
			isValid = issueErrorMessage("Branch Code", false, "");
		}
		setEmployerSignValid(isValid);
		return super.checkEmployerSignValid();
	}

	@Override
	public void refreshForm() {
		if (form != null) {
			form = getFormActraWorkPermitDAO().refresh(form);
		}
	}
	/**
	 * Set the select office to set BranchCode.
	 *
	 * @param event
	 */
	public void listenOfficeListChange(ValueChangeEvent event) {
		Office office = (Office)event.getNewValue();
		if (office != null && !office.equals(event.getOldValue())) {
			form.setOffice(office);
		}
	}

	/**
	 * LS-2502 Listen for Gender drop-down change on ACTRA Work Permit
	 * @param event from framework
	 */
	public void listenGenderChange(ValueChangeEvent event) {
		GenderType gender = (GenderType)event.getNewValue();
		if (gender != null) {
			if (! gender.isOther()) {
				form.setTalentGenderOther(null);
			}
			form.setTalentGender(gender);
		}
	}

	/** See {@link #officeListDL}. */
	public List<SelectItem> getOfficeListDL() {
		if (officeListDL == null) {
			FormActraContractBean bean = FormActraContractBean.getInstance();
			officeListDL = bean.getOfficeListDL();
		}
		return officeListDL;
	}

	/** See {@link #officeListDL}. */
	public void setOfficeListDL(List<SelectItem> officeListDL) {
		this.officeListDL = officeListDL;
	}

}
