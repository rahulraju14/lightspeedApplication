package com.lightspeedeps.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import com.lightspeedeps.util.app.Constants;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import com.lightspeedeps.type.NotificationMethod;

/**
 * MessageInstance entity.  When a Message is sent to one or more Contacts,
 * there is a MessageInstance created for each Contact.  The main purpose of
 * this is to track acknowledgment of the message by that Contact.
 *
 * @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "message_instance")
public class MessageInstance extends PersistentObject<MessageInstance> implements Comparable<MessageInstance>, Cloneable {
	//private static final Log log = LogFactory.getLog(MessageInstance.class);

	// Fields
	private static final long serialVersionUID = 1L;

	/** The Message to be sent. */
	private Message message;

	/** The User account to whom this instance of the message was sent; i.e, the recipient. */
	private User user;

	/** The production Contact (if any) to whom this instance of the message was sent; i.e, the recipient.
	 * May be null for messages outside any production, e.g., password reset. */
	private Contact contact;

	/** The email address to send the message if no User or Contact is specified. */
	private String emailAddress;

	private Date sent;
	private NotificationMethod sentMethod;
	private Byte acknowledged;
	private Date acknowledgedTime;
	private String acknowledgeKey;

	@Transient
	private boolean selected;

	// Constructors

	/** default constructor */
	public MessageInstance() {
	}

	/** Full constructor */
	public MessageInstance(Message message, User user, Contact contact, Date sent,
			NotificationMethod sentMethod) {
		this.message = message;
		this.user = user;
		this.contact = contact;
		this.sent = sent;
		this.sentMethod = sentMethod;
		acknowledged = Constants.FALSE;
	}

	/** Contact-based constructor */
	public MessageInstance(Message message, Contact contact, Date sent,
			NotificationMethod sentMethod, Byte acknowledged, Date acknowledgedTime) {
		this(message, contact.getUser(), contact, sent, sentMethod);
	}

	/** Email-based constructor */
	public MessageInstance(Message message, String emailAddr, Date sent,
			NotificationMethod sentMethod) {
		this(message, null, null, sent, sentMethod);
		emailAddress = emailAddr;
	}

	// Property accessors

	@ManyToOne(fetch = FetchType.EAGER) // EAGER fixes "no session" error when paging in Message Center
	@JoinColumn(name = "Message_Id", nullable = false)
	public Message getMessage() {
		return message;
	}
	public void setMessage(Message message) {
		this.message = message;
	}

	/** See {@link #user}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "User_Id")
	public User getUser() {
		return user;
	}
	/** See {@link #user}. */
	public void setUser(User user) {
		this.user = user;
	}

	@ManyToOne(fetch = FetchType.EAGER) // EAGER fixes "no session" error when paging in "All msgs" tab of Message Center
	@JoinColumn(name = "Contact_Id")
	public Contact getContact() {
		return contact;
	}
	public void setContact(Contact contact) {
		this.contact = contact;
	}

	/** See {@link #emailAddress}. */
	@Column(name = "Email_Address", length = 100)
	public String getEmailAddress() {
		return emailAddress;
	}
	/** See {@link #emailAddress}. */
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Sent", length = 0)
	public Date getSent() {
		return sent;
	}
	public void setSent(Date sent) {
		this.sent = sent;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Sent_Via", nullable = false, length = 30)
	public NotificationMethod getSentMethod() {
		return sentMethod;
	}
	public void setSentMethod(NotificationMethod sentMethod) {
		this.sentMethod = sentMethod;
	}

	@Column(name = "Acknowledged", nullable = false)
	public Byte getAcknowledged() {
		return acknowledged;
	}
	public void setAcknowledged(Byte acknowledged) {
		this.acknowledged = acknowledged;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Acknowledged_Time", length = 0)
	public Date getAcknowledgedTime() {
		return acknowledgedTime;
	}
	public void setAcknowledgedTime(Date acknowledgedTime) {
		this.acknowledgedTime = acknowledgedTime;
	}

	@Transient
	public boolean getAckFlag() {
		return getAcknowledged() == 1 ? true : false;
	}

	@Column(name = "acknowledge_Key", length = 20)
	public String getAcknowledgeKey() {
		return acknowledgeKey;
	}
	public void setAcknowledgeKey(String acknowledgeKey) {
		this.acknowledgeKey = acknowledgeKey;
	}

	/** See {@link #selected}. */
	@Transient
	public boolean getSelected() {
		return selected;
	}
	/** See {@link #selected}. */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}


	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getId() == null) ? 0 : id.hashCode());
		result = prime * result + ((getUser() == null) ? 0 : getUser().hashCode());
		result = prime * result + ((getMessage() == null) ? 0 : getMessage().hashCode());
		result = prime * result + ((getSent() == null) ? 0 : getSent().hashCode());
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;

		MessageInstance other = null;
		try {
			other = (MessageInstance)obj;
		}
		catch (Exception e) {
			return false;
		}
		if (id == null) {
			if (other.id != null)
				return false;
		}
		else if (!id.equals(other.id))
			return false;

		// Added email address and contact tests. LS-1649
		if ((! getSent().equals(other.getSent())) ||
				(! getMessage().equals(other.getMessage())) ||
				(getUser() != null && other.getUser() != null && (! getUser().equals(other.getUser())))
				|| (getContact() != null && other.getContact() != null && (! getContact().equals(other.getContact())))
				|| (getEmailAddress() != null && other.getEmailAddress() != null && ! getEmailAddress().equals(other.getEmailAddress()))) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(MessageInstance arg0) {
		if (equals(arg0)) {
			return 0;
		}
		int ret = 1;
		if (arg0 != null) {
			return compareTo(arg0, "");
		}
		return ret;
	}

	public int compareTo(MessageInstance mi2, String sortField, boolean ascending) {
		int ret = compareTo(mi2, sortField);
		return (ascending ? ret : (0-ret));
	}

	public int compareTo(MessageInstance mi2, String sortField) {
		int ret = 0;
		if (sortField == null) {
			// sort by time (later)
		}
		else if (sortField.equals("to") ) {
			ret = getContact().compareTo(mi2.getContact()); // use default Contact sort (last name/first name)
		}
		else if (sortField.equals("subject") ) {
			ret = getMessage().getSubject().compareToIgnoreCase(mi2.getMessage().getSubject());
		}
		if (ret == 0) { // unsorted, or specified column compared equal
			ret = getSent().compareTo(mi2.getSent()); // sort by timestamp
			if (ret == 0) { // avoid equality - causes random ordering of same-time notifications
				ret = getId().compareTo(mi2.getId()); // use db id as last resort
			}
		}
		return ret;
	}

	/**
	 * These clones are specifically used for Home page notification list.
	 * @see java.lang.Object#clone()
	 */
	@Override
	public MessageInstance clone() {
		MessageInstance msgi;
		try {
			msgi = (MessageInstance)super.clone();
			msgi.user = null;
			msgi.contact = null;
		}
		catch (CloneNotSupportedException e) {
			//log.error(e);
			return null;
		}
		return msgi;
	}

}
