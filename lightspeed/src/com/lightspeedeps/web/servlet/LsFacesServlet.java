/**
 * File: LsFacesServlet.java
 */
package com.lightspeedeps.web.servlet;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.lightspeedeps.dao.ProjectDAO;
import com.lightspeedeps.dao.ProjectMemberDAO;
import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.User;
import com.lightspeedeps.type.UserStatus;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.ServiceFinder;

/**
 * This servlet enforces logged-in status for any user whose request it
 * processes. The web.xml servlet-mapping entries should be configured so this
 * servlet is called for any restricted URLs, typically ones accessing one of
 * our reports (PDFs, XLS, etc.).
 * <p>
 * This servlet also supports retrieving, via a URL, an Image object from our
 * database. This is typically only necessary when the Image object is NOT an
 * actual image type, but is some other type, such as a PDF. An example of this
 * is when someone stores a "paper timecard attachment" which is a PDF.
 * <p>
 * This servlet also supports retrieving, via a URL, a document object component
 * -- either PDF, XFDF, or XOD (for WebViewer) -- from our database.
 * <p>
 * Most of the data-handling methods are in the superclass, {@link BaseServlet}.
 */
public class LsFacesServlet extends BaseServlet {
	private static final long serialVersionUID = 1L;
	private static final Log log = LogFactory.getLog(LsFacesServlet.class);

	/** Pattern of report filenames containing project id and optional user id:
	 *  xxxxxxxx_<projId#>_<userId#>_xxxxxxxx */
	private  static final  Pattern REPORT_PATTERN = Pattern.compile("[^_]*_([0-9]+)_(([0-9]+)_)?.*");

	/** Pattern to extract the Image database id from the URI: _db_image/< id >/filename */
	private static final Pattern IMAGE_PATTERN = Pattern.compile(
			".*/" + Constants.IMAGE_PSEUDO_DIRECTORY + "/([0-9]+)/.*");

	/** Pattern to extract the Document database id from the URI: _db_document/< id >[$params]/filename
	 * where < id > is a numeric string; and '$params' is an optional string of parameter text that
	 * must begin with a '$'. */
	private static final Pattern DOCUMENT_PATTERN = Pattern.compile(
			".*/" + Constants.DOCUMENT_PSEUDO_DIRECTORY + "/([0-9]+)(\\$.*)?/(.*)");

	/** Pattern to extract the Document database id from the URI: _db_document/< id >/filename
	 * where < id > is a numeric string. */
	private static final Pattern ATTACHMENT_PATTERN = Pattern.compile(
			".*/" + Constants.ATTACHMENT_PSEUDO_DIRECTORY + "/([0-9]+)/(.*)");

	/** Pattern to extract a filename from the URI. Note the backslash, which is required to treat
	 * the leading period in the suffix as a period, not an "any-character" symbol. */
	private static final Pattern TIMECARD_PATTERN = Pattern
			.compile(".*/(.*\\" + Constants.TIMECARD_SUFFIX + ")");

	/** Pattern to extract a filename from the URL. Note the backslash, which is required to treat
	 * the leading period in the suffix as a period, not an "any-character" symbol. */
	private static final Pattern JSON_PATTERN = Pattern
			.compile(".*/(.*\\" + Constants.JSON_SUFFIX + ")");

	@Override
	public void doGet(final HttpServletRequest request, final HttpServletResponse response)
			throws ServletException, IOException {
		String uri = request.getRequestURI();
		if (redirectInvalid(uri, response)) {
			log.debug("invalid URI redirected: " + uri);
			return;
		}
		log.debug("URI: " + uri);
		dump(request.getParameterMap());

		boolean pass = true;
		boolean done = false;
		log.debug("restricted request [" + uri + "]: checking logged in status");
		HttpSession session = request.getSession(false);
		if (session == null) {
			pass = false;
		}
		else {
			Boolean loggedIn = (Boolean)session.getAttribute(Constants.ATTR_LOGGED_IN);
			if (loggedIn == null || loggedIn == Boolean.FALSE) {
				pass = false;
			}
			else {
				pass = true;
				try {
					Matcher m = REPORT_PATTERN.matcher(uri);
					if (m.matches()) { // looks like an embedded project id...
//						for (int j = 1; j <= m.groupCount(); j++) {
//							log.debug(j + ": " + m.group(j));
//						}
						Integer userId = (Integer)session.getAttribute(Constants.ATTR_CURRENT_USER);
						if (userId != null) {
							boolean userOk = false;
							ApplicationContext appContext = WebApplicationContextUtils.getRequiredWebApplicationContext(
									request.getSession().getServletContext());
							ServiceFinder.setRequestContext(appContext);
							User user = UserDAO.getInstance().findById(userId);
							if (user != null && ! user.getLockedOut() && user.getStatus() != UserStatus.DELETED) {
								if (m.group(3) != null) {
									Integer repUser = Integer.parseInt(m.group(3));
									if (repUser != null && repUser.intValue() == userId.intValue()) {
										userOk = true;
									}
								}
								if (! userOk) {
									Integer projectId = Integer.parseInt(m.group(1));
									if (projectId > 0) {
										Project project = ProjectDAO.getInstance().findById(projectId);
										if (project != null) {
											pass = ProjectMemberDAO.getInstance().existsUserInProject(user, project);
										}
									}
								}
							}
						}
					}
					else if ((m = IMAGE_PATTERN.matcher(uri)).matches()) {
						String idstr = m.group(1);
						Integer id = null;
						try {
							id = Integer.parseInt(idstr);
						}
						catch (Exception e) {
						}
						if (id != null && id > 0) {
							done = true; // if sendImage throws an error or not, request is done.
							sendImage(request, response, id);
						}
					}
					else if ((m = DOCUMENT_PATTERN.matcher(uri)).matches()) {
						String idstr = m.group(1);
						Integer id = null;
						String parameters = ""; // optional group: may be empty
						String fileName;
						if (m.groupCount() == 3) {
							parameters = m.group(2); // optional group exists
							fileName = m.group(3);
						}
						else {
							fileName = m.group(2);
						}
						try {
							id = Integer.parseInt(idstr);
						}
						catch (Exception e) {
						}
						if (id != null && id > 0) {
							done = true; // if sendImage throws an error or not, request is done.
							sendDocument(request, response, id, fileName, parameters);
						}
					}
					else if (uri.contains(Constants.DOCUMENT_XFDF_PSEUDO_DIRECTORY)) {
						done = true;
						String contactDocId = request.getParameter("did");
						String parameters = null;
						int ix = 0;
						if ((ix=contactDocId.indexOf('$')) > 0) {
							parameters = contactDocId.substring(ix);
							contactDocId = contactDocId.substring(0, ix);
						}
						sendXfdfData(request, response, Integer.parseInt(contactDocId), parameters, "XFDF");
					}
					else if ((m = ATTACHMENT_PATTERN.matcher(uri)).matches()) {
						log.debug("attachment, n=" + m.groupCount());
						String idstr = m.group(1);
						Integer id = null;
						String fileName = m.group(2);
						try {
							id = Integer.parseInt(idstr);
						}
						catch (Exception e) {
						}
						if (id != null && id > 0) {
							done = true; // if sendImage throws an error or not, request is done.
							sendAttachmentData(request, response, id, fileName);
						}
					}
					else {
						log.debug("DO GET");
					}
				}
				catch (NumberFormatException e) {
					log.error("exception: ", e);
				}
				catch (Exception e) {
					log.error("exception: ", e);
				}
			}
		}

		if (! done) {
			if (pass) {
				//log.debug("request allowed");
				if (uri.contains(Constants.TIMECARD_SUFFIX)) {
					String filename;
					Matcher m = TIMECARD_PATTERN.matcher(uri);
					if (m.matches()) {
						filename = m.group(1);
					}
					else {
						filename = "lightspeed" + Constants.TIMECARD_SUFFIX;
					}
					response.setContentType("text/tab-separated-values");
					response.setHeader("Content-disposition", "attachment;filename=" + filename);
				}
				else if (uri.contains(Constants.JSON_SUFFIX)) {
					String filename;
					Matcher m = JSON_PATTERN.matcher(uri);
					if (m.matches()) {
						filename = m.group(1);
					}
					else {
						filename = "lightspeed" + Constants.JSON_SUFFIX;
					}
					response.setContentType("application/json");
					response.setHeader("Content-disposition", "attachment;filename=" + filename);
				}

				String absoluteFilePath = getServletContext().getRealPath(Constants.REPORT_FOLDER);
				String originalFileName = uri;
				int i = originalFileName.lastIndexOf('/');
				if (i > 0) {
					originalFileName = originalFileName.substring(i+1);
				}
				absoluteFilePath += File.separator + originalFileName;
				sendFile(request, response, absoluteFilePath, originalFileName, getServletContext(), false);
			}
			else {
				log.debug("request denied; redirecting to login page");
				response.sendRedirect("../login.jsf");
			}
		}
	}

	/**
	 * Check the requested URI for invalid situations. In any of these cases,
	 * redirect the response to the appropriate page and return true.  These cases usually
	 * happen when a prior /report/ request ended with some sort of error, and the attempt
	 * to redirect the user to an error page didn't work right, so that the requested URL
	 * still included the "/report/" directory, which caused this servlet to be called.
	 *
	 * @param uri The requested URI.
	 * @param response The HttpServletResponse that can be used for redirection.
	 * @return True if the response has been redirected, false otherwise.
	 * @throws IOException
	 */
	private boolean redirectInvalid(String uri, HttpServletResponse response) throws IOException {
		if (uri.indexOf("/error/") >= 0) { // an error page request came to us.
			// The URL is probably something like /ls/report/jsp/error/500b.html
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return true;
		}
		else if (uri.indexOf("/recover.jsf") >= 0) {
			// The (relative) link in one of our error pages resolved such that it
			// included /report/, e.g., "/ls/report/cache/jsp/recover.jsf". Fix it up.
			int i = uri.indexOf("/report/");
			if (i >= 0) {
				uri = uri.substring(0,i);
				uri += "/jsp/recover.jsf";
				response.sendRedirect(uri);
				return true;
			}
		}
		else if (uri.indexOf(".css") > 0 && uri.indexOf("/c/") > 0) {
			// this typically happens if the error page is requested at the wrong
			// directory level -- so it's css file is requested improperly too.
			int i = uri.indexOf("/report/");
			if (i >= 0) {
				int j = uri.indexOf("/c/"); // the directory of our css files
				uri = uri.substring(0,i) + uri.substring(j);
				response.sendRedirect(uri);
				return true;
			}
		}
		return false;
	}

}
