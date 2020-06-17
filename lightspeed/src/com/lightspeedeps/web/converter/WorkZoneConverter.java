/**
 * File: WorkZoneConverter.java
 */
package com.lightspeedeps.web.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;

import com.lightspeedeps.type.WorkZone;

/**
 * A JSF-style converter for the ProductionType enumerated type. This is
 * necessary so that drop-downs (for edit mode) which have lists of
 * WorkZone labels work properly.
 */
@FacesConverter(forClass=com.lightspeedeps.type.WorkZone.class, value="lightspeed.WorkZoneConverter")
public class WorkZoneConverter  extends EnumConverter<WorkZone> implements javax.faces.convert.Converter{

 	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
 		return getAsObject(value, WorkZone.class);
 	}

 	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
 		return getAsString(value);
 	}
}
