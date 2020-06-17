package com.lightspeedeps.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.persistence.*;

import com.lightspeedeps.type.FormFieldType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.common.NumberUtils;

/**
 * This entity contains all the fields of the UDA form. It is used to maintain
 * the user wise record of the UDA entries.
 *
 */

@Entity
@Table(name = "form_uda_contract")
public class FormUDAContract extends Form<FormUDAContract> {

	private static final long serialVersionUID = 36964489245810221L;

	public static final Byte UDA_VERSION_2019 = 19; // last 2 digit of the years
	public static final Byte DEFAULT_UDA_VERSION = UDA_VERSION_2019;

	/** Form version number for 2019. */
	public static final Byte UDA_CONTRACT_VERSION_2019 = 1;
	/** The current form version number. */
	public static final Byte DEFAULT_UDA_CONTRACT_VERSION = UDA_CONTRACT_VERSION_2019;

	public static final Byte  UDA_NUM_TIME_ENTRIES= 4;

	/** INM Number */
	private String inmNumber;
	/** Production Number */
	private String prodNumber;
	/** Name of the Producer */
	private String producerName;
	/** Address of the Producer */
	private Address producerAddress;
	/** Phone number of the Producer */
	private String producerPhone;
	/** Email of the Producer */
	private String producerEmail;
	/** Name of the responsible */
	private String responsibleName;
	/** Producer's UDA member number */
	private String producerUDA;
	/** Name of the artist */
	private String artistName;
	/** Name of the Company  */
	private String companyName;
	/** Address of the artist */
	private Address artistAddress;
	/** Artist Phone */
	private String artistPhone;
	/** Artist Email */
	private String artistEmail;

	private String advertiserName1;

	private String advertiserName2;

	private String advertiserName3;

	private String productName1;

	private String productName2;

	private String productName3;
	/** Artist GST Number */
	private String artistGst;
	/** Artist QST Number */
	private String artistQst;
	/** Artist Social Insurance Number */
	private String socialInsuranceNumber;

	private String noUDAMember;
	/** Artist Date of birth */
	private Date dob;
	/** If artist is active member. */
	private boolean activeMember;
	/** If artist is apprentice member. */
	private boolean apprenticeMember;
	/** If artist is permit holder. */
	private boolean permitHolder;
	/** If artist is less than 18 years. */
	private boolean lessThan18Years;
	/** Title for commercial. */
	private String commercialTitle;
	/** Description for commercial. */
	private String commercialDescription;
	/** Commercial Version. */
	private String commercialVersion;
	/** The Recording details for Artist */
	private Recording recording1;
	/** The Recording details for Artist */
	private Recording recording2;
	/** The Recording details for Artist */
	private Recording recording3;
	/** The Recording details with another information */
	private String recordingOther;
	/** Whether broadcasting language is Quebec- French  */
	private boolean useTableA;
	/** Whether broadcasting language is Quebec any other than French  */
	private boolean useTableB;
	/** Whether broadcasting language is Canada - French  */
	private boolean useTableC;
	/** Whether broadcasting language is Canada any other than French  */
	private boolean useTableD;
	/** Whether broadcasting language is 1 Foreign country, all languages */
	private boolean useTableE;
	/** Whether broadcasting language is International without Canada */
	private boolean useTableF;
	/** Number of months */
	private boolean period3month;

	private boolean period6month;

	private boolean period9month;

	private Date firstUseDate;
	/** Authorization to use for the portfolio */
	private boolean portfolioUseAuthorization;

	private boolean exclusivityDuration;

	private String exclusivityDurationText;

	private String exclusivityService;

	private boolean functionPrincipalPerformer;

	private boolean functionChorist;

	private boolean functionSOCPerformer;

	private boolean functionDemonstrator;

	private boolean functionVoiceOver;

	private boolean functionPrincipalExtra;

	private boolean functionExtra;

	private boolean functionOther;

	private String functionOtherText;

	private String feeRate;

	private BigDecimal negotiableRecordingPercent;

	private String fieldName;

	private String specialCondition1;

	private BigDecimal negotiableInmUsePercent;

	private BigDecimal negotiableInmUseAmt;

	private BigDecimal negotiableRecordingAmt;

	private String specialCondition2;

	private BigDecimal negotiableOtherPercent;

	private BigDecimal negotiableOtherAmt;

	private String specialCondition3;

	private Date negotiableDateA;

	private Date negotiableDateB;

	private WeeklyTimecard weeklyTimecard;

	private Date signatureDate;

	private String functionName;

	private BigDecimal functionRate;

	private BigDecimal additionalAmount;

	private BigDecimal otHour;

	private BigDecimal otHourAmount;

	private BigDecimal otHourTotal;

	private BigDecimal addOtHour;

	private BigDecimal addOtHourAmount;

	private BigDecimal addOtHourTotal;

	private BigDecimal nightHour;

	private BigDecimal nightHourAmount;

	private BigDecimal nightHourTotal;

	private BigDecimal otNightHour;

	private BigDecimal otNightHourAmount;

	private BigDecimal otNightHourTotal;

	private BigDecimal holidayHour;

	private BigDecimal holidayHourAmount;

	private BigDecimal holidayHourTotal;

	private BigDecimal travelHour;

	private BigDecimal travelHourAmount;

	private BigDecimal travelHourTotal;

	private BigDecimal waitingHour;

	private BigDecimal waitingHourAmount;

	private BigDecimal waitingHourTotal;

	private BigDecimal standByDay;

	private BigDecimal standByDayAmount;

	private BigDecimal standByDayTotal;

	private Boolean lodgingAndMeal;

	private BigDecimal lodgingAndMealsNumber;

	private BigDecimal lodgingAndMealsAmount;

	private BigDecimal lodgingAndMealsTotal;

	private Boolean mealsOnly;

	private BigDecimal mealOnlyNumber;

	private BigDecimal mealOnlyAmount;

	private BigDecimal mealOnlyTotal;

	private BigDecimal fittingRehearsal;

	private BigDecimal fittingRehearsalAmount;

	private BigDecimal fittingRehearsalTotal;

	private BigDecimal mileageRate;

	private BigDecimal mileageRateAmount;

	private BigDecimal mileageRateTotal;

	private BigDecimal callBackOnCamera;

	private BigDecimal callBackOnCameraTotal;

	private BigDecimal callBackOffCamera;

	private BigDecimal callBackOffCameraTotal;

	private Integer tagNumber;

	private BigDecimal tagValue;

	private BigDecimal tagTotal;

	private String others;

	private BigDecimal othersTotal;

	private BigDecimal grossFee;

	private BigDecimal apprentice;

	private BigDecimal permitHolderNumber;

	private BigDecimal permitNumber;

	private BigDecimal permitAmount;

	private BigDecimal permitTotal;

	private BigDecimal minorityAgedArtistsFund;

	private BigDecimal feeBeforeTaxes;

	private BigDecimal gst;

	private BigDecimal qst;

	private BigDecimal netFee;

	private BigDecimal producerShare;

	private BigDecimal totalAmountToCSA;

	private Office office;

	public FormUDAContract() {
		super();
		setVersion(UDA_VERSION_2019);
	}

	@Override
	public void fillFieldValues(ContactDocument cd, Map<String,String> fieldValues) {
		DecimalFormat df = new DecimalFormat("###.##");
		df.setRoundingMode(RoundingMode.CEILING);
		DateFormat dateFmt = new SimpleDateFormat(Constants.DATE_FORMAT_CANADA);
		DateFormat timeFmt = new SimpleDateFormat("h:mm a");
		cd = cd.refresh(); // eliminate DAO reference. LS-2737

		fieldValues.put(FormFieldType.INM_NUMBER.name(), getInmNumber());
		fieldValues.put(FormFieldType.PROD_NUMBER.name(), getProdNumber());

		//Producer
		fieldValues.put(FormFieldType.PRODUCER_NAME.name(), getProducerName());
		if (getProducerAddress() != null) {
			fieldValues.put(FormFieldType.PRODUCER_ADDRESS_1.name(), (getProducerAddress().getAddrLine1() != null ? getProducerAddress().getAddrLine1() : ""));
			fieldValues.put(FormFieldType.PRODUCER_ADDRESS_2.name(), (getProducerAddress().getAddrLine2() != null ? getProducerAddress().getAddrLine2() : ""));
		}
		else {
			fieldValues.put(FormFieldType.PRODUCER_ADDRESS_1.name(), "");
			fieldValues.put(FormFieldType.PRODUCER_ADDRESS_2.name(),  "");
		}
		fieldValues.put(FormFieldType.PRODUCER_PHONE.name(), getProducerPhone());
		fieldValues.put(FormFieldType.PRODUCER_EMAIL.name(), getProducerEmail());
		fieldValues.put(FormFieldType.RESPONSIBLE_NAME.name(), getResponsibleName());
		fieldValues.put(FormFieldType.PRODUCER_UDA.name(), getProducerUDA());
		fieldValues.put(FormFieldType.ARTIST_NAME.name(), getArtistName());
		fieldValues.put(FormFieldType.COMPANY_NAME.name(), getCompanyName());
		if (getArtistAddress() != null) {
			fieldValues.put(FormFieldType.ARTIST_ADDRESS_1.name(), (getArtistAddress().getAddrLine1() != null ? getArtistAddress().getAddrLine1() : ""));
			fieldValues.put(FormFieldType.ARTIST_ADDRESS_2.name(),(getArtistAddress().getAddrLine2() != null ? getArtistAddress().getAddrLine2() : ""));
		}
		else {
			fieldValues.put(FormFieldType.ARTIST_ADDRESS_1.name(), "");
			fieldValues.put(FormFieldType.ARTIST_ADDRESS_2.name(),  "");
		}
		fieldValues.put(FormFieldType.ARTIST_PHONE.name(), getArtistPhone());
		fieldValues.put(FormFieldType.ARTIST_EMAIL.name(), getArtistEmail());
		fieldValues.put(FormFieldType.ADVERTISER_NAME_1.name(), getAdvertiserName1());
		fieldValues.put(FormFieldType.ADVERTISER_NAME_2.name(), getAdvertiserName2());
		fieldValues.put(FormFieldType.ADVERTISER_NAME_3.name(), getAdvertiserName3());
		fieldValues.put(FormFieldType.PRODUCT_NAME_1.name(), getProductName1());
		fieldValues.put(FormFieldType.PRODUCT_NAME_2.name(), getProductName2());
		fieldValues.put(FormFieldType.PRODUCT_NAME_3.name(), getProductName3());
		fieldValues.put(FormFieldType.ARTIST_GST.name(), getArtistGst());
		fieldValues.put(FormFieldType.ARTIST_QST.name(), getArtistQst());
		fieldValues.put(FormFieldType.SOCIAL_INSURANCE.name(), getSocialInsuranceNumber());
		fieldValues.put(FormFieldType.NO_UDA_MEMBER.name(), getNoUDAMember());
		fieldValues.put(FormFieldType.DATE_OF_BIRTH.name(), dateFormat(dateFmt, getDob()));
		String member = null;
		if (isActiveMember()) {
			member = "Choice1";
		}
		else if (isApprenticeMember()) {
			member = "Choice2";
		}
		else if (isPermitHolder()) {
			member = "Choice3";
		}
		if (member != null) {
			fieldValues.put(FormFieldType.MEMBER.name(), member);
		}
		else {
			fieldValues.put(FormFieldType.MEMBER.name(), "");
		}
		fieldValues.put(FormFieldType.LESS_THAN_18YEARS.name(), (isLessThan18Years() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.COMMERCIAL_TITLE.name(), getCommercialTitle());
		fieldValues.put(FormFieldType.COMMERCIAL_VERSION.name(), getCommercialVersion());
		fieldValues.put(FormFieldType.COMMERCIAL_DESCRIPTION.name(), getCommercialDescription());
		fieldValues.put(FormFieldType.RECORDING_OTHER.name(), getRecordingOther());
		fieldValues.put(FormFieldType.USE_TABLE_A.name(), (isUseTableA() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.USE_TABLE_B.name(), (isUseTableB() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.USE_TABLE_C.name(), (isUseTableC() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.USE_TABLE_D.name(), (isUseTableD() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.USE_TABLE_E.name(), (isUseTableE() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.USE_TABLE_F.name(), (isUseTableF() ? "Yes" : "Off"));
		String period = null;
		if (isPeriod3month()) {
			period = "Choice4";
		}
		else if (isPeriod6month()) {
			period = "Choice5";
		}
		else if (isPeriod9month()) {
			period = "Choice6";
		}
		if (period != null) {
			fieldValues.put(FormFieldType.PERIOD.name(), period);
		}
		else {
			fieldValues.put(FormFieldType.PERIOD.name(), "");
		}
		fieldValues.put(FormFieldType.FIRST_USE_DATE.name(), dateFormat(dateFmt,getFirstUseDate()));
		fieldValues.put(FormFieldType.PROTFOLIO_USE_AUTHORIZATION.name(), (isPortfolioUseAuthorization() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.EXCLUSIVITY_DURATION.name(), (getExclusivityDuration() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.EXCLUSIVITY_DUR_TEXT.name(), getExclusivityDurationText());
		fieldValues.put(FormFieldType.EXCLUSIVITY_SERVICE.name(),getExclusivityService());

		// LS-2854 converted function radio buttons to check boxes.
		fieldValues.put(FormFieldType.FUNCTION_PRINCIPAL.name(), (isFunctionPrincipalPerformer() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.FUNCTION_PRINCIPAL_EXTRA.name(), (isFunctionPrincipalExtra() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.FUNCTION_EXTRA.name(), (isFunctionExtra() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.FUNCTION_CHORIST.name(), (isFunctionChorist() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.FUNCTION_SOC.name(), (isFunctionSOCPerformer() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.FUNCTION_DEMONSTRATOR.name(), (isFunctionDemonstrator() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.FUNCTION_VOICE_OVER.name(), (isFunctionVoiceOver() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.FUNCTION_OTHER.name(), (isFunctionOther() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.FUNCTION_OTR_TEXT.name(), getFunctionOtherText());

		fieldValues.put(FormFieldType.FEE_RATE.name(), getFeeRate());
		fieldValues.put(FormFieldType.NEGOTIABLE_RECORDING_PERCENT.name(), bigDecimalFormat2Places(getNegotiableRecordingPercent()));
		fieldValues.put(FormFieldType.NEGOTIABLE_INM_USE_AMOUNT.name(), bigDecimalFormat2Places(getNegotiableInmUseAmt()));
		fieldValues.put(FormFieldType.SPECIAL_CONDITION_1.name(), getSpecialCondition1());
		fieldValues.put(FormFieldType.NEGOTIABLE_INM_USE_PERCENT.name(), bigDecimalFormat2Places(getNegotiableInmUsePercent()));
		fieldValues.put(FormFieldType.NEGOTIABLE_RECORDING_AMOUNT.name(), bigDecimalFormat2Places(getNegotiableRecordingAmt()));
		fieldValues.put(FormFieldType.SPECIAL_CONDITION_2.name(), getSpecialCondition2());
		fieldValues.put(FormFieldType.NEGOTIABLE_OTHER_PERCENT.name(), bigDecimalFormat2Places(getNegotiableOtherPercent()));
		fieldValues.put(FormFieldType.NEGOTIABLE_OTHER_AMOUNT.name(), bigDecimalFormat2Places(getNegotiableOtherAmt()));
		fieldValues.put(FormFieldType.SPECIAL_CONDITION_3.name(), getSpecialCondition3());
		if (getNegotiableDateA() != null) {
			DateFormat dateFormat = new SimpleDateFormat("dd-MM");
			String strDate = dateFormat.format(getNegotiableDateA());
			fieldValues.put(FormFieldType.NEGOTIABLE_DATE_A.name(), strDate);
		}
		else {
			fieldValues.put(FormFieldType.NEGOTIABLE_DATE_A.name(), "");
		}
		if (getNegotiableDateB() != null) {
			DateFormat dateFormat = new SimpleDateFormat("yy");
			String strDate = dateFormat.format(getNegotiableDateB());
			fieldValues.put(FormFieldType.NEGOTIABLE_DATE_B.name(), strDate);
		}
		else {
			fieldValues.put(FormFieldType.NEGOTIABLE_DATE_B.name(), "");
		}
		fieldValues.put(FormFieldType.FUNCTION_NAME.name(), getFunctionName());
		fieldValues.put(FormFieldType.FUNCTION_RATE.name(), bigDecimalFormat2Places(getFunctionRate()));
		fieldValues.put(FormFieldType.ADDITIONAL_AMOUNT.name(), bigDecimalFormat2Places(getAdditionalAmount()));
		fieldValues.put(FormFieldType.OT_HOUR_NUMBER.name(), bigDecimalFormat2Places(getOtHour()));
		fieldValues.put(FormFieldType.OT_HOUR_AMOUNT.name(), bigDecimalFormat2Places(getOtHourAmount()));
		fieldValues.put(FormFieldType.OT_HOUR_TOTAL.name(), bigDecimalFormat2Places(getOtHourTotal()));
		fieldValues.put(FormFieldType.ADD_OT_HRS_NUMBER.name(), bigDecimalFormat2Places(getAddOtHour()));
		fieldValues.put(FormFieldType.ADD_OT_HRS_AMOUNT.name(), bigDecimalFormat2Places(getAddOtHourAmount()));
		fieldValues.put(FormFieldType.ADD_OT_HRS_TOTAL.name(), bigDecimalFormat2Places(getAddOtHourTotal()));
		fieldValues.put(FormFieldType.NIGHT_HOUR_NUMBER.name(), bigDecimalFormat2Places(getNightHour()));
		fieldValues.put(FormFieldType.NIGHT_HOUR_AMOUNT.name(), bigDecimalFormat2Places(getNightHourAmount()));
		fieldValues.put(FormFieldType.NIGHT_HOUR_TOTAL.name(), bigDecimalFormat2Places(getNightHourTotal()));
		fieldValues.put(FormFieldType.OT_NIGHT_HRS_NUMBER.name(), bigDecimalFormat2Places(getOtNightHour()));
		fieldValues.put(FormFieldType.OT_NIGHT_HRS_AMOUNT.name(), bigDecimalFormat2Places(getOtNightHourAmount()));
		fieldValues.put(FormFieldType.OT_NIGHT_HRS_TOTAL.name(), bigDecimalFormat2Places(getOtNightHourTotal()));
		fieldValues.put(FormFieldType.HOLIDAY_HOUR_TOTAL.name(), bigDecimalFormat2Places(getHolidayHourTotal()));
		fieldValues.put(FormFieldType.HOLIDAY_HOUR_NUMBER.name(), bigDecimalFormat2Places(getHolidayHour()));
		fieldValues.put(FormFieldType.HOLIDAY_HOUR_AMOUNT.name(), bigDecimalFormat2Places(getHolidayHourAmount()));
		fieldValues.put(FormFieldType.TRAVEL_HOUR_NUMBER.name(), bigDecimalFormat2Places(getTravelHour()));
		fieldValues.put(FormFieldType.TRAVEL_HOUR_AMOUNT.name(), bigDecimalFormat2Places(getTravelHourAmount()));
		fieldValues.put(FormFieldType.TRAVEL_HOUR_TOTAL.name(), bigDecimalFormat2Places(getTravelHourTotal()));
		fieldValues.put(FormFieldType.WAITING_HOUR_NUMBER.name(), bigDecimalFormat2Places(getWaitingHour()));
		fieldValues.put(FormFieldType.WAITING_HOUR_AMOUNT.name(), bigDecimalFormat2Places(getWaitingHourAmount()));
		fieldValues.put(FormFieldType.LODGING_MEALS_NUMBER.name(), bigDecimalFormat2Places(getLodgingAndMealsNumber()));
		fieldValues.put(FormFieldType.WAITING_HOUR_TOTAL.name(), bigDecimalFormat2Places(getWaitingHourTotal()));
		fieldValues.put(FormFieldType.STAND_BY_DAY_NUMBER.name(), bigDecimalFormat2Places(getStandByDay()));
		fieldValues.put(FormFieldType.STAND_BY_DAY_AMOUNT.name(), bigDecimalFormat2Places(getStandByDayAmount()));
		fieldValues.put(FormFieldType.STAND_BY_DAY_TOTAL.name(), bigDecimalFormat2Places(getStandByDayTotal()));
		String mealChoice = null;
		if (getLodgingAndMeal() != null) {
			if (getLodgingAndMeal()) {
				mealChoice = "Choice1";
			}
			else {
				mealChoice = "Choice2";
			}
		}
		if (mealChoice != null) {
			fieldValues.put(FormFieldType.MEAL_CHOICE.name(), mealChoice);
		}
		
		fieldValues.put(FormFieldType.FUNC_LODGING_MEALS.name(), (getLodgingAndMeal() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.FUNC_MEAL_ONLY.name(), (getMealsOnly() ? "Yes" : "Off"));
		fieldValues.put(FormFieldType.LODGING_MEALS_AMOUNT.name(), bigDecimalFormat2Places(getLodgingAndMealsAmount()));
		fieldValues.put(FormFieldType.LODGING_MEALS_TOTAL.name(), bigDecimalFormat2Places(getLodgingAndMealsTotal()));
		fieldValues.put(FormFieldType.MEAL_ONLY_NUMBER.name(), bigDecimalFormat2Places(getMealOnlyNumber()));
		fieldValues.put(FormFieldType.MEAL_ONLY_AMOUNT.name(), bigDecimalFormat2Places(getMealOnlyAmount()));
		fieldValues.put(FormFieldType.MEAL_ONLY_TOTAL.name(), bigDecimalFormat2Places(getMealOnlyTotal()));
		fieldValues.put(FormFieldType.FITTING_REHEARSAL_NUMBER.name(), bigDecimalFormat2Places(getFittingRehearsal()));
		fieldValues.put(FormFieldType.FITTING_REHEARSAL_AMOUNT.name(), bigDecimalFormat2Places(getFittingRehearsalAmount()));
		fieldValues.put(FormFieldType.FITTING_REHEARSAL_TOTAL.name(), bigDecimalFormat2Places(getFittingRehearsalTotal()));
		fieldValues.put(FormFieldType.MILEAGE_RATE_NUMBER.name(), bigDecimalFormat2Places(getMileageRate()));
		fieldValues.put(FormFieldType.MILEAGE_RATE_AMOUNT.name(), bigDecimalFormat2Places(getMileageRateAmount()));
		fieldValues.put(FormFieldType.MILEAGE_RATE_TOTAL.name(), bigDecimalFormat2Places(getMileageRateTotal()));
		fieldValues.put(FormFieldType.CALL_BACK_ON_CAMERA_NUMBER.name(), bigDecimalFormat2Places(getCallBackOnCamera()));
		fieldValues.put(FormFieldType.CALL_BACK_ON_CAMERA_TOTAL.name(), bigDecimalFormat2Places(getCallBackOnCameraTotal()));
		fieldValues.put(FormFieldType.CALL_BACK_OFF_CAMERA_NUMBER.name(), bigDecimalFormat2Places(getCallBackOffCamera()));
		fieldValues.put(FormFieldType.CALL_BACK_OFF_CAMERA_TOTAL.name(), bigDecimalFormat2Places(getCallBackOffCameraTotal()));
		fieldValues.put(FormFieldType.TAG_NUMBER.name(), intFormat(getTagNumber()));
		fieldValues.put(FormFieldType.TAG_VALUE.name(), bigDecimalFormat2Places(getTagValue()));
		fieldValues.put(FormFieldType.TAG_TOTAL.name(), bigDecimalFormat2Places(getTagTotal()));
		fieldValues.put(FormFieldType.OTHERS_NUMBER.name(), getOthers());
		fieldValues.put(FormFieldType.OTHERS_TOTAL.name(), bigDecimalFormat2Places(getOthersTotal()));
		fieldValues.put(FormFieldType.GROSS_FEE.name(), bigDecimalFormat2Places(getGrossFee()));
		fieldValues.put(FormFieldType.APPRENTICE.name(), bigDecimalFormat2Places(getApprentice()));
		fieldValues.put(FormFieldType.PERMIT_HOLDR_NUMBER.name(), bigDecimalFormat2Places(getPermitHolderNumber()));
		fieldValues.put(FormFieldType.PERMIT_NUMBER.name(), bigDecimalFormat2Places(getPermitNumber()));
		fieldValues.put(FormFieldType.PERMITS_TOTAL.name(), bigDecimalFormat2Places(getPermitTotal()));
		fieldValues.put(FormFieldType.PERMIT_AMOUNT.name(), bigDecimalFormat2Places(getPermitAmount()));
		fieldValues.put(FormFieldType.MINORITY_AGED_ARTISTS_FUND.name(), bigDecimalFormat2Places(getMinorityAgedArtistsFund()));
		fieldValues.put(FormFieldType.FEE_BEFORE_TAXES.name(), bigDecimalFormat2Places(getFeeBeforeTaxes()));
		fieldValues.put(FormFieldType.GST_NUMBER.name(), bigDecimalFormat2Places(getGst()));
		fieldValues.put(FormFieldType.QST_NUMBER.name(), bigDecimalFormat2Places(getQst()));
		fieldValues.put(FormFieldType.NET_FEE.name(), bigDecimalFormat2Places(getNetFee()));
		fieldValues.put(FormFieldType.TOTAL_AMOUNT_TO_CSA.name(), bigDecimalFormat2Places(getTotalAmountToCSA()));
		fieldValues.put(FormFieldType.PRODUCER_SHARE.name(), bigDecimalFormat2Places(getProducerShare()));
		fieldValues.put(FormFieldType.SIGNATURE_DATE.name(), dateFormat(dateFmt, getSignatureDate()));

		if (cd.getEmpSignature() != null) {
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_NAME.name(), // Note: 2-line output requires modified PDF.
					cd.getEmpSignature().getSignedBy() + "\n" + cd.getEmpSignature().getUuid());
		}
		else {
			fieldValues.put(FormFieldType.EMPLOYEE_SIGNATURE_NAME.name(), "");
		}

		if (cd.getEmployerSignature() != null) {
			fieldValues.put(FormFieldType.EMP_SIGNATURE_NAME.name(), // Note: 2-line output requires modified PDF.
					cd.getEmployerSignature().getSignedBy() + "\n" + cd.getEmployerSignature().getUuid());
		}
		else {
			fieldValues.put(FormFieldType.EMP_SIGNATURE_NAME.name(), "");
		}

		if (cd.getOptSignature() != null) {
			fieldValues.put(FormFieldType.EMPLOYEE_SIGN_NAME_2.name(), // Note: 2-line output requires modified PDF.
					cd.getOptSignature().getSignedBy() + "\n" + cd.getOptSignature().getUuid());
		}
		else {
			fieldValues.put(FormFieldType.EMPLOYEE_SIGN_NAME_2.name(), "");
		}

		if (cd.getApprovalSignature() != null) {
			fieldValues.put(FormFieldType.EMP_SIGN_NAME_2.name(), // Note: 2-line output requires modified
																			// PDF.
					cd.getApprovalSignature().getSignedBy() + "\n" + cd.getApprovalSignature().getUuid());
		}
		else {
			fieldValues.put(FormFieldType.EMP_SIGN_NAME_2.name(), "");
		}

		// Fill in recording section
		Recording rcd1 = this.recording1;
		if (rcd1 != null)
			fillRecords(rcd1, fieldValues, 1, dateFmt);
		Recording rcd2 = this.recording2;
		if (rcd2 != null)
			fillRecords(rcd2, fieldValues, 2, dateFmt);
		Recording rcd3 = this.recording3;
		if (rcd3 != null)
			fillRecords(rcd3, fieldValues, 3, dateFmt);

		// Fill in work-time section
		int iy = 1;
		for (DailyTime dt : this.getWeeklyTimecard().getDailyTimes()) {
			fillTime(dt, iy++, fieldValues, timeFmt, dateFmt);
		}
	}


	private void fillRecords(Recording rcd, Map<String, String> fieldValues, int ix, DateFormat dateFmt) {
		String suffix = "" + ix;
		fieldValues.put("RECORDING-DATE-" + suffix,(rcd.getRecordingDate() == null) ? "" : dateFmt.format(rcd.getRecordingDate()));
		fieldValues.put("RECORDING-TIME-" + suffix, (rcd.getRecordingTime() == null) ? "" : rcd.getRecordingTime().toString());
		fieldValues.put("RECORDING-LOCATION-" + suffix,(rcd.getRecordingLocation() == null) ? "" : rcd.getRecordingLocation());
	}


	private void fillTime(DailyTime dt, int ix, Map<String, String> fieldValues, DateFormat timeFmt, DateFormat dateFmt) {
		String suffix = "" + ix;

		fieldValues.put("DATE-OF-RECORDING-"+suffix, (dt.getDate() == null) ? "" : dateFmt.format(dt.getDate()));
		fieldValues.put("TIME-OF-ARRIVAL-"+suffix, timeFormat(dt.getCallTime2()));
		fieldValues.put("TRAVEL-LOCATION-FROM-"+suffix, timeFormat(dt.getTrvlToLocFrom()));
		fieldValues.put("TRAVEL-LOCATION-TO-"+suffix, timeFormat(dt.getTrvlToLocTo()));
		fieldValues.put("CALL-TIME-"+suffix, timeFormat(dt.getCallTime()));
		fieldValues.put("M1-OUT-"+suffix, timeFormat(dt.getM1Out()));
		fieldValues.put("M1-IN-"+suffix, timeFormat(dt.getM1In()));
		fieldValues.put("FINISH-TIME-"+suffix, timeFormat(dt.getWrap()));
	}

	/**
	 * Convert a decimal time to printable.
	 * @param time
	 * @return "h:mm AM/PM"
	 */
	private String timeFormat(BigDecimal time) {
		// TODO this could be cleaned up, also need AM/PM support; we might have code somewhere that does this already!!
		// ** see DecimalTimeFormatConverter.getAsString().
		if (time == null) {
			return "";
		}
		int hours = time.intValue();
		time = time.subtract(new BigDecimal(hours));
		int minutes = time.multiply(new BigDecimal(60)).intValue();
		String s = "0" + minutes;
		s = s.substring(s.length()-2);
		s = hours + ":" + s;
		return s;
	}


	/** See {@link #inmNumber}. */
	@Column(name = "INM_Number", nullable = true, length=155)
	public String getInmNumber() {
		return inmNumber;
	}
	public void setInmNumber(String inmNumber) {
		this.inmNumber = inmNumber;
	}

	/** See {@link #prodNumber}. */
	@Column(name = "Prod_Number", nullable = true, length=155)
	public String getProdNumber() {
		return prodNumber;
	}
	public void setProdNumber(String prodNumber) {
		this.prodNumber = prodNumber;
	}

	/** See {@link #producerName}. */
	@Column(name = "Producer_Name", nullable = true, length = 155)
	public String getProducerName() {
		return producerName;
	}
	public void setProducerName(String producerName) {
		this.producerName = producerName;
	}

	/** See {@link #producerAddressId}. */
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Producer_Address_Id")
	public Address getProducerAddress() {
		return producerAddress;
	}
	public void setProducerAddress(Address producerAddress) {
		this.producerAddress = producerAddress;
	}

	/** See {@link #producerPhone}. */
	@Column(name = "Producer_Phone", nullable = true, length = 25)
	public String getProducerPhone() {
		return producerPhone;
	}
	public void setProducerPhone(String producerPhone) {
		this.producerPhone = producerPhone;
	}

	/** See {@link #producerEmail}. */
	@Column(name = "Producer_Email", nullable = true, length = 155)
	public String getProducerEmail() {
		return producerEmail;
	}
	public void setProducerEmail(String producerEmail) {
		this.producerEmail = producerEmail;
	}

	/** See {@link #responsibleName}. */
	@Column(name = "Responsible_Name", nullable = true, length = 155)
	public String getResponsibleName() {
		return responsibleName;
	}
	public void setResponsibleName(String responsibleName) {
		this.responsibleName = responsibleName;
	}

	/** See {@link #producerUDA}. */
	@Column(name = "Producer_UDA", nullable = true, length = 155)
	public String getProducerUDA() {
		return producerUDA;
	}
	public void setProducerUDA(String producerUDA) {
		this.producerUDA = producerUDA;
	}

	/** See {@link #artistName}. */
	@Column(name = "Artist_Name", nullable = true, length = 155)
	public String getArtistName() {
		return artistName;
	}
	public void setArtistName(String artistName) {
		this.artistName = artistName;
	}

	/** See {@link #companyName}. */
	@Column(name = "Company_Name", nullable = true, length = 125)
	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	/** See {@link #artistAddressId}. */
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Artist_Address_Id")
	public Address getArtistAddress() {
		return artistAddress;
	}
	public void setArtistAddress(Address artistAddress) {
		this.artistAddress = artistAddress;
	}

	/** See {@link #artistPhone}. */
	@Column(name = "Artist_Phone", nullable = true, length = 25)
	public String getArtistPhone() {
		return artistPhone;
	}
	public void setArtistPhone(String artistPhone) {
		this.artistPhone = artistPhone;
	}

	/** See {@link #artistEmail}. */
	@Column(name = "Artist_Email", nullable = true, length = 155)
	public String getArtistEmail() {
		return artistEmail;
	}
	public void setArtistEmail(String artistEmail) {
		this.artistEmail = artistEmail;
	}

	/** See {@link #artistGst}. */
	@Column(name = "Artist_Gst", nullable = true, length = 10)
	public String getArtistGst() {
		return artistGst;
	}
	public void setArtistGst(String artistGst) {
		this.artistGst = artistGst;
	}

	/** See {@link #artistQst}. */
	@Column(name = "Artist_Qst", nullable = true, length = 10)
	public String getArtistQst() {
		return artistQst;
	}
	public void setArtistQst(String artistQst) {
		this.artistQst = artistQst;
	}

	/** See {@link #socailInsuranceNumber}. */
	@Column(name = "Social_Insurance_Number", nullable = true, length = 1000)
	public String getSocialInsuranceNumber() {
		return socialInsuranceNumber;
	}
	public void setSocialInsuranceNumber(String socialInsuranceNumber) {
		this.socialInsuranceNumber = socialInsuranceNumber;
	}

	/** See {@link #noUDAMember}. */
	@Column(name = "No_UDA_Member", nullable = true, length = 100)
	public String getNoUDAMember() {
		return noUDAMember;
	}
	public void setNoUDAMember(String noUDAMember) {
		this.noUDAMember = noUDAMember;
	}

	/** See {@link #dob}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "Date_of_Birth", nullable = true, length = 0)
	public Date getDob() {
		return dob;
	}
	public void setDob(Date dob) {
		this.dob = dob;
	}

	/** See {@link #advertiserName1}. */
	@Column(name = "Advertiser_Name_1", length = 155)
	public String getAdvertiserName1() {
		return advertiserName1;
	}
	public void setAdvertiserName1(String advertiserName1) {
		this.advertiserName1 = advertiserName1;
	}

	/** See {@link #advertiserName2}. */
	@Column(name = "Advertiser_Name_2", length = 155)
	public String getAdvertiserName2() {
		return advertiserName2;
	}
	public void setAdvertiserName2(String advertiserName2) {
		this.advertiserName2 = advertiserName2;
	}

	/** See {@link #advertiserName3}. */
	@Column(name = "Advertiser_Name_3", length = 155)
	public String getAdvertiserName3() {
		return advertiserName3;
	}
	public void setAdvertiserName3(String advertiserName3) {
		this.advertiserName3 = advertiserName3;
	}

	/** See {@link #productName1}. */
	@Column(name = "Product_Name_1", length = 155)
	public String getProductName1() {
		return productName1;
	}
	public void setProductName1(String productName1) {
		this.productName1 = productName1;
	}

	/** See {@link #productName2}. */
	@Column(name = "Product_Name_2", length = 155)
	public String getProductName2() {
		return productName2;
	}
	public void setProductName2(String productName2) {
		this.productName2 = productName2;
	}

	/** See {@link #productName3}. */
	@Column(name = "Product_Name_3", length = 155)
	public String getProductName3() {
		return productName3;
	}
	public void setProductName3(String productName3) {
		this.productName3 = productName3;
	}

	/** See {@link #activeMember}. */
	@Column(name = "Active_Member", nullable = true)
	public boolean isActiveMember() {
		return activeMember;
	}
	public void setActiveMember(boolean activeMember) {
		this.activeMember = activeMember;
	}

	/** See {@link #apprenticeMember}. */
	@Column(name = "Apprentice_Member", nullable = true)
	public boolean isApprenticeMember() {
		return apprenticeMember;
	}
	public void setApprenticeMember(boolean apprenticeMember) {
		this.apprenticeMember = apprenticeMember;
	}

	/** See {@link #permitHolder}. */
	@Column(name = "Permit_Holder", nullable = true)
	public boolean isPermitHolder() {
		return permitHolder;
	}
	public void setPermitHolder(boolean permitHolder) {
		this.permitHolder = permitHolder;
	}

	/** See {@link #lessThan18Years}. */
	@Column(name = "Less_Than_18_Years", nullable = true)
	public boolean isLessThan18Years() {
		return lessThan18Years;
	}
	public void setLessThan18Years(boolean lessThan18Years) {
		this.lessThan18Years = lessThan18Years;
	}

	/** See {@link #commercialTitle}. */
	@Column(name = "Commercial_Title", nullable = true, length = 100)
	public String getCommercialTitle() {
		return commercialTitle;
	}
	public void setCommercialTitle(String commercialTitle) {
		this.commercialTitle = commercialTitle;
	}

	/** See {@link #commercialDescription}. */
	@Column(name = "Commercial_Description", nullable = true, length = 100)
	public String getCommercialDescription() {
		return commercialDescription;
	}
	public void setCommercialDescription(String commercialDescription) {
		this.commercialDescription = commercialDescription;
	}

	/** See {@link #commercialVersion}. */
	@Column(name = "Commercial_Version", nullable = true, length = 100)
	public String getCommercialVersion() {
		return commercialVersion;
	}
	public void setCommercialVersion(String commercialVersion) {
		this.commercialVersion = commercialVersion;
	}

	/** See {@link #Recording1}. */
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Recording_id_1")
	public Recording getRecording1() {
		return recording1;
	}
	public void setRecording1(Recording recording1) {
		this.recording1 = recording1;
	}

	/** See {@link #Recording2}. */
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Recording_id_2")
	public Recording getRecording2() {
		return recording2;
	}
	public void setRecording2(Recording recording2) {
		this.recording2 = recording2;
	}

	/** See {@link #Recording3}. */
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Recording_id_3")
	public Recording getRecording3() {
		return recording3;
	}
	public void setRecording3(Recording recording3) {
		this.recording3 = recording3;
	}

	/** See {@link #recordingOther}. */
	@Column(name = "Recording_Other", nullable = true, length = 155)
	public String getRecordingOther() {
		return recordingOther;
	}
	public void setRecordingOther(String recordingOther) {
		this.recordingOther = recordingOther;
	}

	/** See {@link #useTableA}. */
	@Column(name = "Use_Table_A", nullable = true)
	public boolean isUseTableA() {
		return useTableA;
	}
	public void setUseTableA(boolean useTableA) {
		this.useTableA = useTableA;
	}

	/** See {@link #useTableB}. */
	@Column(name = "Use_Table_B", nullable = true)
	public boolean isUseTableB() {
		return useTableB;
	}
	public void setUseTableB(boolean useTableB) {
		this.useTableB = useTableB;
	}

	/** See {@link #useTableC}. */
	@Column(name = "Use_Table_C", nullable = true)
	public boolean isUseTableC() {
		return useTableC;
	}
	public void setUseTableC(boolean useTableC) {
		this.useTableC = useTableC;
	}

	/** See {@link #useTableD}. */
	@Column(name = "Use_Table_D", nullable = true)
	public boolean isUseTableD() {
		return useTableD;
	}
	public void setUseTableD(boolean useTableD) {
		this.useTableD = useTableD;
	}

	/** See {@link #useTableE}. */
	@Column(name = "Use_Table_E", nullable = true)
	public boolean isUseTableE() {
		return useTableE;
	}
	public void setUseTableE(boolean useTableE) {
		this.useTableE = useTableE;
	}

	/** See {@link #useTableF}. */
	@Column(name = "Use_Table_F", nullable = true)
	public boolean isUseTableF() {
		return useTableF;
	}
	public void setUseTableF(boolean useTableF) {
		this.useTableF = useTableF;
	}

	/** See {@link #period3month}. */
	@Column(name = "Period_3month", nullable = true)
	public boolean isPeriod3month() {
		return period3month;
	}

	public void setPeriod3month(boolean period3month) {
		this.period3month = period3month;
	}

	@Column(name = "Period_6month", nullable = true)
	public boolean isPeriod6month() {
		return period6month;
	}

	public void setPeriod6month(boolean period6month) {
		this.period6month = period6month;
	}

	@Column(name = "Period_9month", nullable = true)
	public boolean isPeriod9month() {
		return period9month;
	}

	public void setPeriod9month(boolean period9month) {
		this.period9month = period9month;
	}

	/** See {@link #firstUseDate}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "First_Use_Date", length = 10)
	public Date getFirstUseDate() {
		return firstUseDate;
	}
	public void setFirstUseDate(Date firstUseDate) {
		this.firstUseDate = firstUseDate;
	}

	/** See {@link #portfolioUseAuthorization}. */
	@Column(name = "Portfolio_Use_Authorization", nullable = true)
	public boolean isPortfolioUseAuthorization() {
		return portfolioUseAuthorization;
	}
	public void setPortfolioUseAuthorization(boolean portfolioUseAuthorization) {
		this.portfolioUseAuthorization = portfolioUseAuthorization;
	}

	/** See {@link #exclusivityDuration}. */
	@Column(name = "Exclusivity_Duration", nullable = true)
	public boolean getExclusivityDuration() {
		return exclusivityDuration;
	}
	public void setExclusivityDuration(boolean exclusivityDuration) {
		this.exclusivityDuration = exclusivityDuration;
	}

	/** See {@link #exclusivityDuration}. */
	@Column(name = "Exclusivity_Duration_Text", nullable = true, length = 125)
	public String getExclusivityDurationText() {
		return exclusivityDurationText;
	}
	public void setExclusivityDurationText(String exclusivityDurationText) {
		this.exclusivityDurationText = exclusivityDurationText;
	}

	/** See {@link #exclusivityService}. */
	@Column(name = "Exclusivity_Service", nullable = true, length = 125)
	public String getExclusivityService() {
		return exclusivityService;
	}
	public void setExclusivityService(String exclusivityService) {
		this.exclusivityService = exclusivityService;
	}

	/** See {@link #functionPrincipalPerformer}. */
	@Column(name = "Function_Principal_Performer", nullable = true)
	public boolean isFunctionPrincipalPerformer() {
		return functionPrincipalPerformer;
	}
	public void setFunctionPrincipalPerformer(boolean functionPrincipalPerformer) {
		this.functionPrincipalPerformer = functionPrincipalPerformer;
	}

	/** See {@link #functionChorist}. */
	@Column(name = "Function_Chorist", nullable = true)
	public boolean isFunctionChorist() {
		return functionChorist;
	}
	public void setFunctionChorist(boolean functionChorist) {
		this.functionChorist = functionChorist;
	}

	/** See {@link #functionSOCPerformer}. */
	@Column(name = "Function_SOC_Performer", nullable = true)
	public boolean isFunctionSOCPerformer() {
		return functionSOCPerformer;
	}
	public void setFunctionSOCPerformer(boolean functionSOCPerformer) {
		this.functionSOCPerformer = functionSOCPerformer;
	}

	/** See {@link #functionDemonstrator}. */
	@Column(name = "Function_Demonstrator", nullable = true)
	public boolean isFunctionDemonstrator() {
		return functionDemonstrator;
	}
	public void setFunctionDemonstrator(boolean functionDemonstrator) {
		this.functionDemonstrator = functionDemonstrator;
	}

	/** See {@link #functionVoiceOver}. */
	@Column(name = "Function_Voice_Over", nullable = true)
	public boolean isFunctionVoiceOver() {
		return functionVoiceOver;
	}
	public void setFunctionVoiceOver(boolean functionVoiceOver) {
		this.functionVoiceOver = functionVoiceOver;
	}

	/** See {@link #functionPrincipalExtra}. */
	@Column(name = "Function_Principal_Extra", nullable = true)
	public boolean isFunctionPrincipalExtra() {
		return functionPrincipalExtra;
	}
	public void setFunctionPrincipalExtra(boolean functionPrincipalExtra) {
		this.functionPrincipalExtra = functionPrincipalExtra;
	}

	/** See {@link #functionExtra}. */
	@Column(name = "Function_Extra", nullable = true)
	public boolean isFunctionExtra() {
		return functionExtra;
	}
	public void setFunctionExtra(boolean functionExtra) {
		this.functionExtra = functionExtra;
	}

	/** See {@link #functionOther}. */
	@Column(name = "Function_Other", nullable = true)
	public boolean isFunctionOther() {
		return functionOther;
	}
	public void setFunctionOther(boolean functionOther) {
		this.functionOther = functionOther;
	}

	/** See {@link #functionOtherText}. */
	@Column(name = "Function_Other_Text", precision = 6)
	public String getFunctionOtherText() {
		return functionOtherText;
	}
	public void setFunctionOtherText(String functionOtherText) {
		this.functionOtherText = functionOtherText;
	}

	/** See {@link #feeRate}. */
	@Column(name = "Fee_Rate", nullable = true, length = 155)
	public String getFeeRate() {
		return feeRate;
	}
	public void setFeeRate(String feeRate) {
		this.feeRate = feeRate;
	}

	/** See {@link #negotiableRecordingPercent}. */
	@Column(name = "Negotiable_Recording_Percent", precision = 6)
	public BigDecimal getNegotiableRecordingPercent() {
		return negotiableRecordingPercent;
	}
	public void setNegotiableRecordingPercent(BigDecimal negotiableRecordingPercent) {
		this.negotiableRecordingPercent = negotiableRecordingPercent;
	}

	/** See {@link #fieldName}. */
	@Column(name = "Field_Name", nullable = true, length = 155)
	public String getFieldName() {
		return fieldName;
	}
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	/** See {@link #negotiableRecordingAmt}. */
	@Column(name = "Negotiable_Recording_Amount", precision = 6)
	public BigDecimal getNegotiableRecordingAmt() {
		return negotiableRecordingAmt;
	}
	public void setNegotiableRecordingAmt(BigDecimal negotiableRecordingAmt) {
		this.negotiableRecordingAmt = negotiableRecordingAmt;
	}

	/** See {@link #negotiableInmUsePercent}. */
	@Column(name = "Negotiable_Inm_Use_Percent", precision = 6)
	public BigDecimal getNegotiableInmUsePercent() {
		return negotiableInmUsePercent;
	}
	public void setNegotiableInmUsePercent(BigDecimal negotiableInmUsePercent) {
		this.negotiableInmUsePercent = negotiableInmUsePercent;
	}

	/** See {@link #negotiableInmUseAmt}. */
	@Column(name = "Negotiable_Inm_Use_Amount", precision = 6)
	public BigDecimal getNegotiableInmUseAmt() {
		return negotiableInmUseAmt;
	}
	public void setNegotiableInmUseAmt(BigDecimal negotiableInmUseAmt) {
		this.negotiableInmUseAmt = negotiableInmUseAmt;
	}

	/** See {@link #negotiableOtherPercent}. */
	@Column(name = "Negotiable_Other_Percent", precision = 6)
	public BigDecimal getNegotiableOtherPercent() {
		return negotiableOtherPercent;
	}
	public void setNegotiableOtherPercent(BigDecimal negotiableOtherPercent) {
		this.negotiableOtherPercent = negotiableOtherPercent;
	}

	/** See {@link #negotiableOtherAmt}. */
	@Column(name = "Negotiable_Other_Amount", precision = 6)
	public BigDecimal getNegotiableOtherAmt() {
		return negotiableOtherAmt;
	}
	public void setNegotiableOtherAmt(BigDecimal negotiableOtherAmt) {
		this.negotiableOtherAmt = negotiableOtherAmt;
	}

	/** See {@link #specialCondition1}. */
	@Column(name = "Special_Condition_1", nullable = true, length = 155)
	public String getSpecialCondition1() {
		return specialCondition1;
	}
	public void setSpecialCondition1(String specialCondition1) {
		this.specialCondition1 = specialCondition1;
	}

	/** See {@link #specialCondition2}. */
	@Column(name = "Special_Condition_2", nullable = true, length = 155)
	public String getSpecialCondition2() {
		return specialCondition2;
	}
	public void setSpecialCondition2(String specialCondition2) {
		this.specialCondition2 = specialCondition2;
	}

	/** See {@link #specialCondition3}. */
	@Column(name = "Special_Condition_3", nullable = true, length = 155)
	public String getSpecialCondition3() {
		return specialCondition3;
	}
	public void setSpecialCondition3(String specialCondition3) {
		this.specialCondition3 = specialCondition3;
	}

	/** See {@link #negotiableDateA}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "Negotiable_Date_A", nullable = true, length = 0)
	public Date getNegotiableDateA() {
		return negotiableDateA;
	}
	public void setNegotiableDateA(Date negociableDateA) {
		this.negotiableDateA = negociableDateA;
	}

	/** See {@link #negotiableDateB}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "Negotiable_Date_B", nullable = true, length = 0)
	public Date getNegotiableDateB() {
		return negotiableDateB;
	}

	public void setNegotiableDateB(Date negotiableDateB) {
		this.negotiableDateB = negotiableDateB;
	}

	/** See {@link #weeklyTimecard}. */
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Weekly_Timecard_Id", nullable = true)
	public WeeklyTimecard getWeeklyTimecard() {
		return this.weeklyTimecard;
	}
	/** See {@link #weeklyTimecard}. */
	public void setWeeklyTimecard(WeeklyTimecard weeklyTimecard) {
		this.weeklyTimecard = weeklyTimecard;
	}

	/** See {@link #signatureDate}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "Signature_Date", nullable = true, length = 0)
	public Date getSignatureDate() {
		return signatureDate;
	}
	/** See {@link #signatureDate}. */
	public void setSignatureDate(Date signatureDate) {
		this.signatureDate = signatureDate;
	}

	/** See {@link #functionName}. */
	@Transient
	public String getFunctionName() {
		String funcName = "";
		/**LS-2879 append check box selected values */
		if(functionPrincipalPerformer) {
			/**Function Principal Performer */
			if(!funcName.isEmpty()) {
				funcName+=",";
			}
			funcName+=" Acteur Principal/ Principal Performer";
		}
		if(functionChorist) {
			/**Function Chorist*/
			if(!funcName.isEmpty()) {
				funcName+=",";
			}
			funcName+=" Choriste/Chorist";
		}
		if(functionSOCPerformer) {
			/**Function SOC Performer*/
			if(!funcName.isEmpty()) {
				funcName+=",";
			}
			funcName+=" Rôle muet/SOC Performer";
		}
		if(functionDemonstrator) {
			/**Function Demonstrator*/
			if(!funcName.isEmpty()) {
				funcName+=",";
			}
			funcName+=" Démonstrateur/Demonstrator";
		}
		if(functionVoiceOver) {
			/**Function VoiceOver*/
			if(!funcName.isEmpty()) {
				funcName+=",";
			}
			funcName+=" Voix hors champ/ Voice over";
		}	
		if(functionPrincipalExtra) {
			/**Function Principal Extra*/
			if(!funcName.isEmpty()) {
				funcName+=",";
			}
			funcName+=" Figurant principal/Principal extra";
		}
		if(functionExtra) {
			/**Function Extra*/
			if(!funcName.isEmpty()) {
				funcName+=",";
			}
			funcName+=" Figurant/Extra";
		}
		if(functionOther) {
			/**In Function Other append text value*/
			if(!funcName.isEmpty()) {
				funcName+=",";
			}
			if (functionOtherText != null) {
				funcName += " " + functionOtherText;
			}
		}
		
		return funcName;
	}
	
	@Transient
	public void setFunctionName(String functionName) {
		this.functionName = functionName;
	}

	/** See {@link #functionRate}. */
	@Column(name = "Function_Rate", precision = 6)
	public BigDecimal getFunctionRate() {
		return functionRate;
	}
	public void setFunctionRate(BigDecimal functionRate) {
		this.functionRate = functionRate;
	}

	/** See {@link #additionalAmount}. */
	@Column(name = "Additional_Amount", precision = 6)
	public BigDecimal getAdditionalAmount() {
		return additionalAmount;
	}
	public void setAdditionalAmount(BigDecimal additionalAmount) {
		this.additionalAmount = additionalAmount;
	}

	/** See {@link #otHour}. */
	@Column(name = "OT_Hour", precision = 6)
	public BigDecimal getOtHour() {
		return otHour;
	}
	public void setOtHour(BigDecimal otHour) {
		this.otHour = otHour;
	}

	/** See {@link #otHourAmount}. */
	@Column(name = "OT_Hour_Amount", precision = 6)
	public BigDecimal getOtHourAmount() {
		return otHourAmount;
	}
	public void setOtHourAmount(BigDecimal otHourAmount) {
		this.otHourAmount = otHourAmount;
	}

	/** See {@link #otHourTotal}. */
	@Column(name = "OT_Hour_Total", precision = 6)
	public BigDecimal getOtHourTotal() {
		otHourTotal = NumberUtils.safeMultiply(getOtHour(), getOtHourAmount());
		if (otHourTotal ==null || otHourTotal.compareTo(BigDecimal.ZERO) < 0) {
			otHourTotal = new BigDecimal("0.0");
		}
		return otHourTotal;
	}
	public void setOtHourTotal(BigDecimal otHourTotal) {
		this.otHourTotal = otHourTotal;
	}

	/** See {@link #addOtHour}. */
	@Column(name = "Add_OT_Hour", precision = 6)
	public BigDecimal getAddOtHour() {
		return addOtHour;
	}
	public void setAddOtHour(BigDecimal addOtHour) {
		this.addOtHour = addOtHour;
	}

	/** See {@link #addOtHourTotal}. */
	@Column(name = "Add_OT_Hour_Amount", precision = 6)
	public BigDecimal getAddOtHourAmount() {
		return addOtHourAmount;
	}
	public void setAddOtHourAmount(BigDecimal addOtHourAmount) {
		this.addOtHourAmount = addOtHourAmount;
	}

	/** See {@link #addOtHourTotal}. */
	@Column(name = "Add_OT_Hour_Total", precision = 6)
	public BigDecimal getAddOtHourTotal() {
		addOtHourTotal = NumberUtils.safeMultiply(getAddOtHour(), getAddOtHourAmount());
		if (addOtHourTotal ==null || addOtHourTotal.compareTo(BigDecimal.ZERO) < 0) {
			addOtHourTotal = new BigDecimal("0.0");
		}
		return addOtHourTotal;
	}
	public void setAddOtHourTotal(BigDecimal addOtHourTotal) {
		this.addOtHourTotal = addOtHourTotal;
	}

	/** See {@link #nightHour}. */
	@Column(name = "Night_Hour", precision = 6)
	public BigDecimal getNightHour() {
		return nightHour;
	}
	public void setNightHour(BigDecimal nightHour) {
		this.nightHour = nightHour;
	}

	/** See {@link #nightHourAmount}. */
	@Column(name = "Night_Hour_Amount", precision = 6)
	public BigDecimal getNightHourAmount() {
		return nightHourAmount;
	}
	public void setNightHourAmount(BigDecimal nightHourAmount) {
		this.nightHourAmount = nightHourAmount;
	}

	/** See {@link #nightHourTotal}. */
	@Column(name = "Night_Hour_Total", precision = 6)
	public BigDecimal getNightHourTotal() {
		nightHourTotal = NumberUtils.safeMultiply(getNightHour(), getNightHourAmount());
		if (nightHourTotal == null || nightHourTotal.compareTo(BigDecimal.ZERO) < 0) {
			nightHourTotal = new BigDecimal("0.0");
		}
		return nightHourTotal;
	}
	public void setNightHourTotal(BigDecimal nightHourTotal) {
		this.nightHourTotal = nightHourTotal;
	}

	/** See {@link #otNightHour}. */
	@Column(name = "OT_Night_Hour", precision = 6)
	public BigDecimal getOtNightHour() {
		return otNightHour;
	}
	public void setOtNightHour(BigDecimal otNightHour) {
		this.otNightHour = otNightHour;
	}

	/** See {@link #otNightHourAmount}. */
	@Column(name = "OT_Night_Hour_Amount", precision = 6)
	public BigDecimal getOtNightHourAmount() {
		return otNightHourAmount;
	}
	public void setOtNightHourAmount(BigDecimal otNightHourAmount) {
		this.otNightHourAmount = otNightHourAmount;
	}

	/** See {@link #otNightHourTotal}. */
	@Column(name = "OT_Night_Hour_Total", precision = 6)
	public BigDecimal getOtNightHourTotal() {
		otNightHourTotal = NumberUtils.safeMultiply(getOtNightHour(), getOtNightHourAmount());
		if (otNightHourTotal ==null || otNightHourTotal.compareTo(BigDecimal.ZERO) < 0) {
			otNightHourTotal = new BigDecimal("0.0");
		}
		return otNightHourTotal;
	}
	public void setOtNightHourTotal(BigDecimal otNightHourTotal) {
		this.otNightHourTotal = otNightHourTotal;
	}

	/** See {@link #holidayHour}. */
	@Column(name = "Holiday_Hour", precision = 6)
	public BigDecimal getHolidayHour() {
		return holidayHour;
	}
	public void setHolidayHour(BigDecimal holidayHour) {
		this.holidayHour = holidayHour;
	}

	/** See {@link #holidayHourAmount}. */
	@Column(name = "Holiday_Hour_Amount", precision = 6)
	public BigDecimal getHolidayHourAmount() {
		return holidayHourAmount;
	}
	public void setHolidayHourAmount(BigDecimal holidayHourAmount) {
		this.holidayHourAmount = holidayHourAmount;
	}

	/** See {@link #holidayHourTotal}. */
	@Column(name = "Holiday_Hour_Total", precision = 6)
	public BigDecimal getHolidayHourTotal() {
		holidayHourTotal = NumberUtils.safeMultiply(getHolidayHour(), getHolidayHourAmount());
		if (holidayHourTotal ==null || holidayHourTotal.compareTo(BigDecimal.ZERO) < 0) {
			holidayHourTotal = new BigDecimal("0.0");
		}
		return holidayHourTotal;
	}
	public void setHolidayHourTotal(BigDecimal holidayHourTotal) {
		this.holidayHourTotal = holidayHourTotal;
	}

	/** See {@link #travelHour}. */
	@Column(name = "Travel_Hour", precision = 6)
	public BigDecimal getTravelHour() {
		return travelHour;
	}
	public void setTravelHour(BigDecimal travelHour) {
		this.travelHour = travelHour;
	}

	/** See {@link #travelHourAmount}. */
	@Column(name = "Travel_Hour_Amount", precision = 6)
	public BigDecimal getTravelHourAmount() {
		return travelHourAmount;
	}
	public void setTravelHourAmount(BigDecimal travelHourAmount) {
		this.travelHourAmount = travelHourAmount;
	}

	/** See {@link #travelHourTotal}. */
	@Column(name = "Travel_Hour_Total", precision = 6)
	public BigDecimal getTravelHourTotal() {
		travelHourTotal = NumberUtils.safeMultiply(getTravelHour(), getTravelHourAmount());
		if (travelHourTotal ==null || travelHourTotal.compareTo(BigDecimal.ZERO) < 0) {
			travelHourTotal = new BigDecimal("0.0");
		}
		return travelHourTotal;
	}
	public void setTravelHourTotal(BigDecimal travelHourTotal) {
		this.travelHourTotal = travelHourTotal;
	}

	/** See {@link #waitingHour}. */
	@Column(name = "Waiting_Hour", precision = 6)
	public BigDecimal getWaitingHour() {
		return waitingHour;
	}
	public void setWaitingHour(BigDecimal waitingHour) {
		this.waitingHour = waitingHour;
	}

	/** See {@link #waitingHourAmount}. */
	@Column(name = "Waiting_Hour_Amount", precision = 6)
	public BigDecimal getWaitingHourAmount() {
		return waitingHourAmount;
	}
	public void setWaitingHourAmount(BigDecimal waitingHourAmount) {
		this.waitingHourAmount = waitingHourAmount;
	}

	/** See {@link #waitingHourTotal}. */
	@Column(name = "Waiting_Hour_Total", precision = 6)
	public BigDecimal getWaitingHourTotal() {
		waitingHourTotal = NumberUtils.safeMultiply(getWaitingHour(), getWaitingHourAmount());
		if (waitingHourTotal ==null || waitingHourTotal.compareTo(BigDecimal.ZERO) < 0) {
			waitingHourTotal = new BigDecimal("0.0");
		}
		return waitingHourTotal;
	}
	public void setWaitingHourTotal(BigDecimal waitingHourTotal) {
		this.waitingHourTotal = waitingHourTotal;
	}

	/** See {@link #standByDay}. */
	@Column(name = "StandBy_Day", precision = 6)
	public BigDecimal getStandByDay() {
		return standByDay;
	}
	public void setStandByDay(BigDecimal standByDay) {
		this.standByDay = standByDay;
	}

	/** See {@link #standByDayAmount}. */
	@Column(name = "StandBy_Day_Amount", precision = 6)
	public BigDecimal getStandByDayAmount() {
		return standByDayAmount;
	}
	public void setStandByDayAmount(BigDecimal standByDayAmount) {
		this.standByDayAmount = standByDayAmount;
	}

	/** See {@link #standByDayTotal}. */
	@Column(name = "StandBy_Day_Total", precision = 6)
	public BigDecimal getStandByDayTotal() {
		standByDayTotal = NumberUtils.safeMultiply(getStandByDay(), getStandByDayAmount());
		if (standByDayTotal ==null || standByDayTotal.compareTo(BigDecimal.ZERO) < 0) {
			standByDayTotal = new BigDecimal("0.0");
		}
		return standByDayTotal;
	}
	public void setStandByDayTotal(BigDecimal standByDayTotal) {
		this.standByDayTotal = standByDayTotal;
	}

	/** See {@link #lodgingMeal}. */
	@Column(name = "Lodging_Meal", nullable = true)
	public Boolean getLodgingAndMeal() {
		return lodgingAndMeal;
	}
	public void setLodgingAndMeal(Boolean lodgingAndMeal) {
		this.lodgingAndMeal = lodgingAndMeal;
	}

	/** See {@link #lodgingAndMealsNumber}. */
	@Column(name = "Lodging_Meals_Number", precision = 6)
	public BigDecimal getLodgingAndMealsNumber() {
		return lodgingAndMealsNumber;
	}
	public void setLodgingAndMealsNumber(BigDecimal lodgingAndMeals) {
		this.lodgingAndMealsNumber = lodgingAndMeals;
	}

	/** See {@link #lodgingMealsAmount}. */
	@Column(name = "Lodging_Meals_Amount", precision = 6)
	public BigDecimal getLodgingAndMealsAmount() {
		return lodgingAndMealsAmount;
	}
	public void setLodgingAndMealsAmount(BigDecimal lodgingAndMealsAmount) {
		this.lodgingAndMealsAmount = lodgingAndMealsAmount;
	}

	/** See {@link #lodgingMealsTotal}. */
	@Column(name = "Lodging_Meals_Total", precision = 6)
	public BigDecimal getLodgingAndMealsTotal() {
		lodgingAndMealsTotal = NumberUtils.safeMultiply(getLodgingAndMealsNumber(), getLodgingAndMealsAmount());
		if (lodgingAndMealsTotal ==null || lodgingAndMealsTotal.compareTo(BigDecimal.ZERO) < 0) {
			lodgingAndMealsTotal = new BigDecimal("0.0");
		}
		return lodgingAndMealsTotal;
	}
	public void setLodgingAndMealsTotal(BigDecimal lodgingAndMealsTotal) {
		this.lodgingAndMealsTotal = lodgingAndMealsTotal;
	}

	/** See {@link #mealsOnly}. */
	@Column(name = "Meals_Only", nullable = true)
	public Boolean getMealsOnly() {
		return mealsOnly;
	}
	public void setMealsOnly(Boolean mealsOnly) {
		this.mealsOnly = mealsOnly;
	}

	/** See {@link #mealOnlyNumber}. */
	@Column(name = "Meal_Only_Number", precision = 6)
	public BigDecimal getMealOnlyNumber() {
		return mealOnlyNumber;
	}
	public void setMealOnlyNumber(BigDecimal mealOnlyNumber) {
		this.mealOnlyNumber = mealOnlyNumber;
	}

	/** See {@link #mealOnlyAmount}. */
	@Column(name = "Meal_Only_Amount", precision = 6)
	public BigDecimal getMealOnlyAmount() {
		return mealOnlyAmount;
	}
	public void setMealOnlyAmount(BigDecimal mealOnlyAmount) {
		this.mealOnlyAmount = mealOnlyAmount;
	}

	/** See {@link #mealOnlyTotal}. */
	@Column(name = "Meal_Only_Total", precision = 6)
	public BigDecimal getMealOnlyTotal() {
		mealOnlyTotal = NumberUtils.safeMultiply(getMealOnlyNumber(), getMealOnlyAmount());
		if (mealOnlyTotal ==null || mealOnlyTotal.compareTo(BigDecimal.ZERO) < 0) {
			mealOnlyTotal = new BigDecimal("0.0");
		}
		return mealOnlyTotal;
	}
	public void setMealOnlyTotal(BigDecimal mealOnlyTotal) {
		this.mealOnlyTotal = mealOnlyTotal;
	}

	/** See {@link #fittingRehearsal}. */
	@Column(name = "Fitting_Rehearsal", precision = 6)
	public BigDecimal getFittingRehearsal() {
		return fittingRehearsal;
	}
	public void setFittingRehearsal(BigDecimal fittingRehearsal) {
		this.fittingRehearsal = fittingRehearsal;
	}

	/** See {@link #fittingRehearsalAmount}. */
	@Column(name = "Fitting_Rehearsal_Amount", precision = 6)
	public BigDecimal getFittingRehearsalAmount() {
		return fittingRehearsalAmount;
	}
	public void setFittingRehearsalAmount(BigDecimal fittingRehearsalAmount) {
		this.fittingRehearsalAmount = fittingRehearsalAmount;
	}

	/** See {@link #fittingRehearsalTotal}. */
	@Column(name = "Fitting_Rehearsal_Total", precision = 6)
	public BigDecimal getFittingRehearsalTotal() {
		fittingRehearsalTotal = NumberUtils.safeMultiply(getFittingRehearsal(), getFittingRehearsalAmount());
		if (fittingRehearsalTotal ==null || fittingRehearsalTotal.compareTo(BigDecimal.ZERO) < 0) {
			fittingRehearsalTotal = new BigDecimal("0.0");
		}
		return fittingRehearsalTotal;
	}
	public void setFittingRehearsalTotal(BigDecimal fittingRehearsalTotal) {
		this.fittingRehearsalTotal = fittingRehearsalTotal;
	}

	/** See {@link #mileageRate}. */
	@Column(name = "Mileage_Rate", precision = 6)
	public BigDecimal getMileageRate() {
		return mileageRate;
	}
	public void setMileageRate(BigDecimal mileageRate) {
		this.mileageRate = mileageRate;
	}

	/** See {@link #mileageRateAmount}. */
	@Column(name = "Mileage_Rate_Amount", precision = 6)
	public BigDecimal getMileageRateAmount() {
		return mileageRateAmount;
	}
	public void setMileageRateAmount(BigDecimal mileageRateAmount) {
		this.mileageRateAmount = mileageRateAmount;
	}

	/** See {@link #mileageRateTotal}. */
	@Column(name = "Mileage_Rate_Total", precision = 6)
	public BigDecimal getMileageRateTotal() {
		mileageRateTotal = NumberUtils.safeMultiply(getMileageRate(), getMileageRateAmount());
		if (mileageRateTotal ==null || mileageRateTotal.compareTo(BigDecimal.ZERO) < 0) {
			mileageRateTotal = new BigDecimal("0.0");
		}
		return mileageRateTotal;
	}
	public void setMileageRateTotal(BigDecimal mileageRateTotal) {
		this.mileageRateTotal = mileageRateTotal;
	}


	/** See {@link #callBackOnCamera}. */
	@Column(name = "Call_Back_On_Camera", precision = 6)
	public BigDecimal getCallBackOnCamera() {
		return callBackOnCamera;
	}
	public void setCallBackOnCamera(BigDecimal callBackOnCamera) {
		this.callBackOnCamera = callBackOnCamera;
	}

	/** See {@link #callBackOnCameraTotal}. */
	@Column(name = "Call_Back_On_Camera_Total", precision = 6)
	public BigDecimal getCallBackOnCameraTotal() {
		 callBackOnCameraTotal = NumberUtils.safeMultiply(new BigDecimal("4.0"), getCallBackOnCamera());
		if (callBackOnCameraTotal ==null || callBackOnCameraTotal.compareTo(BigDecimal.ZERO) < 0) {
			callBackOnCameraTotal = new BigDecimal("0.0");
		}
		return callBackOnCameraTotal;
	}
	public void setCallBackOnCameraTotal(BigDecimal callBackOnCameraTotal) {
		this.callBackOnCameraTotal = callBackOnCameraTotal;
	}

	/** See {@link #callBackOffCameraTotal}. */
	@Column(name = "Call_Back_Off_Camera", precision = 6)
	public BigDecimal getCallBackOffCamera() {
		return callBackOffCamera;
	}
	public void setCallBackOffCamera(BigDecimal callBackOffCamera) {
		this.callBackOffCamera = callBackOffCamera;
	}

	/** See {@link #callBackOnCameraTotal}. */
	@Column(name = "Call_Back_OFF_Camera_Total", precision = 6)
	public BigDecimal getCallBackOffCameraTotal() {
		callBackOffCameraTotal = NumberUtils.safeMultiply(new BigDecimal("2.0"), getCallBackOffCamera());
		if (callBackOffCameraTotal ==null || callBackOffCameraTotal.compareTo(BigDecimal.ZERO) < 0) {
			callBackOffCameraTotal = new BigDecimal("0.0");
		}
		return callBackOffCameraTotal;
	}
	public void setCallBackOffCameraTotal(BigDecimal callBackOffCameraTotal) {
		this.callBackOffCameraTotal = callBackOffCameraTotal;
	}

	/** See {@link #tagNumber}. */
	@Column(name = "Tag_Number", precision = 6)
	public Integer getTagNumber() {
		return tagNumber;
	}
	public void setTagNumber(Integer tagNumber) {
		this.tagNumber = tagNumber;
	}

	/** See {@link #tag}. */
	@Column(name = "Tag_Value", precision = 6)
	public BigDecimal getTagValue() {
		return tagValue;
	}
	public void setTagValue(BigDecimal tagValue) {
		this.tagValue = tagValue;
	}

	/** See {@link #tagTotal}. */
	@Column(name = "Tag_Total", precision = 6)
	public BigDecimal getTagTotal() {
		if (getTagNumber() != null) {
			tagTotal = NumberUtils.safeMultiply(getTagValue(), new BigDecimal(getTagNumber()));
		}
		else {
			tagTotal = getTagValue();
		}
		if (tagTotal == null || tagTotal.compareTo(BigDecimal.ZERO) < 0) {
			tagTotal = new BigDecimal("0.0");
		}
		return tagTotal;
	}
	public void setTagTotal(BigDecimal tagTotal) {
		this.tagTotal = tagTotal;
	}

	/** See {@link #others}. */
	@Column(name = "Others", length = 255)
	public String getOthers() {
		return others;
	}
	public void setOthers(String others) {
		this.others = others;
	}

	/** See {@link #othersTotal}. */
	@Column(name = "Others_Total", precision = 6)
	public BigDecimal getOthersTotal() {
		return othersTotal;
	}
	public void setOthersTotal(BigDecimal othersTotal) {
		this.othersTotal = othersTotal;
	}

	/** See {@link #grossFee}. */
	@Column(name = "Gross_Fee", precision = 6)
	public BigDecimal getGrossFee() {
		grossFee = new BigDecimal("0.0");
        
		//LS-3349 Add Function Rate
		if (getFunctionRate() != null) {
			grossFee = grossFee.add(getFunctionRate());
		}
		// Added function rate and additional amount to calculation. LS-2611
		if (getAdditionalAmount() != null) {
			grossFee = grossFee.add(getAdditionalAmount());
		}
		if (getOtHourTotal() != null) {
			grossFee = grossFee.add(getOtHourTotal());
		}
		if (getAddOtHourTotal() != null) {
			grossFee = grossFee.add(getAddOtHourTotal());
		}
		if (getNightHourTotal() != null) {
			grossFee = grossFee.add(getNightHourTotal());
		}
		if (getOtNightHourTotal() != null) {
			grossFee = grossFee.add(getOtNightHourTotal());
		}
		if (getHolidayHourTotal() != null) {
			grossFee = grossFee.add(getHolidayHourTotal());
		}
		if (getTravelHourTotal() != null) {
			grossFee = grossFee.add(getTravelHourTotal());
		}
		if (getWaitingHourTotal() != null) {
			grossFee = grossFee.add(getWaitingHourTotal());
		}
		if (getStandByDayTotal() != null) {
			grossFee = grossFee.add(getStandByDayTotal());
		}
		if (getLodgingAndMealsTotal() != null) {
			grossFee = grossFee.add(getLodgingAndMealsTotal());
		}
		if (getMealOnlyTotal() != null) {
			grossFee = grossFee.add(getMealOnlyTotal());
		}
		if (getFittingRehearsalTotal() != null) {
			grossFee = grossFee.add(getFittingRehearsalTotal());
		}
		// Changed to use mileageRateTotal instead of mileageRateAmount. LS-2611
		if (getMileageRateAmount() != null) {
			grossFee = grossFee.add(getMileageRateTotal());
		}
		if (getCallBackOnCameraTotal() != null) {
			grossFee = grossFee.add(getCallBackOnCameraTotal());
		}
		if (getCallBackOffCameraTotal() != null) {
			grossFee = grossFee.add(getCallBackOffCameraTotal());
		}
		if (getTagTotal() != null) {
			grossFee = grossFee.add(getTagTotal());
		}
		if (getOthersTotal() != null) {
			grossFee = grossFee.add(getOthersTotal());
		}
		if (grossFee.compareTo(BigDecimal.ZERO) < 0) {
			grossFee = new BigDecimal("0.0");
		}
		return grossFee;
	}

	public void setGrossFee(BigDecimal grossFee) {
		this.grossFee = grossFee;
	}

	/** See {@link #apprentice}. */
	@Column(name = "Apprentice", precision = 6)
	public BigDecimal getApprentice() {
		return apprentice;
	}
	public void setApprentice(BigDecimal apprentice) {
		this.apprentice = apprentice;
	}

	/** See {@link #permitHolderNumber}. */
	@Column(name = "Permit_Holder_Number", precision = 6)
	public BigDecimal getPermitHolderNumber() {
		return permitHolderNumber;
	}

	public void setPermitHolderNumber(BigDecimal permitHolderNumber) {
		this.permitHolderNumber = permitHolderNumber;
	}

	/** See {@link #permitNumber}. */
	@Column(name = "Permit_Number", precision = 6)
	public BigDecimal getPermitNumber() {
		return permitNumber;
	}
	public void setPermitNumber(BigDecimal permitNumber) {
		this.permitNumber = permitNumber;
	}

	/** See {@link #permitAmount}. */
	@Column(name = "Permit_Amount", precision = 6)
	public BigDecimal getPermitAmount() {
		return permitAmount;
	}
	public void setPermitAmount(BigDecimal permitAmount) {
		this.permitAmount = permitAmount;
	}

	/** See {@link #permitTotal}. */
	@Column(name = "Permit_Total", precision = 6)
	public BigDecimal getPermitTotal() {
		permitTotal = NumberUtils.safeMultiply(getPermitNumber(), getPermitAmount());
		if (permitTotal ==null || permitTotal.compareTo(BigDecimal.ZERO) < 0) {
			permitTotal = new BigDecimal("0.0");
		}
		return permitTotal;
	}
	public void setPermitTotal(BigDecimal permitsTotal) {
		this.permitTotal = permitsTotal;
	}

	/** See {@link #minorityAgedArtistFun}. */
	@Column(name = "Minority_Aged_Artists_Fund", precision = 6)
	public BigDecimal getMinorityAgedArtistsFund() {
		return minorityAgedArtistsFund;
	}
	public void setMinorityAgedArtistsFund(BigDecimal minorityAgedArtistsFund) {
		this.minorityAgedArtistsFund = minorityAgedArtistsFund;
	}

	/** See {@link #feeBeforeTaxes}. */
	@Column(name = "Fee_Before_Taxes", precision = 6)
	public BigDecimal getFeeBeforeTaxes() {
		feeBeforeTaxes = new BigDecimal("0.0");
		if (getGrossFee() != null) {
			feeBeforeTaxes = getGrossFee();
		}
		if (getApprentice() != null) {
			feeBeforeTaxes = feeBeforeTaxes.subtract(getApprentice());
		}
		// Changed to use permitHolderNumber instead of permitHolder. LS-2611
		if (getPermitHolderNumber() != null) {
			feeBeforeTaxes = feeBeforeTaxes.subtract(getPermitHolderNumber());
		}
		if (getPermitTotal() != null) {
			feeBeforeTaxes = feeBeforeTaxes.subtract(getPermitTotal());
		}
		if (getMinorityAgedArtistsFund() != null) {
			feeBeforeTaxes = feeBeforeTaxes.subtract(getMinorityAgedArtistsFund());
		}
		if (feeBeforeTaxes.compareTo(BigDecimal.ZERO) < 0) {
			feeBeforeTaxes = new BigDecimal("0.0");
		}
		return feeBeforeTaxes;
	}
	public void setFeeBeforeTaxes(BigDecimal feeBeforeTax) {
		this.feeBeforeTaxes = feeBeforeTax;
	}

	/** See {@link #producerShare}. */
	@Column(name = "Producer_Share", precision = 6)
	public BigDecimal getProducerShare() {
		return producerShare;
	}
	public void setProducerShare(BigDecimal producerShare) {
		this.producerShare = producerShare;
	}

	/** See {@link #totalAmountT0CSA}. */
	@Column(name = "Total_Amount_To_CSA", precision = 6)
	public BigDecimal getTotalAmountToCSA() {
		totalAmountToCSA = new BigDecimal("0.0");
		if (getGrossFee() != null) {
			totalAmountToCSA = getGrossFee();
		}
		if (getApprentice() != null) {
			totalAmountToCSA = totalAmountToCSA.add(getApprentice());
		}
		// Changed to use permitHolderNumber instead of permitHolder. LS-2611
		if (getPermitHolderNumber() != null) {
			totalAmountToCSA = totalAmountToCSA.add(getPermitHolderNumber());
		}
		if (getPermitTotal() != null && !getPermitTotal().equals(new BigDecimal("0.0"))) {
			totalAmountToCSA = totalAmountToCSA.add(getPermitTotal());
		}
		if (getMinorityAgedArtistsFund() != null) {
			totalAmountToCSA = totalAmountToCSA.add(getMinorityAgedArtistsFund());
		}
		if (getProducerShare() != null) {
			totalAmountToCSA = totalAmountToCSA.add(getProducerShare());
		}
		if (totalAmountToCSA.intValue() < 0) {
			totalAmountToCSA = new BigDecimal("0.0");
		}
		return totalAmountToCSA;
	}
	public void setTotalAmountToCSA(BigDecimal totalAmountToCSA) {
		this.totalAmountToCSA = totalAmountToCSA;
	}

	/** See {@link #gst}. */
	@Column(name = "Gst", precision = 6)
	public BigDecimal getGst() {
		return gst;
	}
	public void setGst(BigDecimal gst) {
		this.gst = gst;
	}

	/** See {@link #qst}. */
	@Column(name = "Qst", precision = 6)
	public BigDecimal getQst() {
		return qst;
	}
	public void setqst(BigDecimal qst) {
		this.qst = qst;
	}

	/** See {@link #netFee}. */
	@Column(name = "Net_Fee", precision = 6)
	public BigDecimal getNetFee() {
		netFee = new BigDecimal("0.0");
		if (getFeeBeforeTaxes() != null) {
			netFee = netFee.add(getFeeBeforeTaxes());
		}
		if (getGst() != null) {
			netFee = netFee.subtract(getGst());
		}
		if (getQst() != null) {
			netFee = netFee.subtract(getQst());
		}
		return netFee;
	}
	public void setNetFee(BigDecimal netFee) {
		this.netFee = netFee;
	}

	/** See {@link #office}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Office_Id")
	public Office getOffice() {
		return office;
	}

	/** See {@link #office}. */
	public void setOffice(Office office) {
		this.office = office;
	}
}
