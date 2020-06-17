/**
 * File: AccessStatusConverter.java
 */
package com.lightspeedeps.web.converter;

import com.lightspeedeps.type.AccessStatus;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;

/**
 * A JSF-style converter for the AccessStatus enumerated type.
 */
@FacesConverter(forClass=com.lightspeedeps.type.AccessStatus.class)
public class AccessStatusConverter extends EnumConverter<AccessStatus> implements javax.faces.convert.Converter {

 	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
 		return getAsObject(value, AccessStatus.class);
 	}

 	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
 		return getAsString(value);
 	}

 }
