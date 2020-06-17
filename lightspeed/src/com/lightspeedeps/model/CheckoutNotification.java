package com.lightspeedeps.model;



import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * CheckoutNotification entity.
 * Used by our Google checkout servlet to track which Notifications have
 * been processed.
 */
@Entity
@Table(name = "checkout_notification",
		uniqueConstraints = @UniqueConstraint(columnNames = "Serial_Number"))
public class CheckoutNotification extends PersistentObject<CheckoutNotification> implements Serializable {
	private static final long serialVersionUID = 1L;

	// Fields

	/** The Google-generated unique serial number for a checkout Notification. */
	private String serialNumber;

	// Constructors

	/** default constructor */
	public CheckoutNotification() {
	}

	/** full constructor */
	public CheckoutNotification(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	// Property accessors

	@Column(name = "Serial_Number", unique = true, nullable = false, length = 32)
	public String getSerialNumber() {
		return this.serialNumber;
	}
	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

}