package com.lightspeedeps.web.converter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.BaseDAO;
import com.lightspeedeps.model.PersistentObject;


/**
 * A custom JSP converter for converting any object that is
 * displayed in a list using their database id field as the
 * selection value.
 *
 * *NOTE* that the class of the object being converted MUST
 * HAVE an equals() method.  The JSP framework checks for
 * equality between the result of the conversion and one of
 * the items in the list.
 */
public class DbIdConverter<T extends PersistentObject<T>> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(DbIdConverter.class);

	@SuppressWarnings("unchecked")
	public Object getAsObject(String value, BaseDAO dao) {
		if (value == null) {
			return null;
		}
		T dbObject;
		Integer id = new Integer(value);
		if (id.intValue() == -1) {
			dbObject = null;
		}
		else if (id.intValue() < 0) { // used for some special cases, like Role
			dbObject = create(id);
		}
		else {
			dbObject = (T)dao.findByIdBase(id);
		}
		//log.debug("in=" + value + ", out=" + dbObject);
		return dbObject;
	}

	@SuppressWarnings("unchecked")
	public String getAsString(Object value) {
		String str;
		T dbObject = null;
		try {
			dbObject = (T)value;
		}
		catch (ClassCastException e) {
			// ignore -- we'll check for null object value later.
		}
		if (dbObject == null || dbObject.getId() == null) {
			str = "-1";
		}
		else {
			str = dbObject.getId().toString();
		}
		//log.debug("in=" + value + ", out=" + str);
		return str;
	}

	protected T create(Integer id) {
		return null;
	}

}
