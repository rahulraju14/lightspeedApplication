package com.lightspeedeps.dao;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;

import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.StartForm;
import com.lightspeedeps.model.Unions;

/**
 * A data access object (DAO) providing persistence and search support for
 * Unions entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.Unions
 */
public class UnionsDAO extends BaseTypeDAO<Unions> {
	private static final Log log = LogFactory.getLog(UnionsDAO.class);
	//property constants
	private static final String LIST_ORDER = "listOrder";
//	private static final String NUMBER = "number";
	public static final String UNION_KEY = "unionKey";
	private static final String OCCUPATION_UNION = "occupationUnion";
//	private static final String NAME = "name";

	public static UnionsDAO getInstance() {
		return (UnionsDAO)getInstance("UnionsDAO");
	}

	/**
	 * Creates a List of SelectItem`s of all Unions in the database except for
	 * ones associated with Commercial (AICP) productions.
	 *
	 * @return A non-null, and presumably non-empty(!), List of SelectItem`s of
	 *         all non-commercial Unions.
	 */
	public List<SelectItem> createNonCommercialUnionDL() {
		String queryString = "from Unions " +
				" where " + OCCUPATION_UNION + " not like 'CPA%' " +
				" order by " + LIST_ORDER ;
		@SuppressWarnings("unchecked")
		List<Unions> unions = find(queryString);
		return createSelectList(unions);
	}

	/**
	 * Creates a List of SelectItem`s of all "Commercial" Unions in the
	 * database.
	 *
	 * @return A non-null, and presumably non-empty(!), List of SelectItem`s of
	 *         the Union entries used by Commercial productions.
	 */
	public List<SelectItem> createCommercialUnionDL() {
		String queryString = "from Unions " +
				" where " + OCCUPATION_UNION + " like 'CPA%' " +
				// include non-union entry
				" or "+ OCCUPATION_UNION + " = '" + Unions.NON_UNION + "' " +
				" order by " + LIST_ORDER ;
		@SuppressWarnings("unchecked")
		List<Unions> unions = find(queryString);
		return createSelectList(unions);
	}

	/**
	 * Creates a List of SelectItem`s of all the Unions in the database that
	 * apply to the given Production, based on the contracts assigned to the
	 * production. Some unions may be included regardless of contract
	 * assignment, by assigning (in the Unions table) a contractCode of "N_A",
	 * "TVF", or "COM".
	 *
	 * @param prod The Production whose contract assignments will determine the
	 *            unions included in the list.
	 * @return A List of SelectItem's of the appropriate Unions, where the value
	 *         field is Unions.id, and the label field is Unions.name.
	 */
	public List<SelectItem> createContractUnionDL(Production prod) {

		String queryString = "select distinct u.* from production_contract pc, contract c, unions u " +
				" where ( pc.contract_key = c.contract_key " +
					" and c.contract_code REGEXP u.contract_code" +
					" and pc.production_id = :productionId ) " +
				" or u.contract_code = 'N_A' or u.contract_code = :type " +
				" order by list_order ";

		boolean commercial = prod.getType().isAicp();
		Query query = getHibernateSession().createSQLQuery(queryString)
				.addEntity(Unions.class)
				.setString("type", commercial ? "COM" : "TVF")
				.setInteger("productionId", prod.getId());

		@SuppressWarnings("unchecked")
		List<Unions> unions = query.list();
		log.debug(unions.size());
		return createSelectList(unions);
	}

	/**
	 * Given a list of Unions, create an equivalent List of SelectItem`s, where
	 * the value field is Unions.id, and the label field is Unions.name.
	 *
	 * @param unions The generated list of SelectItem entries.
	 * @return A List of SelectItem`s as described.
	 */
	private List<SelectItem> createSelectList(List<Unions> unions) {
		List<SelectItem> list = new ArrayList<SelectItem>();
		for (Unions union : unions) {
			list.add(new SelectItem(union.getId(), union.getName()));
		}
		return list;
	}

	/**
	 * Find the GS Local number corresponding to the union number in the given
	 * StartForm.
	 *
	 * @param sf The StartForm whose union value will be used to look up the
	 *            corresponding GS union number.
	 * @return The corresponding GS union number, or null if not found, or if
	 *         the given 'sf' is null.
	 */
	public static String findGsLocalNum(StartForm sf) {
		String gsLocalNum = null;
		if (sf != null) {
			List<Unions> unions = getInstance().findByProperty(UNION_KEY, sf.getUnionKey());
			if (!unions.isEmpty()) {
				gsLocalNum = unions.get(0).getGsLocalNum();
			}
		}
		return gsLocalNum;
	}

//	public static UnionsDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (UnionsDAO)ctx.getBean("UnionsDAO");
//	}

}
