package com.lightspeedeps.util.script;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.StripColorDAO;
import com.lightspeedeps.dao.StripboardDAO;
import com.lightspeedeps.model.*;
import com.lightspeedeps.type.DayNightType;
import com.lightspeedeps.type.IntExtType;
import com.lightspeedeps.util.app.EventUtils;

public class StripUtils {
	private static final Log log = LogFactory.getLog(StripUtils.class);

	/**
	 * Maps a key created from the Int/Ext and Day/Night values of a Scene or Strip
	 * to the corresponding StripColor object, which defines the text and background
	 * colors for that Strip.  The entries are loaded as needed from the StripColor
	 * table, to eliminate further database access. (See getStripColor().)
	 */
	private static Map<String, StripColor> colorMap; // shared across entire application
	/**
	 * Maps a key created from the Int/Ext and Day/Night values of a Scene or Strip
	 * to the corresponding "xNNN" CSS class name, which defines the text and background
	 * colors for that Strip. (See getStripColor().)
	 */
	private static Map<String, String> colorClassMap; // shared across entire application

	// Our static initializer; load the maps on first reference to our class.
	static {
		loadColors();
	}

	private StripUtils() {
	}

	/**
	 * Called to load the colorClassMap with the class value for the
	 * given Scene.
	 * @param scene
	 */
//	public static void loadStripColor(Scene scene) {
//		getStripColor(scene.getIeType(), scene.getDnType());
//	}

	/**
	 * Find the StripColor that corresponds to the given combination of Int/ext and
	 * day/night values.  It will first fetch the color from a static map; if the
	 * entry is not there, it will get it from the database and add it to the map.
	 * @param ieType
	 * @param dnType
	 * @return The matching StripColor, or StripColorDAO.NORMAL_COLOR if not found.
	 */
	public static StripColor getStripColor( IntExtType ieType, DayNightType dnType) {
		String key = ieType.name() + "/" + dnType.name(); // matches Scene.getColorKey()
		StripColor stripColor = getColorMap().get(key);
		if (stripColor == null) {
			stripColor = StripColorDAO.getInstance().findByIeTypeDnType(ieType, dnType);
			//log.debug("ie=" + ieType + ", dn=" + dnType + ", color=" + stripColor);
			if (stripColor == null) {
				EventUtils.logError("MISSING STRIP COLOR IN DATABASE: ie=" + ieType + ", dn=" + dnType);
				stripColor = StripColorDAO.NORMAL_COLOR;
			}
			else {
				colorMap.put(key, stripColor);
				colorClassMap.put(key, "x" + stripColor.getId());
			}
		}
		else {
			//log.debug("color from map: ie=" + ieType + ", dn=" + dnType + ", color=" + stripColor);
		}
		return stripColor;
	}

	private static void loadColors() {
		log.debug("");
		colorMap = new HashMap<String, StripColor>();
		colorClassMap = new HashMap<String, String>();
		List<StripColor> list = StripColorDAO.getInstance().findAll();
		for (StripColor stripColor : list) {
			String key = stripColor.getIeType().name() + "/" + stripColor.getDnType().name(); // matches Scene.getColorKey()
			colorMap.put(key, stripColor);
			colorClassMap.put(key, "x" + stripColor.getId());
		}
	}

	public static String getColorAsHex( Integer color ) {
		String s = Integer.toHexString(color);
		switch (s.length()) {
			case 2:
				s = "0000" + s;
				break;
			case 4:
				s = "00" + s;
		}
		s = "#" + s;
		return s;
	}

	/** See {@link #colorMap}. */
	public static Map<String, StripColor> getColorMap() {
		return colorMap;
	}

	/** See {@link #colorClassMap}. */
	public static Map<String, String> getColorClassMap() {
		return colorClassMap;
	}


	/**
	 * Create a mapping of Scene.number to Strip.id for each Scene in the
	 * Script, using the current stripboard.
	 *
	 * @return Map of Scene.number to Strip.id for the current Script. If there
	 *         is no Stripboard in the given project, returns null.
	 */
	public static Map<String,Integer> createBreakdownMap(Project project) {
		Stripboard stripboard = project.getStripboard();
		if (stripboard == null) {
			return null;
		}
		stripboard = StripboardDAO.getInstance().refresh(stripboard);
		Set<Strip> strips = stripboard.getStrips();
		Map<String, Integer> breakdownMap = new HashMap<String,Integer>(strips.size());
		for (Strip strip : strips) {
			List<String> sceneNums = strip.getScenes(); // List of scene numbers for this strip
			for (String sceneNum : sceneNums) {
				breakdownMap.put(sceneNum, strip.getId());
			}
		}
		return breakdownMap;
	}

}
