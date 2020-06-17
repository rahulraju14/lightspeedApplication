/**
 * File: DayTypeConverter.java
 */
package com.lightspeedeps.web.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;

import com.lightspeedeps.type.DayType;

/**
 * A JSF-style converter for the ProductionType enumerated type. This is
 * necessary so that drop-downs (for edit mode) which have lists of
 * Day Type labels work properly.
 */
@FacesConverter(forClass=com.lightspeedeps.type.DayType.class, value="lightspeed.DayTypeConverter")
public class DayTypeConverter  extends EnumConverter<DayType> implements javax.faces.convert.Converter{

 	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
 		return getAsObject(value, DayType.class);
 	}

 	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
 		return getAsString(value);
 	}

}
