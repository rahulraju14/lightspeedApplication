package com.lightspeedeps.web.onboard.form;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.ServiceFinder;

/**
 * Sample class for a state W4 bean.  This is only needed if the particular
 * state W4 requires validation or pre-population that is not provided by
 * the standard bean, FormStateW4Bean.  In this sample, "Xx" represents the
 * two-letter state code (e.g., "Ca").
 * <p>
 * If a state-specific bean is created, then ContactFormBean.getBeanInstance()
 * must also be updated, to return an instance of the specific bean.
 */
@ManagedBean
@ViewScoped
public class FormStateXxW4Bean extends FormStateW4Bean {

	/** serialization constant */
	private static final long serialVersionUID = -6825029144953574102L;

	private static final Log LOG = LogFactory.getLog(FormStateXxW4Bean.class);

	/**
	 * default constructor
	 */
	public FormStateXxW4Bean() {
		super("FormStateXxW4Bean");
	}

	public static FormStateXxW4Bean getInstance() {
		return (FormStateXxW4Bean) ServiceFinder.findBean("formStateXxW4Bean");
	}

	@Override
	protected void setUpForm() {
		super.setUpForm();

		// ... any special setup requirements added here

	}

	/**
	 * Method used to check the validity of fields in the form at the time the
	 * form is saved. Note that most validation is done at the time of Submit
	 * rather than Save.
	 *
	 * @return True if validation succeeds, false if it fails. For failure,
	 *         appropriate error messages should have been issued.
	 */
	@Override
	public boolean checkSaveValid() {
		boolean isValid = super.checkSaveValid();

		// .... any additional validations go here & may update 'isValid'

		setSaveValid(isValid);
		return isValid;
	}

	/**
	 * Method used to check the validity of fields in the form at the time the
	 * employee submits (signs) the form.
	 *
	 * @return True if validation succeeds, false if it fails. For failure,
	 *         appropriate error messages should have been issued.
	 */
	@Override
	public boolean checkSubmitValid() {
		boolean isValid = super.checkSubmitValid();

		// .... any additional validations go here & may update 'isValid'

		setSubmitValid(isValid);
		return isValid;
	}

	/** Method to pre-populate the form on creation. */
	@Override
	public void populateForm(boolean prompted) {
		LOG.debug(" ");
		super.populateForm(prompted);
		// ... add any non-standard fields here
	}

}
