package com.lightspeedeps.web.payroll;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ContractDAO;
import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.dao.UnionsDAO;
import com.lightspeedeps.model.Contract;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Unions;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.view.ListView;
import com.lightspeedeps.web.view.SortHolder;
import com.lightspeedeps.web.view.SortableList;
import com.lightspeedeps.web.view.SortableListImpl;

/**
 * Backing bean for the Payroll "Contracts" page. This page allows the
 * user (typically a payroll or production accountant) to ...
 *
 */
@ManagedBean
@ViewScoped
public class ContractsBean extends ListView implements Serializable, SortHolder {
	/** */
	private static final long serialVersionUID = - 6190739835038434764L;

	private static final Log log = LogFactory.getLog(ContractsBean.class);


	/** The current Production. */
	private Production production;

	/** Drop-down list of years for selecting/filtering on the Contract
	 * Selection and Terms mini-tabs. */
	private List<SelectItem> selectYearDL; // applicable year of contract

	/** Selected beginning year of contracts' period. */
	private Integer year = 2015;

	// * * * Fields for CONTRACT SELECTION mini-tab * * *

	private List<SelectItem> unionsDL;

	private Integer unionId;

	/** The list of Contract`s associated with the current Production.  This list
	 * is displayed & sorted via the facilities in our ListView superclass. */
	private List<Contract> prodContracts;

	/** The List of Contract`s displayed on the right -- all contracts that
	 * have not been added to the current Production. */
	private final List<Contract> availableContractList = new ArrayList<>();

	/** The 'automatically' sorted list, accessed by the JSP. The SortableListImpl instance will call
	 * us back at our sort(List, SortableList) method to actually do a sort when necessary. */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private final SortableList sortedFullContractList = new SortableListImpl(this, (List)availableContractList, Contract.SORTKEY_NAME, true);

	private String fullContractCompareColumn;

	/**
	 * default constructor
	 */
	public ContractsBean() {
		super("name", "Payroll.Contracts.");
		log.debug("");

		if (getSelectYearDL().size() > 0) {
			year = (Integer)getSelectYearDL().get(getSelectYearDL().size()-1).getValue();
		}

		createAvailableContractList();
		checkTab();

		forceLazyInit();
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#actionEdit()
	 */
	@Override
	public String actionEdit() {
		forceLazyInit();
		return super.actionEdit();
	}

	/**
	 * The action method for the "Save" button.
	 *
	 * @see com.lightspeedeps.web.view.View#actionSave()
	 * @return null navigation string
	 */
	@Override
	public String actionSave() {
		String ret = null;
		try {
			ProductionDAO.getInstance().attachDirty(production);
			ret  = super.actionSave();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return ret;
	}

	/**
	 * The Action method for Cancel button while in Edit mode. Cleans up the
	 * state of the Production and WeeklyTimecard, and calls our superclass'
	 * actionCancel() method.
	 *
	 * @return null navigation string
	 */
	@Override
	public String actionCancel() {
		log.debug("");
		try {
			super.actionCancel();
			forceLazyInit();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Action method for adding selected contracts to the current production.
	 *
	 * @return null navigation string
	 */
	public String actionAddToProd() {
		log.debug("");
		production = ProductionDAO.getInstance().refresh(production);
		List<Contract> list = new ArrayList<>();
		for (Contract con : availableContractList) {
			if (con.getChecked()) {
				con = ContractDAO.getInstance().refresh(con);
				list.add(con);
			}
		}
		if (list.size() > 0) {
			ProductionDAO.getInstance().addContracts(getProduction(), list);
			prodContracts = null;
			createAvailableContractList();
		}
		else {
			MsgUtils.addFacesMessage("Contract.NoneSelected", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Action method for removing selected contracts from the current
	 * production.
	 *
	 * @return null navigation string
	 */
	public String actionRemoveFromProd() {
		log.debug("");
		production = ProductionDAO.getInstance().refresh(getProduction());
		List<Contract> list = new ArrayList<>();
		for (Contract con : getProdContracts()) {
			if (con.getChecked()) {
				con = ContractDAO.getInstance().refresh(con);
				list.add(con);
			}
		}
		if (list.size() > 0) {
			ProductionDAO.getInstance().removeContracts(production, list);
			prodContracts = null;
			createAvailableContractList();
		}
		else {
			MsgUtils.addFacesMessage("Contract.NoneSelected", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Ensure any fields necessary for re-rendering later are loaded
	 * while our entities are still in the Hibernate session.
	 */
	private void forceLazyInit() {
		production = ProductionDAO.getInstance().refresh(production);
		production.getPayrollPref().getAccountMajor(); // force init
	}

	// * * * LISTENER methods for the CONTRACT SELECTION tab

	/**
	 * ValueChangeListener method for the Year drop-down selection on the
	 * Contract SELECTION page. When this setting changes, we need to re-build
	 * the list of available contracts.
	 *
	 * @param event contains old and new values
	 */
	public void listenSelYearChange(ValueChangeEvent event) {
		try {
			if (event != null) {
				Integer yr = (Integer)event.getNewValue();
				if (yr != null) {
					year = yr;
					createAvailableContractList();
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
		}
	}

	/**
	 * ValueChangeListener method for the
	 *
	 * @param event contains old and new values
	 */
	public void listenProdCheckEntry(ValueChangeEvent event) {
		try {
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
		}
	}

	/**
	 * ValueChangeListener method for the
	 *
	 * @param event contains old and new values
	 */
	public void listenContractCheckEntry(ValueChangeEvent event) {
		try {
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
		}
	}

	/**
	 * ValueChangeListener method for the Union/Guild drop-down selection
	 * control. When this setting changes, we need to re-build the list of
	 * available contracts.
	 *
	 * @param event contains old and new values
	 */
	public void listenUnionChange(ValueChangeEvent event) {
		try {
			if (event != null) {
				Integer id = (Integer)event.getNewValue();
				if (id != null) {
					unionId = id;
					createAvailableContractList();
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
		}
	}

	/**
	 * Create the List of SelectItem`s representing the possibly year choices.
	 *
	 * @return A List of SelectItem`s, where the value is the year number, and
	 *         the label is the format "yearA-yearB", where 'yearB' is one more
	 *         than yearA.
	 */
	private List<SelectItem> createYearDL() {
		List<SelectItem> list = new ArrayList<>();
		List<Integer> years = ContractDAO.getInstance().findAllYears();
		for (Integer yr : years) {
			list.add(new SelectItem(yr, "" + yr + "-" + (yr+1)));
		}
		return list;
	}

	private void createAvailableContractList() {
		List<Contract> list;
		Unions union = null;
		if (unionId != null && unionId >= 0) {
			union = UnionsDAO.getInstance().findById(unionId);
			list = ContractDAO.getInstance().findByYearUnion(year, union.getUnionKey(), true);
		}
		else {
			list = ContractDAO.getInstance().findByYear(year, true);
		}
		for (Contract c : getProdContracts()) {
			list.remove(c);
		}
		availableContractList.clear();
		availableContractList.addAll(list);
	}

	/**
	 * Create the drop-down list of all unions in the database.
	 *
	 * @return A List of SelectItem`s, where the value is the Union.id, and the
	 *         label is the Union.name.
	 */
	private List<SelectItem> createUnionsDL() {
		List<SelectItem> list = new ArrayList<>();
		List<Unions> unions = UnionsDAO.getInstance().findAll();
		for (Unions union : unions) {
			list.add(new SelectItem(union.getId(), union.getName()));
		}
		list.add(0,new SelectItem(-1, "All"));
		unionId = -1;
		return list;
	}

	/**
	 * @return A sorted list of all the Contracts associated with the current
	 *         production.
	 */
	private List<Contract> createProdContracts() {
		List<Contract> list = new ArrayList<>(getProduction().getContracts());
		Collections.sort(list, getComparator());
		return list;
	}

	/**
	 * Method called to sort one of our SortableList objects.  We have to determine which
	 * one to know how to sort it!
	 *
	 * @see com.lightspeedeps.web.view.SortHolder#sort(java.util.List, com.lightspeedeps.web.view.SortableList)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void sort(List list, SortableList sortableList) {
		if (list != null) {
			if (sortableList == sortedFullContractList) {
				fullContractCompareColumn = sortableList.getSortColumnName();
				if (sortableList.isAscending()) {
					Collections.sort(list, fullContractComparator);
				}
				else {
					Collections.sort(list, Collections.reverseOrder(fullContractComparator));
				}
			}
		}
	}

	/**
	 * Sort Comparator for our right-side list of all available contracts.
	 */
	private final Comparator<Contract> fullContractComparator = new Comparator<Contract>() {
		@Override
		public int compare(Contract one, Contract two) {
			int ret = one.compareTo(two, fullContractCompareColumn);
			return ret;
		}
	};

	/**
	 * Sort comparator for our left-side list (of Production contracts).
	 *
	 * @see com.lightspeedeps.web.view.ListView#getComparator()
	 */
	@Override
	protected Comparator<Contract> getComparator() {
		Comparator<Contract> comparator = new Comparator<Contract>() {
			@Override
			public int compare(Contract c1, Contract c2) {
				return c1.compareTo(c2, getSortColumnName(), isAscending());
			}
		};
		return comparator;
	}

	/**
	 * @see com.lightspeedeps.web.view.SortHolder#isSortableDefaultAscending(java.lang.String)
	 */
	@Override
	public boolean isSortableDefaultAscending(String sortColumn) {
		return true; // make columns default to ascending sort
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#setSelected(java.lang.Object, boolean)
	 */
	@Override
	protected void setSelected(Object item, boolean b) {
		((Contract)item).setSelected(b);
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#getItemList()
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List getItemList() {
		return getProdContracts();
	}

	// accessors and mutators

	/**See {@link #production}. */
	public Production getProduction() {
		if (production == null) {
			production = SessionUtils.getProduction();
		}
		return production;
	}

	/**See {@link #selectYearDL}. */
	public List<SelectItem> getSelectYearDL() {
		if (selectYearDL == null) {
			selectYearDL = createYearDL();
		}
		return selectYearDL;
	}
	/**See {@link #selectYearDL}. */
	public void setSelectYearDL(List<SelectItem> selectYearDL) {
		this.selectYearDL = selectYearDL;
	}

	/**See {@link #year}. */
	public Integer getYear() {
		return year;
	}
	/**See {@link #year}. */
	public void setYear(Integer year) {
		this.year = year;
	}

	// * * * accessors & mutators for Contract Selection tab

	/**See {@link #unionsDL}. */
	public List<SelectItem> getUnionsDL() {
		if (unionsDL == null) {
			unionsDL = createUnionsDL();
		}
		return unionsDL;
	}
	/**See {@link #unionsDL}. */
	public void setUnionsDL(List<SelectItem> unionsDL) {
		this.unionsDL = unionsDL;
	}

	/**See {@link #unionId}. */
	public Integer getUnionId() {
		return unionId;
	}
	/**See {@link #unionId}. */
	public void setUnionId(Integer unionId) {
		this.unionId = unionId;
	}

	/**See {@link #prodContracts}. */
	public List<Contract> getProdContracts() {
		if (prodContracts == null) {
			prodContracts = createProdContracts();
		}
		return prodContracts;
	}

	/**See {@link #prodContracts}. */
	public void setProdContracts(List<Contract> prodContracts) {
		this.prodContracts = prodContracts;
	}

	/**See {@link #sortedFullContractList}. */
	public SortableList getSortedFullContractList() {
		return sortedFullContractList;
	}

}
