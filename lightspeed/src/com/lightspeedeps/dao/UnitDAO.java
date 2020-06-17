package com.lightspeedeps.dao;

import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.Unit;

/**
 * A data access object (DAO) providing persistence and search support for Unit
 * entities. Transaction control of the save(), update() and delete() operations
 * can directly support Spring container-managed transactions or they can be
 * augmented to handle user-managed Spring transactions. Each of these methods
 * provides additional information for how to configure it for the desired type
 * of transaction control.
 *
 * @see com.lightspeedeps.model.Unit
 */

public class UnitDAO extends BaseTypeDAO<Unit> {
	// property constants
//	private static final String NUMBER = "number";
//	private static final String NAME = "name";
//	private static final String DESCRIPTION = "description";

	public static UnitDAO getInstance() {
		return (UnitDAO)getInstance("UnitDAO");
	}

//	public static UnitDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (UnitDAO)ctx.getBean("UnitDAO");
//	}

	/**
	 * Find the Unit of the specified Project that has the given unit number.
	 *
	 * @param project The Project whose Unit's are to be searched.
	 * @param unitNumber The number of the unit (not database id) to be found.
	 * @return The Unit matching the parameters given, or null if not found.
	 */
	public Unit findByProjectAndNumber(Project project, Integer unitNumber) {
		Unit unit = null;
		for (Unit u : project.getUnits()) {
			if (u.getNumber().equals(unitNumber)) {
				unit = u;
				break;
			}
		}
		return unit;
	}

}