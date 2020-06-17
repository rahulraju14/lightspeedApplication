package com.lightspeedeps.test.script;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.SceneDAO;

import junit.framework.TestCase;

/**
 * This tests the code that creates a synopsis line (used during Script Import)
 * from a scene's "action" text.
 */
public class SynopsisTest extends TestCase {

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(SynopsisTest.class);

	public void testSentenceParse() throws Exception {

		createSynopsis("a");	// should return null -- string is too short.

		// Should return the input argument (less than the max allowed length)
		createSynopsis("This is enough.");

		// Should return the input argument (less than the max allowed length)
		createSynopsis("This is two sentences.  It is shorter than the maximum.");

		// Should return the first two sentences and no more.
		createSynopsis("The two limos pull up. Patty and Martin step out. Their " +
				"Associates hold UMBRELLAS for them in the pouring rain.");

		// Should return the first sentence plus part of the second, terminated with ellipsis.
		createSynopsis("The two limos pull up. Patty and Martin step out " +
				"while their Associates hold UMBRELLAS for them in the pouring rain.");

	}

	private String createSynopsis(String action) {
		String synopsis = SceneDAO.createSynopsis(action);
		System.out.println(synopsis);
		return synopsis;
	}

}
