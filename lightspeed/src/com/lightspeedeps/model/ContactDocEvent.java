package com.lightspeedeps.model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * A class used to record timed, and often signed, events, such as Submit and
 * Approve, that happen to a ContactDocument object. Part of the On-Boarding
 * system.
 *
 * @see SignedEvent
 */
@Entity
@Table (name = "contact_doc_event")
public class ContactDocEvent extends SignedEvent<ContactDocEvent> {

	private static final long serialVersionUID = -9194314379055540644L;

	private ContactDocument contactDocument;

	public ContactDocEvent() {
		super();
	}
	/** See {@link #contactDocument}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Contact_Document_Id")
	public ContactDocument getContactDocument() {
		return contactDocument;
	}
	/** See {@link #contactDocument}. */
	public void setContactDocument(ContactDocument contactDocument) {
		this.contactDocument = contactDocument;
	}
}
