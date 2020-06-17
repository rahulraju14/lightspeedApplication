/**
 * File: TransferService.java
 */
package com.lightspeedeps.service;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lightspeedeps.dao.PayrollPreferenceDAO;
import com.lightspeedeps.message.HttpUtils;
import com.lightspeedeps.model.*;
import com.lightspeedeps.service.WeeklyBatchService.NoPayInfoModule;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.StringUtils;

/**
 * This class handles transmitting a group of documents (timecards or start
 * documents), typically to a payroll service, via either email and/or FTP.
 * (Options may be set, however, to open the PDF in a browser window instead of
 * e-mailing it.)
 * <p>
 * Subclasses implement methods that may be specific to the type of document
 * being transmitted.
 */
public abstract class TransferService<T, TD> extends BaseService {

	private static final Log log = LogFactory.getLog(TransferService.class);

	// Although the FTP file prefixes are specific, in some sense, to the subclasses,
	// having all the possible values here makes it easier to see what's in use.

	/** FTP file prefix for onboarding documents. */
	protected final static String FTP_DOC_PREFIX = "LD"; 		// standard onboarding documents
	/** FTP file prefix for timecards. */
	protected final static String FTP_TIMECARD_PREFIX = "LS"; // standard crew timecards
	/** FTP file prefix for Tours timecards. */
	protected final static String FTP_TOURS_PREFIX = "LT"; 	// timecards from a Tours production
	/** FTP file prefix for a Project file. */
	protected final static String FTP_PROJECT_PREFIX = "PR"; 	// 'Project' information transfer

	/** The Production whose documents (timecards or start forms) are being processed. */
	protected Production production;

	/** The Project whose documents (timecards or start forms) are being processed. This
	 * will be null for non-Commercial productions. */
	protected Project project;

	/** The current PayrollPreference instance.  This will be the instance related to the
	 * Project for Commercial productions, or the one related to the Production for all other types
	 * of productions. */
	protected PayrollPreference preferences;

	/** The PayrollService for the Production being processed; this is defined in the
	 * Production's PayrollPreference, even for a Commercial production. */
	protected PayrollService service;

	/** The ExportType defined by the ServiceMethod assigned to the current PayrollService. */
	protected ExportType exportType;

	/** The ReportStyle that will be used when generating the PDFs (if any) of the
	 * documents being transferred.  This currently applies only to timecards, as
	 * any Start document will have its own unique PDF report.  For some transfer
	 * methods, the user may be given the option of selecting the report style. */
	protected ReportStyle reportStyle;

	/** The prefix to be use when constructing FTP file names. Value should be set
	 * by the subclass. */
	private String ftpPrefix = "XX";

	/** True iff email is to be used to deliver the generated documents. */
	protected boolean useEmail;

	/** True if the payroll service being used is marked as a Team service.
	 * This is set by {@link #prepareBatch(WeeklyBatch)}. */
	protected boolean isTeamPayroll = false;

	/** True if payroll data is related to specific projects.  Currently this is
	 * equivalent to being a Commercial production.  Value is set equal to the
	 * Production.type.hasPayrollByProject() return value. */
	protected boolean byProject = false;

	/** True iff PayBreakdown information should be included in the files (PDFs and
	 * data files) generated for transmission to the payroll service.  This is set from
	 * the Production preference 'includeBreakdown'. */
	protected boolean payInfo = true;

	/**
	 * Transfer (transmit/send) a collection of WeeklyBatch`s or
	 * ContactDocument`s to the appropriate payroll service. Depending on the
	 * payroll service, this could be done as either a single transmission, or
	 * one per batch.
	 *
	 * @param batches The collection of WeeklyBatch or ContactDocument objects
	 *            to transfer.
	 * @param pproj The Project related to the set of documents being
	 *            transferred.
	 * @param type The type (format) of export file to be generated.
	 * @param style The style (layout) of timecard PDF report to be generated.
	 * @param email True iff the generated files should be emailed to the
	 *            payroll service; if false, the files are delivered to the
	 *            current user by opening a browser window on the file.
	 * @return True if the documents were successful sent, false otherwise.
	 */
	public boolean transmit(Collection<T> batches, Project pproj, ExportType type,
			ReportStyle style, boolean email, boolean isProjectTransmit) {
		boolean ret = false;

		try {
			init(pproj, type, style, email);
			if (service != null) {
				ret = true;
				batches = prepareBatches(batches); // fill batches with transmission information
				for (T batch : batches) {
					ret &= transferBatch(batch);
				}
			}
			else {
				MsgUtils.addFacesMessage("WeeklyBatch.Transmit.NoService", FacesMessage.SEVERITY_ERROR);
				return false;
			}
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("WeeklyBatch.Transmit.Error", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(this, e);
			ret = false;
		}
//		if (ret && ! isProjectTransmit) { // Project transfer is no longer needed. 12/11/18
//			// don't call transmit-project if we're already doing that!
//			DocumentTransferService.getInstance().checkTransmitProject(project);
//		}
		return ret;
	}

	/**
	 * Initialize the TransferService based on the passed parameters and the
	 * current Production's payroll settings.
	 *
	 * @param pproj The Project related to the set of documents being
	 *            transferred.
	 * @param type The type (format) of export file to be generated.
	 * @param style The style (layout) of timecard PDF report to be generated.
	 * @param email True iff the generated files should be emailed to the
	 *            payroll service; if false, the files are delivered to the
	 *            current user by opening a browser window on the file.
	 */
	protected void init(Project pproj, ExportType type,
			ReportStyle style, boolean email) {
		project = pproj;
		exportType = type;
		reportStyle = style;
		useEmail = email;
		try {
			production = SessionUtils.getNonSystemProduction();
			if (production != null) {
				preferences = production.getPayrollPref();
				payInfo = preferences.getIncludeBreakdown();
				byProject = production.getType().hasPayrollByProject();
				if (byProject) {
					preferences = project.getPayrollPref();
				}
				preferences = PayrollPreferenceDAO.getInstance().refresh(preferences);
				service = production.getPayrollPref().getPayrollService();
				if (service != null) {
					isTeamPayroll = service.getTeamPayroll();
				}
			}
		}
		catch (Exception e) {
			MsgUtils.addFacesMessage("WeeklyBatch.Transmit.Error", FacesMessage.SEVERITY_ERROR);
			EventUtils.logError(this, e);
		}
	}

	/**
	 * Transfer one "batch" (of type T) to its intended recipient. This includes
	 * generating any PDF and/or data file, and the sending of the file(s).
	 *
	 * @param byProject True if the document transfer should be split up based
	 *            on the source Project of each document. When true, at least
	 *            one 'transfer' operation will be done for each Project
	 *            included within the batch of documents. If false, only a
	 *            single transfer will be done, regardless of the source
	 *            Project(s) of the given documents.
	 * @param batch The collection of documents to transfer; the nature of the
	 *            batch depends on the subclass.
	 * @return True iff all documents were successfully sent.
	 */
	protected boolean transferBatch(T batch) {
		Collection<Project> projects;
		if (byProject) {
			projects = findProjects(batch);
		}
		else {
			projects = new ArrayList<>();
			projects.add(project); // typically null (non-Commercial)
		}
		boolean ret = true;
		for (Project proj : projects) {
			// Create Deliverables from the items in the batch that match the given project
			List<Deliverable> delivers = createDeliverables(batch, proj);
			for (Deliverable deliver : delivers) {
				// for TEAM there will be one Deliverable for each applicable EOR
				createPDF(batch, proj, deliver);
				createDataFile(batch, proj, deliver);
			}
			ret &= sendFiles(batch, proj, delivers);
		}
		return ret;
	}

	/**
	 * Find all the Project's associated with the list of objects in the given
	 * batch.
	 *
	 * @param batch The (packet of) document(s) to be analyzed.
	 * @return A non-empty List of distinct Project's, where each Project in the list is
	 *         associated with at least one item in the given batch.
	 */
	protected abstract Collection<Project> findProjects(T batch);

	/**
	 * 'Prepare' the documents in the given collection for transfer. This may be
	 * a no-op in some cases, or may involve modifying the collection if there
	 * are restrictions on which documents should be sent. For example, a
	 * payroll service may only want to receive timecards that were updated
	 * since they were last transmitted, in which case this call should return
	 * the limited set of documents to send.
	 *
	 * @param batches The set of all batches which have been requested to
	 *            transfer.
	 * @return The set of batches which will continue through the transfer
	 *         process. This may be the same set as the passed parameter, or it
	 *         may be a newly-constructed collection.
	 */
	protected abstract Collection<T> prepareBatches(Collection<T> batches);

	/**
	 * Create one or more Deliverable instances describing the documents to be
	 * transferred. Must be implemented by the subclass.
	 *
	 * @param batch The batch (type T) of documents that should be described by
	 *            the returned Deliverable(s).
	 * @param proj If not null, then (typically) only documents matching this
	 *            project will be included in the returned Deliverables; other
	 *            documents within the batch will be ignored.
	 * @return A non-empty List of Deliverable instances; these will be used to
	 *         generate and send the information.
	 */
	protected abstract List<Deliverable> createDeliverables(T batch, Project proj);

	/**
	 * Create a PDF of the documents contained in the Deliverable, and place the
	 * filename into the Deliverable.
	 *
	 * @param The batch from which the Deliverable originated.
	 * @param The Project related to the batch or the documents in the
	 *            Deliverable.
	 * @param deliver The Deliverable containing the documents to be printed.
	 */
	protected abstract void createPDF(T batch, Project proj, Deliverable deliver);


	/**
	 * Create a data extract of the documents contained in the Deliverable, and
	 * place the filename into the Deliverable.
	 *
	 * @param The batch from which the Deliverable originated.
	 * @param The Project related to the batch or the documents in the
	 *            Deliverable.
	 * @param deliver The Deliverable containing the documents to be
	 *            extracted/exported.
	 */
	protected abstract void createDataFile(T batch, Project proj, Deliverable deliver);

	/**
	 * Send the files that are listed in the given Deliverable's, to the
	 * proper recipient, using the appropriate method. All of the Deliverables
	 * in the list are related to the same Project.
	 *
	 * @param batch The batch from which the Deliverable originated.
	 * @param proj The Project related to the batch or the documents in the
	 *            Deliverable.
	 * @param delivers The List of Deliverable containing the documents to be
	 *            sent, and the filenames of the PDF and data files that
	 *            (may) have been created.
	 * @return
	 */
	protected abstract boolean sendFiles(T batch, Project proj, List<Deliverable> delivers);

	/**
	 * Fill an OutputStream with the JSON representation of the given Object.
	 *
	 * @param outputStream The OutputStream to contain the JSON data. The stream
	 *            will be closed if the 'close' parameter is true.
	 * @param source The source object, possibly a WeeklyBatch or
	 *            ContactDocument, to be streamed into JSON format.
	 * @param close True if the OutputStream should be closed before returning.
	 * @return True iff the batch was successfully pushed to the stream.
	 */
	protected boolean streamAsJson(OutputStream outputStream, Object source, boolean close) {
		boolean ret = false;
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.setDateFormat(new SimpleDateFormat("MM/dd/yyyy HH:mm:ss"));
			if (! payInfo) {
				// add serializers that SKIP calculated payment data
				mapper.registerModule(new NoPayInfoModule());
			}
//			mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

			byte[] bytes = mapper.writeValueAsBytes(source);
			outputStream.write(bytes);
			ret = true;
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Transmit.StreamFailed", FacesMessage.SEVERITY_ERROR);
		}
		finally {
			if (close) {
				try {
					outputStream.close();
				}
				catch (IOException e) {
					EventUtils.logError(e);
					MsgUtils.addFacesMessage("Transmit.StreamCloseFailed", FacesMessage.SEVERITY_ERROR);
					ret = false;
				}
			}
		}
		return ret;
	}

	/**
	 * Send a set of files to the payroll service's FTP site.
	 *
	 * @param ftpFiles A non-null List of fully-qualified names of the files to
	 *            be sent.
	 * @return True if the files were all sent successfully, including the case
	 *         when the given collection is empty.
	 */
	protected boolean sendFtp(Collection<String> ftpFiles) {
		if (ftpFiles.size() == 0) {
			log.info("called with empty collection");
			return true;
		}
		String domain = service .getFtpDomain();
		String directory = service.getFtpDirectory();
		String userName = service.getFtpLoginName();
		String password = service.getFtpPassword();
		Integer port = service.getFtpPort();

		DateFormat df = new SimpleDateFormat("yyyyMMdd");
		String timestamp = df.format(new Date());
		directory += "/" + timestamp; // Date is directory name

		String suffix = StringUtils.cleanFilename(production.getTitle());
		suffix = suffix.replaceAll(" |_|-", ""); // remove blanks, underscores, hyphens
		suffix = (suffix+"XXX").substring(0, 3).toUpperCase() + ".tab";
		boolean bRet = true;
		for (String sendfile : ftpFiles) {
			File file = new File(sendfile);
			String fname = getFtpPrefix() + findNextSequence3() + suffix;
			bRet = HttpUtils.sendFile(file, fname, domain, port, directory, userName, password);
			if (!bRet) {
				break;
			}
			String msg = "FTP completed; from=" + sendfile + ", to=`" + domain + ":" + directory + "/" + fname + '`';
			EventUtils.logEvent(EventType.INFO, msg);
		}
		return bRet;
	}

	/**
	 * Create an OutputStream based on the given (relative) filename.
	 *
	 * @param reportFileName The filename of the file to be created. This file
	 *            will be created in our default "report" folder.
	 * @return An OutputStream on the newly-created file; the current
	 *         implementation returns a FileOutputStream.
	 */
	public static OutputStream createFileStream(String reportFileName) {
		OutputStream outputStream = null;
		try {
			String reportPath = SessionUtils.getRealReportPath();
			reportFileName = reportPath + reportFileName;
			outputStream = new FileOutputStream(new File(reportFileName));
		}
		catch (FileNotFoundException e) {
			EventUtils.logError(e);
		}
		return outputStream;
	}

	/**
	 * Create a filename appropriate for batch timecard transmission.
	 *
	 * @param prefix A string to be added to the front of the generated
	 *            filename; may be null or empty. If null, a default prefix is
	 *            added.
	 *
	 * @return The generated filename, which includes information from the
	 *         current {@link #production} and {@link #exportType}.
	 */
	protected String createFileName(String prefix) {
		DateFormat df = new SimpleDateFormat("_MMdd");
		String timestamp = df.format(new Date());
		String reportFileName;
		if (prefix != null) {
			reportFileName = prefix + "_";
		}
		else {
			reportFileName = "batch_";
		}
		reportFileName = reportFileName + production.getProdId() + timestamp + ApplicationUtils.findNextSequenceNumber();
		reportFileName += exportType.getExtension();
		log.debug(reportFileName);
		return reportFileName;
	}

	/**
	 * Get the next available sequence number for FTP filenames.
	 * @return The sequence number as a 3-character String.
	 */
	private String findNextSequence3() {
		String seq = "00" + ApplicationUtils.findNextSequenceNumber();
		seq = seq.substring(seq.length()-3);
		return seq;
	}

	/** See {@link #ftpPrefix}. */
	public String getFtpPrefix() {
		return ftpPrefix;
	}

	/** See {@link #ftpPrefix}. */
	public void setFtpPrefix(String ftpPrefix) {
		this.ftpPrefix = ftpPrefix;
	}

	public class Deliverable {
		/** Deliverable name, which may be used in constructing filenames for the
		 * data being delivered. */
		String name = "";
		/** Employer-of-record type. */
		private EmployerOfRecord emplOfRecord = null;
		/** The collection of documents to be delivered. */
		private List<TD> documents = new ArrayList<>();
		/** The id number of the batch containing these documents; may be null. */
		private Integer batchNumber = null;
		/** The filename of the file containing PDFs of the documents. */
		private String pdfFilename = null;
		/** The filename of the file containing the data export of the documents. */
		private String dataFilename = null;

		/**
		 * @param eor
		 */
		public Deliverable(EmployerOfRecord eor) {
			emplOfRecord = eor;
		}

		/** See {@link #name}. */
		public String getName() {
			return name;
		}
		/** See {@link #name}. */
		public void setName(String name) {
			this.name = name;
		}

		/** See {@link #emplOfRecord}. */
		public EmployerOfRecord getEmplOfRecord() {
			return emplOfRecord;
		}
		/** See {@link #emplOfRecord}. */
		public void setEmplOfRecord(EmployerOfRecord emplOfRecord) {
			this.emplOfRecord = emplOfRecord;
		}

		/** See {@link #documents}. */
		public List<TD> getDocuments() {
			return documents;
		}
		/** See {@link #documents}. */
		public void setDocuments(List<TD> documents) {
			this.documents = documents;
		}

		/** See {@link #batchNumber}. */
		public Integer getBatchNumber() {
			return batchNumber;
		}
		/** See {@link #batchNumber}. */
		public void setBatchNumber(Integer batchNumber) {
			this.batchNumber = batchNumber;
		}

		/** See {@link #pdfFilename}. */
		public String getPdfFilename() {
			return pdfFilename;
		}
		/** See {@link #pdfFilename}. */
		public void setPdfFilename(String pdfFilename) {
			this.pdfFilename = pdfFilename;
		}

		/** See {@link #dataFilename}. */
		public String getDataFilename() {
			return dataFilename;
		}
		/** See {@link #dataFilename}. */
		public void setDataFilename(String dataFilename) {
			this.dataFilename = dataFilename;
		}

	};

}
