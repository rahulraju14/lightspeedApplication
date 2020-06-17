package com.lightspeedeps.web.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.object.ContactRole;

/**
 * A custom JSF converter for converting ContactRole objects
 * to and from their equivalent strings for select-list usage.
 */
@FacesConverter(value="lightspeed.ContactRoleConverter")
public class ContactRoleConverter implements Converter {
	//@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(RoleConverter.class);

	@Override
	public Object getAsObject(FacesContext arg0, UIComponent arg1, String arg2) {
		ContactRole contactRole = null;
		if (arg2 == null) {
			return null;
		}
		String parts[] = arg2.split("\\|");
		Integer id = new Integer(parts[0]);
		if (id.intValue() >= 0) {
			contactRole = new ContactRole(id, parts[1]);
		}
		log.debug("in=" + arg2 + ", out=" + contactRole);
		return contactRole;
	}

	@Override
	public String getAsString(FacesContext arg0, UIComponent arg1, Object arg2) {
		String s;
		ContactRole contactRole = (ContactRole)arg2;
		if (contactRole == null || contactRole.getContactId() == null) {
			s = "-1| ";
		}
		else {
			s = contactRole.getContactId().toString() + "|" + contactRole.getRoleName();
		}
		log.debug("in=" + arg2 + ", out=" + s);
		return s;
	}

}
