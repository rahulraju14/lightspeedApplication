package com.lightspeedeps.util.app;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ChangeDAO;
import com.lightspeedeps.model.*;
import com.lightspeedeps.type.ActionType;
import com.lightspeedeps.type.ChangeType;

/**
 * A utility class for methods related to the Changes database table and Change
 * objects.
 */
public class ChangeUtils {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(ChangeUtils.class);

	// public static void logChange(ChangeType type, ActionType action) {
	// logChange( type, action, SessionUtils.getCurrentProject(), null,
	// (User)null, null);
	// }

	private ChangeUtils() {
	}

	/**
	 * Log a system change, using the current User as the logged user value, the
	 * current Project as the logged Project value, and the current Production
	 * as the logged Production value.
	 *
	 * @param type
	 * @param action
	 * @param description
	 */
	public static void logChange(ChangeType type, ActionType action, String description) {
		logChange(type, action, SessionUtils.getCurrentProject(), null, (User)null, description);
	}

	/**
	 * Log a system change, using the current User as the logged user value and
	 * the current Production as the logged Production value.
	 *
	 * @param type
	 * @param action
	 * @param project
	 * @param description
	 */
	public static void logChange(ChangeType type, ActionType action, Project project,
			String description) {
		logChange(type, action, project, null, (User)null, description);
	}

	/**
	 * Log a system change, not related to any specific Project. This uses the
	 * current Production as the logged Production value.
	 *
	 * @param type
	 * @param action
	 * @param user
	 * @param description
	 */
	public static void logChange(ChangeType type, ActionType action, User user, String description) {
		logChange(type, action, null, null, user, description);
	}

	/**
	 * Log a system change related to the given Project. This uses the current
	 * Production as the logged Production value.
	 *
	 * @param type
	 * @param action
	 * @param project
	 * @param user
	 * @param description
	 */
	public static void logChange(ChangeType type, ActionType action, Project project,
			User user, String description) {
		logChange(type, action, project, null, user, description);
	}

	/**
	 * Log a system change related to the given Production and User.
	 *
	 * @param type
	 * @param action
	 * @param production
	 * @param user
	 * @param description
	 */
	public static void logChange(ChangeType type, ActionType action, Production production,
			User user, String description) {
		String username = makeUserString(user);
		Change change = new Change(type, action, production, null, username, new Date(), description);
		ChangeDAO.getInstance().save(change);
	}

	/**
	 * Log a system change related to the given Project and Stripboard, using
	 * the current User as the logged user value and the current Production as
	 * the logged Production value.
	 *
	 * @param type
	 * @param action
	 * @param project
	 * @param board
	 * @param description
	 */
	public static void logChange(ChangeType type, ActionType action, Project project,
			Stripboard board, String description) {
		logChange(type, action, project, board, (User)null, description);
	}

	private static void logChange(ChangeType type, ActionType action, Project project, Stripboard board,
			User user, String description) {
		String username = makeUserString(user);
		logChange(type, action, project, board, username, description);
	}

	private static void logChange(ChangeType type, ActionType action, Project project, Stripboard board,
			String username, String description) {
		Change change = new Change(type, action, SessionUtils.getProduction(), project, username,
				new Date(), description);
		change.setStripboard(board);
		ChangeDAO.getInstance().save(change);
	}

	private static String makeUserString(User user) {
		String username = null;
		if (user == null) {
			user = SessionUtils.getCurrentUser();
		}
		if (user != null) {
			String acct = "";
			String email = "";
			String name = "";
			try {
				acct = user.getAccountNumber();
			}
			catch (Exception e) {
			} // ignore LazyInitializationExceptions
			try {
				email = user.getEmailAddress();
			}
			catch (Exception e) {
			} // ignore LazyInitializationExceptions
			try {
				name = user.getDisplayName();
			}
			catch (Exception e) {
			} // ignore LazyInitializationExceptions
			username = acct + ":" + email + " [" + name + ']';
		}
		return username;
	}

}
