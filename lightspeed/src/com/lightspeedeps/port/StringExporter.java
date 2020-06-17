/**
 * File: Exporter.java
 */
package com.lightspeedeps.port;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * An abstract class used to support generating an export file of "readable"
 * fields. It also includes support for managing a delimiter character, which
 * defaults to a tab; this class does not actually use the delimiter for any
 * purpose.
 * <p>
 * This class converts all the various append calls to String appends. It is up
 * to the subclass to implement the String append.
 */
public abstract class StringExporter implements DelimitedExporter {

	/** The OutputStream to which all fields are added. May be specified in
	 * a constructor or via the {@link #setOut(OutputStream)} method. */
	private OutputStream out;

	/** The field delimiter. Defaults to ASCII tab. */
	private byte delimiter = '\t';

	/** Used for formatted decimal data to any requested number
	 * of decimal digits.  Created once per instance to reduce
	 * export overhead. */
	private final DecimalFormat formatterAny;

	/** Used for formatted decimal data to the default of two
	 * decimal digits.  Created once per instance to reduce
	 * export overhead. */
	private final DecimalFormat formatterTwo;

	/** A StringBuffer used in formatting data; created once per
	 * instance to reduce overhead. */
	private final StringBuffer buffer;

	/** A FieldPosition used in formatting decimal data; created once per
	 * instance to reduce overhead. */
	private final FieldPosition position;

	/** A date formatter, created once per instance to reduce overhead. */
	private SimpleDateFormat dateFormatter;

	/** A date formatter, created once per instance to reduce overhead. */
	private SimpleDateFormat timestampFormatter;


	/**
	 * Default constructor. If you use this constructor, you must specify the
	 * OutputStream to be used via the {@link #setOut(OutputStream)} method
	 * before calling any of the 'append(...)' methods.
	 * <p>
	 * Note that dates and time-stamps exported using this constructor will have
	 * dates formatted as 'M/d/yyyy', where month or day numbers less than 10
	 * only use a single digit. If you want dates always formatted using 2-digit
	 * month and day numbers, use the
	 * {@link #StringExporter(OutputStream, String)} constructor, and pass it a
	 * dateFormat of 'MM/dd/yyyy'.
	 * <p>
	 * You can also use the second constructor if you want other changes to the
	 * date format, such as a 2-digit year, or changing the order of the fields,
	 * e.g., 'yy-mm-dd'.
	 */
	public StringExporter() {
		formatterTwo = new DecimalFormat("###########0.00");
		formatterAny = new DecimalFormat("###########0.");
		buffer = new StringBuffer(20);
		position = new FieldPosition(NumberFormat.INTEGER_FIELD);
		dateFormatter = new SimpleDateFormat("M/d/yyyy");
		timestampFormatter = new SimpleDateFormat("M/d/yyyy HH:mm:ss");
	}

	/**
	 * Construct a new StringExporter which will output data to the stream
	 * given.
	 * <p>
	 * Note that dates and time-stamps exported using this constructor will have
	 * dates formatted as 'M/d/yyyy', where month or day numbers less than 10
	 * only use a single digit. If you want dates always formatted using 2-digit
	 * month and day numbers, use the
	 * {@link #StringExporter(OutputStream, String)} constructor, and pass it a
	 * dateFormat of 'MM/dd/yyyy'.
	 * <p>
	 * You can also use the second constructor if you want other changes to the
	 * date format, such as a 2-digit year, or changing the order of the fields,
	 * e.g., 'yy-mm-dd'.
	 *
	 * @param stream The OutputStream to be used by the export process.
	 */
	public StringExporter(OutputStream stream) {
		this();
		out = stream;
	}

	/**
	 * @param stream
	 * @param dateFormat
	 */
	public StringExporter(OutputStream stream, String dateFormat) {
		this();
		out = stream;
		dateFormatter = new SimpleDateFormat(dateFormat);
		timestampFormatter = new SimpleDateFormat(dateFormat + " HH:mm:ss");
	}

	/**
	 * @see com.lightspeedeps.port.Exporter#append(java.lang.Integer)
	 */
	@Override
	public void append(Integer in) {
		if (in == null) {
			append((String)null);
		}
		else {
			append(in.toString());
		}
	}

	/**
	 * @see com.lightspeedeps.port.Exporter#append(java.lang.Integer)
	 */
	@Override
	public void append(Short in) {
		if (in == null) {
			append((String)null);
		}
		else {
			append(in.toString());
		}
	}

	/**
	 * @see com.lightspeedeps.port.Exporter#append(java.lang.Byte)
	 */
	@Override
	public void append(Byte byt) {
		if (byt == null) {
			append((String)null);
		}
		else {
			append(byt.toString());
		}
	}

	/**
	 * @see com.lightspeedeps.port.Exporter#append(java.lang.Boolean)
	 */
	@Override
	public void append(Boolean b) {
		if (b == null) {
			append((String)null);
		}
		else {
			append( b ? "1" : "0");
		}
	}

	/**
	 * @see com.lightspeedeps.port.Exporter#append(boolean)
	 */
	@Override
	public void append(boolean b) {
		append( b ? "1" : "0");
	}

	/**
	 * @see com.lightspeedeps.port.Exporter#append(java.math.BigDecimal)
	 */
	@Override
	public void append(BigDecimal d) {
		if (d == null) {
			append((String)null);
		}
		else {
			buffer.setLength(0);
			append(formatterTwo.format(d, buffer, position).toString());
		}
	}

	/**
	 * @see com.lightspeedeps.port.Exporter#append(java.math.BigDecimal, int)
	 */
	@Override
	public void append(BigDecimal d, int places) {
		if (d == null) {
			append((String)null);
		}
		else {
			formatterAny.setMaximumFractionDigits(places);
			formatterAny.setMinimumFractionDigits(places);
			append(formatterAny.format(d));
		}
	}

	/**
	 * @see com.lightspeedeps.port.Exporter#append(java.util.Date)
	 */
	@Override
	public void append(Date d) {
		if (d == null) {
			append("");
		}
		else {
			append(dateFormatter.format(d));
		}
	}

	/**
	 * @see com.lightspeedeps.port.Exporter#appendDateTime(java.util.Date)
	 */
	@Override
	public void appendDateTime(Date d) {
		if (d == null) {
			append("");
		}
		else {
			append(timestampFormatter.format(d));
		}
	}

	/** See {@link #out}. */
	public OutputStream getOut() {
		return out;
	}
	/** See {@link #out}. */
	public void setOut(OutputStream out) {
		this.out = out;
	}

	/** See {@link #delimiter}. */
	@Override
	public byte getDelimiter() {
		return delimiter;
	}
	/** See {@link #delimiter}. */
	@Override
	public void setDelimiter(byte delimiter) {
		this.delimiter = delimiter;
	}

}
