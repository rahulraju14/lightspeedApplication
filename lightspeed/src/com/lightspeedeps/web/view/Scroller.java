/**
 * Scroller.java
 */
package com.lightspeedeps.web.view;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.util.JavaScriptRunner;

import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.SessionUtils;

/**
 * This class is used to help maintain the scrolled position of the
 * main div in pages that largely consist of a single scrollable
 * div.
 * <p>
 * For example, see the Call Sheet view/edit pages or the Start
 * Form page.
 */
public class Scroller {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(Scroller.class);

	public static final String JAVA_SCRIPT_SCROLL_POS = "scrollToPos();";

	/** This is a hidden value on the page which tracks the
	 * last-known scroll position value of the scrollable div being
	 * managed. */
	private String scrollPos;

	/** True iff the concrete subclass needs the scroll-preserving functionality. */
	private boolean scrollable;

	/**
	 * Called when the user clicks Edit. Saves the current scroll position if
	 * applicable.
	 *
	 * @return null navigation String.
	 */
	public String actionEdit() {
		maintainScrollPos();
		return null;
	}

	/**
	 * Called when the user clicks Save. Saves the current scroll position if
	 * applicable.
	 *
	 * @return null navigation String.
	 */
	public String actionSave() {
		maintainScrollPos();
		return null;
	}

	/**
	 * Called when the user clicks Cancel. Saves the current scroll position if
	 * applicable.
	 *
	 * @return null navigation String.
	 */
	public String actionCancel() {
		maintainScrollPos();
		return null;
	}

	/**
	 * Attempt to maintain the scrolled position of the main div.
	 */
	public void maintainScrollPos() {
		if (scrollable) {
			restoreScroll();
			saveScroll();
		}
	}

	/**
	 * Force the scrollable div to a specific scroll position.
	 *
	 * @param pos The pixel position to scroll to.
	 */
	public void initScrollPos(int pos) {
		addJavascript( "setScrollPos("+ pos + ");" );
		SessionUtils.put(Constants.ATTR_SCROLL_POS, null); // clear any stored value
	}

	/**
	 * Restore the div's scroll position from a value saved in our session, if
	 * any.
	 */
	public void restoreScrollFromSession() {
		String pos = SessionUtils.getString(Constants.ATTR_SCROLL_POS);
		if (pos != null) {
			scrollPos = pos;
			restoreScroll();
			SessionUtils.put(Constants.ATTR_SCROLL_POS, null); // don't use it again
		}
		else {
			scrollPos = "0";
		}
	}

	/**
	 * Send a JavaScript command to restore the scroll position of our
	 * main scrollable div.
	 */
	public void restoreScroll() {
		addJavascript(JAVA_SCRIPT_SCROLL_POS); // try to maintain scrolled position
	}

	/**
	 * Save the current value of the div's scroll position in our session for
	 * future reference.
	 */
	public void saveScroll() {
		SessionUtils.put(Constants.ATTR_SCROLL_POS, getScrollPos()); // save scroll position for re-instantiation
	}

	/**
	 * Remove the saved scroll position from our session variables.
	 */
	public static void clearScroll() {
		SessionUtils.put(Constants.ATTR_SCROLL_POS, null); // delete any saved scroll position
	}

	/**
	 * Append a chunk of JavaScript code to the response stream, to be
	 * executed on the client browser.
	 * @param script The JavaScript code to be sent.
	 */
	public static void addJavascript(String script) {
		JavaScriptRunner.runScript(FacesContext.getCurrentInstance(), script); // ICEfaces 3.x
		//JavascriptContext.addJavascriptCall(FacesContext.getCurrentInstance(), script); // ICEfaces 1.8
	}


	/**See {@link #scrollPos}. */
	public String getScrollPos() {
		return scrollPos;
	}
	/**See {@link #scrollPos}. */
	public void setScrollPos(String scrollPos) {
		this.scrollPos = scrollPos;
	}

	/**See {@link #scrollable}. */
	public boolean getScrollable() {
		return scrollable;
	}
	/**See {@link #scrollable}. */
	public void setScrollable(boolean scrollable) {
		this.scrollable = scrollable;
	}

}
