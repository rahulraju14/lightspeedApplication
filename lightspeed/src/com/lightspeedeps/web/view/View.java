/**
 * File: View.java
 */
package com.lightspeedeps.web.view;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.User;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.util.HeaderViewBean;

/**
 * The intended superclass for most of our beans that back a web (JSF) page. It
 * contains fields and methods required by many of those beans. It includes
 * support for multiple tabs (based on ICEfaces panelTabset), and some standard
 * operations, such as Edit, Save, and Cancel.  It also handles some of the "resizing"
 * (setting container heights) required for our liquid layouts.
 * <p>
 * It includes some default support for the {@link PopupHolder} interface, which supports
 * most of our dialog boxes.
 * <p>
 * It does NOT include support for an "item list". That support is in the
 * {@link ListView} subclass.
 */
public class View extends Scroller implements PopupHolder, Serializable {
	/** */
	private static final long serialVersionUID = 5330835767374595690L;

	private static final Log log = LogFactory.getLog(View.class);

	/*
	 * The "resize()" JavaScript call is now inserted automatically on every
	 * page request by PageAuthenticatePhaseListener.
	 */
//	private static final String RESIZE_JS = "resize();";

	/** The current User viewing the page. */
	private User vUser;

	/** The current Contact viewing the page. */
	private Contact vContact;

	/** The zero-origin index of the right-hand pane's currently selected tab, often
	 * referred to as "mini-tabs".
	 * This is set via the < ice:panelTabSet selectedIndex=... > attribute. */
	private int selectedTab = 0;

	/** Used to set the desired tab from JSF/JSP; when setting via f:setPropertyActionListener,
	 * we can't pass an int (or Integer) literal, only Strings. */
	private String targetTab = "";

	/** True when we are in Edit (as opposed to View) mode; this will also
	 * be true when editing a NEW object. */
	protected boolean editMode;

	/** True if a "confirmation popup" fragment is being displayed. Currently
	 * this is only used in the mobile environment. */
	protected boolean showPopup;

	/** A prefix string typically added to message ids (which are looked up in the
	 * message resource file), allowing some of our methods, such as actionDelete,
	 *  to be independent of the particular subclass of the current object.  For example,
	 *  ContactListBean sets the messagePrefix to "Contact.". */
	private String messagePrefix;

	/** A prefix string used when generating Session attribute names, for example,
	 * the attribute for saving the selected mini-tab index.  By default, this is set
	 * to the same value as the messagePrefix. */
	private String attributePrefix;

//	protected transient HttpSession session = SessionUtils.getHttpSession();

	/**
	 * Our only constructor. A message prefix string should be supplied.
	 *
	 * @param prefix The message id prefix, typically used by methods that
	 *            provide standard functions (such as Delete), often with dialog
	 *            boxes. The prefix is used to create a full message id which
	 *            will be looked up in our messageResources.properties file. By
	 *            convention, the supplied prefix should end with a period. The
	 *            string usual reflects the primary type of item or function of
	 *            the page being backed, e.g., "Project." or "Contact.".
	 *            Initially, the {@link #attributePrefix} is also set to this value.
	 */
	public View(String prefix) {
		messagePrefix = prefix;
		attributePrefix = prefix;
	}

	/**
	 * See if there is a saved mini-tab index value in our Session, and if so,
	 * go to that tab.
	 */
	protected void checkTab() {
		String attr = Constants.ATTR_CURRENT_MINI_TAB + "." + getAttributePrefix();
		Integer tab = SessionUtils.getInteger(attr);
		if (tab != null) {
			setSelectedTab(tab);
		}
	}

	/**
	 * Clear all the editable (input) fields that are children of the given editPanel field.
	 * 'editPanel' is usually bound to an h:panelGroup that encompasses all the edit fields on a
	 * page.
	 */
	public static void clearEditFields(UIComponent editPanel) {
		//log.debug(editPanel);
		if (editPanel != null) {
			clearEditChildren(editPanel.getChildren());
		}
	}

	private static void clearEditChildren(List<UIComponent> list) {
		for (UIComponent c : list) {
			//log.debug(c);
			if (c instanceof EditableValueHolder) {
				//log.debug(c.getClientId(FacesContext.getCurrentInstance()));
				EditableValueHolder ev = (EditableValueHolder)c;
//				if (ev.getSubmittedValue()!=null) {
//					log.debug(ev.getSubmittedValue());
//				}
				ev.setSubmittedValue(null);
				ev.setValue(null);
				ev.setLocalValueSet(false);
			}
			else if (c.getChildren() != null) {
				clearEditChildren(c.getChildren());
			}
//			else {
//				Class x = c.getClass();
//				log.debug(x.getName());
//			}
		}
	}

//	/**
//	 * Append a JavaScript call to the "resize()" routine, which generally takes
//	 * care of resizing liquid containers that frequently get reset to their "initial"
//	 * size after any pop-up is used.
//	 */
//	public static void addClientResize() {
		// The resize() is now added automatically on every render-response by
		// the ResizePhaseListener.
//		addJavascript(RESIZE_JS);
//	}

	/**
	 * Add a call to the "buttonClicked()" JavaScript function.  This is part of the
	 * system that fixes the ICEfaces bug where a button pressed by a user does not
	 * result in the button's action being called.  Essentially, this lets the
	 * browser client know that a button action was processed.
	 */
	public static void addButtonClicked() {
		String script = "buttonClicked();";
		addJavascript(script);
	}

	/**
	 * Append focus-setting JavaScript code to the response stream, to be
	 * executed on the client browser. This will invoke the 'focusOn' javaScript
	 * function, defined in the set of standard LightSPEED JavaScript functions.
	 *
	 * @param promptType The String which will be used as the leading part of
	 *            the element id, of the element which is to receive the focus.
	 *            The element id should be "<promptType>_focus".
	 */
	public static void addFocus(String promptType) {
		addJavascript("focusOn('" + promptType + "');");
	}

	/**
	 * The Action method of the "Edit" button while in View mode.
	 *
	 * @return null navigation string, since we normally support Edit mode on
	 *         the same page as View mode.
	 */
	@Override
	public String actionEdit() {
		log.debug("");
		editMode = true;
//		addClientResize();
		return super.actionEdit();
	}

	/**
	 * Standard activities for all subclasses when a Save completes.
	 *
	 * @return null navigation string
	 */
	@Override
	public String actionSave() {
		setEditMode(false);
//		addClientResize();
		return super.actionSave();
	}

	/**
	 * The Action method for Cancel button while in Edit mode.
	 *
	 * @return null navigation string
	 */
	@Override
	public String actionCancel() {
		log.debug("");
		editMode = false;
//		addClientResize();  // handled globally now
		addButtonClicked(); // tell the client we processed a button click.
		return super.actionCancel();
	}

	/**
	 * Called when the user switches tabs.  May be overridden by subclasses.
	 */
	protected void setupTabs() {
	}

	/**
	 * Called when user clicks "Ok" (or equivalent) on a standard confirmation
	 * dialog. May be overridden by subclasses if they need to do any processing
	 * beyond just letting the dialog box close.
	 *
	 * @return null navigation string
	 *
	 * @see com.lightspeedeps.web.popup.PopupHolder#confirmOk(int)
	 */
	@Override
	public String confirmOk(int action) {
		return null;
	}

	/**
	 * Called when user clicks "Cancel" on a standard confirmation dialog. May
	 * be overridden by subclasses if they need to do any processing beyond just
	 * letting the dialog box close.
	 *
	 * @return null navigation string
	 *
	 * @see com.lightspeedeps.web.popup.PopupHolder#confirmCancel(int)
	 */
	@Override
	public String confirmCancel(int action) {
//		addClientResize();
		return null;
	}

	/**
	 * @return A Comparator for comparing SelectItem`s (used in drop-down lists)
	 *         based on their label values.
	 */
	public static Comparator<SelectItem> getSelectItemComparator() {
		return selectItemComparator;
	}

	private static Comparator<SelectItem> selectItemComparator = new Comparator<SelectItem>() {
		@Override
		public int compare(SelectItem one, SelectItem two) {
			return one.getLabel().compareToIgnoreCase(two.getLabel());
		}
	};

	/**
	 * Create a Map<String, Object> for use in named-query processing, and add
	 * an entry formed from the parameters given.
	 *
	 * @param label The parameter name used in the query; this will be the key in
	 *            the Map entry created.
	 * @param value The object to be referenced in the query; this will be the
	 *            value in the Map <key,value> pair.
	 * @return A new instance of a Map containing the single entry generated
	 *         from the parameters.
	 */
	protected Map<String, Object> map(String label, Object value) {
		Map<String, Object> valueMap = new HashMap<String, Object>();
		valueMap.put(label, value);
		return valueMap;
	}

	// * * * Get/Set methods

	/** See {@link #messagePrefix}. */
	public String getMessagePrefix() {
		return messagePrefix;
	}
	/** See {@link #messagePrefix}. */
	public void setMessagePrefix(String messagePrefix) {
		this.messagePrefix = messagePrefix;
	}

	/**See {@link #attributePrefix}. */
	public String getAttributePrefix() {
		return attributePrefix;
	}
	/**See {@link #attributePrefix}. */
	public void setAttributePrefix(String attributePrefix) {
		this.attributePrefix = attributePrefix;
	}

	/** See {@link #selectedTab}. */
	public int getSelectedTab() {
		return selectedTab;
	}
	/** See {@link #selectedTab}. */
	public void setSelectedTab(int n) {
		log.debug(n);
//		if (selectedTab != n) {
//			addClientResize();
//		}
		selectedTab = n;
		String attr = Constants.ATTR_CURRENT_MINI_TAB + "." + getAttributePrefix();
		SessionUtils.put(attr, selectedTab);
		HeaderViewBean.getInstance().setMiniTab(n);
		setupTabs();
	}

	/** See {@link #targetTab}. */
	public String getTargetTab() {
		return targetTab;
	}
	/** See {@link #targetTab}. */
	public void setTargetTab(String s) {
		targetTab = s;
		try {
			setSelectedTab(Integer.parseInt(s));
		}
		catch (Exception e) {
			EventUtils.logError("invalid argument to setTargetTab -- check jsp!");
		}
	}

	/** See {@link #editMode}. */
	public boolean getEditMode() {
		return editMode;
	}
	/** See {@link #editMode}. */
	public void setEditMode(boolean editMode) {
		this.editMode = editMode;
	}

	/** See {@link #showPopup}. */
	public boolean getShowPopup() {
		return showPopup;
	}
	/** See {@link #showPopup}. */
	public void setShowPopup(boolean showPopup) {
		this.showPopup = showPopup;
	}

	/**See {@link #vUser}. */
	public User getvUser() {
		if (vUser == null) {
			vUser = SessionUtils.getCurrentUser();
		}
		return vUser;
	}
	/**See {@link #vUser}. */
	public void setvUser(User vUser) {
		this.vUser = vUser;
	}

	/**See {@link #vContact}. */
	public Contact getvContact() {
		if (vContact == null) {
			if (SessionUtils.getProduction() != null) {
				vContact = SessionUtils.getCurrentContact();
			}
		}
		return vContact;
	}
	/**See {@link #vContact}. */
	public void setvContact(Contact vContact) {
		this.vContact = vContact;
	}

}
