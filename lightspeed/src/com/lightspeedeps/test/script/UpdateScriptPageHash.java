/**
 * File: UpdateScriptPageHash.java
 */
package com.lightspeedeps.test.script;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.BaseDAO;
import com.lightspeedeps.dao.ScriptDAO;
import com.lightspeedeps.model.Page;
import com.lightspeedeps.model.Script;
import com.lightspeedeps.test.SpringTestCase;


/**
 * Update the Page "hash" values that are used during Script import to compare
 * old and new revisions of each page of a script.
 * <p>
 * As for all SpringTestCase subclasses, the WEB-INF folder must be added
 * to the classpath for the JUnit test, so the XML configuration files
 * can be located.
 */
public class UpdateScriptPageHash extends SpringTestCase {

	private static Log log = LogFactory.getLog(UpdateScriptPageHash.class);

	public void testUpdate() throws Exception {
		ScriptDAO scriptDAO = ScriptDAO.getFromApplicationContext(ctx);

		String qry = "select max(id) from Script";
		Integer max = (Integer)((BaseDAO)scriptDAO).findOne(qry, null);

		updateScripts(scriptDAO, max);
	}


	public static int updateScripts(ScriptDAO scriptDAO, Integer max) {

		int totalDiff = 0;
		for (int i = 1; i < max; i++) {
			Script s = scriptDAO.findById(i);
			if (s != null && s.getPages() != null) {
				totalDiff += updatePages(scriptDAO, s);
			}
		}
		return totalDiff;
	}


	private static int updatePages(ScriptDAO scriptDAO, Script s) {
		byte[] hash;
		int same = 0, diff = 0;
		log.debug("\n");
		log.debug(s.getDescription());
		for (Page p : s.getPages()) {
			hash = p.getHash();
			p.updateHash();
			if (! Arrays.equals(hash, p.getHash())) {
				log.debug("hash changed for page " + p.getNumber());
				//log.debug("len=" + hash.length + ", old=" + StringUtils.toHexString(hash));
				scriptDAO.attachDirty(p);
				diff++;
			}
			else {
				same++;
			}
		}
		log.debug("same=" + same + ", diff=" + diff);
		return diff;
	}

}
