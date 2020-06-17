package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Unions entity. Each instance describes one Local union.
 */
@Entity
@Table(name = "unions")
public class Unions extends PersistentObject<Unions> {

	private static final long serialVersionUID = - 5268470237119015732L;

	/** The key that represents the Non-Union entry in the table, and
	 * is used in StartForm and other places as the standard non-Union
	 * entry. */
	public static final String NON_UNION = "NonU";

	/** The string used for the DGA (Directors Guild of America) union. */
	public static final String DGA = "DGA";

	/** The Unions key for the 'generic' Teamsters union, used for
	 * local Teamsters unions, not the 399. */
	public static final String TEAMSTERS_NUMBER = "TEAM";

	// Fields

	/** determines the order of the items in displayed lists - numerically ascending. */
	private Short listOrder;

	/** The union "number", which may contain alpha characters, too. It is displayed
	 * when the there isn't room for the full name. */
	private String number;

	/** A unique business identifier; this is the value stored in the Start Form. It
	 * may be used to distinguish sub-groups within a union, such as Photographers
	 * or Publicists in union 600. */
	private String unionKey;

	/** The union value to use in looking up occupations and occupation codes.
	 * This may differ from "number". E.g., all the ASA unions have a common
	 * lookup value of "ASA", since they share the same list of occupations; but
	 * 700 has multiple occupationUnion values like '700E' for Editors and
	 * '700S' for Sound technicians. */
	private String occupationUnion;

	/** The full name of the union, including the (sub)group description, e.g.,
	 * "600 - Publicists" or "78 - Plumbers and Pipe Fitters". */
	private String name;

	/** Local Number being passed to the green screen during export */
	private String gsLocalNum;

	// Constructors

	/** default constructor */
	public Unions() {
	}

	@Column(name = "List_Order", nullable = false)
	public Short getListOrder() {
		return listOrder;
	}
	public void setListOrder(Short listOrder) {
		this.listOrder = listOrder;
	}

	@Column(name = "Number", nullable = false, length = 10)
	public String getNumber() {
		return number;
	}
	public void setNumber(String number) {
		this.number = number;
	}

	/**See {@link #unionKey}. */
	@Column(name = "Union_Key", nullable = false, length = 10)
	public String getUnionKey() {
		return unionKey;
	}
	/**See {@link #unionKey}. */
	public void setUnionKey(String code) {
		unionKey = code;
	}

	/**See {@link #occupationUnion}. */
	@Column(name = "Occupation_Union", nullable = false, length = 10)
	public String getOccupationUnion() {
		return occupationUnion;
	}
	/**See {@link #occupationUnion}. */
	public void setOccupationUnion(String occupationUnion) {
		this.occupationUnion = occupationUnion;
	}

	@Column(name = "Name", nullable = false, length = 100)
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	/** See {@link #gsLocalNum}. */
	@Column(name = "GS_Local_Num")
	public String getGsLocalNum() {
		return gsLocalNum;
	}

	/** See {@link #gsLocalNum}. */
	public void setGsLocalNum(String gsLocalNum) {
		this.gsLocalNum = gsLocalNum;
	}

}
