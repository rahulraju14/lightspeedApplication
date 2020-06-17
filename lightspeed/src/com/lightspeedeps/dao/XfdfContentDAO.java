package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.XfdfContent;
import com.lightspeedeps.object.LoggedException;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.mongodb.BasicDBObject;
import com.mongodb.WriteResult;

/**
 * A class for managing pdf document's xfdf content stored in the MongoDB database.
 * @see com.lightspeedeps.model.XfdfContent
 * @see com.lightspeedeps.model.ContactDocument
 */
public class XfdfContentDAO extends MongoDAO {

	private static final Log log = LogFactory.getLog(XfdfContentDAO.class);

	private static final String DOC_ID_KEY_NAME = "contact_doc_id";
	private static final String CONTENT_KEY_NAME = "content";

	public static XfdfContentDAO getInstance() {
		return (XfdfContentDAO)ServiceFinder.findBean("XfdfContentDAO");
	}

	@Override
	public String getCollectionName() {
		return XfdfContent.COLLECTION_NAME;
	}

	/**
	 * @param contactDocId
	 * @return It returns stored document entry with content from mongodb.
	 */
	public XfdfContent findByContactDocId(Integer contactDocId) {
		return (XfdfContent)findById(XfdfContent.class, DOC_ID_KEY_NAME, contactDocId);
	}

	/**
	 * Removes the entry from mongodb for the passed docId.
	 * @param docId
	 */
	public void removeByDocId(Integer contactDocId) {
		try {
			BasicDBObject whereQuery = new BasicDBObject();
			whereQuery.put(DOC_ID_KEY_NAME, contactDocId);
			WriteResult result = getCollection().remove(whereQuery);
			if (result.getN() == 1) {
				log.info("delete successful for Content, id=" + contactDocId);
			}
			else {
				log.warn("no Content found for doc id=" + contactDocId);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
	}

	/** Updates the xfdf content with the updated data from the web viewer
	 * @param data updated xfdf content
	 * @param contactDocId document id that needs to be updated
	 */
	public void updateXFDFContent(String data, Integer contactDocId) {
		BasicDBObject newXfdf = new BasicDBObject();
		newXfdf.append("$set", new BasicDBObject().append(CONTENT_KEY_NAME, data));

		BasicDBObject searchXfdf = new BasicDBObject().append(DOC_ID_KEY_NAME, contactDocId);
		getCollection().update(searchXfdf, newXfdf);
	}

}
