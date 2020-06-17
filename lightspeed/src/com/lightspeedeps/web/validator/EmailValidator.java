package com.lightspeedeps.web.validator;

import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import com.lightspeedeps.util.app.SessionUtils;

/**
 * A class with static methods to validate email addresses or portions of email
 * addresses. Could also be used as a JSF validator, although it is not
 * currently annotated for that purpose.
 */
public class EmailValidator implements Validator {
	private static final Log log = LogFactory.getLog(EmailValidator.class);

	/** Creates a new instance of Email Validator */
	public EmailValidator() {
	}

	/** email pattern: name@something.tld */
	private static final String EMAIL_ID = "(.+)@([a-z0-9]+[-a-z0-9\\.]*\\.[a-z]+)";

	/** create a mask */
	private static final Pattern mask = Pattern.compile(EMAIL_ID);

	private static final String LOCAL_VALID = "!#$%&'*+-/=?^_`{|}~.";

	private static final Pattern LOCAL_PATTERN = Pattern.compile("[A-Za-z0-9!#$%&'*+\\-/=?^_`{|}~.]+");

	private static final String EMAIL_FAKE_SUFFIX = ".a";

	@Override
	public void validate(FacesContext context, UIComponent component,
			Object value) {

		/* retrieve the string value of the field */
		String emailID = (String) value;

		/* ensure value is an email address */
		if ( ! isValidEmail(emailID)) {
			FacesMessage message = new FacesMessage();
			message.setDetail("email not in valid format");
			message.setSummary("email not in valid format name@company.com");
			message.setSeverity(FacesMessage.SEVERITY_ERROR);
			throw new ValidatorException(message);
		}

	}

	/**
	 * Checks for the validity of the e-mail format.  Does not check if the e-mail is a valid
	 * internet e-mail, only that the e-mail is legal syntax based on RFC 3696.
	 *
	 * @param strEMail complete e-mail address
	 * @return true = valid e-mail format.  false = invalid email format
	 */
	static public boolean isValidEmail (String strEMail)
	{
		log.debug("input=`"+strEMail+"`");
		if (strEMail == null)
			return false;

		strEMail = strEMail.trim().toLowerCase();

		if (strEMail.indexOf(' ') != -1) {
			log.debug("contains embedded blank");
			return false;
		}

		/*
		 * The pattern match will take care of validating:
		 * - contains "@"
		 * - at least one character before "@"
		 * - an alphanumeric follows the "@"
		 * And for the domain:
		 * - only contains alphanumeric, hyphens, and periods
		 * - contains at least one period
		 * - starts with an alphanumeric
		 * - contains at least one alphabetic after the last period
		 * Note that our pattern enforces only alphabetics after the
		 * last period, which is not an RFC rule, but it is true in
		 * practice, as all TLDs are alphabetic-only.
		 */
		Matcher matcher = mask.matcher(strEMail);
		if ( ! matcher.matches() ) {
			log.debug("email pattern match failed");
			return false;
		}

		String strName = matcher.group(1); // name part -- before '@'
		String strDomain = matcher.group(2); // domain -- after '@'

		return (isValidDomain(strDomain) && isValidLocalName(strName));

	} // End method

	/**
	 * Returns true if & only if 'strDomain' is valid domain name syntax.
	 * (This does not validate that the domain actually exists.)
	 */
	static private boolean isValidDomain(String strDomain)
	{
		//log.debug("input=`"+strDomain+"`");
		if (strDomain.length() > 255) {
			log.debug("failed: length over 255");
			return false;
		}

		if (strDomain.indexOf("..") > -1) {
			log.debug("failed: contains `..`");
			return false;
		}

		StringTokenizer st = new StringTokenizer(strDomain, ".");
		while (st.hasMoreTokens()) {
			if ( ! isValidDomainLabel(st.nextToken()) ) {
				return false;
			}
		}
		//log.debug("valid domain: `"+strDomain+"`");
		return true;
	}

	/**
	 * Returns true if & only if 'str' is a valid domain label -- a
	 * string of text delimited by periods in a domain name.  The string
	 * must have a length greater than 0 and less than 64, may not
	 * start or end with a hyphen.
	 * The fact that the text only contains alphanumeric & hyphen was
	 * already validated by the matcher in isValidDomain().
	 *
	 * @param str
	 * @return true if 'str' is a valid domain label
	 */
	static private boolean isValidDomainLabel(String str)
	{
		//log.debug("input=`"+str+"`");
		// labels must be 1 to 63 characters long, consist only
		// of letters, digits, and hyphens; and may not begin or
		// end with a hyphen.
		int len = str.length();
		if ( len == 0 || len > 63 ) {
			log.debug("failed: length check");
			return false;
		}

		if ((str.charAt(0) == '-') || (str.endsWith("-"))) {
			log.debug("failed: leading or trailing hyphen");
			return false;
		}

		//log.debug("valid");
		return true;
	}

	/**
	 *  This does minimal checks on the 'local name' portion (before
	 *  the '@' sign) of an email address.  The variety of allowed
	 * characters, with & without quoting and escapes is quite complicated
	 * and we're not going to bother with it!
	 *
	 * @param strName
	 * @return true if strName is valid.
	 */
	static public boolean isValidLocalName(String strName)
	{
		//log.debug("input=`"+strName+"`");
		if (strName == null || strName.length() == 0 || strName.length() > 64)
			return false;
		char c = strName.charAt(0);
		if ( (c=='.') || (c==' ') ||
				(strName.endsWith(".")) ||
				(strName.endsWith(" ")) ||
				(strName.indexOf("..") > -1) ) {
			log.debug("failed: `" +strName + "`");
			return false;
		}
		// Some additional checks, assuming no quoted strings: mr."Fred Jones".stuff
		Matcher matcher = LOCAL_PATTERN.matcher(strName);
		if ( ! matcher.matches() ) {
			log.debug("local name pattern match failed: `" +strName + "`");
			return false;
		}

		//log.debug("valid local name: `"+strName+"`");
		return true;
	}

	/**
	 * Generate a syntactically valid email address given a person's first and
	 * last name. (Really, any two Strings.) This is generally of the form
	 * "&lt;firstName>.&lt;lastName>@&lt;production-id>.a". However, if the
	 * first or last name contain characters that are invalid in an email
	 * address, they will be replaced. Common characters such as space and comma
	 * are replaced by underscores. More exotic characters (@&lt;>[]) are
	 * replaced by their codePoint integer equivalent, e.g., a "@" is replaced
	 * by "64".
	 *
	 * @param firstName The person's first name; if null or empty, an underscore
	 *            ("_") will be used instead.
	 * @param lastName The person's last name; if null or empty, an underscore
	 *            will be used instead.
	 * @return A "fake" email address that is syntactically valid. That is, it
	 *         satisfies the Internet rules for a valid email address (RFC
	 *         3696).
	 */
	public static String makeFakeEmail(String firstName, String lastName) {
		if (StringUtils.isEmpty(firstName)) {
			firstName = "_";
		}
		if (StringUtils.isEmpty(lastName)) {
			lastName = "_";
		}
		String email = firstName + "." + lastName;
		email = email.replaceAll(" |,", "_").trim(); // eliminate common stuff
		email = email.replaceAll("^\\.", "_"); // eliminate leading period
		email = email.replaceAll("\\.$", "_"); // eliminate trailing period
		email = email.replaceAll("\\.\\.", "_"); // eliminate doubled periods

		Matcher matcher = LOCAL_PATTERN.matcher(email);
		if ( ! matcher.matches() ) {
			log.debug("initial fake email failed: `" +email + "`");
			for (int i=0; i < email.length(); i++) {
				//char ch = email.charAt(i);
				int ch = email.codePointAt(i);
				if ((! Character.isLetterOrDigit(ch)) &&
						(LOCAL_VALID.indexOf(ch) < 0)) {
					email = ( i == 0 ? "" : email.substring(0, i) )
							+ ch + email.substring(i+1);
				}
			}
		}

		if (email.length() > 64) {
			email = email.substring(0, 63);
		}

		email += "@" + SessionUtils.getNonSystemProduction().getId() + EMAIL_FAKE_SUFFIX;
		return email;
	}

	/**
	 * Checks for the validity of the e-mail format whether it is Fake or not.
	 *
	 * @param strEMail complete e-mail address
	 * @return true = valid e-mail format.  false = invalid email format
	 */
	static public boolean isFakeEmail(String strEMail)
	{
		log.debug("input=`"+strEMail+"`");
		if (strEMail == null)
			return false;
		strEMail = strEMail.trim().toLowerCase();
		if (strEMail.indexOf(' ') != -1) {
			log.debug("contains embedded blank");
			return false;
		}
		if (strEMail.endsWith(EMAIL_FAKE_SUFFIX)) {
			return true;
		}
		return false;
	}

}
