package com.lightspeedeps.util.report;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.Scene;
import com.lightspeedeps.model.TaxWageAllocationForm;
import com.lightspeedeps.model.WeeklyBatch;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.common.CalendarUtils;

import net.sf.jasperreports.engine.JRDefaultScriptlet;
import net.sf.jasperreports.engine.JRScriptletException;

/**
 * This class implements methods that are called from our Jasper reports
 * to do special-case calculations or formatting of data.
 */
public class ReportScriptlet extends JRDefaultScriptlet {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(ReportScriptlet.class);

	/**
	 * Calculate the number of worked hours in a day. Used by the Discrepancy
	 * report. The meal times are from the DPR header, which are in a Timestamp
	 * object, while the call and wrap times are from the specific crew entry
	 * being calculated and are in decimal time. Meals times that are outside the
	 * call/wrap times are ignored.
	 *
	 * @param call Call time.
	 * @param wrap Wrap time.
	 * @param m1Out Meal 1 start; will be rounded up if necessary.
	 * @param m1In Meal 1 end; will be rounded down if necessary.
	 * @param m2Out Meal 2 start; will be rounded up if necessary.
	 * @param m2In Meal 2 end; will be rounded down if necessary.
	 * @return The total number of work hours; returns null if either the
	 * call or wrap time is null.
	 */
	public BigDecimal calculatePrHours(BigDecimal call, BigDecimal wrap,
			Timestamp m1Out, Timestamp m1In, Timestamp m2Out, Timestamp m2In) {
		BigDecimal total = null;

		if (call != null && wrap != null) {
			total = wrap.subtract(call);
			total = adjustTotal(total, call, wrap, m1Out, m1In);
			total = adjustTotal(total, call, wrap, m2Out, m2In);
		}
		//log.debug(total + ": " + call + ", " + wrap  + ", " + m1Out  + ", " + m1In  + ", " + m2Out  + ", " + m2In );
		return total;
	}

	/**
	 * Adjust the total hours worked by subtracting out the time spent on a
	 * meal, if the meal times fall within the call/wrap period.
	 *
	 * @param total The currently computed total hours worked.
	 * @param call The employee's call time.
	 * @param wrap The employee's wrap time.
	 * @param mealOut The meal start time.
	 * @param mealIn The meal end time.
	 * @return The total hours which will either be the same as the hours passed
	 *         in the parameter, or that value minus the amount of time between
	 *         the mealIn and mealOut times.
	 */
	private BigDecimal adjustTotal(BigDecimal total, BigDecimal call, BigDecimal wrap,
			Timestamp mealOut, Timestamp mealIn) {
		BigDecimal mealStart, mealEnd;
		if (mealOut != null && mealIn != null) {
			// subtract first meal time from worked hours, if within call/wrap
			mealStart = CalendarUtils.convertTimeToDecimal(mealOut, 2);
			mealEnd = CalendarUtils.convertTimeToDecimal(mealIn, 2);
			if (call.subtract(mealStart).signum() <= 0 && // call is before (or equal to) meal start
					wrap.subtract(mealEnd).signum() >= 0 ) { // wrap is later than (or equal to) meal end
				total = total.subtract(mealEnd.subtract(mealStart));
			}
			else {
				// when day extends past midnight, meal times in DPR may need to be adjusted
				// by adding 24 to them, to compare properly to call & wrap times.
				if (wrap.compareTo(Constants.HOURS_IN_A_DAY) >= 0) {
					mealStart = mealStart.add(Constants.HOURS_IN_A_DAY);
					mealEnd = mealEnd.add(Constants.HOURS_IN_A_DAY);
					if (call.subtract(mealStart).signum() <= 0 && // call is before (or equal to) meal start
							wrap.subtract(mealEnd).signum() >= 0 ) { // wrap is later than (or equal to) meal end
						total = total.subtract(mealEnd.subtract(mealStart));
					}
				}
			}
		}
		return total;
	}

	/**
	 * Convert a Timestamp value to the equivalent decimal time of day, to two
	 * decimal places. Used by Jasper reports -- currently only the DPR
	 * Discrepancy report.
	 *
	 * @param time The time to be converted; only the time of day is used, not
	 *            the calendar day. The time is rounded up to the nearest tenth
	 *            of an hour.
	 * @return The time of day expressed as a value from 0 to 23.99, with a
	 *         maximum of two decimal places, or null if 'time' is null. Note
	 *         that prior to rev 2.9.5107, this returned time rounded to a
	 *         single decimal place.
	 */
	public BigDecimal convertTimeToDecimal(Timestamp time) {
		BigDecimal dTime = null;
		if (time != null) {
			dTime = CalendarUtils.convertTimeToDecimal(time, 2);
		}
		return dTime;
	}

	/**
	 * Compare a PR's meal time Timestamp value to the corresponding timecard
	 * decimal time of day. Used by Jasper reports.
	 *
	 * @param prMealTime The time to be converted; only the time of day is used, not
	 *            the calendar day. The time is rounded up to the nearest hundredth
	 *            of an hour (2 decimal places).
	 * @return True if the two times are 'equal', false if not. 'Equal' includes
	 *         being 24 hours different, as the timecard times may exceed 24
	 *         when a work day extends past midnight.
	 */
	public boolean compareMealTime(BigDecimal tcMealTime, Timestamp prMealTime) {
		boolean result = true;
		BigDecimal prTime = null;
		if (prMealTime != null && tcMealTime != null) {
			prTime = CalendarUtils.convertTimeToDecimal(prMealTime, 2);
			result = tcMealTime.compareTo(prTime) == 0;
			if (! result) {
				prTime = prTime.add(Constants.HOURS_IN_A_DAY);
				result = tcMealTime.compareTo(prTime) == 0;
			}
		}
		return result;
	}

	/**
	 * Format a numeric page length, which is the number of eighth's of a page,
	 * for use in a Jasper report. Jasper already has a page length field from
	 * the database record.
	 *
	 * @param pgLen Integer page-length, identified in the Jasper report as a
	 *            Field.
	 * @return A String with the formatted length as either "m/8", "n", or
	 *         "n m/8". The fractional portion is only included if 'm' is
	 *         non-zero. The integer portion is included if it is more than 0,
	 *         or if 'length' is zero or negative, in which case the returned
	 *         String is just "0".
	 */
	public String formatPages(Integer pgLen) throws JRScriptletException {
//		Integer pgLen = (Integer)getFieldValue("pages");
		return Scene.formatPageLength(pgLen);
	}

	public String formatPhone(String phone) throws JRScriptletException {
		if (phone == null || phone.length() == 0 || phone.charAt(0) == '+' || phone.charAt(0) == '0') {
			// return it as-is, no reformatting
		}
		else if (phone.length() == 10) {
			phone = "(" + phone.substring(0,3) + ") " + phone.substring(3, 6) + "-" + phone.substring(6);
		}
		else if (phone.length() == 11 && phone.charAt(0) == '1') {
			phone = "1-" + phone.substring(1,4) + "-" + phone.substring(4, 7) + "-" + phone.substring(7);
		}

		return phone;
	}

	public String formatShortTwoTimes(DateFormat format, Date date1, Date date2) throws JRScriptletException {
		String result = format.format(date1);
		result = result.replaceAll("PM", "p");
		result = result.replaceAll("AM", "a");
		String result2 = format.format(date2);
		result2 = result2.replaceAll("PM", "p");
		result2 = result2.replaceAll("AM", "a");
		//System.out.println(result + " " + result2);
		return result + " " + result2;
	}

	public String formatShortTime(DateFormat format, Date date) throws JRScriptletException {
		String result = format.format(date);
		result = result.replaceAll("PM", "p");
		result = result.replaceAll("AM", "a");
		return result;
	}

	/**
	 * Format the UUID stored in the TimecardEvent objects. See
	 * TimecardEvent.getUuid() for similar code. Jasper already has the
	 * uuid_bytes field from the database record. We just need to turn it into a
	 * UUID and use that class' toString() method for formatting it.
	 */
	public String formatUuid() throws JRScriptletException {
		byte[] uuidBytes = (byte[])getFieldValue("Uuid_Bytes");
		ByteBuffer bb = ByteBuffer.wrap(uuidBytes);
		long lMsb = bb.getLong();
		long lLsb = bb.getLong();
		UUID uuid = new UUID(lMsb, lLsb);
		return uuid.toString();
	}

	/**
	 * Get a printable complete (9-digit) Federal Tax Id number
	 * for the Indemnification report -- from a FormIndem instance.
	 *
	 * We can't use FormIndemDAO.getInstance() because we may not have a context
	 * at this point as in printing from a scheduled task.
	 *
	 * @param id The FormIndem.id value.
	 * @return The string to print in the report's Federal tax id field.
	 * @throws JRScriptletException
	 */
	public String indemFormFedId(Integer id) throws JRScriptletException {
		FormIndemDAO formIndemDAO = (FormIndemDAO)getParameterValue("formIndemDAO");
		String fedid = "?";
		if (id != null && formIndemDAO != null) {
			try {
				fedid = formIndemDAO.findFedidNumberById(id);
				if (fedid != null && fedid.length() == 9) {
					fedid = fedid.substring(0, 2) + '-' + fedid.substring(2);
				}
			}
			catch (Exception e) {
				fedid = e.getMessage();
			}
		}
		return fedid;
	}

	/**
	 * Get a printable complete (9-digit) SSN.
	 *
	 * We can't use StartFormDAO.getInstance() because we may not have a context
	 * at this point as in printing from a scheduled task.
	 *
	 * @param id The StartFord.id value.
	 * @return The string to print in the report's SSN field.
	 * @throws JRScriptletException
	 */
	public String startFormSSN(Integer id) throws JRScriptletException {
		StartFormDAO startFormDAO = (StartFormDAO)getParameterValue("sfDAO");
		String ssn = "?";
		if (id != null && startFormDAO != null) {
			try {
				ssn = startFormDAO.findSsnById(id);
				if (ssn != null && ssn.length() == 9) {
					ssn = ssn.substring(0, 3) + '-' + ssn.substring(3, 5) + '-' + ssn.substring(5);
				}
			}
			catch (Exception e) {
				ssn = e.getMessage();
			}
		}
		return ssn;
	}

	/**
	 * Get a printable complete (9-digit) Federal Tax Id number
	 * for the Timecard or Start Form report, from the StartForm table.
	 *
	 * We can't use StartFormDAO.getInstance() because we may not have a context
	 * at this point as in printing from a scheduled task.
	 *
	 * @param id The StartForm.id value.
	 * @return The string to print in the report's Federal Tax id (FEIN) field.
	 * @throws JRScriptletException
	 */
	public String startFormFedId(Integer id) throws JRScriptletException {
		StartFormDAO startFormDAO = (StartFormDAO)getParameterValue("sfDAO");
		String fedTaxId = "?";
		if (id != null && startFormDAO != null) {
			try {
				fedTaxId = startFormDAO.findFedTaxIdById(id);
				if (fedTaxId == null) {
					fedTaxId = "";
				}
				else if (fedTaxId.length() == 9) {
					fedTaxId = fedTaxId.substring(0, 2) + '-' + fedTaxId.substring(2);
				}
			}
			catch (Exception e) {
				fedTaxId = e.getMessage();
			}
		}
		return fedTaxId;
	}

	/**
	 * Get a printable complete (9-digit) SSN for an I9 document.
	 *
	 * We can't use FormI9DAO.getInstance() because we may not have a context
	 * at this point as in printing from a scheduled task.
	 *
	 * @param id The FormI9.id value.
	 * @return The string to print in the report's SSN field.
	 * @throws JRScriptletException
	 */
	public String formI9SSN(Integer id) throws JRScriptletException {
		FormI9DAO formI9DAO = (FormI9DAO)getParameterValue("ssnDAO");
		String ssn = "?";
		if (id != null && formI9DAO != null) {
			try {
				ssn = formI9DAO.findSsnById(id);
				if (ssn != null && ssn.length() == 9) {
					ssn = ssn.substring(0, 3) + '-' + ssn.substring(3, 5) + '-' + ssn.substring(5);
				}
			}
			catch (Exception e) {
				ssn = e.getMessage();
			}
		}
		return ssn;
	}

	/**
	 * Get a printable complete (9-digit) SSN for an MTA document.
	 *
	 * We can't use FormMtaDAO.getInstance() because we may not have a context
	 * at this point as in printing from a scheduled task.
	 *
	 * @param id The FormMta.id value.
	 * @return The string to print in the report's SSN field.
	 * @throws JRScriptletException
	 */
	public String formMtaSSN(Integer id) throws JRScriptletException {
		FormMtaDAO formMtaDAO = (FormMtaDAO)getParameterValue("formMtaDAO");
		String ssn = "?";
		if (id != null && formMtaDAO != null) {
			try {
				//ssn = formMtaDAO.findSsnById(id);
				ssn = formMtaDAO.findByIdField(id, "socialSecurity");
				if (ssn != null && ssn.length() == 9) {
					ssn = ssn.substring(0, 3) + '-' + ssn.substring(3, 5) + '-' + ssn.substring(5);
				}
			}
			catch (Exception e) {
				ssn = e.getMessage();
			}
		}
		return ssn;
	}

	/**
	 * Get a printable complete (9-digit) Account Number for an MTA document.
	 *
	 * We can't use FormMtaDAO.getInstance() because we may not have a context
	 * at this point as in printing from a scheduled task.
	 *
	 * @param id The FormMta.id value.
	 * @return The string to print in the report's account number field.
	 * @throws JRScriptletException
	 */
	public String formMtaAcctNumber(Integer id) throws JRScriptletException {
		FormMtaDAO formMtaDAO = (FormMtaDAO)getParameterValue("formMtaDAO");
		String acctNumber = "?";
		if (id != null && formMtaDAO != null) {
			try {
				//acctNumber = formMtaDAO.findAcctNumberById(id);
				acctNumber = formMtaDAO.findByIdField(id, "accountNumber");
			}
			catch (Exception e) {
				acctNumber = e.getMessage();
			}
		}
		return acctNumber;
	}

	/**
	 * Get a printable complete (9-digit) Routing number for an MTA document.
	 *
	 * We can't use FormMtaDAO.getInstance() because we may not have a context
	 * at this point as in printing from a scheduled task.
	 *
	 * @param id The FormMta.id value.
	 * @return The string to print in the report's Routing number field.
	 * @throws JRScriptletException
	 *
	 */
	public String formMtaRoutingNumber(Integer id) throws JRScriptletException {
		FormMtaDAO formMtaDAO = (FormMtaDAO)getParameterValue("formMtaDAO");
		String routingNum = "?";
		if (id != null && formMtaDAO != null) {
			try {
				//routingNum = formMtaDAO.findRoutingNumberById(id);
				routingNum = formMtaDAO.findByIdField(id, "routingNumber");
				if (routingNum != null && routingNum.length() == 9) {
					routingNum = routingNum.substring(0, 4) + '-' + routingNum.substring(4);
				}
			}
			catch (Exception e) {
				routingNum = e.getMessage();
			}
		}
		return routingNum;
	}

	public String formI9SsnSingleDigit(Integer id, Integer position) {
		String ssnDigit = "";		
		String ssn = "";
		try {
			FormI9DAO formI9DAO = (FormI9DAO)getParameterValue("ssnDAO");
			if (id != null && formI9DAO != null) {
				ssn = formI9DAO.findSsnById(id);
				if(!StringUtils.isEmpty(ssn) && position < ssn.length()) {
					ssnDigit = ssn.substring(position, position + 1);
				}	
			}
		}
		catch (Exception e) {
			ssn = e.getMessage();
		}
			
		return ssnDigit;
	}
	
	/**
	 * Get a printable SSN with just the last 4 digits displayed and hash-marks
	 * for the leading 5 positions.
	 *
	 * We can't use StartFormDAO.getInstance() because we may not have a context
	 * at this point as in printing from a scheduled task.
	 *
	 * @param id The StartForm.id value.
	 * @return The string to print in the report's SSN field, such as
	 *         "###-##-1234".
	 * @throws JRScriptletException
	 */
	public String startFormSSN4(Integer id) throws JRScriptletException {
		StartFormDAO startFormDAO = (StartFormDAO)getParameterValue("sfDAO");
		String ssn = "?";
		if (id != null && startFormDAO != null) {
			try {
				ssn = startFormDAO.findSsnById(id);
				if (ssn != null && ssn.length() == 9) {
					ssn = "###-##-" + ssn.substring(5);
				}
			}
			catch (Exception e) {
				ssn = e.getMessage();
			}
		}
		return ssn;
	}

	/**
	 * Get a printable string of only the last 4 digits of the SSN, with a
	 * preceding hyphen.
	 *
	 * We can't use StartFormDAO.getInstance() because we may not have a context
	 * at this point as in printing from a scheduled task.
	 *
	 * @param id The StartFord.id value.
	 * @return A dash followed by the last 4 digits of the SSN, such as "-1234".
	 * @throws JRScriptletException
	 */
	public String startFormSSN4A(Integer id) throws JRScriptletException {
		StartFormDAO startFormDAO = (StartFormDAO)getParameterValue("sfDAO");
		String ssn = "?";
		if (id != null && startFormDAO != null) {
			try {
				ssn = startFormDAO.findSsnById(id);
				if (ssn != null && ssn.length() == 9) {
					ssn = "-" + ssn.substring(5);
				}
			}
			catch (Exception e) {
				ssn = e.getMessage();
			}
		}
		return ssn;
	}

	/**
	 * Get a printable SSN with just the last 4 digits displayed and hash-marks
	 * for the leading 5 positions.
	 *
	 * We can't use UserDAO.getInstance() because we may not have a context
	 * at this point as in printing from a scheduled task.
	 *
	 * @param id The User.id value.
	 * @return The string to print in the report's SSN field, such as
	 *         "###-##-1234".
	 * @throws JRScriptletException
	 */
	public String taxWageAllocationFormSSN4(Integer id) throws JRScriptletException {
		TaxWageAllocationFormDAO twafDAO = (TaxWageAllocationFormDAO)getParameterValue("twafDAO");
		String ssn = "?";
		if (id != null && twafDAO != null) {
			try {
				TaxWageAllocationForm form = twafDAO.findById(id);

				ssn = form.getSocialSecurity();
				if (ssn != null && ssn.length() == 9) {
					ssn = "###-##-" + ssn.substring(5);
				}
			}
			catch (Exception e) {
				ssn = e.getMessage();
			}
		}
		return ssn;
	}

	/**
	 * Get a printable complete (9-digit) SSN for a User object.
	 *
	 * We can't use UserDAO.getInstance() because we may not have a context
	 * at this point as in printing from a scheduled task.
	 *
	 * @param id The User.id value.
	 * @return The string to print in the report's SSN field.
	 * @throws JRScriptletException
	 */
	public String taxWageAllocationFormSSN(Integer id) throws JRScriptletException {
		TaxWageAllocationFormDAO twafDAO = (TaxWageAllocationFormDAO)getParameterValue("twafDAO");
		String ssn = "?";
		if (id != null && twafDAO != null) {
			try {
				TaxWageAllocationForm form = twafDAO.findById(id);

				ssn = form.getSocialSecurity();
				if (ssn != null && ssn.length() == 9) {
					ssn = ssn.substring(0, 3) + '-' + ssn.substring(3, 5) + '-' + ssn.substring(5);
				}
			}
			catch (Exception e) {
				ssn = e.getMessage();
			}
		}
		return ssn;
	}

	/**
	 * Return the name of the Weekly Batch associated with the id.
	 *
	 * @param id
	 * @return
	 * @throws JRScriptletException
	 */
	public String weeklyBatchName(Integer id) throws JRScriptletException {
		WeeklyBatchDAO weeklyBatchDAO = (WeeklyBatchDAO)getParameterValue("wbDAO");
		String weeklyBatchName = "";

		if (id != null && weeklyBatchDAO != null) {
			try {
				WeeklyBatch wb = weeklyBatchDAO.findById(id);

				if(wb != null) {
					weeklyBatchName = wb.getName();
				}
			}
			catch (Exception e) {
				weeklyBatchName = e.getMessage();
			}
		}
		return weeklyBatchName;
	}
	/**
	 *
	 */
/*	public void beforeReportInit() throws JRScriptletException
	{
	}
*/

	/**
	 *
	 */
/*	public void afterReportInit() throws JRScriptletException
	{
	}
*/

	/**
	 *
	 */
/*	public void beforePageInit() throws JRScriptletException
	{
	}
*/

	/**
	 *
	 */
/*	public void afterPageInit() throws JRScriptletException
	{
	}
*/

	/**
	 *
	 */
/*	public void beforeColumnInit() throws JRScriptletException
	{
	}
*/

	/**
	 *
	 */
/*	public void afterColumnInit() throws JRScriptletException
	{
	}
*/

	/**
	 *
	 */
/*	public void beforeGroupInit(String groupName) throws JRScriptletException
	{
	}
*/

	/**
	 *
	 */
/*	public void afterGroupInit(String groupName) throws JRScriptletException
	{
	}
*/

	/**
	 *
	 */
/*	public void beforeDetailEval() throws JRScriptletException
	{
	}
*/

	/**
	 *
	 */
/*	public void afterDetailEval() throws JRScriptletException
	{
	}
*/

}
