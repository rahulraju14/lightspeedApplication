package com.lightspeedeps.dao;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;
import javax.persistence.OrderBy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Office;
import com.lightspeedeps.type.OfficeType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.ServiceFinder;

/**
 	* A data access object (DAO) providing persistence and search support for Office entities.
 			* Transaction control of the save(), update() and delete() operations
		can directly support Spring container-managed transactions or they can be augmented	to handle user-managed Spring transactions.
		Each of these methods provides additional information for how to configure it for the desired type of transaction control.
	 * @see com.lightspeedeps.model.Office
  * @author MyEclipse Persistence Tools
 */
public class OfficeDAO extends BaseTypeDAO<Office>  {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(OfficeDAO.class);
		//property constants
	public static final String OFFICE_NAME = "officeName";
	public static final String OFFICE_CODE = "officeCode";
	public static final String EMAIL_ADDRESS = "emailAddress";
	public static final String OFFICE_TYPE = "officeType";

	public static OfficeDAO getInstance() {
		return (OfficeDAO)ServiceFinder.findBean("OfficeDAO");
	}

	public List<Office> findByOfficeName(Object officeName) {
		return findByProperty(OFFICE_NAME, officeName);
	}

	public List<Office> findByOfficeCode(Object officeCode) {
		return findByProperty(OFFICE_CODE, officeCode);
	}

	public List<Office> findByEmailAddress(Object emailAddress) {
		return findByProperty(EMAIL_ADDRESS, emailAddress);
	}

//	public List<Office> findByOfficeType(Object officeType) {
//		return findByProperty(OFFICE_TYPE, officeType);
//	}

	@SuppressWarnings("unchecked")
	public List<Office> findOffices(OfficeType officeType, String sortOrder) {
		String query = "from Office where officeType=?";

		if(sortOrder != null) {
			query += " order by " + sortOrder;
		}
		return find(query, officeType); // LS-1989
	}

	@SuppressWarnings("unchecked")
	@OrderBy("sortOrder")
	public List<SelectItem> createOfficeSelectList(OfficeType officeType) {
		List<SelectItem> itemList = new ArrayList<>();
		List<Office>offices;
		String query = "from Office where officeType=?";

		offices = find(query, officeType); // LS-1989
		itemList.add(Constants.EMPTY_SELECT_ITEM); // LS-2985
		for(Office office : offices) {
			itemList.add(new SelectItem(office.getId(), office.getOfficeName()));
		}
		return itemList;
	}
	
	@SuppressWarnings("unchecked")
	public Office findOffice(OfficeType officeType) {
		String query = "from Office where officeType=?";
		return findOne(query, officeType);
	}
}