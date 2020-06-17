/**
 * File: ProductionTypeConverter.java
 */
package com.lightspeedeps.web.converter;

import com.lightspeedeps.type.ProductionType;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;

/**
 * A JSF-style converter for the ProductionType enumerated type. This is
 * necessary so that drop-downs (for edit mode) which have lists of
 * ProductionType labels work properly.
 */
@FacesConverter(forClass=com.lightspeedeps.type.ProductionType.class)
public class ProductionTypeConverter extends EnumConverter<ProductionType> implements javax.faces.convert.Converter {

 	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
 		return getAsObject(value, ProductionType.class);
 	}

 	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
 		return getAsString(value);
 	}

 }
