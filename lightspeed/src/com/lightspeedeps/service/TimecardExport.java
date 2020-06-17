/**
 * TimecardExport.java
 */
package com.lightspeedeps.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lightspeedeps.dao.WeeklyTimecardDAO;
import com.lightspeedeps.dao.WeeklyTimecardDAO.TimecardRange;
import com.lightspeedeps.model.PayBreakdown;
import com.lightspeedeps.model.PayJob;
import com.lightspeedeps.model.PayrollPreference;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.port.DelimitedExporter;
import com.lightspeedeps.port.Exporter;
import com.lightspeedeps.port.FlatExporter;
import com.lightspeedeps.type.ExportType;
import com.lightspeedeps.type.PayCategory;
import com.lightspeedeps.type.TimecardSelectionType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.payroll.TimecardCalc;
import com.lightspeedeps.util.payroll.TimecardCheck;
import com.lightspeedeps.web.timecard.ExportTimecardBean;
import com.lightspeedeps.web.timecard.PrintTimecardBean;

/**
 * Class for methods related to exporting one or more timecards
 * to an external file.
 */
public class TimecardExport {
	private static final Log log = LogFactory.getLog(TimecardExport.class);

	private static final BigDecimal NT_PREM_1_10 = new BigDecimal(1.1);
	private static final BigDecimal NT_PREM_1_20 = new BigDecimal(1.2);
	private static final BigDecimal NT_PREM_1_5_10 = new BigDecimal(1.65);
	private static final BigDecimal NT_PREM_1_5_20 = new BigDecimal(1.8);

	/**
	 * Private constructor, since this class has no non-static methods.
	 */
	private TimecardExport() {
	}

	/**
	 * Create the list of timecards to be used as input to an export operation.
	 *
	 * @param bean The ExportTimecardBean instance, which will be queried to
	 *            determine the user's selection parameters.
	 * @param wtc A timecard whose values may be used as defaults for parts of
	 *            the query, e.g., the w/e date.
	 * @param production The Production containing the timecards.
	 * @param project The Project to which the timecards are related; should be
	 *            null for TV/Feature, and non-null for Commercials.
	 * @return A non-null, but possibly empty, List of timecards whose data
	 *         should be included in the export.
	 */
	public static List<WeeklyTimecard> createExportList(ExportTimecardBean bean, WeeklyTimecard wtc,
			Production production, Project project) {

		List<WeeklyTimecard> cards = null;
		WeeklyTimecardDAO weeklyTimecardDAO = WeeklyTimecardDAO.getInstance();
		Date weDate = bean.getWeekEndDate();
		boolean allDates = weDate.equals(Constants.SELECT_ALL_DATE);

		if (allDates) {
			weDate = null;
		}

		String order;
		if (bean.getSortOrder().equals(PrintTimecardBean.ORDER_ACCT)) {
			order = " account.major, account.dtl, account.set, lastName, firstName, occupation, endDate desc ";
		}
		else if (bean.getSortOrder().equals(PrintTimecardBean.ORDER_DATE)) {
			order = " endDate desc, account.major, account.dtl, account.set, lastName, firstName, occupation ";
		}
		else if (bean.getSortOrder().equals(PrintTimecardBean.ORDER_DEPT)) {
			order = " deptName, account.major, account.dtl, account.set, lastName, firstName, occupation, endDate desc ";
		}
		else { // ORDER_NAME
			order = " lastName, firstName, account.major, account.dtl, account.set, occupation, endDate desc ";
		}

		String userAccount = wtc.getUserAccount();
		TimecardSelectionType select = bean.getRangeSelectionType();
		switch(select) {
			case CURRENT:
				if (allDates) {
					cards = weeklyTimecardDAO.findByUserAccountOccupation(production, project, userAccount, wtc.getOccupation());
				}
				else {
					cards = new ArrayList<>();
					cards.add(wtc);
				}
				break;
			case PERSON:
				if (allDates) {
					cards = weeklyTimecardDAO.findByWeekEndDateAccountDept(production, project, null, TimecardRange.ANY,
							userAccount, null, null, order);
				}
				else {
					cards = weeklyTimecardDAO.findByWeekEndDateAccountDept(production, project, weDate, TimecardRange.EXACT,
							userAccount, null, null, order);
				}
				break;
			case DEPT:
				cards = weeklyTimecardDAO.findByWeekEndDateAccountDept(production, project, weDate, TimecardRange.EXACT,
						null, wtc.getDeptName(), null, order);
				break;
			case BATCH:
				cards = weeklyTimecardDAO.findByUserAccountBatchDepartment(production, project, null, wtc.getWeeklyBatch(),
						null, null, order);
				break;
			case UNBATCHED:
				cards = weeklyTimecardDAO.findByWeekEndDateAccountBatchDept(production, project, weDate, TimecardRange.EXACT,
						null, null, null, null, order);
				break;
			case ALL:
				cards = weeklyTimecardDAO.findByWeekEndDateAccountDept(production, project, weDate, TimecardRange.EXACT,
						null, null, null, order);
				break;
			case SELECT:
				// Not yet available in Export UI - shouldn't get here.
				EventUtils.logError("Unsupported Export selection option.");
				break;
		}

		return cards;
	}

	/**
	 * Export (flatten) a collection of WeeklyTimecard`s into a single file.
	 *
	 * @param timecards A Collection of WeeklyTimecard objects to be flattened
	 *            into the output file.
	 * @param exportType The type of export file to create; values match static
	 *            constants in ExportTimecardBean.
	 * @param fringe The percentage of fringe to apply. May be null or zero if
	 *            no fringe should be calculated. Fringes are not calculated for
	 *            all export types.
	 * @param poNumber The PO # (Purchase Order number) to insert into the export file.
	 */
	public static String export(Collection<WeeklyTimecard> timecards, ExportType exportType, BigDecimal fringe, String poNumber) {
		String fileLocation = null;
		WeeklyTimecardDAO weeklyTimecardDAO = WeeklyTimecardDAO.getInstance();
		try {
			Production prod = SessionUtils.getNonSystemProduction();
			PayrollPreference payrollPref;
			if (prod.getType().hasPayrollByProject()) {
				payrollPref = SessionUtils.getCurrentProject().getPayrollPref();
			}
			else {
				payrollPref = prod.getPayrollPref();
			}
			boolean payInfo = payrollPref.getIncludeBreakdown();
			DateFormat df = new SimpleDateFormat("MM-dd_HHmmss");
			String timestamp = df.format(new Date());
			String reportFileName = "timecards";
			reportFileName = reportFileName + "_" + prod.getProdId() + "_" + timestamp;
			if (exportType.getUsesComma()) {
				reportFileName += Constants.COMMA_SUFFIX;
			}
			else if (exportType == ExportType.SHOWBIZ_BUDGET) {
				reportFileName += ".ubx";
			}
			else {
				reportFileName += Constants.TIMECARD_SUFFIX;
			}
			log.debug(reportFileName);
			fileLocation = Constants.REPORT_FOLDER + "/" + reportFileName;
			String reportPath = SessionUtils.getRealReportPath();
			reportFileName = reportPath + reportFileName;
			OutputStream outputStream = new FileOutputStream(new File(reportFileName));
			try {
				DelimitedExporter ex = null;
				if (exportType != ExportType.JSON) {
					if (exportType == ExportType.FULL_TAB) {
						ex = new FlatExporter(outputStream, "MM/dd/yyyy");
					}
					else {
						ex = new FlatExporter(outputStream);
					}
					if (exportType.getUsesComma()) {
						ex.setDelimiter((byte)',');
					}
				}
				switch(exportType) {
					case JSON:
						ObjectMapper mapper = new ObjectMapper();
//						JsonSchema sc = mapper.generateJsonSchema(WeeklyBatch.class);
//						JsonSchema sc = mapper.generateJsonSchema(WeeklyTimecard.class);
//						String s = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(sc);
//						log.debug(s);
						Collection<WeeklyTimecard> fresh = new ArrayList<>();
						for (WeeklyTimecard wtc : timecards) {
							wtc = weeklyTimecardDAO.refresh(wtc);
							fresh.add(wtc);
						}
						mapper.writeValue(outputStream, fresh);
						break;
					case CREW_CARDS:
						for (WeeklyTimecard wtc : timecards) {
							wtc = weeklyTimecardDAO.refresh(wtc);
							wtc.exportCrewcards(ex, payInfo);
							ex.next(); // advance the Exporter to a new record
						}
						break;
					case FULL_TAB:
						for (WeeklyTimecard wtc : timecards) {
							wtc = weeklyTimecardDAO.refresh(wtc);
							if (wtc.getTimeSent() == null) {
								wtc.setBatchStatus(WeeklyTimecard.BATCH_STATUS_NEW);
							}
							else if (wtc.getUpdated().after(wtc.getTimeSent())) {
								wtc.setBatchStatus(WeeklyTimecard.BATCH_STATUS_UPDATED);
							}
							wtc.exportTabbed(ex, payInfo);
							ex.next(); // advance the Exporter to a new record
						}
						break;
					case ABS:
						for (WeeklyTimecard wtc : timecards) {
							wtc = weeklyTimecardDAO.refresh(wtc);
							exportABS(ex, wtc, payInfo);
						}
						break;
					case HOT_BUDGET:
						for (WeeklyTimecard wtc : timecards) {
							wtc = weeklyTimecardDAO.refresh(wtc);
							exportHotBudget(ex, wtc);
						}
						break;
					case SHOWBIZ_BUDGET:
						// Output "header" records
						ex.append("IMEX"); 	// file format descriptor
						ex.append(4);		// version
						ex.next();
						// SUBGRP record dropped - optional.
//						ex.append("SUBGRP");	// record descriptor
//						ex.append("Payroll"); 	// sub-group name
//						ex.append("");			// description
//						ex.append(1);			// "include"
//						ex.next();
						for (WeeklyTimecard wtc : timecards) {
							wtc = weeklyTimecardDAO.refresh(wtc);
							exportShowbizBudget(ex, wtc, fringe, poNumber);
						}
						break;
					case PAYROLL:
						boolean first = true;
						for (WeeklyTimecard wtc : timecards) {
							wtc = weeklyTimecardDAO.refresh(wtc);
							exportGrossPayroll(ex, wtc, first, payInfo);
							first = false; // only true on first call
						}
						break;
					case NONE:
						// shouldn't happen!
						break;
				}
			}
			catch (Exception e) {
				EventUtils.logError(e);
				fileLocation = null;
			}
			finally {
				outputStream.close();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			fileLocation = null;
		}
		return fileLocation;
	}

	/**
	 * @param ex
	 * @param wtc
	 * @param payInfo True iff calculated pay data, e.g., pay breakdown numbers,
	 *            should be included in the transferred data.
	 */
	public static void exportABS(Exporter ex, WeeklyTimecard wtc, boolean payInfo) {
		for (PayBreakdown pb : wtc.getPayLines()) {
			PayCategory pc = PayCategory.CUSTOM;
			try {
				pc = PayCategory.valueOf(pb.getCategoryEnum());
			}
			catch (Exception e) {
			}
			ex.append(wtc.getSocialSecurity());
			if (pc == PayCategory.CUSTOM) {
				ex.append(pb.getCategory());
			}
			else {
				ex.append(pc.name());
			}
			if (pc == PayCategory.FLAT_RATE) {
				if (pb.getQuantity() != null && pb.getMultiplier() != null) {
					ex.append(pb.getQuantity().multiply(pb.getMultiplier()));
				}
				else {
					ex.append(BigDecimal.ZERO);
				}
				ex.append(pb.getRate());
			}
			else {
				ex.append(pb.getQuantity()); // hours or quantity
				if (pb.getMultiplier() != null && pb.getRate() != null) {
					ex.append(pb.getMultiplier().multiply(pb.getRate()));
				}
				else {
					ex.append(BigDecimal.ZERO);
				}
			}
			ex.next(); // advance the Exporter to a new record
		}
	}

	/**
	 * Output the records for a Hot Budget export of a single timecard. The
	 * timecard must have been "grossed up" already, as we use the PayJob and
	 * PayBreakdown tables to generate the output.
	 *
	 * @param ex The Exporter to use.
	 * @param wtc The timecard being exported.
	 */
	private static void exportHotBudget(Exporter ex, WeeklyTimecard wtc) {
		BigDecimal dailyRate;
		BigDecimal hours;
		BigDecimal guarHours;
		String name = "\"" + wtc.getLastName() + ", " + wtc.getFirstName() + "\"";

		// First output the labor, using the PayJob tables as input
		for (PayJob pj : wtc.getPayJobs()) {
			pj.calculateTotals();
			ex.append(wtc.getAccountMajor()); // TODO "Line" number: currently prep/wrap code
			ex.append(name);
			ex.append(""); // unused = PW?
			ex.append(""); // unused = Fringe?
			if (wtc.getAllowWorked()) {
				guarHours = Constants.HOURS_IN_A_DAY;
			}
			else {
				guarHours = wtc.getGuarHours();
				if (guarHours != null) {
					if (guarHours.compareTo(Constants.HOURS_IN_A_DAY) >= 0) {
						guarHours = guarHours.divide(Constants.DECIMAL_FIVE);
					}
				}
				else {
					guarHours = Constants.WORKED_HOURS_NON_UNION;
				}
			}
			ex.append(guarHours);
			ex.append(pj.getDaysWorked()); // Number of days worked
			dailyRate = calculateDailyRate(pj, wtc, guarHours);
			ex.append(dailyRate);	// Daily Rate (for guaranteed hours)
			if (pj.getTotal15() != null) {
				if (wtc.getAllowWorked()) {
					hours = pj.getTotal15(); // shouldn't really be any if exempt!
				}
				else {
					hours = guarHours.subtract(Constants.WORKED_HOURS_NON_UNION);
					hours = hours.multiply(new BigDecimal(pj.getDaysWorked()));
					hours = hours.max(BigDecimal.ZERO);
					hours = pj.getTotal15().subtract(hours);
				}
			}
			else {
				hours = BigDecimal.ZERO;
			}
			if (pj.getTotalCust1() != null && pj.getCustomMult1().compareTo(Constants.DECIMAL_TWO) < 0) {
				hours = hours.add(pj.getTotalCust1());
			}
			ex.append(hours); 		// 1.5x OT beyond guaranteed hours

			if (pj.getCustomMult1().compareTo(Constants.DECIMAL_TWO) >= 0) {
				ex.append(pj.getTotalCust1()); // 2.0x OT
				ex.append(pj.getTotalCust2()); // 3.0x OT
			}
			else if (pj.getCustomMult2().compareTo(Constants.DECIMAL_TWO) >= 0) {
				ex.append(pj.getTotalCust2()); // 2.0x OT
				ex.append(pj.getTotalCust3()); // 3.0x OT
			}
			else {
				ex.append(0); // 2.0x OT
				ex.append(0); // 3.0x OT
			}

			ex.append(0); // misc taxable
			ex.append(0); // misc non-taxable
			ex.append(0); // total straight time - not used by Hot Budget
			ex.append(0); // total OT - not used by Hot Budget
			ex.next(); // advance the Exporter to a new record
		}

		// Then output non-labor ("miscellaneous") from the Pay Breakdown table.
		for (PayBreakdown pb : wtc.getPayLines()) {
			PayCategory pc = null;
			try {
				pc = PayCategory.valueOf(pb.getCategoryEnum());
			}
			catch (Exception e) {
			}
			if (pc != null && pc.getIsExpense()) {
				ex.append(pb.getAccountMajor());
				ex.append(name);
				ex.append(""); // unused = PW?
				ex.append(""); // unused = Fringe?
				ex.append(0); // OT base
				ex.append(0); // days worked
				ex.append(0); // day rate
				ex.append(0); // 1.5x hours
				ex.append(0); // 2.0x hours
				ex.append(0); // 3.0x hours
				if (pc.getIsTaxable()) {
					ex.append(pb.getTotal()); // Misc taxable
					ex.append(0); // Misc non-taxable
				}
				else {
					ex.append(0); // Misc taxable
					ex.append(pb.getTotal()); // Misc non-taxable
				}
				ex.append(0); // total straight time
				ex.append(0); // total overtime
				ex.next(); // advance the Exporter to a new record
			}
		}
	}

	/**
	 * Output the records for a Showbiz Budgeting export of a single timecard.
	 * The timecard must have been "grossed up" already, as we use the PayJob
	 * and PayBreakdown tables to generate the output.
	 *
	 * @param ex The Exporter to use.
	 * @param wtc The timecard being exported.
	 * @param fringe The percentage of each line amount to put into the Fringe
	 *            field.
	 * @param poNumber The PO # (Purchase Order number) to insert into the export file.
	 */
	private static void exportShowbizBudget(DelimitedExporter ex, WeeklyTimecard wtc, BigDecimal fringe, String poNumber) {

		String acctCode;
		for (PayJob pj : wtc.getPayJobs()) {
			acctCode = pj.calculateTotals(Boolean.FALSE); // calculate prep/wrap days total
			if (pj.getDaysWorked() > 0 && ! StringUtils.isEmpty(acctCode)) {
				exportShowbizJob(ex, wtc, pj, fringe, acctCode, poNumber);
			}
			acctCode = pj.calculateTotals(Boolean.TRUE); // calculate shooting days totals
			if (pj.getDaysWorked() > 0 && ! StringUtils.isEmpty(acctCode)) {
				exportShowbizJob(ex, wtc, pj, fringe, acctCode, poNumber);
			}
			pj.calculateTotals(); // leave PayJob in a "normal" state, with complete totals
		}

		for (PayBreakdown pb : wtc.getPayLines()) {
			PayCategory pc = null;
			try {
				pc = PayCategory.valueOf(pb.getCategoryEnum());
			}
			catch (Exception e) {
			}
			if (pc != null &&
					(pc.getMultiplier() == null) && // eliminate usual labor entries
					(pc != PayCategory.FLAT_RATE) && // eliminate Flats/exempt
					(! pb.getCategory().contains(" Hours")) &&  // eliminate labor with Custom multipliers
					(! pb.getCategory().contains("Extended Day")) &&  // eliminate labor for Extended Day (DGA)
					(! pc.name().contains("NIGHT_PREM"))) { // eliminate Night Premium
				// Assume the rest is "non labor" and goes into "Miscellaneous"
				acctCode = pb.getAccountMajor();
				if (! StringUtils.isEmpty(acctCode)) {
					exportShowbizExpense(ex, wtc, pb, pc, fringe, acctCode, poNumber);
				}
			}
		}
	}

	/**
	 * Output a single export record for Showbiz Budgeting, using data from the
	 * given PayJob instance.
	 *
	 * @param ex The Exporter to use.
	 * @param wtc The timecard being exported.
	 * @param pj The PayJob being exported.
	 * @param fringe The percentage of each line amount to put into the Fringe field.
	 * @param acctCode The account code ("line number") for this export record.
	 * @param poNumber The PO # (Purchase Order number) to insert into the export file.
	 */
	private static void exportShowbizJob(DelimitedExporter ex, WeeklyTimecard wtc,
			PayJob pj, BigDecimal fringe, String acctCode, String poNumber) {
		BigDecimal dailyRate, hourlyRate, otSum;
		BigDecimal straightHours, guarHours, guarOtHours, baseHours;
//		BigDecimal otHours, ot15Hours, ot2Hours, ot3Hours;

		String name = wtc.getLastName() + ", " + wtc.getFirstName();
		ex.append("EXPNS");			// (A) record descriptor [comments indicate equivalent excel column letter]
		ex.append(acctCode); 		// (B) Account code or "line number"
		ex.append(wtc.getEndDate());// (C) week-ending date
		ex.append(poNumber); 		// (D) PO Number (may be blank)
		ex.append(name);			// (E) "Vendor" = employee name
		ex.append(wtc.getOccupation());// (F) "Description" = Occupation
		ex.append(1);  				// (G) Pay Type 1= timecard
		ex.append(pj.getJobAccountNumber()); // (H) Reference

		// guarOtHours = # of hrs/day included in guarantee for which employee is paid OT.
		guarOtHours = BigDecimal.ZERO; // assume zero
		if (wtc.getAllowWorked()) {
			guarHours = TimecardCheck.calculateExemptHours(wtc);
		}
		else {
			guarHours = wtc.getGuarHours();
			if (guarHours != null) {
				if (guarHours.compareTo(Constants.HOURS_IN_A_DAY) >= 0) {
					guarHours = guarHours.divide(Constants.DECIMAL_FIVE);
				}
				guarOtHours = guarHours.subtract(Constants.WORKED_HOURS_NON_UNION);
			}
			else {
				guarHours = Constants.WORKED_HOURS_NON_UNION;
			}
		}

		straightHours = pj.getTotal10();

		BigDecimal cmult1 = pj.getCustomMult1();
		if (cmult1.signum() < 0) { // "Days" count in column, not hours
			baseHours = Constants.DAILY_STRAIGHT_HOURS.add(
					guarHours.subtract(Constants.DAILY_STRAIGHT_HOURS)
					.multiply(Constants.DECIMAL_ONE_FIVE));
			straightHours = straightHours.add(baseHours.multiply(pj.getTotalCust1()).setScale(2, RoundingMode.HALF_UP));
			cmult1 = BigDecimal.ZERO;
		}

		ex.append(straightHours);	// (I) Number of hours worked (or days)
		ex.append(0);				// (J) Hours (not used)

		dailyRate = calculateDailyRate(pj, wtc, guarHours);

		hourlyRate = pj.getRate();
		if (hourlyRate == null) {
			hourlyRate = wtc.getHourlyRate();
		}

		if (hourlyRate == null) {
			if (guarOtHours.signum() == 0) {
				baseHours = guarHours;
				hourlyRate = dailyRate.divide(guarHours, 4, RoundingMode.HALF_UP);
			}
			else {
				baseHours = Constants.WORKED_HOURS_NON_UNION.add(guarHours.subtract(
						Constants.WORKED_HOURS_NON_UNION).multiply(Constants.DECIMAL_ONE_FIVE));
				hourlyRate = dailyRate.divide(baseHours, 4, RoundingMode.HALF_UP);
			}
		}
		ex.append(hourlyRate,4);	// (K) Rate (Hourly; or Daily for guaranteed hours)

		ex.append(0); 		// (L) all OT hours beyond guarantee -- was otHours; not used, SBB will use our total OT

		otSum = NumberUtils.safeAdd(
				NumberUtils.safeMultiply(NT_PREM_1_10, pj.getTotal10Np1()),
				NumberUtils.safeMultiply(NT_PREM_1_20, pj.getTotal10Np2()),
				NumberUtils.safeMultiply(Constants.DECIMAL_ONE_FIVE, pj.getTotal15()),
				NumberUtils.safeMultiply(cmult1, pj.getTotalCust1()),
				NumberUtils.safeMultiply(pj.getCustomMult2(), pj.getTotalCust2()),
				NumberUtils.safeMultiply(pj.getCustomMult3(), pj.getTotalCust3()),
				NumberUtils.safeMultiply(pj.getCustomMult4(), pj.getTotalCust4()),
				NumberUtils.safeMultiply(pj.getCustomMult5(), pj.getTotalCust5()),
				NumberUtils.safeMultiply(pj.getCustomMult6(), pj.getTotalCust6()),
				NumberUtils.safeMultiply(NT_PREM_1_5_10, pj.getTotal15Np1()),
				NumberUtils.safeMultiply(NT_PREM_1_5_20, pj.getTotal15Np2())
				);
		if (otSum == null) {
			otSum = BigDecimal.ZERO;
		}
		else {
			otSum = otSum.multiply(hourlyRate);
			otSum = otSum.max(BigDecimal.ZERO);
		}
		otSum = otSum.setScale(2, RoundingMode.HALF_UP);
		ex.append(otSum); 	// (M) OT $ amount (all pay except straight time)

		ex.append(fringe);	// (N) PW (fringe) percent
		ex.append(0); 		// (O) PW (fringe) amount
		ex.append(0);		// (P) Total $ (including non-labor items) -- not required (SB computes)
		ex.append(0); 		// (Q) Breakout 1
		ex.append(0); 		// (R) Breakout 2
		ex.append(0); 		// (S) Breakout 3
		ex.append(0); 		// (T) Multiplier 2(?)

		ex.append(0); 		// (U) OT Base hours (was 'baseHours') - use Zero per Richard so SBB won't recalculate OT

		ex.append(0); 		// (V) 1.5x hours -- was ot15Hours; not used, SBB will use our total OT
		ex.append(0); 		// (W) 2x hours -- was ot2Hours; not used, SBB will use our total OT
		ex.append(0); 		// (X) 3x hours -- was ot3Hours; not used, SBB will use our total OT

		ex.append(""); 		// (Y) Units 1
		ex.append(""); 		// (Z) Units 2

		ex.append(otSum); 	// (AA) OT $ amount -- everything above guarantee

		ex.append(0);		// (AB) Misc Non-taxable
		ex.append(0); 		// (AC) Misc Taxable
		ex.append(""); 		// (AD) Sub Groups

		ex.next(); // advance the Exporter to a new record
	}

//	/**
//	 * @param otHours
//	 * @param custHours
//	 * @param customMult
//	 * @param exportMult
//	 * @return
//	 */
//	private static BigDecimal addAdjustedHours(BigDecimal otHours, BigDecimal custHours,
//			BigDecimal customMult, BigDecimal exportMult) {
//		BigDecimal ratio = customMult.divide(exportMult, 2, RoundingMode.HALF_UP);
//		custHours = custHours.multiply(ratio);
//		otHours = NumberUtils.safeAdd(otHours, custHours);
//		return otHours;
//	}

	/**
	 * Generate the export record for a single line item from our Pay Breakdown table.
	 * @param ex The Exporter to use.
	 * @param wtc The timecard being exported.
	 * @param pb The PayBreakdown item being exported.
	 * @param pc The PayCategory enum corresponding to the PayBreakdown line item.
	 * @param fringe The percentage of each line amount to put into the Fringe field.
	 * @param acctCode The account code ("line number") for this export record.
	 * @param poNumber The PO Number to insert into each line item.
	 */
	private static void exportShowbizExpense(DelimitedExporter ex, WeeklyTimecard wtc,
			PayBreakdown pb, PayCategory pc, BigDecimal fringe, String acctCode, String poNumber) {

		String name = wtc.getLastName() + ", " + wtc.getFirstName();
		ex.append("EXPNS");			// (A) record descriptor [comments indicate equivalent excel column letter]
		ex.append(acctCode); 		// (B) Account code or "line number"
		ex.append(wtc.getEndDate());// (C) week-ending date
		ex.append(poNumber); 		// (D) PO Number
		ex.append(name);			// (E) "Vendor" = employee name
		ex.append(pb.getCategory());// (F) "Description" = line item category
//		ex.append(wtc.getViewSSN());// (F) "Description" = SSN-4
		ex.append(1);  				// (G) Pay Type 1= timecard
		ex.append(wtc.getJobNumber()); // (H) Reference
		ex.append(0);				// (I) Number of hours worked (or days)
		ex.append(0);				// (J) Hours (not used)
		ex.append(0);		// (K) Rate (Hourly; or Daily for guaranteed hours)
		ex.append(0); 		// (L) all OT hours beyond guarantee
		ex.append(0); 		// (M) OT $ amount (all pay except straight time)
		ex.append(fringe);	// (N) PW (fringe) percent
		ex.append(0); 		// (O) PW (fringe) amount
		ex.append(0);		// (P) Total $ (including non-labor items) - not required (SB computes)
		ex.append(0); 		// (Q) Breakout 1
		ex.append(0); 		// (R) Breakout 2
		ex.append(0); 		// (S) Breakout 3
		ex.append(0); 		// (T) Multiplier 2(?)
		ex.append(0); 		// (U) OT Base hours (was 'baseHours')
		ex.append(0); 		// (V) 1.5x hours
		ex.append(0); 		// (W) 2x hours
		ex.append(0); 		// (X) 3x hours
		ex.append(""); 		// (Y) Units 1
		ex.append(""); 		// (Z) Units 2
		ex.append(0); 		// (AA) OT $ amount -- everything above guarantee

		// Then compute non-labor ("miscellaneous") from the Pay Breakdown table.
		BigDecimal amtTax = BigDecimal.ZERO;
		BigDecimal amtNonTax = BigDecimal.ZERO;
		if (pc.getIsTaxable()) {
			amtTax = pb.getTotal();
		}
		else {
			amtNonTax = pb.getTotal();
		}
		ex.append(amtNonTax);	// (AB) Misc Non-taxable
		ex.append(amtTax); 		// (AC) Misc Taxable
		ex.append(""); 			// (AD) Sub Groups

		ex.next(); // advance the Exporter to a new record
	}

	/**
	 * @param ex
	 * @param wtc
	 * @param payInfo True iff calculated pay data, e.g., pay breakdown numbers,
	 *            should be included in the transferred data.
	 */
	/* package */ static void exportGrossPayroll(DelimitedExporter ex, WeeklyTimecard wtc, boolean first, boolean payInfo) {
		if (first) {
			ex.append("W/E date");
			ex.append("Last");
			ex.append("First");
			ex.append("SSN-4");
			ex.append("Department");
			ex.append("Occupation");
			ex.append("Occ-Code");
			ex.append("Union");
			ex.append("Loc");
			ex.append("Prd/Epi");
			ex.append("Detail");
			ex.append("Sub");
			ex.append("Set");
			ex.append("Free");
			ex.append("Free2");
			ex.append("Gross");
			ex.next();
		}
		ex.append(wtc.getEndDate());
//		ex.append(wtc.getAdjusted());
		ex.append(wtc.getLastName());
		ex.append(wtc.getFirstName());
		ex.append(wtc.getViewSSN());
//		ex.append(wtc.getLoanOutCorp());
//		ex.append(wtc.getProdName());
//		ex.append(wtc.getProdCo());
		ex.append(wtc.getDeptName());
		ex.append(wtc.getOccupation());
		ex.append(wtc.getOccCode());
		ex.append(wtc.getUnionNumber());
//		ex.append(wtc.getStateWorked());
//		ex.append(wtc.getCityWorked());
//		ex.append(wtc.getFedCorpId());
//		ex.append(wtc.getStateCorpId());
		ex.append(wtc.getAccountLoc());
		ex.append(wtc.getAccountMajor());
		ex.append(wtc.getAccountDtl());
		ex.append(wtc.getAccountSub());
		ex.append(wtc.getAccountSet());
		ex.append(wtc.getAccountFree());
		ex.append(wtc.getAccountFree2());
//		ex.append(StringUtils.saveHtml(wtc.getComments()));
//		ex.append(StringUtils.saveHtml(wtc.getPrivateComments()));
		ex.append(wtc.getGrandTotal());

		ex.next(); // advance the Exporter to a new record
	}

	/**
	 * Find or calculate a daily rate for the given PayJob and timecard. The
	 * PayJob is checked first for a weekly or hourly rate; if either exists,
	 * the daily rate is calculated from that. Otherwise the timecard rates, if
	 * they exist, will be used. If none of those are set, then the StartForm is
	 * checked for rates. If no rates are found, zero is returned.
	 *
	 * @param pj The PayJob to use for rates.
	 * @param wtc The timecard to use for rates.
	 * @param guarHours
	 * @return The appropriate daily rate, or zero if no rates are found.
	 */
	private static BigDecimal calculateDailyRate(PayJob pj, WeeklyTimecard wtc, BigDecimal guarHours) {
		BigDecimal dailyRate = BigDecimal.ZERO;
		if (pj.getWeeklyRate() != null) {
			dailyRate = pj.getWeeklyRate().divide(Constants.DECIMAL_FIVE, 2, RoundingMode.HALF_UP);
		}
		else if (pj.getRate() != null) {
			dailyRate = TimecardCalc.calculateDailyRate(pj.getRate(), guarHours);
		}
		else if (wtc.getWeeklyRate() != null) {
			dailyRate = wtc.getWeeklyRate().divide(Constants.DECIMAL_FIVE, 2, RoundingMode.HALF_UP);
		}
		else if (wtc.getDailyRate() != null) {
			dailyRate = wtc.getDailyRate();
		}
		else if (wtc.getRate() != null) {
			dailyRate = TimecardCalc.calculateDailyRate(wtc.getRate(), guarHours);
		}
		else {
			if (wtc.getStartForm().isStudioRate()) {
				dailyRate = wtc.getStartForm().getProd().getDaily().getStudio();
				if (dailyRate == null) {
					dailyRate = wtc.getStartForm().getProd().getHourly().getStudio();
					dailyRate = TimecardCalc.calculateDailyRate(dailyRate, guarHours);
				}
			}
			else {
				dailyRate = wtc.getStartForm().getProd().getDaily().getLoc();
				if (dailyRate == null) {
					dailyRate = wtc.getStartForm().getProd().getHourly().getLoc();
					dailyRate = TimecardCalc.calculateDailyRate(dailyRate, guarHours);
				}
			}
			if (dailyRate == null) {
				dailyRate = BigDecimal.ZERO;
			}
		}
		return dailyRate;
	}

}
