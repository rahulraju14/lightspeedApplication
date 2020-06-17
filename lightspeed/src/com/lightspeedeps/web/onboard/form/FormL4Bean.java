package com.lightspeedeps.web.onboard.form;

import java.io.Serializable;

import javax.faces.application.FacesMessage;
import javax.faces.bean.*;

import org.apache.commons.logging.*;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.StringUtils;

/**
 * Backing bean for the L-4 form on the Payroll / Start Forms page.
 *
 * @see com.lightspeedeps.model.FormL4
 * @see StandardFormBean
 */
@ManagedBean
@ViewScoped
public class FormL4Bean extends StandardFormBean<FormL4> implements Serializable {

	private static final long serialVersionUID = -7406869333088919808L;

	private static final Log LOG = LogFactory.getLog(FormL4Bean.class);

	public FormL4Bean() {
		super("FormL4Bean");
	}

	public static FormL4Bean getInstance() {
		return (FormL4Bean) ServiceFinder.findBean("formL4Bean");
	}

	@Override
	public void rowClicked(ContactDocument contactDocument) {
		setContactDoc(contactDocument);
		setUpForm();
	}

	@Override
	public String actionEdit() {
		try {
			if (form != null && form.getAddress() == null) {
				form.setAddress(new Address());
			}
			calculateEditFlags(false);
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
			LOG.debug("Form L4");
			FormL4DAO.getInstance().attachDirty(form);
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
		setUpForm();
		return super.actionCancel();
	}

	/** Method used to fetch the saved data for the selected form and to set that data in the Form instance. */
	private void setUpForm() {
		Integer relatedFormId = contactDoc.getRelatedFormId();
		LOG.debug("");
		if (relatedFormId != null) {
			setForm(FormL4DAO.getInstance().findById(relatedFormId));
		}
		else {
			setForm(new FormL4());
		}
		if (form.getAddress() == null) {
			form.setAddress(new Address());
		}
		form.getAddress().getAddrLine1(); // Prevents LIE when returning to form mini-tab
		if (form.getFirstNameMidInitial() == null) {
			FormL4DAO.getInstance().attachDirty(form);
			contactDoc.setRelatedFormId(form.getId());
			ContactDocumentDAO.getInstance().attachDirty(contactDoc);
		}
	}

	/* (non-Javadoc)
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#checkSaveValid()
	 */
	@Override
	public boolean checkSaveValid() {
		boolean isValid = true;
		if (form.getBlockA() != null && (form.getBlockA() < 0 || form.getBlockA() > 3)) {
			isValid = false;
			MsgUtils.addFacesMessage("FormL4Bean.ValidationMessage.BlockA", FacesMessage.SEVERITY_ERROR);
		}
		if (form.getBlockB() != null && (form.getBlockB() < 0 || form.getBlockB() > 99)) {
			isValid = false;
			MsgUtils.addFacesMessage("FormL4Bean.ValidationMessage.BlockB", FacesMessage.SEVERITY_ERROR);
		}
		isValid &= checkAddressValid(form.getAddress(), null);
		setSaveValid(isValid);
		return super.checkSaveValid();
	}

	/** Method used to check the validity of fields in the form.
	 * @return isValid
	 */
	@Override
	public boolean checkSubmitValid() {
		boolean isValid = true;
		User currentUser = SessionUtils.getCurrentUser();
		if (StringUtils.isEmpty(form.getFirstNameMidInitial())) {
			isValid = issueErrorMessage("First Name", false, "");
		}
		if (StringUtils.isEmpty(form.getLastName())) {
			isValid = issueErrorMessage("Last Name", false, "");
		}
		if (StringUtils.isEmpty(form.getSocialSecurity())){
			isValid = issueErrorMessage("Social Security Number", false, "");
		}
		if (! StringUtils.isEmpty(form.getSocialSecurity()) && currentUser != null &&
				(! StringUtils.isEmpty(currentUser.getSocialSecurity())) &&
				(! form.getSocialSecurity().equals(currentUser.getSocialSecurity()))) {
			isValid = issueErrorMessage("", false, ".SocialSecurity");
		}
		if (StringUtils.isEmpty(form.getMaritalStatus())) {
			isValid = issueErrorMessage("Marital Status", false, "");
		}
		isValid &= checkAddressValidMsg(form.getAddress());

		setSubmitValid(isValid);
		return super.checkSubmitValid();
	}

	/**
	 * Auto-fill the L4 form.
	 */
	@Override
	public String autoFillForm(boolean prompted) {
		try {
			User user = getContactDoc().getContact().getUser();
			user = UserDAO.getInstance().refresh(user);
			LOG.debug("Form id: " + getForm().getId());
			if (! StringUtils.isEmpty(user.getMiddleName())) { // LS-4793
				form.setFirstNameMidInitial(user.getFirstName() + " " + user.getMiddleName());
			}
			else {
				form.setFirstNameMidInitial(user.getFirstName());
			}
			form.setLastName(user.getLastName());
			form.setSocialSecurity(user.getSocialSecurity());
			if (form.getAddress() == null) {
				form.setAddress(new Address());
			}
			if (user.getHomeAddress() != null) {
				form.getAddress().copyFrom(user.getHomeAddress());
			}
			form.setMaritalStatus(user.getW4Marital());
			return super.autoFillForm(prompted);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#getFormById(Integer)
	 */
	@Override
	public FormL4 getFormById(Integer id) {
		return FormL4DAO.getInstance().findById(id);
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#refreshForm()
	 */
	@Override
	public void refreshForm() {
		if (form != null) {
			form = FormL4DAO.getInstance().refresh(form);
		}
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#getBlankForm()
	 */
	@Override
	public FormL4 getBlankForm() {
		FormL4 formL4 = new FormL4();
		formL4.setAddress(new Address());
		return formL4;
	}

}
