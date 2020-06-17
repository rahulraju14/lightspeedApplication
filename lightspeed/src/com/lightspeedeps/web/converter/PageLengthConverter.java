package com.lightspeedeps.web.converter;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Scene;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.web.util.HeaderViewBean;

/**
 * A custom JSF converter for converting page length fields between Integer
 * (number of 1/8th's) and string value ("n m/8") for edit and display.
 */
@FacesConverter(value="lightspeed.PageLengthConverter")
public class PageLengthConverter implements Converter {
	private static final Log log = LogFactory.getLog(PageLengthConverter.class);

	/** Convert String format to number of 1/8ths of a page. */
	@Override
	public Object getAsObject(FacesContext arg0, UIComponent arg1, String arg2) {
		if (arg2 == null) {
			return null;
		}
		int len = 0;
		try {
			len = Scene.convertPageLength(arg2);
			if (len == 0 && ! arg2.trim().equals("0")) {
				throw new NumberFormatException();
			}
		}
		catch (NumberFormatException e) {
			HeaderViewBean.getInstance().setMsgExists(true);
			String msg = MsgUtils.formatMessage("Convert.PageLength", arg2);
			throw new ConverterException(new FacesMessage(FacesMessage.SEVERITY_ERROR,msg,msg));
		}
		return (Integer)len;
	}

	/** Convert Integer -- number of 1/8ths of a page -- to pretty display format. */
	@Override
	public String getAsString(FacesContext arg0, UIComponent arg1, Object arg2) {
		String s = "";
		int number = 0;
		if (arg2 instanceof Integer) {
			number = (Integer)arg2;
		}
		else if (arg2 instanceof Long) {
			number = ((Long)arg2).intValue();
		}
		else {
			if (arg2 != null) {
				log.error("unable to convert: " + arg2.toString());
			}
			return "";
		}
		s = Scene.formatPageLength(number);
		return s;
	}

}
