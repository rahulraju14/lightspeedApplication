/**
 * File: WeeklyBatchService.java
 */
package com.lightspeedeps.service;

import java.io.*;
import java.util.*;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import com.lightspeedeps.dao.ProjectDAO;
import com.lightspeedeps.dao.WeeklyBatchDAO;
import com.lightspeedeps.dao.WeeklyTimecardDAO;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.message.HttpUtils;
import com.lightspeedeps.model.*;
import com.lightspeedeps.port.DelimitedExporter;
import com.lightspeedeps.port.FlatExporter;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.payroll.*;
import com.lightspeedeps.web.payroll.EmailBatchPopupBean;
import com.lightspeedeps.web.view.View;

/**
 * Contains methods with application logic related to timecard batch
 * (WeeklyBatch) management.
 */
public class WeeklyBatchService extends TransferService<WeeklyBatch, WeeklyTimecard> {
	private static final Log log = LogFactory.getLog(WeeklyBatchService.class);

	/** The filename of a report supplied by a caller rather than generated
	 * during the transfer process. */
	String suppliedReport = null;

	public WeeklyBatchService() {
//		log.debug("");
		setFtpPrefix(FTP_TIMECARD_PREFIX);
	}

	public static WeeklyBatchService getInstance() {
		return new WeeklyBatchService();
	}

	/**
	 * Update the status of one or more batches by sending a query (or multiple
	 * queries) to the appropriate payroll service.
	 *
	 * @param batches A Collection of WeeklyBatch objects to be updated.
	 * @return True iff the status of every batch was successfully updated.
	 */
	public static boolean updateStatusFromPayroll(Collection<WeeklyBatch> batches) {
		WeeklyBatchService service= new WeeklyBatchService();
		boolean ret = service.updateBatchStatus(batches);
		return ret;
	}

	/**
	 * Update the status of one or more batches by sending a query (or multiple
	 * queries) to the appropriate payroll service.
	 *
	 * @param batches A Collection of WeeklyBatch objects to be updated.
	 * @return True iff the status of every batch was successfully updated.
	 */
	private boolean updateBatchStatus(Collection<WeeklyBatch> batches) {
		boolean bRet = false;
		try {
			Production prod = SessionUtils.getNonSystemProduction();
			if (prod != null) {
				if (service != null) {
					switch (service.getSendBatchMethod()) {
						case AUTH_POST:
							if (sendAuthQuery(batches)) {
								bRet = true;
							}
							break;
						case ABS_FILE:
						case CSV_FILE:
						case PDF_ONLY:
						case TEAM_FILE:
						case TEAM_FTP:
						case TEAM_PDF:
							MsgUtils.addFacesMessage("WeeklyBatch.Status.NotAvailableOnline", FacesMessage.SEVERITY_ERROR);
							break;
						case CREW_CARDS:
							MsgUtils.addFacesMessage("WeeklyBatch.Status.NotAvailableOnline", FacesMessage.SEVERITY_ERROR);
							break;
					}
				}
				else {
					MsgUtils.addFacesMessage("WeeklyBatch.Transmit.NoService", FacesMessage.SEVERITY_ERROR);
				}
			}
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("WeeklyBatch.Status.Error", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(e);
		}
		return bRet;
	}

	/**
	 * Send a batch status query to the given payroll service for one or more
	 * batches. An authorization credential will be retrieved first, then used
	 * for the status queries.
	 *
	 * @param service The PayrollService to contact.
	 * @param batches The Collection of WeeklyBatch objects, each of whose
	 *            status should be queried.
	 * @return True iff we successfully received a status for all the batches in
	 *         the Collection.
	 */
	private boolean sendAuthQuery(Collection<WeeklyBatch> batches) {
		boolean bRet = true;
		PayrollStatus status = null;
		String authToken = HttpUtils.getAuthToken(service.getAuthUrl(), service.getLoginName(), service.getPassword());
		if (authToken != null) {
			String url;
			url = service.getStatusUrl();
			for (WeeklyBatch batch : batches) {
				// don't let formatText format id's - it adds commas!
				String qUrl = MsgUtils.formatText(url, ""+batch.getId());
				status = HttpUtils.sendBatchQuery(qUrl, authToken);
				if (status == null) {
					MsgUtils.addFacesMessage("WeeklyBatch.Status.Error", FacesMessage.SEVERITY_ERROR);
					bRet = false;
					break;
				}
				else if (status == PayrollStatus.UNAVAILABLE) {
					MsgUtils.addFacesMessage("WeeklyBatch.Status.Unavailable", FacesMessage.SEVERITY_ERROR);
					bRet = false;
					break;
				}
				else if (status == PayrollStatus.UNKNOWN) {
					if (batch.getSent() == null) {
						// Expected: batch is unknown since we haven't sent it yet
						MsgUtils.addFacesMessage("WeeklyBatch.Status.Unsent", FacesMessage.SEVERITY_ERROR, batch.getName());
					}
					else { // problem: we sent it, but status came back "Unknown"
						MsgUtils.addFacesMessage("WeeklyBatch.Status.UnknownSent", FacesMessage.SEVERITY_ERROR, batch.getName());
						updateStatus(batch, status);
					}
				}
				else { // Normal status return
					updateStatus(batch, status);
				}
			}
		}
		else {
			MsgUtils.addFacesMessage("WeeklyBatch.TransmitError.AuthFailed", FacesMessage.SEVERITY_ERROR);
		}
		return bRet;
	}

	/**
	 * Update the status field(s) of a WeeklyBatch based on the value of the
	 * supplied PayrollStatus. The changes will be applied to the database.
	 *
	 * @param batch The WeeklyBatch to be updated.
	 * @param status The new PayrollStatus, typically retrieved from the payroll
	 *            service.
	 */
	private void updateStatus(WeeklyBatch batch, PayrollStatus status) {
		switch (status) {
			case PAID:
				switch(batch.getStatus()) { // NOTE that we fall thru several cases!
					default: // Did not reach EDIT_RCVD status, update Sent, Edit, and Final counts
						updateSentTimecards(batch);
						batch.setTimecardsEdit(batch.getTimecardsSent());
					case EDIT_RCVD: // Never received FINAL status, update Final count
						batch.setTimecardsFinal(batch.getTimecardsEdit());
					case FINAL: // best/normal case, we had FINAL status prior to PAID
						batch.setStatus(WeeklyBatchStatus.PAID);
						batch.setTimecardsPaid(batch.getTimecardsFinal());
					case PAID: //  normal case (no change in status)
						break;
				}
				break;
			case FINAL: // NOTE that we fall thru several cases!
				switch(batch.getStatus()) {
					default: // Did not reach EDIT_RCVD status, update Sent & Edit counts
						updateSentTimecards(batch);
						batch.setTimecardsEdit(batch.getTimecardsSent());
					case EDIT_RCVD: // normal case
						batch.setStatus(WeeklyBatchStatus.FINAL);
						batch.setTimecardsFinal(batch.getTimecardsEdit());
					case FINAL: // normal case (no change in status)
						break;
				}
				break;
			case EDIT:
				switch(batch.getStatus()) {
					default: // Never marked in SENT status, update Sent counts
						updateSentTimecards(batch);
					case IN_PROGRESS: // normal case
					case SENT: // normal case
						batch.setStatus(WeeklyBatchStatus.EDIT_RCVD);
						batch.setTimecardsEdit(batch.getTimecardsSent());
					case EDIT_RCVD: // normal case (no change in status)
						break;
				}
				break;
			case IN_PROGRESS:
				batch.setStatus(WeeklyBatchStatus.IN_PROGRESS);
				updateSentTimecards(batch);
				break;
			case RECEIVED:
				batch.setStatus(WeeklyBatchStatus.SENT);
				updateSentTimecards(batch);
				break;
			case UNKNOWN:
				// The batch is not recognized by the payroll service
				batch.setStatus(WeeklyBatchStatus.UNKNOWN);
				batch.setTimecardsPaid(0);
				batch.setTimecardsEdit(0);
				break;
			case UNAVAILABLE:
				// No status received - do not change batch status
				break;
		}
		WeeklyBatchDAO.getInstance().attachDirty(batch);
	}

	/**
	 * Count the number of timecards in a given batch that are marked as "sent",
	 * and which have not been updated since the time they were sent. That is,
	 * the number of timecards which should match the payroll service's copy.
	 * This count is then set in the batch's timecardsSent field, but only if it
	 * is an increase -- we never lower the timecardsSent value.
	 *
	 * @param batch The batch whose timecards will be counted, and whose
	 *            "sent count" may be udpated.
	 */
	private void updateSentTimecards(WeeklyBatch batch) {
		int items = 0;
		if (batch.getSent() == null) {
			batch.setSent(new Date());
		}
		for (WeeklyTimecard wtc : batch.getTimecards()) {
			if ((wtc.getTimeSent() != null) && wtc.getUpdated().before(wtc.getTimeSent())) {
				items++;
			}
		}
		if (batch.getTimecardsSent() < items) {
			batch.setTimecardsSent(items);
		}
	}

	/**
	 * Set up final parameters and call the service to transmit (or email) one
	 * or more batches and/or set of timecards to the payroll service. Either
	 * 'batches' or 'batch' should be null; the non-null one will be processed.
	 *
	 * @param method The ServiceMethod to be used for transmitting the
	 *            batch(es).
	 * @param proj The Project that relates to the given timecards; only
	 *            meaningful for a Commercial production.
	 * @param transmitBean The bean holding the options in effect; typically the
	 *            pop-up bean for the transmit options dialog box.
	 * @param batches The collection of batches to be sent; should be null if
	 *            'batch' and 'cards' are not null.
	 * @param batch A single batch from which the timecards in 'cards' have been
	 *            selected. This will be ignored if 'batches' is not null.
	 * @param cards The timecards to be sent, when an entire batch was not
	 *            selected. This will be ignored if 'batches' is not null.
	 * @param reportFile The name of a report file to be included in the
	 *            transmitted data; this is used by Tours productions to send a
	 *            Timesheet report. If null, PDFs of the timecards will be
	 *            generated and included instead; this is the usual procedure
	 *            for all non-Tours productions.
	 * @return A count of the number of batches (if 'batches' is not null), or
	 *         the number of timecards (if 'batch' is not null) transmitted. If
	 *         zero, no transfer occurred.
	 */
	public int transmitBatch(ServiceMethod method, Project proj, EmailBatchPopupBean transmitBean,
			List<WeeklyBatch> batches, WeeklyBatch batch, Collection<WeeklyTimecard> cards, String reportFile) {

		suppliedReport = reportFile;
		ReportStyle reptStyle = ReportStyle.TC_FULL;
		if (SessionUtils.getProduction().getPayrollPref().getUseModelRelease()) {
			reptStyle = ReportStyle.TC_MODEL;
		}
		else if (SessionUtils.getProduction().getType().isAicp()) {
			reptStyle = ReportStyle.TC_AICP;
		}
		ExportType expType = ExportType.CREW_CARDS;
		boolean chkOptions = true;
		boolean email = false;
		String msgPrefix = "File.";

		switch (method) {
			case AUTH_POST:
				expType = ExportType.JSON;
				chkOptions = false;
				msgPrefix = "Send.";
				break;
			case CREW_CARDS:
				expType = ExportType.CREW_CARDS;
				break;
			case ABS_FILE:
				expType = ExportType.ABS;
				break;
			case CSV_FILE:
				expType = ExportType.PAYROLL;
				break;
			case PDF_ONLY:
				expType = ExportType.NONE;
				break;
			case TEAM_FILE:
				expType = ExportType.FULL_TAB;
				break;
			case TEAM_FTP:
				chkOptions = false; // ignore user-selected options
				email = true; // 10/6/16 email PDFs along with FTP of data
				expType = ExportType.FULL_TAB;
				msgPrefix = "Send.";
				reptStyle = ReportStyle.TC_AICP_TEAM;
				break;
			case TEAM_PDF:
				expType = ExportType.NONE;
				reptStyle = ReportStyle.TC_AICP_TEAM;
				break;
		}
		if (chkOptions && transmitBean != null) {
			email = true;
			msgPrefix = "Email.";
			expType = transmitBean.getExportTypeEnum();
			reptStyle = transmitBean.getReportStyleEnum();
			if (reptStyle == ReportStyle.TC_AICP && method.isTeam()) {
				reptStyle = ReportStyle.TC_AICP_TEAM;
			}
		}

		if (reportFile != null) { // only used for Tours
			reptStyle = ReportStyle.TIMESHEET;
		}

		int count;
		boolean success;
		if (batches != null) {
			msgPrefix = "WeeklyBatch.Transmit." + msgPrefix;
			success = transmit(batches, proj, expType, reptStyle, email, false);
			count = batches.size();
		}
		else {
			msgPrefix = "WeeklyBatch.TransmitTC." + msgPrefix;
			success = transmit(batch, cards, false, proj, expType, reptStyle, email);
			count = cards.size();
		}

		if (success) {
			if (! production.getType().isTours()) { // don't issue message for tours, it's redundant
				MsgUtils.addFacesMessage(msgPrefix + "SentOk", FacesMessage.SEVERITY_INFO, count);
			}
		}
		else {
			MsgUtils.addFacesMessage(msgPrefix + "Failed", FacesMessage.SEVERITY_ERROR);
		}
		return count;
	}

	/**
	 * Send a collection of timecards from a single WeeklyBatch, using the
	 * appropriate method for the current Production.
	 *
	 * @param batch The WeeklyBatch that contains the timecards to transmit.
	 * @param timecards The collection of timecards to be sent.
	 * @param remove True if the timecards passed in the collection are being
	 *            REMOVED from the given batch. The payroll service will be
	 *            notified of their removal.
	 * @param proj The Project associated with the timecards.
	 * @param type The type (format) of export file to be generated.
	 * @param style The style (layout) of timecard PDF report to be
	 *            generated.
	 * @param email True iff the generated files should be emailed to the
	 *            payroll service; if false, the files are delivered to the
	 *            current user by opening a browser window on the file.
	 * @return Either a file location, a normal response String, or null. If
	 *         null, then the transmit failed.
	 */
	public boolean transmit(WeeklyBatch batch, Collection<WeeklyTimecard> timecards,
			boolean remove, Project proj, ExportType type, ReportStyle style,
			boolean email) {

		boolean ret = false;
		try {
			init(proj, type, style, email);
			if (service != null) {
				batch = WeeklyBatchDAO.getInstance().refresh(batch);
				WeeklyBatchDAO.getInstance().evict(batch);
				Set<WeeklyTimecard> fresh = new HashSet<>();
				for (WeeklyTimecard tc : timecards) {
					tc = WeeklyTimecardDAO.getInstance().refresh(tc);
					if (remove) {
						tc.setBatchStatus(WeeklyTimecard.BATCH_STATUS_REMOVED);
					}
					fresh.add(tc);
				}
				batch.setTimecards(fresh);
				if (! prepareBatch(batch)) {
//						MsgUtils.addFacesMessage("WeeklyBatch.Transmit.NoChanges", FacesMessage.SEVERITY_ERROR);
				}
				else {
					log.debug("sending selected timecards from batch " + batch.getName());
					ret = createAndSendFiles(batch);
				}
				batch = WeeklyBatchDAO.getInstance().findById(batch.getId()); // get fresh copy
			}
			else {
				MsgUtils.addFacesMessage("WeeklyBatch.Transmit.NoService", FacesMessage.SEVERITY_ERROR);
			}
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("WeeklyBatch.Transmit.Error", FacesMessage.SEVERITY_ERROR);
			ret = false;
			EventUtils.logError(this, e);
		}
//		if (ret) { // Project transfer is no longer needed. 12/11/18
//			DocumentTransferService.getInstance().checkTransmitProject(project);
//		}
		return ret;
	}

	/**
	 * @param batch
	 * @return
	 */
	private boolean createAndSendFiles(WeeklyBatch batch) {
		boolean fileLocation = transferBatch(batch);
		return fileLocation;
	}

	@Override
	protected Collection<Project> findProjects(WeeklyBatch batch) {
		Set<Project> list = new HashSet<>();
		for (WeeklyTimecard wtc : batch.getTimecards()) {
			list.add(wtc.getStartForm().getProject());
		}
		return list;
	}

	/**
	 * Create a list of Deliverable`s from the timecards in 'batch'. If 'proj'
	 * is not null (which will be the case for Commercial productions), then
	 * only those timecards related to the given project will be included in the
	 * Deliverables. If the payroll service has the "split by EOR" flag set
	 * (i.e., it's TEAM), then one Deliverable will be created for each EOR
	 * found within the selected timecards.
	 *
	 * @see com.lightspeedeps.service.TransferService#createDeliverables(java.lang.Object,
	 *      com.lightspeedeps.model.Project)
	 */
	@Override
	protected List<Deliverable> createDeliverables(WeeklyBatch batch, Project proj) {

		// Need a mapping from EOR to Deliverable so we can create one Deliverable per unique EOR
		Map<EmployerOfRecord,Deliverable> map = new HashMap<>();

		TransferService<WeeklyBatch,WeeklyTimecard>.Deliverable deliverable;

		EmployerOfRecord defaultEor = null;
		if (production.getPayrollPref().getTeamEor() != null) {
			defaultEor = production.getPayrollPref().getTeamEor();
		}

		for (WeeklyTimecard wtc : batch.getTimecards()) {
			// Select timecards for the specified Project (if any)
			if (proj == null || proj.equals(wtc.getStartForm().getProject())) {
				EmployerOfRecord eor = EmployerOfRecord.NONE;
				if (service.getSplitByEor()) {
					if (defaultEor != null) {
						eor = defaultEor;
					}
					else if (production.getType().isTours()) {
						eor = EmployerOfRecord.TEAM_TOURS;
					}
					else {
						eor = wtc.getStartForm().getTeamEor();
					}
				}
				// see if we have a deliverable for this EOR already....
				deliverable = map.get(eor);
				if (deliverable == null) { // no -- create one.
					deliverable = new Deliverable(eor);
					deliverable.setName("B#" + batch.getId() + eor.getFilenameKey());
					deliverable.setBatchNumber(batch.getId());
					map.put(eor, deliverable);
				}
				deliverable.getDocuments().add(wtc); // add TC to matching deliverable
			}
		}

		// Get a List of all the Deliverable's that we created (one per unique EOR)
		ArrayList<Deliverable> list = new ArrayList<>(map.values());
		return list;
	}

	/**
	 * @param deliver
	 */
	@Override
	protected void createPDF(WeeklyBatch batch, Project proj, Deliverable deliver) {
		List<String> pdfFiles =  null;
		if (reportStyle == ReportStyle.TIMESHEET) {
			if (suppliedReport != null) {
				pdfFiles =  new ArrayList<>();
				pdfFiles.add(suppliedReport);
			}
		}
		else if (reportStyle != ReportStyle.N_A) {
			pdfFiles =  new ArrayList<>();
			switch (service.getSendBatchMethod()) {
				case AUTH_POST:
					sendSeparateReports(production, batch, reportStyle);	// send PDF of each timecard
					break;
				case ABS_FILE:
				case CSV_FILE:
				case PDF_ONLY:
				case TEAM_FILE:
				case TEAM_PDF:
					if (useEmail) {
						pdfFiles = createPdfReports(batch, deliver, reportStyle);
					}
					break;
				case TEAM_FTP:
					// 10/6/16 SKIP individual PDF files for now! (for TEAM?)
//					if (! sendSeparateReports(prod, batches, reportStyle)) {	// send PDF of each timecard.
//						pdfFile = null;
//						break;
//					}
					// create batch PDF for later transfer...
					pdfFiles = createPdfReports(batch, deliver, reportStyle);
					break;
				case CREW_CARDS:
					if (useEmail) {
						pdfFiles = createPdfReports(batch, deliver, reportStyle);
					}
					break;
			}
		}
		if (pdfFiles != null && pdfFiles.size() > 0) {
			deliver.setPdfFilename(pdfFiles.get(0));
		}
		return;
	}

	/**
	 * @param deliver
	 */
	@Override
	protected void createDataFile(WeeklyBatch batch, Project proj, Deliverable deliver) {
		String exportFileName = null;
		if (exportType != ExportType.NONE) {
			switch (service.getSendBatchMethod()) {
				case AUTH_POST:
					// get authorization credentials and send the batch(es)
					if (sendAuthPost(batch)) {
						exportFileName = "ok";
					}
					break;
				case ABS_FILE:
				case CREW_CARDS:
				case CSV_FILE:
				case TEAM_FILE:
				case TEAM_FTP:
					exportFileName = createFile(batch.getId(), deliver, false);
					break;
				case PDF_ONLY:
				case TEAM_PDF:
					// no data file associated with this connection method
					break;
			}
		}
		if (exportFileName != null && ! exportFileName.equals("ok")) {
			deliver.setDataFilename(exportFileName);
		}
		return;

	}

	/**
	 * Send (FTP, email, or open in browser) both the data file and PDFs
	 * contained in the given Deliverables.
	 *
	 * @param batch The WeeklyBatch that is the source of the Deliverables.
	 * @param proj The relevant Project.
	 * @param delivers The Deliverables.
	 * @return True if the delivery was successful, false otherwise.
	 */
	@Override
	protected boolean sendFiles(WeeklyBatch batch, Project proj, List<Deliverable> delivers) {
		boolean sentOk = true;
		List<String> dataFiles = new ArrayList<>();
		List<String> ftpFiles = new ArrayList<>();
		String gnrtdReportsPath = File.separator;
		gnrtdReportsPath = SessionUtils.getRealPath("") + File.separator;
		if (exportType != ExportType.NONE) {
			for (Deliverable deliver : delivers) {
				if (deliver.getDataFilename() != null) {
					String datafile = deliver.getDataFilename();
					switch (service.getSendBatchMethod()) {
						case AUTH_POST:
							// get authorization credentials and send the batch(es)
							// NOT CURRENTLY SUPPORTED
//							sentOk = sendAuthPost(batch);
							break;
						case ABS_FILE:
						case CREW_CARDS:
						case CSV_FILE:
						case TEAM_FILE:
							if (! useEmail) {
								// Open the file in a browser window
								sentOk &= showFile(/* File.separator + */datafile);
							}
							else {
								dataFiles.add(gnrtdReportsPath + datafile);
							}
							break;
						case TEAM_FTP:
							ftpFiles.add(gnrtdReportsPath + datafile);
							break;
						case PDF_ONLY:
						case TEAM_PDF:
							// no data file associated with this connection method
							break;
					}
				}
			}
		}

		if (ftpFiles.size() > 0) { // something to FTP
			if (production.getType().isTours()) {
				setFtpPrefix(FTP_TOURS_PREFIX);
			}
			sentOk &= sendFtp(ftpFiles);
		}

		if (sentOk) {
			// Update batch status and timestamp
			updateSentStatus(batch);
		}
		if (useEmail) {
			Collection<String> fileNames = new ArrayList<>();
			int deliveryCount = 0;
			for (Deliverable deliver : delivers) {
				deliveryCount += deliver.getDocuments().size();
				if (deliver.getPdfFilename() != null) {
					fileNames.add(deliver.getPdfFilename());
				}
			}
			fileNames.addAll(dataFiles);
			String emailAddress = preferences.getBatchEmailAddress();
			String batchId = "B#" + batch.getId();
			String batchMessage = batchId + ": " + batch.getName() + " (";
			if (deliveryCount != batch.getTimecardCount()) {
				// batch was split, probably by project - show delivered count in message
				batchMessage += deliveryCount + " of ";
			}
			batchMessage += batch.getTimecardCount() + ")" + "<p/>";
			if (! sentOk) {
				batchMessage += " ** ERROR ** Delivery of data file was unsuccessful ** <p/>";
			}
			project = ProjectDAO.getInstance().refresh(project);
			if (production.getType().isTours()) {
				DoNotification.getInstance().sendTimesheet(emailAddress, fileNames, project, deliveryCount);
			}
			else {
				DoNotification.getInstance().sendTimecardBatch(emailAddress, fileNames, project, 1,
						batchMessage, deliveryCount, batch);
			}
		}
		return sentOk;
	}

	/**
	 * Create files with the printed reports of the timecards that are in the
	 * supplied batches.
	 *
	 * @param prod The Production holding the timecards.
	 * @param deliver
	 * @param batches The Collection of WeeklyBatch`s whose timecard's should be
	 *            printed.
	 * @param reptStyle The style (layout) of timecard PDF report to be
	 *            generated.
	 * @return A list of the filenames of the files containing the printed
	 *         reports; one file is created for each WeeklyBatch in the provided
	 *         input collection.
	 */
	private List<String> createPdfReports(WeeklyBatch wb,
			TransferService<WeeklyBatch, WeeklyTimecard>.Deliverable deliver, ReportStyle reptStyle) {
		List<String> files = new ArrayList<>();
		Collection<Integer> ids = new ArrayList<>();
		String fileWithAttachments = null;
			for (WeeklyTimecard wtc : deliver.getDocuments()) {
				ids.add(wtc.getId());
			}
			String filePrefix = deliver.getName() + "_" + wb.getName();
			String filename = TimecardPrintUtils.printTimecardList(production, ids, reptStyle,
					filePrefix,	// prefix of generated filename
					" w.last_name, w.first_name, w.end_date, w.Occupation ",
					production.getPayrollPref().getIncludeBreakdown(), true, wb.getProject());
			log.debug("File name = " + filename);
			if (filename == null) { // Error in printing!
				files = null;
				return null;
			}
			else if (filename != null) {
				try {
					fileWithAttachments = TimecardPrintUtils.appendAttachmentInTcReport(filename, true);
				}
				catch (Exception e) {
					// LS-2889 - catch attachment merge errors, although most should be caught earlier.
					fileWithAttachments = null;
					EventUtils.logError(e);
					MsgUtils.addFacesMessage("Timecard.Print.AllAttachmentsInTransferError", FacesMessage.SEVERITY_WARN);
				}
			}
			if (fileWithAttachments != null) {
				files.add(fileWithAttachments);
			}
			else {
				files.add(filename);
			}
			ids.clear();
		return files;
	}

	/**
	 * Send printed reports of the timecards to the payroll service.
	 *
	 * @param prod The Production holding the timecards.
	 * @param batches The Collection of WeeklyBatch`s whose timecard's should be
	 *            printed.
	 * @param reportStyle The style (layout) of timecard PDF report to be generated.
	 */
	private static boolean sendSeparateReports(Production prod, WeeklyBatch wb,
			ReportStyle reportStyle) {
		boolean bRet = true;
		bRet = sendSeparateReport(prod, wb.getTimecards(), reportStyle);
		return bRet;
	}

	/**
	 * Send a separate printed report to the payroll service for each of the given timecards.
	 *
	 * @param prod The Production holding the timecards.
	 * @param timecards The Collection of Timecard`s to be printed.
	 * @param reportStyle The style (layout) of timecard PDF report to be generated.
	 */
	private static boolean sendSeparateReport(Production prod, Collection<WeeklyTimecard> timecards, ReportStyle reportStyle) {
		boolean bRet = true;
		PayrollService service = prod.getPayrollPref().getPayrollService();
		String domain = service .getFtpDomain();
		String directory = service.getFtpDirectory();
		String userName = service.getFtpLoginName();
		String password = service.getFtpPassword();
		Integer port = service.getFtpPort();
		boolean includeBreakdown = prod.getPayrollPref().getIncludeBreakdown();
		File file;
		String filename;
		for (WeeklyTimecard tc : timecards) {
			filename = TimecardPrintUtils.printTimecard(tc, reportStyle, includeBreakdown, true);
			if (service.getSendBatchMethod() == ServiceMethod.AUTH_POST ||
					service.getSendBatchMethod() == ServiceMethod.TEAM_FTP) {
				filename = StringUtils.cleanFilename(filename);
				file = new File(filename);
				bRet = HttpUtils.sendFile(file, tc.getId() + ".pdf", domain, port, directory, userName, password);
				if (!bRet) {
					break;
				}
			}
		}
		return bRet;
	}

	/**
	 * Present the file to the user by opening a browser window on the file.
	 *
	 * @param datafile The *relative* path and name of the file to be opened in
	 *            the browser.
	 * @return True iff the datafile name is not null.
	 */
	private boolean showFile(String datafile) {
		boolean ret = false;
		if (datafile != null) {
			ret = true;
			datafile = datafile.replace('\\', '/');
			// open in "same window" ('_self'), since user should get prompt to save as file
			String javascriptCode = "window.open('../../" + datafile
					+ "','_self');";
			View.addJavascript(javascriptCode);
			/** Note: we depend on {@link LsFacesServlet} to encourage the browser to
			 prompt for saving the file (instead of opening it in a browser window)
			 by setting the content-disposition in the response header appropriately.
			 */
		}
		return ret;
	}

	/**
	 * Create a file and stream the timecards into that file. Update the "sent"
	 * status of the batches with the current date and time.
	 *
	 * @param batchNumber The id number of the batch containing the timecards being sent.
	 * @param deliver The Deliverable that describes the data to be streamed into a file.
	 * @param json If true, create JSON data file; if false, create "flat"
	 *            tab-delimited file.
	 *
	 * @return Filename of the file holding the batches of timecards, relative
	 *         to our web context location.
	 */
	private String createFile(Integer batchNumber, TransferService<WeeklyBatch, WeeklyTimecard>.Deliverable deliver, boolean json) {
		String reportFileName = createFileName(deliver.getName());
		String fileLocation = Constants.REPORT_FOLDER + File.separator + reportFileName;
		OutputStream outputStream = createFileStream(reportFileName);
		if (outputStream != null) {
			if (streamBatches(outputStream, deliver, json)) {
			}
			else {
				fileLocation = null;
			}
		}
		else {
			fileLocation = null;
		}
		return fileLocation;
	}

	/**
	 * 2/4/2017 (~ rev 7399) This code is not currently used, and was not updated when this
	 * class was heavily refactored and made a subclass of TransferService.
	 *
	 * Send all the batches supplied using the "authenticate then post" method.
	 *
	 * @param batch The batch to be transmitted.
	 * @return True iff the transmission of the batch was successful.
	 */
	private boolean sendAuthPost(WeeklyBatch batch) {
		boolean ret = true;
		String authToken = HttpUtils.getAuthToken(service.getAuthUrl(), service.getLoginName(), service.getPassword());
//		String authToken = "Test-Auth-Token";
		if (authToken != null) {
			OutputStream outputStream = createByteStream();
			if (streamAsJson(outputStream, batch, true)) {
				ByteArrayOutputStream baos = (ByteArrayOutputStream)outputStream;
				String url;
//					url = "https://app.lightspeedeps.com/beta/IP/timecard.test";
				url = service.getBatchUrl();
				log.debug("ready to transmit batch: " + batch.getName());
				String response = HttpUtils.sendTimecards(url, authToken, baos.toString());
				if (response == null) {
					updateSentStatus(batch);
				}
				else {
					handleMessage(response);
					ret = false;
				}
			}
			else {
				ret = false;
			}
		}
		else {
			ret = false;
			MsgUtils.addFacesMessage("WeeklyBatch.TransmitError.AuthFailed", FacesMessage.SEVERITY_ERROR);
		}
		return ret;
	}

	/**
	 * Generate the appropriate FACES messages based on the error information
	 * contained in the supplied message.
	 *
	 * @param msg The error message returned by the payroll service. It is expected
	 * that the first 2 characters of the message is a 2-digit error number.
	 */
	private static void handleMessage(String msg) {
		String rcStr = msg.substring(0, 2);

		int rc = -1;
		try {
			rc = Integer.parseInt(rcStr);
		}
		catch (NumberFormatException e) {
			log.error("Unable to extract timecard id from transmission error message.");
			//EventUtils.logError(e);
		}

		switch (rc) {
			case 1:		// bad credentials
				MsgUtils.addFacesMessage("WeeklyBatch.TransmitError.AuthError", FacesMessage.SEVERITY_ERROR);
				break;
			case 2:		// unknown production
				MsgUtils.addFacesMessage("WeeklyBatch.TransmitError.UnknownProduction", FacesMessage.SEVERITY_ERROR);
				break;
			case 3:		// missing PDF for timecard
				MsgUtils.addFacesMessage("WeeklyBatch.TransmitError.MissingPDF", FacesMessage.SEVERITY_ERROR);
				MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_ERROR, msg);
				break;
			case 4:
			case 5:
			case 6:
				MsgUtils.addFacesMessage("WeeklyBatch.TransmitError.MissingBatchField", FacesMessage.SEVERITY_ERROR);
				MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_ERROR, msg);
				break;
			case 99:	// internal server error
				MsgUtils.addFacesMessage("WeeklyBatch.TransmitError.ServerError", FacesMessage.SEVERITY_ERROR);
				break;
			default:	// missing field
				int tcId = parseTimecardId(msg);
				MsgUtils.addFacesMessage("WeeklyBatch.TransmitError.MissingTimecardField", FacesMessage.SEVERITY_ERROR, tcId);
				MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_ERROR, msg);
				break;
		}
		//MsgUtils.addFacesMessage("WeeklyBatch.Transmit.Error", FacesMessage.SEVERITY_ERROR);
	}

	/**
	 * Parse a message to find a WeeklyTimecard id value; it is expected to be
	 * enclosed in parentheses.
	 *
	 * @param msg The message text to parse.
	 * @return The value of the id found within the first pair of parentheses.
	 *         Returns 0 if it does not find a pair of parentheses, or -1 if the
	 *         data within the parentheses is not an integer.
	 */
	private static int parseTimecardId(String msg) {
		int id = 0;
		int ix = msg.indexOf('(');
		if (ix >= 0) {
			int ix2 = msg.indexOf(')', ix+1);
			if (ix2 > 0) {
				String idStr = msg.substring(ix+1, ix2);
				try {
					id = Integer.parseInt(idStr);
				}
				catch (NumberFormatException e) {
					id = -1;
				}
			}
		}
		return id;
	}

	/**
	 * Prepare the batches for transmission -- setting production ids, current
	 * timestamp, timecard count, etc.
	 *
	 * @param batchez The Collection of batches to be prepared for transmission.
	 * @return A new Collection, containing the given batches, but which have
	 *         been "refreshed" from the database and prepared for sending.
	 */
	@Override
	protected Collection<WeeklyBatch> prepareBatches(Collection<WeeklyBatch> batches) {
		WeeklyBatchDAO weeklyBatchDAO = WeeklyBatchDAO.getInstance();
		Collection<WeeklyBatch> fresh = new HashSet<>();
		for (WeeklyBatch batch : batches) {
			batch = weeklyBatchDAO.refresh(batch);
			fresh.add(batch);
			prepareBatch(batch);
		}
		return fresh;
	}

	/**
	 * Prepare a batch for transmission. Currently this sets the payroll
	 * service's production id in the batch header, and removes timecards
	 * that are not transmittable.
	 *
	 * @param prod The Production to which the batch belongs.
	 * @param batch The batch to be prepared.
	 * @return True iff the batch contains at least one timecard to be processed.
	 */
	private boolean prepareBatch(WeeklyBatch batch) {
		boolean bRet = true;
		boolean tours = production.getType().isTours();
		boolean sendOnlyChanges = production.getPayrollPref().getPayrollService().getSendOnlyChanges();
		batch.setPrProductionId(production.getPayrollPref().getPayrollProdId());

		for (Iterator<WeeklyTimecard> iter = batch.getTimecards().iterator(); iter.hasNext(); ) {
			WeeklyTimecard wtc = iter.next();
			// For Payroll Service that do not want to transmit
			// empty/uncalculatedTimecards
			boolean includeInTransmit = true;

			if(isTeamPayroll) {
				if(TimecardUtils.checkForMissingPaymentInfo(wtc)) {
					includeInTransmit = false;
				}
			}

			if (tours || (wtc.getTransmittable() && includeInTransmit)) {
				// For tours, timecards were already inspected/filtered for 'transmitability'
				if (! wtc.getBatchStatus().equals(WeeklyTimecard.BATCH_STATUS_REMOVED)) {
					if (wtc.getTimeSent() == null) {
						wtc.setBatchStatus(WeeklyTimecard.BATCH_STATUS_NEW);
					}
					else if (wtc.getUpdated().after(wtc.getTimeSent())) {
						wtc.setBatchStatus(WeeklyTimecard.BATCH_STATUS_UPDATED);
					}
					else if (sendOnlyChanges) {
						iter.remove();
					}
					else { // if allowed to re-send unchanged TCs, don't leave status blank:
						wtc.setBatchStatus(WeeklyTimecard.BATCH_STATUS_UPDATED);
					}
				}
			}
			else { // timecard is missing required fields, omit from sending
				iter.remove();
			}
		}
		if (batch.getTimecards().size() == 0) {
			log.debug("No timecards in batch were 'transmittable'.");
			bRet = false;
		}
		return bRet;
	}

	/**
	 * Create a ByteArrayOutputStream.
	 *
	 * @return a new (memory-based) ByteArrayOutputStream.
	 */
	private static OutputStream createByteStream() {
		OutputStream outputStream = null;
		try {
			outputStream = new ByteArrayOutputStream(); //(new File(reportFileName));
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return outputStream;
	}

	/**
	 * Fill an OutputStream with the JSON or flat representation of all the
	 * batches in the given collection. The batches are simply appended, one
	 * after the other, in the stream.
	 *
	 * @param outputStream The OutputStream to contain the JSON data. The stream
	 *            will be closed if the process is successful.
	 * @param batches The collection of WeeklyBatch`s to be streamed.
	 * @param payInfo True iff calculated pay data, e.g., pay breakdown numbers,
	 *            should be included in the transferred data.
	 * @param json If true, create JSON data file; if false, create "flat"
	 *            tab-delimited file.
	 * @return True iff all the batches were successfully pushed to the stream.
	 */
	private boolean streamBatches(OutputStream outputStream, TransferService<WeeklyBatch,
			WeeklyTimecard>.Deliverable deliver, boolean json) {
		boolean ret = false;
		if (json) {
			ret = streamAsJson(outputStream, deliver, false);
		}
		else {
			ret = streamFlatBatch(outputStream, deliver);
		}
		try {
			outputStream.close();
		}
		catch (IOException e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Transmit.StreamCloseFailed", FacesMessage.SEVERITY_ERROR);
			ret = false;
		}
		return ret;
	}

	/**
	 * Write a batch of timecards to an output stream as a "flattened" export,
	 * using our {@link FlatExporter} class. This is a tab-delimited stream.
	 *
	 * @param outputStream The stream where the data should be written.
	 * @param deliver The batch of timecards to flatten.
	 * @param exportType The type (format) of export file to be generated.
	 * @return True unless an exception is thrown while exporting the batch.
	 */
	private boolean streamFlatBatch(OutputStream outputStream, TransferService<WeeklyBatch, WeeklyTimecard>.Deliverable deliver) {
		boolean ret = false;
		try {
			WeeklyTimecardDAO dao = WeeklyTimecardDAO.getInstance();

			DelimitedExporter ex;
			if (exportType == ExportType.FULL_TAB) {
				ex = new FlatExporter(outputStream, "MM/dd/yyyy");
			}
			else {
				ex = new FlatExporter(outputStream);
			}
			if (exportType.getUsesComma()) {
				ex.setDelimiter((byte)',');
			}
			Integer batchId = deliver.getBatchNumber();

			switch(exportType) {
				case CREW_CARDS:
					for (WeeklyTimecard wtc : deliver.getDocuments()) {
						wtc = dao.refresh(wtc);
						wtc.exportCrewcards(ex, payInfo);
						ex.next(); // advance the Exporter to a new record
					}
					ret = true;
					break;
				case FULL_TAB:
					List<WeeklyTimecard> tcList = new ArrayList<>(deliver.getDocuments());
					Collections.sort(tcList, getComparator());
					for (WeeklyTimecard wtc : tcList) {
						wtc = dao.refresh(wtc);
						wtc.exportTabbed(ex, payInfo, isTeamPayroll, batchId);
						ex.next(); // advance the Exporter to a new record
					}
					ret = true;
					break;
				case ABS:
					for (WeeklyTimecard wtc : deliver.getDocuments()) {
						wtc = dao.refresh(wtc);
						TimecardExport.exportABS(ex, wtc, payInfo);
					}
					ret = true;
					break;
				case PAYROLL:
					boolean first = true;
					for (WeeklyTimecard wtc : deliver.getDocuments()) {
						wtc = dao.refresh(wtc);
						TimecardExport.exportGrossPayroll(ex, wtc, first, payInfo);
						first = false; // only true on first call
					}
					ret = true;
					break;
				case JSON: // handled by streamJsonBatch()
				case NONE:
				case HOT_BUDGET:
				case SHOWBIZ_BUDGET:
					// we shouldn't get here for any of these export formats
					EventUtils.logEvent(EventType.DATA_ERROR, "Unexpected parameter value -- logic error");
					break;
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Transmit.StreamFailed", FacesMessage.SEVERITY_ERROR);
		}
		return ret;
	}

	/**
	 * @param batch
	 */
	private void updateSentStatus(WeeklyBatch batch) {
		WeeklyBatchDAO weeklyBatchDAO = WeeklyBatchDAO.getInstance();
		WeeklyTimecardDAO weeklyTimecardDAO = WeeklyTimecardDAO.getInstance();
		Date sent = new Date();
		int count = batch.getTimecardsSent();
		for (WeeklyTimecard wtc : batch.getTimecards()) {
			weeklyTimecardDAO.evict(wtc);
			if (wtc.getBatchStatus().equals(WeeklyTimecard.BATCH_STATUS_REMOVED)) {
				count--;
			}
			else {
				if (wtc.getBatchStatus().equals(WeeklyTimecard.BATCH_STATUS_NEW)) {
					count++;
				}
				wtc = weeklyTimecardDAO.findById(wtc.getId());
				wtc.setTimeSent(sent);
				weeklyTimecardDAO.attachDirty(wtc);
			}
		}
		weeklyBatchDAO.evict(batch);
		batch = weeklyBatchDAO.findById(batch.getId());
		batch.setSent(sent);
		batch.setTimecardsSent(count);
		if (isTeamPayroll) {
			// mark as "in edit" - code will check 'processed' flag later
			batch.setTimecardsEdit(count);
		}
		weeklyBatchDAO.attachDirty(batch);
	}

	/**
	 * The comparator for sorting the list of TimecardEntry's. Note that it
	 * actually invokes the compareTo function on the respective WeeklyTimecards
	 * represented by each TimecardEntry.
	 *
	 * @see com.lightspeedeps.web.view.ListView#getComparator()
	 */
	private static Comparator<WeeklyTimecard> getComparator() {
		return comparator;
	}

	private static Comparator<WeeklyTimecard> comparator = new Comparator<WeeklyTimecard>() {
		@Override
		public int compare(WeeklyTimecard c1, WeeklyTimecard c2) {
			return c1.compareTo(c2, WeeklyTimecard.SORTKEY_NAME, true);
		}
	};

	@SuppressWarnings("unused")
	private static void mapJsonClasses() {
		try {
			ObjectMapper mapper = new ObjectMapper();
//			JsonSchema sc = mapper.generateJsonSchema(WeeklyBatch.class);
//			JsonSchema sc = mapper.generateJsonSchema(WeeklyTimecard.class);
//			String s = "";// mapper.writerWithDefaultPrettyPrinter().writeValueAsString(sc);

			SchemaFactoryWrapper visitor = new SchemaFactoryWrapper();
			mapper.acceptJsonFormatVisitor(mapper.constructType(WeeklyBatch.class), visitor);
			JsonSchema jsonSchema = visitor.finalSchema();

//			log.debug(s); // what to output?
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * This static class is a JSON (Jackson) "SimpleModule" which includes two
	 * serializers that will prevent calculated payment information from being
	 * included in the serialized JSON output.
	 */
	static final class NoPayInfoModule extends SimpleModule {
		private static final long serialVersionUID = 1L;

		public NoPayInfoModule() {
	        addSerializer(PayJob.class, new SkipPayJobSerializer());
	        addSerializer(PayBreakdown.class, new SkipPayBreakdownSerializer());
	    }
	}

}
