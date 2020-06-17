package com.lightspeedeps.web.onboard;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.event.SelectEvent;
import org.icefaces.ace.model.table.RowState;
import org.icefaces.ace.model.table.RowStateMap;
import org.icefaces.ace.util.IceOutputResource;

import com.lightspeedeps.dao.AttachmentContentDAO;
import com.lightspeedeps.dao.AttachmentDAO;
import com.lightspeedeps.dao.ContactDocumentDAO;
import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.dao.WeeklyTimecardDAO;
import com.lightspeedeps.model.Attachment;
import com.lightspeedeps.model.AttachmentContent;
import com.lightspeedeps.model.ContactDocument;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.User;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.object.ImageResource;
import com.lightspeedeps.service.DocumentService;
import com.lightspeedeps.type.MimeType;
import com.lightspeedeps.type.PayrollFormType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.onboard.form.StandardFormBean;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.util.ApplicationScopeBean;
import com.lightspeedeps.web.util.HeaderViewBean;
import com.lightspeedeps.web.view.Scroller;
import com.lightspeedeps.web.view.View;
import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.sdf.SDFDoc;

@ManagedBean
@ViewScoped
public class AttachmentBean extends View implements Serializable {

	private static final long serialVersionUID = -1538289443700878958L;

	private static final Log log = LogFactory.getLog(AttachmentBean.class);

	/** Currently selected contact document. */
	private ContactDocument contactDocument;

	/** Currently selected WeeklyTimecard. */
	private WeeklyTimecard weeklyTimecard;

	/** Attachment list for the currently selected contact document. */
	private List<Attachment> attachmentList;

	/** the string variable used to hold the name of the document for the currently selected document chain */
	private String clickedAttachmentName;

	private Attachment attachment;

	/** The RowStateMap instance used to manage the clicked row on the data table */
	private RowStateMap stateMap = new RowStateMap();

	/** "Delete" action code for popup confirmation/prompt dialog. */
	private static final int ACT_DELETE = 22;

	/** Used to show message on Attachments view. */
	private String infoMessage = null;

	public AttachmentBean() {
		super("AttachmentBean.");
		Integer id = null;
		log.debug("contactDocument = "+ contactDocument);
		log.debug("weeklyTimecard = " + weeklyTimecard);
		if (contactDocument != null) {
			log.debug("");
			id = SessionUtils.getInteger(Constants.ATTR_PAYROLL_ATTACHMENT_ID);
		}
		else if (weeklyTimecard != null) {
			log.debug("");
			id = SessionUtils.getInteger(Constants.ATTR_TIMECARD_ATTACHMENT_ID);
		}
		if (id != null) {
			previewAttachment(AttachmentDAO.getInstance().findById(id));
		}
	}

	/** Used to return the instance of the AttachmentBean */
	public static AttachmentBean getInstance() {
		return (AttachmentBean)ServiceFinder.findBean("attachmentBean");
	}

	/** Method used to set the intial environment for the attachment tab.
	 * @param cd ContactDocument, not null if attachment tab is
	 * 		 for ContactDocument else it will be null for WeeklyTimecard.
	 * @param wtc WeeklyTimecard, not null if attachment tab is
	 * 		 for WeeklyTimecard else it will be null for ContactDocument.
	 */
	public void setDefaultAttachment(ContactDocument cd, WeeklyTimecard wtc) {
		log.debug("");
		Integer id = null;
		if (cd != null) {
			log.debug("");
			contactDocument = cd;
			weeklyTimecard = null;
			id = SessionUtils.getInteger(Constants.ATTR_PAYROLL_ATTACHMENT_ID);
		}
		else if (wtc != null) {
			log.debug("");
			weeklyTimecard = wtc;
			contactDocument = null;
			id = SessionUtils.getInteger(Constants.ATTR_TIMECARD_ATTACHMENT_ID);
		}
		createAttachmentList();
		Attachment atc = null;
		if (id != null) {
			log.debug("");
			atc = AttachmentDAO.getInstance().findById(id);
		}
		else if (id == null && getAttachmentList() != null && (! getAttachmentList().isEmpty())) {
			log.debug("");
			atc = getAttachmentList().get(0);
			if (cd != null) {
				log.debug("");
				SessionUtils.put(Constants.ATTR_PAYROLL_ATTACHMENT_ID, atc.getId());
			}
			else if (wtc != null) {
				log.debug("");
				SessionUtils.put(Constants.ATTR_TIMECARD_ATTACHMENT_ID, atc.getId());
			}
		}
	}

	/** Method creates the list of attachments to the current ContactDocument,
	 * and sets the transient isOwner value. */
	private void createAttachmentList() {
		try {
			log.debug("Create Attachment List");
			attachmentList = new ArrayList<>();
			if (contactDocument != null && contactDocument.getId() != null/* && isCdAttachment*/) {
				log.debug("");
				attachmentList = AttachmentDAO.getInstance().findByProperty("contactDocument", contactDocument);
			}
			else if (weeklyTimecard != null && weeklyTimecard.getId() != null/* && (! isCdAttachment)*/) {
				log.debug("");
				weeklyTimecard = WeeklyTimecardDAO.getInstance().refresh(weeklyTimecard);
				attachmentList = AttachmentDAO.getInstance().findByProperty("weeklyTimecard", weeklyTimecard);
			}
			if (attachmentList != null) {
				for (Attachment atc : attachmentList) {
					if ((atc.getUser().equals(SessionUtils.getCurrentUser()))) {
						atc.setIsOwner(true);
					}
					else {
						atc.setIsOwner(false);
					}
					log.debug("Is Owner = " + atc.getIsOwner());
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Listener used for row selection, sets the contactDocument with the currently selected row object . */
	public void listenRowClicked(SelectEvent evt) {
		try {
			log.debug("");
			Attachment clicked = (Attachment)evt.getObject();
			log.debug("Attachment : " + clicked.getId() + " , " + clicked.getName());
			viewAttachment(clicked);
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/**
	 * Used to refresh the attachment tab when switching viewed attachments.
	 * This will force a page refresh if necessary.
	 *
	 * @param attachmt The Attachment to be displayed.
	 */
	private void viewAttachment(Attachment attachmt) {
		if (! isTcAttachment(attachmt)) {
			log.debug("");
			contactDocument = ContactDocumentDAO.getInstance().refresh(contactDocument);
			contactDocument.getContactDocEvents();
			contactDocument.getEmpSignature();
			SessionUtils.put(Constants.ATTR_PAYROLL_ATTACHMENT_ID, attachmt.getId());
			// NOTE: the 'navigate' call will result in dispose() being called on this instance!
			// so far this is the only way to get WebViewer to show the new document
			String targetNav = HeaderViewBean.PAYROLL_START_FORMS; // usual "in production" page.
			if (SessionUtils.getNonSystemProduction() == null) { // User is in My Starts
				targetNav = HeaderViewBean.MYFORMS_MENU_DETAILS; // stay on My Starts page
			}
			HeaderViewBean.navigate(targetNav);

			// TODO PROBABBLY not enough - need contactFormBean to do rowClicked or similar before it switches to this tab,
			// since the user may click "Attach" on this tab, and think he's attaching to the displayed doc, but it won't,
			// contactFormBean may still be using some other CD. This is in the case where the user clicks on a paper-clip icon
			// on a row that's NOT the current row in the Start Forms list.

			// Since we're refreshing the page, the ContactFormBean will get recreated, too. We need
			// to set up its session variables so it selects the document we're displaying!
			SessionUtils.put(Constants.ATTR_ONBOARDING_CONTACTDOC_ID, contactDocument.getId());
			SessionUtils.put(Constants.ATTR_ONBOARDING_EMPLOYMENT_ID, contactDocument.getEmployment().getId());
			SessionUtils.put(Constants.ATTR_CONTACT_ID, contactDocument.getContact().getId());
		}
		else {
			//weeklyTimecard = WeeklyTimecardDAO.getInstance().refresh(weeklyTimecard);
			SessionUtils.put(Constants.ATTR_TIMECARD_ATTACHMENT_ID, attachmt.getId());
			SessionUtils.put(Constants.ATTR_CONTACT_ID, null);
			SessionUtils.put(Constants.ATTR_TIMECARD_ID, weeklyTimecard.getId());
			if (attachmt.getMimeType().isPdf()) { // uses WebViewer, force page refresh
				timecardNavigation();
			}
			else {
				previewAttachment(attachmt);
			}
		}
	}

	private void timecardNavigation() {
		Integer tcViewId = SessionUtils.getInteger(Constants.ATTR_TIMECARD_VIEW);
		Production prod = SessionUtils.getNonSystemProduction();
		String targetNav = "";
		if (prod == null) { //My Timecards
			log.debug("");
			if (tcViewId != null && tcViewId == 0) { // basic
				log.debug("");
				targetNav = HeaderViewBean.MYTIMECARDS_MENU_DETAILS;
			}
			else if (tcViewId != null && tcViewId == 1) { // full
				log.debug("");
				targetNav = HeaderViewBean.MYTIMECARDS_FULL_TC;
			}
		}
		else { // Payroll/Timecard
			log.debug("");
			if (tcViewId != null && tcViewId == 0) { // basic
				log.debug("");
				targetNav = HeaderViewBean.PAYROLL_TIMECARD;
			}
			else if (tcViewId != null && tcViewId == 1) { // full
				log.debug("");
				targetNav = HeaderViewBean.PAYROLL_FULL_TC;
			}
		}
		log.debug("Target Navigation = " + targetNav);
		HeaderViewBean.navigate(targetNav);
	}

	/**
	 * Action method used to load the web viewer with clicked contactDocument.
	 *
	 * @param attachmentId clicked contactDocument's attachment's id; this will be used by
	 *            our servlet to return the Document body to the WebViewer.
	 * @param fileName clicked contactDocument's Document name
	 */
	private void actionOpenDocument(Integer attachmentId, String fileName) {
		if (fileName != null) {
			log.debug("");
			boolean useXod = ApplicationScopeBean.getInstance().getUseXod();
			String attachmentIdStr = "" + attachmentId;
			fileName = "../../" + Constants.ATTACHMENT_PSEUDO_DIRECTORY + "/" + attachmentIdStr + "/" + fileName;
			String type = ",'pdf'"; // document-type parameter for WebViewer
			if (useXod) {
				type = ",'xod'";
				fileName += Constants.DOCUMENT_XOD_SUFFIX; // so servlet will send XOD, not PDF.
			}
			// Attachments have no saved annotations, so pass "-1" for contactDocId
			String javascriptCode = "webviewAttach('" + fileName + "'," + "-1" + type +");";
			Scroller.addJavascript(javascriptCode);
		}
	}

	/**
	 * Method used to preview the attachment
	 */
	public void previewAttachment(Attachment latestAttachment) {
		try {
			log.debug("");
			if (latestAttachment != null) {
				log.debug("latestAttactment = " + latestAttachment);
				if (attachment != null && ! attachment.equals(latestAttachment)) {
					RowState state = new RowState();
					state.setSelected(false);
					getStateMap().put(attachment, state);
				}
				setAttachment(latestAttachment);
				if (isTcAttachment(latestAttachment)) {
					log.debug("");
//					weeklyTimecard = WeeklyTimecardDAO.getInstance().refresh(weeklyTimecard);
					//setWeeklyTimecard(latestAttachment.getWeeklyTimecard());
					SessionUtils.put(Constants.ATTR_TIMECARD_ATTACHMENT_ID, latestAttachment.getId());
				}
				else {
					log.debug("");
					contactDocument = ContactDocumentDAO.getInstance().refresh(contactDocument);
					//setContactDocument(latestAttachment.getContactDocument());
					SessionUtils.put(Constants.ATTR_PAYROLL_ATTACHMENT_ID, latestAttachment.getId());
				}
				log.debug("");
				RowState state = new RowState();
				state.setSelected(true);
				log.debug("Attachment : " + attachment.getId() + " , " + attachment.getName());
				getStateMap().put(attachment, state);
				setClickedAttachmentName(attachment.getName());
				if (attachment.getIsPrivate() && (! (attachment.getUser().equals(SessionUtils.getCurrentUser())))) {
					log.debug("");
					infoMessage = "The selected document is private and may only be viewed by its owner.";
				}
				else if (attachment.getMimeType().isPdf()) {
					if (isTcAttachment(attachment)) {
						actionOpenDocument(attachment.getId(), attachment.getName());
					}
					else {
						log.debug("");
						actionOpenDocument(attachment.getId(), attachment.getName());
					}
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Action method for deleting attachment
	 * @return null navigation string.
	 */
	public String actionDelete() {
		try {
			if (attachment != null) {
				log.debug("attachment id to delete " + attachment.getId());
				PopupBean bean = PopupBean.getInstance();
				bean.show(this, ACT_DELETE, "AttachmentBean.DeleteAttachment.");
				bean.setMessage(MsgUtils.formatMessage("AttachmentBean.DeleteAttachment.Text", attachment.getName()));
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Called when the user clicks OK on the "Delete Document" pop-up dialog.
	 * @return null navigation string.
	 */
	private String actionDeleteOk() {
		try {
			if (attachment != null) {
				// Now delete the attachment
				log.debug("<>");
				boolean isTimecard = isTcAttachment(attachment);
				if (isTimecard) {
					weeklyTimecard = WeeklyTimecardDAO.getInstance().refresh(weeklyTimecard);
				}
				else {
					contactDocument = ContactDocumentDAO.getInstance().refresh(contactDocument);
				}
				attachment = AttachmentDAO.getInstance().refresh(attachment);
				MimeType oldDocMimeType = attachment.getMimeType();
				if (isTimecard) {
					weeklyTimecard.getAttachments().remove(attachment);
				}
				else {
					contactDocument.getAttachments().remove(attachment);
				}
				AttachmentDAO.getInstance().delete(attachment);
				setClickedAttachmentName(null);
				RowState state = new RowState();
				state.setSelected(false);
				stateMap.put(attachment, state);
				setAttachment(null);
				setAttachmentList(null);
				if (getAttachmentList() != null && (! getAttachmentList().isEmpty())) {
					attachment = getAttachmentList().get(0);
					state.setSelected(true);
					stateMap.put(attachment, state);
					if (attachment != null) {
						if (oldDocMimeType == MimeType.PDF) {
							viewAttachment(attachment);
						}
						else {
							previewAttachment(attachment);
						}
					}
				}
				else if (isTimecard) {
					timecardNavigation();
				}
				else {
					ContactFormBean.getInstance().setSelectedTab(ContactFormBean.TAB_DOCUMENTS);
					ContactFormBean.getInstance().refreshContactDoc(); // get rid of paperclip icon
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError("ContactFormBean delete Contact Document failed Exception: ", e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method for the "Print" button on the Attachment mini-tab,
	 * for printing any attachment.
	 *
	 * @return null navigation string
	 */
	public String actionPrint() {
		try {
			log.debug("");
			printAttachmment(attachment);
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	public static boolean printAttachmment(Attachment attachment) {
		try {
			log.debug("");
			if (attachment != null) {
				log.debug("");
				String filePrefix = PayrollFormType.OTHER.getReportPrefix();
				User user = attachment.getUser();
				user = UserDAO.getInstance().refresh(user);
				DateFormat df = new SimpleDateFormat("mmssSSS");
				String timestamp = df.format(new Date());

				// Note that the PDF filename pattern used is required by LsFacesServlet for access validation.
				// Looks like this: "some-text_<project-id>_<user-id>_more-text"
				String fileNamePdf = filePrefix + SessionUtils.getCurrentOrViewedProject().getId() + "_" + user.getId() + "_" + timestamp + ".pdf";
				AttachmentContent content = AttachmentContentDAO.getInstance().findByAttachmentId(attachment.getId());
				if (content != null) {
					log.debug("content id "+content.getAttachmentId());
					if (attachment.getMimeType().isPdf()) {
						log.debug("");
						String outputFileName = SessionUtils.getRealReportPath() + fileNamePdf;
						log.debug("output file=" + outputFileName);
						PDFDoc in_doc = new PDFDoc(content.getContent());
						in_doc.initSecurityHandler();
						in_doc.save(outputFileName, SDFDoc.e_linearized, null);
						log.debug("saved");
						in_doc.close();
						StandardFormBean.openDocumentInNewTab(fileNamePdf);
					}
					else if (attachment.getMimeType().isImage()) {
						log.debug("");
						DocumentService.printImageAttachment(attachment, fileNamePdf, true);
					}
				}
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
		return false;
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#confirmOk(int)
	 */
	@Override
	public String confirmOk(int action) {
		String res = null;
		switch(action) {
			case ACT_DELETE:
				actionDeleteOk();
				break;
			default:
				res = super.confirmOk(action);
				break;
		}
		return res;
	}

	/**
	 * Set up to show the attachments for the given ContactDocument.
	 * Called from ContactFormBean when an attachment icon is clicked.
	 * @param cd The ContactDocument whose attachments are to be displayed.
	 */
	public void viewAttachments(ContactDocument cd, Attachment attach, WeeklyTimecard wtc) {
		if (cd != null) {
			setContactDocument(cd);
		}
		else {
			setWeeklyTimecard(wtc);
		}
		createAttachmentList();
		getStateMap().clear();
		if (attach != null) {
			previewAttachment(attach);
		}
		else {
			previewAttachment(getAttachmentList().get(0));
		}
	}

	/** Used to display image attachment.
	 * @return
	 */
	public byte[] getShowImage() {
		log.debug("");
		if (attachment != null && attachment.getMimeType().isImage()) {
			log.debug("");
			AttachmentContent content = AttachmentContentDAO.getInstance().findByAttachmentId(attachment.getId());
			if (content != null) {
				log.debug("content id " + content.getAttachmentId());
				return content.getContent();
			}
		}
		return null;
	}

	/**
	 * Convert an attached Image to an IceOutputResource, to avoid bug in
	 * ICEfaces. (LS-987)
	 *
	 * @return An IceOutputResource for the current attachment, if it is of an
	 *         image type.
	 */
	@Transient
	public IceOutputResource getImageResource() {
		log.debug("");
		ImageResource imageResource = null;
		if (attachment != null && attachment.getMimeType().isImage()) {
			AttachmentContent content = AttachmentContentDAO.getInstance().findByAttachmentId(attachment.getId());
			if (content != null) {
				log.debug("content id " + content.getAttachmentId());
				imageResource = new ImageResource(attachment.getId().toString(), content.getContent(), "image/png");
				imageResource.setId(attachment.getId());
				imageResource.setTitle(attachment.getName());
				imageResource.setImage(null); // only needed when deleting an Image; N/A for attachments
			}
		}
		return imageResource;
	}

	/** Listener for the private checkbox on StartForms/attachments tab.
	 * @param evt
	 */
	public void listenChangePrivate(ValueChangeEvent evt) {
		try {
			Boolean newValue = (Boolean) evt.getNewValue();
			log.debug("Private =  " + newValue);
			if (attachment != null) {
				attachment.setIsPrivate(newValue);
				AttachmentDAO.getInstance().merge(attachment);
				attachmentList = null;
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	public boolean isTcAttachment(Attachment atc) {
		if (atc != null && atc.getWeeklyTimecard() != null) {
			return true;
		}
		else {
			return false;
		}
	}

	/** See {@link #contactDocument}*/
	public ContactDocument getContactDocument() {
		return contactDocument;
	}
	/** See {@link #contactDocument}*/
	public void setContactDocument(ContactDocument contactDocument) {
		if (contactDocument == null || contactDocument != this.contactDocument) {
			log.debug("");
			attachmentList = null;
			attachment = null;
			SessionUtils.put(Constants.ATTR_PAYROLL_ATTACHMENT_ID, null); // LS-987 clear 'last attachment'
			setClickedAttachmentName(null);
			if (contactDocument != null) {
				contactDocument.getEmpSignature();  // Fixed some LIEs when switching between attachments; LS-987
			}
		}
		this.contactDocument = contactDocument;
	}

	/** See {@link #weeklyTimecard}*/
	public WeeklyTimecard getWeeklyTimecard() {
		return weeklyTimecard;
	}
	/** See {@link #weeklyTimecard}*/
	public void setWeeklyTimecard(WeeklyTimecard weeklyTimecard) {
		if (weeklyTimecard == null || weeklyTimecard != this.weeklyTimecard) {
			log.debug("");
			weeklyTimecard = WeeklyTimecardDAO.getInstance().refresh(weeklyTimecard);
			attachmentList = null;
			attachment = null;
			SessionUtils.put(Constants.ATTR_TIMECARD_ATTACHMENT_ID, null);
			setClickedAttachmentName(null);
			if (weeklyTimecard != null) {
				weeklyTimecard.getTimecardEvents(); // Fixed some LIEs
			}
		}
		this.weeklyTimecard = weeklyTimecard;
	}

	/** See {@link #attachmentList}*/
	public List<Attachment> getAttachmentList() {
		if (attachmentList == null) {
			log.debug("");
			createAttachmentList();
		}
		return attachmentList;
	}
	/** See {@link #attachmentList}*/
	public void setAttachmentList(List<Attachment> attachmentList) {
		this.attachmentList = attachmentList;
	}

	/** See {@link #clickedAttachmentName}*/
	public String getClickedAttachmentName() {
		return clickedAttachmentName;
	}
	/** See {@link #clickedAttachmentName}*/
	public void setClickedAttachmentName(String clickedAttachmentName) {
		this.clickedAttachmentName = clickedAttachmentName;
	}

	/** See {@link #attachment}*/
	public Attachment getAttachment() {
		return attachment;
	}
	/** See {@link #attachment}*/
	public void setAttachment(Attachment attachment) {
		this.attachment = attachment;
	}

	/**See {@link #stateMap}. */
	public RowStateMap getStateMap() {
		return stateMap;
	}
	/**See {@link #stateMap}. */
	public void setStateMap(RowStateMap stateMap) {
		this.stateMap = stateMap;
	}

	/**See {@link #infoMessage}. */
	public String getInfoMessage() {
		return infoMessage;
	}
	/**See {@link #infoMessage}. */
	public void setInfoMessage(String infoMessage) {
		this.infoMessage = infoMessage;
	}

}
