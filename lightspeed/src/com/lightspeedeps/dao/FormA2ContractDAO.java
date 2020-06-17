package com.lightspeedeps.dao;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.FormA2Contract;
import com.lightspeedeps.util.app.ServiceFinder;

/**
 * A data access object (DAO) providing persistence and search support for
 * FormA2Contract entities. Transaction control of the save(), update() and
 * delete() operations can directly support Spring container-managed
 * transactions or they can be augmented to handle user-managed Spring
 * transactions. Each of these methods provides additional information for how
 * to configure it for the desired type of transaction control.
 * 
 * @see com.lightspeedeps.model.FormA2Contract
 * @author MyEclipse Persistence Tools
 */
public class FormA2ContractDAO extends BaseTypeDAO< FormA2Contract> {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(FormA2ContractDAO.class);
	
	//property constants
	public static final String VERSION = "version";
	public static final String WARDROBE_PROVIDED_BY = "wardrobeProvidedBy";
	public static final String SPECIAL_PROVISIONS = "specialProvisions";
	public static final String SPECIAL_PROVISIONS_SIGN_ID = "specialProvisionsSignId";
	public static final String MINOR_CONSENT_SIGNER = "minorConsentSigner";
	public static final String MINOR_CONSENT_SIGN_ID = "minorConsentSignId";
	public static final String COMPENSATION = "compensation";
	public static final String TALENT_FIRST_LAST_NAME = "talentFirstLastName";
	public static final String TALENT_FIRST_NAME = "talentFirstName";
	public static final String TALENT_LAST_NAME = "talentLastName";
	public static final String TALENT_PHONE = "talentPhone";
	public static final String TALENT_EMAIL_ADDRESS = "talentEmailAddress";
	public static final String TALENT_PRIMARY_ADDRESS_ID = "talentPrimaryAddressId";
	public static final String TALENT_CONAME = "talentCOName";
	public static final String TALENT_COADDRESS_ID = "talentCOAddressId";
	public static final String TALENT_PAY_TO = "talentPayTo";
	public static final String TALENT_SIGN_ID = "talentSignId";
	public static final String ESTIMATE_NUM = "estimateNum";
	public static final String JOB_NUM = "jobNum";
	public static final String AGENCY_NAME = "agencyName";
	public static final String AGENCY_ADDRESS_ID = "agencyAddressId";
	public static final String AGENCY_EMAIL_ADDRESS = "agencyEmailAddress";
	public static final String PRODUCER_NAME = "producerName";
	public static final String PRODUCER_SIGN_NAME = "producerSignName";
	public static final String PRODUCER_SIGN_ID = "producerSignId";
	public static final String PROD_COMPANY_NAME = "prodCompanyName";
	public static final String ADVERTISER_NAME = "advertiserName";
	public static final String ADVERTISER_PRODUCTS = "advertiserProducts";
	public static final String NON_EVENING_WEAR_CNT = "nonEveningWearCnt";
	public static final String EVENING_WEAR_CNT = "eveningWearCnt";
	public static final String TOTAL_WARDROBE_FEE = "totalWardrobeFee";
	public static final String FLIGHT_INSURANCE_DAYS = "flightInsuranceDays";
	public static final String PLACE_ENGAGEMENT = "placeEngagement";
	public static final String DATE_HOUR_ENGAGEMENT = "dateHourEngagement";
	public static final String SERVICES_RENDERED_ADDRESS_ID = "servicesRenderedAddressId";
	public static final String PART_PLAYED = "partPlayed";
	public static final String USE_UNLIMITED = "useUnlimited";
	public static final String USE13_WEEKS = "use13Weeks";
	public static final String USE_CABLE_ONLY = "useCableOnly";
	public static final String USE_INTERNET_ONLY = "useInternetOnly";
	public static final String USE_NEW_MEDIA_ONLY = "useNewMediaOnly";
	public static final String USE_SOCIAL_MEDIA_ONLY = "useSocialMediaOnly";
	public static final String TALENT_AGENCY_NAME = "talentAgencyName";
	public static final String AGENT_COMMISSION = "agentCommission";
	public static final String TRAVEL_WITHIN_ZONE_DAYS = "travelWithinZoneDays";
	public static final String MOPED_DAY_CNT = "mopedDayCnt";
	public static final String MOPED_TYPE = "mopedType";
	public static final String MOPED_TOLLS_AMT = "mopedTollsAmt";
	public static final String MOPED_MILEAGE_AMT = "mopedMileageAmt";
	public static final String MOPED_PARKING_AMT = "mopedParkingAmt";
	public static final String AUTO_DAY_CNT = "autoDayCnt";
	public static final String AUTO_TYPE = "autoType";
	public static final String AUTO_TOLLS_AMT = "autoTollsAmt";
	public static final String AUTO_MILEAGE_AMT = "autoMileageAmt";
	public static final String AUTO_PARKING_AMT = "autoParkingAmt";

	public static FormA2ContractDAO getInstance() {
		return (FormA2ContractDAO)ServiceFinder.findBean("FormA2ContractDAO");
	}

	public List<FormA2Contract> findByWardrobeProvidedBy(Object wardrobeProvidedBy) {
		return findByProperty(WARDROBE_PROVIDED_BY, wardrobeProvidedBy);
	}

	public List<FormA2Contract> findBySpecialProvisions(Object specialProvisions) {
		return findByProperty(SPECIAL_PROVISIONS, specialProvisions);
	}

	public List<FormA2Contract> findBySpecialProvisionsSignId(Object specialProvisionsSignId) {
		return findByProperty(SPECIAL_PROVISIONS_SIGN_ID, specialProvisionsSignId);
	}

	public List<FormA2Contract> findByMinorConsentSigner(Object minorConsentSigner) {
		return findByProperty(MINOR_CONSENT_SIGNER, minorConsentSigner);
	}

	public List<FormA2Contract> findByMinorConsentSignId(Object minorConsentSignId) {
		return findByProperty(MINOR_CONSENT_SIGN_ID, minorConsentSignId);
	}

	public List<FormA2Contract> findByCompensation(Object compensation) {
		return findByProperty(COMPENSATION, compensation);
	}

	public List<FormA2Contract> findByTalentFirstLastName(Object talentFirstLastName) {
		return findByProperty(TALENT_FIRST_LAST_NAME, talentFirstLastName);
	}

	public List<FormA2Contract> findByTalentFirstName(Object talentFirstName) {
		return findByProperty(TALENT_FIRST_NAME, talentFirstName);
	}

	public List<FormA2Contract> findByTalentLastName(Object talentLastName) {
		return findByProperty(TALENT_LAST_NAME, talentLastName);
	}

	public List<FormA2Contract> findByTalentPhone(Object talentPhone) {
		return findByProperty(TALENT_PHONE, talentPhone);
	}

	public List<FormA2Contract> findByTalentEmailAddress(Object talentEmailAddress) {
		return findByProperty(TALENT_EMAIL_ADDRESS, talentEmailAddress);
	}

	public List<FormA2Contract> findByTalentPrimaryAddressId(Object talentPrimaryAddressId) {
		return findByProperty(TALENT_PRIMARY_ADDRESS_ID, talentPrimaryAddressId);
	}

	public List<FormA2Contract> findByTalentCOName(Object talentCOName) {
		return findByProperty(TALENT_CONAME, talentCOName);
	}

	public List<FormA2Contract> findByTalentCOAddressId(Object talentCOAddressId) {
		return findByProperty(TALENT_COADDRESS_ID, talentCOAddressId);
	}

	public List<FormA2Contract> findByTalentPayTo(Object talentPayTo) {
		return findByProperty(TALENT_PAY_TO, talentPayTo);
	}

	public List<FormA2Contract> findByTalentSignId(Object talentSignId) {
		return findByProperty(TALENT_SIGN_ID, talentSignId);
	}

	public List<FormA2Contract> findByEstimateNum(Object estimateNum) {
		return findByProperty(ESTIMATE_NUM, estimateNum);
	}

	public List<FormA2Contract> findByJobNum(Object jobNum) {
		return findByProperty(JOB_NUM, jobNum);
	}

	public List<FormA2Contract> findByAgencyName(Object agencyName) {
		return findByProperty(AGENCY_NAME, agencyName);
	}

	public List<FormA2Contract> findByAgencyAddressId(Object agencyAddressId) {
		return findByProperty(AGENCY_ADDRESS_ID, agencyAddressId);
	}

	public List<FormA2Contract> findByAgencyEmailAddress(Object agencyEmailAddress) {
		return findByProperty(AGENCY_EMAIL_ADDRESS, agencyEmailAddress);
	}

	public List<FormA2Contract> findByProducerName(Object producerName) {
		return findByProperty(PRODUCER_NAME, producerName);
	}

	public List<FormA2Contract> findByProducerSignName(Object producerSignName) {
		return findByProperty(PRODUCER_SIGN_NAME, producerSignName);
	}

	public List<FormA2Contract> findByProducerSignId(Object producerSignId) {
		return findByProperty(PRODUCER_SIGN_ID, producerSignId);
	}

	public List<FormA2Contract> findByProdCompanyName(Object prodCompanyName) {
		return findByProperty(PROD_COMPANY_NAME, prodCompanyName);
	}

	public List<FormA2Contract> findByAdvertiserName(Object advertiserName) {
		return findByProperty(ADVERTISER_NAME, advertiserName);
	}

	public List<FormA2Contract> findByAdvertiserProducts(Object advertiserProducts) {
		return findByProperty(ADVERTISER_PRODUCTS, advertiserProducts);
	}

	public List<FormA2Contract> findByNonEveningWearCnt(Object nonEveningWearCnt) {
		return findByProperty(NON_EVENING_WEAR_CNT, nonEveningWearCnt);
	}

	public List<FormA2Contract> findByEveningWearCnt(Object eveningWearCnt) {
		return findByProperty(EVENING_WEAR_CNT, eveningWearCnt);
	}

	public List<FormA2Contract> findByTotalWardrobeFee(Object totalWardrobeFee) {
		return findByProperty(TOTAL_WARDROBE_FEE, totalWardrobeFee);
	}

	public List<FormA2Contract> findByFlightInsuranceDays(Object flightInsuranceDays) {
		return findByProperty(FLIGHT_INSURANCE_DAYS, flightInsuranceDays);
	}

	public List<FormA2Contract> findByPlaceEngagement(Object placeEngagement) {
		return findByProperty(PLACE_ENGAGEMENT, placeEngagement);
	}

	public List<FormA2Contract> findByDateHourEngagement(Object dateHourEngagement) {
		return findByProperty(DATE_HOUR_ENGAGEMENT, dateHourEngagement);
	}

	public List<FormA2Contract> findByServicesRenderedAddressId(Object servicesRenderedAddressId) {
		return findByProperty(SERVICES_RENDERED_ADDRESS_ID, servicesRenderedAddressId);
	}

	public List<FormA2Contract> findByPartPlayed(Object partPlayed) {
		return findByProperty(PART_PLAYED, partPlayed);
	}

	public List<FormA2Contract> findByUseUnlimited(Object useUnlimited) {
		return findByProperty(USE_UNLIMITED, useUnlimited);
	}

	public List<FormA2Contract> findByUse13Weeks(Object use13Weeks) {
		return findByProperty(USE13_WEEKS, use13Weeks);
	}

	public List<FormA2Contract> findByUseCableOnly(Object useCableOnly) {
		return findByProperty(USE_CABLE_ONLY, useCableOnly);
	}

	public List<FormA2Contract> findByUseInternetOnly(Object useInternetOnly) {
		return findByProperty(USE_INTERNET_ONLY, useInternetOnly);
	}

	public List<FormA2Contract> findByUseNewMediaOnly(Object useNewMediaOnly) {
		return findByProperty(USE_NEW_MEDIA_ONLY, useNewMediaOnly);
	}

	public List<FormA2Contract> findByUseSocialMediaOnly(Object useSocialMediaOnly) {
		return findByProperty(USE_SOCIAL_MEDIA_ONLY, useSocialMediaOnly);
	}

	public List<FormA2Contract> findByTalentAgencyName(Object talentAgencyName) {
		return findByProperty(TALENT_AGENCY_NAME, talentAgencyName);
	}

	public List<FormA2Contract> findByAgentCommission(Object agentCommission) {
		return findByProperty(AGENT_COMMISSION, agentCommission);
	}

	public List<FormA2Contract> findByTravelWithinZoneDays(Object travelWithinZoneDays) {
		return findByProperty(TRAVEL_WITHIN_ZONE_DAYS, travelWithinZoneDays);
	}

	public List<FormA2Contract> findByMopedDayCnt(Object mopedDayCnt) {
		return findByProperty(MOPED_DAY_CNT, mopedDayCnt);
	}

	public List<FormA2Contract> findByMopedType(Object mopedType) {
		return findByProperty(MOPED_TYPE, mopedType);
	}

	public List<FormA2Contract> findByMopedTollsAmt(Object mopedTollsAmt) {
		return findByProperty(MOPED_TOLLS_AMT, mopedTollsAmt);
	}

	public List<FormA2Contract> findByMopedMileageAmt(Object mopedMileageAmt) {
		return findByProperty(MOPED_MILEAGE_AMT, mopedMileageAmt);
	}

	public List<FormA2Contract> findByMopedParkingAmt(Object mopedParkingAmt) {
		return findByProperty(MOPED_PARKING_AMT, mopedParkingAmt);
	}

	public List<FormA2Contract> findByAutoDayCnt(Object autoDayCnt) {
		return findByProperty(AUTO_DAY_CNT, autoDayCnt);
	}

	public List<FormA2Contract> findByAutoType(Object autoType) {
		return findByProperty(AUTO_TYPE, autoType);
	}

	public List<FormA2Contract> findByAutoTollsAmt(Object autoTollsAmt) {
		return findByProperty(AUTO_TOLLS_AMT, autoTollsAmt);
	}

	public List<FormA2Contract> findByAutoMileageAmt(Object autoMileageAmt) {
		return findByProperty(AUTO_MILEAGE_AMT, autoMileageAmt);
	}

	public List<FormA2Contract> findByAutoParkingAmt(Object autoParkingAmt) {
		return findByProperty(AUTO_PARKING_AMT, autoParkingAmt);
	}
}
