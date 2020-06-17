//	File Name:	EventType.java
package com.lightspeedeps.type;

/**
 * An enumeration used in Event.
 */
public enum EventType {
	/** Recorded once when TTCO/Lightspeed application starts; includes version number. */
	APP_START,
	/** Indicates successful login of a user. */
	LOGIN_OK,
	/** Indicates user logging out.  Note that users may time out (or close browser, etc.),
	 * in which case no LOGOUT record is written. */
	LOGOUT,
	/** Login failure, typically due to invalid password or email address. */
	LOGIN_FAILED,
	/** User locked out due to excessive failed attempts at login. */
	USER_LOCKED_OUT,
	/** Application error, typically a 'trapped' Java run-time Exception. Usually
	 * includes a stack trace. */
	APP_ERROR,
	/** Logging of some sort of invalid data; similar situation to an "IllegalArgumentException". */
	DATA_ERROR,
	/** */
	SESSION_DROP,
	/** General information record, for "important" events.  Currently records some SFTP transfers,
	 * automatic timecard generation, others. */
	INFO,
	/** ESS event : user downloaded pay stub */
	CHECK_DOWNLOAD,
	/** ESS event: user downloaded Direct Deposit form */
	DIRECT_DEPOSIT_DOWNLOAD,
	/** ESS event: user downloaded W2 */
	W2_DOWNLOAD,
	/** ESS event: user downloaded W2 */
	W2_WAGE_BREAKDOWN_DOWNLOAD,
	/** ESS event: user viewed check detail */
	VIEW_CHECK_DETAIL,
	/** ESS event: user viewed direct deposit detail */
	VIEW_DIRECT_DEPOSIT_DETAIL,
	/** ESS event: user login device details */
	DEVICE_INFORMATION,
	;

	/**
	 * @return  enum name; used by JSP.
	 */
	public String getName() {
		return name();
	}
}
