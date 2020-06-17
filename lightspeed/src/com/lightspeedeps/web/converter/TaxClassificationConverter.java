/**
 * File: TaxClassificationConverter.java
 */
package com.lightspeedeps.web.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

import com.lightspeedeps.type.TaxClassificationType;

/**
 * A custom JSF converter for converting ContactRole objects
 * to and from their equivalent strings for select-list usage.
 */
@FacesConverter(value="lightspeed.TaxClassificationConverter")
public class TaxClassificationConverter extends EnumConverter<TaxClassificationType> implements javax.faces.convert.Converter {
	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value)
			throws ConverterException {
		return getAsObject(value, TaxClassificationType.class);
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value)
			throws ConverterException {
		return getAsString(value);
	}
}
