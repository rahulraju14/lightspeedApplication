package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Content;
import com.lightspeedeps.model.Document;
import com.lightspeedeps.object.LoggedException;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.mongodb.BasicDBObject;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

/**
 * A class for managing document content stored in the MongoDB database.
 * @see com.lightspeedeps.model.Content
 * @see com.lightspeedeps.model.Document
 */
public class ContentDAO extends MongoDAO {

	private static final Log log = LogFactory.getLog(ContentDAO.class);

	private static final String DOC_ID_KEY_NAME = "doc_id";
	private static final String XFDF_KEY_NAME = "xfdfData";
	private static final String XOD_KEY_NAME = "xodContent";

	public static ContentDAO getInstance() {
		return (ContentDAO)ServiceFinder.findBean("ContentDAO");
	}

	@Override
	public String getCollectionName() {
		return Content.COLLECTION_NAME;
	}

	/**
	 * @param docId
	 * @return It returns stored document entry with content from mongodb.
	 */
	public Content findByDocId(Integer docId, Integer originalDocId) {
		log.debug("");
		Content content = (Content)findById(Content.class, DOC_ID_KEY_NAME, docId);
		if (content == null && originalDocId != null) {
			log.debug("FETCHING CONTENT OF ORIGINAL DOCUMENT");
			content = (Content)findById(Content.class, DOC_ID_KEY_NAME, originalDocId);
		}
		return content;
	}

	/**
	 * Removes the entry from mongodb for the passed docId.
	 * @param docId
	 */
	public void removeByDocId(Integer docId) {
		try {
			BasicDBObject whereQuery = new BasicDBObject();
			whereQuery.put(DOC_ID_KEY_NAME, docId);
			WriteResult result = getCollection().remove(whereQuery, WriteConcern.ACKNOWLEDGED);
			if (result.getN() == 1) {
				log.info("delete successful for Content, id=" + docId);
			}
			else {
				log.warn("no Content found for doc id=" + docId);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
	}

	/** Updates the content with the updated xfdf data from the web viewer
	 * @param data updated content's xfdf data
	 * @param docId document id that needs to be updated
	 */
	public void updateContent(String data, Integer docId) {
		Content content = (Content)findById(Content.class, DOC_ID_KEY_NAME, docId);
		if (content == null) {
			Document doc = DocumentDAO.getInstance().findById(docId);
			if (doc != null) {
				log.debug("FETCHING CONTENT OF ORIGINAL DOCUMENT");
				docId = doc.getOriginalDocumentId();
			}
		}
		BasicDBObject newXfdf = new BasicDBObject();
		newXfdf.append("$set", new BasicDBObject().append(XFDF_KEY_NAME, data));
		BasicDBObject searchXfdf = new BasicDBObject().append(DOC_ID_KEY_NAME, docId);
		getCollection().update(searchXfdf, newXfdf);
	}

	/** Updates the content with the converted xod data from uploaded pdf document
	 * @param xodContent updated content's xod data
	 * @param docId document id that needs to be updated
	 */
	public void updateXodContent(Integer docId, byte[] xodContent) {
		BasicDBObject newXfdf = new BasicDBObject();
		newXfdf.append("$set", new BasicDBObject().append(XOD_KEY_NAME, xodContent));

		BasicDBObject searchXfdf = new BasicDBObject().append(DOC_ID_KEY_NAME, docId);
		getCollection().update(searchXfdf, newXfdf);
	}

}
