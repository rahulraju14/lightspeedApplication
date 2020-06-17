package com.lightspeedeps.model;

import java.util.Date;

import javax.persistence.*;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import com.lightspeedeps.type.MimeType;

@NamedQueries ({
	@NamedQuery(name=Attachment.GET_ATTACHMENT_BY_NAME_CONTACT_DOC, query = "from Attachment a where a.name =:name and a.contactDocument =:contactDocument"),
	@NamedQuery(name=Attachment.GET_ATTACHMENT_BY_NAME_TIMECARD, query = "from Attachment a where a.name =:name and a.weeklyTimecard =:weeklyTimecard"),
})
@Entity
@Table(name = "attachment", 
		uniqueConstraints = {@UniqueConstraint(columnNames = {"Name", "Contact_Document_Id"}), 
							@UniqueConstraint(columnNames = {"Name", "Weekly_Timecard_Id"})})
public class Attachment extends PersistentObject<Attachment> {

	private static final long serialVersionUID = 2722699496457211654L;

	//Named Queries Literals
	public static final String GET_ATTACHMENT_BY_NAME_CONTACT_DOC = "getAttachmentByNameContactDoc";
	
	public static final String GET_ATTACHMENT_BY_NAME_TIMECARD = "getAttachmentByNameTimecard";

	/** The Attachment's owner. */
	private User user;

	/** The name of the Attachment, which must be unique within it's containing
	 * Folder. Attachment names are NOT case sensitive.*/
	private String name;

	/** The date the original (underlying) Attachment was uploaded. */
	private Date uploaded;

	/** The ContactDocument that contains this attachment. **/
	private ContactDocument contactDocument;
	
	/** The WeeklyTimecard that contains this attachment. **/
	private WeeklyTimecard weeklyTimecard;

	private boolean isPrivate;

	/** True if the current user is the owner of the private document. */
	@Transient
	private boolean isOwner = false;

	/** The enum for attachment's content type */
	private MimeType mimeType;

	// Constructors

	/** default constructor */
	public Attachment() {

	}

	// Property accessors

	/** See {@link #user}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Owner_Id")
	public User getUser() {
		return user;
	}
	/** See {@link #user}. */
	public void setUser(User user) {
		this.user = user;
	}

	/** See {@link #name}. */
	@Column(name = "Name", nullable = false, length = 100)
	public String getName() {
		return name;
	}
	/** See {@link #name}. */
	public void setName(String name) {
		this.name = name;
	}

	/** See {@link #uploaded}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Uploaded", nullable = false, length = 0)
	public Date getUploaded() {
		return uploaded;
	}
	/** See {@link #uploaded}. */
	public void setUploaded(Date uploaded) {
		this.uploaded = uploaded;
	}
	
	/** See {@link #contactDocument}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Contact_Document_Id", nullable = true)
	public ContactDocument getContactDocument() {
		return contactDocument;
	}
	/** See {@link #contactDocument}. */
	public void setContactDocument(ContactDocument contactDocument) {
		this.contactDocument = contactDocument;
	}
	
	/** See {@link #weeklyTimecard}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Weekly_Timecard_Id", nullable = true)
	public WeeklyTimecard getWeeklyTimecard() {
		return weeklyTimecard;
	}
	/** See {@link #weeklyTimecard}. */
	public void setWeeklyTimecard(WeeklyTimecard weeklyTimecard) {
		this.weeklyTimecard = weeklyTimecard;
	}

	/** See {@link #isPrivate}. */
	@Column(name = "Is_Private", nullable = false)
	public boolean getIsPrivate() {
		return isPrivate;
	}
	/** See {@link #isPrivate}. */
	public void setIsPrivate(boolean isPrivate) {
		this.isPrivate = isPrivate;
	}

	/** See {@link #mimeType}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Mime_Type", nullable = false, length = 30)
	public MimeType getMimeType() {
		return mimeType;
	}
	/** See {@link #mimeType}. */
	public void setMimeType(MimeType mimeType) {
		this.mimeType = mimeType;
	}

	/** See {@link #isOwner}. */
	@Transient
	public boolean getIsOwner() {
		return isOwner;
	}
	/** See {@link #isOwner}. */
	public void setIsOwner(boolean isOwner) {
		this.isOwner = isOwner;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "name=" + (getName() == null ? "null" : getName());
		s += "]";
		return s;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
		return result;
	}

	/**
	 * Compares Attachment objects based on their database id and/or name.
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		Attachment other = null;
		try {
			other = (Attachment) obj;
		} catch (Exception e) {
			return false;
		}
		if (getId() == null) {
			if (other.getId() != null) {
				return false;
			} else { // neither one is persisted, compare names
						// return StringUtils.compare(name, other.name) == 0;
			}
		}
		return getId().equals(other.getId());
	}

	/**
	 * The default comparison for Attachment, which uses the Attachment name.
	 *
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Attachment other) {
		if (equals(other)) {
			return 0;
		}
		return 0;// StringUtils.compare(name, other.name);
	}

}
