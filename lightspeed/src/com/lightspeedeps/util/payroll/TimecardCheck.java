package com.lightspeedeps.util.payroll;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.*;

import com.lightspeedeps.model.*;
import com.lightspeedeps.object.*;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.*;

/**
 * Superclass for the IndivTimecardBean, FullTimecardBean, and
 * MobileTimecardBean classes, which are the backing beans for the "basic",
 * "full", and several mobile timecard pages. This class is designed to
 * encapsulate all the code common to those pages, such as Edit, Save, Submit,
 * Approve, Reject, and data validation.
 */
public class TimecardCheck {
	/** */
	private static final Log LOG = LogFactory.getLog(TimecardCheck.class);

	private static final String CITY_OTHER ="Other";

	private TimecardCheck() { // prevent instantiation
	}

	/**
	 * Validate the all fields in the Timecard table. This is done when the
	 * timecard is saved, so additional validations, not done during data entry,
	 * are warranted.
	 *
	 * @param wtc The timecard to check.
	 * @return false if the validation is false, in which case one or more error
	 *         messages will have been queued. If true is returned, one or more
	 *         informational messages may have been queued.
	 */
	public static boolean validateAll(WeeklyTimecard wtc) {

		// Validate everything except PayJob and PayBreakdown
		boolean check = validateUserData(wtc);

		// Validate PayJob and PayBreakdown
		check &= validateNonUserData(wtc);

		return check;
	}

	/**
	 * Validate the fields normally entered by the end user. This is everything
	 * except the Job Tables (PayJob) and Pay Breakdown table.
	 *
	 * @param wtc The timecard to check.
	 * @return false if the validation is false, in which case one or more error
	 *         messages will have been queued. If true is returned, one or more
	 *         informational messages may have been queued.
	 */
	public static boolean validateUserData(WeeklyTimecard wtc) {
		boolean check = true;
//		wtc.setRate(NumberUtils.scaleHourlyRate(wtc.getRate())); // not editable
//		wtc.setGuarHours(NumberUtils.scaleTo2Places(wtc.getGuarHours())); // not editable
		BigDecimal exemptHours = calculateExemptHours(wtc);

		DailyTime priorDt = null;

		boolean checkCityState = wtc.getProduction().getType().isAicp(); // only for commercial (Team)

		//LS-1842 city and state validation for timecard
		if (checkCityState && (wtc.getCityWorked() != null) && (! wtc.getCityWorked().isEmpty())) {
			boolean result =
					LocationUtils.validateCityState(wtc.getCityWorked(), wtc.getStateWorked());

			if (! result && !(wtc.getStartForm().getModelRelease() != null &&
					wtc.getCityWorked().equalsIgnoreCase(CITY_OTHER))) {
				//LS-4589 model release check for allow "Other"  as city

					check = false;
					MsgUtils.addFacesMessage("Address.CityStateMisMatched",
							FacesMessage.SEVERITY_ERROR, wtc.getCityWorked(), wtc.getStateWorked());
			}
		}

		boolean isHybrid = wtc.getProduction().getPayrollPref().getIncludeTouring();
		boolean isTeamPayroll = false;
		if (wtc.getProduction().getPayrollPref().getPayrollService() != null) {
			isTeamPayroll =
					wtc.getProduction().getPayrollPref().getPayrollService().getTeamPayroll();
		}
		boolean showResident = FF4JUtils.useFeature(FeatureFlagType.TTCO_RESIDENT_COMMERCIAL_TIMECARDS);
		// 1) Validation for the DailyTime
		// For each day of the week
		for (DailyTime dailyTime :  wtc.getDailyTimes()) {
			// LS-2161 Hybrid Timecard State/City Validations
			if (isHybrid) {
				if (dailyTime.getDayType() != null) {
					check &= validateDaytypeCityState(dailyTime);
				}
			}
			else if (showResident && isTeamPayroll && dailyTime.getDayType() != null &&
					StringUtils.isEmpty(dailyTime.getCity())) {
				MsgUtils.addFacesMessage("Timecard.DayType.MissingCity",
						FacesMessage.SEVERITY_ERROR);
				check = false;
			}

			//LS-1842 city and state validation for timecard
			if (checkCityState && dailyTime.getCity() != null && ! dailyTime.getCity().isEmpty() &&
					! Constants.TOURS_HOME_STATE.equals(dailyTime.getState()) &&
					Constants.DEFAULT_COUNTRY_CODE.equals(dailyTime.getCountry())) {
				boolean result =
						LocationUtils.validateCityState(dailyTime.getCity(), dailyTime.getState());
				if (! result && ! (dailyTime.getCity().equalsIgnoreCase(CITY_OTHER))) {
					//LS-4589 model release check for allow "Other"  as city
						check = false;
						MsgUtils.addFacesMessage("Address.CityStateMisMatched",
								FacesMessage.SEVERITY_ERROR, dailyTime.getCity(),
								dailyTime.getState());
				}
			}

			// Get the list of validation error messages
			check &= validateWorkDay(dailyTime, priorDt, exemptHours, true);

			// For Full Time Card
			check &= validateFullTimecardHours(dailyTime, true);

			if (check) {
				// ok so far; verify hours entered or "Worked" checked if day type = Work. LS-2189
				if (dailyTime.getDayType() != null && dailyTime.getDayType().isWorkTime() && ! dailyTime.reportedWork()) {
					String date = new SimpleDateFormat("M/d").format(dailyTime.getDate());
					if (wtc.getAllowWorked()) {
						MsgUtils.addFacesMessage("Timecard.WorkTypeWithoutHoursOrWorked", FacesMessage.SEVERITY_ERROR, date);
					}
					else {
						MsgUtils.addFacesMessage("Timecard.WorkTypeWithoutHours", FacesMessage.SEVERITY_ERROR, date);
					}
					check = false;
				}
			}

			priorDt = dailyTime;
		}

		// Check hours, rates, mileages
		check &= validateWeeklyFields(wtc);

		// Check all other values -- those not related to daily inputs.
		check &= validateExpenseItems(wtc);

		// Validate Mileage form line items
		if (wtc.getMileage() != null) {
			for (MileageLine mileageLine : wtc.getMileage().getMileageLines()) {
				check &= validateMileageLine(mileageLine);
			}
		}

		// Validate Box Rental form amount
		if (wtc.getBoxRental() != null) {
			check &= validateBoxRental(wtc.getBoxRental().getAmount());
		}
		//LS-1562
		if (wtc.getStartForm().getHasTourRates() && wtc.getDailyTimes() != null) {
			check &= validateTouringRates(wtc);
		}
		return check;
	}

	private static boolean validateNonUserData(WeeklyTimecard wtc) {
		boolean check = true;

		// Check Job Table (PayJob) hour entries within range
		check &= validatePayJobFields(wtc);

		// Validate pay-breakdown line items
		check &= validatePayBreakdown(wtc);

		return check;
	}

	/**
	 * Validate the fields normally entered by the user on Preferences/ Globals tab.
	 *
	 * @param wtc The timecard to check.
	 * @return false if the validation is false, in which case one or more error
	 *         messages will have been queued. If true is returned, one or more
	 *         informational messages may have been queued.
	 */
	public static boolean validateGlobalsData(WeeklyTimecard wtc) {
		boolean check = true;

		// 1) Validation for the DailyTime
		// For each day of the week
		for (DailyTime dailyTime :  wtc.getDailyTimes()) {
			// Get the list of validation error messages
			// For Full Time Card
			check &= validateFullTimecardHours(dailyTime, false);
		}

		return check;
	}

	/**
	 * Validate the employee's input values for a given day. Issue error
	 * messages for any problems (if showMsgs is true). If no problems are
	 * found, calculate the work hours for the day and update the value in the
	 * given DailyTime.
	 *
	 * @param dt The DailyTime entry to be validated and possibly updated.
	 * @param showMsgs If true, error messages will be issued for any validation
	 *            failures.
	 * @return True iff no validation errors were found.
	 */
	public static boolean validateAndCalcWorkDay(DailyTime dt, boolean showMsgs) {
		dt.setHours(null);
		BigDecimal exemptHours = calculateExemptHours(dt.getWeeklyTimecard());
		DailyTime priorDt = null;
		if (dt.getDayNum() != 1) {
			priorDt = dt.getWeeklyTimecard().getDailyTimes().get(dt.getDayNum()-2);
		}
		boolean bRet = TimecardCheck.validateWorkDay(dt, priorDt, exemptHours, showMsgs);
		if (bRet) {
			// Calculate the hours of work for the given day
			if (! dt.getWorked()) {
				TimecardCalc.calculateDailyHours(dt);
			}
		}
		return bRet;
	}

	/**
	 * Validate user inputs and issues error messages for any validation
	 * failures for a single day.
	 *
	 * @param dt the DailyTime whose fields are to be validated.
	 * @param exemptHours
	 * @param showMsgs True if messages should be issued for errors.
	 * @return True iff all fields passed validation.
	 */
	public static boolean validateWorkDay(DailyTime dt, DailyTime priorDt, BigDecimal exemptHours, boolean showMsgs) {
		boolean bRet = true;

		String date = new SimpleDateFormat("M/d").format(dt.getDate());

		if (dt.getWorked() || (dt.getDayType() != null && dt.getDayType().isIdle())) {
			dt.setCallTime(null);
			dt.setM1Out(null);
			dt.setM1In(null);
			dt.setM2Out(null);
			dt.setM2In(null);
			dt.setWrap(null);
			dt.setOnCallStart(null);
			dt.setOnCallEnd(null);
			dt.setHours(null);
		}
		if (dt.getWorked()) {
			if (dt.getDayType() != null && dt.getDayType().isIdle()) {
				dt.setWorked(false);
			}
			else if (dt.getDayType() != null && dt.getDayType() == DayType.HA) {
				// Someone checked "Worked" when Day type was HOA - set to Work. LS-2189
				dt.setDayType(DayType.WK);
			}
			else {
				dt.setHours(exemptHours);
				dt.setMpvUser(null);
			}
//			if (dt.getDayType() != null && dt.getDayType().isIdle()) {
//				MsgUtils.addFacesMessage("Timecard.IdleWorked", FacesMessage.SEVERITY_ERROR, date);
//				bRet = false;
//			}
			return bRet;
		}
		BigDecimal call, wrap, start1, start2, end1, end2, split2, split3, ocStart, ocEnd;

		call = dt.getCallTime();
		wrap = dt.getWrap();
		start1 = dt.getM1Out();
		end1 = dt.getM1In();
		start2 = dt.getM2Out();
		end2 = dt.getM2In();
		ocStart = dt.getOnCallStart();
		ocEnd = dt.getOnCallEnd();
		split2 = dt.getSplitStart2();
		split3 = dt.getSplitStart3();

		if (call != null && call.compareTo(Constants.HOURS_IN_A_DAY) < 0) {
			// Call looks reasonable.
			// If any other entry is < call, add 12 or 24 to it.
			BigDecimal lastTime = call;
			if (start1 != null) {
				if (start1.compareTo(call) < 0) {
					start1 = start1.add(Constants.HOURS_FROM_AM_TO_PM);
					if (start1.compareTo(call) < 0) { // adding 12 wasn't enough... go 12 more
						start1 = start1.add(Constants.HOURS_FROM_AM_TO_PM);
					}
					dt.setM1Out(start1);
				}
				lastTime = start1;
			}
			if (end1 != null) {
				if (end1.compareTo(lastTime) < 0) {
					end1 = end1.add(Constants.HOURS_FROM_AM_TO_PM);
					if (end1.compareTo(lastTime) < 0) {
						end1 = end1.add(Constants.HOURS_FROM_AM_TO_PM);
					}
					dt.setM1In(end1);
				}
				lastTime = end1;
			}
			if (start2 != null) {
				if (start2.compareTo(lastTime) < 0) {
					start2 = start2.add(Constants.HOURS_FROM_AM_TO_PM);
					if (start2.compareTo(lastTime) < 0) {
						start2 = start2.add(Constants.HOURS_FROM_AM_TO_PM);
					}
					dt.setM2Out(start2);
				}
				lastTime = start2;
			}
			if (end2 != null) {
				if (end2.compareTo(lastTime) < 0) {
					end2 = end2.add(Constants.HOURS_FROM_AM_TO_PM);
					if (end2.compareTo(lastTime) < 0) {
						end2 = end2.add(Constants.HOURS_FROM_AM_TO_PM);
					}
					dt.setM2In(end2);
				}
				lastTime = end2;
			}
			if (wrap != null) {
				if (wrap.compareTo(lastTime) < 0) {
					wrap = wrap.add(Constants.HOURS_FROM_AM_TO_PM);
					if (wrap.compareTo(lastTime) < 0) {
						wrap = wrap.add(Constants.HOURS_FROM_AM_TO_PM);
					}
				}
				dt.setWrap(wrap);
				lastTime = wrap;
			}


			if (ocStart != null) {
				if (ocStart.compareTo(lastTime) < 0) {
					ocStart = lastTime;
					dt.setOnCallStart(ocStart);
				}
				lastTime = ocStart;
			}
			if (ocEnd != null) {
				dt.setOnCallEnd((ocEnd = calcNextTime(ocEnd, lastTime)));
				lastTime = ocEnd;
			}
		}

		try {

			List<Validate> validateFields = new ArrayList<>();
			validateFields.add(new Validate(FieldType.BIG_DECIMAL, call,
					CompareType.LESS_THAN, Constants.LATEST_CALL_TIME_24, "Timecard.CallTimeOver24", date));
			validateFields.add(new Validate(FieldType.BIG_DECIMAL, wrap,
					CompareType.LESS_THAN_EQUAL, Constants.LATEST_WRAP_TIME_72, "Timecard.TimeOver72", date));
			validateFields.add(new Validate(FieldType.BIG_DECIMAL, start1,
					CompareType.LESS_THAN_EQUAL, Constants.LATEST_WRAP_TIME_72, "Timecard.TimeOver72", date));
			validateFields.add(new Validate(FieldType.BIG_DECIMAL, start2,
					CompareType.LESS_THAN_EQUAL, Constants.LATEST_WRAP_TIME_72, "Timecard.TimeOver72", date));
			validateFields.add(new Validate(FieldType.BIG_DECIMAL, end1,
					CompareType.LESS_THAN_EQUAL, Constants.LATEST_WRAP_TIME_72, "Timecard.TimeOver72", date));
			validateFields.add(new Validate(FieldType.BIG_DECIMAL, end2,
					CompareType.LESS_THAN_EQUAL, Constants.LATEST_WRAP_TIME_72, "Timecard.TimeOver72", date));


			// Validate split fields
			if (split2 != null) {
				validateFields.add(new Validate(FieldType.BIG_DECIMAL, split2,
						CompareType.LESS_THAN_EQUAL, Constants.DECIMAL_100, "Timecard.FillJob.SplitHigh", date));
				if (! dt.getSplitByPercent()) {
					validateFields.add(new Validate(FieldType.BIG_DECIMAL, split2,
							CompareType.LESS_THAN_EQUAL, wrap, "Timecard.FillJob.SplitOutside", date));
					validateFields.add(new Validate(FieldType.BIG_DECIMAL, split2,
							CompareType.GREATER_THAN_EQUAL, call, "Timecard.FillJob.SplitOutside", date));
				}
			}
			if (split3 != null) {
				validateFields.add(new Validate(FieldType.BIG_DECIMAL, split3,
						CompareType.LESS_THAN_EQUAL, Constants.DECIMAL_100, "Timecard.FillJob.SplitHigh", date));
				if (! dt.getSplitByPercent()) {
					validateFields.add(new Validate(FieldType.BIG_DECIMAL, split3,
							CompareType.LESS_THAN_EQUAL, wrap, "Timecard.FillJob.SplitOutside", date));
					validateFields.add(new Validate(FieldType.BIG_DECIMAL, split3,
							CompareType.GREATER_THAN_EQUAL, call, "Timecard.FillJob.SplitOutside", date));
				}
				if (split2 != null) {
					if (dt.getSplitByPercent()) {
						validateFields.add(new Validate(FieldType.BIG_DECIMAL, split2.add(split3),
								CompareType.LESS_THAN_EQUAL, Constants.DECIMAL_100, "Timecard.FillJob.SplitHigh", date));
					}
					else {
						validateFields.add(new Validate(FieldType.BIG_DECIMAL, split2,
								CompareType.LESS_THAN_EQUAL, split3, "Timecard.FillJob.SplitInvalid", date));
					}
				}
				else {
					MsgUtils.addFacesMessage("Timecard.FillJob.SplitOneFirst", FacesMessage.SEVERITY_ERROR, date);
					bRet = false;
				}
			}

			// Validate hour fields
			bRet &= ValidateFields.validateFields(validateFields);

			if (dt.getMpvUser() != null) {
				String mpv = dt.getMpvUser().trim();
				int iMpv = 0;
				try {
					iMpv = Integer.parseInt(mpv);
				}
				catch(Exception e) {
				}
				if (iMpv < 0) {
					if (showMsgs) {
						MsgUtils.addFacesMessage("Timecard.MpvUserNegative", FacesMessage.SEVERITY_ERROR, date);
					}
					bRet = false;
				}
				else if (iMpv > Constants.MAX_DAILY_MPV) {
					if (showMsgs) {
						MsgUtils.addFacesMessage("Timecard.MpvUserTooHigh", FacesMessage.SEVERITY_ERROR, date);
					}
					bRet = false;
				}
			}

			if (! bRet) {
				return false;
			}
			if (call == null || wrap == null) { // not enough to validate
				return true;
			}

			validateFields.clear();
			//Set validation fields
			//If the following conditions are not satisfied, set error message
			// All the time field should be positive
			validateFields.add(new Validate( call,
					CompareType.GREATER_THAN_EQUAL, "Timecard.AllTimeShouldPositive", date));
			validateFields.add(new Validate( wrap,
					CompareType.GREATER_THAN_EQUAL, "Timecard.AllTimeShouldPositive", date));
			validateFields.add(new Validate( start1,
					CompareType.GREATER_THAN_EQUAL, "Timecard.AllTimeShouldPositive", date));
			validateFields.add(new Validate( start2,
					CompareType.GREATER_THAN_EQUAL, "Timecard.AllTimeShouldPositive", date));
			validateFields.add(new Validate( end1,
					CompareType.GREATER_THAN_EQUAL, "Timecard.AllTimeShouldPositive", date));
			validateFields.add(new Validate( end2,
					CompareType.GREATER_THAN_EQUAL, "Timecard.AllTimeShouldPositive", date));
			validateFields.add(new Validate( ocStart,
					CompareType.GREATER_THAN_EQUAL, "Timecard.AllTimeShouldPositive", date));
			validateFields.add(new Validate( ocEnd,
					CompareType.GREATER_THAN_EQUAL, "Timecard.AllTimeShouldPositive", date));


			if (call != null && wrap != null && (call.signum() >= 0 || wrap.signum() >= 0)) {
				validateFields.add(new Validate(FieldType.BIG_DECIMAL, wrap,
						CompareType.GREATER_THAN, call, "Timecard.OutIsGreaterThanCall", date));// Wrap time should be greater than Call time
				if (ocStart != null && ocEnd != null) {
					validateFields.add(new Validate(FieldType.BIG_DECIMAL, ocStart,
							CompareType.GREATER_THAN_EQUAL, wrap, "Timecard.OnCallStartGreaterThanWrap", date));
					validateFields.add(new Validate(FieldType.BIG_DECIMAL, ocEnd,
							CompareType.GREATER_THAN_EQUAL, ocStart, "Timecard.OnCallEndGreaterThanStart", date));
				}
			}

			if (start1 != null && start1.signum() >= 0) {
				validateFields.add(new Validate(FieldType.BIG_DECIMAL, call,
						CompareType.LESS_THAN, start1, "Timecard.Meal1StartIsGreaterThanCall", date));// In Meal 1, Out time should be greater than Call time
				if (end1 != null) {
					if (dt.getDayType() != null && (dt.getDayType() == DayType.TW || dt.getDayType() == DayType.WT)) {
						validateFields.add(new Validate(FieldType.BIG_DECIMAL, end1,
								CompareType.GREATER_THAN_EQUAL, start1, "Timecard.Meal1EndIsGreaterThanStart", date));// In Meal 1, In time should be greater than Out time
					}
					else {
						validateFields.add(new Validate(FieldType.BIG_DECIMAL, end1,
								CompareType.GREATER_THAN, start1, "Timecard.Meal1EndIsGreaterThanStart", date));// In Meal 1, In time should be greater than Out time
					}
				}
			}
			if (start2 != null && start2.signum() >= 0) {
				validateFields.add(new Validate(FieldType.BIG_DECIMAL, call,
					CompareType.LESS_THAN, start2, "Timecard.Meal2StartIsGreaterThanCall", date));// In Meal 2, Out time should be greater than Call time
				if (end2 != null) {
					if (dt.getDayType() != null && (dt.getDayType() == DayType.TW || dt.getDayType() == DayType.WT)) {
						validateFields.add(new Validate(FieldType.BIG_DECIMAL, end2,
								CompareType.GREATER_THAN_EQUAL, start2, "Timecard.Meal2EndIsGreaterThanStart", date));// In Meal 2, In time should be greater than Out time
					}
					else {
						validateFields.add(new Validate(FieldType.BIG_DECIMAL, end2,
							CompareType.GREATER_THAN, start2, "Timecard.Meal2EndIsGreaterThanStart", date));// In Meal 2, In time should be greater than Out time
					}
				}
			}

			validateFields.add(new Validate(FieldType.BIG_DECIMAL, wrap,
					CompareType.GREATER_THAN_EQUAL, end1, "Timecard.Meal1EndIsGreaterThanOut", date));// In Meal 1, In time should be less than Wrap time
			validateFields.add(new Validate(FieldType.BIG_DECIMAL, wrap,
					CompareType.GREATER_THAN_EQUAL, end2, "Timecard.Meal2EndIsGreaterThanOut", date));// In Meal 2, In time should be less than Wrap time

			if (priorDt != null && priorDt.getWrap() != null) {
				if (priorDt.getOnCallEnd() != null && priorDt.getOnCallEnd().compareTo(Constants.HOURS_IN_A_DAY) > 0) {
					BigDecimal priorEnd =  priorDt.getOnCallEnd().subtract(Constants.HOURS_IN_A_DAY);
					// Prior day's On-call end time should be less than Call time
					validateFields.add(new Validate(FieldType.BIG_DECIMAL, priorEnd,
							CompareType.LESS_THAN_EQUAL, call, "Timecard.CallEarlierThanPriorOnCallEnd", date));
				}
				else if (priorDt.getWrap().compareTo(Constants.HOURS_IN_A_DAY) > 0) {
					BigDecimal priorWrap =  priorDt.getWrap().subtract(Constants.HOURS_IN_A_DAY);
					// Prior day's Wrap time should be less than Call time
					validateFields.add(new Validate(FieldType.BIG_DECIMAL, priorWrap,
							CompareType.LESS_THAN_EQUAL, call, "Timecard.CallEarlierThanPriorWrap", date));
				}
			}
			//Validate all the fields
			return ValidateFields.validateFields(validateFields, showMsgs);
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
		}
		return false;
	}

	private static BigDecimal calcNextTime(BigDecimal checkTime, BigDecimal lastTime) {
		if (checkTime != null) {
			if (checkTime.compareTo(lastTime) < 0) {
				checkTime = checkTime.add(Constants.HOURS_FROM_AM_TO_PM);
				if (checkTime.compareTo(lastTime) < 0) {
					checkTime = checkTime.add(Constants.HOURS_FROM_AM_TO_PM);
				}
			}
		}
		return checkTime;
	}

	/**
	 * Additional validation of daily information, done only when the timecard
	 * is about to be saved.
	 *
	 * @param dailyTime
	 * @param isFullTc True for Full Timecard's validation and false for Globals page.
	 * @return False iff any error was detected, in which case one or more error
	 *         messages will have been queued.
	 */
	private static boolean validateFullTimecardHours(DailyTime dailyTime, boolean isFullTc) {
		try {
			String date = new SimpleDateFormat("M/d").format(dailyTime.getDate());

			List<Validate> validateFields = new ArrayList<>();

			// Set validation conditions
			if (isFullTc) {
				Integer mpv1Payroll = (dailyTime.getMpv1Payroll() == null ? 0 : (int)dailyTime.getMpv1Payroll());
				Integer mpv2Payroll = (dailyTime.getMpv2Payroll() == null ? 0 : (int)dailyTime.getMpv2Payroll());
				validateFields.add(new Validate(FieldType.INTEGER, mpv1Payroll,
						CompareType.GREATER_THAN_EQUAL, 0, "Timecard.MpvPayrollShouldPositive", date));// Mpv Payroll should be positive.
				validateFields.add(new Validate(FieldType.INTEGER, mpv2Payroll,
						CompareType.GREATER_THAN_EQUAL, 0, "Timecard.MpvPayrollShouldPositive", date));// Mpv Payroll should be positive.
				validateFields.add(new Validate(FieldType.BIG_DECIMAL, dailyTime.getLastManIn(),
						CompareType.LESS_THAN_EQUAL, Constants.LATEST_WRAP_TIME_72, "Timecard.TimeOver72", date));
			}
			validateFields.add(new Validate(FieldType.BIG_DECIMAL, dailyTime.getGrace1(),
					CompareType.LESS_THAN_EQUAL, BigDecimal.ONE, "Timecard.GraceTooLarge", date));
			validateFields.add(new Validate(FieldType.BIG_DECIMAL, dailyTime.getGrace2(),
					CompareType.LESS_THAN_EQUAL, BigDecimal.ONE, "Timecard.GraceTooLarge", date));
			validateFields.add(new Validate(FieldType.BIG_DECIMAL, dailyTime.getGrace1(),
					CompareType.GREATER_THAN_EQUAL, BigDecimal.ZERO, "Timecard.GraceNegative", date));
			validateFields.add(new Validate(FieldType.BIG_DECIMAL, dailyTime.getGrace2(),
					CompareType.GREATER_THAN_EQUAL, BigDecimal.ZERO, "Timecard.GraceNegative", date));
			validateFields.add(new Validate(FieldType.BIG_DECIMAL, dailyTime.getNdmEnd(),
					CompareType.LESS_THAN_EQUAL, Constants.LATEST_WRAP_TIME_72, "Timecard.TimeOver72", date));
			if (dailyTime.getNdmStart() != null && dailyTime.getNdmEnd() != null) {
				validateFields.add(new Validate(FieldType.BIG_DECIMAL, dailyTime.getNdmEnd(),
						CompareType.GREATER_THAN, dailyTime.getNdmStart(), "Timecard.NdmEnd", date));
			}

			// Validate all the fields & generate error messages, if any.
			boolean bRet = ValidateFields.validateFields(validateFields);

			if (isFullTc && dailyTime.getCallTime() == null &&
					(dailyTime.getWrap() != null ||
					dailyTime.getM1Out() != null || dailyTime.getM2Out() != null)) {
				// missing call time & at least one "later" time filled in
				MsgUtils.addFacesMessage("Timecard.CallTimeMissing", FacesMessage.SEVERITY_ERROR, date);
				bRet = false;
			}

			if (isFullTc && dailyTime.getCallTime() != null && dailyTime.getWrap() == null) {
				MsgUtils.addFacesMessage("Timecard.WrapTimeMissing", FacesMessage.SEVERITY_ERROR, date);
				bRet = false;
			}

			if (isFullTc && dailyTime.getWrap() != null &&
					((dailyTime.getM1Out() != null && dailyTime.getM1In() == null) ||
					(dailyTime.getNdmStart() != null && dailyTime.getNdmEnd() == null) ||
					(dailyTime.getM2Out() != null && dailyTime.getM2In() == null))) {
				// have wrap, but missing matching end of meal for a start of meal
				MsgUtils.addFacesMessage("Timecard.MealTimeMissing", FacesMessage.SEVERITY_ERROR, date);
				bRet = false;
			}

			if (isFullTc && ((dailyTime.getM1In() != null && dailyTime.getM1Out() == null) ||
					(dailyTime.getNdmEnd() != null && dailyTime.getNdmStart() == null) ||
					(dailyTime.getM2In() != null && dailyTime.getM2Out() == null))) {
				// missing start of meal when we have the corresponding end of meal
				MsgUtils.addFacesMessage("Timecard.MealTimeMissing", FacesMessage.SEVERITY_ERROR, date);
				bRet = false;
			}

			if (isFullTc && dailyTime.getM1In() != null && dailyTime.getNdmStart() != null &&
					dailyTime.getNdmStart().compareTo(dailyTime.getM1In()) <= 0) {
				//- if Meal 1 In is entered, then NDM start (if entered) must be later than meal 1 in.
				MsgUtils.addFacesMessage("Timecard.NdmStartBeforeMeal1", FacesMessage.SEVERITY_ERROR, date);
				bRet = false;
			}

			if (bRet && isFullTc && dailyTime.getWrap() != null) {
				if (dailyTime.getOnCallStart() != null && dailyTime.getOnCallEnd() == null) {
					MsgUtils.addFacesMessage("Timecard.OnCallEndMissing", FacesMessage.SEVERITY_ERROR, date);
					bRet = false;
				}
				else if (isFullTc && dailyTime.getOnCallStart() == null && dailyTime.getOnCallEnd() != null) {
					MsgUtils.addFacesMessage("Timecard.OnCallStartMissing", FacesMessage.SEVERITY_ERROR, date);
					bRet = false;
				}
			}


//			if (! dailyTime.getNonDeductMeal2Payroll() &&
//					dailyTime.getNdmStart() != null && dailyTime.getNdmEnd() != null) {
//				dailyTime.setNonDeductMeal2Payroll(true);
//			}

			if (bRet && isFullTc && dailyTime.getM2In() != null && dailyTime.getNdmStart() != null) {
				// no errors so far; both meal 2 & NDM meal specified; check for conflicts
				if (! TimecardCalc.ndmMatches(dailyTime, false)) {
					MsgUtils.addFacesMessage("Timecard.NdmConflictsWithMeal2", FacesMessage.SEVERITY_ERROR, date);
					bRet = false;
				}
			}
//			if (dailyTime.getM2Out() != null && dailyTime.getNdmEnd() != null &&
//					dailyTime.getNdmEnd().compareTo(dailyTime.getM2Out()) >= 0) {
//				//- if Meal 2 Out is entered, then NDM end (if entered) must be earlier than meal 2 out.
//				MsgUtils.addFacesMessage("Timecard.NdmEndAfterMeal2", FacesMessage.SEVERITY_INFO, date);
//				bRet = false;
//			}

			return bRet;
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
		}
		return false;
	}

	/**
	 * Validate hourly data within the Job Table (PayJob`s).
	 *
	 * @param wtc The timecard of interest.
	 * @return true iff the PayJob data passed all validation tests.
	 */
	private static boolean validatePayJobFields(WeeklyTimecard wtc) {
		boolean bRet = true;
		try {
			List<String> errorList = new ArrayList<>(); // list of error message ids
			for (PayJob pj : wtc.getPayJobs()) {
				for (PayJobDaily pjd : pj.getPayJobDailies()) {
					ValidateFields.validateBigDecimal(pjd.getHours10(), CompareType.LESS_THAN_EQUAL,
							Constants.MAX_PAY_HOURS_IN_A_DAY, "Timecard.PayJob.HoursTooBig", errorList);
					ValidateFields.validateBigDecimal(pjd.getHours15(), CompareType.LESS_THAN_EQUAL,
							Constants.MAX_PAY_HOURS_IN_A_DAY, "Timecard.PayJob.HoursTooBig", errorList);
					ValidateFields.validateBigDecimal(pjd.getHoursCust1(), CompareType.LESS_THAN_EQUAL,
							Constants.MAX_PAY_HOURS_IN_A_DAY, "Timecard.PayJob.HoursTooBig", errorList);
					ValidateFields.validateBigDecimal(pjd.getHoursCust2(), CompareType.LESS_THAN_EQUAL,
							Constants.MAX_PAY_HOURS_IN_A_DAY, "Timecard.PayJob.HoursTooBig", errorList);
					ValidateFields.validateBigDecimal(pjd.getHoursCust3(), CompareType.LESS_THAN_EQUAL,
							Constants.MAX_PAY_HOURS_IN_A_DAY, "Timecard.PayJob.HoursTooBig", errorList);
					ValidateFields.validateBigDecimal(pjd.getHoursCust4(), CompareType.LESS_THAN_EQUAL,
							Constants.MAX_PAY_HOURS_IN_A_DAY, "Timecard.PayJob.HoursTooBig", errorList);
					ValidateFields.validateBigDecimal(pjd.getHoursCust5(), CompareType.LESS_THAN_EQUAL,
							Constants.MAX_PAY_HOURS_IN_A_DAY, "Timecard.PayJob.HoursTooBig", errorList);
					ValidateFields.validateBigDecimal(pjd.getHoursCust6(), CompareType.LESS_THAN_EQUAL,
							Constants.MAX_PAY_HOURS_IN_A_DAY, "Timecard.PayJob.HoursTooBig", errorList);
					ValidateFields.validateByte(pjd.getMpv1(), CompareType.GREATER_THAN_EQUAL,
							(byte)0, "Timecard.PayJob.MpvPositive", errorList);
					ValidateFields.validateByte(pjd.getMpv2(), CompareType.GREATER_THAN_EQUAL,
							(byte)0, "Timecard.PayJob.MpvPositive", errorList);
					if (pj.getHasNpHours()) {
						ValidateFields.validateBigDecimal(pjd.getHours10Np1(), CompareType.LESS_THAN_EQUAL,
								Constants.HOURS_IN_A_DAY, "Timecard.PayJob.HoursTooBig", errorList);
						ValidateFields.validateBigDecimal(pjd.getHours10Np2(), CompareType.LESS_THAN_EQUAL,
								Constants.HOURS_IN_A_DAY, "Timecard.PayJob.HoursTooBig", errorList);
						ValidateFields.validateBigDecimal(pjd.getHours15Np1(), CompareType.LESS_THAN_EQUAL,
								Constants.HOURS_IN_A_DAY, "Timecard.PayJob.HoursTooBig", errorList);
						ValidateFields.validateBigDecimal(pjd.getHours15Np2(), CompareType.LESS_THAN_EQUAL,
								Constants.HOURS_IN_A_DAY, "Timecard.PayJob.HoursTooBig", errorList);
					}
				}

				ValidateFields.validateByte(pj.getTotalMpv1(), CompareType.GREATER_THAN_EQUAL,
						(byte)0, "Timecard.PayJob.MpvPositive", errorList);
				ValidateFields.validateByte(pj.getTotalMpv2(), CompareType.GREATER_THAN_EQUAL,
						(byte)0, "Timecard.PayJob.MpvPositive", errorList);
			}
			if (errorList.size() > 0) {
				bRet = false;
				MsgUtils.addFacesMessage(errorList, FacesMessage.SEVERITY_ERROR);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
//			MsgUtils.addGenericErrorMessage();
//			bRet = false;
		}
		return bRet;
	}

	/**
	 * Validate "weekly" fields in Timecard - currently a no-op!!!
	 * @return True
	 */
	public static boolean validateWeeklyFields(WeeklyTimecard wtc) {
		// ************ currently nothing to validate here; keep as placeholder *************
		return true;
	}

	/**
	 * Validate the Expense/Reimbursement table fields in a timecard.
	 * @param wtc the WeeklyTimecard whose fields are to be validated.
	 * @return true iff the items passed all validation tests.
	 */
	public static boolean validateExpenseItems( WeeklyTimecard wtc ) {
		boolean bRet = true;
		try {
			List<String> errorList = new ArrayList<>(); // list of error message ids

			for (PayExpense pe : wtc.getExpenseLines()) {

				validateExpenseItem(pe, errorList);
			}

			if (errorList.size() > 0) {
				bRet = false;
				MsgUtils.addFacesMessage(errorList, FacesMessage.SEVERITY_ERROR);
			}
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
			MsgUtils.addGenericErrorMessage();
			bRet = false;
		}
		return bRet;
	}

	/**
	 * Validate fields in a single Expense/Reimbursement line item in a
	 * timecard. This will generate the necessary Faces error messages.
	 *
	 * @param pe The PayExpense object to validate.
	 */
	public static boolean validateExpenseItem(PayExpense pe) {
		boolean bRet = true;
		List<String> errorList = new ArrayList<>(); // list of error message ids

		validateExpenseItem(pe, errorList);

		if (errorList.size() > 0) {
			bRet = false;
			MsgUtils.addFacesMessage(errorList, FacesMessage.SEVERITY_ERROR);
		}

		return bRet;
	}

	/**
	 * Validate fields in a single Expense/Reimbursement line item in a
	 * timecard.
	 *
	 * @param pe The PayExpense object to validate.
	 * @param errorList List of error messages to be added to, if necessary.
	 */
	private static void validateExpenseItem(PayExpense pe, List<String> errorList) {

		ValidateFields.validateBigDecimal(pe.getQuantity(), CompareType.LESS_THAN,
				Constants.DECIMAL_10K, "Timecard.Expenses.HoursTooBig", errorList);
		ValidateFields.validateBigDecimal(pe.getRate(), CompareType.LESS_THAN,
				Constants.DECIMAL_100K, "Timecard.Expenses.RateTooBig", errorList);
		ValidateFields.validateBigDecimal(pe.getTotal(), CompareType.LESS_THAN,
				Constants.DECIMAL_100_MILLION, "Timecard.Expenses.TotalTooBig", errorList);

		ValidateFields.validateBigDecimal(pe.getQuantity(), CompareType.GREATER_THAN,
				Constants.DECIMAL_NEG_10K, "Timecard.Expenses.HoursTooSmall", errorList);
		ValidateFields.validateBigDecimal(pe.getRate(), CompareType.GREATER_THAN,
				Constants.DECIMAL_NEG_100K, "Timecard.Expenses.RateTooSmall", errorList);
		ValidateFields.validateBigDecimal(pe.getTotal(), CompareType.GREATER_THAN,
				Constants.DECIMAL_NEG_100_MILLION, "Timecard.Expenses.TotalTooSmall", errorList);

		if (! pe.getCategoryType().getAllowsNegative()) { // LS-3961 Prevent negative values
			if ((pe.getQuantity() != null && pe.getQuantity().signum() < 0) ||
					(pe.getRate() != null && pe.getRate().signum() < 0)) {
				errorList.add("Timecard.Expenses.NegativeNotAllowed");
			}
		}
	}

	/**
	 * Determine if the total hours listed in the PayJob objects equals the
	 * total of raw hours entered on the timecard.
	 *
	 * @param wtc The WeeklyTimecard to be examined.
	 * @return True if any of these are true: (a) the timecard is NOT for an
	 *         hourly employee; (b) there are no PayJobs created yet; (c) no
	 *         hours are entered in the PayJobs yet (the total of all PayJob
	 *         hours is zero); or (d) the total of the PayJob hours matches the
	 *         WeeklyTimecard.totalHours field.
	 */
	public static boolean validateJobHours(WeeklyTimecard wtc) {
		if (wtc.getEmployeeRateType() != EmployeeRateType.HOURLY) {
			// for daily or weekly employees, return true, as hours check is often wrong
			return true;
		}
		if (wtc.getPayJobs().size() == 0) {
			return true;
		}
		boolean bRet = false;
		try {
			BigDecimal total = BigDecimal.ZERO;
			for (PayJob pj : wtc.getPayJobs()) {
				total = NumberUtils.safeAdd(total, pj.getTotalHours());
			}
			if (total.signum() == 0 || total.compareTo(wtc.getTotalHours()) == 0) {
				bRet = true;
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return bRet;
	}

	/**
	 * Validate that the values in the Pay Breakdown line items are within the
	 * allowable ranges.
	 *
	 * @param wtc The timecard to be validated.
	 * @return True iff all values are within expected bounds. When false is
	 *         returned, one or more error messages will have been issued.
	 */
	public static boolean  validatePayBreakdown(WeeklyTimecard wtc) {
		boolean bRet = true;
		List<String> errorList = new ArrayList<>(); // list of error message ids

		for (PayBreakdown pb : wtc.getPayLines()) {
			ValidateFields.validateBigDecimal(pb.getMultiplier(), CompareType.GREATER_THAN_EQUAL,
					BigDecimal.ZERO, "Timecard.PayBreakdown.MultiplierPositive", errorList);

			ValidateFields.validateBigDecimal(pb.getQuantity(), CompareType.LESS_THAN,
					Constants.DECIMAL_10K, "Timecard.PayBreakdown.HoursTooBig", errorList);
			ValidateFields.validateBigDecimal(pb.getRate(), CompareType.LESS_THAN,
					Constants.DECIMAL_100K, "Timecard.PayBreakdown.RateTooBig", errorList);
			ValidateFields.validateBigDecimal(pb.getMultiplier(), CompareType.LESS_THAN,
					Constants.DECIMAL_1K, "Timecard.PayBreakdown.MultiplierTooBig", errorList);
			ValidateFields.validateBigDecimal(pb.getTotal(), CompareType.LESS_THAN,
					Constants.DECIMAL_100_MILLION, "Timecard.PayBreakdown.TotalTooBig", errorList);

			ValidateFields.validateBigDecimal(pb.getQuantity(), CompareType.GREATER_THAN,
					Constants.DECIMAL_NEG_10K, "Timecard.PayBreakdown.HoursTooSmall", errorList);
			ValidateFields.validateBigDecimal(pb.getRate(), CompareType.GREATER_THAN,
					Constants.DECIMAL_NEG_100K, "Timecard.PayBreakdown.RateTooSmall", errorList);
			ValidateFields.validateBigDecimal(pb.getTotal(), CompareType.GREATER_THAN,
					Constants.DECIMAL_NEG_100_MILLION, "Timecard.PayBreakdown.TotalTooSmall", errorList);
		}

		ValidateFields.validateBigDecimal(wtc.getGrandTotal(), CompareType.LESS_THAN,
				Constants.DECIMAL_100_MILLION, "Timecard.PayBreakdown.GrandTotalTooBig", errorList);
		ValidateFields.validateBigDecimal(wtc.getGrandTotal(), CompareType.GREATER_THAN,
				Constants.DECIMAL_NEG_100_MILLION, "Timecard.PayBreakdown.GrandTotalTooSmall", errorList);

		if (errorList.size() > 0) {
			bRet = false;
			MsgUtils.addFacesMessage(errorList, FacesMessage.SEVERITY_ERROR);
		}
		return bRet;
	}

	/**
	 * Validate the values in one MileageLine (detail line) entry.
	 *
	 * @param mileageLine The entry to validate
	 * @return True iff all fields are valid (which may include null).
	 */
	public static boolean validateMileageLine(MileageLine mileageLine) {
		try {
			boolean ret = true;
			if (mileageLine.getDate() == null) {
				MsgUtils.addFacesMessage("Mileage.MissingDate", FacesMessage.SEVERITY_ERROR);
				ret = false;
			}
			List<Validate> validateFields = new ArrayList<>();
			// Set validation fields

			validateFields.add(new Validate( mileageLine.getOdometerStart(),
					CompareType.GREATER_THAN_EQUAL, "Mileage.NegativeOdometer", ""));
			validateFields.add(new Validate( mileageLine.getOdometerStart(),
					CompareType.LESS_THAN, Constants.DECIMAL_1_MILLION, "Mileage.OdometerHigh"));

			validateFields.add(new Validate( mileageLine.getOdometerEnd(),
					CompareType.GREATER_THAN_EQUAL, "Mileage.NegativeOdometer", ""));
			validateFields.add(new Validate( mileageLine.getOdometerEnd(),
					CompareType.LESS_THAN, Constants.DECIMAL_1_MILLION, "Mileage.OdometerHigh"));

			if (mileageLine.getMiles() == null || mileageLine.getMiles().signum() == 0) {
				if (mileageLine.getOdometerStart() != null && mileageLine.getOdometerEnd() != null) {
					mileageLine.setMiles(mileageLine.getOdometerEnd().subtract(mileageLine.getOdometerStart()));
				}
			}

			validateFields.add(new Validate( mileageLine.getMiles(),
					CompareType.GREATER_THAN_EQUAL, "Mileage.NegativeOdometer", ""));
			validateFields.add(new Validate( mileageLine.getMiles(),
					CompareType.LESS_THAN, Constants.DECIMAL_10K, "Mileage.MilesHigh"));

			//Validate all the fields
			return ret && ValidateFields.validateFields(validateFields);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return false;
	}

	/**
	 * Validate the box rental amount on the Box Rental form.
	 * @param amount
	 * @return True if the value is valid.
	 */
	public static boolean validateBoxRental(BigDecimal amount) {
		boolean bRet = true;
		if (amount != null) {
			List<String> errorList = new ArrayList<>(); // list of error message ids
			// Validate amount & generate any error messages
			ValidateFields.validateBigDecimal(amount, CompareType.LESS_THAN,
					Constants.DECIMAL_100K, "Timecard.BoxAmountTooBig", errorList);
			ValidateFields.validateBigDecimal(amount, CompareType.GREATER_THAN,
					Constants.DECIMAL_NEG_100K, "Timecard.BoxAmountTooSmall", errorList);
			if (errorList.size() > 0) {
				bRet = false;
				MsgUtils.addFacesMessage(errorList, FacesMessage.SEVERITY_ERROR);
			}
		}
		return bRet;
	}

	/** Validate the rates for the touring day types selected on the Timecard.
	 * @param wtc
	 * @return True if the rate value for the selected touring day types is not null.
	 */
	public static boolean validateTouringRates(WeeklyTimecard wtc) {
		boolean bRet = true;
		try {
			LOG.debug("");
			StartForm sf = wtc.getStartForm();
			BigDecimal rate;
			for (DailyTime dt : wtc.getDailyTimes()) {
				DayType dayType = dt.getDayType();
				if (dayType != null && dayType.isTours() && dayType != DayType.NONE) {
					rate = sf.getToursRate(dayType);
					if (rate == null || rate.equals(BigDecimal.ZERO)) {
						MsgUtils.addFacesMessage("Timecard.HTG.TouringRateMissing", FacesMessage.SEVERITY_ERROR, dayType.toString());
						bRet = false;
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return bRet;
	}

	/**
	 * Determine the default number of work hours in a day for an "exempt"
	 * employee. This is for employees who can check "Worked" for a day instead
	 * of entering hours. This is a constant (12) for all union positions, but
	 * calculated as "guaranteedHours / 5" for non-union personnel, or the
	 * constant 8 (for non-union) if guaranteed hours aren't specified on the
	 * timecard.
	 *
	 * @param wtc The timecard of the employee.
	 * @return The daily number of hours to report for this exempt employee if
	 *         they just check the "Worked" check-box.
	 */
	public static BigDecimal calculateExemptHours(WeeklyTimecard wtc) {
		BigDecimal exemptHours = Constants.WORKED_HOURS_UNION; // default for Union personnel
		if (wtc.getAllowWorked()) { // value is only used for exempt workers, skip lookup for others.
			if (wtc.getGuarHours() != null) { // no point in trying this if don't have guaranteed hours as basis
				BigDecimal hours = calculateNonUnionGuarHours(wtc);
				if (hours != null) {
					exemptHours = hours;
				}
			}
			else if (wtc.isNonUnion()) {
				exemptHours = Constants.WORKED_HOURS_NON_UNION;
			}
		}
		return exemptHours;
	}

	/**
	 * Determine the guaranteed number of work hours in a day for a non-union
	 * employee.
	 *
	 * @param wtc The timecard of the employee.
	 * @return The guaranteed number of hours to pay this employee if they are
	 *         non-union. Returns null for union employees, or for non-union
	 *         employees without a guaranteed number of hours specified on their
	 *         timecard.
	 */
	public static BigDecimal calculateNonUnionGuarHours(WeeklyTimecard wtc) {
		BigDecimal hours = null; // default for Union personnel
		if (wtc.getGuarHours() != null) { // no point in trying this if don't have guaranteed hours as basis
			// Only calculate from guaranteed hours for non-union personnel
			if (wtc.isNonUnion()) {
				if (wtc.getEmployeeRateType() == EmployeeRateType.WEEKLY) {
					hours = wtc.getGuarHours().divide(Constants.DECIMAL_FIVE);
				}
				else {
					hours = wtc.getGuarHours();
					if (hours.compareTo(Constants.HOURS_IN_A_DAY) > 0) {
						hours = hours.divide(Constants.DECIMAL_FIVE);
					}
				}
			}
		}
		return hours;
	}

	/**
	 * Validate required city/state values according to day type and country
	 * code. This is ONLY used for Hybrid productions. LS-2161
	 *
	 * @param dailyTime The DailyTime record to be validated.
	 * @return True if the necessary fields are filled in. If one or more
	 *         required fields are null or blank, an error message is issued,
	 *         and false is returned.
	 */
	private static boolean validateDaytypeCityState(DailyTime dailyTime) {
		DayType dayType = dailyTime.getDayType();
		String countryCode = dailyTime.getCountry();
		String state = dailyTime.getState();
		String city = dailyTime.getCity();
		//LS-2313 Hybrid Timecard State/City Validations 2 rule based on spreadsheet
		if (dayType.isUsCityStateRequired()) { // LS-2942
			if (countryCode.equals(Constants.DEFAULT_COUNTRY_CODE) && StringUtils.isEmpty(city)) {
				MsgUtils.addFacesMessage("Timesheet.Error.MissingCity", FacesMessage.SEVERITY_ERROR,
						dayType);
				return false;
			}
			if (StringUtils.isEmpty(state)) {
				MsgUtils.addFacesMessage("Timesheet.Error.MissingState",
						FacesMessage.SEVERITY_ERROR, dayType);
				return false;
			}
		}
		else if (dayType.isCityRequiredStateHM()) {
			if (StringUtils.isEmpty(city)) {
				MsgUtils.addFacesMessage("Timesheet.Error.MissingCity", FacesMessage.SEVERITY_ERROR,
						dayType);
				return false;
			}
			if (StringUtils.isEmpty(state)) { // shouldn't happen as state should be forced to "HM"
				MsgUtils.addFacesMessage("Timesheet.Error.MissingState",
						FacesMessage.SEVERITY_ERROR, dayType);
				return false;
			}
		}
		return true;
	}

	/**
	 * allowModelReleaseFields is used to enabled the checkboxes if ModelRelease WeatherDay / Intimate is Active
	 */
	public static void allowModelReleaseFields(WeeklyTimecard wtc) {
		if (wtc.getStartForm().getModelRelease() != null) {
			for (DailyTime dt : wtc.getDailyTimes()) {
				//LS-4589 enabled checkboxes of DailyTime
				if (dt.getDayType() == DayType.PS || dt.getDayType() == DayType.WD) {
					if (wtc.getStartForm().getModelRelease().getWeatherDay()) {
						dt.setAllowWeather(true);
					}
					else {
						dt.setWeatherDay(false);
					}
					if (wtc.getStartForm().getModelRelease().getIntimatesDay()) {
						dt.setAllowIntimates(true);
					}
					else {
						dt.setIntimatesDay(false);
					}
				}
			}

		}
	}
}
