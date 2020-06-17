package com.lightspeedeps.web.onboard.form;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.common.StringUtils;

/**
 * Backing bean for the MA-W4 form on the Payroll / Start Forms page.
 *
 * @see com.lightspeedeps.model.FormStateW4
 * @see StandardFormBean
 */
@ManagedBean
@ViewScoped
public class FormStateMAW4Bean extends FormStateW4Bean {

	private static final long serialVersionUID = 7051786239795829520L;
	private static final Log LOG = LogFactory.getLog(FormStateMAW4Bean.class);


	/**
	 * default constructor
	 */
	public FormStateMAW4Bean() {
		super("FormStateMAW4Bean");
	}

	public static FormStateMAW4Bean getInstance() {
		return (FormStateMAW4Bean)ServiceFinder.findBean("formStateMAW4Bean");
	}

	@Override
	public boolean checkSubmitValid() {
		boolean isValid = super.checkSubmitValid();
		setSubmitValid(isValid);
		return isValid;
	}

	/**
	 * Auto-fill the MA-W4 form.
	 */
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
	public void setUpForm() {
		super.setUpForm();
	}


}
