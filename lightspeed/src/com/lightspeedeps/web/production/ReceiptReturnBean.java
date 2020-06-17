package com.lightspeedeps.web.production;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.checkout.AuthNetCheckoutServlet;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.util.HeaderViewBean;

/**
 * This class supports the GET link back from the Authorize.net Receipt
 * page.  The request parameters are used to update the Production status,
 * if appropriate; then the user is forwarded to the My Productions page.
 * This bean is invoked from the paid.jsp page, which should never actually
 * be displayed to the user.
 */
@ManagedBean
@ViewScoped
public class ReceiptReturnBean implements Serializable {
	/** */
	private static final long serialVersionUID = - 4952706374162665846L;

	private static final Log log = LogFactory.getLog(ReceiptReturnBean.class);

	/** True (not null) after getForward() is called once.  This simply allows
	 * our request processing routine to be called once during the page rendering. */
	private Boolean forward;

	public ReceiptReturnBean() {
	}

	/**
	 * Process the parameters in the HTTP request, which may result in the
	 * creation of a new Production for the user.
	 */
	private void checkParameters() {
		log.debug("");
		HttpServletRequest request = SessionUtils.getHttpRequest();
		AuthNetCheckoutServlet.doAuthPost(request, AuthNetCheckoutServlet.CALLBACK_TYPE_RETURN, false);
		HeaderViewBean.navigateToUrl(Constants.CHECKOUT_RETURN_LINK);
	}

	/** See {@link #forward}. */
	public Boolean getForward() {
		if (forward == null) {	// first call since bean instantiation
			checkParameters();	// process the request parameters
			forward = true;		// ignore subsequent calls
		}
		return forward;
	}
	/** See {@link #forward}. */
	public void setForward(Boolean forward) {
		this.forward = forward;
	}

}
