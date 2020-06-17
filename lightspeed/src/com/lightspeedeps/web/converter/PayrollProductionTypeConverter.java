/**
 * File: PayrollProductionTypeConverter.java
 */
package com.lightspeedeps.web.converter;

import com.lightspeedeps.type.PayrollProductionType;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;

/**
 * A JSF-style converter for the PayrollProductionType enumerated type. This is
 * necessary so that drop-downs (for edit mode) which have lists of
 * PayrollProductionType labels work properly.
 */
@FacesConverter(forClass=com.lightspeedeps.type.PayrollProductionType.class)
public class PayrollProductionTypeConverter extends EnumConverter<PayrollProductionType> implements javax.faces.convert.Converter {

 	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
 		return getAsObject(value, PayrollProductionType.class);
 	}

 	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
 		return getAsString(value);
	}

 }
