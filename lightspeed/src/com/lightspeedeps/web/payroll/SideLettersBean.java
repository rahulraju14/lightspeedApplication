package com.lightspeedeps.web.payroll;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.dao.SideLetterDAO;
import com.lightspeedeps.model.Contract;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.SideLetter;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.web.view.ListView;

/**
 * Backing bean for the Payroll "Contracts" page. This page allows the
 * user (typically a payroll or production accountant) to ...
 *
 */
@ManagedBean
@ViewScoped
public class SideLettersBean extends ListView implements Serializable  {
	/** */
	private static final long serialVersionUID = - 6190739835038434764L;

	private static final Log log = LogFactory.getLog(SideLettersBean.class);

	/** True iff we have initialized ourselves. */
	private boolean initialized = false;

	/** The current Production. */
	private Production production;

	/** The list of Contract`s associated with the current Production.  This list
	 * is displayed & sorted via the facilities in our ListView superclass. */
	private List<Contract> prodContracts;

	// * * * Fields for Side Letter selection mini-tab * * *

	/** List of Contract Codes to be used for column headers */
	List<String> contractCodes;

	/** List of rows for the page; one per side letter */
	private List<SideLetterData> rows;

	private static ProductionDAO productionDAO;

	/**
	 * default constructor
	 */
	public SideLettersBean() {
		super("name", "Payroll.SideLetters.");
		log.debug("");
		contractCodes = null;
		rows = null;
		checkTab();
	}

	/**
	 * Handle most initialization of the bean. This is delayed until we are
	 * called via a JSP reference on the Approval Paths mini-tab, to avoid the
	 * initialization overhead when the mini-tab is not being rendered.
	 */
	private void init() {
		try {
			initialized = true;
			getProduction();
			getContractCodes();
			getRows();
			forceLazyInit();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
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
			//Collection of contracts with associated in a comma delimited String.
			Map<String, String>contractSideLettersString = new HashMap<>();

			// Collection of contracts with associated list of side letters.
			// This collection will be used to generated the comma delimited string.
			Map<String, List<String>>contractSideLettersList = new HashMap<>();

			// Initialize the map with lists
			for(int i = 1; i < contractCodes.size(); i++) {
				contractSideLettersList.put(contractCodes.get(i), new ArrayList<String>());
			}
			// Iterate through the rows and save the checked cells.
			for(SideLetterData slData : rows) {
				String slCode = slData.getCode();

				List<Boolean>selectedContracts = slData.getSideLettersSelected();

				for(int i = 1; i < contractCodes.size(); i++) {
					Boolean selected = selectedContracts.get(i - 1);

					if(selected) {
						String contractCode = contractCodes.get(i);

						contractSideLettersList.get(contractCode).add(slCode);
					}
				}
			}

			Set<Entry<String, List<String>>> entrySet = contractSideLettersList.entrySet();
			Iterator<Entry<String, List<String>>> itr = entrySet.iterator();

			while(itr.hasNext()) {
				Entry<String, List<String>> entry = itr.next();
				String contractCode = entry.getKey();
				if (entry.getValue().size() > 0) {
					String sideLetters = StringUtils.getStringFromList(entry.getValue(), ",");
					contractSideLettersString.put(contractCode, sideLetters);
				}
			}

			production = getProductionDAO().refresh(getProduction());
			production.setContractSideLetters(contractSideLettersString);

			getProductionDAO().attachDirty(production);

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
	 * Ensure any fields necessary for re-rendering later are loaded
	 * while our entities are still in the Hibernate session.
	 */
	private void forceLazyInit() {
		production = getProductionDAO().refresh(getProduction());
		production.getPayrollPref().getAccountMajor(); // force init
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

	private void createContractCodesList() {
		contractCodes = new ArrayList<>();
		production = getProductionDAO().refresh(getProduction());
		if (getProdContracts() != null) {
			// Add "Side Letters" column for first row of table.
			contractCodes.add("Side Letters");
			for (Contract contract : prodContracts) {
				contractCodes.add(contract.getContractCode());
			}
		}
	}

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

	/**See {@link #prodContracts}. */
	private List<Contract> getProdContracts() {
		if (prodContracts == null) {
			prodContracts = createProdContracts();
		}
		return prodContracts;
	}
//	/**See {@link #prodContracts}. */
//	public void setProdContracts(List<Contract> prodContracts) {
//		this.prodContracts = prodContracts;
//	}

	/** See {@link #rows}. */
	public List<SideLetterData> getRows() {
		if (initialized && rows == null) {
			rows = new ArrayList<>();
			List<SideLetter> sideLetters = SideLetterDAO.getInstance().findAll();
			production = getProductionDAO().refresh(getProduction());
			Map<String, List<String>> sideLettersByContract =
					getProduction().getContractSideLettersDisplayMap();
			getContractCodes();

			for (SideLetter sideLetter : sideLetters) {
				SideLetterData sideLetterData = new SideLetterData(sideLetter, contractCodes, sideLettersByContract);
				if (sideLetterData.getContractMatched()) {
					rows.add(sideLetterData);
				}
			}
		}
		return rows;
	}

	public void setRows(ArrayList<SideLetterData> rows) {
		this.rows = rows;
	}

	/** See {@link #contractCodes}. */
	public List<String> getContractCodes() {
		if (contractCodes == null) {
			createContractCodesList();
		}
		return contractCodes;
	}
	/** See {@link #contractCodes}. */
	public void setContractCodes(List<String> contractCodes) {
		this.contractCodes = contractCodes;
	}

	private ProductionDAO getProductionDAO() {
		if (productionDAO == null) {
			productionDAO = ProductionDAO.getInstance();
		}
		return productionDAO;
	}

	/**
	 * Called due to hidden field value in our JSP page. Make sure we're
	 * initialized to show something. Note that this must be public as
	 * it is called from JSP.
	 */
	public boolean getSetUp() {
		try {
			if (! initialized) {
				init();
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return false;
	}

	/**
	 * This class represents the information about one side-letter. It holds the
	 * name and code (unique key/abbreviation) of the side-letter, and a list of
	 * the contractCode values for all contracts in which this side-letter
	 * exists.
	 * <p>
	 * A matching List of boolean values indicate which of those side-letters
	 * (for each contract) have been selected to be applied during HTG
	 * processing for the current Production.
	 * <p>
	 * Each instance of this class is presented on the UI as one row.
	 * Check-boxes will appear wherever the side-letter exists in the given
	 * contract (represented by a column). The check-box is checked if the
	 * side-letter is currently applied within this Production.
	 */
	public class SideLetterData {
		private String name;
		private String code;

		private boolean contractMatched = false;

		private List<String> slContractCodes;

		/** List of Boolean values indicating if a given contract's entry is
		 * selected. This corresponds to the check-box values across one row. */
		private List<Boolean> sideLettersSelected;

		/** List of Boolean values indicating if a given contract can be
		 * associated with this side-letter. If this is false, no checkbox
		 * should be displayed, as the selection is not a valid choice. */
		private List<Boolean> contractValid;

		/**
		 * The normal constructor.
		 *
		 * @param label The 'friendly' label for this side-letter.
		 * @param code The unique code (across all side-letters) assigned to
		 *            this side-letter.
		 * @param contractCodes The List of the contract codes being presented
		 *            in the UI. It represents all the contracts that are
		 *            currently assigned to this Production; that list is
		 *            maintained on the Contract Selection page.
		 * @param sideLettersByContract A Map from a contractCode to a List of
		 *            side-letter codes that are currently applied to that
		 *            contract within the current Production. The contents of
		 *            the List are used to determine which check-boxes are
		 *            checked.
		 */
		public SideLetterData(SideLetter sideLetter, List<String> contractCodes, Map<String, List<String>> sideLettersByContract) {
			name = sideLetter.getSideLetterDesc();
			code = sideLetter.getSideLetterCode();
			slContractCodes = new ArrayList<>();
			if (! StringUtils.isEmpty(sideLetter.getContracts())) {
				if (sideLetter.getContracts().equalsIgnoreCase("All")) {
					slContractCodes = contractCodes;
				}
				else {
					String[] codes = sideLetter.getContracts().split(",");
					slContractCodes = new ArrayList<>(Arrays.asList(codes));
				}
			}
			sideLettersSelected = new ArrayList<>();
			contractValid = new ArrayList<>();

			// Skip the first entry because it is blank for using on the side letter page.
			for (int i = 1; i < contractCodes.size(); i++) {
				String contractCode = contractCodes.get(i);
				List<String> sideLetters = sideLettersByContract.get(contractCode);
				Boolean selected = false;
				Boolean valid = false;

				if (sideLetters != null) {
					if (sideLetters.contains(code)) {
						selected = true;
						valid = true;
					}
				}
				valid = valid || slContractCodes.contains(contractCode);
				sideLettersSelected.add(selected);
				contractValid.add(valid);
				contractMatched |= valid;
			}
		}

		/** See {@link #contractMatched}. */
		public boolean getContractMatched() {
			return contractMatched;
		}

		/** See {@link #sideLettersSelected}. */
		public List<Boolean> getSideLettersSelected() {
			return sideLettersSelected;
		}
		/** See {@link #sideLettersSelected}. */
		public void setSideLettersSelected(List<Boolean> sideLettersSelected) {
			this.sideLettersSelected = sideLettersSelected;
		}

		/** See {@link #contractValid}. */
		public List<Boolean> getContractValid() {
			return contractValid;
		}

		/** See {@link #name}. */
		public String getName() {
			return name;
		}

		/** See {@link #slContractCodes}. */
		public List<String> getSlContractCodes() {
			return slContractCodes;
		}

		/** {@link #code}. */
		public String getCode() {
			return code;
		}
	}
}
