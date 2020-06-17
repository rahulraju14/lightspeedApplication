/**
 * StartFormUtils.java
 */
package com.lightspeedeps.util.payroll;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.faces.application.FacesMessage;

import com.lightspeedeps.dao.ProjectDAO;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.Validate;
import com.lightspeedeps.type.CompareType;
import com.lightspeedeps.type.EmployeeRateType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.LocationUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.common.StringUtils;

/**
 * Class to hold various static utilities for filling and validating a StartForm.
 */
public class StartFormUtils {

	/**
	 * Private constructor to prevent instantiation; all methods are static.
	 */
	private StartFormUtils() {
	}

	/**
	 * Create the standard "label" that is used to represent a StartForm in
	 * drop-down lists.
	 *
	 * @param sf The StartForm of interest.
	 * @return A non-null String with the "name" of this particular StartForm to
	 *         be displayed to the user.
	 */
	public static String createStartFormLabel(StartForm sf) {
		SimpleDateFormat sdf = new SimpleDateFormat(Constants.START_FORM_DATE_FORMAT);
		return createStartFormLabel(sf, sdf);
	}

	/**
	 * Create the standard "label" that is used to represent a StartForm in
	 * drop-down lists.
	 *
	 * @param sf The StartForm of interest.
	 * @param dateFormat The SimpleDateFormat object to be used to format the date.
	 * @return A non-null String with the "name" of this particular StartForm to
	 *         be displayed to the user.
	 */
	public static String createStartFormLabel(StartForm sf, SimpleDateFormat dateFormat) {
		String label = "";
		if (sf.getProject() != null) {
			Project proj = ProjectDAO.getInstance().refresh(sf.getProject());
			label = proj.getEpisode() + " - ";
		}
		label += dateFormat.format(sf.getWorkStartOrHireDate());
		if (sf.getJobClass() == null) {
			label += " - [no job specified]";
		}
		else {
			label += " - " + sf.getJobClass();
		}
		if (sf.getSequence() != 1) {
			label += " (" + sf.getSequence() + ")";
		}
		return label;
	}

	/**
	 * Apply the start form's default account codes to all the empty fields within the
	 * given set of rates.
	 */
	public static void fillAllowanceAccounts(StartForm startForm) {
		String loc = startForm.getAccountLoc();
		String major = startForm.getAccountMajor();
		String dtl = startForm.getAccountDtl();
		String sub = startForm.getAccountSub();
		String set = startForm.getAccountSet();
		String free = startForm.getAccountFree();
		String free2 = startForm.getAccountFree2();

		fillAccounts(startForm.getBoxRental(), loc, major, dtl, sub, set, free, free2);
		fillAccounts(startForm.getCarAllow(), loc, major, dtl, sub, set, free, free2);
		fillAccounts(startForm.getMealAllow(), loc, major, dtl, sub, set, free, free2);
		fillAccounts(startForm.getMealPenalty(), loc, major, dtl, sub, set, free, free2);
		fillAccounts(startForm.getPerdiemTx(), loc, major, dtl, sub, set, free, free2);
		fillAccounts(startForm.getPerdiemNtx(), loc, major, dtl, sub, set, free, free2);
		fillAccounts(startForm.getPerdiemAdv(), loc, major, dtl, sub, set, null, null);
		fillAccounts(startForm.getMealMoney(), loc, major, dtl, sub, set, free, free2);
		fillAccounts(startForm.getMealMoneyAdv(), loc, major, dtl, sub, set, null, null);
	}

	/**
	 * Apply the start form's default account codes to all the empty fields within the
	 * given set of rates.
	 * @param rates
	 */
	public static void fillRateAccounts(StartForm startForm, StartRateSet rates) {
		String loc = startForm.getAccountLoc();
		String major = startForm.getAccountMajor();
		String dtl = startForm.getAccountDtl();
		String sub = startForm.getAccountSub();
		String set = startForm.getAccountSet();
		String free = startForm.getAccountFree();
		String free2 = startForm.getAccountFree2();

		fillAccounts(rates.getHourly(), loc, major, dtl, sub, set, free, free2);
		fillAccounts(rates.getDaily(), loc, major, dtl, sub, set, free, free2);
		fillAccounts(rates.getWeekly(), loc, major, dtl, sub, set, free, free2);
		fillAccounts(rates.getX15(), loc, major, dtl, sub, set, free, free2);
		fillAccounts(rates.getX20(), loc, major, dtl, sub, set, free, free2);
		fillAccounts(rates.getDay6(), loc, major, dtl, sub, set, free, free2);
		fillAccounts(rates.getDay7(), loc, major, dtl, sub, set, free, free2);
		fillAccounts(rates.getIdleDay6(), loc, major, dtl, sub, set, free, free2);
		fillAccounts(rates.getIdleDay7(), loc, major, dtl, sub, set, free, free2);
	}

	/**
	 * Fill in the rates in the given StartRateSet (hourly, daily, weekly, 6th,
	 * ...) from the given PayRate instance. The StartRateSet is the set of all
	 * rates corresponding either to Prep time or Shooting time. We do not fill
	 * in any information for Idle 6th or 7th.
	 *
	 * @param rateSet The StartRateSet from the StartForm (either Prep or
	 *            Shooting) to be filled in.
	 * @param payRates The pair (Studio,Distant) of PayRate objects with the
	 *            applicable rates to be applied to the StartForm.
	 */
	public static boolean fillRateSet(StartForm startForm, StartRateSet rateSet, PayRate[] payRates) {

		boolean ratesFilled = false;
		if ((payRates[0] != null && payRates[0].getHasRate()) ||
				(payRates[1] != null && payRates[1].getHasRate())) {

			ratesFilled = true;
			// First clear all existing values (hours and rates) in the rateSet
			rateSet.clearRates();

			if (payRates[0] != null) {
				rateSet.getHourly().setStudio(payRates[0].getHourlyRate());
				rateSet.getDaily().setStudio(payRates[0].getDailyRate());
				rateSet.getWeekly().setStudio(payRates[0].getWeeklyRate());

				rateSet.getHourly().setStudioHrs(payRates[0].getGuarHours());
				rateSet.getDaily().setStudioHrs(payRates[0].getGuarHours());
				rateSet.getWeekly().setStudioHrs(payRates[0].getGuarHours());
			}

			if (payRates[1] != null) {
				rateSet.getHourly().setLoc(payRates[1].getHourlyRate());
				rateSet.getDaily().setLoc(payRates[1].getDailyRate());
				rateSet.getWeekly().setLoc(payRates[1].getWeeklyRate());

				rateSet.getHourly().setLocHrs(payRates[1].getGuarHours());
				rateSet.getDaily().setLocHrs(payRates[1].getGuarHours());
				rateSet.getWeekly().setLocHrs(payRates[1].getGuarHours());
			}

			if ( startForm.getRateType() != EmployeeRateType.DAILY) {
				// for Daily employees, do NOT fill in Day6 or Day7 rates/hours

				if (payRates[0] != null) {
					// Fill in 6/7 for Studio: 6=1.5x, 7=2x
					if (! payRates[0].getWeekly()) {
						// also skip setting 6/7 for Weekly(On-call) types
						if (payRates[0].getHourlyRate() != null) {
							rateSet.getDay6().setStudio(payRates[0].getHourlyRate().multiply(Constants.DECIMAL_ONE_FIVE));
							rateSet.getDay7().setStudio(payRates[0].getHourlyRate().multiply(Constants.DECIMAL_TWO));
							if (payRates[0].getGuarHours().compareTo(Constants.HOURS_IN_A_DAY) < 0) {
								rateSet.getDay6().setStudioHrs(payRates[0].getGuarHours());
								rateSet.getDay7().setStudioHrs(payRates[0].getGuarHours());
							}
						}
					}
				}

				if (payRates[1] != null) {
					// Fill in 6/7 for Distant (Location): 6=1x, 7=2x
					if (! payRates[1].getWeekly()) {
						// also skip setting 6/7 for Weekly(On-call) types
						if (payRates[1].getHourlyRate() != null) {
							rateSet.getDay6().setLoc(payRates[1].getHourlyRate());
							rateSet.getDay7().setLoc(payRates[1].getHourlyRate().multiply(Constants.DECIMAL_TWO));
							if (payRates[1].getGuarHours().compareTo(Constants.HOURS_IN_A_DAY) < 0) {
								rateSet.getDay6().setLocHrs(payRates[1].getGuarHours());
								rateSet.getDay7().setLocHrs(payRates[1].getGuarHours());
							}
						}
					}
				}
			}
		}
		return ratesFilled;
	}

	/**
	 * Propagate default account codes into a single line item in the Rates/Accounts
	 * table of the StartForm.
	 * @param line The rate or allowance line item whose account codes are to be updated.
	 *
	 * @param defLoc the default Loc account code
	 * @param defMajor the default Major account code
	 * @param defDtl the default detail account code
	 * @param defSet the default Set account code
	 * @param defFree the default Free account code
	 * @param defFree2 the default second Free account code
	 */
	private static void fillAccounts(AccountCodes line, String defLoc, String defMajor, String defDtl, String defSub, String defSet, String defFree, String defFree2) {
		if (StringUtils.isEmpty(line.getAloc())) 		line.setAloc(defLoc);
		if (StringUtils.isEmpty(line.getMajor())) 		line.setMajor(defMajor);
		if (StringUtils.isEmpty(line.getDtl())) 		line.setDtl(defDtl);
		if (StringUtils.isEmpty(line.getSub())) 		line.setSub(defSub);
		if (StringUtils.isEmpty(line.getSet())) 		line.setSet(defSet);
		if (StringUtils.isEmpty(line.getFree())) 		line.setFree(defFree);
		if (StringUtils.isEmpty(line.getFree2())) 		line.setFree2(defFree2);
	}

	/**
	 * Validate all the fields in the StartForm. Checks for required fields, and
	 * valid ranges of values.
	 * @param isUnion True iff this StartForm is for a union position.
	 *
	 * @return True iff all required fields are filled in, and all filled in
	 *         fields are within the range of allowed values. If false is
	 *         returned, a FacesMessage has already been added describing the
	 *         error.
	 */
	public static boolean validateStartForm(StartForm startForm, boolean isUnion) {
		boolean bRet = true;
		// Note: no validation is done of "effective dates" vs "work start/end/hire" dates

		if (startForm.getCreationDate() == null) {
			MsgUtils.addFacesMessage("StartForm.CreationDateMissing", FacesMessage.SEVERITY_ERROR);
			bRet = false;
		}
		if (startForm.getHireDate() == null) {
			MsgUtils.addFacesMessage("StartForm.HireDateMissing", FacesMessage.SEVERITY_ERROR);
			bRet = false;
		}
		Date workStart = startForm.getWorkStartDate();
//		if (workStart == null) {
//			MsgUtils.addFacesMessage("StartForm.WorkStartDateMissing", FacesMessage.SEVERITY_ERROR);
//			bRet = false;
//		}
		if (bRet) {
			if (startForm.getCreationDate().before(Constants.EARLIEST_PAYROLL_DATE) ||
					startForm.getHireDate().before(Constants.EARLIEST_PAYROLL_DATE) ||
					(workStart != null && workStart.before(Constants.EARLIEST_PAYROLL_DATE)) ||
					startForm.getEffectiveStartDate() != null && startForm.getEffectiveStartDate().before(Constants.EARLIEST_PAYROLL_DATE) ||
					startForm.getEffectiveEndDate() != null && startForm.getEffectiveEndDate().before(Constants.EARLIEST_PAYROLL_DATE)
					) {
				MsgUtils.addFacesMessage("StartForm.DateTooEarly", FacesMessage.SEVERITY_ERROR);
				bRet = false;
			}
		}
		if (startForm.getFormType().equals(StartForm.FORM_TYPE_CHANGE) && startForm.getEffectiveStartDate() == null) {
			MsgUtils.addFacesMessage("StartForm.EffectiveStartMissing", FacesMessage.SEVERITY_ERROR);
			bRet = false;
		}
		if (startForm.getAllowWorked() && startForm.getRateType() == EmployeeRateType.HOURLY) {
			MsgUtils.addFacesMessage("StartForm.HourlyCantBeExempt", FacesMessage.SEVERITY_ERROR);
			bRet = false;
		}

		if (bRet) {
			if (startForm.getWorkEndDate() != null && (workStart == null || startForm.getWorkEndDate().before(workStart))) {
				// Work End date must be on or after start date
				MsgUtils.addFacesMessage("StartForm.WorkStartEnd", FacesMessage.SEVERITY_ERROR);
				bRet = false;
			}
			if (workStart != null && workStart.before(startForm.getHireDate())) {
				// start date may not be prior to hire date (start date must be on or after hire date)
				MsgUtils.addFacesMessage("StartForm.WorkStartHire", FacesMessage.SEVERITY_ERROR);
				bRet = false;
			}
			if (startForm.getEffectiveEndDate() != null && startForm.getEffectiveStartDate() != null &&
					startForm.getEffectiveEndDate().before(startForm.getEffectiveStartDate())) {
				// Effective End date must be on or after Effective start date
				MsgUtils.addFacesMessage("StartForm.EffectiveStartEnd", FacesMessage.SEVERITY_ERROR);
				bRet = false;
			}
			if (startForm.getWorkEndDate() != null && startForm.getEffectiveStartDate() != null &&
					startForm.getWorkEndDate().before(startForm.getEffectiveStartDate())) {
				// Effective from (start) date must be on or before Work End date
				MsgUtils.addFacesMessage("StartForm.WorkEndEffectiveFrom", FacesMessage.SEVERITY_ERROR);
				bRet = false;
			}
			if (startForm.getWorkStartDate() != null && startForm.getEffectiveStartDate() != null &&
					startForm.getEffectiveStartDate().before(startForm.getWorkStartDate())) {
				// Effective from (start) date must be on or after Work Start date
				MsgUtils.addFacesMessage("StartForm.WorkStartEffectiveFrom", FacesMessage.SEVERITY_ERROR);
				bRet = false;
			}
			if (startForm.getWorkEndDate() != null && startForm.getEffectiveEndDate() != null &&
					startForm.getWorkEndDate().before(startForm.getEffectiveEndDate())) {
				// Effective to (end) date must be on or before Work End date
				MsgUtils.addFacesMessage("StartForm.WorkEndEffectiveEnd", FacesMessage.SEVERITY_ERROR);
				bRet = false;
			}
			if (startForm.getWorkStartDate() != null && startForm.getEffectiveEndDate() != null &&
					startForm.getEffectiveEndDate().before(startForm.getWorkStartDate())) {
				// Effective to (End) date must be on or after Work Start date
				MsgUtils.addFacesMessage("StartForm.WorkStartEffectiveEnd", FacesMessage.SEVERITY_ERROR);
				bRet = false;
			}
		}

		if (bRet && startForm.getLoanOutAddress() != null && (! startForm.getLoanOutAddress().isZipValidOrEmpty())) {
			MsgUtils.addFacesMessage("Form.Address.LoanOutZipCode", FacesMessage.SEVERITY_ERROR);
			bRet = false;
		}

		if (bRet && startForm.getDateOfBirth() != null) {
			Date today = CalendarUtils.todaysDate();
			if (today.before(startForm.getDateOfBirth())) {
				// birth date may not be in the future
				MsgUtils.addFacesMessage("FormI9Bean.ValidationMessage.Birthdate", FacesMessage.SEVERITY_ERROR);
				bRet = false;
			}
			else {
				Calendar cal = CalendarUtils.getInstance(today);
				cal.add(Calendar.YEAR, -120);
				if (startForm.getDateOfBirth().before(cal.getTime())) {
					// birth date may not be more than 120 years in the past
					MsgUtils.addFacesMessage("FormI9Bean.ValidationMessage.Birthdate", FacesMessage.SEVERITY_ERROR);
					bRet = false;
				}
			}
		}

		/*if (bRet && startForm.getMailingAddress() != null && (! startForm.getMailingAddress().isZipValidOrEmpty())) {
			MsgUtils.addFacesMessage("Form.Address.MailingZipCode", FacesMessage.SEVERITY_ERROR);
			bRet = false;
		}
		if (bRet && startForm.getPermAddress() != null && (! startForm.getPermAddress().isZipValidOrEmpty())) {
			MsgUtils.addFacesMessage("Form.Address.PermanentZipCode", FacesMessage.SEVERITY_ERROR);
			bRet = false;
		}
		if (bRet && startForm.getAgencyAddress() != null && (! startForm.getAgencyAddress().isZipValidOrEmpty())) {
			MsgUtils.addFacesMessage("Form.Address.AgencyZipCode", FacesMessage.SEVERITY_ERROR);
			bRet = false;
		}*/

		// For Hourly employees, clear out unused Daily & Weekly rates - they can cause errors during validation
		if (startForm.getRateType() == EmployeeRateType.HOURLY) {
			startForm.getProd().getDaily().clearRates();
			startForm.getProd().getWeekly().clearRates();
			if (startForm.getPrep() != null) {
				startForm.getPrep().getDaily().clearRates();
				startForm.getPrep().getWeekly().clearRates();
			}
		}
		else { // Not hourly, so exempt - either daily or weekly
			if (startForm.getRateType() == EmployeeRateType.DAILY) {
				// For Daily employees, clear out unused weekly rates
				startForm.getProd().getWeekly().clearRates();
				if (startForm.getPrep() != null) {
					startForm.getPrep().getWeekly().clearRates();
				}
			}
			else {
				// For weekly employees, clear out unused daily rates
				startForm.getProd().getDaily().clearRates();
				if (startForm.getPrep() != null) {
					startForm.getPrep().getDaily().clearRates();
				}
			}
			if (! isUnion) {
				// exempt, non-union: clear hourly rates and Overtime Table rates. LS-2567
				startForm.getProd().getHourly().clearRates();
				startForm.getProd().clearOtRates();
				if (startForm.getPrep() != null) {
					startForm.getPrep().getHourly().clearRates();
					startForm.getPrep().clearOtRates();
				}
			}
		}

		if (startForm.getProject() != null) { // commercial production
			startForm.getProd().getDay6().clearRates();
			startForm.getProd().getDay7().clearRates();
			if (startForm.getPrep() != null) {
				startForm.getPrep().getDay6().clearRates();
				startForm.getPrep().getDay7().clearRates();
			}
		}

		checkAndClearOT(startForm.getProd());
		if (startForm.getPrep() != null) {
			checkAndClearOT(startForm.getPrep());
		}

		if (startForm.getPrep() != null) {
			if (bRet && (! checkOrderOfOT(startForm.getPrep(), false))) {
				MsgUtils.addFacesMessage("StartForm.OvertimeWrongOrder", FacesMessage.SEVERITY_ERROR);
				bRet = false;
			}
			if (bRet && (! checkOrderOfOT(startForm.getPrep(), true))) {
				MsgUtils.addFacesMessage("StartForm.OvertimeWrongIncreasingOrder", FacesMessage.SEVERITY_ERROR);
				bRet = false;
			}
		}
		if (startForm.getProd() != null) {
			if (bRet && (! checkOrderOfOT(startForm.getProd(), false))) {
				MsgUtils.addFacesMessage("StartForm.OvertimeWrongOrder", FacesMessage.SEVERITY_ERROR);
				bRet = false;
			}
			if (bRet && (! checkOrderOfOT(startForm.getProd(), true))) {
				MsgUtils.addFacesMessage("StartForm.OvertimeWrongIncreasingOrder", FacesMessage.SEVERITY_ERROR);
				bRet = false;
			}
		}
		//LS-2236 If out of range value given in Agent commission field it should show an error message
		if (startForm.getEmpAgentCommisssion() != null) {
			if (bRet && ((startForm.getEmpAgentCommisssion().compareTo(BigDecimal.ZERO) < 0) ||
					(startForm.getEmpAgentCommisssion().compareTo(Constants.DECIMAL_100) > 0))) {
				MsgUtils.addFacesMessage("StartForm.AgentCommissionOutOfRange", FacesMessage.SEVERITY_ERROR);
				bRet = false;
			}
		}
		// LS-2380 Reuse fields take out of range value and negative value
		if (startForm.getEmpReuse() != null) {
			if (bRet && ((startForm.getEmpReuse().compareTo(BigDecimal.ZERO) < 0) || startForm.getEmpReuse().compareTo(Constants.DECIMAL_1_MILLION) > 0)){
				MsgUtils.addFacesMessage("StartForm.EmpReuseOutOfRange", FacesMessage.SEVERITY_ERROR);
				bRet = false;
			}
		}
		scaleFields(startForm);

		bRet &= validateRates(startForm.getProd());
		if (startForm.getPrep() != null) {
			bRet &= validateRates(startForm.getPrep());
		}

		bRet &= validateAllowances(startForm);

		//LS-1843
		bRet &= validateCityState(startForm);

		return bRet;
	}

	/**
	 * Method to validate city/state for each addresses on PayRoll Start LS-1843
	 *
	 * @param startForm
	 * @return true/false based on the city/state match
	 */
	private static boolean validateCityState(StartForm startForm) {
		boolean bRet = true;

		//LS-2087 Skip work City validation for Payroll start for Tours
		if (! startForm.getContact().getProduction().getType().isTours()) {
			if ((! StringUtils.isEmpty(startForm.getWorkCity())) ||
					(! StringUtils.isEmpty(startForm.getWorkState()))) {
				if (StringUtils.isEmpty(startForm.getWorkCountry()) ||
						startForm.getWorkCountry().equals(Constants.DEFAULT_COUNTRY_CODE)) {
					// LS-2341 Only do work city/state check if country is blank or US
					boolean match = LocationUtils.validateCityState(startForm.getWorkCity(),
							startForm.getWorkState());
					if (! match) {
						MsgUtils.addFacesMessage("Address.CityStateMisMatched",
								FacesMessage.SEVERITY_ERROR, startForm.getWorkCity(),
								startForm.getWorkState());
						bRet = false;
					}
				}
			}
		}

		Address addr = startForm.getMailingAddress();
		if (addr != null) {
			bRet &= LocationUtils.checkCityState(addr);
		}

		addr = startForm.getPermAddress();
		if (addr != null) {
			bRet &= LocationUtils.checkCityState(addr);
		}

		addr = startForm.getLoanOutAddress();
		if (addr != null) {
			bRet &= LocationUtils.checkCityState(addr);
		}

		addr = startForm.getAgencyAddress();
		if (addr != null) {
			bRet &= LocationUtils.checkCityState(addr);
		}

		return bRet;
	}

	/** Checks whether the user has entered the overtime rates properly
	 * in the increasing order from the first line or not.
	 * @param rateSet in-Production (shooting) rates or Pre-Production ("prep") rates.
	 * @param checkIncreasingOrder if true, checks the increasing order of the rates;
	 *  if false, method will check the blank fields from the first line.
	 * @return true if rates are entered properly.
	 */
	private static boolean checkOrderOfOT(StartRateSet rateSet, boolean checkIncreasingOrder) {
		if (! checkIncreasingOrder) {
			// Overtime Row1
			if (rateSet.getOt1AfterHours() == null) {
				if (! (rateSet.getOt2AfterHours() == null && rateSet.getOt3AfterHours() == null)) {
					return false;
				}
			}
			// Overtime Row2
			if (rateSet.getOt2AfterHours() == null) {
				if (rateSet.getOt3AfterHours() != null) {
					return false;
				}
			}
			return true;
		}
		else {
			boolean incrsOrder = true;
			if (rateSet.getOt1AfterHours() != null) {
				if (rateSet.getOt2AfterHours() != null &&
						(rateSet.getOt1AfterHours().compareTo(rateSet.getOt2AfterHours()) >= 0)) {
					incrsOrder = false;
				}
				if (incrsOrder && rateSet.getOt3AfterHours() != null &&
						(rateSet.getOt1AfterHours().compareTo(rateSet.getOt3AfterHours()) >= 0)) {
					incrsOrder = false;
				}
			}
			if (incrsOrder && rateSet.getOt2AfterHours() != null) {
				if (incrsOrder && rateSet.getOt3AfterHours() != null &&
						(rateSet.getOt2AfterHours().compareTo(rateSet.getOt3AfterHours()) >= 0)) {
					incrsOrder = false;
				}
			}
			return incrsOrder;
		}
	}

	/**
	 * Clear sections of the OT rate table if values are missing or invalid.
	 *
	 * @param prod The rate-set to process.
	 */
	private static void checkAndClearOT(StartRateSet prod) {
		if (prod.getOt1AfterHours() == null || prod.getOt1AfterHours().signum() <= 0 ||
				prod.getOt1Rate() == null || prod.getOt1Rate().signum() <= 0 ||
				prod.getOt1Multiplier() == null) {
			prod.setOt1AfterHours(null);
			prod.setOt1Rate(null);
			prod.setOt1Multiplier(null);
		}
		if (prod.getOt2AfterHours() == null || prod.getOt2AfterHours().signum() <= 0 ||
				prod.getOt2Rate() == null || prod.getOt2Rate().signum() <= 0 ||
				prod.getOt2Multiplier() == null) {
			prod.setOt2AfterHours(null);
			prod.setOt2Rate(null);
			prod.setOt2Multiplier(null);
		}
		if (prod.getOt3AfterHours() == null || prod.getOt3AfterHours().signum() <= 0 ||
				prod.getOt3Rate() == null || prod.getOt3Rate().signum() <= 0 ||
				prod.getOt3Multiplier() == null) {
			prod.setOt3AfterHours(null);
			prod.setOt3Rate(null);
			prod.setOt3Multiplier(null);
		}
	}

	/**
	 * Set the OT rate table multipliers based on the rates entered.
	 */
	public static void calculateOvertimeMultipliers(StartForm startForm) {
		calculateOvertimeMultipliers(startForm.getProd());
		if (startForm.getPrep() != null) {
			calculateOvertimeMultipliers(startForm.getPrep());
		}
	}

	/**
	 * Set the OT rate table multipliers based on the rates entered.
	 *
	 * @param rate The StartRateSet to process.
	 */
	private static void calculateOvertimeMultipliers(StartRateSet rate) {
		if (rate.getOt1AfterHours() != null && rate.getOt1AfterHours().signum() > 0 &&
				rate.getHourly().getStudio() != null && rate.getHourly().getStudio().signum() > 0 &&
				rate.getOt1Rate() != null && rate.getOt1Rate().signum() > 0) {
			BigDecimal mult = rate.getOt1Rate().divide(rate.getHourly().getStudio(), 5, RoundingMode.HALF_UP);
			rate.setOt1Multiplier(NumberUtils.scaleTo(mult, 1, 6));
		}
		else {
			rate.setOt1Multiplier(null);
		}
		if (rate.getOt2AfterHours() != null && rate.getOt2AfterHours().signum() > 0 &&
				rate.getHourly().getStudio() != null && rate.getHourly().getStudio().signum() > 0 &&
				rate.getOt2Rate() != null && rate.getOt2Rate().signum() > 0) {
			BigDecimal mult = rate.getOt2Rate().divide(rate.getHourly().getStudio(), 5, RoundingMode.HALF_UP);
			rate.setOt2Multiplier(NumberUtils.scaleTo(mult, 1, 6));
		}
		else {
			rate.setOt2Multiplier(null);
		}
		if (rate.getOt3AfterHours() != null && rate.getOt3AfterHours().signum() > 0 &&
				rate.getHourly().getStudio() != null && rate.getHourly().getStudio().signum() > 0 &&
				rate.getOt3Rate() != null && rate.getOt3Rate().signum() > 0) {
			BigDecimal mult = rate.getOt3Rate().divide(rate.getHourly().getStudio(), 5, RoundingMode.HALF_UP);
			rate.setOt3Multiplier(NumberUtils.scaleTo(mult, 1, 6));
		}
		else {
			rate.setOt3Multiplier(null);
		}
	}

	/**
	 * Validate the allowances information for rates within appropriate ranges.
	 *
	 * @return True iff all fields passed validation. If any fields did not
	 *         pass, appropriate Faces messages will have been issued.
	 */
	private static boolean validateAllowances(StartForm startForm) {
		List<Validate> validateFields = new ArrayList<>();

		addValidation(startForm.getBoxRental(), validateFields);
		addValidation(startForm.getCarAllow(), validateFields);
		addValidation(startForm.getMealAllow(), validateFields);
		addValidation(startForm.getMealMoney(), validateFields);
		addAdvanceValidation(startForm.getMealMoneyAdv().getLoc(), validateFields, Constants.DECIMAL_100K, "StartForm.BoxOrCarOver100K");
		addAdvanceValidation(startForm.getMealMoneyAdv().getStudio(), validateFields, Constants.DECIMAL_100K, "StartForm.BoxOrCarOver100K");

		addRateValidation(startForm.getBoxRental().getPaymentCap(), validateFields, Constants.DECIMAL_100K, "StartForm.BoxOrCarOver100K");
		addRateValidation(startForm.getCarAllow().getPaymentCap(), validateFields, Constants.DECIMAL_100K, "StartForm.BoxOrCarOver100K");

		addRateValidation(startForm.getPerdiemTx().getAmt(), validateFields, Constants.DECIMAL_100K, "StartForm.BoxOrCarOver100K");
		addRateValidation(startForm.getPerdiemNtx().getAmt(), validateFields, Constants.DECIMAL_100K, "StartForm.BoxOrCarOver100K");

		addAdvanceValidation(startForm.getPerdiemAdv().getAmt(), validateFields, Constants.DECIMAL_100K, "StartForm.BoxOrCarOver100K");

		return ValidateFields.validateFields(validateFields);
	}

	/**
	 * Validate the "Rate" table entries, including both the hours and the
	 * actual rate values.
	 *
	 * @param rates The StartRateSet to be validated (which will correspond to
	 *            either the production or prep rates).
	 * @return True iff all fields passed validation. If any fields did not
	 *         pass, appropriate Faces messages will have been issued.
	 */
	private static boolean validateRates(StartRateSet rates) {

		List<Validate> validateFields = new ArrayList<>();

		// guaranteed hours validations
		addHoursValidation(rates.getHourly(), validateFields, Constants.MAX_WEEKLY_GUAR_HOURS);
		addHoursValidation(rates.getDaily(), validateFields, Constants.MAX_WEEKLY_GUAR_HOURS);
		addHoursValidation(rates.getWeekly(), validateFields, Constants.MAX_WEEKLY_GUAR_HOURS);
		addHoursValidation(rates.getX15(), validateFields);
		addHoursValidation(rates.getX20(), validateFields);
		addHoursValidation(rates.getDay6(), validateFields, Constants.HOURS_IN_A_DAY);
		addHoursValidation(rates.getDay7(), validateFields, Constants.HOURS_IN_A_DAY);

		addHoursValidation(rates.getIdleDay6().getLocHrs(), validateFields, Constants.HOURS_IN_A_DAY);
		addHoursValidation(rates.getIdleDay7().getLocHrs(), validateFields, Constants.HOURS_IN_A_DAY);

		addHoursValidation(rates.getOt1AfterHours(), validateFields, Constants.MAX_WEEKLY_GUAR_HOURS);
		addHoursValidation(rates.getOt2AfterHours(), validateFields, Constants.MAX_WEEKLY_GUAR_HOURS);
		addHoursValidation(rates.getOt3AfterHours(), validateFields, Constants.MAX_WEEKLY_GUAR_HOURS);

		// Rate validations
		addRateValidation(rates.getHourly(), validateFields, Constants.DECIMAL_100K, "StartForm.RateOver100K");
		addRateValidation(rates.getDaily(), validateFields, Constants.DECIMAL_100K, "StartForm.RateOver100K");
		addRateValidation(rates.getWeekly(), validateFields, Constants.DECIMAL_1_MILLION, "StartForm.RateOver1Million");
		addRateValidation(rates.getDay6(), validateFields, Constants.DECIMAL_100K, "StartForm.RateOver100K");
		addRateValidation(rates.getDay7(), validateFields, Constants.DECIMAL_100K, "StartForm.RateOver100K");

		addRateValidation(rates.getIdleDay6().getLoc(), validateFields, Constants.DECIMAL_100K, "StartForm.RateOver100K");
		addRateValidation(rates.getIdleDay7().getLoc(), validateFields, Constants.DECIMAL_100K, "StartForm.RateOver100K");

		addRateValidation(rates.getOt1Rate(), validateFields, Constants.DECIMAL_100K, "StartForm.RateOver100K");
		addRateValidation(rates.getOt2Rate(), validateFields, Constants.DECIMAL_100K, "StartForm.RateOver100K");
		addRateValidation(rates.getOt3Rate(), validateFields, Constants.DECIMAL_100K, "StartForm.RateOver100K");

		return ValidateFields.validateFields(validateFields);
	}

	/**
	 * Scales all the decimal values in the StartForm to the appropriate number
	 * of decimal places.
	 */
	private static void scaleFields(StartForm startForm) {
		StartRateSet rates = startForm.getProd();

		scaleRateRow(rates.getHourly());
		scaleRateRow(rates.getDaily());
		scaleRateRow(rates.getWeekly());
		scaleRateRow(rates.getDay6());
		scaleRateRow(rates.getDay7());

		rates.getX15().setLocHrs(NumberUtils.scaleTo2Places(rates.getX15().getLocHrs()));
		rates.getX15().setStudioHrs(NumberUtils.scaleTo2Places(rates.getX15().getStudioHrs()));
		rates.getX20().setLocHrs(NumberUtils.scaleTo2Places(rates.getX20().getLocHrs()));
		rates.getX20().setStudioHrs(NumberUtils.scaleTo2Places(rates.getX20().getStudioHrs()));

		rates.getIdleDay6().setLocHrs(NumberUtils.scaleTo2Places(rates.getIdleDay6().getLocHrs()));
		rates.getIdleDay7().setLocHrs(NumberUtils.scaleTo2Places(rates.getIdleDay7().getLocHrs()));

		rates.getIdleDay6().setLoc(NumberUtils.scaleHourlyRate(rates.getIdleDay6().getLoc()));
		rates.getIdleDay7().setLoc(NumberUtils.scaleHourlyRate(rates.getIdleDay7().getLoc()));

		startForm.getBoxRental().setLoc(NumberUtils.scaleTo2Places(startForm.getBoxRental().getLoc()));
		startForm.getBoxRental().setStudio(NumberUtils.scaleTo2Places(startForm.getBoxRental().getStudio()));

		startForm.getCarAllow().setLoc(NumberUtils.scaleTo2Places(startForm.getCarAllow().getLoc()));
		startForm.getCarAllow().setStudio(NumberUtils.scaleTo2Places(startForm.getCarAllow().getStudio()));
	}

	/**
	 * Scale the hours and rates in one Rate row of the Start Form to the
	 * appropriate number of decimal places.
	 *
	 * @param line The rate line item whose fields are to be scaled.
	 */
	private static void scaleRateRow(RateHoursGroup line) {
		line.setLocHrs(NumberUtils.scaleTo2Places(line.getLocHrs()));
		line.setStudioHrs(NumberUtils.scaleTo2Places(line.getStudioHrs()));

		line.setLoc(NumberUtils.scaleHourlyRate(line.getLoc()));
		line.setStudio(NumberUtils.scaleHourlyRate(line.getStudio()));
	}

	/**
	 * Add validation entries (non-negative and less than max) for location and
	 * studio guaranteed hours for the given rate line item.
	 *
	 * @param line The rate line item to check.
	 * @param validateFields The List of validations to which the new
	 *            validations will be added.
	 * @param maxHours
	 */
	private static void addHoursValidation(RateHoursGroup line, List<Validate> validateFields, BigDecimal maxHours) {
		addHoursValidation(line.getLocHrs(), validateFields, maxHours);
		addHoursValidation(line.getStudioHrs(), validateFields, maxHours);
	}

	/**
	 * Add validation entries (non-negative and less than max) for location and
	 * studio guaranteed hours for the given rate line item.
	 *
	 * @param line The rate line item to check.
	 * @param validateFields The List of validations to which the new
	 *            validations will be added.
	 */
	private static void addHoursValidation(HoursGroup line, List<Validate> validateFields) {
		addHoursValidation(line.getLocHrs(), validateFields, Constants.MAX_WEEKLY_GUAR_HOURS);
		addHoursValidation(line.getStudioHrs(), validateFields, Constants.MAX_WEEKLY_GUAR_HOURS);
	}

	/**
	 * Add validation entries (non-negative and less than max) for a guaranteed
	 * hours field.
	 *
	 * @param hours The hours amount to check.
	 * @param validateFields The List of validations to which the new
	 *            validations will be added.
	 */
	private static void addHoursValidation(BigDecimal hours, List<Validate> validateFields, BigDecimal maxHours) {
		validateFields.add(new Validate(hours, CompareType.GREATER_THAN_EQUAL, "StartForm.NegativeHours", ""));
		validateFields.add(new Validate(hours, CompareType.LESS_THAN, maxHours, "StartForm.HoursOver100"));
	}

	/**
	 * Add validation entries (non-negative and less than max) for location and
	 * studio rates for the given rate line item.
	 *
	 * @param line The rate line item to be checked.
	 * @param validateFields The List of validations to which the new
	 *            validations will be added.
	 * @param maximum The maximum value allowed for the rates in this line item.
	 * @param maxMessage The message id to be used in the validation entry.
	 */
	private static void addRateValidation(RateGroup line, List<Validate> validateFields, BigDecimal maximum, String maxMessage) {
		addRateValidation(line.getLoc(), validateFields, maximum, maxMessage);
		addRateValidation(line.getStudio(), validateFields, maximum, maxMessage);
	}

	/**
	 * Add validation entries (non-negative and less than max) for location and
	 * studio rates for the given allowance line item.
	 *
	 * @param allowance The Allowance line item to check.
	 * @param validateFields The List of validations to which the new
	 *            validations will be added.
	 */
	private static void addValidation(RateGroup allowance, List<Validate> validateFields) {
		addRateValidation(allowance.getLoc(), validateFields, Constants.DECIMAL_100K, "StartForm.BoxOrCarOver100K");
		addRateValidation(allowance.getStudio(), validateFields, Constants.DECIMAL_100K, "StartForm.BoxOrCarOver100K");
	}

	/**
	 * Add validation entries (non-negative and less than max) for the given
	 * rate.
	 *
	 * @param rate The rate to be checked.
	 * @param validateFields The List of validations to which the new
	 *            validations will be added.
	 * @param maximum The maximum value allowed for the rate.
	 * @param maxMessage The message id to be used in the validation entry.
	 */
	private static void addRateValidation(BigDecimal rate, List<Validate> validateFields, BigDecimal maximum, String maxMessage) {
		validateFields.add(new Validate(rate, CompareType.GREATER_THAN_EQUAL, "StartForm.NegativeRate", ""));
		validateFields.add(new Validate(rate, CompareType.LESS_THAN, maximum, maxMessage));
	}

	/**
	 * Add validation entries (less than max and greater than -max) for the given
	 * advance fields.  This differs from "rate" validations because the values may
	 * be negative.
	 *
	 * @param rate The rate to be checked.
	 * @param validateFields The List of validations to which the new
	 *            validations will be added.
	 * @param maximum The maximum value allowed for the rate.
	 * @param maxMessage The message id to be used in the validation entry.
	 */
	private static void addAdvanceValidation(BigDecimal rate, List<Validate> validateFields, BigDecimal maximum, String maxMessage) {
		validateFields.add(new Validate(rate, CompareType.LESS_THAN, maximum, maxMessage));
		validateFields.add(new Validate(rate, CompareType.GREATER_THAN, maximum.negate(), maxMessage));
	}

	/**
	 * Trim leading & trailing blanks from most of the user-entered string
	 * fields in a StartForm.
	 *
	 * @param startForm The StartForm to be updated.
	 */
	public static void trimFields(StartForm startForm) {
		if (startForm.getFirstName() != null) {
			startForm.setFirstName(startForm.getFirstName().trim());
		}
		if (startForm.getLastName() != null) {
			startForm.setLastName(startForm.getLastName().trim());
		}
		if (startForm.getProdCompany() != null) {
			startForm.setProdCompany(startForm.getProdCompany().trim());
		}
		if (startForm.getProdTitle() != null) {
			startForm.setProdTitle(startForm.getProdTitle().trim());
		}
		if (startForm.getWorkCity() != null) {
			startForm.setWorkCity(startForm.getWorkCity().trim());
		}
		if (startForm.getWorkState() != null) {
			startForm.setWorkState(startForm.getWorkState().trim());
		}
		if (startForm.getStateOfResidence() != null) {
			startForm.setStateOfResidence(startForm.getStateOfResidence().trim());
		}
		if (startForm.getJobClass() != null) {
			startForm.setJobClass(startForm.getJobClass().trim());
		}
		if (startForm.getSocialSecurity() != null) {
			startForm.setSocialSecurity(startForm.getSocialSecurity().trim());
		}
	}

}
