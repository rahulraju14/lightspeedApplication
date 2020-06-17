package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import com.lightspeedeps.type.TimecardFieldType;
import com.lightspeedeps.type.TimedEventType;

/**
 * A class for recording events that change data on a WeeklyTimecard.
 */
@NamedQueries ({
	@NamedQuery(name=TimecardChangeEvent.GET_TIMECARD_CHANGE_EVENT_LIST,
		query = "from TimecardChangeEvent tc where tc.timecardId =:weeklyTimecardId order by tc.date DESC")
})

@Entity
@Table(name = "timecard_change_event")
public class TimecardChangeEvent extends ChangeEvent {

	private static final long serialVersionUID = -2933902057962222324L;

	public static final String GET_TIMECARD_CHANGE_EVENT_LIST = "getTimecardChangeEventList";

	private Integer timecardId;

	private TimecardFieldType fieldType;

	private int lineNumber;

	private String description;

	/**
	 * Default constructor
	 */
	public TimecardChangeEvent() {
	}

	/**
	 * Parameterized constructor
	 *
	 * @param user current user
	 * @param eventType Timed Event Type usually CHANGE type
	 */
	public TimecardChangeEvent(User user, TimedEventType eventType) {
		super(user, eventType);
	}

	/**
	 * Constructor with most fields, excluding the "old" and "new" values.
	 *
	 * @param user The User responsible for (causing) the event.
	 * @param eventType The type of the event.
	 * @param tcId The database id of the related object.
	 * @param fldType The type of field being modified or otherwise affected.
	 * @param lineNum The line number, where relevant. Use -1 if not relevant.
	 * @param desc A textual description of the event.
	 */
	public TimecardChangeEvent(User user, TimedEventType eventType, Integer tcId,
			TimecardFieldType fldType, int lineNum, String desc) {
		super(user, eventType);
		setTimecardId(tcId);
		setLineNumber(lineNum);
		setFieldType(fldType);
		setDescription(desc);
	}

	/**
	 * Full constructor.
	 *
	 * @param user The User responsible for (causing) the event.
	 * @param eventType The type of the event.
	 * @param tcId The database id of the related object.
	 * @param fldType The type of field being modified or otherwise affected.
	 * @param lineNum The line number, where relevant. Use -1 if not relevant.
	 * @param desc A textual description of the event.
	 * @param oldVal The "old value" -- the field value before the event.
	 * @param newVal The "new value" -- the field value after the event.
	 */
	public TimecardChangeEvent(User user, TimedEventType eventType, Integer tcId,
			TimecardFieldType fldType, int lineNum, String desc, Object oldVal, Object newVal) {
		this(user, eventType, tcId, fldType, lineNum, desc);
		if (oldVal != null) {
			//log.debug("........field OldValue=" + oldVal.toString());
			setOldValue(oldVal.toString());
		}
		if (newVal != null) {
			//log.debug("......field NewValue=" + newVal.toString());
			setNewValue(newVal.toString());
		}
	}

	@Column(name = "Timecard_Id" , nullable = false)
	public Integer getTimecardId() {
		return timecardId;
	}
	public void setTimecardId(Integer timecardId) {
		this.timecardId = timecardId;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Field_Type" , nullable = false, length = 30)
	public TimecardFieldType getFieldType() {
		return fieldType;
	}
	public void setFieldType(TimecardFieldType fieldType) {
		this.fieldType = fieldType;
	}

	@Column(name = "Line_Number" , nullable = false)
	public int getLineNumber() {
		return lineNumber;
	}
	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	@Column(name = "Description" , length = 100)
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
}
