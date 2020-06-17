/**
 * File: Exporter.java
 */
package com.lightspeedeps.port;

import java.io.IOException;
import java.io.OutputStream;

import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;

/**
 * A class used to generate an export file of "human readable" fields, separated
 * by delimiters.  The delimiter character defaults to a tab.  Such a file
 * is suitable for importing into Excel or similar products.
 */
public class FlatExporter extends StringExporter {

	/** True iff a delimiter should be output before more data is sent.  This is
	 * set true whenever we output a field, and false when we finish a record. */
	private boolean needDelimiter;

	@SuppressWarnings("unused")
	private FlatExporter() {
		// hide the default constructor.
	}

	/**
	 * Construct a new Exporter which will output data to the stream given.
	 *
	 * @param stream
	 */
	public FlatExporter(OutputStream stream) {
		super(stream);
		needDelimiter = false;
	}

	/**
	 * Construct a new Exporter which will output data to the stream given, with
	 * a custom date format for all exported Date values.
	 *
	 * @param outputStream
	 * @param dateFormat The format style to use for exported dates, e.g.,
	 *            "M/d/yy".
	 */
	public FlatExporter(OutputStream stream, String dateFormat) {
		super(stream, dateFormat);
		needDelimiter = false;
	}

	/**
	 * @see com.lightspeedeps.port.Exporter#next()
	 */
	@Override
	public void next() {
		try {
			getOut().write(Constants.NEW_LINE.getBytes());
		}
		catch (IOException e) {
			EventUtils.logError(e);
		}
		needDelimiter = false;
	}

	/**
	 * @see com.lightspeedeps.port.Exporter#append(java.lang.String)
	 */
	@Override
	public void append(String s) {
		try {
			if (needDelimiter) {
				getOut().write(getDelimiter());
			}
			if (s != null) {
				getOut().write(s.getBytes());
			}
			needDelimiter = true;
		}
		catch (IOException e) {
			EventUtils.logError(e);
		}
	}

}
