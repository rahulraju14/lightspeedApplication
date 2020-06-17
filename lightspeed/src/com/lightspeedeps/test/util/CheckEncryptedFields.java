//	File Name:	CheckEncryptedFields.java
package com.lightspeedeps.test.util;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.BaseDAO;
import com.lightspeedeps.dao.StartFormDAO;
import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.dao.WeeklyTimecardDAO;
import com.lightspeedeps.test.SpringTestCase;

/**
 * A class to test all occurrences of a field for length. This method is
 * necessary for encrypted fields. (For non-encrypted fields, you could just run
 * a SQL script checking the field length.)
 * <p>
 * Note that this needs the applicationHibernateContext.xml file to be included
 * in the run-time classpath. That information is used to create the
 * Spring/Hibernate environment. <b>Specify the database to use</b> in that
 * context file.
 */
public class CheckEncryptedFields extends SpringTestCase {

	private static final Log LOG = LogFactory.getLog(CheckEncryptedFields.class);

	/**
	 * @param args - not used.
	 */
	public static void main(String[] args) {
		CheckEncryptedFields ef = new CheckEncryptedFields();
		try {
			ef.setUp();		// do Spring/hibernate setup
			ef.runCheckFields();	// do the checks
			ef.tearDown();	// clean up Spring environment
		}
		catch (Exception e) {
			LOG.debug("Exception: ", e);
			System.out.println("Exception logged");
		}
	}

	/**
	 * Check fields using Spring/Hibernate environment already established.
	 *
	 * @throws Exception
	 */
	public void runCheckFields() throws Exception {

		/* check Start_Form.socialSecurity */
		testField(StartFormDAO.getInstance(), "StartForm", "socialSecurity");

		/* check User.socialSecurity */
		testField(UserDAO.getInstance(), "User", "socialSecurity");

		/* check Weekly_Time_card.socialSecurity */
		testField(WeeklyTimecardDAO.getInstance(), "WeeklyTimecard", "socialSecurity");

		LOG.debug("All tables completed.");
//		System.out.println("All tables completed.");

		System.out.println("DONE");

	}

	/**
	 * Test all occurrences of a field for length. This method is necessary for
	 * encrypted fields. (For non-encrypted fields, you could just run a SQL
	 * script checking the field length.) The unique identifier column of the
	 * table is assumed to be named 'id' (for debugging output).
	 *
	 * @param dao The appropriate DAO instance for the table containing the
	 *            field being tested.
	 * @param tableName The table name.
	 * @param fieldName The Java field name; note that the case must be correct!
	 */
	private void testField(BaseDAO dao, String tableName, String fieldName) {
		// create, for example: "select id, fedid_Number from form_wtpa where fedid_Number is not null";
		String qry = "select id, " + fieldName + " from " + tableName + " where " + fieldName + " is not null";

		@SuppressWarnings("unchecked")
		List<Object[]> list = dao.find(qry);

		LOG.debug("The " + tableName + " table has " + list.size() + " non-null entries in the " + fieldName + " column ");
		//System.out.println("The " + tableName + " table has " + list.size() + " non-null entries in the " + fieldName + " column ");

		for (Object[] objArray : list) {
			Integer id = (Integer)objArray[0];
			String field = (String)objArray[1];
			if (field != null && field.length() != 0 && field.length() != 9) {
				String msg = "error, SSN length incorrect, =" + field.length() + ", id='" + id + ", value='" + field + "'";
				LOG.debug(msg);
				//System.out.println(msg);
			}
		}

		LOG.debug(tableName + " table completed.");
		//System.out.println(tableName + " table completed.");
	}

}
