/**
 * File: Form.java
 */
package com.lightspeedeps.model;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.port.Exporter;
import com.lightspeedeps.util.app.EventUtils;

/**
 *
 */
@SuppressWarnings("rawtypes")
@MappedSuperclass
public class Form<T extends Form> extends PersistentObject<T> {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(Form.class);

	public static final String NOT_APPLICABLE = "N/A";
	public static final String NOT_APPLICABLE_ENUM = "NA";

	// Fields that are common to all Forms

	private Byte version = 1;

	/** public (default) constructor. */
	public Form() {
	}

	@Column(name = "version", nullable = false)
	public Byte getVersion() {
		return version;
	}
	public void setVersion(Byte version) {
		this.version = version;
	}

	/**
	 * Creates a mapping from the place-holder strings in the XFDF content to
	 * the actual values of the fields that should appear in the PDF when
	 * printed.
	 *
	 * @param cd The ContactDocument that "owns" the document being printed.
	 * @param fieldValues The Map from XFDF place-holder text to actual printed
	 *            text.
	 */
	public void fillFieldValues(ContactDocument cd, Map<String,String> fieldValues) {
		// subclasses which fill PDF fields need to override this method.
	}

	/**
	 * A method to apply the String trim() method to all of the fields
	 * in the class.
	 */
	public void trim() {
		// subclasses should override this with code that trims
		// all of their String fields.
	}

	/**
	 * Export the fields in this Form using the supplied Exporter.
	 * @param ex
	 */
	public void exportFlat(Exporter ex) {
		ex.append("ERROR: export of unsupported Form type");
		EventUtils.logError("Attempt to export unsupported Form type: " + this.getClass().getName());
	}

	/**
	 * Short-cut method to return the trim of a field, taking care of the
	 * check for null.
	 *
	 * @param s The String to trim, or null.
	 * @return The trimmed string, or null if 's' is null.
	 */
	public static String trim(String s) {
		return s == null ? s : s.trim();
	}

	/**
	 * Short-cut method to return the trim of a field, taking care of the
	 * check for null. It also converts either "na" or "n/a" to "N/A".
	 * Note that "Na" (mixed case) is NOT converted -- so if a name is
	 * entered like this it won't be changed.
	 *
	 * @param s The String to trim, or null.
	 * @return The trimmed string, or null if 's' is null.
	 */
	public static String trimNA(String s) {
		s = trim(s);
		if (s != null &&
				(s.equals("na") || s.equals("NA") || s.equalsIgnoreCase(NOT_APPLICABLE))) {
			s = NOT_APPLICABLE;
		}
		return s;
	}

	/**
	 * Check if a String has no significant data.
	 *
	 * @return true if the String is null, empty (zero length) or equal to "NA"
	 *         or "N/A"; otherwise false.
	 */
	public static boolean isEmptyNA(String s) {
		return (s == null || s.length() == 0 || s.equalsIgnoreCase(NOT_APPLICABLE) || s.equals(NOT_APPLICABLE_ENUM));
	}

	/**
	 * Format a date as specified, or return an empty string if the date is
	 * null.
	 *
	 * @param format The DateFormat to be used for formatting the date.
	 * @param date The Date to format or null.
	 * @return If 'date' is null, returns "" (an empty String), otherwise it
	 *         returns 'date' formatted using the supplied 'format'.
	 */
	protected static String dateFormat(DateFormat format, Date date) {
		if (date == null) {
			return "";
		}
		return format.format(date);
	}

	/**
	 * Format a date as specified, or return an empty string if the date is
	 * null.
	 *
	 * @param format The DateFormat to be used for formatting the date.
	 * @param date The Date to format or null.
	 * @return If 'date' is null, returns "" (an empty String), otherwise it
	 *         returns 'date' formatted using the supplied 'format'.
	 */
	protected static String dateFormat(DateFormat format, LocalDate date) {
		if (date == null) {
			return "";
		}
		return format.format(date);
	}

	/**
	 * Format a local date as specified, or return an empty string if the date is
	 * null.
	 *
	 * @param pattern The format to be used for formatting the date.
	 * @param localDate The LocalDate to format or null.
	 * @return If 'localDate' is null, returns "" (an empty String), otherwise it
	 *         returns 'date' formatted using the supplied 'pattern'.
	 */
	protected static String dateFormat(String pattern, LocalDate localDate) {
		if (localDate == null) {
			return "";
		}
		return localDate.format(DateTimeFormatter.ofPattern(pattern));
	}

	/**
	 * Format the given data using the supplied pattern.
	 *
	 * @param pattern A pattern string compatible with the DecimalFormat class.
	 * @param data The data to be formatted or null.
	 * @return The formatted string. If either parameter is null, returns an
	 *         empty String ("").
	 */
	protected static String patternFormat(String pattern, Integer data) {
		if (data == null || pattern == null) {
			return "";
		}
		DecimalFormat format = new DecimalFormat(pattern);
		return format.format(data);
	}

	/**
	 * Format the given data using the supplied pattern.
	 *
	 * @param pattern A pattern string compatible with the DecimalFormat class.
	 * @param data The data to be formatted or null.
	 * @return The formatted string. If either parameter is null, returns an
	 *         empty String ("").
	 */
	protected static String patternFormatBigDecimal(String pattern, BigDecimal data) {
		if (data == null || pattern == null) {
			return "";
		}
		DecimalFormat format = new DecimalFormat(pattern);
		return format.format(data);
	}

	/**
	 * Format a Byte fields into a String.
	 *
	 * @param data The value to format or null.
	 * @return The formatted value, or an empty string if 'data' is null.
	 */
	protected static String byteFormat(Byte data) {
		if (data == null) {
			return "";
		}
		return data.toString();
	}

	/**
	 * Format a Integer fields into a String.
	 *
	 * @param data The value to format or null.
	 * @return The formatted value, or an empty string if 'data' is null.
	 */
	protected static String intFormat(Integer data) {
		if (data == null) {
			return "";
		}
		return data.toString();
	}

	/**
	 * Format a Double fields into a String.
	 *
	 * @param data The value to format or null.
	 * @return The formatted value, or an empty string if 'data' is null.
	 */
	protected static String doubleFormat(Double data) {
		if (data == null) {
			return "";
		}
		return data.toString();
	}

	/**
	 * Format a BigDecimal fields into a String.
	 *
	 * @param data The value to format or null.
	 * @return The formatted value, or an empty string if 'data' is null.
	 */
	protected static String bigDecimalFormat(BigDecimal data) {
		if (data == null) {
			return "";
		}
		return data.toString();
	}

	/**
	 * Format a decimal value to 2 decimal places.
	 *
	 * @param bigDecimal big decimal to be converted into string with only 2
	 *            decimal places
	 * @return formatted String, or empty string if bigDecimal is null.
	 */
	public static String bigDecimalFormat2Places(BigDecimal bigDecimal) {
		return bigDecimal != null ? bigDecimal.setScale(2, BigDecimal.ROUND_HALF_EVEN).toString() : "";
	}

}
