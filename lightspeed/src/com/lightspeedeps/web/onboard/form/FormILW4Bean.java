package com.lightspeedeps.web.onboard.form;

import java.io.Serializable;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ContactDocumentDAO;
import com.lightspeedeps.dao.FormILW4DAO;
import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.model.Address;
import com.lightspeedeps.model.ContactDocument;
import com.lightspeedeps.model.FormILW4;
import com.lightspeedeps.model.User;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.StringUtils;

/**
 * Backing bean for the IL-W4 form on the Payroll / Start Forms page.
 *
 * @see com.lightspeedeps.model.FormILW4
 * @see StandardFormBean
 */
@ManagedBean
@ViewScoped
public class FormILW4Bean extends StandardFormBean<FormILW4> implements Serializable {

	private static final long serialVersionUID = -4435458699490141669L;

	private static final Log LOG = LogFactory.getLog(FormILW4Bean.class);

	public FormILW4Bean() {
		super("FormILW4Bean");
	}

	public static FormILW4Bean getInstance() {
		return (FormILW4Bean) ServiceFinder.findBean("formILW4Bean");
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
			LOG.debug("Form IL-W4");
			FormILW4DAO.getInstance().attachDirty(form);
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
			setForm(FormILW4DAO.getInstance().findById(relatedFormId));
		}
		else {
			setForm(new FormILW4());
		}
		if (form.getAddress() == null) {
			form.setAddress(new Address());
		}
		form.getAddress().getAddrLine1(); // Prevents LIE when returning to form mini-tab
		if (form.getName() == null) {
			FormILW4DAO.getInstance().attachDirty(form);
			contactDoc.setRelatedFormId(form.getId());
			ContactDocumentDAO.getInstance().attachDirty(contactDoc);
		}
	}

	@Override
	public boolean checkSaveValid() {
		boolean isValid = true;
		if (form.getNumOfDependents() != null && form.getNumOfDependents() < 0) {
			isValid = false;
			MsgUtils.addFacesMessage("FormILW4Bean.ValidationMessage.NumOfDependents", FacesMessage.SEVERITY_ERROR);
		}
		if (form.getPersonalAllowances() != null && (form.getPersonalAllowances() < 0
				|| form.getPersonalAllowances() > form.getBasicPersonalAllowances())) {
			isValid = false;
			MsgUtils.addFacesMessage("FormILW4Bean.ValidationMessage.PersonalAllowances", FacesMessage.SEVERITY_ERROR);
		}
		if (form.getAddtlAllowances() != null && (form.getAddtlAllowances() < 0
				|| form.getAddtlAllowances() > form.getTotalAddtlAllowances())) {
			isValid = false;
			MsgUtils.addFacesMessage("FormILW4Bean.ValidationMessage.AddtlAllowances", FacesMessage.SEVERITY_ERROR);
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
		if (StringUtils.isEmpty(form.getName())) {
			isValid = issueErrorMessage("Name", false, "");
		}
		if (StringUtils.isEmpty(form.getSocialSecurity())){
			isValid = issueErrorMessage("Social Security Number", false, "");
		}
		if (! StringUtils.isEmpty(form.getSocialSecurity()) && currentUser != null &&
				(! StringUtils.isEmpty(currentUser.getSocialSecurity())) &&
				(! form.getSocialSecurity().equals(currentUser.getSocialSecurity()))) {
			isValid = issueErrorMessage("", false, ".SocialSecurity");
		}
		isValid &= checkAddressValidMsg(form.getAddress());
		setSubmitValid(isValid);
		return super.checkSubmitValid();
	}

	/**
	 * Auto-fill the IL-W4 form.
	 */
	@Override
	public String autoFillForm(boolean prompted) {
		try {
			User user = getContactDoc().getContact().getUser();
			user = UserDAO.getInstance().refresh(user);
			LOG.debug("Form id: " + getForm().getId());
			form.setName(user.getFullName());
			form.setSocialSecurity(user.getSocialSecurity());
			if (form.getAddress() == null) {
				form.setAddress(new Address());
			}
			if (user.getHomeAddress() != null) {
				form.getAddress().copyFrom(user.getHomeAddress());
			}
			form.setExempt(user.getW4Exempt());
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
	public FormILW4 getFormById(Integer id) {
		return FormILW4DAO.getInstance().findById(id);
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#refreshForm()
	 */
	@Override
	public void refreshForm() {
		if (form != null) {
			form = FormILW4DAO.getInstance().refresh(form);
		}
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#getBlankForm()
	 */
	@Override
	public FormILW4 getBlankForm() {
		FormILW4 formILW4 = new FormILW4();
		formILW4.setAddress(new Address());
		return formILW4;
	}

}
