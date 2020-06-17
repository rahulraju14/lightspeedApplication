//	File Name:	UtilitiesTest.java
package com.lightspeedeps.test.util;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

import com.lightspeedeps.dao.BaseDAO;
import com.lightspeedeps.dao.FormG4DAO;
import com.lightspeedeps.dao.FormWtpaDAO;
import com.lightspeedeps.dao.PayrollPreferenceDAO;
import com.lightspeedeps.dao.StartFormDAO;
import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.dao.WeeklyTimecardDAO;
import com.lightspeedeps.test.SpringTestCase;
import com.lightspeedeps.util.common.StringUtils;

/**
 * A class to encrypt the database fields containing Federal tax id numbers that
 * were not previously encrypted. LS-2080.
 * <p>
 * Note that this needs the applicationHibernateContext.xml file to be included
 * in the run-time classpath. That information is used to create the
 * Spring/Hibernate environment. <b>Specify the database to use</b> in that
 * context file.
 */
public class EncryptFields extends SpringTestCase {

	private static final Log LOG = LogFactory.getLog(EncryptFields.class);

	/** The encryptor instance. */
	static StandardPBEStringEncryptor myEncryptor;

	/**
	 * @param args - not used.
	 */
	public static void main(String[] args) {
		EncryptFields ef = new EncryptFields();
		try {
			ef.setUp();		// do Spring/hibernate setup
			ef.runEncryption();	// do the encryption
			ef.tearDown();	// clean up Spring environment
		}
		catch (Exception e) {
			LOG.debug("Exception: ", e);
			System.out.println("Exception logged");
		}
	}

	/**
	 * Run encryption of fields using Spring/Hibernate environment already
	 * established.
	 *
	 * @throws Exception
	 */
	public void runEncryption() throws Exception {

/*  The 'myEncryptor' settings below are based on these (current) encryption settings in applicationContext:

	<bean id="strongEncryptor" class="org.jasypt.encryption.pbe.StandardPBEStringEncryptor">
		<!-- Don't specify algorithm for offline version -->
		<property name="algorithm">
			<value>PBEWITHSHAAND256BITAES-CBC-BC</value>
		</property>
		<property name="password">
			<value>**password**
			</value>
		</property>
		<property name="keyObtentionIterations">
			<value>999</value><!-- default is 1,000 -->
		</property>
		<property name="providerName">
			<value>BC</value>
		</property>
	</bean>
 */
		myEncryptor = new StandardPBEStringEncryptor();
		myEncryptor.setProvider(new BouncyCastleProvider());
		myEncryptor.setAlgorithm("PBEWITHSHAAND256BITAES-CBC-BC");
		myEncryptor.setPassword("tlr9ls!oee57(2,bgmiwe.pd4*63uv^8xozcq?p/m5\n\t\t\t"); //  NOTE!! Password includes Newline and 3 tabs from the XML file!!
		myEncryptor.setKeyObtentionIterations(999);
		myEncryptor.setProviderName("BC");

		// These may be used for debugging/testing the encryption setup:
//		roundTrip("abc");
//		tryDecrypt("7M42jY0kH2UUlHdAnK1YPD4nXzx+EqtCGO5nS3wnoBQ=");
//		tryDecrypt("670xMChjmcXQKro0S3K6BnhJdyRcPbOl9DzfBIbcpEk=");

		/* convert Form_G4.employer_FEIN */
		encryptField(FormG4DAO.getInstance(), "form_g4", "employer_FEIN");

		/* convert Form_WTPA.fedid_Number */
		encryptField(FormWtpaDAO.getInstance(), "form_wtpa", "fedid_Number");

		/* convert Payroll_Preference.fedid_Number */
		encryptField(PayrollPreferenceDAO.getInstance(), "Payroll_Preference", "fedid_Number");

		/* convert Start_Form.federal_Tax_Id */
		encryptField(StartFormDAO.getInstance(), "Start_Form", "federal_Tax_Id");

		/* convert User.federal_Tax_Id */
		encryptField(UserDAO.getInstance(), "User", "federal_Tax_Id");

		/* convert Weekly_Time_card.fed_Corp_Id */
		encryptField(WeeklyTimecardDAO.getInstance(), "Weekly_Time_card", "fed_Corp_Id");

		LOG.debug("All tables completed.");
		System.out.println("All tables completed.");

		System.out.println("DONE");

	}

	/**
	 * Encrypt all the values in a federal tax id number field of one table.
	 * <p>
	 * <b>The clear-text (source) value of the field will be "cleaned" so that
	 * only the digits are encrypted. If this method is used for non-tax-id
	 * values, the bit of code that "cleans" the source must be removed!!</b>
	 * <p>
	 * The encrypted values will be stored in the column named < fieldName
	 * >_enc. A decrypted copy of the field will be stored in the column <
	 * fieldName >_check. These target columns must already exist in the
	 * database. The unique identifier column of the table is assumed to be
	 * named 'id'.
	 *
	 * @param dao The appropriate DAO instance for the table being updated.
	 * @param tableName The native SQL table name.
	 * @param fieldName The native SQL field name.
	 */
	private void encryptField(BaseDAO dao, String tableName, String fieldName) {
		// create, for example: "select id, fedid_Number from form_wtpa where fedid_Number is not null";
		String qry = "select id, " + fieldName + " from " + tableName + " where " + fieldName + " is not null";

		// create, for example: "update form_wtpa set fedid_Number_enc = :field1, fedid_Number_check = :field2 where id = :id";
		String updateEnc = "update " + tableName + " set " + fieldName + "_enc = :field1, "  + fieldName + "_check = :field2 where id = :id";

		@SuppressWarnings("unchecked")
		List<Object[]> list = (List<Object[]>)dao.sqlQuery(qry, true);

		LOG.debug("The " + tableName + " table has " + list.size() + " non-null entries in the " + fieldName + " column to convert");
		System.out.println("The " + tableName + " table has " + list.size() + " non-null entries in the " + fieldName + " column to convert");

		for (Object[] objArray : list) {
			Integer id = (Integer)objArray[0];
			String fein = (String)objArray[1];
			String cleanFein = StringUtils.cleanTaxId(fein); // remove punctuation
			if (cleanFein != null && cleanFein.length() > 0) { // valid FEIN
				fein = cleanFein; // encrypt the clean one (no punctuation)
			} // (otherwise, we'll encrypt the original (invalid) string; this should only happen on "old" forms where it wasn't validated)
			String encryptedFedId = myEncryptor.encrypt(fein);
			String decryptedFedId = myEncryptor.decrypt(encryptedFedId);
			dao.sqlUpdate(updateEnc, id, encryptedFedId, decryptedFedId);
		}

		LOG.debug(tableName + " table completed.");
		System.out.println(tableName + " table completed.");
	}

	/**
	 * For encryption testing. Attempts to decrypt the supplied text.
	 *
	 * @param str A string that has already been encrypted using the TTCO method
	 *            and key.
	 * @return The decrypted (clear) text.
	 */
	private String tryDecrypt(String str) {
		String dec = "";
		System.out.println("encrypted text is:`" + str + "`" );
		try {
			dec = myEncryptor.decrypt(str);
			System.out.println("result of decrypt: `" + dec + "`" );
		}
		catch (Exception e) {
			System.out.println("decrypt failed: " + e.getMessage());
			System.out.println(e.toString());
		}
		return dec;
	}

	/**
	 * For encryption testing. Encrypts the supplied text, then decrypts it,
	 * then compares the original text to the decrypted text.
	 */
	@SuppressWarnings("unused")
	private void roundTrip(String str) {
		System.out.println("clear text is: `" + str + "`" );
		String enc = myEncryptor.encrypt(str);
		assertEquals(str, tryDecrypt(enc));
	}

}
