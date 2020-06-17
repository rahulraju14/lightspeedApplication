package com.lightspeedeps.dao;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.Attachment;
import com.lightspeedeps.model.AttachmentContent;
import com.lightspeedeps.model.ContactDocument;
import com.lightspeedeps.model.Content;
import com.lightspeedeps.type.MimeType;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.pdftron.common.PDFNetException;
import com.pdftron.filters.Filter;
import com.pdftron.filters.FilterReader;
import com.pdftron.pdf.Convert;
import com.pdftron.pdf.PDFDoc;

/**
 * A data access object (DAO) providing persistence and search support for
 * AttachmentDAO entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.Document
 */

public class AttachmentDAO extends BaseTypeDAO<Attachment> {
	private static final Log log = LogFactory.getLog(AttachmentDAO.class);


	public static AttachmentDAO getInstance() {
		return (AttachmentDAO)getInstance("AttachmentDAO");
	}

	/**
	 * Save the contents of a Attachment using the Content storage system.
	 * 
	 * @param contactDocument
	 * @param name
	 * @param data
	 * @param mt 
	 * @return AttachmentId
	 */
	@Transactional
	public Integer saveData(ContactDocument contactDocument, String name, byte[] data, MimeType mt) {
		Map<String, Object> values = new HashMap<> ();
		values.put("name", name);
		values.put("contactDocument", contactDocument);
		if (findOneByNamedQuery(Attachment.GET_ATTACHMENT_BY_NAME_CONTACT_DOC, values) != null) {
			return null;
		}
		else {
			Attachment attachment = new Attachment();
			attachment.setName(name);
			attachment.setUser(SessionUtils.getCurrentUser());
			attachment.setContactDocument(contactDocument);
			Date now = Calendar.getInstance().getTime();
			attachment.setUploaded(now);
			attachment.setMimeType(mt);
			Integer attachmentId = save(attachment);
			saveContent(data, attachmentId);
			return attachmentId;
		}
	}
	
	/**
	 * Save the contents of a Attachment using the Content storage system.
	 *
	 * @param data The data (Attachment contents) to be stored.
	 * @param attachmentId The id under which the contents will be stored.
	 */
	@Transactional
	private void saveContent(byte[] data, Integer attachmentId) {
		AttachmentContent attachmentContent = new AttachmentContent();
		attachmentContent.setAttachmentId(attachmentId);
		attachmentContent.setContent(data);
		attachmentContent.setXodContent(AttachmentContent.EMPTY_XOD.getBytes());
		AttachmentContentDAO.getInstance().insert(attachmentContent);
	}
	
	/** Method converts the uploaded pdf's content into an XOD formats and saves ,
	 * it to the corresponding document id
	 * @param attachmentId Attachment ID whose content is to be converted
	 * @throws Exception PDFNetException
	 */
	public void convertToXod(Integer attachmentId) throws Exception {
		//document.OriginalDocumentId will be null in this case, because it is not null for copied documents only.
		AttachmentContent attachContent = AttachmentContentDAO.getInstance().findByAttachmentId(attachmentId);
		try {
			log.debug("attachment Id =" + attachmentId);
			PDFDoc pdfDoc = new PDFDoc(attachContent.getContent());
//			XODOutputOptions opts = new XODOutputOptions();
//			opts.setAnnotationOutput(XODOutputOptions.e_flatten); // "flatten" annotations
			log.debug("converting PDF to XOD with NO options");
			Filter filter = Convert.toXod(pdfDoc);
			FilterReader filterRdr = new FilterReader(filter);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte [] buf = new byte[1024];
			for (long readNum; (readNum = filterRdr.read(buf)) > 0;) {
				// Write bytes from the byte array starting at offset 0 to the byte array output stream.
				bos.write(buf, 0, (int)readNum);
			}
			byte[] xodContent = bos.toByteArray();
			log.debug("xod data length >>>>> "+xodContent.length);
			AttachmentContentDAO.getInstance().updateXodContent(attachmentId, xodContent);
			saveAsFile(xodContent, attachmentId); 	// TODO temporary - for debugging
		}
		catch (PDFNetException e) {
			EventUtils.logError(e);
//			throw new LoggedException(e); // TODO for now, allow process to continue 'normally'
		}
	}
	
	/**
	 * Temporary code to save the XOD data as an external file for debugging.
	 * @param xodContent
	 * @param attachmentId
	 */
	private void saveAsFile(byte[] xodContent, Integer attachmentId) {
		DateFormat df = new SimpleDateFormat("MM-dd_HHmmss");
		String timestamp = df.format(new Date());
		String reportPath = SessionUtils.getRealReportPath();
		String fileName = reportPath +  "xod_" + attachmentId + "_" + timestamp;
		try {
			OutputStream outputStream = new FileOutputStream(new File(fileName));
			outputStream.write(xodContent);
			outputStream.close();
		}
		catch (IOException e) {
			log.error(e);
		}
	}

}