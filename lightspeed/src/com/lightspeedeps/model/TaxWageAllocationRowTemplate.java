package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.Constants;

/**
 * TaxWageAllocationForm entity. @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "tax_wage_allocation_row_template")
public class TaxWageAllocationRowTemplate extends PersistentObject<TaxWageAllocationRowTemplate> {
	private static final long serialVersionUID = 4281072272551886951L;
	@SuppressWarnings("unused")
	private static Log log = LogFactory.getLog(TaxWageAllocationRowTemplate.class);
	
	// Fields    
	/** Which version of the form to get. Form can change from year to year */
	private String version;
	/** State which wage is being paid to. Can be null if going to a specific city */
	private String state;
	/** Internal tax code used by TEAM */
	private String teamTaxAreaCode;
	/** Code associated with the State. Will be null if wage goes to specific city */
	private String stateCode;
	/** Wage goes to a specific city where city taxes will apply. Is null if wages are going to the state level. */
	private String city;
	/** Whether to have TEAM do the tax calculation. The client will enter the word "CALCULATE" into this field so
	 * TEAM will perform the calculation. Only for New York City can a dollar amount be entered. This is because
	 * the client is putting in the amount to be applied to taxes so no calculation is done. There are particular
	 * Cities or States where the field is not editable and will be filled with the value "CALCULATE" or 
	 * "NO-TAX STATE".
	 */
	private String calculateTax;
	/** If calculateTax field is editable */
	private Boolean calculateTaxEditable;
	/** Any special instructions for this object */
	private String specialInstructions;
	/** Where the Special Instructions field can be edited */
	private Boolean specialInstructionsEditable;
	/** Row number of the form table */
	private Integer rowNumber;
	
	// Constructors

	/** default constructor */
	public TaxWageAllocationRowTemplate() {
	}

	/** See {@link #version}. */
	@Column(name = "Version", nullable = false, length = 10)
	public String getVersion() {
		return this.version;
	}
	
	/** See {@link #version}. */
	public void setVersion(String version) {
		this.version = version;
	}

	/** See {@link #state}. */
	@Column(name = "State", length = 45)
	public String getState() {
		return this.state;
	}

	/** See {@link #state}. */
	public void setState(String state) {
		this.state = state;
	}

	/** See {@link #teamTaxAreaCode}. */
	@Column(name = "Team_Tax_Area_Code", length = 45)
	public String getTeamTaxAreaCode() {
		return this.teamTaxAreaCode;
	}

	/** See {@link #teamTaxAreaCode}. */
	public void setTeamTaxAreaCode(String teamTaxAreaCode) {
		this.teamTaxAreaCode = teamTaxAreaCode;
	}

	/** See {@link #stateCode}. */
	@Column(name = "State_Code", length = 15)
	public String getStateCode() {
		return this.stateCode;
	}

	/** See {@link #stateCode}. */
	public void setStateCode(String stateCode) {
		this.stateCode = stateCode;
	}

	/** See {@link #city}. */
	@Column(name = "City", length = 100)
	public String getCity() {
		return this.city;
	}

	/** See {@link #city}. */
	public void setCity(String city) {
		this.city = city;
	}

	/** See {@link #calculateTax}. */
	@Column(name = "Calculate_Tax", length = 45)
	public String getCalculateTax() {
		return this.calculateTax;
	}

	/** See {@link #calculateTax}. */
	public void setCalculateTax(String calculateTax) {
		this.calculateTax = calculateTax;
	}

	/** See {@link #calculateTaxEditable}. */
	@Column(name = "Calculate_Tax_Editable", nullable = false)
	public Boolean getCalculateTaxEditable() {
		return this.calculateTaxEditable;
	}

	/** See {@link #calculateTaxEditable}. */
	public void setCalculateTaxEditable(Boolean calculateTaxEditable) {
		this.calculateTaxEditable = calculateTaxEditable;
	}

	/** See {@link #specialInstructions}. */
	@Column(name = "Special_Instructions", length = 100)
	public String getSpecialInstructions() {
		return this.specialInstructions;
	}

	/** See {@link #specialInstructions}. */
	public void setSpecialInstructions(String specialInstructions) {
		this.specialInstructions = specialInstructions;
	}

	/** See {@link #specialInstructionsEditable}. */
	@Column(name = "Special_Instructions_Editable", nullable = false)
	public Boolean getSpecialInstructionsEditable() {
		return specialInstructionsEditable;
	}

	/** See {@link #specialInstructionsEditable}. */
	public void setSpecialInstructionsEditable(Boolean specialInstructionsEditable) {
		this.specialInstructionsEditable = specialInstructionsEditable;
	}

	/** See {@link #rowNumber}. */
	@Column(name = "Row_Number")
	public Integer getRowNumber() {
		return rowNumber;
	}

	/** See {@link #rowNumber}. */
	public void setRowNumber(Integer rowNumber) {
		this.rowNumber = rowNumber;
	}
	
	/**
	 * We need to do special processing for NYC on xhtml page.
	 * @return
	 */
	@Transient
	public boolean getIsNewYorkCity() {
		if(city == null) {
			return false;
		}
		return city.equalsIgnoreCase(Constants.NEW_YORK_CITY);
	}
	
	/**
	 * We need to do special processing for Washington DC on xhtml page.
	 * @return
	 */	
	@Transient
	public boolean getIsDC() {
		return (stateCode != null && stateCode.equals(Constants.DC_STATE));
	}
}
