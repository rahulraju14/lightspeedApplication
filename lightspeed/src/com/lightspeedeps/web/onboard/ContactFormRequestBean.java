/**
 * ContactFormRequestBean.java
 */
package com.lightspeedeps.web.onboard;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.view.facelets.component.UIRepeat;
import org.icefaces.ace.component.list.ListBase;

import com.lightspeedeps.util.app.ServiceFinder;

/**
 * The purpose of this bean is to provide access, through the binding=
 * attribute, to the left side table of the Payroll | Start Forms page,
 * which is backed by ContactFormBean.
 * <p>
 * There a problem that the bound fields are not Serializable, and so
 * can't be members of a View scoped bean, which must be Serializable.
 * <p>
 * This needs to be a request-scoped bean -- the binding= forces a new
 * instantiation of the bean on each request, even if the bean were marked View
 * scope. So we bind the control to this bean, and our "real" View-scoped
 * backing bean simply finds this bean if it needs access to the bound control.
 * <p>
 * If we don't use this technique, the View-scoped bean gets instantiated
 * on every view cycle.
 */
@ManagedBean
@RequestScoped
public class ContactFormRequestBean {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(ContactFormRequestBean.class);

	/** Variable used to bind the data table with the backing bean, also used in refreshing the data table after delete */
	private UIRepeat empTable = null; // new PanelSeries();

	public ContactFormRequestBean() {
	}

	public static ContactFormRequestBean getInstance() {
		return (ContactFormRequestBean)ServiceFinder.findBean("contactFormRequestBean");
	}

	/** See {@link #empTable}. */
	public UIRepeat getEmpTable() {
		return empTable;
	}
	/** See {@link #empTable}. */
	public void setEmpTable(UIRepeat empTable) {
		this.empTable = empTable;
	}

}
