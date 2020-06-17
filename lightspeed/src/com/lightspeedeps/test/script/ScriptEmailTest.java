/**
 * File: ScriptPagesTest.java
 */
package com.lightspeedeps.test.script;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.lightspeedeps.dao.ContactDAO;
import com.lightspeedeps.dao.ProjectDAO;
import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.Scene;
import com.lightspeedeps.model.Script;
import com.lightspeedeps.model.User;
import com.lightspeedeps.test.SpringTestCase;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.report.ScriptReporter;
import com.lightspeedeps.web.report.ReportBean;

/**
 * This class tests the functionality of the "Email" button on the Script
 * Revisions page.
 */
public class ScriptEmailTest extends SpringTestCase {

	/**
	 * This tests the functionality of the "Email" button on the Script
	 * Revisions page. Note that the source may need to be edited to set the
	 * Project.id value of a project that contains the Script to be tested.
	 * <p>
	 * ** NOTE **<br/>
	 * There is one line of code in SessionUtils.getRealPath() that needs to be
	 * changed depending on if this test is being run under Eclipse, or in batch
	 * (from the command line). In either case, it will log warning messages due
	 * to path validation issues, but the test will work as long as that one
	 * line is correct!
	 */
	public void testScriptFormat() throws Exception {

		final ProjectDAO projectDAO = ProjectDAO.getInstance();
		final Project project = projectDAO.findById(3);	// Set to known project with Script!!
		final Script script = project.getScript();

		assertNotSame("missing script", script, null);

		final List<Contact> contacts = new ArrayList<Contact>();
		final ContactDAO contactDAO = ContactDAO.getInstance();

		final Contact cn = contactDAO.findById(1); // *** Set to a known Contact id ***
		contacts.add(cn);

		List<Scene> sceneList = script.getScenes();
		final ScriptReporter reporter = new ScriptReporter(script, sceneList, 1, 2);

		// If reporter is given non-blank character name, it will highlight output
		//reporter.setCharacterName("JAKE");

		// Pick either day-range or page-range for testing:
		// reporter.setDayRange(true); // requires stripboard have scheduled strips
		reporter.setPageRange(true);

		final ReportBean report = new ReportBean(project, ReportBean.TYPE_SCRIPT);

		final String msgPrefix = "Script.EmailDelivery.";
		final User user = UserDAO.getInstance().findById(1); // System user
		final String sender = user.getAnyName();
		final String email = user.getEmailAddress();
		final Production prod = project.getProduction();
		Locale locale = Constants.LOCALE_US;

		final String subject = DoNotification.formatMessage(msgPrefix + "Subject",
				prod.getTitle(), project, locale, null, sender, email);
		final String body = DoNotification.formatMessage(msgPrefix + "Msg", prod.getTitle(),
				project, locale, null, sender, email);

		String rptName = report.generateScript(reporter, "JUnit test", contacts, script.getDate(),
				false, false, 1, subject, body);

		assertNotNull("Script print failed.", rptName);

	}

}
