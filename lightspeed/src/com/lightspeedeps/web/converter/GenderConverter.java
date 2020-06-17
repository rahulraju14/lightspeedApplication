/**
 * File: GenderConverter.java
 */
package com.lightspeedeps.web.converter;

import com.lightspeedeps.type.GenderType;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;

/**
 * A JSF-style converter for the GenderType enumerated type. This is
 * necessary so that drop-downs (for edit mode) which have lists of
 * GenderType labels work properly.
 */
@FacesConverter(value = "lightspeed.GenderConverter")
public class GenderConverter extends EnumConverter<GenderType> implements javax.faces.convert.Converter {

 	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
 		return getAsObject(value, GenderType.class);
 	}

 	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
 		return getAsString(value);
 	}

 }
