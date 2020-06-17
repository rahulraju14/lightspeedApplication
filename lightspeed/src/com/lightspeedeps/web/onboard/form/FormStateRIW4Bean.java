package com.lightspeedeps.web.onboard.form;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
public class FormStateRIW4Bean extends FormStateW4Bean {
	private static final long serialVersionUID = 7051786239795829520L;
	private static final Log LOG = LogFactory.getLog(FormStateRIW4Bean.class);

	/**
	 * default constructor
	 */
	public FormStateRIW4Bean() {
		super("FormStateRIW4Bean");
	}

	public static FormStateRIW4Bean getInstance() {
		return (FormStateRIW4Bean) ServiceFinder.findBean("formStateRIW4Bean");
	}

	@Override
	public boolean checkSubmitValid() {
		boolean isValid = super.checkSubmitValid();
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

}
