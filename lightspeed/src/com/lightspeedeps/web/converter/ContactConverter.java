package com.lightspeedeps.web.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

import com.lightspeedeps.dao.ContactDAO;
import com.lightspeedeps.model.Contact;

/**
 * A custom JSF converter for converting Contact objects
 * to and from their IDs for select-list usage.
 *
 */
@FacesConverter(value="lightspeed.ContactConverter")
public class ContactConverter extends DbIdConverter<Contact> implements Converter {
	@Override
	public Object getAsObject(FacesContext arg0, UIComponent arg1, String value) {
		return this.getAsObject(value, ContactDAO.getInstance());
	}

	@Override
	public String getAsString(FacesContext arg0, UIComponent arg1, Object value) {
		return getAsString(value);
	}

}
