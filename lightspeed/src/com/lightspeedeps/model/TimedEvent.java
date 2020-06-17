/**
 * TimedEvent.java
 */
package com.lightspeedeps.model;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.type.TimedEventType;
import com.lightspeedeps.util.app.Constants;

/**
 * Each instance represents a time-stamped event related to another object. This
 * is currently used for StartForm and WeeklyTimecard events, such as such as
 * submission or approval, and document editing events.
 *
 * @see com.lightspeedeps.type.TimedEventType
 * @see com.lightspeedeps.model.SignedEvent
 */
@SuppressWarnings("rawtypes")
@MappedSuperclass
public class TimedEvent<T extends PersistentObject> extends PersistentObject<T> {

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(TimedEvent.class);

	private static final long serialVersionUID = 1L;

	// Fields

	/** The date & time this event was recorded. */
	private Date date;

	/** The LightSPEED account number associated with the person who caused
	 * this event. */
	private String userAccount;

	/** The last name of the person who caused this event, taken from their
	 * LightSPEED account information. */
	private String lastName;

	/** The first name of the person who caused this event, taken from their
	 * LightSPEED account information. */
	private String firstName;

	/** The type of event, e.g., Approval or Rejection. */
	private TimedEventType type;

	/** The formatted text to display, a transient field. */
	@Transient
	protected String display;

	/** The formatted date & time to display, a transient field. */
	@Transient
	private String displayTime;

	/**
	 * Default constructor.
	 */
	public TimedEvent() {
	}

	/** minimal constructor */
	public TimedEvent(Date date) {
		this.date = date;
	}

	/**
	 * Parameterized constructor
	 *
	 * @param user User causing this event, typically the current user.
	 * @param eventType Timed Event Type usually CHANGE type
	 */
	public TimedEvent(User user, TimedEventType eventType) {
		setDate(new Date());
		setType(eventType);
		if (user != null) {
			setFirstName(user.getFirstName());
			setLastName(user.getLastName());
			setUserAccount(user.getAccountNumber());
		}
		else {
			setFirstName("");
			setLastName("");
			setUserAccount("");
		}
	}

	@Column(name = "Date", nullable = false, length = 19)
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}

	@Column(name = "User_Account", length = 20)
	public String getUserAccount() {
		return userAccount;
	}
	public void setUserAccount(String userAccount) {
		this.userAccount = userAccount;
	}

	@Column(name = "Last_Name", length = 30)
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@Column(name = "First_Name", length = 30)
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Type", length = 30)
	public TimedEventType getType() {
		return type;
	}
	public void setType(TimedEventType t) {
		type = t;
	}

	@Transient
	public String getName() {
		return getFirstName() + " " + getLastName();
	}

	/**
	 *  @return the initials of the signer.
	 */
	@Transient
	public String getInitials() {
		String result = "";
		if (getFirstName() != null) {
			result = getFirstName().substring(0, 1);
		}
		if (getLastName() != null) {
			result += getLastName().substring(0, 1);
		}
		return result;
	}

	/**
	 * @return A formatted version of the time at which this event occurred.
	 *         Currently formatted as "MM/dd/yyyy HH:mm:ss".
	 */
	@Transient
	public String getDisplayTime() {
		if (displayTime == null) {
			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
			displayTime = sdf.format(getDate());
		}
		return displayTime;
	}

	/**
	 * @return A standard formatted display of this event, including the time of
	 *         the event, the type of the event, and the name of the user who
	 *         caused the event.
	 */
	@Transient
	public String getDisplay() {
		if (display == null) {
			display = getDisplayTime() + " - " + getType().getLabel();
			if (getType() != TimedEventType.DECLINE) {
				display += " by " + getName();
			}
		}
		return display;
	}

	/**
	 * @return A standard formatted display of this event, including the time of
	 *         the event and the name of the user who caused the event.
	 */
	@Transient
	public String getSignedBy() {
		if (display == null) {
			display = Constants.SIGNED_BY + getName() + " " + getDisplayTime();
		}
		return display;
	}

}
