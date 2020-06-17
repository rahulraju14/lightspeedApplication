//	File Name:	UtilitiesTest.java
package com.lightspeedeps.test.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

import junit.framework.TestCase;

public class EncryptTest extends TestCase {


	static StandardPBEStringEncryptor myEncryptor;
	/**
	 * Tests for UtilHelper.stringFromList().
	 * @throws Exception
	 */
	public void testOne() throws Exception {

/*  Test based on this (current) encryption setting in applicatinoContext:

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
		myEncryptor.setPassword("**password**\n\t\t\t"); //  NOTE!! Password includes Newline and 3 tabs from the XML file!!
		myEncryptor.setKeyObtentionIterations(999);
		myEncryptor.setProviderName("BC");

		roundTrip("x");

		tryDecrypt("7M42jY0kH2UUlHdAnK1YPD4nXzx+EqtCGO5nS3wnoBQ=");
		tryDecrypt("670xMChjmcXQKro0S3K6BnhJdyRcPbOl9DzfBIbcpEk=");

		System.out.println("DONE");

	}

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

	private void roundTrip(String str) {
		System.out.println("clear text is: `" + str + "`" );
		String enc = myEncryptor.encrypt(str);
		assertEquals(str, tryDecrypt(enc));
	}

}
