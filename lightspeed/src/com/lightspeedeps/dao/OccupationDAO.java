package com.lightspeedeps.dao;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Occupation;
import com.lightspeedeps.model.PayrollPreference;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.Unions;
import com.lightspeedeps.type.MediumType;
import com.lightspeedeps.type.PayrollProductionType;
import com.lightspeedeps.type.StudioType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.payroll.ContractUtils;

/**
 * A data access object (DAO) providing persistence and search support for
 * Occupation entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 * <p>
 * Note that the methods which create drop-down lists all have a secondary sort
 * key of "id". This is because we often have to filter the lists to remove
 * duplicate entries, and only insert the first one of a set of duplicates. By
 * sorting all lists by id, the selection lists should have the same id for a
 * given occupation, whether it is for occupation codes or occupation names.
 * This makes the code in StartFormBean, which is trying to match up the
 * occupation selection with the occ-code selection, AND vice-versa, reliable.
 * <p>
 * Without the additional id sort we could have, for example, an occupation
 * selection of "Best Boy" with an id of 500 (which duplicates 501), whereas the
 * occ-code selection list created could have an id of 501. When the user
 * selects the occ-code, StartFormBean would try to match the id of 501 to an
 * entry in the occupation list and fail.
 *
 * @see com.lightspeedeps.model.Occupation
 */
public class OccupationDAO extends BaseTypeDAO<Occupation> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(OccupationDAO.class);

	//property constants
	private static final String OCC_UNION = "occUnion";
	private static final String CONTRACT_CODE = "contractCode";
	private static final String OCC_CODE = "occCode";
	private static final String LS_OCC_CODE = "lsOccCode";
	private static final String NAME = "name";
	private static final String REGION = "region";
	private static final String MAJOR_INDIE = "majorIndie";
	private static final String FEATURE_TV = "featureTv";
	private static final String AGREEMENT = "agreement";
	private static final String PROD_TYPE = "prodType";
	private static final String SEASON = "season";
	private static final String START_RANGE = "startRange";

	public static OccupationDAO getInstance() {
		return (OccupationDAO)getInstance("OccupationDAO");
	}

	/**
	 * Find the Occupation that matches the supplied parameters. Since the
	 * combination of the three values is required to be unique by the table,
	 * then only one will exist if all three parameters are supplied.
	 *
	 * @param prod The Production containing the Occupation entry; this is used
	 *            to narrow the selection by attributes of the production such
	 *            as TV vs Feature, region, etc.
	 * @param project The Project of interest; if null, ignore the timecard's
	 *            Project affiliation; should be non-null only for Commercial productions.
	 * @param occUnion The code for a Union used to look up its occupations.
	 *            This may be a superset, as in the case of ASA locals, or it
	 *            may be a subset, like '700E' for Editors.
	 * @param jobClass The job classification (position name).
	 * @param lsOccupationCode The LS occupation code, which may be different
	 *            from the official (PayMaster) occupation code; may be null.
	 * @return The single (we hope!) Occupation entity matching this
	 *         combination. If a null lsOccupationCode is supplied, then
	 *         multiple values may match, and an arbitrary entry is returned
	 *         from the set matching the unionCode and jobClass parameters.
	 */
	public Occupation findByUnionJobAndOccCode(Production prod, Project project, String occUnion, String jobClass, String lsOccupationCode) {
		String queryString = "from Occupation where " +
				createProductionQualifier(prod, project) +
				OCC_UNION + " = ? and " +
				NAME + " = ? ";
		Occupation occupation = findByUnionJobOccCode(queryString, occUnion, jobClass, lsOccupationCode);
		if (occupation == null) { // not found - repeat query without production type qualifiers
			queryString = "from Occupation where " +
					OCC_UNION + " = ? and " +
					NAME + " = ? ";
			occupation = findByUnionJobOccCode(queryString, occUnion, jobClass, lsOccupationCode);
		}
		return occupation;
	}

	/**
	 * Issue the query for an occupation based on the passed parameters.
	 * @param queryString
	 * @param occUnion
	 * @param jobClass
	 * @param lsOccupationCode
	 * @return The matching Occupation entry, or null if not found.
	 */
	private Occupation findByUnionJobOccCode(String queryString, String occUnion, String jobClass, String lsOccupationCode) {
		Occupation occupation;
		if (lsOccupationCode == null) {
			Object[] values = { occUnion, jobClass };
			occupation = findOne(queryString, values);
		}
		else {
			queryString += " and " + LS_OCC_CODE + " = ? ";
			Object[] values = { occUnion, jobClass, lsOccupationCode };
			occupation = findOne(queryString, values);
		}
		return occupation;
	}

	/**
	 * Find the Occupation that matches the supplied parameters.
	 *
	 * @param prod The Production containing the Occupation entry; this is used
	 *            to narrow the selection by attributes of the production such
	 *            as TV vs Feature, region, etc.
	 * @param project The Project of interest; if null, ignore the timecard's
	 *            Project affiliation; should be non-null only for Commercial productions.
	 * @param occUnion The code for a Union used to look up its occupations.
	 *            This may be a superset, as in the case of ASA locals, or it
	 *            may be a subset, like '700E' for Editors.
	 * @param lsOccupationCode The LS occupation code, which may be different
	 *            from the official (PayMaster) occupation code.
	 * @return The single (we hope!) Occupation entity matching this
	 *         combination. If multiple values match, an arbitrary entry is
	 *         returned from the set.
	 */
	public Occupation findByUnionAndOccCode(Production prod, Project project, String occUnion, String lsOccupationCode) {
		String queryString = "from Occupation where " +
				createProductionQualifier(prod, project) +
				OCC_UNION + " = ? and " +
				LS_OCC_CODE + " = ? ";

		Object[] values = { occUnion, lsOccupationCode };

		Occupation occupation = findOne(queryString, values);

		if (occupation == null) { // not found - repeat query without production type qualifiers
			queryString = "from Occupation where " +
					OCC_UNION + " = ? and " +
					LS_OCC_CODE + " = ? ";
			occupation = findOne(queryString, values);
		}
		return occupation;
	}

	/**
	 * Create a List of SelectItem to populate a drop-down list of occupations
	 * that exist within a particular union. The list is in name order with
	 * duplicate names removed.
	 *
	 * @param prod The Production containing the Occupation entry; this is used
	 *            to narrow the selection by attributes of the production such
	 *            as TV vs Feature, region, etc.
	 * @param project The Project of interest; if null, ignore the timecard's
	 *            Project affiliation; should be non-null only for Commercial
	 *            productions.
	 * @param union The Union of interest.
	 * @return A non-null (but possibly empty) List of selections.
	 */
	@SuppressWarnings("unchecked")
	public List<SelectItem> createOccupationDLbyUnion(Production prod, Project project, Unions union) {


		List<SelectItem> list = new ArrayList<>();
		List<Object> valueList = new ArrayList<>();
		valueList.add(union.getOccupationUnion());

		String contractQuery = "";
		if (prod.getPayrollPref().getUse30Htg()) { // Use 3.0 "full" HTG processing...
			// restrict occupation list to assigned contracts
			contractQuery = prod.getContractCodes();
			if (contractQuery.length() > 0) {
				contractQuery = " and " + CONTRACT_CODE + " in ( " + contractQuery + " ) ";
			}
		}

		String queryString = "from Occupation where " +
				createProductionQualifier(prod, project) +
				OCC_UNION + " = ? " +
				contractQuery +
				" order by " + NAME + ", id ";

		List<Occupation> occupations = find(queryString, valueList.toArray());
		if (occupations.size() == 0) { // try again without production type qualifiers
			queryString = "from Occupation where " +
					OCC_UNION + " = ? " +
					contractQuery +
					" order by " + NAME + ", id ";
			occupations = find(queryString, valueList.toArray());
			if (occupations.size() == 0 && contractQuery.length() > 0) { // try again without contract qualifier
				queryString = "from Occupation where " +
						OCC_UNION + " = ? " +
						" order by " + NAME + ", id ";
				occupations = find(queryString, union.getOccupationUnion());
			}
		}

		String lastName = "zzzz";
		for (Occupation occupation : occupations) {
			// eliminate duplicate job classifications from the list
			if (! lastName.equals(occupation.getName())) {
				list.add(new SelectItem(occupation.getId(), occupation.getName()));
				lastName = occupation.getName();
			}
		}
		return list;
	}

	/**
	 * Create a List of SelectItem to populate a drop-down list of occupations
	 * that exist within a particular union. The list is in name order with
	 * duplicate names removed. The SelectItem.value field is the database id of
	 * the Occupation instance, and the label is the occupation name field.
	 *
	 * @param occUnion The business key that identifies the union of interest.
	 * @return A non-null (but possibly empty) List of selections.
	 */
	public List<SelectItem> createOccupationDLbyUnionOccCode(String occUnion, String occCode) {
		List<SelectItem> list = new ArrayList<>();
		String queryString = "from Occupation where " +
				OCC_UNION + " = ? and " +
				OCC_CODE + " = ? " +
				" order by " + NAME + ", id ";
		Object[] values = { occUnion, occCode };
		@SuppressWarnings("unchecked")
		List<Occupation> occupations = find(queryString, values);
		String lastName = "zzzz";
		for (Occupation occupation : occupations) {
			// eliminate duplicate job classifications from the list
			if (! lastName.equals(occupation.getName())) {
				list.add(new SelectItem(occupation.getId(), occupation.getName()));
				lastName = occupation.getName();
			}
		}
		return list;
	}

	/**
	 * Create a List of SelectItem`s to populate a drop-down list of occupation
	 * codes that match a particular union and job name. The list is in
	 * occupation code order, with duplicate ones removed.
	 *
	 * @param prod The Production containing the Occupation entry; this is used
	 *            to narrow the selection by attributes of the production such
	 *            as TV vs Feature, region, etc.
	 * @param project The Project of interest; if null, ignore the timecard's
	 *            Project affiliation; should be non-null only for Commercial
	 *            productions.
	 * @param occUnion The LS union code value, used to select a group of
	 *            occupations.
	 * @param jobClass The job classification (name).
	 * @return A non-null, but possibly empty, List of SelectItem`s, where the
	 *         value is the database id and the label is the occupation code.
	 */
	@SuppressWarnings("unchecked")
	public List<SelectItem> createOccCodeDL(Production prod, Project project, String occUnion, String jobClass) {
		List<SelectItem> list = new ArrayList<>();

		String contractQuery = "";
		if (prod.getPayrollPref().getUse30Htg()) { // Use 3.0 "full" HTG processing...
			// restrict occupation list to assigned contracts
			contractQuery = prod.getContractCodes();
			if (contractQuery.length() > 0) {
				contractQuery = CONTRACT_CODE + " in ( " + contractQuery + " ) and ";
			}
		}

		String queryString = "from Occupation where " +
				createProductionQualifier(prod, project) +
				OCC_UNION + " = ? and " +
				contractQuery +
				NAME + " = ? " +
				" order by " + OCC_CODE + ", id " ;
		Object[] values = { occUnion, jobClass };

		List<Occupation> occupations = find(queryString, values);

		if (occupations.size() == 0 && contractQuery.length() > 0) { // try again without contract qualifier
			queryString = "from Occupation where " +
					OCC_UNION + " = ? and " +
					NAME + " = ? " +
					" order by " + OCC_CODE + ", id " ;
			occupations = find(queryString, values);
		}

		String lastCode = "zzzz";
		for (Occupation occupation : occupations) {
			// eliminate duplicate occ codes from the list
			if (! lastCode.equals(occupation.getOccCode())) {
				list.add(new SelectItem(occupation.getId(), occupation.getOccCode()));
				lastCode = occupation.getOccCode();
			}
		}
		return list;
	}

	/**
	 * Generate a List of SelectItem`s (for a drop-down list) of all OccCodes
	 * for a particular union-code. Note that the union-code may be more
	 * specific than a local number, e.g., 600C for Local 600 Photographers
	 * (camera).  Blank OccCodes are omitted from the list.
	 * @param prod The Production containing the Occupation entry; this is used
	 *            to narrow the selection by attributes of the production such
	 *            as TV vs Feature, region, etc.
	 * @param project The Project of interest; if null, ignore the timecard's
	 *            Project affiliation; should be non-null only for Commercial productions.
	 * @param occUnion The unionCode for which all matching Occupation Codes
	 *            should be retrieved.
	 * @return A non-null, but possibly empty, List of SelectItem instances,
	 *         where the SelectItem.value field is occupation.id, and the label
	 *         field is occupation.name.
	 */
	public List<SelectItem> createOccCodeDL(Production prod, Project project, String occUnion) {
		List<SelectItem> list = new ArrayList<>();
		String queryString = "from Occupation where " +
				createProductionQualifier(prod, project) +
				OCC_UNION + " = ? and " +
				OCC_CODE + " <> '' " + /* eliminate entries with no union occCode */
				" order by " + OCC_CODE + ", id " ;
		@SuppressWarnings("unchecked")
		List<Occupation> occupations = find(queryString, occUnion);
		String lastCode = "zzzz";
		for (Occupation occupation : occupations) {
			// eliminate duplicate occ codes from the list
			if (! lastCode.equals(occupation.getOccCode())) {
				list.add(new SelectItem(occupation.getId(), occupation.getOccCode()));
				lastCode = occupation.getOccCode();
			}
		}
		return list;
	}

	/**
	 * Create a partial SQL query string incorporating the values of the given
	 * Production which can help narrow the search for matching Occupations.
	 *
	 * @param prod The Production containing the Occupation entry; this is used
	 *            to narrow the selection by attributes of the production such
	 *            as TV vs Feature, region, etc.
	 * @param project The Project of interest; if null, ignore the timecard's
	 *            Project affiliation; should be non-null only for Commercial productions.
	 * @return A partial SQL query, beginning with "(" and ending with " and ".
	 */
	private static String createProductionQualifier(Production prod, Project project) {
		String pcityRegion;
		PayrollPreference pref = prod.getPayrollPref();
		if (project != null) {
			project = ProjectDAO.getInstance().refresh(project);
			pref = project.getPayrollPref();
		}
		if (pref != null && pref.getInProductionCity()) {
			pcityRegion = Constants.PRODUCTION_CITY_REGION;
		}
		else {
			pcityRegion = Constants.NON_PRODUCTION_CITY_REGION;
		}

		String qual = "(" + REGION + " = 'A' ";
		if (pref != null && ! pref.getNyRegion().equals("NA")) {
			qual += " or " + REGION + " like '%" + pref.getNyRegion() + "%' ";
		}
		qual +=  " or " + REGION + " like '%" + pcityRegion + "%') and ";

		if (prod.getType().isAicp()) {
			// Don't use major/indie if commercial production (cannot be set on Preference page)
			qual += "(" + FEATURE_TV 	+ " = 'A' or " + FEATURE_TV + " = 'C') and ";
		}
		else if (pref != null) {
			qual += "(" + MAJOR_INDIE 	+ " = 'A' or " + MAJOR_INDIE+ " = '" +
							(pref.getStudioType() == StudioType.IN ? "I" : "M") + "') and ";

			qual += "(" + FEATURE_TV 	+ " = 'A' or " + FEATURE_TV + " = '" + pref.getMediumType().getOccupationCode() + "') and ";

			if (pref.getMediumType() == MediumType.FT) { // Feature
				qual += "(" + AGREEMENT 	+ " = 'A' or " + AGREEMENT 	+ " like '%" + pref.getDetailType() + "%') and ";
			}
			else { // Television
				if (pref.getAsaContract() == PayrollProductionType.ASA_OTHER) { // ASA-specific handling
					// ignore "detail type" and just look for "Other" coding in occupation's production type field
					qual += "(" + PROD_TYPE 	+ " = 'A' or " + PROD_TYPE 	+ " like '%O%') and ";
				}
				else { // DetailType values come from entries in SelectionItem table!
					qual += "(" + PROD_TYPE 	+ " = 'A' ";
					if (ContractUtils.calculateUsesVideoTape(prod)) {
						if (pref.getTvDramatic()) {
							qual += " or " + PROD_TYPE 	+ " like '%DR%' ";
						}
						else {
							qual += " or " + PROD_TYPE 	+ " like '%ND%' ";
						}
					}
					qual += " or " + PROD_TYPE 	+ " like '%" + pref.getDetailType() + "%') and ";
				}
				qual += "(" + SEASON 		+ " = 'A' or " + SEASON 	+ " like '%" + pref.getTvSeason() + "%') and " +
						"(" + START_RANGE 	+ " = 'A' or " + START_RANGE+ " like '%" + pref.getTvEra() + "%') and ";

			}
		}
		return qual;
	}

//	/**
//	 * @param occUnion The value to match.
//	 * @return A list of Occupations with the given unionCode value.
//	 */
//	public List<Occupation> findByUnionCode(String occUnion) {
//		String queryString = "from Occupation where " +
//				OCC_UNION + " = ? and " +
//				OCC_CODE + " <> '' " + /* eliminate entries with no union occCode */
//				" order by " + OCC_CODE ;
//		@SuppressWarnings("unchecked")
//		List<Occupation> occupations = find(queryString, occUnion);
//		return occupations;
//	}

//	/**
//	 * @param occUnion The value to match.
//	 * @param occCode The occupation code to match.
//	 * @return The Occupation with the given unionCode and occCode values --
//	 *         there should only be one.
//	 */
//	public Occupation findOccupationByUnionAndOccCode(String occUnion, String occCode) {
//		String queryString = "from Occupation where " +
//				OCC_UNION + " = ? and " +
//				LS_OCC_CODE + " = ? " ;
//		Object[] values = { occUnion, occCode };
//		Occupation occupation = findOne(queryString, values);
//		return occupation;
//	}

//	public Occupation findByUnionAndName(String unionKey, String jobClass) {
//		String queryString = "from Occupation where " +
//				UNION_CODE + " = ? and " +
//				NAME + " = ? " +
//				" order by " + OCC_CODE ;
//		Object[] values = { unionKey, jobClass };
//		@SuppressWarnings("unchecked")
//		List<Occupation> occupations = find(queryString, values);
//		String lastCode = "";
//		for (Occupation occupation : occupations) {
//			if (! lastCode.equals(occupation.getOccCode())) {
//				list.add(new SelectItem(occupation.getOccCode(), occupation.getOccCode()));
//				lastCode = occupation.getOccCode();
//			}
//		}
//		return list;
//		return null;
//	}

//	public static OccupationDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (OccupationDAO)ctx.getBean("OccupationDAO");
//	}

}
