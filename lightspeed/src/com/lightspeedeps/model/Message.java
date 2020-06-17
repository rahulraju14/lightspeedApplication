package com.lightspeedeps.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

import com.lightspeedeps.type.NotificationMethod;

/**
 * Message entity. One Message exists for each notification and each method
 * of delivery.  Each Message is associated with a Set of MessageInstances,
 * one instance for each recipient (Contact) of the Message.
 */
@Entity
@Table(name = "message")
public class Message extends PersistentObject<Message> {

	private static final long serialVersionUID = 5680277265394623800L;

	public static final int MAX_MESSAGE_BODY_LENGTH = 16000;

	/** The separator referred to in the 'fileName' description. It separates the fully-qualified
	 * filename from the "display name" portion. */
	public static final String NAME_SEPARATOR = "?";

	// Fields

	/** How this message is to be delivered, e.g., via email or SMS. */
	private NotificationMethod method;

	/** The name or other identity of the sender. */
	private String sender;

	/** The subject line of the message (e.g., for emails). */
	private String subject;

	/** The body text of the message. May be plain text or HTML formatted. */
	private String body;

	/**
	 * Comma-delimited list of fully-qualified file names of attachment(s); null
	 * if no attachment. Each entry in the list may contain a "display name"
	 * following the path name, separated by '?'. E.g.,
	 * <pre>
	 * &quot;/usr/report/script123_456.pdf?Lost Horizons.pdf;/usr/report/aSecondFileToAttach.txt&quot;
	 * </pre>
	 * */
	private String fileName;

	/** The Notification instance which is the "parent" of this message. */
	private Notification notification;

	/** The Set of recipients (usually Contacts) who are to be sent this message. */
	private Set<MessageInstance> messageInstances = new HashSet<>();

	// Constructors

	/** default constructor */
	public Message() {
	}

	/** full constructor */
	public Message(NotificationMethod method, String sender, String subject, String body,
			Notification notification) {
		this.method = method;
		this.sender = sender;
		this.subject = subject;
		this.body = body;
		this.notification = notification;
	}

	// Property accessors

	@Enumerated(EnumType.STRING)
	@Column(name = "Method", nullable = false, length = 30)
	public NotificationMethod getMethod() {
		return method;
	}
	public void setMethod(NotificationMethod method) {
		this.method = method;
	}

	@Column(name = "Sender", length = 50)
	public String getSender() {
		return sender;
	}
	public void setSender(String sender) {
		this.sender = sender;
	}

	@Column(name = "Subject", length = 200)
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}

	@Column(name = "Body", length = 20000)
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}

	@ManyToOne(fetch = FetchType.LAZY) // Changed to Lazy rev 3.0.4801
	@JoinColumn(name = "Notification_Id")
	public Notification getNotification() {
		return notification;
	}
	public void setNotification(Notification notification) {
		this.notification = notification;
	}

	// GnG had EAGER; other side (MessageInstance) has Eager, which appears sufficient
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "message")
	public Set<MessageInstance> getMessageInstances() {
		return messageInstances;
	}
	public void setMessageInstances(Set<MessageInstance> messageInstances) {
		this.messageInstances = messageInstances;
	}

	/** See {@link #fileName}. */
	@Column(name = "filename", length = 600)
	public String getFileName() {
		return fileName;
	}
	/** See {@link #fileName}. */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
		result = prime * result + ((getMethod() == null) ? 0 : getMethod().hashCode());
		result = prime * result + ((getNotification() == null) ? 0 : getNotification().hashCode());
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
		Message other = null;
		try {
			other = (Message)obj;
		}
		catch (Exception e) {
			return false;
		}
		if (getId() == null) {
			if (other.getId() != null)
				return false;
		}
		else if (! id.equals(other.getId()))
			return false;
		if (getMethod() != other.getMethod())
			return false;
		if (getNotification() == null) {
			if (other.getNotification() != null)
				return false;
		}
		else if (! getNotification().equals(other.getNotification()))
			return false;
		return true;
	}

}