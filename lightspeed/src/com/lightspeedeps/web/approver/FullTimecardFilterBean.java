/**
 * FilterBean.java
 */
package com.lightspeedeps.web.approver;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.ServiceFinder;

/**
 * This bean is used to manage the controls at the top of the Full Timecard
 * page. It holds the drop-down lists, the currently selected value for each
 * list, and listener methods for each list.
 * <p>
 * The FilterBean also tracks which mini-tab is currently selected, and restores
 * this mini-tab when the user returns to the Dashboard. To share
 * responsibility, the FilterBean needs to call back to the other beans, so
 * there is a "register()" method on the FilterBean which each of the other
 * beans calls. Rather than having separate "listener" callbacks for each
 * drop-down (filter) type, we have a single callback and pass a "FilterType"
 * enum to indicate which filter was changed by the user.
 */
@ManagedBean
@ViewScoped
public class FullTimecardFilterBean extends FilterBean {
	private static final Log log = LogFactory.getLog(FullTimecardFilterBean.class);

	public FullTimecardFilterBean() {
		log.debug("");
	}

	public static FullTimecardFilterBean getInstance() {
		return (FullTimecardFilterBean)ServiceFinder.findBean("fullTimecardFilterBean");
	}

}
