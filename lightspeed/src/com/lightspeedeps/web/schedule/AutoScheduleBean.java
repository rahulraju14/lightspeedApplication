/**
 * File: AutoSchedule.java
 */
package com.lightspeedeps.web.schedule;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.SceneDAO;
import com.lightspeedeps.dao.ScriptDAO;
import com.lightspeedeps.dao.StripDAO;
import com.lightspeedeps.dao.StripboardDAO;
import com.lightspeedeps.model.Scene;
import com.lightspeedeps.model.Script;
import com.lightspeedeps.model.Strip;
import com.lightspeedeps.model.Stripboard;
import com.lightspeedeps.model.Unit;
import com.lightspeedeps.object.StripScene;
import com.lightspeedeps.type.StripStatus;
import com.lightspeedeps.type.StripType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.common.StringUtils;

/**
 * Backing bean for the Auto-Schedule popup dialog.  The Stripboard editor
 * (StripBoardEditBean) finds our instance bean, and sets the Script, Stripboard,
 * and Unit fields.  The popup is then displayed, and the controls in that
 * dialog will set our fields that determine the sort keys and ordering, and
 * end-of-day distribution.  When the user clicks "ok", our schedule() method
 * is called, which does all the work.
 */
@ManagedBean
@ViewScoped
public class AutoScheduleBean implements Serializable {
	/** */
	private static final long serialVersionUID = 2954205069653309473L;

	private static final Log log = LogFactory.getLog(AutoScheduleBean.class);

	private final static int SORT_NONE_IX = 0;
	private final static int SORT_INTEXT_IX = 1;
	private final static int SORT_DAYNITE_IX = 2;
	private final static int SORT_SET_IX = 3;

	private final static String ORDER_ASC = "ASC";
	@SuppressWarnings("unused")
	private final static String ORDER_DESC = "DESC";

	/** For breakOption, break every 'n' pages/strips */
	private final static String OPTION_EVERY = "EVERY";
	/** For breakOption, spread schedule over 'n' days */
	private final static String OPTION_SPREAD = "SPREAD";

	/** For breakType, break every 'n' pages */
	private final static String BREAK_PAGES = "PAGES";
	/** For breakType, break every 'n' strips */
	private final static String BREAK_STRIPS = "STRIPS";

	private boolean showDialog;

	/** String value for entry selected (0-3) for 1st key */
	private String sortKey1 = ""+SORT_SET_IX;
	/** String value for entry selected (0-3) for 2nd key */
	private String sortKey2 = ""+SORT_DAYNITE_IX;
	/** String value for entry selected (0-3) for 3rd key */
	private String sortKey3 = ""+SORT_NONE_IX;

	/** int value (0-3) for field to use for 1st key */
	private int sortType1;
	/** int value (0-3) for field to use for 2nd key */
	private int sortType2;
	/** int value (0-3) for field to use for 3rd key */
	private int sortType3;

	/** ASCending or DEScending option for 1st sort key, from pop-up radio selection. */
	private String sortOrder1 = ORDER_ASC;
	/** ASCending or DEScending option for 2nd sort key, from pop-up radio selection. */
	private String sortOrder2 = ORDER_ASC;
	/** ASCending or DEScending option for 3rd sort key, from pop-up radio selection. */
	private String sortOrder3 = ORDER_ASC;

	/** True if 1st sort key is ascending */
	private boolean sortAsc1;
	/** True if 2nd sort key is ascending */
	private boolean sortAsc2;
	/** True if 3rd sort key is ascending */
	private boolean sortAsc3;

	/** Indicates which radio button is selected -- insert EVERY 'n' pages/strips, or
	 * SPREAD the schedule across 'n' days. */
	private String breakOption = OPTION_EVERY;
	/** Indicates, for 'EVERY' option, is day limited by number of pages, or number of strips. */
	private String breakType = BREAK_PAGES;
	/** */
	private int breakCount = 5;
	/** */
	private int spreadCount = 25;

	private List<SelectItem> sortOptionDL;
	private List<SelectItem> breakTypeDL;

	private Script script;
	private Stripboard stripboard;
	private Unit unit;

	public AutoScheduleBean() {

		// create drop-down list choices
		sortOptionDL = new ArrayList<SelectItem>();
		sortOptionDL.add(new SelectItem(""+SORT_SET_IX, "Set"));
		sortOptionDL.add(new SelectItem(""+SORT_INTEXT_IX, "Int/Ext"));
		sortOptionDL.add(new SelectItem(""+SORT_DAYNITE_IX, "Day/Night"));
		sortOptionDL.add(new SelectItem(""+SORT_NONE_IX, "None"));

		breakTypeDL = new ArrayList<SelectItem>();
		breakTypeDL.add(new SelectItem(BREAK_PAGES, "Pages"));
		breakTypeDL.add(new SelectItem(BREAK_STRIPS, "Strips"));
	}

	public static AutoScheduleBean getInstance() {
		return (AutoScheduleBean) ServiceFinder.findBean("autoScheduleBean");
	}

	public boolean show() {
		if (script == null || stripboard == null) {
			return false;
		}
		setShowDialog(true);
//		ListView.addClientResize();
		return true;
	}

	public String actionCancel() {
		log.debug("");
		setShowDialog(false);
//		ListView.addClientResize();
		return null;
	}

	public void schedule() {
		try {
			setShowDialog(false);
			log.debug("keys=" + sortKey1 + "/" + sortOrder1 + ", " +
					sortKey2 + "/" + sortOrder2 + ", " + sortKey3 + "/" + sortOrder3);
			log.debug("breaks=" + breakOption + ", " + breakType + ", " + breakCount + ", " + spreadCount);
			StripboardDAO stripboardDAO = StripboardDAO.getInstance();
			stripboard = stripboardDAO.refresh(stripboard);
			script = ScriptDAO.getInstance().refresh(script);
			int breakEvery = breakCount;
			int unitId = unit.getId();

			// Remove all end-of-day strips, change all strips to scheduled status,
			// and create a StripScene object for each Strip -- needed for sort.
			List<Strip> deletedStrips = new ArrayList<Strip>();
			List<Strip> bannerStrips = new ArrayList<Strip>();
			List<Strip> ignoredStrips = new ArrayList<Strip>();
			List<StripScene> stripScenes = new ArrayList<StripScene>(stripboard.getStrips().size());
			StripScene stripScene;
			int pageLength = 0;
			for (Strip strip : stripboard.getStrips()) {
				if (strip.getUnitId() != null && strip.getUnitId().intValue() != unitId) {
					ignoredStrips.add(strip);
				}
				else if (strip.getType() == StripType.BREAKDOWN) {
					if (strip.getStatus() == StripStatus.OMITTED) {
						ignoredStrips.add(strip);
					}
					else {
						strip.setStatus(StripStatus.SCHEDULED);
						strip.setUnitId(getUnit().getId());
						stripScene = createStripscene(strip);
						if (stripScene != null) { // will be null if script is missing scene
							stripScenes.add(stripScene);
							pageLength += strip.getLength(); // calculate total page length
						}
					}
				}
				else if (strip.getType() == StripType.BANNER) {
					bannerStrips.add(strip);
				}
				else if (strip.getType() == StripType.END_STRIPBOARD) {
					ignoredStrips.add(strip);
				}
				else {
					deletedStrips.add(strip);
				}
			}

			int remainder = 0;
			String breakBy = breakType;
			if (breakOption.equals(OPTION_SPREAD)) {
				if (spreadCount >= stripScenes.size()) { // unusual, # of days >= # of strips!
					// convert "spread" option into "every 1 strip(s)"
					breakBy = BREAK_STRIPS;
					breakEvery = 1;
					remainder = 0;
				}
				else {
					// convert "spread" option into values for "every n pages"
					breakBy = BREAK_PAGES;
					breakEvery = pageLength / spreadCount;
					remainder = pageLength - (spreadCount * breakEvery);
				}
				log.debug("spread:  breakEvery=" + breakEvery + ", rem=" + remainder);
			}
			else if (breakType.equals(BREAK_PAGES)) {
				breakEvery *= 8;	// change to 8ths of a page
			}

			// Set up parameters used by comparator
			sortType1 = Integer.parseInt(sortKey1);
			sortType2 = Integer.parseInt(sortKey2);
			sortType3 = Integer.parseInt(sortKey3);
			sortAsc1 = sortOrder1.equals(ORDER_ASC);
			sortAsc2 = sortOrder2.equals(ORDER_ASC);
			sortAsc3 = sortOrder3.equals(ORDER_ASC);
			// sort StripScenes according to user criteria
			Collections.sort(stripScenes, getComparator());

			List<Strip> strips = new ArrayList<Strip>(stripScenes.size()+50);
			int order = 0;	// the order number to be assigned, incremented as we go
			// Place all the banner strips at the beginning of the new scheduled list
			for (Strip s : bannerStrips) {
				order += StripDAO.ORDER_INCREMENT;
				s.setOrderNumber(order);
				strips.add(s);
			}

			// insert new end-of-day strips based on user criteria
			// & pull Strips out of StripScenes
			int stripCount = 0; // strips between EODs
			int pageCount = 0;	// pages since beginning
			int nextPageBreak = breakEvery; // next EOD point if by pages

			for (StripScene s : stripScenes) {
				int stripLimit = breakEvery;
				if (remainder > 0) {
					stripLimit++;
				}
				if ((breakBy.equals(BREAK_STRIPS) && (stripCount >= stripLimit)) ||
						(breakBy.equals(BREAK_PAGES) && (pageCount >= nextPageBreak))) {
					// time to add an End-of-Day strip
					order += StripDAO.ORDER_INCREMENT;
					strips.add(new Strip(order, StripType.END_OF_DAY, StripStatus.SCHEDULED, stripboard, unit));
					log.debug("EOD @ " + order);
					stripCount = 0;
					nextPageBreak += breakEvery;
					if (remainder > 0) {
						nextPageBreak++;
						remainder--;
					}
					log.debug("rem=" + remainder + ", pgCount=" + pageCount + ", nextPgBrk=" + nextPageBreak);
				}
				stripCount++;
				pageCount += s.getStrip().getLength();
				order += StripDAO.ORDER_INCREMENT;
				s.getStrip().setOrderNumber(order);
				strips.add(s.getStrip());
			}

			order += StripDAO.ORDER_INCREMENT;
			strips.add(new Strip(order, StripType.END_OF_DAY, StripStatus.SCHEDULED, stripboard, unit));
			log.debug("EOD @ " + order);

			// Add end-of-stripboard Strips
			//strips.add(stripboardDAO.createScheduledEndStrip(stripboard, unit));

			// Add all the strips that were unchanged (omitted, other units, and end-stripboard)
			strips.addAll(ignoredStrips);

			// Set last-updated values to current user and current date/time
			stripboard.setLastSaved(new Date());
			stripboard.setUser(SessionUtils.getCurrentUser());
			// Update database
			stripboardDAO.replaceStrips(stripboard, strips, deletedStrips);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}

	}

	private StripScene createStripscene(Strip strip) {
		Scene scn = null;
		StripScene stripScene = null;
		scn = SceneDAO.getInstance().findByStripAndScript(strip, getScript());
		if (scn != null) {
			// (do NOT create bean for Breakdown strips which have no Scene)
			stripScene = new StripScene();
			stripScene.setStrip(strip);
			stripScene.setSeqNum(scn.getSequence());
			stripScene.setIntExtType(scn.getIeType());
			stripScene.setDayNType(scn.getDnType());
			if (scn.getScriptElement() != null) {
				stripScene.setName(scn.getScriptElement().getName());
				stripScene.setLocationId(scn.getScriptElement().getId());
			}
			else {
				stripScene.setName(Constants.NO_SET_NAME);
			}
		}
		return stripScene;
	}

//	/**
//	 * ValueChangeListener method for the sort key #1 drop-down.
//	 */
//	public void changeKey1(ValueChangeEvent evt) {
//		log.debug(evt.getNewValue());
//	}
//	/**
//	 * ValueChangeListener method for the sort key #2 drop-down.
//	 */
//	public void changeKey2(ValueChangeEvent evt) {
//		log.debug(evt.getNewValue());
//	}
//	/**
//	 * ValueChangeListener method for the sort key #3 drop-down.
//	 */
//	public void changeKey3(ValueChangeEvent evt) {
//		log.debug(evt.getNewValue());
//	}

//	/**
//	 * ValueChangeListener method for the 'Day Break' type
//	 * drop-down (pages or strips).
//	 */
//	public void changeBreakType(ValueChangeEvent evt) {
//		log.debug(evt.getNewValue());
//	}

	protected Comparator<StripScene> getComparator() {
		Comparator<StripScene> comparator = new Comparator<StripScene>() {
			@Override
			public int compare(StripScene s1, StripScene s2) {
				int ret = 0;
				int comps[] = new int[4];
				// compute the compare value (-1/0/1) for each possible sort field
				comps[SORT_NONE_IX] = 0;
				comps[SORT_INTEXT_IX] = NumberUtils.compare(s1.getIntExtType().getSortOrder(), s2.getIntExtType().getSortOrder());
				comps[SORT_DAYNITE_IX] = NumberUtils.compare(s1.getDayNType().getSortOrder(),s2.getDayNType().getSortOrder());
				comps[SORT_SET_IX] = StringUtils.compare(s1.getName(), s2.getName());
				ret = comps[sortType1]; // check first key chosen by user
				if (ret == 0) {
					ret = comps[sortType2]; // check 2nd key chosen by user
					if (ret == 0) {
						ret = comps[sortType3]; // check 3rd key chosen by user
						if (ret == 0) {
							// all keys equal, use scene/script ordering
							ret = NumberUtils.compare(s1.getSeqNum(), s2.getSeqNum());
						}
						else if (! sortAsc3) {
							ret = - ret; // invert return if descending order requested for 3rd key
						}
					}
					else if (! sortAsc2) {
						ret = - ret; // invert return if descending order requested for 2nd key
					}
				}
				else if (! sortAsc1) {
					ret = - ret; // invert return if descending order requested for 1st key
				}
				return ret;
			}
		};
		return comparator;
	}

	// Accessors & Mutators

	/** See {@link #script}. */
	public Script getScript() {
		return script;
	}
	/** See {@link #script}. */
	public void setScript(Script script) {
		this.script = script;
	}

	/** See {@link #stripboard}. */
	public Stripboard getStripboard() {
		return stripboard;
	}
	/** See {@link #stripboard}. */
	public void setStripboard(Stripboard stripboard) {
		this.stripboard = stripboard;
	}

	/** See {@link #unit}. */
	public Unit getUnit() {
		return unit;
	}
	/** See {@link #unit}. */
	public void setUnit(Unit unit) {
		this.unit = unit;
	}

	/** See {@link #showDialog}. */
	public boolean getShowDialog() {
		return showDialog;
	}
	/** See {@link #showDialog}. */
	public void setShowDialog(boolean showOptions) {
		this.showDialog = showOptions;
	}

	/** See {@link #sortKey1}. */
	public String getSortKey1() {
		return sortKey1;
	}
	/** See {@link #sortKey1}. */
	public void setSortKey1(String sortKey1) {
		this.sortKey1 = sortKey1;
	}

	/** See {@link #sortKey2}. */
	public String getSortKey2() {
		return sortKey2;
	}
	/** See {@link #sortKey2}. */
	public void setSortKey2(String sortKey2) {
		this.sortKey2 = sortKey2;
	}

	/** See {@link #sortKey3}. */
	public String getSortKey3() {
		return sortKey3;
	}
	/** See {@link #sortKey3}. */
	public void setSortKey3(String sortKey3) {
		this.sortKey3 = sortKey3;
	}

	/** See {@link #sortOrder1}. */
	public String getSortOrder1() {
		return sortOrder1;
	}
	/** See {@link #sortOrder1}. */
	public void setSortOrder1(String sortOrder1) {
		this.sortOrder1 = sortOrder1;
	}

	/** See {@link #sortOrder2}. */
	public String getSortOrder2() {
		return sortOrder2;
	}
	/** See {@link #sortOrder2}. */
	public void setSortOrder2(String sortOrder2) {
		this.sortOrder2 = sortOrder2;
	}

	/** See {@link #sortOrder3}. */
	public String getSortOrder3() {
		return sortOrder3;
	}
	/** See {@link #sortOrder3}. */
	public void setSortOrder3(String sortOrder3) {
		this.sortOrder3 = sortOrder3;
	}

	/** See {@link #breakOption}. */
	public String getBreakOption() {
		return breakOption;
	}
	/** See {@link #breakOption}. */
	public void setBreakOption(String breakOption) {
		this.breakOption = breakOption;
	}

	/** See {@link #breakType}. */
	public String getBreakType() {
		return breakType;
	}
	/** See {@link #breakType}. */
	public void setBreakType(String breakType) {
		this.breakType = breakType;
	}

	/** See {@link #breakCount}. */
	public int getBreakCount() {
		return breakCount;
	}
	/** See {@link #breakCount}. */
	public void setBreakCount(int breakCount) {
		this.breakCount = breakCount;
	}

	/** See {@link #spreadCount}. */
	public int getSpreadCount() {
		return spreadCount;
	}
	/** See {@link #spreadCount}. */
	public void setSpreadCount(int spreadCount) {
		this.spreadCount = spreadCount;
	}

	/** See {@link #sortOptionDL}. */
	public List<SelectItem> getSortOptionDL() {
		return sortOptionDL;
	}
	/** See {@link #sortOptionDL}. */
	public void setSortOptionDL(List<SelectItem> sortOptionDL) {
		this.sortOptionDL = sortOptionDL;
	}

	/** See {@link #breakTypeDL}. */
	public List<SelectItem> getBreakTypeDL() {
		return breakTypeDL;
	}
	/** See {@link #breakTypeDL}. */
	public void setBreakTypeDL(List<SelectItem> breakTypeDL) {
		this.breakTypeDL = breakTypeDL;
	}

}
