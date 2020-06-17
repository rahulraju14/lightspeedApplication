package com.lightspeedeps.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.type.HolidayType;
import com.lightspeedeps.type.RuleType;

/**
 * HolidayListRule entity.  These rules determine which holidays are recognized
 * under a particular agreement.
 */
@Entity
@Table(name = "holiday_list_rule", uniqueConstraints = @UniqueConstraint(columnNames = "Rule_Key"))
public class HolidayListRule extends Rule implements java.io.Serializable {
	private static final Log log = LogFactory.getLog(HolidayListRule.class);

	private static final long serialVersionUID = 1L;

	// Fields

	/** A String of HolidayType Enum values such as "NWYR,,PRES,,GDFR....", representing
	 * the holidays that are recognized for this particular rule. */
	private String holidayString;

	/** The List of holidays recognized for this rule, represented as HolidayType`s. */
	@Transient
	private List<HolidayType> holidays;

	// Constructors

	/** default constructor */
	public HolidayListRule() {
		setType(RuleType.HL);
	}

//	/** minimal constructor */
//	public HolidayListRule(String ruleKey) {
//		super(ruleKey);
//	}

	// Property accessors

	/**See {@link #holidayString}. */
	@Column(name = "Holiday_String")
	public String getHolidayString() {
		return holidayString;
	}
	/**See {@link #holidayString}. */
	public void setHolidayString(String holidayString) {
		this.holidayString = holidayString;
	}

	/**See {@link #holidays}. */
	@Transient
	public List<HolidayType> getHolidays() {
		if (holidays == null) {
			createHolidays();
		}
		return holidays;
	}

	/**
	 * @param ht The HolidayType to check for.
	 * @return True if this rule includes the specified holiday.
	 */
	public boolean includesHoliday(HolidayType ht) {
		return getHolidays().indexOf(ht) >= 0;
	}

	/**
	 * Create our List of HolidayType values based on the String of holiday
	 * Enum values stored in the database.
	 */
	private void createHolidays() {
		holidays = new ArrayList<HolidayType>();
		String[] hols = StringUtils.split(holidayString, ',');
		for (String hol : hols) {
			try {
				HolidayType ht = HolidayType.valueOf(hol.trim().toUpperCase());
				holidays.add(ht);
			}
			catch (Exception e) {
				log.error("invalid holiday enumeration code in holiday list, key=" + getRuleKey());
			}
		}
	}

}
