package com.lightspeedeps.dao;

import com.lightspeedeps.model.VehicleLog;

/**
 * A data access object (DAO) providing persistence and search support for
 * VehicleLog entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.VehicleLog
 */

public class VehicleLogDAO extends BaseTypeDAO<VehicleLog> {
	// property constants
//	private static final String VEHICLE_NAME = "vehicleName";
//	private static final String DRIVER = "driver";
//	private static final String PASSENGERS = "passengers";
//	private static final String VEHICLE_ID = "vehicleId";
//	private static final String NOTE = "note";

	public static VehicleLogDAO getInstance() {
		return (VehicleLogDAO)getInstance("VehicleLogDAO");
	}

//	public static VehicleLogDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (VehicleLogDAO) ctx.getBean("VehicleLogDAO");
//	}

}