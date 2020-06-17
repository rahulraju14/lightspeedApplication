/**
 * WeeklyBatchUtils.java
 */
package com.lightspeedeps.util.payroll;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.StartFormDAO;
import com.lightspeedeps.dao.WeeklyBatchDAO;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.ProductionBatch;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.StartForm;
import com.lightspeedeps.model.WeeklyBatch;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.object.LoggedException;
import com.lightspeedeps.type.ApprovalStatus;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.common.NumberUtils;

/**
 * Utility methods for managing WeeklyBatch objects.
 */
public class WeeklyBatchUtils {
	private static final Log log = LogFactory.getLog(WeeklyBatchUtils.class);

	/**
	 * Private constructor - all methods are static.
	 */
	private WeeklyBatchUtils() {
	}

	/**
	 * Add the given WeeklyTimecard to the appropriate WeeklyBatch. The batch to
	 * which is belongs will be determined from its StartForm. A new WeeklyBatch
	 * will be created if necessary.
	 * <p>
	 * Note that this may be called from a "batch" (scheduled transaction) environment.
	 *
	 * @param wtc The WeeklyTimecard which should be added to the appropriate
	 *            batch, if any.
	 */
	public static void addToBatch(WeeklyTimecard wtc) {
		try {
			StartForm sf = wtc.getStartForm();
			ProductionBatch batch = null;
			if (sf.getEmployment() != null) {
				// getEmployment() == null only if SF was not successfully converted from 3.0 to 3.1
				batch = sf.getEmployment().getProductionBatch();
			}
			if (batch != null) {
				Production prod = batch.getProduction();
				Project project = sf.getProject();
				Date date = wtc.getEndDate();
				String batchName = createBatchName(prod, batch, date);
				WeeklyBatchDAO weeklyBatchDAO = WeeklyBatchDAO.getInstance();
				List<WeeklyBatch> wbs = weeklyBatchDAO.findByProductionProjectNameDate(prod, project, batchName, date);
				WeeklyBatch wb;
				boolean exists;
				if (wbs.size() == 0) { // batch does not yet exist
					wb = new WeeklyBatch(prod, project, batchName, date);
					wb.setTimecardStatus(wtc.getStatus());
					exists = false;
					if (project != null && prod.getType().isAicp()) {
						wb.setWorkersComp(project.getPayrollPref().getWorkersComp());
					}
				}
				else {
					wb = wbs.get(0);
					exists = true;
				}
				wb.setLastUpdated(wtc.getUpdated());
				wb.setGross(wtc.getGrandTotal());
				wb.getTimecards().add(wtc);
				wtc.setWeeklyBatch(wb);
				if (exists) {
					checkAndUpdateStatus(wb, true); // update status & save to db
				}
				else { // newly created; batch status is already correct
					weeklyBatchDAO.save(wb);
				}
				log.info("cascaded save of new WeeklyTimecard, id=" + wtc.getId());
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
	}

	/**
	 * Add the given WeeklyTimecardlist to the appropriate WeeklyBatch. The batch to
	 * which is belongs will be determined from its work ending date. A new WeeklyBatch
	 * will be created if necessary.
	 *
	 * @param weeklyTimecardList The WeeklyTimecard list which should be added to the appropriate
	 *            batch, if any.
	 * @param weDate Work ending date of timecards in the list
	 *
	 * @return WeeklyBatch instance either newly created or the existing one for the the given timecard list.
	 *
	 */
	public static WeeklyBatch addToursTimecardsToBatch(List<WeeklyTimecard> weeklyTimecardList, Date weDate) {
		try {
			if (weeklyTimecardList.size() == 0) {
				return null;
			}
			Production prod = weeklyTimecardList.get(0).getProduction();
			WeeklyBatch wb = null;
			String batchName = createBatchName(null, null, weDate);
			WeeklyBatchDAO weeklyBatchDAO = WeeklyBatchDAO.getInstance();
			List<WeeklyBatch> wbs = weeklyBatchDAO.findByProductionProjectNameDate(prod, null, batchName, weDate);
			if (wbs.size() == 0) { // batch does not yet exist
				wb = new WeeklyBatch(prod, null, batchName, weDate);
				weeklyBatchDAO.save(wb);
			}
			else {
				wb = wbs.get(0);
			}
			for (WeeklyTimecard wtc : weeklyTimecardList) {
				wb.getTimecards().add(wtc);
				wtc.setWeeklyBatch(wb);
			}
			checkAndUpdateStatus(wb, true); // update status & save to db
			return wb;
		}
		catch (Exception e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
	}

	/**
	 * Determine the proper 'aggregate' setting for the given batch.
	 *
	 * @param weeklyBatch The WeeklyBatch to be checked.
	 * @return The calculated value of the {@link WeeklyBatch#aggregate} flag,
	 *         based on the timecards currently in the batch. The given batch's
	 *         aggregate field has also been set to this value.
	 */
	public static boolean checkAndSetAggregate(WeeklyBatch weeklyBatch) {
		boolean agg = false;
		if (weeklyBatch.getProject() != null) {
			int projId = weeklyBatch.getProject().getId();
			for (WeeklyTimecard wtc : weeklyBatch.getTimecards()) {
				StartForm sf = StartFormDAO.getInstance().refresh(wtc.getStartForm());
				if (sf.getProject() != null &&
						sf.getProject().getId().intValue() != projId) {
					agg = true;
					break;
				}
			}
		}
		weeklyBatch.setAggregate(agg);
		return agg;
	}

	/**
	 * Check if the given WeeklyTimecard is in a batch, and if so, update the
	 * batch's status and timestamp, if necessary.
	 *
	 * @param wtc The WeeklyTimecard that has (probably) been changed.
	 */
	public static void updateBatchStatus(WeeklyTimecard wtc) {
		WeeklyBatch weeklyBatch = wtc.getWeeklyBatch();
		if (weeklyBatch != null) {
			weeklyBatch = WeeklyBatchDAO.getInstance().refresh(weeklyBatch);
			checkAndUpdateStatus(weeklyBatch);
		}
	}

	/**
	 * Check to see if the status and last-updated timestamp of the given
	 * WeeklyBatch need to be updated to match the data in the WeeklyTimecard`s
	 * that it contains. If so, update the WeeklyBatch in the database.
	 *
	 * @param weeklyBatch The WeeklyBatch to be checked.
	 */
	public static void checkAndUpdateStatus(WeeklyBatch weeklyBatch) {
		checkAndUpdateStatus(weeklyBatch, false);
	}

	/**
	 * Create a List of SelectItem`s describing the WeeklyBatch objects in the
	 * current Production, for the given week-ending date. The SelectItem.value
	 * field will be the WeeklyBatch.id, and the SelectItem.label will be the
	 * name of the batch.
	 *
	 * @param prod The Production containing the batches.
	 * @param project The Project containing the batches. Should be null for
	 *            TV/Feature productions. For commercial productions, null
	 *            indicates batches from all projects should be included; if not
	 *            null, only batches from the matching project will be included.
	 * @param weekEndDate The week-ending date of interest.
	 * @return A non-null, possibly empty, List of SelectItem`s as described.
	 */
	public static List<SelectItem> createBatchList(Production prod, Project project, Date weekEndDate) {
		List<WeeklyBatch> batches = new ArrayList<>();
		createBatchList(batches, prod, project, weekEndDate, (project == null));
		boolean showJobNumber = false;
		if (prod.getType().hasPayrollByProject() && project == null) {
			showJobNumber = true;
		}
		List<SelectItem> list = new ArrayList<>();
		list.add(Constants.SELECT_NOT_BATCHED);	// "un-batched" - will be top item if no others exist
		if (batches.size() > 0) {
			for (WeeklyBatch wb : batches) {
				if (showJobNumber) {
					list.add(new SelectItem(wb.getId(),
							"(" + wb.getProject().getEpisode() + ") " + wb.getName()));
				}
				else {
					list.add(new SelectItem(wb.getId(), wb.getName()));
				}
			}
			list.add(0,Constants.SELECT_ALL_IDS); // top item is "All", followed by unbatched
		}
		return list;
	}

	/**
	 * Create a List of WeeklyBatch`s for the given production, project, and
	 * week-ending date.
	 *
	 * @param batchList The List whose contents will be replaced by the batches
	 *            matching the request criteria. The contents of the List upon
	 *            entry are cleared.
	 * @param prod The Production containing the batches.
	 * @param project The Project containing the batches. Should be null for
	 *            TV/Feature productions. For commercial productions, null
	 *            indicates batches from all projects should be included; if not
	 *            null, only batches from the matching project will be included.
	 * @param weekEndDate The week-ending date of interest.
	 * @param includeAllProjects If true, batches from all projects will be
	 *            included.
	 */
	public static void createBatchList(List<WeeklyBatch> batchList, Production prod, Project project,
			Date weekEndDate, boolean includeAllProjects) {
		boolean aggregate = false; // exclude "aggregate" batches by default
		Project showProject = project;
		if (includeAllProjects) {
			showProject = null;
			aggregate = true; // include "aggregate" batches
		}
		batchList.clear();
		if (weekEndDate.equals(Constants.SELECT_ALL_DATE)) {
			batchList.addAll(WeeklyBatchDAO.getInstance()
					.findByProductionProject(prod, showProject, aggregate));
		}
		else if (weekEndDate.equals(Constants.SELECT_PRIOR_DATE)) {
			Date weDate = TimecardUtils.calculateDefaultWeekEndDate();
			batchList.addAll(WeeklyBatchDAO.getInstance()
					.findByProductionProjectPriorDate(prod, showProject, weDate, aggregate));
		}
		else {
			batchList.addAll(WeeklyBatchDAO.getInstance()
					.findByProductionProjectDate(prod, showProject, weekEndDate, aggregate));
		}
	}

	/**
	 * Check to see if the status and last-updated timestamp of the given
	 * WeeklyBatch need to be updated to match the data in the WeeklyTimecard`s
	 * that it contains. If so, update the WeeklyBatch in the database.
	 *
	 * @param weeklyBatch The WeeklyBatch to be checked.
	 * @param forceSave If true, the given WeeklyBatch will be persisted to the
	 *            database even if no changes were made within this method.
	 */
	private static void checkAndUpdateStatus(WeeklyBatch weeklyBatch, boolean forceSave) {
		// Find the "lowest" (worst) status within the batch's timecards
		ApprovalStatus status;
		Date date = weeklyBatch.getLastUpdated();
		BigDecimal oldGross = weeklyBatch.getGross();
		if (oldGross == null) {
			oldGross = BigDecimal.ZERO;
		}
		BigDecimal gross = BigDecimal.ZERO;
		if (weeklyBatch.getTimecards().size() == 0) {
			status = ApprovalStatus.BATCH_INITIAL_STATUS;
		}
		else {
			status = ApprovalStatus.BATCH_FINAL_STATUS; // "highest" possible status
			for (WeeklyTimecard wtc : weeklyBatch.getTimecards()) {
				if (wtc.getGrandTotal() != null) {
					gross = gross.add(wtc.getGrandTotal());
				}
				if (wtc.getStatus().compareBatch(status) < 0) {
					// A timecard has a "lower" status, update our running value
					status = wtc.getStatus();
				}
				if (date == null || (wtc.getUpdated() != null &&  wtc.getUpdated().after(date))) {
					// A timecard was updated later than the batch timestamp -- track most recent
					date = wtc.getUpdated();
				}
			}
			if (date == null) {
				date = new Date();
			}
		}
		if (forceSave || (! status.equals(weeklyBatch.getTimecardStatus())) ||
				oldGross.compareTo(gross) != 0 ||
				weeklyBatch.getLastUpdated() == null || date.after(weeklyBatch.getLastUpdated())) {
			weeklyBatch.setGross(NumberUtils.scaleTo(gross,2,2));
			weeklyBatch.setTimecardStatus(status);
			weeklyBatch.setLastUpdated(date); // matches most recently changed timecard
			WeeklyBatchDAO.getInstance().attachDirty(weeklyBatch);
		}
	}

	/**
	 * Create the appropriate name for a WeeklyBatch, based on the given
	 * Production, ProductionBatch, and week-ending date. This takes the name
	 * from the ProductionBatch, and, if appropriate, adds a prefix or suffix
	 * using the date.
	 *
	 * @param prod The Production in which the batch is being created.
	 * @param batch The ProductionBatch that is the template for the new
	 *            WeeklyBatch.
	 * @param date The week-ending date of the batch being created.
	 * @return The name which should be used for the matching WeeklyBatch.
	 */
	private static String createBatchName(Production prod, ProductionBatch batch, Date date) {
		SimpleDateFormat sdf = null;
		String strDate;
		if (batch != null && prod != null) {
			sdf = new SimpleDateFormat("MMddyy");
			strDate = sdf.format(date);
			String name = batch.getName();
			if (prod.getPayrollPref().getIncludeWeSuffix()) {
				if (prod.getPayrollPref().getUseWeAsPrefix()) {
					name = "WE" + strDate + "-" + name;
				}
				else {
					name += "-WE" + strDate;
				}
			}
			return name;
		}
		else {
			sdf = new SimpleDateFormat("yyMMdd");
			strDate = sdf.format(date);
			String batchName = "WE-" + strDate;
			return batchName;
		}
	}

}
