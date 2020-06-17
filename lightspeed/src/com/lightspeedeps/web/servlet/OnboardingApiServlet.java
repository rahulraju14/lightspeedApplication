package com.lightspeedeps.web.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import com.lightspeedeps.service.FormService;
import com.lightspeedeps.util.app.Constants;

public class OnboardingApiServlet extends BaseServlet {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(OnboardingApiServlet.class);
	/**
	 * The doGet method of the servlet. <br>
	 *
	 * This method is called when a form has its tag value method equals to get.
	 *
	 * @param request
	 *            the request send by the client to the server
	 * @param response
	 *            the response send by the server to the client
	 * @throws ServletException
	 *             if an error occurred
	 * @throws IOException
	 *             if an error occurred
	 */
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String queryString = request.getQueryString();

		if(!StringUtils.isEmpty(queryString)) {
			String id;
			int index;

			// Parse out the ContactDocument id to pass to the onboarding api to find the form.
			index = queryString.indexOf("id=");
			id = queryString.substring(index + 3);
			ByteArrayOutputStream baso = new ByteArrayOutputStream();

			/// Populate the ByteArrayOutputStream
			FormService.getInstance().print(id, Constants.ONBOARDING_PRINT_URL, baso);
			ServletOutputStream out = response.getOutputStream();
			response.setContentType("application/pdf");
			response.setContentLengthLong(baso.size());

			// Write the byte array to the Servlet Contex OutputStream
			out.write(baso.toByteArray());
			out.flush();

			out.close();
			baso.close();
		}
	}

}
