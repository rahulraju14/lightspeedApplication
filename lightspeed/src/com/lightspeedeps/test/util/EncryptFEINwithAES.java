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
import com.lightspeedeps.test.SpringTestCase;
import com.lightspeedeps.util.common.StringUtils;

/**
 * A class to re-encrypt the User.Federal_Tax_Id database fields using AES
 * encryption.  This will allow non-Lightspeed code to decrypt the field using
 * standard (built-in) MySQL support. LS-4572
 * <p>
 * Note that this needs the applicationHibernateContext.xml file to be included
 * in the run-time classpath. That information is used to create the
 * Spring/Hibernate environment. <b>Specify the database to use</b> in that
 * context file.
 */
public class EncryptFEINwithAES extends SpringTestCase {

	private static final Log LOG = LogFactory.getLog(EncryptFEINwithAES.class);

	/** Encryption key used for password, SSN, FEIN, etc. */
	private final static String ENCRYPT_KEY = "concat('tlr9ls!oee57(2,bgmiwe.pd4*63uv^8xozcq', 0x3f, 'p/m5\n\t\t\t')";
	//                        key without hex coding: "tlr9ls!oee57(2,bgmiwe.pd4*63uv^8xozcq?p/m5\n\t\t\t"
	// Note that the "?" in the key must be coded in hex to avoid the ColumnTransformer throwing an error.

	/** The standard TTCO encryptor instance. */
	private static StandardPBEStringEncryptor myEncryptor;

	/**
	 * @param args - not used.
	 */
	public static void main(String[] args) {

		EncryptFEINwithAES ef = new EncryptFEINwithAES();
		try {
			ef.setUp();		// do Spring/hibernate setup
			ef.runEncryption();	// do the decryption/encryption
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

		UserDAO userDAO = UserDAO.getInstance();

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
		roundTrip("abc");
//		tryDecrypt("7M42jY0kH2UUlHdAnK1YPD4nXzx+EqtCGO5nS3wnoBQ=");

		decryptField(userDAO, "user", "Federal_Tax_Id");

		LOG_debug("Completed.");
		System.out.println("Completed.");

	}

	/**
	 * DEcrypt the given field of User table and re-encrypt it using MySql's AES.
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

		String fieldNameAes = fieldName + "_Aes";
		// create, for example: "select id, fedid_Number from user where fedid_Number is not null";
		String qry = "select id, " + fieldName + ", " +
				" cast(AES_DECRYPT(" + fieldNameAes + ", " + ENCRYPT_KEY + ") AS char)" +
				" from " + tableName + " where " + fieldName + " is not null"; // + " limit 20";

		String updateAes = "update " + tableName +
				// " set social_Security_clear = :field, " +
				" set " + fieldNameAes + " = AES_ENCRYPT( :field, " + ENCRYPT_KEY + ") where id = :id";

		String nullify = "update " + tableName +
				" set " + fieldNameAes + " = :field where id = :id";

		@SuppressWarnings("unchecked")
		List<Object[]> list = (List<Object[]>)dao.sqlQuery(qry, true);

		LOG_debug("The " + tableName + " table has " + list.size() + " non-null entries in the " + fieldName + " column to convert");
		System.out.println("The " + tableName + " table has " + list.size() + " non-null entries in the " + fieldName + " column to convert");

		int matched = 0;
		int updated = 0;

		for (Object[] objArray : list) {
			Integer id = (Integer)objArray[0];
			String decryptedValue = (String)objArray[1];
			String aesValue = (String)objArray[2];
			decryptedValue = myEncryptor.decrypt(decryptedValue); // decrypt using standard TTCO encryptor/decryptor

			if (decryptedValue == null || decryptedValue.length() == 0) {
				if (aesValue != null) {
					dao.sqlUpdate(nullify, id, null);
				}
				continue;
			}

			if (aesValue != null && decryptedValue.equals(aesValue)) {
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
			LOG_debug("Updated: id=" + id + " FEIN=" + decryptedValue);
		}

		LOG_debug(tableName + " table completed.");
		LOG_debug(matched + " items previously encrypted and unchanged; " + updated + " items updated.");

		String hqry = "select id, federalTaxId from User where federalTaxId is not null and id > ? order by id desc";
		@SuppressWarnings("unchecked")
		List<Object[]> hlist = dao.findLimited(hqry, new Integer(0), 20);
		for (Object[] objArray : hlist) {
			Integer id = (Integer)objArray[0];
			String decValue1 = (String)objArray[1];
			LOG_debug("id=" + id + " ssn=" + decValue1);
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

	private void LOG_debug(String s) {
		System.out.println(s);
	}

}
