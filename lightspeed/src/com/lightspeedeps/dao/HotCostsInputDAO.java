package com.lightspeedeps.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;

import com.lightspeedeps.model.HotCostsInput;
import com.lightspeedeps.model.Production;

/**
 * A data access object (DAO) providing persistence and search support for HotCostsInput entities.
 * Transaction control of the save(), update() and delete() operations can directly support Spring
 * container-managed transactions or they can be augmented to handle user-managed Spring
 * transactions. Each of these methods provides additional information for how to configure it for
 * the desired type of transaction control.
 *
 * @see com.lightspeedeps.model.HotCostsInput
 * @author MyEclipse Persistence Tools
 */
public class HotCostsInputDAO extends BaseTypeDAO<HotCostsInput> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(HotCostsInputDAO.class);

	//property constants
	public static final String WORK_ZONE = "workZone";
	public static final String DAY_TYPE = "dayType";
	public static final String CALL_TIME = "callTime";
	public static final String M1_OUT = "m1Out";
	public static final String M1_IN = "m1In";
	public static final String M2_OUT = "m2Out";
	public static final String M2_IN = "m2In";
	public static final String WRAP = "wrap";
	public static final String NDB_END = "ndbEnd";
	public static final String NDM_START = "ndmStart";
	public static final String NDM_END = "ndmEnd";
	public static final String GRACE1 = "grace1";
	public static final String GRACE2 = "grace2";
	public static final String LAST_MAN_IN = "lastManIn";
	public static final String MPV1_PAYROLL = "mpv1Payroll";
	public static final String MPV2_PAYROLL = "mpv2Payroll";
	public static final String OFF_PRODUCTION = "offProduction";
	public static final String FORCED_CALL = "forcedCall";
	public static final String CAMERA_WRAP = "cameraWrap";
	public static final String FRENCH_HOURS = "frenchHours";
	public static final String WEEK_END_DATE = "weekEndDate";
	public static final String DAY_OF_WEEK_NUM = "dayOfWeekNum";

	public static HotCostsInputDAO getInstance() {
		return (HotCostsInputDAO)getInstance("HotCostsInputDAO");
	}

	public List<HotCostsInput> findByWorkZone(Object workZone) {
		return findByProperty(WORK_ZONE, workZone);
	}

	public List<HotCostsInput> findByDayType(Object dayType) {
		return findByProperty(DAY_TYPE, dayType);
	}

	public List<HotCostsInput> findByCallTime(Object callTime) {
		return findByProperty(CALL_TIME, callTime);
	}

	public List<HotCostsInput> findByM1Out(Object m1Out) {
		return findByProperty(M1_OUT, m1Out);
	}

	public List<HotCostsInput> findByM1In(Object m1In) {
		return findByProperty(M1_IN, m1In);
	}

	public List<HotCostsInput> findByM2Out(Object m2Out) {
		return findByProperty(M2_OUT, m2Out);
	}

	public List<HotCostsInput> findByM2In(Object m2In) {
		return findByProperty(M2_IN, m2In);
	}

	public List<HotCostsInput> findByWrap(Object wrap) {
		return findByProperty(WRAP, wrap);
	}

	public List<HotCostsInput> findByNdbEnd(Object ndbEnd) {
		return findByProperty(NDB_END, ndbEnd);
	}

	public List<HotCostsInput> findByNdmStart(Object ndmStart) {
		return findByProperty(NDM_START, ndmStart);
	}

	public List<HotCostsInput> findByNdmEnd(Object ndmEnd) {
		return findByProperty(NDM_END, ndmEnd);
	}

	public List<HotCostsInput> findByGrace1(Object grace1) {
		return findByProperty(GRACE1, grace1);
	}

	public List<HotCostsInput> findByGrace2(Object grace2) {
		return findByProperty(GRACE2, grace2);
	}

	public List<HotCostsInput> findByLastManIn(Object lastManIn) {
		return findByProperty(LAST_MAN_IN, lastManIn);
	}

	public List<HotCostsInput> findByMpv1Payroll(Object mpv1Payroll) {
		return findByProperty(MPV1_PAYROLL, mpv1Payroll);
	}

	public List<HotCostsInput> findByMpv2Payroll(Object mpv2Payroll) {
		return findByProperty(MPV2_PAYROLL, mpv2Payroll);
	}

	public List<HotCostsInput> findByOffProduction(Object offProduction) {
		return findByProperty(OFF_PRODUCTION, offProduction);
	}

	public List<HotCostsInput> findByForcedCall(Object forcedCall) {
		return findByProperty(FORCED_CALL, forcedCall);
	}

	public List<HotCostsInput> findByCameraWrap(Object cameraWrap) {
		return findByProperty(CAMERA_WRAP, cameraWrap);
	}

	public List<HotCostsInput> findByFrenchHours(Object frenchHours) {
		return findByProperty(FRENCH_HOURS, frenchHours);
	}

	public HotCostsInput findByProdIdWeekEndDateDayNum(Production production, Date weekEndDate, Byte dayOfWeekNum) {
		List<Object>parms = new ArrayList<Object>();
		String query = "from HotCostsInput where prod_id = ? and Week_End_date = ? and Day_Of_Week_Num=?";
		parms.add(production.getProdId());
		parms.add(weekEndDate);
		parms.add(dayOfWeekNum);

		return findOne(query, parms.toArray());
	}

	public static HotCostsInputDAO getFromApplicationContext(ApplicationContext ctx) {
		return (HotCostsInputDAO) ctx.getBean("HotCostsInputDAO");
	}
}