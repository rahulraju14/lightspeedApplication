package com.lightspeedeps.object;

import java.io.Serializable;
import java.util.Date;

import com.lightspeedeps.type.ApprovalStatus;

/** Wrapper class to hold some of the ContactDocument's information,
 * used to display on UI for Transfer tab . */
public class ContactDocumentInfo implements Serializable  {

	private static final long serialVersionUID = -1529332486596996399L;
	
	/** Holds the Id of the ContactDocument. */
	private Integer contactDocumentId;
	
	/** Holds the approval status of the ContactDocument. */
	private ApprovalStatus status;
	
	/** Holds the time when this contact document was sent to the payroll service. This will be null
    * if the document has not been transmitted yet. */
	private Date timeSent;
	
	private Date lastUpdated;
	
	private boolean selected;
	
	private boolean disabled;

	private boolean sentPerformer = false;
	private boolean sentUnion = false;
	private boolean sentTPS = false;

	public ContactDocumentInfo(Integer contactDocumentId, ApprovalStatus status, Date timeSent, Date lastUpdated) {
		super();
		this.contactDocumentId = contactDocumentId;
		this.status = status;
		this.timeSent = timeSent;
		this.lastUpdated = lastUpdated;
	}

	/**See {@link #contactDocumentId}. */
	public Integer getContactDocumentId() {
		return contactDocumentId;
	}
	/**See {@link #contactDocumentId}. */
	public void setContactDocumentId(Integer contactDocumentId) {
		this.contactDocumentId = contactDocumentId;
	}

	/**See {@link #status}. */
	public ApprovalStatus getStatus() {
		return status;
	}
	/**See {@link #status}. */
	public void setStatus(ApprovalStatus status) {
		this.status = status;
	}

	/**See {@link #timeSent}. */
	public Date getTimeSent() {
		return timeSent;
	}
	/**See {@link #timeSent}. */
	public void setTimeSent(Date timeSent) {
		this.timeSent = timeSent;
	}

	/**See {@link #lastUpdated}. */
	public Date getLastUpdated() {
		return lastUpdated;
	}
	/**See {@link #lastUpdated}. */
	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
	
	/** See {@link #selected}. */
	public boolean getSelected() {
		return selected;
	}
	/** See {@link #selected}. */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/** See {@link #disabled}. */
	public boolean getDisabled() {
		return disabled;
	}
	/** See {@link #disabled}. */
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	/** See {@link #sentPerformer. */
	public boolean isSentPerformer() {
		return sentPerformer;
	}

	/** See {@link #sentPerformer. */
	public void setSentPerformer(boolean sentPerformer) {
		this.sentPerformer = sentPerformer;
	}

	/** See {@link #sentUnion. */
	public boolean isSentUnion() {
		return sentUnion;
	}

	/** See {@link #sentUnion. */
	public void setSentUnion(boolean sentUnion) {
		this.sentUnion = sentUnion;
	}

	/** See {@link #sentTPS. */
	public boolean isSentTPS() {
		return sentTPS;
	}

	/** See {@link #sentTPS. */
	public void setSentTPS(boolean sentTPS) {
		this.sentTPS = sentTPS;
	}

}
