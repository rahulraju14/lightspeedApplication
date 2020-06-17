package com.lightspeedeps.util.payroll;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.faces.model.SelectItem;

import com.lightspeedeps.dao.TaxWageAllocationRowTemplateDAO;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.TaxWageAllocationForm;
import com.lightspeedeps.model.TaxWageAllocationRow;
import com.lightspeedeps.model.TaxWageAllocationRowTemplate;
import com.lightspeedeps.model.User;
import com.lightspeedeps.type.TaxWageAllocationFrequencyType;

/**
 * 
 */
public class TaxWageAllocationUtils {

	// Do not allow instantiation.
	private TaxWageAllocationUtils() {
		
	}
	
	public static TaxWageAllocationForm createBlankAllocationForm(String version, Production production) {
		List<TaxWageAllocationRowTemplate> rowTemplates = TaxWageAllocationUtils.getTaxWageAllocationTemplateRows(version);
		
		TaxWageAllocationForm form = null;
		
		if(production != null) {
			form = new TaxWageAllocationForm();
			Calendar todayCal = Calendar.getInstance();
			form.setContact(new Contact());
			
			form.setProduction(production);
			form.setClientName(production.getTitle());
			form.setFrequency(TaxWageAllocationFrequencyType.ANNUAL);
			form.setTaxYear(todayCal.get(Calendar.YEAR));
			form.setUpdated(todayCal.getTime());
			
			// Go through the template rows and create the form rows
			for(TaxWageAllocationRowTemplate template : rowTemplates) {
				TaxWageAllocationRow row = new TaxWageAllocationRow();
				
				row.setRowTemplate(template);
				row.setRowNumber(template.getRowNumber());
				row.setForm(form);
				row.setSpecialInstructions(template.getSpecialInstructions());
				row.setTeamTaxAreaCode(template.getTeamTaxAreaCode());
				row.setCalculateTax(template.getCalculateTax());
				form.getAllocationFormRows().add(row);
			}
		}
		return form;
	}
	
	public static TaxWageAllocationForm createNewAllocationForm(String version, Production production, Contact empContact, User clientUser) {
		List<TaxWageAllocationRowTemplate> rowTemplates = TaxWageAllocationUtils.getTaxWageAllocationTemplateRows(version);
		
		TaxWageAllocationForm form = null;
		
		if(production != null) {
			form = new TaxWageAllocationForm();
			Calendar todayCal = Calendar.getInstance();
			form.setContact(empContact);
			
			form.setProduction(production);
			form.setClientName(production.getTitle());
			form.setFrequency(TaxWageAllocationFrequencyType.ANNUAL);
			form.setTaxYear(todayCal.get(Calendar.YEAR));
			form.setUpdated(null);
			form.setUpdatedBy(null);
			form.setCreated(new Date());
			form.setCreatedBy(clientUser.getAccountNumber());
			form.setTransmitted(false);
			form.setReallocationWages(false);
			form.setNetZeroCheck(false);
			form.setCalculateFit(null);
			
			// Go through the template rows and create the form rows
			for(TaxWageAllocationRowTemplate template : rowTemplates) {
				TaxWageAllocationRow row = new TaxWageAllocationRow();
				
				row.setRowTemplate(template);
				row.setRowNumber(template.getRowNumber());
				row.setForm(form);
				row.setSpecialInstructions(template.getSpecialInstructions());
				row.setTeamTaxAreaCode(template.getTeamTaxAreaCode());
				row.setCalculateTax(template.getCalculateTax());
				row.setCalcFitChecked(false);
				row.setVersion(version);
				form.getAllocationFormRows().add(row);
			}
		}
		
		return form;
	}
	
	@SuppressWarnings("unchecked")
	public static List<TaxWageAllocationRowTemplate> getTaxWageAllocationTemplateRows(String version) {
		TaxWageAllocationRowTemplateDAO rowTemplateDAO = TaxWageAllocationRowTemplateDAO.getInstance();
		String query = "from TaxWageAllocationRowTemplate where version = ? order by rowNumber";

		return rowTemplateDAO.find(query, version);
	}
	
	public static List<SelectItem> getTaxYearsDL(Integer startYear, Integer endYear, Integer defaultYear) {
		List<SelectItem> taxYears = new ArrayList<>();
		
		while(startYear <= endYear) {
			SelectItem item = new SelectItem(startYear, startYear.toString());
			
			if(startYear.equals(defaultYear)) {
				item.setValue(defaultYear);
			}
			taxYears.add(item);
			startYear ++;
		}
		
		return taxYears;
	}
}
