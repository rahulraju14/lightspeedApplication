/**
 * File: EnumConverter.java
 */
package com.lightspeedeps.web.converter;

import com.lightspeedeps.util.app.EventUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A super class for all of our JSF-style converters for enumerated types. Enum's
 * require a JSP converter so that drop-downs (for edit mode) which have lists
 * of the Enum's labels work properly.
 */
public class EnumConverter<T extends Enum<T>> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(EnumConverter.class);

	public Object getAsObject(String value, Class<T> claz) {
 		T result = null;
 		if (value != null && value.length() > 0) {
 			result = (T)T.valueOf(claz, value);
 		}
 		//log.debug("value=" + value + ", result=" + result);
 		return result;
 	}

	@SuppressWarnings("unchecked")
	public String getAsString(Object value) {
 		String result = null;
 		if (value != null) {
 			if (value instanceof Enum<?>) {
 				result = ((T)value).name();
 			}
 			else if (value instanceof String) {
 				result = (String)value;
 			}
 			else {
 				EventUtils.logError("unable to convert expected enum: " + value.getClass());
 			}
 		}
 		//log.debug("value=" + value + " [" + value.getClass() +  "], result=" + result);
 		return result;
 	}

 }
