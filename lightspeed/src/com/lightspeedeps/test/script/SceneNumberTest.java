//	File Name:	SceneNumberTest.java
package com.lightspeedeps.test.script;

import com.lightspeedeps.dao.SceneDAO;
import com.lightspeedeps.model.*;

import junit.framework.TestCase;

/**
 * A JUnit class to test our scene number generation.
 */
public class SceneNumberTest extends TestCase {

	private final SceneDAO sceneDAO = new SceneDAO();

	public void testNextSceneNumber() throws Exception {

		// tests for SceneDAO.createNextSceneNumber()

		// test for inserting at beginning -- prior scene null
		sceneTestOne( null, null, "1" );	// empty script
		sceneTestOne( null, "1", "A1" );
		sceneTestOne( null, "A1", "AA1" );
		sceneTestOne( null, "G1", "A1" );
		sceneTestOne( null, "ABC1", "ABA1" );
		sceneTestOne( null, "ABXA1", "ABXAA1" );
		sceneTestOne( null, "2", "1" );
		sceneTestOne( null, "58A", "1" );

		// test next scene null (end of script)
		sceneTestOne( "6", null, "7" );
		sceneTestOne( "9F", null, "10" );
		sceneTestOne( "84", null, "85" );

		// test no alpha end on either
		sceneTestOne( "6", "7", "A6" );
		sceneTestOne( "6", "19", "A6" );

		// test alpha on 1st, none on second (higher number)
		sceneTestOne( "C6", "7", "D6" );
		sceneTestOne( "C64", "65", "D64" );
		sceneTestOne( "Z6", "7", "ZA6" );
		sceneTestOne( "ZC6", "7", "ZD6" );
		sceneTestOne( "ZKP6", "7", "ZL6" );

		// test alpha on both, second is higher number
		sceneTestOne( "C6", "A7", "D6" );
		sceneTestOne( "C64", "C65", "D64" );
		sceneTestOne( "Z6", "A7", "ZA6" );

		// test alpha on both, same number
		sceneTestOne( "C6", "D6", "CA6" );
		sceneTestOne( "C6", "F6", "CA6" ); // "6D" would be possible, but we don't do that
		sceneTestOne( "CK6", "F6", "CL6" );
		sceneTestOne( "CK6", "CLAA6", "CLAAA6" );
		sceneTestOne( "CK6", "CLAB6", "CLAA6" );
		sceneTestOne( "CK6", "CLRG6", "CLRA6" );
		sceneTestOne( "Z6", "ZC6", "ZA6" );
		sceneTestOne( "R6", "RA6", "RAA6" );
		sceneTestOne( "R6", "RB6", "RA6" );
		sceneTestOne( "Z6", "ZA6", "ZAA6" );
		sceneTestOne( "6", "AA6", "AAA6" );

		// test some longer strings
		sceneTestOne( "ZZC6", "ZZD6", "ZZCA6" );
		sceneTestOne( "ZZZZC6", "ZZZZD6", "ZZZZCA6" );
		sceneTestOne( "ZZZZCK6", "ZZZZD6", "ZZZZCL6" );
		sceneTestOne( "CRT6", "CRX6", "CRTA6" );

		// test non-numeric scene number
		sceneTestOne( "ABC", null, "A" );
	}

	private void sceneTestOne(String s1, String s2, String result) {
		Scene scene1 = null;
		Scene scene2 = null;
		if (s1 != null) {
			scene1 = new Scene();
			scene1.setNumber(s1);
		}
		if (s2 != null) {
			scene2 = new Scene();
			scene2.setNumber(s2);
		}
		assertEquals("inputs: "+s1+", "+s2+"; ",
				result, sceneDAO.createNextSceneNumber(null,scene1,scene2));

	}
}
