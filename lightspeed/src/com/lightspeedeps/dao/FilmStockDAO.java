package com.lightspeedeps.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.FilmMeasure;
import com.lightspeedeps.model.FilmStock;
import com.lightspeedeps.model.Material;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.util.app.ApplicationUtils;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;

/**
 * A data access object (DAO) providing persistence and search support for
 * FilmStock entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.FilmStock
 */

public class FilmStockDAO extends BaseTypeDAO<FilmStock> {
	private static final Log log = LogFactory.getLog(FilmStockDAO.class);
	// property constants
//	private static final String INVENTORY_PRIOR = "inventoryPrior";
//	private static final String INVENTORY_RECEIVED = "inventoryReceived";
//	private static final String INVENTORY_USED_TODAY = "inventoryUsedToday";
//	private static final String INVENTORY_TOTAL = "inventoryTotal";

	public static FilmStockDAO getInstance() {
		return (FilmStockDAO)getInstance("FilmStockDAO");
	}

//	public static FilmStockDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (FilmStockDAO) ctx.getBean("FilmStockDAO");
//	}

	@SuppressWarnings("unchecked")
	public List<FilmStock> findLatestStock(Material mat, Date date) {
		log.debug("finding latest Film Stock instance: " + mat);

		try {
			Object[] values = { mat, date, mat };
			String queryString = "select fs from FilmStock fs " +
					"where fs.date=" +
					"(SELECT max(Date) FROM FilmStock fs1 where fs1.material = ? and fs1.date <= ?) " +
					" and fs.material = ? order by id desc";
			List<FilmStock> list = find(queryString, values);
			return list;
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw re;
		}
	}

	/**
	 * Returns list of FilmStock entries using the specified Material, in descending
	 * date order, since that is how it is presented on the Materials page. There is a
	 * secondary sort by id, in case more than one entry exists for the same date.
	 * @param mat The Material whose stock entries are desired.
	 * @return A possibly empty (not null) List of FilmStock entries.
	 */
	@SuppressWarnings("unchecked")
	public List<FilmStock> findByMaterial(Material mat) {
		log.debug("find by material: " + mat);
		String queryString = "from FilmStock fs where fs.material = ? order by date desc, id desc";
		return find(queryString, mat);
	}

	/**
	 * Get the most recent FilmStock records within each material type. Used for
	 * the Home Page "Materials" table. If the SQL query returns more than one
	 * record for a given Material, the Java code keeps only the one with the
	 * highest database id (most recently created).
	 *
	 * @return A non-null, but possibly empty, List of FilmStock objects as
	 *         described above.
	 */
	@SuppressWarnings("unchecked")
	public List<FilmStock> findCurrentMaterials() {
		log.debug("");
		if (ApplicationUtils.isOffline()) {
			// SQLite doesn't support this query!
			log.debug("offline mode - material query ignored.");
			return new ArrayList<FilmStock>();
		}
		List<FilmStock> list = null;
		try {
			String queryString = "from FilmStock fs where (fs.date, fs.material) IN " +
					"(Select max(f.date),f.material from FilmStock f " +
					" where f.material.production = ? " +
					" group by f.material)" +
					" order by fs.material.type, fs.id desc";
			Production prod = SessionUtils.getProduction();
			list = find(queryString, prod);
			Integer lastId = -1;
			FilmStock fs;
			for (Iterator<FilmStock> iter = list.iterator(); iter.hasNext(); ) {
				fs = iter.next();
				if (fs.getMaterial().getId().equals(lastId)) {
					//log.debug("removing " + fs);
					iter.remove();
				}
				lastId = fs.getMaterial().getId();
			}
			return list;
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw e;
		}
	}

	/**
	 * Get the latest FilmStock records where a detail record has a date prior
	 * to or equal to the given date. This is used to populate the Material
	 * Usage table in the DPR. If the SQL query returns more than one record for
	 * a given Material, the Java code keeps only the one with the highest
	 * database id (most recently created).
	 *
	 * @param date Only return records with dates before or on this date.
	 * @return A non-null, but possibly empty, List of FilmStock objects as
	 *         described above.
	 */
	@SuppressWarnings("unchecked")
	public List<FilmStock> findLatestThroughDate(Date date) {
		log.debug(" getFilmStockDpr ");

		try {
			Production prod = SessionUtils.getProduction();
			Object[] values = { date, prod };
			String queryString = "from FilmStock fs where (fs.date, fs.material) IN " +
					"(Select max(f.date),f.material from FilmStock f " +
					" where f.date <= ? " +
					" and f.material.production = ? " +
					" and f.material.inUse = true " +
					" group by f.material)" +
					" order by fs.material.type, fs.id desc";
			List<FilmStock> list = find(queryString, values);
			Integer lastId = -1;
			FilmStock fs;
			for (Iterator<FilmStock> iter = list.iterator(); iter.hasNext(); ) {
				fs = iter.next();
				if (fs.getMaterial().getId().equals(lastId)) {
					//log.debug("removing " + fs);
					iter.remove();
				}
				lastId = fs.getMaterial().getId();
			}
			return list;
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw re;
		}
	}

	/**
	 * Run through all the FilmStock and FilmMeasure records associated
	 * with the specified Material and update any totals that are incorrect.
	 *
	 */
	@SuppressWarnings("unchecked")
	@Transactional
	public void updateTotals(Material mat) {
		log.debug("material: " + mat);
		try {
			List<FilmStock> list;
			FilmMeasureDAO fmDAO = FilmMeasureDAO.getInstance();
			String queryString = "from FilmStock fs where fs.material = ? order by date asc, id asc";
			list = find(queryString, mat);
			int inventory = 0;
			FilmMeasure fm = new FilmMeasure(0,0,0);
			for (FilmStock fs : list) {
				fs.getUsedPrior().setNoGood(fm.getNoGood());
				fs.getUsedPrior().setWaste(fm.getWaste());
				fs.getUsedPrior().setPrint(fm.getPrint());
				fmDAO.attachDirty(fs.getUsedPrior());
				fs.setUsedTotal(null); // force refresh
				fm = fs.getUsedTotal();
				fs.setInventoryPrior(inventory);
				inventory = fs.getInventoryTotal();
			}
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw re;
		}
	}

}