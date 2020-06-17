//	File Name:	Permission.java
package com.lightspeedeps.type;

import java.util.HashSet;
import java.util.Set;

/**
 * An enumeration of all the Permission`s in the system. The list of Permissions
 * is maintained outside the source files in a spreadsheet, and used to create this
 * enum class. Other permission data is loaded from that spreadsheet into the database,
 * including the Role, RoleGroup and PageFieldAccess information.
 */
public enum Permission {
	COMPARE_REVISIONS			( 1, 0,0,2,"Compare Revisions"),
	WRITE_ANY					( 2, 0,0,1,"Write Any"),
	EDIT_CALENDAR				( 3,49,1,3,"Edit Calendar"),
	EDIT_CALL_SHEET				( 4,46,1,3,"Edit Call Sheets"),
	EDIT_CONTACTS_CAST			( 5,28,0,3,"Edit Cast List"),
	EDIT_CONTACTS_CREW			( 6,29,0,3,"Edit Crew List"),
	EDIT_CONTACTS_HIDDEN		( 7,30,0,3,"Edit Hidden Cast & Crew"),
	EDIT_CONTACTS_NONPROD		( 8, 0,0,3,"Edit Vendors"),
	EDIT_PRODUCTION_PREFERENCES	( 9,31,0,3,"Edit Production Preferences"),
	EDIT_EXHIBIT_G				(10,32,1,3,"Edit Exhibit G"),
	VIEW_TIMECARD_HTG			(11, 0,0,2,"View Timecard Gross"),
	UNUSED_1					(12,39,1,3,"was: Edit Media Inventory"), // LS-1595
	MANAGE_PROJECTS				(13,41,0,3,"Manage Projects/Episodes"),
	EDIT_PRODUCTION_REPORT		(14,40,1,3,"Edit Production Reports"),
	EDIT_PROJECT				(15,41,1,3,"Edit Project/Episode"),
	EDIT_PROJECT_PREFERENCES	(16,42,1,3,"Edit Project Preferences"),
	EDIT_RW_ELEMENTS_LOCATIONS	(17,43,0,3,"Edit Locations & Pts of Interest"),
	EDIT_RW_ELEMENTS_OTHER		(18,43,0,3,"Edit Other Production Elements"),
	EDIT_SCENE					(19,44,1,3,"Edit Scenes"),
	EDIT_SCENE_NOTES			(20,45,1,3,"Edit Scene Notes"),
	EDIT_SCRIPT_REVISIONS		(21,47,1,3,"Edit Script Revisions"),
	EDIT_STRIPBOARD				(22,48,1,3,"Edit Strip Boards"),
	EDIT_STRIPBOARD_REVISIONS	(23, 0,1,3,"Edit Strip Board List"),
	EDIT_SYSTEM_MESSAGES		(24,50,0,3,"Delete Sent Notifications"),
	UNUSED_4					(25,51,1,3,"was: Edit Cast Timecard"), // LS-1595
	ONLINE						(26, 0,0,0,"Online"),
	VIEW_CALL_SHEET_LIST		(27, 0,0,2,"View Call Sheet List"),
	VIEW_CONTACTS_CAST			(28, 0,0,2,"View Cast List"),
	VIEW_CONTACTS_CREW			(29, 0,0,2,"View Crew List"),
	VIEW_CONTACTS_HIDDEN		(30, 0,0,2,"View Hidden Cast & Crew"),
	VIEW_PRODUCTION_PREFERENCES	(31, 0,0,2,"View Production Preferences"),
	VIEW_EXHIBIT_G				(32, 0,0,2,"View Exhibit G"),
	EDIT_FINANCE_PERMISSIONS	(33, 0,1,3,"Edit Financial & Onboarding Permissions"),
//	VIEW_HOME_ELEMENT_STATUS	(34, 0,0,2,"View Element Status (Home)"),
	EDIT_TALENT_TIMESHEETS		(34,64,1,3,"Edit Talent Timesheets (Contracts)"),
	EDIT_START_FORMS			(35, 0,0,3,"Edit Crew Start Forms"),
	VIEW_ALL_DISTRIBUTED_FORMS	(36, 0,0,2,"View All Distributed Forms"),
	VIEW_HOME_REPORT_STATUS		(37, 0,0,2,"View Report Status (Home)"),
	VIEW_HOME_WORK_STATUS		(38, 0,0,2,"View Prod Report Data (Home)"),
	UNUSED_2					(39, 0,0,2,"was: View Media Inventory"), // LS-1595
	VIEW_PRODUCTION_REPORT		(40, 0,0,2,"View Production Report"),
	VIEW_PROJECT_DETAILS		(41, 0,0,2,"View Project/Episode Details"),
	VIEW_PROJECT_PREFERENCES	(42, 0,0,2,"View Project Preferences"),
	VIEW_RW_ELEMENTS			(43, 0,0,2,"View Production Elements"),
	VIEW_SCENE					(44, 0,0,2,"View Scenes"),
	VIEW_SCENE_NOTES			(45, 0,0,2,"View Scene Notes"),
	VIEW_CALL_SHEET				(46, 0,0,2,"View Call Sheets"),
	VIEW_SCRIPT_REVISIONS		(47, 0,0,2,"View Script Revisions"),
	VIEW_STRIPBOARD				(48, 0,0,2,"View Strip Board"),
	VIEW_CALENDAR				(49, 0,0,2,"View Calendar"),
	VIEW_SYSTEM_MESSAGES		(50, 0,0,2,"View Sent Notifications"),
	UNUSED_3					(51, 0,0,2,"was: View Cast Timecard"), // LS-1595
	EDIT_LOGIN_STATUS			(52, 0,0,3,"Change User Login Access"),
	MANAGE_START_DOCS			(53, 0,1,3,"Assemble and Distribute Start Forms"),
	APPROVE_PRODUCTION_REPORT	(54,40,1,3,"Approve Production Report"),
	MANAGE_START_DOC_APPROVERS	(55, 0,1,3,"Manage Start Form Approvers"),
	PUBLISH_CALL_SHEET			(56,46,1,3,"Finalize Call Sheets"),
	PUSH_CALL_TIMES				(57,46,1,3,"Broadcast Call Times"),
	SUBMIT_PRODUCTION_REPORT	(58,40,1,3,"Submit Production Report"),
	VIEW_ALL_PROJECTS			(59, 0,0,2,"View All Projects"),
	VIEW_REPORTS_DOOD			(60, 0,0,2,"View DooD Report"),
	EDIT_TIMECARD_HTG			(61,11,1,3,"Edit Timecard Gross"),
	APPROVE_TIMECARD			(62, 0,1,3,"Approve Crew Timecards"),
	APPROVE_START_DOCS			(63, 0,1,3,"Start Form Approver"),
	VIEW_TALENT_TIMESHEETS		(64, 0,0,2,"View Talent Timesheets (Contracts)"),
	;

	private final static Permission[] types = new Permission[65]; // ids 1...64 are valid; 0 is not used

//	private static final Log log = LogFactory.getLog(Permission.class);

	public final static int NUM_PERM_BITS = 64;

	/**
	 * Create a static array for looking up Permissions by id number, and
	 * calculate the "impliedByMask" fields for all Permissions.
	 */
	static {
		for (Permission type : Permission.values()) {
			types[type.id] = type;
			long impByMask = 0;
			for (Permission p2 : Permission.values()) {
				if ((p2.impliesMask & type.mask) != 0) {
					impByMask |= p2.mask;
				}
			}
			type.impliedByMask = impByMask;
		}
//		String unused = "";
//		for (int i=1; i < types.length; i++) {
//			if (types[i] == null) {
//				unused += i + ", ";
//			}
//		}
//		log.info("Unused permission values: " + unused);
	}

	/** Unique id (used to be the database key). Specified in the Permissions spreadsheet,
	 * it specifies the position of this Permission's bit within the 64-bit mask. */
	private final short id;

	/** The printable name for this permission */
	private final String name;

	/** The bit mask for this permission.  If the result of a bit-wise ANDing of
	 * this mask against a RoleGroup.permissionMask field is non-zero, then that
	 * RoleGroup includes this Permission. */
	private final long mask;

	/** The bit mask of permissions "implied" by this Permission. This is used by
	 * the Permissions page -- when a Permission is checked on, all Permissions
	 * which that Permission implies are also checked on. */
	private final long impliesMask;

	/** The bit mask of permissions that imply this Permission. This is used by
	 * the Permissions page -- when a Permission is checked OFF, then all
	 * Permissions which imply that Permission are also turned off. */
	private long impliedByMask;

	/** The PermissionStyle, usually READ or WRITE, or the special cases
	 * SUPER_USER and ONLINE.  WRITE for this setting indicates the permission
	 * allows the user to change something in the database; the changed data
	 * may be related to a Project, a Production, or the overall system. */
	private final PermissionStyle style;

	/** This permission gives the user write access to *project* data in the system. */
	private final Boolean allowsWrite;

	private Permission(int pid, int implies, int write, int iStyle, String heading) {
		id = (short)pid;
		name = heading;
		mask = 1L << (id-1);
		if (implies != 0) {
			impliesMask = 1L << (implies-1);
		}
		else {
			impliesMask = 0;
		}
		allowsWrite = write > 0;
		switch(iStyle) {
			case 1:
				style = PermissionStyle.SUPER_USER;
				break;
			case 2:
				style = PermissionStyle.READ;
				break;
			case 3:
				style = PermissionStyle.WRITE;
				break;
			case 0:
			default:
				style = PermissionStyle.ONLINE;
				break;
		}
		//System.out.println(id + "-" + mask + ", " + getName() + ", " + allowsWrite + ", " + style);
	}

	/** See {@link #id}. */
	public short getId() {
		return id;
	}

	/** See {@link #style}. */
	public PermissionStyle getStyle() {
		return style;
	}

	/** See {@link #allowsWrite}. */
	public Boolean getAllowsWrite() {
		return allowsWrite;
	}

	/** See {@link #name}. */
	public String getName() {
		return name;
	}

	/** See {@link #mask}. */
	public long getMask() {
		return mask;
	}

	/** See {@link #impliesMask}. */
	public long getImpliesMask() {
		return impliesMask;
	}

	/** See {@link #impliedByMask}. */
	public long getImpliedByMask() {
		return impliedByMask;
	}

	/**
	 * Determine if this Permission is included in the given mask.
	 *
	 * @param pMask The mask of interest.
	 * @return True iff this Permission is included in (is "on") in the given
	 *         mask.
	 */
	public boolean inMask(long pMask) {
		return (pMask & mask) != 0;
	}

	@Override
	public String toString() {
		return name;
	}

	public static Permission findById(int pId) {
		return types[pId];
	}

	public static Permission findByMask(long pMask) {
		long mask = 1;
		for (int i = 1; i <= NUM_PERM_BITS; i++) {
			if ((pMask & mask) != 0) {
				return Permission.findById(i);
			}
			mask <<= 1;
		}
		return null;
	}

	/**
	 * Create a Set of Permission`s corresponding to the supplied
	 * bit mask, which may have any number of bits set on.
	 * @param pMask
	 * @return A non-null, but possibly empty, Set of Permission`s.
	 */
	public static Set<Permission> createPermissionSet(long pMask) {
		Set<Permission> ptypes =  new HashSet<>();
		long mask = 1;
		for (int i = 1; i <= NUM_PERM_BITS; i++) {
			if ((pMask & mask) != 0) {
				ptypes.add(Permission.findById(i));
			}
			mask <<= 1;
		}
		return ptypes;
	}

	/**
	 * Convert the provided permission mask into a 64-character String, where
	 * each character is a '1' or '0', representing the value of the
	 * corresponding bit in the original long. Used for debugging purposes.
	 *
	 * @param pMask The permission mask as a 'long'.
	 * @return The 64-character printable representation of the mask.
	 */
	public static String toBinaryString(long pMask) {
		String s = Long.toBinaryString(pMask);
		s = "0000000000000000000000000000000000000000000000000000000000000000".substring(0, 64-s.length()) + s;
		s = s.substring(0,4) + " " + s.substring(4,14) + " " + s.substring(14,24) + " " +
				s.substring(24,34) + " " + s.substring(34,44) + " " + s.substring(44,54)+ " " + s.substring(54,64);
		return s;
	}

}
