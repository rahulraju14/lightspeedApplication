package com.lightspeedeps.model;

import javax.persistence.Id;

import org.springframework.data.mongodb.core.mapping.Document;

/**
 * The entity representing the persisted contents of a XFDF string. This is
 * designed so that a pdf document's xfdf data can be stored in a separate
 * (possibly non-SQL) database.
 *
 * @see com.lightspeedeps.dao.XfdfContentDAO
 * @see com.lightspeedeps.model.ContactDocument
 */
@Document(collection=XfdfContent.COLLECTION_NAME)
public class XfdfContent {

	/**
	 * The name of the Collection in which the data will be stored. This MUST
	 * match the value of the 'collection' keyword in our @Document tag. It is
	 * used by the {@link com.lightspeedeps.dao.XfdfContentDAO} class.
	 */
	public static final String COLLECTION_NAME = "xfdfcontent";

	/**
	 * String literal used to store the initial XFDF content into mongo
	 */
	public static final String EMPTY_XFDF = "XFDF";

	@Id
	/** Contact Document Id, unique for each distributed document */
	private Integer contact_doc_id;

	/** Distributed documents XFDF content */
	private String content;

	public XfdfContent (Integer contact_doc_id, String content) {
		setContactDocId(contact_doc_id);
		setContent(content);
	}

	/**See {@link #contact_doc_id}. */
	public Integer getContactDocId() {
		return contact_doc_id;
	}
	/**See {@link #contact_doc_id}. */
	public void setContactDocId(Integer contact_doc_id) {
		this.contact_doc_id = contact_doc_id;
	}

	/**See {@link #content}. */
	public String getContent() {
		return content;
	}
	/**See {@link #content}. */
	public void setContent(String content) {
		this.content = content;
	}

}
