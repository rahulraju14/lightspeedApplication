package com.lightspeedeps.web.servlet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.DocumentDAO;
import com.lightspeedeps.model.Document;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.ServiceFinder;

/**
 * This class supports downloading files from the File Repository, and the
 * Project export facility.
 * <p>
 * To use this class, the code should create a URL which matches the download
 * servlet URL pattern in the web.xml file, then send the browser a request to
 * open that URL. See
 * {@link com.lightspeedeps.web.repository.FileRepositoryBean#actionDownloadDocument()}
 * as an example.
 */
public class FileDownloadServlet extends BaseServlet {

	protected static final Log log = LogFactory.getLog(FileDownloadServlet.class);

	private static final long serialVersionUID = -6468499747044220680L;

	/**
	 * Constructor of the servlet.
	 */
	public FileDownloadServlet() {
		super();
	}

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
		log.debug("");

		String fileName = request.getParameter("fileName");
		@SuppressWarnings("deprecation")
		String downloadPath = request.getRealPath(Constants.ARCHIVE_RETRIEVAL_FOLDER) + File.separator;
		if (fileName != null && fileName.length() > 0) {
			sendFile(request, response, downloadPath + fileName, fileName, true);
		}
		else {
			String id = request.getParameter("file");
			log.debug("id="+id);
			Document document = null;
			if (id != null) {
				Integer fileId = Integer.parseInt(id);
				HttpSession session = request.getSession();
				ServletContext servletContext = session.getServletContext();
				DocumentDAO documentDAO = (DocumentDAO) ServiceFinder.findBean("DocumentDAO", servletContext);
				document = documentDAO.findById(fileId);
				if (document != null) {
					int randomNumber = (int) (Math.random() * 1000000);
					fileName = document.getName() + ".t" + randomNumber + document.getType();
					log.debug("filename="+fileName);
					FileOutputStream fos = new FileOutputStream(downloadPath + fileName);
					fos.write(document.getContent());
					fos.flush();
					fos.close();
					sendFile(request, response, downloadPath + fileName,
							document.getName() + document.getType(), true);
				}
			}
		}
	}

}
