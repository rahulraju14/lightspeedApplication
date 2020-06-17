/**
 * File: DprPopupBean.java
 */
package com.lightspeedeps.web.report;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupHolder;

/**
 * This class extends our basic PopupBean by adding controls to get
 * user selections regarding updating a DPR with call and wrap times.
 */
@ManagedBean
@ViewScoped
public class DprPopupBean extends PopupBean implements Serializable {
	/** */
	private static final long serialVersionUID = - 8195841393472014862L;

	private static final Log log = LogFactory.getLog(DprPopupBean.class);

	/** True iff the user selects the checkbox to import call times. */
	private boolean importCallTime;

	/** True iff the user selects the checkbox to import wrap times. */
	private boolean importWrapTime;

	/** Indicates the call time source selected (via radio buttons). */
	private String callTimeSource = SOURCE_CALLSHEET;
	public static final String SOURCE_CALLSHEET = "c";
	public static final String SOURCE_TIMECARDS = "t";

	public DprPopupBean() {
	}

	public static DprPopupBean getInstance() {
		return (DprPopupBean)ServiceFinder.findBean("dprPopupBean");
	}

	/**
	 * Display the Import Crew Times dialog with the specified values.
	 *
	 * @param holder The object which is "calling" us, and will get the
	 *            callbacks.
	 * @param act An arbitrary integer which will be returned in the callbacks.
	 */
	public void showImportTimes(PopupHolder holder, int act) {
		log.debug("action=" + act);
		importCallTime = false;
		importWrapTime = false;
		callTimeSource = SOURCE_CALLSHEET;
		super.show(holder, act, "Dpr.ImportTimes.Title", null, "Dpr.ImportTimes.Ok", "Confirm.Cancel");
	}

	/** See {@link #importCallTime}. */
	public boolean getImportCallTime() {
		return importCallTime;
	}
	/** See {@link #importCallTime}. */
	public void setImportCallTime(boolean importCallTime) {
		this.importCallTime = importCallTime;
	}

	/** See {@link #importWrapTime}. */
	public boolean getImportWrapTime() {
		return importWrapTime;
	}

	/** See {@link #importWrapTime}. */
	public void setImportWrapTime(boolean importWrapTime) {
		this.importWrapTime = importWrapTime;
	}

	/** See {@link #callTimeSource}. */
	public String getCallTimeSource() {
		return callTimeSource;
	}
	/** See {@link #callTimeSource}. */
	public void setCallTimeSource(String rangeSelection) {
		this.callTimeSource = rangeSelection;
	}

}
