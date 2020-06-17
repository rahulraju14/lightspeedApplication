package com.lightspeedeps.web.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

import com.lightspeedeps.dao.RoleDAO;
import com.lightspeedeps.model.Role;

/**
 * A custom JSF converter for converting Role objects
 * to and from their IDs for select-list usage.
 *
 */
@FacesConverter(value="lightspeed.RoleConverter")
public class RoleConverter extends DbIdConverter<Role> implements Converter {

	@Override
	public Object getAsObject(FacesContext arg0, UIComponent arg1, String value) {
		return getAsObject(value, RoleDAO.getInstance());
	}

	/**
	 * @see com.lightspeedeps.web.converter.DbIdConverter#create(java.lang.Integer)
	 */
	@Override
	protected Role create(Integer id) {
		return new Role(id);
	}

	@Override
	public String getAsString(FacesContext arg0, UIComponent arg1, Object value) {
		return getAsString(value);
	}

}
