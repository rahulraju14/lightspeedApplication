package com.lightspeedeps.model;

import javax.persistence.Id;

import org.springframework.data.mongodb.core.mapping.Document;

/**
 * The entity representing the persisted contents of a Document. This is
 * designed so that a document's content (body) can be stored in a separate
 * (possibly non-SQL) database.
 *
 * @see com.lightspeedeps.dao.AttachmentContentDAO
 * @see com.lightspeedeps.model.Attachment
 */
@Document(collection=AttachmentContent.COLLECTION_NAME)
public class AttachmentContent {

	/**
	 * The name of the Collection in which the data will be stored. This MUST
	 * match the value of the 'collection' keyword in our @Document tag. It is
	 * used by the {@link com.lightspeedeps.dao.ContentDAO} class.
	 */
	public static final String COLLECTION_NAME = "attachmentcontent";

	/** String literal used to initially store the XOD converted content into mongo. */
	public static final String EMPTY_XOD = "XOD";

	@Id
	/** Unique id of the uploaded document */
	private Integer attachment_id;

	/** Uploaded documents content stored into a byte format */
	private byte[] content;

	/** Uploaded PDF's XOD format content */
	private byte [] xodContent;

	/**See {@link #attachment_id}. */
	public Integer getAttachmentId() {
		return attachment_id;
	}
	/**See {@link #attachment_id}. */
	public void setAttachmentId(Integer attachment_id) {
		this.attachment_id = attachment_id;
	}

	/**See {@link #content}. */
	public byte[] getContent() {
		return content;
	}
	/**See {@link #content}. */
	public void setContent(byte[] content) {
		this.content = content;
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