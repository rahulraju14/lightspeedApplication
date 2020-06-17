/**
 * File: DayTypeConverter.java
 */
package com.lightspeedeps.web.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

import com.lightspeedeps.dao.OfficeDAO;
import com.lightspeedeps.model.Office;


/**
 * A JSF-style converter for the Office. This is
 * necessary so that drop-downs (for edit mode) which have lists of
 * Day Type labels work properly.
 */
@FacesConverter(value="lightspeed.OfficeConverter")
public class OfficeConverter extends DbIdConverter<Office> implements javax.faces.convert.Converter{

    @Override
    public Object getAsObject(FacesContext arg0, UIComponent arg1, String arg2)
            throws ConverterException {
        return getAsObject(arg2, OfficeDAO.getInstance());
    }

    @Override
    public String getAsString(FacesContext arg0, UIComponent arg1, Object arg2)
            throws ConverterException {
        return getAsString(arg2);
    }

}
