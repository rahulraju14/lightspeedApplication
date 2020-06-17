package com.lightspeedeps.dao;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.FormA1Contract;
import com.lightspeedeps.util.app.ServiceFinder;

/**
 * A data access object (DAO) providing persistence and search support for
 * FormA1Contract entities. Transaction control of the save(), update() and
 * delete() operations can directly support Spring container-managed
 * transactions or they can be augmented to handle user-managed Spring
 * transactions. Each of these methods provides additional information for how
 * to configure it for the desired type of transaction control.
 * 
 * @see com.lightspeedeps.model.FormA1Contract
 * @author MyEclipse Persistence Tools
 */
public class FormA1ContractDAO extends BaseTypeDAO< FormA1Contract> {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(FormA1ContractDAO.class);
	
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
	public static final String MULTI_TRACK = "multiTrack";
	public static final String SWEETENING_TRACK_COUNT = "sweeteningTrackCount";
	public static final String DANCER_FOOTWEAR_CNT = "dancerFootwearCnt";
	public static final String TOTAL_DANCER_FOOTWEAR_FEE = "totalDancerFootwearFee";
	public static final String DEALER_COM_TYPE_A = "dealerComTypeA";
	public static final String DEALER_COM_TYPE_B = "dealerComTypeB";
	public static final String SEASONAL_COM = "seasonalCom";
	public static final String TEST_MARKET_COM = "testMarketCom";
	public static final String NON_AIR_COM = "nonAirCom";
	public static final String CABLE_ONLY_COM = "cableOnlyCom";
	public static final String INTERNET_ONLY_COM = "internetOnlyCom";
	public static final String NEW_MEDIA_COM = "newMediaCom";
	public static final String SOCIAL_MEDIA_COM = "socialMediaCom";
	public static final String WORK_IN_SMOKE = "workInSmoke";
	public static final String LANG_TRANS_SERVICE = "langTransService";
	public static final String OTHER_RESIDUAL = "otherResidual";
	public static final String OTHER_RESIDUAL_TEXT = "otherResidualText";
	public static final String CONSENT_INTERNET = "consentInternet";
	public static final String CONSENT_NEW_MEDIA = "consentNewMedia";
	public static final String CONSENT_SOCIAL_MEDIA = "consentSocialMedia";
	public static final String CONSENT_DEALER_RATES = "consentDealerRates";
	public static final String CONSENT_SIMULCAST = "consentSimulcast";
	public static final String STRIKE_FOREIGN_USE = "strikeForeignUse";
	public static final String STRIKE_THEATRICAL_INDUSTRY_USE = "strikeTheatricalIndustryUse";
	public static final String PRODUCER_NOTICE_NAME = "producerNoticeName";
	public static final String PRODUCER_NOTICE_ADDRESS_ID = "producerNoticeAddressId";

	public static FormA1ContractDAO getInstance() {
		return (FormA1ContractDAO)ServiceFinder.findBean("FormA1ContractDAO");
	}
	
	public List<FormA1Contract> findByVersion(Object version) {
		return findByProperty(VERSION, version);
	}

	public List<FormA1Contract> findByWardrobeProvidedBy(Object wardrobeProvidedBy) {
		return findByProperty(WARDROBE_PROVIDED_BY, wardrobeProvidedBy);
	}

	public List<FormA1Contract> findBySpecialProvisions(Object specialProvisions) {
		return findByProperty(SPECIAL_PROVISIONS, specialProvisions);
	}

	public List<FormA1Contract> findBySpecialProvisionsSignId(Object specialProvisionsSignId) {
		return findByProperty(SPECIAL_PROVISIONS_SIGN_ID, specialProvisionsSignId);
	}

	public List<FormA1Contract> findByMinorConsentSigner(Object minorConsentSigner) {
		return findByProperty(MINOR_CONSENT_SIGNER, minorConsentSigner);
	}

	public List<FormA1Contract> findByMinorConsentSignId(Object minorConsentSignId) {
		return findByProperty(MINOR_CONSENT_SIGN_ID, minorConsentSignId);
	}

	public List<FormA1Contract> findByCompensation(Object compensation) {
		return findByProperty(COMPENSATION, compensation);
	}

	public List<FormA1Contract> findByTalentFirstLastName(Object talentFirstLastName) {
		return findByProperty(TALENT_FIRST_LAST_NAME, talentFirstLastName);
	}

	public List<FormA1Contract> findByTalentFirstName(Object talentFirstName) {
		return findByProperty(TALENT_FIRST_NAME, talentFirstName);
	}

	public List<FormA1Contract> findByTalentLastName(Object talentLastName) {
		return findByProperty(TALENT_LAST_NAME, talentLastName);
	}

	public List<FormA1Contract> findByTalentPhone(Object talentPhone) {
		return findByProperty(TALENT_PHONE, talentPhone);
	}

	public List<FormA1Contract> findByTalentEmailAddress(Object talentEmailAddress) {
		return findByProperty(TALENT_EMAIL_ADDRESS, talentEmailAddress);
	}

	public List<FormA1Contract> findByTalentPrimaryAddressId(Object talentPrimaryAddressId) {
		return findByProperty(TALENT_PRIMARY_ADDRESS_ID, talentPrimaryAddressId);
	}

	public List<FormA1Contract> findByTalentCOName(Object talentCOName) {
		return findByProperty(TALENT_CONAME, talentCOName);
	}

	public List<FormA1Contract> findByTalentCOAddressId(Object talentCOAddressId) {
		return findByProperty(TALENT_COADDRESS_ID, talentCOAddressId);
	}

	public List<FormA1Contract> findByTalentPayTo(Object talentPayTo) {
		return findByProperty(TALENT_PAY_TO, talentPayTo);
	}

	public List<FormA1Contract> findByTalentSignId(Object talentSignId) {
		return findByProperty(TALENT_SIGN_ID, talentSignId);
	}

	public List<FormA1Contract> findByEstimateNum(Object estimateNum) {
		return findByProperty(ESTIMATE_NUM, estimateNum);
	}

	public List<FormA1Contract> findByJobNum(Object jobNum) {
		return findByProperty(JOB_NUM, jobNum);
	}

	public List<FormA1Contract> findByAgencyName(Object agencyName) {
		return findByProperty(AGENCY_NAME, agencyName);
	}

	public List<FormA1Contract> findByAgencyAddressId(Object agencyAddressId) {
		return findByProperty(AGENCY_ADDRESS_ID, agencyAddressId);
	}

	public List<FormA1Contract> findByAgencyEmailAddress(Object agencyEmailAddress) {
		return findByProperty(AGENCY_EMAIL_ADDRESS, agencyEmailAddress);
	}

	public List<FormA1Contract> findByProducerName(Object producerName) {
		return findByProperty(PRODUCER_NAME, producerName);
	}

	public List<FormA1Contract> findByProducerSignName(Object producerSignName) {
		return findByProperty(PRODUCER_SIGN_NAME, producerSignName);
	}

	public List<FormA1Contract> findByProducerSignId(Object producerSignId) {
		return findByProperty(PRODUCER_SIGN_ID, producerSignId);
	}

	public List<FormA1Contract> findByProdCompanyName(Object prodCompanyName) {
		return findByProperty(PROD_COMPANY_NAME, prodCompanyName);
	}

	public List<FormA1Contract> findByAdvertiserName(Object advertiserName) {
		return findByProperty(ADVERTISER_NAME, advertiserName);
	}

	public List<FormA1Contract> findByAdvertiserProducts(Object advertiserProducts) {
		return findByProperty(ADVERTISER_PRODUCTS, advertiserProducts);
	}

	public List<FormA1Contract> findByNonEveningWearCnt(Object nonEveningWearCnt) {
		return findByProperty(NON_EVENING_WEAR_CNT, nonEveningWearCnt);
	}

	public List<FormA1Contract> findByEveningWearCnt(Object eveningWearCnt) {
		return findByProperty(EVENING_WEAR_CNT, eveningWearCnt);
	}

	public List<FormA1Contract> findByTotalWardrobeFee(Object totalWardrobeFee) {
		return findByProperty(TOTAL_WARDROBE_FEE, totalWardrobeFee);
	}

	public List<FormA1Contract> findByFlightInsuranceDays(Object flightInsuranceDays) {
		return findByProperty(FLIGHT_INSURANCE_DAYS, flightInsuranceDays);
	}

	public List<FormA1Contract> findByPlaceEngagement(Object placeEngagement) {
		return findByProperty(PLACE_ENGAGEMENT, placeEngagement);
	}

	public List<FormA1Contract> findByDateHourEngagement(Object dateHourEngagement) {
		return findByProperty(DATE_HOUR_ENGAGEMENT, dateHourEngagement);
	}

	public List<FormA1Contract> findByServicesRenderedAddressId(Object servicesRenderedAddressId) {
		return findByProperty(SERVICES_RENDERED_ADDRESS_ID, servicesRenderedAddressId);
	}

	public List<FormA1Contract> findByPartPlayed(Object partPlayed) {
		return findByProperty(PART_PLAYED, partPlayed);
	}

	public List<FormA1Contract> findByMultiTrack(Object multiTrack) {
		return findByProperty(MULTI_TRACK, multiTrack);
	}

	public List<FormA1Contract> findBySweeteningTrackCount(Object sweeteningTrackCount) {
		return findByProperty(SWEETENING_TRACK_COUNT, sweeteningTrackCount);
	}

	public List<FormA1Contract> findByDancerFootwearCnt(Object dancerFootwearCnt) {
		return findByProperty(DANCER_FOOTWEAR_CNT, dancerFootwearCnt);
	}

	public List<FormA1Contract> findByTotalDancerFootwearFee(Object totalDancerFootwearFee) {
		return findByProperty(TOTAL_DANCER_FOOTWEAR_FEE, totalDancerFootwearFee);
	}

	public List<FormA1Contract> findByDealerComTypeA(Object dealerComTypeA) {
		return findByProperty(DEALER_COM_TYPE_A, dealerComTypeA);
	}

	public List<FormA1Contract> findByDealerComTypeB(Object dealerComTypeB) {
		return findByProperty(DEALER_COM_TYPE_B, dealerComTypeB);
	}

	public List<FormA1Contract> findBySeasonalCom(Object seasonalCom) {
		return findByProperty(SEASONAL_COM, seasonalCom);
	}

	public List<FormA1Contract> findByTestMarketCom(Object testMarketCom) {
		return findByProperty(TEST_MARKET_COM, testMarketCom);
	}

	public List<FormA1Contract> findByNonAirCom(Object nonAirCom) {
		return findByProperty(NON_AIR_COM, nonAirCom);
	}

	public List<FormA1Contract> findByCableOnlyCom(Object cableOnlyCom) {
		return findByProperty(CABLE_ONLY_COM, cableOnlyCom);
	}

	public List<FormA1Contract> findByInternetOnlyCom(Object internetOnlyCom) {
		return findByProperty(INTERNET_ONLY_COM, internetOnlyCom);
	}

	public List<FormA1Contract> findByNewMediaCom(Object newMediaCom) {
		return findByProperty(NEW_MEDIA_COM, newMediaCom);
	}

	public List<FormA1Contract> findBySocialMediaCom(Object socialMediaCom) {
		return findByProperty(SOCIAL_MEDIA_COM, socialMediaCom);
	}

	public List<FormA1Contract> findByWorkInSmoke(Object workInSmoke) {
		return findByProperty(WORK_IN_SMOKE, workInSmoke);
	}

	public List<FormA1Contract> findByLangTransService(Object langTransService) {
		return findByProperty(LANG_TRANS_SERVICE, langTransService);
	}

	public List<FormA1Contract> findByOtherResidual(Object otherResidual) {
		return findByProperty(OTHER_RESIDUAL, otherResidual);
	}

	public List<FormA1Contract> findByOtherResidualText(Object otherResidualText) {
		return findByProperty(OTHER_RESIDUAL_TEXT, otherResidualText);
	}

	public List<FormA1Contract> findByConsentInternet(Object consentInternet) {
		return findByProperty(CONSENT_INTERNET, consentInternet);
	}

	public List<FormA1Contract> findByConsentNewMedia(Object consentNewMedia) {
		return findByProperty(CONSENT_NEW_MEDIA, consentNewMedia);
	}

	public List<FormA1Contract> findByConsentSocialMedia(Object consentSocialMedia) {
		return findByProperty(CONSENT_SOCIAL_MEDIA, consentSocialMedia);
	}

	public List<FormA1Contract> findByConsentDealerRates(Object consentDealerRates) {
		return findByProperty(CONSENT_DEALER_RATES, consentDealerRates);
	}

	public List<FormA1Contract> findByConsentSimulcast(Object consentSimulcast) {
		return findByProperty(CONSENT_SIMULCAST, consentSimulcast);
	}

	public List<FormA1Contract> findByStrikeForeignUse(Object strikeForeignUse) {
		return findByProperty(STRIKE_FOREIGN_USE, strikeForeignUse);
	}

	public List<FormA1Contract> findByStrikeTheatricalIndustryUse(
			Object strikeTheatricalIndustryUse) {
		return findByProperty(STRIKE_THEATRICAL_INDUSTRY_USE, strikeTheatricalIndustryUse);
	}

	public List<FormA1Contract> findByProducerNoticeName(Object producerNoticeName) {
		return findByProperty(PRODUCER_NOTICE_NAME, producerNoticeName);
	}

	public List<FormA1Contract> findByProducerNoticeAddressId(Object producerNoticeAddressId) {
		return findByProperty(PRODUCER_NOTICE_ADDRESS_ID, producerNoticeAddressId);
	}
}
