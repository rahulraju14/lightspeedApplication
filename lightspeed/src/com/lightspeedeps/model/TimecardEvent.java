package com.lightspeedeps.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.port.Exporter;
import com.lightspeedeps.type.TimecardSubmitType;
import com.lightspeedeps.type.TimedEventType;

/**
 * TimecardEvent entity. Each instance represents an event related to an individual timecard,
 * such as submission or approval.
 * @see com.lightspeedeps.type.TimedEventType
 */
@Entity
@Table(name = "time_card_event")
public class TimecardEvent extends SignedEvent<TimecardEvent> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(TimecardEvent.class);

	private static final long serialVersionUID = 1L;

	/** The value in 'field' when the old & new value fields are Approver.id values. */
	public static final short FIELD_APPROVER = -1;
	/** The value in 'field' when the old & new value fields are User.id values. */
	public static final short FIELD_USER = -2;

	/** Used as an out-of-line approver indicator in the dayNum field. */
	public static final byte DAY_OUT_OF_LINE = 10;

	// Fields

	/** The WeeklyTimecard to which this event applies. */
	private WeeklyTimecard weeklyTimecard;

	/** The day number that this event is associated with; used for Change events. */
	private byte dayNum;

	/** The field (in the WeeklyTimeCard) that this event is associated with; used
	 * for Change events. */
	private Short field;

	/** For Change events, an old identifier, typically Approver or User (prior to the event). */
	private Integer oldId;

	/** For Change events, a new identifier, typically Approver or User (after to the event). */
	private Integer newId;

	/** A value that represents the "sub-type" of Submit that was done.  This
	 * value is actually stored in the dayNum field, which is otherwise unused
	 * in a Submit event. */
	@Transient
	private TimecardSubmitType submitType;

	// Constructors

	/** default constructor */
	public TimecardEvent() {
	}

	/** minimal constructor */
	public TimecardEvent(WeeklyTimecard weeklyTimecard, Date date) {
		super(date);
		this.weeklyTimecard = weeklyTimecard;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Weekly_Id", nullable = false)
	public WeeklyTimecard getWeeklyTimecard() {
		return weeklyTimecard;
	}
	public void setWeeklyTimecard(WeeklyTimecard wtc) {
		weeklyTimecard = wtc;
	}

	@Column(name = "Day_Num")
	public byte getDayNum() {
		return dayNum;
	}
	public void setDayNum(byte dayNum) {
		this.dayNum = dayNum;
	}

	@Column(name = "Field")
	public Short getField() {
		return field;
	}
	public void setField(Short field) {
		this.field = field;
	}

	@Column(name = "Old_Id")
	public Integer getOldId() {
		return oldId;
	}
	public void setOldId(Integer oldValue) {
		oldId = oldValue;
	}

	@Column(name = "New_Id")
	public Integer getNewId() {
		return newId;
	}
	public void setNewId(Integer newValue) {
		newId = newValue;
	}

	@Transient
	/** See {@link #submitType}. */
	public TimecardSubmitType getSubmitType() {
		TimecardSubmitType type;
		try {
			type = TimecardSubmitType.values()[getDayNum()];
		}
		catch (Exception e) {
			type = TimecardSubmitType.NORMAL;
		}
		return type;
	}
	public void setSubmitType(TimecardSubmitType type) {
		setDayNum((byte)type.ordinal());
	}

	@Override
	@Transient
	public String getDisplay() {
		if (display == null) {
			String text = super.getDisplay();
			if (getType() == TimedEventType.SUBMIT && getSubmitType() != TimecardSubmitType.NORMAL) {
				text += " on behalf of " + getWeeklyTimecard().getLastNameFirstName();
				switch(getSubmitType()) {
				case PAPER_SIGNATURE:
					text += ". Employee submitted a paper timecard.";
					break;
				case UNDER_CONTRACT:
					text += ". Employee is under contract.";
					break;
				case OTHER:
					text += ". See Comments for reason.";
					break;
				case NORMAL:
					break;
				}
			}
			else if (getType() == TimedEventType.RECALL ||
					getType() == TimedEventType.PULL) {
				if (getOldId() != null) {
					User user = UserDAO.getInstance().findById(getOldId());
					text += " from " + user.getDisplayName();
				}
			}
			else if (getType() == TimedEventType.REJECT) {
				if (getNewId() != null) {
					User user = UserDAO.getInstance().findById(getNewId());
					text += " to " + user.getDisplayName();
				}
			}

			display = text;
		}
		return display;
	}


	/**
	 * Export data in this instance via an Exporter. This is currently used to
	 * turn this object into a flat record. The timecard data can then be loaded
	 * into other products, such as Crew Cards.
	 *
	 * @param ex The Exporter to which each field should be passed.
	 */
	@Override
	public void flatten(Exporter ex) {
		super.flatten(ex);
		if (getSubmitType() != null) {
			ex.append(getSubmitType().name());
		}
		else {
			ex.append("");
		}
	}

}
