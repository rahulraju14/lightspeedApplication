//	File Name:	StripColorDAO.java
package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.StripColor;
import com.lightspeedeps.type.DayNightType;
import com.lightspeedeps.type.IntExtType;

/**
 * A data access object (DAO) providing persistence and search support for
 * StripColor entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.StripColor
 */
public class StripColorDAO extends BaseTypeDAO<StripColor> {
	private static final Log log = LogFactory.getLog(StripColorDAO.class);
	// property constants
//	private static final String IE_TYPE = "ieType";
//	private static final String DN_TYPE = "dnType";
//	private static final String BACKGROUND_RGB = "backgroundRgb";
//	private static final String TEXT_RGB = "textRgb";

	public static final Integer WHITE_RGB = 0xffffff;
	public static final Integer BLACK_RGB = 0x000000;
	public static final Integer COLOR_BANNER_RGB = 0xff9900;
	public static final Integer COLOR_END_OF_DAY_RGB = 0x969696;

	public static final StripColor NORMAL_COLOR = new StripColor(IntExtType.INTERIOR, DayNightType.DAY, WHITE_RGB, BLACK_RGB);

	public static StripColorDAO getInstance() {
		return (StripColorDAO)getInstance("StripColorDAO");
	}

	/**
	 * Find the StripColor associated with the given int/ext and day/night
	 * settings. Note that StripUtils loads this entire table (sequentially)
	 * when first needed and caches it, so this method would only be called in
	 * the unlikely event that some combination occurred that wasn't in the
	 * table at start-up.
	 *
	 * @param ieType The IntExtType of interest.
	 * @param dnType The DayNightType of interest.
	 * @return the strip color for the given combination, or null if that
	 *         combination is not found in the table.
	 */
	public StripColor findByIeTypeDnType(IntExtType ieType, DayNightType dnType) {
		log.debug("finding StripColor instance with I/E: " + ieType + ", D/N: " + dnType);
		String queryString = "from StripColor where ieType = ? and dnType = ?";
		StripColor stripColor = findOne(queryString, new Object[] { ieType, dnType });
		return stripColor;
	}

//	public static StripColorDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (StripColorDAO) ctx.getBean("StripColorDAO");
//	}

}