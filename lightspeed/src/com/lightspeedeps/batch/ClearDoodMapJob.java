package com.lightspeedeps.batch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dood.ProductionDood;
import com.lightspeedeps.type.EventType;
import com.lightspeedeps.util.app.EventUtils;

/**
 * This class clears the data maintained by the ProductionDood class. It is
 * designed to run as a Quartz-scheduled task.  It should be run periodically
 * to prevent the (static) DooD data from taking up too much memory.
 */
public class ClearDoodMapJob extends SpringBatch {
	private static final Log log = LogFactory.getLog(ClearDoodMapJob.class);

	public ClearDoodMapJob() {
		log.debug("");
	}

	/**
	 * Called via a scheduled job, such as a Quartz task.
	 * See applicationContextScheduler.xml.
	 * @return empty string
	 */
	public String execute() {
		log.debug("");
		setUp();	// required for SpringBatch subclasses - initializes context
		try {
			int units = ProductionDood.getProductionDoodMap().size();

			if (units > 0) {
				int count = ProductionDood.getElementCount();

				String description = "Clearing DooD map: # of units mapped=" + units + "; # of elements=" + count;
				log.info(description);

				EventUtils.logEvent(EventType.INFO, null, null, "lightspeed", description);

				ProductionDood.clear(); // Clear all production DooD information
			}
			else {
				log.debug("DooD map is currently empty - no clear necessary.");
			}
		}
		catch (Exception ex) {
			EventUtils.logError(ex);
		}
		finally {
			tearDown();	// required for SpringBatch subclasses - clean up.
		}
		return "";
	}

}
