//	File Name:	EncryptSSNwithAES.java
package com.lightspeedeps.test.util;

import java.security.Provider;
import java.security.Security;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

import com.lightspeedeps.dao.BaseDAO;
import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.service.FormService;
import com.lightspeedeps.test.SpringTestCase;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.common.StringUtils;

/**
 * A class to re-encrypt the User.social_security (SSN) database fields using AES
 * encryption.  This will allow non-Lightspeed code to decrypt the field using
 * standard (built-in) MySQL support.
 * <p>
 * Note that this needs the applicationHibernateContext.xml file to be included
 * in the run-time classpath. That information is used to create the
 * Spring/Hibernate environment. <b>Specify the database to use</b> in that
 * context file.
 */
public class EncryptSSNwithAES extends SpringTestCase {

	private static final Log LOG = LogFactory.getLog(EncryptSSNwithAES.class);

	/** The standard TTCO encryptor instance. */
	private static StandardPBEStringEncryptor myEncryptor;

	/**
	 * @param args - not used.
	 */
//	public static void main(String[] args) {
//
//		EncryptSSNwithAES ef = new EncryptSSNwithAES();
//		try {
//			ef.setUp();		// do Spring/hibernate setup
//			ef.runEncryption();	// do the decryption/encryption
//			ef.tearDown();	// clean up Spring environment
//		}
//		catch (Exception e) {
//			LOG.debug("Exception: ", e);
//			System.out.println("Exception logged");
//		}
//	}

	/**
	 * LS-4354
	 * @param args - not used. This main enty point is used when using a 
	 * micro-service to encrypt fields
	 * 
	 * @since version 20.10.0
	 */
	public static void main(String[] args) {
		EncryptSSNwithAES ef = new EncryptSSNwithAES();
		try {
			ef.decryptApiField();
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

//		UserDAO userDAO = UserDAO.getInstance();

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

//		cipher = Cipher.getInstance("AES_128/ECB/NoPadding", "SunJCE");

		myEncryptor = new StandardPBEStringEncryptor();
		myEncryptor.setProviderName("BC");
		myEncryptor.setProvider(new BouncyCastleProvider());
		myEncryptor.setAlgorithm("PBEWITHSHAAND256BITAES-CBC-BC"); // "PBEWITHSHAAND256BITAES-CBC-BC"
		myEncryptor.setPassword("tlr9ls!oee57(2,bgmiwe.pd4*63uv^8xozcq?p/m5\n\t\t\t"); //  NOTE!! Password includes Newline and 3 tabs from the XML file!!
		myEncryptor.setKeyObtentionIterations(999);

		// These may be used for debugging/testing the encryption setup:
//		roundTrip("abc");
//		tryDecrypt("7M42jY0kH2UUlHdAnK1YPD4nXzx+EqtCGO5nS3wnoBQ=");

		decryptField(UserDAO.getInstance(), "user", "social_security");

		LOG.debug("Completed.");
		System.out.println("Completed.");

	}

	/**
	 * LS-4354
	 * 
	 * Call micro-service to encrypt database field holding clear text data into another field
	 * defined as LongBlob.
	 * 
	 * @since version 20.10.0
	 */
	private void decryptApiField() {
		// create, for example: "select id, fedid_Number from user where fedid_Number is not null";
		String apiUrl;
		String tableName = "form_state_w4";
		String decryptedFieldName = "Social_Security";
		String encryptedFieldName = "Social_Security_Enc";
		String apiControllerPath = "formStateW4/encryptfields"; 
		String urlPrefix = Constants.ONBOARDING_URL_PREFIX;
		// Determine whether the micro-service needs to remove "-"
		String isTaxId =  "true";
		
		// Local api url
		apiUrl = "http://localhost:7073";
		// beta api url
//		apiUrl = "http://172.16.10.170:7074";
		// prod api url
//		apiUrl = "http://172.16.10.164:7074";
		
		apiUrl += urlPrefix + apiControllerPath + "/" + tableName + "/" + decryptedFieldName + "/" + encryptedFieldName + "/" + isTaxId;
		
		if(FormService.getInstance().encryptFields(apiUrl)) {
			System.out.print("Fields encryption completed successfully");
		}
		else {
			System.out.print("Fields encryption failed");
		}
	}
	
	/**
	 * DEcrypt the SSN field of User table and re-encrypt it using MySql's AES.
	 * <p>
	 * <b>The clear-text (source) value of the field will be "cleaned" so that
	 * only the digits are encrypted. If this method is used for non-tax-id
	 * values, the bit of code that "cleans" the source must be removed!!</b>
	 * <p>
	 * The unique identifier column of the table is assumed to be
	 * named 'id'.
	 *
	 * @param dao The appropriate DAO instance for the table being updated.
	 * @param tableName The native SQL table name.
	 * @param fieldName The native SQL field name.
	 */
	private void decryptField(BaseDAO dao, String tableName, String fieldName) {
		// create, for example: "select id, fedid_Number from user where fedid_Number is not null";
		String qry = "select id, social_security, cast(AES_DECRYPT(Social_Security_Enc,'tlr9ls!oee57(2,bgmiwe.pd4*63uv^8xozcq?p/m5\n\t\t\t') AS char) from " + tableName + " where " + fieldName + " is not null"; // + " limit " + LIMIT;

		String updateAes = "update " + tableName +
				// " set social_Security_clear = :field, " +
				" set social_Security_Aes = AES_ENCRYPT( :field, 'tlr9ls!oee57(2,bgmiwe.pd4*63uv^8xozcq?p/m5\n\t\t\t') where id = :id";

		@SuppressWarnings("unchecked")
		List<Object[]> list = (List<Object[]>)dao.sqlQuery(qry, true);

		LOG.debug("The " + tableName + " table has " + list.size() + " non-null entries in the " + fieldName + " column to convert");
		System.out.println("The " + tableName + " table has " + list.size() + " non-null entries in the " + fieldName + " column to convert");

		int matched = 0;
		int updated = 0;

		for (Object[] objArray : list) {
			Integer id = (Integer)objArray[0];
			String decryptedValue = (String)objArray[1];
			String aesValue = (String)objArray[2];
			decryptedValue = myEncryptor.decrypt(decryptedValue); // decrypt using standard TTCO encryptor/decryptor

			if (decryptedValue != null && aesValue != null && decryptedValue.equals(aesValue)) {
				matched++;
				continue;
			}

			String cleanValue = StringUtils.cleanTaxId(decryptedValue); // remove punctuation
			if (cleanValue != null && cleanValue.length() > 0) { // valid SSN/FEIN
				decryptedValue = cleanValue; // store the clean one (no punctuation)
			} // (otherwise, we'll store the original (invalid) string; this should only happen on "old" forms where it wasn't validated)
			else if (decryptedValue != null) {
				decryptedValue = decryptedValue.trim();
			}

			// Add AES-encrypted field to database
			dao.sqlUpdate(updateAes, id, decryptedValue);
			updated++;
			LOG.debug("Updated: id=" + id + " ssn=" + decryptedValue);
		}

		LOG.debug(tableName + " table completed.");
		LOG.debug(matched + " items previously encrypted and unchanged; " + updated + " items updated.");

		String hqry = "select id, socialSecurity from User where socialSecurity is not null and id > ? order by id desc";
		@SuppressWarnings("unchecked")
		List<Object[]> hlist = dao.findLimited(hqry, new Integer(0), 20);
		for (Object[] objArray : hlist) {
			Integer id = (Integer)objArray[0];
			String decValue1 = (String)objArray[1];
			LOG.debug("id=" + id + " ssn=" + decValue1);
		}
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
//	@SuppressWarnings("unused")
	private void roundTrip(String str) {
		System.out.println("clear text is: `" + str + "`" );
		String enc = myEncryptor.encrypt(str);
		assertEquals(str, tryDecrypt(enc));
	}

	/**
	 * Output a list of all the security providers in the system, along
	 * with the algorithms they support.
	 */
	@SuppressWarnings("unused")
	private static void listProviders() {
		for (Provider provider : Security.getProviders()) {
			System.out.println("Provider: " + provider.getName());
			for (Provider.Service service : provider.getServices()) {
				System.out.println("  Algorithm: " + service.getAlgorithm());
			}
		}
		Provider p = new BouncyCastleProvider();
		System.out.println("provider BC:");
		for (Provider.Service service : p.getServices()) {
			System.out.println("  Algorithm: " + service.getAlgorithm());
		}
	}

}
