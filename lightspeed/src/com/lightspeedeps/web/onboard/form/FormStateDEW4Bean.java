package com.lightspeedeps.web.onboard.form;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.common.StringUtils;

/**
 * Backing bean for the ME-W4 form on the Payroll / Start Forms page.
 *
 * @see com.lightspeedeps.model.FormStateW4
 * @see StandardFormBean
 */
@ManagedBean
@ViewScoped
public class FormStateDEW4Bean extends FormStateW4Bean {
	private static final long serialVersionUID = 7051786239795829520L;
	private static final Log LOG = LogFactory.getLog(FormStateDEW4Bean.class);

	/**
	 * default constructor
	 */
	public FormStateDEW4Bean() {
		super("FormStateDEW4Bean");
	}

	public static FormStateDEW4Bean getInstance() {
		return (FormStateDEW4Bean) ServiceFinder.findBean("formStateDEW4Bean");
	}

	@Override
	public boolean checkSubmitValid() {
		boolean isValid = super.checkSubmitValid();
		if (StringUtils.isEmpty(form.getFirstName())) {
			isValid = issueErrorMessage("First Name", false, "");
		}
		if (StringUtils.isEmpty(form.getLastName())) {
			isValid = issueErrorMessage("Last Name", false, "");
		}
		if (!form.isSingle() && !form.isMarried()) {
			isValid = false;
			MsgUtils.addFacesMessage("Form.MartialStatus", FacesMessage.SEVERITY_ERROR);
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
	}

	//Listener
	public void listenSingleMartialStatus(ValueChangeEvent event) {
		form.setSingle(true);
		form.setMarried(false);
	}
	
	public void listenMarriedMartialStatus(ValueChangeEvent event) {
		form.setSingle(false);
		form.setMarried(true);
	}
}
