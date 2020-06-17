/**
 * LogPhaseListener.java
 */
package com.lightspeedeps.web.listener;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//import com.icesoft.faces.context.BridgeFacesContext; // not available in ICEfaces 3.2

/**
 * This class may be called before and after every phase for logging purposes. It is
 * normally not enabled, but can be enabled by un-commenting the entry in faces-config.xml.
 */
public class LogPhaseListener implements PhaseListener {

	private static final long serialVersionUID = 1L;
	private static final Log log = LogFactory.getLog(LogPhaseListener.class);

	/**
	 * Identifies which phase this listener will be called for.
	 * @see javax.faces.event.PhaseListener#getPhaseId()
	 */
	@Override
	public PhaseId getPhaseId() {
		if (log.isDebugEnabled()) {
			// if debug is on, log all phases
			return PhaseId.ANY_PHASE;
		}
		// logging disabled, minimize calls...
		return PhaseId.RESTORE_VIEW;
	}

	/**
	 * Called at the beginning of the specified phase(s).
	 * @see javax.faces.event.PhaseListener#beforePhase(javax.faces.event.PhaseEvent)
	 */
	@Override
	public void beforePhase(PhaseEvent event) {
		try {
			if (log.isDebugEnabled()) {
				log.debug("before phase " + event.getPhaseId());
				FacesContext fc = event.getFacesContext();
				//String vnum = ((BridgeFacesContext)fc).getViewNumber();
				//String vnum = "?";
				String sessionId = ((HttpSession)fc.getExternalContext().getSession(false)).getId();
				String uri = "";
				if (fc.getViewRoot() != null) {
					uri = fc.getViewRoot().getViewId();
				}
				if (event.getPhaseId() == PhaseId.RESTORE_VIEW) { // phase 1, no view id
					log.debug("sessionId=" + sessionId);
				}
				else if (event.getPhaseId() == PhaseId.APPLY_REQUEST_VALUES) { // phase 2
					log.debug("viewId='" + uri + "'");
				}
				else if (event.getPhaseId() == PhaseId.PROCESS_VALIDATIONS) { // phase 3
				}
				else if (event.getPhaseId() == PhaseId.UPDATE_MODEL_VALUES) { // phase 4
				}
				else if (event.getPhaseId() == PhaseId.INVOKE_APPLICATION) { // phase 5
				}
				else if (event.getPhaseId() == PhaseId.RENDER_RESPONSE) { // phase 6
					log.debug("viewId='" + uri + "', sessionId=" + sessionId);
				}
			}
		}
		catch (Exception e) {
			log.error("exception: ",e);
		}
		return;
	}

	/**
	 * Called at the end of the specified phase(s).
	 * @see javax.faces.event.PhaseListener#afterPhase(javax.faces.event.PhaseEvent)
	 */
	@Override
	public void afterPhase(PhaseEvent event) {
		log.debug("after phase " + event.getPhaseId());
	}

}
