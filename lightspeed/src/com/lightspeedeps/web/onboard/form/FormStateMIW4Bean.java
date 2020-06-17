package com.lightspeedeps.web.onboard.form;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.*;

import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.web.onboard.form.FormStateW4Bean;

@ManagedBean
@ViewScoped
public class FormStateMIW4Bean extends FormStateW4Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7163363194499336390L;
	private static final Log LOG = LogFactory.getLog(FormStateMIW4Bean.class);

	/**
	 * 
	 * default constructor
	 * 
	 */

	public FormStateMIW4Bean() {
		super("FormStateMIW4Bean");
	}

	public static FormStateMIW4Bean getInstance() {
		return (FormStateMIW4Bean) ServiceFinder.findBean("formStateMIW4Bean");
	}
	

	@Override
	public boolean checkSubmitValid() {
		
		boolean isValid = super.checkSubmitValid();
		if (form.getDateofBirth()==null){
			isValid = issueErrorMessage("Date of Birth", false, "");
		}
		if (StringUtils.isEmpty(form.getLicenseNo())){
			isValid = issueErrorMessage("Driver License Number or State ID", false, "");
		}
		if (form.isEmployerYes()) {
			if (form.getDateofHire()==null) {
				isValid = issueErrorMessage("Date of Hire", false, "");
			}
		}
		if (form.isIs8b()) {
			if (StringUtils.isEmpty(form.getExplain())) {
				isValid = issueErrorMessage("Explain", false, "");
			}
		}
		if (form.isIs8c()) {
			if (StringUtils.isEmpty(form.getRenaissanceZone())) {
				isValid = issueErrorMessage("Renaissance Zone", false, "");
			}
		}
		
		if (!(form.isEmployerYes() || form.isEmployerNo())) {
			isValid = false;
			MsgUtils.addFacesMessage("Form.EmployerMIW4", FacesMessage.SEVERITY_ERROR);
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
	
	@Override
	public void setUpForm() {
		super.setUpForm();
	}
   
	//Add listner 
	public void listenEmployerYes(ValueChangeEvent event) {
		form.setEmployerNo(false);
	}
	
	public void listenEmployerNo(ValueChangeEvent event) {
		form.setEmployerYes(false);
		form.setDateofHire(null);
	}
	
	//Add listner for claim exception
	public void listen8A(ValueChangeEvent event) {
		form.setIs8b(false);
		form.setIs8c(false);
	}
	
	public void listen8B(ValueChangeEvent event) {
		form.setIs8a(false);
		form.setIs8c(false);
		form.setRenaissanceZone(null);
	}
	
	public void listen8C(ValueChangeEvent event) {
		form.setIs8a(false);
		form.setIs8b(false);
		form.setExplain(null);
	}

}
