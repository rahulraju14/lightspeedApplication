/**
 * File: PopupTwoInputBean.java
 */
package com.lightspeedeps.web.popup;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.lightspeedeps.util.app.ServiceFinder;

/**
 * Backing bean for a generic dialog box with TWO input fields. This is a minor
 * extension to {@link PopupInputBigBean}.
 * 
 * @see PopupInputBean
 */
@ManagedBean
@ViewScoped
public class PopupTwoInputBean extends PopupInputBigBean {

	/**  */
	private static final long serialVersionUID = 1L;
	
	/** Backing field for the user's input. */
	private String inputB;

	/** Field for text above the inputB area. */
	private String messageB;

	/** Field for text to left of the inputB area. */
	private String leftLabelB;

	/** Field for text to right of the inputB area. */
	private String rightLabelB;

	/**
	 * The default constructor, which should not be used in the application
	 * code. Callers should use the getInstance() method.
	 */
	public PopupTwoInputBean() {
	}

	/**
	 * @return The instance of this bean appropriate for the current context.
	 */
	public static PopupTwoInputBean getInstance() {
		return (PopupTwoInputBean)ServiceFinder.findBean("popupTwoInputBean");
	}
	
	/** See {@link #inputB}. */
	public String getInputB() {
		return inputB;
	}
	/** See {@link #inputB}. */
	public void setInputB(String inputB) {
		this.inputB = inputB;
	}
	
	/** See {@link #messageB}. */
	public String getMessageB() {
		return messageB;
	}
	/** See {@link #messageB}. */
	public void setMessageB(String messageB) {
		this.messageB = messageB;
	}

	/** See {@link #leftLabelB}. */
	public String getLeftLabelB() {
		return leftLabelB;
	}
	/** See {@link #leftLabelB}. */
	public void setLeftLabelB(String leftLabelB) {
		this.leftLabelB = leftLabelB;
	}

	/** See {@link #rightLabelB}. */
	public String getRightLabelB() {
		return rightLabelB;
	}
	/** See {@link #rightLabelB}. */
	public void setRightLabelB(String rightLabelB) {
		this.rightLabelB = rightLabelB;
	}

}
