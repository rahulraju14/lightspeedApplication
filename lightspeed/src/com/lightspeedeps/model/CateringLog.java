package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * CateringLog entity. Used to track meals served, for publishing in a Callsheet or DPR.
 * Note that beginning in rev 2.2.4329, we always have a single CateringLog object
 * per DPR, and use it to populate two rows, with two fields in each row. We fill
 * in the default text in the left-hand fields, and leave the right-hand fields
 * (typically counts) null.
 */
@Entity
@Table(name = "catering_log")
public class CateringLog extends PersistentObject<CateringLog> implements Cloneable {
	private static final Log log = LogFactory.getLog(CateringLog.class);

	private static final long serialVersionUID = - 448511717320080009L;

	// Fields
	private Dpr dpr;
	private String meal = "Total Amount Ordered:";
	private String vendor = "Total Amount Served:";
	private Integer mealCount;
	private String note;

	// Constructors

	/** default constructor */
	public CateringLog() {
	}

	// Property accessors

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "DPR_Id")
	public Dpr getDpr() {
		return dpr;
	}
	public void setDpr(Dpr dpr) {
		this.dpr = dpr;
	}

	@Column(name = "Meal", length = 50)
	public String getMeal() {
		return meal;
	}
	public void setMeal(String meal) {
		this.meal = meal;
	}

	@Column(name = "Vendor", length = 100)
	public String getVendor() {
		return vendor;
	}
	public void setVendor(String vendor) {
		this.vendor = vendor;
	}

	@Column(name = "Meal_Count")
	public Integer getMealCount() {
		return mealCount;
	}
	public void setMealCount(Integer mealCount) {
		this.mealCount = mealCount;
	}

	@Column(name = "Note", length = 1000)
	public String getNote() {
		return note;
	}
	public void setNote(String note) {
		this.note = note;
	}

	@Override
	public CateringLog clone() {
		CateringLog newLog;
		try {
			newLog = (CateringLog)super.clone();
			newLog.id = null;
			newLog.dpr = null;
		}
		catch (CloneNotSupportedException e) {
			log.error("CateringLog clone error: ", e);
			return null;
		}
		return newLog;
	}

}