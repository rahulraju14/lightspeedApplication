/**
 * File: ScriptPagesTest.java
 */
package com.lightspeedeps.test.script;

import com.lightspeedeps.batch.ScriptPagesCheck;
import com.lightspeedeps.test.SpringTestCase;
import com.lightspeedeps.util.app.Constants;

/**
 * This class tests either (or both) the formatting of script pages, used when generating e-mail of
 * script pages or scenes, as well as the actual "advance script" e-mail generation process.
 */
public class ScriptPagesTest extends SpringTestCase {

	/**
	 * Tests the overall function of sending formatted script pages to users who have the preference
	 * set to send "advance script" pages. This may actually generate e-mails based on the current
	 * data in the database.
	 * <p>
	 * BECAUSE THIS MAY GENERATE EMAILS, the method name is changed and stored in the repository
	 * so that it will not, by default, be started during a normal JUnit run.  To actually run
	 * the test, simply rename the method so that it begins with "test".
	 */
	public void DO_NOT_testScriptPagesCheck() throws Exception {
		ScriptPagesCheck check = (ScriptPagesCheck)ctx.getBean("scriptPagesCheck");

		String result = check.execute();
		assertEquals("test failed", Constants.SUCCESS, result);
	}

	/**
	 * This tests the script formatting process which is used to generate the e-mail text for the
	 * "advance script pages" function. It only generates output to the system console. No e-mails
	 * or other notifications are created.
	 */
//	public void testScriptFormat() throws Exception {
//
//
//		ScriptDAO scriptDAO = (ScriptDAO)ServiceFinder.findBean("ScriptDAO");
//		Script script = SessionUtils.getCurrentProject().getScript();
//
//		if (script == null) {
//			log.warn("Current script is null!");
//			script = scriptDAO.findById(101);
//		}
//		assertNotSame("missing script", script, null);
//
//		String result = null;
//
//		for (Scene scene : script.getScenes()) {
//			result = ScriptReporter.formatScene(scene);
//			System.out.println(result);
//		}
//
///*		ScriptUtils util = ScriptUtils.getInstance();
// 		result = util.formatScene(script, "4");
//		System.out.println(result);
//*/
//
//		assertNotSame("failed", null, result);
//		assertNotSame("failed", "", result);
//
//	}

}
