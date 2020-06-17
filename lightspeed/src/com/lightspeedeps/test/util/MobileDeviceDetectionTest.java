//	File Name:	MobileDeviceDetectionTest.java
package com.lightspeedeps.test.util;

import com.lightspeedeps.web.listener.MobileDeviceListener;

import junit.framework.TestCase;

/**
 * Test our phone- and tablet-detection routines by passing in
 * various user-agent strings.
 */
public class MobileDeviceDetectionTest extends TestCase {

	/**
	 * Test phone- and tablet-detection routines
	 */
	public void testMobile() throws Exception {

		/* iPhone */
		testPhone( "Mozilla/5.0 (iPhone; CPU iPhone OS 7_0_4 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) CriOS/32.0.1700.21 Mobile/11B554a Safari/9537.53" );
		/* Samsung I9000 phone */
		testPhone( "Mozilla/5.0 (Linux; U; Android 4.3.1; tr-tr; GT-I9000 Build/JLS36I) AppleWebKit/534.30 (KHTML like Gecko) Version/4.0 Mobile Safari/534.30 CyanogenMod/10.2/galaxysmtd" );
		/* Windows phone 7.5 */
		testPhone( "Mozilla/5.0 (compatible; MSIE 9.0; Windows Phone OS 7.5; Trident/5.0; IEMobile/9.0; SAMSUNG; SGH-i917)" );
		/* Lumia 1020 phone */
		testPhone( "Mozilla/5.0 (compatible; MSIE 10.0; Windows Phone 8.0; Trident/6.0; IEMobile/10.0; ARM; Touch; NOKIA; Lumia 1020)" );
		/* Opera Mini on Android phone - Samsung Galaxy S */
		testPhone( "Opera/9.80 (Android; Opera Mini/6.5.27452/26.1283; U; en) Presto/2.8.119 Version/10.54" );
		/* Opera Mobile on samsung galaxy S */
		testPhone( "Opera/9.80 (Android 2.3.5; Linux; Opera Mobi/ADR-1111021320; U; en) Presto/2.9.201 Version/11.50" );
		/*  */
//		testPhone( "" );


		/* Samsung Galaxy Tab Pro 10.1, SM-T520 */
		testTablet( "Mozilla/5.0 (Linux; Android 4.4.2; en-us; SAMSUNG SM-T520 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/1.5 Chrome/28.0.1500.94 Safari/537.36" );
		/* Samsung Galaxy Tab 7 (verizon?) */
		testTablet( "Mozilla/5.0 (Linux; U; Android 2.2; en-us; SCH-I800 Build/FROYO) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1" );
		/* Samsung Galaxy Tab 7 */
		testTablet( "Mozilla/5.0 (Linux; U; Android 2.2; es-es; GT-P1000 Build/FROYO) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1" );
		/* Opera Mobile on Asus EEE Pad tablet */
		testTablet( "Opera/9.80 (Android 3.2.1; Linux; Opera Tablet/ADR-1111101157; U; en) Presto/2.9.201 Version/11.50" );
		/*  */
//		testTablet( "" );

	}

	private void testPhone(String agent) {
		boolean result = MobileDeviceListener.isPhone(agent);
		assertEquals("Should have been recognized as phone, but wasn't:\n" + agent, true, result);
		result = MobileDeviceListener.isTablet(agent);
		assertEquals("Should NOT have been recognized as tablet, but was:\n" + agent, false, result);
	}

	private void testTablet(String agent) {
		boolean result = MobileDeviceListener.isTablet(agent);
		assertEquals("Should have been recognized as tablet, but wasn't:\n" + agent, true, result);

		if (! result) {
			// Only test phone detection if it was NOT recognized as a tablet.
			// We assume some tablet UAs will also look like phone UAs (e.g, contain "mobile").
			result = MobileDeviceListener.isPhone(agent);
			assertEquals("Should NOT have been recognized as phone, but was:\n" + agent, false, result);
		}
	}

}
