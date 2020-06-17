/**
 * File: RuleFilter.java
 */
package com.lightspeedeps.object;

import com.lightspeedeps.type.RuleField;
import com.lightspeedeps.type.RuleOperator;

/**
 * The interface for the RuleFilterImpl class. Each RuleFilter contains one
 * comparison, or filter, that determines whether or not a contract rule will be
 * applied in a particular situation. Typically this means whether a rule will
 * apply to a particular day of a particular timecard, based on data in a
 * particular DailyTime object. In some cases, however, the test(s) may use data
 * from the Production or WeeklyTimecard objects.
 *
 * @see RuleFilterImpl
 */
public interface RuleFilter {

	public RuleField getField();

	public void setField(RuleField compareField);

	public RuleOperator getOperator();

	public void setOperator(RuleOperator operator);

	public Object getValue();

	public void setValue(Object compareValue);

}
