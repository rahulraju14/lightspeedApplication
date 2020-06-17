package com.lightspeedeps.web.onboard.form;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.service.DocumentService;
import com.lightspeedeps.type.ApprovalStatus;
import com.lightspeedeps.type.PayrollFormType;
import com.lightspeedeps.type.TimedEventType;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.web.onboard.ContactFormBean;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.util.ApplicationScopeBean;
import com.lightspeedeps.web.view.Scroller;
import com.pdftron.common.PDFNetException;
import com.pdftron.fdf.FDFDoc;
import com.pdftron.pdf.*;
import com.pdftron.pdf.Page;
import com.pdftron.sdf.SDFDoc;

/** Backing bean for the non standard and user uploaded pdf forms
 * it takes care of the edit/save/cancel of pdfs and also uses web viewer tool
 * to showcase the pdf document
 *
 */
@ManagedBean
@ViewScoped
public class CustomFormBean extends StandardFormBean<CustomForm> implements Serializable {

	private static final long serialVersionUID = 9001584261294245584L;

	private static final Log log = LogFactory.getLog(CustomFormBean.class);

	/** Read-only parameter; set here; checked by LsFacesServlet and/or BaseServlet. */
	public final static String READ_ONLY = "$RO";

	/** boolean used to enable/disable the Read-only mode for the custom document. */
	private boolean readOnly = true;

	public CustomFormBean() {
		super("customFormBean");
		super.setAttributePrefix("ContactForm.");
	}

	public static CustomFormBean getInstance() {
		return (CustomFormBean) ServiceFinder.findBean("customFormBean");
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#getFormById(java.lang.Integer)
	 */
	@Override
	public CustomForm getFormById(Integer id) {
		return null;
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#getBlankForm()
	 */
	@Override
	public CustomForm getBlankForm() {
		return null;
	}

	/** Method used to preview the blank Custom form.
	 * @param doc Document to preview.
	 */
	public void previewBlankCustomDocument(Document doc) {
		String path = SessionUtils.getRealReportPath();
		try {
			if (doc != null) {
				User user = getvUser();
				user = UserDAO.getInstance().refresh(user);
				Project project = SessionUtils.getCurrentProject();
				if (project == null) {
					project = SessionUtils.getCurrentOrViewedProject();
					if (project == null) {
						project = SessionUtils.getProduction().getProjects().iterator().next();
					}
				}
				DateFormat df = new SimpleDateFormat("mmssSSS");
				String timestamp = df.format(new Date());

				String filePrefix = PayrollFormType.OTHER.getReportPrefix();
				String fileNamePdf = filePrefix + project.getId() + "_" + user.getId() + "_" + timestamp;

				// Pass just the file name when opening the document in the new tab.
				String outputFileNamePdf = fileNamePdf + ".pdf";
				fileNamePdf = path + fileNamePdf + ".pdf"; // make path absolute
				log.debug(fileNamePdf);

				Content content = ContentDAO.getInstance().findByDocId(doc.getId(), doc.getOriginalDocumentId());
				if (content != null) {
					log.debug("content id "+content.getDocId());
					PDFDoc in_doc = new PDFDoc(content.getContent());
					in_doc.initSecurityHandler();
					// Set read only flag for all the fields.
					// (placing ReadOnly in flag attribute (enableReadOnlyMode method) is not working therefore this method is used)
					FieldIterator pdfitr = in_doc.getFieldIterator();
					while (pdfitr.hasNext()) {
						Field current = (Field) pdfitr.next();
						log.debug("field name: " + current.getName());
						// Mark all fields as read-only
						current.setFlag(Field.e_read_only, true);
						// Hide on-document buttons for print-outs
						if (current.getName().startsWith("Btn")) {
							// following removes button graphic in Adobe reader & Foxit reader
							current.eraseAppearance();
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
					in_doc.save(fileNamePdf, SDFDoc.e_linearized, null);
					log.debug("saved");
					in_doc.close();
					openDocumentInNewTab(outputFileNamePdf);
				}
			}
			else {
				MsgUtils.addFacesMessage("ContactFormBean.Print.MissingStdDoc",
						FacesMessage.SEVERITY_ERROR, getContactDoc().getFormType().getFileName());
			}
		}
		catch (PDFNetException e) {
			EventUtils.logError("ContactFormBean actionPrint failed PDFNetException: ", e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Called when a particular document entry in the left-hand list
	 * has been clicked by the user.
	 * @param contactDocument The ContactDocument whose entry was clicked.
	 */
	@Override
	public void rowClicked(ContactDocument contactDocument) {
		rowClicked(contactDocument, ContactFormBean.getInstance());
	}

	/**
	 * Called when a particular document entry in the left-hand list has been
	 * clicked by the user. This method allows passing the contactFormBean
	 * instance, so we don't need to do a getInstance(). If we're called from
	 * the ContactFormBean constructor, this method must be used, as doing a
	 * getInstance() could result in an infinite loop of calls, and will
	 * therefore cause a 'cyclic reference to managedBean' error.
	 *
	 * @param contactDocument The ContactDocument whose entry was clicked.
	 * @param bean The ContactFormBean calling this method.
	 */
	@Override
	public void rowClicked(ContactDocument contactDocument, ContactFormBean bean) {
		setContactDoc(ContactDocumentDAO.getInstance().refresh(contactDocument));
		if (editMode) {
			readOnly = false;
		}
		else {
			readOnly = true;
			// handle unlock requirement for either Cancel or Save:
			if (ContactDocumentDAO.getInstance().unlock(contactDoc, getvUser().getId())) {
				// unlock updated db, refresh our copy
				setContactDoc(ContactDocumentDAO.getInstance().refresh(contactDocument));
			}
		}
		log.debug("readOnly : " + readOnly);
		try {
			displayOtherForm(contactDoc);
			// Code to disable sign buttons
			List<String> signButtonsToDisable = new ArrayList<>();
			if (bean.getEditAuth() || bean.getIsApprover()) {
				log.debug("<>");
				if (contactDoc.getSubmitable()) {
					log.debug("Disable BtnAppSign...");
					signButtonsToDisable.add(Constants.BUTTON_APP_SIGN);
				}
				if ((! contactDoc.getSubmitable()) ||
						(! contactDoc.getContact().equals(getvContact()))) {
					log.debug("Disable BtnEmpSign...; vContact=" + getvContact());
					signButtonsToDisable.add(Constants.BUTTON_EMP_SIGN);
					signButtonsToDisable.add(Constants.BUTTON_EMP_INIT);
					if (! contactDoc.getSubmitable() && (! bean.getMayApprove())) {
						for (ContactDocEvent evt : contactDoc.getContactDocEvents()) {
							if (evt.getType() == TimedEventType.APPROVE && evt.getUserAccount().equals(getvUser().getAccountNumber())) {
								log.debug("Disable BtnAppSign...");
								signButtonsToDisable.add(Constants.BUTTON_APP_SIGN);
								break;
							}
						}
					}
				}
			}
			else {
				log.debug("DISABLE BOTH BtnEmpSign... and BtnAppSign...");
				signButtonsToDisable.add(Constants.BUTTON_EMP_SIGN);
				signButtonsToDisable.add(Constants.BUTTON_APP_SIGN);
				signButtonsToDisable.add(Constants.BUTTON_EMP_INIT);
			}
			if (signButtonsToDisable.isEmpty()) {
				DocumentService.getMapOfContactDocButtonsToDisable().remove(contactDoc.getId());
			}
			else {
				DocumentService.getMapOfContactDocButtonsToDisable().put(contactDoc.getId(), signButtonsToDisable);
			}

			String attr = Constants.ATTR_CURRENT_MINI_TAB + "." + getAttributePrefix();
			Integer tab = SessionUtils.getInteger(attr);
			if (tab == null || tab == ContactFormBean.TAB_DOCUMENTS) {
				// Only open doc if on Forms mini-tab; avoid issuing webview call if on Attachments mini-tab.
				actionOpenDocument(contactDoc.getDocument().getId(), contactDoc.getDocument().getName(), contactDoc.getId());
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Method to update the XFDF data in a custom document, prior to displaying
	 * in the PDFTron webViewer. This is where a 'private' XFDF will be updated
	 * with the production and user's information the first time the custom
	 * document is viewed.
	 *
	 * @param pContactDoc The ContactDocument whose document will be displayed.
	 */
	private void displayOtherForm(ContactDocument pContactDoc) {
		try {
			Content content;
			log.debug("");
			Document document = pContactDoc.getDocument();
			//String fileName = pContactDoc.getDocument().getName();
			content = ContentDAO.getInstance().findByDocId(document.getId(), document.getOriginalDocumentId());
			XfdfContent xfdf = XfdfContentDAO.getInstance().findByContactDocId(pContactDoc.getId());
			if (content != null) {
				log.debug("content " + content.getDocId());
				String xfdfString = content.getXfdfData();
				if (xfdfString != null && (! xfdfString.equalsIgnoreCase(XfdfContent.EMPTY_XFDF))) {
					if (xfdf == null) { // First time - pre-fill form fields.
						insertXfdfContent(pContactDoc.getId(), updateXfdfString(xfdfString));
					}
					else {
						// for testing, use this to re-build individual XFDF from master with current user info
						// XfdfContentDAO.getInstance().updateXFDFContent(updateXfdfString(xfdfString), pContactDoc.getId());
					}
				}
				else {
					if (xfdf == null) {
						String xfdfContent = createXfdfFromPdf(pContactDoc.getDocument());
						insertXfdfContent(pContactDoc.getId(), xfdfContent);
					}
				}
			}
			//actionOpenDocument(docId, fileName, pContactDoc.getId());
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Utility method used to replace the enum field names with data from the
	 * appropriate sources. Typically this may include information from the User
	 * associated with the current contactDocument, and/or from the current
	 * Production or Project.
	 *
	 * @param xfdfString XFDF string to be updated, containing FormFieldType
	 *            enum names (constant text) to be replaced.
	 * @return updated XFDF String with the enums replaced by available
	 *         information or blanks.
	 */
	private String updateXfdfString(String xfdfString) {
		Map<String, String> fieldValues = new HashMap<>();
		CustomForm cf = new CustomForm();
		cf.fillFieldValues(contactDoc, fieldValues);
		String outputString = StandardFormBean.replaceFormFields(xfdfString, fieldValues);
		log.debug("output "+outputString);
		return outputString;
	}

	/**
	 * Action method used to load the web viewer with clicked contactDocument.
	 *
	 * @param docId clicked contactDocument's document id; this will be used by
	 *            our servlet to return the Document body to the WebViewer.
	 * @param fileName clicked contactDocument's Document name
	 * @param contactDocId clicked contactDocument id; this will be used by our
	 *            servlet to return the XFDF data to the WebViewer.
	 */
	private void actionOpenDocument(Integer docId, String fileName, Integer contactDocId) {
		if (fileName != null) {
			boolean useXod = ApplicationScopeBean.getInstance().getUseXod();
			String docIdStr = "" + docId;
			String cdIdStr = "" + contactDocId;
			if (readOnly) {
				log.debug("read only");
				docIdStr += READ_ONLY;
				cdIdStr = "'" + cdIdStr + READ_ONLY + "'";
			}
			fileName = "../../" + Constants.DOCUMENT_PSEUDO_DIRECTORY + "/" + docIdStr + "/" + fileName;
			String type = ",'pdf'"; // document-type parameter for WebViewer
			if (useXod) {
				type = ",'xod'";
				fileName += Constants.DOCUMENT_XOD_SUFFIX; // so servlet will send XOD, not PDF.
			}
			String javascriptCode = "webview('" + fileName + "',"+ cdIdStr + type +");";
			Scroller.addJavascript(javascriptCode);
		}
	}

	/* (non-Javadoc)
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#actionSave()
	 */
	@Override
	public String actionSave() {
		try {
			log.debug("SAVE CUSTOM DOCUMENT");
			editMode = false;
			//ContactDocumentDAO.getInstance().unlock(contactDoc, getvUser().getId());
			rowClicked(contactDoc);
			contactDoc.setLastUpdated(new Date());
			ContactDocumentDAO.getInstance().attachDirty(contactDoc);
			return super.actionSave();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
			return Constants.ERROR_RETURN;
		}
	}

	/** Method used to insert the xfdf string data into Mongo DB.
	 * @param id the contact document id
	 * @param xfdfString xfdf data string
	 */
	private void insertXfdfContent(Integer id, String xfdfString) {
		XfdfContent content = new XfdfContent(id, xfdfString);
		XfdfContentDAO.getInstance().insert(content);
		log.debug("id=" + id + ", len=" + xfdfString.length());
	}

	@Override
	public String actionPrint() {
		try {
			if (getContactDoc() != null) {
				printCustomDoc(getContactDoc());
			}
			else {
				MsgUtils.addFacesMessage("ContactFormBean.Print.MissingStdDoc",
						FacesMessage.SEVERITY_ERROR, getContactDoc().getFormType().getFileName());
			}

		}
		catch (PDFNetException e) {
			EventUtils.logError("ContactFormBean actionPrint failed PDFNetException: ", e);
			MsgUtils.addGenericErrorMessage();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Print the custom document specified by the given ContactDocument.
	 * @param cd The ContactDocument describing the document to be printed.
	 * @throws PDFNetException
	 */
	private void printCustomDoc(ContactDocument cd) throws PDFNetException {
		String path = SessionUtils.getRealReportPath();
		log.debug("Contact Doc: " + cd.getId());
		User user = contactDoc.getContact().getUser();
		user = UserDAO.getInstance().refresh(user);
		Project project = SessionUtils.getCurrentProject();
		if (project == null) {
			project = contactDoc.getProject();
			if (project == null) {
				project = SessionUtils.getCurrentOrViewedProject();
				if (project == null) {
					project = contactDoc.getProduction().getProjects().iterator().next();
				}
			}
		}
		DateFormat df = new SimpleDateFormat("mmssSSS");
		String timestamp = df.format(new Date());

		String fileNamePrefix = PayrollFormType.OTHER.getReportPrefix() + project.getId() + "_" + user.getId() + "_" + timestamp;
		String outputFileNamePdf = fileNamePrefix + "C.pdf"; // unqualified (relative) filename for custom report
		String fileNamePdf = path + fileNamePrefix + ".pdf"; // make path absolute
		log.debug(fileNamePdf + ", " + outputFileNamePdf);
		//TEST//String fileName = "../../" + Constants.DOCUMENT_PSEUDO_DIRECTORY + "/" + cd.getDocument().getId() + "/" + fileNamePdf;
		boolean printSign = true;

		XfdfContent xfdfContent = XfdfContentDAO.getInstance().findByContactDocId(cd.getId());
		Document document = cd.getDocument();
		Content content = ContentDAO.getInstance().findByDocId(document.getId(), document.getOriginalDocumentId());
		log.debug("content id "+content.getDocId());
		PDFDoc in_doc = new PDFDoc(content.getContent());
		in_doc.initSecurityHandler();

		if (xfdfContent != null) {
			log.debug("xfdfContent id " + xfdfContent.getContactDocId());
			String xfdfData = xfdfContent.getContent();
			if (xfdfData != null) {
				//log.debug("data=`" + xfdfData + "`");
				FDFDoc fdf_doc = FDFDoc.createFromXFDF(xfdfData);
				in_doc.fdfMerge(fdf_doc);
				DocumentService.eraseButtons(in_doc);
				in_doc.save(fileNamePdf, SDFDoc.e_linearized, null);
				log.debug("saved");
				in_doc.close();
				if (cd.getStatus() == ApprovalStatus.VOID) {
					fileNamePdf = DocumentService.printVoidWatermark(fileNamePdf, fileNamePdf);
				}
				// "printWithSignature" feature is not currently implemented
//				printSign = DocumentService.printWithSignature(cd, fileNamePdf, path + outputFileNamePdf);
//				if (! printSign) {
//					MsgUtils.addGenericErrorMessage();
//				}
				outputFileNamePdf = fileNamePrefix + ".pdf";
			}
			else {
				MsgUtils.addFacesMessage("ContactFormBean.Print.MissingXFDF",
						FacesMessage.SEVERITY_ERROR, cd.getDocument().getName());
			}
		}
		else {
			log.debug("");
			in_doc.save(fileNamePdf, SDFDoc.e_linearized, null);
			log.debug("saved");
			in_doc.close();
			if (cd.getStatus() == ApprovalStatus.VOID) {
				fileNamePdf = DocumentService.printVoidWatermark(fileNamePdf, fileNamePdf);
			}
			// "printWithSignature" feature is not currently implemented
//			printSign = DocumentService.printWithSignature(cd, fileNamePdf, path + outputFileNamePdf);
//			if (! printSign) {
//				MsgUtils.addGenericErrorMessage();
//			}
			outputFileNamePdf = fileNamePrefix + ".pdf";
		}
		if (printSign) {
			log.debug("");
			//openDocumentInNewTab(outputFileNamePdf);
			String fileWithAttachmentsIfExist = "";
			if (outputFileNamePdf != null) {
				log.debug("");
				fileWithAttachmentsIfExist = DocumentService.mergeAttachments(null, contactDoc, outputFileNamePdf, user);
				openDocumentInNewTab(fileWithAttachmentsIfExist);
			}
		}
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#actionEdit()
	 */
	@Override
	public String actionEdit() {
		setContactDoc(ContactDocumentDAO.getInstance().refresh(contactDoc));
		boolean isLocked = ContactDocumentDAO.getInstance().lock(contactDoc, getvUser());
		if (! isLocked) {
			PopupBean.getInstance().show(null, 0,
					"ContactFormBean.DocumentLocked.Title",
					"ContactFormBean.DocumentLocked.Text",
					"Confirm.OK", null); // no cancel button
			log.debug("edit prevented: locked by user #" + contactDoc.getLockedBy());
			return null;
		}
		editMode = true;
		rowClicked(contactDoc);
		return super.actionEdit();
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#actionCancel()
	 */
	@Override
	public String actionCancel() {
		editMode = false;
//		ContactDocumentDAO.getInstance().unlock(contactDoc, getvUser().getId());
		rowClicked(contactDoc);
		return super.actionCancel();
	}

	/**
	 * Extract the XFDF string from a PDF. This is used to create a "private
	 * XFDF" for a custom PDF that does not have a "master XFDF".
	 *
	 * @param doc The Document whose XFDF is to be extracted from the Document's
	 *            "content", which should be a PDF.
	 * @return The XFDF string.
	 */
	public String createXfdfFromPdf(Document doc) {
		String xfdfData = null;
		try {
			log.debug("Create XFDFContent from PDF");
			// PDFNet.initialize(); // Not needed here - done during startup in ApplicationScopeBean.
			Content ourDoc = null; // need to get Content from database (from Document)
			ourDoc = ContentDAO.getInstance().findByDocId(doc.getId(), doc.getOriginalDocumentId());
			// PDF to FDF
			PDFDoc pdfDoc = new PDFDoc(ourDoc.getContent());
			pdfDoc.initSecurityHandler();
			// both form fields and annotations
			log.debug("Extract both form fields and annotations to FDF.");
			FDFDoc fdfDoc = pdfDoc.fdfExtract(PDFDoc.e_both);
			// both form fields and annotations
			log.debug("Export both form fields and annotations from FDF to XFDF.");
			xfdfData = fdfDoc.saveAsXFDF();
			pdfDoc.close();
			log.debug("Done.");
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return xfdfData;
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#refreshForm()
	 */
	@Override
	public void refreshForm() {
		// no action necessary for custom forms.
	}

	/**See {@link #readOnly}. */
	public boolean getReadOnly() {
		return readOnly;
	}
	/**See {@link #readOnly}. */
	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	/**
	 * @param contactDocument
	 *
	 */
	public void recall(ContactDocument contactDocument) {
		//String signFieldName = "ApproverSignature";
		//String dateFieldName = "ApproverSignatureDate";
//		Integer evtIndex = 1;
		// TODO future - figure out how to handle multiple custom approver buttons.
		/* For now, un-hide ALL buttons/fields. If we need to select just one, we'd need
		 * something more than the code below. We'd need to loop through events (backwards or forwards?), looking
		 * for last Approve event, while figuring out if it was approval #1,2,3, etc. by counting prior Approve
		 * events, and also have to cancel out Approve/Reject or Approve/Recall pairs. E.g., consider
		 * an example where events are:
		 *    Submit, Appr1, Appr2, Recall(to 2), Appr2
		 * If approver2 is now doing Recall, we have to figure out that it's button/field Appr2 that has
		 * to be cleared.
		 */
//		for (ContactDocEvent evt : contactDocument.getContactDocEvents()) {
//			evtIndex += 1;
//			if (evt.getType() == TimedEventType.SUBMIT && evt.getUserAccount().equals(getvUser().getAccountNumber())) {
//				signFieldName += evtIndex;
//				dateFieldName += evtIndex;
				String updatedXfdf = DocumentService.showSignButton(contactDocument.getId(), Constants.BUTTON_APP_SIGN, Constants.APP_SIGN_VALUE_FIELD, Constants.APP_DATE_VALUE_FIELD);
				if (updatedXfdf != null) {
					XfdfContentDAO.getInstance().updateXFDFContent(updatedXfdf, contactDocument.getId());
					log.debug("Updated XFDFContent, contactDocId=" + contactDocument.getId() + ", len=" + updatedXfdf.length());
				}
				rowClicked(contactDocument);
//			}
//		}
	}

	/**
	 * Update the XFDF for the given CD due to a "Recall to employee" action
	 * being performed.  Unhide all the Employee signature buttons, and clear
	 * any employee signature text/date fields.
	 *
	 * @param contactDocument CD to be updated.
	 */
	public void recallToEmpl(ContactDocument contactDocument) {
		//String signFieldName = "EmployeeSignature";
		//String dateFieldName = "EmployeeSignatureDate";
		// TODO Do we need to select just one employee signature?
		// I think we want to clear all employee signatures; if there is more than one place, we don't
		// know why the employee is recalling or which one to re-do, so they need to re-sign in all places.

//		Integer evtIndex = 0;
//		for (ContactDocEvent evt : contactDocument.getContactDocEvents()) {
//			evtIndex += 1;
//			if ((evt.getType() == TimedEventType.SUBMIT || evt.getType() == TimedEventType.SIGN) &&
//					evt.getUserAccount().equals(getvUser().getAccountNumber())) {
//				// This may be true more than once if custom doc has multiple employee signatures
//				signFieldName = "EmployeeSignature" + evtIndex;
//				dateFieldName = "EmployeeSignatureDate" + evtIndex;
				String updatedXfdf = DocumentService.showSignButton(contactDocument.getId(), Constants.BUTTON_EMP_SIGN, Constants.EMP_SIGN_VALUE_FIELD, Constants.EMP_DATE_VALUE_FIELD);
				if (updatedXfdf != null) {
					XfdfContentDAO.getInstance().updateXFDFContent(updatedXfdf, contactDocument.getId());
					log.debug("Updated XFDFContent, contactDocId=" + contactDocument.getId() + ", len=" + updatedXfdf.length());
				}
//			}
//		}
		rowClicked(contactDocument);
	}

	/** Update the XFDF for the given CD due to a "Reject" action
	 * being performed.  Unhide all the Employee/Approver signature buttons, and clear
	 * any employee signature text/date fields.
	 *
	 * @param contactDocument Rejected ContactDocument
	 */
	public void reject(ContactDocument contactDocument) {
		String updatedXfdf = null;
		if (contactDocument.getSubmitable()) {
			updatedXfdf = DocumentService.showSignButton(contactDocument.getId(), Constants.BUTTON_EMP_SIGN, Constants.EMP_SIGN_VALUE_FIELD, Constants.EMP_DATE_VALUE_FIELD);
			if (updatedXfdf != null) {
				XfdfContentDAO.getInstance().updateXFDFContent(updatedXfdf, contactDocument.getId());
				log.debug("Updated XFDFContent, contactDocId=" + contactDocument.getId() + ", len=" + updatedXfdf.length());
			}
			updatedXfdf = DocumentService.showSignButton(contactDocument.getId(), Constants.BUTTON_APP_SIGN, Constants.APP_SIGN_VALUE_FIELD, Constants.APP_DATE_VALUE_FIELD);
		}
		else {
			int approveEvents = 0;
			int rejectRecallEvents = 0;
			for (int i = 0; i < contactDocument.getEvents().size(); i++) {
				@SuppressWarnings("rawtypes")
				SignedEvent evt = contactDocument.getEvents().get(i);
				log.debug("");
				if (evt.getType() == TimedEventType.REJECT || evt.getType() == TimedEventType.RECALL) {
					if (approveEvents > 0) {
						rejectRecallEvents = rejectRecallEvents + 1;
					}
				}
				else if (evt.getType() == TimedEventType.APPROVE) {
					approveEvents = approveEvents + 1;
				}
			}
			log.debug("Contact Document Events size : " + contactDocument.getEvents().size());
			log.debug("Approve Events : " + approveEvents);
			log.debug("Reject/ Recall Events : " + rejectRecallEvents);
			if (approveEvents <= rejectRecallEvents) { // at least one approval by this user
				updatedXfdf = DocumentService.showSignButton(contactDocument.getId(), Constants.BUTTON_APP_SIGN, Constants.APP_SIGN_VALUE_FIELD, Constants.APP_DATE_VALUE_FIELD);
			}
		}
		if (updatedXfdf != null) {
			XfdfContentDAO.getInstance().updateXFDFContent(updatedXfdf, contactDocument.getId());
			log.debug("Updated XFDFContent, contactDocId=" + contactDocument.getId() + ", len=" + updatedXfdf.length());
		}
		rowClicked(contactDocument);
	}

}
