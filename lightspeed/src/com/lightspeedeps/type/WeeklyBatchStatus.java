// File Name: WeeklyBatchStatus.java
package com.lightspeedeps.type;

/**
 * An enumeration used in WeeklyBatch to indicate the
 * current status of the batch.
 */
public enum WeeklyBatchStatus {

	/** Initial status, batch has not been sent to payroll yet. */
	OPEN		("Open"),

	/** Batch has been transmitted to the payroll service. */
	SENT		("Sent"),

	/** The payroll service has indicated that the batch is in progress. (For IndiePay, this
	 * is indicated by their "imported successfully" status.) */
	IN_PROGRESS	("Edit in progress"),

	/** Edit report has been received. */
	EDIT_RCVD	("Edit Received"),

	/** The client has approved the latest edit report. */
	FINAL		("Client approved"),

	/** Paychecks have been printed. */
	PAID		("Paycheck printed"),

	/** A status request to the payroll service returned an indication that the
	 * batch id was not recognized. */
	UNKNOWN		("Batch not recognized by payroll service");

	private final String heading;

	private WeeklyBatchStatus(String head) {
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

//	/**
//	 * @return True iff
//	 */
//	public boolean getClosed() {
//		return (this != OPEN);
//	}

}
