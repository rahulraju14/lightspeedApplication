/**
 * BreakdownRequestBean.java
 */
package com.lightspeedeps.web.script;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.component.html.HtmlPanelGroup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * The purpose of this bean is to provide access, through the binding=
 * attribute, to the editing area (a PanelGroup) of the Breakdown page. This
 * needs to be a request-scoped bean -- the binding= forces a new instantiation
 * of the bean on each request, even if the bean were marked View scope. So we
 * bind the control to this bean, and our "real" View-scoped backing bean for
 * this page, BreakdownBean, simply finds this bean if it needs access to the
 * bound control.
 */
@ManagedBean
@RequestScoped
public class BreakdownRequestBean {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(BreakdownRequestBean.class);

	/** Field bound to panel containing all edit fields; used for clearing edit
	 * fields when Cancel is clicked. */
	private HtmlPanelGroup editPanel;

	public BreakdownRequestBean() {
	}

	/**See {@link #editPanel}. */
	public HtmlPanelGroup getEditPanel() {
		//log.debug(editPanel);
		return editPanel;
	}
	/**See {@link #editPanel}. */
	public void setEditPanel(HtmlPanelGroup editPanel) {
		//log.debug(editPanel);
		this.editPanel = editPanel;
	}

}
