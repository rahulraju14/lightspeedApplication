/**
 * CallSheetRequestBean.java
 */
package com.lightspeedeps.web.report;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.view.facelets.component.UIRepeat;

import com.lightspeedeps.util.app.ServiceFinder;

/**
 * The purpose of this bean is to provide access, through the binding=
 * attribute, to the crew-list areas (UIRepeat) of the Callsheet and DPR
 * pages in View and Edit modes. This needs to be a request-scoped bean -- the
 * binding= forces a new instantiation of the bean on each request, even if the
 * bean is marked View scope.
 * <p>
 * There's also a problem that the bound fields are not Serializable, and so
 * can't be members of a View scoped bean, which must be Serializable.
 * <p>
 * So we bind the controls to this bean, and our "real" View-scoped backing
 * beans, CallsheetViewBean and DprViewBean, simply find this bean when access
 * is needed to the bound controls.
 */
@ManagedBean
@RequestScoped
public class CallSheetRequestBean {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(CallSheetRequestBean.class);

	/** The object bound to the left-hand column on the crew-call page in Edit mode. */
	private UIRepeat crewTable0;
	/** The object bound to the middle column on the crew-call page in Edit mode. */
	private UIRepeat crewTable1;
	/** The object bound to the right-hand column on the crew-call page in Edit mode. */
	private UIRepeat crewTable2;

	/** The object bound to the left-hand column on the crew-call page in View mode. */
	private UIRepeat crewTable0v;
	/** The object bound to the middle column on the crew-call page in View mode. */
	private UIRepeat crewTable1v;
	/** The object bound to the right-hand column on the crew-call page in View mode. */
	private UIRepeat crewTable2v;

	public CallSheetRequestBean() {
	}

	public static CallSheetRequestBean getInstance() {
		return (CallSheetRequestBean)ServiceFinder.findBean("callSheetRequestBean");
	}

	/** See {@link #crewTable0}. */
	public UIRepeat getCrewTable0() {
		return crewTable0;
	}
	/** See {@link #crewTable0}. */
	public void setCrewTable0(UIRepeat crewTable0) {
		this.crewTable0 = crewTable0;
	}

	/** See {@link #crewTable1}. */
	public UIRepeat getCrewTable1() {
		return crewTable1;
	}
	/** See {@link #crewTable1}. */
	public void setCrewTable1(UIRepeat crewTable1) {
		this.crewTable1 = crewTable1;
	}

	/** See {@link #crewTable2}. */
	public UIRepeat getCrewTable2() {
		return crewTable2;
	}
	/** See {@link #crewTable2}. */
	public void setCrewTable2(UIRepeat crewTable2) {
		this.crewTable2 = crewTable2;
	}

	/**See {@link #crewTable0v}. */
	public UIRepeat getCrewTable0v() {
		return crewTable0v;
	}
	/**See {@link #crewTable0v}. */
	public void setCrewTable0v(UIRepeat crewTable0v) {
		this.crewTable0v = crewTable0v;
	}

	/**See {@link #crewTable1v}. */
	public UIRepeat getCrewTable1v() {
		return crewTable1v;
	}
	/**See {@link #crewTable1v}. */
	public void setCrewTable1v(UIRepeat crewTable1v) {
		this.crewTable1v = crewTable1v;
	}

	/**See {@link #crewTable2v}. */
	public UIRepeat getCrewTable2v() {
		return crewTable2v;
	}
	/**See {@link #crewTable2v}. */
	public void setCrewTable2v(UIRepeat crewTable2v) {
		this.crewTable2v = crewTable2v;
	}

}
