/**
 * File: StartFormService.java
 */
package com.lightspeedeps.service;

import java.math.*;
import java.util.*;

import javax.faces.model.SelectItem;

import org.apache.commons.logging.*;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.*;


/**
 * Contains methods with application logic related to StartForm management.
 */
public class StartFormService extends BaseService {
	private static final Log log = LogFactory.getLog(StartFormService.class);

	/** Regular time = 8 hours. */
	public static final BigDecimal REGULAR_RATE_HOURS = new BigDecimal(8);
	/** Max allowed "Day Rate" hours = 16. */
	public static final BigDecimal MAX_DAY_RATE_HOURS = new BigDecimal(16);
	/** CA double-time starts after 12 hours. */
	public static final BigDecimal CA_DOUBLE_TIME_HOURS = new BigDecimal(12);
	/** Equivalent straight-pay hours for time before CA double-time = ((12-8)*1.5)+8 = 14 */
	public static final BigDecimal CA_PAY_HOURS = CA_DOUBLE_TIME_HOURS.subtract(REGULAR_RATE_HOURS)
										.multiply(Constants.DECIMAL_ONE_FIVE).add(REGULAR_RATE_HOURS);

	/** The list of retirement plans to choose from; loaded from
	 * the SelectionItem table. */
	private static List<SelectItem> retirementPlanDL;

	private StartFormService() {
//		log.debug("");
	}

//	public static StartFormService getInstance() {
//		return (StartFormService)getInstance("StartFormService");
//	}

	/**
	 * Calculate the variously hourly rates based on a daily amount for a given
	 * number of hours. This technique is common in Commercial productions.
	 *
	 * @param sf The StartForm to be updated.
	 * @param amount The total daily pay.
	 * @param hours The number of hours that must be worked to earn the given
	 *            amount. This will also be set as the guaranteed hours.
	 * @param prepOnly
	 * @param caLaw Use California law. This generates a double-time entry.
	 */
	public static void calculateDayRate(StartForm sf, BigDecimal amount, BigDecimal hours,
			boolean prepOnly, boolean caLaw) {
		// LS-2294
		Project project = sf.getProject();
		project = ProjectDAO.getInstance().refresh(project);
		boolean usePremiumRate = project.getPayrollPref().getUsePremiumRate();
		StartRateSet rs = sf.getProd();
		if (prepOnly) {
			rs = sf.getPrep();
		}

		// set guaranteed hours if not CA
		if (! caLaw) {
			rs.getHourly().setStudioHrs(hours);
		}

		BigDecimal payHrs; // # of straight-rate hours equivalent to the worked hours times rate(s)
		if (caLaw && (hours.compareTo(CA_DOUBLE_TIME_HOURS) > 0)) {
			payHrs = hours.subtract(CA_DOUBLE_TIME_HOURS); // hours of double-time
			payHrs = payHrs.multiply(Constants.DECIMAL_TWO); // Pay hours for double-time
			payHrs = payHrs.add(CA_PAY_HOURS); // total pay hours (regular + OT + double-time)
		}
		else {
			// calc hours for regular plus OT (time-and-a-half)
			payHrs = hours.subtract(REGULAR_RATE_HOURS).multiply(Constants.DECIMAL_ONE_FIVE).add(REGULAR_RATE_HOURS);
		}

		// Comment examples use "$500/10" pay rate. payHrs would = 11 = (8 + ((10-8)*1.5))
		BigDecimal perceived1xRate = amount.divide(hours, 6, RoundingMode.HALF_UP); // e.g., $50/hr
		BigDecimal actualRate = amount.divide(payHrs, 6, RoundingMode.HALF_UP);	//  e.g., $45.45/hr

		rs.getHourly().setStudio(actualRate.setScale(4, RoundingMode.HALF_UP));

		rs.setOt1AfterHours(REGULAR_RATE_HOURS);
		rs.setOt1Multiplier(Constants.DECIMAL_ONE_FIVE);
		rs.setOt1Rate(actualRate.multiply(Constants.DECIMAL_ONE_FIVE)
				.setScale(4, RoundingMode.HALF_UP)); // e.g., $68.18/hr

		BigDecimal perceived15Rate = perceived1xRate.multiply(Constants.DECIMAL_ONE_FIVE); // perceived OT rate, e.g., $75/hr
		BigDecimal multiplier = perceived15Rate.divide(actualRate, 6, RoundingMode.HALF_UP); // "premium" multiplier, e.g., 1.65x

		rs.setOt3AfterHours(null); // assume no gold rate entries for now.
		rs.setOt3Multiplier(null);
		rs.setOt3Rate(null);

		if (hours.compareTo(Constants.DECIMAL_EIGHT) <= 0) { // <= 8 hours? No second OT rate entry necessary
			rs.setOt2AfterHours(null); // clear them out
			rs.setOt2Multiplier(null);
			rs.setOt2Rate(null);
		}
		else if (! caLaw) { // If not CA, then only the OT rate entry (no gold)
			rs.setOt2AfterHours(hours);
			// LS-2294 Update to Day Rate Calculator on Payroll Start
			if (usePremiumRate) {
				rs.setOt2Multiplier(multiplier);
				rs.setOt2Rate(perceived15Rate.setScale(4, RoundingMode.HALF_UP));
			}
			else {
				rs.setOt2Multiplier(Constants.DECIMAL_ONE_FIVE);
				rs.setOt2Rate(actualRate.multiply(Constants.DECIMAL_ONE_FIVE).setScale(4,
						RoundingMode.HALF_DOWN));
			}
		}
		else {
			if (usePremiumRate) {
				// calculate perceived double-time rate
				BigDecimal perceived2xRate = perceived1xRate.multiply(Constants.DECIMAL_TWO); // e.g., $100/hr
				if ((hours.compareTo(CA_DOUBLE_TIME_HOURS) < 0)) { // "guaranteed" hours less than CA gold
					rs.setOt2AfterHours(hours);
					rs.setOt2Multiplier(multiplier);
					rs.setOt2Rate(perceived15Rate.setScale(4, RoundingMode.HALF_UP));
					multiplier = perceived2xRate.divide(actualRate, 6, RoundingMode.HALF_UP); // "premium" multiplier, e.g., 2.2x
					rs.setOt3AfterHours(CA_DOUBLE_TIME_HOURS);
					rs.setOt3Multiplier(multiplier);
					rs.setOt3Rate(perceived2xRate);
				}
				else if ((hours.compareTo(CA_DOUBLE_TIME_HOURS) == 0)) { // "guaranteed" hours = CA gold (12)
					multiplier = perceived2xRate.divide(actualRate, 6, RoundingMode.HALF_UP); // "premium" multiplier, e.g., 2.2x
					rs.setOt2AfterHours(CA_DOUBLE_TIME_HOURS);
					rs.setOt2Multiplier(multiplier);
					rs.setOt2Rate(perceived2xRate);
				}
				else { // guaranteed hours > CA gold
					rs.setOt2AfterHours(CA_DOUBLE_TIME_HOURS);
					rs.setOt2Multiplier(Constants.DECIMAL_TWO);
					rs.setOt2Rate(actualRate.multiply(Constants.DECIMAL_TWO)
							.setScale(4, RoundingMode.HALF_UP)); // e.g., $90.90/hr
					multiplier = perceived2xRate.divide(actualRate, 6, RoundingMode.HALF_UP); // "premium" multiplier, e.g., 2.2x
					rs.setOt3AfterHours(hours);
					rs.setOt3Multiplier(multiplier);
					rs.setOt3Rate(perceived2xRate);
				}
			}
			else {
				//LS-2294 Update to Day Rate Calculator on Payroll Start
				// calculate perceived double-time rate
				BigDecimal perceived2xRate = actualRate.multiply(Constants.DECIMAL_TWO);
				if ((hours.compareTo(CA_DOUBLE_TIME_HOURS) < 0)) { // "guaranteed" hours less than CA gold
					rs.setOt2AfterHours(hours);
					rs.setOt2Multiplier(Constants.DECIMAL_ONE_FIVE);
					rs.setOt2Rate(actualRate.multiply(Constants.DECIMAL_ONE_FIVE).setScale(4,
							RoundingMode.HALF_DOWN));
					rs.setOt3AfterHours(CA_DOUBLE_TIME_HOURS);
					rs.setOt3Multiplier(Constants.DECIMAL_TWO); // "non-premium" multiplier, e.g., 2.0x);
					rs.setOt3Rate(perceived2xRate);
				}
				else if ((hours.compareTo(CA_DOUBLE_TIME_HOURS) == 0)) { // "guaranteed" hours = CA gold (12)
					rs.setOt2AfterHours(CA_DOUBLE_TIME_HOURS);
					rs.setOt2Multiplier(Constants.DECIMAL_TWO);// "non-premium" multiplier, e.g., 2.0x
					rs.setOt2Rate(perceived2xRate);
				}
				else { // guaranteed hours > CA gold
					rs.setOt2AfterHours(CA_DOUBLE_TIME_HOURS);
					rs.setOt2Multiplier(Constants.DECIMAL_TWO);
					rs.setOt2Rate(actualRate.multiply(Constants.DECIMAL_TWO).setScale(4,
							RoundingMode.HALF_UP));
					rs.setOt3AfterHours(hours);
					rs.setOt3Multiplier(Constants.DECIMAL_TWO);
					rs.setOt3Rate(perceived2xRate);
				}
			}
		}
	}

	/**
	 * Creates a "New" StartForm for a user.
	 *
	 * @param contact The new Contact entry.
	 * @param projectMember The database id of the Role object describing the position
	 *            assigned to this Contact.
	 * @return The new StartForm, which has been saved to the database.
	 */
	public static StartForm createFirstStartForm(Contact contact, Employment employment) {
		return createFirstStartForm(contact, employment, SessionUtils.getCurrentProject());
	}

	/**
	 * Creates a "New" StartForm for a user.
	 *
	 * @param contact The Contact entry.
	 * @param employment The Employment object describing the position assigned
	 *            to this Contact.
	 * @param project The Project to be associated with the new StartForm. Only
	 *            used for Commercial productions; ignored for other
	 *            productions.
	 * @return The new StartForm, which has been saved to the database.
	 */
	public static StartForm createFirstStartForm(Contact contact, Employment employment, Project project) {
		Date today = CalendarUtils.todaysDate();
		return createStartForm(StartForm.FORM_TYPE_NEW, null, null, contact,
				employment, project, today, null, null, false, true);
	}


	/**
	 * Creates a "New" StartForm for a user, using additional information from a
	 * completed I9 and/or W4. Used during export/transfer when no existing
	 * StartForm (Payroll Start) is found.
	 *
	 * @param contact The Contact entry.
	 * @param employment The Employment object describing the position assigned
	 *            to this Contact.
	 * @param project The Project to be associated with the new StartForm. Only
	 *            used for Commercial productions; ignored for other
	 *            productions.
	 * @param formI9 The user's federal Form I-9.
	 * @param contact The user's federal Form W-4.
	 * @return The new StartForm, which has NOT been saved to the database.
	 */
	@SuppressWarnings("null")
	public static StartForm createFirstStartForm(Contact contact, Employment employment, Project project, FormI9 formI9, FormW4 formW4) {
		Date today = CalendarUtils.todaysDate();
		StartForm sf = createStartForm(StartForm.FORM_TYPE_NEW, null, null, contact,
				employment, project, today, null, null, false,
				false/* do not save */,
				true/* do not update ER*/);

		// Set defaults for some fields for which we have no data
		sf.setUnionLocalNum(Unions.NON_UNION);	// assume Non-Union, no other data source

		if (formI9 != null || formW4 != null) {
			Address mailAddr = new Address();
			if (formW4 != null) {
				// set additional fields from W4
				sf.setSocialSecurity(formW4.getSocialSecurity());
				if (formW4.getAddress() != null) {
					mailAddr.copyFrom(formW4.getAddress());
				}
			}
			else {
				sf.setSocialSecurity(formI9.getSocialSecurity());
				if (formI9.getAddress() != null) {
					mailAddr.copyFrom(formI9.getAddress());
				}
			}

			Address permAddr = new Address();
			if (formI9 != null) {
				// set additional fields from I9
				if (formI9.getAddress() != null) {
					permAddr.copyFrom(formI9.getAddress());
					sf.setStateOfResidence(formI9.getAddress().getState());
				}
				sf.setDateOfBirth(formI9.getDateOfBirth());
			}
			else {
				if (formW4.getAddress() != null) {
					permAddr.copyFrom(formW4.getAddress());
				}
			}

			sf.setMailingAddress(mailAddr);
			sf.setPermAddress(permAddr);
		}

		return sf;
	}

	/**
	 * Create and initialize a StartForm. This may include copying information
	 * from either a "related" StartForm or the most recent StartForm.
	 *
	 * @param addSdFormType The type (New/Change/Rehire) of StartForm to be
	 *            created.
	 * @param related The related StartForm in the case of a "Change" or
	 *            "Re-Hire" form being created. May be null for "Re-Hire". Not
	 *            used for a "New".
	 * @param latest The most recent StartForm -- this will be used for
	 *            demographic data if 'related' is null, or this is a New form.
	 * @param contact The Contact who will own the new StartForm.
	 * @param employment The Employment related to the position chosen by
	 *            the user when adding the StartForm.
	 * @param project The project associated with this StartForm. Only used for
	 *            Commercial productions.
	 * @param creation The creation date to assign to the new StartForm.
	 * @param effectiveStart The effective start date to assign to the new
	 *            StartForm.
	 * @param effectiveEnd The effective end date to assign to the PRIOR
	 *            StartForm, if it was supplied. (Only applies to Change types.)
	 * @param copyRates Copy rate table information from the 'related' or
	 *            'latest' StartForm into the new StartForm.
	 * @param saveIt If true, save the StartForm, otherwise do NOT save it to the database.
	 * @return The new StartForm, which may have been saved to the database. By
	 *         default, the work start date will be set to today's date. For a
	 *         New or Re-Hire form, the Hire date will also be set to today's
	 *         date. For a Change form, if the Hire date is null it will be set
	 *         to today's date.
	 */
	public static StartForm createStartForm(String addSdFormType, StartForm related, StartForm latest, Contact contact,
			Employment employment, Project project, Date creation, Date effectiveStart, Date effectiveEnd, boolean copyRates, boolean saveIt) {
		return createStartForm(addSdFormType, related, latest, contact, employment, project, creation, effectiveStart, effectiveEnd, copyRates, saveIt, true);
	}

	/**
	 * Create and initialize a StartForm. This may include copying information
	 * from either a "related" StartForm or the most recent StartForm.
	 *
	 * @param addSdFormType The type (New/Change/Rehire) of StartForm to be
	 *            created.
	 * @param related The related StartForm in the case of a "Change" or
	 *            "Re-Hire" form being created. May be null for "Re-Hire". Not
	 *            used for a "New".
	 * @param latest The most recent StartForm -- this will be used for
	 *            demographic data if 'related' is null, or this is a New form.
	 * @param contact The Contact who will own the new StartForm.
	 * @param employment The Employment related to the position chosen by the
	 *            user when adding the StartForm.
	 * @param project The project associated with this StartForm. Only used for
	 *            Commercial productions.
	 * @param creation The creation date to assign to the new StartForm.
	 * @param effectiveStart The effective start date to assign to the new
	 *            StartForm.
	 * @param effectiveEnd The effective end date to assign to the PRIOR
	 *            StartForm, if it was supplied. (Only applies to Change types.)
	 * @param copyRates Copy rate table information from the 'related' or
	 *            'latest' StartForm into the new StartForm.
	 * @param saveIt If true, save the StartForm, otherwise do NOT save it to
	 *            the database.
	 * @param updateEmp If true, update the given Employment instance to include
	 *            the new StartForm in its collection; if false, the
	 *            Employment's collection is not updated.
	 * @return The new StartForm, which may have been saved to the database. By
	 *         default, the work start date will be set to today's date. For a
	 *         New or Re-Hire form, the Hire date will also be set to today's
	 *         date. For a Change form, if the Hire date is null it will be set
	 *         to today's date.
	 */
	public static StartForm createStartForm(String addSdFormType, StartForm related, StartForm latest, Contact contact,
			Employment employment, Project project, Date creation, Date effectiveStart, Date effectiveEnd, boolean copyRates,
			boolean saveIt, boolean updateEmp) {

		StartForm newStartForm;
		Production prod = contact.getProduction();
		PayrollPreference pref = prod.getPayrollPref();
		if (prod.getType().hasPayrollByProject()) {
			project = ProjectDAO.getInstance().refresh(project);
			pref = project.getPayrollPref();
		}
		else { // non-Commercial; don't set project-specific values.
			project = null;
		}
		pref = PayrollPreferenceDAO.getInstance().refresh(pref);
		Role role = null;
		if (employment != null) {
			// Generally employment should not be null, but best to be safe to avoid NPE.
			if (employment.getId() != null) {
				employment = EmploymentDAO.getInstance().refresh(employment);
			}
			role = employment.getRole();
		}
		role = RoleDAO.getInstance().refresh(role);

		if (addSdFormType.equals(StartForm.FORM_TYPE_NEW) || related == null) {
			// "New" type, or
			// "Re-Hire" type, but no "related form" reference selected
			if (copyRates) { // probably from Create Role or Project Add(copy)
				newStartForm = latest.deepCopy(); // copy everything!
				newStartForm.setEffectiveEndDate(null); // inappropriate to copy these dates
				newStartForm.setWorkEndDate(null);
				newStartForm.setEmployment(employment);
			}
			else {
				newStartForm = new StartForm();
				newStartForm.setEmployment(employment);
				newStartForm.getEmployment().setWageState(Constants.STATE_WORKED_CODE);
				if (latest != null) {
					newStartForm.setProdCompany(latest.getProdCompany());
					newStartForm.setUnionLocalNum(latest.getUnionLocalNum());
					newStartForm.setUnionKey(latest.getUnionKey());
					newStartForm.setUseStudioOrLoc(latest.getUseStudioOrLoc());
					// copy demographic data from most recent (effective date) form, if any.
					copyDemographics(newStartForm, latest);
					copyLoanOut(newStartForm, latest);
				}
				else {
					if (! StringUtils.isEmpty(prod.getStudio())) {
						newStartForm.setProdCompany(prod.getStudio());
					}
					else {
						newStartForm.setProdCompany(prod.getTitle());
					}
					User user = contact.getUser();
					newStartForm.setFirstName(user.getFirstName());
					newStartForm.setMiddleName(user.getMiddleName());
					newStartForm.setLastName(user.getLastName());
				}
			}
			if (project != null) { // Commercial/AICP production
				newStartForm.setProject(project);
				newStartForm.setJobName(project.getTitle());
				newStartForm.setJobNumber(project.getEpisode());
			}

			// Copy the SSN, Employee Type and Rate
			if(related != null) {
				newStartForm.setSocialSecurity(related.getSocialSecurity());
				newStartForm.setRate(related.getRate());
				newStartForm.setRateType(related.getRateType());
			}

			// copy Payroll Preference data
			newStartForm.setWorkCity(pref.getWorkCity());
			newStartForm.setWorkState(pref.getWorkState());
			newStartForm.setWorkCountry(pref.getWorkCountry());
			newStartForm.setWorkZip(pref.getWorkZip()); // LS-2343 Include work zip code.
			newStartForm.getEmployment().setWageState(pref.getOvertimeRule());
			newStartForm.setAccountMajor(pref.getAccountMajor());
			// Set jobClass based on role selected in pop-up
			if (role != null) {
				newStartForm.setJobClass(role.getName());
				newStartForm.setLsOccCode(role.getLsOccCode()); // LS-2477 get non-union Occ Codes
			}
			else {
				newStartForm.setJobClass(contact.getRoleName());
			}
		}
		else {
			// "Change" or "Re-hire" type, with back reference
			newStartForm = related.deepCopy();
			newStartForm.setProject(related.getProject());
			newStartForm.setEffectiveEndDate(null); // inappropriate to copy this
			newStartForm.setWorkEndDate(null);
			if (addSdFormType.equals(StartForm.FORM_TYPE_REHIRE)) {
				newStartForm.setWorkCity(pref.getWorkCity());
				newStartForm.setWorkState(pref.getWorkState());
				newStartForm.setWorkZip(pref.getWorkZip()); // LS-2343 Include work zip code.
				newStartForm.setWorkCountry(pref.getWorkCountry());
				if (! newStartForm.getUnionLocalNum().equals(Unions.NON_UNION)) {
					// for union role, blank out rates that were copied from reference form
					newStartForm.getProd().clearRates();
					if (newStartForm.getPrep() != null) {
						newStartForm.getPrep().clearRates();
					}
				}
			}
			else { // "Change" type
				newStartForm.getEmployment().setProductionBatch(related.getEmployment().getProductionBatch());
			}
		}
		if (updateEmp && employment != null && related == null) {
			// have associated Employment, but no "prior" StartForm
			newStartForm.setOffProduction(employment.getOffProduction());
			newStartForm.setJobClass(employment.getOccupation());
			// note: getAccount() never returns null:
			newStartForm.getAccount().copyFrom(employment.getAccount());
			if (StringUtils.isEmpty(newStartForm.getAccountMajor())) {
				newStartForm.setAccountMajor(pref.getAccountMajor());
			}
			newStartForm.setAdditionalStaff(employment.getAdditionalStaff());
		}

		newStartForm.setFormType(addSdFormType);	// New / Change / Re-Hire
		newStartForm.setSequence(1);				// Caller may update this
		newStartForm.setFormNumber(contact.getId() + "-1"); // caller may update this

		newStartForm.setProdTitle(prod.getTitle());

		newStartForm.setContact(contact);
		contact.getStartForms().add(newStartForm);

		// Department & Role always set from drop-down selection in pop-up
//		newStartForm.setProjectMemberId(projectMember.getId());
//		if (role != null) {
//			newStartForm.getEmployment().setDepartment(role.getDepartment());
//		}
//		else {
//			newStartForm.getEmployment().setDepartment(contact.getDepartment());
//		}

		newStartForm.setCreationDate(creation);
		newStartForm.setEffectiveStartDate(effectiveStart);

		if ((! addSdFormType.equals(StartForm.FORM_TYPE_CHANGE)) ||
				newStartForm.getHireDate() == null) {
			Calendar cal = Calendar.getInstance(ApplicationUtils.getTimeZoneStatic());
			CalendarUtils.setStartOfDay(cal);
			Date hireDate = cal.getTime();
			if (effectiveStart != null && effectiveStart.before(hireDate)) {
				hireDate = effectiveStart;
			}
			if (newStartForm.getHireDate() == null || ! copyRates) {
				newStartForm.setHireDate(hireDate);
			}
			newStartForm.setWorkStartDate(hireDate);
		}
		/*if (prod.getAllowOnboarding()) {
			 // For Onboarding productions, starts as PENDING; otherwise is OPEN
			newStartForm.setStatus(ApprovalStatus.PENDING);
		}*/

		if (prod.getType().isTours()) {
			// Tours employees are always Daily/Exempt
			newStartForm.setAllowWorked(true);
			newStartForm.setRateType(EmployeeRateType.DAILY);
			newStartForm.getPerdiemTx().setWeekly(true);
			newStartForm.getPerdiemNtx().setWeekly(true);
			newStartForm.getPerdiemAdv().setWeekly(true);
		}

		// LS-3594 Set the useXfdf flag for commercial productions
		// This does not include hybrid productions
		if(prod.getType().isAicp() && !prod.getPayrollPref().getIncludeTouring()) {
			newStartForm.setUseXfdf(true);
		}

		//LS-2159, set default work country for the StartForm
		if (newStartForm.getWorkCountry() == null) {
			if (pref != null) {
				newStartForm.setWorkCountry(pref.getWorkCountry());
				newStartForm.setWorkState(pref.getWorkState());
			}
		}

		final StartFormDAO startFormDAO = StartFormDAO.getInstance();
		if (saveIt) {
			startFormDAO.save(newStartForm);
			ChangeUtils.logChange(ChangeType.PAYROLL, ActionType.CREATE, prod, SessionUtils.getCurrentUser(), "Start Form for "+contact.getUser().getEmailAddress());
		}

		if (related != null && newStartForm.getFormType().equals(StartForm.FORM_TYPE_CHANGE)) {
			related.setEffectiveEndDate(effectiveEnd);
			startFormDAO.attachDirty(related);
		}
		// Not null check for id, to avoid this code for Employments to be created. Otherwise this code will create an extra employment.
		if (employment != null && employment.getId() != null) {
			// Generally employment should not be null, but best to be safe to avoid NPE.
			employment.getStartForms().add(newStartForm);
			startFormDAO.attachDirty(employment); // update db
		}
		return newStartForm;
	}

	/**
	 * Copy the "Loan-Out" fields from one StartForm to another.
	 *
	 * @param newSf The target StartForm.
	 * @param priorSf The source StartForm.
	 */
	private static void copyLoanOut(StartForm newSf, StartForm priorSf) {
		newSf.setLoanOutCorpName(priorSf.getLoanOutCorpName());
		newSf.setFederalTaxId(priorSf.getFederalTaxId());
		newSf.setStateTaxId(priorSf.getStateTaxId());
		newSf.setIncorporationState(priorSf.getIncorporationState());
		newSf.setIncorporationDate(priorSf.getIncorporationDate());
		newSf.setLoanOutPhone(priorSf.getLoanOutPhone());
		newSf.setLoanOutQualifiedCa(priorSf.getLoanOutQualifiedCa());
		newSf.setLoanOutQualifiedNy(priorSf.getLoanOutQualifiedNy());
		newSf.setLoanOutQualifiedStates(priorSf.getLoanOutQualifiedStates());
		// LS-2576 - Copy Tax Classification and LLC type.
		newSf.setTaxClassification(priorSf.getTaxClassification());
		newSf.setLlcType(priorSf.getLlcType());

		if (priorSf.getLoanOutAddress() != null) {
			newSf.setLoanOutAddress(priorSf.getLoanOutAddress().clone());
		}
		// LS-3592
		if (priorSf.getLoanOutMailingAddress() != null) {
			newSf.setLoanOutMailingAddress(priorSf.getLoanOutMailingAddress().clone());
		}
	}

	/**
	 * Copy the "Loan-Out" fields from User settings to StartForm.
	 *
	 * @param newSf The target StartForm.
	 * @param user The User to be used as the source.
	 */
	private static void copyLoanOut(StartForm newSf, User user) {
		newSf.setLoanOutCorpName(user.getLoanOutCorpName());
		newSf.setFederalTaxId(user.getFederalTaxId());
		newSf.setStateTaxId(user.getStateTaxId());
		newSf.setIncorporationState(user.getIncorporationState());
		newSf.setIncorporationDate(user.getIncorporationDate());
		newSf.setLoanOutPhone(user.getLoanOutPhone());
		newSf.setLoanOutQualifiedCa(user.getLoanOutQualifiedCa());
		newSf.setLoanOutQualifiedNy(user.getLoanOutQualifiedNy());
		newSf.setLoanOutQualifiedStates(user.getLoanOutQualifiedStates());
		// LS-2576 - Copy Tax Classification and LLC type.
		newSf.setTaxClassification(user.getTaxClassification());
		newSf.setLlcType(user.getLlcType());

		if (user.getLoanOutAddress() != null) {
			newSf.setLoanOutAddress(user.getLoanOutAddress().clone());
		}
		// LS-3592
		if (user.getLoanOutMailingAddress() != null) {
			newSf.setLoanOutMailingAddress(user.getLoanOutMailingAddress().clone());
		}
	}

	/**
	 * Copy the demographic information from one StartForm to another.
	 *
	 * @param newSf The target StartForm.
	 * @param priorSf The source StartForm.
	 */
	private static void copyDemographics(StartForm newSf, StartForm priorSf) {
		newSf.setFirstName(priorSf.getFirstName());
		newSf.setMiddleName(priorSf.getMiddleName());
		newSf.setLastName(priorSf.getLastName());
		/*newSf.setAddrLine1(priorSf.getAddrLine1());
		newSf.setAddrLine2(priorSf.getAddrLine2());
		newSf.setCity(priorSf.getCity());
		newSf.setState(priorSf.getState());
		newSf.setZip(priorSf.getZip());
		newSf.setCountry(priorSf.getCountry());*/
		newSf.setPhone(priorSf.getPhone());
		newSf.setSocialSecurity(priorSf.getSocialSecurity());
		newSf.setEthnicCode(priorSf.getEthnicCode());
		newSf.setCitizenStatus(priorSf.getCitizenStatus());
		newSf.setStateOfResidence(priorSf.getStateOfResidence());
		newSf.setMinor(priorSf.getMinor());
		newSf.setGender(priorSf.getGender());
		newSf.setDateOfBirth(priorSf.getDateOfBirth());
		newSf.setAgencyName(priorSf.getAgencyName());
		newSf.setAgentRep(priorSf.getAgentRep());
		newSf.setEmergencyName(priorSf.getEmergencyName());
		newSf.setEmergencyPhone(priorSf.getEmergencyPhone());
		newSf.setEmergencyRelation(priorSf.getEmergencyRelation());
		if (priorSf.getAgencyAddress() != null) {
			newSf.setAgencyAddress(priorSf.getAgencyAddress().clone());
		}
		if (priorSf.getMailingAddress() != null) {
			newSf.setMailingAddress(priorSf.getMailingAddress().clone());
		}
		if (priorSf.getPermAddress() != null) {
			newSf.setPermAddress(priorSf.getPermAddress().clone());
		}
	}

	/**
	 * Find the StartForm that has the latest starting date (either work-start,
	 * effective-start, or hire date).
	 *
	 * @param emp The Employment whose StartForm's are to be searched.
	 * @return The one with the latest date, or null if there are no StartForm`s
	 *         associated with the given Employment.
	 */
	public static StartForm findCurrentStart(Employment emp) {
		emp = EmploymentDAO.getInstance().refresh(emp);
		StartForm latestSf = emp.getStartForm();
		for (StartForm sf : emp.getStartForms()) {
			if (sf.getAnyStartDate().after(latestSf.getAnyStartDate())) {
				latestSf = sf;
			}
		}
		return latestSf;
	}

	/**See {@link #retirementPlanDL}. */
	public static List<SelectItem> getRetirementPlanDL() {
		if (retirementPlanDL == null) {
			retirementPlanDL = SelectionItemDAO.getInstance().createDLbyType(SelectionItem.RETIREMENT_PLAN, "None");
		}
		return retirementPlanDL;
	}

	/**
	 * Currently unused.
	 * <p>
	 * Update the StartForm's contractCode based on the occupation code and
	 * schedule fields. The contractCode is looked up in the PayRate table.
	 *
	 * @param sf The StartForm to be updated.
	 * @return The contractCode found, or null if none was found.
	 */
	/*@Deprecated
	public static String updateContractCode(StartForm sf) {
		String key = null;
		PayRate[] rates = PayRateDAO.getInstance().findByOccCodeAndSchedule(sf.getEffectiveStartDate(),
				sf.getLsOccCode(), sf.getScheduleCode());
		if (rates != null && rates.length > 0) {
			key = rates[0].getContractCode();
			sf.setContractCode(key);
			StartFormDAO.getInstance().attachDirty(sf);
		}
		return key;
	}*/

	/**
	 * This will update any of the 3 required fields (SSN, Work city, Work
	 * State) on the StartForm that are blank with their corresponding values
	 * from the given timecard. (This is typically done whenever someone updates
	 * (i.e., Saves) a timecard.
	 *
	 * @param wtc The timecard whose StartForm is to be updated.
	 */
	public static void updateStartFormRequiredFields(WeeklyTimecard wtc) {
		StartFormDAO startFormDAO = StartFormDAO.getInstance();
		StartForm sf = wtc.getStartForm();
		boolean isTeamPayroll = false;
		Production prod = wtc.getProduction();
		PayrollPreference pref = prod.getPayrollPref();

		if(pref != null) {
			// LS-2726 & LS-2733 - Push changes to Paid As and FEIN to start form
			pref = PayrollPreferenceDAO.getInstance().refresh(pref);
			if(pref.getPayrollService() != null) {
				isTeamPayroll = pref.getPayrollService().getTeamPayroll();
			}
		}
		if (! sf.getHasRequiredFields()) { // don't do anything if Start is already complete
			if (StringUtils.isEmpty(wtc.getSocialSecurity()) && StringUtils.isEmpty(wtc.getCityWorked())
					&& StringUtils.isEmpty(wtc.getStateWorked())) {
				// No point in trying to update - nothing useful in timecard;
				// this avoids locking & unlocking the StartForm needlessly.
			}
			else {
				sf = startFormDAO.refresh(sf);
				User user = SessionUtils.getCurrentUser();
				if (startFormDAO.lock(sf, user)) {
					if (StringUtils.isEmpty(sf.getSocialSecurity())) {
						if ( ! StringUtils.isEmpty(wtc.getSocialSecurity())) {
							sf.setSocialSecurity(wtc.getSocialSecurity());
						}
					}
					if (StringUtils.isEmpty(sf.getWorkCity())) {
						if ( ! StringUtils.isEmpty(wtc.getCityWorked())) {
							sf.setWorkCity(wtc.getCityWorked());
						}
					}
					if (StringUtils.isEmpty(sf.getWorkState())) {
						if ( ! StringUtils.isEmpty(wtc.getStateWorked())) {
							sf.setWorkState(wtc.getStateWorked());
						}
					}
					if (StringUtils.isEmpty(sf.getWorkCountry())) { // LS-2156
						if ( ! StringUtils.isEmpty(wtc.getWorkCountry())) {
							sf.setWorkCountry(wtc.getWorkCountry());
						}
					}

					if(isTeamPayroll) {
						// LS-2726 & LS-2733 - Push changes to Paid As and FEIN to start form
						sf.setPaidAs(wtc.getPaidAs());
						sf.setFederalTaxId(wtc.getFedCorpId());
					}

					// Update and unlock the StartForm
					startFormDAO.unlock(sf, user.getId());
				}
			}
		}
	}

	/**
	 * Get Payroll Preferences from StartForm
	 * @param startForm
	 * @return  PayrollPreference, which has been getting from  StartForm.
	 */
	public static PayrollPreference getSFPayrollPreference(StartForm startForm) {
		PayrollPreference payrollPref = null;
		Production production = SessionUtils.getCurrentOrViewedProduction();
		log.debug("");
		if (startForm != null && production != null) {
			log.debug("");
			if (production != null) {
				if (production.getType().hasPayrollByProject()) {
					if (startForm.getProject() != null) {
						Project proj = ProjectDAO.getInstance().refresh(startForm.getProject());
						payrollPref = proj.getPayrollPref();
					}
				}
				else {
					payrollPref = production.getPayrollPref();
				}
				payrollPref = PayrollPreferenceDAO.getInstance().refresh(payrollPref);
			}
		}
		return payrollPref;
	}

	/**
	 * Update a StartForm with information from a FormModelRelease.
	 * @param formModelRelease The model release form containing payroll information, to be applied to the StartForm (Payroll Start).
	 * @return The updated StartForm.
	 */
	public static StartForm updateStartForm(StartForm sf, FormModelRelease form) {

		sf = StartFormDAO.getInstance().refresh(sf); // prevent LIE
		sf.setModelRelease(form);
		sf.setUnionLocalNum(Unions.NON_UNION);
		sf.setJobClass("Model");
		sf.setLsOccCode("PMDL");
		sf.setJobNumber(form.getJob());
		sf.setJobName(form.getProjectName());
		if (StringUtils.isEmpty(sf.getSocialSecurity())) {
			sf.setSocialSecurity(form.getModelSSN());
		}

		sf.setWorkCity(form.getJobCity());
		sf.setWorkState(form.getJobState());

		if (form.getModelCorporationYes()) {
			sf.setPaidAs(PaidAsType.LO);
			User user = sf.getContact().getUser();
			copyLoanOut(sf, user); // copy User's "My Account" loan-out info
			sf.setLoanOutCorpName(form.getModelCorporationText());
			sf.setFederalTaxId(form.getCorporationFederalId());
		}
		else {
			sf.setPaidAs(PaidAsType.I);
			sf.setFederalTaxId(null);
		}

		// Agency info
		sf.setAgencyName(form.getModelAgencyText());
		Address addr = sf.getAgencyAddress();
		if (addr == null) {
			addr = new Address();
			sf.setAgencyAddress(addr);
		}
		addr.setAddrLine1(form.getModelAgencyStreet());
		if (form.getModelAddress() != null) {
			addr.setAddrLine2(form.getModelAddress().getAddrLine1());
		}

		// Rates & payments
		sf.setStartRates(StartRatesType.RATES_STD);
		sf.setUseStudioOrLoc(StartForm.USE_STUDIO_RATE);

		sf.setEmpAgentCommisssion(form.getAgentPercentage() == null ? null
				: new BigDecimal(form.getAgentPercentage()));
		sf.setEmpReuse(null);
		if (form.getReuseFee() != null) {
			try {
				sf.setEmpReuse(new BigDecimal(form.getReuseFee()));
			}
			catch (Exception e) {
				// ignore, assume text in field
			}
		}

		BigDecimal hourlyRate = form.getRateForService();
		if (form.getPerHour()) {
			sf.setRateType(EmployeeRateType.HOURLY);
			sf.setRate(form.getRateForService());
		}
		else if (form.getPerDay()) {
			sf.setRateType(EmployeeRateType.DAILY);
			sf.setRate(form.getRateForService());
			sf.setGuarHours(sf.getProd(), form.getServiceHours());
			if (form.getServiceHours() != null && form.getServiceHours().signum() > 0) {
				hourlyRate = hourlyRate.divide(form.getServiceHours(), 2, RoundingMode.HALF_UP);
			}
		}

		// OVERTIME
		if (form.getAddlYes()) {
			if (form.getAddlOverTime()) {
				sf.getProd().setOt1Rate(form.getAddlPerHour());
				if (form.getAddlPerHour() != null && hourlyRate != null) {
					sf.getProd().setOt1Multiplier(form.getAddlPerHour().divide(hourlyRate, 4, RoundingMode.HALF_UP));
				}
			}
		}

		// PREP RATES
		if (form.getPrepDay()) {
			if (sf.getPrep() == null) {
				sf.setPrep(new StartRateSet());
			}
			sf.setStartRates(StartRatesType.RATES_PREP);
			sf.getPrep().getDaily().setStudioHrs(new BigDecimal(form.getPrepPerHours()));
			if (sf.getRateType() == EmployeeRateType.DAILY) {
				sf.getPrep().getDaily().setStudio(form.getPrepPerDay());
				sf.getPrep().getHourly().setStudio(null);
			}
			else if (sf.getRateType() == EmployeeRateType.HOURLY) {
				BigDecimal rate = form.getPrepPerDay();
				if (form.getPrepPerHours() != null) {
					rate = rate.divide(new BigDecimal(form.getPrepPerHours()), 4, RoundingMode.HALF_UP);
				}
				sf.getPrep().getDaily().setStudio(null);
				sf.getPrep().getHourly().setStudio(rate);
			}
		}

		// PER DIEM
		sf.getPerdiemNtx().setAmt(form.getPerdiemAmount());
		sf.getPerdiemNtx().setWeekly(false);

		StartFormDAO.getInstance().attachDirty(sf);
		return sf;
	}

}
