/**
 * File: EncryptUtils.java
 */
package com.lightspeedeps.util.app;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

/**
 * Utility methods for encrypting and decrypting text. LS-3204
 */
public class EncryptUtils {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(EncryptUtils.class);

	/** The standard TTCO encryptor instance. */
	private final static StandardPBEStringEncryptor encryptor;

	static {
		encryptor = new StandardPBEStringEncryptor();
		encryptor.setProviderName("BC");
		encryptor.setProvider(new BouncyCastleProvider());
		encryptor.setAlgorithm("PBEWITHSHAAND256BITAES-CBC-BC"); // "PBEWITHSHAAND256BITAES-CBC-BC"
		encryptor.setPassword("tlr9ls!oee57(2,bgmiwe.pd4*63uv^8xozcq?p/m5\n\t\t\t"); //  NOTE!! Password includes Newline and 3 tabs from the XML file!!
		encryptor.setKeyObtentionIterations(999);
	}

	public static String decrypt(String cipher) {
		String clear = null;
		if (cipher != null) {
			clear = encryptor.decrypt(cipher);
		}
//		LOG.debug("input=" + cipher + ", clear=" + clear);
		return clear;
	}

	public static String encrypt(String clear) {
		String cipher = null;
		if (clear != null) {
			cipher = encryptor.encrypt(clear); // encrypt using standard TTCO encryptor/decryptor
		}
//		LOG.debug("input=" + clear + ", cipher=" + cipher);
		return cipher;
	}

}
