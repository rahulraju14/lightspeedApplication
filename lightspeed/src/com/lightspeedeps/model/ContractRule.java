package com.lightspeedeps.model;

import java.util.Date;
import java.util.List;

import javax.persistence.*;

import com.lightspeedeps.object.RuleFilter;
import com.lightspeedeps.type.*;

/**
 * ContractRule entity. This defines a set of conditions and a "supporting rule"
 * to be applied to a {@link WeeklyTimecard} during HTG processing if that
 * timecard (and its related {@link StartForm}) matches the specified conditions.
 * <p>
 * The "supporting rule" is defined by the fields {@link #useRuleKey} and
 * {@link #ruleType}; the ruleType defines the model class of the rule to be
 * applied (e.g., {@link OvertimeRule}, and the useRuleKey is the unique name of
 * a single rule within that model class (table).
 * <p>
 * The remaining fields of this entity specify the set of conditions that
 * determine whether this rule is to be applied. Some of the values apply to
 * fields within the timecard (e.g., {@link #dayType}) , while others apply to
 * values of the related StartForm or Production (e.g., {@link #medium}).
 */
@Entity
@Table(name = "contract_rule")
public class ContractRule extends Rule {

	private static final long serialVersionUID = 1L;

	/** 'Agreement' field value for non-union TV/Feature production HTG */
	//public static final String AGREEMENT_TV_FEATURE = "TVF";

	/** 'Agreement' field value for non-union Commercial production HTG */
	//public static final String AGREEMENT_COMMERCIAL = "COM";

	/** Schedule code used in contract rules for non-union, non-exempt (hourly). */
	public static final String SCHEDULE_NON_UNION_HOURLY = "HR";

	/** Schedule code used in contract rules for non-union, exempt (includes both daily and weekly). */
	public static final String SCHEDULE_NON_UNION_EXEMPT = "EX";

	/** Schedule code used in contract rules for non-union, exempt weekly. */
	public static final String SCHEDULE_NON_UNION_WEEKLY = "WK";

	/** Schedule code used in contract rules for non-union, exempt daily. */
	public static final String SCHEDULE_NON_UNION_DAILY  = "DY";

	/** A field value indicating that the field is not restrictive,
	 * that is, the containing rule applies to all possible values of
	 * that particular field. */
	public static final String RULE_FIELD_NA = "N_A";

	/** dayNumber field value indicating that the rule applies only to work day
	 * numbers 1 through 5. */
	public static final String DAY_NUMBER_ONE_TO_FIVE = "15";

	/** dayNumber field value indicating that the rule applies only to work day
	 * numbers 1 through 6. */
	public static final String DAY_NUMBER_ONE_TO_SIX = "16";

	/** dayNumber field value indicating that the rule applies only to work day
	 * numbers 5 and 6. */
	public static final String DAY_NUMBER_FIVE_OR_SIX = "56";

	/** dayNumber field value indicating that the rule applies only to work day
	 * numbers 6 and 7. */
	public static final String DAY_NUMBER_SIX_OR_SEVEN = "67";


	// Fields

	/** Which agreements (contracts) this rule applies to.  This field
	 * matches values in the PayRate.contractCode field.  A value of "N_A"
	 * indicates the rule applies to all agreements. */
	private String agreement;

	/** The number of the 'side-letter' which 'generated' this rule.  This rule
	 * will only be applied if a Production's preferences settings indicate
	 * that the particular sideLetter is in use. */
	private String sideLetter;

	/** Which union this rule applies to.  This field matches values in the
	 * Occupations.unionCode and Unions.unionKey fields.  A value of "N_A"
	 * indicates the rule applies to all agreements.*/
	private String unionKey;

	/** Which occupations this rule applies to; blank or null if it applies to
	 * all occupations. This field matches values in the Occupation.lsOccCode field.
	 * A value of "N_A" indicates the rule applies to all agreements. */
	private String occCode;

	/** Which contract (letter) schedule this rule applies to.  This field
	 * matches values in the PayRate.contractSchedule field.
	 * A value of "N_A" indicates the rule applies to all agreements. */
	private String schedule;

	/** The area where the work was done, e.g., Stage.  See the {@link WorkZone} enum
	 * class for values. */
	private WorkZone workZone;

	/** The On-production vs Off-production attribute for this rule.  Values are ON,
	 * OP, or N_A. A value of "N_A" indicates the rule applies to either condition. */
	private DayType onProduction;

	/** Indicates if this rule applies to TV, Feature, or both types of productions. */
	private MediumType medium;

	/** Which day types (from the timecard's daily entry) this rule applies to.
	 * A value of "N_A" indicates the rule applies to all DayType values. */
	private DayType dayType;

	/** Indicates which working days this rule applies to. A value of "N_A"
	 * indicates the rule applies to all days. Otherwise it is a String of one
	 * or more day numbers, such as "6", "136", or "67", where each of the
	 * numbers are days that the rule applies; except for the special values
	 * "15", indicating days one through five, and "16", indicating days one
	 * through six. The numbers are working-day numbers, not the sequential day
	 * number within the work week. For example, when the production work week
	 * begins on Monday, then Monday is working-day 1, but Sunday could be
	 * working-day 6 or 7, depending on circumstances. */
	private String dayNumber;

	/** If not null, the first date on which this rule may be applied. */
	private Date firstEffectiveDate;

	/** If not null, the last date on which this rule may be applied. */
	private Date lastEffectiveDate;
	/**
	 * May contain a list of additional restrictions (filters) that control
	 * under what conditions the rule is applied. The text is a list of
	 * comparisons separated by semicolons, such as "WD=Fri;CL>20.0".
	 * <p>
	 * Each comparison is of the form "< field > < operator > < value >", where
	 * < field > is a value of {@link com.lightspeedeps.type.RuleField
	 * RuleField}, < operator > is a value of
	 * {@link com.lightspeedeps.type.RuleOperator RuleOperator}, and < value > is
	 * usually a numeric or text literal, depending on the data type of the <
	 * field >.
	 * <p>
	 * During rule application, each comparison is converted to an instance of a
	 * {@link com.lightspeedeps.object.RuleFilter RuleFilter}.
	 */
	private String extraFilters;

	/** The type of supporting rule to be invoked when a particular timecard, or
	 * day within a timecard, matches this contractRule's conditions.
	 * The RuleType normally determines in which table the supporting rule is
	 * stored, such as OvertimeRule or GoldenRule.
	 * @see RuleType */
	private RuleType ruleType;

	/** The key (unique name) of the supporting rule to be used when this contract rule
	 * applies. */
	private String useRuleKey;

	/** Priority of the rule: if multiple rules of the same type qualify, the one with the
	 * highest priority will be used. */
	private int priority;

	/** The List of RuleFilter`s that corresponds to the {@link #extraFilters} text. */
	@Transient
	private List<RuleFilter> ruleFilters;

	/** If true, the ruleFilters are 'AND'd together; if false, they are 'OR'd together. */
	@Transient
	private boolean andFilters = true;

	// Constructors

	/** default constructor */
	public ContractRule() {
	}

	/**
	 * Constructor used for unit testing. This creates a rule that is as broad
	 * as possible -- all qualifiers are set to include all values (e.g.,
	 * "N_A").
	 *
	 * @param rt The type of rule being referenced.
	 * @param ruleKey The key/name of the rule referenced.
	 */
	public ContractRule(RuleType rt, String ruleKey) {
		ruleType = rt;
		useRuleKey = ruleKey;
		workZone = WorkZone.N_A;
		onProduction = DayType.N_A;
		dayType = DayType.N_A;
		medium = MediumType.N_A;

		schedule = "N_A";
		agreement = "N_A";
		dayNumber = "N_A";
	}

	/**
	 * Determine if the given day number matches the Day Number specification in
	 * this rule.
	 *
	 * @param dayNum The day number to be tested; should be from 1-7.
	 * @return True iff the given day number matches one of the possible day
	 *         numbers specified in this rule's {@link #dayNumber} field.
	 */
	public boolean matchesDayNumber(int dayNum) {
		boolean b = true;
		if (getDayNumber().indexOf("" + dayNum) < 0) {
			b = false;
			switch(getDayNumber()) {
				case DAY_NUMBER_ONE_TO_FIVE:
					b = dayNum < 6;
					break;
				case DAY_NUMBER_ONE_TO_SIX:
					b = dayNum < 7;
					break;
				case DAY_NUMBER_FIVE_OR_SIX:
				case DAY_NUMBER_SIX_OR_SEVEN:
					// must be false since "indexOf" test above failed.
					break;
			}
		}
		return b;
	}
	// Property accessors

	/** See {@link #agreement}. */
	@Column(name = "Agreement", nullable = false, length = 30)
	public String getAgreement() {
		return agreement;
	}
	/** See {@link #agreement}. */
	public void setAgreement(String agreement) {
		this.agreement = agreement;
	}

	/**
	 * @return the state or province for which this rule entry applies. This is
	 *         only used for non-union processing. Currently it is a synonym for
	 *         the {@link #agreement} value, since non-union Start Forms do not have a
	 *         agreement value, but do have an {@link StartForm#overtimeRule}
	 *         value. That "overtimeRule" value indicates either (a) which state's
	 *         rules should be applied, or (b) a code indicating to use the
	 *         {@link WeeklyTimecard#stateWorked}.
	 */
	@Transient
	public String getState() {
		return getAgreement();
	}

	/** See {@link #sideLetter}. */
	@Column(name = "Side_Letter", nullable = true, length = 20)
	public String getSideLetter() {
		return sideLetter;
	}
	/** See {@link #sideLetter}. */
	public void setSideLetter(String sideLetter) {
		this.sideLetter = sideLetter;
	}

	@Column(name = "Union_Key", nullable = false, length = 10)
	public String getUnionKey() {
		return unionKey;
	}
	public void setUnionKey(String unionKey) {
		this.unionKey = unionKey;
	}

	@Column(name = "Occ_Code", nullable = false, length = 10)
	public String getOccCode() {
		return occCode;
	}
	public void setOccCode(String occCode) {
		this.occCode = occCode;
	}

	@Column(name = "Schedule", nullable = false, length = 10)
	public String getSchedule() {
		return schedule;
	}
	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}

	/** See {@link #workZone}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Locality", nullable = false, length = 10)
	public WorkZone getWorkZone() {
		return workZone;
	}
	/** See {@link #workZone}. */
	public void setWorkZone(WorkZone workZone) {
		this.workZone = workZone;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "On_Production", nullable = false, length = 10)
	public DayType getOnProduction() {
		return onProduction;
	}
	public void setOnProduction(DayType onProduction) {
		this.onProduction = onProduction;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Medium", nullable = false, length = 10)
	public MediumType getMedium() {
		return medium;
	}
	public void setMedium(MediumType medium) {
		this.medium = medium;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Day_Type", nullable = false, length = 10)
	public DayType getDayType() {
		return dayType;
	}
	public void setDayType(DayType dayType) {
		this.dayType = dayType;
	}

	@Column(name = "Day_Number", nullable = false, length = 10)
	public String getDayNumber() {
		return dayNumber;
	}
	public void setDayNumber(String dayNumber) {
		this.dayNumber = dayNumber;
	}

	/** See {@link #firstEffectiveDate}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "First_Effective_Date", nullable = true, length = 0)
	public Date getFirstEffectiveDate() {
		return firstEffectiveDate;
	}
	/** See {@link #firstEffectiveDate}. */
	public void setFirstEffectiveDate(Date firstEffectiveDate) {
		this.firstEffectiveDate = firstEffectiveDate;
	}

	/** See {@link #lastEffectiveDate}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "Last_Effective_Date", nullable = true, length = 0)
	public Date getLastEffectiveDate() {
		return lastEffectiveDate;
	}
	/** See {@link #lastEffectiveDate}. */
	public void setLastEffectiveDate(Date lastEffectiveDate) {
		this.lastEffectiveDate = lastEffectiveDate;
	}

	@Column(name = "Extra_Filters", length = 500)
	public String getExtraFilters() {
		return extraFilters;
	}
	public void setExtraFilters(String extraFilters) {
		this.extraFilters = extraFilters;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Rule_Type", nullable = false, length = 20)
	public RuleType getRuleType() {
		return ruleType;
	}
	public void setRuleType(RuleType ruleType) {
		this.ruleType = ruleType;
	}

	@Column(name = "Use_Rule_Key", nullable = false, length = 20)
	public String getUseRuleKey() {
		return useRuleKey;
	}
	public void setUseRuleKey(String useRuleKey) {
		this.useRuleKey = useRuleKey;
	}

	/** See {@link #priority}. */
	@Column(name = "Priority", nullable = false)
	public int getPriority() {
		return priority;
	}
	/** See {@link #priority}. */
	public void setPriority(int priority) {
		this.priority = priority;
	}

	/**See {@link #ruleFilters}. */
	@Transient
	public List<RuleFilter> getRuleFilters() {
		return ruleFilters;
	}
	/**See {@link #ruleFilters}. */
	public void setRuleFilters(List<RuleFilter> ruleFilters) {
		this.ruleFilters = ruleFilters;
	}

	/** See {@link #andFilters}. */
	@Transient
	public boolean getAndFilters() {
		return andFilters;
	}
	/** See {@link #andFilters}. */
	public void setAndFilters(boolean andFilters) {
		this.andFilters = andFilters;
	}

	/* (non-Javadoc)
	 * @see com.lightspeedeps.model.Rule#toString()
	 */
	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "select=";
		s += getAgreement() + "/";
		s += getSideLetter() + "/";
		s += getUnionKey() + "/";
		s += getOccCode() + "/";
		s += getSchedule() + "/";
		s += getWorkZone() + "/";
		s += getOnProduction() + "/";
		s += getMedium() + "/";
		s += getDayType() + "/";
		s += getDayNumber() + "/";
		s += getExtraFilters() + "/";
		s += ", rule=" + (getUseRuleKey()==null ? "null" : getUseRuleKey());
		s += "]";
		return s;
	}

}
