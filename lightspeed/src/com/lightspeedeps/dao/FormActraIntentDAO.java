package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.FormActraIntent;
import com.lightspeedeps.util.app.ServiceFinder;

/**
 * A data access object (DAO) providing persistence and search support for
 * FormActraIntent entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.FormActraIntent
 */
public class FormActraIntentDAO extends BaseTypeDAO<FormActraIntent>  {
	 @SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(FormActraIntentDAO.class);
	//property constants
//	public static final String ADVERTISER = "advertiser";
//	public static final String PRODUCT = "product";
//	public static final String AGENCY_NAME = "agencyName";
//	public static final String PRODUCER_NAME = "producerName";
//	public static final String SIGNATORY_ENGAGER = "signatoryEngager";
//	public static final String DIRECTOR_NAME = "directorName";
//	public static final String PRODUCTION_HOUSE_NAME = "productionHouseName";
//	public static final String LINE_PRODUCER_NAME = "lineProducerName";
//	public static final String CASTING_DIRECTOR_NAME = "castingDirectorName";
//	public static final String MULTI_BRANCH = "multiBranch";
//	public static final String MULTI_BRANCH_LOCATIONS = "multiBranchLocations";
//	public static final String USE_CANADA = "useCanada";
//	public static final String USE_US = "useUs";
//	public static final String DIGITAL_MEDIA1 = "digitalMedia1";
//	public static final String DIGITAL_MEDIA2 = "digitalMedia2";
//	public static final String OTHER1 = "other1";
//	public static final String OTHER2 = "other2";
//	public static final String NATIONAL_COM = "nationalCom";
//	public static final String TV = "tv";
//	public static final String RADIO = "radio";
//	public static final String PSA = "psa";
//	public static final String LOC_REGION_COM = "locRegionCom";
//	public static final String DOUBLE_SHOOT = "doubleShoot";
//	public static final String DEMO = "demo";
//	public static final String A706_EXCLUSIONS = "a706Exclusions";
//	public static final String SEASONAL_COM = "seasonalCom";
//	public static final String TAGS = "tags";
//	public static final String DEALER = "dealer";
//	public static final String INFORMERCIAL = "informercial";
//	public static final String SHORT_LIFE7_DAYS = "shortLife7Days";
//	public static final String SHORT_LIFE14_DAYS = "shortLife14Days";
//	public static final String SHORT_LIFE31_DAYS = "shortLife31Days";
//	public static final String SHORT_LIFE45_DAYS = "shortLife45Days";
//	public static final String COMMERCIAL_ID = "commercialId";
//	public static final String MINOR = "minor";
//	public static final String NUM_MINORS = "numMinors";
//	public static final String MINOR_AGES = "minorAges";
//	public static final String NUM_EXTRAS_GENERAL = "numExtrasGeneral";
//	public static final String NUM_EXTRAS_GROUP = "numExtrasGroup";
//	public static final String NUM_EXTRAS_GROUP31 = "numExtrasGroup31";
//	public static final String STUNTS = "stunts";
//	public static final String STUNT_TYPE = "stuntType";
//	public static final String EXT_SCENES = "extScenes";
//	public static final String EXT_SCENES_TYPE = "extScenesType";
//	public static final String LOCATION_SHOOT40_RADIUS = "locationShoot40Radius";
//	public static final String WEATHER_PERMITTING = "weatherPermitting";
//	public static final String WEEKEND_NIGHT = "weekendNight";
//	public static final String NUDE_SCENES = "nudeScenes";
//	public static final String TALENT_ID = "talentId";

	public static FormActraIntentDAO getInstance() {
		return (FormActraIntentDAO)ServiceFinder.findBean("FormActraIntentDAO");
	}

}