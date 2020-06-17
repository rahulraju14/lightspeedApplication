//	File Name:	IntExtType.java
package com.lightspeedeps.type;

/**
 * An enumeration used in Scene, indicating whether the Scene represents
 * a shot indoors (INTERIOR) or outdoors (EXTERIOR).  Scenes which move from
 * one environment to the other may use the combined indicators 'INT_EXT'
 * or 'EXT_INT'.
 */
public enum IntExtType {
	// The order of the values determines the order in the drop-down lists,
	// for example, on the Breakdown Edit page.
	INTERIOR ("INT", "I",  4,	null),
	EXTERIOR ("EXT", "E",  1, 	null),
	INT_EXT  ("I/E", "IE", 3,	INTERIOR),
	EXT_INT  ("E/I", "EI", 2,	EXTERIOR),
	N_A      ("N_A", "I",  5,	INTERIOR);


	static { // set the filters we can't set via constructor
		INTERIOR.filterType = INTERIOR;
		EXTERIOR.filterType = EXTERIOR;
	}

	/** The standard label used in drop-down lists and in generated
	 * scene headings. */
	private String shortLabel;

	/** A very short string (one or two characters) to represent
	 * the value.  Used for Strip board viewer and editor. */
	private String tinyLabel;

	/** Ordering used when sorting in stripboard editor */
	private int sortOrder;

	/** A value of either INTERIOR or EXTERIOR, indicating whether this instance
	 * is considered Int or Ext for purposes of the stripboard editor
	 * filter.*/
	private IntExtType filterType;

	private IntExtType(String pShort, String tiny, int sort, IntExtType filter) {
		shortLabel = pShort;
		tinyLabel = tiny;
		sortOrder = sort;
		filterType = filter;
	}

	/** See {@link #shortLabel}. */
	public String getShortLabel() {
		return shortLabel;
	}

	/** See {@link #tinyLabel}. */
	public String getTinyLabel() {
		return tinyLabel;
	}

	/** See {@link #filterType}.	 */
	public IntExtType getFilterType() {
		return filterType;
	}

	/** See {@link #sortOrder}. */
	public int getSortOrder() {
		return sortOrder;
	}

}
