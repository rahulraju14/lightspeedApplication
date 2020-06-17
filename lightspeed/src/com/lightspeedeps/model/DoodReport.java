package com.lightspeedeps.model;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import com.lightspeedeps.dood.ElementDood;

/**
 * DoodReport entity. Each instance represents one row in a DooD report. A given
 * report consists of all the records matching a particular reportId value.
 * Since one page of a report only covers 21 days, the report is broken into
 * 'segments', each covering a 21-day period of the production.
 * <p>
 * The 'segment' field identifies which segment of the report a record belongs
 * in.
 * <p>
 * The 'type' field indicates which ScriptElementType the record relates to, or
 * if it is one of the three header rows (type=-1) generated for each segment.
 * <p>
 * The 'sequence' field is used to keep the header and trailer rows in proper
 * order. For the three header lines, it will be -3, -2, and -1 (starting from
 * the top). The header row identifying the ScriptElementType (e.g., Props) will
 * be zero. The records corresponding to actual elements will all be one(1). The
 * trailing 'spacer' row for each element type will be a very large value (e.g.,
 * 999999).
 * <p>
 * So the records within a report are always ordered at least by segment, type,
 * sequence. Additional sort keys are used depending on the user's requested
 * order of the report: by element name/cast id, start date, or total number of
 * work-days.
 */
@Entity
@Table(name = "dood_report")
public class DoodReport extends PersistentObject<DoodReport> implements java.io.Serializable {

	// Fields

	private static final long serialVersionUID = -1768409051188831001L;

	/** An id identifying all the records comprising one DooD report. */
	private String reportId;
	/** The segment number of the report to which this record belongs. There is a
	 * report segment for each group of 21 days covered by the report.  A report
	 * segment always starts on a Sunday and ends on a Saturday. */
	private Integer segment;
	/** The ordinal value of the ScriptElementType of the element related to
	 * this record; or -1 for a header record. */
	private Integer type;
	/** The ScriptElementType descriptive name. */
	private String typeName;
	/** A value to help order the records appropriately for printing. Segment header rows have
	 * negative values.  An element type header row has zero, and the trailing element type
	 * row has a very high value.  "Normal" element (detail) rows have a value of 1. */
	private Integer sequence;
	/** The name of the real person or thing playing the part of this script element. */
	private String person;
	/** The (alphanumeric) cast id assigned to this element. May be null or blank. */
	private String castId;
	/** The numeric cast id value, used for sorting in 'cast id order'. May be null. */
	private Integer castIdNum;
	/** The name of the ScriptElement, or the text to display for a header row. */
	private String elementName;
	/**
	 * The work status to be displayed (e.g., "W", "WF") for day 1 (out of 21)
	 * in a particular report segment. If the day is NOT a work day, then this
	 * is set as follows: "1"=Holiday, "2"=off, "3"=Travel.
	 */
	private String day1;
	/** Day 2 work status: See {@link #day1}. */
	private String day2;
	/** Day 3 work status: See {@link #day1}. */
	private String day3;
	/** Day 4 work status: See {@link #day1}. */
	private String day4;
	/** Day 5 work status: See {@link #day1}. */
	private String day5;
	/** Day 6 work status: See {@link #day1}. */
	private String day6;
	/** Day 7 work status: See {@link #day1}. */
	private String day7;
	/** Day 8 work status: See {@link #day1}. */
	private String day8;
	/** Day 9 work status: See {@link #day1}. */
	private String day9;
	/** Day 10 work status: See {@link #day1}. */
	private String day10;
	/** Day 11 work status: See {@link #day1}. */
	private String day11;
	/** Day 12 work status: See {@link #day1}. */
	private String day12;
	/** Day 13 work status: See {@link #day1}. */
	private String day13;
	/** Day 14 work status: See {@link #day1}. */
	private String day14;
	/** Day 15 work status: See {@link #day1}. */
	private String day15;
	/** Day 16 work status: See {@link #day1}. */
	private String day16;
	/** Day 17 work status: See {@link #day1}. */
	private String day17;
	/** Day 18 work status: See {@link #day1}. */
	private String day18;
	/** Day 19 work status: See {@link #day1}. */
	private String day19;
	/** Day 20 work status: See {@link #day1}. */
	private String day20;
	/** Day 21 work status: See {@link #day1}. */
	private String day21;
	/** The total number of work days. */
	private Integer work;
	/** The total number of Hold days. */
	private Integer hold;
	/** The total number of travel days. */
	private Integer travel;
	/** The total number of holiday days. */
	private Integer holiday;
	/** The first work date for this element. */
	private Date start;
	/** The last work date for this element. */
	private Date finish;
	/** The total number of work + travel + holiday days. */
	private Integer total;
	/** The ScriptElement.id field (database id) for the related element. */
	private Integer scriptElementId;

	// Constructors

	/** default constructor */
	public DoodReport() {
	}

	/** full constructor */
/*	public DoodReport(String reportId, Integer segment, Integer type, String typeName,
			Integer sequence, String person, String castId, String elementName,
			String day1, String day2, String day3, String day4, String day5, String day6,
			String day7, String day8, String day9, String day10, String day11,
			String day12, String day13, String day14, String day15, String day16,
			String day17, String day18, String day19, String day20, String day21,
			Integer work, Integer hold, Integer travel, Integer holiday, Date start,
			Date finish, Integer total, Integer scriptElementId) {
		this.reportId = reportId;
		this.segment = segment;
		this.type = type;
		this.typeName = typeName;
		this.sequence = sequence;
		this.person = person;
		this.castId = castId;
		this.elementName = elementName;
		this.day1 = day1;
		this.day2 = day2;
		this.day3 = day3;
		this.day4 = day4;
		this.day5 = day5;
		this.day6 = day6;
		this.day7 = day7;
		this.day8 = day8;
		this.day9 = day9;
		this.day10 = day10;
		this.day11 = day11;
		this.day12 = day12;
		this.day13 = day13;
		this.day14 = day14;
		this.day15 = day15;
		this.day16 = day16;
		this.day17 = day17;
		this.day18 = day18;
		this.day19 = day19;
		this.day20 = day20;
		this.day21 = day21;
		this.work = work;
		this.hold = hold;
		this.travel = travel;
		this.holiday = holiday;
		this.start = start;
		this.finish = finish;
		this.total = total;
		this.id = scriptElementId;
	}
*/
	// Property accessors

	/** See {@link #reportId}. */
	@Column(name = "report_id", length = 30)
	public String getReportId() {
		return this.reportId;
	}
	/** See {@link #reportId}. */
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

	@Column(name = "type")
	public Integer getType() {
		return this.type;
	}
	public void setType(Integer type) {
		this.type = type;
	}

	@Column(name = "type_name", length = 50)
	public String getTypeName() {
		return this.typeName;
	}
	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	@Column(name = "sequence")
	public Integer getSequence() {
		return this.sequence;
	}
	public void setSequence(Integer sequence) {
		this.sequence = sequence;
	}

	@Column(name = "person", length = 65)
	public String getPerson() {
		return this.person;
	}
	public void setPerson(String person) {
		this.person = person;
	}

	@Column(name = "cast_id", length = 10)
	public String getCastId() {
		return this.castId;
	}
	public void setCastId(String castId) {
		this.castId = castId;
	}

	@Column(name = "cast_id_num")
	public Integer getCastIdNum() {
		return castIdNum;
	}
	public void setCastIdNum(Integer id) {
		castIdNum = id;
	}

	@Column(name = "element_name", length = 100)
	public String getElementName() {
		return this.elementName;
	}
	public void setElementName(String elementName) {
		this.elementName = elementName;
	}

	@Transient
	public String getDay(int daynum) {
		String day = null;
		switch(daynum) {
			case 1:  day = day1;  break;
			case 2:  day = day2;  break;
			case 3:  day = day3;  break;
			case 4:  day = day4;  break;
			case 5:  day = day5;  break;
			case 6:  day = day6;  break;
			case 7:  day = day7;  break;
			case 8:  day = day8;  break;
			case 9:  day = day9;  break;
			case 10: day = day10; break;
			case 11: day = day11; break;
			case 12: day = day12; break;
			case 13: day = day13; break;
			case 14: day = day14; break;
			case 15: day = day15; break;
			case 16: day = day16; break;
			case 17: day = day17; break;
			case 18: day = day18; break;
			case 19: day = day19; break;
			case 20: day = day20; break;
			case 21: day = day21; break;
		}
		return day;
	}
	public void setDay(int daynum, String day) {
		switch(daynum) {
			case 1:  day1  = day;  break;
			case 2:  day2  = day;  break;
			case 3:  day3  = day;  break;
			case 4:  day4  = day;  break;
			case 5:  day5  = day;  break;
			case 6:  day6  = day;  break;
			case 7:  day7  = day;  break;
			case 8:  day8  = day;  break;
			case 9:  day9  = day;  break;
			case 10: day10 = day;  break;
			case 11: day11 = day;  break;
			case 12: day12 = day;  break;
			case 13: day13 = day;  break;
			case 14: day14 = day;  break;
			case 15: day15 = day;  break;
			case 16: day16 = day;  break;
			case 17: day17 = day;  break;
			case 18: day18 = day;  break;
			case 19: day19 = day;  break;
			case 20: day20 = day;  break;
			case 21: day21 = day;  break;
		}
	}

	@Column(name = "day1", length = 4)
	public String getDay1() {
		return this.day1;
	}
	public void setDay1(String day1) {
		this.day1 = day1;
	}

	@Column(name = "day2", length = 4)
	public String getDay2() {
		return this.day2;
	}
	public void setDay2(String day2) {
		this.day2 = day2;
	}

	@Column(name = "day3", length = 4)
	public String getDay3() {
		return this.day3;
	}
	public void setDay3(String day3) {
		this.day3 = day3;
	}

	@Column(name = "day4", length = 4)
	public String getDay4() {
		return this.day4;
	}
	public void setDay4(String day4) {
		this.day4 = day4;
	}

	@Column(name = "day5", length = 4)
	public String getDay5() {
		return this.day5;
	}
	public void setDay5(String day5) {
		this.day5 = day5;
	}

	@Column(name = "day6", length = 4)
	public String getDay6() {
		return this.day6;
	}
	public void setDay6(String day6) {
		this.day6 = day6;
	}

	@Column(name = "day7", length = 4)
	public String getDay7() {
		return this.day7;
	}
	public void setDay7(String day7) {
		this.day7 = day7;
	}

	@Column(name = "day8", length = 4)
	public String getDay8() {
		return this.day8;
	}
	public void setDay8(String day8) {
		this.day8 = day8;
	}

	@Column(name = "day9", length = 4)
	public String getDay9() {
		return this.day9;
	}
	public void setDay9(String day9) {
		this.day9 = day9;
	}

	@Column(name = "day10", length = 4)
	public String getDay10() {
		return this.day10;
	}
	public void setDay10(String day10) {
		this.day10 = day10;
	}

	@Column(name = "day11", length = 4)
	public String getDay11() {
		return this.day11;
	}
	public void setDay11(String day11) {
		this.day11 = day11;
	}

	@Column(name = "day12", length = 4)
	public String getDay12() {
		return this.day12;
	}
	public void setDay12(String day12) {
		this.day12 = day12;
	}

	@Column(name = "day13", length = 4)
	public String getDay13() {
		return this.day13;
	}
	public void setDay13(String day13) {
		this.day13 = day13;
	}

	@Column(name = "day14", length = 4)
	public String getDay14() {
		return this.day14;
	}
	public void setDay14(String day14) {
		this.day14 = day14;
	}

	@Column(name = "day15", length = 4)
	public String getDay15() {
		return this.day15;
	}
	public void setDay15(String day15) {
		this.day15 = day15;
	}

	@Column(name = "day16", length = 4)
	public String getDay16() {
		return this.day16;
	}
	public void setDay16(String day16) {
		this.day16 = day16;
	}

	@Column(name = "day17", length = 4)
	public String getDay17() {
		return this.day17;
	}
	public void setDay17(String day17) {
		this.day17 = day17;
	}

	@Column(name = "day18", length = 4)
	public String getDay18() {
		return this.day18;
	}
	public void setDay18(String day18) {
		this.day18 = day18;
	}

	@Column(name = "day19", length = 4)
	public String getDay19() {
		return this.day19;
	}
	public void setDay19(String day19) {
		this.day19 = day19;
	}

	@Column(name = "day20", length = 4)
	public String getDay20() {
		return this.day20;
	}
	public void setDay20(String day20) {
		this.day20 = day20;
	}

	@Column(name = "day21", length = 4)
	public String getDay21() {
		return this.day21;
	}
	public void setDay21(String day21) {
		this.day21 = day21;
	}

	@Column(name = "work")
	public Integer getWork() {
		return this.work;
	}
	public void setWork(Integer work) {
		this.work = work;
	}

	@Column(name = "hold")
	public Integer getHold() {
		return this.hold;
	}
	public void setHold(Integer hold) {
		this.hold = hold;
	}

	@Column(name = "travel")
	public Integer getTravel() {
		return this.travel;
	}
	public void setTravel(Integer travel) {
		this.travel = travel;
	}

	@Column(name = "holiday")
	public Integer getHoliday() {
		return this.holiday;
	}
	public void setHoliday(Integer holiday) {
		this.holiday = holiday;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "start", length = 10)
	public Date getStart() {
		return this.start;
	}
	public void setStart(Date start) {
		this.start = start;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "finish", length = 10)
	public Date getFinish() {
		return this.finish;
	}
	public void setFinish(Date finish) {
		this.finish = finish;
	}

	@Column(name = "total")
	public Integer getTotal() {
		return this.total;
	}
	public void setTotal(Integer total) {
		this.total = total;
	}

	@Transient
	public void setFromElementDood(ElementDood edood) {

		this.setStart(edood.getFirstWorkDate());
		this.setFinish(edood.getLastWorkDate());

		this.setHold(edood.getHoldDays());
		this.setHoliday(edood.getHolidayDays());
		this.setTravel(edood.getTravelDays());
		this.setWork(edood.getWorkDays());

		this.setTotal(work+hold+travel+holiday);
	}

	@Column(name = "scriptElement_Id")
	public Integer getScriptElementId() {
		return scriptElementId;
	}
	public void setScriptElementId(Integer scriptElementId) {
		this.scriptElementId = scriptElementId;
	}

}