//	File Name:	ConvertPageLengthTest.java
package com.lightspeedeps.test.script;

import com.lightspeedeps.model.Scene;

import junit.framework.TestCase;

/**
 * Test the utility function that converts a textual input of the length of a
 * portion of a script (typically a scene), including 1/8th's, into the
 * equivalent integer number of 1/8th's.
 *
 * @see com.lightspeedeps.model.Scene#convertPageLength(String)
 */
public class ConvertPageLengthTest extends TestCase {

	public void testConvertPageLength() throws Exception {

		// tests for Scene.convertPageLength()

		sceneTestOne( null, 0 );
		sceneTestOne( "", 0 );
		sceneTestOne( " ", 0 );
		sceneTestOne( "1", 8 );
		sceneTestOne( "2", 16 );
		sceneTestOne( " 2", 16 );
		sceneTestOne( "2 ", 16 );
		sceneTestOne( "23", 184 );

		sceneTestOne( "1/8", 1 );
		sceneTestOne( "2/8", 2 );
		sceneTestOne( "3/8", 3 );
		sceneTestOne( "4/8", 4 );
		sceneTestOne( "5/8", 5 );
		sceneTestOne( "6/8", 6 );
		sceneTestOne( "7/8", 7 );
		sceneTestOne( "1 1/8", 9 );
		sceneTestOne( "2 5/8", 21 );

		sceneTestOne( "2 5/8 ", 21 );
		sceneTestOne( " 2 5/8", 21 );
		sceneTestOne( " 2 5/8 ", 21 );
		sceneTestOne( "90 1/8 ", 721 );
	}

	private void sceneTestOne(String str, int result) {

		assertEquals("input: "+str+"; ", result, Scene.convertPageLength(str));

	}
}
