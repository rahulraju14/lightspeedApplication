/**
 * File: WatermarkPreferenceConverter.java
 */
package com.lightspeedeps.web.converter;

import com.lightspeedeps.type.WatermarkPreference;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;


/**
 * A JSF-style converter for the WatermarkPreference enumerated type. This is
 * necessary so that drop-downs (for edit mode) which have lists of
 * WatermarkPreference labels work properly.
 */
@FacesConverter(forClass=com.lightspeedeps.type.WatermarkPreference.class)
public class WatermarkPreferenceConverter extends EnumConverter<WatermarkPreference> implements javax.faces.convert.Converter {

 	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
 		return getAsObject(value, WatermarkPreference.class);
 	}

 	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
 		return getAsString(value);
 	}

 }
