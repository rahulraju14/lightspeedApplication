package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.CanadaProjectDetail;
import com.lightspeedeps.util.app.ServiceFinder;

/**
 * A data access object (DAO) providing persistence and search support for
 * CanadaProjectDetail entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.CanadaProjectDetail
 */
public class CanadaProjectDetailDAO extends BaseTypeDAO<CanadaProjectDetail> {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(CanadaProjectDetailDAO.class);

	//property constants
//	public static final String BRANCH_CODE = "brandCode";
//	public static final String BRAND_NAME = "brandName";
//	public static final String AGENCY_NAME = "agencyName";
//	public static final String AGENCY_PRODUCER = "agencyProducer";
//	public static final String AGENCY_ADDRESS_ID = "agencyAddressId";
//	public static final String ADVERTISER_NAME = "advertiserName";
//	public static final String PRODUCT_NAME = "productName";
//	public static final String PROD_HOUSE_NAME = "prodHouseName";
//	public static final String PROD_HOUSE_ADDRESS_ID = "prodHouseAddressId";
//	public static final String DIRECTOR_NAME = "directorName";
//	public static final String COMMERCIAL_NAME = "commercialName";
//	public static final String ADDITIONAL_TITLES = "additionalTitles";
//	public static final String NATIONAL_TV = "nationalTv";
//	public static final String NATIONAL_RADIO = "nationalRadio";
//	public static final String NATIONAL_DIGITAL_MEDIA_VIDEO = "nationalDigitalMediaVideo";
//	public static final String NATIONAL_DIGITAL_MEDIA_AUDIO = "nationalDigitalMediaAudio";
//	public static final String DEMO_TV = "demoTv";
//	public static final String DEMO_RADIO = "demoRadio";
//	public static final String DEMO_DIGITAL = "demoDigital";
//	public static final String DEMO_PRESENTATION = "demoPresentation";
//	public static final String DEMO_INFOMERCIAL = "demoInfomercial";
//	public static final String TAGS_TV = "tagsTv";
//	public static final String TAGS_RADIO = "tagsRadio";
//	public static final String TAGS_DIGITAL_MEDIA = "tagsDigitalMedia";
//	public static final String REGIONAL_CHANGES_TV = "regionalChangesTv";
//	public static final String REGIONAL_CHANGES_RADIO = "regionalChangesRadio";
//	public static final String REGIONAL_CHANGES_DIGITAL_MEDIA = "regionalChangesDigitalMedia";
//	public static final String PSA_TV = "psaTv";
//	public static final String PSA_RADIO = "psaRadio";
//	public static final String PSA_DIGITAL_MEDIA = "psaDigitalMedia";
//	public static final String SEASONAL_TV ="seasonalTv";
//	public static final String SEASONAL_RADIO = "seasonalRadio";
//	public static final String SEASONAL_DEALER ="seasonalDealer";
//	public static final String SEASONAL_DEALER_TV = "seasonalDealerTv";
//	public static final String SEASONAL_DEALER_RADIO = "seasonalDealerRadio";
//	public static final String SEASONAL_DOUBLE_SHOOT = "seasonalDoubleShoot";
//	public static final String SEASONAL_JOINT_PROMO = "seasonalJointPromo";
//	public static final String LOCAL_REGIONAL_CATEGORY_NUM = "localRegionalCategoryNum";
//	public static final String LOCAL_REGIONAL_TV = "localRegionalTv";
//	public static final String LOCAL_REGIONAL_RADIO = "localRegionalRadio";
//	public static final String LOCAL_REGIONAL_DIGITAL_MEDIA = "localRegionalDigitalMedia";
//	public static final String LOCAL_REGIONAL_DEMO ="localRegionalDemo";
//	public static final String LOCAL_REGIONAL_DIGITAL_MEDIA_BROADCAST = "localRegionalDigitalMediaBroadcast";
//	public static final String LOCAL_REGIONAL_BROADCAST_DIGITAL_MEDIA = "localRegionalBroadcastDigitalMedia";
//	public static final String LOCAL_REGIONAL_OTHER = "localRegionalOther";
//	public static final String LOCAL_REGIONAL_PILOT_PROJECT = "localRegionalPilotProject";
//	public static final String SHORT_LIFE_TV_7DAYS = "shortLifeTv7Days";
//	public static final String SHORT_LIFE_TV_14DAYS = "shortLifeTv14Days";
//	public static final String SHORT_LIFE_TV_31DAYS = "shortLifeTv31Days";
//	public static final String SHORT_LIFE_TV_45DAYS = "shortLifeTv45Days";
//	public static final String SHORT_LIFE_RADIO_7DAYS = "shortLifeRadio7Days";
//	public static final String SHORT_LIFE_RADIO_14DAYS = "shortLifeRadio14Days";
//	public static final String SHORT_LIFE_RADIO_31DAYS = "shortLifeRadio31Days";
//	public static final String SHORT_LIFE_RADIO_45DAYS = "shortLifeRadio45Days";
//	public static final String TV_BROADCAST_DIGITAL_MEDIA = "tvBroadcastDigitalMedia";
//	public static final String DIGITAL_MEDIA_BROADCAST_TV = "digitalMediaBroadcastTv";
//	public static final String DIGITAL_MEDIA_OTHER = "digitalMediaOther";
//	public static final String RADIO_DIGITAL_MEDIA = "radioDigitalMedia";
//	public static final String ACTRA_ONLINE = "actraOnline";
//	public static final String ARTICLE_2403 = "article2403";
//	public static final String ARTICLE_2404 = "article2404";
//	public static final String ARTICLE_2405 = "article2405";
//	public static final String ARTICLE_2406 = "article2406";
//	public static final String DOCKET = "docket";

	public static CanadaProjectDetailDAO getInstance() {
		return (CanadaProjectDetailDAO)ServiceFinder.findBean("CanadaProjectDetailDAO");
	}

}
