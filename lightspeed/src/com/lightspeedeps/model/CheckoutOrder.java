package com.lightspeedeps.model;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import javax.persistence.Table;

/**
 * CheckoutOrder entity.
 */
@Entity
@Table(name = "checkout_order")
public class CheckoutOrder extends PersistentObject<CheckoutOrder> implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	// Fields

	/** The timestamp when the transaction was added to the database. */
	private Date date;

	/** The unique order number or transaction id assigned by
	 * Authorize.net or Google Checkout. */
	private String transactionId;

	/** Authorize.net transaction type */
	private String transactionType;

	/** Lightspeed-generated invoice number  */
	private String invoiceNumber;

	/** User's (Lightspeed) account number  */
	private String accountNumber;

	/** Production.prod_id field of associated production. */
	private String productionId;

	/** SKU of associated Product. */
	private String sku;

	/** Amount of transaction. */
	private BigDecimal amount;

	/** The OrderSummary XML object supplied by Google Checkout, or
	 * the stream of request parameters from Authorize.Net Post. */
	private String orderSummary;

	// Constructors

	/** default constructor */
	public CheckoutOrder() {
	}

	/** minimal constructor */
	public CheckoutOrder(String orderNumber) {
		date = new Date();
		transactionId = orderNumber;
	}

	public CheckoutOrder(String orderNumber, String orderSummary) {
		date = new Date();
		transactionId = orderNumber;
		this.orderSummary = orderSummary;
	}

	// Property accessors

	/** See {@link #date}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Date", nullable = false)
	public Date getDate() {
		return date;
	}
	/** See {@link #date}. */
	public void setDate(Date date) {
		this.date = date;
	}

	/** See {@link #transactionId}. */
	@Column(name = "Transaction_Id", nullable = false, length = 30)
	public String getTransactionId() {
		return transactionId;
	}
	public void setTransactionId(String orderNumber) {
		transactionId = orderNumber;
	}

	/** See {@link #transactionType}. */
	@Column(name = "Transaction_Type", length = 30)
	public String getTransactionType() {
		return transactionType;
	}
	/** See {@link #transactionType}. */
	public void setTransactionType(String transactionType) {
		this.transactionType = transactionType;
	}

	/** See {@link #invoiceNumber}. */
	@Column(name = "Invoice_Number", length = 30)
	public String getInvoiceNumber() {
		return invoiceNumber;
	}
	/** See {@link #invoiceNumber}. */
	public void setInvoiceNumber(String invoiceNumber) {
		this.invoiceNumber = invoiceNumber;
	}

	/** See {@link #accountNumber}. */
	@Column(name = "Account_Number", length = 30)
	public String getAccountNumber() {
		return accountNumber;
	}
	/** See {@link #accountNumber}. */
	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	/** See {@link #productionId}. */
	@Column(name = "Production_Id", length = 10)
	public String getProductionId() {
		return productionId;
	}
	/** See {@link #productionId}. */
	public void setProductionId(String productionId) {
		this.productionId = productionId;
	}

	/** See {@link #sku}. */
	@Column(name = "Sku", length = 30)
	public String getSku() {
		return sku;
	}
	/** See {@link #sku}. */
	public void setSku(String sku) {
		this.sku = sku;
	}

	/** See {@link #amount}. */
	@Column(name = "Amount", precision = 8, scale = 2)
	public BigDecimal getAmount() {
		return amount;
	}
	/** See {@link #amount}. */
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	@Column(name = "Order_Summary")
	public String getOrderSummary() {
		return orderSummary;
	}
	public void setOrderSummary(String orderSummary) {
		this.orderSummary = orderSummary;
	}

}