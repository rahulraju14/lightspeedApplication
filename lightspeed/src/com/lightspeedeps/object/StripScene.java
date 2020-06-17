package com.lightspeedeps.object;

import java.io.Serializable;

import com.lightspeedeps.model.Strip;
import com.lightspeedeps.type.DayNightType;
import com.lightspeedeps.type.IntExtType;

/**
 * This class is used by the AutoScheduleBean. Each instance represents one
 * Strip being scheduled, and holds information taken from the first Scene
 * represented by the corresponding Strip. This class is also extended by
 * StripBoardSceneBean.
 */
public class StripScene implements Serializable {
	// TODO See if the Stripboard Editor could use this object instead of StripBoardSceneBean
	/** */
	private static final long serialVersionUID = - 9096473124317324780L;

	/** Reference to the related Strip; this is only used by the auto-scheduler. */
	private Strip strip;

	/** The Int/Ext setting from the first Scene on this Strip. */
	private IntExtType intExtType;
	/** The Day/Night setting from the first Scene on this Strip. */
	private DayNightType dayNType;

	/** The sequence field from the first Scene on this Strip,
	 * which specifies its order in the script. */
	private int seqNum;

	/** The database id of the Location ("set") for the first Scene
	 * in this Strip. */
	private int locationId;
	/** The name of the Location ("set") for the first Scene in
	 * this Strip. */
	private String name;
//	/** The name of the "set" (Location) for the first Scene in
//	 * this Strip, shortened to the width required for the stripboard editor. */
//	private String nameAlt;


	public StripScene() {
	}

	/** See {@link #strip}. */
	public Strip getStrip() {
		return strip;
	}
	/** See {@link #strip}. */
	public void setStrip(Strip strip) {
		this.strip = strip;
	}

	/** See {@link #intExtType}. */
	public IntExtType getIntExtType() {
		return intExtType;
	}
	/** See {@link #intExtType}. */
	public void setIntExtType(IntExtType intExtType) {
		this.intExtType = intExtType;
	}

	/**
	 * @return The 'tiny label' for the StripScene's {@link #intExtType}
	 */
	public String getIntEType() {
		return (intExtType==null ? null : intExtType.getTinyLabel());
	}

	/** See {@link #dayNType}. */
	public DayNightType getDayNType() {
		return dayNType;
	}
	/** See {@link #dayNType}. */
	public void setDayNType(DayNightType dayNType) {
		this.dayNType = dayNType;
	}

	/** See {@link #locationId}. */
	public int getLocationId() {
		return locationId;
	}
	/** See {@link #locationId}. */
	public void setLocationId(int locationId) {
		this.locationId = locationId;
	}

	/** See {@link #seqNum}. */
	public int getSeqNum() {
		return seqNum;
	}
	/** See {@link #seqNum}. */
	public void setSeqNum(int seqNum) {
		this.seqNum = seqNum;
	}

	/** See {@link #name}. */
	public String getName() {
		return name;
	}
	/** See {@link #name}. */
	public void setName(String str) {
		name = str;
//		nameAlt = StringUtils.trimToWidth(name, StripBoardBaseBean.SB_EDIT_WIDTH_LOC_ALT);
	}

	/** See {@link #nameAlt}. */
//	public String getNameAlt() {
//		return nameAlt;
//	}

}
