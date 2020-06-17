/**
 * PopupEmailTextBean.java
 */
package com.lightspeedeps.web.popup;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.ServiceFinder;

/**
 * Backing bean for the popupEmailText.xhtml dialog box. This extends
 * the PopupInputBean by adding a second input field (for the email body),
 * and surrogate methods so that PopupInputBean's input field can be
 * referred to as the "subject" field.
 */
@ManagedBean
@ViewScoped
public class PopupEmailTextBean extends PopupInputBean {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(PopupEmailTextBean.class);

	/** */
	private static final long serialVersionUID = 1106624262074527592L;

	/** The maximum length allowed for the input field. */
	private int maxBodyLength = 5000;

	/** Backing field for the user's input. */
	private String body;

	/**
	 * Default constructor.
	 */
	public PopupEmailTextBean() {
		setSubjectRequired(true);
	}

	/**
	 * @return The instance of this bean appropriate for the current context.
	 */
	public static PopupEmailTextBean getInstance() {
		return (PopupEmailTextBean)ServiceFinder.findBean("popupEmailTextBean");
	}

	/** See {@link #getInputRequired()}. */
	public boolean getSubjectRequired() {
		return getInputRequired();
	}
	/** See {@link #setInputRequired(boolean)}. */
	public void setSubjectRequired(boolean required) {
		setInputRequired(required);
	}

	/**See {@link #getMaxLength()} */
	public int getMaxSubjectLength() {
		return getMaxLength();
	}
	/**See {@link #setMaxLength(int)} */
	public void setMaxSubjectLength(int maxLength) {
		setMaxLength(maxLength);
	}

	/** See {@link #maxBodyLength}. */
	public int getMaxBodyLength() {
		return maxBodyLength;
	}
	/** See {@link #maxBodyLength}. */
	public void setMaxBodyLength(int maxBodyLength) {
		this.maxBodyLength = maxBodyLength;
	}

	public String getSubject() {
		return getInput();
	}
	public void setSubject(String subject) {
		setInput(subject);
	}

	/** See {@link #body}. */
	public String getBody() {
		return body;
	}
	/** See {@link #body}. */
	public void setBody(String body) {
		this.body = body;
	}

}
