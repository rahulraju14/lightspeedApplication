//	File Name:	RealLinkStatus.java
package com.lightspeedeps.type;

/**
 * An enumeration used in {@link com.lightspeedeps.model.RealLink}.
 */
public enum RealLinkStatus {
	UNDER_REVIEW,
	SELECTED,
	REJECTED;

	public final static String[] headings = {
		"Candidate",
		"Selected",
		"Not Selected" };

	@Override
	public String toString() {
		return headings[this.ordinal()];
	}

}

