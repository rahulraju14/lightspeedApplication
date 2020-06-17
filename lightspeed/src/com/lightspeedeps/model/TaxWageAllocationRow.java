package com.lightspeedeps.model;

import java.math.BigDecimal;
import java.text.NumberFormat;

import javax.persistence.*;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.port.Exporter;


/**
 * TaxWageAllocationRow entity. @author MyEclipse Persistence Tools
 */
@Entity
@Table(name="tax_wage_allocation_row")
public class TaxWageAllocationRow extends PersistentObject<TaxWageAllocationRow> implements Cloneable {
//	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(TaxWageAllocationRow.class);
	private static final long serialVersionUID = 4623057779761915149L;
	
	// Fields    
	/** Version number of the form. */
	private String version;
	/** Row number of the individual row. */
	private Integer rowNumber;
	/** Team Tax Area Code - Used internally */
	private String teamTaxAreaCode;
	/** Any special instructions pertaining to this row */
	private String specialInstructions;
	/** Form containing this row */
	private TaxWageAllocationForm form;
	/** The row template containing the literals for this row */
	private TaxWageAllocationRowTemplate rowTemplate;
	/** Determines whether to calculate tax for this row.
	 * Value is either "CALCULATE" or a numeric value. If
	 * numeric value is 0 a warning message will be generated.
	 * For a value of 0 the client must submit a form explaining why
	 * the tax is 0.
	 */
	private String calculateTax;
	/** Keep track of the state of the calc tax check box.  */
	private Boolean calcFitChecked;
	/** Wages for this row */
	private BigDecimal wages;

    // Constructors

    /** default constructor */
    public TaxWageAllocationRow() {
    }
  
    // Property accessors
    
    /** See {@link #version}. */
    @Column(name="Version", nullable=false, length=15)
    public String getVersion() {
        return this.version;
    }
    
    /** See {@link #version}. */
    public void setVersion(String version) {
        this.version = version;
    }
    
    /** See {@link #rowNumber}. */
    @Column(name="Row_Number", nullable=false)
    public Integer getRowNumber() {
        return this.rowNumber;
    }
    
    /** See {@link #rowNumber}. */
    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }
    
    /** See {@link #teamTaxAreaCode}. */
    @Column(name="Team_Tax_Area_Code", nullable=false, length=45)
    public String getTeamTaxAreaCode() {
        return this.teamTaxAreaCode;
    }
    
    /** See {@link #teamTaxAreaCode}. */
    public void setTeamTaxAreaCode(String teamTaxAreaCode) {
        this.teamTaxAreaCode = teamTaxAreaCode;
    }
    
    /** See {@link #specialInstructions}. */
    @Column(name="Special_Instructions", length=1000)
    public String getSpecialInstructions() {
        return this.specialInstructions;
    }
    
    /** See {@link #specialInstructions}. */
    public void setSpecialInstructions(String specialInstructions) {
        this.specialInstructions = specialInstructions;
    }

	/** See {@link #form}. */
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "Form_Id")
	public TaxWageAllocationForm getForm() {
		return form;
	}

	/** See {@link #form}. */
	public void setForm(TaxWageAllocationForm form) {
		this.form = form;
	}

	/** See {@link #rowTemplate}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Template_Id")
    public TaxWageAllocationRowTemplate getRowTemplate() {
		return rowTemplate;
	}

	/** See {@link #rowTemplate}. */
	public void setRowTemplate(TaxWageAllocationRowTemplate rowTemplate) {
		this.rowTemplate = rowTemplate;
	}

	/** See {@link #calculateTax}. */
	@Column(name = "Calculate_Tax", length = 30)
	public String getCalculateTax() {
		if(NumberUtils.isNumber(calculateTax)) {
			// If is numeric value, format it
			double calcSit = Double.parseDouble(calculateTax);
			NumberFormat numFormat = NumberFormat.getNumberInstance();
			numFormat.setGroupingUsed(true);
			numFormat.setMaximumFractionDigits(2);
			numFormat.setMinimumFractionDigits(2);
			calculateTax = numFormat.format(calcSit);
		}		
		return calculateTax;
	}

	/** See {@link #calculateTax}. */
	public void setCalculateTax(String calculateTax) {
		this.calculateTax = calculateTax;
	}

	/** See {@link #calcFitChecked}. */
	@Column(name = "Calc_Tax_Checked", nullable = false)
	public Boolean getCalcFitChecked() {
		return calcFitChecked;
	}

	/** See {@link #calcFitChecked}. */
	public void setCalcFitChecked(Boolean calcFitChecked) {
		this.calcFitChecked = calcFitChecked;
	}

	/** See {@link #wages}. */
	@Column(name = "Wages", precision = 13)
	public BigDecimal getWages() {
		return wages;
	}

	/** See {@link #wages}. */
	public void setWages(BigDecimal wages) {
		this.wages = wages;
	}

	/**
	 * This is different than the clone method since we are populating
	 * this new instance with values from the existing instance.
	 * @return
	 */
	public TaxWageAllocationRow deepCopy() {
		TaxWageAllocationRow row = new TaxWageAllocationRow();
		
		row.setCalculateTax(getCalculateTax());
		row.setCalcFitChecked(getCalcFitChecked());
		row.setRowNumber(new Integer(getRowNumber()));
		row.setWages(getWages());
		row.setRowTemplate(getRowTemplate());
		row.setSpecialInstructions(getSpecialInstructions());
		row.setTeamTaxAreaCode(getTeamTaxAreaCode());
		row.setVersion(getVersion());		
		
		return row;
	}
	
	public TaxWageAllocationRow clone() {
		TaxWageAllocationRow row;
		
		try {
			row = (TaxWageAllocationRow)super.clone();
			
//			row.setForm(null);
			row.setCalculateTax(null);
			row.setCalcFitChecked(false);
			row.setRowNumber(null);
			row.setWages(null);
			row.setRowTemplate(null);
			row.setSpecialInstructions(null);
			row.setTeamTaxAreaCode(null);
			row.setVersion(null);
		}
		catch (CloneNotSupportedException e) {
			log.error(e);
			return null;
		}
		
		return row;
	}
		
	/**
	 * Export the fields in this TaxWageAllocationRow using the supplied Exporter.
	 * @param ex
	 */
	public void exportFlat(Exporter ex) {
		ex.append(getRowTemplate().getCity());
		ex.append(getRowTemplate().getState());
		ex.append(getRowTemplate().getStateCode());
		ex.append(getRowTemplate().getTeamTaxAreaCode());
		ex.append(getCalculateTax());
		ex.append(getSpecialInstructions());
		ex.append(getWages());
	}

}
