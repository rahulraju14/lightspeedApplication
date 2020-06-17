/**
 * File: ProjectService.java
 */
package com.lightspeedeps.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.RealWorldElementDAO;
import com.lightspeedeps.dao.ScriptElementDAO;
import com.lightspeedeps.dao.StripDAO;
import com.lightspeedeps.dao.UnitDAO;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.model.RealWorldElement;
import com.lightspeedeps.model.Scene;
import com.lightspeedeps.model.ScriptElement;
import com.lightspeedeps.model.Strip;
import com.lightspeedeps.model.Unit;
import com.lightspeedeps.port.TaggedExporter;
import com.lightspeedeps.port.XmlExporter;
import com.lightspeedeps.type.ScriptElementType;
import com.lightspeedeps.type.StripStatus;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.project.ScheduleUtils;
import com.lightspeedeps.util.script.StripUtils;
import com.lightspeedeps.web.view.View;

/**
 * Contains methods with application logic related to Project management.
 */
@ManagedBean
public class ProjectService extends BaseService {
	private static final Log log = LogFactory.getLog(ProjectService.class);

	public ProjectService() {
		log.debug("");
	}

	public static ProjectService getInstance() {
		return (ProjectService)getInstance("ProjectService");
	}

	/**
	 * Action method to generate a Showbiz Budgeting export file. This file
	 * contains ScriptElement and scheduling information. (This is completely
	 * separate from the export of timecard information to Showbiz Budgeting.)
	 *
	 * @return empty navigation string
	 */
	public String actionExportBudget() {
		Project project = SessionUtils.getCurrentProject();
		String filename = exportShowbizBudget(project);
		if ( filename != null) {
			MsgUtils.addFacesMessage("Report.Export.OK", FacesMessage.SEVERITY_INFO);
			// open in "same window" ('_self'), since user should get prompt to save as file
			String javascriptCode = "window.open('../../" + filename
					+ "','_self');";
			View.addJavascript(javascriptCode);
			/** Note: we depend on {@link LsFacesServlet} to encourage the browser to
			 prompt for saving the file (instead of opening it in a browser window)
			 by setting the content-disposition in the response header appropriately.
			 */
		}
		else {
			MsgUtils.addFacesMessage("Report.Export.Failed", FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Create an export file suitable for importing into Showbiz Budgeting.
	 *
	 * @param project The project whose data is to be exported. This will use
	 * the current Script and Stripboard.
	 *
	 * @return The name of the file.
	 */
	public String exportShowbizBudget(Project project) {
		return export(project);
	}

	/**
	 * Export the data from the given Project based on the current Script and
	 * Stripboard, in an XML format suitable for import into Showbiz Budgeting.
	 *
	 * @param project The project whose data is to be exported. This will use
	 *            the current Script and Stripboard.
	 */
	private String export(Project project) {
		String fileLocation = null;
		try {
			Production prod = SessionUtils.getProduction();
			DateFormat df = new SimpleDateFormat("MM-dd_HHmmss");
			String timestamp = df.format(new Date());
			String reportFileName = "schedule";
			reportFileName = reportFileName + "_" + prod.getProdId() + "_" + timestamp;
			reportFileName += ".tab";
			log.debug(reportFileName);
			fileLocation = Constants.REPORT_FOLDER + "/" + reportFileName;
			String reportPath = SessionUtils.getRealReportPath();
			reportFileName = reportPath + reportFileName;
			OutputStream outputStream = new FileOutputStream(new File(reportFileName));
			try {
				TaggedExporter ex = new XmlExporter(outputStream);
				putHeader(ex, project);
				putCategories(ex);
				putElements(ex, project);
				putCast(ex, project);
				if (project.getScript() != null) {
					putScenes(ex, project);
					putUsage(ex, project);
				}
				ex.append("Footer", "Export Complete");
			}
			catch (Exception e) {
				EventUtils.logError(e);
				fileLocation = null;
			}
			finally {
				outputStream.close();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			fileLocation = null;
		}
		return fileLocation;
	}

	/**
	 * Output the Header section of the Showbiz Budgeting export file.
	 *
	 * @param ex The TaggedExporter to which the data should be sent.
	 * @param project The Project whose data is being exported.
	 */
	private void putHeader(TaggedExporter ex, Project project) {
		ex.open("Header");
		ex.append("Info", "LightSPEED export");
		ex.append("Version", "1.0");
		ex.append("Project", project.getTitle());
		ex.append("Episode", project.getEpisode());
		ex.append("Start Date", project.getMainUnit().getProjectSchedule().getStartDate());
		ex.close();
	}

	/**
	 * Output all the Category sections of the Showbiz Budgeting export file.
	 * There is one Category element for each ScriptElementType except CHARACTER
	 * and LOCATION.
	 *
	 * @param ex The TaggedExporter to which the data should be sent.
	 */
	private void putCategories(TaggedExporter ex) {
		for (ScriptElementType type : ScriptElementType.values()) {
			if (type == ScriptElementType.N_A) {
				break;
			}
			if (type != ScriptElementType.CHARACTER) {
				ex.open("Category");
				ex.append("Name",type.getLabel());
				ex.close();
			}
		}
	}

	/**
	 * Output all the Resource sections of the Showbiz Budgeting export file.
	 * There is one Resource element for each ScriptElement in the Project.
	 *
	 * @param ex The TaggedExporter to which the data should be sent.
	 */
	private void putElements(TaggedExporter ex, Project project) {
		List<ScriptElement> list = ScriptElementDAO.getInstance().findByProject(project);
		for (ScriptElement se: list) {
			if (se.getType() == ScriptElementType.CHARACTER || se.getType() == ScriptElementType.LOCATION) {
				// ignore these types
			}
			else {
				ex.open("Resource");
				se.flatten(ex);
				ex.close();
			}
		}
	}

	/**
	 * Output all the Resource sections of the Showbiz Budgeting export file.
	 * There is one Resource element for each ScriptElement in the Project.
	 *
	 * @param ex The TaggedExporter to which the data should be sent.
	 * @param project The Project whose data is being exported.
	 */
	private void putCast(TaggedExporter ex, Project project) {
		List<ScriptElement> list = ScriptElementDAO.getInstance()
				.findByTypeAndProject(ScriptElementType.CHARACTER, project);
		RealWorldElement actor;
		String name;
		for (ScriptElement se: list) {
			ex.open("Cast");
			ex.append("ID", se.getId());
			actor = RealWorldElementDAO.getInstance().findLinkedRealWorldElement(se);
			if (actor != null && actor.getActor() != null) {
				name = actor.getActor().getUser().getFirstNameLastName();
			}
			else {
				name = "";
			}
			ex.append("Actor", name);
			ex.append("Character", se.getName());
			ex.close();
		}
	}

	/**
	 * Output all the Scene sections of the Showbiz Budgeting export file. There
	 * is one Scene element for each Scene in the Project's current Script.
	 *
	 * @param ex The TaggedExporter to which the data should be sent.
	 * @param project The Project whose data is being exported.
	 */
	private void putScenes(TaggedExporter ex, Project project) {
		Date shootDate;
		Strip strip;
		Unit unit = project.getMainUnit();
		Integer unitId = unit.getId();
		ScheduleUtils su = new ScheduleUtils(unit);
		StripDAO stripDAO = StripDAO.getInstance();
		Map<String, Integer> breakdownMap = StripUtils.createBreakdownMap(project);
		if (breakdownMap == null || project.getScript() == null) {
			return;
		}

		for (Scene scene : project.getScript().getScenes()) {
			Integer id = breakdownMap.get(scene.getNumber());
			if (id != null) {
				strip = stripDAO.findById(id);
				if (strip != null && strip.getStatus() != StripStatus.OMITTED) {
					shootDate = null;
					int dayNum = 0;
					if (strip.getStatus() == StripStatus.SCHEDULED) {
						dayNum = stripDAO.findShootDayNumber(strip);
					}
					if (dayNum > 0) {
						if (strip.getUnitId() != null && unitId != strip.getUnitId()) {
							// Unit changed, create new scheduleUtils
							unitId = strip.getUnitId();
							unit = UnitDAO.getInstance().findById(unitId);
							su = new ScheduleUtils(unit);
						}
						shootDate = su.findShootingDay(dayNum);
					}
					ex.open("Scene");
					scene.flatten(ex, shootDate, strip);
					ex.close();
				}
			}
		}
	}

	/**
	 * Output all the Usage sections of the Showbiz Budgeting export file. There
	 * is one Usage element for each ScriptElement's appearance in a Scene in
	 * the Project's current Script.
	 *
	 * @param ex The TaggedExporter to which the data should be sent.
	 */
	private void putUsage(TaggedExporter ex, Project project) {
		for (Scene sc : project.getScript().getScenes()) {
			for (ScriptElement se : sc.getScriptElements()) {
				ex.open("Usage");
				ex.append("Resource ID", se.getId());
				ex.append("Scene ID", sc.getId());
				ex.close();
			}
		}
	}

}
