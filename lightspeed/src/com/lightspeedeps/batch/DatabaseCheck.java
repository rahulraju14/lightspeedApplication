package com.lightspeedeps.batch;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.util.app.Constants;

/**
 * This class checks database availability. It is designed to run as a Quartz-scheduled task.
 */
public class DatabaseCheck extends SpringBatch {
	private static final Log log = LogFactory.getLog(DatabaseCheck.class);

	/** Set in applicationContextGeneric.xml. */
	private transient ProductionDAO productionDAO;

	public DatabaseCheck() {
		log.debug("");
	}

	/**
	 * Called via a scheduled job, such as a Quartz task.
	 * See applicationContextScheduler.xml.
	 * @return Success or Failure
	 */
	public String execute() {
		log.info("starting at " + new Date());
		String ret = Constants.FAILURE;
		setUp();	// required for SpringBatch subclasses - initializes context
		try {
			if (productionDAO.checkDb()) {
				log.debug("database check ok");
				ret = Constants.SUCCESS;
			}
			else {
				log.error("database check failed");
			}
		}
		catch (Exception ex) {
			log.error("Exception: ", ex);
		}
		finally {
			tearDown();	// required for SpringBatch subclasses - clean up.
		}
		return ret;
	}

	public ProductionDAO getProductionDAO() {
		return productionDAO;
	}
	public void setProductionDAO(ProductionDAO dao) {
		productionDAO = dao;
	}

}
