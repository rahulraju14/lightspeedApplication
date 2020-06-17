package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.WriteResultChecking;

import com.lightspeedeps.object.LoggedException;
import com.lightspeedeps.util.app.EventUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;

/**
 * A class for managing pdf document's xfdf content stored in the MongoDB database.
 * @see com.lightspeedeps.model.XfdfContent
 * @see com.lightspeedeps.model.ContactDocument
 */
public abstract class MongoDAO {

	private static final Log log = LogFactory.getLog(MongoDAO.class);

	/**
	 * This template configures the mongoDB environment with the provided
	 * details of host, port and database name. Its value is set through an
	 * entry in applicationContextMongo.xml.
	 */
	private MongoTemplate mongoTemplate;

	/**
	 * Insert an arbitrary Object into the database.
	 * @param mb
	 */
	public void insert(Object mb) {
		mongoTemplate.insert(mb);
	}

	public abstract String getCollectionName();

	public DBCollection getCollection() {
		return mongoTemplate.getCollection(getCollectionName());
	}

	public Object findById(Class<?> clazz, String keyName, Integer id) {
		Object md = null;
		DBCursor cursor = null;
		try {
			BasicDBObject whereQuery = new BasicDBObject();
			whereQuery.put(keyName, id);
			cursor = getCollection().find(whereQuery);
			//log.debug("db cursor :: " + cursor.toString());
			if (cursor.hasNext()) {
				md = getMongoTemplate().getConverter().read(clazz, cursor.next());
				log.debug(clazz.getName() + " instance found, id=" + id);
			}
			else {
				log.debug("NO instance of " + clazz.getName() + " found for id=" + id);
			}
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw new LoggedException(re);
		}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return md;
	}

	/**See {@link #mongoTemplate}. */
	public MongoTemplate getMongoTemplate() {
		return mongoTemplate;
	}
	/**See {@link #mongoTemplate}. */
	public void setMongoTemplate(MongoTemplate pmongoTemplate) {
		this.mongoTemplate = pmongoTemplate;
		mongoTemplate.setWriteResultChecking(WriteResultChecking.EXCEPTION);
	}
}
