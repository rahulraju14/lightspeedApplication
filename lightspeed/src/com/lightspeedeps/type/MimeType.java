//	File Name:	MimeType.java
package com.lightspeedeps.type;

import com.lightspeedeps.util.common.StringUtils;

/**
 * Enumeration of file types, based on Internet standards; used in
 * the File Repository and On-boarding areas.
 */
public enum MimeType {
	BMP 			("BMP", "image/bmp", "bmp"),
	EXCEL 			("Excel", "application/x-excel", "xls"),
	GIF 			("GIF", "image/gif", "gif"),
	JPEG 			("JPEG", "image/jpeg", "jpg"),
	MS_EXCEL1 		("Excel", "application/vnd.ms-excel", "xls"),
	MS_EXCEL2 		("Excel", "application/ms-excel", "xls"),
	MS_EXCEL3 		("Excel", "application/msexcel", "xls"),
	MS_WORD 		("Word", "application/msword", "doc"),
	PDF 			("PDF", "application/pdf", "pdf"),
	PNG 			("PNG", "image/png", "png"),
	TIFF 			("TIFF", "image/tiff", "tif"),
	TEXT 			("Text", "text/plain", "txt"),
	LS_FORM			("Text", "text/plain", "lsf"),
	N_A 			("Unknown", "N/A", "");		// unknown / uninitialized


	private static String[] typeNames;
	private static String[] extensions;

	static {
		// create & populate the arrays of typeNames & extensions
		typeNames = new String[MimeType.values().length];
		extensions = new String[MimeType.values().length];

		for (MimeType mime : MimeType.values()) {
			typeNames[mime.ordinal()] = mime.typeName;
			extensions[mime.ordinal()] = mime.extension;
		}
	}

	private final String label;
	private final String typeName;
	private final String extension;

	MimeType(String lab, String type, String ext) {
		label = lab;
		typeName = type;
		extension = ext;
	}

	/**
	 * Returns the equivalent MIME content type for the enumerated value.
	 * <p>
	 * NOTE: This overrides the default toString(), which returns
	 * the same value as name(), which is the exact text of the enum name.
	 */
	@Override
	public String toString() {
		return label;
	}

	/**
	 * Convert a string to its corresponding MimeType. In this case, the string should
	 * match one of the values from the "typeNames" array, which are the values returned by the
	 * toString() method. Therefore, for any MimeType mt, the following is true:
	 * <p>
	 * mt == MimeType.toValue(mt.toString())
	 * <p>
	 * In a sense, toValue() is the inverse of toString(), in the same way that the (Java-defined)
	 * method valueOf() is the inverse of name().
	 * <p>
	 * If the string does not match a 'typeNames' value, an attempt is made to use valueOf() to match
	 * the string to an enumeration name. If this also fails, then "N_A" is returned.
	 *
	 * @param s The string representing an enumerated value.
	 * @return The equivalent MimeType value, or N_A; null is never returned.
	 */
	public static MimeType toValue(String s) {
		MimeType type = StringUtils.toValue(s, typeNames, MimeType.class);
		if (type == null) {
			type = StringUtils.toValue(s, extensions, MimeType.class);
			if (type == null) {
				try {
					type = MimeType.valueOf(s);
				}
				catch (Exception e) {
				}
				if (type == null) {
					type = N_A;
				}
			}
		}
		return type;
	}

	/**
	 * Returns the equivalent MIME content type for the enumerated value. It is
	 * the same as toString, but can be used from jsp pages since it follows the
	 * bean accessor (getter) naming convention. This could be enhanced to use
	 * the current locale setting for i18n purposes.
	 *
	 * @return The value of the enumerated type as mixed-case text.
	 */
	public String getLabel() {
		return toString();
	}

	/**
	 * Returns the file extension which is normally associated (in the Windows
	 * environment) with a file of this MIME type.
	 *
	 * @return file extension
	 */
	public String getExtension() {
		return extension;
	}
	
	/**
	 * @return True iff the mime type is for Image.
	 */
	public boolean isImage() {
		return this == JPEG || this == PNG || this == GIF;
	}
	
	/**
	 * @return True iff the mime type is for PDF.
	 */
	public boolean isPdf() {
		return this == PDF;
	}

}
