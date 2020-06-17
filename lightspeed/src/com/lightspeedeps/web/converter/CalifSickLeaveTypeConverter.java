/**
 * File: CalifSickLeaveTypeConverter.java
 */
package com.lightspeedeps.web.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;

import com.lightspeedeps.type.CalifSickLeaveType;

/**
 * A JSF-style converter for the CalifSickLeaveType enumerated type. This is
 * necessary so that drop-downs or radio buttons which use instances of
 * CalifSickLeaveType names work properly.
 */
@FacesConverter( value="lightspeed.CalifSickLeaveTypeConverter")
public class CalifSickLeaveTypeConverter extends EnumConverter<CalifSickLeaveType>  implements javax.faces.convert.Converter {

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
 		return getAsObject(value, CalifSickLeaveType.class);
 	}

 	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
 		return getAsString(value);
 	}

}
