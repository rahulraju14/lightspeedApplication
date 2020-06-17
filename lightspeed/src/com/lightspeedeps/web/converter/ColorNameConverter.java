package com.lightspeedeps.web.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

import com.lightspeedeps.dao.ColorNameDAO;
import com.lightspeedeps.model.ColorName;

/**
 * A custom JSF converter for converting ColorName objects
 * to and from their IDs for select-list usage.
 *
 * *NOTE* that the class of the object being converted MUST
 * HAVE an equals() method.  The JSF framework checks for
 * equality between the result of the conversion and one of
 * the items in the list.
 */
@FacesConverter(value="lightspeed.ColorNameConverter")
public class ColorNameConverter extends DbIdConverter<ColorName> implements Converter {

	@Override
	public Object getAsObject(FacesContext arg0, UIComponent arg1, String value) {
		return getAsObject(value, ColorNameDAO.getInstance());
	}

	@Override
	public String getAsString(FacesContext arg0, UIComponent arg1, Object value) {
		return getAsString(value);
	}

}
