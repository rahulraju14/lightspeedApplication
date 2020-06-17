package com.lightspeedeps.web.admin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ContactDocumentDAO;
import com.lightspeedeps.dao.PayrollServiceDAO;
import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.dao.WeeklyTimecardDAO;
import com.lightspeedeps.model.ContactDocument;
import com.lightspeedeps.model.PayrollService;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.port.FlatExporter;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.web.view.View;

/**
 * Backing bean for the Admin "WeeklyTimecard" page, which is used to generate discount
 * weeklyTimecard codes that are stored in the database.
 */
@ManagedBean
@ViewScoped
public class BillingReportBean extends View implements Serializable {
	/** */
	private static final long serialVersionUID = 6256483727646243011L;

	private static final Log log = LogFactory.getLog(BillingReportBean.class);

	private static final int REPORT_INVOICE = 1;
	private static final int REPORT_FULL = 2;

	// Type of report to generate, Timecards or Documents
	private static final int REPORT_TYPE_TIMECARDS = 0;
	private static final int REPORT_TYPE_DOCUMENTS = 1;

	private static final BigDecimal DEFAULT_TIMECARD_RATE = new BigDecimal("0.1"); // 0.1% is default fee rate
	private static final BigDecimal DEFAULT_DOCUMENT_FEE= new BigDecimal("1.5"); // $1.50 default fee

	// Fields

	/** The currently displayed List of WeeklyTimecard`s */
	private List<WeeklyTimecard> timecardList = new ArrayList<>();

	/** The currently selected (Detail tab) WeeklyTimecard. */
	private WeeklyTimecard weeklyTimecard;

	/** The currently displayed list of "ContactDocument"s */
	List<ContactDocument>docList;

	/** Generator input: id number of production to select timecards from. */
	private Integer productionId;

	/** Generator input: year and month (YYYYMM) to match week-ending dates
	 * of selected timecards. */
	private String yearMonth;

	/** Generator input: plain text or regular expression to match production company name(s). */
	private String prodCompPattern = "%";

	/** Generator input: The payroll service ID number of selected timecards. */
	private Integer serviceId;

	private BigDecimal totalGross = BigDecimal.ZERO;

	private transient WeeklyTimecardDAO weeklyTimecardDAO;

	private transient ContactDocumentDAO contactDocumentDAO;

	/** Type of report being generated: Timecards or Documents */
	private int reportType;

	/* Constructor */
	public BillingReportBean() {
		super("WeeklyTimecard.");

		reportType = REPORT_TYPE_TIMECARDS;
		log.debug("");
	}

	/**
	 * Action method for the "Generate" button. We validate all the parameters,
	 * and if they're OK we execute a query to retrieve the matching timecards.
	 *
	 * @return null navigation string
	 */
	public String actionGenerate() {
		if (productionId != null) {
			Production prod = ProductionDAO.getInstance().findById(productionId);
			if (prod == null) {
				MsgUtils.addFacesMessage("Timecard.Billing.InvalidProduction", FacesMessage.SEVERITY_ERROR);
				return null;
			}
		}
		if (serviceId != null) {
			PayrollService service = PayrollServiceDAO.getInstance().findById(serviceId);
			if (service == null) {
				MsgUtils.addFacesMessage("Timecard.Billing.InvalidService", FacesMessage.SEVERITY_ERROR);
				return null;
			}
		}
		if (StringUtils.isEmpty(yearMonth)) {
			MsgUtils.addFacesMessage("Timecard.Billing.MissingDate", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		yearMonth = yearMonth.trim();
		if (yearMonth.length() <= 6 && ! yearMonth.startsWith("20")) {
			yearMonth = "20" + yearMonth;
		}
		int yrMoDay = 0;
		int year = 0;
		try {
			yrMoDay = Integer.parseInt(yearMonth);
		}
		catch (NumberFormatException e) {
		}
		if (yearMonth.length() == 6) {
			int month = yrMoDay % 100;
			year = (yrMoDay-month) / 100;
			if (month == 0 || month > 12) {
				MsgUtils.addFacesMessage("Timecard.Billing.InvalidDate", FacesMessage.SEVERITY_ERROR);
				return null;
			}
		}
		else if (yearMonth.length() == 4) {
			year = yrMoDay;
		}
		else if (yearMonth.length() == 8) {
			int day = yrMoDay % 100;
			yrMoDay = (yrMoDay-day) / 100;
			int month = yrMoDay % 100;
			year = (yrMoDay-month) / 100;
			if (day == 0 || month > 31) {
				MsgUtils.addFacesMessage("Timecard.Billing.InvalidDate", FacesMessage.SEVERITY_ERROR);
				return null;
			}
			if (month == 0 || month > 12) {
				MsgUtils.addFacesMessage("Timecard.Billing.InvalidDate", FacesMessage.SEVERITY_ERROR);
				return null;
			}
		}
		if (year < 2000 || year > 2020) {
			MsgUtils.addFacesMessage("Timecard.Billing.InvalidDate", FacesMessage.SEVERITY_ERROR);
			return null;
		}

		if (productionId == null && serviceId == null && StringUtils.isEmpty(prodCompPattern)) {
			MsgUtils.addFacesMessage("Timecard.Billing.MissingRequired", FacesMessage.SEVERITY_ERROR);
			return null;
		}

		doQuery();
		totalGross = BigDecimal.ZERO;
		for (WeeklyTimecard wtc : timecardList) {
			if (wtc.getGrandTotal() != null) {
				totalGross = totalGross.add(wtc.getGrandTotal());
			}
		}

		if (timecardList.size() == 0) {
			MsgUtils.addFacesMessage("Timecard.Billing.NoTimecardsMatch", FacesMessage.SEVERITY_ERROR);
		}

		return null;
	}

	/**
	 * Action method for the "Generate" button. We validate all the parameters,
	 * and if they're OK we execute a query to retrieve the matching ContactDocuments.
	 *
	 * @return null navigation string
	 */
	public String actionGenerateDocList() {
		// Validate filters
		if (productionId != null) {
			Production prod = ProductionDAO.getInstance().findById(productionId);
			if (prod == null) {
				MsgUtils.addFacesMessage("Timecard.Billing.InvalidProduction", FacesMessage.SEVERITY_ERROR);
				return null;
			}
		}
		if (serviceId != null) {
			PayrollService service = PayrollServiceDAO.getInstance().findById(serviceId);
			if (service == null) {
				MsgUtils.addFacesMessage("Timecard.Billing.InvalidService", FacesMessage.SEVERITY_ERROR);
				return null;
			}
		}
		if (StringUtils.isEmpty(yearMonth)) {
			MsgUtils.addFacesMessage("Timecard.Billing.MissingDate", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		yearMonth = yearMonth.trim();
		if (yearMonth.length() <= 6 && ! yearMonth.startsWith("20")) {
			yearMonth = "20" + yearMonth;
		}
		int yrMoDay = 0;
		int year = 0;
		try {
			yrMoDay = Integer.parseInt(yearMonth);
		}
		catch (NumberFormatException e) {
		}
		if (yearMonth.length() == 6) {
			int month = yrMoDay % 100;
			year = (yrMoDay-month) / 100;
			if (month == 0 || month > 12) {
				MsgUtils.addFacesMessage("Timecard.Billing.InvalidDate", FacesMessage.SEVERITY_ERROR);
				return null;
			}
		}
		else if (yearMonth.length() == 4) {
			year = yrMoDay;
		}
		else if (yearMonth.length() == 8) {
			int day = yrMoDay % 100;
			yrMoDay = (yrMoDay-day) / 100;
			int month = yrMoDay % 100;
			year = (yrMoDay-month) / 100;
			if (day == 0 || month > 31) {
				MsgUtils.addFacesMessage("Timecard.Billing.InvalidDate", FacesMessage.SEVERITY_ERROR);
				return null;
			}
			if (month == 0 || month > 12) {
				MsgUtils.addFacesMessage("Timecard.Billing.InvalidDate", FacesMessage.SEVERITY_ERROR);
				return null;
			}
		}
		if (year < 2000 || year > 2020) {
			MsgUtils.addFacesMessage("Timecard.Billing.InvalidDate", FacesMessage.SEVERITY_ERROR);
			return null;
		}

		if (productionId == null && serviceId == null && StringUtils.isEmpty(prodCompPattern)) {
			MsgUtils.addFacesMessage("Timecard.Billing.MissingRequired", FacesMessage.SEVERITY_ERROR);
			return null;
		}

		doDocsQuery();

		if (docList.size() == 0) {
			MsgUtils.addFacesMessage("Timecard.Billing.NoDocumentsMatch", FacesMessage.SEVERITY_ERROR);
		}

		return null;
	}
	/**
	 * Generate a file containing the weeklyTimecards in the current list, with
	 * only the fields used for the invoicing report.
	 *
	 * @return null navigation string
	 */
	public String actionExport() {
		if (timecardList.size() == 0) {
			MsgUtils.addFacesMessage("Timecard.Billing.NoTimecardsMatch", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		if (export(REPORT_INVOICE) == null) {
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Generate a file containing the weeklyTimecards in the current list, with
	 * the fields for the invoicing report plus additional fields for audit or
	 * debugging purposes, such as timecard id number and user account.
	 *
	 * @return null navigation string
	 */
	public String actionExportFull() {
		if (timecardList.size() == 0) {
			MsgUtils.addFacesMessage("Timecard.Billing.NoDocumentsMatch", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		if (export(REPORT_FULL) == null) {
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Generate a file containing the contact documents in the current list, with
	 * the fields for the invoicing report plus additional fields for audit or
	 * debugging purposes, such as contact document id number and user account.
	 *
	 * @return null navigation string
	 */
	public String actionExportDocList() {
		if (docList != null && docList.size() == 0) {
			MsgUtils.addFacesMessage("Timecard.Billing.NoDocumentsMatch", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		if (exportDocList() == null) {
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}
	public void listenReportTypeChange(ValueChangeEvent event) {
		if(event != null) {
			reportType = (int)event.getNewValue();
		}
	}

	/**
	 * Create an export file (tab-delimited) containing the current list of
	 * ContactDocuments. Sends a JavaScript command to the browser to open the
	 * exported file.
	 *
	 * @return The generated filename of the file created.
	 */
	@SuppressWarnings("null")
	private String exportDocList() {
		String fileLocation = null;
		try {
			DateFormat df = new SimpleDateFormat("MM/dd/yyyy"); // for W/E dates in export file

			// Create file name
			String timestamp = new SimpleDateFormat("yyyy-MM").format(docList.get(0).getDelivered());
			String reportFileName = "documentBilling"+ "_" + timestamp;

			reportFileName += ".tab";
			log.debug(reportFileName);
			fileLocation = Constants.REPORT_FOLDER + "/" + reportFileName;
			reportFileName = SessionUtils.getRealReportPath() + reportFileName;

			// Get stream on file, and Exporter on stream...
			OutputStream outputStream = new FileOutputStream(new File(reportFileName));
			FlatExporter exp = new FlatExporter(outputStream);


			BigDecimal fee  = DEFAULT_DOCUMENT_FEE;; // calculated fee for a given timecard
			String prodId = ""; // productionId (string) of production containing current timecard
			Production prod = null; // Production containing current timecard
			PayrollService payrollService = null; // PayrollService of current Production (if any; may be null)

			for (ContactDocument cd : docList) {
				// Process all contact documents, exporting one billing record for each one.
				cd = ContactDocumentDAO.getInstance().refresh(cd);
				if (! prodId.equals(cd.getContact().getProduction().getProdId())) {
					// Production has changed since the prior timecard.
					prodId = cd.getContact().getProduction().getProdId();
					// ... get current Production instance
					prod = ProductionDAO.getInstance().findByProdId(prodId);
					log.debug("PRODUCTION : " + prod);
					// ... get current payrollService instance, if any
					payrollService = prod.getPayrollPref().getPayrollService();
					// Recalculate fees for documents in this Production...
					if (prod.getDocumentFeeAmount() != null && prod.getDocumentFeeAmount().signum() > 0) {
						fee = prod.getDocumentFeeAmount();
						log.debug(fee);
					}
					else if (payrollService != null && payrollService.getDocumentFeeAmount() != null &&
							payrollService.getDocumentFeeAmount().signum() > 0) {
						fee = payrollService.getDocumentFeeAmount();
						log.debug(fee);
					}
					else { // use default rate (as a percentage, i.e., 0.1% = factor of 0.001)
						fee = DEFAULT_DOCUMENT_FEE;
					}
				}

				exp.append(cd.getId());
				exp.append(cd.getProduction().getProdId());
				exp.append(cd.getContact().getProduction().getStudio());
				exp.append(cd.getContact().getProduction().getTitle());
				exp.append(df.format(cd.getDelivered()));
				exp.append(cd.getDocument().getName());
				if (cd.getEmployment() != null && cd.getEmployment().getStartForm() != null) {
					exp.append(cd.getEmployment().getStartForm().getViewSSN());
				}
				else {
					exp.append("???-??-????");
				}
				exp.append(cd.getContact().getUser().getAccountNumber());
				exp.append(cd.getContact().getUser().getLastName());
				exp.append(cd.getContact().getUser().getFirstName());

				if (cd.getEmployment() != null && cd.getEmployment().getRole() != null) {
					exp.append(cd.getEmployment().getRole().getName());
				}
				else {
					exp.append("?missing role?");
				}

				exp.append(cd.getStatus().getLabel());

				exp.append(prod.getType().name());

				exp.append(fee);
				exp.next();
			}

			outputStream.close();
			// open in "same window" ('_self'), since user should get prompt to save as file
			String javascriptCode = "window.open('../../" + fileLocation
					+ "','LS_billingDocuments');";
			addJavascript(javascriptCode);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			fileLocation = null;
		}

		return fileLocation;
	}

	/**
	 * Create an export file (tab-delimited) containing the current list of
	 * weeklyTimecards. Sends a JavaScript command to the browser to open the
	 * exported file.
	 *
	 * @return The generated filename of the file created.
	 */
	@SuppressWarnings("null")
	private String export(int pReportType) {
		String fileLocation = null;
		try {
			DateFormat df = new SimpleDateFormat("MM/dd/yyyy"); // for W/E dates in export file
			// Calculated average gross pay values for timecards in list
			BigDecimal unionAvg = BigDecimal.ZERO, nonUnionAvg = BigDecimal.ZERO;
			int unionCount = 0, nonUnionCount = 0;

			// Using all timecards in list with a non-zero gross, calculate the average gross
			for (WeeklyTimecard wtc : timecardList) {
				if (wtc.getGrandTotal() != null && wtc.getGrandTotal().signum() > 0) {
					// do separate totals/averages for union vs non-union timecards
					if (wtc.isNonUnion()) {
						nonUnionCount++;
						nonUnionAvg = nonUnionAvg.add(wtc.getGrandTotal());
					}
					else {
						unionCount++;
						unionAvg = unionAvg.add(wtc.getGrandTotal());
					}
				}
			}
			if (nonUnionCount > 0) {
				nonUnionAvg = nonUnionAvg.divide(new BigDecimal(nonUnionCount),4,RoundingMode.HALF_UP);
			}
			if (unionCount > 0) {
				unionAvg = unionAvg.divide(new BigDecimal(unionCount),4,RoundingMode.HALF_UP);
			}

			// Create file name
			String timestamp = new SimpleDateFormat("yyyy-MM").format(timecardList.get(0).getEndDate());
			String reportFileName = "timecardBilling"+ "_" + timestamp;
			if (pReportType == REPORT_FULL) {
				reportFileName += "_full";
			}
			reportFileName += ".tab";
			log.debug(reportFileName);
			fileLocation = Constants.REPORT_FOLDER + "/" + reportFileName;
			reportFileName = SessionUtils.getRealReportPath() + reportFileName;

			// Get stream on file, and Exporter on stream...
			OutputStream outputStream = new FileOutputStream(new File(reportFileName));
			FlatExporter exp = new FlatExporter(outputStream);

			int count = 1;
			BigDecimal fee; // calculated fee for a given timecard
			BigDecimal rate = DEFAULT_TIMECARD_RATE; // rate used to calculate fee, as percentage
			String prodId = ""; // productionId (string) of production containing current timecard
			Production prod = null; // Production containing current timecard
			PayrollService payrollService = null; // PayrollService of current Production (if any; may be null)

			for (WeeklyTimecard wtc : timecardList) {
				// Process all timecards, exporting one billing record for each one.

				if (! prodId.equals(wtc.getProdId())) {
					// Production has changed since the prior timecard.
					prodId = wtc.getProdId();
					// ... get current Production instance
					prod = ProductionDAO.getInstance().findByProdId(prodId);
					log.debug("PRODUCTION : " + prod);
					// ... get current payrollService instance, if any
					payrollService = prod.getPayrollPref().getPayrollService();
					// Recalculate rate for timecards in this Production...
					if (prod.getTimecardFeePercent() != null && prod.getTimecardFeePercent().signum() > 0) {
						rate = prod.getTimecardFeePercent();
						log.debug(rate);
					}
					else if (payrollService != null && payrollService.getTimecardFeePercent() != null &&
							payrollService.getTimecardFeePercent().signum() > 0) {
						rate = payrollService.getTimecardFeePercent();
						log.debug(rate);
					}
					else { // use default rate (as a percentage, i.e., 0.1% = factor of 0.001)
						rate = DEFAULT_TIMECARD_RATE;
					}
				}

				exp.append(count++);
				exp.append(wtc.getProdCo());
				exp.append(wtc.getProdName());
				exp.append(df.format(wtc.getEndDate()));
				exp.append(wtc.getLastName());
				exp.append(wtc.getFirstName());
				String str = wtc.getSocialSecurity();
				if (str != null && str.length() == 9) {
					str = "#-" + str.substring(5);
					//str = "\"-" + str.substring(5) + "\"";
				}
				exp.append(str);
				exp.append(wtc.getOccupation());
				exp.append(wtc.getLoanOutCorp());
				exp.append(wtc.getFedCorpIdFmtd());
				exp.append(wtc.getStateCorpId());
				exp.append(rate,4);
				exp.append(wtc.getGrandTotal());
				if (wtc.getGrandTotal() != null && wtc.getGrandTotal().signum() > 0) {
					// timecard has a total gross pay, so calculate fee based on that
					fee = wtc.getGrandTotal().divide(Constants.DECIMAL_100).multiply(rate);
				}
				else { // no gross pay on timecard, use average gross computed earlier
					if (wtc.isNonUnion()) {
						fee = nonUnionAvg.divide(Constants.DECIMAL_100).multiply(rate);
					}
					else {
						fee = unionAvg.divide(Constants.DECIMAL_100).multiply(rate);
					}
				}
				exp.append(fee,4); // output the calculated fee
				exp.append(wtc.getUnionNumber());
				if (pReportType == REPORT_FULL) {
					exp.append(wtc.getProdId());
					if (wtc.getWeeklyBatch() != null) {
						exp.append(wtc.getWeeklyBatch().getName());
					}
					else {
						exp.append("");
					}
					exp.append(wtc.getId());
					exp.append(wtc.getUserAccount());
					exp.append(wtc.getStatus().name());
				}
				exp.append(prod.getType().name());
				exp.next();
			}
			outputStream.close();
			// open in "same window" ('_self'), since user should get prompt to save as file
			String javascriptCode = "window.open('../../" + fileLocation
					+ "','LS_billingTimecards');";
			addJavascript(javascriptCode);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			fileLocation = null;
		}

		return fileLocation;
	}

	/**
	 * Force lazy-loading of fields to be displayed in JSP.
	 */
	private void forceLazyInit() {
		if (weeklyTimecard != null) {
			weeklyTimecard = WeeklyTimecardDAO.getInstance().refresh(weeklyTimecard);
			if (weeklyTimecard != null) {
				initWeeklyTimecard(weeklyTimecard);
			}
		}
		for (WeeklyTimecard wtc : timecardList) {
			initWeeklyTimecard(wtc);
		}
	}

	/**
	 * Force lazy-loading of fields to be displayed in JSP.
	 * @param wtc The WeeklyTimecard whose fields should be pre-loaded.
	 */
	private void initWeeklyTimecard(WeeklyTimecard wtc) {
		try {
			wtc.getWeeklyBatch().getName();
		}
		catch (Exception e) { // catch errors if referenced object has been deleted.
		}
		return;
	}

	/**
	 * Create a query matching the user's selections, and execute it.
	 */
	@SuppressWarnings("unchecked")
	private void doQuery() {

		List<Object> values = new ArrayList<>();

		String query = "select w from WeeklyTimecard w, Production p "
				+ " where updated is not null " //  omit Global preference records
				+ " and w.prodId = p.prodId " //  match timecards to their Production
				+ " and (w.status <> 'open' and w.status <> 'void') " // include all others
				+ "";

		query = addQuery( query, createQuery(values));

		query += " order by w.prodCo, w.prodName, w.endDate, w.lastName, w.firstName ";

		timecardList = getWeeklyTimecardDAO().find(query, values.toArray());

		log.debug("# of results: " + timecardList.size());

		forceLazyInit();
	}

	/**
	 * Create a query matching the user's selections, and execute it.
	 */
	@SuppressWarnings("unchecked")
	private void doDocsQuery() {

		List<Object> values = new ArrayList<>();

		String query = "select cd from ContactDocument cd, Production p "
				+ " where cd.contact.production.prodId = p.prodId " //  match timecards to their Production
				+ " and cd.status <> 'pending' " // include all others
				+ "";

		query = addQuery( query, createQuery(values));

		query += " order by p.studio, p.title, cd.document.id, cd.delivered ";

		docList = getContactDocumentDAO().find(query, values.toArray());

		log.debug("# of results: " + docList.size());

		// Initialize the start forms collection
		for(ContactDocument cd : docList) {
			cd.getEmployment().getStartForms().size();
			cd.getContact().getUser().getAccountNumber();
		}
	}

	/**
	 * Build the variable portions of the query; the returned string is
	 * dependent on which parameters were supplied by the user.
	 * <p>
	 * Note that the query assumes these table references have been
	 * defined: p:Production, cd:ContactDocument, w:WeeklyTimecard
	 *
	 * @param values An empty List of Objects to which this method will add the
	 *            objects as required by the query string created.
	 * @return The variable portion of the query string; for each "?" in the
	 *         returned string, an Object has been added to the 'values'
	 *         parameter.
	 */
	private String createQuery(List<Object> values) {
		String q = "";

		if (serviceId != null && serviceId != 0) {
			q = addQuery( q, "p.payrollPref.payrollService.id = ? " );
			values.add(serviceId);
		}
		if (productionId != null && productionId != 0) {
			q = addQuery( q, "p.id = ? " );
			values.add(productionId);
		}

		String dateField = "w.endDate";
		if (reportType == REPORT_TYPE_TIMECARDS) {
			if (prodCompPattern != null && prodCompPattern.trim().length() > 0) {
				prodCompPattern = prodCompPattern.trim();
				q = addQuery(q, "w.prodCo like ? ");
				values.add(prodCompPattern);
			}
		}
		else if (reportType == REPORT_TYPE_DOCUMENTS) {
			dateField = "cd.delivered";
			if (prodCompPattern != null && prodCompPattern.trim().length() > 0) {
				prodCompPattern = prodCompPattern.trim();
				q = addQuery(q, "p.studio like ? ");
				values.add(prodCompPattern);
			}
		}

		if (yearMonth != null && yearMonth.trim().length() >= 4) {
			yearMonth = yearMonth.trim();
			String pattern = "%Y%m%d"; // default date pattern
			if (yearMonth.length() == 4) {
				pattern = "%Y"; // only year
			}
			else if (yearMonth.length() == 6) {
				pattern = "%Y%m"; // year and month
			}
			q = addQuery( q, "date_format(" + dateField + ",'" + pattern + "') = ? " );
			values.add(yearMonth);
		}

		return q;
	}

	/**
	 * Combine two portions of an SQL query, adding an " and " between them if
	 * both are not empty.
	 *
	 * @param q1
	 * @param q2
	 * @return If either q1 or q2 is empty, then the other parameter is returned
	 *         unchanged. If both are not empty, the returned string is 'q1 +
	 *         " and " + q2'.
	 */
	private String addQuery(String q1, String q2) {
		if ( q1.length() > 0) {
			if (q2.length() > 0) {
				q1 += " and " + q2;
			}
			return q1;
		}
		return q2;
	}

	public List<WeeklyTimecard> getWeeklyTimecardList() {
		return timecardList;
	}
	public void setWeeklyTimecardList(List<WeeklyTimecard> list) {
		timecardList = list;
	}

	public int getListSize() {
		if (reportType == REPORT_TYPE_TIMECARDS) {
			if (timecardList != null) {
				return timecardList.size();
			}
		}
		else {
			if (docList != null) {
				return docList.size();
			}
		}
		return 0;
	}

	public WeeklyTimecard getWeeklyTimecard() {
		return weeklyTimecard;
	}
	/** See {@link #totalGross}. */
	public BigDecimal getTotalGross() {
		return totalGross;
	}
	/** See {@link #totalGross}. */
	public void setTotalGross(BigDecimal totalGross) {
		this.totalGross = totalGross;
	}

	public void setWeeklyTimecard(WeeklyTimecard weeklyTimecard) {
		this.weeklyTimecard = weeklyTimecard;
	}

	/** See {@link #productionId}. */
	public Integer getProductionId() {
		return productionId;
	}
	/** See {@link #productionId}. */
	public void setProductionId(Integer quantity) {
		productionId = quantity;
	}

	/** See {@link #yearMonth}. */
	public String getYearMonth() {
		return yearMonth;
	}
	/** See {@link #yearMonth}. */
	public void setYearMonth(String prefix) {
		yearMonth = prefix;
	}

	/** See {@link #prodCompPattern}. */
	public String getProdCompPattern() {
		return prodCompPattern;
	}
	/** See {@link #prodCompPattern}. */
	public void setProdCompPattern(String skuPattern) {
		prodCompPattern = skuPattern;
	}

	/** See {@link #serviceId}. */
	public Integer getServiceId() {
		return serviceId;
	}
	/** See {@link #serviceId}. */
	public void setServiceId(Integer useCount) {
		serviceId = useCount;
	}

	/** See {@link #reportType}. */
	public int getReportType() {
		return reportType;
	}

	/** See {@link #reportType}. */
	public void setReportType(int reportType) {
		this.reportType = reportType;
	}

	/** See {@link #docList}. */
	public List<ContactDocument> getDocList() {
		return docList;
	}

	/** See {@link #docList}. */
	public void setDocList(List<ContactDocument> docList) {
		this.docList = docList;
	}

	private WeeklyTimecardDAO getWeeklyTimecardDAO() {
		if (weeklyTimecardDAO == null) {
			weeklyTimecardDAO = WeeklyTimecardDAO.getInstance();
		}
		return weeklyTimecardDAO;
	}

	private ContactDocumentDAO getContactDocumentDAO() {
		if (contactDocumentDAO == null) {
			contactDocumentDAO = ContactDocumentDAO.getInstance();
		}
		return contactDocumentDAO;
	}
}
