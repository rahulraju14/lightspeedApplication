package com.lightspeedeps.web.servlet;

import java.io.*;
import java.net.SocketException;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.service.DocumentService;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.web.onboard.form.CustomFormBean;
import com.pdftron.common.PDFNetException;

/**
 * This class supports displaying and downloading files from Lightspeed.
 * It is meant to be a superclass to our concrete servlet classes, where
 * we can have methods common to them.
 */
public class BaseServlet extends HttpServlet {

	private static final Log log = LogFactory.getLog(BaseServlet.class);

	private static final long serialVersionUID = -6468499747044220680L;
	private static final String FILE_ERROR_PAGE = "../jsp/error/500b.html";
	private static final String IMAGE_NOT_FOUND_PAGE = "../../jsp/error/500b.html";

	/**
	 * Constructor of the servlet.
	 */
	public BaseServlet() {
		super();
	}

	/**
	 * Destruction of the servlet.
	 */
	@Override
	public void destroy() {
		super.destroy(); // Just puts "destroy" string in log
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
	}


	/**
	 * Send a file to the supplied response object. If an error occurs, the
	 * user's browser will be redirected to a standard error page.
	 *
	 * @param request The HttpServletRequest that is responsible for this
	 *            request.
	 * @param response The HttpServletResponse to which the data should be sent.
	 * @param absoluteFilePath The absolute file page (i.e., in the server's
	 *            directory structure) of the file to be sent.
	 * @param originalFileName The original filename of the file whose data is
	 *            being sent. If 'preferSaveAs' is true, then this parameter is
	 *            passed in the response as "attachment" data, so that if the
	 *            user is prompted to save the file, the prompt should use this
	 *            filename.
	 * @param preferSaveAs If true, a response header is added indicating that
	 *            this is an "attachment", which most browsers will interpret as
	 *            an indication to prompt the user to save the data as a file,
	 *            rather than the browser attempting to display the file
	 *            contents.
	 */
	protected void sendFile(HttpServletRequest request, HttpServletResponse response,
			String absoluteFilePath, String originalFileName, boolean preferSaveAs) {

		ServletContext context = getServletConfig().getServletContext();
		sendFile(request, response, absoluteFilePath, originalFileName, context, preferSaveAs);
	}

	/**
	 * Send a file to the supplied response object. If an error occurs, the
	 * user's browser will be redirected to a standard error page.
	 *
	 * @param request The HttpServletRequest that is responsible for this
	 *            request.
	 * @param response The HttpServletResponse to which the data should be sent.
	 * @param absoluteFilePath The absolute file page (i.e., in the server's
	 *            directory structure) of the file to be sent.
	 * @param originalFileName The original filename of the file whose data is
	 *            being sent. If 'preferSaveAs' is true, then this parameter is
	 *            passed in the response as "attachment" data, so that if the
	 *            user is prompted to save the file, the prompt should use this
	 *            filename.
	 * @param context The ServletContext corresponding to the request.
	 * @param preferSaveAs If true, a response header is added indicating that
	 *            this is an "attachment", which most browsers will interpret as
	 *            an indication to prompt the user to save the data as a file,
	 *            rather than the browser attempting to display the file
	 *            contents.
	 */
	protected static void sendFile(HttpServletRequest request, HttpServletResponse response,
			String absoluteFilePath, String originalFileName, ServletContext context, boolean preferSaveAs) {

		File file = new File(absoluteFilePath);
		log.debug("file=" + absoluteFilePath + ", len=" + file.length());

		String mimetype = context.getMimeType(absoluteFilePath);
		response.setContentType((mimetype != null) ? mimetype : "application/octet-stream");
		response.setContentLength((int) file.length());

		// response.setHeader("Pragma", "No-cache");
		// response.setHeader("Cache-Control", "no-cache");
		response.setDateHeader("Expires",0);

		if (preferSaveAs) {
			response.setHeader("Content-Disposition", "attachment; filename=\"" + originalFileName + "\"");
		}

		FileInputStream fis = null;
		try {
			fis = new FileInputStream(absoluteFilePath);
			ServletOutputStream out = response.getOutputStream();
			byte[] buffer = new byte[1024];
			int length;
			while ((length = fis.read(buffer)) > 0) {
				out.write(buffer, 0, length);
			}
			out.flush();
			fis.close();
			//log.debug("done");
		}
		catch (FileNotFoundException ioe) {
			try {
				log.debug("requested file not found in BaseServlet");
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			}
			catch (Exception e) {
				log.error(e);
			}
			return;
		}
		catch (IOException ioe) {
			String msg = "IO Exception in download servlet, file=" + absoluteFilePath + ": ";
//			EventUtils.logError(msg, ioe);
			log.error(msg + ioe);
			if (fis != null) {
				try {
					fis.close();
				}
				catch (Exception e) { // ignore this
				}
			}
			try {
				log.debug("redirecting to error page");
				response.sendRedirect(FILE_ERROR_PAGE);
			}
			catch (Exception e) {
//				EventUtils.logError("Exception redirecting in download servlet: ", e);
				log.error("Exception redirecting in download servlet: " + e);
			}
		}
	}


	/**
	 * Send an image from the database to the supplied response object. If an
	 * error occurs, the user's browser will be redirected to a standard error
	 * page.
	 *
	 * @param request The HttpServletRequest that is responsible for this
	 *            request.
	 * @param response The HttpServletResponse to which the image data should be
	 *            sent.
	 * @param id The database id in the Image table of the image to be sent.
	 */
	protected void sendImage(HttpServletRequest request, HttpServletResponse response, Integer id) {
		HttpSession session = request.getSession();
		ServletContext servletContext = session.getServletContext();
		ImageDAO imageDAO = (ImageDAO) ServiceFinder.findBean("ImageDAO", servletContext);
		Image image = imageDAO.findById(id);
		if (image != null) {
			sendData(request, response, image.getContent(), image.getTitle(), IMAGE_NOT_FOUND_PAGE);
		}
	}

	/**
	 * Send a document from the database to the supplied response object. If an
	 * error occurs, the user's browser will be redirected to a standard error
	 * page.
	 *
	 * @param request The HttpServletRequest that is responsible for this
	 *            request.
	 * @param response The HttpServletResponse to which the document data should be
	 *            sent.
	 * @param id The database id in the content table of the document to be sent.
	 * @param parameters A string of parameters parsed out of the URI. May be null.
	 */
	protected void sendDocument(HttpServletRequest request, HttpServletResponse response, Integer id, String fileName, String parameters) {
		HttpSession session = request.getSession();
		ServletContext servletContext = session.getServletContext();
		Integer originalDocId = null;
		ContentDAO contentDAO = (ContentDAO) ServiceFinder.findBean("ContentDAO", servletContext);
		DocumentDAO documentDAO = (DocumentDAO) ServiceFinder.findBean("DocumentDAO", servletContext);
		Document document = documentDAO.findById(id);
		if (document != null) {
			originalDocId = document.getOriginalDocumentId();
		}
		Content content = contentDAO.findByDocId(id, originalDocId);
		if (content != null) {
//			ApplicationScopeBean appBean = (ApplicationScopeBean) ServiceFinder.findBean("applicationScopeBean", servletContext);
			if (fileName.endsWith(Constants.DOCUMENT_XOD_SUFFIX)) {
				sendData(request, response, content.getXodContent(), fileName, IMAGE_NOT_FOUND_PAGE);
			}
			else {
				sendData(request, response, content.getContent(), fileName, IMAGE_NOT_FOUND_PAGE);
			}
		}
	}

	/**
	 * Send a document from the database to the supplied response object. If an
	 * error occurs, the user's browser will be redirected to a standard error
	 * page.
	 *
	 * @param request The HttpServletRequest that is responsible for this
	 *            request.
	 * @param response The HttpServletResponse to which the document data should be
	 *            sent.
	 * @param parameters
	 * @param fileName
	 * @param id The database id in the content table of the document to be sent.
	 */
	protected void sendXfdfData(HttpServletRequest request, HttpServletResponse response, Integer contactDocId, String parameters, String fileName) {
		HttpSession session = request.getSession();
		ServletContext servletContext = session.getServletContext();
		XfdfContentDAO contentDAO = (XfdfContentDAO) ServiceFinder.findBean("XfdfContentDAO", servletContext);
		XfdfContent xfdfContent = contentDAO.findByContactDocId(contactDocId);
		if (xfdfContent != null) {
			String xfdfData = xfdfContent.getContent();
			if (xfdfData != null) {
				//log.debug(xfdfData);
				if (parameters != null) {
					if (parameters.indexOf(CustomFormBean.READ_ONLY) >= 0) {
						// The following modifies the xfdfData such that all fields are read-only in UI.
						xfdfData = DocumentService.enableReadOnlyMode(xfdfData);
					}
				}
				//xfdfData = DocumentService.disableSignButton(xfdfData, contactDocId);
				sendData(request, response, xfdfData.getBytes(), fileName, IMAGE_NOT_FOUND_PAGE);
			}
		}
		else {
			log.warn("XfdfContent object not found for contactDocId=" + contactDocId);
		}
	}

	/**
	 * Send an Attachment from the database to the supplied response object. If an
	 * error occurs, the user's browser will be redirected to a standard error
	 * page.
	 *
	 * @param request The HttpServletRequest that is responsible for this
	 *            request.
	 * @param response The HttpServletResponse to which the Attachment data should be
	 *            sent.
	 * @param parameters
	 * @param fileName
	 * @param id The database id in the content table of the Attachment to be sent.
	 */
	protected void sendAttachmentData(HttpServletRequest request, HttpServletResponse response, Integer attachmentId, String fileName) {
		log.debug("sendAttachmentData");
		HttpSession session = request.getSession();
		ServletContext servletContext = session.getServletContext();
		AttachmentContentDAO contentDAO = (AttachmentContentDAO) ServiceFinder.findBean("AttachmentContentDAO", servletContext);
		AttachmentContent attachmentContent = contentDAO.findByAttachmentId(attachmentId);
		if (attachmentContent != null) {
			if (fileName.endsWith(Constants.DOCUMENT_XOD_SUFFIX)) {
				log.debug("");
				sendData(request, response, attachmentContent.getXodContent(), fileName, IMAGE_NOT_FOUND_PAGE);
			}
			else {
				log.debug("");
				sendData(request, response, attachmentContent.getContent(), fileName, IMAGE_NOT_FOUND_PAGE);
			}
		}
		else {
			log.warn("attachmentContent object not found for attachmentId=" + attachmentId);
		}
	}

	/**
	 * Send the given data stream, as the appropriate mimeType, to the supplied
	 * response object. If an error occurs, the user's browser will be
	 * redirected to the errorPage URL supplied.
	 *
	 * @param request The HttpServletRequest that is responsible for this
	 *            request.
	 * @param response The HttpServletResponse to which the data should be sent.
	 * @param data The data to be sent. The length of the array is the length
	 *            that is sent.
	 * @param originalFileName The original filename of the file whose data is
	 *            being sent. This is used to determine the mimeType that will
	 *            be sent to the browser. If no mimeType can be determined, a
	 *            type of "application/octet-stream" is used.
	 * @param errorPage The relative URL to which the user should be redirected
	 *            if there is an error while trying to send the data.
	 */
	protected void sendData(HttpServletRequest request, HttpServletResponse response,
			byte[] data, String originalFileName, String errorPage) {

		ServletContext context = getServletConfig().getServletContext();
		sendData(request, response, data, originalFileName, context, errorPage);
	}

	/**
	 * Send the given data stream, as the appropriate mimeType, to the supplied
	 * response object. If an error occurs, the user's browser will be
	 * redirected to the errorPage URL supplied.
	 *
	 * @param request The HttpServletRequest that is responsible for this
	 *            request.
	 * @param response The HttpServletResponse to which the data should be sent.
	 * @param data The data to be sent. The length of the array is the length
	 *            that is sent.
	 * @param originalFileName The original filename of the file whose data is
	 *            being sent. This is used to determine the mimeType that will
	 *            be sent to the browser. If no mimeType can be determined, a
	 *            type of "application/octet-stream" is used.
	 * @param context The ServletContext corresponding to the request.
	 * @param errorPage The relative URL to which the user should be redirected
	 *            if there is an error while trying to send the data.
	 */
	protected static void sendData(HttpServletRequest request, HttpServletResponse response,
			byte[] data, String originalFileName, ServletContext context, String errorPage) {
		log.debug("len=" + data.length);

		String mimetype = context.getMimeType(originalFileName);
		response.setContentType((mimetype != null) ? mimetype : "application/octet-stream");

		response.setContentLength(data.length);
		response.setDateHeader("Expires",0);

		try {
			ServletOutputStream out = response.getOutputStream();
			out.write(data);
			out.flush();
			//log.debug("done");
		}
		catch (SocketException se) {
			log.error("Exception in output servlet: ", se);
		}
		catch (Exception ioe) {
			log.error("Exception in output servlet: ", ioe);
			try {
				log.debug("redirecting to error page");
				response.sendRedirect(errorPage);
				//RequestDispatcher rd = context.getRequestDispatcher(FILE_NOT_FOUND);
				//rd.forward(request, response);
			}
			catch (Exception e) {
				log.error("forwarding in output servlet: ", e);
				//EventUtils.logError("Exception forwarding in output servlet: ", e);
			}
		}
	}

	/**
	 * The doPost method of the servlet. <br>
	 *
	 * This method is called when a form has its tag value method equals to post.
	 *
	 * @param request the request send by the client to the server
	 * @param response the response send by the server to the client
	 * @throws ServletException if an error occurred
	 * @throws IOException if an error occurred
	 */
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		log.debug("");
		String uri = request.getRequestURI();
		if (uri.contains("db_annotations")) {
			try {
				saveAnnotations(request, response);
			}
			catch (PDFNetException e) {
				e.printStackTrace();
			}
		}
		else {
			doGet(request,response);
		}
	}

	/** Method used to save the pdf's form data and annotations into the No-sql database (Mongo),
	 *  invoked from the 'save' button on the web viewer tool.
	 * @param request HttpServletRequest
	 * @param response HttpServletResponse
	 * @throws PDFNetException
	 */
	private void saveAnnotations(HttpServletRequest request, HttpServletResponse response) throws PDFNetException {
		String contactDocId = request.getParameter("did");
		String data = request.getParameter("data");
		HttpSession session = request.getSession();
		ServletContext servletContext = session.getServletContext();
		XfdfContentDAO xfdfContentDAO = (XfdfContentDAO) ServiceFinder.findBean("XfdfContentDAO", servletContext);
//		String parameters = null;
		int ix = 0;
		if ((ix=contactDocId.indexOf('$')) > 0) {
//			parameters = contactDocId.substring(ix);
			contactDocId = contactDocId.substring(0, ix);
		}
		XfdfContent xfdf = xfdfContentDAO.findByContactDocId(Integer.parseInt(contactDocId));
		if (xfdf != null) {
			xfdfContentDAO.updateXFDFContent(data, Integer.parseInt(contactDocId));
			log.debug("Updated XFDFContent, contactDocId=" + contactDocId + ", len=" + data.length());
		}
		else {
			xfdf = new XfdfContent(Integer.parseInt(contactDocId), data);
			xfdfContentDAO.insert(xfdf);
			log.debug("Saved new XFDFContent, contactDocId=" + contactDocId + ", len=" + data.length());
		}
		//log.debug("Contact document id=" + contactDocId);
		log.debug("data=`" + data + "`");
	/*
		ContactDocumentDAO contactDocumentDAO = (ContactDocumentDAO) ServiceFinder.findBean("ContactDocumentDAO", servletContext);
		ContactDocument cd = contactDocumentDAO.findById(Integer.parseInt(contactDocId));
		List<SignatureBox> signatureBoxList = null;
		if (cd != null && cd.getDocument() != null) {
			log.debug("document id=" + cd.getDocument().getId()); // SIGNATURE'S DOCUMENT ID
			signatureBoxList = parseXmlData(data, signatureBoxList);
			if (signatureBoxList != null && signatureBoxList.size() > 0) {
				SignatureBoxDAO signatureBoxDAO = SignatureBoxDAO.getInstance();
				for (SignatureBox box : signatureBoxList) {
					box.setDocumentId(cd.getDocument().getId());
					signatureBoxDAO.save(box);
				}
			}
		}*/
	}

//	@SuppressWarnings({ "unchecked" })
	/*private List<SignatureBox> parseXmlData(String data, List<SignatureBox> signatureBoxList) {
		signatureBoxList = new ArrayList<>();
		log.debug("data=`" + data + "`");
		SAXBuilder builder = new SAXBuilder();
		try {
			Document document = builder.build(new ByteArrayInputStream(data.getBytes()));
			log.debug("Root element : " + document.getRootElement().getName());
			Element rootNode = document.getRootElement();
			List<Element> tagList = rootNode.getChildren();
			for (Element tag : tagList) {
				if (tag.getName().equalsIgnoreCase("annots")) {
					List<Element> subTagList = tag.getChildren();
					for (Element subTag : subTagList) {
						if (subTag.getName().equalsIgnoreCase("square")) {
							parseSignatureBox(subTag, signatureBoxList);
						}
					}
				}
			}
		}
		catch (IOException io) {
			log.debug(io.getMessage());
		}
		catch (JDOMException jdomex) {
			log.debug(jdomex.getMessage());
		}
		return signatureBoxList;
	}*/

	/**
	 * Given an Element of type "square", create a SignatureBox instance to
	 * match.
	 *
	 * @param subTag The "square" element.
	 * @param signatureBoxList The List to which the SignatureBox will be added.
	 */
	/*private void parseSignatureBox(Element subTag, List<SignatureBox> signatureBoxList) {
		Integer[] points;
		SignatureBox signatureBox = new SignatureBox();
		//TODO DH: Where do we need MaximumSignature's value? Why it is in signature box?
		signatureBox.setMaximumSignatures(2);
		points = null;
		log.debug("..in annots sub tag : "+ subTag.getName());
		log.debug(" Signature type : " + subTag.getValue());
		if (subTag.getValue().contains("Employee")) {
			// SIGNATURE'S SIGNATURE TYPE
			signatureBox.setSignatureType(SignatureType.EMPLOYEE);
		}
		else if(subTag.getValue().contains("Approver") ||
				subTag.getValue().contains("Employer")) {
			// SIGNATURE'S SIGNATURE TYPE
			signatureBox.setSignatureType(SignatureType.APPROVER);
		}
		else {
			// SIGNATURE'S SIGNATURE TYPE
			signatureBox.setSignatureType(SignatureType.ALL);
		}
		@SuppressWarnings("unchecked")
		List<Attribute> attributeList = subTag.getAttributes();
		for (Attribute attr : attributeList) {
			log.debug("attr: " + attr.toString());
			if (attr.getName().equalsIgnoreCase("subject")) {
				log.debug("----------------------");
				log.debug("attribute name = " + attr.getName());
				log.debug("attribute value = " + subTag.getAttributeValue("subject"));
			}
			else if (attr.getName().equalsIgnoreCase("page")) {
				log.debug("----------------------");
				log.debug("attribute name = " + attr.getName());
				log.debug("Page No. = " + subTag.getAttributeValue("page"));
				Integer pageNum = Integer.parseInt(subTag.getAttributeValue("page"));
				// SIGNATURE'S PAGE NUMBER
				signatureBox.setPageNumber(pageNum + 1);
			}
			else if (attr.getName().equalsIgnoreCase("rect")) {
				log.debug("----------------------");
				log.debug("attribute name = " + attr.getName());
				log.debug("Position of rectangle = " + subTag.getAttributeValue("rect"));
				points = getRectangleCoordinates(subTag.getAttributeValue("rect"));
				// SIGNATURE'S COORDINATES
				signatureBox.setX1(points[0]);
				signatureBox.setY2(points[1]); // Since, getting wrong order of coordinates
				signatureBox.setX2(points[2]);
				signatureBox.setY1(points[3]);
				signatureBoxList.add(signatureBox);
				log.debug("No.of points = " + points.length);
				log.debug(" Signature Box Saved ");
			}
			else if(attr.getName().equalsIgnoreCase("name")) {
				log.debug("----------------------");
				log.debug("attribute name = " + attr.getName());
				log.debug(" Name of rectangle = " + subTag.getAttributeValue("name"));
			}
		}
	}*/

	/** Utility method used to generate the array of coordinates of the rectangle box
	 * @param coordinates comma separated string of coordinates
	 * @return String array
	 */
	/*private Integer[] getRectangleCoordinates(String coordinates){
		Integer[] points = new Integer[4];
		Integer count = 0;

		for (String coordinate: coordinates.split(",")){
			points[count] = Math.round(Float.parseFloat(coordinate));
			count++;
	    }

		for(Integer point : points) {
			log.debug("::::: point = " + point);
		}
		return points;
	}*/

	/**
	 * @param parameterMap
	 */
	public void dump(Map<String, String[]> parameterMap) {
		for (String key : parameterMap.keySet()) {
			log.debug(key + ": " + parameterMap.get(key)[0]);
			if (parameterMap.get(key).length > 1) {
				for (int i = 1; i < parameterMap.get(key).length; i++) {
					log.debug(parameterMap.get(key)[i]);
				}
			}
		}
	}

	/**
	 * Initialization of the servlet.
	 *
	 * @throws ServletException if an error occurs
	 */
	@Override
	public void init() throws ServletException {
		// nothing to do here.
	}

}
