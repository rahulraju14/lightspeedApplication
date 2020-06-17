//	File Name:	StripboardReport.java
package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * StripboardReport entity. Each instance represents one line of
 * output in a report being generated.
 */
@Entity
@Table(name = "stripboard_report")
public class StripboardReport extends PersistentObject<StripboardReport> implements java.io.Serializable {
	private static final long serialVersionUID = 197149381113429887L;

	public static final int ISTATUS_UNSCHEDULED = 100;
	public static final int ISTATUS_OMITTED = 101;

	// Fields
	private String reportId;
	private Integer segment;

	/** The StripStatus value (as returned by toString()) for the
	 * represented Strip, or the unit name; i.e., UNSCHEDULED, OMITTED,
	 * or the Unit name of the unit this Strip is scheduled in. */
	private String status;

	/** The represented Strip's status as an integer value, for jasper report
	 * management.  This is the associated unit number for Scheduled strips,
	 * 100 for Unscheduled strips, and 101 for Omitted strips. */
	private Integer iStatus;

	private String type;
	private Integer sequence;
	private Integer sheet;
	private Integer scriptOrder;
	private String scenes;
	private String intExt;
	private String dayNight;
	private String scriptDay;
	private String location;
	private String synopsis;
	private String pages;
	private String castIds;
	private Integer backgroundRgb;
	private Integer textRgb;

	// Constructors

	/** default constructor */
	public StripboardReport() {
	}

	/** full constructor */
/*	public StripboardReport(String reportId, Integer segment, String status, String type, Integer sequence,
			Integer sheet, String scenes, String intExt, String dayNight, String scriptDay, String location,
			String synopsis, String pages, String castIds, Integer backgroundColor, Integer textColor) {
		this.reportId = reportId;
		this.segment = segment;
		this.status = status;
		this.type = type;
		this.sequence = sequence;
		this.sheet = sheet;
		this.scenes = scenes;
		this.intExt = intExt;
		this.dayNight = dayNight;
		this.scriptDay = scriptDay;
		this.location = location;
		this.synopsis = synopsis;
		this.pages = pages;
		this.castIds = castIds;
		this.backgroundRgb = backgroundColor;
		this.textRgb = textColor;
	}
*/
	// Property accessors

	@Column(name = "report_id", length = 100)
	public String getReportId() {
		return this.reportId;
	}
	public void setReportId(String reportId) {
		this.reportId = reportId;
	}

	@Column(name = "segment")
	public Integer getSegment() {
		return this.segment;
	}
	public void setSegment(Integer segment) {
		this.segment = segment;
	}

	/** See {@link #status}. */
	@Column(name = "status", length = 20)
	public String getStatus() {
		return this.status;
	}
	/** See {@link #status}. */
	public void setStatus(String status) {
		this.status = status;
	}

	/** See {@link #iStatus}. */
	@Column(name = "istatus")
	public Integer getIStatus() {
		return iStatus;
	}
	/** See {@link #iStatus}. */
	public void setIStatus(Integer status) {
		iStatus = status;
	}

	@Column(name = "type", length = 20)
	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Column(name = "sequence")
	public Integer getSequence() {
		return this.sequence;
	}

	public void setSequence(Integer sequence) {
		this.sequence = sequence;
	}

	@Column(name = "sheet")
	public Integer getSheet() {
		return this.sheet;
	}

	public void setSheet(Integer sheet) {
		this.sheet = sheet;
	}

	/** See {@link #scriptOrder}. */
	@Column(name = "script_order")
	public Integer getScriptOrder() {
		return scriptOrder;
	}
	/** See {@link #scriptOrder}. */
	public void setScriptOrder(Integer scriptOrder) {
		this.scriptOrder = scriptOrder;
	}

	@Column(name = "scenes", length = 100)
	public String getScenes() {
		return this.scenes;
	}

	public void setScenes(String scenes) {
		this.scenes = scenes;
	}

	@Column(name = "int_ext", length = 5)
	public String getIntExt() {
		return this.intExt;
	}

	public void setIntExt(String intExt) {
		this.intExt = intExt;
	}

	@Column(name = "day_night", length = 20)
	public String getDayNight() {
		return this.dayNight;
	}

	public void setDayNight(String dayNight) {
		this.dayNight = dayNight;
	}

	@Column(name = "script_day", length = 20)
	public String getScriptDay() {
		return this.scriptDay;
	}

	public void setScriptDay(String scriptDay) {
		this.scriptDay = scriptDay;
	}

	@Column(name = "location", length = 200)
	public String getLocation() {
		return this.location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	@Column(name = "synopsis", length = 100)
	public String getSynopsis() {
		return this.synopsis;
	}

	public void setSynopsis(String synopsis) {
		this.synopsis = synopsis;
	}

	@Column(name = "pages", length = 10)
	public String getPages() {
		return this.pages;
	}

	public void setPages(String pages) {
		this.pages = pages;
	}

	@Column(name = "cast_ids", length = 500)
	public String getCastIds() {
		return this.castIds;
	}

	public void setCastIds(String castIds) {
		this.castIds = castIds;
	}

	@Column(name = "background_rgb")
	public Integer getBackgroundRgb() {
		return this.backgroundRgb;
	}
	public void setBackgroundRgb(Integer backgroundColor) {
		this.backgroundRgb = backgroundColor;
	}

	@Column(name = "text_rgb")
	public Integer getTextRgb() {
		return this.textRgb;
	}
	public void setTextRgb(Integer textColor) {
		this.textRgb = textColor;
	}

}
