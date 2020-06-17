/**
 * CreateTimesheetBean.java
 */
package com.lightspeedeps.web.timecard;

import java.util.Date;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupHolder;

/**
 * This is the backing bean for the "New Timesheet" button on Timesheet page.
 * It includes the functions of creating New Timessheet for Tours Production.
 */
@ManagedBean
@ViewScoped
public class CreateTimesheetBean extends PopupBean {

	private static final long serialVersionUID = 1L;

	private static final Log log = LogFactory.getLog(CreateTimesheetBean.class);

	/** FIELDS */
	
	/** The week-starting date of the source timesheet. */
	private Date weekStartingDate;

	/**
	 * default constructor.
	 */
	public CreateTimesheetBean() {
		log.debug("");
	}

	public static CreateTimesheetBean getInstance() {
		return (CreateTimesheetBean)ServiceFinder.findBean("createTimesheetBean");
	}

	/**
	 * @see com.lightspeedeps.web.popup.PopupBean#show(com.lightspeedeps.web.popup.PopupHolder, int, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void show(PopupHolder holder, int act, String titleId, String buttonOkId, String buttonCancelId) {
		log.debug("");
		super.show(holder, act, titleId, buttonOkId, buttonCancelId);
	}

	/** See {@link #weekStartingDate}. */
	public Date getWeekStartingDate() {
		return weekStartingDate;
	}

	/** See {@link #weekStartingDate}. */
	public void setWeekStartingDate(Date weekStartingDate) {
		this.weekStartingDate = weekStartingDate;
	}
}
