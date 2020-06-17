/**
 * PageAuthenticatePhaseListener.java
 */
package com.lightspeedeps.web.listener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.application.NavigationHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.PayrollServiceDAO;
import com.lightspeedeps.model.PayrollService;
import com.lightspeedeps.model.User;
import com.lightspeedeps.type.EventType;
import com.lightspeedeps.type.FeatureFlagType;
import com.lightspeedeps.util.app.ApplicationUtils;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.FF4JUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.login.AuthorizationBean;
import com.lightspeedeps.web.login.LoginBean;
import com.lightspeedeps.web.util.HeaderViewBean;

/**
 * This class validates that a user has access to a particular page.
 * Since most accesses are via the navigation tabs, which only appear when valid
 * for the user, this routine is really only necessary to prevent users from
 * directly entering a URL to which they are not allowed access.
 * <p>
 * It currently does a hard-coded comparison of page filenames (excluding the path
 * and .jsp or whatever suffix) and the appropriate page-field-access values in
 * the user's auth.getPgFields() map.
 * <p>
 * # Validated : Control goes to the requested page
 * # Not Validated : Control goes to "myhome" page, or "login" if no user is logged in.
 * <p>
 * NOTE: all phase-listeners are Singleton (application-scope) objects, and MUST be
 * thread-safe.  For example, do NOT use any non-final fields (class variables)!
 */
public class PageAuthenticatePhaseListener implements PhaseListener {

	private static final long serialVersionUID = 4227321411715295467L;
	private static final Log log = LogFactory.getLog(PageAuthenticatePhaseListener.class);

	/** URL used to direct the user to the ESS login page; used when user is logging out, but
	 * originally logged in from ESS. LS-3758 */
	private static final String ESS_LOGIN_URL =  ApplicationUtils.getEssBaseUrl() + "login";

	private static final List<PageKey> pageKeys = new ArrayList<>();

	/** A list of partial URLs, which must match values of 'pageInfo' exactly, of pages that
	 * should not be allowed access to if the production is read-only.  Note that currently the
	 * code only checks this situation for pages beginning with "/m/t/" */
	private static String mobileRwPages =
			"/m/t/app /m/t/appd /m/t/cnf /m/t/pin /m/t/rej /m/t/rejd /m/t/sub /m/t/subap";

	static {
		createPageKeys();
	}

	@Override
	public PhaseId getPhaseId() {
		return PhaseId.RENDER_RESPONSE;
	}

	@Override
	public void beforePhase(PhaseEvent event) {
		String uri;
		String pageInfo;
		FacesContext fc;
		fc = event.getFacesContext();
		uri = fc.getViewRoot().getViewId();
		if (uri == null) {
			log.debug("null uri");
			uri = "?.";
		}
		if (uri.indexOf("jsp/") >= 0) {
			pageInfo = uri.substring(uri.lastIndexOf("jsp/") + 4, uri.indexOf('.'));
		}
		else if (uri.indexOf("/m/") >= 0) {
			pageInfo = uri.substring(uri.lastIndexOf("/m/"), uri.indexOf('.'));
		}
		else {
			pageInfo = uri.substring(uri.lastIndexOf("/") + 1, uri.indexOf('.'));
		}

//		log.info("pageInfo: " + pageInfo);
		AuthorizationBean auth = (AuthorizationBean) FacesContext.getCurrentInstance().getExternalContext()
				.getSessionMap().get("authBean");
		if (auth != null && auth.isEmpty()) {
			// no real data, probably created trying to render a page with no user logged in.
			auth = null; // treat as if no AuthorizationBean exists
		}

		if (pageInfo.contains("login")		// reset, resetpwreturn, newaccount, etc.
				|| pageInfo.contains("logout")
				|| pageInfo.equals("r")		// short form of "resetpwreturn"
				|| pageInfo.equals("recover")	// error recovery page
				|| pageInfo.startsWith("/m/l/") // mobile login screens
				) {
			log.debug("login/reset page, no auth required");
			boolean mobile = MobileDeviceListener.checkMobile(fc); // test & set mobile session parameters
			if (mobile) {
				if (pageInfo.equals("login")) {
					// mobile user on standard login page - redirect to mobile login page
					HttpServletRequest request = SessionUtils.getHttpRequest();
					String expired = request.getParameter("ex");
					if (expired != null) {
						SessionUtils.put(Constants.ATTR_SESSION_EXPIRED, expired); // pass to LoginBean
					}
					NavigationHandler nh = fc.getApplication().getNavigationHandler();
					nh.handleNavigation(fc, null, HeaderViewBean.VIEW_LOGIN + "m");
				}
				else if (pageInfo.contains("resetpwreturn") || pageInfo.contains("resetnew")) {
					// mobile user on special password page - redirect to mobile version
					HttpServletRequest request = SessionUtils.getHttpRequest();
					String key = request.getParameter("key");
					if (key == null) {
						key = request.getParameter("nk");
					}
					if (key != null) {
						SessionUtils.put(Constants.ATTR_PW_RESET_KEY, key); // save it!
					}
					NavigationHandler nh = fc.getApplication().getNavigationHandler();
					if (pageInfo.contains("resetpwreturn")) {
						nh.handleNavigation(fc, null, "resetpwreturnm"); // to /m/l/rp.jsp
					}
					else {
						nh.handleNavigation(fc, null, "resetnewm"); // to /m/l/rn.jsp
					}
				}
			}
			HttpServletRequest request = SessionUtils.getHttpRequest();
			String refer = request.getParameter("ref");
			if (refer != null && refer.trim().length() > 0) {
				refer = refer.trim();
				SessionUtils.put(Constants.ATTR_REFERRED_BY, refer);
			}
			String referrer = request.getHeader("referer");
			if(referrer == null) {
				referrer = request.getRequestURL().toString();
			}
			checkRefer(refer, referrer);
			String source = request.getParameter("source");
			if ("ESS".equalsIgnoreCase(source)) {
				String origin = request.getParameter("or");
				log.debug("logout->login redirect from ESS; originESS=" + origin);
				if ("true".equalsIgnoreCase(origin)) {
					// user originally logged in through ESS. Send back to ESS login page.
					String url = ESS_LOGIN_URL;
					if (fc != null) {
						ExternalContext ec = fc.getExternalContext();
						if (ec != null) {
							try {
								ec.redirect(url);
							}
							catch (IOException e) {
								EventUtils.logError(e);
							}
							log.debug("redirect completed to " + url);
							return;
						}
					}
				}
			}
			String ipAddr = SessionUtils.getClientIpAddr(SessionUtils.getHttpRequest());
			if (ipAddr != null) {
				SessionUtils.put(Constants.ATTR_IP_ADDR, ipAddr);
			}
			log.info("pageInfo: " + pageInfo + ", IP=" + ipAddr + ", UserAgent=" + SessionUtils.get(Constants.ATTR_USER_AGENT));
			return;	// no authorization required
		}

		boolean allowSeamless = FF4JUtils.useFeature(FeatureFlagType.TTCO_ESS_SEAMLESS_INTEGRATION); // LS-3758

		if (allowSeamless) { // LS-3758 Check for request from ESS (or other) application
			Map<String, String> paramMap = fc.getExternalContext().getRequestParameterMap();
			log.debug("param count=" + paramMap.size());
			String token = paramMap.get("token");
			if (token != null && auth == null) {
				// do "auto-login" of current User with token
				String ret = paramMap.get("ret"); // Is there a 'return' parameter?
				boolean returnESS = ((ret == null) ? true : ret.equalsIgnoreCase("y"));
				String nav = LoginBean.getInstance().autoLogin(token); // Get the user logged in
				log.debug("checkLogin returned: " + nav);
				if (nav != null) {
					auth = AuthorizationBean.getInstance(); // setup for following code
				}
			} else {
				log.debug("no token param found");
			}
		}
		if (auth == null || auth.getPgFields() == null || auth.getPgFields().size() == 0) {
			log.info("pageInfo: " + pageInfo + " -- auth bean missing or empty, forced to login");
			HttpServletRequest request = SessionUtils.getHttpRequest();
			String refer = request.getParameter("ref");
			if (refer != null && refer.trim().length() > 0) {
				SessionUtils.put(Constants.ATTR_REFERRED_BY, refer.trim());
			}
			savePageInfo(uri, fc);
			NavigationHandler nh = fc.getApplication().getNavigationHandler();
			nh.handleNavigation(fc, null, HeaderViewBean.VIEW_LOGIN);
		}
		else if (SessionUtils.getInteger(Constants.ATTR_CURRENT_USER) == null) {
			log.info("pageInfo: " + pageInfo + " -- no user, forced to login");
			savePageInfo(uri, fc);
			NavigationHandler nh = fc.getApplication().getNavigationHandler();
			nh.handleNavigation(fc, null, HeaderViewBean.VIEW_LOGIN);
		}
		else { // check user login status
			Integer userId = null;
			try {
				HttpSession session = ((HttpServletRequest)fc.getExternalContext().getRequest()).getSession();
				userId = (Integer)session.getAttribute(Constants.ATTR_CURRENT_USER);
			}
			catch (Exception e) { // ignore any error retrieving user id
			}

			log.info("pageInfo: " + pageInfo + ", user#=" + userId);

			if (allowSeamless && pageInfo.contains("jump/")) { // LS-3758
				String remainder = pageInfo.substring(pageInfo.indexOf("jump/")+5);
				String url = ApplicationUtils.getEssBaseUrl() + remainder;
				SessionUtils.put(Constants.ATTR_CROSS_APP, true); // remember cross-app in use
				if (HeaderViewBean.navigateWithToken(url)) {
					return;
				}
			}

			/**
			 * True if the page may be accessed only when NOT inside a production, and
			 * therefore false if the page may not be accessed when in a production.
			 */
			boolean isOuterPage = pageInfo.contains("product/") || pageInfo.contains("user/")
					|| pageInfo.contains("sys/") || pageInfo.contains("/u/");

			if (pageInfo.startsWith("/m/t/")) {
				if (! auth.getPgFields().containsKey(Constants.PGKEY_WRITABLE_PRODUCTION)) {
					// read-only production --
					// We block some mobile pages that are only accessible on an active (r/w) production.
					if (mobileRwPages.indexOf(pageInfo) >= 0) {
						goHome(uri, fc);
						return;
					}
				}
			}

			Integer prodId = SessionUtils.getProductionId();
			if (isOuterPage && prodId != null && ! prodId.equals(Constants.SYSTEM_PRODUCTION_ID)) {
				// URL is outside a production, but environment is inside -- exit production
				if (SessionUtils.getInteger(Constants.ATTR_ENTERING_PROD)==null) {
					User user = SessionUtils.getCurrentUser();
					HeaderViewBean.exitProduction(user);
				}
			}
			else if (!isOuterPage && (prodId == null || prodId.equals(Constants.SYSTEM_PRODUCTION_ID))) {
				// URL accessible only inside a production, but environment is outside - don't allow
				log.info("inner URL attempted while outside a production");
				savePageInfo(uri, fc); // can jump to page after selecting production. LS-3758
				goHome(uri, fc);
				return;
			}
			SessionUtils.put(Constants.ATTR_ENTERING_PROD, null);
			for (PageKey pageKey : pageKeys) {
				if (pageInfo.contains(pageKey.page)) {
					if (! auth.getPgFields().containsKey(pageKey.key)) {
						goHome(uri, fc);
						uri = null;
					}
					break; // once we have a partial URL match, we're done
				}
			}
			if (! pageInfo.contains("user/myprod")) { // don't save production select page.
				savePageInfo(uri, fc);
			}
			// Make sure "back" & "forward" cause the browser to re-request the page!
			HttpServletResponse response = (HttpServletResponse) fc.getExternalContext().getResponse();
			response.addHeader("Pragma", "no-cache"); // for http 1.0 clients
			response.addHeader("Cache-Control", "no-cache"); // for http 1.1+ clients
			response.addHeader("Cache-Control", "no-store");
			response.addHeader("Cache-Control", "must-revalidate");
			response.addHeader("Expires", "0"); // expires immediately
		}
		return; // passed all checks
	}

	/**
	 * See if we have a payroll service referrer, either from ref= keyword on the
	 * URL, or presence of some particular text in the referring URL.
	 *
	 * @param refer
	 * @param referrer
	 */
	private void checkRefer(String refer, String referrer) {
		int id = -1;

		if (refer != null) {
			for (PayrollService ps : PayrollServiceDAO.getPayrollServices()) {
				if (ps.getReferParam() != null && refer.equalsIgnoreCase(ps.getReferParam())) {
					id = ps.getId();
					break;
				}
			}
		}
		if (id < 0 && referrer != null) {
			referrer = referrer.toLowerCase().trim();
			for (PayrollService ps : PayrollServiceDAO.getPayrollServices()) {
				if (ps.getReferUrlPart() != null && referrer.indexOf(ps.getReferUrlPart()) > -1) {
					id = ps.getId();
					refer = ps.getReferParam();
					break;
				}
			}
		}

		log.debug("id=" + id + ", ref=" + refer + ", refBy=" + referrer);
		if (id > 0) {
			SessionUtils.put(Constants.ATTR_BRAND_SERVICE_ID, id);
			SessionUtils.put(Constants.ATTR_BRAND_SERVICE_REFER, refer);
			// Display appropriate text depending on the referrer. LS-1691
			boolean isTTCOnlineDomain = false;
			if(referrer.indexOf(Constants.TTC_ONLINE_DOMAIN_REFER_PART) > -1) {
				isTTCOnlineDomain = true;
			}
			SessionUtils.put(Constants.ATTR_IS_TTCONLINE_DOMAIN, isTTCOnlineDomain);
		}

		// Find the domain from the referrer text.
		int protocalIdx, postDomainIdx;
		// Find the double slashes. "https://"
		protocalIdx = referrer.indexOf("//");
		// Find the first slash after the double slash; https://xxx.com/
		postDomainIdx = referrer.indexOf("/", protocalIdx + 2);
		String domain = referrer.substring(protocalIdx + 2, postDomainIdx);
		if (domain.contains(":")) {
			int i = domain.indexOf(':');
			domain = domain.substring(0, i);
		}
		// Put the domain in the session
		SessionUtils.put(Constants.ATTR_HTTP_REQUEST_DOMAIN, domain);
		// For the sender's email address, strip off sub-domain part:
		if (domain.contains(".lightspeedeps.com") || domain.contains(".ttconline.com")) {
			int i = domain.indexOf('.');
			domain = domain.substring(i+1);
		}
		SessionUtils.put(Constants.ATTR_EMAIL_SENDING_DOMAIN, domain);
	}

	private void savePageInfo(String uri, FacesContext fc) {
		SessionUtils.put(Constants.ATTR_SAVED_PAGE_INFO, uri);
		Map<String,String> params = fc.getExternalContext().getRequestParameterMap();
		if (params != null) {
			SessionUtils.put(Constants.ATTR_SAVED_PAGE_UNIT, // save Unit id, if any
					params.get(Constants.UNIT_ID_URL_KEY));
		}
	}

	/**
	 * Route the user's request to either "My Home" or "My Productions" page.
	 */
	private void goHome(String uri, FacesContext fc) {
		String username = (SessionUtils.getCurrentUser()== null ? "null" : SessionUtils.getCurrentUser().getEmailAddress());
		String msg = "*** INVALID ACCESS ATTEMPTED *** URI=" + uri + ", userName=" + username;
		log.warn(msg);
		EventUtils.logEvent(EventType.INFO, null, null, username, msg);
		MobileDeviceListener.checkMobile(fc);
		NavigationHandler nh = fc.getApplication().getNavigationHandler();
		Integer prodId = SessionUtils.getProductionId();
		if (prodId != null && ! prodId.equals(Constants.SYSTEM_PRODUCTION_ID)) {
			nh.handleNavigation(fc, null,
					SessionUtils.mobilize(HeaderViewBean.getHomeNavigation()));
		}
		else {
			nh.handleNavigation(fc, null,
					SessionUtils.mobilize(HeaderViewBean.MYPROD_MENU_PROD));
		}
	}

	private static void createPageKeys() {
		/*
		 * Create a list of PARTIAL URLs, and the key required if the user's
		 * requested URL contains that part.
		 * Note that if items in the list contain each other (such as "callsheet"
		 * and "callsheetlist"), then the longer entry must be added first, so
		 * that the shorter partial value is only checked if the longer partial
		 * value does not match.
		 */
		pageKeys.add(new PageKey("/t/app",   Constants.PGKEY_TC_APPROVAL));
		pageKeys.add(new PageKey("/t/dash",  Constants.PGKEY_TC_APPROVAL));
		pageKeys.add(new PageKey("/t/rej",   Constants.PGKEY_TC_APPROVAL));
		pageKeys.add(new PageKey("/t/rev",   Constants.PGKEY_TC_APPROVAL));
		pageKeys.add(new PageKey("/t/subap", Constants.PGKEY_TC_APPROVAL));
		pageKeys.add(new PageKey("admin/", "0.2,admin_tab"));
		pageKeys.add(new PageKey("adminsys/", "0.2,admin_tab"));
		pageKeys.add(new PageKey("approver", "0.2,tc_approval"));
		pageKeys.add(new PageKey("breakdown", "0.2,breakdowns"));
		pageKeys.add(new PageKey("calendaredit", "6.0,edit"));
		pageKeys.add(new PageKey("calendarview", "0.2,calendar"));
		pageKeys.add(new PageKey("callsheet", "0.2,callsheet"));
		pageKeys.add(new PageKey("contactlist", "0.2,contact_view"));
//		pageKeys.add(new PageKey("contractview", "0.2,contracts"));
		pageKeys.add(new PageKey("dpr/", "7.2,view_production_report"));
		pageKeys.add(new PageKey("elements/poi", "0.2,real_element_view"));
		pageKeys.add(new PageKey("elements/real", "0.2,real_element_view"));
		pageKeys.add(new PageKey("elements/script", "0.2,script_element_view"));
		pageKeys.add(new PageKey("exhibitgview", "7.2,view_exhibit_g"));
		pageKeys.add(new PageKey("filerepository", "0.2,files"));
		pageKeys.add(new PageKey("import", "0.2,new_revision"));
		pageKeys.add(new PageKey("main/home", Constants.PGKEY_PRODUCTION)); // logged into a production
		pageKeys.add(new PageKey("main/project", Constants.PGKEY_PRODUCTION));
		pageKeys.add(new PageKey("materials", "0.2,materials"));
		pageKeys.add(new PageKey("members", "0.2,project_subhead"));
		pageKeys.add(new PageKey("permissions", "0.2,permissions"));
		pageKeys.add(new PageKey("reports", "0.2,report_tab"));
		pageKeys.add(new PageKey("script/import", "0.2,new_revision"));
		pageKeys.add(new PageKey("scriptrev", "0.2,drafts"));
		pageKeys.add(new PageKey("stripboardeditm", "6.1.2,all"));
		pageKeys.add(new PageKey("stripboardlist", "0.2,stripboard_list"));
		pageKeys.add(new PageKey("stripboardview", "0.2,stripboard"));
		pageKeys.add(new PageKey("tcpref/prefer", "0.2,tc_preferences"));
	}

	static class PageKey {
		public String page;
		public String key;
		PageKey(String p, String k) {
			page = p;
			key = k;
		}
	}

	@Override
	public void afterPhase(PhaseEvent event) {
		// nothing to do here.
	}

}
