package com.lightspeedeps.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import com.lightspeedeps.type.ProductionType;

/**
 * Product entity. @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "product", uniqueConstraints = @UniqueConstraint(columnNames = "SKU"))
public class Product extends PersistentObject<Product> {
	private static final long serialVersionUID = 1L;

	// Fields

	/**
	 * Unique business code for each product. Currently all SKUs follow a simple
	 * format: <br>
	 * X-YY-nn where <br>
	 * X indicates the type of production -- either "F" for feature film or "T"
	 * for TV (episodic); <br>
	 * YY is one of the following:
	 * <ul>
	 * <li>"FR" for a Free product;
	 * <li>"ED" for an Educational product (only available to Users with Student
	 * status;
	 * <li>"IN" for Indie film;
	 * <li>"ST" for Studio (unlimited, feature film);
	 * <li>"PS" for per-season (unlimited TV episodes);
	 * <li>"MV" for TV Movie or pilot;
	 * <li>"PR" for a pre-purchased option.
	 * </ul>
	 * nn is a 2-digit number just to allow unique keys where the other options
	 * are identical. A value of "99" is special -- entries with this value are
	 * only displayed to LS Admin users; this is designed for testing new
	 * Product table entries. The "pre-purchased" Product entry also uses this:
	 * its SKU is "F-PR-99"; this prevents it from being listed on the Product
	 * selection page.
	 */
	private String sku;

	/** The text displayed on the "select" button for this product. */
	private String button;

	/** The style of button to display -- an actual CSS style class listed in
	 * our global.css file.  This transient value is determined from other factors such as
	 * the 'type' and price. */
	@Transient
	private String buttonStyle;

	/** A short description of the product. This may appear on the purchase receipt (if any). */
	private String title;

	/** A longer description of the product, displayed on the product list page. */
	private String description;

	/** The ProductionType that will be assigned to the Production created for this product. */
	private ProductionType type;

	/** The maximum number of projects (episodes) supported by this product. This value is copied
	 * into the Production upon creation. */
	private Integer maxProjects;

	/** The maximum number of users (Contact`s) supported by this product. This value is copied
	 * into the Production upon creation.  If this value is 10,000 or more, the product listing will display
	 * "Unlimited" in the "max users" column. */
	private Integer maxUsers;

	/** How long the Production will be active, in months. A value of '1' indicates this is a monthly
	 * (subscription) product. A duration of 0 (zero) indicates there is no expiration to
	 * this product. */
	private Integer duration;

	/** True iff the Production created is allowed to send SMS messages for notifications. (Student
	 * and other free products are typically NOT SMS-enabled.) */
	private Boolean smsEnabled;

	/** The purchase price; if 'duration' is 1, this is the monthly subscription price. If
	 * this value is zero, the product listing will display "Free" in the price column. If
	 * this value is negative, the product listing will display "Contact us". */
	private BigDecimal price;

	/** Used when a discount coupon is applied; referenced by the product confirmation page. */
	@Transient
	private BigDecimal originalPrice;

	// Constructors

	/** default constructor */
	public Product() {
	}

//	/** full constructor */
//	public Product(String sku, String button, String title, String description, String type,
//			Integer maxProjects, Integer maxUsers, Integer duration, Boolean smsEnabled,
//			Double price) {
//		this.sku = sku;
//		this.button = button;
//		this.title = title;
//		this.description = description;
//		this.type = type;
//		this.maxProjects = maxProjects;
//		this.maxUsers = maxUsers;
//		this.duration = duration;
//		this.smsEnabled = smsEnabled;
//		this.price = price;
//	}

	// Property accessors

	@Column(name = "SKU", unique = true, nullable = false, length = 20)
	public String getSku() {
		return this.sku;
	}
	public void setSku(String sku) {
		this.sku = sku;
	}

	@Column(name = "Button", length = 20)
	public String getButton() {
		return this.button;
	}
	public void setButton(String button) {
		this.button = button;
	}

	/** See {@link #buttonStyle}. */
	@Transient
	public String getButtonStyle() {
		if (buttonStyle == null) {
			if (getPrice().intValue() == 0 && getMaxUsers() <= 5) {
				buttonStyle = "x101"; // "free trial" style button
			}
			else if (getType() == ProductionType.FEATURE_FILM) {
				buttonStyle = "x110"; // Film style button
			}
			else if (getType() == ProductionType.TOURS) {
				buttonStyle = "x130"; // Tours style button
			}
			else if (getType() == ProductionType.TV_COMMERCIALS) {
				buttonStyle = "x117"; // Commercial style button
			}
			else {
				buttonStyle = "x115"; // TV/episodic style button
			}
		}
		return buttonStyle;
	}

	@Column(name = "Title", length = 100)
	public String getTitle() {
		return this.title;
	}
	public void setTitle(String title) {
		this.title = title;
	}

	@Column(name = "Description", length = 1000)
	public String getDescription() {
		return this.description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Type", nullable = false, length = 30)
	public ProductionType getType() {
		return this.type;
	}
	public void setType(ProductionType type) {
		this.type = type;
	}

	@Column(name = "Max_Projects", nullable = false)
	public Integer getMaxProjects() {
		return this.maxProjects;
	}
	public void setMaxProjects(Integer maxProjects) {
		this.maxProjects = maxProjects;
	}

	@Column(name = "Max_Users", nullable = false)
	public Integer getMaxUsers() {
		return this.maxUsers;
	}
	public void setMaxUsers(Integer maxUsers) {
		this.maxUsers = maxUsers;
	}

	@Column(name = "Duration", nullable = false)
	public Integer getDuration() {
		return this.duration;
	}
	public void setDuration(Integer duration) {
		this.duration = duration;
	}

	@Column(name = "Sms_Enabled", nullable = false)
	public Boolean getSmsEnabled() {
		return this.smsEnabled;
	}
	public void setSmsEnabled(Boolean smsEnabled) {
		this.smsEnabled = smsEnabled;
	}

	@Column(name = "Price", nullable = false, precision = 8, scale = 2)
	public BigDecimal getPrice() {
		return this.price;
	}
	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	@Transient
	/** See {@link #originalPrice}. */
	public BigDecimal getOriginalPrice() {
		if (originalPrice == null) {
			originalPrice = getPrice();
		}
		return originalPrice;
	}

}