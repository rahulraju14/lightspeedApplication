package com.lightspeedeps.type;

/**
 * An enumeration of "clusters" of permissions, used to simplify the user interface
 * for customizing permissions.  A set of permissions that are all related to the
 * same business area or data are grouped into a named "cluster".
 */
public enum PermissionCluster {
	PRODUCTION_ADMIN(1, "Production Admin","Includes: Administrative access, production preferences.", (1L<<8)+(1L<<12)+(1L<<6)+(1L<<51)+(1L<<23)+0, (1L<<30)+(1L<<49)+(1L<<29)+0),
	PROJECT_EPISODE_ADMIN(2, "Project/Episode Admin","Includes: Project/Episode management & preferences.", (1L<<14)+(1L<<15)+0, (1L<<40)+(1L<<41)+0),
	CREW_LIST(3, "Crew List","Includes: Crew names, positions, departments.", (1L<<5)+(1L<<7)+0, (1L<<28)+0),
	CAST_LIST(4, "Cast List","Includes: Cast names, roles, representation.", (1L<<4)+0, (1L<<27)+0),
	SCRIPT(5, "Script","Includes: Script pages, revisions, breakdown, script elements.", (1L<<20)+(1L<<18)+(1L<<19)+0, (1L<<43)+(1L<<44)+(1L<<46)+(1L<<0)+0),
	SCHEDULE(6, "Schedule","Includes: Calendar, strip boards, call sheets, DooD.", (1L<<2)+(1L<<21)+(1L<<22)+0, (1L<<48)+(1L<<45)+(1L<<26)+(1L<<47)+(1L<<59)+0),
	PRODUCTION_ELEMENTS(7, "Production Elements","Includes: Locations, Props, and all other non-Cast production elements.", (1L<<16)+(1L<<17)+0, (1L<<42)+0),
//	MEDIA_MATERIALS(8, "Media & Materials","Includes: Film stock inventory and other media.", (1L<<11)+0, (1L<<38)+0),
	CALL_SHEETS_PRS(9, "Call Sheets & PRs","Includes: Daily call sheets and production reports.", (1L<<3)+(1L<<13)+(1L<<55)+(1L<<56)+(1L<<57)+(1L<<53)+0, (1L<<36)+(1L<<37)+(1L<<39)+0),
	CAST_TIMECARD(10, "Cast Timecard","Includes: Exhibit G report.", (1L<<9)+0, (1L<<31)+0),
	FINANCIAL_PERMISSIONS(11, "Financial Permissions","Includes: Crew timecards, start forms.", (1L<<34)+(1L<<60)+(1L<<32)/*+(1L<<33)*/+0, (1L<<10)/*+(1L<<63)*/+0),
	ONBOARDING(12, "Onboarding","Includes: Viewing and managing Start documents", (1L<<52)+(1L<<54)+0, (1L<<35)+0),	N_A(13, "N/A","", (1L<<25)+0, 0),
	;

	public final static short NONE = 0;
	public final static short VIEW = 1;
	public final static short EDIT = 2;
	public final static short CUSTOM = 3;

	/** Unique id, used to be the database key. Specified in the Permissions spreadsheet,
	 * it is used to generate the bit mask. */
	private final short id;

	/** The printable name for this permission */
	private final String name;

	private final String description;

	/** The bit mask for the "View" option of this cluster.  If the result of a bit-wise ANDing of
	 * this mask against a Permission.mask field is non-zero, then that Permission is included
	 * in this cluster. */
	private final long viewMask;

	/** The bit mask for the "Edit" option of this cluster.  If the result of a bit-wise ANDing of
	 * this mask against a Permission.mask field is non-zero, then that Permission is included
	 * in this cluster. */
	private final long editMask;

	private PermissionCluster(int pId, String heading, String desc, long edMask, long vwMask) {
		id = (short)pId;
		name = heading;
		viewMask = vwMask;
		editMask = edMask;
		description = desc;
	}

	/** See {@link #id}. */
	public short getId() {
		return id;
	}

	/** See {@link #name}. */
	public String getName() {
		return name;
	}

	/** See {@link #description}. */
	public String getDescription() {
		return description;
	}

	/** See {@link #viewMask}. */
	public long getViewMask() {
		return viewMask;
	}

	/** See {@link #editMask}. */
	public long getEditMask() {
		return editMask;
	}

	@Override
	public String toString() {
		return name;
	}

//	/**
//	 * Create a List of Read/Write/Neither indicators related to each PermissionCluster`s
//	 * setting within the supplied bit mask, which may have any number of bits
//	 * set on.
//	 *
//	 * @param pMask
//	 * @return An array of short`s, indexed by the ordinal values
//	 * of the PermissionCluster class, where each entry is one of the
//	 */
//	public static short[] createClusterArray(long pMask) {
//		short[] clusters =  new short[PermissionCluster.values().length];
//		for (int i = 0; i < clusters.length; i++) {
//			PermissionCluster pc = PermissionCluster.values()[i];
//			if ((pc.editMask & pMask) == pc.editMask) {
//				clusters[i] = EDIT;
//			}
//			else if ((pc.viewMask & pMask) == pc.viewMask) {
//				clusters[i] = VIEW;
//			}
//			else if (((pc.viewMask | pc.editMask) & pMask) != 0) {
//				// the supplied mask does not contain all the permissions
//				// of this Cluster's view or edit set, but it contains
//				// at least one of the permissions - consider it "custom".
//				clusters[i] = CUSTOM;
//			}
//			else {
//				clusters[i] = NONE;
//			}
//		}
//		return clusters;
//	}

//	public static Set<PermissionCluster> createClusterSet(long pMask) {
//		Set<PermissionCluster> clusters =  new HashSet<PermissionCluster>();
//		for (PermissionCluster pc : PermissionCluster.values()) {
//			if ((pc.viewMask & pMask) == pc.viewMask) {
//				clusters.add(pc);
//			}
//		}
//		return clusters;
//	}

}
