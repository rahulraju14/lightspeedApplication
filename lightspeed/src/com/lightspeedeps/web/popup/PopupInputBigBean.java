/**
 * File: PopupInputBigBean.java
 */
package com.lightspeedeps.web.popup;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.ServiceFinder;

/**
 * A class to display a simple confirmation dialog box, and get back an OK or
 * Cancel result, and pass that to our "holder", with the addition of a single
 * input field. This is an extension of the PopupInputBean that includes
 * optional text before & after the input field, and a second message area below
 * the input field.
 * <p>
 * The page that fronts the bean which is using this class should include the
 * common/popupInputBig.xhtml fragment.
 */
@ManagedBean
@ViewScoped
public class PopupInputBigBean extends PopupInputBean implements Serializable {
	/** */
	private static final long serialVersionUID = - 6013795720837282468L;

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(PopupInputBigBean.class);

	/** Field for text to left of input area. */
	private String leftLabel;

	/** Field for text to right of input area. */
	private String rightLabel;

	/** Field for second message, below input area. */
	private String message2;

	/**
	 * The default constructor, which should not be used in the application
	 * code. Callers should use the getInstance() method.
	 */
	public PopupInputBigBean() {
	}

	/**
	 * @return The instance of this bean appropriate for the current context.
	 */
	public static PopupInputBigBean getInstance() {
		return (PopupInputBigBean)ServiceFinder.findBean("popupInputBigBean");
	}

	/** See {@link #leftLabel}. */
	public String getLeftLabel() {
		return leftLabel;
	}
	/** See {@link #leftLabel}. */
	public void setLeftLabel(String leftLabel) {
		this.leftLabel = leftLabel;
	}

	/** See {@link #rightLabel}. */
	public String getRightLabel() {
		return rightLabel;
	}
	/** See {@link #rightLabel}. */
	public void setRightLabel(String rightLabel) {
		this.rightLabel = rightLabel;
	}

	/** See {@link #message2}. */
	public String getMessage2() {
		return message2;
	}
	/** See {@link #message2}. */
	public void setMessage2(String message2) {
		this.message2 = message2;
	}

}
