/**
 * File: FormStateALW4Bean.java
 */
package com.lightspeedeps.web.onboard.form;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.User;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;

/**
 * 
 */
@ManagedBean
@ViewScoped
public class FormStateALW4Bean extends FormStateW4Bean {
	private static final long serialVersionUID = - 5748443087842468298L;
	private static final Log LOG = LogFactory.getLog(FormStateALW4Bean.class);
	
	public static FormStateALW4Bean getInstance() {
		return (FormStateALW4Bean)ServiceFinder.findBean("formStateALW4Bean");
	}
	
	/**
	 * default constructor
	 */
	public FormStateALW4Bean() {
		super("FormStateALW4Bean");
	}
	
	/**
	 * Auto-fill the State W-4 form.
	 */
	@Override
	public String autoFillForm(boolean prompted) {
		try {
			populateForm(prompted);
			return super.autoFillForm(prompted);
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

		User user = getContactDoc().getContact().getUser();
		user = getUserDAO().refresh(user);
		if(form.getAddress() != null) {
			form.getAddress().setAddrLine1(user.getHomeAddress().getAddrLine1Line2());
		}
	}
}
