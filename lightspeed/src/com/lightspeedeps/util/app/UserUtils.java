/**
 * File: UserUtils.java
 */
package com.lightspeedeps.util.app;

import javax.faces.application.FacesMessage;

import com.lightspeedeps.dao.AddressDAO;
import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.model.Address;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.User;
import com.lightspeedeps.web.validator.EmailValidator;

/**
 * A class for some utility methods related to the User object.
 */
public class UserUtils {

	private UserUtils() {
	}

	public static boolean checkFields(User user) {
		checkImdb(user);
		if (user.getEmailAddress() != null) {
			user.setEmailAddress(user.getEmailAddress().trim());
		}
		boolean bRet = validateEmail(user);

		bRet &= checkName(user);
		bRet &= checkAllowances(user);
		//LS-4478 and LS-4717
		bRet &= validateAddress(user);

		if (bRet) {
			checkSaveAddress(user);
		}
		return bRet;
	}

	private static boolean checkName(User pUser) {
		boolean ret = true;
		if (pUser.getFirstName() != null) {
			pUser.setFirstName(pUser.getFirstName().trim());
		}
		if (pUser.getLastName() != null) {
			pUser.setLastName(pUser.getLastName().trim());
		}
		if (pUser.getFirstName() == null || pUser.getFirstName().length() == 0) {
			MsgUtils.addFacesMessage("Contact.BlankFirstName", FacesMessage.SEVERITY_ERROR);
			ret = false;
		}
		if (pUser.getLastName() == null || pUser.getLastName().length() == 0) {
			MsgUtils.addFacesMessage("Contact.BlankLastName", FacesMessage.SEVERITY_ERROR);
			ret = false;
		}
		return ret;
	}

	private static boolean checkAllowances(User user) {
		boolean ret = true;
		if (user.getW4Allowances() != null && (user.getW4Allowances() < 0 || user.getW4Allowances() > 99)) {
			MsgUtils.addFacesMessage("FormW4Bean.ValidationMessage", FacesMessage.SEVERITY_ERROR);
			ret = false;
		}
		return ret;
	}

	private static void checkSaveAddress(User user) {
		Address addr = user.getHomeAddress();
		if (addr != null) {
			if (! addr.trimIsEmpty()) {
				addr = AddressDAO.getInstance().merge(addr);
				user.setHomeAddress(addr);
			}
			else {
				user.setHomeAddress(null);
			}
		}
		addr = user.getLoanOutAddress();
		if (addr != null) {
			if (! addr.trimIsEmpty()) {
				addr = AddressDAO.getInstance().merge(addr);
				user.setLoanOutAddress(addr);
			}
			else {
				user.setLoanOutAddress(null);
			}
		}
		// LS-3578
		addr = user.getLoanOutMailingAddress();
		if (addr != null) {
			if (! addr.trimIsEmpty()) {
				addr = AddressDAO.getInstance().merge(addr);
				user.setLoanOutMailingAddress(addr);
			}
			else {
				user.setLoanOutMailingAddress(null);
			}
		}
		addr = user.getAgencyAddress();
		if (addr != null) {
			if (! addr.trimIsEmpty()) {
				addr = AddressDAO.getInstance().merge(addr);
				user.setAgencyAddress(addr);
			}
			else {
				user.setAgencyAddress(null);
			}
		}
		addr = user.getMailingAddress();
		if (addr != null) {
			if (! addr.trimIsEmpty()) {
				addr = AddressDAO.getInstance().merge(addr);
				user.setMailingAddress(addr);
			}
			else {
				user.setMailingAddress(null);
			}
		}
	}

	private static void checkImdb(User user) {
		if (user.getImdbLink() != null) {
			String link = user.getImdbLink().toLowerCase();
			if (link.startsWith("http://")) {
				user.setImdbLink(user.getImdbLink().substring(7));
			}
		}
	}

	/**
	 * Validate the contact's email address syntactically and to see if it
	 * duplicates another user's email address.
	 *
	 * @param pContact The Contact whose email should be validated.
	 * @param doMsg True iff this method should issue a FacesMessage in case of
	 *            a duplicate-email error.
	 * @return 0 if the email is valid; -1 if the email failed the syntax check;
	 *         and 1 if it passed the syntax check, but was equal to the email
	 *         address of some other User. For return code -1, a FacesMessage
	 *         will have been issued; for return code 1, a FacesMessage was
	 *         issued iff 'doMsg' was true.
	 */
	public static int validateEmail(Contact pContact, boolean doMsg) {
		int ret = 0;
		if (pContact.getEmailAddress() != null) {
			pContact.setEmailAddress(pContact.getEmailAddress().trim());
		}
		if ( ! validateEmail(pContact.getEmailAddress())) {
			ret = -1;
		}
		else {
			final UserDAO userDAO = UserDAO.getInstance();
			User matchUser = userDAO.findSingleUser(pContact.getEmailAddress());
			if (matchUser != null) {
				if (! matchUser.getId().equals(pContact.getUser().getId())) {
					if (doMsg) {
						MsgUtils.addFacesMessage("Contact.DuplicateEmail", FacesMessage.SEVERITY_ERROR);
					}
					ret = 1;
				}
				userDAO.evict(matchUser);
			}
		}
		return ret;
	}

	private static boolean validateEmail(User pUser) {
		if (pUser.getEmailAddress() != null) {
			pUser.setEmailAddress(pUser.getEmailAddress().trim());
		}
		boolean bRet = validateEmail(pUser.getEmailAddress());
		if (bRet) {
			final UserDAO userDAO = UserDAO.getInstance();
			User matchUser = userDAO.findSingleUser(pUser.getEmailAddress());
			if (matchUser != null) {
				if (! matchUser.getId().equals(pUser.getId())) {
					MsgUtils.addFacesMessage("Contact.DuplicateEmail", FacesMessage.SEVERITY_ERROR);
					bRet = false;
				}
				userDAO.evict(matchUser);
			}
		}
		return bRet;
	}

	/**
	 * Checks the syntactic validity of the given email address.
	 *
	 * @param email The email address to check.
	 * @return True iff the email address is syntactically correct. When false
	 *         is returned a message has been generated for the user.
	 */
	private static boolean validateEmail(String email) {
		boolean bRet = false;
		if (email != null) {
			email = email.trim();
			if (email.length() > 0) {
				if ( ! EmailValidator.isValidEmail(email)) {
					MsgUtils.addFacesMessage("Contact.InvalidEmail", FacesMessage.SEVERITY_ERROR);
				}
				else {
					bRet = true;
				}
			}
			else {
				MsgUtils.addFacesMessage("Contact.BlankEmail", FacesMessage.SEVERITY_ERROR);
			}
		}
		else {
			MsgUtils.addFacesMessage("Contact.BlankEmail", FacesMessage.SEVERITY_ERROR);
		}
		return bRet;
	}

	/**
	 * Method for null check of each address on MyAccount page - LS-4478,LS-4479,LS-4480,LS-4481,LS-4482
	 * @param user parameter to get all addresses and LS-4717
	 * @return true or false as per validation
	 */
	private static boolean validateAddress(User user) {
		boolean bRet = true;
		Address addr = user.getHomeAddress();
		if (addr != null) {
			if (! addr.trimIsEmpty()) {
				bRet = LocationUtils.checkAddress(addr,Constants.HOME_ADDRESS);
			}
		}

		addr = user.getLoanOutAddress();
		if (addr != null) {
			if (! addr.trimIsEmpty()) {
				bRet &= LocationUtils.checkAddress(addr,Constants.LOANOUT_ADDRESS);
			}
		}
		addr = user.getLoanOutMailingAddress();
		if (addr != null) {
			if (! addr.trimIsEmpty()) {
				bRet &= LocationUtils.checkAddress(addr,Constants.LOANOUT_MAILING_ADDRESS);
			}
		}

		addr = user.getAgencyAddress();
		if (addr != null) {
			if (! addr.trimIsEmpty()) {
				bRet &= LocationUtils.checkAddress(addr,Constants.AGENCY_ADDRESS);
			}
		}

		addr = user.getMailingAddress();
		if (addr != null) {
			if (! addr.trimIsEmpty()) {
				bRet &= LocationUtils.checkAddress(addr,Constants.MAILING_ADDRESS);
			}
		}
		return bRet;
	}



}
