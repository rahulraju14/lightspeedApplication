/**
 * File: TimecardSubmitTypeConverter.java
 */
package com.lightspeedeps.web.converter;

import com.lightspeedeps.type.TimecardSubmitType;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;


/**
 * A JSF-style converter for the TimecardSubmitType enumerated type. This is
 * necessary so that drop-downs which have lists of the enumerated type work properly.
 */
@FacesConverter(forClass=com.lightspeedeps.type.TimecardSubmitType.class)
public class TimecardSubmitTypeConverter extends EnumConverter<TimecardSubmitType> implements javax.faces.convert.Converter {

 	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
 		return getAsObject(value, TimecardSubmitType.class);
 	}

 	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
 		return getAsString(value);
 	}

 }
