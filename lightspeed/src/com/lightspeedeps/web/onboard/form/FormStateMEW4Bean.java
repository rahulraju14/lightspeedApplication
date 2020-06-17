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
public class FormStateMEW4Bean extends FormStateW4Bean {
	private static final long serialVersionUID = 7051786239795829520L;
	private static final Log LOG = LogFactory.getLog(FormStateMEW4Bean.class);

	/**
	 * default constructor
	 */
	public FormStateMEW4Bean() {
		super("FormStateMEW4Bean");
	}

	public static FormStateMEW4Bean getInstance() {
		return (FormStateMEW4Bean) ServiceFinder.findBean("formStateMEW4Bean");
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
		if (!form.isSingle() && !form.isMarried() && !form.isMarriedWithhold())  {
			isValid = false;
			MsgUtils.addFacesMessage("Form.CheckMEVaidation", FacesMessage.SEVERITY_ERROR);
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
	
	// Add listner
	public void listenSingleStatus(ValueChangeEvent event) {
		form.setSingle(true);
		form.setMarried(false);
		form.setMarriedWithhold(false);
	}

	public void listenMarriedStatus(ValueChangeEvent event) {
		form.setSingle(false);
		form.setMarried(true);
		form.setMarriedWithhold(false);
	}

	public void listenMarriedWithholdStatus(ValueChangeEvent event) {
		form.setSingle(false);
		form.setMarried(false);
		form.setMarriedWithhold(true);
	}

}
