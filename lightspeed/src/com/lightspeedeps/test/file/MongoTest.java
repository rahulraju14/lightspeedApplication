package com.lightspeedeps.test.file;

import junit.framework.TestCase;

import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.lightspeedeps.model.Content;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.WriteResult;

public class MongoTest extends TestCase {

	private static final String COLLECTION = "content";

	@Test
	public void test() {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContextMongo.xml");
		MongoTemplate template = (MongoTemplate)ctx.getBean("mongoTemplate");

		final String testDocument = "This is a text document";
		final int testId = 2;

		Content content = new Content();
		content.setDocId(testId);
		content.setContent(testDocument.getBytes());

		//insert data in mongo
		template.insert(content);

		//fetch data from mongo
		BasicDBObject whereQuery = new BasicDBObject();
		whereQuery.put("doc_id", testId);
		DBCursor cursor = template.getCollection(COLLECTION).find(whereQuery);
		content = template.getConverter().read(Content.class, cursor.next());
		assertEquals("add and read executed", testDocument, new String(content.getContent()));

		//remove document
		WriteResult write = template.getCollection(COLLECTION).remove(whereQuery);

		assertEquals("remove count", 1, write.getN()); // should have affected 1 document

		cursor = template.getCollection(COLLECTION).find(whereQuery);
		if (cursor.hasNext()) {
			content = template.getConverter().read(Content.class, cursor.next());
		}
		else {
			content = null;
		}
		assertNull("remove executed", content);
		ctx.close();
	}

}
