//	File Name:	ScriptDAO.java
package com.lightspeedeps.dao;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.Scene;
import com.lightspeedeps.model.Script;

/**
 * A data access object (DAO) providing persistence and search support for
 * Script entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.Script
 */
public class ScriptDAO extends BaseTypeDAO<Script> {
	private static final Log log = LogFactory.getLog(ScriptDAO.class);
	// property constants
//	private static final String COPYRIGHT = "copyright";
//	private static final String REVISION_NUMBER = "revisionNumber";
//	private static final String DESCRIPTION = "description";
//	private static final String LAST_PAGE = "lastPage";

	public static ScriptDAO getInstance() {
		return (ScriptDAO)getInstance("ScriptDAO");
	}

	public static ScriptDAO getFromApplicationContext(ApplicationContext ctx) {
		return (ScriptDAO) ctx.getBean("ScriptDAO");
	}

	public Script findByRevisionAndProject(int revision, Project project) {
		final String FIND_BY_REVISION =
			"from Script s where s.revisionNumber = ? and s.project = ?";
		Integer irev = new Integer(revision);
		Script script = findOne(FIND_BY_REVISION, new Object[]{irev,project});
		return script;
	}

	/**
	 * Find the highest revisionNumber value for all Scripts in the given Project.
	 * Returns 0 if there are no scripts in the Project.
	 */
	@SuppressWarnings("unchecked")
	public int findMaxScriptRevision(Project project) {
		String strQuery = "select max(s.revisionNumber) from Script s where s.project=?";
		Integer iRev = null;
		int revision = 0;
		List<Object> list = find(strQuery, project);
		if (list != null && list.size() > 0 && list.get(0) instanceof Integer) {
			iRev = (Integer)list.get(0);
			if (iRev != null) {
				revision = iRev.intValue();
			}
		}
		return revision;
	}

	/**
	 * Delete a scene and remove it from the specified Script.
	 * @param script
	 * @param scene
	 * @return The updated script object.
	 */
	@Transactional
	public Script removeScene(Script script, Scene scene) {
		script = refresh(script);
		script.getScenes().remove(scene);
		delete(scene);
		script = merge(script);
		return script;
	}

	/**
	 * @param script
	 */
	@Transactional
	public void remove(Project project, Script script) {
		log.debug("");
		ProjectDAO projectDAO = ProjectDAO.getInstance();
		project.getScripts().remove(script);
		if (project.getScript() != null && project.getScript().getId() == script.getId()) {
			Script otherScript = null;
			if (project.getScripts().size() > 0) {
				otherScript = project.getScripts().iterator().next();
			}
			projectDAO.setScript(project, otherScript);
		}
		projectDAO.attachDirty(project); // rev 3712; was 'merge'
		// Delete the script from the database -- will delete Scenes, too.
		delete(script);
		//project = projectDAO.refresh(project);
		if (project.getScripts().size() == 0) { // deleted the last script
			ScriptElementDAO.getInstance().deleteAll(project); // so remove all script elements
		}
	}

}