/**
 * File: OrderStatusConverter.java
 */
package com.lightspeedeps.web.converter;

import com.lightspeedeps.type.OrderStatus;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;

/**
 * A JSF-style converter for the OrderStatus enumerated type. This is
 * necessary so that drop-downs (for edit mode) which have lists of
 * OrderStatus labels work properly.
 */
@FacesConverter(forClass=com.lightspeedeps.type.OrderStatus.class)
public class OrderStatusConverter extends EnumConverter<OrderStatus> implements javax.faces.convert.Converter {

 	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
 		return getAsObject(value, OrderStatus.class);
 	}

 	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
 		return getAsString(value);
 	}

 }
