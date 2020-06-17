/**
 * File: MobileUserBean.java
 */
package com.lightspeedeps.web.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.object.Item;
import com.lightspeedeps.type.AccessStatus;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.payroll.TimecardUtils;
import com.lightspeedeps.web.util.HeaderViewBean;

/**
 * This bean backs several pages used by mobile devices when not entered
 * into a production, such as My Account, Change Password, etc.
 */
@ManagedBean
@ViewScoped
public class MobileUserBean implements Serializable {
	/** */
	private static final long serialVersionUID = 580652710708673574L;

	private static final Log log = LogFactory.getLog(MobileUserBean.class);

	/** The Production currently selected by the user. The list of
	 * positions available will depend on this value. */
	private transient Production production;

	/** The database id of the Production selected by the user on the
	 * My Productions page.  Set via the JSP. */
	private Integer productionId;

	/** The currently selected category of Productions to be displayed
	 * (Active, Inactive, or All) on the "My Timecards" mobile page. */
	private String category = Constants.CATEGORY_ALL;

	/** The List of productions that the current User has access to.  The
	 * Item.id value will be the Production.id value. */
	private transient List<Item> productions;

	/** The default (and only) constructor. */
	public MobileUserBean() {
		loadValues();
	}

//	/**
//	 * Action method called when the user clicks on a production button in the
//	 * My Productions page. The productionId has been set by a
//	 * f:setPropertyActionListener in the JSP.
//	 *
//	 * @return Navigation string which is null if the production could not be
//	 *         entered, or the appropriate string to jump to the My Positions
//	 *         page.
//	 * OBSOLETE: replaced by ProductionContactBean::actionEnterMobileProduction()
//	 */
//	public String actionSelectProduction() {
//		SessionUtils.put(Constants.ATTR_TC_TCUSER_ID, null); // force tcUser to default
//		String ret = "mypositionsm";
//		if (! selectProduction()) {
//			ret = null;
//		}
//		return ret;
//	}

	/**
	 * Action method called when the user clicks on a production button in the
	 * My Timecards page. The productionId has been set by a
	 * f:setPropertyActionListener in the JSP.
	 *
	 * @return Navigation string which is null if the production could not be
	 *         entered, or the appropriate string to jump to the Timecard List
	 *         page.
	 */
	public String actionSelectProdTimecards() {
		SessionUtils.put(Constants.ATTR_TC_TCUSER_ID, null); // force tcUser to default
		SessionUtils.put(Constants.ATTR_CONTACT_ID, null);
		String ret = "timecardlistm";
		if (! selectProduction()) {
			ret = null;
		}
		else if (production.getType().hasPayrollByProject()) {
			ret = HeaderViewBean.MYPROJECTS_PAGE;
		}
		SessionUtils.put(Constants.ATTR_TCS_BACK_PAGE, HeaderViewBean.MYTIMECARDS_PAGE);
		return ret;
	}

	private boolean selectProduction() {
		boolean ret = true;
		if (productionId != null) {
			if (enterProduction(productionId)) {
				// set attribute so PageAuthenticatePhaseListener lets us in!
				SessionUtils.put(Constants.ATTR_ENTERING_PROD, 1);
			}
			else {
				ret = false;
			}
			saveValues();
		}
		return ret;
	}

	/**
	 * Called to place the user into a Production.
	 *
	 * @param id The database id of the Production to be entered.
	 * @return True if the user has been placed into the Production; false if
	 *         not, which can occur if the user does not have access rights to
	 *         the production.
	 */
	private boolean enterProduction(Integer id) {
		production = TimecardUtils.enterProductionForTimecards(id);
		return (production != null);
	}

	/**
	 * Action method for the "Full Site" link on the mobile pages. We clear the
	 * "saved page" information, to prevent our normal code from trying to
	 * forward us back to a mobile page; and return the JSF navigation string
	 * for the desktop home, "My Productions".
	 *
	 * @return The "My Productions" (desktop) navigation string.
	 */
	public String actionFullSite() {
		SessionUtils.put(Constants.ATTR_SAVED_PAGE_INFO, null);
		return HeaderViewBean.MYPROD_MENU_PROD; // go to "my productions" desktop page
	}

	/**
	 * The Value Change Listener for the category (production type or status)
	 * selection drop-down list on the "My Timecards" production list display.
	 *
	 * @param evt The event created by the framework.
	 */
	public void listenCategoryChange(ValueChangeEvent evt) {
		try {
			if (evt.getNewValue() != null) {
				category = (String)evt.getNewValue();
				UserPrefBean.getInstance().put(Constants.ATTR_MYPROD_CATEGORY, category);
				createProductionList();
			}
			else {
				log.warn("Null newValue in category change event: " + evt);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Recover our saved information from the Session, and load the appropriate
	 * database objects, e.g., Production, WeeklyTimecard.
	 */
	private void loadValues() {
		Production cProd = SessionUtils.getProduction();
		if (cProd != null && ! cProd.isSystemProduction()) {
			Integer id = UserPrefBean.getInstance().getInteger(Constants.ATTR_LAST_PROD_ID);
			if (id != null) {
				production = ProductionDAO.getInstance().findById(id);
//				if (! cProd.equals(production)) { // it's not the current one
//					if (enterProduction(id)) {	// so try to log us in.
//						// set attribute so PageAuthenticatePhaseListener lets us in!
//						SessionUtils.put(Constants.ATTR_ENTERING_PROD, 1);
//					}
//				}
			}
			else {
				production = null;
			}
		}
		category = Constants.CAT_ACTIVE; // Mobile page list defaults to "Active"
		category = UserPrefBean.getInstance().getString(Constants.ATTR_MYPROD_CATEGORY, category);
	}

	/**
	 * Save our current state to the user's HTTP Session.
	 */
	private void saveValues() {
		UserPrefBean.getInstance().put(Constants.ATTR_LAST_PROD_ID,
				production == null ? null : production.getId());
	}

	/**
	 * Create the List of Item`s used on the "Productions" list on
	 * the My Timecards page.  The list is sorted in descending start-date order.
	 */
	private void createProductionList() {
		productions = new ArrayList<>();
		List<Production> prods = ProductionDAO.getInstance().findByUser(SessionUtils.getCurrentUser());
		Collections.sort(prods, getProductionComparator());
		boolean showAll = category.equals(Constants.CATEGORY_ALL);
		boolean showActive = category.equals(Constants.CAT_ACTIVE);
		for (Production prod : prods) {
			if (! prod.isSystemProduction()) {
				if (showAll || ((prod.getStatus()==AccessStatus.ACTIVE) == showActive)) {
					productions.add(new Item(prod.getId(), prod.getTitle()));
				}
			}
		}
	}

	/**
	 * @return The comparator used for sorting the list on the "My Productions"
	 *         mobile page. This is by Production start date, descending.
	 */
	private static Comparator<Production> getProductionComparator() {
		Comparator<Production> comparator = new Comparator<Production>() {
			@Override
			public int compare(Production c1, Production c2) {
				return c1.compareTo(c2, Production.SORTKEY_START, false);
			}
		};
		return comparator;
	}

	/** See {@link #productionId}. */
	public Integer getProductionId() {
		return productionId;
	}
	/** See {@link #productionId}. */
	public void setProductionId(Integer productionId) {
		this.productionId = productionId;
	}

	/** See {@link #productions}. */
	public List<Item> getProductions() {
		if (productions == null) {
			createProductionList();
		}
		return productions;
	}
	/** See {@link #productions}. */
	public void setProductions(List<Item> productions) {
		this.productions = productions;
	}

	public String getCategory() {
		return category;
	}
	/** This is only used by the framework, and we need to IGNORE that, because we
	 * may have changed the category during an earlier phase of the life-cycle. */
	public void setCategory(String category) {
		//this.category = category;
	}

	public List<SelectItem> getProductionStatusDL() {
		return Constants.PRODUCTION_STATUS_DL;
	}
}