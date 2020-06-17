/**
 * File: CustomExceptionHandler.java
 */
package com.lightspeedeps.web.util;

import java.util.Iterator;

import javax.faces.FacesException;
import javax.faces.application.ViewExpiredException;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerWrapper;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.SessionUtils;

/**
 * Class which intercepts server-side exceptions and deals with them.
 */
public class CustomExceptionHandler extends ExceptionHandlerWrapper {

	private static final Log LOG = LogFactory.getLog(CustomExceptionHandler.class);

    private ExceptionHandler wrapped;

    CustomExceptionHandler(ExceptionHandler exception) {
        this.wrapped = exception;
    }

    @Override
    public ExceptionHandler getWrapped() {
        return wrapped;
    }

	@Override
	public void handle() throws FacesException {

		final Iterator<ExceptionQueuedEvent> i = getUnhandledExceptionQueuedEvents().iterator();
		while (i.hasNext()) {
			ExceptionQueuedEvent event = i.next();
			ExceptionQueuedEventContext context = (ExceptionQueuedEventContext)event.getSource();

			// get the exception from context
			Throwable t = context.getException();

			try {
				if (t instanceof ViewExpiredException) {
					// Allow ICEfaces/JSF to handle expired sessions
					continue; // leave it in queue
				}
			}
			catch (Exception e) {
				LOG.error(e);
			}

			try {
				//log error ?
				LOG.error("Critical Exception!", t);
				String errorPage;
				if (SessionUtils.isPhoneUser()) {
					errorPage = "m/common/500.html";
				}
				else {
					errorPage = "jsp/error/500.html";
				}
				HeaderViewBean.navigateToUrl(errorPage);

				// un-comment the line below if you want to report the error in a JSF error message
				//JsfUtil.addErrorMessage(t.getMessage());

			}
			finally {
				//remove it from queue
				i.remove();
			}
		}
		//parent handle
		getWrapped().handle();
	}

}
