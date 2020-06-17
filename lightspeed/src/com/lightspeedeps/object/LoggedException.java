/**
 * File: LoggedException.java
 */
package com.lightspeedeps.object;

/**
 * An exception thrown after an error Event has been logged, to allow
 * the exception to be passed up through the call chain, without
 * creating a duplicate Event log entry.
 */
public class LoggedException extends RuntimeException {

	/**  */
	private static final long serialVersionUID = 3914816693975249548L;

	/**
	 * The default constructor.
	 */
	public LoggedException() {
		super("Exception previously logged.");
	}

	/**
	 * @param ex A description of the error that occurred.
	 */
	public LoggedException(String ex) {
		super("Exception previously logged: " + ex);
	}

	/**
	 * @param ex The Exception to be replaced.
	 */
	public LoggedException(Throwable ex) {
		super(createMsg(null, ex), ex);
	}

	/**
	 * @param text A description of the error, beyond that provided within the
	 *            Exception itself.
	 * @param ex The Exception to be replaced.
	 */
	public LoggedException(String text, Throwable ex) {
		super(createMsg(text, ex), ex);
	}

	/**
	 * Generates an appropriate message string relating to the cause of the
	 * underlying exception.
	 *
	 * @param text Any additional text supplied by the caller related to the
	 *            error.
	 * @param ex The exception that was thrown.
	 * @return An appropriate message to display for this exception.
	 */
	private static String createMsg(String text, Throwable ex) {
		String msg = "Exception previously logged: ";
		if (text != null) {
			msg += text;
		}
		if (ex.getMessage() == null) {
			msg += ex.toString();
		}
		else {
			msg += ex.getMessage();
		}
		return msg;
	}

}
