/**
 * ResizePhaseListener.java
 */
package com.lightspeedeps.web.listener;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.util.JavaScriptRunner;

/**
 * This class is called before and after the RenderResponse phase; we use it to add
 * the "resize()" javascript call to all rendered pages.
 */
public class ResizePhaseListener implements PhaseListener {

	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(ResizePhaseListener.class);

	/**
	 * Identifies which phase this listener will be called for.
	 * @see javax.faces.event.PhaseListener#getPhaseId()
	 */
	@Override
	public PhaseId getPhaseId() {
		return PhaseId.RENDER_RESPONSE;
	}

	/**
	 * Called at the beginning of the specified phase(s).
	 * @see javax.faces.event.PhaseListener#beforePhase(javax.faces.event.PhaseEvent)
	 */
	@Override
	public void beforePhase(PhaseEvent event) {
		//log.debug("before phase " + event.getPhaseId() + " - adding resize() call");
		JavaScriptRunner.runScript(FacesContext.getCurrentInstance(), "resize();");
		return;
	}

	/**
	 * Called at the end of the specified phase(s).
	 * @see javax.faces.event.PhaseListener#afterPhase(javax.faces.event.PhaseEvent)
	 */
	@Override
	public void afterPhase(PhaseEvent event) {
		//log.debug("after phase " + event.getPhaseId());
	}

}
