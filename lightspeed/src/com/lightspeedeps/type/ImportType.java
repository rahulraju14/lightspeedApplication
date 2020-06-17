//	File Name:	ImportType.java
package com.lightspeedeps.type;

/**
 * Enumeration used in the Script object to indicate the type of source file
 * provided by the user during import to create the Script object.
 */
public enum ImportType {
	/** Script was imported from a PDF. */
	PDF			("PDF"),
	/** Script was imported from a .sex - Script EXchange - file. */
	SEX			("Script EXchange"),
	/** Script was created manually - no file was imported. */
	MANUAL		("Manually entered"),
	/** Script was imported from a Final Draft (ver 8 or later) file. */
	FDX			("Final Draft FDX"),
	/** FDX (Final Draft) import changes to FDX_F after we format it to create Page entries */
	FDX_F		("Final Draft FDX - formatted"),
	/** Script was imported from a Final Draft Tagger (ver 7 or earlier) XML file. */
	TAGGER		("Tagger XML"),
	/** Tagger import changes to TAGGER_F after we format it to create Page entries */
	TAGGER_F	("Tagger XML - formatted")
	;
	private final String heading;

	ImportType(String head) {
		heading = head;
	}

	/**
	 * Returns a "pretty" mixed-case version of the enumerated value.
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
	 * This could be enhanced to use the current locale setting for i18n purposes.
	 *
	 * @return The value of the enumerated type as mixed-case text.
	 */
	public String getLabel() {
		return this.toString();
	}

	/**
	 * @return True if this style of Script file is considered "formatted", usually meaning
	 * that we have Page objects associated with the script.
	 */
	public boolean isFormatted() {
		return this == PDF || this == FDX_F || this == TAGGER_F;
	}

}
