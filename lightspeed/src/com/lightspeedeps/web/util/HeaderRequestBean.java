/**
 * ContactFormRequestBean.java
 */
package com.lightspeedeps.web.util;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.component.html.HtmlOutputText;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
public class HeaderRequestBean {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(HeaderRequestBean.class);

	/** This is bound to a hidden outputText field on every page; the outputText should have
	 * an f:attribute child tag that specifies the tabId to be "selected" when the page
	 * is rendered.  This is part of the system that causes the correct tab to show as
	 * selected when we navigate to some arbitrary page from within the Java code. */
	private transient HtmlOutputText tabName;


	public HeaderRequestBean() {
//		log.debug("");
	}

	public static HeaderRequestBean getInstance() {
		return (HeaderRequestBean)ServiceFinder.findBean("headerRequestBean");
	}


	/**
	 * Support binding our tabname field to the ice:outputText field in each
	 * webpage, whose 'tabid' attribute will identify the matching tab for the page.
	 * @return The outputText object
	 */
	public HtmlOutputText getTabName() {
		return tabName;
	}
	public void setTabName(HtmlOutputText name) {
		tabName = name;
	}

}
