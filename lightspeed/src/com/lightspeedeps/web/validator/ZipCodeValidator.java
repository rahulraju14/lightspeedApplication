package com.lightspeedeps.web.validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

/**
 * JSF validator for 5-digit zip codes.  Not currently used.
 */
public class ZipCodeValidator implements Validator {

	/** Creates a new instance of ZipCode Validator */
	public ZipCodeValidator() {
	}

	/** ZipCode */
	private static final String ZipCode = "[0-9]{5}";

	@Override
	public void validate(FacesContext context, UIComponent component,
			Object value) {
		/*
		 * create a mask
		 */
		Pattern mask = Pattern.compile(ZipCode);

		/* retrieve the string value of the field */
		String number = (String) value;

		/* ensure value is a ZipCode */
		Matcher matcher = mask.matcher(number);

		if (!matcher.matches()) {
			FacesMessage message = new FacesMessage();
			message.setDetail("Invalid ZipCode.");
			message.setSummary("Invalid ZipCode.");
			message.setSeverity(FacesMessage.SEVERITY_ERROR);
			throw new ValidatorException(message);
		}

	}

}
