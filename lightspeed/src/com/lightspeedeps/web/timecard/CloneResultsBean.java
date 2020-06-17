/**
 * CloneResultsBean.java
 */
package com.lightspeedeps.web.timecard;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.WeeklyTimecardDAO;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.object.TimecardEntry;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.payroll.TimecardUtils;

/**
 * This is the backing bean for the (mobile) page that reports the
 * results of a timecard clone operation.
 */
@ManagedBean
@ViewScoped
public class CloneResultsBean {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(CloneResultsBean.class);

	/** FIELDS */

	/** The timecard that was cloned. */
	private WeeklyTimecard weeklyTimecard;

	/** The week-ending date of the cloned timecard. */
	private Date weekEndingDate = TimecardUtils.calculateLastDayOfCurrentWeek();

	/** The Production holding all the cloned timecards. */
	private final Production production = SessionUtils.getProduction();

	/** True iff the clone operation finished without any errors. */
	private boolean clonedOk;

	/** The (possibly empty) list of WeeklyTimecard.id values of timecards
	 * which were NOT cloned. */
	private List<Integer> errorList;

	/** The (possibly empty) list of WeeklyTimecard`s that were NOT cloned. */
	private List<TimecardEntry> failedList;

	/** Count of successfully cloned timecards (for messaging). */
	private Integer cloned;

	WeeklyTimecardDAO weeklyTimecardDAO = WeeklyTimecardDAO.getInstance();

	/**
	 * default constructor
	 */
	@SuppressWarnings("unchecked")
	public CloneResultsBean() {
		Integer id = SessionUtils.getInteger(Constants.ATTR_TIMECARD_ID);
		if (id != null) { // Have a specific time card to copy
			setWeeklyTimecard(WeeklyTimecardDAO.getInstance().findById(id));
			if (weeklyTimecard != null) {
				weekEndingDate = weeklyTimecard.getEndDate();
				if (! weeklyTimecard.getProdId().equals(production.getProdId())) {
					// User is in a production, but the timecard Id from the session doesn't match this production
					weeklyTimecard = null;
					SessionUtils.put(Constants.ATTR_TIMECARD_ID, null);
				}
			}
		}
		errorList = (List<Integer>)SessionUtils.get(Constants.ATTR_TC_CLONE_ERRORS);
		if (errorList == null) {
			errorList = new ArrayList<Integer>();
		}
		failedList = createFailedList();
		clonedOk = (failedList.size() == 0);
		cloned = SessionUtils.getInteger(Constants.ATTR_TC_CLONE_COUNT);
	}

	public String actionContinue() {
		SessionUtils.put(Constants.ATTR_TC_CLONE_ERRORS, null);
		return "pickdaym";
	}

	/**
	 * Create the list of failed timecards, based on the 'errorList', which is
	 * the list of database ids of the failed timecards.
	 *
	 * @return List of TimecardEntry instances, each one containing a
	 *         WeeklyTimecard that was NOT cloned.
	 */
	private List<TimecardEntry> createFailedList() {
		List<TimecardEntry> list = new ArrayList<TimecardEntry>();
		if (errorList != null) {
			for (Integer id : errorList) {
				WeeklyTimecard wtc = weeklyTimecardDAO.findById(id);
				if (wtc != null) {
					list.add(new TimecardEntry(wtc));
				}
			}
		}
		return list;
	}


	/**See {@link #weeklyTimecard}. */
	public WeeklyTimecard getWeeklyTimecard() {
		return weeklyTimecard;
	}
	/**See {@link #weeklyTimecard}. */
	public void setWeeklyTimecard(WeeklyTimecard weeklyTimecard) {
		this.weeklyTimecard = weeklyTimecard;
	}

	/**See {@link #weekEndingDate}. */
	public Date getWeekEndingDate() {
		return weekEndingDate;
	}
	/**See {@link #weekEndingDate}. */
	public void setWeekEndingDate(Date weekEndingDate) {
		this.weekEndingDate = weekEndingDate;
	}

	/**See {@link #failedList}. */
	public List<TimecardEntry> getFailedList() {
		if (failedList == null) {
			failedList = createFailedList();
		}
		return failedList;
	}
	/**See {@link #failedList}. */
	public void setFailedList(List<TimecardEntry> recipients) {
		failedList = recipients;
	}

	/**See {@link #clonedOk}. */
	public boolean getClonedOk() {
		return clonedOk;
	}
	/**See {@link #clonedOk}. */
	public void setClonedOk(boolean cloneOverwriteTimes) {
		clonedOk = cloneOverwriteTimes;
	}

	/**See {@link #cloned}. */
	public Integer getCloned() {
		return cloned;
	}
	/**See {@link #cloned}. */
	public void setCloned(Integer cloned) {
		this.cloned = cloned;
	}

}
