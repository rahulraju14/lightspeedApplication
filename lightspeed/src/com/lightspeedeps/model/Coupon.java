/* File: Coupon.java */
package com.lightspeedeps.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import javax.persistence.Table;

/**
 * Coupon entity. This describes a unique coupon, including the code
 * that is used to redeem the coupon, how many times this particular
 * coupon has been used, and the last person and production it was
 * used by.
 */
@Entity
@Table(name = "coupon")
public class Coupon extends PersistentObject<Coupon> implements java.io.Serializable {

	private static final long serialVersionUID = -7515459638924429860L;

	// Fields

	/** The coupon code the user enters to match and get the discount. */
	private String code;

	/** The type of coupon -- this object describes all the attributes common
	 * to a set of coupons generated at one time.  */
	private CouponType couponType;

	/** The number of times this coupon code has been used (redeemed). */
	private Short timesUsed;

	/** The date and time this coupon was last used (redeemed). */
	private Date redeemed;

	/** The User.account field of the last person who redeemed this coupon. */
	private String redeemerAcct;

	/** The Production.prod_id of the last production that was purchased or upgraded
	 * using this coupon. */
	private String prodId;

	// Constructors

	/** default constructor */
	public Coupon() {
	}

	/** our usual constructor */
	public Coupon(String code, CouponType type) {
		this.code = code;
		couponType = type;
		timesUsed = 0;
	}

	// Property accessors

	@Column(name = "Code", unique = true, nullable = false, length = 30)
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Coupon_Type_Id", nullable = false)
	public CouponType getCouponType() {
		return couponType;
	}
	public void setCouponType(CouponType type) {
		couponType = type;
	}

	@Column(name = "Times_Used", nullable = false)
	public Short getTimesUsed() {
		return timesUsed;
	}
	public void setTimesUsed(Short numberUsed) {
		timesUsed = numberUsed;
	}

	@Column(name = "Redeemed", length = 19)
	public Date getRedeemed() {
		return redeemed;
	}
	public void setRedeemed(Date redeemed) {
		this.redeemed = redeemed;
	}

	@Column(name = "Redeemer_Acct", length = 20)
	public String getRedeemerAcct() {
		return redeemerAcct;
	}
	public void setRedeemerAcct(String redeemerAcct) {
		this.redeemerAcct = redeemerAcct;
	}

	@Column(name = "Prod_Id", length = 10)
	public String getProdId() {
		return prodId;
	}
	public void setProdId(String prodId) {
		this.prodId = prodId;
	}

	/**
	 * @return the number of times that this coupon has remaining to be used.
	 */
	@Transient
	public int getUsesLeft() {
		return getCouponType().getMaximumUses() - getTimesUsed();
	}

}
