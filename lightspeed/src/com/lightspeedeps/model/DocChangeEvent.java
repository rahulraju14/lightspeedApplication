package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import com.lightspeedeps.type.FormFieldType;
import com.lightspeedeps.type.TimedEventType;

/** DocChangeEvent monitors the change in the value of each and every field
 * of PDF document.
 * It is basically used to audit the change type and change value of PDF form fields
 * @author root
 *
 */
@NamedQueries ({
	@NamedQuery(name=DocChangeEvent.GET_DOCUMENT_EVENT_LIST_BY_CONTACT_DOCUMENT_ID, query = "from DocChangeEvent d where d.contactDocumentId =:contactDocumentId order by d.date desc, d.id desc")
})

@Entity
@Table(name = "doc_change_event")
public class DocChangeEvent extends ChangeEvent {

	/**
	 *
	 */
	private static final long serialVersionUID = -7587199040873769669L;

	public static final String GET_DOCUMENT_EVENT_LIST_BY_CONTACT_DOCUMENT_ID = "getDocumentEventListByContactDocumentId";

	private Integer contactDocumentId;

	private FormFieldType formFieldType;

	public DocChangeEvent() {
	}

	/**
	 * Parameterized constructor
	 *
	 * @param user User causing this event, typically the current user.
	 * @param eventType Timed Event Type usually CHANGE type
	 */
	public DocChangeEvent(User user, TimedEventType eventType) {
		super(user, eventType);
	}

	@Column(name = "Contact_Document_Id" , nullable = false)
	public Integer getContactDocumentId() {
		return contactDocumentId;
	}
	public void setContactDocumentId(Integer contactDocumentId) {
		this.contactDocumentId = contactDocumentId;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Form_Field_Type" , nullable = false, length = 30)
	public FormFieldType getFormFieldType() {
		return formFieldType;
	}
	public void setFormFieldType(FormFieldType formFieldType) {
		this.formFieldType = formFieldType;
	}

}
