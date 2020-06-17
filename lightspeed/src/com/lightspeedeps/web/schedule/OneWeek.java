/**
 * OneWeek.java
 */
package com.lightspeedeps.web.schedule;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.lightspeedeps.object.SceneItem;

/**
 * A class representing one week's worth of days in a calendar. Used for display
 * and editing of the project schedule calendar.
 * <p>
 * Note that the fields are 'package' (default) access, as they are directly
 * accessed by the calendar code.
 */
public class OneWeek implements Serializable {

	private static final long serialVersionUID = 7728537759484567868L;

	/** The day number (within the month); used for display purposes. */
	String[] dayNumber = new String[8];

	/** true: show icon fields; false: non-working day */
	boolean[] showDataDay = new boolean[8];
	/** filename for main image to display, usually based on work-day type */
	String[] imagePathDay = new String[8];
	/** Actual date of this day. */
	Date[] date = new Date[8];

	/** DateEvent-Id for Edit Calendar screen, for drag & drop */
	int[] dateEventIdDay = new int[8];

	// Fields currently used in View mode but not Edit mode:

	String[] callTimeDay = new String[8];
	String[] strpBrdNo = new String[8];

	/** True if a icon+link should be generated on this day to link to the
	 * stripboard display. */
	boolean[] stripBrdDay = new boolean[8];

	boolean[] callSheetDayNo = new boolean[8];

	/** ArrayList for Scene Numbers - used to generate links on
	 * the view page to the breakdown page. */
	@SuppressWarnings("unchecked")
	List<SceneItem>[] sceneListDay = new ArrayList[8];

	/** CSS class - used to highlight current date in calendar */
	String[] highlightDay = new String[8];


	public OneWeek() {
		// default constructor
	}

	/** See {@link #dayNumber}. */
	public String[] getDayNumber() {
		return dayNumber;
	}
	/** See {@link #dayNumber}. */
	public void setDayNumber(String[] dayNumber) {
		this.dayNumber = dayNumber;
	}

	/** See {@link #showDataDay}. */
	public boolean[] getShowDataDay() {
		return showDataDay;
	}
	/** See {@link #showDataDay}. */
	public void setShowDataDay(boolean[] showDataDay) {
		this.showDataDay = showDataDay;
	}

	/** See {@link #imagePathDay}. */
	public String[] getImagePathDay() {
		return imagePathDay;
	}
	/** See {@link #imagePathDay}. */
	public void setImagePathDay(String[] imagePathDay) {
		this.imagePathDay = imagePathDay;
	}

	/** See {@link #dateEventIdDay}. */
	public int[] getDateEventIdDay() {
		return dateEventIdDay;
	}
	/** See {@link #dateEventIdDay}. */
	public void setDateEventIdDay(int[] dateEventIdDay) {
		this.dateEventIdDay = dateEventIdDay;
	}

	/** See {@link #callSheetDayNo}. */
	public boolean[] getCallSheetDayNo() {
		return callSheetDayNo;
	}
	/** See {@link #callSheetDayNo}. */
	public void setCallSheetDayNo(boolean[] callSheetDayNo) {
		this.callSheetDayNo = callSheetDayNo;
	}

	/** See {@link #callTimeDay}. */
	public String[] getCallTimeDay() {
		return callTimeDay;
	}
	/** See {@link #callTimeDay}. */
	public void setCallTimeDay(String[] callTimeDay) {
		this.callTimeDay = callTimeDay;
	}

	/** See {@link #stripBrdDay}. */
	public boolean[] getStripBrdDay() {
		return stripBrdDay;
	}
	/** See {@link #stripBrdDay}. */
	public void setStripBrdDay(boolean[] stripBrdDay) {
		this.stripBrdDay = stripBrdDay;
	}

	/** See {@link #strpBrdNo}. */
	public String[] getStrpBrdNo() {
		return strpBrdNo;
	}
	/** See {@link #strpBrdNo}. */
	public void setStrpBrdNo(String[] strpBrdNo) {
		this.strpBrdNo = strpBrdNo;
	}

	/** See {@link #sceneListDay}. */
	public List<SceneItem>[] getSceneListDay() {
		return sceneListDay;
	}
	/** See {@link #sceneListDay}. */
	public void setSceneListDay(List<SceneItem>[] sceneListDay) {
		this.sceneListDay = sceneListDay;
	}

	/** See {@link #highlightDay}. */
	public String[] getHighlightDay() {
		return highlightDay;
	}
	/** See {@link #highlightDay}. */
	public void setHighlightDay(String[] highlightDay) {
		this.highlightDay = highlightDay;
	}

	/** See {@link #date}. */
	public Date[] getDate() {
		return date;
	}
	/** See {@link #date}. */
	public void setDate(Date[] workDateDayView) {
		this.date = workDateDayView;
	}

}
