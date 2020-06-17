/**
 * File: ExecuteReport.java
 */
package com.lightspeedeps.batch;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.task.TaskExecutor;

import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.model.Callsheet;
import com.lightspeedeps.model.Folder;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.User;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.object.ReportRequest;
import com.lightspeedeps.type.MimeType;
import com.lightspeedeps.type.ReportStyle;
import com.lightspeedeps.type.ReportType;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.file.FileRepositoryUtils;
import com.lightspeedeps.util.payroll.TimecardPrintUtils;
import com.lightspeedeps.web.report.PrintDailyReportBean;

/**
 * This class both schedules report generation requests, and then executes the
 * requests asynchronously. The requests are stored in a FIFO queue. They are
 * executed asynchronously when a TaskExecutor calls our run() method.
 * <p/>
 * This is a subclass of SpringBatch, so that it can use that class' facilities
 * for setting up and tearing down the necessary application context.
 * <p>
 * This bean is registered in the applicationContextPart1.xml file.
 */
public class ExecuteReport extends SpringBatch implements Runnable {
	private static final Log log = LogFactory.getLog(ExecuteReport.class);

	/** The FIFO queue of ReportRequest`s waiting to be executed. */
	private final Queue<ReportRequest> queue = new LinkedList<>();

	// Our taskExecutor is set via a bean property definition
	private TaskExecutor taskExecutor;

	public ExecuteReport() {
	}

	/**
	 * @return An instance of the ExecuteReport bean.
	 */
	public static ExecuteReport getInstance() {
		return (ExecuteReport)ServiceFinder.findBean("ExecuteReport");
	}

	/**
	 * Schedule a report task to be executed (asynchronously). This creates
	 * the ReportRequest object and schedules it.
	 *
	 * @param reportType The ReportType to be scheduled, e.g., CALL_SHEET.
	 * @param rept The report to be executed (printed or archived).
	 * @param user The user that will be made the 'owner' of the archived
	 *            report.
	 */
	public static void scheduleReport(ReportType reportType, Object rept, User user) {
		// Create a ReportRequest to be queued.
		ReportRequest request = new ReportRequest(reportType, rept, user);
		// Get our bean & queue the request
		ExecuteReport ex = ExecuteReport.getInstance();
		ex.schedule(request);
	}

	/**
	 * Schedule the given request for later execution. This queues up a new
	 * instance of ReportRequest, then uses our TaskExecutor to request
	 * asynchronous execution.
	 *
	 * @param request The ReportRequest to be executed asynchronously.
	 */
	private void schedule(ReportRequest request) {
		synchronized(queue) {
			queue.add(request);
		}
		// schedule ourselves for execution using Spring's taskExecutor
		taskExecutor.execute(this);
	}

	/**
	 * Checks our queue for any report requests and processes them. We extract
	 * the report type and report object from the request, and process it by
	 * invoking the appropriate method of one of the report classes, such as
	 * PrintDailyReportBean. For LS purposes, this method is usually called by
	 * a Spring TaskExecutor instance, so it will be running in a "batch"
	 * environment, without a FacesContext.
	 *
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		ReportRequest rq;
		synchronized(queue) {
			rq = queue.poll();
		}
		if (rq == null) {
			return;
		}
		try {
			setUp(); // initialize the application context

			// Note that here we just instantiate PrintDailyReportBean ourselves, we don't retrieve
			// a "bean" instance.
			PrintDailyReportBean rb = null;

			while (rq != null) {
				log.info("report type=" + rq.getType());
				try {
					switch(rq.getType()) {
					case CALL_SHEET:
						Callsheet cs = (Callsheet)rq.getReportSource();
						if (cs != null) {
							if (rb == null) {
								rb = new PrintDailyReportBean();
							}
							rb.archiveCallsheet(cs, rq.getUser());
						}
						break;
					case TIMECARD:
						WeeklyTimecard wtc = (WeeklyTimecard)rq.getReportSource();
						if (wtc != null) {
							Production prod = ProductionDAO.getInstance().findByProdId(wtc.getProdId());
							ReportStyle reptStyle = ReportStyle.TC_FULL;
							if (prod.getType().isAicp()) {
								reptStyle = ReportStyle.TC_AICP;
							}
							String fileName = TimecardPrintUtils.printTimecard(wtc, reptStyle,
									true, /* include pay breakdown */
									false /* not part of data transfer */ );
							Folder root = prod.getRootFolder();
							String folderName = "Archives/Timecards";
							DateFormat df = new SimpleDateFormat("_yyMMdd_HHmmss");
							Date createDate = new Date();
							String now = df.format(createDate);
							String name = "TC" + wtc.getId() + now;
						//	User owner = UserDAO.getInstance().findOneByProperty(UserDAO.ACCOUNT_NUMBER, wtc.getUserAccount());
							User owner = rq.getUser();
							File file = new File(fileName);
							MimeType type = MimeType.PDF;
							FileRepositoryUtils.storeItem(folderName , name, owner, file, type, createDate, root);
						}
						break;
					default:
						log.error("unsupported report type " + rq.getType());
						break;
					}
				}
				catch (Exception e) {
					log.error("Exception in async report generation: ", e);
				}
				synchronized(queue) {
					rq = queue.poll(); // see if another one is waiting
				}
			}
		}
		catch (Exception e) {
			log.error("exception: ",e);
		}
		finally {
			tearDown();	// required for SpringBatch subclasses - clean up.
		}
	}

	/** See {@link #taskExecutor}. */
	public TaskExecutor getTaskExecutor() {
		return taskExecutor;
	}

	/** See {@link #taskExecutor}. */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

}
