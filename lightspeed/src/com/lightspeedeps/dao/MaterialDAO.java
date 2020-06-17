package com.lightspeedeps.dao;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.FilmStock;
import com.lightspeedeps.model.Material;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.util.app.SessionUtils;

/**
 * A data access object (DAO) providing persistence and search support for
 * Material entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.Material
 */

public class MaterialDAO extends BaseTypeDAO<Material> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(MaterialDAO.class);

	// property constants
	private static final String TYPE = "type";
	private static final String PRODUCTION = "production";
//	private static final String DESCRIPTION = "description";

	public static MaterialDAO getInstance() {
		return (MaterialDAO)getInstance("MaterialDAO");
	}

	public List<Material> findByProduction(Production prod) {
		return findByProperty(PRODUCTION, prod);
	}

	/**
	 * @deprecated Currently there is no valid reason to get a list of all
	 *             materials of a specific type across all Productions. The
	 *             method you probably want is
	 *             {@link #findByProductionAndType(String)}.
	 *
	 * @return a non-null List of all Material`s matching the specified type
	 *         across ALL Productions, which is probably of little or no use.
	 */
	/*@Deprecated
	public List<Material> findByType(Object type) {
		return findByProperty(TYPE, type);
	}*/

	/**
	 * Find all Materials in the current Production with the specified type.
	 * Note that there should never be more than one Material with a given type
	 * within a single Production.
	 *
	 * @param type The type (name) of the Material.
	 * @return The non-null (but possibly empty) List of matching Material`s.
	 */
	public List<Material> findByProductionAndType(String type) {
		return findByProductionAndType(SessionUtils.getProduction(), type);
	}

	/**
	 * Find all Materials in the given Production with the specified type.
	 * Note that there should never be more than one Material with a given type
	 * within a single Production.
	 *
	 * @param prod The Production of interest.
	 * @param type The type (name) of the Material.
	 * @return The non-null (but possibly empty) List of matching Material`s.
	 */
	@SuppressWarnings("unchecked")
	public List<Material> findByProductionAndType(Production prod, String type) {
		Object[] values = {prod, type};
		return find("from Material where " +
				PRODUCTION + " = ? and " +
				TYPE + " = ?", values);
	}

//	public static MaterialDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (MaterialDAO) ctx.getBean("MaterialDAO");
//	}

	// Getting Material data for DPR (no longer used)
/*	@SuppressWarnings("unchecked")
	public List<Material> getMaterialDpr(Integer projectId, Date date) {
		log.debug("getMaterialDpr ");
		try {
			Object[] values = { projectId, date };
			String queryString = "select distinct(mat) from Material mat, FilmStock fs " +
					" where mat.id = fs.material.id and fs.project.id = ? and fs.date = ?" +
					" order by mat.type";
			return find(queryString, values);
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw re;
		}
	}
*/
	/**
	 * Remove a Material type and all its related stock records.
	 * @param material
	 */
	@Transactional
	public void remove(Material material) {
		FilmStockDAO filmStockDAO = FilmStockDAO.getInstance();
		FilmMeasureDAO filmMeasureDAO = FilmMeasureDAO.getInstance();

		List<FilmStock> matDelList = filmStockDAO.findByMaterial(material);

		for (FilmStock filmStock : matDelList) {
			filmMeasureDAO.delete(filmStock.getUsedPrior());
			filmMeasureDAO.delete(filmStock.getUsedToday());
//			filmMeasureDAO.delete(filmStock.getUsedTotal());

			filmStockDAO.delete(filmStock);
		}
		delete(material);
	}

}