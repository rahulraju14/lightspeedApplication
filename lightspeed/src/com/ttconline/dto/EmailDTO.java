package com.ttconline.dto;

/**
 * DTO object used for email purposes, e.g., sending email via an API.
 *  ESS-1471
 */
public class EmailDTO {

	/** 4-digit year which may be included in the email body. */
	private String copyRightYear;

	/** action URL to perform particular action from the email **/
	private String actionUrl;

	/** this contains the type of email we are sending, used in if else condition
	 *  to change the email content based on the type.**/
	private String type;

	/** based on this indicator the email content will display French. **/
	private String displayFrench;

	/** indicates if the source is ESS/TTCO **/
	private String source;

	/** Name of the TTCO production related to the email. */
	private String productionName;

	/** The email address to which the email will be sent. */
	private String toAddress;

	/** The email of the person issuing an invitation to a TTCO production.
	 * May be used in the subject or body of the generated email. */
	private String inviterEmail;

	/** The full name of the person issuing an invitation to a TTCO production.
	 * May be used in the subject or body of the generated email. */
	private String inviterName;

	/** If false, email indicates that user has been given a new Account. */
	private boolean existingUser;
	
	
	/** Usual constructor */
	public EmailDTO(String type) {
		super();
		this.type = type;
	}
	
	public EmailDTO(String actionUrl,String copyRightYear) {
		super();
		this.actionUrl = actionUrl;
		this.copyRightYear = copyRightYear;
	}

	public String getCopyRightYear() {
		return copyRightYear;
	}
	public void setCopyRightYear(String copyRightYear) {
		this.copyRightYear = copyRightYear;
	}

		/** See {@link #actionUrl}. */
	public String getActionUrl() {
		return actionUrl;
	}
	/** See {@link #actionUrl}. */
	public void setActionUrl(String actionUrl) {
		this.actionUrl = actionUrl;
	}

	/** See {@link #type}. */
	public String getType() {
		return type;
	}
	/** See {@link #type}. */
	public void setType(String type) {
		this.type = type;
	}

	/** See {@link #displayFrench}. */
	public String getDisplayFrench() {
		return displayFrench;
	}
	/** See {@link #displayFrench}. */
	public void setDisplayFrench(String displayFrench) {
		this.displayFrench = displayFrench;
	}

	/** See {@link #source}. */
	public String getSource() {
		return source;
	}
	/** See {@link #source}. */
	public void setSource(String source) {
		this.source = source;
	}

	/** See {@link #productionName}. */
	public String getProductionName() {
		return productionName;
	}
	/** See {@link #productionName}. */
	public void setProductionName(String productionName) {
		this.productionName = productionName;
	}

	/** See {@link #toAddress}. */
	public String getToAddress() {
		return toAddress;
	}
	/** See {@link #toAddress}. */
	public void setToAddress(String toAddress) {
		this.toAddress = toAddress;
	}

	/** See {@link #inviterEmail}. */
	public String getInviterEmail() {
		return inviterEmail;
	}
	/** See {@link #inviterEmail}. */
	public void setInviterEmail(String inviterEmail) {
		this.inviterEmail = inviterEmail;
	}

	/** See {@link #inviterName}. */
	public String getInviterName() {
		return inviterName;
	}
	/** See {@link #inviterName}. */
	public void setInviterName(String inviterName) {
		this.inviterName = inviterName;
	}

	/** See {@link #existingUser}. */
	public boolean isExistingUser() {
		return existingUser;
	}
	/** See {@link #existingUser}. */
	public void setExistingUser(boolean existingUser) {
		this.existingUser = existingUser;
	}

}
