package com.lightspeedeps.util.payroll;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;

import com.lightspeedeps.object.Validate;
import com.lightspeedeps.type.CompareType;
import com.lightspeedeps.type.FieldType;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;

/**
 * Validate the fields
 */
public class ValidateFields {

	private ValidateFields() {
	}


	/**
	 * Validate the fields stored in each object of Validate class. The passed
	 * conditions are checked against each Validate object. If the condition(s)
	 * are not satisfied, set the error message(s) and return false.
	 *
	 * @param fields List of Validate objects
	 * @return true if the validation(s) are satisfied
	 */
	public static boolean validateFields(List<Validate> fields) {
		return validateFields(fields, true);
	}

	/**
	 * Validate the fields stored in each object of Validate class. The passed
	 * conditions are checked against each Validate object. If the condition(s)
	 * are not satisfied, set the error message(s) (if requested) and return false.
	 *
	 * @param fields List of Validate objects
	 * @param showMsgs if true, Faces messages are generated for each error detected.
	 * @return true if the validation(s) are satisfied
	 */
	public static boolean validateFields(List<Validate> fields, boolean showMsgs) {
		boolean check = true;
		List<String> errorList = new ArrayList<String>(); //Store the list of error message
		if (fields != null) {
			for (Validate field : fields) {
				try{
					if (! field.validate()) {
						String msg = MsgUtils.formatMessage(field.getErrorMessage(), field.getMsgSubstitution());
						if (! errorList.contains(msg)) {
							errorList.add(msg);
						}
//						MsgUtils.addFacesMessage(field.getErrorMessage(), FacesMessage.SEVERITY_ERROR, field.getMsgSubstitution());
//						check = false;
					}
				}
				catch(Exception e) {
					EventUtils.logError("Error: ", e);
				}
			}
		}

		// Issue any accumulated error messages.
		if (errorList != null && errorList.size() > 0) {
			check = false;
			if (showMsgs) {
				for (String err : errorList) {
					MsgUtils.addFacesMessageText(err, FacesMessage.SEVERITY_ERROR);
				}
			}
		}
		return check;
	}

	public static void validateByte(Byte fieldValue, CompareType compareType, Byte compareValue,
			String errorMessage, List<String> errorList) {
		Validate v = new Validate(FieldType.BYTE, fieldValue, compareType, compareValue, errorMessage, "");
		boolean res = v.validate();
		if ((! res) && (! errorList.contains(errorMessage))) {
			errorList.add(errorMessage);
		}
	}

	public static void validateBigDecimal(BigDecimal fieldValue, CompareType compareType,
			BigDecimal compareValue, String errorMessage, List<String> errorList) {
		Validate v = new Validate(FieldType.BIG_DECIMAL, fieldValue, compareType, compareValue, errorMessage, "");
		boolean res = v.validate();
		if ((! res) && (! errorList.contains(errorMessage))) {
			errorList.add(errorMessage);
		}
	}

//	public static void validateInteger(Integer fieldValue, CompareType compareType,
//			Integer compareValue, String errorMessage, List<String> errorList) {
//		Validate v = new Validate(FieldType.INTEGER, fieldValue, compareType, compareValue, errorMessage, "");
//		v.validate(errorList);
//	}

}
