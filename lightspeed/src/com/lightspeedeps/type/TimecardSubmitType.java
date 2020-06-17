//	File Name:	TimecardSubmitType.java
package com.lightspeedeps.type;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

/**
 * An enumeration used in TimecardEvent to indicate a sub-type of
 * the TimecardEventType.SUBMIT event.  The value is stored in one
 * of the fields not normally used for SUBMIT events.
 */
public enum TimecardSubmitType {

	/** The Submit was done by the employee. */
	NORMAL			("Normal",
			"The employee submitted his own timecard"),

	/** The Submit was done by someone who had evidence of a paper
	 * signature, e.g., on a paper timecard signed by the employee. */
	PAPER_SIGNATURE	("Paper signature",
			"I have received a signed paper timecard from the employee"),

	/** The Submit was done by someone who confirmed that the
	 * employee was "under contract", and therefore did not need
	 * to sign individual timecards. */
	UNDER_CONTRACT	("Under contract",
			"The employee is \"Under Contract\""),

	/** The Submit was done by someone other than the employee
	 * for a reason other than the usual cases of "paper signature"
	 * or "under contract".  A comment should have been added to the
	 * history/audit trail giving details. */
	OTHER			("Other",
			"Other (please provide a comment below)"),
	;

	private static List<SelectItem> typeContractSelectList;
	private static List<SelectItem> typeDefaultSelectList;
	private static List<SelectItem> typeDocumentSelectList;
	static {
		typeDefaultSelectList = new ArrayList<SelectItem>();
		typeDefaultSelectList.add(new SelectItem(PAPER_SIGNATURE, 	PAPER_SIGNATURE.prompt));
		typeDefaultSelectList.add(new SelectItem(OTHER,				OTHER.prompt));

		typeContractSelectList = new ArrayList<SelectItem>();
		typeContractSelectList.add(new SelectItem(PAPER_SIGNATURE, 	PAPER_SIGNATURE.prompt));
		typeContractSelectList.add(new SelectItem(UNDER_CONTRACT, 	UNDER_CONTRACT.prompt));
		typeContractSelectList.add(new SelectItem(OTHER,			OTHER.prompt));
		
		typeDocumentSelectList = new ArrayList<SelectItem>();
		typeDocumentSelectList.add(new SelectItem(PAPER_SIGNATURE, "I have received a signed paper document from the employee"));
		typeDocumentSelectList.add(new SelectItem(OTHER,			OTHER.prompt));
	}

	private final String heading;
	private final String prompt;

	private TimecardSubmitType(String head, String pr) {
		heading = head;
		prompt = pr;
	}

	/**
	 * @return The "pretty" mixed-case version of the enumerated value.
	 * This could be enhanced to use the current locale setting for i18n purposes.
	 * <p>
	 * NOTE: This overrides the default toString(), which returns
	 * the same value as name(), which is the exact text of the enum name.
	 */
	@Override
	public String toString() {
		return heading;
	}

	/**
	 * @return The "pretty" printable version of this enumerated type. It is the same as toString,
	 * but can be used from jsp pages since it follows the bean accessor (getter) naming convention.
	 */
	public String getLabel() {
		return toString();
	}

	/**
	 * @return The 'prompt' text to use in a drop-down list or other presentation
	 * to the user, when selecting one of the SubmitType choices.
	 */
	public String getPrompt() {
		return prompt;
	}

	/** See {@link #typeContractSelectList}. */
	public static List<SelectItem> getTypeContractSelectList() {
		return typeContractSelectList;
	}

	/** See {@link #typeDefaultSelectList}. */
	public static List<SelectItem> getTypeDefaultSelectList() {
		return typeDefaultSelectList;
	}

	/** See {@link #typeDocumentSelectList}. */
	public static List<SelectItem> getTypeDocumentSelectList() {
		return typeDocumentSelectList;
	}

}

