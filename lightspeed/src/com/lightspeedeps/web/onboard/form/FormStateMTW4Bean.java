package com.lightspeedeps.web.onboard.form;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.common.StringUtils;

/**
 * Backing bean for the MT-W4 form on the Payroll / Start Forms page.
 *
 * @see com.lightspeedeps.model.FormStateW4
 * @see StandardFormBean
 */
@ManagedBean
@ViewScoped
public class FormStateMTW4Bean extends FormStateW4Bean {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8862851882793731162L;
	private static final Log LOG = LogFactory.getLog(FormStateMTW4Bean.class);

	/**
	 * default constructor
	 */
	public FormStateMTW4Bean() {
		super("FormStateMTW4Bean");
	}

	public static FormStateMTW4Bean getInstance() {
		return (FormStateMTW4Bean) ServiceFinder.findBean("formStateMTW4Bean");
	}

	@Override
	public boolean checkSubmitValid() {
		boolean isValid = super.checkSubmitValid();
		if (StringUtils.isEmpty(form.getFirstName())) {
			isValid = issueErrorMessage("First name and middle initial", false, "");
		}
		if (StringUtils.isEmpty(form.getLastName())) {
			isValid = issueErrorMessage("Last Name", false, "");
		}
		if (StringUtils.isEmpty(form.getCompleteAddress())) {
			isValid = issueErrorMessage("City state and Zip", false, "");
		}
		setSubmitValid(isValid);
		return isValid;
	}

	/** Method to pre-populate the form on creation. */
	@Override
	public void populateForm(boolean prompted) {
		LOG.debug(" ");
		super.populateForm(prompted);
		if (! StringUtils.isEmpty(form.getAddress().getAddrLine2())) {
			form.getAddress().setAddrLine1(form.getAddress().getAddrLine1Line2());
			form.getAddress().setAddrLine2(null);
		}
		form.setCompleteAddress(form.getAddress().getCityStateZip());
	}
	
	//listeners for MN
	public void listenWithholdingSectionA(ValueChangeEvent event) {
		form.setMnWithholdingSectionA(true);
		form.setMnWithholdingSectionB(false);
		form.setMnWithholdingSectionC(false);
		form.setMnWithholdingSectionD(false);
	}
	
	public void listenWithholdingSectionB(ValueChangeEvent event) {
		form.setMnWithholdingSectionA(false);
		form.setMnWithholdingSectionB(true);
		form.setMnWithholdingSectionC(false);
		form.setMnWithholdingSectionD(false);
	}
	
	public void listenWithholdingSectionC(ValueChangeEvent event) {
		form.setMnWithholdingSectionA(false);
		form.setMnWithholdingSectionB(false);
		form.setMnWithholdingSectionC(true);
		form.setMnWithholdingSectionD(false);
	}
	
	public void listenWithholdingSectionD(ValueChangeEvent event) {
		form.setMnWithholdingSectionA(false);
		form.setMnWithholdingSectionB(false);
		form.setMnWithholdingSectionC(false);
		form.setMnWithholdingSectionD(true);
	}
	
}
