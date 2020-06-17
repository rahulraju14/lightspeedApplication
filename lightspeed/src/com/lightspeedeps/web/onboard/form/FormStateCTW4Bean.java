package com.lightspeedeps.web.onboard.form;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.common.StringUtils;

/**
 * Backing bean for the CT-W4 form on the Payroll / Start Forms page.
 *
 * @see com.lightspeedeps.model.FormStateW4
 * @see StandardFormBean
 */
@ManagedBean
@ViewScoped
public class FormStateCTW4Bean extends FormStateW4Bean {

	private static final long serialVersionUID = - 4391297100235752568L;
	private static final Log LOG = LogFactory.getLog(FormStateCTW4Bean.class);
	private static final String WITHHOLDING_CODE_REGEX = "[A-Za-z]";
	private static final Pattern WITHHOLDING_CODE_PATTERN = Pattern.compile(WITHHOLDING_CODE_REGEX);

	/**
	 * default constructor
	 */
	public FormStateCTW4Bean() {
		super("FormStateCTW4Bean");
	}

	public static FormStateCTW4Bean getInstance() {
		return (FormStateCTW4Bean)ServiceFinder.findBean("formStateCTW4Bean");
	}

	@Override
	public String autoFillForm(boolean prompted) {
		try {
			super.autoFillForm(prompted);
			populateForm(prompted);
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
		LOG.debug(" ");
		super.populateForm(prompted);
		if (! StringUtils.isEmpty(form.getAddress().getAddrLine2())) {
			form.getAddress().setAddrLine1(
					form.getAddress().getAddrLine1() + " " + form.getAddress().getAddrLine2());
			form.getAddress().setAddrLine2(null);
		}
	}

	@Override
	public boolean checkSubmitValid() {
		boolean isValid = super.checkSubmitValid();
		if (StringUtils.isEmpty(form.getWithholdingCode())) {
			isValid = issueErrorMessage("Withholding Code", false, "");
		}
		if (StringUtils.isEmpty(form.getFirstName())) {
			isValid = issueErrorMessage("First Name", false, "");
		}
		if (StringUtils.isEmpty(form.getLastName())) {
			isValid = issueErrorMessage("Last Name", false, "");
		}
		if (form.getMsrraExempt() && StringUtils.isEmpty(form.getLegalStateOfRes())) {
			MsgUtils.addFacesMessage("Form.CT.MSSRA.Exempt.Validation",
					FacesMessage.SEVERITY_ERROR);
			isValid = false;
		}
		if (! StringUtils.isEmpty(form.getWithholdingCode())) {
			Matcher matcher = WITHHOLDING_CODE_PATTERN.matcher(form.getWithholdingCode());
			boolean valid = matcher.matches();
			if (! valid) {
				MsgUtils.addFacesMessage("FormStateCTW4Bean.ValidationMessage.WithholdCode",
						FacesMessage.SEVERITY_ERROR);
				isValid = false;
			}
		}
		setSubmitValid(isValid);
		return isValid;
	}

	/**
	 * listener for MSSRA exempt checkbox value change
	 * 
	 * @param evt
	 */
	public void listenMSSRAExemptChange(ValueChangeEvent evt) {
		boolean value = (boolean)evt.getNewValue();
		if (! value) {
			form.setLegalStateOfRes(null);
		}
	}

}

