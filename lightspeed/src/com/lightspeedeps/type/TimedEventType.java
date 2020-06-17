//	File Name:	TimecardEventType.java
package com.lightspeedeps.type;


/**
 * An enumeration used in TimedEvent to indicate the nature of the event being
 * recorded. This is used primarily in the payroll system for events related to
 * timecards or other payroll documents.
 * @see com.lightspeedeps.model.TimedEvent
 *
 */
public enum TimedEventType {
	/** A document has been sent by the employee to the first
	 * approver for processing. */
	SUBMIT		("Submitted"), 	// a signed event

	/** Currently used only for the I-9 "Preparer" signature field. */
	PREPARE		("Prepared"),	// a signed event

	/** A User has Viewed the form I9 (for audit trail); or a Custom document
	 * with an employee action of "Read" has been viewed by the employee. */
	VIEW		("Viewed"),

	/** Acknowledge receipt of a document; currently only used for
	 * custom documents. */
	ACK		("Acknowledged"),

	/** An Initial event. This differs from a signature event in that only the
	 * PIN is required (instead of password and PIN). This is used for the
	 * Direct Deposit form, the ACTRA contract, and possibly others. */
	INITIAL		("Initialed"),

	/** A document has been signed by the employee or employer. For an employee,
	 * this may represent simply acknowledging receipt of it, without
	 * "submitting" any other information for approval. */
	SIGN		("Signed"),		// a signed event

	/** A document has been signed (again) by the employee or employer.  This
	 * may be used in cases where more than one signature by the same person
	 * is needed.  Added for ACTRA contract. LS-1411 */
	SIGN_OPT		("Signed (Optional)"),		// an optional signing event

	/** An approver has sent the document back, either to a prior
	 * approver, or to the employee, for correction. */
	REJECT		("Rejected"),

	/** An Employee has declined to sign the WTPA form. */
	DECLINE		("Employee Declined to Sign"),

	/** An approver who has already approved the document has
	 * retrieved it from the queue of a future approver. */
	RECALL		("Recalled"),

	/** An approver who has not yet received the document has
	 * taken control of the document (moved it to their own
	 * queue). */
	PULL		("Pulled"),

	/** An approver has indicated their approval of the document,
	 * sending it along to the next approver, or marking it Final
	 * Approved if they are the last approver in the chain. */
	APPROVE		("Approved"), 	// a signed event

	/** An approver has marked the document as "Void", making it
	 * ineligible for any further processing. */
	VOID		("Voided"),		// a signed event

	/** An approver has marked a document "locked".  This can only
	 * be done to documents which were in a Final approval state. */
	LOCK		("Locked"),

	/** A person has stored a document in the repository from an
	 * outside source, e.g., via uploading it from their PC. */
	STORE		("Stored"),

	// * * * The following event types are used for the FormActraContract * * *

	/** This event type is created when the employee (performer) initials in
	 * the "Agree" box of Section B of the ACTRA contract.  LS-1434 */
	AGREE		("Agree"),

	/** This event type is created when the employee (performer) initials in
	 * the "Disagree" box of Section B of the ACTRA contract.  LS-1434 */
	DISAGREE	("Disagree"),

	// * * * The following event types are used for the timecard "audit" log * * *

	/** The user has executed the AutoPay action on a timecard. */
	AUTO_PAY ("Auto Pay"),

	/** A Change event for the related object (e.g., a timecard). */
	CHANGE		("Change"),

	/** The related object (e.g., a timecard) is being edited. */
	EDIT		("Edit"),

	/** The related object (e.g., a timecard) has been saved. */
	SAVE		("Save"),

	/** The user cancelled an Edit session on the object (e.g., timecard). */
	CANCEL		("Cancel"),

	/** The user deleted the related object (e.g., timecard). */
	DELETE		("Delete"),
	;

	private final String heading;

	private TimedEventType(String head) {
		heading = head;
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
	 * @return True iff this event requires (includes) an electronic signature from the user.
	 */
	public boolean getSigned() {
		return (this == SUBMIT
				|| this == PREPARE
				|| this == SIGN
				|| this == SIGN_OPT
				|| this == APPROVE
				|| this == AGREE
				|| this == DISAGREE
				|| this == VOID);
	}

}
