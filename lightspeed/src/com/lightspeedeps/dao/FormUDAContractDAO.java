package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.FormUDAContract;

/**
 * A data access object (DAO) providing persistence and search support for
 * FormA1Contract entities. Transaction control of the save(), update() and
 * delete() operations can directly support Spring container-managed
 * transactions or they can be augmented to handle user-managed Spring
 * transactions. Each of these methods provides additional information for how
 * to configure it for the desired type of transaction control.
 *
 * @see com.lightspeedeps.model.FormUDAContract
 * @author MyEclipse Persistence Tools
 */
public class FormUDAContractDAO extends BaseTypeDAO<FormUDAContract> {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(FormUDAContractDAO.class);

	// property constants
//	public static final String VERSION = "version";
//	public static final String INM_NUMBER = "inmNumber";
//	public static final String PROD_NUMBER = "prodNumber";
//	public static final String PRODUCER_NAME = "producerName";
//	public static final String PRODUCER_ADDRESS = "producerAddress";
//	public static final String PRODUCER_PHONE = "producerPhone";
//	public static final String PRODUCER_EMAIL = "producerEmail";
//	public static final String RESPONSIBLE_NAME = "responsibleName";
//	public static final String PRODUCER_UDA = "producerUDA";
//	public static final String ADVERTISER_NAME = "advertiserName";
//	public static final String PRODUCT_NAME = "productName";
//	public static final String ARTIST_NAME = "artistName";
//	public static final String COMPANY_NAME = "companyName";
//	public static final String ARTIST_ADDRESS = "artistAddress";
//	public static final String ARTIST_PHONE = "artistPhone";
//	public static final String ARTIST_EMAIL = "artistEmail";
//	public static final String ADVERTISER_NAME_1 = "advertiserName1";
//	public static final String ADVERTISER_NAME_2 = "advertiserName2";
//	public static final String ADVERTISER_NAME_3 = "advertiserName3";
//	public static final String PRODUCT_NAME_1 = "productName1";
//	public static final String PRODUCT_NAME_2 = "productName2";
//	public static final String PRODUCT_NAME_3 = "productName3";
//	public static final String ARTIST_GST = "artistGst";
//	public static final String ARTIST_QST = "artistQst";
//	public static final String SOCIAL_INSURANCE = "socialInsuranceNumber";
//	public static final String NO_UDA_MEMBER = "noUDAMember";
//	public static final String DATE_OF_BIRTH = "dob";
//	public static final String ACTIVE_MEMBER = "activeMember";
//	public static final String APPRENTICE_MEMBER = "apprenticeMember";
//	public static final String PERMIT_HOLDER = "permitHolder";
//	public static final String LESS_THAN_18YEARS = "lessThan18Years";
//	public static final String COMMERCIAL_TITLE = "commercialTitle";
//	public static final String COMMERCIAL_VERSION = "commercialVersion";
//	public static final String RECORDING1 = "recording1";
//	public static final String RECORDING2 = "recording2";
//	public static final String RECORDING3 = "recording3";
//	public static final String RECORDING_OTHER = "recordingOther";
//	public static final String USE_TABLE_A = "useTableA";
//	public static final String USE_TABLE_B = "useTableB";
//	public static final String USE_TABLE_C = "useTableC";
//	public static final String USE_TABLE_D = "useTableD";
//	public static final String USE_TABLE_E = "useTableE";
//	public static final String USE_TABLE_F = "useTableF";
//	public static final String PEROID = "period";
//	public static final String FIRST_USE_DATE = "firstUseDate";
//	public static final String PROTFOLIO_USE_AUTHORIZATION = "portfolioUseAuthorization";
//	public static final String EXCLUSIVITY_DURATION = "exclusivityDuration";
//	public static final String EXCLUSIVITY_DURATION_TEXT = "exclusivityDurationText";
//	public static final String EXCLUSIVITY_SERVICE = "exclusivityService";
//	public static final String FUNCTION_PRINCIPAL_PERFORMER = "functionPrincipalPerformer";
//	public static final String FUNCTION_CHORIST = "functionChorist";
//	public static final String FUNCTION_SOC_PERFORMER = "functionSOCPerformer";
//	public static final String FUNCTION_DEMOSTRATOR = "functionDemonstrator";
//	public static final String FUNCTION_VOICE_OVER = "functionVoiceOver";
//	public static final String FUNCTION_PRINCIPAL_EXTRA = "functionPrincipalExtra";
//	public static final String FUNCTION_EXTRA = "functionExtra";
//	public static final String FUNCTION_OTHER = "functionOther";
//	public static final String FUNCTION_OTHER_TEXT = "functionOtherText";
//	public static final String FEE_RATE = "feeRate";
//	public static final String NEGOTIABLE_RECORDING_PERCENT = "negotiableRecordingPercent";
//	public static final String FIELD_NAME = "fieldName";
//	public static final String SPECIAL_CONDITION_1 = "specialCondition1";
//	public static final String NEGOTIABLE_INM_USE_PERCENT = "negotiableInmUsePercent";
//	public static final String NEGOTIABLE_INM_USE_AMOUNT = "negotiableInmUseAmt";
//	public static final String NEGOTIABLE_RECORDING_AMOUNT = "negotiableRecordingAmt";
//	public static final String SPECIAL_CONDITION_2 = "specialCondition2";
//	public static final String NEGOTIABLE_OTHER_PERCENT = "negotiableOtherPercent";
//	public static final String NEGOTIABLE_OTHER_AMOUNT = "negotiableOtherAmt";
//	public static final String SPECIAL_CONDITION_3 = "specialCondition3";
//	public static final String NEGOTIABLE_DATE_A = "negotiableDateA";
//	public static final String NEGOTIABLE_DATE_B = "negotiableDateB";
//	public static final String WEEKLY_TIME_CARD = "weeklyTimecard";
//	public static final String FUNCTION_NAME = "functionName";
//	public static final String FUNCTION_RATE = "functionRate";
//	public static final String ADDITIONAL_AMOUNT = "additionalAmount";
//	public static final String OT_HOUR = "otHour";
//	public static final String OT_HOUR_AMOUNT = "otHourAmount";
//	public static final String OT_HOUR_TOTAL = "otHourTotal";
//	public static final String ADD_OT_HOUR = "addOtHour";
//	public static final String ADD_OT_HOUR_AMOUNT = "addOtHourAmount";
//	public static final String ADD_OT_HOUR_TOTAL = "addOtHourTotal";
//	public static final String NIGHT_HOUR = "nightHour";
//	public static final String NIGHT_HOUR_AMOUNT = "nightHourAmount";
//	public static final String NIGHT_HOUR_TOTAL = "nightHourTotal";
//	public static final String OT_NIGHT_HOUR = "otNightHour";
//	public static final String OT_NIGHT_HOUR_AMOUNT = "otNightHourAmount";
//	public static final String OT_NIGHT_HOUR_TOTAL = "otNightHourTotal";
//	public static final String HOLIDAY_HOUR = "holidayHour";
//	public static final String HOLIDAY_HOUR_AMOUNT = "holidayHourAmount";
//	public static final String HOLIDAY_HOUR_TOTAL = "holidayHourTotal";
//	public static final String TRAVEL_HOUR = "travelHour";
//	public static final String TRAVEL_HOUR_AMOUNT = "travelHourAmount";
//	public static final String TRAVEL_HOUR_TOTAL = "travelHourTotal";
//	public static final String WAITING_HOUR = "waitingHour";
//	public static final String WAITING_HOUR_AMOUNT = "waitingHourAmount";
//	public static final String WAITING_HOUR_TOTAL = "waitingHourTotal";
//	public static final String STAND_BY_DAY = "standByDay";
//	public static final String STAND_BY_DAY_AMOUNT = "standByDayAmount";
//	public static final String STAND_BY_DAY_TOTAL = "standByDayTotal";
//	public static final String LODGING_MEAL = "lodgingAndMeal";
//	public static final String LODGING_MEALS_NUMBER = "lodgingAndMealsNumber";
//	public static final String LODGING_MEALS_AMOUNT = "lodgingAndMealsAmount";
//	public static final String LODGING_MEALS_TOTAL = "lodgingAndMealsTotal";
//	public static final String MEALS_ONLY = "mealsonly";
//	public static final String MEAL_ONLY_NUMBER = "mealOnlyNumber";
//	public static final String MEAL_ONLY_AMOUNT = "mealOnlyAmount";
//	public static final String MEAL_ONLY_TOTAL = "mealOnlyTotal";
//	public static final String FITTING_REHEARSAL = "fittingRehearsal";
//	public static final String FITTING_REHEARSAL_AMOUNT = "fittingRehearsalAmount";
//	public static final String FITTING_REHEARSAL_TOTAL = "fittingRehearsalTotal";
//	public static final String MILEAGE_RATE = "mileageRate";
//	public static final String MILEAGE_RATE_AMOUNT = "mileageRateAmount";
//	public static final String MILEAGE_RATE_AMOUNT_TOTAL = "mileageRateTotal";
//	public static final String CALL_BACK_ON_CAMERA = "callBackOnCamera";
//	public static final String CALL_BACK_ON_CAMERA_TOTAL = "callBackOnCameraTotal";
//	public static final String CALL_BACK_OFF_CAMERA = "callBackOffCamera";
//	public static final String CALL_BACK_OFF_CAMERA_TOTAL = "callBackOffCameraTotal";
//	public static final String TAG_NUMBER = "tagNumber";
//	public static final String TAG_TOTAL = "tagTotal";
//	public static final String TAG_VALUE = "tagValue";
//	public static final String OTHERS = "others";
//	public static final String OTHERS_TOTAL = "othersTotal";
//	public static final String GROSS_FEE = "grossFee";
//	public static final String APPRENTICE = "apprentice";
//	public static final String PERMIT_HOLDER_NUMBER = "permitHolderNumber";
//	public static final String PERMIT_NUMBER = "permitNumber";
//	public static final String PERMITS_TOTAL = "permitTotal";
//	public static final String PERMIT_AMOUNT = "permitAmount";
//	public static final String MINORITY_AGED_ARTISTS_FUND = "minorityAgedArtistsFund";
//	public static final String FEE_BEFORE_TAXES = "feeBeforeTaxes";
//	public static final String GST = "gst";
//	public static final String QST = "qst";
//	public static final String NET_FEE = "netFee";
//	public static final String PRODUCER_SHARE = "producerShare";
//	public static final String TOTAL_AMOUNT_TO_CSA = "totalAmountToCSA";


	public static FormUDAContractDAO getInstance() {
		return (FormUDAContractDAO) getInstance("FormUDAContractDAO");
	}

}
