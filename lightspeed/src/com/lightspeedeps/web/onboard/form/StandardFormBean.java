/**
 * File: StandardFormBean.java
 */
package com.lightspeedeps.web.onboard.form;

import java.io.*;
import java.math.BigDecimal;
import java.text.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;

import javax.faces.application.FacesMessage;
import javax.faces.component.*;
import javax.faces.event.*;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.*;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.AddressInformation;
import com.lightspeedeps.service.DocumentService;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.onboard.ContactFormBean;
import com.lightspeedeps.web.popup.*;
import com.lightspeedeps.web.user.UserPrefBean;
import com.lightspeedeps.web.view.View;

/**
 * This is the superclass for all the beans that manage (display, edit, save)
 * one of our standard forms, such as the I-9 or W-4. It also defines some
 * abstract or empty (skeleton) methods that may be implemented or overridden by
 * its subclasses. These methods are generally used by {@link ContactFormBean}
 * to interact with those beans on the Payroll | Start Forms page.
 */
@SuppressWarnings("rawtypes")
public abstract class StandardFormBean<T extends Form> extends View  {
	private static final Log log = LogFactory.getLog(StandardFormBean.class);

	/** standard serial ID */
	private static final long serialVersionUID = -895853729292041355L;

	//private static String output_path = SessionUtils.getRealReportPath();

	/** The standard form in use by the subclass. */
	protected T form;

	/** The ContactDocument currently being processed by this form bean. */
	protected ContactDocument contactDoc;

	/** The production that the current document is associated with.  May be set
	 * by ContactFormBean, or due to call to {@link #getProduction()} */
	private Production production;

	/** Controls the style of the "History" display - true if all information
	 * should be displayed, false if only "signature" information should be
	 * displayed. */
	private boolean showAllHistory;

	/** True iff employee edit mode */
	private boolean empEdit;

	/** True iff company/ Employer edit mode */
	private boolean appEdit;

	/** True iff, the fields those are similar for form and MyAccount have the same values on both the places,
	 *  false if any of the field is different. */
	private boolean validData = true;

	/** True iff, all the required fields to submit the form are filled by the user, False if any of them is empty. */
	private boolean isSubmitValid = true;

	/** True iff, all the fields filled by the user are valid to save the form, False if any of them is not valid. */
	private boolean isSaveValid = true;

	/** True iff the current user is Pseudo Approver. */
	private boolean pseudoApprover = AuthorizationBean.getInstance().getPseudoApprover();

	/** True iff, all the required fields to sign the form are filled by the employer, False if any of them is empty. */
	private boolean isEmployerSignValid = true;

	/** True iff the current production uses the Team payroll service. */
	private Boolean isTeamPayroll;

	// DAO classes.
	private transient FormI9DAO formI9DAO = null;
	private transient FormW4DAO formW4DAO = null;
	private transient FormDepositDAO formDepositDAO = null;
	private transient StartFormDAO startFormDAO = null;
	private transient ContactDocumentDAO contactDocumentDAO = null;
	private transient UserDAO userDAO = null;

	/**
	 * @param prefix
	 */
	public StandardFormBean(String prefix) {
		super(prefix);
	}

	/** Method called for Cancel button. */
	@Override
	public String actionCancel() {
		exitEdit();
		return super.actionCancel();
	}

	/**
	 * Method called when we are leaving edit mode, or doing something like
	 * switching selected documents, where we need to clear all edit-mode
	 * indications.
	 */
	public void exitEdit() {
		// set flags related to edit-mode to false.
		setEmpEdit(false);
		setAppEdit(false);
		setEditMode(false);
	}

	/** */
	@Override
	public String actionEdit() {
		return super.actionEdit();
	}

	/**
	 * Print this bean's current document.
	 */
	public String actionPrint() {
		try {
			if (contactDoc != null) {
				Integer id = contactDoc.getRelatedFormId();
				if (id != null) {
					form = getFormById(id);
				}
				else {
					form = getBlankForm();
				}
				print(contactDoc.getFormType());
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * LS-3247
	 * Print documents supporting main document.
	 * This is for printing documents like the w-4 worksheet and instructions.
	 * @param docType
	 * @return
	 */
	public String actionPrintSupportingDoc(String docType) {
		try {
			PayrollFormType formType = PayrollFormType.valueOf(docType);

			if(formType != null) {
				print(formType);
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** */
	@Override
	public String actionSave() {
		setEmpEdit(false);
		setAppEdit(false);
		return super.actionSave();
	}

	/**
	 * Action method
	 * @return null navigation String
	 */
	public String actionHistoryShowAll() {
		setShowAllHistory(true);
		View.addButtonClicked();
		return null;
	}

	/**
	 * Action method
	 * @return null navigation String
	 */
	public String actionHistoryShowSignatures() {
		setShowAllHistory(false);
		View.addButtonClicked();
		return null;
	}

	/**
	 * Called when a particular document entry in the left-hand list has been
	 * clicked by the user.
	 *
	 * @param contactDocument The ContactDocument whose entry was clicked.
	 */
	public abstract void rowClicked(ContactDocument contactDocument);

	/**
	 * Called when a particular document entry in the left-hand list has been
	 * clicked by the user. This method allows passing the contactFormBean
	 * instance, so we don't need to do a getInstance(). If we're called from
	 * the ContactFormBean constructor, this method should be used, as doing a
	 * getInstance() could result in an infinite loop of calls, and will
	 * therefore cause a 'cyclic reference to managedBean' error. Forms that do
	 * not require the ContactFormBean reference do not need to override this
	 * method; the default implementation here just calls the simpler method
	 * that takes only the contactDocument parameter.
	 *
	 * @param contactDocument The ContactDocument whose entry was clicked.
	 * @param contactFormBean The ContactFormBean calling this method.
	 */
	public void rowClicked(ContactDocument contactDocument, ContactFormBean contactFormBean) {
		rowClicked(contactDocument); // Pass the call to the 'default' rowClicked method
	}

	/**
	 * Print and display (in a new browser window) the form associated with the
	 * current {@link #contactDoc}
	 *
	 * @param formType The PayrollFormType of the form associated with the
	 *            current {@link #contactDoc}.
	 * @return True if the print was successful. False (failure to print)
	 *         usually indicates a severe, i.e., unexpected, error.
	 */
	private boolean print(PayrollFormType formType) {
		String filePrefix = formType.getReportPrefix();
		User user = contactDoc.getContact().getUser();
		user = UserDAO.getInstance().refresh(user);
		DateFormat df = new SimpleDateFormat("mmssSSS");
		String timestamp = df.format(new Date());

		// Note that the PDF filename pattern used is required by LsFacesServlet for access validation.
		// Looks like this: "some-text_<project-id>_<user-id>_more-text"
		String fileNamePdf = filePrefix + SessionUtils.getCurrentOrViewedProject().getId() + "_" + user.getId() + "_" + timestamp + ".pdf";
		fileNamePdf = DocumentService.printDocument(formType, contactDoc, form, fileNamePdf, true);
		String fileWithAttachmentsIfExist = fileNamePdf;
		if (fileNamePdf != null) {
			fileWithAttachmentsIfExist = DocumentService.mergeAttachments(null, contactDoc, fileNamePdf, user);
			openDocumentInNewTab(fileWithAttachmentsIfExist);
		}
		return (fileWithAttachmentsIfExist != null);
	}

	/**
	 * Called when our form has been Submitted, typically via "Sign & Submit"
	 * button. Most of the processing is handled in ContactFormBean. This method
	 * is called to allow form-specific handling.
	 */
	public void submitted() {
		// Default is no action. Subclasses may override.
	}

	/**
	 * For debugging purposes, this may be used to save the updated XFDF string into a file.
	 * @param replacedXfdf
	 * @param filePrefix
	 * @param timestamp
	 */
	protected static void saveXfdf(String replacedXfdf, String filePrefix, String timestamp) {
		try {
			String fileNameXfdf = filePrefix + SessionUtils.getCurrentProject().getId() + "_" + timestamp + ".xfdf";
			FileOutputStream fop = null;
			String output_path = SessionUtils.getRealReportPath();
			String pathName = output_path + fileNameXfdf;
			File file = new File(pathName);
			fop = new FileOutputStream(file);
			fop.write(replacedXfdf.getBytes());
			fop.flush();
			fop.close();
			log.debug("xfdf saved");
		}
		catch (FileNotFoundException e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();

		}
		catch (IOException e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();

		}
	}

	/**
	 * @param fieldValues
	 */
	 void fillFieldValues(Map<String, String> fieldValues) {
		// Subclasses should override this method if using printWithXfdf()
	}

	/**
	 * @param format
	 * @param date
	 * @return
	 */
	protected static String dateFormat(DateFormat format, Date date) {
		if (date == null) {
			return "";
		}
		return format.format(date);
	}

	protected static String byteFormat(Byte data) {
		if (data == null) {
			return "";
		}
		return data.toString();
	}

	protected static String intFormat(Integer data) {
		if (data == null) {
			return "";
		}
		return data.toString();
	}

	protected static String bigDecimalFormat(BigDecimal data) {
		if (data == null) {
			return "";
		}
		return data.toString();
	}

	/**
	 * Replace the keyword fields in an XFDF string with the text values
	 * supplied in a Map.
	 *
	 * @param xfdfString The XFDF data to be updated.
	 * @param fieldValues A map of "key" strings, such as "USER_LAST_NAME", to
	 *            the replacement value to be put into the XFDF string.
	 * @return The XFDF string, with each "key" string from the Map replaced by
	 *         its associated value.
	 */
	public static String replaceFormFields(String xfdfString, Map<String,String> fieldValues) {

		log.debug(" input xfdf len=" + xfdfString.length());
		for (Entry<String,String> entry : fieldValues.entrySet()) {
			String value = entry.getValue();
			//log.debug("Original value : " + value );
			value = StringEscapeUtils.escapeXml(value);
			log.debug("Replaced value : " + value );
			if (value != null) {
				if (value.indexOf('$') >= 0 || value.indexOf('\\') >= 0) {
					// LS-1282 "$" in project name caused replaceAll to fail
					value = Matcher.quoteReplacement(value);
				}
				xfdfString = xfdfString.replaceAll(entry.getKey(), value);
			}
			else {
				xfdfString = xfdfString.replaceAll(entry.getKey(), "");
			}
		}
		log.debug("output xfdf len=" + xfdfString.length());

		return xfdfString;
	}

	/** Utility method used to open the specified document in the browser window
	 * @param fileName document to be opened
	 */
	public static void openDocumentInNewTab(String fileName) {
		log.debug("fileName "+fileName);
		String javascriptCode = "reportOpen('../../" + Constants.REPORT_FOLDER
				+ "/" + fileName + "','LSreport" + fileName + "');";
		addJavascript(javascriptCode);
	}

	/**
	 * Determine the values of the {@link #appEdit} and {@link #empEdit} fields,
	 * based on the currently logged-in user, their permissions, and whether
	 * they are the "owner" of the current document.
	 * <p>
	 * Note that the {@link #contactDoc} object should have been refreshed
	 * before this method is called.
	 *
	 * @param appEditsSubmitable True if the current form allows an
	 *            approver/employer to edit a document that is in a
	 *            "submittable" status.
	 */
	protected void calculateEditFlags(boolean appEditsSubmittable) {
		Contact currentContact = getvContact();
		boolean submittable = false;

		if (contactDoc != null) {
			//contactDoc = ContactDocumentDAO.getInstance().refresh(contactDoc);
			submittable = contactDoc.getSubmitable();
			if ((submittable && contactDoc.getContact().equals(currentContact))) {
				setEmpEdit(true);
			}
		}
		else if (form instanceof StartForm) {
			// no ContactDoc -- must be Non-onboarding case;
			submittable = true; // so SFs are always "submitable" (open to editing)
			StartForm sf = (StartForm)form;
			if (sf.getContact().equals(currentContact)) {
				setEmpEdit(true);
			}
			AuthorizationBean bean = AuthorizationBean.getInstance();
			if (bean.hasPageField("9.3,edit")) {
				// non-Onboarding, & user has "Edit Crew Start Forms" permission
				setAppEdit(true); // treat as "approver edit"
			}
		}
		if (submittable && (! getEmpEdit()) && pseudoApprover) {
			setEmpEdit(true);
		}
		// The employer (approver) may edit some forms before the employee has signed it(StartForm and WTPA),
		// so allow appEdit for submitable if caller passes true parameter.
		if ((submittable && appEditsSubmittable) ||
				contactDoc.getStatus().isPendingApproval() || contactDoc.getStatus() == ApprovalStatus.PENDING) {
			if (! appEdit) {
				AuthorizationBean bean = AuthorizationBean.getInstance();
				currentContact = ContactDAO.getInstance().refresh(currentContact); // DH 4/21/16
				boolean hasPermission = Permission.APPROVE_START_DOCS.inMask(currentContact.getPermissionMask()) ||
						bean.hasPermission(currentContact, (Permission.APPROVE_START_DOCS)) || pseudoApprover;
				setAppEdit(hasPermission);
			}
			if (contactDoc != null && contactDoc.getFormType().isWtpa() && contactDoc.getEmployerSignature() != null) {
				setAppEdit(false);
			}
		}
	}

	/**
	 * Method called when a form is first created and its fields should be
	 * populated with information from other sources, e.g, the User object.
	 * Should be overridden by subclasses.
	 */
	public void populateForm(boolean prompted) {
		// Save "never show" option in User Preferences...
		if (prompted) {
			boolean show = PopupCheckboxBean.getInstance().getCheck();
			UserPrefBean.getInstance().put(Constants.ATTR_AUTO_FILL_PROMPT, show);
		}
	}

	/**
	 * Given a form's database id, return it.
	 *
	 * @param id The database id of the required Form.
	 * @return The requested Form, or null if not found.
	 */
	public abstract T getFormById(Integer id);

	/**
	 * @return A new instance of the appropriate Form.
	 */
	public abstract T getBlankForm();

	/**
	 * Refresh the instance of the related form (if any) currently held by this
	 * FormBean. This is called by ContactFormBean at the beginning of an edit
	 * cycle, to ensure that the FormBean has the latest version of the data.
	 */
	public abstract void refreshForm();

	/** Method to set the related form id of the contact document. */
//	public abstract void setRelatedFormId(ContactDocument cd);
//	public void setRelatedFormId(ContactDocument cd) {
//		PersistentObject<?> obj = getForm();
//		log.debug("Form id: " + obj.getId());
//		cd.setRelatedFormId(obj.getId());
//		cd = getContactDocumentDAO().merge(cd);
//	}


	/**
	 * Method called when user clicks Auto-fill button for StartForm or W4. This
	 * will populate/fill the form fields with information from other sources,
	 * e.g, the User object. Should be overridden by subclasses.
	 *
	 * @param prompted True if the user was prompted, in which case the
	 *            check-box setting should be saved.
	 * @return null navigation String
	 */
	public String autoFillForm(boolean prompted) {
		// Save "never show" option in User Preferences...
		if (prompted) {
			boolean show = PopupCheckboxBean.getInstance().getCheck();
			UserPrefBean.getInstance().put(Constants.ATTR_AUTO_FILL_PROMPT, show);
		}
		return null;
	}

	/**
	 * Method to compare form fields with the same fields in user.
	 * @param user User of the contact document/form which will be updated.
	 * @return User updated user
	 */
	public User compareFormAccountDetails(User user) {
		return user;
	}

	/**
	 * @param currentAction The action being completed.
	 * @return The {@link com.lightspeedeps.type.TimedEventType TimedEventType}
	 *         to be used to record this 'Initial' action. The default (provided
	 *         here) is {@link com.lightspeedeps.type.TimedEventType#INITIAL
	 *         INITIAL} event, but a subclass may override this method to change
	 *         the event type.
	 */
	public TimedEventType calculateInitialEvent(int currentAction) {
		return TimedEventType.INITIAL;
	}

	/**
	 * Determine if a document is ready to be Approved.
	 *
	 * @return True. May be overridden by a subclass if data validation is
	 *         necessary, such as for the G-4.
	 */
	public boolean checkApproveValid() {
		return true;
	}

	/**
	 * Called to verify that a form is ready to be signed by the employer. This
	 * method should be overridden for forms that support conditional employer
	 * signatures beyond the typical approver signature flow.
	 *
	 * @return True if the signature operation should be allowed to continue (by
	 *         prompting for password & PIN). False if the operation should not
	 *         continue -- in this case an error message should have been
	 *         issued.
	 */
	public boolean checkEmployerSignValid() {
		return isEmployerSignValid;
	}

	/**
	 * Called when an employer is signing the form. This is not the standard
	 * approval path, but is used for certain documents such as WTPA and I-9.
	 * The appropriate bean subclass must override this method to handle the
	 * process.
	 *
	 * @param The dialog box action currently being processed. This may be used
	 *            to distinguish, for example, between a signature and an
	 *            initial event.
	 */
	public void employerSign(int action) {
	}

	/** Method used to check the validity of fields in the form on action submit.
	 * @return isSubmitValid, True if all the fields are valid to submit the form.
	 */
	public boolean checkSubmitValid() {
		return isSubmitValid;
	}

	/**
	 * LS-4240
	 * Method used to check the validity of fields in the form on action submit.
	 * Second submit validation if there are two signatures on the page and only
	 * one is required.
	 * @return isSubmitValid, True if all the fields are valid to submit the form.
	 */
	public boolean checkSecondSubmitValid() {
		return isSubmitValid;
	}

	/** Method used to check the validity of fields in the form on action save.
	 * @return isSaveValid, True if all the fields are valid to save the form.
	 */
	public boolean checkSaveValid() {
		return isSaveValid;
	}

	/**
	 * Method used to check the validity of address' Zip-code field in the form
	 * on action save. An error message will be issued if the Zip code is
	 * invalid.
	 *
	 * @return True if the Zip code is valid.
	 */
	public boolean checkAddressValid(Address address, String msg) {
		if (msg == null) {
			msg = "ZipCode";
		}
		if (address != null && (! address.isZipValidOrEmpty())) {
			log.debug("");
			MsgUtils.addFacesMessage("Form.Address." + msg, FacesMessage.SEVERITY_ERROR);
			return false;
		}
		return true;
	}

	/**
	 * Method used to check whether the address fields are empty or not in the
	 * form on action submit.
	 *
	 * @return The name of the blank field, or null if all the fields are valid
	 *         to submit the form.
	 */
	protected String checkAddressValid(Address address) {
		String blankField = null;
		if (address == null) {
			blankField = "Address Fields";
		}
		else if (StringUtils.isEmpty(address.getAddrLine1())) {
			blankField = "Address";
		}
		else if (StringUtils.isEmpty(address.getCity())) {
			blankField = "City";
		}
		else if (StringUtils.isEmpty(address.getState())) {
			blankField = "State";
		}
		else if (StringUtils.isEmpty(address.getZip())) {
			blankField = "Zip Code";
		}
		return blankField;
	}

	/**
	 * Method used to check whether the address fields are empty or not in the
	 * form on action submit. This will issue error messages for any required
	 * fields that are blank.
	 *
	 * @param address The Address object whose fields are to be validated.
	 *
	 * @return True if all the fields are valid to submit the form.
	 */
	protected boolean checkAddressValidMsg(Address address) {
		boolean valid = true;
		if (address == null) {
			valid = issueErrorMessage("Address Fields", false, "");
		}
		else {
			if (StringUtils.isEmpty(address.getAddrLine1())) {
				valid = issueErrorMessage("Address", false, "");
			}
			if (StringUtils.isEmpty(address.getCity())) {
				valid = issueErrorMessage("City", false, "");
			}
			if (StringUtils.isEmpty(address.getState())) {
				valid = issueErrorMessage("State", false, "");
			}
			if (StringUtils.isEmpty(address.getZip())) {
				valid = issueErrorMessage("Zip Code", false, "");
			}
		}
		return valid;
	}

	/**
	 * Method used to check whether the secondary address fields are empty or not in the
	 * form on action submit. This will issue error messages for any required
	 * fields that are blank.
	 *
	 * @param address The Secondary Address object whose fields are to be validated.
	 *
	 * @return True if all the fields are valid to submit the form.
	 */
	protected boolean checkSecondaryAddressValidMsg(Address secondaryddress) {
		return true;
	}

	/**
	 * LS-3997
	 *
	 * Method that validates a 5-digit zip code.
	 * Should be overridden by the sub-class to do the actual
	 * validation.
	 *
	 * @return
	 */
	protected boolean isZipValid() {
		return true;
	}

	/**
	 * Check to see if this ssm matches other submitted or approved forms
	 * that have a ssn.
	 *
	 * @param ssn
	 * @return
	 */
	protected boolean isValidSsn(String ssn) {
       boolean isValid = true;
        if (ssn != null && ! ssn.isEmpty()) {
            // Validate SSN against other submitted or approved forms that have a SSN.
            isValid = checkNoSsnDiscrepancy(getContactDoc(), ssn, getContactDoc().getFormType());
            if (! isValid) {
                MsgUtils.addFacesMessage("Form.ValidationMessage.SocialSecurityDiscrepancy",
                        FacesMessage.SEVERITY_ERROR);
            }
        }
        return isValid;
	}

	/**
	 * Validate the current form's ssn against other forms that have already been submitted or approved.
	 *
	 * @param currentDocument
	 * @param currentFormSsn
	 * @param formType - Used so we don't retrieve the current document from the database when doing the
	 * 			ssn validation.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private boolean checkNoSsnDiscrepancy(ContactDocument currentDocument, String currentFormSsn, PayrollFormType formType) {
		boolean noDiscrepancy = true;
		String query;
		List<ContactDocument> docs;

		String status = " and (status = 'SUBMITTED' or status = 'SUBMITTED_NO_APPROVERS' or status = 'RESUBMITTED'" +
				" or status = 'APPROVED' or status = 'LOCKED')";
		String payFormType;

		switch(formType) {
			case W4:
				payFormType = " and (formType = 'START' or formType = 'I9' or formType = 'DEPOSIT')";
				break;
			case I9:
				payFormType = " and (formType = 'START' or formType = 'W4' or formType = 'DEPOSIT')";
				break;
			case DEPOSIT:
				payFormType = " and (formType = 'START' or formType = 'W4' or formType = 'I9')";
				break;
			case START:
				payFormType = " and (formType = 'W4' or formType = 'I9' or formType = 'DEPOSIT')";
				break;
			default:
				payFormType = "";

		}

		query = "from ContactDocument where employment = ?" + status +   payFormType;
		docs = getContactDocumentDAO().find(query,  currentDocument.getEmployment());

		for (ContactDocument cd : docs) {
			Integer relatedFormId = cd.getRelatedFormId();
			String ssn = null;

			switch(cd.getFormType()) {
				case START:
					ssn = getStartFormDAO().findSsnById(relatedFormId);
					break;
				case I9:
					ssn = getFormI9DAO().findSsnById(relatedFormId);
					break;
				case W4:
					ssn = getFormW4DAO().findSsnById(relatedFormId);
					break;
				case DEPOSIT:
					ssn = getFormDepositDAO().findSsnById(relatedFormId);
					break;
				default:
			}

			// I9 ssn maybe empty so skip it
			if (ssn == null || ssn.isEmpty()) {
				continue;
			}

			if (! currentFormSsn.equals(ssn)) {
				noDiscrepancy = false;
				break;
			}
		}

		return noDiscrepancy;
	}

	/**
	 * Issue a formatted error message for a required field that is currently
	 * blank, and return false.
	 *
	 * @param blankField The name of the field which is blank.
	 * @param isNAmsg If true, the message text includes the "or enter N/A"
	 *            text.
	 * @param msgSuffix This string is appended to the normal message id string
	 *            to allow for a custom message to be issued.
	 * @return False.
	 */
	protected boolean issueErrorMessage( String blankField, boolean isNAmsg, String msgSuffix) {
		if (isNAmsg) {
			MsgUtils.addFacesMessage("Form.ValidationNaMessage", FacesMessage.SEVERITY_ERROR, blankField);
		}
		else {
			MsgUtils.addFacesMessage("Form.ValidationMessage" + msgSuffix, FacesMessage.SEVERITY_ERROR, blankField);
		}
		return false;
	}

	/**
	 * Determine if a document is ready to be 'sent' (delivered) to the owner
	 * (employee).
	 *
	 * @return True. May be overridden by a subclass if data validation is
	 *         necessary, such as for the WTPA.
	 */
	public boolean checkSendValid() {
		return true;
	}

	/** Method used to clear the selections on radio buttons.
	 * @param event
	 */
	public void refreshRadioButtons(ValueChangeEvent event) {
		if (event != null) {
			if (event.getPhaseId() != PhaseId.INVOKE_APPLICATION) {
				log.debug("");
		        event.setPhaseId(PhaseId.INVOKE_APPLICATION);
		        event.queue();
		        return;
		    }
			UIComponent parent = event.getComponent().getParent();
			log.debug("parent = " + parent.getId());
			if (null != parent) {
			    List<UIComponent> components = parent.getChildren();
			    for (UIComponent component : components) {
			        if (component instanceof UIInput) {
			            UIInput input = (UIInput) component;
			            log.debug("input = " + input.getId());
			            //input.setSubmittedValue(null);
			            //input.setValue(null);
			            input.setLocalValueSet(false);
			            //input.setValid(true);
			            // JSF 1.2+
			            //input.resetValue();
			        }
			    }
			}
		}
	}
	
	/** Listener invoked by the value change event in the Zip code
	 * on basis of Zip code option then other cities and state input box is visible
	 * @param event
	 * @param address address object of the current form to be processed
	 */
	public void listenZipCode(ValueChangeEvent event, Address address) {
		String zipCode = event.getNewValue().toString();
		List<AddressInformation> addressInformation = new ArrayList<>();
		if (StringUtils.isEmpty(zipCode)) {
			address.clearCityStateZip();
		}
		if (zipCode != null && zipCode.length() == 5) {
			addressInformation = LocationUtils.getCityStateByZip(zipCode);
			if(addressInformation == null || addressInformation.isEmpty()) {
				MsgUtils.addFacesMessage("Form.Address.ZipCode", FacesMessage.SEVERITY_ERROR);
			}
			else if (addressInformation != null && addressInformation.size() == 1) {
				addressInformation.get(0).setSelected(true);
				LocationUtils.setCityStateByZipCode(addressInformation, address);
			} 
			else if (addressInformation != null && addressInformation.size() > 1) {
				// Show Pop up for city selection
				ZipCitiesPopupBean bean = ZipCitiesPopupBean.getInstance();
				bean.show(this, 0, "PopupSelectZipcodeBean.ZipcodePopup.Title", "Confirm.Enter", "Confirm.Close");
				bean.setAddress(address);
				bean.setCityStateList(addressInformation);
			}
		}
	}

	/**
	 * @return The 'current' Production. If not on "My Starts", this is the
	 *         normal 'current production', i.e., whatever production the user
	 *         has entered. On the My Starts page, the production is determined
	 *         from the current document, which should match the production
	 *         selected in the Production drop-down.
	 */
	public Production getProduction() {
		if (production == null) {
			production = SessionUtils.getCurrentOrViewedProduction();
		}
		if (production != null && ! editMode) {
			production = ProductionDAO.getInstance().refresh(production);
		}
		return production;
	}
	/** See {@link #production}. */
	public void setProduction(Production production) {
		this.production = production;
	}

	/**
	 * @return the "is a Team payroll service" flag based on the current
	 *         production's Payroll Service.
	 */
	public boolean getIsTeamPayroll() {
		if (isTeamPayroll == null) {
			isTeamPayroll = false;
			if (getProduction() != null) {
				setProduction(ProductionDAO.getInstance().refresh(getProduction()));
				// LS-3809. c:set w/ scope=request got NPE here.
				PayrollService payrollService = getProduction().getPayrollPref().getPayrollService();
				if (payrollService != null) {
					isTeamPayroll = payrollService.getTeamPayroll();
				}
			}
		}
		return isTeamPayroll;
	}

	/** See {@link #form}. */
	public T getForm() {
		return form;
	}
	/** See {@link #form}. */
	@SuppressWarnings("unchecked")
	public void setForm(Object form) {
		this.form = (T)form;
	}

	public ContactDocument getContactDoc() {
		return contactDoc;
	}
	public void setContactDoc(ContactDocument contactDoc) {
		this.contactDoc = contactDoc;
	}

	/**See {@link #showAllHistory}. */
	public boolean getShowAllHistory() {
		return showAllHistory;
	}
	/**See {@link #showAllHistory}. */
	public void setShowAllHistory(boolean showAllHistory) {
		this.showAllHistory = showAllHistory;
	}

	/** See {@link #empEdit} */
	public boolean getEmpEdit() {
		return empEdit;
	}
	/** See {@link #empEdit} */
	public void setEmpEdit(boolean empEdit) {
		this.empEdit = empEdit;
	}

	/** See {@link #appEdit}*/
	public boolean getAppEdit() {
		return appEdit;
	}
	/** See {@link #appEdit}*/
	public void setAppEdit(boolean appEdit) {
		this.appEdit = appEdit;
	}

	/** See {@link #output_path}*/
	/*public static String getOutputPath() {
		return output_path;
	}
	*//** See {@link #output_path}*//*
	public static void setOutputPath(String output_path) {
		StandardFormBean.output_path = output_path;
	}*/

	/** See {@link #validData}*/
	public boolean getValidData() {
		return validData;
	}
	/** See {@link #validData}*/
	public void setValidData(boolean validData) {
		this.validData = validData;
	}

	/** See {@link #isSubmitValid}*/
	public boolean getSubmitValid() {
		return isSubmitValid;
	}
	/** See {@link #isSubmitValid}*/
	public void setSubmitValid(boolean isSubmitValid) {
		this.isSubmitValid = isSubmitValid;
	}

	/** See {@link #isSaveValid}*/
	public boolean getSaveValid() {
		return isSaveValid;
	}
	/** See {@link #isSaveValid}*/
	public void setSaveValid(boolean isSaveValid) {
		this.isSaveValid = isSaveValid;
	}

	/** See {@link #pseudoApprover}*/
	public boolean isPseudoApprover() {
		return pseudoApprover;
	}
	/** See {@link #pseudoApprover}*/
	public void setPseudoApprover(boolean pseudoApprover) {
		this.pseudoApprover = pseudoApprover;
	}

	/** See {@link #isEmployerSignValid}*/
	public boolean isEmployerSignValid() {
		return isEmployerSignValid;
	}
	/** See {@link #isEmployerSignValid}*/
	public void setEmployerSignValid(boolean isEmployerSignValid) {
		this.isEmployerSignValid = isEmployerSignValid;
	}

	// Retrieve DAO instances

	/** See {@link #formI9DAO}. */
	private FormI9DAO getFormI9DAO() {
		if(formI9DAO == null) {
			formI9DAO = FormI9DAO.getInstance();
		}
		return formI9DAO;
	}

	/** See {@link #formW4DAO}. */
	private FormW4DAO getFormW4DAO() {
		if(formW4DAO == null) {
			formW4DAO = FormW4DAO.getInstance();
		}
		return formW4DAO;
	}

	/** See {@link #formW9DAO}. */
//	TODO
//	private FormW9DAO getFormW9DAO() {
//		if(formW9DAO == null) {
//			formW9DAO = FormW9DAO.getInstance();
//		}
//		return formW9DAO;
//	}

	/** See {@link #formDepositDAO}. */
	private FormDepositDAO getFormDepositDAO() {
		if(formDepositDAO == null) {
			formDepositDAO = FormDepositDAO.getInstance();
		}
		return formDepositDAO;
	}

	/** See {@link #startFormDAO}. */
	private StartFormDAO getStartFormDAO() {
		if(startFormDAO == null) {
			startFormDAO = StartFormDAO.getInstance();
		}
		return startFormDAO;
	}

	/** See {@link #contactDocumentDAO}. */
	protected ContactDocumentDAO getContactDocumentDAO() {
		if(contactDocumentDAO == null) {
			contactDocumentDAO = ContactDocumentDAO.getInstance();
		}
		return contactDocumentDAO;
	}

	/** See {@link #userDAO}. */
	protected UserDAO getUserDAO() {
		if(userDAO == null) {
			userDAO = UserDAO.getInstance();
		}
		return userDAO;
	}

}
