/**
 * File: ExportTimecardBean.java
 */
package com.lightspeedeps.web.timecard;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.type.ExportType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.payroll.TimecardUtils;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.popup.PopupHolder;

/**
 * This class extends the TimecardSelectBean to support the export-selection
 * dialog box.
 */
@ManagedBean
@ViewScoped
public class ExportTimecardBean extends TimecardSelectBase implements Serializable {
	/** */
	private static final long serialVersionUID = - 1468410578461867368L;
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(ExportTimecardBean.class);

	/** The type of export file to create - set by user via a drop-down. */
	private String exportType = ExportType.CREW_CARDS.name();

	private static final SelectItem ABS_SELECT 		= new SelectItem(ExportType.ABS.name(), ExportType.ABS.getLabel());
	private static final SelectItem CC_SELECT  		= new SelectItem(ExportType.CREW_CARDS.name(), ExportType.CREW_CARDS.getLabel());
	private static final SelectItem HB_SELECT  		= new SelectItem(ExportType.HOT_BUDGET.name(), ExportType.HOT_BUDGET.getLabel());
	private static final SelectItem SHOWBIZ_SELECT	= new SelectItem(ExportType.SHOWBIZ_BUDGET.name(), ExportType.SHOWBIZ_BUDGET.getLabel());
	private static final SelectItem JSON_SELECT		= new SelectItem(ExportType.JSON.name(), ExportType.JSON.getLabel());
	private static final SelectItem PAYROLL_SELECT	= new SelectItem(ExportType.PAYROLL.name(), ExportType.PAYROLL.getLabel());
	private static final SelectItem FULL_TAB_SELECT	= new SelectItem(ExportType.FULL_TAB.name(), ExportType.FULL_TAB.getLabel());

	/**
	 * The drop-down list for selecting the type of export to be generated.
	 */
	private List<SelectItem> exportTypeDL = new ArrayList<>();

	public ExportTimecardBean() {
	}

	public static ExportTimecardBean getInstance() {
		return (ExportTimecardBean)ServiceFinder.findBean("exportTimecardBean");
	}

	/**
	 * Display the confirmation dialog with the specified values. Note that the
	 * Strings are all message-ids, which will be looked up in the message
	 * resource file.
	 *
	 * @param holder The object which is "calling" us, and will get the
	 *            callbacks.
	 * @param action An arbitrary integer which will be returned in the
	 *            callbacks.
	 * @param weeklyTimecard
	 * @param showAllProj If true, timecards for all projects will be included
	 *            in the export; this applies only to Commercial productions.
	 */
	public void show(boolean full, PopupHolder holder, int action, WeeklyTimecard weeklyTimecard,
			boolean showAllProj) {
		String dept = "";
		String batch = "";
		String msg = "";

		if (weeklyTimecard != null) {
			dept = weeklyTimecard.getDeptName();
			if (weeklyTimecard.getWeeklyBatch() != null) {
				batch = weeklyTimecard.getWeeklyBatch().getName();
			}
			msg = "(" + weeklyTimecard.getFirstName() + " " + weeklyTimecard.getLastName() + " - " +
					weeklyTimecard.getOccupation() + ")";
		}
		Production prod = TimecardUtils.findProduction(weeklyTimecard);
		Project proj = TimecardUtils.findProject(prod, weeklyTimecard);

		super.show(prod, proj, dept, batch, full, showAllProj, holder, action,
				"Timecard.Export.Title", null, "Timecard.Export.Ok", "Confirm.Cancel");

		if (weeklyTimecard != null) {
			setWeekEndDate(weeklyTimecard.getEndDate());
		}
		setMessage(msg);

		exportTypeDL = new ArrayList<>(4);

		AuthorizationBean authBean = AuthorizationBean.getInstance();

		if (authBean.hasPageField(Constants.PGKEY_EXPORT_ABS)) {
			if (prod != null && prod.getPayrollPref().getPayrollService() != null) {
				if (prod.getPayrollPref().getPayrollService().getName().contains("ABS")) {
					exportTypeDL.add(ABS_SELECT);
					exportType = ExportType.ABS.name();	// Default to ABS export
				}
			}
		}

		if (authBean.hasPageField(Constants.PGKEY_EXPORT_TEAM)) {
			if (prod != null && prod.getPayrollPref().getPayrollService() != null) {
				if (prod.getPayrollPref().getPayrollService().getName().contains("TEAM")) {
					exportTypeDL.add(FULL_TAB_SELECT);
					exportType = ExportType.FULL_TAB.name();	// Default to TEAM (full tabbed) export
				}
			}
		}

		exportTypeDL.add(CC_SELECT);

		if (prod != null && prod.getType().isAicp() && authBean.hasPageField(Constants.PGKEY_EXPORT_SBB)) {
			exportTypeDL.add(SHOWBIZ_SELECT);
		}
		else if (authBean.hasPageField(Constants.PGKEY_WRITE_ANY)) { // TV/Feature TESTING - LS Admins only
			exportTypeDL.add(SHOWBIZ_SELECT);
		}

		if (prod != null && prod.getType().isAicp() && authBean.hasPageField(Constants.PGKEY_EXPORT_HB)) {
			exportTypeDL.add(HB_SELECT);
		}

		if (authBean.hasPageField(Constants.PGKEY_EXPORT_JSON)) {
			exportTypeDL.add(JSON_SELECT);
		}

		if (authBean.hasPageField(Constants.PGKEY_EXPORT_PAYROLL)) {
			exportTypeDL.add(PAYROLL_SELECT);
		}
	}

	public ExportType getType() {
		return ExportType.valueOf(exportType);
	}

	/** See {@link #exportType}. */
	public String getExportType() {
		return exportType;
	}
	/** See {@link #exportType}. */
	public void setExportType(String exportType) {
		this.exportType = exportType;
	}

	/**See {@link #exportTypeDL}. */
	public List<SelectItem> getExportTypeDL() {
		return exportTypeDL;
	}

}
