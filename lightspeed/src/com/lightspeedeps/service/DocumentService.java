package com.lightspeedeps.service;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;
import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.TextMarkup;
import com.lightspeedeps.object.TextStyle;
import com.lightspeedeps.object.WaterMark;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.file.PdfUtils;
import com.lightspeedeps.web.onboard.form.StandardFormBean;
import com.lightspeedeps.web.report.ReportBean;
import com.pdftron.common.PDFNetException;
import com.pdftron.fdf.FDFDoc;
import com.pdftron.pdf.*;
import com.pdftron.pdf.Page;
import com.pdftron.sdf.SDFDoc;

/**
 * A class containing a variety of methods to support managing Onboarding
 * documents. This includes methods related to printing the documents, and
 * manipulating the XFDF aspect of documents for various purposes, e.g., hiding
 * button widgets that should not be displayed.
 */
public class DocumentService extends BaseService {

	private static final Log log = LogFactory.getLog(DocumentService.class);

	/** The default TextStyle used for e-signatures. */
	static final TextStyle DEF_SIGNATURE_STYLE =
			new TextStyle(8, TextDirection.HORIZONTAL, new Color(0), 100, false);

	/** The height of a box, in points, required to hold our standard e-signature display. */
	private static final BigDecimal POINTS_PER_SIGNATURE = new BigDecimal(24);

	private static final String WATERMARK_SUFFIX = "W";

	/** A default SignatureBox set with the default values for font, location, and size. */
	private static SignatureBox DEF_SIGNATURE_BOX = null;

	/** Map of Id of ContactDocument and List of , used to hold the names of sign buttons
	 * to disable for that contact document. */
	private static Map<Integer, List<String>> mapOfContactDocButtonsToDisable = new HashMap<>();

	static {
		try {
			double height =  Constants.POINTS_PER_INCH*(0.75);
			DEF_SIGNATURE_BOX = new SignatureBox(0, Constants.POINTS_PER_INCH,
					(Constants.POINTS_PER_INCH + (Constants.POINTS_PER_INCH * 3)),
					Constants.POINTS_PER_INCH, (int) (Constants.POINTS_PER_INCH + height), 2);
			DEF_SIGNATURE_BOX.setSignatureType(SignatureType.EMPLOYEE);
			log.debug("Height: " + DEF_SIGNATURE_BOX.getY2());
		}
		catch(Exception e) {
			log.error("Failed creating static Signature Box: ", e);
		}
	}

	/**
	 * Given a ContactDocument, and an existing PDF of the underlying document,
	 * create a new PDF that is the original PDF with the addition of signature
	 * and other event information from the ContactDocument.
	 *
	 * @param cd The ContactDocument of interest; the events to be added to the
	 *            PDF are those ContactDocEvent`s associated with this CD. The
	 *            related Document will be used to determine the SignatureBox`s
	 *            controlling where the event information is placed on the PDF.
	 * @param inputFile The fully-qualified name of the existing PDF.
	 * @param outputFile The fully-qualified name to be used for the PDF created
	 *            by this process. This must be different than the input file.
	 * @return True iff the output PDF was successfully created.
	 */
	public static boolean printWithSignature(ContactDocument cd, String inputFile, String outputFile) {
		boolean res = false;
		List<SignatureBox> signatureBoxList = new ArrayList<>();
		List<ContactDocEvent> events = new ArrayList<>();
		List<TextMarkup> textMarkUpList = new ArrayList<>();
		try {
			if (cd != null) {
				Document document = cd.getDocument();
				if (document != null) {
					signatureBoxList = SignatureBoxDAO.getInstance().findByProperty("documentId", document.getId());
					if (signatureBoxList.size() > 0) {
						events = ContactDocEventDAO.getInstance().findByProperty("contactDocument", cd);
						textMarkUpList = createMarkUp(signatureBoxList, events);
					}
					TextStyle textStyle = DEF_SIGNATURE_STYLE;
					res = PdfUtils.addMarkup(inputFile, outputFile, textStyle, textMarkUpList);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return res;
	}

	/**
	 * Create a List of TextMarkup objects describing the text and position of
	 * data to be applied to a PDF based on a List of SignatureBox instances for
	 * the document and a List of events (such as signatures and approvals) to
	 * be turned into text.
	 *
	 * @param signatureBoxList The (possibly empty) List of SignatureBox`s where
	 *            the text should be placed. If empty List, or null, a default
	 *            signatureBox will be provided.
	 * @param events The List of ContactDocEvent`s to be formatted within the
	 *            space of the SignatureBox`s.
	 * @return a List of TextMarkup instances corresponding to the given events
	 *         and boxes.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static List<TextMarkup> createMarkUp(List<SignatureBox> signatureBoxList, List<ContactDocEvent> events) {
		List<TextMarkup> textMarkUpList = new ArrayList<>();
		if (events.size() == 0) { // nothing to put in the signature box
			return textMarkUpList; // so return with no markup.
		}
		int eventNumber = 0; // use events sequentially to fill up signature boxes
		if (signatureBoxList == null) {
			signatureBoxList = new ArrayList<>();
		}
		if (signatureBoxList.isEmpty()) {
			signatureBoxList.add(DEF_SIGNATURE_BOX);
		}
	boxes: // to break out of both loops when we run out of signed events.
		for (SignatureBox signatureBox : signatureBoxList) {
			TextMarkup textMarkup = new TextMarkup();
			textMarkup.setHorizontalPos(signatureBox.getX1());
			textMarkup.setVerticalPos(signatureBox.getY1());
			textMarkup.setPageNumber(signatureBox.getPageNumber());
			textMarkUpList.add(textMarkup);
			// how to distinguish between signatures of Employee/Employer/Approver?.
				// DH: ignore signature type for now
			List<String> markUpText = new ArrayList<>();
			textMarkup.setText(markUpText);
			for (int i=0; i < signatureBox.getMaximumSignatures(); i++) {
				// how to relate a signed event and a signature box?
				//How we will know that a signature is for that particular box?
					// DH: just take events until we fill up max signatures.
//				for (ContactDocEvent event : events) {
				SignedEvent event = events.get(eventNumber++);
				if ((signatureBox.getSignatureType().equals(SignatureType.EMPLOYEE))
						&& (event.getType().equals(TimedEventType.SUBMIT))) {
					markUpText.addAll(event.get2LineSignature());
				}
				else {
					markUpText.addAll(event.get2LineSignature());
				}
				if (eventNumber >= events.size()) {
					break boxes; // have processed all events.
				}
//				}
			}
		}
		return textMarkUpList;
	}

	/**
	 * Creates a printed PDF report for one of our documents. This includes our
	 * standard documents as well as custom documents. It incorporates merging
	 * of updated XFDF data with the original PDF document, and optionally
	 * applying signatures via a "water-marking" technique (overlaid PDF data).
	 * The updated XFDF data can come from either a directly-edited PDF, for
	 * custom PDFs edited in WebViewer; or from the merging of our specialized
	 * XFDF for standard documents with data saved in SQL tables.
	 *
	 * @param formType The type of form, e.g., W4, I9, OTHER...
	 * @param cd The ContactDocument representing the data to be printed; the
	 *            document referenced by this CD is the basis of the print
	 *            output.
	 * @param prtForm The Form instance being printed; for standard documents,
	 *            this will be the source of the data to be merged into the
	 *            specialized XFDF.
	 * @param fileNamePdf The unqualifed file name to use for the generated PDF
	 *            file; if null, a standard filename will be created.
	 * @param isTransfer If true, the file is being generated as part of a
	 *            transfer process, and should not be displayed; if false, a
	 *            browser window will be opened on the generated PDF file.
	 * @return The filename of the printed file. This may be different from
	 *         'fileNamePdf' in some cases, such as where a VOID watermark has
	 *         been applied (outside of Jasper reports). If null is returned, it
	 *         generally indicates a severe error, such as an Exception, or a
	 *         missing component, such as the XFDF data.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static String printDocument(PayrollFormType formType, ContactDocument cd, Form prtForm, String fileNamePdf, boolean isTransfer) {
		try {
			log.debug("formType=" + formType);
			Document document = null;
			if (formType.equals(PayrollFormType.OTHER)) {
				document = cd.getDocument();
			}
			else if (formType.printUsingXFDF() || (FF4JUtils.useFeature(FeatureFlagType.TTCO_PAYROLL_START_PDF_PRINTING)
					&& formType == PayrollFormType.START && ((StartForm)prtForm).getUseXfdf())) {
				// LS-3824 Print the Payroll Start if the useXfdf flag is set.
				// use "master" document (body and XFDF) for printing standard documents
				document = getMasterDocument(formType, prtForm.getVersion());
			}
			else if (prtForm == null) {
				return null;
			}
			else {
				ReportBean report = ReportBean.getInstance();
				report.setOpenReportPopupWindow(false);
				if (formType.equals(PayrollFormType.START)) {
					report.generateStartForm((StartForm)prtForm, fileNamePdf, cd.getStatus().name());
				}
				else if (formType.equals(PayrollFormType.I9)) {
					report.generateFormI9((FormI9)prtForm, fileNamePdf, cd.getId(), cd.getStatus().name());
				}
				else if (formType.equals(PayrollFormType.INDEM)) {
					report.generateFormIndem((FormIndem)prtForm, fileNamePdf, cd.getId(), cd.getStatus().name());
				}
				else if (formType.equals(PayrollFormType.MTA)) {
					report.generateFormMta((FormMTA)prtForm, fileNamePdf, cd.getId(), cd.getStatus().name());
				}
				else if (formType.equals(PayrollFormType.ACTRA_INTENT)) {
					report.generateFormActraIntent((FormActraIntent)prtForm, fileNamePdf);
				}
				else {
					EventUtils.logEvent(EventType.DATA_ERROR, "Unexpected form type in printDocument");
				}
				return fileNamePdf;
			}
			if (document != null) {
				Content content = ContentDAO.getInstance().findByDocId(document.getId(), document.getOriginalDocumentId()); // content of Master doc from Mongodb
				if (content != null) {
					log.debug("content id "+content.getDocId());
					String outputFileName = SessionUtils.getRealReportPath() + fileNamePdf;
					log.debug("output file=" + outputFileName);
					String xfdfData = null;
					PDFDoc in_doc = new PDFDoc(content.getContent());
					in_doc.initSecurityHandler();
					if (! formType.equals(PayrollFormType.OTHER)) {
						if (! XfdfContent.EMPTY_XFDF.equals(content.getXfdfData())) {
							Map<String, String> fieldValues = new HashMap<>();
							if (prtForm != null) {
								prtForm.fillFieldValues(cd, fieldValues);
							}
							xfdfData = StandardFormBean.replaceFormFields(content.getXfdfData(), fieldValues);
						}
					}
					else {
						XfdfContent xfdfContent = XfdfContentDAO.getInstance().findByContactDocId(cd.getId());
						log.debug("");
						if (xfdfContent != null) {
							log.debug("xfdfContent id "+xfdfContent.getContactDocId());
							xfdfData = xfdfContent.getContent();
						}
						else {
							log.debug("");
							in_doc.save(outputFileName, SDFDoc.e_linearized, null);
							log.debug("saved");
							in_doc.close();
							if (cd.getStatus() == ApprovalStatus.VOID) {
								// generates a new file (with a different name) that has the VOID watermark
								fileNamePdf = printVoidWatermark(fileNamePdf, outputFileName);
							}
							/*if (! isTransfer) {
								StandardFormBean.openDocumentInNewTab(fileNamePdf);
							}*/
							return fileNamePdf;
						}
					}
					if (xfdfData != null) {
						//log.debug(replacedXfdf);
						FDFDoc fdf_doc = FDFDoc.createFromXFDF(xfdfData);
						in_doc.fdfMerge(fdf_doc);
						eraseButtons(in_doc);
						in_doc.save(outputFileName, SDFDoc.e_linearized, null);
						log.debug("saved");
						in_doc.close();
						if (cd.getStatus() == ApprovalStatus.VOID) {
							// generates a new file (with a different name) that has the VOID watermark
							fileNamePdf = printVoidWatermark(fileNamePdf, outputFileName);
						}
						/*if (! isTransfer) {
							StandardFormBean.openDocumentInNewTab(fileNamePdf);
						}*/
						return fileNamePdf;
					}
					else if (! isTransfer) {
						// add logging event
						MsgUtils.addFacesMessage("ContactFormBean.Print.MissingXFDF",
								FacesMessage.SEVERITY_ERROR, document.getName());
					}
				}
				else if (! isTransfer) {
					MsgUtils.addFacesMessage("ContactFormBean.Print.MissingStdDocContent",
							FacesMessage.SEVERITY_ERROR,  document.getName());
				}
			}
			else if (! isTransfer) {
				MsgUtils.addFacesMessage("ContactFormBean.Print.MissingStdDoc",
						FacesMessage.SEVERITY_ERROR, formType.getFileName());
			}
		}
		catch (PDFNetException e) {
			EventUtils.logError("printWithXfdf failed, PDFNetException: ", e);
			MsgUtils.addGenericErrorMessage();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Erase/hide all custom buttons from the provided document. This also sets
	 * all fields to read-only. Designed for part of the process of printing
	 * custom documents, either for on-screen viewing, or for the Transfer
	 * process.
	 * <p>
	 * LS-1378 refactored this code into a separate method; it was part of
	 * CustomFormBean.actionPrint().
	 *
	 * @param inputDoc The custom document whose buttons need to be hidden.
	 * @throws PDFNetException
	 */
	public static void eraseButtons(PDFDoc inputDoc) throws PDFNetException {
		// Set read only flag for all the fields.
		// (placing ReadOnly in flag attribute (enableReadOnlyMode method) is not working therefore this method is used)
		FieldIterator pdfitr = inputDoc.getFieldIterator();
		while (pdfitr.hasNext()) {
			Field current = pdfitr.next();
			//log.debug("field name: " + current.getName());
			// Mark all fields as read-only
			current.setFlag(Field.e_read_only, true);
			// Hide on-document buttons for print-outs
			if (current.getName().startsWith("Btn")) {
				// following removes button graphic in Adobe reader & Foxit reader
				current.eraseAppearance();
					// however:
					// in Firefox: word "Off" is printed
					// in Chrome: button is still displayed; (however, buttons disappear in print preview)
				//current.setFlag(Field.e_button, false); // had no effect
				//current.setValue(" "); // had no effect, as did setValue("");
				//current.setValue((String)null); // no effect, still prints "Off"
				//Obj x = com.pdftron.sdf.Obj.__Create(com.pdftron.sdf.Obj.e_null, ""); // setValue caused NPE
				//Obj x = com.pdftron.sdf.Obj.__Create(com.pdftron.sdf.Obj.e_string, ""); // setValue crashed JVM!!
				//current.setValue(x);

				// Get the annotation from the Field.
				Annot fieldAnnot = new Annot(current.getSDFObj());
				Page btnPage = fieldAnnot.getPage();
				log.debug("Page = " + btnPage.getIndex());
				// check if the field is a valid annotation (i.e. must have Subtype key)
				if (current.getSDFObj().findObj("Subtype") != null && btnPage != null) {
					btnPage.annotRemove(fieldAnnot);
					log.debug(current.getName() + " - erased;");
				}
			}
		}
	}

	/**
	 * Print all the attachments for a given ContactDocument.
	 *
	 * @param cd The ContactDocument whose attachments should be printed.
	 * @param user Owner of the ContactDocument and attachments.
	 * @param fileList The list of files printed so far; this method will add
	 *            entries for any attachments printed.
	 * @param timestamp The timestamp String used in creating filenames for the
	 *            attachment PDFs.
	 * @param attachmentOutputFileName Relative file name (not fully qualified)
	 *            to use for the final merged PDF. This file will be created by
	 *            this method.
	 * @param badAttachmentCount beginning count of failed attachments.
	 * @return The given badAttachmentCount plus the number of attachments that
	 *         failed to print.
	 */
	private static Integer printAttachments(ContactDocument cd, User user,/* String fileNameText,*/
			List<String> fileList, String attachmentOutputFileName, Integer badAttachmentCount) {
		try {
			String filePrefix = PayrollFormType.OTHER.getReportPrefix();
			int attNum = 0;
			String realReportPath = SessionUtils.getRealReportPath();
			DateFormat df = new SimpleDateFormat("MMdd-HHmmss");
			String timestamp = df.format(new Date());
			for (Attachment attach : cd.getAttachments()) {
				attach = AttachmentDAO.getInstance().refresh(attach);
				attNum++; // Each attachment needs a unique filename
				if (attach.getMimeType().isPdf()) {
					String atcFileNamePdf = filePrefix + "Attachment_" + SessionUtils.getCurrentOrViewedProject().getId() + "_" + user.getId() + "_" + timestamp + attNum + ".pdf";
					boolean print = DocumentService.printAttachment(attach, atcFileNamePdf);
					if (print) {
						fileList.add(realReportPath + atcFileNamePdf);
						/*fileNameText += atcFileNamePdf + "<br/>";*/
					}
					else {
						badAttachmentCount++;
					}
				}
				else if (attach.getMimeType().isImage()) {
					String atcFileNamePdf = filePrefix + "Attachment_" + SessionUtils.getCurrentOrViewedProject().getId() + "_" + user.getId() + "_" + timestamp + attNum + ".pdf";
					boolean print = false;
					try {
						print = DocumentService.printImageAttachment(attach, atcFileNamePdf, false);
					}
					catch (Exception e) {
						EventUtils.logError(e);
						MsgUtils.addGenericErrorMessage();
					}
					if (print) {
						fileList.add(realReportPath + atcFileNamePdf);
						/*fileNameText += atcFileNamePdf + "<br/>";*/
					}
					else {
						badAttachmentCount++;
					}
				}
			}
			int finalPdf = PdfUtils.combinePdfs(fileList, (realReportPath + attachmentOutputFileName));
			log.debug("finalPdf = "  + finalPdf);
			if (finalPdf != 0) {
				MsgUtils.addFacesMessage("ContactFormBean.Print.AttachmentError", FacesMessage.SEVERITY_WARN);
			}
			for (String fileName : fileList) {
				File file = new File(fileName);
				String check = null;
				if (file.exists()) {
					if (file.delete()) {
						check = "YES";
					}
					else {
						check = "NO";
					}
				}
				log.debug("Delete file  = "  + check + ", file name = " + fileName);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return badAttachmentCount;
	}

	/** Utility method used to print a ContactDocument.
	 * @param cd
	 * @param user
	 * @param projectId
	 * @param timestamp
	 * @return
	 */
	public static String printContactDocument(ContactDocument cd, User user, String projectId, String timestamp, Production production) {
		user = UserDAO.getInstance().refresh(user);
		String docName = cd.getFormType().getReportPrefix();
		docName = docName.substring(0, docName.length() - 1);
		String fileNameNoPdf = production.getProdId() + "-"
				+ StringUtils.cleanFilename(production.getTitle() + projectId + "-" + user.getLastName()
						+ "-" + user.getFirstName() + "-" + docName + "-" + timestamp + "-" + cd.getId());
		String fileNamePdf = fileNameNoPdf + ".pdf";
		@SuppressWarnings("rawtypes")
		Form prtForm = FormService.findRelatedOrBlankForm(cd);
		fileNamePdf = printDocument(cd.getFormType(), cd, prtForm, fileNamePdf, true);
		return fileNamePdf;
	}

	/** This method is used to filter the ContactDocuments into groups.
	 * Type of grouping will is pre-defined. It is defined by the admin.
	 * @param type
	 * @param pdfGroupingMap
	 */
	public static  Map<Object, List<ContactDocument>> groupContactDocuments(PdfGroupingType type,
			List<ContactDocument> selectedContactDocuments) {
		Map<Object, List<ContactDocument>> pdfGroupingMap = new HashMap<>();
		Iterator<ContactDocument> itr = selectedContactDocuments.iterator();
		while (itr.hasNext()) {
			ContactDocument cd = itr.next();
			cd = ContactDocumentDAO.getInstance().refresh(cd);

			if (type == PdfGroupingType.EMPLOYEE) {
				Employment emp = cd.getEmployment();
				if (pdfGroupingMap.containsKey(emp) && pdfGroupingMap.get(emp) != null) {
					pdfGroupingMap.get(emp).add(cd);
				}
				else {
					List<ContactDocument> cdList = new ArrayList<>();
					cdList.add(cd);
					pdfGroupingMap.put(emp, cdList);
				}
			}
			else if (type == PdfGroupingType.DOC_TYPE) {
				PayrollFormType formType = cd.getFormType();
				if (pdfGroupingMap.containsKey(formType) && pdfGroupingMap.get(formType) != null) {
					pdfGroupingMap.get(formType).add(cd);
				}
				else {
					List<ContactDocument> cdList = new ArrayList<>();
					cdList.add(cd);
					pdfGroupingMap.put(formType, cdList);
				}
			}
			else if (type == PdfGroupingType.NONE) {
				if (pdfGroupingMap.containsKey(type) && pdfGroupingMap.get(type) != null) {
					pdfGroupingMap.get(type).add(cd);
				}
				else {
					List<ContactDocument> cdList = new ArrayList<>();
					cdList.add(cd);
					pdfGroupingMap.put(type, cdList);
				}
			}
		}
		return pdfGroupingMap;
	}

	/** Prints and merges all the ContactDocuments and Attachments of a group.
	 * @param projectId
	 * @param timestamp
	 * @param mapOfErorrCounts
	 * @param realReportPath
	 * @param contactDocumentList
	 * @param groupFileList
	 * @return
	 */
	public static Map<String, Integer> printGroupDocuments(String projectId, String timestamp, Map<String, Integer> mapOfErorrCounts, String realReportPath,
			List<ContactDocument> contactDocumentList, List<String> groupFileList, Production production, List<String> userNames) {
		Integer badAttachmentCount;

		//String attachmentFileNameText = "";
		boolean isTeamPayroll = false;
		PayrollService payrollService = production.getPayrollPref().getPayrollService();//LS-2289 get PayrollService to check TeamPayroll Clients
		if (payrollService != null) {
			isTeamPayroll = payrollService.getTeamPayroll();
		}
		String cdName;
		Iterator<ContactDocument> iter = contactDocumentList.iterator();
		while (iter.hasNext()) {
			ContactDocument cd = iter.next();
			String fileNamePdf = null;
			cd = ContactDocumentDAO.getInstance().refresh(cd);
			//LS-2289 TTCO document Transfer - do not email I9 forms
			if (!isTeamPayroll || !cd.getFormType().equals(PayrollFormType.I9)) {
				User user = cd.getContact().getUser();
				user = UserDAO.getInstance().refresh(user);
				badAttachmentCount = 0;
				cdName = cd.getId() + "_" + user.getFirstName() + "-" +user.getLastName() + "-" +
						cd.getEmployment().getOccupation() + "-" + cd.getFormType().toString() + "_";
				fileNamePdf = DocumentService.printContactDocument(cd, user, projectId, timestamp, production);
				if (fileNamePdf != null) {
					log.debug("");
					userNames.add(user.getLastNameFirstName());
					if (cd.getAttachments() != null && (! cd.getAttachments().isEmpty())) {
						String fileWithAttachments = "";
						fileWithAttachments = mergeAttachments(badAttachmentCount,
								 cd, fileNamePdf, user);
						groupFileList.add(realReportPath + fileWithAttachments);
						// TODO badAttachmentCount is never updated!
						if (badAttachmentCount != null && badAttachmentCount > 0) {
							cdName = cdName + cd.getAttachments().size();
							mapOfErorrCounts.put(cdName, badAttachmentCount);
						}
					}
					else {
						groupFileList.add(realReportPath + fileNamePdf);
					}
				}
				else {
					log.debug("");
					mapOfErorrCounts.put(cdName, null);
					cd.setChecked(false);
					iter.remove();
					}
			}
		}
		return mapOfErorrCounts;
	}

	/**
	 * Method merges an existing PDF (presumed to contain a printed
	 * ContactDocument) with the CD's attachments into a single PDF. If the
	 * ContactDocument has no attachments, the original filename is simply
	 * returned.
	 *
	 * @param badAttachmentCount Count of attachments that failed to print; NOT
	 *            CURRENTLY USED.
	 * @param cd the ContactDocument whose attachments are to be printed and
	 *            merged.
	 * @param fileNamePdf Relative file name (NOT fully qualified) of the file
	 *            that already contains the printed ContactDocument form.
	 * @param user Owner of the ContactDocument and attachments.
	 * @return Returns the relative file name of new resultant file after
	 *         merging, if attachments exists, otherwise it will return the name
	 *         of original printed ContactDocument file.
	 */
	public static String mergeAttachments(Integer badAttachmentCount, /*String attachmentFileNameText,*/
			ContactDocument cd, String fileNamePdf, User user) {
		log.debug("");
		if (cd.getAttachments() != null && (! cd.getAttachments().isEmpty())) {
			String fileWithAttachments = "";
			log.debug("Print and attach Attachments");
			List<String> attachmentFileList = new ArrayList<>();
			// LS-1211: ensure attachments have unique filenames per CD
			fileWithAttachments = fileNamePdf.replace(".pdf", "");
			fileWithAttachments = fileWithAttachments + "-A.pdf";
			attachmentFileList.add(SessionUtils.getRealReportPath()+fileNamePdf);
			badAttachmentCount = printAttachments(cd, user, /*attachmentFileNameText,*/ attachmentFileList, fileWithAttachments, badAttachmentCount);
			return fileWithAttachments;
		}
		else {
			return fileNamePdf;
		}

	}

	/**
	 * Creates a printed PDF report for the given PDF attachment.
	 *
	 * @param attachment The attachment representing the data (PDF) to be printed.
	 * @param fileNamePdf The unqualified file name to use for the generated PDF
	 *            file.
	 * @return True if the printed file was successfully generated. False
	 *         generally indicates a severe error, such as an Exception, or a
	 *         missing component.
	 */
	public static boolean printAttachment(Attachment attachment, String fileNamePdf) {
		try {
			log.debug("Attachment=" + attachment.getId());
			if (attachment != null) {
				AttachmentContent content = AttachmentContentDAO.getInstance().findByAttachmentId(attachment.getId());
				if (content != null) {
					log.debug("content id =" + content.getAttachmentId());
					String outputFileName = SessionUtils.getRealReportPath() + fileNamePdf;
					log.debug("output file =" + outputFileName);
					PDFDoc in_doc = new PDFDoc(content.getContent());
					in_doc.initSecurityHandler();
					in_doc.save(outputFileName, SDFDoc.e_linearized, null);
					log.debug("saved");
					in_doc.close();
					// Delete the extra xod zip files.
					File folder = new File(SessionUtils.getRealReportPath());
					File[] listOfFiles = folder.listFiles();
					boolean check;
					for (int i = 0; i < listOfFiles.length; i++) {
						if (listOfFiles[i].getName().contains("xod_")) {
							check = false;
							if (listOfFiles[i].exists()) {
								if (listOfFiles[i].delete()) {
									check = true;
								}
							}
							log.debug((check ? "file deleted; " : "WARNING: delete failed, ") + "file=" + listOfFiles[i].getName());
						}
					}
				return true;
				}
			}
			return false;
		}
		catch (PDFNetException e) {
			EventUtils.logError("printWithXfdf failed, PDFNetException: ", e);
			MsgUtils.addGenericErrorMessage();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return false;
	}

	/**
	 * Creates a printed PDF report for the given image attachment.
	 *
	 * @param attachment The attachment representing the data to be printed; the
	 *            document referenced by this CD is the basis of the print
	 *            output.
	 * @param fileNamePdf The unqualifed file name to use for the generated PDF
	 *            file; if null, a standard filename will be created.
	 * @param openReportPopupWindow True if the file should be opened in a
	 *            browser window.
	 * @return True if the printed file was successfully generated. False
	 *         generally indicates a severe error, such as an Exception, or a
	 *         missing component.
	 */
	public static boolean printImageAttachment(Attachment attachment, String fileNamePdf, boolean openReportPopupWindow) throws IOException {
		File file = null;
		boolean ret = false;
		try {
			log.debug("");
			if (attachment != null) {
				AttachmentContent content = AttachmentContentDAO.getInstance().findByAttachmentId(attachment.getId());
				if (content != null) {
					InputStream in = new ByteArrayInputStream(content.getContent());
					BufferedImage bImageFromConvert = ImageIO.read(in);
					attachment = AttachmentDAO.getInstance().refresh(attachment);
					String imageName = attachment.getId() + "_" + attachment.getName(); // make unique
					imageName = StringUtils.cleanFilename(imageName);
					imageName = SessionUtils.getRealPath("report") + File.separator + imageName;
					file = new File(imageName);
					ImageIO.write(bImageFromConvert, attachment.getMimeType().getExtension(), file);
					ReportBean report = ReportBean.getInstance();
					report.setOpenReportPopupWindow(openReportPopupWindow);
					report.printImageAttachment(attachment, fileNamePdf, imageName);
					boolean check = false;
					if (file.exists()) {
						if (file.delete()) {
							check = true;
							file = null;
						}
					}
					log.debug((check ? "file deleted; " : "WARNING: delete failed, ") + "file=" + imageName);
					ret = true;
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		finally {
			if (file != null && file.exists()) {
				file.delete();
			}
		}
		return ret;
	}

	/** Deletes the given list of files.
	 * @param files
	 */
	public static void deleteIntermediateFiles(List<String> files) {
		try {
			for (String fileNm : files) {
				File file = new File(fileNm);
				String check = null;
				if (file.exists()) {
					if (file.delete()) {
						check = "YES";
					}
					else {
						check = "NO";
					}
				}
				log.debug("Delete file  = "  + check + ", file name = " + fileNm);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Copies all the Onboarding packets from the old Project to new Project of a Production.
	 *
	 * @param prod The Production in which the Project will be added.
	 * @param oldProject The source Project.
	 * @param newProject The target project.
	 * @return false if an exception occurred copying any of the paths, otherwise true.
	 */
	@Transactional
	public static boolean copyOnboardingPackets(Production prod, Project oldProject, Project newProject) {
		log.debug("");
		boolean ret = true;
		if (prod != null) {
			List<Packet> packetList = PacketDAO.getInstance().findByProperty("project", oldProject);
			// Create new copies of Packets.
			for (Packet oldPacket : packetList) {
				log.debug("oldPacket = " + oldPacket.getName());
				Packet newPacket = new Packet();
				newPacket.setName(oldPacket.getName());
				newPacket.setDescription(oldPacket.getDescription());
				newPacket.setStatus(oldPacket.getStatus());
				newPacket.setProduction(prod);
				newPacket.setProject(newProject);
				newPacket.setDocumentList(new ArrayList<Document>());
				newPacket.getDocumentList().addAll(oldPacket.getDocumentList());
				Integer id = PacketDAO.getInstance().save(newPacket);
				ret &= (id != null);
				log.debug("Ret = " + ret);
			}
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	public static List<SignatureBox> parseXmlData(String data, List<SignatureBox> signatureBoxList) {
		signatureBoxList = new ArrayList<>();
		log.debug("data=`" + data + "`");
		SAXBuilder builder = new SAXBuilder();
		try {
			org.jdom.Document document = builder.build(new ByteArrayInputStream(data.getBytes()));
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
	}

	/**
	 * Given an Element of type "square", create a SignatureBox instance to
	 * match.
	 *
	 * @param subTag The "square" element.
	 * @param signatureBoxList The List to which the SignatureBox will be added.
	 */
	private static void parseSignatureBox(Element subTag, List<SignatureBox> signatureBoxList) {
		Integer[] points;
		SignatureBox signatureBox = new SignatureBox();
		signatureBox.setMaximumSignatures(1); // default; may adjust based on height
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

				// Set # of signatures that will fit based on box height:
				BigDecimal height = new BigDecimal(signatureBox.getY1() - signatureBox.getY2());
				height = height.divide(POINTS_PER_SIGNATURE, RoundingMode.HALF_UP);
				signatureBox.setMaximumSignatures(height.intValue());

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
	}

	/** Utility method used to generate the array of coordinates of the rectangle box
	 * @param coordinates comma separated string of coordinates
	 * @return String array
	 */
	private static Integer[] getRectangleCoordinates(String coordinates){
		Integer[] points = new Integer[4];
		Integer count = 0;

		for (String coordinate: coordinates.split(",")){
			points[count] = Math.round(Float.parseFloat(coordinate));
			count++;
		}
		for(Integer point : points) {
			log.debug("point = " + point);
		}
		return points;
	}

	public static Document getMasterDocument(PayrollFormType formType, Byte version) {
		log.debug(" MASTER DOCUMENT : " + formType.getFileNameWithVersion(version));
		// TODO Dummy code to avoid error message, to be changed.
		Document doc = null;
		doc = DocumentDAO.getInstance().findOneByProperty("name", formType.getFileNameWithVersion(version)); // get "Master" document
		if (doc == null) {
			doc = DocumentDAO.getInstance().findOneByProperty("name", formType.getFileName()); // get "Master" document
		}
		return doc;
	}

	/** Method used to put the value of given signature in appropriate signature field
	 * in given contact document's XFDf and updates the XFDF in database.
	 * @param cd ContactDocument
	 * @param evt SignedEvent
	 * @param signIndex Index of signature, which is used as the suffix of the field to be updated in the XFDF.
	 */
	@SuppressWarnings("rawtypes")
	public static void updateXfdf(ContactDocument cd, SignedEvent evt, int signIndex) {
		XfdfContent xfdfContent = XfdfContentDAO.getInstance().findByContactDocId(cd.getId());
		log.debug("");
		if (xfdfContent != null) {
			String signatureValue = evt.getSignedBy() + " \n#" + evt.getUuid();
			String dateValue =  new SimpleDateFormat("MM/dd/yyyy").format(evt.getDate());
			String signFieldName = null;
			String dateFieldName = null;
			String signButtonName = null;
			if ((evt.getType() == TimedEventType.SIGN || evt.getType() == TimedEventType.SUBMIT) && signIndex != 0) {
				signFieldName = Constants.EMP_SIGN_VALUE_FIELD;
				dateFieldName = Constants.EMP_DATE_VALUE_FIELD;
				signButtonName = Constants.BUTTON_EMP_SIGN;
			}
			else if (evt.getType() == TimedEventType.INITIAL && signIndex != 0) {
				signatureValue = evt.getInitials();
				signFieldName = Constants.EMP_INIT_VALUE_FIELD;
				dateFieldName = Constants.EMP_INIT_DATE_VALUE_FIELD;
				signButtonName = Constants.BUTTON_EMP_INIT;
			}
			else {
				signFieldName = Constants.APP_SIGN_VALUE_FIELD;
				dateFieldName = Constants.APP_DATE_VALUE_FIELD;
				signButtonName = Constants.BUTTON_APP_SIGN;
			}
			signFieldName += signIndex;
			dateFieldName += signIndex;
			signButtonName += signIndex;
			log.debug("signButtonName : " + signButtonName);
			log.debug("xfdfContent id " + xfdfContent.getContactDocId());
			String xfdf = insertValue(xfdfContent.getContent(), signFieldName, signatureValue, dateFieldName, dateValue);
			xfdf = hideSignButton(xfdf, signButtonName);
			// Update the database with revised XFDF
			if (xfdf != null) {
				XfdfContentDAO.getInstance().updateXFDFContent(xfdf, cd.getId());
				log.debug("Updated XFDFContent, contactDocId=" + cd.getId() + ", len=" + xfdf.length());
			}
		}

	}

	/** Method to insert the value of signature for custom document at specific position.
	 * @param xfdf Xfdf String to modify
	 * @param signFieldName Name of the signature field.
	 * @param signValue Value of the signature
	 * @param dateValue
	 * @param dateFieldName
	 * @return Updated XFDF String
	 */
	@SuppressWarnings("unchecked")
	public static String insertValue(String xfdf, String signFieldName, String signValue, String dateFieldName, String dateValue) {
		log.debug("fieldName = " + signFieldName);
		log.debug("value = " + signValue);
		//log.debug("data=`" + xfdf + "`");
		boolean didSignature = false;
		SAXBuilder builder = new SAXBuilder();
		try {
			org.jdom.Document document = builder.build(new ByteArrayInputStream(xfdf.getBytes()));
			log.debug("Root element : " + document.getRootElement().getName());
			Element rootNode = document.getRootElement();
			List<Element> tagList = rootNode.getChildren();
			for (Element tag : tagList) {
				if (tag.getName().equalsIgnoreCase("fields")) {
					List<Element> subTagList = tag.getChildren();
					// Search for the <field> tag that has the requested 'name=' attribute
					for (Element subTag : subTagList) {
						List<Attribute> attributeList = subTag.getAttributes();
						for(Attribute attr : attributeList) {
							log.debug("attribute name=" + attr.getName() + ", value=" + attr.getValue());
							//Signature
							if (attr.getName().equalsIgnoreCase("name") &&
									attr.getValue().equalsIgnoreCase(signFieldName)) {
								log.debug("FOUND SIGNATURE FIELD");
								// Have the right <field>, now find its <value> child and update it.
								// False, if no value sub tag is found else true if found.
								boolean found = false;
								List<Element> childTagList = subTag.getChildren();
								for (Element fieldSubTag : childTagList) {
									if (fieldSubTag.getName().equalsIgnoreCase("value")) {
										fieldSubTag.setText(signValue);
										log.debug("REPLACED SIGNATURE VALUE");
										didSignature = true;
										found = true;
										break; // done with this field
									}
								}
								// Solution for the problem, if "<field name="EmployeeSignature1" />"
								if (! found) {
									Element valueTag = new Element("value");
									valueTag.setText(signValue);
									log.debug("INSERTED NEW SIGNATURE VALUE");
									didSignature = true;
									childTagList.add(valueTag);
								}

							}
							//Date
							if (attr.getName().equalsIgnoreCase("name") &&
									attr.getValue().equalsIgnoreCase(dateFieldName)) {
								log.debug("FOUND SIGNATURE DATE FIELD");
								// Have the right <field>, now find its <value> child and update it.
								// False, if no value sub tag is found else true if found.
								boolean found = false;
								List<Element> childTagList = subTag.getChildren();
								for (Element fieldSubTag : childTagList) {
									if (fieldSubTag.getName().equalsIgnoreCase("value")) {
										fieldSubTag.setText(dateValue);
										log.debug("REPLACED DATE VALUE");
										found = true;
										break; // done with this field
									}
								}
								// Solution for the problem, if "<field name="EmployeeSignatureDate1" />"
								if (! found) {
									Element valueTag = new Element("value");
									valueTag.setText(dateValue);
									log.debug("INSERTED NEW DATE VALUE");
									childTagList.add(valueTag);
								}

							}
						}
					}
					break; // finished with <field> tags within <fields>, so we're done.
				}
			}
			if (! didSignature) {
				String msg = "Signature field not found after signing; field=" + signFieldName + ", signature=" + signValue;
				EventUtils.logEvent(EventType.INFO, msg);
				msg += "; data=`" + xfdf + "`";
				log.info(msg);
			}
			// we need the following to get the changed attributes back into a string
			xfdf = new XMLOutputter().outputString(document);
			//log.debug("data=`" + xfdf + "`");
		}
		catch (IOException io) {
			log.debug(io.getMessage());
		}
		catch (JDOMException jdomex) {
			log.debug(jdomex.getMessage());
		}
		return xfdf;
	}

	private final static String READ_ONLY_FLAG = "ReadOnly";
	private final static String PUSH_BUTTON_FLAG = "PushButton";

	/** Method used to set the fields of XFDF in read only mode.
	 * @param xfdfData
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static String enableReadOnlyMode(String xfdfData) {
		log.debug("");
		//log.debug("data=`" + xfdfData + "`");
		SAXBuilder builder = new SAXBuilder();
		try {
			org.jdom.Document document = builder.build(new ByteArrayInputStream(xfdfData.getBytes()));
			log.debug("Root element : " + document.getRootElement().getName());
			Element rootNode = document.getRootElement();
			List<Element> tagList = rootNode.getChildren();
			for (Element tag : tagList) {
				if (tag.getName().equalsIgnoreCase("pdf-info")) {
					List<Element> subTagList = tag.getChildren();
					// Search for the <field> tag that has the requested 'name=' attribute
					for (Element subTag : subTagList) {
						if (subTag.getName().equalsIgnoreCase("ffield") || (subTag.getName().equalsIgnoreCase("field"))) {
							Attribute flagAttr = subTag.getAttribute("flags");
							if (flagAttr != null && ! flagAttr.getValue().contains(PUSH_BUTTON_FLAG)) {
								// Have the 'flags' attribute field, now find its <value> child and update it.
								// LS-2467 Don't set "read-only" on PushButton fields, it disables them
								if (! flagAttr.getValue().contains(READ_ONLY_FLAG)) {
									String newValue = READ_ONLY_FLAG + " " + flagAttr.getValue();
									flagAttr.setValue(newValue);
								}
							}
						}
					}
				}
			}
			// we need the following to get the changed attribute back into a string
			xfdfData = new XMLOutputter().outputString(document);
			//log.debug("------------------");
			log.debug("data=`" + xfdfData + "`");
		}
		catch (IOException io) {
			log.debug(io.getMessage());
		}
		catch (JDOMException jdomex) {
			log.debug(jdomex.getMessage());
		}
		return xfdfData;
	}

	/** Method to insert the value of signature for custom document at specific position.
	 * @param xfdf Xfdf String to modify
	 * @param signButtonName Name of the Signature Button.
	 * @return Updated XFDF String
	 */
	@SuppressWarnings("unchecked")
	public static String hideSignButton(String xfdf, String signButtonName) {
		log.debug("signButtonName = " + signButtonName);
		//log.debug("data=`" + xfdf + "`");
		SAXBuilder builder = new SAXBuilder();
		try {
			org.jdom.Document document = builder.build(new ByteArrayInputStream(xfdf.getBytes()));
			log.debug("Root element : " + document.getRootElement().getName());
			Element rootNode = document.getRootElement();
			List<Element> tagList = rootNode.getChildren();
			for (Element tag : tagList) {
				if (tag.getName().equalsIgnoreCase("pdf-info")) {
					List<Element> subTagList = tag.getChildren();
					outer:
					for (Element subTag : subTagList) {
						if (subTag.getName().equalsIgnoreCase("widget")) {
							List<Attribute> attributeList = subTag.getAttributes();
							for(Attribute attr : attributeList) {
								if (attr.getName().equalsIgnoreCase("field") &&
										attr.getValue().startsWith(signButtonName)) {
									Attribute flagAttr = new Attribute("flags", "Hidden");
									log.debug("attributeList Size Before = " + attributeList.size());
									attributeList.add(flagAttr);
									log.debug("attributeList Size After = " + attributeList.size());
									break outer; // finished with <field> tags within <fields>, so we're done.
								}
							}
						}
					}
				}
			}
			// we need the following to get the changed attributes back into a string
			xfdf = new XMLOutputter().outputString(document);
			//log.debug("------------------");
			//log.debug("data=`" + xfdf + "`");
		}
		catch (IOException io) {
			log.debug(io.getMessage());
		}
		catch (JDOMException jdomex) {
			log.debug(jdomex.getMessage());
		}
		return xfdf;
	}

	/**
	 * Method to show the sign button.
	 *
	 * @param contactDocId ContactDocument id, which is key used to find existing XFDF data.
	 * @param signButtonName Prefix of name the Signature Button(s) to be
	 *            un-hidden.
	 * @param signFieldName Prefix of name of the signature field(s) to be
	 *            cleared.
	 * @param dateFieldName Prefix of name of the date field(s) to be cleared.
	 * @return Updated XFDF String
	 */
	@SuppressWarnings("unchecked")
	public static String showSignButton(Integer contactDocId, String signButtonName , String signFieldName, String dateFieldName) {
		SAXBuilder builder = new SAXBuilder();
		String xfdfString = "";
		try {
			XfdfContent xfdfContent = XfdfContentDAO.getInstance().findByContactDocId(contactDocId);
			if (xfdfContent != null) {
				xfdfString = xfdfContent.getContent();
				//log.debug("data=`" + xfdfString + "`");
				log.debug("signButtonName = " + signButtonName);
				//log.debug("data=`" + xfdf + "`");
				org.jdom.Document document = builder.build(new ByteArrayInputStream(xfdfString.getBytes()));
				log.debug("Root element : " + document.getRootElement().getName());
				Element rootNode = document.getRootElement();
				List<Element> tagList = rootNode.getChildren();
				for (Element tag : tagList) {
					if (tag.getName().equalsIgnoreCase("pdf-info")) {
						List<Element> subTagList = tag.getChildren();
						for (Element subTag : subTagList) {
							if (subTag.getName().equalsIgnoreCase("widget")) {
								List<Attribute> attributeList = subTag.getAttributes();
								outer:
								for(Attribute attr : attributeList) {
									if (attr.getName().equalsIgnoreCase("field") &&
											attr.getValue().startsWith(signButtonName)) {
										log.debug("<><><><>Field Attribute<><><><>" + attr.getValue());
										Iterator<Attribute> itr = attributeList.iterator();
										while (itr.hasNext()) {
											Attribute flagAttr = itr.next();
											log.debug("<><><><>Attribute<><><><>" + flagAttr.getName());
											if (flagAttr.getName().equalsIgnoreCase("flags")) {
												log.debug("<><><><>FOUND<><><><>");
												log.debug("attributeList Size Before = " + attributeList.size());
												attributeList.remove(flagAttr);
												log.debug("attributeList Size After = " + attributeList.size());
												break outer; // finished with <field> tags within <fields>, so we're done.
											}
											log.debug("<><><><>NOT FOUND<><><><>");
										}
									}
								}
							}
						}
					}

					if (tag.getName().equalsIgnoreCase("fields")) {
						List<Element> subTagList = tag.getChildren();
						// Search for the <field> tag that has the requested 'name=' attribute
						for (Element subTag : subTagList) {
							List<Attribute> attributeList = subTag.getAttributes();
							for(Attribute attr : attributeList) {
								log.debug("attribute name=" + attr.getName() + ", value=" + attr.getValue());
								//Signature
								if (attr.getName().equalsIgnoreCase("name") &&
										attr.getValue().startsWith(signFieldName)) {
									log.debug("FOUND SIGNATURE VALUE");
									// Have the right <field>, now find its <value> child and update it.
									for (Element fieldSubTag : (List<Element>)subTag.getChildren()) {
										if (fieldSubTag.getName().equalsIgnoreCase("value")) {
											fieldSubTag.setText(" ");
											break; // done with this field
										}
									}
								}
								//Date
								if (attr.getName().equalsIgnoreCase("name") &&
										attr.getValue().startsWith(dateFieldName)) {
									// Have the right <field>, now find its <value> child and update it.
									for (Element fieldSubTag : (List<Element>)subTag.getChildren()) {
										if (fieldSubTag.getName().equalsIgnoreCase("value")) {
											fieldSubTag.setText(" ");
											break; // done with this field
										}
									}
								}
							}
						}
						break; // finished with <field> tags within <fields>, so we're done.
					}
				}

				// we need the following to get the changed attributes back into a string
				xfdfString = new XMLOutputter().outputString(document);
				log.debug("------------------");
				log.debug("data=`" + xfdfString + "`");
			//	log.debug("------------------");
			}
		}
		catch (IOException io) {
			log.debug(io.getMessage());
		}
		catch (JDOMException jdomex) {
			log.debug(jdomex.getMessage());
		}
		return xfdfString;
	}

	/** Checks the suffix(Jr/Sr) present in the given text.
	 * @param originalText
	 * @return Returns the text with the correct format of suffix.
	 */
	public static String checkSuffix(String originalText) {
		log.debug("");
		String replacedText = null;
        replacedText = matchAndReplaceSuffix(originalText, Constants.LAST_NAME_SUFFIX_JUNIOR);
        if (replacedText != null) {
        	return replacedText;
        }
        else {
        	 replacedText = matchAndReplaceSuffix(originalText, Constants.LAST_NAME_SUFFIX_SENIOR);
        	 if (replacedText != null) {
             	return replacedText;
             }
        	 else {
        		 return originalText;
        	 }
        }
	}

	/** Method used to check the presence of given suffix in the given string
	 * and replaces that suffix with the correct format of suffix,
	 * changes the case of suffix, removes period and comma
	 * from the suffix in the given text/string.
	 * @param originalText
	 * @param suffix
	 * @return
	 */
	private static String matchAndReplaceSuffix(String originalText, String suffix) {
		log.debug(" " + suffix);
		String suffixWithSpace = " " + suffix + " ";
		String stringToMatch;
		String replacedText = null;
		originalText = originalText.trim();

		if (Pattern.compile(Pattern.quote(suffix), Pattern.CASE_INSENSITIVE).matcher(originalText).find()) {
			replacedText = originalText;
			if (originalText.equalsIgnoreCase(suffix) || originalText.equalsIgnoreCase(suffix + ".")) {
				return suffix;
			}
			//Below two else if cases will fix the case of suffix
			else if (Pattern.compile(Pattern.quote(suffixWithSpace), Pattern.CASE_INSENSITIVE).matcher(originalText).find()) {
				replacedText = originalText.replaceAll(("(?i)"+ suffixWithSpace), suffixWithSpace);
			}
			else if (Pattern.compile(Pattern.quote(" " + suffix), Pattern.CASE_INSENSITIVE).matcher(originalText).find()) {
				replacedText = originalText.replaceAll(("(?i)"+ " " + suffix), (" " + suffix));
			}

			stringToMatch = (", " + suffix); // take special care of ", Sr" it can be ", SrXXXX"
			if (replacedText.contains(stringToMatch + " ") || replacedText.contains(stringToMatch + ".") ||
					replacedText.endsWith(stringToMatch)) {
				replacedText = replacedText.replaceAll(("(?i)"+ stringToMatch), (" " + suffix));
			}

			stringToMatch = (" " + suffix + ".");
			if (replacedText.contains(stringToMatch) || replacedText.endsWith(stringToMatch)) {
				replacedText = replacedText.replaceAll(("(?i)"+ stringToMatch), suffixWithSpace);
			}
			return replacedText;
		}
		return null;
	}

	/** Method to disable the sign button for custom documents.
	 * @param xfdf Xfdf String to modify
	 * @param signButtonName Name of the Signature Button.
	 * @return Updated XFDF String
	 */
	/*@SuppressWarnings("unchecked")
	public static String disableSignButton(String xfdf, Integer contactDocId) {
		List<String> signButtonNameList = new ArrayList<>();
		if (mapOfContactDocButtonsToDisable != null && mapOfContactDocButtonsToDisable.get(contactDocId) != null) {
			signButtonNameList = mapOfContactDocButtonsToDisable.get(contactDocId);
			log.debug("signButtonName = " + signButtonNameList);
		}
		else {
			return xfdf;
		}
		//log.debug("data=`" + xfdf + "`");
		SAXBuilder builder = new SAXBuilder();
		String empSignButton = Constants.BUTTON_EMP_SIGN;
		String appSignButton = Constants.BUTTON_APP_SIGN;
		try {
			org.jdom.Document document = builder.build(new ByteArrayInputStream(xfdf.getBytes()));
			log.debug("Root element : " + document.getRootElement().getName());
			Element rootNode = document.getRootElement();
			List<Element> tagList = rootNode.getChildren();
			for (Element tag : tagList) {
				if (tag.getName().equalsIgnoreCase("pdf-info")) {
					List<Element> subTagList = tag.getChildren();
					outer:
					for (Element subTag : subTagList) {
						if (subTag.getName().equalsIgnoreCase("widget")) {
							boolean reqButton = false;
							List<Attribute> attributeList = subTag.getAttributes();
							for(Attribute attr : attributeList) {
								if (attr.getName().equalsIgnoreCase("field") &&
										((signButtonNameList.contains(empSignButton) && attr.getValue().startsWith(empSignButton)) ||
												(signButtonNameList.contains(appSignButton) && attr.getValue().startsWith(appSignButton)))) {
									log.debug("FOUND REQUIRED WIDGET ");
									reqButton = true;
									break;
								}
							}
							if (reqButton) {
								List<Element> subTagList2 = subTag.getChildren();
								log.debug("Subtag list size for widget before = " + subTagList2.size());
								Iterator<Element> itr = subTagList2.iterator();
								while (itr.hasNext()) {
									Element element = itr.next();
									if (element.getName().equalsIgnoreCase("actions")) {
										log.debug("disable button ");
										itr.remove();
										log.debug("Subtag list size for widget after = " + subTagList2.size());
										continue outer;
									}
								}
							}
						}
					}
				}
			}
			// we need the following to get the changed attributes back into a string
			xfdf = new XMLOutputter().outputString(document);
			log.debug("------------------");
			//log.debug("data=`" + xfdf + "`");
			//log.debug("------------------");
		}
		catch (IOException io) {
			log.debug(io.getMessage());
		}
		catch (JDOMException jdomex) {
			log.debug(jdomex.getMessage());
		}
		return xfdf;
	}*/

	/** See {@link #mapOfContactDocButtonsToDisable}*/
	public static Map<Integer, List<String>> getMapOfContactDocButtonsToDisable() {
		return mapOfContactDocButtonsToDisable;
	}
	/** See {@link #mapOfContactDocButtonsToDisable}*/
	public static void setMapOfContactDocButtonsToDisable(
			Map<Integer, List<String>> mapOfContactDocButtonsToDisable) {
		DocumentService.mapOfContactDocButtonsToDisable = mapOfContactDocButtonsToDisable;
	}

	/**
	 * Adds a VOID watermark text to every page of an existing PDF, creating a
	 * new PDF in the process.
	 *
	 * @param fileName - The existing PDF to which a water-mark will be added;
	 *            this may be specified with or without a path. This value will
	 *            be modified appropriately and returned as the newly-generated
	 *            (output) filename.
	 * @param fullFileName - The existing PDF to which a water-mark will be
	 *            added; specifies the fully-qualified name (complete path).
	 * @return The filename of the newly-created output file. If the 'fileName'
	 *         parameter is qualified, then this will be qualified; if
	 *         'fileName' is not qualified, this will not be, either.
	 */
	public static String printVoidWatermark(String fileName, String fullFileName) {
		WaterMark waterMark = new WaterMark(-1, -1, 216, TextDirection.HORIZONTAL, new Color(0xff0000)/*red*/, 30, false, "VOID");
		if (fileName.endsWith(".pdf")) {
			fileName = fileName.substring(0, fileName.length()-4);
		}
		fileName += WATERMARK_SUFFIX + ".pdf";
		String outputFullFilename = fullFileName;
		if (outputFullFilename.endsWith(".pdf")) {
			outputFullFilename = outputFullFilename.substring(0, outputFullFilename.length()-4);
		}
		outputFullFilename += WATERMARK_SUFFIX + ".pdf";
		PdfUtils.addWatermark(fullFileName, outputFullFilename, waterMark, true);
		File f = new File(fullFileName);
		if (f.exists()) {
			if ( ! f.delete()) {
				log.error("Report file delete failed for '" + f.getAbsolutePath() + "'");
				EventUtils.logError("Report file delete failed for '" + f.getAbsolutePath() + "'");
			}
			else {
				log.debug("Report file deleted OK: '" + f.getAbsolutePath() + "'");
			}
		}
		return fileName;
	}

	@SuppressWarnings("unchecked")
	public static String truncateXfdf(String xfdfString) {
		log.debug("");
		SAXBuilder builder = new SAXBuilder();
		try {
			if (! StringUtils.isEmpty(xfdfString)) {
				org.jdom.Document document = builder.build(new ByteArrayInputStream(xfdfString.getBytes()));
				log.debug("Root element : " + document.getRootElement().getName());
				Element rootNode = document.getRootElement();
				Iterator<Element> itr = rootNode.getChildren().iterator();
				while (itr.hasNext()) {
					Element tag = itr.next();
					log.debug(" TAG = " + tag.getName());
					if (! tag.getName().equalsIgnoreCase("fields")) {
						// remove all top-level children EXCEPT "fields"
						itr.remove();
					}
				}

				// we need the following to get the changed attributes back into a string
				xfdfString = new XMLOutputter().outputString(document);
				log.debug("------------------");
				log.debug("data=`" + xfdfString + "`");
			//	log.debug("------------------");
			}
		}
		catch (IOException io) {
			log.debug(io.getMessage());
		}
		catch (JDOMException jdomex) {
			log.debug(jdomex.getMessage());
		}
		return xfdfString;
	}

}
