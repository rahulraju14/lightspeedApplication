/**
 * ReassignPopupBean.java
 */
package com.lightspeedeps.web.payroll;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.WeeklyBatch;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.web.popup.PopupSelectBean;

/**
 * This is the backing bean for one of the "Reassign" pop-up dialogs on the
 * Transfer to Payroll page. This one is only used when the user has selected a
 * W/E filter value of "All" on this page. It supports displaying a list of
 * week-ending dates. When the user selects a date, then the list of
 * WeeklyBatch`s for that date are displayed using the selection support in the
 * superclass, PopupSelectBean.
 */
@ManagedBean
@ViewScoped
public class ReassignPopupBean extends PopupSelectBean {
	private static final Log log = LogFactory.getLog(PopupSelectBean.class);

	/** */
	private static final long serialVersionUID = 1L;

	/** Backing field for the user's selection from the list
	 * of week-ending dates. */
	private Date selectedWeek;

	/** The batch from which the timecard(s) are being transferred.  This batch
	 * is automatically excluded from the list of batches selectable by the user. */
	private WeeklyBatch fromBatch;

	/** The List of batches being displayed on the Transfer page.  This is the
	 * source from which the code selects the batches to display in the drop-
	 * down list in the pop-up. */
	private List<WeeklyBatch> batchList = new ArrayList<>();

	/** List of W/E dates for the user to choose from. */
	private List<SelectItem> weekEndingList;

	/**
	 * default constructor
	 */
	public ReassignPopupBean() {
	}

	/**
	 * @return The instance of this bean appropriate for the current context.
	 */
	public static ReassignPopupBean getInstance() {
		return (ReassignPopupBean)ServiceFinder.findBean("reassignPopupBean");
	}

	/**
	 * This method should be called by the using class to initiate the
	 * pop-up display, after the weekEndingList has been initialized.
	 */
	public void show() {
		if (weekEndingList != null && weekEndingList.size() > 0) {
			selectedWeek = (Date)weekEndingList.get(0).getValue();
			setSelectList(createBatchSelectList(selectedWeek));
			if (getSelectList().size() > 0) {
				setSelectedObject(getSelectList().get(0).getValue().toString());
			}
		}
	}

	/**
	 * ValueChangeListener for week-ending date drop-down list.
	 * @param event contains old and new values
	 */
	public void listenWeekEndChange(ValueChangeEvent event) {
		try {
			log.debug("new val = " + event.getNewValue()+ ", ID =" +  event.getComponent().getId());
			if (event.getNewValue() != null) {
				selectedWeek = (Date)event.getNewValue();
				setSelectList(createBatchSelectList(selectedWeek));
				if (getSelectList().size() > 0) {
					setSelectedObject(getSelectList().get(0).getValue().toString());
				}
				else {
					setSelectedObject(null);
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
		}
	}

	/**
	 * Create the list of batches from which the user may choose the "target"
	 * batch.
	 *
	 * @param date All the batches matching this week-ending date will be
	 *            included in the returned list.
	 * @return A non-null, but possibly empty, list of selections, where the
	 *         value is the WeeklyBatch.id, and the label is the
	 *         WeeklyBatch.name.
	 */
	private List<SelectItem> createBatchSelectList(Date date) {
		List<SelectItem> list = new ArrayList<>();
		if (! fromBatch.isUnBatched()) {
			list.add(new SelectItem(0, "(Unbatched)"));
		}
		for (WeeklyBatch wb : batchList) {
			if ((! wb.isUnBatched()) && wb.getEndDate().equals(date) && (! wb.equals(fromBatch))) {
				list.add(new SelectItem(wb.getId(), wb.getName()));
			}
		}
		return list;
	}

	// Accessors and mutators

	/**See {@link #fromBatch}. */
	public WeeklyBatch getFromBatch() {
		return fromBatch;
	}
	/**See {@link #fromBatch}. */
	public void setFromBatch(WeeklyBatch fromBatch) {
		this.fromBatch = fromBatch;
	}

	/**See {@link #batchList}. */
	public List<WeeklyBatch> getBatchList() {
		return batchList;
	}
	/**See {@link #batchList}. */
	public void setBatchList(List<WeeklyBatch> batchList) {
		this.batchList = batchList;
	}

	/**See {@link #selectedWeek}. */
	public Date getSelectedWeek() {
		return selectedWeek;
	}
	/**See {@link #selectedWeek}. */
	public void setSelectedWeek(Date selectedWeek) {
		this.selectedWeek = selectedWeek;
	}

	/**See {@link #weekEndingList}. */
	public List<SelectItem> getWeekEndingList() {
		if (weekEndingList == null) {
			return new ArrayList<>();
		}
		return weekEndingList;
	}
	/**See {@link #weekEndingList}. */
	public void setWeekEndingList(List<SelectItem> weekEndingList) {
		this.weekEndingList = weekEndingList;
	}

}
