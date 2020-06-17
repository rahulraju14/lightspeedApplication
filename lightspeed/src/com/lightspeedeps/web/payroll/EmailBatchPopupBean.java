/**
 * File: EmailBatchPopupBean.java
 */
package com.lightspeedeps.web.payroll;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.PayrollService;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.type.ExportType;
import com.lightspeedeps.type.ReportStyle;
import com.lightspeedeps.type.ServiceMethod;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.timecard.TimecardSelectBase;
import com.lightspeedeps.web.util.EnumList;

/**
 * This class extends the TimecardSelectBean to support the transfer batch
 * popup for emailing to a payroll service.
 */
@ManagedBean
@ViewScoped
public class EmailBatchPopupBean extends TimecardSelectBase implements Serializable {
	/** */
	private static final long serialVersionUID = - 1468410578461867368L;
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(EmailBatchPopupBean.class);

	/** The type of export file to create - set by user via a drop-down. */
	private String exportType = ExportType.NONE.name();

	private static final SelectItem NO_EXPORT_SELECT= new SelectItem("NONE", "None");
	private static final SelectItem ABS_SELECT 		= ExportType.ABS.getSelectItem();
	private static final SelectItem TEAM_SELECT 	= ExportType.FULL_TAB.getSelectItem();
	private static final SelectItem CC_SELECT  		= ExportType.CREW_CARDS.getSelectItem();

	/**
	 * The drop-down list for selecting the type of export to be generated.
	 */
	private List<SelectItem> exportTypeDL;

	/** Indicates the report style selected */
	private String reportStyle = ReportStyle.TC_FULL.name();

	/** Drop-down list of styles of timecard reports presented to user. */
	private List<SelectItem> reportStyleDL = REPORT_STYLE_DL;

	/** Drop-down list of styles of timecard reports available for TV/Feature (non-AICP). */
	private static final List<SelectItem> REPORT_STYLE_DL = EnumList.createEnumSelectListStopNA(ReportStyle.class);

	/** Drop-down list of styles of timecard reports available for Commercial/AICP. */
	private static final List<SelectItem> REPORT_STYLE_AICP_DL = new ArrayList<>();

	/** Drop-down list of THE timecard report style when only one is available for Commercial/AICP. */
	private static final List<SelectItem> REPORT_STYLE_AICP_ONLY_DL = new ArrayList<>();

	private static final SelectItem AICP_SELECT = ReportStyle.TC_AICP.getSelectItem();
	private static final SelectItem NO_PDF_SELECT = ReportStyle.N_A.getSelectItem();

	static {
		REPORT_STYLE_DL.add(0, NO_PDF_SELECT);

		REPORT_STYLE_AICP_DL.add(NO_PDF_SELECT);
		REPORT_STYLE_AICP_DL.add(AICP_SELECT);

		REPORT_STYLE_AICP_ONLY_DL.add(AICP_SELECT);
	}

	public EmailBatchPopupBean() {
	}

	public static EmailBatchPopupBean getInstance() {
		return (EmailBatchPopupBean)ServiceFinder.findBean("emailBatchPopupBean");
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
	 * @param msgPrefix The message-id prefix used to customize messages in the
	 *            pop-up.
	 */
	@Override
	public void show(PopupHolder holder, int action, String msgPrefix) {
		Production prod = SessionUtils.getNonSystemProduction();

		exportTypeDL = new ArrayList<>(4);
		exportTypeDL.add(NO_EXPORT_SELECT);
		exportType = ExportType.NONE.name();

		PayrollService service = prod.getPayrollPref().getPayrollService();

		if (service != null) {
			if (service.getSendBatchMethod() == ServiceMethod.ABS_FILE) {
				exportTypeDL.add(ABS_SELECT);
				exportType = ExportType.ABS.name();	// Default to ABS export
			}
			else if (service.getSendBatchMethod() == ServiceMethod.CREW_CARDS) {
				exportTypeDL.add(CC_SELECT);
				exportType = ExportType.CREW_CARDS.name();	// Default to ABS export
			}
			else if (service.getSendBatchMethod() == ServiceMethod.TEAM_FILE) {
				exportTypeDL.add(TEAM_SELECT);
				exportType = ExportType.FULL_TAB.name();	// Default to Full Tabbed (TEAM) export
			}
			else {
				exportTypeDL = null;
			}
		}

		if (prod.getType().isAicp()) {
			reportStyle = ReportStyle.TC_AICP.name();
			if (exportTypeDL == null) { // no export choices,
				// so use drop-down that does not have the "No PDF" option.
				reportStyleDL = REPORT_STYLE_AICP_ONLY_DL;
			}
			else {
				reportStyleDL = REPORT_STYLE_AICP_DL;
			}
		}
		else {
			reportStyle = ReportStyle.TC_FULL.name();
			reportStyleDL = REPORT_STYLE_DL;
		}

		super.show(holder, action, msgPrefix);

	}

	public ExportType getType() {
		return ExportType.valueOf(exportType);
	}

	/** See {@link #reportStyle}. */
	public String getReportStyle() {
		return reportStyle;
	}
	/** See {@link #reportStyle}. */
	public void setReportStyle(String reportStyle) {
		this.reportStyle = reportStyle;
	}

	/**
	 * @return The user-selected report style as an enum value.
	 */
	public ReportStyle getReportStyleEnum() {
		return ReportStyle.valueOf(reportStyle);
	}

	/** See {@link #reportStyleDL}. */
	public List<SelectItem> getReportStyleDL() {
		return reportStyleDL;
	}

	/** See {@link #exportType}. */
	public String getExportType() {
		return exportType;
	}
	/** See {@link #exportType}. */
	public void setExportType(String exportType) {
		this.exportType = exportType;
	}

	/**
	 * @return The user-selected export file type as an enum value.
	 */
	public ExportType getExportTypeEnum() {
		return ExportType.valueOf(exportType);
	}

	/**See {@link #exportTypeDL}. */
	public List<SelectItem> getExportTypeDL() {
		return exportTypeDL;
	}

}
