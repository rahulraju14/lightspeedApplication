/**
 * File: PaidAsConverter.java
 */
package com.lightspeedeps.web.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

import com.lightspeedeps.type.PaidAsType;

/**
 * A JSF-style converter for the GenderType enumerated type. This is
 * necessary so that drop-downs (for edit mode) which have lists of
 * GenderType labels work properly. LS-2562
 */
@FacesConverter(value = "lightspeed.PaidAsConverter")
public class PaidAsConverter extends EnumConverter<PaidAsType> implements javax.faces.convert.Converter {

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value)
			throws ConverterException {
		return getAsObject(value, PaidAsType.class);
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value)
			throws ConverterException {
		return getAsString(value);
	}
}
