package com.lightspeedeps.web.onboard.form;

import java.io.Serializable;
import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.service.StartFormService;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.web.onboard.ContactFormBean;
import com.lightspeedeps.web.onboard.DocumentEventsBean;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.report.ReportBean;
import com.lightspeedeps.web.util.ApplicationScopeBean;
import com.lightspeedeps.web.validator.EmailValidator;

/**
 * Backing bean for the I-9 form on the Payroll / Start Forms page.
 * @see com.lightspeedeps.model.FormI9
 * @see StandardFormBean
 * @see com.lightspeedeps.web.onboard.ContactFormBean
 */
@ManagedBean
@ViewScoped
public class FormI9Bean extends StandardFormBean<FormI9> implements Serializable {

	/**
	 */
	private static final long serialVersionUID = 1924948024103208012L;

	private static final Log log = LogFactory.getLog(FormI9Bean.class);

	/** Component ID (from xhtml) of the A1 Document Title field. */
	private final static String A1_DOC_TITLE = "A1_DOC_TITLE";
	/** Component ID (from xhtml) of the A1 Document Title field. */
	private final static String A2_DOC_TITLE = "A2_DOC_TITLE";
	/** Component ID (from xhtml) of the A1 Document Title field. */
	private final static String A3_DOC_TITLE = "A3_DOC_TITLE";
	/** Component ID (from xhtml) of the B Document Title field. */
	private final static String B_DOC_TITLE = "B_DOC_TITLE";
	/** Component ID (from xhtml) of the C Document Title field. */
	private final static String C_DOC_TITLE = "C_DOC_TITLE";
	/** Component ID (from xhtml) of the Citizenship status radio buttons. */
	private final static String CITIZENSHIP_STATUS = "CITIZENSHIP_STATUS";

	/** The I9 document drop-down lists; loaded from
	 * the SelectionItem table. */
	private List<SelectItem> i9DocListA = null;

	/** The I9 document drop-down lists; loaded from
	 * the SelectionItem table. */
	private List<SelectItem> i9DocListB = null;

	/** The I9 document drop-down lists; loaded from
	 * the SelectionItem table. */
	private List<SelectItem> i9DocListC = null;

	/** The I9 document drop-down list for section 3 loaded from the SelectionItem table. */
	private List<SelectItem> i9DocListSec3 = null;

	/** True iff Preparer edit mode */
	private boolean prepEdit;

	/** The currently logged-in User. */
	private final User currentUser = SessionUtils.getCurrentUser();

	private final DocumentEventsBean documentEventsBean = DocumentEventsBean.getInstance();

	/** Used to hold the help text for different UI Components. */
	private String helpText;

	/** Identifies the row of help to be opened up on screen. */
	private String helpRow;

	/** Text to clear most fields, either null or N/A */
	String clearText1;

	/** Text to clear some drop-downs, particularly document List titles,
	 * either null or "NA" (used as drop-down value, to be enum-commpatible). */
	String clearText2;

	/** state values for form i9 state drop down list */
	private static List<SelectItem> stateCodeDL;

	Date today;

	public FormI9Bean() {
		super("FormI9Bean.");
	}

	public static FormI9Bean getInstance() {
		return (FormI9Bean) ServiceFinder.findBean("formI9Bean");
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#getFormById(Integer)
	 */
	@Override
	public FormI9 getFormById(Integer id) {
		return FormI9DAO.getInstance().findById(id);
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#refreshForm()
	 */
	@Override
	public void refreshForm() {
		if (form != null) {
			form = FormI9DAO.getInstance().refresh(form);
		}
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#getBlankForm()
	 */
	@Override
	public FormI9 getBlankForm() {
		FormI9 f = new FormI9();
		f.setDefaults();
		f.setAddress(new Address());
		f.setPreparerAddress(new Address());
		f.setEmpAddress(new Address());
		return f;
	}

	@Override
	public String actionEdit() {
		try {
			if (form.getAddress() == null) {
				form.setAddress(new Address());
			}
			if (form.getPreparerAddress() == null) {
				form.setPreparerAddress(new Address());
			}
			if (form.getEmpAddress() == null) {
				form.setEmpAddress(new Address());
			}
			if (form.getSec2CitizenshipStatus() == null) {
				form.setSec2CitizenshipStatus(form.getCitizenshipStatus());
			}
			calculateEditFlags(false);
			setPrepEdit(getEmpEdit());
			storeEventForDocuments(TimedEventType.EDIT);
			return super.actionEdit();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	@Override
	public String actionSave() {
		try {
			// Save Information from Section 1 into Section 2 fields.
			if (contactDoc.getRelatedFormId() != null) {
				if (getEmpEdit() && form.getLastName() != null) {
					form.setSec2LastName(form.getLastName());
				}
				if (getEmpEdit() && form.getFirstName() != null) {
					form.setSec2FirstName(form.getFirstName());
				}
				if (getEmpEdit() && form.getMiddleName() != null) {
					form.setSec2MiddleInitial(form.getMiddleName());
				}
				if (getEmpEdit() && form.getCitizenshipStatus() != null) {
					form.setSec2CitizenshipStatus(form.getCitizenshipStatus());
				}
				form = FormI9DAO.getInstance().merge(form);
			}
			else {
				form.setUserAccount(currentUser.getAccountNumber());
				form.setContact(getvContact());
				//TODO UNTESTED Fixes for issue #659.
				if(form.getContact() == null){
					form.setContact(SessionUtils.getCurrentContact());
				}
				form.setVersion(getI9Version());
				FormI9DAO.getInstance().save(form);
			}
			storeEventForDocuments(TimedEventType.SAVE);
			documentEventsBean.setDocumentEventList(null);
			return super.actionSave();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
			return Constants.ERROR_RETURN;
		}
	}

	@Override
	public String actionCancel() {
		try {
			if (ContactFormBean.getInstance().getEditMode()) {
				// store an event for cancel only when the Form I9's cancel button is clicked - exiting Edit mode
				log.debug("cancel event saved");
				storeEventForDocuments(TimedEventType.CANCEL);
				documentEventsBean.setDocumentEventList(null);
			}
			setUpForm();
			return super.actionCancel();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** Method used to fetch the saved data for the selected form and to set that data in the Form instance. */
	private void setUpForm() {
		Integer relatedFormId = contactDoc.getRelatedFormId();
		log.debug("");
		if (relatedFormId != null) {
			FormI9 formI9 = FormI9DAO.getInstance().findById(relatedFormId);
			setForm(formI9);
			if (formI9 != null) {
				// force Address instances to load to avoid LIE's when switching mini-tabs
				formI9.getAddress().getAddrLine1();
				formI9.getPreparerAddress().getAddrLine1();
				formI9.getEmpAddress().getAddrLine1();
			}
		}
		else {
			setForm(getBlankForm());
		}
		if (getForm().getAddress() == null) {
			getForm().setAddress(new Address());
		}
		if (getForm().getPreparerAddress() == null) {
			getForm().setPreparerAddress(new Address());
		}
		if (getForm().getEmpAddress() == null) {
			getForm().setEmpAddress(new Address());
		}
	}

	/**
	 * Update user's "My Account" citizenship information from the I9.
	 */
	@Override
	public void submitted() {
		if (FF4JUtils.useFeature(FeatureFlagType.TTCO_ADDR_UNIF_USER_PROFILE) && getIsTeamPayroll()) {
			User user = contactDoc.getContact().getUser();
			refreshForm();
			// LS-3524; propagate DOB, Citizenship status, phone number, and country of origin to User (My Account)
			user.setAlienAuthCountryCode(form.getForeignCountryOfIssuance());
			user.setCitizenStatus(form.getCitizenshipStatus().getKey());
			user.setBirthdate(form.getDateOfBirth());
			if (! Form.isEmptyNA(form.getTelephoneNumber())) { // phone might be "N/A"
				switch(user.getPrimaryPhoneIndex()) {
					case 0:
						user.setBusinessPhone(form.getTelephoneNumber());
						break;
					case 1:
						user.setCellPhone(form.getTelephoneNumber());
						break;
					case 2:
						user.setHomePhone(form.getTelephoneNumber());
						break;
				}
			}
			getUserDAO().attachDirty(user);
		}
	}

	/**
	 * @see com.lightspeedeps.web.onboard.StandardFormBean#rowClicked()
	 */
	@Override
	public void rowClicked(ContactDocument contactDocument) {
		SessionUtils.put(Constants.ATTR_ONBOARDING_SELECTED_FORM_I9_ID, contactDocument.getId());
		log.debug("contact doc id of form_i9 in session" + contactDocument.getId());
		setContactDoc(contactDocument);
		//initScrollPos(7130);
		setUpForm();
		if (contactDocument != null) {
			storeEventForDocuments(TimedEventType.VIEW);
			documentEventsBean.setDocumentEventList(null);
			contactDocument.getContactDocEvents().size(); // avoid LIE on mini-tab switch
		}
	}

	/** Listener for the fields of Form I9, used to save the change event for each field of I9.
	 * @param event
	 */
	public void listenValueChange(ValueChangeEvent event) {
		log.debug("");
		if (! event.getPhaseId().equals(PhaseId.INVOKE_APPLICATION)) {
			// Need to schedule event for later - after "setXxxx()" are called from framework.
			// Simpler to do it once here then for every field on the I9
			event.setPhaseId(PhaseId.INVOKE_APPLICATION);
			event.queue();
			return;
		}
		try {
			if (event.getComponent() != null && form != null) {
				String id = event.getComponent().getId();
				if(id != null) {
					if (id.endsWith("_17")) {
						id = id.substring(0, id.indexOf("_17"));
					}
					else if (id.endsWith("_20")) {
						id = id.substring(0, id.indexOf("_20"));
					}

					if (form.getVersion().equals(FormI9.I9_VERSION_2013)) {
						clearText1 = null;
						clearText2 = null;
					}
					else {
						clearText1 = Form.NOT_APPLICABLE;
						clearText2 = "NA";
					}
					if (event.getOldValue() == null) {
						if (event.getNewValue() != null) {
							if (! event.getNewValue().toString().trim().isEmpty()) {
								// do entry true
								log.debug(" entry for : " + id);
								saveChangeEvent(event, id);
							}
						}
					}
					else {
						if(event.getOldValue().toString().isEmpty()) {
							if((event.getNewValue() != null) && (! event.getOldValue().equals(event.getNewValue()))) {
								// do entry
								log.debug(" entry for : " + id);
								saveChangeEvent(event, id);
							}
						}
						else {
							if ((event.getNewValue() == null) || (! event.getOldValue().equals(event.getNewValue()))) {
								// do entry
								log.debug("entry for : " + id);
								saveChangeEvent(event, id);
							}
						}
					}
					if (event.getNewValue() != null && (id.contains(CITIZENSHIP_STATUS))) {
						refreshRadioButtons(event);
						CitizenType type = form.getCitizenshipStatus();
						log.debug("");
						if (type != null) {
							// LS-3514 Clear alien and perm resident fields when changing citizen types.
							log.debug("");
							clearPermanentResidentFields(clearText1);
							clearAlienFields(clearText1);
							form.setSec2CitizenshipStatus(form.getCitizenshipStatus());
						}
					}
					if (event.getNewValue() != null && id.equals("USER_FIRST_NAME")) {
						form.setSec2FirstName(form.getFirstName());
					}
					if (event.getNewValue() != null && id.equals("USER_LAST_NAME")) {
						form.setSec2LastName(form.getLastName());
					}
					if (event.getNewValue() != null && id.equals("USER_MIDDLE_NAME")) {
						form.setSec2MiddleInitial(form.getMiddleName());
					}
					//Clear other fields if ALIEN_REG_NUMBER2 is selected.
					if (event.getNewValue() != null && event.getNewValue().toString().length() > 0) {
						if (id.equals("ALIEN_REG_NUMBER2")) {
							form.setFormI9AdmissionNo(clearText1);
							form.setForeignPassportNo(clearText1);
							form.setForeignCountryOfIssuance(clearText1);
						}
						else if (id.equals("FORM_I94_ADMISSION_NO")) {
							form.setAlienRegNumber2(clearText1);
							form.setAlienUscis2(AlienUscisType.NA);
							form.setForeignPassportNo(clearText1);
							form.setForeignCountryOfIssuance(clearText1);
						}
						else if (id.equals("FOREIGN_PASSPORT_NO")) {
							form.setAlienRegNumber2(clearText1);
							form.setAlienUscis2(AlienUscisType.NA);
							form.setFormI9AdmissionNo(clearText1);
						}
					}

					// LS-3528 If Alien/Uscis dropdown for permanent resident is set to N/A
					// set the alienRegNumber1 to N/A.
					if(id.equals("ALIEN_USCIS1") && form.getAlienUscis1() == AlienUscisType.NA) {
						form.setAlienRegNumber1(clearText1);
					}
					if (event.getNewValue() != null && ! event.getNewValue().toString().equals(Form.NOT_APPLICABLE_ENUM)) {
						if (A1_DOC_TITLE.equals(id)) {
							clearDocBCfields();
						}
						else if (B_DOC_TITLE.equals(id) || C_DOC_TITLE.equals(id)) {
							clearDocAfields();
						}
					}
					else {
						if (A1_DOC_TITLE.equals(id)) {
							clearDocA1fields();
						}
						else if (A2_DOC_TITLE.equals(id)) {
							clearDocA2fields();
						}
						else if (A3_DOC_TITLE.equals(id)) {
							clearDocA3fields();
						}
						else if (B_DOC_TITLE.equals(id)) {
							clearDocBfields();
						}
						else if (C_DOC_TITLE.equals(id)) {
							clearDocCfields();
						}
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();

		}
	}

	/**
	 * Value change listener for help icon's click. The component id of the
	 * clicked item should be in the format "<name>-<n>", where
	 * <name> identifies the entry in our message resource file, which should be
	 * labeled "Help_I9_<name>". The value of <n> is set into the 'helpRow'
	 * field and is used by the jsp on the page to determine which section (row)
	 * of help displays the text.
	 *
	 * @param evt Action event provided by framework.
	 */
	public void listenShowHelp(ActionEvent evt) {
		try {
			if (evt != null) {
				String id = evt.getComponent().getId();
				if (id != null) {
					log.debug("");
					// To make the help icon act as toggle button.
					int j;
					String newHelpText = id;
					if ((j = id.indexOf('-')) > 0) {
						newHelpText = "Help_I9_" + id.substring(0, j);
					}
					if (helpText != null && newHelpText.equals(helpText)) {
						log.debug("help Row = " +  helpRow);
						setHelpText(null);
						setHelpRow(null);
					}
					else {
						log.debug("");
						int i;
						setHelpRow(null);
						if ((i = id.indexOf('-')) > 0) {
							setHelpRow(id.substring(i+1));
							id = id.substring(0, i);
						}
						log.debug("ID = " + id);
						// LS-4152 Strip off version number of form xx...x_versionNumber-rowNumber
						int index = id.lastIndexOf('_');
						if(index > -1) {
							id = id.substring(0, index);
						}
						setHelpText("Help_I9_" + id);
					}

				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Action method used to close the help text area.
	 */
	public String actionCloseHelp() {
		try {
			if (helpText != null) {
				setHelpText(null);
				setHelpRow(null);
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** Value change listener for preparer used /not used checkbox's event
	 * @param event
	 */
	public void listenChangePreparer(ValueChangeEvent event) {
		try {
			log.debug("new val = " + event.getNewValue());
			boolean prepValue = (boolean)event.getNewValue();
			String id = event.getComponent().getId();
			log.debug("ID = " + id);
			if (id.equals("PREPARER_NOT_USED_17") && prepValue) {
				log.debug(" ");
				form.setPreparerUsed(false);
			}
			else if (id.equals("PREPARER_USED_17") && prepValue) {
				log.debug(" ");
				form.setPreparerNotUsed(false);
			}
			listenValueChange(event);
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
		}
	}

	/**
	 * Clear all fields related to all three verification documents in List A.
	 */
	private void clearDocAfields() {
		clearDocA1fields();
		clearDocA2fields();
		clearDocA3fields();
	}

	/**
	 * Clear all fields related to the first verification document in List A.
	 */
	private void clearDocA1fields() {
		form.setA1DocTitle(clearText2);
		form.setA1DocNumber(clearText1);
		form.setA1IssuingAuth(clearText1);
		form.setA1DocExpiration(null);
	}

	/**
	 * Clear all fields related to the second verification document in List A.
	 */
	private void clearDocA2fields() {
		form.setA2DocTitle(clearText2);
		form.setA2DocNumber(clearText1);
		form.setA2IssuingAuth(clearText1);
		form.setA2DocExpiration(null);
	}

	/**
	 * Clear all fields related to the third verification document in List A.
	 */
	private void clearDocA3fields() {
		form.setA3DocTitle(clearText2);
		form.setA3DocNumber(clearText1);
		form.setA3IssuingAuth(clearText1);
		form.setA3DocExpiration(null);
	}

	/**
	 * Clear all fields related to verification documents in List B and List C.
	 */
	private void clearDocBCfields() {
		clearDocBfields();
		clearDocCfields();
	}

	/**
	 * Clear all fields related to verification documents in List B.
	 */
	private void clearDocBfields() {
		form.setBDocTitle(clearText2);
		form.setBDocNumber(clearText1);
		form.setBIssuingAuth(clearText1);
		form.setBDocExpiration(null);
	}

	/**
	 * Clear all fields related to verification documents in List C.
	 */
	private void clearDocCfields() {
		form.setCDocTitle(clearText2);
		form.setCDocNumber(clearText1);
		form.setCIssuingAuth(clearText1);
		form.setCDocExpiration(null);
	}

	/** Method to clear the field for permanent resident citizenship status type.
	 * @param clearText
	 */
	private void clearPermanentResidentFields(String clearText) {
		form.setAlienRegNumber1(clearText);
		form.setAlienUscis1(AlienUscisType.NA);
	}

	/** Method to clear the field for permanent resident citizenship status type.
	 * @param clearText
	 */
	private void clearAlienFields(String clearText) {
		form.setWorkAuthExpirationDate(null);
		form.setAlienWorkExpirationDate(Form.NOT_APPLICABLE);
		form.setAlienRegNumber1(clearText);
		form.setAlienUscis1(AlienUscisType.NA);
		form.setAlienRegNumber2(clearText);
		form.setAlienUscis2(AlienUscisType.NA);
		form.setFormI9AdmissionNo(clearText);
		form.setForeignPassportNo(clearText);
		form.setForeignCountryOfIssuance(clearText);
	}

	private void clearPreparerFields() {
		form.setPreparerFirstName(null);
		form.setPreparerLastName(null);
		if (form.getPreparerAddress() != null) {
			form.getPreparerAddress().clear();
		}
	}

	/** Method used to save the doc change event for each event received by the listener.
	 * Method will be invoked every time when the user changes any number field for any number of times.
	 * @param event
	 */
	private void saveChangeEvent(ValueChangeEvent event, String id) {
		FormFieldType type = null;

		try {
			type = FormFieldType.valueOf(id);
		}
		catch (Exception e) {
			log.error("********** unknown field id=" + event.getComponent().getId());
			EventUtils.logError(e);
		}
		if (type != null) {
			DocChangeEvent docChange = new DocChangeEvent(currentUser, TimedEventType.CHANGE);
			docChange.setContactDocumentId(contactDoc.getId());
			docChange.setFormFieldType(type);
			if (event.getOldValue() != null) {
				docChange.setOldValue(event.getOldValue().toString());
			}
			if (event.getNewValue() != null) {
				docChange.setNewValue(event.getNewValue().toString());
			}
			DocChangeEventDAO.getInstance().save(docChange);
			// Index is to maintain the order of events in the list
			documentEventsBean.getDocumentEventList().add(0, docChange);
		}
	}

	private void storeEventForDocuments(TimedEventType type) {
		DocChangeEvent docChange = new DocChangeEvent(currentUser, type);
		docChange.setContactDocumentId(contactDoc.getId());
		//docChange.setDate(new Date());
		//User user = SessionUtils.getCurrentUser();
		//docChange.setFirstName(currentUser.getFirstName());
		//docChange.setLastName(currentUser.getLastName());
		//docChange.setUserAccount(currentUser.getAccountNumber());
		docChange.setFormFieldType(FormFieldType.N_A);
		//docChange.setType(type);
		DocChangeEventDAO.getInstance().save(docChange);
		documentEventsBean.getDocumentEventList().add(0, docChange);
	}

	private void storeAutoFillEventForDocument(FormFieldType formFieldType, String newValue) {
		if (formFieldType != null) {
			String oldValue = "";
			DocChangeEvent docChange = new DocChangeEvent(currentUser, TimedEventType.CHANGE);
			docChange.setContactDocumentId(contactDoc.getId());
			docChange.setFormFieldType(formFieldType);
			docChange.setContactDocumentId(contactDoc.getId());
			docChange.setOldValue(oldValue);
			if (newValue != null) {
				docChange.setNewValue(newValue);
			}
			DocChangeEventDAO.getInstance().save(docChange);
		}
	}

	/**
	 * No automatic pre-population of I9.
	 */
	@Override
	public void populateForm(boolean prompted) {
		super.populateForm(prompted);
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#compareFormAccountDetails(com.lightspeedeps.model.User)
	 */
	@Override
	public User compareFormAccountDetails(User user) {
		boolean valid = true;
		log.debug("Form id: " + getForm().getId());

		// LS-3524; only check for & propagate DOB, Citizenship status, phone number, and country of origin to User (My Account)

//		if (! StringUtils.isEmpty(form.getFirstName()) && ! form.getFirstName().equals(user.getFirstName())) {
//			user.setFirstName(form.getFirstName());
//			valid = false;
//		}
//		if ((! Form.isEmptyNA(form.getMiddleName())) && ! form.getMiddleName().equals(user.getMiddleName())) {
//			user.setMiddleName(form.getMiddleName());
//			valid = false;
//		}
//		if (! StringUtils.isEmpty(form.getLastName()) && ! form.getLastName().equals(user.getLastName())) {
//			user.setLastName(form.getLastName());
//			valid = false;
//		}
//		if (! StringUtils.isEmpty(form.getSocialSecurity()) && ! form.getSocialSecurity().equals(user.getSocialSecurity())) {
//			user.setSocialSecurity(form.getSocialSecurity());
//			valid = false;
//		}
		if (user.getHomeAddress() == null) {
			user.setHomeAddress(new Address());
		}
		user.getHomeAddress().getAddrLine1(); // prevent LIE
		// LS-3524 Do not update user home address from the I-9
//		user.setHomeAddress(AddressDAO.getInstance().refresh(user.getHomeAddress()));
//		if (form.getAddress() != null && (! form.getAddress().isEmpty())
//				&& (! form.getAddress().equalsAddress(user.getHomeAddress()))) {
//			user.getHomeAddress().copyFrom(form.getAddress());
//			valid = false;
//		}
		if (form.getDateOfBirth() != null && ! form.getDateOfBirth().equals(user.getBirthdate())) {
			user.setBirthdate(form.getDateOfBirth());
			valid = false;
		}
		if (! Form.isEmptyNA(form.getTelephoneNumber()) && ! form.getTelephoneNumber().equals(user.getPrimaryPhone())) {
			switch(user.getPrimaryPhoneIndex()) {
				case 0:
					user.setBusinessPhone(form.getTelephoneNumber());
					break;
				case 1:
					user.setCellPhone(form.getTelephoneNumber());
					break;
				case 2:
					user.setHomePhone(form.getTelephoneNumber());
					break;
			}
			valid = false;
		}
		if (form.getCitizenshipStatus() != null && ! form.getCitizenshipStatus().getKey().equals(user.getCitizenStatus())) {
			user.setCitizenStatus(form.getCitizenshipStatus().getKey());
			valid = false;
		}
		if (! StringUtils.isEmpty(form.getForeignCountryOfIssuance()) && ! form.getForeignCountryOfIssuance().equals(user.getAlienAuthCountryCode())) {
			user.setAlienAuthCountryCode(form.getForeignCountryOfIssuance());
			valid = false;
		}
		setValidData(valid);
		log.debug("validData = " + valid);
		return super.compareFormAccountDetails(user);
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
		if (! checkSection2Valid()) {
			//MsgUtils.addFacesMessage("ContactFormBean.EmployerSignatureInfo", FacesMessage.SEVERITY_ERROR);
			return false;
		}
		// LS-2101 Popup prompting for uploading supporting I-9 documentation
		if (contactDoc.getAttachments().isEmpty()) {
			if (getProduction().getPayrollPref().getI9Attachment() == ExistencePolicy.REQUIRE) {
				PopupBean.getInstance().show(this, 0, "FormI9Bean.MissingAttachment.Title",
						"FormI9Bean.MissingAttachment.Text", "Confirm.OK", null);
				return false;
			}
		}
		return true;
	}

	/**
	 * Handle the employer signature on the I-9 form.
	 *
	 * @param action The dialog box action currently being processed. This may
	 *            be used to distinguish, for example, between a signature and
	 *            an initial event.
	 *
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#employerSign()
	 */
	@Override
	public void employerSign(int action) {
		contactDoc.setApproverId(null);
		if (contactDoc.getStatus() == ApprovalStatus.SUBMITTED &&
				contactDoc.getEmployerSignature() == null) {
			log.debug("");
			// normal case - first approver signing after submission; mark I9 approved
			ContactDocEventDAO.getInstance().createEvent(contactDoc, TimedEventType.APPROVE);
		}
		else {
			// Must be Section 3 signing - for re-hire; distinguish by different event type
			ContactDocEventDAO.getInstance().createEvent(contactDoc, TimedEventType.SIGN);
		}
		contactDoc.setStatus(ApprovalStatus.APPROVED);
	}

	/**
	 * Method used to check the validity of fields in the form. Note that when
	 * this is called, FormI9.trim() has already been called to trim and clean
	 * our fields.
	 *
	 * @return isValid: true if all validated fields are correct, false
	 *         otherwise. When false is returned, one or more error messages
	 *         have also been issued.
	 */
	@Override
	public boolean checkSaveValid() {
		boolean isValid = true;
		String field = "";
		today = CalendarUtils.todaysDate();

		if (form.getPreparerNotUsed()) {
			clearPreparerFields();
		}
		if (form.getEmptyA1DocTitle()) {
			clearDocA2fields();
			clearDocA3fields();
		}
		else if (form.getEmptyA2DocTitle()) {
			clearDocA3fields();
		}
		if (form.getAddress() != null) {
			isValid = ! (StringUtils.isPoBox(form.getAddress().getAddrLine1()));
			field = "Address";
			if (! isValid) {
				MsgUtils.addFacesMessage("FormI9Bean.ValidationMessage." + field, FacesMessage.SEVERITY_ERROR);
			}
			isValid &= checkAddressValid(form.getAddress(), "I9ZipCode");
		}
		if (form.getDateOfBirth() != null) {
			if (today.before(form.getDateOfBirth())) {
				// birth date may not be in the future
				field = "Birthdate";
				isValid = false;
				MsgUtils.addFacesMessage("FormI9Bean.ValidationMessage." + field, FacesMessage.SEVERITY_ERROR);
			}
			else {
				Calendar cal = CalendarUtils.getInstance(today);
				cal.add(Calendar.YEAR, -120);
				if (form.getDateOfBirth().before(cal.getTime())) {
					// birth date may not be more than 120 years in the past (Fed I9 form rule)
					field = "Birthdate";
					isValid = false;
					MsgUtils.addFacesMessage("FormI9Bean.ValidationMessage." + field, FacesMessage.SEVERITY_ERROR);
				}
			}
		}

		if (! StringUtils.isEmpty(form.getSocialSecurity())) {
			if (StringUtils.cleanTaxId(form.getSocialSecurity()) == null ||
					StringUtils.cleanTaxId(form.getSocialSecurity()).isEmpty()) {
				isValid = issueErrorMessage("", false, ".BadSSN");
			}
			else if (getEmpEdit() && currentUser != null && // Only validate SSN if employee is editing the form
					(! StringUtils.isEmpty(currentUser.getSocialSecurity())) &&
					(! form.getSocialSecurity().equals(currentUser.getSocialSecurity()))) {
				isValid = issueErrorMessage("", false, ".SocialSecurity");
			}
		}

		if (! StringUtils.isEmpty(form.getEmailAddress())) {
			if ((! form.getEmailAddress().equals(Form.NOT_APPLICABLE)) &&
					(! EmailValidator.isValidEmail(form.getEmailAddress()))) {
				isValid = false;
				field = "Email";
				MsgUtils.addFacesMessage("FormI9Bean.ValidationMessage." + field, FacesMessage.SEVERITY_ERROR);
			}
		}
		if ((! StringUtils.isEmpty(form.getMiddleName())) &&
				(! form.getMiddleName().equals(Form.NOT_APPLICABLE)) &&
				form.getMiddleName().length() != 1) {
			if (form.getMiddleName().length() == 2 &&
					form.getMiddleName().endsWith(".")){
				form.setMiddleName(form.getMiddleName().substring(0, 1));
			}
			else {
				isValid = false;
				field = "MiddleInitial";
				MsgUtils.addFacesMessage("FormI9Bean.ValidationMessage." + field, FacesMessage.SEVERITY_ERROR);
			}
		}
		if (form.getCitizenshipStatus().equals(CitizenType.ALIEN) &&
				(! Form.isEmptyNA(form.getFormI9AdmissionNo())) &&
				(! checkValidFormI94Number(form.getFormI9AdmissionNo()))) {
			isValid = false;
			field = "formI94AdmissionNo";
			MsgUtils.addFacesMessage("FormI9Bean.ValidationMessage." + field, FacesMessage.SEVERITY_ERROR);
		}
		isValid &= checkAddressValid(form.getPreparerAddress(), "I9PrepZipCode");
		isValid &= checkAddressValid(form.getEmpAddress(), "I9EmpZipCode");
		if(form.getVersion() != FormI9.I9_VERSION_2020) {
			isValid &= checkDateNotExpired(form.getWorkAuthExpirationDate(), "Work Authorization Expiration Date");
		}
		isValid &= checkDateNotExpired(form.getA1DocExpiration(), "first List A");
		isValid &= checkDateNotExpired(form.getA2DocExpiration(), "second List A");
		isValid &= checkDateNotExpired(form.getA3DocExpiration(), "third List A");
		isValid &= checkDateNotExpired(form.getBDocExpiration(), "List B");
		isValid &= checkDateNotExpired(form.getCDocExpiration(), "List C");
		setSaveValid(isValid);

		return super.checkSaveValid();
	}

	/**
	 * Method used to check the validity of fields in Section 1 of the form I9,
	 * which are the fields that must be valid at the time the employee does a
	 * Submit.
	 *
	 * @return isValid: True if all Section 1 fields are valid; false otherwise.
	 */
	@Override
	public boolean checkSubmitValid() {
		boolean isValid = true;
		if (form.getFirstName() == null || form.getFirstName().isEmpty()) {
			isValid = issueErrorMessage("First Name", false, "");
		}
		if (form.getLastName() == null || form.getLastName().isEmpty()) {
			isValid = issueErrorMessage("Last Name", false, "");
		}
		isValid &= checkAddressValidMsg(form.getAddress());
		if (form.getDateOfBirth() == null) {
			isValid = issueErrorMessage("Date Of Birth", false, "");
		}
		if (! StringUtils.isEmpty(form.getSocialSecurity()) && currentUser != null &&
				(! StringUtils.isEmpty(currentUser.getSocialSecurity())) &&
				(! form.getSocialSecurity().equals(currentUser.getSocialSecurity()))) {
			isValid = issueErrorMessage("", false, ".SocialSecurity");
		}
		if (form.getCitizenshipStatus() == null) {
			isValid = issueErrorMessage("Citizenship Status", false, "");
		}
		else if (form.getCitizenshipStatus() == CitizenType.PERM_RESIDENT) {
			if (form.getAlienRegNumber1() == null || form.getAlienRegNumber1().isEmpty()) {
				isValid = issueErrorMessage("(first) Alien Registration Number", false, "");
			}
			else if (! checkValidAlienRegNumber(form.getAlienRegNumber1())) {
				isValid = issueErrorMessage("(first) Alien Registration Number", false, ".AlienRegNumber");
			}
			if ((form.getAlienUscis1() == null || form.getAlienUscis1() == AlienUscisType.NA)) {
				isValid = issueErrorMessage("Alien/USCIS Number drop-down selection", false, "");
			}
		}
		else if (form.getCitizenshipStatus().equals(CitizenType.ALIEN)) {

			if (form.getVersion() != FormI9.I9_VERSION_2020 && form.getWorkAuthExpirationDate() == null) {
				isValid = issueErrorMessage("Work Authorization Expiration Date", false, "");
			}
			if (form.getVersion() == FormI9.I9_VERSION_2020 && StringUtils.isEmpty(form.getAlienWorkExpirationDate())) {
				isValid = issueErrorMessage("Work Authorization Expiration Date", false, "");
			}
			if (Form.isEmptyNA(form.getAlienRegNumber2()) &&
					Form.isEmptyNA(form.getFormI9AdmissionNo()) &&
					Form.isEmptyNA(form.getForeignPassportNo())) {
				isValid = issueErrorMessage("Alien Registration Number/ Form I-94 Admission Number/ Foreign Passport Number", false, "");
			}
			else if (! Form.isEmptyNA(form.getAlienRegNumber2())) {
				if (form.getAlienUscis2() == null || form.getAlienUscis2() == AlienUscisType.NA) {
					isValid = issueErrorMessage("Alien/USCIS Number drop-down selection", false, ".AlienUscis");
				}
				else if (! checkValidAlienRegNumber(form.getAlienRegNumber2())) {
					isValid = issueErrorMessage("(second) Alien Registration Number", false, ".AlienRegNumber");
				}
			}
			else if ((form.getForeignPassportNo() != null && ! form.getForeignPassportNo().isEmpty()) &&
					(form.getForeignCountryOfIssuance() == null || form.getForeignCountryOfIssuance().isEmpty())) {
				isValid = issueErrorMessage("Foreign Country of Issuance", false, "");
			}
		}

		if (form.getVersion() > 1) {
			if (form.getMiddleName() == null || form.getMiddleName().isEmpty()) {
				isValid = issueErrorMessage("Middle Initial", true, "");
			}
			if (form.getOtherName() == null || form.getOtherName().isEmpty()) {
				isValid = issueErrorMessage("Other last names", true, "");
			}
			if (form.getAptNumber() == null || form.getAptNumber().isEmpty()) {
				isValid = issueErrorMessage("Apartment Number", true, "");
			}
			if (form.getEmailAddress() == null || form.getEmailAddress().isEmpty()) {
				isValid = issueErrorMessage("Email address", true, "");
			}
			if (form.getTelephoneNumber() == null || form.getTelephoneNumber().isEmpty()) {
				isValid = issueErrorMessage("Telephone number", true, "");
			}
			if ((! form.getPreparerNotUsed()) && (! form.getPreparerUsed())) {
				isValid = issueErrorMessage("Preparer Used and Not Used Checkboxes", false, ".PreparerBoxes");
			}
		}
		isValid &= isValidSsn(form.getSocialSecurity());

		log.debug("valid fields : " + isValid);
		setSubmitValid(isValid);
//		if (! isValid) {
//			if (isNAmsg) {
//				MsgUtils.addFacesMessage("Form.ValidationNaMessage", FacesMessage.SEVERITY_ERROR, blankField);
//			}
//			else {
//				MsgUtils.addFacesMessage("Form.ValidationMessage" + msgSuffix, FacesMessage.SEVERITY_ERROR, blankField);
//			}
//		}
		return super.checkSubmitValid();
	}

	/**
	 * Check if a date is the same as later than today's date; field
	 * {@link #today} must be set prior to calling this method! An error message
	 * is issued if the date is expired (earlier than today's date).
	 *
	 * @param date The date to check.
	 * @param field The field name to insert into the error message if the date
	 *            is prior to today's date.
	 * @return True if the date is 'not expired', that is, it is equal to or
	 *         later than {@link #today}; false if it is earlier than today's
	 *         date.
	 */
	private boolean checkDateNotExpired(Date date, String field) {
		if (date == null) {
			return true;
		}
		if (date.before(today)) {
			MsgUtils.addFacesMessage("Form.ValidationMessage.Date", FacesMessage.SEVERITY_ERROR, field);
			return false;
		}
		return true;
	}

	/** Method validates the given Alien Registration Number.
	 * @param regNum Alien Registration Number to validate.
	 * @return true, if the given Alien Registration Number is valid and false if not.
	 */
	private boolean checkValidAlienRegNumber(String regNum) {
		boolean valid = true;
		String alienRegNum = regNum.replaceAll("-", "");
		int i = alienRegNum.length();
		if (i < 7 || i > 9) {
			valid = false;
		}
		else {
			try {
				i = Integer.parseInt(alienRegNum);
			}
			catch (NumberFormatException e) {
				i = -1;
			}
			if (i < 0) {
				valid = false;
			}
		}
		return valid;
	}

	/** Method used to check whether the given Form-I94 Number is of 11 numeric digits or not.
	 * @param FormI94Number Form I-94 number entered by the user.
	 * @return true, if the given Form I-94 Number is valid and false if not.
	 */
	private boolean checkValidFormI94Number(String FormI94Number) {
		boolean valid = true;
		double i = FormI94Number.length();
		if (i < 11 || i > 11 ) {
			valid = false;
			log.debug("");
		}
		else {
				valid = org.apache.commons.lang.StringUtils.isNumeric(FormI94Number);
				log.debug("Is valid" + valid);
		}
		return valid;
	}

	/**
	 * Method used to check the validity of fields in the form when the approver
	 * is signing Section 2.
	 *
	 * @return True if it is OK to sign the document -- all fields required for
	 *         final approval have been verified.
	 */
	public boolean checkSection2Valid() {
		String blankField = null;
		today = CalendarUtils.todaysDate();
		if (form.getEmptyA1DocTitle() &&
				(form.getEmptyBDocTitle() || form.getEmptyCDocTitle())) {
			blankField = "Verification documents";
			issueErrorMessage(blankField, false, "");
		}
		else if (! form.getEmptyA1DocTitle() &&
				(StringUtils.isEmpty(form.getA1DocNumber()) || Form.isEmptyNA(form.getA1IssuingAuth()))) {
			blankField = "A1 document number and issuing authority";
			issueErrorMessage(blankField, false, "");
		}
		else if (! form.getEmptyA1DocTitle() &&
				! checkDateNotExpired(form.getA1DocExpiration(), "first List A")) {
			blankField = "";
		}
		else if (! form.getEmptyA2DocTitle() &&
				(StringUtils.isEmpty(form.getA2DocNumber()) || Form.isEmptyNA(form.getA2IssuingAuth()))) {
			blankField = "A2 document number and issuing authority";
			issueErrorMessage(blankField, false, "");
		}
		else if (! form.getEmptyA2DocTitle() &&
				! checkDateNotExpired(form.getA2DocExpiration(), "second List A")) {
			blankField = "";
		}
		else if (! form.getEmptyA3DocTitle() &&
				(StringUtils.isEmpty(form.getA3DocNumber()) || Form.isEmptyNA(form.getA3IssuingAuth()))) {
			blankField = "A3 document number and issuing authority";
			issueErrorMessage(blankField, false, "");
		}
		else if (! form.getEmptyA3DocTitle() &&
				! checkDateNotExpired(form.getA3DocExpiration(), "third List A")) {
			blankField = "";
		}
		else if (! form.getEmptyBDocTitle() &&
				(StringUtils.isEmpty(form.getBDocNumber()) || Form.isEmptyNA(form.getBIssuingAuth()))) {
			blankField = "B document number and issuing authority";
			issueErrorMessage(blankField, false, "");
		}
		else if (! form.getEmptyBDocTitle() &&
				! checkDateNotExpired(form.getBDocExpiration(), "List B")) {
			blankField = "";
		}
		else if (! form.getEmptyCDocTitle() &&
				(StringUtils.isEmpty(form.getCDocNumber()) || Form.isEmptyNA(form.getCIssuingAuth()))) {
			blankField = "C document number and issuing authority";
			issueErrorMessage(blankField, false, "");
		}
		else if (! form.getEmptyCDocTitle() &&
				! checkDateNotExpired(form.getCDocExpiration(), "List C")) {
			blankField = "";
		}

		if (blankField != null && blankField.length() > 0) {
			MsgUtils.addFacesMessage("Form.ValidationMessage", FacesMessage.SEVERITY_ERROR, blankField);
		}

		if (form.getFirstDayOfEmployment() == null) {
			blankField = "Employee's first day of employment";
			issueErrorMessage(blankField, false, "");
		}
		if (StringUtils.isEmpty(form.getEmpFirstName())) {
			blankField = "Employer First Name";
			issueErrorMessage(blankField, false, "");
		}
		if (StringUtils.isEmpty(form.getEmpLastName())) {
			blankField = "Employer Last Name";
			issueErrorMessage(blankField, false, "");
		}
		if (StringUtils.isEmpty(form.getEmpBusinessName())) {
			blankField = "Employer Business Name";
			issueErrorMessage(blankField, false, "");
		}
		if (StringUtils.isEmpty(form.getEmpTitle())) {
			blankField = "Employer Title";
			issueErrorMessage(blankField, false, "");
		}

		boolean isValid = true;
		if (form.getEmpAddress() == null || form.getEmpAddress().trimIsEmpty()) {
			blankField = "Employer Address fields";
			issueErrorMessage(blankField, false, "");
		}
		else {
			isValid = checkAddressValidMsg(form.getEmpAddress());
		}

		if (blankField != null) {
			isValid = false;
		}

		log.debug("valid fields : " + isValid);
		return isValid;
	}

	/**
	 * Action method for the "Print" button on the Start Forms mini-tab. Called
	 * via ContactFormBean.
	 *
	 * @return null navigation string
	 */
	@Override
	public String actionPrint() {
		try {
			if (contactDoc != null) {
				log.debug("");
				Integer id = contactDoc.getRelatedFormId();
				FormI9 formI9 = null;
				if (id != null) {
					formI9 = FormI9DAO.getInstance().findById(id);
				}
				else {
					formI9 = getBlankForm();
					formI9.setId(0); // this retrieves blank I-9 record in database!
				}
				if (formI9 != null) {
					ReportBean report = ReportBean.getInstance();
					formI9 = FormI9DAO.getInstance().refresh(formI9);
					report.generateFormI9(formI9, null, contactDoc.getId(), contactDoc.getStatus().name());
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
	        MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Method called when user clicks Auto-fill button for Employer fields
	 * of I9. This will populate/fill the Employer fields in the form
	 * with the information of the currently logged in approver.
	 *
	 * @return null navigation String
	 */
	public void actionAutoFillCertification() {
		try {
			log.debug("");
			contactDoc = ContactDocumentDAO.getInstance().refresh(contactDoc);
			Production production = contactDoc.getProduction();
			Contact currcontact = ContactDAO.getInstance().findByUserProduction(getvUser(), production);
			Employment emp = contactDoc.getEmployment();
			StartForm sf = StartFormService.findCurrentStart(emp);

			if (form.getFirstDayOfEmployment() == null) {
				if (sf == null) { // use today's date if no StartForm
					form.setFirstDayOfEmployment(CalendarUtils.todaysDate());
				}
				else {
					form.setFirstDayOfEmployment(sf.getWorkStartDate());
				}
				if (form.getFirstDayOfEmployment() != null) {
					storeAutoFillEventForDocument(FormFieldType.FIRST_DAY_OF_EMPLOYMENT, form.getFirstDayOfEmployment().toString());
				}
			}
			if (StringUtils.isEmpty(form.getEmpTitle())) {
				form.setEmpTitle(currcontact.getRoleName());
				storeAutoFillEventForDocument(FormFieldType.EMP_TITLE, currcontact.getRoleName());
			}
			if (StringUtils.isEmpty(form.getEmpLastName())) {
				form.setEmpLastName(currentUser.getLastName());
				storeAutoFillEventForDocument(FormFieldType.EMP_LAST_NAME, currentUser.getLastName());
			}
			if (StringUtils.isEmpty(form.getEmpFirstName())) {
				form.setEmpFirstName(currentUser.getFirstName());
				storeAutoFillEventForDocument(FormFieldType.EMP_FIRST_NAME, currentUser.getFirstName());
			}
			if (StringUtils.isEmpty(form.getEmpBusinessName())) {
				form.setEmpBusinessName(production.getStudio());
				storeAutoFillEventForDocument(FormFieldType.EMP_BUSINESS_NAME, production.getStudio());
			}

			if (form.getEmpAddress() == null) {
				form.setEmpAddress(new Address());
			}
			if (form.getEmpAddress().isEmpty() && production.getAddress() != null) {
				form.getEmpAddress().copyFrom(production.getAddress());

				if (production.getAddress().getAddrLine1() != null &&
						(! StringUtils.isEmpty(production.getAddress().getAddrLine1()))) {
					storeAutoFillEventForDocument(FormFieldType.EMP_ADDR_STREET, production.getAddress().getAddrLine1());
				}
				if (production.getAddress().getCity() != null &&
						(! StringUtils.isEmpty(production.getAddress().getCity()))) {
					storeAutoFillEventForDocument(FormFieldType.EMP_ADDR_CITY_ONLY, production.getAddress().getCity());
				}
				if (production.getAddress().getState() != null &&
						(! StringUtils.isEmpty(production.getAddress().getState()))) {
					storeAutoFillEventForDocument(FormFieldType.EMP_ADDR_STATE, production.getAddress().getState());
				}
				if (production.getAddress().getZip() != null &&
						(! StringUtils.isEmpty(production.getAddress().getZip()))) {
					storeAutoFillEventForDocument(FormFieldType.EMP_ADDR_ZIP, production.getAddress().getZip());
				}
			}

			ContactFormBean bean = ContactFormBean.getInstance();
			if (! bean.getEditMode()) {
				log.debug("");
				actionSave();
			}
			documentEventsBean.setDocumentEventList(null);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	public String getCitizenshipType() {
		if (form != null) {
			if (form.getCitizenshipStatus() != null) {
				return form.getCitizenshipStatus().getKey();
			}
		}
		return CitizenType.CITIZEN.getKey();
	}
	public void setCitizenshipType(String citizenshipType) {
		if (form != null) {
			form.setCitizenshipStatus(CitizenType.fromKey(citizenshipType));
		}
	}

	/**See {@link #i9DocListA}. */
	public List<SelectItem> getI9DocListA() {
		if (i9DocListA == null) {
			i9DocListA = SelectionItemDAO.getInstance().createDLbyType(SelectionItem.I9_DOC_LIST_A, "None");
		}
		return i9DocListA;
	}

	/**See {@link #i9DocListB}. */
	public List<SelectItem> getI9DocListB() {
		if (i9DocListB == null) {
			i9DocListB = SelectionItemDAO.getInstance().createDLbyType(SelectionItem.I9_DOC_LIST_B, "None");
		}
		return i9DocListB;
	}

	/**See {@link #i9DocListC}. */
	public List<SelectItem> getI9DocListC() {
		if (i9DocListC == null) {
			i9DocListC = SelectionItemDAO.getInstance().createDLbyType(SelectionItem.I9_DOC_LIST_C, "None");
		}
		return i9DocListC;
	}

	/**See {@link #i9DocListSec3}. */
	public List<SelectItem> getI9DocListSec3() {
		if (i9DocListSec3 == null) {
			i9DocListSec3 = new ArrayList<>();
			i9DocListSec3.addAll(i9DocListA);
			for (SelectItem s : i9DocListC) {
				if (s.getValue() != null && ! s.getValue().equals("NA")) {
					i9DocListSec3.add(s);
				}
			}
		}
		return i9DocListSec3;
	}

	/**
	 * Method to find the version of the current formI9.
	 *
	 * @return version of the current form I9. If the current form cannot be
	 *         found, or HAS NOT BEEN PERSISTED, then default version is
	 *         returned.
	 */
	public byte getI9Version() {
		byte v = FormI9.DEFAULT_I9_VERSION;

		// LS-3077 Use the 2020 version
		if(FF4JUtils.useFeature(FeatureFlagType.TTCO_I9_2020)) {
			v = FormI9.I9_VERSION_2020;
		}

		if (form != null && form.getId() != null && form.getVersion() != null) {
			v = form.getVersion();
		}
		else {
			// if form does not exist, or has not been persisted yet,
			// then use default set above.
		}

		return v;
	}

	public boolean getDisableListA() {
		return ((! Form.isEmptyNA(form.getBDocTitle())) || (! Form.isEmptyNA(form.getCDocTitle())));
	}

	/**See {@link #prepEdit}. */
	public boolean getPrepEdit() {
		return prepEdit;
	}
	/**See {@link #prepEdit}. */
	public void setPrepEdit(boolean prepEdit) {
		this.prepEdit = prepEdit;
	}

	/** See {@link #helpText}. */
	public String getHelpText() {
		return helpText;
	}
	/** See {@link #helpText}. */
	public void setHelpText(String helpText) {
		this.helpText = helpText;
	}

	/** See {@link #helpRow}. */
	public String getHelpRow() {
		return helpRow;
	}
	/** See {@link #helpRow}. */
	public void setHelpRow(String helpRow) {
		this.helpRow = helpRow;
	}

	/** List of number of Preparers used. */
	public List<SelectItem> getNumberOfPreparerList() {
		List<SelectItem> numberOfPrepList = new ArrayList<>();
		for (int i = 1; i < 6 ; i++) {
			numberOfPrepList.add(new SelectItem(i));
		}
		return numberOfPrepList;
	}

	/** List of Citizenship status. */
	public List<SelectItem> getCitizenshipTypeList() {
		List<SelectItem> citizenshipTypeList = new ArrayList<>();
		for (CitizenType type : CitizenType.values()) {
			citizenshipTypeList.add(new SelectItem(type, type.name()));
		}
		return citizenshipTypeList;
	}

	/** List of Docuument Numbers. */
	public List<SelectItem> getAlienUscisTypeList() {
		List<SelectItem> alienUscisTypeList = new ArrayList<>();
		//alienUscisTypeList.add(new SelectItem(0, ""));
		for (AlienUscisType type : AlienUscisType.values()) {
			alienUscisTypeList.add(new SelectItem(type, type.getLabel()));
		}
		return alienUscisTypeList;
	}

	/** List of State codes LS-1937 */
	public List<SelectItem> getStateCodeDL() {
		if (stateCodeDL == null) {
			stateCodeDL = new ArrayList<>(ApplicationScopeBean.getInstance().getStateCodeDL("US"));
			stateCodeDL.add(new SelectItem(Constants.CANADA_STATE, Constants.CANADA_STATE));
			stateCodeDL.add(new SelectItem(Constants.MEXICO_STATE, Constants.MEXICO_STATE));
		}
		return stateCodeDL;
	}

}