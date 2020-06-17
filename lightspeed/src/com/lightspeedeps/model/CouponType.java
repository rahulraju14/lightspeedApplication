/* File: CouponType.java */
package com.lightspeedeps.model;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import com.lightspeedeps.type.DiscountType;

/**
 * CouponType entity. This describes all the common attributes of a set of
 * coupons that are generated at one time.
 */
@Entity
@Table(name = "coupon_type")
public class CouponType extends PersistentObject<CouponType> implements java.io.Serializable {

	private static final long serialVersionUID = -7515459638924429860L;

	// Fields

	/** description, probably for LS use only */
	private String description;

	/** message displayed after a user redeems a coupon of this type. */
	private String message;

	/** type of discount, enum DiscountType */
	private DiscountType discountType;

	/** amount or percent of discount (meaning varies with type) */
	private BigDecimal amount;

	/** The value to set in the purchased production's maximum number
	 * of users field. */
	private Short numberUsers;

	/** The value to set in the purchased production's smsEnabled field. */
	private boolean smsEnabled;

	/** reg-exp pattern must match the SKU user is buying */
	private String skuPattern;

	/** number of times this coupon code has been used */
	private Short maximumUses;

	/** Date and time this coupon was created (by LS) */
	private Date created;

	/** Date and time this coupon expires. If null, never. */
	private Date expires;

	// Constructors

	/** default constructor */
	public CouponType() {
	}

	/** minimal constructor */
	public CouponType(DiscountType discType, BigDecimal amt, String sku, Short numUsers,
			boolean smsOk, Short maxUses, Date createDate, String desc, String msg, Date exp) {
		discountType = discType;
		amount = amt;
		numberUsers = numUsers;
		smsEnabled = smsOk;
		skuPattern = sku;
		maximumUses = maxUses;
		created = createDate;
		description = desc;
		message = msg;
		expires = exp;
	}

	// Property accessors

	@Column(name = "Description", length = 100)
	public String getDescription() {
		return this.description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	/** See {@link #message}. */
	@Column(name = "Message", length = 100)
	public String getMessage() {
		return message;
	}

	/** See {@link #message}. */
	public void setMessage(String message) {
		this.message = message;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Discount_Type", nullable = false, length = 30)
	public DiscountType getDiscountType() {
		return this.discountType;
	}
	public void setDiscountType(DiscountType discountType) {
		this.discountType = discountType;
	}

	@Column(name = "Amount", nullable = false, precision = 6)
	public BigDecimal getAmount() {
		return this.amount;
	}
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	/** See {@link #numberUsers}. */
	@Column(name = "Number_Users", nullable = false)
	public Short getNumberUsers() {
		return numberUsers;
	}
	/** See {@link #numberUsers}. */
	public void setNumberUsers(Short numberUsers) {
		this.numberUsers = numberUsers;
	}

	/** See {@link #smsEnabled}. */
	@Column(name = "Sms_Enabled", nullable = false)
	public boolean getSmsEnabled() {
		return smsEnabled;
	}
	/** See {@link #smsEnabled}. */
	public void setSmsEnabled(boolean smsEnabled) {
		this.smsEnabled = smsEnabled;
	}

	@Column(name = "Sku_Pattern", nullable = false, length = 50)
	public String getSkuPattern() {
		return this.skuPattern;
	}
	public void setSkuPattern(String skuPattern) {
		this.skuPattern = skuPattern;
	}

	@Column(name = "Maximum_Uses", nullable = false)
	public Short getMaximumUses() {
		return this.maximumUses;
	}
	public void setMaximumUses(Short numberUsed) {
		this.maximumUses = numberUsed;
	}

	@Column(name = "Created", nullable = false, length = 19)
	public Date getCreated() {
		return this.created;
	}
	public void setCreated(Date created) {
		this.created = created;
	}

	@Column(name = "Expires", length = 19)
	public Date getExpires() {
		return this.expires;
	}
	public void setExpires(Date expires) {
		this.expires = expires;
	}

}
