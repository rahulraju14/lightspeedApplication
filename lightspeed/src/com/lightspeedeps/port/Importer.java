/**
 * File: Importer.java
 */
package com.lightspeedeps.port;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A class used to read fields from an import file of "readable" fields,
 * separated by delimiters. The delimiter character defaults to a tab. Such a
 * file might have been created by a spreadsheet program, a payroll processing
 * program, or other products.
 */
public class Importer {
	private static final Log log = LogFactory.getLog(Importer.class);

	/** The OutputStream to which all fields are added. Passed to this
	 * class in its constructor. */
	private BufferedReader bReader;

	/** The current line of data being processed. */
	private String line;

	/** The data fields extracted from the current line, created
	 * by splitting the line using the currently defined delimiter. */
	private String[] fields = new String[0];

	/** The index (origin 0) of the next field to be returned by
	 * our getField() method. */
	private int fieldIx = 0;

	/** The field delimiter. Defaults to ASCII tab. */
	private String delimiter = "\\t";

	/** A date formatter (used for parsing incoming dates), created once
	 * per instance to reduce overhead. */
	private SimpleDateFormat dateFormatter;


	@SuppressWarnings("unused")
	private Importer() {
		// hide the default constructor.
	}

	/**
	 * Create an import source using the fully-qualified name provided.
	 *
	 * @param fileName the fully-qualified name of the source (input) file.
	 * @throws FileNotFoundException If the file does not exist.
	 */
	public Importer(String fileName) throws FileNotFoundException {
		bReader = new BufferedReader(new FileReader(fileName));
		dateFormatter = new SimpleDateFormat("MM/dd/yyyy");
	}

	/**
	 * @return The next field from the import source, as a String; will not
	 *         return null.
	 * @throws IOException if there is an error reading from the source.
	 */
	public String getString() throws IOException {
		return getField();
	}

	/**
	 * @return The next field from the import source, as a byte.
	 * @throws IOException if there is an error reading from the source.
	 */
	public byte getByte() throws EOFException, IOException {
		String str = getField();
		if (str.trim().length() == 0) {
			return 0;
		}
		return Byte.parseByte(str);
	}

	/**
	 * @return The next field from the import source, as an int.
	 * @throws IOException if there is an error reading from the source.
	 */
	public int getInt() throws NumberFormatException, EOFException, IOException {
		String str = getField();
		if (str.trim().length() == 0) {
			return 0;
		}
		return Integer.parseInt(str);
	}

	/**
	 * @return The next field from the import source, as a boolean. The source
	 *         data may be either "1"/"0", or "true"/"false" (where case is
	 *         ignored).
	 * @throws IOException if there is an error reading from the source.
	 */
	public boolean getBoolean() throws EOFException, IOException {
		String s = getField().trim();
		return s.equals("1") || s.equalsIgnoreCase("true");
	}

	/**
	 * @return The next field from the import source, as a BigDecimal.
	 * @throws IOException if there is an error reading from the source.
	 */
	public BigDecimal getBigDecimal() throws EOFException, IOException {
		String str = getField();
		if (str.trim().length() == 0) {
			return null;
		}
		return BigDecimal.valueOf(Double.parseDouble(str));
	}

	/**
	 * @return The next field from the import source, as a Date.
	 * @throws IOException if there is an error reading from the source.
	 */
	public Date getDate() throws EOFException, ParseException, IOException {
		return dateFormatter.parse(getField());
	}

	/**
	 * Advance the input source to the next line of data. Any remaining data not
	 * acquired from the current line is discarded.
	 *
	 * @return True if there is another (non-empty) line in the file. False if
	 *         either the file system returns an end-of-file indication, or if
	 *         the line read is empty (contains no fields).
	 * @throws IOException If the file system throws an IOException.
	 */
	public boolean next() throws IOException {
		boolean bRet = true;
		line = bReader.readLine();
		if (line == null) { // readLine returns null at end of file
			bRet = false;
		}
		else {
			fields = line.split(delimiter, -1); // don't discard trailing empty fields!
			if (fields.length == 0) {
				// for our purposes, an empty line is treated as an end-of-file
				bRet = false;
			}
			fieldIx = 0;
		}
		return bRet;
	}

	/**
	 * Close the import source file.
	 */
	public void close() {
		try {
			bReader.close();
		}
		catch (IOException e) {
			log.error(e);
		}
	}

	/**
	 * Gets the next input field from the source, as a String. This
	 * automatically proceeds to the next record when the fields in the current
	 * record have been exhausted.
	 *
	 * @return The next field as a String; will not return null.
	 * @throws IOException if there is an error reading from the source.
	 * @throws EOFException if end-of-file is recognized when trying to read the
	 *             next input field.
	 */
	private String getField() throws IOException, EOFException {
		if (line == null || fieldIx >= fields.length) {
			if (! next()) {
				throw(new EOFException());
			}
		}
		String res = fields[fieldIx];
		fieldIx++;
		return res;
	}

	/** See {@link #delimiter}. */
	public String getDelimiter() {
		return delimiter;
	}
	/** See {@link #delimiter}. */
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

}
