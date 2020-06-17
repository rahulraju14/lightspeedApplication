package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.AttachmentContent;
import com.lightspeedeps.object.LoggedException;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.mongodb.BasicDBObject;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

/**
 * A class for managing Attachment content stored in the MongoDB database.
 * 
 * @see com.lightspeedeps.model.AttachmentContent
 * @see com.lightspeedeps.model.Attachment
 */
public class AttachmentContentDAO extends MongoDAO {

	private static final Log log = LogFactory.getLog(AttachmentContentDAO.class);

	private static final String ATTACHMENT_ID_KEY_NAME = "attachment_id";
	private static final String XOD_KEY_NAME = "xodContent";

	public static AttachmentContentDAO getInstance() {
		return (AttachmentContentDAO) ServiceFinder.findBean("AttachmentContentDAO");
	}

	@Override
	public String getCollectionName() {
		return AttachmentContent.COLLECTION_NAME;
	}

	/**
	 * @param attachmentId
	 * @return It returns stored document entry with content from mongodb.
	 */
	public AttachmentContent findByAttachmentId(Integer attachmentId) {
		log.debug("");
		AttachmentContent content = (AttachmentContent) findById(AttachmentContent.class, ATTACHMENT_ID_KEY_NAME, attachmentId);
		return content;
	}

	/**
	 * Removes the entry from mongodb for the passed attachmentId.
	 * 
	 * @param attachmentId
	 */
	public void removeByAttachmentId(Integer attachmentId) {
		try {
			BasicDBObject whereQuery = new BasicDBObject();
			whereQuery.put(ATTACHMENT_ID_KEY_NAME, attachmentId);
			WriteResult result = getCollection().remove(whereQuery, WriteConcern.ACKNOWLEDGED);
			if (result.getN() == 1) {
				log.info("delete successful for Content, id=" + attachmentId);
			} else {
				log.warn("no Content found for doc id=" + attachmentId);
			}
		} catch (Exception e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
	}

	/**
	 * Updates the content with the converted xod data from uploaded pdf
	 * document
	 * 
	 * @param xodContent
	 *            updated content's xod data
	 * @param attachmentId
	 *            document id that needs to be updated
	 */
	public void updateXodContent(Integer attachmentId, byte[] xodContent) {
		BasicDBObject newXfdf = new BasicDBObject();
		newXfdf.append("$set", new BasicDBObject().append(XOD_KEY_NAME, xodContent));

		BasicDBObject searchXfdf = new BasicDBObject().append(ATTACHMENT_ID_KEY_NAME, attachmentId);
		getCollection().update(searchXfdf, newXfdf);
	}

}
