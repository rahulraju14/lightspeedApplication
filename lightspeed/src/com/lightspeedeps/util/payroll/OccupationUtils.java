/**
 * File: OccupationUtils.java
 */
package com.lightspeedeps.util.payroll;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lightspeedeps.dao.GsOccCodeCrossRefDAO;
import com.lightspeedeps.model.GsOccCodeCrossRef;

/**
 * A class for static utility functions related to Occupations, Occ codes, etc.
 */
public class OccupationUtils {

	/** A mapping from Lightspeed occupation codes to AS/400 occupation codes. */
	private static Map<String,String> occCodeMap;

	/**
	 * Map a Lightspeed occupation code to an AS/400 occupation code.
	 *
	 * @param occCode The Lightspeed occupation code
	 * @return The equivalent AS/400 ("green screen") occupation code; if no
	 *         mapping is found, then the given (argument) value is returned.
	 */
	public static String mapOccCode(String occCode) {
		if (occCodeMap == null) {
			occCodeMap = createOccCodeMap();
		}
		String mappedCode = occCodeMap.get(occCode);
		if (mappedCode == null) {
			mappedCode = occCode;
		}
		return mappedCode;
	}

	/**
	 * @return A Map for mapping Lightspeed Occ Codes to AS400 (Team) Occ Codes.
	 */
	private static Map<String, String> createOccCodeMap() {
		Map<String,String> map = new HashMap<>();
		List<GsOccCodeCrossRef> occList = GsOccCodeCrossRefDAO.getInstance().findAll();
		for (GsOccCodeCrossRef occ : occList) {
			map.put(occ.getOccCode(), occ.getGsOccCode());
		}
		return map;
	}

}
