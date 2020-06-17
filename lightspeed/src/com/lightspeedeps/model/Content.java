package com.lightspeedeps.model;

import javax.persistence.Id;

import org.springframework.data.mongodb.core.mapping.Document;

/**
 * The entity representing the persisted contents of a Document. This is
 * designed so that a document's content (body) can be stored in a separate
 * (possibly non-SQL) database.
 *
 * @see com.lightspeedeps.dao.ContentDAO
 * @see com.lightspeedeps.model.Document
 */
@Document(collection=Content.COLLECTION_NAME)
public class Content {

	/**
	 * The name of the Collection in which the data will be stored. This MUST
	 * match the value of the 'collection' keyword in our @Document tag. It is
	 * used by the {@link com.lightspeedeps.dao.ContentDAO} class.
	 */
	public static final String COLLECTION_NAME = "content";

	/**
	 * String literal used to initially store the XOD converted content into mongo
	 */
	public static final String EMPTY_XOD = "XOD";

	@Id
	/** Unique id of the uploaded document */
	private Integer doc_id;

	/** Uploaded documents content stored into a byte format */
	private byte[] content;

	/** Uploaded PDF's XFDF data, initially saved as "XFDF" */
	private String xfdfData;

	/** Uploaded PDF's XOD format content */
	private byte [] xodContent;

	/**See {@link #doc_id}. */
	public Integer getDocId() {
		return doc_id;
	}
	/**See {@link #doc_id}. */
	public void setDocId(Integer doc_id) {
		this.doc_id = doc_id;
	}

	/**See {@link #content}. */
	public byte[] getContent() {
		return content;
	}
	/**See {@link #content}. */
	public void setContent(byte[] content) {
		this.content = content;
	}

	/**See {@link #xfdfData}. */
	public String getXfdfData() {
		return xfdfData;
	}
	/**See {@link #xfdfData}. */
	public void setXfdfData(String xfdfData) {
		this.xfdfData = xfdfData;
	}

	/**See {@link #xodContent}. */
	public byte[] getXodContent() {
		return xodContent;
	}
	/**See {@link #xodContent}. */
	public void setXodContent(byte[] xodContent) {
		this.xodContent = xodContent;
	}

}