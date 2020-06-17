/**
 * File: ContractUtils.java
 */
package com.lightspeedeps.util.payroll;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.lightspeedeps.dao.ContractDAO;
import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.model.Contract;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.util.app.SessionUtils;

/**
 *
 */
public class ContractUtils {
	/** Sort comparator for Contracts - sorts ascending by name. */
	private static Comparator<Contract> comparator = new Comparator<Contract>() {
		@Override
		public int compare(Contract c1, Contract c2) {
			return c1.compareTo(c2, ContractDAO.NAME, true);
		}
	};

	/**See {@link #comparator}. */
	private static Comparator<Contract> getComparator() {
		return comparator;
	}

	/**
	 * Create sorted list of Contract`s that have been assigned to the
	 * current Production. Sets {@link #activeContractList}.
	 */
	public static List <Contract> createActiveContractList(Production prod) {
		if(prod == null) {
			prod = SessionUtils.getProduction();
		}
		prod = ProductionDAO.getInstance().refresh(prod);
		List <Contract> activeContractList = new ArrayList<Contract>(prod.getContracts());
		activeContractList.size();
		Collections.sort(activeContractList, ContractUtils.getComparator());

		return activeContractList;
	}

	/** See {@link #activeContractList}. */
	public static List<Contract> getActiveContractList(Production prod) {
		return createActiveContractList(prod);
	}

	/**
	 * @return True if this production has the VideoTape ("VT") contract assigned.
	 */
	public static Boolean calculateUsesVideoTape(Production prod) {
		List<Contract> list = ContractUtils.getActiveContractList(prod);

		boolean vt = false;
		for (Contract c : list) {
			if (c.getUnionKey().contains("VT")) {
				vt = true;
				break;
			}
		}
		return vt;
	}

}
