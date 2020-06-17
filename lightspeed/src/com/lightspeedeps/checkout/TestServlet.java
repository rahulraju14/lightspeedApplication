/**
 * File: TestServlet.java
 */
package com.lightspeedeps.checkout;

import com.lightspeedeps.util.app.Constants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is designed to test HTTP requests, by logging the header,
 * parameters, and data (body) of the request.
 */
public class TestServlet extends HttpServlet {
	private static final Log log = LogFactory.getLog(TestServlet.class);
	private static final long serialVersionUID = 1L;

	/**
	 * The normal entry point for the HTTP post
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		log.debug("");
		doTestPost(request);
		sendOk(response);
	}

	/**
	 * Process a posting.
	 *
	 * @param request The ServletRequest containing all the parameters passed.
	 */
	public static void doTestPost(HttpServletRequest request) {
		try {
			Enumeration<String> names = request.getHeaderNames();
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				log.debug("header: " + name + ", value: " + request.getHeader(name));
			}
			String msg = "Request parameters:" + Constants.NEW_LINE + formatParams(request);
			log.debug(msg);

			log.debug("Body:");
			BufferedReader rdr = request.getReader();
			String line = "";
			while( line != null) {
				line = rdr.readLine();
				log.debug(line);
			}
			log.debug("END Body");
		}
		catch (Exception e) {
			log.error("", e);
		}
	}

	/**
	 * Created a printable representation of all the parameters passed as part
	 * of the given request.
	 *
	 * @param request The HttpServletRequest whose parameters are to be
	 *            formatted.
	 * @return A non-null String containing the names and values of all the
	 *         parameters in the given request, in ascending alphabetical order
	 *         of the parameter names.
	 */
	private static String formatParams(HttpServletRequest request) {
		String msg = "";
		Map<String, String[]> parms = request.getParameterMap();
		List<String> keys = new ArrayList<String>(parms.keySet());
		Collections.sort(keys);
		for (String s : keys) {
			msg += "param: " + s + ", value(s): ";
			String[] values = parms.get(s);
			for (String v : values) {
				msg += v + "; ";
			}
			msg += Constants.NEW_LINE;
		}
		return msg;
	}

	/**
	 * Sends a tiny, but valid, HTML page with the text "test OK".
	 *
	 * @param response
	 */
	private void sendOk(HttpServletResponse response) {
		response.setContentType("text/html");
		try {
			PrintWriter out = response.getWriter();
			response.setStatus(HttpServletResponse.SC_OK);
			out.println("<HTML><HEAD></HEAD><BODY>test OK</BODY></HTML>");
			out.flush();
			out.close();
		}
		catch (IOException e) {
			log.error("exception: ", e);
		}
	}

	/**
	 * Entry point for HTTP "GET" operation.  Responds with a simple
	 * "hello from Lightspeed" message in a valid HTML page.
	 */
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		log.debug("");
		doTestPost(request);

		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">");
		out.println("<HTML>");
		out.println("  <HEAD><TITLE>Test servlet</TITLE></HEAD>");
		out.println("  <BODY>");
		out.print("    Hello from Lightspeed TestServlet!");
		out.println("  </BODY>");
		out.println("</HTML>");
		out.flush();
		out.close();
	}

}
