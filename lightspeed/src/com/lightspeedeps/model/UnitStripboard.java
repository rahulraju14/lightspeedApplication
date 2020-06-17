package com.lightspeedeps.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * UnitStripboard entity. This holds information related to the set of Strip`s
 * that are scheduled for a particular Unit within one Stripboard. Currently the
 * only data is the number of shooting days defined by that set of Strip`s.
 */
@Entity
@Table(name = "unit_stripboard")
public class UnitStripboard extends PersistentObject<UnitStripboard> implements Cloneable {

	private static final long serialVersionUID = -1367649982709356748L;

	// Fields

	/** The Unit that this information relates to. */
	private Unit unit;

	/** The Stripboard that this information relates to. */
	private Stripboard stripboard;

	/** The number of shooting days currently defined by the Strip`s that
	 * have been scheduled in this Unit's stripboard tab. */
	private Integer shootingDays = 1;

	// Constructors

	/** default constructor */
	public UnitStripboard() {
	}

	/** full constructor */
	public UnitStripboard(Unit unit, Stripboard stripboard) {
		this.unit = unit;
		this.stripboard = stripboard;
	}

	// Property accessors

	@ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
	@JoinColumn(name = "Unit_Id", nullable = false)
	public Unit getUnit() {
		return this.unit;
	}
	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Stripboard_Id", nullable = false)
	public Stripboard getStripboard() {
		return this.stripboard;
	}
	public void setStripboard(Stripboard stripboard) {
		this.stripboard = stripboard;
	}

	@Column(name = "Shooting_Days", nullable = false)
	public Integer getShootingDays() {
		return this.shootingDays;
	}
	public void setShootingDays(Integer shootingDays) {
		this.shootingDays = shootingDays;
	}

	@Override
	public Object clone() {
		try {
			UnitStripboard copy = (UnitStripboard)super.clone();
			copy.id = null; // it's a transient object
			return copy;
		}
		catch (CloneNotSupportedException e) { // should not happen
			return null;
		}
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "id=" + (getId()==null ? "null" : id);
		s += ", unit=" + (getUnit()==null ? "null" : getUnit());
		s += ", days=" + (getShootingDays()==null ? "null" : getShootingDays());
		s += "]";
		return s;
	}

}