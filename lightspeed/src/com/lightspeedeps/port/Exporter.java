/**
 * Export.java
 */
package com.lightspeedeps.port;

import java.math.BigDecimal;
import java.util.Date;

/**
 *
 */
public interface Exporter {

	/**
	 * End the current record, and prepare for a new record.
	 */
	public void next();

	/**
	 * Add a String field to the current output record. For delimited files, the
	 * field will be preceded by this Exporter's delimiter character, unless
	 * this is the first field of a record.
	 *
	 * @param s The String to be output.
	 */
	public void append(String s);

	/**
	 * Add an Integer field to the current output record, formatted using the
	 * Integer.toString() method. For delimited files, the field will be
	 * preceded by this Exporter's delimiter character, unless this is the first
	 * field of a record.
	 *
	 * @param in The Integer to be output.
	 */
	public void append(Integer in);

	/**
	 * Add an Short field to the current output record, formatted using the
	 * Short.toString() method. For delimited files, the field will be
	 * preceded by this Exporter's delimiter character, unless this is the first
	 * field of a record.
	 *
	 * @param in The Short to be output.
	 */
	public void append(Short in);

	/**
	 * Add a Byte field to the current output record, formatted using the
	 * Byte.toString() method. For delimited files, the field will be preceded
	 * by this Exporter's delimiter character, unless this is the first field of
	 * a record.
	 *
	 * @param byt The Byte value to be output.
	 */
	public void append(Byte byt);

	/**
	 * Add a Boolean field to the current output record, using a value of "0"
	 * for false and "1" for true. For delimited files, the field will be
	 * preceded by this Exporter's delimiter character, unless this is the first
	 * field of a record.
	 *
	 * @param b The Boolean value to be output.
	 */
	public void append(Boolean b);

	/**
	 * Add a boolean field to the current output record, using a value of "0"
	 * for false and "1" for true. For delimited files, the field will be
	 * preceded by this Exporter's delimiter character, unless this is the first
	 * field of a record.
	 *
	 * @param b The boolean value to be output.
	 */
	public void append(boolean b);

	/**
	 * Add a BigDecimal field to the current output record, formatted with two
	 * decimal places. For delimited files, the field will be preceded by this
	 * Exporter's delimiter character, unless this is the first field of a
	 * record.
	 *
	 * @param d The BigDecimal value to be output.
	 */
	public void append(BigDecimal d);

	/**
	 * Add a BigDecimal field to the current output record, formatted with the
	 * number of decimal places specified. For delimited files, the field will
	 * be preceded by this Exporter's delimiter character, unless this is the
	 * first field of a record.
	 *
	 * @param d The BigDecimal value to be output.
	 * @param places The number of decimal places to format in the output.
	 */
	public void append(BigDecimal d, int places);

	/**
	 * Add a Date field to the current output record, formatted as "MM/dd/yyyy".
	 * For delimited files, the field will be preceded by this Exporter's
	 * delimiter character, unless this is the first field of a record.
	 *
	 * @param d The Date to be output.
	 */
	public void append(Date d);

	/**
	 * Add a Date & Time field to the current output record, formatted as
	 * "MM/dd/yyyy hh:mm:ss". For delimited files, the field will be preceded by
	 * this Exporter's delimiter character, unless this is the first field of a
	 * record.
	 *
	 * @param d The Date object to be output (including time of day).
	 */
	public void appendDateTime(Date d);

}
