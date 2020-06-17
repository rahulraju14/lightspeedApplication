package com.lightspeedeps.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.common.NumberUtils;

/**
 * DprDays entity. This represents information about the number of days used or
 * planned for various aspects of a production, e.g., for each unit, for
 * rehearsal, and so forth. The data is entered and displayed through the DPR
 * page.
 */
@Entity
@Table(name = "dpr_days")
public class DprDays extends PersistentObject<DprDays> implements Cloneable {

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(DprDays.class);

	private static final long serialVersionUID = 2685592679362884579L;

	/** Zero with a scale of 2, to match scaling of user-entered values. */
	public static final BigDecimal ZERO_DAYS = new BigDecimal(0).setScale(2);

	// Fields
	private BigDecimal unit1;
	private BigDecimal unit2;
	private BigDecimal unit3;
	private BigDecimal unit4;
	private BigDecimal unit5;
	private BigDecimal unit6;
	private BigDecimal unit7;
	private BigDecimal unit8;
	private BigDecimal unit9;
	private BigDecimal unit10;
	private BigDecimal rehearse;
	private BigDecimal test;
	private BigDecimal reshoot;
	private BigDecimal holiday;
	private BigDecimal insurance;
	private BigDecimal travel;
	private String status;

	@Transient
	private BigDecimal units[];

	// Constructors

	/** default constructor */
	public DprDays() {
	}

	/** full constructor */
/*	public DprDays(BigDecimal firstUnit, BigDecimal secondUnit, BigDecimal rehearse,
			BigDecimal test, BigDecimal was, BigDecimal reshoot) {
		this.firstUnit = firstUnit;
		this.secondUnit = secondUnit;
		this.rehearse = rehearse;
		this.test = test;
		this.was = was;
		this.reshoot = reshoot;
	}
*/
	// Property accessors

	/** See {@link #units}. */
	@Transient
	public BigDecimal[] getUnits() {
		if (units == null) {
			units = new BigDecimal[11];
			units[0] = null;
			units[1] = getUnit1();
			units[2] = getUnit2();
			units[3] = getUnit3();
			units[4] = getUnit4();
			units[5] = getUnit5();
			units[6] = getUnit6();
			units[7] = getUnit7();
			units[8] = getUnit8();
			units[9] = getUnit9();
			units[10] = getUnit10();
		}
		return units;
	}
	/** See {@link #units}. */
	public void setUnits(BigDecimal[] units) {
		this.units = units;
		pushUnits();
	}

	public void pushUnits() {
		if (units != null) {
			setUnit1(units[1]);
			setUnit2(units[2]);
			setUnit3(units[3]);
			setUnit4(units[4]);
			setUnit5(units[5]);
			setUnit6(units[6]);
			setUnit7(units[7]);
			setUnit8(units[8]);
			setUnit9(units[9]);
			setUnit10(units[10]);
		}
	}

	/** See {@link #unit1}. */
	@Column(name = "Unit1", precision = 5, scale = 2)
	public BigDecimal getUnit1() {
		return unit1;
	}
	/** See {@link #unit1}. */
	public void setUnit1(BigDecimal unit1) {
		this.unit1 = unit1;
	}

	/** See {@link #unit2}. */
	@Column(name = "Unit2", precision = 5, scale = 2)
	public BigDecimal getUnit2() {
		return unit2;
	}
	/** See {@link #unit2}. */
	public void setUnit2(BigDecimal unit2) {
		this.unit2 = unit2;
	}

	/** See {@link #unit3}. */
	@Column(name = "Unit3", precision = 5, scale = 2)
	public BigDecimal getUnit3() {
		return unit3;
	}
	/** See {@link #unit3}. */
	public void setUnit3(BigDecimal unit3) {
		this.unit3 = unit3;
	}

	/** See {@link #unit4}. */
	@Column(name = "Unit4", precision = 5, scale = 2)
	public BigDecimal getUnit4() {
		return unit4;
	}
	/** See {@link #unit4}. */
	public void setUnit4(BigDecimal unit4) {
		this.unit4 = unit4;
	}

	/** See {@link #unit5}. */
	@Column(name = "Unit5", precision = 5, scale = 2)
	public BigDecimal getUnit5() {
		return unit5;
	}
	/** See {@link #unit5}. */
	public void setUnit5(BigDecimal unit5) {
		this.unit5 = unit5;
	}

	/** See {@link #unit6}. */
	@Column(name = "Unit6", precision = 5, scale = 2)
	public BigDecimal getUnit6() {
		return unit6;
	}
	/** See {@link #unit6}. */
	public void setUnit6(BigDecimal unit6) {
		this.unit6 = unit6;
	}

	/** See {@link #unit7}. */
	@Column(name = "Unit7", precision = 5, scale = 2)
	public BigDecimal getUnit7() {
		return unit7;
	}
	/** See {@link #unit7}. */
	public void setUnit7(BigDecimal unit7) {
		this.unit7 = unit7;
	}

	/** See {@link #unit8}. */
	@Column(name = "Unit8", precision = 5, scale = 2)
	public BigDecimal getUnit8() {
		return unit8;
	}
	/** See {@link #unit8}. */
	public void setUnit8(BigDecimal unit8) {
		this.unit8 = unit8;
	}

	/** See {@link #unit9}. */
	@Column(name = "Unit9", precision = 5, scale = 2)
	public BigDecimal getUnit9() {
		return unit9;
	}
	/** See {@link #unit9}. */
	public void setUnit9(BigDecimal unit9) {
		this.unit9 = unit9;
	}

	/** See {@link #unit10}. */
	@Column(name = "Unit10", precision = 5, scale = 2)
	public BigDecimal getUnit10() {
		return unit10;
	}
	/** See {@link #unit10}. */
	public void setUnit10(BigDecimal unit10) {
		this.unit10 = unit10;
	}

	@Column(name = "Rehearse", precision = 5, scale = 2)
	public BigDecimal getRehearse() {
		return rehearse;
	}
	public void setRehearse(BigDecimal rehearse) {
		this.rehearse = rehearse;
	}

	@Column(name = "Test", precision = 5, scale = 2)
	public BigDecimal getTest() {
		return test;
	}
	public void setTest(BigDecimal test) {
		this.test = test;
	}

	@Column(name = "Reshoot", precision = 5, scale = 2)
	public BigDecimal getReshoot() {
		return reshoot;
	}
	public void setReshoot(BigDecimal reshoot) {
		this.reshoot = reshoot;
	}

	/**See {@link #holiday}. */
	@Column(name = "Holiday", precision = 5, scale = 2)
	public BigDecimal getHoliday() {
		return holiday;
	}
	/**See {@link #holiday}. */
	public void setHoliday(BigDecimal holiday) {
		this.holiday = holiday;
	}

	/**See {@link #insurance}. */
	@Column(name = "Insurance", precision = 5, scale = 2)
	public BigDecimal getInsurance() {
		return insurance;
	}
	/**See {@link #insurance}. */
	public void setInsurance(BigDecimal insurance) {
		this.insurance = insurance;
	}

	/**See {@link #travel}. */
	@Column(name = "Travel", precision = 5, scale = 2)
	public BigDecimal getTravel() {
		return travel;
	}
	/**See {@link #travel}. */
	public void setTravel(BigDecimal travel) {
		this.travel = travel;
	}

	/**See {@link #status}. */
	@Column(name = "Status", length = 50)
	public String getStatus() {
		return status;
	}
	/**See {@link #status}. */
	public void setStatus(String status) {
		this.status = status;
	}

	@Transient
	public BigDecimal getTotal() {
		BigDecimal total = BigDecimal.ZERO;
		for (BigDecimal unit : getUnits()) {
			if (unit != null) {
				total = total.add(unit);
			}
		}
		total = NumberUtils.safeAdd(total, getRehearse(), getReshoot(), getTest(),
				getTravel(), getHoliday(), getInsurance());
		return total;
	}

	@Override
	public Object clone() {
		try {
			DprDays copy = (DprDays)super.clone();
			copy.id = null; // it's a transient object
			return copy;
		}
		catch (CloneNotSupportedException e) { // should not happen
			return null;
		}
	}

}