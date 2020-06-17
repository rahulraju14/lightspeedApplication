package com.lightspeedeps.object;

import java.math.BigDecimal;

import com.lightspeedeps.type.CompareType;
import com.lightspeedeps.type.FieldType;

/**
 * Characteristics of the fields and values to compare
 */
public class Validate {
	private FieldType fieldType;
	private Object fieldValue;
	private CompareType compareType;
	private Object compareValue;
	private String errorMessage;
	private String msgSubstitution;

	/**
	 * Parameterized constructor to set validation field values.
	 *
	 * @param fieldType Type of the field to be compared
	 * @param fieldValue Field value to be compared
	 * @param compareType Type of the value to be compared with
	 * @param compareValue The value to be compared with
	 * @param errorMessage If the above condition is not satisfied error message
	 *            is used
	 */
	public Validate(FieldType fieldType, Object fieldValue, CompareType compareType,
			Object compareValue, String errorMessage, String msgSub) {
		super();
		this.fieldType = fieldType;
		this.fieldValue = fieldValue;
		this.compareType = compareType;
		this.compareValue = compareValue;
		this.errorMessage = errorMessage;
		msgSubstitution = msgSub;
	}

	/**
	 * Parameterized constructor to set validation field values when comparing
	 * to a BigDecimal (the most common).
	 *
	 * @param fieldValue The value of the field to be compared
	 * @param compareType Type of the value to be compared with
	 * @param compareValue The value to be compared with
	 * @param errorMessage If the above condition is not satisfied error message
	 *            is used
	 */
	public Validate(BigDecimal fieldValue, CompareType compareType, Object compareValue,
			String errorMessage) {
		super();
		fieldType = FieldType.BIG_DECIMAL;
		this.fieldValue = fieldValue;
		this.compareType = compareType;
		this.compareValue = compareValue;
		this.errorMessage = errorMessage;
	}

	/**
	 * Short-form constructor for comparing BigDecimal fields to zero.
	 *
	 * @param bigDecimal Value to compare
	 * @param compareType Type of comparison (less, equal, etc.)
	 * @param errorMessage Message id.
	 * @param msgSub The message substitution text.
	 */
	public Validate( BigDecimal bigDecimal, CompareType compareType, String errorMessage, String msgSub) {
		super();
		fieldType = FieldType.BIG_DECIMAL;
		fieldValue = bigDecimal;
		this.compareType = compareType;
		compareValue = BigDecimal.ZERO;
		this.errorMessage = errorMessage;
		msgSubstitution = msgSub;
	}

	/**
	 * Validate the values in this instance.
	 *
	 * @return True if the field value has the specified relationship to the
	 *         comparison value.
	 */
	public boolean validate() {
		if (fieldValue == null || compareValue == null) {
			return true;
		}
		boolean res = true;
		switch(fieldType) {
			case BIG_DECIMAL:
				res = compareBigDecimalVals();
				break;
			case INTEGER:
				res = compareIntegerVals();
				break;
			case BYTE:
				res =  compareByteVals();
				break;
			default:
				break;
		}
		return res;
	}

	/**
	 * Compare BigDecimal type values
	 * @return true if compared condition satisfied
	 */
	private boolean compareBigDecimalVals() {
		/*
		 * Compares this BigDecimal with the specified BigDecimal.
		 * Two BigDecimal objects that are equal in value but have a different scale
		 * (like 2.0 and 2.00) are considered equal by this method. This method is
		 * provided in preference to individual methods for each of the six boolean
		 * comparison operators (<, ==, >, >=, !=, <=). The suggested idiom for
		 * performing these comparisons is: (x.compareTo(y) <op> 0),
		 * where <op> is one of the six comparison operators.
		 */
		int result = ((BigDecimal)fieldValue).compareTo((BigDecimal)compareValue);
		return checkResult(result);
	}

	/**
	 * Compare Integer type values
	 * @return true if compared condition satisfied
	 */
	private boolean compareIntegerVals() {
		/*
		 * compareTo() for Integer returns the value 0 if this Integer is equal to the argument Integer;
		 * a value less than 0 if this Integer is numerically less than the argument Integer;
		 * and a value greater than 0 if this Integer is numerically greater than the argument Integer (signed comparison).
		 */
		int result = ((Integer)fieldValue).compareTo((Integer)compareValue);
		return checkResult(result);
	}

	/**
	 * Compare Byte type values
	 * @return true if compared condition satisfied
	 */
	private boolean compareByteVals() {
		/*
		 * compareTo() for Byte returns the value 0 if this Byte is equal to the argument Byte;
		 * a value less than 0 if this Byte is numerically less than the argument Byte;
		 * and a value greater than 0 if this Byte is numerically greater than the argument Byte (signed comparison).
		 */
		int result = ((Byte)fieldValue).compareTo((Byte)compareValue);
		return checkResult(result);
	}

	/**
	 * Check a comparison result versus the comparison type, and return true or
	 * false.
	 *
	 * @param result The result of the comparison operation (-1/0/1).
	 * @return True if the comparison result correctly matches the compare type.
	 */
	private boolean checkResult(int result) {
		//Check for the compare type
		switch(compareType) {
			case EQUAL:
				return result == 0;
			case NOT_EQUAL:
				return result != 0;
			case GREATER_THAN:
				return result > 0;
			case GREATER_THAN_EQUAL:
				return result >= 0;
			case LESS_THAN:
				return result < 0;
			case LESS_THAN_EQUAL:
				return result <= 0;
			default:
				return false;
		}
	}

	//Getter-setter methods
	public FieldType getFieldType() {
		return fieldType;
	}
	public void setFieldType(FieldType fieldType) {
		this.fieldType = fieldType;
	}

	public Object getFieldValue() {
		return fieldValue;
	}
	public void setFieldValue(Object fieldValue) {
		this.fieldValue = fieldValue;
	}

	public CompareType getCompareType() {
		return compareType;
	}
	public void setCompareType(CompareType compareType) {
		this.compareType = compareType;
	}

	public Object getCompareValue() {
		return compareValue;
	}
	public void setCompareValue(Object compareValue) {
		this.compareValue = compareValue;
	}

	public String getErrorMessage() {
		return errorMessage;
	}
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	/** See {@link #msgSubstitution}. */
	public String getMsgSubstitution() {
		return msgSubstitution;
	}
	/** See {@link #msgSubstitution}. */
	public void setMsgSubstitution(String msgSubstitution) {
		this.msgSubstitution = msgSubstitution;
	}

}
