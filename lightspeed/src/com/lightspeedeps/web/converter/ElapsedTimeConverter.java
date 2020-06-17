package com.lightspeedeps.web.converter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.web.util.HeaderViewBean;

/**
 * A custom JSF converter for converting elapsed times from
 * Integer (number of minutes) to "h:mm" format for input and display.
 *
 */
@FacesConverter(value="lightspeed.ElapsedTimeConverter")
public class ElapsedTimeConverter implements Converter {
	private static final Log log = LogFactory.getLog(ElapsedTimeConverter.class);

	private static final Pattern TIME_PATTERN = Pattern.compile("([0-9]*:)?[0-9]+");

	/**
	 * Called to convert a String formatted as "h:mm" back to an internal
	 * Integer value (number of minutes).
	 *
	 * @see javax.faces.convert.Converter#getAsObject(javax.faces.context.FacesContext,
	 *      javax.faces.component.UIComponent, java.lang.String)
	 */
	@Override
	public Object getAsObject(FacesContext arg0, UIComponent arg1, String arg2) {
		String strMinutes = null;
		String strHours = null;
		int iMinutes = 0;
		int iHours = 0;
		if (arg2 == null) {
			return null;
		}
		arg2 = arg2.trim();
		if (arg2.length() == 0) {
			return null; // treat empty string same as null
		}
		try {
			Matcher match = TIME_PATTERN.matcher(arg2);
			if ( ! match.matches()) {
				throw new NumberFormatException();
			}
			int i = arg2.indexOf(':');
			if (i >= 0) {
				strMinutes = arg2.substring(i+1).trim();
				if (i > 0) {
					strHours = arg2.substring(0, i).trim();
				}
			}
			else {
				strMinutes = arg2.trim();
			}
			if (strMinutes != null && strMinutes.length() > 0) {
				iMinutes = Integer.parseInt(strMinutes);
			}
			if (strHours != null && strHours.length() > 0) {
				if (strHours.charAt(0) == '-') {
					throw new NumberFormatException();
				}
				iHours = Integer.parseInt(strHours);
			}
			if (iHours < 0 || iMinutes < 0 ) {
				throw new NumberFormatException();
			}
		}
		catch (NumberFormatException e) {
			if (arg0 != null) {
				HeaderViewBean.getInstance().setMsgExists(true);
				String msg = MsgUtils.formatMessage("javax.faces.converter.DateTimeConverter.TIME", arg2, "2:15");
				throw new ConverterException(new FacesMessage(FacesMessage.SEVERITY_ERROR,msg,msg));
			}
			else { // should only happen in JUnit (test) environment
				System.out.println("Illegal input or number format exception: " + arg2);
				return -1;	// our JUnit test class checks for this
			}
		}
		iMinutes += 60 * iHours;
		return (Integer)iMinutes;
	}

	/**
	 * Called to convert internal Integer value (number of minutes) to String
	 * formatted as h:mm for display.
	 *
	 * @see javax.faces.convert.Converter#getAsString(javax.faces.context.FacesContext,
	 *      javax.faces.component.UIComponent, java.lang.Object)
	 */
	@Override
	public String getAsString(FacesContext arg0, UIComponent arg1, Object arg2) {
		String s = "";
		int minutes = 0;
		if (arg2 instanceof Integer) {
			minutes = (Integer)arg2;
		}
		else if (arg2 instanceof Long) {
			minutes = ((Long)arg2).intValue();
		}
		else {
			if (arg2 != null) {
				log.error("unable to convert: " + arg2.toString());
			}
			return "";
		}
		if (minutes < 0) {
			return "#:##";
		}
		int hours = minutes / 60;
		minutes = minutes % 60;
		s = "0" + minutes;
		s = s.substring(s.length()-2);
		s = hours + ":" + s;
		return s;
	}

}
