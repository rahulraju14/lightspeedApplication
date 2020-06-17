package com.lightspeedeps.test.payroll;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.FormW9DAO;
import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.model.FormW9;
import com.lightspeedeps.model.User;
import com.lightspeedeps.test.SpringTestCase;

/**
 * This code currently iterates over StartForms from one production and updates
 * them, setting the Social Security number to one that starts with a "9" (and
 * is therefore invalid).
 * <p>
 * See {@link com.lightspeedeps.test.RunSomethingTest} for an example of running
 * this class' methods as a JUnit test.
 */
public class FormW9Test extends SpringTestCase {
	private static final Log LOG = LogFactory.getLog(FormW9Test.class);

//	private static final FormW9DAO formW9DAO = FormW9DAO.getInstance();
//	private static final UserDAO userDAO = UserDAO.getInstance();

	public void testFormW9Ssn() {
		LOG.debug("");
		try {
			doUser();
			doW9();
		}
		catch (Exception ex) {
			LOG.error("Exception: ", ex);
		}
	}

	/**
	 */
	private void doUser() {
		LOG.debug("");
		int empty = 0;
		try {
			String query = User.GET_NON_NULL_SSN;
			List<User> list = UserDAO.getInstance().findByNamedQuery(query);
			for (User form : list) {
				LOG.debug("id: " + form.getId() + ", ssn: `" + form.getSocialSecurity() + "`");
				if (form.getSocialSecurity() == null) {
					LOG.debug("SSN is null");
				}
//				if (form.getSocialSecurity().length() == 0) {
//					empty++;
//					form.setSocialSecurity(null);
//					UserDAO.getInstance().attachDirty(form);
//				}
			}
		}
		catch (Exception ex) {
			LOG.error("Exception: ", ex);
		}
		LOG.debug("empty = " + empty);
	}

	/**
	 */
	private void doW9() {
		LOG.debug("");
		int emptyS = 0;
		int emptyF = 0;
		try {
			String query = FormW9.GET_NON_NULL_SSN_FEDID;
			List<FormW9> list = FormW9DAO.getInstance().findByNamedQuery(query);
			for (FormW9 form : list) {
				LOG.debug("id: " + form.getId() + ", ssn: `" + form.getSocialSecurity() + "`, fed id: `" + form.getFedidNumber() + "`");
				if (form.getSocialSecurity() == null) {
					LOG.debug("SSN is null");
				}
				if (form.getFedidNumber() == null) {
					LOG.debug("fed id number is null");
				}
//				if (form.getSocialSecurity().length() == 0) {
//					form.setSocialSecurity(null);
//					FormW9DAO.getInstance().attachDirty(form);
//					emptyS++;
//					LOG.debug("blank ssn " + form.getFullName() );
//				}
//				if (form.getFedidNumber().length() == 0) {
//					form.setFedidNumber(null);
//					FormW9DAO.getInstance().attachDirty(form);
//					emptyF++;
//					LOG.debug("blank fed id " + form.getFullName() );
//				}
			}
		}
		catch (Exception ex) {
			LOG.error("Exception: ", ex);
		}
		LOG.debug("empty SSN = " + emptyS);
		LOG.debug("empty Fed id = " + emptyF);
	}

}
