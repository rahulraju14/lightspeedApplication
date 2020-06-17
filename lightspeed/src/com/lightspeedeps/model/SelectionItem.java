package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * SelectionItem entity. These are used primarily to create drop-down lists for
 * user selection. The 'type' field is used as a key to retrieve all the entries
 * to create a given drop-down list. This facility is used in place of
 * hard-coded enumerations in cases where the list of choices is likely to
 * change more frequently than is desirable for something to be hard-coded.
 */
@Entity
@Table(name = "selection_item",
		uniqueConstraints = @UniqueConstraint(columnNames = {"Type", "Name" }))
public class SelectionItem extends PersistentObject<SelectionItem> {

	/** */
	private static final long serialVersionUID = 1L;

	// Values of the 'type' field for use in creating drop-down lists.

	public static final String TV_PRODUCTION_TYPE = "TV-PT";
	public static final String TV_PRODUCTION_SEASON = "TV-SS";
	public static final String TV_PRODUCTION_START_DATE = "TV-SD";
	public static final String FEATURE_PRODUCTION_TYPE = "FT-PT";
//	public static final String INDIE_FEATURE_PRODUCTION_TYPE = "FT-I-PT";
//	public static final String MAJOR_FEATURE_PRODUCTION_TYPE = "FT-M-PT";
	public static final String NEW_YORK_REGION = "NY-REG";

	public static final String RETIREMENT_PLAN = "RETIRE";

	public static final String I9_DOC_LIST_A = "I9-DOC-A";
	public static final String I9_DOC_LIST_B = "I9-DOC-B";
	public static final String I9_DOC_LIST_C = "I9-DOC-C";
	// LS-2196
	public static final String BILLER_TYPE_CA = "BILLER-CA";

	// Fields

	/** The key for the drop-down list. */
	private String type;

	/** The name (value) of the selection item. */
	private String name;

	/** The label of the selection item, which is displayed in the
	 * drop-down list. */
	private String label;

	// Constructors

	/** default constructor */
	public SelectionItem() {
	}

	/** See {@link #type}. */
	@Column(name = "Type", nullable = false, length = 10)
	public String getType() {
		return type;
	}
	/** See {@link #type}. */
	public void setType(String enumType) {
		type = enumType;
	}

	/** See {@link #name}. */
	@Column(name = "Name", nullable = false, length = 10)
	public String getName() {
		return name;
	}
	/** See {@link #name}. */
	public void setName(String name) {
		this.name = name;
	}

	/** See {@link #label}. */
	@Column(name = "Label", length = 50)
	public String getLabel() {
		return label;
	}
	/** See {@link #label}. */
	public void setLabel(String label) {
		this.label = label;
	}

}
