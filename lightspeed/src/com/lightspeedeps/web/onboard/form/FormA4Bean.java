package com.lightspeedeps.web.onboard.form;

import java.io.Serializable;
import java.util.Arrays;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ContactDocumentDAO;
import com.lightspeedeps.dao.FormA4DAO;
import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.model.Address;
import com.lightspeedeps.model.ContactDocument;
import com.lightspeedeps.model.FormA4;
import com.lightspeedeps.model.User;
import com.lightspeedeps.type.WithholdPercentage;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.StringUtils;

/**
 * Backing bean for the A-4 form on the Payroll / Start Forms page.
 *
 * @see com.lightspeedeps.model.FormA4
 * @see StandardFormBean
 */
@ManagedBean
@ViewScoped
public class FormA4Bean extends StandardFormBean<FormA4> implements Serializable {

	private static final long serialVersionUID = 1107417216337387440L;

	private static final Log LOG = LogFactory.getLog(FormA4Bean.class);

	/** Boolean array used for the selection of withold/non-withold radio buttons. */
	private Boolean zeroWitholding[] = new Boolean[2];

	/** Boolean array used for the selection of withold percentage radio buttons. */
	private Boolean witholdPercentage[] = new Boolean[8];


	public FormA4Bean() {
		super("FormA4Bean");
		resetBooleans();
	}

	public static FormA4Bean getInstance() {
		return (FormA4Bean) ServiceFinder.findBean("formA4Bean");
	}

	/**
	 * Set all our backing boolean arrays to all False.
	 */
	private void resetBooleans() {
		Arrays.fill(zeroWitholding, Boolean.FALSE);
		Arrays.fill(witholdPercentage, Boolean.FALSE);
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
			LOG.debug("Form A4");
			for (WithholdPercentage type : WithholdPercentage.values()) {
				if (witholdPercentage[type.getIndex()]) {
					form.setWithholdPercentage(type);
					break;
				}
			}
			if (witholdPercentage[0]) {
				form.setZeroWithholding(false);
			}
			else if (witholdPercentage[1]) {
				form.setZeroWithholding(true);
			}
			FormA4DAO.getInstance().attachDirty(form);
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

	/** Method used to check the validity of fields in the form.
	 * @return isValid
	 */
	@Override
	public boolean checkSubmitValid() {
		boolean isValid = true;
		User currentUser = SessionUtils.getCurrentUser();
		if (StringUtils.isEmpty(form.getFullName())) {
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
	 * Auto-fill the A4 form.
	 */
	@Override
	public String autoFillForm(boolean prompted) {
		try {
			User user = getContactDoc().getContact().getUser();
			user = UserDAO.getInstance().refresh(user);
			LOG.debug("Form id: " + getForm().getId());
			form.setFullName(user.getFullName());
			form.setSocialSecurity(user.getSocialSecurity());
			if (form.getAddress() == null) {
				form.setAddress(new Address());
			}
			if (user.getHomeAddress() != null) {
				form.getAddress().copyFrom(user.getHomeAddress());
			}
			if (! StringUtils.isEmpty(form.getAddress().getAddrLine2())) {
				LOG.debug("Check for address: ");
				form.getAddress().setAddrLine1(form.getAddress().getAddrLine1Line2());
				form.getAddress().setAddrLine2(null);
			}
			return super.autoFillForm(prompted);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** Method used to fetch the saved data for the selected form and to set that data in the Form instance. */
	private void setUpForm() {
		Integer relatedFormId = contactDoc.getRelatedFormId();
		LOG.debug("");
		resetBooleans();
		if (relatedFormId != null) {
			FormA4 form = FormA4DAO.getInstance().findById(relatedFormId);
			setForm(form);
			if (form.getZeroWithholding() != null) {
				if (! form.getZeroWithholding()) {
					LOG.debug("");
					zeroWitholding[0] = true;
					if (form.getWithholdPercentage() != null) {
						int index = form.getWithholdPercentage().getIndex();
						witholdPercentage[index] = true;
					}
				}
				else {
					LOG.debug("");
					zeroWitholding[1] = true;
				}
			}
		}
		else {
			setForm(new FormA4());
		}
		if (form.getAddress() == null) {
			form.setAddress(new Address());
		}
		form.getAddress().getAddrLine1();  // Prevents LIE when returning to form mini-tab
		if (form.getFullName() == null) {
			FormA4DAO.getInstance().attachDirty(form);
			contactDoc.setRelatedFormId(form.getId());
			ContactDocumentDAO.getInstance().attachDirty(contactDoc);
		}
	}

	/** Listener for the Radio buttons for "Withhold Percentage"
	 *  used to clear the fields for the unselected button.
	 * @param event
	 */
	public void listenChangeWithhold(ValueChangeEvent event) {
		refreshRadioButtons(event);
		LOG.debug("");
		boolean newValue = (boolean) event.getNewValue();
		if (zeroWitholding[0] && newValue) {
			LOG.debug("");
			form.setZeroWithholding(false);
			form.setExtraAmount(null);
			witholdPercentage[0] = true;
			form.setWithholdPercentage(WithholdPercentage.Per_27);
			zeroWitholding[1] = false;
		}
	}
	
	public void listenChangeWithholdBox(ValueChangeEvent event) {
		refreshRadioButtons(event);
		LOG.debug("");
		boolean newValue = (boolean) event.getNewValue();
		if (zeroWitholding[1] && newValue) {
			LOG.debug("");
			form.setZeroWithholding(true);
			form.setWithholdPercentage(null);
			form.setExtraAmount(null);
			Arrays.fill(witholdPercentage, Boolean.FALSE);
			zeroWitholding[0] = false;
		}
	}

	/** Listener for the Radio buttons for "Withhold Percentage"
	 *  used to clear the fields for the unselected button.
	 * @param event
	 */
	public void listenChangeWithholdPercentage(ValueChangeEvent event) {
		refreshRadioButtons(event);
		LOG.debug("");
		for (WithholdPercentage type : WithholdPercentage.values()) {
			LOG.debug("TYPE = " + type.name());
			if (witholdPercentage[type.getIndex()]) {
				form.setWithholdPercentage(type);
				if (type != WithholdPercentage.Per_Extra) {
					form.setExtraAmount(null);
				}
				break;
			}
		}
		for (WithholdPercentage type : WithholdPercentage.values()) {
			if (type != form.getWithholdPercentage()) {
				witholdPercentage[type.getIndex()] = false;
			}
		}
	}

	@Override
	public boolean checkSaveValid() {
		boolean isValid = true;
		isValid = checkAddressValid(form.getAddress(), null);
		setSaveValid(isValid);
		return super.checkSaveValid();
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#getFormById(Integer)
	 */
	@Override
	public FormA4 getFormById(Integer id) {
		return FormA4DAO.getInstance().findById(id);
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#refreshForm()
	 */
	@Override
	public void refreshForm() {
		if (form != null) {
			form = FormA4DAO.getInstance().refresh(form);
		}
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#getBlankForm()
	 */
	@Override
	public FormA4 getBlankForm() {
		FormA4 formA4 = new FormA4();
		formA4.setAddress(new Address());
		return formA4;
	}

	/**See {@link #zeroWitholding}. */
	public Boolean[] getZeroWitholding() {
		return zeroWitholding;
	}
	/**See {@link #zeroWitholding}. */
	public void setZeroWitholding(Boolean[] zeroWitholding) {
		this.zeroWitholding = zeroWitholding;
	}

	/**See {@link #witholdPercentage}. */
	public Boolean[] getWitholdPercentage() {
		return witholdPercentage;
	}
	/**See {@link #witholdPercentage}. */
	public void setWitholdPercentage(Boolean[] witholdPercentage) {
		this.witholdPercentage = witholdPercentage;
	}

}
