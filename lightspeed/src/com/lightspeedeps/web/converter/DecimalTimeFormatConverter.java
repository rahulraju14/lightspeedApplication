package com.lightspeedeps.web.converter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.common.CalendarUtils;

@FacesConverter(value="lightspeed.DecimalTimeFormatConverter")
public class DecimalTimeFormatConverter extends DecimalTimeConverter {

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
		Object obj = super.getAsObject(context, component, value);
		return obj;
	}

	/**
	 * Break the value into hour and minutes and format the output.
	 */
	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) throws ConverterException {
		if(value == null) {
			return "";
		}

		BigDecimal hoursMin;
		int hr,	mins;

		hoursMin = (BigDecimal)value;
		BigDecimal [] timeParts = hoursMin.divideAndRemainder(BigDecimal.ONE);

		hr = timeParts[0].intValue();
		mins = timeParts[1].multiply(Constants.MINUTES_PER_HOUR).setScale(0, RoundingMode.HALF_UP).intValue();
		String formattedTime = CalendarUtils.formatDate(CalendarUtils.setTime(new Date(), hr, mins), "h:mm a");

		return formattedTime;
	}

	/**
	 * @return The default input and output pattern for this converter. May be
	 *         overridden by subclasses to change the default pattern.
	 */
	@Override
	protected String getDefaultPattern() {
		return "h:mma";
	}

}
