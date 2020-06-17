//	File Name:	AdminTimecardBean.java
package com.lightspeedeps.web.admin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.batch.CalculateHtg;
import com.lightspeedeps.batch.TimecardCreator;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.object.TimecardMessage;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.payroll.TimecardUtils;
import com.lightspeedeps.web.payroll.HtgMessageBean;

/**
 * The backing class for some Admin-only timecard functions. Most of these are
 * accessed from either the Prod Admin / Other page or the Admin / Misc page.
 */
@ManagedBean
@ViewScoped
public class AdminTimecardBean implements Serializable {
	/** */
	private static final long serialVersionUID = 3046618678321215447L;

	private static final Log log = LogFactory.getLog(AdminTimecardBean.class);

	/** If true, then create new timecards regardless of the status of the prior week's
	 * timecard.  If false, new timecards are NOT created when the prior week's timecard
	 * exists but is empty (unused). */
	private boolean ignorePriorWeek;

	/** If true, then create a new timecard only if the prior week's timecard exists. No
	 * hours need to have been entered. */
	private boolean onlyIfPriorWeek;

	/** The week-ending date of the timecards to be created. */
	private Date calculateStartDate;

	/** The week-ending date of the timecards to be created. */
	private Date calculateEndDate;

	/** The week-ending date of the timecards to be created. */
	private Date timecardDate;

	/** If true, the "duplicate" function will overwrite existing timecards. */
	private boolean overWrite;

	/** If true, the duplicated timecards will be used for a "Retro" operation;
	 * mark the new copies as a "Adjusted" timecards, ignore the "overWrite"
	 * flag, set the status of the copies to SUBMITTED, update each timecard
	 * from its relevant StartForm, and set the approverId to the first
	 * Production approver. */
	private boolean retro;

	/** The week-ending date of the source timecards for the "duplicate" function. */
	private Date timecardSourceDate;

	/** The week-ending date of the target timecards to be created
	 *  by the "duplicate" function.. */
	private Date timecardTargetDate;

	/** If true, the "Delete Timecards" button in Commercial Production
	 * will delete the timecards for the current Project only. */
	private boolean onlyCurrentProject;

	/** The week-ending date of the timecards to be deleted. */
	private Date deleteStartDate;

	/** The week-ending date of the timecards to be deleted. */
	private Date deleteEndDate;

	public AdminTimecardBean() {
		log.debug("this="+this);
	}

	/**
	 * Action method to duplicate one week's timecards into a new set of
	 * timecards with a different week-ending date, within the current
	 * Production.
	 *
	 * @return null navigation string
	 */
	public String actionDuplicateTimecards() {
		String result = "no production";
		Production prod = SessionUtils.getNonSystemProduction();
		Project project = null;
		if (prod.getType().hasPayrollByProject()) {
			project = SessionUtils.getCurrentProject();
		}
		if (prod != null) {
			if (retro) {
				timecardTargetDate = timecardSourceDate;
			}
			if (timecardSourceDate == null || timecardTargetDate == null) {
				MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_ERROR, "Source and Target dates are required");
			}
			else {
				try {
					result = TimecardUtils.createDuplicateTimecards(prod, project, timecardSourceDate, timecardTargetDate,
							overWrite, retro);
					MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_INFO, "Result: " + result );
				}
				catch (Exception e) {
					EventUtils.logError(e);
					MsgUtils.addGenericErrorMessage();
				}
			}
		}


		return null;
	}

	/**
	 * Action method to run the TimecardCreator task for all Production`s.
	 *
	 * @return null navigation string
	 */
	public String actionTimecardCreator() {
		TimecardCreator bean = (TimecardCreator)ServiceFinder.findBean("timecardCreator");

		int result = bean.execute(null, ignorePriorWeek, onlyIfPriorWeek, null);
		if (result >= 0) {
			MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_INFO, result + " timecards created" );
		}
		else {
			MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_ERROR, "Error during timecard creation." );
		}

		return null;
	}

	/**
	 * Action method to run the TimecardCreator for the current Production task,
	 * for a specified week-ending date.
	 *
	 * @return null navigation string
	 */
	public String actionTimecardCreatorOneProd() {
		TimecardCreator bean = (TimecardCreator)ServiceFinder.findBean("timecardCreator");

		int result = -1;
		Production prod = SessionUtils.getNonSystemProduction();
		if (prod != null) {
			result = bean.execute(prod, ignorePriorWeek, onlyIfPriorWeek, timecardDate);
		}
		if (result >= 0) {
			MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_INFO, result + " timecard(s) created" );
		}
		else {
			MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_ERROR, "Error during timecard creation." );
		}

		return null;
	}

	/**
	 * Action method for the "Calculate HTG" admin function, which allows the
	 * user to calculate HTG on all the timecards for a range of week-ending
	 * dates.
	 *
	 * @return null navigation string
	 */
	public String actionTimecardCalculateOneProd() {
		CalculateHtg bean = (CalculateHtg)ServiceFinder.findBean("calculateHtg");

		try {
			List<Integer> results;
			Production prod = SessionUtils.getNonSystemProduction();
			if (prod != null) {
				List<TimecardMessage> msgs = new ArrayList<>();
				results = bean.execute(prod, false, calculateStartDate, calculateEndDate, msgs);
				HtgMessageBean.showHtgMessages(results, msgs);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();

		}

		return null;
	}

	/**
	 * Action method to delete all the timecards within a range of dates,
	 * for the current Production.
	 *
	 * @return null navigation string
	 */
	public String actionDeleteTimecards() {
		String result = "no Timecards deleted";
		Production prod = SessionUtils.getNonSystemProduction();
		Project project = null;
		if (prod.getType().hasPayrollByProject()) {
			project = SessionUtils.getCurrentProject();
		}
		if (prod != null) {
			result = TimecardUtils.deleteTimecards(prod, project, deleteStartDate, deleteEndDate, onlyCurrentProject);
		}
		MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_INFO, "Result: " + result );
		return null;
	}

	/**See {@link #ignorePriorWeek}. */
	public boolean getIgnorePriorWeek() {
		return ignorePriorWeek;
	}
	/**See {@link #ignorePriorWeek}. */
	public void setIgnorePriorWeek(boolean ignorePriorWeek) {
		this.ignorePriorWeek = ignorePriorWeek;
	}


	/** See {@link #onlyIfPriorWeek}. */
	public boolean getOnlyIfPriorWeek() {
		return onlyIfPriorWeek;
	}
	/** See {@link #onlyIfPriorWeek}. */
	public void setOnlyIfPriorWeek(boolean onlyIfPriorWeek) {
		this.onlyIfPriorWeek = onlyIfPriorWeek;
	}

	/** See {@link #overWrite}. */
	public boolean getOverWrite() {
		return overWrite;
	}
	/** See {@link #overWrite}. */
	public void setOverWrite(boolean overWrite) {
		this.overWrite = overWrite;
	}

	/** See {@link #retro}. */
	public boolean getRetro() {
		return retro;
	}
	/** See {@link #retro}. */
	public void setRetro(boolean adjusted) {
		retro = adjusted;
	}

	/** See {@link #calculateStartDate}. */
	public Date getCalculateStartDate() {
		return calculateStartDate;
	}
	/**This sets the timecardDate to the week-ending (Saturday) date of the same
	 * week as the date entered. See {@link #calculateStartDate}. */
	public void setCalculateStartDate(Date date) {
		if (date != null) {
			date = TimecardUtils.calculateWeekEndDate(date);
		}
		calculateStartDate = date;
	}

	/** See {@link #calculateEndDate}. */
	public Date getCalculateEndDate() {
		return calculateEndDate;
	}
	/** See {@link #calculateEndDate}. */
	public void setCalculateEndDate(Date date) {
		if (date != null) {
			date = TimecardUtils.calculateWeekEndDate(date);
		}
		calculateEndDate = date;
	}

	/**See {@link #timecardDate}. */
	public Date getTimecardDate() {
		return timecardDate;
	}
	/** This sets the timecardDate to the week-ending (Saturday) date of the same
	 * week as the date entered. See {@link #timecardDate}. */
	public void setTimecardDate(Date date) {
		if (date != null) {
			date = TimecardUtils.calculateWeekEndDate(date);
		}
		timecardDate = date;
	}

	/** See {@link #timecardSourceDate}. */
	public Date getTimecardSourceDate() {
		return timecardSourceDate;
	}
	/** See {@link #timecardSourceDate}. */
	public void setTimecardSourceDate(Date date) {
		if (date != null) {
			// ensure date given is a valid week-ending date
			date = TimecardUtils.calculateWeekEndDate(date);
		}
		timecardSourceDate = date;
	}

	/** See {@link #timecardTargetDate}. */
	public Date getTimecardTargetDate() {
		return timecardTargetDate;
	}
	/** See {@link #timecardTargetDate}. */
	public void setTimecardTargetDate(Date date) {
		if (date != null) {
			// ensure date given is a valid week-ending date
			date = TimecardUtils.calculateWeekEndDate(date);
		}
		timecardTargetDate = date;
	}

	/** See {@link #onlyCurrentProject}. */
	public boolean getOnlyCurrentProject() {
		return onlyCurrentProject;
	}
	/** See {@link #onlyCurrentProject}. */
	public void setOnlyCurrentProject(boolean onlyCurrentProject) {
		this.onlyCurrentProject = onlyCurrentProject;
	}

	/** See {@link #deleteStartDate}. */
	public Date getDeleteStartDate() {
		return deleteStartDate;
	}
	/** See {@link #deleteStartDate}. */
	public void setDeleteStartDate(Date deleteStartDate) {
		this.deleteStartDate = deleteStartDate;
	}

	/** See {@link #deleteEndDate}. */
	public Date getDeleteEndDate() {
		return deleteEndDate;
	}
	/** See {@link #deleteEndDate}. */
	public void setDeleteEndDate(Date deleteEndDate) {
		this.deleteEndDate = deleteEndDate;
	}

}
