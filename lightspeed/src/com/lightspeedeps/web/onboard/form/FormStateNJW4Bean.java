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
 * Backing bean for the NJ-W4 form on the Payroll / Start Forms page.
 *
 * @see com.lightspeedeps.model.FormStateW4
 * @see StandardFormBean
 */
@ManagedBean
@ViewScoped
public class FormStateNJW4Bean extends FormStateW4Bean {
	private static final long serialVersionUID = 7051786239795829520L;
	private static final Log LOG = LogFactory.getLog(FormStateNJW4Bean.class);

	/**
	 * default constructor
	 */
	public FormStateNJW4Bean() {
		super("FormStateNJW4Bean");
	}

	public static FormStateNJW4Bean getInstance() {
		return (FormStateNJW4Bean) ServiceFinder.findBean("formStateNJW4Bean");
	}

	@Override
	public boolean checkSubmitValid() {
		boolean isValid = super.checkSubmitValid();
		
		if(!(form.isSingle() || form.isUnionCoupleJoint() || form.isUnionCoupleSeparate() || form.isHeadofHousehold() || form.isWidower()))
		{
			isValid = false;
			MsgUtils.addFacesMessage("Form.FillingStatusNJW4", FacesMessage.SEVERITY_ERROR);
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
	
	//Add listener for radio button
	public void listenSingle(ValueChangeEvent event) {
		form.setUnionCoupleJoint(false);
		form.setUnionCoupleSeparate(false);
		form.setHeadofHousehold(false);
		form.setWidower(false);
	}
	
	public void listenUnionCoupleJoint(ValueChangeEvent event) {
		form.setSingle(false);
		form.setUnionCoupleSeparate(false);
		form.setHeadofHousehold(false);
		form.setWidower(false);
	}
	
	public void listenUnionCoupleSeparate(ValueChangeEvent event) {
		form.setSingle(false);
		form.setUnionCoupleJoint(false);
		form.setHeadofHousehold(false);
		form.setWidower(false);
	}
	
	public void listenHeadofHousehold(ValueChangeEvent event) {
		form.setSingle(false);
		form.setUnionCoupleJoint(false);
		form.setUnionCoupleSeparate(false);
		form.setWidower(false);
	}
	
	public void listenWidower(ValueChangeEvent event) {
		form.setSingle(false);
		form.setUnionCoupleJoint(false);
		form.setUnionCoupleSeparate(false);
		form.setHeadofHousehold(false);
	}
}
