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
 * Integer (number of seconds) to "h:mm:ss" format for entry or display.
 *
 */
@FacesConverter(value="lightspeed.SecondsConverter")
public class SecondsConverter implements Converter {
	private static final Log log = LogFactory.getLog(SecondsConverter.class);

	private static final Pattern TIME_PATTERN = Pattern.compile("(([0-9]*:)?([0-9]*:))?([0-9]*)");

	/**
	 * Called to convert output String back to internal Integer value (number of
	 * seconds).  Accepts input as "h:mm:ss" or "mm:ss" or "ss".
	 *
	 * @see javax.faces.convert.Converter#getAsObject(javax.faces.context.FacesContext,
	 *      javax.faces.component.UIComponent, java.lang.String)
	 */
	@Override
	public Object getAsObject(FacesContext arg0, UIComponent arg1, String arg2) {
		int iSeconds = 0;
		int iMinutes = 0;
		int iHours = 0;
		if (arg2 == null) {
			return null;
		}
		arg2 = arg2.trim();
		if (arg2.length() == 0) {
			return 0;
		}
		String ss, sh, sm;
		try {
			Matcher match = TIME_PATTERN.matcher(arg2);
			if ( arg2.equals(":") || ! match.matches()) {
				// the pattern match allows ":", but we don't want that!
				throw new NumberFormatException();
			}
			sh = match.group(2);
			if (sh != null && sh.length() > 1) {
				iHours = Integer.parseInt(sh.substring(0, sh.length()-1));
			}
			sm = match.group(3);
			if (sm != null && sm.length() > 1) {
				iMinutes = Integer.parseInt(sm.substring(0, sm.length()-1));
			}
			ss = match.group(4);
			if (ss != null && ss.length() > 0) {
				iSeconds = Integer.parseInt(ss);
			}
		}
		catch (NumberFormatException e) {
			if (arg0 != null) {
				HeaderViewBean.getInstance().setMsgExists(true);
				String msg = MsgUtils.formatMessage("javax.faces.converter.DateTimeConverter.DATE", arg2);
				throw new ConverterException(new FacesMessage(FacesMessage.SEVERITY_ERROR,msg,msg));
			}
			else { // should only happen in JUnit (test) environment
				System.out.println("Illegal input or number format exception: " + arg2);
				return -1;	// our JUnit test class checks for this
			}
		}
//		System.out.println(ss + "=" + iSeconds);
//		System.out.println(sm + "=" + iMinutes);
//		System.out.println(sh + "=" + iHours);
		iSeconds += 60 * (iMinutes + (60 * iHours));

		return (Integer)iSeconds;
	}

	/**
	 * Called to convert internal Integer value (number of seconds) to String
	 * formatted as h:mm:ss for display.
	 *
	 * @see javax.faces.convert.Converter#getAsString(javax.faces.context.FacesContext,
	 *      javax.faces.component.UIComponent, java.lang.Object)
	 */
	@Override
	public String getAsString(FacesContext arg0, UIComponent arg1, Object arg2) {
		String s = "";
		int seconds = 0;
		if (arg2 instanceof Integer) {
			seconds = (Integer)arg2;
		}
		else if (arg2 instanceof Long) {
			seconds = ((Long)arg2).intValue();
		}
		else {
			if (arg2 != null) {
				log.error("unable to convert: " + arg2.toString());
			}
			return "";
		}
		if (seconds < 0) {
			return "#:##:##";
		}
		int minutes = seconds / 60;
		int hours = minutes / 60;
		seconds = seconds % 60;
		minutes = minutes % 60;
		s = "0" + seconds;
		s = s.substring(s.length()-2);
		s = "0" + minutes + ":" + s;
		s = s.substring(s.length()-5);
		s = hours + ":" + s;
		return s;
	}

}
