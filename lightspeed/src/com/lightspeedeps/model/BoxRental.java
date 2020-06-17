package com.lightspeedeps.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lightspeedeps.port.Exporter;
import com.lightspeedeps.util.common.StringUtils;

/**
 * BoxRental entity. Holds the information for one Box Rental form,
 * which is associated with a particular WeeklyTimecard.
 */
@Entity
@Table(name = "box_rental")
@JsonIgnoreProperties({"id","weeklyTimecard"}) // used by generateJsonSchema()
public class BoxRental extends PersistentObject<BoxRental> implements Cloneable {
	private static final Log log = LogFactory.getLog(BoxRental.class);
	/**  */
	private static final long serialVersionUID = 1L;

	// Fields

	private WeeklyTimecard weeklyTimecard;
	private BigDecimal amount = BigDecimal.ZERO;
	private Boolean inventoryOnFile = false;
	private String inventory;
	private String comments;

	/** True iff the amount specified in the Box Rental form matches the box rental
	 * amount listed in the Start Form. */
	@Transient
	private Boolean matchesStart = null;

	/** The edit-mode version of the inventory, in which html breaks are replaced by
	 * new-line characters. */
	@Transient
	private String inventoryEdit;

	// Constructors

	/** default constructor */
	public BoxRental() {
	}

	/** minimal constructor */
	public BoxRental(WeeklyTimecard weeklyTimeCard) {
		weeklyTimecard = weeklyTimeCard;
	}

	// Property accessors

	@JsonIgnore
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Weekly_Id", nullable = false)
	public WeeklyTimecard getWeeklyTimecard() {
		return weeklyTimecard;
	}
	public void setWeeklyTimecard(WeeklyTimecard wtc) {
		weeklyTimecard = wtc;
	}

	@Column(name = "Amount", precision = 8, scale=2)
	public BigDecimal getAmount() {
		return amount;
	}
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	@Column(name = "Inventory_On_File", nullable = false)
	public Boolean getInventoryOnFile() {
		return inventoryOnFile;
	}
	public void setInventoryOnFile(Boolean inventoryOnFile) {
		this.inventoryOnFile = inventoryOnFile;
	}

	@Column(name = "Inventory", length = 5000)
	public String getInventory() {
		return inventory;
	}
	public void setInventory(String inventory) {
		this.inventory = inventory;
	}

	/** See {@link #inventoryEdit}. */
	@JsonIgnore
	@Transient
	public String getInventoryEdit() {
		if (inventoryEdit == null) {
			inventoryEdit = StringUtils.editHtml(getInventory());
		}
		return inventoryEdit;
	}
	/** See {@link #inventoryEdit}. */
	public void setInventoryEdit(String inventoryEdit) {
		this.inventoryEdit = inventoryEdit;
		inventory = StringUtils.saveHtml(inventoryEdit);
	}

	@Column(name = "Comments", length = 5000)
	public String getComments() {
		return comments;
	}
	public void setComments(String comments) {
		this.comments = comments;
	}

	/**See {@link #matchesStart}. */
	@JsonIgnore
	@Transient
	public Boolean getMatchesStart() {
		return matchesStart;
	}
	/**See {@link #matchesStart}. */
	public void setMatchesStart(Boolean matchesStart) {
		this.matchesStart = matchesStart;
	}

	/**
	 * @see java.lang.Object#clone()
	 */
	@Override
	public BoxRental clone() {
		BoxRental bx;
		try {
			bx = (BoxRental)super.clone();
			bx.id = null;
			bx.weeklyTimecard = null; // only one BoxRental allowed per timecard
			bx.matchesStart = null; // transient
		}
		catch (CloneNotSupportedException e) {
			log.error("BoxRental clone error: ", e);
			return null;
		}
		return bx;
	}

	/**
	 * Export data in this instance via an Exporter. This is currently used to
	 * turn this object into a flat record. The timecard data can then be loaded
	 * into other products, such as Crew Cards.
	 *
	 * @param ex The Exporter to which each field should be passed.
	 */
	public void flatten(Exporter ex) {
//		ex.append(getId());
		ex.append(getAmount());
		ex.append(getInventoryOnFile());
		ex.append(getInventory());
		ex.append(getComments());
	}

}
