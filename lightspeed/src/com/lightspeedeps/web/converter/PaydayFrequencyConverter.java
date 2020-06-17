/**
 * File: PaydayFrequencyConverter.java
 */
package com.lightspeedeps.web.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;

import com.lightspeedeps.type.PaydayFrequency;

/**
 * A JSF-style converter for the PaydayFrequency enumerated type. This is
 * necessary so that drop-downs or radio buttons which use instances of
 * PaydayFrequency names work properly.
 */
@FacesConverter( value="lightspeed.PaydayFrequencyConverter")
public class PaydayFrequencyConverter extends EnumConverter<PaydayFrequency> implements javax.faces.convert.Converter{

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
 		return getAsObject(value, PaydayFrequency.class);
 	}

 	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
 		return getAsString(value);
 	}

}
