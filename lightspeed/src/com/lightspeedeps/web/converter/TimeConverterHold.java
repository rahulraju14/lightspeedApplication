package com.lightspeedeps.web.converter;

import javax.faces.convert.FacesConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.type.WorkdayType;

/**
 * This class provides a slight modification to our TimeConverterOC class --
 * it treats null values as "H" (the normal "Hold" indicator) and vice-versa.
 * <p/>
 * Since it is built on our DateTimeConverter, it still allows the same variety
 * of time inputs, such as 'a' for am and 'p' for pm, or omitting am/pm
 * entirely.
 */
@FacesConverter(value="lightspeed.TimeConverterHold")
public class TimeConverterHold extends TimeConverterOC {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(TimeConverterHold.class);

	@Override
	protected String getNullValue() {
		return WorkdayType.HOLD.asWorkStatus();
	}

}
