package com.lightspeedeps.web.onboard.form;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.common.StringUtils;

/**
 * Backing bean for the VA-W4 form on the Payroll / Start Forms page.
 *
 * @see com.lightspeedeps.model.FormStateW4
 * @see StandardFormBean
 */
@ManagedBean
@ViewScoped
public class FormStateKYW4Bean extends FormStateW4Bean {
	private static final long serialVersionUID = 7051786239795829520L;
	private static final Log LOG = LogFactory.getLog(FormStateKYW4Bean.class);

	/**
	 * default constructor
	 */
	public FormStateKYW4Bean() {
		super("FormStateKYW4Bean");
	}

	public static FormStateKYW4Bean getInstance() {
		return (FormStateKYW4Bean) ServiceFinder.findBean("formStateKYW4Bean");
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
		if (!StringUtils.isEmpty(form.getAddress().getAddrLine2())) {
			form.getAddress().setAddrLine1(form.getAddress().getAddrLine1Line2());
			form.getAddress().setAddrLine2(null);
		}
	}
	
	//Listner
	public void listen8B(ValueChangeEvent event) {
		form.setCountyOfResidence(null);
	}
	
	public void listenIllinois(ValueChangeEvent event) {
		setMarkedAsFalse();
		form.setIllinois(true);
	}
	
	public void listenIndiana(ValueChangeEvent event) {
		setMarkedAsFalse();
		form.setIndiana(true);
	}
	public void listenMichingan(ValueChangeEvent event) {
		setMarkedAsFalse();
		form.setMichingan(true);
	}
	
	public void listenWestVirgina(ValueChangeEvent event) {
		setMarkedAsFalse();
		form.setWestVirgina(true);
	}
	
	public void listenWisconsin(ValueChangeEvent event) {
		setMarkedAsFalse();
		form.setWisconsin(true);
	}
	
	public void listenVirginia(ValueChangeEvent event) {
		setMarkedAsFalse();
		form.setVirginia(true);
	}
	
	public void listenOhio(ValueChangeEvent event) {
		setMarkedAsFalse();
		form.setOhio(true);
	}

	public void listenKYSectionAYes(ValueChangeEvent event) {
		form.setCheckBox1(true);
		form.setCheckBox2(false);
	}

	public void listenKYSectionANo(ValueChangeEvent event) {
		form.setCheckBox1(false);
		form.setCheckBox2(true);
	}

	public void listenKYSectionBYes(ValueChangeEvent event) {
		form.setCheckBox3(true);
		form.setCheckBox4(false);
	}
	
	public void listenKYSectionBNo(ValueChangeEvent event) {
		form.setCheckBox3(false);
		form.setCheckBox4(true);
	}

	public void listenKYSectionCYes(ValueChangeEvent event) {
		form.setCheckBox5(true);
		form.setCheckBox6(false);
	}
	
	public void listenKYSectionCNo(ValueChangeEvent event) {
		form.setCheckBox5(false);
		form.setCheckBox6(true);
	}
	
	public void listenKYSectionDYes(ValueChangeEvent event) {
		form.setCheckBox7(true);
		form.setCheckBox8(false);
	}
	
	public void listenKYSectionDNo(ValueChangeEvent event) {
		form.setCheckBox7(false);
		form.setCheckBox8(true);
	}
	
	public void listenKYSectionEYes(ValueChangeEvent event) {
		form.setCheckBox9(true);
		form.setCheckBox10(false);
	}
	
	public void listenKYSectionENo(ValueChangeEvent event) {
		form.setCheckBox9(false);
		form.setCheckBox10(true);
	}
	
	public void listenKYSectionFYes(ValueChangeEvent event) {
		form.setCheckBox11(true);
		form.setCheckBox12(false);
	}
	
	public void listenKYSectionFNo(ValueChangeEvent event) {
		form.setCheckBox11(false);
		form.setCheckBox12(true);
	}
	private void setMarkedAsFalse() {
		form.setIllinois(false);	
		form.setIndiana(false);
		form.setMichingan(false);
		form.setWestVirgina(false);
		form.setWisconsin(false);
		form.setVirginia(false);
		form.setOhio(false);
	}
}