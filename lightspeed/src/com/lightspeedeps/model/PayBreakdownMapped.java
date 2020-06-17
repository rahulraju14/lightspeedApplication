package com.lightspeedeps.model;

import java.io.*;
import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.*;

import org.apache.commons.logging.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lightspeedeps.port.*;
import com.lightspeedeps.type.PayCategory;
import com.lightspeedeps.util.common.*;

/**
 * This is equivalent to the {@link PayBreakdown} entity, but as
 * a @MappedSuperclass. Hibernate does not allow a single class to be both
 * an @Entity and a @MappedSuperclass, so we basically need two identical
 * classes, one of which is a @MappedSuperclass, and the other which is
 * an @Entity that extends the first one.
 * <p>
 * The PayBreakdown entity extends the PayExpense entity by adding the
 * multiplier, extended rate, and job number fields. The job number is only used
 * for Commercial productions.
 * <p>
 * The extended rate field ({@link #extRate}) is displayed for Team clients;
 * non-Team clients see the (base) rate field. LS-1831
 */
@MappedSuperclass
public class PayBreakdownMapped extends PayExpenseMapped {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(PayBreakdownMapped.class);
	private static final long serialVersionUID = -3065030383864320410L;

	// Fields

	/** Job "number", for Commercial productions. */
	private String jobNumber;

	/** Arbitrary multiplier */
	private BigDecimal multiplier;

	/** The result of multiplying the rate field times the multiplier. LS-1831 */
	private BigDecimal extRate;

	// Constructors

	/** default constructor */
	public PayBreakdownMapped() {
	}

	/** minimal constructor */
	public PayBreakdownMapped(WeeklyTimecard wtc, byte lineNumber) {
		super(wtc, lineNumber);
	}

	/**
	 * Calculate and set the line-item total for this instance.
	 * @return the calculated total.
	 */
	public BigDecimal calculateTotal() {
		BigDecimal total = NumberUtils.safeMultiply(getQuantity(), getMultiplier(), getRate());
		PayCategory cat = null;
		if (total == null) {
			total = BigDecimal.ZERO;
		}
		else if ((cat=getCategoryType()).getIsLabor()) {
			total = NumberUtils.scaleTo(total, 0, 4);
		}
		else {
			total = NumberUtils.scaleTo2Places(total);
		}
		setTotal(total);
		if (cat == PayCategory.FLAT_RATE || cat == PayCategory.MEET_GUARANTEE) {
			setExtRate(getRate());
		}
		else {
			setExtRate(NumberUtils.safeMultiply(getMultiplier(), getRate())); // update extended rate. LS-1831
		}
		return total;
	}

	/** See {@link #jobNumber}. */
	@Column(name = "Job_Number", length = 20)
	public String getJobNumber() {
		return jobNumber;
	}
	/** See {@link #jobNumber}. */
	public void setJobNumber(String jobNumber) {
		this.jobNumber = jobNumber;
	}

	/** See {@link #multiplier}. */
	@Column(name = "Multiplier", precision = 9, scale = 6)
	public BigDecimal getMultiplier() {
		return multiplier;
	}
	/** See {@link #multiplier}. */
	public void setMultiplier(BigDecimal multiplier) {
		this.multiplier = multiplier;
	}

	/** See {@link #extRate}. */
	@Column(name = "Ext_Rate", precision = 12, scale = 6)
	public BigDecimal getExtRate() {
		return extRate;
	}
	/** See {@link #extRate}. */
	public void setExtRate(BigDecimal extRate) {
		this.extRate = extRate;
	}

	@JsonIgnore
	@Transient
	/** A "dummy" method which always returns null, allowing PayBreakdown
	 * and PayBreakdownDaily instances to be treated identically for
	 * display purposes. The PayBreakdownDaily subclass will override
	 * this to return the actual date for those instances.
	 * @return null.
	 */
	public Date getDate() {
		return null;
	}

	/**
	 * @return The value of 'total', rounded to 2 decimal places.
	 */
	@JsonIgnore
	@Transient
	public BigDecimal getTotal2() {
		return NumberUtils.scaleTo2Places(getTotal());
	}

	@JsonIgnore
	@Transient
	public boolean isEmpty() {
		return StringUtils.isEmpty(getCategory()) &&
				NumberUtils.isEmpty(getQuantity()) &&
				NumberUtils.isEmpty(getMultiplier()) &&
				NumberUtils.isEmpty(getRate()) &&
				StringUtils.isEmpty(getAccountMajor()) &&
				StringUtils.isEmpty(getAccountDtl()) &&
				StringUtils.isEmpty(getAccountSet()) &&
				StringUtils.isEmpty(getAccountFree()) &&
				StringUtils.isEmpty(getAccountFree2());
	}

	/**
	 * Import data via an Importer into this instance.
	 *
	 * @param imp The Importer containing the data to be loaded.
	 * @throws EOFException
	 * @throws IOException
	 */
	public void imports(Importer imp) throws EOFException, IOException {
//		setId(imp.getBigDecimal());
//		setWeeklyId(imp.getBigDecimal());
//		setLineNumber(imp.getBigDecimal());
//		setProdEpisode(imp.getString());
		setAccountMajor(imp.getString());
		setAccountDtl(imp.getString());
		setAccountSet(imp.getString());
		setAccountFree(imp.getString());
		setAccountFree2(imp.getString());
		setCategory(imp.getString());
		setQuantity(imp.getBigDecimal());
		setMultiplier(imp.getBigDecimal());
		setRate(imp.getBigDecimal());
		setTotal(imp.getBigDecimal());

	}

	/**
	 * Export data in this instance via an Exporter. This is currently used to
	 * turn this object into a flat record. The timecard data can then be loaded
	 * into other products, such as Crew Cards.
	 *
	 * @param ex The Exporter to which each field should be passed.
	 */
	@Override
	public void exportCrewcards(Exporter ex) {
//		ex.append(getId());
//		ex.append(getWeeklyId());
//		ex.append(getLineNumber());
//		ex.append(getProdEpisode());
		ex.append(getAccountMajor());
		if (getJobNumber() != null) { // AICP/Commercial production
			ex.append(getJobNumber());
		}
		else {
			ex.append(getAccountDtl());
		}
		ex.append(getAccountSet());
		ex.append(getAccountFree());
		ex.append(getAccountFree2());
		ex.append(getCategory());
		ex.append(getQuantity());
		ex.append(getMultiplier(), 4);
		ex.append(getRate(), 4);
		ex.append(getTotal());
	}

	/**
	 * Export data in this instance via an Exporter. This is currently used to
	 * turn this object into a flat record. The timecard data can then be
	 * transmitted to other services capable of importing it.
	 *
	 * @param ex The Exporter to which each field should be passed.
	 * @param pCast True if this is for a Cast-department timecard; this
	 *            affects the mapping (translation) of some PayCategory values.
	 * @param pModelRelease True if this is for a model-release-based timecard. LS-4664
	 */
	public void exportTabbed(Exporter ex, boolean pCast, boolean pModelRelease) {
		setIsCast(pCast);
		setIsModelRelease(pModelRelease);  // LS-4664
//		ex.append(getId());
//		ex.append(getWeeklyId());
//		ex.append(getLineNumber());
		ex.append(getAccountLoc());
		ex.append(getAccountMajor());
		if (getJobNumber() != null) { // AICP/Commercial production
			ex.append(getJobNumber());
		}
		else {
			ex.append(getAccountDtl());
		}
		ex.append(getAccountSub());
		ex.append(getAccountSet());
		ex.append(getAccountFree());
		ex.append(getAccountFree2());

		exportCategoryEnum(ex, getMultiplier()); //LS-2011 Change export pay codes for Travel Day Type - added getMultiplier() as argument

		PayCategory type = getCategoryType();
		if (type == PayCategory.SAL_ADVANCE_NONTAX ||
				type == PayCategory.SAL_ADVANCE_TAX ||
				type == PayCategory.LODGING_ADVANCE ||
				type == PayCategory.PER_DIEM_ADVANCE) {
			// LS-3961 "Advance" pay categories should always export as positive
			ex.append(getQuantity() == null ? null : getQuantity().abs());
			ex.append(getMultiplier() == null ? null : getMultiplier().abs(), 4);
			ex.append(getRate() == null ? null : getRate().abs(), 4);
			ex.append(getTotal() == null ? null : getTotal().abs());
		}
		else {
			ex.append(getQuantity());
			ex.append(getMultiplier(), 4);
			ex.append(getRate(), 4);
			ex.append(getTotal());
		}
	}

}