package com.lightspeedeps.model;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.jasypt.hibernate3.type.EncryptedStringType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lightspeedeps.port.Exporter;
import com.lightspeedeps.type.TaxWageAllocationFrequencyType;
import com.lightspeedeps.util.common.StringUtils;

/**
 * TaxWageAllocationForm entity. Contains form fields and row data
 * from the Tax Wage Allocation form.
 */

@TypeDefs(
		{
		@TypeDef(
			// TypeDef for encrypted strings - this TypeDef is shared by all models that use encrypted fields
			name="encryptedString",
			typeClass=EncryptedStringType.class,
			parameters= {
				@Parameter(name="encryptorRegisteredName", value="hibernateStringEncryptor")
				// Registered name is defined in applicationContextPart1.xml
			}
		)
		}
	)

@Entity
@Table(name = "tax_wage_allocation_form")
public class TaxWageAllocationForm extends Form<TaxWageAllocationForm> implements Comparable<TaxWageAllocationForm>, Cloneable {
//	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(TaxWageAllocationForm.class);
	private static final long serialVersionUID = - 2280020128121481695L;

	// Fields
	public static final Byte ALLOC_FORM_2017 = 1;
	public static final Byte DEFAULT_ALLOCATION_FORM_VERSION = ALLOC_FORM_2017;

	public static final String SORTKEY_NAME = "name";

	/** The Production/Client associated with this allocation form. */
	private Production production;
	/** Contact record for the employee. */
	private Contact contact;
	/** Name of the employee */
//	private String employeeName;
	/** Resident city of the employee. */
	private String residentCity;
	/** Resident state code of the employee. */
	private String residentState;
	/** Indicates that this form is replacing an existing allocation form */
	private Boolean reallocationWages;
	/** Name of the Client Company */
	private String clientName;
	/** The frequency in which this form occurs.
	 * Annually, Quarterly, Semi-Monthly, Biweekly or Weekly.
	 * The vast majority of the forms are annual. The remainder are mostly quarterly.
	 */
	private TaxWageAllocationFrequencyType frequency;
	/** Total wages for this form */
	private BigDecimal totalWages;
	/** The cash advance amount taken that can be accounted for on the form. */
	private BigDecimal cashAdvanceDeduction;
	/** The retirement deduction amount taken that can be accounted for on the form. */
	private BigDecimal retirementDeduction;
	/** The fringe amount taken that can be accounted for on the form. */
	private BigDecimal fringe;
	/** The 2% Shareholder insurance amount taken that can be accounted for on the form. */
	private BigDecimal shareholderInsurance;
	/** Whether this is a Net Zero check */
	private Boolean netZeroCheck;
	/** Any Special Instructions to be applied to this form. */
	private String specialInstructions;
	/** Tax year that the form was generated for. */
	private Integer taxYear;
	/** Date the form was last updated */
	private Date updated;
	/** LS Account number of person updated the form. */
	private String updatedBy;
	/** Date form was created */
	private Date created;
	/** LS account number of person creating the form */
	private String createdBy;
	/** Whether this form has been transmitted/exported */
	private Boolean transmitted;
	/** Revision date is the date that the form was transmitted */
	private Date revisionDate;
	/** Name given to this revision. */
	private String revisionName;
	/** The date and time that the transmit was performed */
	private Date timeSent;
	/** Social Security number associated with this form */
	private String socialSecurity;
	/** Amount for FIT Calculation. Must be numeric value or CALCULATE or ZERO */
	private String calculateFit;
	/** Keep track of the Calculate FIT checkbox */
	private Boolean calculateFitChecked;
	/** List containing the individual rows of the report */
	private List<TaxWageAllocationRow> allocationFormRows;

	/** The less-restricted view of an SSN, typically "###-##-nnnn". */
	@Transient
	private String viewSSN;

	// Constructors

	/** default constructor */
	public TaxWageAllocationForm() {
		super();
		setVersion(DEFAULT_ALLOCATION_FORM_VERSION);
		allocationFormRows = new ArrayList<>(0);
	}

	/** See {@link #production}. */
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Prod_Id", nullable = false)
	public Production getProduction() {
		return this.production;
	}

	/** See {@link #production}. */
	public void setProduction(Production production) {
		this.production = production;
	}

	/** See {@link #contact}. */
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Contact_Id", nullable = false)
	public Contact getContact() {
		return contact;
	}

	/** See {@link #contact}. */
	public void setContact(Contact contact) {
		this.contact = contact;
	}

	/** See {@link #socialSecurity}. */
	@Type(type="encryptedString") // See TypeDef above, prior to 'class' statement
	@Column(name = "Social_Security_Num", length = 1000)
	public String getSocialSecurity() {
		return socialSecurity;
	}

	/** See {@link #socialSecurity}. */
	public void setSocialSecurity(String socialSecurity) {
		this.socialSecurity = socialSecurity;
	}

	/** See {@link #residentCity}. */
	@Column(name = "Resident_City", length = 100)
	public String getResidentCity() {
		return this.residentCity;
	}

	/** See {@link #residentCity}. */
	public void setResidentCity(String residentCity) {
		this.residentCity = residentCity;
	}

	/** See {@link #residentState}. */
	@Column(name = "Resident_State", length = 2)
	public String getResidentState() {
		return this.residentState;
	}

	/** See {@link #residentState}. */
	public void setResidentState(String residentState) {
		this.residentState = residentState;
	}

	/** See {@link #reallocationWages}. */
	@Column(name = "Reallocation_Wages")
	public Boolean getReallocationWages() {
		return this.reallocationWages;
	}

	/** See {@link #reallocationWages}. */
	public void setReallocationWages(Boolean reallocationWages) {
		this.reallocationWages = reallocationWages;
	}

	/** See {@link #frequency}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Frequency", length = 30, nullable = false)
	public TaxWageAllocationFrequencyType getFrequency() {
		return this.frequency;
	}

	/** See {@link #frequency}. */
	public void setFrequency(TaxWageAllocationFrequencyType frequency) {
		this.frequency = frequency;
	}

	/** See {@link #totalWages}. */
	@Column(name = "Total_Wages", precision = 13)
	public BigDecimal getTotalWages() {
		return totalWages;
	}

	/** See {@link #totalWages}. */
	public void setTotalWages(BigDecimal totalWages) {
		this.totalWages = totalWages;
	}

	/** See {@link #cashAdvanceDeduction}. */
	@Column(name = "Cash_Advance_Deduction", precision = 13)
	public BigDecimal getCashAdvanceDeduction() {
		return this.cashAdvanceDeduction;
	}

	/** See {@link #cashAdvanceDeduction}. */
	public void setCashAdvanceDeduction(BigDecimal cashAdvanceDeduction) {
		this.cashAdvanceDeduction = cashAdvanceDeduction;
	}

	/** See {@link #retirementDeduction}. */
	@Column(name="Retirement_Deduction", precision=13)
    public BigDecimal getRetirementDeduction() {
        return this.retirementDeduction;
    }

	/** See {@link #retirementDeduction}. */
	public void setRetirementDeduction(BigDecimal retirementDeduction) {
        this.retirementDeduction = retirementDeduction;
    }

	/** See {@link #fringe}. */
	@Column(name = "Fringe", precision = 13)
	public BigDecimal getFringe() {
		return this.fringe;
	}

	/** See {@link #fringe}. */
	public void setFringe(BigDecimal fringe) {
		this.fringe = fringe;
	}

	/** See {@link #shareholderInsurance}. */
	@Column(name = "Shareholder_Insurance", precision = 13)
	public BigDecimal getShareholderInsurance() {
		return this.shareholderInsurance;
	}

	/** See {@link #shareholderInsurance}. */
	public void setShareholderInsurance(BigDecimal shareholderInsurance) {
		this.shareholderInsurance = shareholderInsurance;
	}

	/** See {@link #netZeroCheck}. */
	@Column(name = "Net_Zero_Check")
	public Boolean getNetZeroCheck() {
		return this.netZeroCheck;
	}

	/** See {@link #netZeroCheck}. */
	public void setNetZeroCheck(Boolean netZeroCheck) {
		this.netZeroCheck = netZeroCheck;
	}

	/** See {@link #specialInstructions}. */
	@Column(name = "Special_Instructions", length = 1000)
	public String getSpecialInstructions() {
		return this.specialInstructions;
	}

	/** See {@link #specialInstructions}. */
	public void setSpecialInstructions(String specialInstructions) {
		this.specialInstructions = specialInstructions;
	}

	/** See {@link #taxYear}. */
	@Column(name = "Tax_Year")
	public Integer getTaxYear() {
		return taxYear;
	}

	/** See {@link #taxYear}. */
	public void setTaxYear(Integer taxYear) {
		this.taxYear = taxYear;
	}

	/** See {@link #calculateFit}. */
	@Column(name = "Calc_FIT")
	public String getCalculateFit() {
		if(NumberUtils.isNumber(calculateFit)) {
			// If is numeric value, format it
			double calcFit = Double.parseDouble(calculateFit);
			NumberFormat numFormat = NumberFormat.getNumberInstance();
			numFormat.setGroupingUsed(true);
			numFormat.setMaximumFractionDigits(2);
			numFormat.setMinimumFractionDigits(2);
			calculateFit = numFormat.format(calcFit);
		}

		return calculateFit;
	}

	/** See {@link #calculateFit}. */
	public void setCalculateFit(String calculateFit) {
		this.calculateFit = calculateFit;
	}

	/** See {@link #calculateFitChecked}. */
	@Column(name = "Calc_FIT_Checked")
	public Boolean getCalculateFitChecked() {
		return calculateFitChecked;
	}

	/** See {@link #calculateFitChecked}. */
	public void setCalculateFitChecked(Boolean calculateFitChecked) {
		this.calculateFitChecked = calculateFitChecked;
	}

	/** See {@link #updated}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Updated", length = 0)
	public Date getUpdated() {
		return updated;
	}

	/** See {@link #updated}. */
	public void setUpdated(Date updated) {
		this.updated = updated;
	}

	/** See {@link #updatedBy}. */
	@Column(name = "Updated_By", length = 20)
	public String getUpdatedBy() {
		return updatedBy;
	}

	/** See {@link #updatedBy}. */
	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	/** See {@link #created}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Created", length = 0)
	public Date getCreated() {
		return created;
	}

	/** See {@link #created}. */
	public void setCreated(Date created) {
		this.created = created;
	}

	/** See {@link #createdBy}. */
	@Column(name = "Created_By", length = 20)
	public String getCreatedBy() {
		return createdBy;
	}

	/** See {@link #createdBy}. */
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	/** See {@link #transmitted}. */
	@Column(name = "Transmitted")
	public Boolean getTransmitted() {
		return transmitted;
	}

	/** See {@link #transmitted}. */
	public void setTransmitted(Boolean transmitted) {
		this.transmitted = transmitted;
	}

	/** See {@link #timeSent}. */
	@JsonIgnore
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Time_Sent", length = 0)
	public Date getTimeSent() {
		return timeSent;
	}

	/** See {@link #timeSent}. */
	public void setTimeSent(Date timeSent) {
		this.timeSent = timeSent;
	}

	/** See {@link #allocationFormRows}. */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY,  orphanRemoval = true, mappedBy = "form")
	public List<TaxWageAllocationRow> getAllocationFormRows() {
		return allocationFormRows;
	}

	/** See {@link #allocationFormRows}. */
	public void setAllocationFormRows(List<TaxWageAllocationRow> allocationFormRows) {
		this.allocationFormRows = allocationFormRows;
	}

	/** See {@link #clientName}. */
	@Column(name = "Client_Name")
	public String getClientName() {
		return clientName;
	}

	/** See {@link #clientName}. */
	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	/** See {@link #revisionDate}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Revision_Date")
	public Date getRevisionDate() {
		return revisionDate;
	}

	/** See {@link #revisionDate}. */
	public void setRevisionDate(Date revisionDate) {
		this.revisionDate = revisionDate;
	}

	/** See {@link #revisionName}. */
	@Column(name = "Revision_Name", length = 75)
	public String getRevisionName() {
		return revisionName;
	}

	/** See {@link #revisionName}. */
	public void setRevisionName(String revisionName) {
		this.revisionName = revisionName;
	}

	/** See {@link #viewSSN}. */
	@JsonIgnore
	@Transient
	public String getViewSSN() {
		String str = getSocialSecurity();
		if (str == null) {
			viewSSN = null;
		}
		else if (viewSSN == null) {
			if (str.length() >= 4) {
				viewSSN = "###-##-" + str.substring(str.length()-4);
			}
			else {
				viewSSN = str;
			}
		}
		return viewSSN;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
		result = prime * result + ((getContact().getDisplayName() == null) ? 0 : getContact().getDisplayName().hashCode());
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		TaxWageAllocationForm other;
		try {
			other = (TaxWageAllocationForm)obj;
		}
		catch (Exception e) {
			return false;
		}
		if ( getId() != null && getId().equals(other.getId()) ) {
			return true;
		}
		return (compareTo(other) == 0);
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(TaxWageAllocationForm other) {
		int ret = 1;

		if (other != null) {
			if(getContact().getDisplayName() == null) {
				ret = -1;
			}
			else if (other.getContact().getUser().getLastNameFirstName() == null) {
				ret = 1;
			}
			else {
				ret = getContact().getUser().getLastNameFirstName().compareTo(other.getContact().getUser().getLastNameFirstName());
			}
		}
		return ret;
	}

	public int compareTo(TaxWageAllocationForm other, String sortField, boolean ascending) {
		int ret = compareTo(other, sortField);
		return (ascending ? ret : (0-ret));
	}

	public int compareTo(TaxWageAllocationForm other, String sortField) {
		int ret = 0;
		if (sortField == null) {
			// sort by title (later)
		}
		else if (sortField.equals(SORTKEY_NAME) ) {
			ret = StringUtils.compareIgnoreCase(getContact().getUser().getLastNameFirstName(), other.getContact().getUser().getLastNameFirstName());
		}

		if (ret == 0) { // unsorted, or specified column compared equal
			ret = compareTo(other);
		}
		return ret;
	}

	/**
	 * This is different than the clone method since we are populating
	 * this new instance with values from the existing instance.
	 * @return
	 */
	public TaxWageAllocationForm deepCopy() {
		TaxWageAllocationForm form = new TaxWageAllocationForm();

		form.allocationFormRows = new ArrayList<>();
		for(TaxWageAllocationRow row : getAllocationFormRows()) {
			TaxWageAllocationRow newRow = row.deepCopy();
			newRow.setForm(form);
			form.allocationFormRows.add(newRow);
		}

		form.cashAdvanceDeduction = getCashAdvanceDeduction();
		form.clientName = getClientName();
		form.contact = getContact();
		form.created = getCreated();
		form.createdBy = getCreatedBy();
		form.frequency = getFrequency();
		form.fringe = getFringe();
		form.netZeroCheck = getNetZeroCheck();
		form.production = getProduction();
		form.reallocationWages = getReallocationWages();
		form.residentCity = getResidentCity();
		form.residentState = getResidentState();
		form.retirementDeduction = getRetirementDeduction();
		form.shareholderInsurance = getShareholderInsurance();
		form.taxYear = getTaxYear();
		form.totalWages = getTotalWages();
		form.updated = getUpdated();
		form.updatedBy = getUpdatedBy();
		form.transmitted = getTransmitted();
		form.socialSecurity = getSocialSecurity();
		form.calculateFit = getCalculateFit();
		form.calculateFitChecked = getCalculateFitChecked();

		return form;
	}

	@Override
	public TaxWageAllocationForm clone() {
		TaxWageAllocationForm form;
		try {
			form = (TaxWageAllocationForm)super.clone();

			form.allocationFormRows = new ArrayList<>();
			for(TaxWageAllocationRow row : allocationFormRows) {
				form.allocationFormRows.add(row.clone());
			}
			form.cashAdvanceDeduction = null;
			form.clientName = null;
			form.contact = null;
			form.created = null;
			form.createdBy = null;
//			form.employeeName = null;
			form.frequency = null;
			form.fringe = null;
			form.netZeroCheck = null;
			form.production = null;
			form.reallocationWages = null;
			form.residentCity = null;
			form.residentState = null;
			form.retirementDeduction = null;
			form.shareholderInsurance = null;
			form.taxYear = null;
			form.totalWages = null;
//			form.user = null;
			form.updated = null;
			form.updatedBy = null;
			form.transmitted = false;
		}
		catch (CloneNotSupportedException e) {
			log.error("TaxWageAllocationForm clone error: ", e);

			return null;
		}

		return form;
	}

	/**
	 * Export the fields in this TaxWageAllocationForm using the supplied Exporter.
	 * @param ex
	 */
	@Override
	public void exportFlat(Exporter ex) {
		ex.append("TW");
		// Version = 1
		ex.append("1");
		ex.append(getId());
		ex.append(getProduction().getId());
		ex.append(getContact().getDisplayName());
		ex.append(getResidentCity());
		ex.append(getResidentState());
		ex.append(getSocialSecurity());
		ex.append(getClientName());
		ex.append(getReallocationWages());
		ex.append(getFrequency().name());
		ex.append(getTotalWages());
		ex.append(getCashAdvanceDeduction());
		ex.append(getFringe());
		ex.append(getShareholderInsurance());
		ex.append(getNetZeroCheck());
		ex.append(getSpecialInstructions());
		ex.append(getTaxYear());
		ex.append(getCalculateFit());
		ex.appendDateTime(getUpdated());
		ex.append(getUpdatedBy());
		ex.appendDateTime(getCreated());
		ex.append(getCreatedBy());
		ex.append(getRevisionName());
		ex.append(getRevisionDate());
		ex.appendDateTime(getTimeSent());
//		Format timecardDateFormat = new SimpleDateFormat(Constants.WEEK_END_DATE_FILE_FORMAT);
//		String date = timecardDateFormat.format(getTimeSent());
//		ex.append(date);

		for(TaxWageAllocationRow row : getAllocationFormRows()) {
			row.exportFlat(ex);
		}

		ex.append("");
	}

}
