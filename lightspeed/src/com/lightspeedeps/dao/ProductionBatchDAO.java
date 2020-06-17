package com.lightspeedeps.dao;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.ProductionBatch;
import com.lightspeedeps.model.Project;

/**
 * A data access object (DAO) providing persistence and search support for
 * ProductionBatch entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.ProductionBatch
 */
public class ProductionBatchDAO extends BaseTypeDAO<ProductionBatch> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(ProductionBatchDAO.class);

	//property constants
	//private static final String NAME = "name";
	private static final String PRODUCTION = "production";
	private static final String PROJECT = "project";

	public static ProductionBatchDAO getInstance() {
		return (ProductionBatchDAO)getInstance("ProductionBatchDAO");
	}

	/**
	 * Get a List of all the ProductionBatch objects for a given Production,
	 * sorted in ascending name order.
	 *
	 * @param prod The Production of interest
	 * @param project The Project of interest; if null, ignore the timecard's
	 *            Project affiliation; should be non-null only for Commercial productions.
	 * @return A non-null, but possibly empty, List of the ProductionBatch
	 *         objects associated with the specified Production.
	 */
	@SuppressWarnings("unchecked")
	public List<ProductionBatch> findByProductionProject(Production prod, Project project) {
		String query = "from ProductionBatch where " +
				PRODUCTION + " = ? ";

		List<Object> valueList = new ArrayList<Object>();
		valueList.add(prod);

		if (project != null) {
			valueList.add(project);
			query += " and " + PROJECT + " = ? ";
		}
		query += " order by name ";

		return find(query, valueList.toArray());
	}

}
