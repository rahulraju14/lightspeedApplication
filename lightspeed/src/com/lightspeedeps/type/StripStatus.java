//	File Name:	StripStatus.java
package com.lightspeedeps.type;

/**
 * An enumeration used in Strip - the current status of the Strip
 * within the Stripboard.  Note that the order of the enumeration determines
 * the order of the Strips for the Stripboard report generation!
 */
public enum StripStatus {
	SCHEDULED,
	UNSCHEDULED,
	OMITTED
}

