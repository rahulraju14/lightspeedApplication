/**
 * File: RealLinkStatusConverter.java
 */
package com.lightspeedeps.web.converter;

import com.lightspeedeps.type.RealLinkStatus;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;


/**
 * A JSF-style converter for the RealLinkStatus enumerated type. This is
 * necessary so that drop-downs which have lists of the enumerated type work properly.
 */
@FacesConverter(forClass=com.lightspeedeps.type.RealLinkStatus.class)
public class RealLinkStatusConverter extends EnumConverter<RealLinkStatus> implements javax.faces.convert.Converter {

 	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
 		return getAsObject(value, RealLinkStatus.class);
 	}

 	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
 		return getAsString(value);
 	}

 }
