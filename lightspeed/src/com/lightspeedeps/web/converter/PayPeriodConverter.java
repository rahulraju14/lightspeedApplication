/**
 * File: PayPeriodConverter.java
 */
package com.lightspeedeps.web.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;

import com.lightspeedeps.type.PayPeriodType;

/**
 * A JSF-style converter for the PayPeriod enumerated type. This is necessary so
 * that drop-downs or radio buttons which use instances of PayPeriod names work
 * properly.
 */
@FacesConverter(value = "lightspeed.PayPeriodConverter")
public class PayPeriodConverter extends EnumConverter<PayPeriodType>
		implements javax.faces.convert.Converter {

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
		return getAsObject(value, PayPeriodType.class);
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
		return getAsString(value);
	}

}
