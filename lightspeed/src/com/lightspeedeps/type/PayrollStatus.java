//	File Name:	PayrollStatus.java
package com.lightspeedeps.type;

/**
 * An enumeration used in the timecard/payroll processing system. It is
 * typically the value returned by a payroll service in response to a query of
 * the status of a batch or timecard.
 */
public enum PayrollStatus {

	/** Corresponds to IP1 - the batch file was successfully received. */
	RECEIVED	(1, "Received"),

	/** Corresponds to IP2 - the batch is in progress; this might simply mean it has
	 * been moved from the initial reception location into the service's payroll system. */
	IN_PROGRESS	(2, "In progress"),

	/** Corresponds to IP3 - An edit report is ready; it may have been sent to the client. */
	EDIT		(3, "Edit ready"),

	/** Corresponds to IP4 - The payroll service has received client approval of the latest
	 * edit report. */
	FINAL		(4, "Client approved"),

	/** Corresponds to IP5 - "payrun processed" (for IndiePay); checks have been printed. */
	PAID		(5, "Paycheck printed"),

	/** Corresponds to message rc23 from a status request -- the requested batch
	 * id was not recognized by the payroll service. This is a "normal" response
	 * to a batch query only if the batch has not yet been sent to the payroll
	 * service. */
	UNKNOWN		(0, "Unknown Batch"),

	/** Corresponds to some other error in obtaining the status of the batch. */
	UNAVAILABLE (-1, "Status not available");

	/** An integer value related to the status; for positive values, it matches the status
	 * message value returned when we query a batch's status.  This should always be in
	 * increasing value as the batch progresses from initial receipt by the payroll service
	 * up to its last possible value, currently "PAID".  A non-positive value is used for
	 * status values that should not normally be encountered. */
	private final int payCode;
	private final String heading;

	private PayrollStatus(int code, String head) {
		payCode = code;
		heading = head;
	}

	/**
	 * Returns a "pretty" mixed-case version of the enumerated value.
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
	 * Returns the "pretty" printable version of this enumerated type. It is the same as toString,
	 * but can be used from jsp pages since it follows the bean accessor (getter) naming convention.
	 *
	 * @return The value of the enumerated type as mixed-case text.
	 */
	public String getLabel() {
		return toString();
	}

	/**See {@link #payCode}. */
	public int getPayCode() {
		return payCode;
	}

}
