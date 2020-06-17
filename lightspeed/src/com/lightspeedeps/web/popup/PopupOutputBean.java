package com.lightspeedeps.web.popup;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.lightspeedeps.util.app.ServiceFinder;

/**
 * Backing bean for a simple pop-up dialog box with a
 * single display area.
 */
@ManagedBean
@ViewScoped
public class PopupOutputBean extends PopupInputBigBean {

	private static final long serialVersionUID = 1L;

	private String output;

	public PopupOutputBean() {
	}

	/**
	 * @return The instance of this bean appropriate for the current context.
	 */
	public static PopupOutputBean getInstance() {
		return (PopupOutputBean)ServiceFinder.findBean("popupOutputBean");
	}

	public String getOutput() {
		return output;
	}
	public void setOutput(String output) {
		this.output = output;
	}

}
