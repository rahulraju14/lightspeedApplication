package com.lightspeedeps.web.approver;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.web.popup.PopupInputBigBean;

/**
 * Displays the results of the Hot Costs Import.
 * Will display the number of timecards imported, not imported
 * and the reason why each timecard was not imported.
 */
@ManagedBean
@ViewScoped
public class HotCostsImportMsgBean extends PopupInputBigBean {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(HotCostsImportMsgBean.class);
	private static final long serialVersionUID = 1L;

	/** Output Section Header */
	private String outputSectionTitle;
	/** Buffer containing the results messages */
	private StringBuffer outputBuffer;

	/**
	 * Returns the current instance of our bean. Note that this may not be available in a batch
	 * environment, in which case null is returned.
	 */
	public static HotCostsImportMsgBean getInstance() {
		HotCostsImportMsgBean bean = null;

		bean = (HotCostsImportMsgBean) ServiceFinder.findBean("hotCostsImportMsgBean");

		return bean;
	}

	// Constructor
	public HotCostsImportMsgBean() {
		outputSectionTitle = MsgUtils.getMessage("HotCosts.Import.Output.Section.Title");
		outputBuffer = new StringBuffer();
	}

	/**
	 * Generate the messages to display to the user for the import status
	 * @param displayObjects - Items to display in the following order:
	 * 0 - Number of timecards imported
	 * 1 - Number of timecards not imported
	 * 2 - List of messages to display
	 */
	@SuppressWarnings({ "unchecked"})
	private void generateTimecardImportOutputMsg(List<Object> importResults) {
		Integer tcImported, tcNotImported;
		List<String>outputMsgs;
		outputBuffer = new StringBuffer();

		if(importResults != null && !importResults.isEmpty()) {
			tcImported = (Integer) importResults.get(0);
			tcNotImported = (Integer) importResults.get(1);
			outputMsgs = (List<String>) importResults.get(2);

			outputBuffer.append(MsgUtils.formatMessage("HotCosts.Import.Timecards.Imported.Section.Title", tcImported) + Constants.NEW_LINE);
			outputBuffer.append(MsgUtils.formatMessage("HotCosts.Import.Timecards.Not.Imported.Section.Title", tcNotImported) + Constants.NEW_LINE + Constants.NEW_LINE);

			// Individual messages for timecards not imported.
			outputBuffer.append(MsgUtils.getMessage("HotCosts.Import.OutputSection.Notification.Title"));
			if(outputMsgs != null && !outputMsgs.isEmpty()) {
				for(String msg : outputMsgs) {
					outputBuffer.append(msg);
				}
			}
		}
	}

	/** See {@link #outputBuffer}. */
	public String getOutput() {
		return outputBuffer.toString();
	}

    /** See {@link #outputBuffer}. */
    public void setOutput(String output) {

	}

	/** See {@link #outputSectionTitle}. */
	public String getOutputSectionTitle() {
		return outputSectionTitle;
	}

	/**
	 * Show the popup
	 * @param popupTitleId
	 * @param importResults
	 */
	public void show(String popupTitleId, List<Object> importResults) {
		generateTimecardImportOutputMsg(importResults);

		super.show(null, 0, popupTitleId, null, "Confirm.Close");
	}
}
