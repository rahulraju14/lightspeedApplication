package com.lightspeedeps.type;

import java.math.BigDecimal;

public enum TimecardFieldType {
	EXP_CATEGORY ("Expense Category", String.class),
	EXP_QUANTITY ("Expense Quantity", BigDecimal.class),
	EXP_RATE ("Expense Rate", BigDecimal.class),
	PB_CATEGORY_FIELD ("Pay Breakdown Category", String.class),
	PB_CATEGORY ("Pay Breakdown Category", String.class),
	PB_QUANTITY ("Pay Breakdown Quantity", BigDecimal.class),
	PB_MULTIPLIER ("Pay Breakdown Multiplier", BigDecimal.class),
	PB_RATE ("Pay Breakdown Rate", BigDecimal.class),
	GRAND_TOTAL ("Total Gross", BigDecimal.class),
	ADD_LINE ("Add Line", String.class),
	DELETE_LINE ("Delete Line", String.class),
	UPDATE_FROM_START ("Update From Start", String.class),
	DT_DAY_TYPE ("Day Type", String.class),
	DT_DAY_WORKED ("Worked", Boolean.class),
	DT_CALL ("Call", BigDecimal.class),
	DT_MEAL1_OUT ("Meal 1 Out", BigDecimal.class),
	DT_MEAL1_IN ("Meal 1 In", BigDecimal.class),
	DT_MEAL2_OUT ("Meal 2 Out", BigDecimal.class),
	DT_MEAL2_IN ("Meal 2 In", BigDecimal.class),
	DT_WRAP ("Wrap", BigDecimal.class),
	DT_LAST_MAN_IN ("Last Man In", BigDecimal.class),
	DT_MPV_USER ("MPV (user)", String.class),
	DT_MPV_1 ("MPV 1 (payroll)", Byte.class),
	DT_MPV_2 ("MPV 2 (payroll)", Byte.class),
	DT_NDB ("NDB", Boolean.class),
	DT_NDB_END ("NDB End", BigDecimal.class),
	DT_NDM ("NDM", Boolean.class),
	DT_NDM_START ("NDM Start", BigDecimal.class),
	DT_NDM_END ("NDM End", BigDecimal.class),
	DT_ON_CALL_START ("On-Call Start", BigDecimal.class),
	DT_ON_CALL_END ("On-Call End", BigDecimal.class),
	DT_WORK_ZONE ("Work Zone", String.class),
	DT_WORK_STATE ("Work State", String.class),
	DT_WORK_COUNTRY ("Work Country", String.class),
	JS_SPLIT1 ("Split 1", BigDecimal.class),
	JS_SPLIT2 ("Split 2", BigDecimal.class),
	JS_SPLIT_PCT ("Split Percent", Boolean.class),
	DT_PAY_AMOUNT ("Pay Amount", BigDecimal.class),
	N_A ("N/A", String.class);

	/** A description of the field; this may be used in the detailed Audit trail when displaying
	 * a  {@link com.lightspeedeps.object.RuleFilter RuleFilter} applied as part of a
	 * {@link com.lightspeedeps.model.ContractRule ContractRule}. */
	private final String description;

	/** The expected class of a value to which this field will be compared. The currently used
	 * values are Integer, BigDecimal, and String. */
	@SuppressWarnings("rawtypes")
	private final Class clazz;

	/**
	 * The only constructor.
	 *
	 * @param clas See {@link #clazz}
	 * @param desc See {@link #description}
	 */
	@SuppressWarnings("rawtypes")
	TimecardFieldType(String desc, Class clas) {
		description = desc;
		clazz = clas;
	}

	/**See {@link #clazz}. */
	@SuppressWarnings("rawtypes")
	public Class getClazz() {
		return clazz;
	}

	/**
	 * Returns a "pretty" mixed-case version of the enumerated value.
	 * <p>
	 * NOTE: This overrides the default toString(), which returns
	 * the same value as name(), which is the exact text of the enum name.
	 */
	@Override
	public String toString() {
		return description;
	}

	/**
	 * Returns the "pretty" printable version of this enumerated type. It is the same as toString,
	 * but can be used from jsp pages since it follows the bean accessor (getter) naming convention.
	 * This could be enhanced to use the current locale setting for i18n purposes.
	 *
	 * @return The value of the enumerated type as mixed-case text.
	 */
	public String getLabel() {
		return toString();
	}

}
