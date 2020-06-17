package com.lightspeedeps.object;

import java.math.BigDecimal;
import java.util.regex.*;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.*;

import com.lightspeedeps.dao.DateEventDAO;
import com.lightspeedeps.model.*;
import com.lightspeedeps.service.HtgData;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.payroll.*;

/**
 * RuleFilter implementation class. This contains one comparison, or filter,
 * that determines whether or not a contract rule will be applied in a
 * particular situation. Typically this means whether a rule will apply to a
 * particular day of a particular timecard, based on data in a particular
 * DailyTime object. In some cases, however, the test(s) may use data from the
 * Production or WeeklyTimecard objects.
 * <p>
 * The comparison is evaluated by applying the binary {@link #operator} to the
 * operands specified by the {@link #field} and the {@link #value}.
 */
public class RuleFilterImpl implements RuleFilter, java.io.Serializable {
	private static final Log log = LogFactory.getLog(RuleFilterImpl.class);

	/** */
	private static final long serialVersionUID = 1L;

	// Fields

	/** The field (typically in the timecard or StartForm) whose value
	 * will be tested by this filter.
	 * @see RuleField */
	private RuleField field;

	/** The operator to be used in the comparison between the field's
	 * value and the value stored in this filter.
	 * @see RuleOperator */
	private RuleOperator operator;

	/** The value against which the specified field will be compared. The
	 * actual class must match the class specified by the {@link #field}; it
	 * could be, for example, String, BigDecimal, or Integer. */
	private Object value;

	/** The Hours-to-gross data area, passed to our evaluation method. */
	private HtgData htg;

	// Constructors

	/** default constructor */
	public RuleFilterImpl() {
	}

	public RuleFilterImpl(RuleField fld, RuleOperator oper, Object val) {
		field = fld;
		operator = oper;
		value = val;
	}

	/**
	 * @see com.lightspeedeps.object.RuleFilter#getField()
	 */
	@Override
	public RuleField getField() {
		return field;
	}
	/**
	 * @see com.lightspeedeps.object.RuleFilter#setField(com.lightspeedeps.type.RuleField)
	 */
	@Override
	public void setField(RuleField compareField) {
		field = compareField;
	}

	/**
	 * @see com.lightspeedeps.object.RuleFilter#getOperator()
	 */
	@Override
	public RuleOperator getOperator() {
		return operator;
	}
	/**
	 * @see com.lightspeedeps.object.RuleFilter#setOperator(com.lightspeedeps.type.RuleOperator)
	 */
	@Override
	public void setOperator(RuleOperator operator) {
		this.operator = operator;
	}

	/**
	 * @see com.lightspeedeps.object.RuleFilter#getValue()
	 */
	@Override
	public Object getValue() {
		return value;
	}
	/**
	 * @see com.lightspeedeps.object.RuleFilter#setValue(java.lang.Object)
	 */
	@Override
	public void setValue(Object compareValue) {
		value = compareValue;
	}

	/**
	 * Evaluate this filter using the arguments provided. The result of an
	 * evaluation is always a boolean.
	 *
	 * @param prod The Production to use.
	 * @param dt The DailyTime to use.
	 * @param pHtg The Hours-to-gross shared data area.
	 * @return the boolean result of evaluating the filter expression.
	 */
	public boolean evaluate(Production prod, DailyTime dt, HtgData pHtg) {
		boolean b = true;
		htg = pHtg;
		WeeklyTimecard wtc = htg.getWeeklyTimecard();
		try {
			if (dt == null && getField().getDailyOnly()) {
				// no DailyTime, but the field requires it; assume rule is true.
				b = true;
			}
			else if (getOperator() == RuleOperator.NL) {
				b = (getValue() == null);
			}
			else if (getField().getClazz() == BigDecimal.class) {
				b = evaluateBigDecimal(dt, wtc);
			}
			else if (getField().getClazz() == Integer.class) {
				b = evaluateInt(dt, wtc);
			}
			else if (getField().getClazz() == Boolean.class) {
				b = evaluateBoolean(prod, dt, wtc);
			}
			else {
				b = evaluateString(prod, dt, wtc);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			log.error("rule=" + this);
		}
		return b;
	}

	/**
	 * Evaluate a comparison operation (our {@link #operator}) that expects two
	 * String operands ({@link #field} and {@link #value}) and returns a
	 * boolean. All comparison operations are valid.
	 * @param prod
	 *
	 * @param dt The DailyTime to use as a possible value source.
	 * @param wtc The WeeklyTimecard to use as a possible value source.
	 * @return True iff the String comparison represented by this RuleFilter
	 *         evaluates to true. All compares are done using the
	 *         String.compareToIgnoreCase() method, so the case of the operands
	 *         is irrelevant.
	 */
	private boolean evaluateString(Production prod, DailyTime dt, WeeklyTimecard wtc) {
		boolean b = true;
		String comp = (String)getValue();
		String left = getStringValue(getField(), prod, dt, wtc);
		boolean negate = false;
		switch(getOperator()) {
			case EQ:
				b = left.compareToIgnoreCase(comp) == 0;
				break;
			case GE:
				b = left.compareToIgnoreCase(comp) >= 0;
				break;
			case GT:
				b = left.compareToIgnoreCase(comp) > 0;
				break;
			case LE:
				b = left.compareToIgnoreCase(comp) <= 0;
				break;
			case LT:
				b = left.compareToIgnoreCase(comp) < 0;
				break;
			case NE:
				b = left.compareToIgnoreCase(comp) != 0;
				break;
			case CO:
				b = left.contains(comp);
				break;
			case NME:
				negate = true;
			case ME:
				// is "left" a member of "comp"? comp may be comma-delimited list.
				// If "left" is comma-delimited, the meaning is changed (used for TvType) --
				// ...the left list must contain all of the right-list elements to be true.
				if (comp.length() > 2) {
					// strip any surrounding braces or parens
					if (comp.startsWith("(") || comp.startsWith("{")) {
						comp = comp.substring(1, comp.length()-1);
					}
				}
				if (left.indexOf(',') >= 0) {
					String[] comps = comp.split(",");
					for (String cmp : comps) {
						b &= left.contains("," + cmp + ",");
					}
				}
				else {
					comp = "," + comp + ",";
					b = comp.contains("," + left + ",");
				}
				if (negate) {
					b = !b;
				}
				break;
			default:
				break;
		}
		return b;
	}

	/**
	 * Evaluate a comparison operation (our {@link #operator}) that expects two
	 * BigDecimal operands ({@link #field} and {@link #value}) and returns a
	 * boolean. All comparison operations are valid.
	 *
	 * @param dt The DailyTime to use as a possible value source.
	 * @param wtc The WeeklyTimecard to use as a possible value source.
	 * @return True iff the decimal comparison represented by this RuleFilter
	 *         evaluates to true. The comparison is done using the
	 *         BigDecimal.compareTo() method, so that numbers with different
	 *         scales can be compared properly.
	 */
	private boolean evaluateBigDecimal(DailyTime dt, WeeklyTimecard wtc) {
		boolean b = true;
		BigDecimal comp = (BigDecimal)getValue();
		BigDecimal left = getBigDecimalValue(getField(), dt, wtc);
		if (left == null) {
			return true;
		}
		switch(getOperator()) {
			case EQ:
				b = left.compareTo(comp) == 0;
				break;
			case GE:
				b = left.compareTo(comp) >= 0;
				break;
			case GT:
				b = left.compareTo(comp) > 0;
				break;
			case LE:
				b = left.compareTo(comp) <= 0;
				break;
			case LT:
				b = left.compareTo(comp) < 0;
				break;
			case NE:
				b = left.compareTo(comp) != 0;
				break;
			default:
				break;
		}
		return b;
	}


	/**
	 * Evaluate a comparison operation (our {@link #operator}) that expects two
	 * Integer operands ({@link #field} and {@link #value}) and returns a
	 * boolean. All comparison operations are valid.
	 *
	 * @param dt The DailyTime to use as a possible value source.
	 * @param wtc The WeeklyTimecard to use as a possible value source.
	 * @return True iff the integer comparison represented by this RuleFilter
	 *         evaluates to true.
	 */
	private boolean evaluateInt(DailyTime dt, WeeklyTimecard wtc) {
		boolean b = true;
		int comp = (Integer)getValue();
		int left = getIntValue(getField(), dt, wtc);
		switch(getOperator()) {
			case EQ:
				b = left == comp;
				break;
			case GE:
				b = left >= comp;
				break;
			case GT:
				b = left > comp;
				break;
			case LE:
				b = left <= comp;
				break;
			case LT:
				b = left < comp;
				break;
			case NE:
				b = left != comp;
				break;
			default:
				break;
		}
		return b;
	}

	/**
	 * Evaluate a comparison operation (our {@link #operator}) that expects two
	 * boolean operands ({@link #field} and {@link #value}) and returns a
	 * boolean. The only legal operations in this case are EQ and NE.
	 *
	 * @param prod The Production to use as a possible value source.
	 * @param dt The DailyTime to use as a possible value source.
	 * @param wtc The WeeklyTimecard to use as a possible value source.
	 * @return True iff the boolean comparison represented by this RuleFilter
	 *         evaluates to true.
	 */
	private boolean evaluateBoolean(Production prod, DailyTime dt, WeeklyTimecard wtc) {
		boolean b = true;
		boolean comp = (Boolean)getValue();
		boolean left = getBooleanValue(getField(), prod, dt, wtc);
		switch(getOperator()) {
			case EQ:
				b = left == comp;
				break;
			case NE:
				b = left != comp;
				break;
			default:
				break;
		}
		return b;
	}

	/**
	 * Determine the String value of the specified field, by examining the
	 * relevant object, which may be the timecard, its Start Form, the
	 * Production, or some other related and accessible object.
	 *
	 * @param rf The field to be evaluated.
	 * @param prod
	 * @param dt The DailyTime being evaluated.
	 * @param wtc The timecard being evaluated.
	 * @return The value of the specified field, extracted from the appropriate
	 *         object.
	 */
	private String getStringValue(RuleField rf, Production prod, DailyTime dt, WeeklyTimecard wtc) {
		String s = "";
		switch (rf) {
			case AW:
				// State worked - either from DailyTime or timecard
				s = dt.getState();
				if (StringUtils.isEmpty(s)) {
					s = wtc.getStateWorked();
					if (s == null) {
						s = "";
					}
				}
				break;
			case ET:
				// Employee Type from Start Form: 'H'(hourly), 'D'(daily), or 'W'(weekly).
				StartForm sf = wtc.getStartForm();
				switch (sf.getRateType()) {
					case HOURLY:
						s = "H";
						break;
					case DAILY:
						s = "D";
						break;
					case WEEKLY:
						s = "W";
						break;
				}
				break;
			case HO:
				// Holiday: the name of the HolidayType enum that matches current day's holiday,
				// if it is one; or empty string if the day being processed is not a holiday.
				if (dt.getDayType().isHoliday()) {
					HolidayType hType = DateEventDAO.getInstance().findSystemHolidayType(dt.getDate());
					if (hType != null) {
						s = hType.name();
					}
				}
				break;
			case PH: // Phase - return internal name for Prep/Shoot/Wrap, which is P/S/W.
				if (dt.getPhase() != null) {
					s = dt.getPhase().name();
				}
				else if (dt.getDayType() == DayType.PR || dt.getDayType() == DayType.WD) {
					s = ProductionPhase.P.name();
				}
				else if (dt.getDayType() == DayType.WP) {
					s = ProductionPhase.W.name();
				}
				else {
					s = ProductionPhase.S.name();
				}
				break;
			case PZ: // Prior day's WorkZone
				DailyTime priorDt = findPriorDt(dt, wtc);
				if (priorDt != null) {
					WorkZone z = priorDt.getWorkZone();
					if (z == null) {
						z = wtc.getDefaultZone();
					}
					if (z != null) {
						s = z.name();
					}
				}
				break;
			case TP:
				// TvType: short string (1-2 characters) indicating type of TV production:
				/* e.g., "L,DR", "P,H1,NDR"; L=long-form, P=Pilot, 1=1st season (2,3),
				 * DR=dramatic, NDR=non-dramatic, PT=prime-time, NPT=non-prime-time,
				 * H1=1 hr series, H5=1/2hr series, RIEM=reality, info, entertainment/magazine,
				 * SC=single camera. */
				if (! prod.getType().isAicp()) {
					PayrollPreference pref = prod.getPayrollPref();
					if (pref.getMediumType() == MediumType.TV) {
						s = pref.getTvSeason();
						switch (pref.getDetailType()) {
							case "P":  // pilot
							case "LF":  // long form (same as MOW)
							case "H1": // 1-Hour series
							case "H5": // 1/2 (0.5) Hour series
							case "O":  // Other
								s = pref.getDetailType();
							default:
								break;
						}
						if (pref.getTvDramatic()) {
							s += ",DR";
						}
						else {
							s += ",NDR";
						}
						if (pref.getTvRIEM()) {
							s += ",RIEM";
						}
						if (pref.getSingleCamera()) {
							s += ",SC";
						}
						if (pref.getBasicCable()) {
							s += ",BC";
						}
						s = "," + s + ",";
					}
				}
				break;
			case WD: // Weekday name
				if (dt != null) {
					s = dt.getWeekDayName();
				}
				break;
			default:
				break;
		}
		return s;
	}

	/**
	 * Determine the BigDecimal value for the specified field, by examining the
	 * relevant object, which may be the timecard, its Start Form, the
	 * Production, or some other related and accessible object.
	 *
	 * @param rf The field to be evaluated.
	 * @param dt The DailyTime being evaluated.
	 * @param wtc The timecard being evaluated.
	 * @return The value of the specified field, extracted from the appropriate
	 *         object.
	 */
	private BigDecimal getBigDecimalValue(RuleField rf, DailyTime dt, WeeklyTimecard wtc) {
		BigDecimal bd = BigDecimal.ZERO;
		switch (rf) {
			case CL:
				if (dt != null) {
					bd = dt.getCallTime();
				}
				break;
			case WR:
				if (dt != null) {
					bd = dt.getWrap();
				}
				break;
			case EH:
				if (dt != null) {
					bd = TimecardCalc.calculateElapsedHours(dt);
				}
				break;
			case HW:
				if (dt != null) {
					bd = dt.getHours();
				}
				break;
			case EXHR:
				// calculate "excess hours" - hours worked past "span" end.
				DailyTime dt1 = null, dt5 = null;
				for (DailyTime dtx: wtc.getDailyTimes()) {
					if (dtx.getWorkDayNum() == 1) {
						dt1 = dtx;
					}
					else if (dtx.getWorkDayNum() == 5) {
						dt5 = dtx;
					}
				}
				if (dt1 != null && dt5 != null) {
					if (dt5.getDayNum() < dt1.getDayNum()) {
						// TODO work-day 5 comes before work-day 1, have to check last week!
						String msg = "'ExcessHours' value in Restriction expression unsupported for shifted week!";
						MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_ERROR, msg);
						log.error(msg);
					}
					else {
						BigDecimal diff = dt5.getWrap().subtract(dt1.getCallTime().add(Constants.HOURS_IN_A_DAY));
						if (diff.signum() > 0) {
							bd = diff;
						}
					}
				}
				break;
			case TO:
				if (dt.getCallTime() != null) {
					bd = dt.getCallTime().add(Constants.HOURS_IN_A_DAY).subtract(htg.getLastWrap());
				}
				else {
					bd = Constants.HOURS_IN_A_DAY;
				}
				break;
			case PHE:
				// Prior day's elapsed hours
				DailyTime priorDt = findPriorDt(dt, wtc);
				if (priorDt != null) {
					bd = TimecardCalc.calculateElapsedHours(priorDt);
				}
				break;
			case PHW:
				// Prior day's hours-worked
				priorDt = findPriorDt(dt, wtc);
				if (priorDt != null) {
					bd = priorDt.getHours();
				}
			default:
				break;
		}
		return bd;
	}

	/**
	 * Determine the boolean value for the specified field, by examining the
	 * relevant object, which may be the timecard, its Start Form, the
	 * Production, or some other related and accessible object.
	 *
	 * @param rf The field to be evaluated.
	 * @param prod The Production for the current timecard.
	 * @param dt The DailyTime being evaluated.
	 * @param wtc The timecard being evaluated.
	 * @return The value of the specified field, extracted from the appropriate
	 *         object.
	 */
	private boolean getBooleanValue(RuleField rf, Production prod, DailyTime dt, WeeklyTimecard wtc) {
		boolean b = true;
		switch (rf) {
			case AD:	// StartForm additionalStaff flag
				b = wtc.getStartForm().getAdditionalStaff();
				break;
			case EX:	// StartForm exempt flag
				b = wtc.getStartForm().getAllowWorked();
				break;
			case FR:	// DailyTime French-hours flag
				if (dt != null) {
					b = dt.getFrenchHours();
				}
				break;
			case MJ: // True if Production Preference for studio type is Major
				b = prod.getPayrollPref().getStudioType() == StudioType.MJ;
				break;
			case MXW:
				// true if the current week has both Studio and Distant day types
				if (dt != null) {
					b = TimecardUtils.calculateIsMixedWeek(wtc);
				}
				break;
			case NXH: // True if next day is a holiday
				if (wtc != null) {
					if (dt != null) { // true if day after "dt" is a holiday
						int i = dt.getDayNum();
						if (i < 7) {
							dt = wtc.getDailyTimes().get(i);
							b = (dt.getDayType() != null) && dt.getDayType().isHoliday();
						}
					}
					else { // no specific day; true if any holiday in week
						b = TimecardUtils.calculateHasHoliday(wtc);
					}
				}
				break;
			case NH: // True if StartForm.nearbyHire is true
				wtc.getStartForm().getNearbyHire();
				break;
			case PW:
				// true if current week is "partial" - less than 5 days, either (a) starting after
				// the first production day and continuing to at least the 5th day of the production week,
				// or (b) starting on the first production day but ending prior to the 5th work day.
				// For (b), the previous week must have been a "complete" week.
				int i = TimecardUtils.calculateDaysWorked(wtc);
				if (i >= 5) {
					b = false;
				}
				else {
					int fd = htg.preference.getFirstWorkDayNum(); // day number (index) (1-7) of first production day within timecard
					DailyTime fdt = wtc.getDailyTimes().get(fd - 1);
					if (fdt.reportedWork()) {
						// could be case (b) -- ending week
					}
					else {
						// could be case (a) -- first week of work
					}
					// TODO Finish "PartialWeek" contract rule restriction implementation
					String msg = "Using unimplemented 'PartialWeek' value in Restriction expression!";
					MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_ERROR, msg);
					log.error(msg);
				}
				break;
			case SH:
				// TODO determine if production schedule shifted this week
				String msg = "Using unimplemented 'Shifted' value in Restriction expression!";
//				MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_ERROR, msg);
				log.error(msg);
				break;
			default:
				break;
		}
		return b;
	}

	/**
	 * Determine the integer value for the specified field, by examining the
	 * relevant object, which may be the timecard, its Start Form, the
	 * Production, or some other related and accessible object.
	 *
	 * @param rf The field to be evaluated.
	 * @param dt The DailyTime being evaluated.
	 * @param wtc The timecard being evaluated.
	 * @return The value of the specified field, extracted from the appropriate
	 *         object.
	 */
	private int getIntValue(RuleField rf, DailyTime dt, WeeklyTimecard wtc) {
		int i = 0;
		switch (rf) {
			case PDN: // Production Day Number
				i = htg.preference.getFirstWorkDayNum(); // day number/index (1-7) of first production day
				i = dt.getDayNum() + 1 - i;
				if (i < 1) {
					i += 7;
				}
				break;
			case PWDP:
				if (htg.priorTimecard != null) {
					i = TimecardUtils.calculateDaysWorked(htg.priorTimecard);
					// TODO Do we need to determine days paid that were not worked?
				}
				break;
			case WDN: // worked-day number
				if (dt != null) {
					i = dt.getWorkDayNum();
				}
				break;
			case WRKD: // number of days worked in this week
				if (wtc != null) {
					i = TimecardUtils.calculateDaysWorked(wtc);
				}
				break;
			default:
				break;
		}
		return i;
	}

	/**
	 * Find the previous day's DailyTime instance.
	 *
	 * @param dt The current DailyTime instance
	 * @param wtc The current timecard
	 * @return The dailyTime instance for the day preceding the given dailyTime.
	 */
	private DailyTime findPriorDt(DailyTime dt, WeeklyTimecard wtc) {
		DailyTime priorDt = null;
		if (dt.getDayNum() <= 1) {
			if (htg.priorTimecard != null) {
				priorDt = htg.priorTimecard.getDailyTimes().get(6);
			}
		}
		else {
			priorDt = wtc.getDailyTimes().get(dt.getDayNum()-2);
		}
		return priorDt;
	}


	private final static String RE_COMPARE = "([A-Z]*)[ ]*([:<>=!]+)[ ]*(.*)";
	private final static Pattern P_COMPARE = Pattern.compile(RE_COMPARE);
	/**
	 * @param filterText
	 */
	public static RuleFilter createFilter(String filterText) {
		RuleFilter rf = null;
		filterText = filterText.trim().toUpperCase();
		Matcher m = P_COMPARE.matcher(filterText);
		if (m.matches()) {
			rf = new RuleFilterImpl();
			try {
//				for (int x = 1; x <= m.groupCount(); x++) {
//					log.debug(m.group(x));
//				}
				rf.setField(RuleField.valueOf(m.group(1)));
				rf.setOperator(RuleOperator.toValue(m.group(2)));
				RuleField fl = rf.getField();
				if (fl.getClazz() == Boolean.class ||
						rf.getOperator() == RuleOperator.NL) {
					// 'is null' operator always gets boolean result
					if (m.group(3).equalsIgnoreCase("Y")) {
						rf.setValue(Boolean.TRUE);
					}
					else {
						rf.setValue(new Boolean(m.group(3)));
					}
				}
				else if (fl.getClazz() == BigDecimal.class) {
					rf.setValue(new BigDecimal(m.group(3)));
				}
				else if (fl.getClazz() == Integer.class) {
					rf.setValue(new Integer(m.group(3)));
				}
				else {
					rf.setValue(m.group(3));
				}
			}
			catch (Exception e) {
				String msg = MsgUtils.formatMessage("Timecard.HTG.ParsingError", filterText);
				EventUtils.logError(msg, e);
				rf = null;
			}
		}
		else {
			EventUtils.logError("Invalid rule filter definition: " + filterText);
		}
		return rf;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String s = super.toString();
		s += ", Test=[" + getField().getLabel() + " " + getOperator().getLabel() + " " + getValue() + "]";
		return s;
	}

}
