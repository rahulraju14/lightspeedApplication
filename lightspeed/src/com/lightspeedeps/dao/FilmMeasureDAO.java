package com.lightspeedeps.dao;

import com.lightspeedeps.model.FilmMeasure;

/**
 * A data access object (DAO) providing persistence and search support for
 * FilmMeasure entities. Transaction control of the save(), update() and
 * delete() operations can directly support Spring container-managed
 * transactions or they can be augmented to handle user-managed Spring
 * transactions. Each of these methods provides additional information for how
 * to configure it for the desired type of transaction control.
 *
 * @see com.lightspeedeps.model.FilmMeasure
 */

public class FilmMeasureDAO extends BaseTypeDAO<FilmMeasure> {

	// property constants
//	private static final String GROSS = "gross";
//	private static final String PRINT = "print";
//	private static final String NO_GOOD = "noGood";
//	private static final String WASTE = "waste";

	public static FilmMeasureDAO getInstance() {
		return (FilmMeasureDAO)getInstance("FilmMeasureDAO");
	}

//	public static FilmMeasureDAO getFromApplicationContext(
//			ApplicationContext ctx) {
//		return (FilmMeasureDAO) ctx.getBean("FilmMeasureDAO");
//	}

}
