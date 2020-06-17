package com.lightspeedeps.object;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.type.StripStatus;
import com.lightspeedeps.type.StripType;

/**
 * This class is used by the Stripboard View and Edit classes to represent a
 * Strip being viewed, and related information from the first Scene associated
 * with the Strip.
 */
public class StripBoardScene extends StripScene implements Serializable, Cloneable {
	// TODO can some of these fields be eliminated and use the Strip fields directly?
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(StripBoardScene.class);

	private static final long serialVersionUID = -136611094636473838L;

	private Integer id;
	private StripType type;
	private StripStatus status = StripStatus.UNSCHEDULED;
	private int orderNumber;
	private Integer sheetNumber;
	private Integer length;
	private String comment;
	private String dayNumber;
	private String sceneNumbers;
	private List<String> sceneNumberList;
	private String synopsis;
	private String locationSynopsis;
	private String pageLength;

	/** The text for the strip; used for banner text and End-of-Day text, and for
	 * the whole strip when it is output as a single <div>. */
	private String stripText;

	/** This is set to one of two CSS classes, for displaying either "thin" vs "thick" styles. */
	private String style;

	/** The CSS class which will set the appropriate background and text color for this strip. */
	private String colorClass;

	private Date endOfDayDate;
	private int elapsedTime = 0;
	private boolean selectedScheduled;
	private boolean selectedUnScheduled;

	/** true if the row is selected. */
	private boolean selected;

	/** The list of elementId (cast-id) values for all the ScriptElements used in this Strip.
	 * If the last entry is "0", the list was truncated (for space reasons), and the JSP code
	 * will add a ".." to the display. */
	private List<String> elementIdList;

	/** The list of title strings (tool tips) to be displayed for each of the corresponding
	 * entries in elementIdList.  This is normally the name of the matching ScriptElement,
	 * except that for a trailing ellipsis entry, it is a list of the omitted elementIds. */
	private List<String> elementNameList;

	/** The list of database id's for each of the corresponding
	 * entries in elementIdList.  This is used to make the displayed id a link
	 * to the Script Element page displaying the appropriate element. */
	private List<Integer> elementDbIdList;


	public StripBoardScene() {
	}

	public StripBoardScene( StripType type, String text, String comment) {
		setStripText(text);
		setComment(comment);
		setType(type);
	}

	/**
	 * @return the status
	 */
	public StripStatus getStatus() {
		return status;
	}
	/**
	 * @param status the status to set
	 */
	public void setStatus(StripStatus status) {
		this.status = status;
	}

	/**
	 * @return the stripText
	 */
	public String getStripText() {
		return stripText;
	}
	/**
	 * @param stripText the stripText to set
	 */
	public void setStripText(String stripText) {
		this.stripText = stripText;
	}

	/** See {@link #elementNameList}. */
	public List<String> getElementNameList() {
		return elementNameList;
	}
	/** See {@link #elementNameList}. */
	public void setElementNameList(List<String> elementNames) {
		elementNameList = elementNames;
	}

	/** See {@link #elementIdList}. */
	public List<String> getElementIdList() {
		return elementIdList;
	}
	/** See {@link #elementIdList}. */
	public void setElementIdList(List<String> elementArray) {
		elementIdList = elementArray;
	}

	/** See {@link #elementDbIdList}. */
	public List<Integer> getElementDbIdList() {
		return elementDbIdList;
	}
	/** See {@link #elementDbIdList}. */
	public void setElementDbIdList(List<Integer> elementDbIdList) {
		this.elementDbIdList = elementDbIdList;
	}

	public int getIdCount() {
		if (elementIdList == null) {
			return 0;
		}
		return elementIdList.size();
	}

	/**
	 * @return the locationSynopsis
	 */
	public String getLocationSynopsis() {
		return locationSynopsis;
	}
	/**
	 * @param locationSynopsis the locationSynopsis to set
	 */
	public void setLocationSynopsis(String locationSynopsis) {
		this.locationSynopsis = locationSynopsis;
	}
	/**
	 * @return the endOfDayDate
	 */
	public Date getEndOfDayDate() {
		return endOfDayDate;
	}
	/**
	 * @param endOfDayDate the endOfDayDate to set
	 */
	public void setEndOfDayDate(Date endOfDayDate) {
		this.endOfDayDate = endOfDayDate;
	}

	/**
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @return the orderNumber
	 */
	public int getOrderNumber() {
		return orderNumber;
	}
	/**
	 * @param orderNumber the orderNumber to set
	 */
	public void setOrderNumber(int orderNumber) {
		this.orderNumber = orderNumber;
	}
	/**
	 * @return the comment
	 */
	public String getComment() {
		return comment;
	}
	/**
	 * @param comment the comment to set
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}
	/**
	 * @return the dayNumber
	 */
	public String getDayNumber() {
		return dayNumber;
	}
	/**
	 * @param dayNumber the dayNumber to set
	 */
	public void setDayNumber(String dayNumber) {
		this.dayNumber = dayNumber;
	}
	/**
	 * @return the sheetNumber
	 */
	public Integer getSheetNumber() {
		return sheetNumber;
	}
	/**
	 * @param sheetNumber the sheetNumber to set
	 */
	public void setSheetNumber(Integer sheetNumber) {
		this.sheetNumber = sheetNumber;
	}
	/**
	 * @return the sceneNumbers
	 */
	public String getSceneNumbers() {
		return sceneNumbers;
	}
	/**
	 * @param sceneNumbers the sceneNumbers to set
	 */
	public void setSceneNumbers(String sceneNumbers) {
		this.sceneNumbers = sceneNumbers;
	}
	/** See {@link #sceneNumberList}. */
	public List<String> getSceneNumberList() {
		return sceneNumberList;
	}

	/** See {@link #sceneNumberList}. */
	public void setSceneNumberList(List<String> sceneNumberList) {
		this.sceneNumberList = sceneNumberList;
	}

	/**
	 * @return the length
	 */
	public Integer getLength() {
		return length;
	}
	/**
	 * @param length the length to set
	 */
	public void setLength(Integer length) {
		this.length = length;
	}

	/**
	 * @return the synopsis
	 */
	public String getSynopsis() {
		return synopsis;
	}
	/**
	 * @param syn the synopsis to set
	 */
	public void setSynopsis(String syn) {
		synopsis = syn;
	}

	/**
	 * @return the elapsedTime
	 */
	public int getElapsedTime() {
		return elapsedTime;
	}
	/**
	 * @param elapsedTime the elapsedTime to set
	 */
	public void setElapsedTime(int elapsedTime) {
		this.elapsedTime = elapsedTime;
	}

	/**
	 * @return the type
	 */
	public StripType getType() {
		return type;
	}
	/**
	 * @param type the type to set
	 */
	public void setType(StripType type) {
		this.type = type;
	}

//	/**
//	 * @return the elememtIds
//	 */
//	public String getElementIds() {
//		return elementIds;
//	}
//	/**
//	 * @param elememtIds the elememtIds to set
//	 */
//	public void setElementIds(String elememtIds) {
//		this.elementIds = elememtIds;
//	}

	public String getPageLength() {
		return pageLength;
	}
	/**
	 * @param pageLength the pageLength to set
	 */
	public void setPageLength(String pageLength) {
		this.pageLength = pageLength;
	}

	/** See {@link #selected}. */
	public boolean getSelected() {
		return selected;
	}
	/** See {@link #selected}. */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/**
	 * @return the selectedScheduled
	 */
	public boolean isSelectedScheduled() {
		return selectedScheduled;
	}
	/**
	 * @param selectedScheduled the selectedScheduled to set
	 */
	public void setSelectedScheduled(boolean selectedScheduled) {
		this.selectedScheduled = selectedScheduled;
	}

	/**
	 * @return the selectedUnScheduled
	 */
	public boolean isSelectedUnScheduled() {
		return selectedUnScheduled;
	}
	/**
	 * @param selectedUnScheduled the selectedUnScheduled to set
	 */
	public void setSelectedUnScheduled(boolean selectedUnScheduled) {
		this.selectedUnScheduled = selectedUnScheduled;
	}

	/** See {@link #style}. */
	public String getStyle() {
		return style;
	}
	/** See {@link #style}. */
	public void setStyle(String style) {
		this.style = style;
	}

	/** See {@link #colorClass}. */
	public String getColorClass() {
		return colorClass;
	}
	/** See {@link #colorClass}. */
	public void setColorClass(String colorClass) {
		this.colorClass = colorClass;
	}

	@Override
	public Object clone() {
		try {
			StripBoardScene copy = (StripBoardScene)super.clone();
			if (elementIdList != null) {
				copy.elementIdList = new ArrayList<String>();
				for (String s : elementIdList) {
					copy.elementIdList.add(s);
				}
			}
			if (elementNameList != null) {
				copy.elementNameList = new ArrayList<String>();
				for (String s : elementNameList) {
					copy.elementNameList.add(s);
				}
			}
			return copy;
		}
		catch (CloneNotSupportedException e) { // should not happen
			return null;
		}
	}

	@Override
	public String toString() {
		String s = ""; // usually have super.toString() here, but it really clutters the output.
		s += "[";
		s += "id=" + (getId()==null ? "null" : id);
		if (type == StripType.BREAKDOWN) {
			s += ", sn=" + sceneNumbers;
		}
		else {
			s += ", t=" + type;
		}
		s += ", st=" + status;
		s += ", o=" + orderNumber;
		s += "]";
		return s;
	}

}
