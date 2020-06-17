package com.lightspeedeps.object;

import java.io.Serializable;
import java.util.List;

import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.ContactDocument;

/** The wrapper class used to hold the contact of the current production
 *  and the contact documents of that contact. It is also helpful in creating
 *  the employee detail view on the start status tab.
 */
public class ContactDocItem implements Serializable{

	private static final long serialVersionUID = 4627135723324511414L;

	/** The contact instance, the contact that is part of the current production */
	private Contact contact;

	/** The List of contact documents of a specific contact */
	private List<ContactDocument> contactDocument;

	/** Default constructor */
	public ContactDocItem(){

	}

	/** Parameterized constructor
	 * @param contact current contact
	 * @param contactDocument list of contact documents of that contact
	 */
	public ContactDocItem(Contact contact, List<ContactDocument> contactDocument) {
		super();
		this.contact = contact;
		this.contactDocument = contactDocument;
	}

	/** See {@link #contact}. */
	public Contact getContact() {
		return contact;
	}
	/** See {@link #contact}. */
	public void setContact(Contact contact) {
		this.contact = contact;
	}

	/** See {@link #contactDocument}. */
	public List<ContactDocument> getContactDocument() {
		return contactDocument;
	}
	/** See {@link #contactDocument}. */
	public void setContactDocument(List<ContactDocument> contactDocument) {
		this.contactDocument = contactDocument;
	}
}
