package com.lightspeedeps.model;

import javax.persistence.*;

/**
 * UdaProjectDetail entity. Used for Canada Uda project specific fields to
 * auto-fill the contract and other forms.
 */
@Entity
@Table(name = "uda_project_detail")
public class UdaProjectDetail extends PersistentObject<UdaProjectDetail> {

	/**
	 *
	 */
	private static final long serialVersionUID = -3648420819649756696L;

	/** Name of the Producer */
	private String producerName;
	/** Address of the Producer */
	private Address producerAddress;
	/** Phone number of the Producer */
	private String producerPhone;
	/** Email of the Producer */
	private String producerEmail;
	/** Name of the responsible */
	private String responsibleName;
	/** Production Number LS-2615 */
	private String prodNumber;
	/** Producer's UDA member number LS-2615 */
	private String producerUDA;
	/** Name of the Advertiser for whom this commercial is being made for. */
	private String advertiserName;
	/** Title for commercial. */
	private String commercialTitle;
	/** Description for commercial. */
	private String commercialDescription;
	/** Commercial Version. */
	private String commercialVersion;
	/** Name of the product */
	private String productName;

	// Constructors

	/** default constructor */
	public UdaProjectDetail() {
		producerAddress = new Address(true);
	}

	/** See {@link #producerName}. */
	@Column(name = "Producer_Name", length = 150)
	public String getProducerName() {
		return producerName;
	}

	/** See {@link #producerName}. */
	public void setProducerName(String producerName) {
		this.producerName = producerName;
	}

	/** See {@link #producerAddressId}. */
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Producer_Address_Id")
	public Address getProducerAddress() {
		return producerAddress;
	}

	/** See {@link #producerAddress}. */
	public void setProducerAddress(Address producerAddress) {
		this.producerAddress = producerAddress;
	}

	/** See {@link #producerPhone}. */
	@Column(name = "Producer_Phone", length = 20)
	public String getProducerPhone() {
		return producerPhone;
	}

	/** See {@link #producerPhone}. */
	public void setProducerPhone(String producerPhone) {
		this.producerPhone = producerPhone;
	}

	/** See {@link #producerEmail}. */
	@Column(name = "Producer_Email", length = 30)
	public String getProducerEmail() {
		return producerEmail;
	}

	/** See {@link #producerEmail}. */
	public void setProducerEmail(String producerEmail) {
		this.producerEmail = producerEmail;
	}

	/** See {@link #responsibleName}. */
	@Column(name = "Responsible_Name", length = 150)
	public String getResponsibleName() {
		return responsibleName;
	}

	/** See {@link #responsibleName}. */
	public void setResponsibleName(String responsibleName) {
		this.responsibleName = responsibleName;
	}

	/** See {@link #advertiserName}. */
	@Column(name = "Advertiser_Name", length = 150)
	public String getAdvertiserName() {
		return advertiserName;
	}

	/** See {@link #advertiserName}. */
	public void setAdvertiserName(String advertiserName) {
		this.advertiserName = advertiserName;
	}

	/** See {@link #commercialTitle}. */
	@Column(name = "Commercial_Title", length = 150)
	public String getCommercialTitle() {
		return commercialTitle;
	}

	/** See {@link #commercialTitle}. */
	public void setCommercialTitle(String commercialTitle) {
		this.commercialTitle = commercialTitle;
	}

	/** See {@link #commercialDescription}. */
	@Column(name = "Commercial_Description", length = 150)
	public String getCommercialDescription() {
		return commercialDescription;
	}

	/** See {@link #commercialDescription}. */
	public void setCommercialDescription(String commercialDescription) {
		this.commercialDescription = commercialDescription;
	}

	/** See {@link #commercialVersion}. */
	@Column(name = "Commercial_Version", length = 150)
	public String getCommercialVersion() {
		return commercialVersion;
	}

	/** See {@link #commercialVersion}. */
	public void setCommercialVersion(String commercialVersion) {
		this.commercialVersion = commercialVersion;
	}

	/** See {@link #productName}. */
	@Column(name = "Product_Name", length = 150)
	public String getProductName() {
		return productName;
	}

	/** See {@link #productName}. */
	public void setProductName(String productName) {
		this.productName = productName;
	}

	/** See {@link #prodNumber}. */
	@Column(name = "Prod_Number", nullable = true, length=150)
	public String getProdNumber() {
		return prodNumber;
	}
	public void setProdNumber(String prodNumber) {
		this.prodNumber = prodNumber;
	}

	/** See {@link #producerUDA}. */
	@Column(name = "Producer_UDA", nullable = true, length = 155)
	public String getProducerUDA() {
		return producerUDA;
	}
	public void setProducerUDA(String producerUDA) {
		this.producerUDA = producerUDA;
	}

	/**
	 * @see java.lang.Object#clone()
	 */
	@Override
	public UdaProjectDetail clone() {
		UdaProjectDetail upd;
		try {
			upd = (UdaProjectDetail)super.clone();
			upd.id = null;
			// any complex object references should be null:
			upd.producerAddress = null;
		}
		catch (CloneNotSupportedException e) {
			return null;
		}
		return upd;
	}

	/**
	 * @return A copy of this object, including separate copies of all the included
	 *         data objects, such as Address. (This is significantly different than
	 *         the clone() method, which copies only the primitive data items, and
	 *         all referenced objects are null in the returned copy.)
	 */
	public UdaProjectDetail deepCopy() {
		UdaProjectDetail upd = clone();
		if (upd == null) {
			upd = new UdaProjectDetail();
		}
		if (producerAddress != null) {
			upd.setProducerAddress(producerAddress.clone());
		}

		return upd;
	}

	/**
	 * Method to clear all the fields from project details page for canada
	 *
	 * @return
	 */
	public void clearFields() {
		setProducerName(null);
		setProducerAddress(new Address(true));
		setProducerPhone(null);
		setProducerEmail(null);
		setResponsibleName(null);
		setProdNumber(null);
		setAdvertiserName(null);
		setCommercialTitle(null);
		setCommercialDescription(null);
		setCommercialVersion(null);
		setProductName(null);
		setProducerUDA(null);
	}
}
