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
 * Backing bean for the MN-W4 form on the Payroll / Start Forms page.
 *
 * @see com.lightspeedeps.model.FormStateW4
 * @see StandardFormBean
 */
@ManagedBean
@ViewScoped
public class FormStateMNW4Bean extends FormStateW4Bean {
	private static final long serialVersionUID = 7051786239795829520L;
	private static final Log LOG = LogFactory.getLog(FormStateMNW4Bean.class);

	/**
	 * default constructor
	 */
	public FormStateMNW4Bean() {
		super("FormStateMNW4Bean");
	}

	public static FormStateMNW4Bean getInstance() {
		return (FormStateMNW4Bean) ServiceFinder.findBean("formStateMNW4Bean");
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
		int checkGender=0;
		int checkAllowance=0;
		if (form.isSingle()) {
			checkGender++;
		} else if (form.isMarried()) {
			checkGender++;
		} else if (form.isMarriedWithhold()) {
			checkGender++;
		}
		if (checkGender==0){
			isValid = false;
			MsgUtils.addFacesMessage("Form.MartialStatus", FacesMessage.SEVERITY_ERROR);
		}
		if (form.isMnAllowances()) {
			checkAllowance++;
		} else if (form.isMnWithholding()) {
			checkAllowance++;
		} 
		if (checkAllowance==0){
			isValid = false;
			MsgUtils.addFacesMessage("Form.MNSection1Section2", FacesMessage.SEVERITY_ERROR);
		}		
		setSubmitValid(isValid);
		return isValid;
	}

	/** Method to pre-populate the form on creation. */
	@Override
	public void populateForm(boolean prompted) {
		LOG.debug(" ");
		form.setMnAllowances(true);
		super.populateForm(prompted);
		if (! StringUtils.isEmpty(form.getAddress().getAddrLine2())) {
			form.getAddress().setAddrLine1(form.getAddress().getAddrLine1Line2());
			form.getAddress().setAddrLine2(null);
		}
	}
	//Add listner
	public void listenSingleMartialStatus(ValueChangeEvent event) {
		form.setMarried(false);
		form.setMarriedWithhold(false);
	}
	
	public void listenMarriedStatus(ValueChangeEvent event) {
		form.setSingle(false);
		form.setMarriedWithhold(false);
	}
	
	public void listenMarriedWithholdStatus(ValueChangeEvent event) {
		form.setMarried(false);
		form.setSingle(false);
	}

	public void listenSectionA(ValueChangeEvent event) {
		form.setMnWithholding(false);
		form.setMnAllowances(false);
		form.setMnWithholdingSectionA(false);
		form.setMnWithholdingSectionB(false);
		form.setMnWithholdingSectionC(false);
		form.setMnWithholdingSectionD(false);
		form.setMnWithholdingSectionE(false);
		form.setMnWithholdingSectionF(false);
		form.setDomicileLine(null);
		form.setMnAllowancesSectionA(null);
		form.setMnAllowancesSectionB(null);
		form.setMnAllowancesSectionC(null);
		form.setMnAllowancesSectionD(null);
		form.setMnAllowancesSectionE(null);
		form.setMnAllowancesSectionF(null);

	}
	
	public void listenSectionB(ValueChangeEvent event) {
		form.setMnAllowances(false);
		form.setMnAllowancesSectionA(null);
		form.setMnAllowancesSectionB(null);
		form.setMnAllowancesSectionC(null);
		form.setMnAllowancesSectionD(null);
		form.setMnAllowancesSectionE(null);
		form.setMnAllowancesSectionF(null);
		form.setMnWithholding(false);
		form.setMnWithholdingSectionA(false);
		form.setMnWithholdingSectionB(false);
		form.setMnWithholdingSectionC(false);
		form.setMnWithholdingSectionD(false);
		form.setMnWithholdingSectionE(false);
		form.setMnWithholdingSectionF(false);
		form.setDomicileLine(null);

	}
}
