package com.lightspeedeps.web.approver;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.popup.PopupInputBigBean;

/**
 * Popup that controls the import functionality for timecards into Hot Costs.
 * Eventually maybe used to import Production Reports.
 */
@ManagedBean
@ViewScoped
public class HotCostsImportBean extends PopupInputBigBean {
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(HotCostsImportBean.class);

	/** Indicates that the import will come from timecards. */
	public static final String IMPORT_TYPE_TIMECARD = "TC";
	/** Indicates that the import will be for budgeted values. */
	public static final String IMPORT_TYPE_BUDGETED_VALUES = "BV";
	/* May be used in the future. */
//	private static final String IMPORT_TYPE_PROD_REPORT = "PR";
	/** The type of import selected by the user. Only currently supporting timecards */
	private String selectedImportType;
	/** Whether or not to overwrite existing Hot Costs data with the data from the import. */
	private boolean overwriteData;
	/** List of possible date that budgeted values can be imported from */
	private List<SelectItem> dayDatesList;
	/** List of possible dates to select */
	private List<Date> dayDates;
	/** Day that budgeted values that are to be imported from. */
	private Date dayDate;
	/** Import from timecard flag */
	private boolean importTimecard;

	/**
	 * Returns the current instance of our bean. Note that this may not be available in a batch
	 * environment, in which case null is returned.
	 */
	public static HotCostsImportBean getInstance() {
		HotCostsImportBean bean = null;

		bean = (HotCostsImportBean) ServiceFinder.findBean("hotCostsImportBean");

		return bean;
	}

	// Constructor
	public HotCostsImportBean() {
		super();
		setVisible(false);
		overwriteData = false;
		importTimecard = true;
	}

	/** See {@link #dayDates}. */
	public void setDayDates(List<Date> dayDates) {
		this.dayDates = dayDates;
	}

	/** See {@link #dayDate}. */
	public Date getDayDate() {
		return dayDate;
	}

	/** See {@link #dayDate}. */
	public void setDayDate(Date dayDate) {
		this.dayDate = dayDate;
	}

	/** See {@link #selectedImportType}. */
	public String getSelectedImportType() {
		return selectedImportType;
	}

	/** See {@link #selectedImportType}. */
	public void setSelectedImportType(String selectedImportType) {
		this.selectedImportType = selectedImportType;
	}

	/** See {@link #overwriteData}. */
	public boolean getOverwriteData() {
		return overwriteData;
	}

	/** See {@link #overwriteData}. */
	public void setOverwriteData(boolean overwriteData){
		this.overwriteData = overwriteData;
	}

	/** See {@link #importTimecard}. */
	public boolean getImportTimecard() {
		return importTimecard;
	}

	/** See {@link #importTimecard}. */
	public void setImportTimecard(boolean importTimecard) {
		this.importTimecard = importTimecard;
	}

	/**
	 * Return the list of dates that have budgeted values set.
	 * @return
	 */
	public List <SelectItem> getDatesList() {
		dayDatesList = new ArrayList<>();

		for(Date date : dayDates) {
			dayDatesList.add(new SelectItem(date, date.toString()));
		}
		return dayDatesList;
	}

	public String actionImport() {
		if(getConfirmationHolder() != null) {
			// Set the attributes on the Hot Costs bean to do the import.
			HotCostsDataEntryBean bean = (HotCostsDataEntryBean)getConfirmationHolder();
			bean.setImportType(IMPORT_TYPE_TIMECARD);
			bean.setOverwriteData(overwriteData);
		}

		super.actionOk();
		return null;
	}

	/**
	 * User has selected a date from which to pull the budgetedValues from.
	 * @return
	 */
	public String actionImportBudgetedValues() {
		if(getConfirmationHolder() != null) {
			// Set the attributes on the Hot Costs bean to do the import.
			HotCostsDataEntryBean bean = (HotCostsDataEntryBean)getConfirmationHolder();

			bean.setDayDate(dayDate);
		}

		super.actionOk();
		return null;
	}


	public void listenDayDateChanged(ValueChangeEvent event) {
		Date selectedDate = (Date)event.getNewValue();

		dayDate = selectedDate;
	}

	/**
	 * Show the popup
	 * @param popupHolder - Caller
	 * @param action - action to be performed
	 */
	@Override
	public void show(PopupHolder popupHolder, int  action, String titleId) {
		super.show(popupHolder, action, titleId, "Confirm.OK", "Confirm.Cancel");
	}
}
