//	File Name:	PermissionStyle.java
package com.lightspeedeps.type;

/**
 * An enumeration used in the Permission object.
 * <p>
 * ** NOTE **
 * These are stored as ordinal values in the Permission table, so
 * the order of the enumerated values below is critical and must match the
 * order implied by the values defined in the Permissions spreadsheet!!
 * **
 */
public enum PermissionStyle {
	/** 0 -- the 'online' permission */
	ONLINE,
	/** 1 -- the write-any permission assigned to LS Admin users */
	SUPER_USER,
	/** 2 -- permissions that only require read access to the production data */
	READ,
	/** 3 -- permissions that allow changing the production data */
	WRITE;
}
