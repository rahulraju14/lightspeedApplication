/**
 * StripBoardRequestBean.java
 */
package com.lightspeedeps.web.schedule;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.component.html.HtmlInputTextarea;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.component.html.HtmlSelectOneListbox;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.ServiceFinder;

/**
 * The purpose of this bean is to provide access, through the binding=
 * attribute, to the scheduled area (left side) of the Stripboard Edit page.
 * <p>
 * There's also a problem that the bound fields are not Serializable, and so
 * can't be members of a View scoped bean, which must be Serializable.
 * <p>
 * This needs to be a request-scoped bean -- the binding= forces a new
 * instantiation of the bean on each request, even if the bean were marked View
 * scope. So we bind the control to this bean, and our "real" View-scoped
 * backing bean simply finds this bean if it needs access to the bound control.
 */
@ManagedBean
@RequestScoped
public class StripBoardRequestBean {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(StripBoardRequestBean.class);

	/** The component bound to the left-hand (scheduled) list, used for clearing
	 * the children in the list. */
	private HtmlSelectOneListbox scheduledPanel;

	/** The input area on the Banner strip pop-up. We use this binding to set
	 * the focus on the input field. */
	private HtmlInputTextarea bannerText;

	/** An h:panelGroup wrapper around the list of Banner+EOD source strips. */
	private HtmlPanelGroup panelEodSource;

	public StripBoardRequestBean() {
	}

	public static StripBoardRequestBean getInstance() {
		return (StripBoardRequestBean)ServiceFinder.findBean("stripBoardRequestBean");
	}

	/** See {@link #panelEodSource}. */
	public HtmlPanelGroup getPanelEodSource() {
		return panelEodSource;
	}
	/** See {@link #panelEodSource}. */
	public void setPanelEodSource(HtmlPanelGroup panelEodSource) {
		this.panelEodSource = panelEodSource;
	}

	/** See {@link #scheduledPanel}. */
	public HtmlSelectOneListbox getScheduledPanel() {
		return scheduledPanel;
	}
	/** See {@link #scheduledPanel}. */
	public void setScheduledPanel(HtmlSelectOneListbox scheduledPanel) {
		this.scheduledPanel = scheduledPanel;
	}

	/**See {@link #bannerText}. */
	public HtmlInputTextarea getBannerText() {
		return bannerText;
	}
	/**See {@link #bannerText}. */
	public void setBannerText(HtmlInputTextarea bannerText) {
		this.bannerText = bannerText;
	}

}
