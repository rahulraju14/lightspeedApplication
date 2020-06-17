package com.lightspeedeps.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.batch.SpringBatch;
import com.lightspeedeps.dao.ProjectDAO;
import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.model.User;
import com.lightspeedeps.util.app.Constants;

/**
 * This class may be used for easy JUnit testing of code that requires
 * Spring/Hibernate support. Modify it as necessary to run your desired
 * method(s). Changes may be committed to the source repository if that seems
 * appropriate, or you can just throw them away when you're done.
 * <p>
 * See {@link com.lightspeedeps.test.RunSomethingTest} for an example of running
 * this class' methods as a JUnit test.
 * <p>
 * If you add DAO's as fields, you will need to add the matching property tag to
 * the XML in applicationContextTest.xml.
 */
public class RunSomething extends SpringBatch {
	private static final Log log = LogFactory.getLog(RunSomething.class);

	/** Project access object. Set in applicationContextTest.xml. */
	private transient ProjectDAO projectDAO;
	/**  User access object. Set in applicationContextTest.xml. */
	private transient UserDAO userDAO;


	/** Default constructor */
	public RunSomething() {
		log.debug("");
	}

	/**
	 * Called via a scheduled job, such as a Quartz task, or from a JUnit test
	 * case. See applicationContextScheduler.xml. The method name (execute) is
	 * arbitrary.
	 *
	 * @return Any value applicable for the test you want to run.
	 */
	public String execute() {
		log.debug("");
		String ret = Constants.FAILURE;
		setUp();	// required for SpringBatch subclasses - initializes context

		try {
			User user;

			user = getUserDAO().findById(1);
			//getProjectDAO().findProjectsWithPermission(user, Permission.EDIT_MATERIALS);
			log.debug(user.getPassword());
			ret = Constants.SUCCESS;
		}
		catch (Exception ex) {
			log.error("Exception: ", ex);
		}
		finally {
			tearDown();	// required for SpringBatch subclasses - clean up.
		}
		return ret;
	}

	/** See {@link #projectDAO}. */
	public ProjectDAO getProjectDAO() {
		if (projectDAO == null) {
			projectDAO = ProjectDAO.getInstance();
		}
		return projectDAO;
	}
	/** See {@link #projectDAO}. */
	public void setProjectDAO(ProjectDAO dao) {
		projectDAO = dao;
	}

	/** See {@link #userDAO}. */
	public UserDAO getUserDAO() {
		if (userDAO == null) {
			userDAO = UserDAO.getInstance();
		}
		return userDAO;
	}
	/** See {@link #userDAO}. */
	public void setUserDAO(UserDAO dao) {
		userDAO = dao;
	}

}
