package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.FormActraContract;
import com.lightspeedeps.util.app.ServiceFinder;

/**
 * A data access object (DAO) providing persistence and search support for
 * FormActraContract entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.FormActraContract
 */
public class FormActraContractDAO extends BaseTypeDAO<FormActraContract> {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(FormActraContractDAO.class);

	//property constants
//	public static final String CONTRACT_NUM = "contractNum";
//	public static final String BRANCH_CODE = "branchCode";
//	public static final String AGENCY_NAME = "agencyName";
//	public static final String AGENCY_PRODUCER = "agencyProducer";
//	public static final String AGENCY_ADDRESS_ID = "agencyAddressId";
//	public static final String ADVERTISER_NAME = "advertiserName";
//	public static final String PRODUCT_NAME = "productName";
//	public static final String PROD_HOUSE_NAME = "prodHouseName";
//	public static final String PROD_HOUSE_ADDRESS_ID = "prodHouseAddressId";
//	public static final String PAY_SESSION_FEE = "paySessionFee";
//	public static final String LOAN_OUT_NAME = "loanOutName";
//	public static final String TALENT_NAME = "talentName";
//	public static final String TALENT_ADDRESS_ID = "talentAddressId";
//	public static final String TALENT_EMAIL_ADDRESS = "talentEmailAddress";
//	public static final String TALENT_PHONE_NUM = "talentPhoneNum";
//	public static final String SOCIAL_INSURANCE_NUM = "socialInsuranceNum";
//	public static final String GST_HST = "gstHst";
//	public static final String QST = "qst";
//	public static final String TALENT_AGENCY_NAME = "talentAgencyName";
//	public static final String PERFORMANCE_CATEGORY = "performanceCategory";
//	public static final String FULL_MEMBER_NUM = "fullMemberNum";
//	public static final String APPRENTICE_NUM = "apprenticeNum";
//	public static final String WORK_PERMIT_NUM = "workPermitNum";
//	public static final String NATIONAL_TV = "nationalTv";
//	public static final String NATIONAL_RADIO = "nationalRadio";
//	public static final String NATIONAL_NEW_MEDIA_VIDEO = "nationalNewMediaVideo";
//	public static final String NATIONAL_NEW_MEDIA_RADIO = "nationalNewMediaRadio";
//	public static final String TAGS_TV = "tagsTv";
//	public static final String TAGS_RADIO = "tagsRadio";
//	public static final String TAGS_NEW_MEDIA = "tagsNewMedia";
//	public static final String REGIONAL_CHANGES_TV = "regionalChangesTv";
//	public static final String REGIONAL_CHANGES_RADIO = "regionalChangesRadio";
//	public static final String REGIONAL_CHANGES_NEW_MEDIA = "regionalChangesNewMedia";
//	public static final String PSA_TV = "psaTv";
//	public static final String PSA_RADIO = "psaRadio";
//	public static final String PSA_NEW_MEDIA = "psaNewMedia";
//	public static final String DEMO_TV = "demoTv";
//	public static final String DEMO_RADIO = "demoRadio";
//	public static final String DEMO_PRESENTATION = "demoPresentation";
//	public static final String DEMO_INFOMERCIAL = "demoInfomercial";
//	public static final String SEASONAL_TV = "seasonalTv";
//	public static final String SEASONAL_RADIO = "seasonalRadio";
//	public static final String SEASONAL_DEALER = "seasonalDealer";
//	public static final String SEASONAL_DEALER_TV = "seasonalDealerTv";
//	public static final String SEASONAL_DEALER_RADIO = "seasonalDealerRadio";
//	public static final String SEASONAL_DOUBLE_SHOOT = "seasonalDoubleShoot";
//	public static final String SEASONAL_JOINT_PROMO = "seasonalJointPromo";
//	public static final String LOCAL_REGIONAL_CATEGORY_NUM = "localRegionalCategoryNum";
//	public static final String LOCAL_REGIONAL_TV = "localRegionalTv";
//	public static final String LOCAL_REGIONAL_RADIO = "localRegionalRadio";
//	public static final String LOCAL_REGIONAL_NEW_MEDIA = "localRegionalNewMedia";
//	public static final String LOCAL_REGIONAL_DEMO = "localRegionalDemo";
//	public static final String LOCAL_REGIONAL_PREPAID = "localRegionalPrepaid";
//	public static final String LOCAL_REGIONAL_NEW_MEDIA_BROADCAST =
//			"localRegionalNewMediaBroadcast";
//	public static final String LOCAL_REGIONAL_BROADCAST_NEW_MEDIA =
//			"localRegionalBroadcastNewMedia";
//	public static final String LOCAL_REGIONAL_OTHER = "localRegionalOther";
//	public static final String SHORT_LIFE_TV7_DAYS = "shortLifeTv7Days";
//	public static final String SHORT_LIFE_TV14_DAYS = "shortLifeTv14Days";
//	public static final String SHORT_LIFE_TV31_DAYS = "shortLifeTv31Days";
//	public static final String SHORT_LIFE_TV45_DAYS = "shortLifeTv45Days";
//	public static final String SHORT_LIFE_RADIO7_DAYS = "shortLifeRadio7Days";
//	public static final String SHORT_LIFE_RADIO14_DAYS = "shortLifeRadio14Days";
//	public static final String SHORT_LIFE_RADIO31_DAYS = "shortLifeRadio31Days";
//	public static final String SHORT_LIFE_RADIO45_DAYS = "shortLifeRadio45Days";
//	public static final String TV_BROADCAST_NEW_MEDIA = "tvBroadcastNewMedia";
//	public static final String NEW_MEDIA_BROADCAST_TV = "newMediaBroadcastTv";
//	public static final String NEW_MEDIA_OTHER = "newMediaOther";
//	public static final String ARTICLE2403 = "article2403";
//	public static final String ARTICLE2404 = "article2404";
//	public static final String ARTICLE2405 = "article2405";
//	public static final String ARTICLE2406 = "article2406";
//	public static final String COMMERCIAL_NAME = "commercialName";
//	public static final String DOCKET = "docket";
//	public static final String OTHER = "other";
//	public static final String SESSION_FEES = "sessionFees";
//	public static final String RESIDUAL_FEES = "residualFees";
//	public static final String OTHER_FEES = "otherFees";
//	public static final String TALENT_SIGN_ID = "talentSignId";
//	public static final String PRODUCER_SIGN_ID = "producerSignId";
//	public static final String WARDROBE_CALL = "wardrobeCall";
//	public static final String WARDROBE_WRAP = "wardrobeWrap";
//	public static final String REHEARSAL_CALL = "rehearsalCall";
//	public static final String REHEARSAL_WRAP = "rehearsalWrap";
//	public static final String HOLDING_CALL = "holdingCall";
//	public static final String HOLDING_WRAP = "holdingWrap";
//	public static final String NDM = "ndm";
//	public static final String TALENT_INITIAL_AGREE_ID = "talentInitialAgreeId";
//	public static final String TALENT_INITIAL_DISAGREE_ID = "talentInitialDisagreeId";
//	public static final String PRODUCER_INITIAL_ID = "producerInitialId";
//	public static final String WEEKLY_TIMECARD_ID = "weeklyTimecardId";
//	public static final String CREATED_BY_ID = "createdById";
//	public static final String UPDATED_BY_ID = "updatedById";

	public static FormActraContractDAO getInstance() {
		return (FormActraContractDAO)ServiceFinder.findBean("FormActraContractDAO");
	}

}
