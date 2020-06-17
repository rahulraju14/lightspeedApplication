package com.lightspeedeps.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

/**
 * FilmStock entity.
 *
 * @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "film_stock")
public class FilmStock extends PersistentObject<FilmStock> implements Cloneable {
	private static final long serialVersionUID = - 6689218668582464030L;

	// Fields
	private Material material;
	@Transient
	private FilmMeasure usedTotal;
	private FilmMeasure usedToday;
	private FilmMeasure usedPrior;
	private Date date;
	private Integer inventoryPrior;
	private Integer inventoryReceived;
	private Integer inventoryUsedToday;

	/** For tracking which row is being edited on Material page. */
	@Transient
	boolean  editableFlag = false;

	/** Used to track row selection on Materials page Inventory (detail) list. */
	@Transient
	private boolean selected;

	// Constructors

	/** default constructor */
	public FilmStock() {
		inventoryPrior = 0;
		inventoryReceived = 0;
		inventoryUsedToday = 0;
	}

	/** full constructor */
/*	public FilmStock(Project project, Material material,
			FilmMeasure filmMeasureByUsedTotalId,
			FilmMeasure filmMeasureByUsedTodayId,
			FilmMeasure filmMeasureByUsedPriorId, Date date,
			Integer inventoryPrior, Integer inventoryReceived,
			Integer inventoryUsedToday) {
		this.project = project;
		this.material = material;
		this.usedTotal = filmMeasureByUsedTotalId;
		this.usedToday = filmMeasureByUsedTodayId;
		this.usedPrior = filmMeasureByUsedPriorId;
		this.date = date;
		this.inventoryPrior = inventoryPrior;
		this.inventoryReceived = inventoryReceived;
		this.inventoryUsedToday = inventoryUsedToday;
	}
*/
	// Property accessors

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Material_Id", nullable = false)
	public Material getMaterial() {
		return this.material;
	}
	public void setMaterial(Material material) {
		this.material = material;
	}

//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "Used_Total_Id")
	@Transient
	public FilmMeasure getUsedTotal() {
		if (usedTotal == null) {
			usedTotal = new FilmMeasure();
			usedTotal.setPrint(getUsedPrior().getPrint() + getUsedToday().getPrint());
			usedTotal.setNoGood(getUsedPrior().getNoGood() + getUsedToday().getNoGood());
			usedTotal.setWaste(getUsedPrior().getWaste() + getUsedToday().getWaste());
		}
		return this.usedTotal;
	}
	public void setUsedTotal(FilmMeasure filmMeasure) {
		this.usedTotal = filmMeasure;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Used_Today_Id")
	public FilmMeasure getUsedToday() {
		return this.usedToday;
	}
	public void setUsedToday(FilmMeasure filmMeasure) {
		this.usedToday = filmMeasure;
		usedTotal = null;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Used_Prior_Id")
	public FilmMeasure getUsedPrior() {
		return this.usedPrior;
	}
	public void setUsedPrior(FilmMeasure filmMeasure) {
		this.usedPrior = filmMeasure;
		usedTotal = null;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "Date", length = 0)
	public Date getDate() {
		return this.date;
	}
	public void setDate(Date date) {
		this.date = date;
	}

	@Column(name = "Inventory_Prior")
	public Integer getInventoryPrior() {
		return this.inventoryPrior;
	}
	public void setInventoryPrior(Integer inventoryPrior) {
		this.inventoryPrior = inventoryPrior;
	}

	@Column(name = "Inventory_Received")
	public Integer getInventoryReceived() {
		return this.inventoryReceived;
	}
	public void setInventoryReceived(Integer inventoryReceived) {
		this.inventoryReceived = inventoryReceived;
	}

	@Column(name = "Inventory_Used_Today")
	public Integer getInventoryUsedToday() {
		return this.inventoryUsedToday;
	}
	public void setInventoryUsedToday(Integer inventoryUsedToday) {
		this.inventoryUsedToday = inventoryUsedToday;
	}

	@Transient
	public int getInventoryTotal() {
		return getInventoryPrior() + getInventoryReceived() - getInventoryUsedToday();
	}

	/** See {@link #editableFlag}. */
	@Transient
	public boolean isEditableFlag() {
		return editableFlag;
	}
	/** See {@link #editableFlag}. */
	public void setEditableFlag(boolean editableFlag) {
		this.editableFlag = editableFlag;
	}

	/** See {@link #selected}. */
	@Transient
	public boolean getSelected() {
		return selected;
	}
	/** See {@link #selected}. */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FilmStock other = null;
		try {
			other = (FilmStock)obj;
		}
		catch (Exception e) {
			return false;
		}
		if (id == null) {
			if (other.id != null)
				return false;
		}
		else if (!id.equals(other.id))
			return false;
		return true;
	}

	/**
	 * These clones are specifically used for Home page list.
	 * @see java.lang.Object#clone()
	 */
	@Override
	public FilmStock clone() {
		FilmStock fstk;
		try {
			fstk = (FilmStock)super.clone();
			fstk.id = null;
		}
		catch (CloneNotSupportedException e) {
			//log.error(e);
			return null;
		}
		return fstk;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "id=" + (getId()==null ? "null" : getId());
		s += ", date=" + (getDate()==null ? "null" : getDate());
		s += ", material=" + (getMaterial()==null ? "null" : getMaterial());
		s += "]";
		return s;
	}

}