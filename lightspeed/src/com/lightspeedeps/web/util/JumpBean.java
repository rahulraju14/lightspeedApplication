/**
 * File: JumpBean.java
 */
package com.lightspeedeps.web.util;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.CallsheetDAO;
import com.lightspeedeps.dao.ContactDAO;
import com.lightspeedeps.dao.DprDAO;
import com.lightspeedeps.dao.ScriptElementDAO;
import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.model.Callsheet;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.Dpr;
import com.lightspeedeps.model.ScriptElement;
import com.lightspeedeps.model.User;
import com.lightspeedeps.type.ReportType;
import com.lightspeedeps.type.ScriptElementType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.report.DprViewBean;
import com.lightspeedeps.web.report.ReportBean;
import com.lightspeedeps.web.user.UserPrefBean;

/**
 * A bean to facilitate inter-page jumps.
 * <p>
 * One use of the JumpBean is to push values into a Session variable, where it
 * will be retrieved by some other bean. This is done using a pair
 * propertyActionListener tabs like this:
 * <pre>
 * < f:setPropertyActionListener value="com.lightspeedeps.callsheetdate" target="#{jumpBean.attribute}"/>
 * < f:setPropertyActionListener value="#{oneweek.date[1]}" target="#{jumpBean.value}"/>
 * </pre>
 * See the Calendar View page for a lot of examples of this.
 * <p>
 * Another use is to have small "action" methods here which can be called from
 * JSF. They are put here, rather than in their "native" bean, to avoid having
 * some of those "heavy-weight" beans instantiated twice in the process of
 * jumping to "their" page. For example, actionViewCallSheet() is used by the
 * Calendar View page, instead of calling a method in CallSheetViewBean, because
 * JSF would instantiate CallSheetViewBean to execute the action code, which
 * would then cause a navigation to the Callsheet View page, which would then
 * instantiate a new instance of CallSheetViewBean to actually render the page.
 */
@ManagedBean
@ViewScoped
public class JumpBean implements Serializable {
	/** */
	private static final long serialVersionUID = 3782913978718584304L;

	private static final Log log = LogFactory.getLog(JumpBean.class);

	/** A value to store in the current Session, which will (presumably) be used by
	 * the target page. */
	private Object value;

	/** The attribute that the 'value' will be stored under in the current Session. */
	private String attribute;

	/** The JSP navigation string to use when our actionJump method is called. */
	private String jumpTo;

	public JumpBean() {
	}

//	public void putValue(ActionEvent evt) {
//		if (attribute == null) {
//			attribute = "currentRow";
//		}
//		SessionUtils.put(attribute, value);
//	}

	/**
	 * Action method to jump to viewing a contact, given the corresponding
	 * User's database id. The jumpBean.value field must already be set to the
	 * desired user's id. This is typically done in the JSP using a
	 * f:setPropertyActionListener tag.
	 *
	 * @return The navigation string to jump to the Cast & Crew View page.
	 */
	public String actionViewContactByUserId() {
		try {
			log.debug("id=" + value);
			if (value != null && value instanceof Integer) {
				Integer id = (Integer)value;
				User user = UserDAO.getInstance().findById(id);
				Contact contact = null;
				List<Contact> contacts = ContactDAO.getInstance().findByProperty("user", user);
				if (contacts.size() > 0) {
					contact = contacts.get(0);
				}
				if (contact != null) {
					SessionUtils.put(Constants.ATTR_CONTACT_ID, contact.getId()); // used by onboarding and cast&crew
					SessionUtils.put(Constants.ATTR_TC_TCUSER_ID, id); // used by timecard pages
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		SessionUtils.put(Constants.ATTR_CURRENT_MINI_TAB, null);
		return HeaderViewBean.PEOPLE_MENU_CONTACTS;
	}

	/**
	 * Action method to jump to viewing a specific CHARACTER script element
	 * using a cast id value, from a different page.
	 *
	 * @return Navigation string to jump to the Script Element View page.
	 */
	public String actionViewElementByCastId() {
		String castId = (String)value;
		if (castId != null) {
			ScriptElement scriptElement = ScriptElementDAO.getInstance()
					.findByElementIdProjectType(castId, SessionUtils.getCurrentProject(),
							ScriptElementType.CHARACTER);
			if (scriptElement != null) {
				SessionUtils.put(Constants.ATTR_SE_ELEMENT_ID, scriptElement.getId());
			}
		}
		SessionUtils.put(Constants.ATTR_CURRENT_MINI_TAB, null);
		return HeaderViewBean.ELEMENTS_MENU_SCRIPT_ELEMENTS;
	}

	/**
	 * Action method to jump to viewing a specific script element using the
	 * database id value, from a different page.
	 *
	 * @return Navigation string to jump to the Script Element View page.
	 */
	public String actionViewElementByDbId() {
		try {
			Integer id = (Integer)value;
			if (id != null) {
				ScriptElement scriptElement = ScriptElementDAO.getInstance().findById(id);
				if (scriptElement != null) {
					SessionUtils.put(Constants.ATTR_SE_ELEMENT_ID, scriptElement.getId());
				}
			}
		}
		catch (Exception e) {
			// ignore it
		}
		SessionUtils.put(Constants.ATTR_CURRENT_MINI_TAB, null);
		return HeaderViewBean.ELEMENTS_MENU_SCRIPT_ELEMENTS;
	}

	/**
	 * Action method to jump to view a particular DPR. Used from the Home page.
	 * The date of the desired report should already be in the Session variable
	 * 'reportDate'.
	 *
	 * @return The navigation text to jump to the DPR View page, or null in case
	 *         of a severe error.
	 */
	public String actionViewDpr() {
		try {
			// Set the report type so if the user goes back to the Reports page, it'll be on DPRs
			SessionUtils.put(ReportBean.ATTR_RPT_TYPE, ReportBean.TYPE_DAILY_DPR);
			SessionUtils.put(Constants.ATTR_CURRENT_MINI_TAB, null);
			Date date = SessionUtils.getDate(Constants.ATTR_REPORT_DATE);
			log.debug("date=" + date);
			Dpr dpr = DprDAO.getInstance().findByDateAndProject(date, SessionUtils.getCurrentProject());
			if (dpr != null) {
				// Set the id for DprViewBean
				SessionUtils.put(DprViewBean.ATTR_DPR_ID, dpr.getId());
				return HeaderViewBean.REPORTS_MENU_DPR_VIEW;
			}
			else { // matching DPR not found - go to Report page
				return HeaderViewBean.REPORTS_MENU_REPORTS;
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Action method used to jump to a particular call sheet by date and unit
	 * number, typically from the Calendar page. The session attributes for the
	 * callsheet date and unit number must already be set.
	 */
	public String actionViewCallSheet() {
		try {
			Date callsheetDate = SessionUtils.getDate(Constants.ATTR_CALL_SHEET_DATE);
			Integer number = SessionUtils.getInteger(Constants.ATTR_UNIT_NUMBER);
			log.debug("date: " + callsheetDate + ", unit# " + number);
			Callsheet sheet = CallsheetDAO.getInstance().findByDateProjectUnitNumber(callsheetDate, SessionUtils.getCurrentProject(), number);
			if (sheet != null) {
				SessionUtils.put(Constants.ATTR_CALL_SHEET_ID, sheet.getId());
			}
		}
		catch (Exception e) {
			EventUtils.logError("exception: ", e);
		}
		SessionUtils.put(Constants.ATTR_CURRENT_MINI_TAB, null);
		return HeaderViewBean.SCHEDULE_MENU_CALLSHEET_VIEW;
	}

	/**
	 * Action method used from Home page to jump to one of the Reports
	 * page miniviews.  This interprets the ReportType set in the Home page
	 * via JumpBean's attribute/value pair, and pushes the appropriate
	 * ReportBean attribute value for the desired tab.
	 * @return the appropriate navigation string
	 */
	public String actionViewReport() {
		ReportType rtype = (ReportType)SessionUtils.get(ReportBean.ATTR_RPT_TYPE);
		String typeStr = null;
		switch(rtype) {
		case CALL_SHEET:
			typeStr = ReportBean.TYPE_DAILY_CALLSHEET;
			break;
		case DPR:
		case APPROVE_DPR:
			typeStr = ReportBean.TYPE_DAILY_DPR;
			break;
		case EXHIBIT_G:
			typeStr = ReportBean.TYPE_DAILY_EXHIBITG;
			break;
		default:
			break;
		}

		SessionUtils.put(ReportBean.ATTR_RPT_TYPE, typeStr);
		SessionUtils.put(Constants.ATTR_CURRENT_MINI_TAB, null);

		return HeaderViewBean.REPORTS_MENU_REPORTS;
	}

	/**
	 * Used in a JSP action= attribute to jump to a navigation string that has
	 * been set, typically via an f:setPropertyActionListener tag.
	 *
	 * @return the navigation string has been set in our {@link #jumpTo} field.
	 */
	public String actionJump() {
		return jumpTo;
	}

	/** See {@link #attribute}. */
	public String getAttribute() {
		return attribute;
	}
	/** See {@link #attribute}. */
	public void setAttribute(String attribute) {
		log.debug(attribute);
		this.attribute = attribute;
	}

	/** See {@link #value}. */
	public Object getValue() {
		return value;
	}
	/** See {@link #value}. */
	public void setValue(Object val) {
		value = val;
		if (attribute == null) {
			attribute = "currentRow";
		}
		SessionUtils.put(attribute, value);
		UserPrefBean.getInstance().put(attribute, value);
	}

	/** See {@link #jumpTo}. */
	public String getJumpTo() {
		return jumpTo;
	}
	/** See {@link #jumpTo}. */
	public void setJumpTo(String jumpTo) {
		this.jumpTo = jumpTo;
	}

}
