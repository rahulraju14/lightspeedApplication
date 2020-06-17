/**
 * File: SessionSetUtils.java
 */
package com.lightspeedeps.util.app;

import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.PayrollServiceDAO;
import com.lightspeedeps.model.PayrollService;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.User;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.user.UserPrefBean;
import com.lightspeedeps.web.util.HeaderViewBean;

/**
 * Methods moved from SessionUtils to allow them to be removed from
 * the ttco-base-model project.  LS-3067
 */
public class SessionSetUtils {

	private static final Log LOG = LogFactory.getLog(SessionSetUtils.class);

	/**
	 * Set the current User object. Its id is saved in the HTTP session. If a
	 * null value is passed, the attribute is removed from the HTTP session;
	 * this is typically done during logout processing.
	 *
	 * @param user The User object to be made the current user.
	 */
	public static void setCurrentUser(User user) {
		HttpSession session = SessionUtils.getHttpSession();
		if (user == null) {
			session.removeAttribute(Constants.ATTR_CURRENT_USER);
		}
		else {
			session.setAttribute(Constants.ATTR_CURRENT_USER,user.getId());
			if (! SessionUtils.noSession) {
				// Make sure we have an instance of UserPrefBean, & initialize it!
				UserPrefBean.getInstance().setUserId(user.getId());
			}
		}
		// clear current project, so it will get looked up on next request
		session.setAttribute(Constants.ATTR_CURRENT_PROJECT,null);
	}

	/**
	 * Set the current Production object. Its id is saved in the HTTP session,
	 * and the user's preference object may be updated.
	 *
	 * @param production
	 */
	public static void setProduction(Production production) {
		HttpSession session = SessionUtils.getHttpSession();
		if (session != null) {
			session.removeAttribute(Constants.ATTR_TC_WEEK_END_DAY);
			if (production == null) {
				session.removeAttribute(Constants.ATTR_PRODUCTION);
			}
			else {
				Integer productionId = production.getId();
				session.setAttribute(Constants.ATTR_PRODUCTION, productionId);
				LOG.debug("prod#: " + productionId);
				AuthorizationBean auth = AuthorizationBean.getInstance();
				if (auth != null) { // not available in batch
					auth.setProductionId(productionId);
					if (! production.isSystemProduction()) {
						UserPrefBean.getInstance().put(Constants.ATTR_PRODUCTION, productionId);
						UserPrefBean.getInstance().put(Constants.ATTR_LAST_PROD_ID, productionId);
					}
				}
				if (! SessionUtils.noSession) { // avoid HeaderViewBean lookup for Web Service
					SessionSetUtils.setupBranding(production, false, HeaderViewBean.getInstance());
				}
			}
		}
	}

	/**
	 * Set HeaderViewBean's fields that affect display of branding logos.
	 *
	 * @param prod The current, or soon-to-be current, production; we may be in
	 *            the process of switching productions.
	 * @param branded True if this is a "branded" production.
	 * @param headerBean Instance of HeaderViewBean to update for branding
	 *            changes.
	 */
	public static void setupBranding(Production prod, boolean branded, HeaderViewBean headerBean) {
		boolean isTTCprod = false;
		if (! branded && (! SessionUtils.noSession)) {
			headerBean.setBrandDesktopLogo(null);
			headerBean.setBrandMobileLogo(null);
			PayrollService service = null;
			if (prod != null && ! prod.isSystemProduction()) {
				service = prod.getPayrollPref().getPayrollService();
			}
			if (service == null) {
				// not in a production, or production has no service
				Integer id = SessionUtils.getInteger(Constants.ATTR_BRAND_SERVICE_ID);
				if (id != null) {
					service = PayrollServiceDAO.getInstance().findById(id);
				}
			}
	
			if (service != null) {
				headerBean.setBranded(true);
				if (service.getDesktopLogo() != null) {
					headerBean.setBrandDesktopLogo(service.getDesktopLogo());
				}
				if (service.getMobileLogo() != null) {
					headerBean.setBrandMobileLogo(service.getMobileLogo());
				}
				isTTCprod = service.getTeamPayroll();
			}
		}
		SessionUtils.put(Constants.ATTR_IS_TTC_PROD, isTTCprod); // LS-1763
	}

}
