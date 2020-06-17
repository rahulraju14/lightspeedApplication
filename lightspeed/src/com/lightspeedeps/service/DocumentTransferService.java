/**
 * File: TransferService.java
 */
package com.lightspeedeps.service;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ContactDocumentDAO;
import com.lightspeedeps.dao.EmploymentDAO;
import com.lightspeedeps.dao.FormW4DAO;
import com.lightspeedeps.dao.OfficeDAO;
import com.lightspeedeps.dao.ProjectDAO;
import com.lightspeedeps.dao.TaxWageAllocationFormDAO;
import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.ContactDocument;
import com.lightspeedeps.model.Employment;
import com.lightspeedeps.model.FormI9;
import com.lightspeedeps.model.FormW4;
import com.lightspeedeps.model.Office;
import com.lightspeedeps.model.PayrollService;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.StartForm;
import com.lightspeedeps.model.TaxWageAllocationForm;
import com.lightspeedeps.model.User;
import com.lightspeedeps.port.DelimitedExporter;
import com.lightspeedeps.port.FlatExporter;
import com.lightspeedeps.type.ApprovalStatus;
import com.lightspeedeps.type.EmployerOfRecord;
import com.lightspeedeps.type.EventType;
import com.lightspeedeps.type.ExportType;
import com.lightspeedeps.type.PayrollFormType;
import com.lightspeedeps.type.ReportStyle;
import com.lightspeedeps.type.ServiceMethod;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.report.ReportUtils;
import com.lightspeedeps.web.report.ReportBean;
import com.lightspeedeps.web.report.ReportQueries;

/**
 *
 */
public class DocumentTransferService extends TransferService<Collection<ContactDocument>, ContactDocument> {

	private ContactDocumentDAO contactDocumentDAO = ContactDocumentDAO.getInstance();
	
	private static final Log LOG = LogFactory.getLog(DocumentTransferService.class);

	/**
	 * The default constructor.
	 */
	public DocumentTransferService() {
		setFtpPrefix(FTP_DOC_PREFIX);
	}

	/**
	 * A helper method to get an instance of DocumentTransferService.
	 * @return A new instance.
	 */
	public static DocumentTransferService getInstance() {
		return new DocumentTransferService();
	}

	/**
	 * Prepare the batches for transmission -- setting production ids, current
	 * timestamp, timecard count, etc.
	 *
	 * @param batches The Collection of batches to be prepared for transmission.
	 * @return A Collection, containing the given batches, but which have
	 *         been "refreshed" from the database and prepared for sending.
	 */
	@Override
	protected Collection<Collection<ContactDocument>> prepareBatches(Collection<Collection<ContactDocument>> batches) {
		return batches;
	}

	/**
	 * @see com.lightspeedeps.service.TransferService#findProjects(java.lang.Object)
	 */
	@Override
	protected Collection<Project> findProjects(Collection<ContactDocument> batch) {
		Set<Project> list = new HashSet<>();
		for (ContactDocument cd : batch) {
			list.add(cd.getProject());
		}
		return list;
	}

	/**
	 * @see com.lightspeedeps.service.TransferService#createDeliverables(java.lang.Object, com.lightspeedeps.model.Project)
	 */
	@Override
	protected List<TransferService<Collection<ContactDocument>, ContactDocument>.Deliverable>
			createDeliverables(Collection<ContactDocument> batch, Project proj) {

		// Need a mapping from EOR to Deliverable so we can create one Deliverable per unique EOR
		Map<EmployerOfRecord,Deliverable> map = new HashMap<>();

		TransferService<Collection<ContactDocument>,ContactDocument>.Deliverable deliverable;

		EmployerOfRecord defaultEor = null;
		if (production.getPayrollPref().getTeamEor() != null) {
			defaultEor = production.getPayrollPref().getTeamEor();
		}

		for (ContactDocument cd : batch) {
			// Select timecards for the specified Project (if any)
			if (proj == null || proj.equals(cd.getProject())) {
				EmployerOfRecord eor = EmployerOfRecord.NONE;
				if (cd.getFormType() == PayrollFormType.PROJECT) {
					setFtpPrefix(FTP_PROJECT_PREFIX); // ** Set prefix of FTP transfer file **
					// use default EOR (NONE)
				}
				else if (service.getSplitByEor()) {
					if (defaultEor != null) {
						eor = defaultEor;
					}
					else if (production.getType().isTours()) {
						eor = EmployerOfRecord.TEAM_TOURS;
					}
					else if (cd.getEmployment() != null && cd.getEmployment().getStartForm() != null) {
						eor = cd.getEmployment().getStartForm().getTeamEor();
					}
				}
				// see if we have a deliverable for this EOR already....
				deliverable = map.get(eor);
				if (deliverable == null) { // no -- create one.
					deliverable = new Deliverable(eor);
					map.put(eor, deliverable);
				}
				deliverable.getDocuments().add(cd); // add TC to matching deliverable
			}
		}
		// Get a List of all the Deliverable's that we created (one per unique EOR)
		ArrayList<Deliverable> list = new ArrayList<>(map.values());
		return list;
	}

	/**
	 * @see com.lightspeedeps.service.TransferService#createPDF(java.lang.Object, com.lightspeedeps.model.Project, com.lightspeedeps.service.TransferService.Deliverable)
	 */
	@Override
	protected void createPDF(Collection<ContactDocument> batch, Project proj,
			TransferService<Collection<ContactDocument>, ContactDocument>.Deliverable deliver) {

		// currently the PDFs are generated in TransferBean for Contact Documents, prior to calling the TransferService,
		// Tax Wage Allocation forms will be generated here.
		List<String> pdfFiles =  null;
		if (reportStyle != ReportStyle.N_A) {
			pdfFiles =  new ArrayList<>();
			switch (service.getSendBatchMethod()) {
				case TEAM_FTP:
					// 10/6/16 SKIP individual PDF files for now! (for TEAM?)
//					if (! sendSeparateReports(prod, batches, reportStyle)) {	// send PDF of each timecard.
//						pdfFile = null;
//						break;
//					}
					// create batch PDF for later transfer...
					pdfFiles = createPdfReports(batch, deliver);
					break;
				default:
					break;
			}
		}
		if (pdfFiles != null && pdfFiles.size() > 0) {
			deliver.setPdfFilename(pdfFiles.get(0));
		}
	}

	/**
	 * Create files with the printed reports of the timecards that are in the
	 * supplied batches.
	 * @param batch
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
	private List<String> createPdfReports(Collection<ContactDocument> batch, TransferService<Collection<ContactDocument>, ContactDocument>.Deliverable deliver) {
		List<String> files = new ArrayList<>();
		Collection<Integer> ids = new ArrayList<>();
		String reportName, sqlQuery;
		String fileName = null;

		for (ContactDocument cd : deliver.getDocuments()) {
			ids.add(cd.getId());
		}

		switch(reportStyle) {
			case ALLOCATION:
				reportName = ReportBean.JASPER_TOURS_TAX_WAGE_ALLOCATION;
				sqlQuery = ReportQueries.taxWageAllocationQuery;
				fileName = ReportUtils.generateTaxWageAllocationReport(project, reportName, sqlQuery, null, false, false, deliver.getDocuments().get(0).getId(), null);
				break;
			default:
				break;
		}

		if (fileName == null) { // Error in printing!
			files = null;
			return null;
		}

		files.add(fileName);
		ids.clear();

		return files;
	}
	/**
	 * @see com.lightspeedeps.service.TransferService#createDataFile(java.lang.Object, com.lightspeedeps.model.Project, com.lightspeedeps.service.TransferService.Deliverable)
	 */
	@Override
	protected void createDataFile(Collection<ContactDocument> docs, Project proj,
			TransferService<Collection<ContactDocument>, ContactDocument>.Deliverable deliver) {
		String exportFileName = null;
		String gnrtdReportsPath = SessionUtils.getRealPath("") + File.separator;
		if (exportType != ExportType.NONE) {
			switch (service.getSendBatchMethod()) {
				case TEAM_FILE:
				case TEAM_FTP:
					Collection<ContactDocument> docsToSend = createTransferableList(deliver.getDocuments());
					if (docsToSend.size() > 0) {
						exportFileName = createFile(docsToSend, false);
						exportFileName = gnrtdReportsPath + exportFileName;
						deliver.setDataFilename(exportFileName);
					}
					break;
				case AUTH_POST:
				case ABS_FILE:
				case CSV_FILE:
				case CREW_CARDS:
				case PDF_ONLY:
				case TEAM_PDF:
					break;
			}
		}
	}

	/**
	 * @see com.lightspeedeps.service.TransferService#sendFiles(java.lang.Object, com.lightspeedeps.model.Project, java.util.List)
	 */
	@Override
	protected boolean sendFiles(Collection<ContactDocument> batches, Project proj,
			List<TransferService<Collection<ContactDocument>, ContactDocument>.Deliverable> delivers) {
		boolean ret = true;
		if (exportType != ExportType.NONE) {
			switch (service.getSendBatchMethod()) {
				case TEAM_FTP:
					List<String> ftpFiles = new ArrayList<>();
					for (Deliverable deliver : delivers) {
						if (deliver.getDataFilename() != null) {
							ftpFiles.add(deliver.getDataFilename());
						}
					}
					ret = sendFtp(ftpFiles);

					if(ret && reportStyle == ReportStyle.ALLOCATION) {
						List<String> pdfFiles = new ArrayList<>();
						for (Deliverable deliver : delivers) {
							if (deliver.getDataFilename() != null) {
								pdfFiles.add(deliver.getPdfFilename());
							}
						}
						// Get the contact record associated with the document.
						ContactDocument cd = delivers.get(0).getDocuments().get(0);
						Contact ct = cd.getContact();
						String emailAddress = service.getToursNotificationEmail();
						DoNotification.getInstance().sendAllocation(emailAddress, project, pdfFiles, ct);
					}
					break;
				case AUTH_POST:
				case ABS_FILE:
				case CSV_FILE:
				case CREW_CARDS:
				case PDF_ONLY:
				case TEAM_FILE:
				case TEAM_PDF:
					break;
			}
		}
		return ret;
	}

	/**
	 * Create a collection of ContactDocument`s that are eligible to be
	 * transferred; this is a subset of the Collection given which only includes
	 * documents which have passed some test(s) ensuring that they should be
	 * transferred.
	 * <p>
	 * Currently, the only test involved is whether the related document's type
	 * is considered transferable.
	 *
	 * @param documents A collection of candidate ContactDocument`s to be sent.
	 * @return A non-null, but possibly empty, collection of ContactDocument`s
	 *         that have been determined to be eligible to be sent.
	 */
	private Collection<ContactDocument> createTransferableList(Collection<ContactDocument> documents) {
		Collection<ContactDocument> qualified = new ArrayList<>();

		for (ContactDocument doc : documents) {
			PayrollFormType type = doc.getFormType();
			if (type.isTransferable()) {
				qualified.add(doc);
			}
		}
		return qualified;
	}

	/**
	 * Create a file and stream the timecards into that file. Update the "sent"
	 * status of the batches with the current date and time.
	 *
	 * @param docs The collection of ContactDocument objects to place in the
	 *            file.
	 * @param json If true, create JSON data file; if false, create "flat"
	 *            tab-delimited file.
	 * @return Filename of the file holding the batches of timecards, relative
	 *         to our web context location.
	 */
	private String createFile(Collection<ContactDocument> docs, boolean json) {
		String reportFileName = createFileName(null);
		String fileLocation = Constants.REPORT_FOLDER + File.separator + reportFileName;
		OutputStream outputStream = createFileStream(reportFileName);
		if (outputStream != null) {
			if (streamBatches(outputStream, docs, json)) {
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
	 * Fill an OutputStream with the JSON or flat representation of all the
	 * batches in the given collection. The batches are simply appended, one
	 * after the other, in the stream.
	 *
	 * @param outputStream The OutputStream to contain the JSON data. The stream
	 *            will be closed if the process is successful.
	 * @param docs The collection of ContactDocument`s to be streamed.
	 * @param json If true, create JSON data file; if false, create "flat"
	 *            tab-delimited file.
	 * @return True iff all the batches were successfully pushed to the stream.
	 */
	private boolean streamBatches(OutputStream outputStream, Collection<ContactDocument> docs,
			boolean json) {
		boolean ret = false;
		if (json) {
			if (! streamAsJson(outputStream, docs, false)) {
				//break;
			}
		}
		else {
			if (! streamFlatDocuments(outputStream, docs)) {
				//break;
			}
		}
		ret = true;
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
	 * Write a collection of documents to an output stream as a "flattened" export,
	 * using our {@link FlatExporter} class. This is a tab-delimited stream.
	 *
	 * @param outputStream The stream where the data should be written.
	 * @param cds The collection of documents to flatten.
	 * @return True unless an exception is thrown while exporting the batch.
	 */
	private boolean streamFlatDocuments(OutputStream outputStream, Collection<ContactDocument> cds) {
		boolean ret = false;
		try {
			DelimitedExporter ex;
			ex = new FlatExporter(outputStream, "MM/dd/yyyy");
			if (exportType.getUsesComma()) {
				ex.setDelimiter((byte)',');
			}

			switch(exportType) {
				case FULL_TAB:
					checkCreateMissingStarts(cds);
					List<ContactDocument> cdList = new ArrayList<>(cds);
					Collections.sort(cdList, getTransferComparator()); // order the docs by formType
					Integer lastId = -1;
					ArrayList<Employment> sentEmps = new ArrayList<>();
					FormW4DAO formW4DAO = FormW4DAO.getInstance();
					for (ContactDocument cdoc : cdList) {
						if (cdoc.getFormType() == PayrollFormType.PROJECT) {
							project.exportFlat(ex);
							project.setTimeSent(new Date());
							ProjectDAO.getInstance().attachDirty(project);
						}
						else if (cdoc.getFormType() == PayrollFormType.T_W_ALLOC) {
							// Get the Allocation Form to create export file.
							if(cds != null) {
								Integer formId = cdoc.getId();
								setFtpPrefix(PayrollFormType.T_W_ALLOC.getExportType());
								TaxWageAllocationFormDAO formDAO = TaxWageAllocationFormDAO.getInstance();
								TaxWageAllocationForm form = formDAO.findById(formId);
								form.exportFlat(ex);
								form.setTimeSent(new Date());
								formDAO.attachDirty(form);
							}
						}
						else {
							cdoc = contactDocumentDAO.refresh(cdoc);
							if (cdoc.getEmployment().getId() != lastId) {
								lastId = cdoc.getEmployment().getId();
								Employment er = cdoc.getEmployment();
								Contact exportContact = cdoc.getContact();
								FormW4 formW4 = null;
								// if cdoc is W4, use that; else search 'cdList' for W4;
								if (cdoc.getFormType() == PayrollFormType.W4 && cdoc.getRelatedFormId() != null) {
									formW4 = formW4DAO.findById(cdoc.getRelatedFormId());
								}
								else {
									for (ContactDocument cd : cdList) {
										// (note that cdList has docs for multiple contacts, need to just look for this contact's W4);
										if (cd.getFormType() == PayrollFormType.W4 && cd.getRelatedFormId() != null
												&& cd.getContact().equals(exportContact)) {
											formW4 = formW4DAO.findById(cd.getRelatedFormId());
											break;
										}
									}
								}
								// if not in list, do db query for a W4 for this Contact,
								// in this production, with status='approved'.
								if (formW4 == null) {
									Map<String, Object> valueMap = new HashMap<>();
									valueMap.put("contact",exportContact);
									ContactDocument contactDoc = contactDocumentDAO.findOneByNamedQuery(
											ContactDocument.GET_APPROVED_W4_CONTACT_DOCUMENT_BY_CONTACT, valueMap);
									if (contactDoc != null) {
										formW4 = formW4DAO.findById(contactDoc.getRelatedFormId());
									}
								}

								er.exportFlat(ex, formW4); // generate "Summary record"
								ex.next(); // advance the Exporter to next record
								sentEmps.add(er);
							}
							cdoc.exportFlat(ex, isTeamPayroll); // generate form-specific record
						}
						ex.next(); // advance the Exporter to a new record
					}
					updateLastSent(sentEmps);
					ret = true;
					break;
				default:
					// we shouldn't get here for any other export format
					EventUtils.logEvent(EventType.DATA_ERROR, "Unexpected parameter value -- logic error, exportType=" + exportType);
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
	 * Check if each Contact included in the set of transferred documents also
	 * has a StartForm included in the transfer. If not, determine if such a
	 * StartForm exists. If it exists, add it to the transfer list; if not,
	 * create a 'temporary' one, using data from the I9 and W4 records for the
	 * same Contact.
	 *
	 * @param cds The collection of all ContactDocuments being transferred.
	 */
	private void checkCreateMissingStarts(Collection<ContactDocument> cds) {
		Map<Integer, Collection<ContactDocument>> cdMap = new HashMap<>();
		for (ContactDocument cdoc : cds) {
			if (cdoc.getFormType() != PayrollFormType.PROJECT) {
				Integer ctId = cdoc.getContact().getId();
				Collection<ContactDocument> docs = cdMap.get(ctId);
				if (docs == null) {
					docs = new ArrayList<>();
					cdMap.put(ctId, docs);
				}
				docs.add(cdoc);
			}
		}

		for (Collection<ContactDocument> docs : cdMap.values()) {
			ContactDocument start = checkCreateMissingStart(docs);
			if (start != null) {
				cds.add(start);
			}
		}
	}

	/**
	 * Check if the set of transferred documents includes a a StartForm. If not,
	 * determine if such a StartForm exists. If it exists, return it; if not,
	 * create a 'temporary' one, using data from the I9 and W4 records for the
	 * same Contact, and return that.
	 *
	 * @param cds The collection of documents being transferred for one person
	 *            (Contact).
	 * @return null if the collection contains a StartForm; or a StartForm to be
	 *         added to the collection. The StartForm returned may be either an
	 *         actual StartForm from the database, or a newly-created StartForm
	 *         that has not been persisted.
	 */
	private ContactDocument checkCreateMissingStart(Collection<ContactDocument> cds) {
		ContactDocument i9cd = null;
		ContactDocument w4cd = null;
		Employment employment = null;
		ContactDocument startCd = null;
		boolean haveStart = false;
		for (ContactDocument cdoc : cds) {
			employment = cdoc.getEmployment();
			if (cdoc.getFormType() == PayrollFormType.START) {
				haveStart = true;
				break;
			}
			else if (cdoc.getFormType() == PayrollFormType.I9) {
				i9cd = cdoc;
			}
			else if (cdoc.getFormType() == PayrollFormType.W4) {
				w4cd = cdoc;
			}
		}

		if (! haveStart) {
			// No startForm in the collection; see if one exists.
			Map<String, Object> valueMap = new HashMap<>();
			valueMap.put("employment", employment);
			List<ContactDocument> startCds = contactDocumentDAO.findByNamedQuery(
					ContactDocument.GET_CONTACT_DOC_LIST_START_FORM_NON_VOID_BY_EMPLOYMENT, valueMap);
			for (ContactDocument cd : startCds) {
				startCd = cd;
				haveStart = true;
				if (cd.getStatus() == ApprovalStatus.APPROVED) {
					break; // stop on approved one
				}
			}
		}

		if (! haveStart && (i9cd != null || w4cd != null)) {
			// No Payroll Start, but at least one of I9 or W4
			FormI9 formI9 = null;
			FormW4 formW4 = null;
			Contact contact = null;
			if (i9cd != null) {
				formI9 = (FormI9)FormService.findRelatedOrBlankForm(i9cd);
				contact = i9cd.getContact();
				employment = i9cd.getEmployment();
			}
			if (w4cd != null) {
				formW4 = (FormW4)FormService.findRelatedOrBlankForm(w4cd);
				contact = w4cd.getContact();
				employment = w4cd.getEmployment();
			}
			StartForm sf = StartFormService.createFirstStartForm(contact, employment, project, formI9, formW4);
			startCd = new ContactDocument();
			startCd.setProject(project);
			startCd.setFormType(PayrollFormType.START);
			startCd.setRelatedFormId(-1);
			startCd.setTransferStart(sf);
			startCd.setContact(contact);
			startCd.setEmployment(employment);
		}
		return startCd;
	}

	/** Updates the last sent field for the given employments.
	 * @param sentEmps
	 */
	private void updateLastSent(ArrayList<Employment> sentEmps) {
		Date date = new Date();
		for (Employment emp : sentEmps) {
			emp.setLastSent(date);
			EmploymentDAO.getInstance().attachDirty(emp);
		}
	}

	/**
	 * Determine if it is appropriate to transmit the Project data
	 * for the given project; if so, transmit it.
	 *
	 * @param proj
	 * @return
	 */
	public Boolean checkTransmitProject(Project proj) {
		boolean ret = false;
		// Project transfer is no longer needed. 12/11/18
//		try {
//			if (proj == null) { // happens for non-Commercial productions
//				proj = SessionUtils.getCurrentProject(); // this will get a usable project
//			}
//			init(proj, ExportType.FULL_TAB, ReportStyle.N_A, true);
//			if (! service.getTeamPayroll()) {
//				return null;
//			}
//			if (project.getTimeSent() == null) {
//				ret = true;
//			}
//			else if (project.getUpdated() == null) {
//				return null;
//			}
//			else {
//				Calendar updated = CalendarUtils.getInstance(project.getUpdated());
//				Calendar sent = CalendarUtils.getInstance(project.getTimeSent());
//				if (updated.after(sent)) {
//					ret = true;
//				}
//			}
//			if (! ret) {
//				return null;
//			}
//			ret = transmitProject(project);
//			if (! ret) {
//				EventUtils.logError("Transfer of Project data failed.");
//			}
//		}
//		catch (Exception e) {
//			EventUtils.logError(e);
//		}
		return ret;
	}

	/**
	 * Transmit a Project data file to the appropriate payroll service
	 * for the specified Project.
	 *
	 * @param proj The Project whose data is to be transferred.
	 */
	public boolean transmitProject(Project proj) {
		init(proj, ExportType.FULL_TAB, ReportStyle.N_A, true);
		String fileNameText = project.getEpisode();
		boolean ret = false;
		if (production != null) {
			Date date = new Date();
			proj.setTimeSent(date);
			contactDocumentDAO.attachDirty(proj);
			project = proj;
			if (service.getSendBatchMethod() == ServiceMethod.TEAM_FTP) {
				Collection<Collection<ContactDocument>> docs = new ArrayList<>();
				ContactDocument cd = new ContactDocument();
				cd.setProject(project);
				cd.setFormType(PayrollFormType.PROJECT);
				List<ContactDocument> cdList = new ArrayList<>();
				cdList.add(cd);
				docs.add(cdList);
				ret = transmit(docs, project, ExportType.FULL_TAB, ReportStyle.N_A, true, true);
				if (! ret) {
					fileNameText += "<p/> ** ERROR ** Delivery of data file(s) was unsuccessful ** <p/>";
				}
				String emailAddress = ""; // preferences.getBatchEmailAddress();
				emailAddress = "dHarm@theTeamCompanies.com"; // Re-direct for now
				DoNotification.getInstance().sendProject(emailAddress, project, fileNameText);
			}
		}
		return ret;
	}

	/**
	 * Transmit a Project data file to the appropriate payroll service
	 * for the specified Project.
	 *
	 * @param proj The Project whose data is to be transferred.
	 */
	public boolean transmitTaxWageAllocationForm(TaxWageAllocationForm form, Project proj) {
		init(proj, ExportType.FULL_TAB, ReportStyle.ALLOCATION, true);

		boolean ret = false;
		if (production != null) {
			if(service == null) {
				// Do not proceed if there is no Payroll Service set.
				// TaxWageAllocationBean will put up the error message.
				return false;
			}
			else if (service.getSendBatchMethod() == ServiceMethod.TEAM_FTP) {
				Collection<Collection<ContactDocument>> docs = new ArrayList<>();
				ContactDocument cd = new ContactDocument();
				cd.setProject(project);
				cd.setFormType(PayrollFormType.T_W_ALLOC);
				cd.setContact(form.getContact());
				cd.setId(form.getId());
				cd.setContact(form.getContact());

				List<ContactDocument> cdList = new ArrayList<>();
				cdList.add(cd);
				docs.add(cdList);
				ret = transmit(docs, project, exportType, ReportStyle.ALLOCATION, true, false);
			}
		}

		return ret;
	}
	
	/** Method called on the click event of "Send" button on "Transfer Documents"
	 *  popup. Transfers the selected documents to the payroll service.
	 * @return
	 */
	public static String transferDocumentToOffice(ContactDocument cd, Integer OfficeId, Project project) {
		try {
			if (cd != null ) {
				LOG.debug("");
				String projectId = ("" + project.getId());
				DateFormat df = new SimpleDateFormat("MMdd-HHmmss");
				String timestamp = df.format(new Date());
				String realReportPath = SessionUtils.getRealReportPath();
				
				// Print reports for the ContactDocument/Attachments
				String fileNamePdf = null;
				cd = ContactDocumentDAO.getInstance().refresh(cd);
				User user = cd.getContact().getUser();
				user = UserDAO.getInstance().refresh(user);
				fileNamePdf = DocumentService.printContactDocument(cd, user, projectId, timestamp, SessionUtils.getNonSystemProduction());
				//String outputFileName = fileNamePdf;
				//Merge Attachments
				/*if (fileNamePdf != null) {
					LOG.debug("");
					if (cd.getAttachments() != null && (! cd.getAttachments().isEmpty())) {
						String fileWithAttachments = "";
						Integer badAttachmentCount = 0;
						fileWithAttachments = DocumentService.mergeAttachments(badAttachmentCount,
								 cd, fileNamePdf, user);
						outputFileName = fileWithAttachments;
					}
				}*/
				
				Office office = OfficeDAO.getInstance().findById(OfficeId);
				String officeEmail = office.getEmailAddress();
				// Send to selected office and group all docs into one file.
				if (officeEmail != null && (! officeEmail.isEmpty())) {
					Collection<String> fileNames = new ArrayList<>();
					fileNames.add(realReportPath + fileNamePdf);
					Collection<String> emailList = new ArrayList<>();
					// Replace semi-colon with commas
					if (officeEmail != null) { 
						officeEmail = officeEmail.replaceAll(";", ",");
						// Parse multiple email address. LS-1649
						emailList.addAll(parseEmailAddressByDelimitter(officeEmail, ","));
					}
					DoNotification.getInstance().sendIntentForm(emailList, fileNames, project);
				}
				List<ContactDocument> cdList = new ArrayList<>();
				cdList.add(cd);
				boolean transferred = transmitData(project, cdList, SessionUtils.getNonSystemProduction(), true);
				if (transferred) {
					MsgUtils.addFacesMessage("FormActraIntent.TransferSuccessful", FacesMessage.SEVERITY_INFO,
							cdList.size());
				}
				else {
					MsgUtils.addFacesMessage("FormActraIntent.TransferError", FacesMessage.SEVERITY_INFO);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}
	
	public static List<String> parseEmailAddressByDelimitter(String emailString, String delimitter) {
		List<String> emailAddress = new ArrayList<>(Arrays.asList(emailString.split(delimitter)));
		return emailAddress;
	}
	
	/** Performs transmission of selected documents
	 * to the payroll service.
	 * @param project
	 * @return true if transmission is successful else returns false.
	 */
	public static boolean transmitData(Project project, List<ContactDocument> cdList, Production production, boolean forIntent) {
		LOG.debug("");
		boolean ret = true;
		Date date = new Date();
		List<ContactDocument> transferDocs = new ArrayList<>();
		ContactDocumentDAO contactDocumentDAO = ContactDocumentDAO.getInstance();
		for (ContactDocument cd : cdList) {
			cd = contactDocumentDAO.refresh(cd);
			cd.setTimeSent(date);
			cd.setChecked(false);
			cd.setLastSent(date);
			ContactDocumentDAO.getInstance().attachDirty(cd);
			transferDocs.add(cd);
		}
		PayrollService service = production.getPayrollPref().getPayrollService();
		if (forIntent || service.getSendBatchMethod() == ServiceMethod.TEAM_FTP) {
			Collection<Collection<ContactDocument>> docs = new ArrayList<>();
			docs.add(transferDocs);
			DocumentTransferService transferService = DocumentTransferService.getInstance();
			ret = transferService.transmit(docs, project, ExportType.FULL_TAB, ReportStyle.TC_AICP_TEAM, false, false);
		}
		return ret;
	}

	/**
	 * The comparator for sorting the list of ContactDocument's into the
	 * appropriate order for the document transfer. The documents will be
	 * ordered by the Contact, then by Employment, and finally by the ordinal
	 * value of the formType (enumeration) field.
	 */
	private static Comparator<ContactDocument> getTransferComparator() {
		return transferComparator;
	}

	private static Comparator<ContactDocument> transferComparator = new Comparator<ContactDocument>() {
		@Override
		public int compare(ContactDocument c1, ContactDocument c2) {
			return c1.compareTo(c2, ContactDocument.SORTKEY_TRANSFER, true);
		}
	};

}
