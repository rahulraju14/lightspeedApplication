package com.lightspeedeps.web.script;

import static com.lightspeedeps.util.app.Constants.ATTR_SP_FROM_DAY;
import static com.lightspeedeps.util.app.Constants.ATTR_SP_FROM_PAGE;
import static com.lightspeedeps.util.app.Constants.ATTR_SP_FROM_SCENE;
import static com.lightspeedeps.util.app.Constants.ATTR_SP_GROUP;
import static com.lightspeedeps.util.app.Constants.ATTR_SP_HIGHLIGHT;
import static com.lightspeedeps.util.app.Constants.ATTR_SP_TO_DAY;
import static com.lightspeedeps.util.app.Constants.ATTR_SP_TO_PAGE;
import static com.lightspeedeps.util.app.Constants.ATTR_SP_TO_SCENE;
import static com.lightspeedeps.util.app.Constants.BLANK_LINE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ActionEvent;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ColorNameDAO;
import com.lightspeedeps.dao.SceneDAO;
import com.lightspeedeps.dao.ScriptDAO;
import com.lightspeedeps.dao.ScriptElementDAO;
import com.lightspeedeps.dao.StripDAO;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.ScriptLine;
import com.lightspeedeps.type.TextElementType;
import com.lightspeedeps.type.WatermarkPreference;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.project.ScheduleUtils;
import com.lightspeedeps.util.report.ScriptReporter;
import com.lightspeedeps.util.script.ScriptUtils;
import com.lightspeedeps.web.popup.PopupEmailTextBean;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.popup.SelectContactsBean;
import com.lightspeedeps.web.popup.SelectContactsHolder;
import com.lightspeedeps.web.report.ReportBean;
import com.lightspeedeps.web.view.ListView;

/**
 * Backing bean for portions the "Script Revisions" web page.  This code primarily
 * handles the display, printing, and emailing of the currently selected script.
 * See ScriptDraftsBean for code that manages the list of scripts, adding and
 * deleting script revisions, etc.
 */
@ManagedBean
@ViewScoped
public class ScriptPageBean implements SelectContactsHolder, PopupHolder, Serializable {
	/** */
	private static final long serialVersionUID = - 1567430313552718140L;

	private static final Log log = LogFactory.getLog(ScriptPageBean.class);

	private static final String NOT_AVAILABLE = "Script text is not available";

	private static final int ACT_EDIT_TEXT = 11;

	private static final String GROUP_SCENE = "SCENE";
	private static final String GROUP_PAGE = "PAGE";
	private static final String GROUP_DAY = "DAY";

	private static final String CSS_COLOR_PREFIX = "PgRev";
	private static final String CSS_CLASS_PREFIX = "ST_";
	private static final String CSS_CLASS_PREFIX_NOT = "STN_";
//	private static final String CSS_HIGH_SUFFIX = "_HIGH"; // appended if highlighting is on
	private static final String CSS_CLASS_FINAL = "ST_END";

	private List<SelectItem> selectItems = new ArrayList<>();
	private String selectedGroup = GROUP_SCENE;

	/** The Script being displayed -- the one selected by the user from the revision list. */
	private Script script;

	/** Physical page number of the first scene in the script. */
	private Integer firstPageNum;

	/** Starting Scene number -- as index into list (origin 0) -- from drop-down box on screen. */
	private Integer fromScene = 0;
	/** Ending Scene number -- as index into list (origin 0) -- from drop-down box on screen. */
	private Integer toScene = 0;

	/** The Scene.sequence value of the first Scene in the range to be displayed. */
	private Integer fromSceneSeq;
	/** The Scene.sequence value of the last Scene in the range to be displayed. */
	private Integer toSceneSeq;

	/** Starting physical page number (origin 1) from drop-down box on screen. */
	private Integer fromPageNumber = 1;
	/** Ending physical page number (origin 1) from drop-down box on screen. */
	private Integer toPageNumber = 1;

	/** Starting page, either the user's selected value for a page range, or as
	 * calculated from the scene range. */
	private int startPage;
	/** Ending page, either the user's selected value for a page range, or as
	 * calculated from the scene range. */
	private int endPage;

	/** A list of all the logical page numbers, indexed by the physical page number.
	 * That is, pageNumbers.get(i) is the logical page number of the i'th physical
	 * page. */
	private List<String> scriptPageNumbers;

	/** A list of the revision colors that apply to each page.  This is based on
	 * maximum of the Scene.lastRevised value for all the Scenes that appear
	 * on that page. */
	private List<Integer> pageColors;

	/** Starting shooting day number (origin 1) from drop-down box on screen. */
	private Integer toDay = 1;
	/** Ending shooting day number (origin 1) from drop-down box on screen. */
	private Integer fromDay = 1;

	private boolean override = false;
	private boolean overrideGroup = false;

	/** If true, highlight the user's Character's dialogue lines on the screen. */
	private boolean highlight = false;
	/** The names of the characters whose lines should be highlighted; or null if highlighting is off. */
	private List<String> characterNames = null;

	/** Set to true if the Project preference is set to allow the
	 * display of script text. */
	private boolean showText;

	/** True iff we are supporting the Breakdown page, which only shows the text from
	 * one scene at a time, without page breaks. */
	private boolean singleScene;

	/** If true, allow the "shoot days" selection option.  We set this
	 * to true if there is a stripboard, and it has more than one shooting day. */
	private boolean showDays = false;

	/** The list of Scenes matching the current selection criteria. */
	private List<Scene> sceneList;

	/** The list of ScriptLine to be displayed, generated from the
	 * list of selected Scenes or pages.  */
	private List<ScriptLine> lineList;

	/** A List of the scene.id values for ALL the scenes in the script, in script order.
	 * This will match the list of SelectItems for the scene number selection. */
	private final List<Integer> allSceneIds = new ArrayList<>();

	/** List of all scene numbers, for drop-down list, in script order.  The SelectItem
	 * values are sequential, origin zero, and the labels are the logical scene numbers. */
	private List<SelectItem> sceneNumberList = new ArrayList<>();

	/** List of SelectItem`s of page numbers, sequential beginning with the first page of
	 * the script through the last page number in script.  The value is sequential, origin 1,
	 * that is, the physical page number, and the label is the logical page number (what is
	 * printed on the page). */
	private List<SelectItem> pageNumbers = new ArrayList<>();

	/** List of shooting day numbers for drop-down lists (sequential 1...n). */
	private List<SelectItem> dayNumbers = new ArrayList<>();

	/** Number of lines to display per page, including trailing blank lines. */
	private int pageLength;

	// Print / Email options

	/** True iff 'print options' dialog should be displayed. */
	private boolean showPrintOptions;

	/** True if user hit Print button, false if user hit Email button. */
	private boolean doPrint;

	/** Print full pages or "sides" style (2-up) */
	private String printStyle = PRINT_STYLE_FULL; // 's'(sides) or 'f'(full)
	private static final String PRINT_STYLE_SIDES = "s";
	private static final String PRINT_STYLE_FULL = "f";

	/** Which of the "sides" styles to use, 1=Sequential, 2=Duplicate */
	private int sidesType = ScriptReporter.SIDES_TYPE_DUPLICATE;

	/** True if user changed style in last input cycle, in which case we ignore
	 *  the next 'setWatermark' (generated from framework) so watermark value
	 *  keeps change caused by style change. */
	private boolean styleChanged;

	/** Include date on watermark? */
	private String watermarkDate = PRINT_MARK_DATE; // 'y' or 'n'
	private static final String PRINT_MARK_DATE = "y";
	//private static final String PRINT_MARK_NO_DATE = "n";

	/** Include watermark on print? */
	private boolean watermark = true;

	/** If true, highlight the user's Character's dialogue lines on the printout. */
	private boolean printHighlight = false;

	/** Include the script's title page(s), if any. */
	private boolean includeTitlePages = false;

	/** The user's chosen style of coloring printed pages. */
	private int colorStyle = PRINT_COLOR_ALL;

	/** drop-down selection for print-color style */
	private List<SelectItem> colorStyleDL = Arrays.asList(
			new SelectItem(PRINT_COLOR_NONE,	"Do not print color"),
			new SelectItem(PRINT_COLOR_ALL,		"On entire page"),
			new SelectItem(PRINT_COLOR_STRIPE,	"On right edge only"));
	public static final int PRINT_COLOR_ALL = 0;
	public static final int PRINT_COLOR_STRIPE = 1;
	public static final int PRINT_COLOR_NONE = 2;

	/** Indicates how print range will be selected - either the currently displayed
	 * data, or the entire script, or a list of Scene numbers. */
	private String printSelection = PRINT_CURRENT; // 'c'=current, 'a'=all, 'l'=list
	private static final String PRINT_CURRENT = "c";
	private static final String PRINT_ALL = "a";
	private static final String PRINT_REVISION = "r";
	private static final String PRINT_SCENE_LIST = "s";

	/** print options: print range: current selection */
	private boolean printRangeCurrent = true;

	/** print options: print range: entire script */
	private boolean printRangeAll;

	/** print options: print range: pages from selected revision */
	private boolean printRangeRevision;

	/** print options: print range: list of specific scenes */
	private boolean printRangeScenes;

	/** The revision number the user has asked to print. */
	private int printRevision;

	/** Drop-down list from which user can choose which revision pages to print. */
	private List<SelectItem> revisionDL;

	/** The list of Scene numbers to print, comma-delimited. */
	private String scenesToPrint;

	/** List of Scene`s to print, matching the 'scenesToPrint' string entered
	 * by the user. */
	private List<Scene> printSceneList;

	/** Title of pop-up dialog box (varies for email vs print) */
	private String printTitle;
	/** Label for 'ok' button in first print dialog box. */
	private String printButtonOkLabel = ""; /* init to prevent NPE in renderer */

	/** The possibly customized subject line for the emails generated when
	 * sending a script. */
	private String emailSubject;

	/** The possibly customized body text for the emails generated when
	 * sending a script. */
	private String emailBody;

	/** The list of Contacts selected by the user when emailing a script. */
	private Collection<Contact> emailList;

	public ScriptPageBean() {
		log.debug("");
		try {
			Project project = SessionUtils.getCurrentProject();
			showText = project.getScriptTextAccessible();
			Production prod = SessionUtils.getProduction();
			watermark = (prod.getWatermark() != WatermarkPreference.FORBIDDEN);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	public static ScriptPageBean getInstance() {
		return (ScriptPageBean) ServiceFinder.findBean("scriptPageBean");
	}

	/**
	 * Called from the Script Revision page bean to set up to display some part
	 * of the given Script, typically when a user clicks on some row in the
	 * revision list.
	 *
	 * @param pScript The Script to display, or null if no Script is available,
	 *            usually because no Script has been loaded yet.
	 */
	public void setupScript(Script pScript) {
		log.debug("");
		sceneList = null;
		lineList = null;
		script = pScript;
		showText = false;
		clearScriptDisplayData(); // clear old data, even if new script is null
		if (script != null) {
			showText = SessionUtils.getCurrentProject().getScriptTextAccessible()
					&& script.getSceneText();
			loadScriptData(-1);
		}
	}

	/**
	 * Set up to display a single scene from a Script -- used by the breakdown
	 * page.
	 *
	 * @param pScript The Script to be displayed.
	 * @param listIndex The sequential scene number (not logical alphanumeric).
	 */
	public void setupScriptScene(Script pScript, int listIndex) {
		log.debug("");
		sceneList = null;
		lineList = null;
		script = pScript;
		showText = false;
		if (script != null) {
			showText = SessionUtils.getCurrentProject().getScriptTextAccessible()
					&& script.getSceneText();
			loadScriptData(listIndex);
		}
	}

	/**
	 * CURRENTLY UNUSED -- jump to specific page on Script Page screen.
	 * @param evt
	 */
	public void openScriptPage(ActionEvent evt) {
		SessionUtils.put(ATTR_SP_FROM_PAGE, getFromPageNumber());
		SessionUtils.put(ATTR_SP_TO_PAGE, getToPageNumber());
		SessionUtils.put(ATTR_SP_GROUP, GROUP_PAGE);
	}

	/**
	 * Set up to display a specific Script. Called when this bean is
	 * instantiated, and again whenever the user chooses a different Script to
	 * display.
	 *
	 * @param sceneNbr The scene number to be displayed if single-scene mode
	 *            (used for the Breakdown page), or any negative value for the
	 *            normal Script Revision page.
	 */
	private void loadScriptData(int sceneNbr) {
		log.debug("");
		try {
			clearScriptDisplayData();

			Project project = SessionUtils.getCurrentProject();
			// Set up scene number list and defaults
			List<Scene> listscene = script.getScenes();
			int i = 0;
			for (Scene scene : listscene) {
				sceneNumberList.add(new SelectItem(new Integer(i++), scene.getNumber()));
				allSceneIds.add(scene.getId());
			}

			if (script.getLinesPerPage() == 0) {
				pageLength = Constants.SCRIPT_MAX_LINES_PER_PAGE + 1;
			}
			else {
				pageLength = script.getLinesPerPage() + 3;
			}

			if (sceneNbr >= 0) { // showing a single scene - for breakdown page
				singleScene = true;
				highlight = false;
				selectedGroup = GROUP_SCENE;
				fromScene = Math.min(sceneNbr, listscene.size()-1);
				toScene = fromScene;
				firstPageNum = 1;
			}
			else { // for Script Revisions page
				singleScene = false;
				// check for "highlight" saved setting
				highlight = (SessionUtils.getInteger(ATTR_SP_HIGHLIGHT, 0).intValue() != 0);
				Contact user = SessionUtils.getCurrentContact();
				final ScriptElementDAO scriptElementDAO = ScriptElementDAO.getInstance();
				List<ScriptElement> list = scriptElementDAO.findCharacterFromContact(user, SessionUtils.getCurrentProject());
				if (list.size() > 0) {
					characterNames = new ArrayList<>();
					for (ScriptElement se : list) {
						characterNames.add(se.getName().toUpperCase());
					}
				}
				// Set up shooting-day list & defaults
				int days = 1;
				if (project.getStripboard() != null) {
					Unit unit = project.getMainUnit(); // Use Main Unit for "shooting days" drop-down
					days = project.getStripboard().getShootingDays(unit);
					if (days > 0) {
						setShowDays(true);
					}
					for (i = 1; i <= days; i++) {
						dayNumbers.add(new SelectItem(new Integer(i)));
					}
					Date date = new Date();
					ScheduleUtils su = new ScheduleUtils(unit);
					int today = su.findShootingDayNumber(date);
					if (today == 0) {
						Calendar cal = su.findNextWorkDate(date);
						if (cal != null) {
							today = su.findShootingDayNumber(cal.getTime());
						}
					}
					if (today != 0) {
						setFromDay(today);
						setToDay(today);
					}
				}
				// Set up page number list and defaults
				if (listscene.size() > 0) {
					Scene firstScene = listscene.get(0);
					firstPageNum = firstScene.getPageNumber();
					if (firstPageNum < 1) { // can happen with FDX imports. rev 3819
						firstPageNum = 1;
					}
				}
				else {
					firstPageNum = 1;
				}
				setFromPageNumber(firstPageNum);
				setToPageNumber(firstPageNum);
				scriptPageNumbers = ScriptReporter.createPageNumberList(script);
				for (i = 1; i <= script.getLastPage(); i++) {
					pageNumbers.add(new SelectItem(i,scriptPageNumbers.get(i)));
				}

				// Get default/saved group and other settings...
				selectedGroup = SessionUtils.getString(ATTR_SP_GROUP, GROUP_SCENE);
				Integer from, to;

				from = SessionUtils.getInteger(ATTR_SP_FROM_PAGE, firstPageNum);
				to = SessionUtils.getInteger(ATTR_SP_TO_PAGE, firstPageNum);
				fromPageNumber = Math.min(from, script.getLastPage());
				toPageNumber = Math.min(to, script.getLastPage());

				from = SessionUtils.getInteger(ATTR_SP_FROM_DAY, 1);
				to = SessionUtils.getInteger(ATTR_SP_TO_DAY, 1);
				fromDay = Math.min(from, days);
				toDay = Math.min(to, days);

				from = SessionUtils.getInteger(ATTR_SP_FROM_SCENE, 0);
				if (from < 0) { // happens if last viewed script had no scenes
					from = 0;
				}
				to = SessionUtils.getInteger(ATTR_SP_TO_SCENE, 0);
				if (to < 0) { // happens if last viewed script had no scenes
					to = 0;
				}
				fromScene = Math.min(from, listscene.size()-1);
				toScene = Math.min(to, listscene.size()-1);
			}

			pageColors = ScriptReporter.createPageColorList(script,
					ScriptReporter.createPageRevisionList(script));

			createSceneListByGroupSelected();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Reset fields related to displaying the script to their
	 * "empty" values.
	 */
	private void clearScriptDisplayData() {
		characterNames = null;
		fromDay = 1;
		toDay = 1;
		dayNumbers.clear();
		setShowDays(false);
		fromScene = 0;
		toScene = 0;
		sceneNumberList.clear();
		allSceneIds.clear();
		pageNumbers.clear();
	}

	/**
	 * Action method of the (initial) "Print" button.  We invoke a ReportBean method to generate
	 * a PDF of the currently selected range of Scenes.
	 * @return null navigation string
	 */
	public String actionPrint() {
		log.debug("");
		try {
			if (showPrintOptions) { // double-clicked? dialog box is already open
				return null;
			}
			if (script != null && showText) {
				initPrint();
				doPrint = true;
				printTitle = MsgUtils.getMessage("Script.Print.Title");
				printButtonOkLabel = MsgUtils.getMessage("Script.Print.OkButton");
				if (characterNames == null) {
					printHighlight = false;
				}
			}
			else {
				MsgUtils.addFacesMessage("Script.NoScriptText", FacesMessage.SEVERITY_ERROR);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Action method of the "Email" button.  Display the print optinns
	 * dialog box.
	 * @return null navigation string
	 */
	public String actionEmail() {
		log.debug("");
		try {
			if (showPrintOptions) { // double-clicked? dialog box is already open
				return null;
			}
			if (script != null && showText) {
				initPrint();
				doPrint = false;
				printTitle = MsgUtils.getMessage("Script.Email.Title");
				printButtonOkLabel = MsgUtils.getMessage("Script.Email.OkButton");
			}
			else {
				MsgUtils.addFacesMessage("Script.NoScriptText", FacesMessage.SEVERITY_ERROR);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	private void initPrint() {
		if (printSelection == null || printStyle == null || watermarkDate == null || colorStyle < 0) {
			// something odd happened; we saw this from double-clicking Print or Email
			initDefaultOptions();
		}
		showPrintOptions = true;
		if (! printSelection.equals(PRINT_SCENE_LIST)) {
			scenesToPrint = "";
		}
		revisionDL = new ArrayList<>();
		ScriptDraftsBean sdb = (ScriptDraftsBean)ServiceFinder.findBean("scriptDraftsBean");
		// create revision selection list, starting with currently selected script
		boolean include = false;
		for (Script s : sdb.getScriptList()) { // Use displayed revision list
			if ( (! include) && s.getId().equals(script.getId())) {
				include = true;
			}
			if (include) {
				s = ScriptDAO.getInstance().refresh(s);
				int revNumber = s.getRevisionNumber();
				if (revNumber == 1 || s.getColorName().getScriptRevision() == 1) {
					// don't list rev 1, or any "White(1)" revision
					break;
				}
				String color = s.getColorName().getName() + " - rev " + revNumber;
				revisionDL.add(new SelectItem(revNumber, color));
			}
		}
		printRevision = script.getRevisionNumber();
		if (printSelection.equals(PRINT_REVISION) && revisionDL.size() == 0) {
			printSelection = PRINT_CURRENT;
		}
	}

	/**
	 * Action method of the Cancel button on the Print Options
	 * dialog box.
	 * @return null navigation string
	 */
	public String actionPrintCancel() {
		showPrintOptions = false;
		ListView.addClientResizeScroll();
		return null;
	}

	/**
	 * Action method for the "X" close icon button on the "Print Script" pop-up dialog.
	 * This just closes the dialog box.
	 * @param evt Ajax event from the framework.
	 */
	public void actionPrintClose(AjaxBehaviorEvent evt) {
		log.debug("");
		actionPrintCancel();
	}

	/**
	 * Action method of the "Print" button. If the user started this sequence
	 * using the Print button, we have gathered all option settings and are ready
	 * to do the actual printing.  Invoke a ReportBean method to generate a PDF
	 * based on the user's selected options.
	 * <p>
	 * If the user started by clicking the Email button, we proceed to the next
	 * step in email: the recipient selection.
	 *
	 * @return null navigation string
	 */
	public String actionPrintOk() {
		try {
			if (printSelection == null || printStyle == null || watermarkDate == null) {
				// something odd happened; we saw this from double-clicking Print or Email
				initDefaultOptions();
				MsgUtils.addFacesMessage("Script.Print.MissingOptions", FacesMessage.SEVERITY_INFO);
				return null;
			}
			if (printSelection.equals(PRINT_SCENE_LIST)) {
				printSceneList = createSceneList(scenesToPrint);
				if (printSceneList == null) {
					return null;
				}
			}
			showPrintOptions = false;
			ListView.addClientResizeScroll(); // check scrollable list position
			if (doPrint) {
				print(null);
			}
			else {
				return actionEmailRecipients();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Action method called (indirectly) when user clicks the equivalent of the
	 * "ok" button on the Email Options dialog box. Here we put up the
	 * 'recipient selection' dialog box.
	 *
	 * @return null navigation string
	 */
	private String actionEmailRecipients() {
		log.debug("");
		try {
			SelectContactsBean.getInstance().show(
					0, this, null, "Script.EmailContacts.Title" );
			// control returns via the contactsSelected() method
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * This method is a callback -- called from SelectContactsBean after the user
	 * has finished selected the email recipients for a script report.
	 *
	 * @see com.lightspeedeps.web.popup.SelectContactsHolder#contactsSelected(int, java.util.Collection)
	 */
	@Override
	public void contactsSelected(int action, Collection<Contact> list) {
		showPrintOptions = false;
		if (list != null && list.size() > 0) {
			switch(action) {
			default:
				emailList = list;
				actionEmailEditText();
				break;
			}
		}
	}

	/**
	 * Action method called after the user is done with the 'recipient
	 * selection' dialog box. Here we set up to display the dialog box allowing
	 * the user to edit the subject and body of the email being sent with the
	 * script.
	 * <p>
	 * When the user completes the editing, the Ok/Send button on that dialog
	 * box will return control to us via the confirmOk (or confirmCancel)
	 * method.
	 *
	 * @return null navigation string
	 */
	private String actionEmailEditText() {
		log.debug("");
		try {
			Project project = SessionUtils.getCurrentProject();
			Production prod = project.getProduction();

			PopupEmailTextBean bean = PopupEmailTextBean.getInstance();
			bean.show(this, ACT_EDIT_TEXT, "Script.EditEmail." );

			final String msgPrefix = "Script.EmailDelivery."; // "MessageHandler.script";
			final User user = SessionUtils.getCurrentUser();
			final String sender = user.getAnyName();
			final String email = user.getEmailAddress();
			Locale locale = Constants.LOCALE_US;

			final String subject = DoNotification.formatMessage(msgPrefix + "Subject",
					prod.getTitle(), project, locale, null, sender, email);
			final String body = DoNotification.formatMessage(msgPrefix + "Msg", prod.getTitle(),
					project, locale, null, sender, email);

			bean.setSubject(subject);
			bean.setBody(body);
			// control returns via the confirmOk or confirmCancel methods
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * @see com.lightspeedeps.web.popup.PopupHolder#confirmOk(int)
	 */
	@Override
	public String confirmOk(int action) {
		switch(action) {
			case ACT_EDIT_TEXT:
				PopupEmailTextBean bean = PopupEmailTextBean.getInstance();
				emailSubject = bean.getSubject();
				emailBody = bean.getBody();
				print(emailList);
				ListView.addClientResizeScroll(); // check scrollable list position
			default:
				break;
		}
		return null;
	}

	/**
	 * @see com.lightspeedeps.web.popup.PopupHolder#confirmCancel(int)
	 */
	@Override
	public String confirmCancel(int action) {
		// no action required
		return null;
	}

	/**
	 * ActionListener method on the radio buttons selecting which print style -
	 * full page or Sides - is to be used.
	 *
	 * @param event The event object created by the framework.
	 */
	public void listenPrintStyle(ValueChangeEvent event) {
		try {
			if (event.getNewValue() != null) {
				Production prod = SessionUtils.getProduction();
				String style = (String)event.getNewValue();
				if (style.equals(PRINT_STYLE_FULL)) {
					watermark = (prod.getWatermark() != WatermarkPreference.FORBIDDEN);
				}
				else {
					watermark = (prod.getWatermarkSides() == WatermarkPreference.REQUIRED);
				}
				styleChanged = true; // set special flag...
				// ...so usual modelUpdatePhase call to setWatermark() doesn't change it back
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * ActionListener method on the checkbox for Apply Watermark. We just need
	 * to clear the trick flag so the user's action on the checkbox will be
	 * respected.
	 *
	 * @param event The event object created by the framework.
	 */
	public void listenWatermark(ValueChangeEvent event) {
		styleChanged = false;
	}

	/**
	 * Do the requested print action. The various print parameters should all be
	 * set by the time this is called.
	 *
	 * @param list List of Contacts to receive the script print if emailing;
	 *            or 'null' if the print is to be displayed immediately in the
	 *            user's browser.
	 */
	private void print(Collection<Contact> list) {
		log.debug("");
		try {
			script = ScriptDAO.getInstance().refresh(script);
			final SceneDAO sceneDAO = SceneDAO.getInstance();
			List<Scene> scenes = sceneList;
			if (printSelection.equals(PRINT_ALL) ||
					printSelection.equals(PRINT_REVISION)) {
				startPage = 1;
				endPage = script.getLastPage();
			}
			else if (selectedGroup.equals(GROUP_DAY) || printSelection.equals(PRINT_SCENE_LIST)) {
				startPage = 0;
				endPage = Integer.MAX_VALUE;
			}
			if (printSelection.equals(PRINT_SCENE_LIST)) {
				scenes = new ArrayList<>();
				for (Scene s : printSceneList) {
					scenes.add(sceneDAO.refresh(s));
				}
			}
			else if ((! printSelection.equals(PRINT_CURRENT)) && (! script.hasPageData())) {
				scenes = new ArrayList<>();
				for (Scene s : script.getScenes()) {
					scenes.add(sceneDAO.refresh(s));
				}
			}

			if (printSelection.equals(PRINT_CURRENT) && scenes.size() == 0) {
				// check for no data to print; rev 4063
				if (selectedGroup.equals(GROUP_DAY) ||
						(selectedGroup.equals(GROUP_PAGE) && ! script.hasPageData()) ) {
					MsgUtils.addFacesMessage("Script.Print.NoTextInSelection", FacesMessage.SEVERITY_ERROR);
					return;
				}
			}
			ScriptReporter reporter = new ScriptReporter(script, scenes, startPage, endPage);
			reporter.setFromSceneSeq(0);
			reporter.setToSceneSeq(Integer.MAX_VALUE);
			if (printHighlight) {
				// If reporter is given non-blank character name, it will highlight output
				reporter.setCharacterNames(characterNames);
			}
			if (printSelection.equals(PRINT_CURRENT)) {
				if (selectedGroup.equals(GROUP_SCENE)) {
					reporter.setFromSceneSeq(fromSceneSeq);
					reporter.setToSceneSeq(toSceneSeq);
				}
				else if (selectedGroup.equals(GROUP_DAY)) {
					reporter.setDayRange(true);
				}
				else if (selectedGroup.equals(GROUP_PAGE)) {
					reporter.setPageRange(true);
				}
			}
			else if (printSelection.equals(PRINT_ALL)) {
				reporter.setPageRange(true);
			}
			else if (printSelection.equals(PRINT_REVISION)) {
				reporter.setRevision(true);
				reporter.setRevisionNumberHigh(printRevision);
				// Determine if there are contiguous, previous, revisions with same color
				// as the requested revision's color; if so, include them in print range.
				int lowrev = printRevision;
				boolean check = false;
				int colorId = 0;
				ScriptDraftsBean sdb = (ScriptDraftsBean)ServiceFinder.findBean("scriptDraftsBean");
				for (Script s : sdb.getScriptList()) { // Use displayed revision list
					if ((! check) && (s.getRevisionNumber() == printRevision)) {
						check = true; // start checking lower revisions for matching color
						colorId = s.getColorName().getId().intValue();
					}
					else if (check) { // at a revision prior to the print requested
						if (s.getColorName().getId().intValue() == colorId) {
							// ... and it's the same color, so include it in print
							lowrev = s.getRevisionNumber();
						}
						else { // no more contiguous matching color revisions
							break;
						}
					}
				}
				reporter.setRevisionNumberLow(lowrev);
			}
			else if (printSelection.equals(PRINT_SCENE_LIST)) {
				reporter.setDayRange(true);
			}
			reporter.setSidesStyle(printStyle.equals(PRINT_STYLE_SIDES));
			reporter.setSidesType(sidesType);
			reporter.setIncludeTitlePages(includeTitlePages);
			ReportBean.getInstance().generateScript(reporter, SessionUtils.getCurrentUser().getAnyName(),
					list, script.getDate(), watermark,
					watermarkDate.equals(PRINT_MARK_DATE), colorStyle,
					emailSubject, emailBody);
			log.debug("done");
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addFacesMessage("Generic.SystemError", FacesMessage.SEVERITY_ERROR);
		}
	}

	/**
	 * Validates the user-entered string of scene numbers to be printed, and
	 * creates a List of Scene`s matching the request.
	 *
	 * @param sceneStr A String of comma-delimited scene numbers.
	 * @return A List of Scene`s, if the user's input is valid, otherwise null.
	 *         If null is returned, a Faces error message has been queued for
	 *         display.
	 */
	private List<Scene> createSceneList(String sceneStr) {
		List<Scene> scenes = null;
		if (sceneStr == null || sceneStr.trim().length() == 0) {
			MsgUtils.addFacesMessage("Script.Print.MissingScenes", FacesMessage.SEVERITY_ERROR);
		}
		else {
			String[] parts = sceneStr.split(",");
			if (parts.length > 0) {
				final SceneDAO sceneDAO = SceneDAO.getInstance();
				scenes = new ArrayList<>();
				for (int i=0; i < parts.length; i++) {
					Scene scn = sceneDAO.findByScriptAndNumber(script, parts[i].trim());
					if (scn == null) {
						MsgUtils.addFacesMessage("Script.Print.UnknownScene", FacesMessage.SEVERITY_ERROR, parts[i]);
						return null;
					}
					scenes.add(scn);
				}
			}
		}
		return scenes;
	}

	/**
	 * ActionListener method on the radio buttons selecting which
	 * type of range (page, scene, or shooting day) is to be used.
	 * @param event
	 */
	public void listenSelectedGroup(ValueChangeEvent event) {
		try {
			if (event.getNewValue() != null) {
				String group = (String)event.getNewValue();
				setSelectedGroup(group);
				createSceneListByGroupSelected();
				saveSettings();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * ActionListener for "highlight" check-box.
	 * @param event
	 */
	public void listenHighlight(ValueChangeEvent event) {
		log.debug("");
		if (event.getNewValue() != null) {
			setHighlight((Boolean)event.getNewValue());
			createSceneListByGroupSelected(); // force refresh
		}
	}

	/**
	 * ActionListener on "From" Page selector.
	 */
	public void listenFromPage(ValueChangeEvent event) {
		log.debug("");
		if (event.getNewValue() != null) {
			displayPageRange((Integer)event.getNewValue(), getToPageNumber(), true);
		}
	}

	/**
	 * ActionListener on "To" Page selector.
	 */
	public void listenToPage(ValueChangeEvent event) {
		log.debug("");
		if (event.getNewValue() != null) {
			displayPageRange(getFromPageNumber(), (Integer)event.getNewValue(), false);
		}
	}

	/**
	 * ActionListener on "From" Scene selector.
	 */
	public void listenFromScene(ValueChangeEvent event) {
		log.debug("");
		if (event.getNewValue() != null) {
			displaySceneRange( (Integer)event.getNewValue(), getToScene(), true);
			saveSettings();
		}
	}

	/**
	 * ActionListener on "To" Scene selector.
	 */
	public void listenToScene(ValueChangeEvent event) {
		log.debug("");
		if (event.getNewValue() != null) {
			displaySceneRange( getFromScene(), (Integer)event.getNewValue(), false);
			saveSettings();
		}
	}

	/**
	 * ActionListener on "From" Shooting Day selector.
	 */
	public void listenFromDay(ValueChangeEvent event) {
		log.debug("");
		if (event.getNewValue() != null) {
			displayDayRange((Integer)event.getNewValue(), getToDay(), true);
		}
	}

	/**
	 * ActionListener on "To" Shooting Day selector.
	 */
	public void listenToDay(ValueChangeEvent event) {
		log.debug("");
		if (event.getNewValue() != null) {
			displayDayRange(getFromDay(), (Integer)event.getNewValue(), false);
		}
	}

	public void listenChangeRange(ValueChangeEvent event) {
		printRangeAll = false;
		printRangeScenes = false;
		printRangeCurrent = false;
		printRangeRevision = false;
		if (event.getNewValue() != null) {
			log.debug(event.getComponent().getClientId());
			String id = event.getComponent().getClientId();
			id = id.substring(id.length()-1);
			setPrintSelection(id);
			switch(id) {
				case PRINT_CURRENT:
					printRangeCurrent = true;
					break;
				case PRINT_ALL:
					printRangeAll = true;
					break;
				case PRINT_REVISION:
					printRangeRevision = true;
					break;
				case PRINT_SCENE_LIST:
					printRangeScenes = true;
					break;
				default: /* shouldn't happen */
					setPrintSelection(PRINT_CURRENT);
					printRangeCurrent = true;
					break;

			}
		}
	}

	/**
	 * Display the Script text from a single scene. Used by the Breakdown page.
	 *
	 * @param sceneIx The sequential scene number (origin 0) to be displayed.
	 */
	/* package */ void displayScene(Integer sceneIx) {
		displaySceneRange(sceneIx, sceneIx, true);
	}

	/**
	 * Display the Script text that covers the specified range of pages. The
	 * page numbers are the physical page numbers (not the logical, printed,
	 * page numbers.)
	 *
	 * @param from The first page of text to be displayed.
	 * @param to The last page of text to be displayed.
	 * @param fromChanged True if the "from" value has changed since the last
	 *            display call. This value is used to determine which parameter
	 *            has "precedence", as we may need to adjust one of the
	 *            parameters to force the "from" value to be less than or equal
	 *            to the "to" value.
	 */
	private void displayPageRange(Integer from, Integer to, boolean fromChanged) {
		override = false;
		setSelectedGroup(GROUP_PAGE);
		overrideGroup = true;
		try {
			if (from != null) {
				setFromPageNumber(from);
			}
			if (to != null) {
				setToPageNumber(to);
			}
			if (toPageNumber < fromPageNumber) {
				if (fromChanged) {
					setToPageNumber(fromPageNumber);
				}
				else {
					setFromPageNumber(toPageNumber);
				}
				override = true;
			}
			createSceneListByGroupSelected();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Set up to display the specified range of scenes; updates the 'sceneList'
	 * field.
	 *
	 * @param from The beginning (internal) scene number, origin zero.
	 * @param to The ending (inclusive, internal) scene number, origin zero.
	 * @param fromChanged True if the user changed the "from" (starting) value.
	 *            This is used to determine which parameter to change -- 'from'
	 *            or 'to' -- when the range is reversed ('from' is greater than
	 *            'to').
	 */
	private void displaySceneRange(Integer from, Integer to, boolean fromChanged) {
		override = false;
		setSelectedGroup(GROUP_SCENE);
		overrideGroup = true;
		if (from != null) {
			setFromScene(from);
		}
		if (to != null) {
			setToScene(to);
		}
		try {
			if (to != null && from != null) {
				if (to < from) {
					if (fromChanged) {
						setToScene(from);
					}
					else {
						setFromScene(to);
					}
					override = true;
				}
				createSceneListByGroupSelected();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	private void displayDayRange(Integer from, Integer to, boolean fromChanged) {
		if (from == null || to == null) {
			return;
		}
		override = false;
		setSelectedGroup(GROUP_DAY);
		overrideGroup = true;
		if (from != null) {
			setFromDay(from);
		}
		if (to != null) {
			setToDay(to);
		}
		try {
			if (to != null && from != null) {
				if (to < from) {
					if (fromChanged) {
						setToDay(from);
					}
					else {
						setFromDay(to);
					}
					override = true;
				}
				createSceneListByGroupSelected();
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * This creates 'sceneList', the List of Scene's to be displayed.
	 * This List controls the data generated by createTextList().  Whenever
	 * this method is called, it sets textList to null, which will force
	 * a call to createTextList() as soon as the JSP tries to render the
	 * textList contents.
	 */
	private void createSceneListByGroupSelected() {
		log.debug("selected Group=" + selectedGroup);
		script = ScriptDAO.getInstance().refresh(script);
		lineList = null;	// force refresh
		try {
			if (selectedGroup.equals(GROUP_SCENE)) {
				//saveSettings(); don't save here, may be from breakdown page.
				sceneList = findScenesBySceneRange();
				if (sceneList.size() == 0) {
					//String message = "No Scene numbers matched; script text not available.";
					//MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_INFO, message);
					// don't display warning; script text area will display "text missing/not available".
				}
			}
			else if (selectedGroup.equals(GROUP_PAGE)) {
				saveSettings();
				if (pageNumbers.size() > 0) {
					sceneList = findScenesByPageRange(fromPageNumber, toPageNumber);
				}
				else {
					String message = "No page numbers; script text not available.";
					MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_INFO, message);
				}
				startPage = fromPageNumber;
				endPage = toPageNumber;
			}
			else { // shooting day number range
				saveSettings();
				if (dayNumbers.size() > 0) {
					sceneList = findScenesByDayRange();
				}
				else {
					String message = "No shooting day numbers; script text not available.";
					MsgUtils.addFacesMessage("GenericMessage", FacesMessage.SEVERITY_INFO, message);
				}
				startPage = 0;
				endPage = Integer.MAX_VALUE;
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Find the List of Scenes which covers the given page range, including all
	 * of the start page -- which means we have to include whatever is last on
	 * the prior page, because it may extend onto the beginning page of the
	 * range.
	 *
	 * @return the non-null List described above.
	 */
	private List<Scene> findScenesByPageRange(int start, int end) {
		log.debug("");
		List<Scene> scenes =  new ArrayList<>();
		Scene priorPageScene = null;
		final SceneDAO sceneDAO = SceneDAO.getInstance();
		if (start > firstPageNum) {
			// we only need this if range begins after the first script page
			for (int fromPage = start-1; fromPage >= firstPageNum; fromPage--) {
				scenes = sceneDAO.findByPageRangeAndScript(fromPage, start-1, script);
				if (scenes.size() > 0) {
					priorPageScene = scenes.get(scenes.size()-1); // save last scene in List
					break;
				}
			}
		}
		for (int fromPage = start; fromPage >= firstPageNum; fromPage--) {
			scenes = sceneDAO.findByPageRangeAndScript(fromPage, end, script);
			if (scenes.size() > 0) {
				if (priorPageScene != null) {
					if (priorPageScene.getSequence() < scenes.get(0).getSequence()) {
						// Add prior page scene if it's earlier than the first one in list
						scenes.add(0, priorPageScene);
					}
				}
				break;
			}
		}
		if (log.isDebugEnabled()) {
			String str = "(none)";
			if (scenes.size() > 0) {
				str = scenes.get(0).getNumber() + "--" +
						scenes.get(scenes.size()-1).getNumber();
			}
			log.debug("page range: " + start + "--" + end
					+" == scene range: " + str);
		}
		lineList = null;	// force refresh
		return scenes;
	}

	/**
	 * Create a List of Scenes corresponding to all the scenes in the currently
	 * selected scene range. Note that 'fromScene' and 'toScene' are indexes
	 * into the drop-down list (SelectItems) presented to the user, which
	 * matches the 'allSceneIds' List. Therefore, we can pull the database id's
	 * of the required scenes directly from that List.
	 *
	 * @return the non-null List described above.
	 */
	private List<Scene> findScenesBySceneRange() {
		log.debug("");
		fromSceneSeq = 0;
		toSceneSeq = Integer.MAX_VALUE;
		startPage = firstPageNum;
		if (fromScene < 0) { // may happen if script has NO scenes
			return new ArrayList<>();
		}
		final SceneDAO sceneDAO = SceneDAO.getInstance();
		Scene first = null;
		Integer id = allSceneIds.get(fromScene);
		first = sceneDAO.findById(id);
		if (fromScene > 0) {
			if (first != null) {
				startPage = first.getPageNumber();
				fromSceneSeq = first.getSequence();
			}
		}
		if (first != null && first.getTextElements() != null && first.getTextElements().size() > 0) {
			script.setSceneText(true);
		}

		endPage = script.getLastPage();
		if (toScene < allSceneIds.size()-1) {
			// The last page to retrieve is the starting page of
			// the scene after the requested range
			id = allSceneIds.get(toScene+1);
			Scene last = sceneDAO.findById(id);
			if (last != null) {
				endPage = last.getPageNumber();
				// TODO can we determine if this scene is at top of page, and, if so, not include it?
				toSceneSeq = last.getSequence()-1;
			}
		}
		List<Scene> scenes;
		if (singleScene) { // Breakdown page - only display one scene
			scenes = new ArrayList<>();
			scenes.add(first);
		}
		else { // Revision page - extend data to fill start & end pages of scene range
			scenes = findScenesByPageRange(startPage, endPage);
		}
		return scenes;
	}

	/**
	 * Find the list of Scenes which covers the range of shooting days currently
	 * selected. The 'fromDay' and 'toDay' values are the actual shooting day
	 * numbers. We turn this range into a list of Strips, and then combine the
	 * Scenes corresponding to each Strip into the final list of Scenes.
	 *
	 * @return the non-null List described above.
	 */
	private List<Scene> findScenesByDayRange() {
		log.debug("");
		List<Scene> scenes = new ArrayList<>();
		Stripboard stripboard = SessionUtils.getCurrentProject().getStripboard();
		final StripDAO stripDAO = StripDAO.getInstance();
		final SceneDAO sceneDAO = SceneDAO.getInstance();
		for (int day = fromDay; day  <= toDay; day++) {
			List<Strip> strips = stripDAO.findByShootDay(stripboard, stripboard.getProject().getMainUnit(), day);
			for (Strip strip : strips) {
				for (String sceneNum : strip.getScenes()) {
					Scene scene = sceneDAO.findByScriptAndNumber(script, sceneNum);
					if (scene != null) {
						scenes.add(scene);
					}
				}
			}
		}
		lineList = null;
		return scenes;
	}

	/**
	 * This generates the data which is displayed in the script page area.
	 */
	private void createTextList() {
		lineList = null;
		try {
			if (script != null && script.hasPageData() && selectedGroup.equals(GROUP_PAGE)) {
				createTextListPages();
			}
			else if (sceneList == null || sceneList.size() == 0 || ! script.getSceneText()) {
				lineList = new ArrayList<>();
				//lineList.add(new ScriptLine(TextElementType.ACTION, "No scene data is available.", ""));
			}
			else {
				if (script.hasPageData() && selectedGroup.equals(GROUP_DAY)) {
					createTextListDays();
				}
				else if (script.getImportType().isFormatted()) {
					createTextListPDF();
				}
				else {
					createTextListOther();
				}
			}
			if (singleScene && lineList.size() == 1 &&
					lineList.get(0).getType() == TextElementType.BLANK) {
				lineList.clear();	// return empty instead of single blank line.
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			lineList = new ArrayList<>();
		}
	}

	/**
	 * Fills 'lineList' with the data to be displayed when the user has
	 * requested a display by shooting days, and we have 'pageData' (typical for
	 * PDF imports). Each scene in sceneList is separately formatted, so that
	 * it's page(s) appears intact, but with only the one scene in normal (not
	 * grayed-out) style.
	 */
	private void createTextListDays() {
		log.debug("");
		lineList = new ArrayList<>(sceneList.size()*40);
		int firstPage, lastPage = 1;
		for (Scene scene : sceneList) {
			// determine the pages to format for each scene
			firstPage = scene.getPageNumber();
			lastPage = firstPage;
			lastPage = ScriptUtils.findLastPage(scene);
			int sceneSeq = scene.getSequence();
			for (int page = firstPage; page <= lastPage; page++) {
				createTextListPage(page, sceneSeq, sceneSeq); // Get one page's worth of data
			}
		}
		insertEndOfOutput(10000, lastPage); // finish off bottom of display
	}

	/**
	 * Fills 'lineList' with the data to be displayed when the user has
	 * requested a display by page and the script has Page data (typical for PDF
	 * imports).
	 */
	private void createTextListPages() {
		log.debug("");
		lineList = new ArrayList<>((toPageNumber-fromPageNumber)*60);
		for (int page = fromPageNumber; page <= toPageNumber; page++) {
			createTextListPage(page, -1, -1); // Get one page's worth of data
		}
		insertEndOfOutput(10000, toPageNumber); // finish off bottom of display
	}

	/**
	 * Fills 'lineList' with the data to be displayed when the underlying Script
	 * was imported from a PDF, and either (a) we have no 'pageData' (should not
	 * happen) or (b) the user requested displaying by Scenes.
	 * <p>
	 * The List 'sceneList' determines the set of Scene's whose data will be
	 * formatted for display.
	 */
	private void createTextListPDF() {
		boolean foundChar = false;
		boolean output = false;
		boolean didPageHeading = singleScene; // ignoring headings for singlescene mode
		boolean requested = true;
		int pageNumber = 0;
		int line = 0;
		lineList = new ArrayList<>(sceneList.size()*40);

		if (selectedGroup.equals(GROUP_DAY) || singleScene) { // no page headings for these
			line = sceneList.get(0).getLineNumber();
			if (singleScene) {
				ScriptLine blank = new ScriptLine(TextElementType.BLANK, " ");
				applyCss(blank, true, pageNumber);
				lineList.add(blank);
			}
		}
		else if (startPage == firstPageNum) {
			insertPageBreak(firstPageNum, scriptPageNumbers.get(firstPageNum), 0);
			didPageHeading = true;
		}
		sceneLoop:
		for (Scene scene : sceneList) {
			pageNumber = scene.getPageNumber();
			if (! didPageHeading && scene.getTextElements().size() > 0 &&
					scene.getTextElements().get(0).getType() == TextElementType.PAGE_HEADING) {
				pageNumber--;	// it will get incremented back up when we process the page heading element
			}
			if (selectedGroup.equals(GROUP_SCENE)) {
				requested = (scene.getSequence() >= fromSceneSeq && scene.getSequence() <= toSceneSeq);
			}
			for (TextElement tx : scene.getTextElements()) {
				if (tx.getType() == TextElementType.PAGE_HEADING && ! singleScene) {
					if (! didPageHeading) {
						pageNumber++;
						insertPageBreak(pageNumber, scriptPageNumbers.get(pageNumber), line);
						didPageHeading = true;
					}
					line = 0;
					if (pageNumber <= endPage) {
						foundChar = applyCss(tx, requested, foundChar, pageNumber);
						line = insertTextPDF(tx, line, output, requested, pageNumber);
					}
					continue;
				}
				else {
					didPageHeading = singleScene;
				}
				if (pageNumber > endPage) {
					pageNumber--;	// "end output" needs current page number, not next one!
					break sceneLoop;
				}
				output = (pageNumber >= startPage);
				if (output) {
					foundChar = applyCss(tx, requested, foundChar, pageNumber);
					switch (tx.getType()) {
						case CHARACTER:
						case DIALOGUE:
						case START_ACT:
						case END_ACT:
						case PARENTHETICAL:
						case ACTION:
						case TRANSITION:
						case OTHER:
						case BLANK:
							if (showText) {
								line = insertTextPDF(tx, line, output, requested, pageNumber);
							}
							break;
						case MORE:	// MORE & Footer are ignored in "single scene" mode (breakdown page)
						case PAGE_FOOTER:
							if (showText && ! singleScene) {
								line = insertTextPDF(tx, line, output, requested, pageNumber);
							}
							break;
						case PAGE_HEADING:
							log.debug("new page, line=" + line);
							if (! singleScene) {
								insertPageBreak(pageNumber, scriptPageNumbers.get(pageNumber), line);
								line = insertTextPDF(tx, line, output, requested, pageNumber);
							}
							line = 0;
							didPageHeading = true;
							break;
						case SCENE_HEADING:
							String head = ScriptReporter.formatHeading(scene,ScriptReporter.HEADING_SCREEN);
							TextElement hdr = new TextElement(scene, TextElementType.SCENE_HEADING, 1, head);
							hdr.setLineNumber(tx.getLineNumber());
							hdr.setChanged(tx.getChanged());
							foundChar = applyCss(hdr, requested, foundChar, pageNumber);
							line = insertTextPDF(hdr, line, output, requested, pageNumber);
							if (!showText) {
								ScriptLine sl = new ScriptLine(TextElementType.ACTION, NOT_AVAILABLE);
								applyCss(sl, requested, pageNumber);
								lineList.add(sl);
							}
							break;
						case CONTINUATION:
						default:
							break;
					}
				}
			}
		}
		insertEndOfOutput(line, pageNumber); // insert final padding/end of output
	}

	/**
	 * Fills 'lineList' with a List of TextElement's that corresponds to a
	 * single page of the script, specified by pageNumber. The fromScnSeq and
	 * toScnSeq parameters can, optionally, control the display of text in
	 * requested versus non-requested styles (typically grayed-out text for
	 * non-selected portions).
	 * <p>
	 * This method is used if we have 'pageData' (typical for PDF imports) and
	 * we are displaying by either page or shooting days.
	 * <p>
	 * Upon return, 'lineList' contains the List of TextElement's constituting
	 * the page to be displayed. May be empty, but never null.
	 *
	 * @param pageNumber The physical page number for which the TextElement data
	 *            should be created.
	 * @param fromScnSeq The lowest value of Scene.sequence which will be
	 *            considered "requested" output. Ignored if toScnSeq is
	 *            negative.
	 * @param toScnSeq The highest value of Scene.sequence which will be
	 *            considered "requested" output. If this is negative, the entire
	 *            page is considered "requested".
	 */
	private void createTextListPage(int pageNumber, int fromScnSeq, int toScnSeq) {
		log.debug("page=" + pageNumber + ", fromSeq=" + fromScnSeq + ", toSeq=" + toScnSeq);
		boolean foundChar = false;
		boolean output = true;
		boolean requested = true;
		boolean didPageHeading = false;
		int line = 0;
		Page page = script.getPages().get(pageNumber-1);
		pageNumber = page.getNumber();
		for (TextElement tx : page.getTextElements()) {
			Scene scene = tx.getScene();
			if (toScnSeq >= 0) {
				if (scene == null) { // shouldn't happen with to/from scnSeq values.
					// scene should only be null for "title" pages
					requested = false;
				}
				else {
					requested = (scene.getSequence() >= fromScnSeq && scene.getSequence() <= toScnSeq);
				}
			}
			if (! didPageHeading) {
				log.debug("new page, line=" + line);
				insertPageBreak(pageNumber, scriptPageNumbers.get(pageNumber), line);
				line = 0;
				didPageHeading = true;
			}
			foundChar = applyCss(tx, requested, foundChar, pageNumber);
			switch (tx.getType()) {
				case CHARACTER:
				case DIALOGUE:
				case START_ACT:
				case END_ACT:
				case PARENTHETICAL:
				case ACTION:
				case TRANSITION:
				case OTHER:
				case BLANK:
				case MORE:
					if (showText) {
						line = insertTextPDF(tx, line, output, requested, pageNumber);
					}
					break;
				case PAGE_HEADING:
				case PAGE_FOOTER:
					line = insertTextPDF(tx, line, output, requested, pageNumber);
					break;
				case SCENE_HEADING:
					String head = ScriptReporter.formatHeading(scene,ScriptReporter.HEADING_SCREEN);
					TextElement hdr = new TextElement(scene, TextElementType.SCENE_HEADING, 1, head);
					hdr.setLineNumber(tx.getLineNumber());
					hdr.setChanged(tx.getChanged());
					foundChar = applyCss(hdr, requested, foundChar, pageNumber);
					line = insertTextPDF(hdr, line, output, requested, pageNumber);
					if (!showText) {
						ScriptLine sl = new ScriptLine(TextElementType.ACTION, NOT_AVAILABLE);
						applyCss(sl, requested, pageNumber);
						lineList.add(sl);
					}
					break;
				case CONTINUATION:
				default:
					break;
			}
		}
		insertEndOfPage(line, pageNumber); // pad out the page if necessary
		return;
	}

	/**
	 * Create the output lines corresponding to the given TextElement. The new
	 * lines will be added to the 'lineList' field.
	 *
	 * @param tx The element whose text is to be added to the display array.
	 * @param line The line number (within a page) of the next line to be
	 *            output.
	 * @param output If we are actually generating output for this TextElement.
	 * @param requested If this TextElement is included in the range requested
	 *            by the user; this will be false, for example, when a scene or
	 *            day range has been specified, and we are outputing
	 *            non-requested data that's on the same page as the requested
	 *            data.
	 * @param pageNumber The current physical page number being output.
	 * @return The new line number value.
	 */
	private int insertTextPDF(TextElement tx, int line, boolean output, boolean requested, int pageNumber) {
		if (output) {
			if (line < tx.getLineNumber()) {
				ScriptLine blank = new ScriptLine(TextElementType.BLANK, " ");
				applyCss(blank, requested, pageNumber);
				while (line < tx.getLineNumber()) {
					lineList.add(blank);
					line++;
					if (singleScene) {
						break; // never add more than 1 blank line in single scene mode.
					}
				}
			}
			ScriptLine sl = new ScriptLine(tx);
			lineList.add(sl);
			line++;
			String s = sl.getText();
			int i = s.indexOf(Constants.SCRIPT_NEW_LINE_CHAR);
			if (tx.getChanged()) {
				if (i < 0) {
					s = addChangeMarker(s, tx.getType());
					if (tx.getType() == TextElementType.BLANK) {
						sl.setType(TextElementType.OTHER); // JSP ignores data in BLANK types! (rev 2649)
					}
				}
				else {
					String lines[] = s.split(Constants.SCRIPT_NEW_LINE);
					s = "";
					boolean addNewline = false;
					for (String str : lines) {
						str = addChangeMarker(str, tx.getType());
						if (addNewline) {
							s += Constants.SCRIPT_NEW_LINE_CHAR;
						}
						s += str;
						addNewline = true;
					}
				}
				i = s.indexOf(Constants.SCRIPT_NEW_LINE_CHAR);
			}
			if ((tx.getType() == TextElementType.PAGE_HEADING ||
					tx.getType() == TextElementType.OTHER ||
					tx.getType() == TextElementType.PAGE_FOOTER) &&
					s.length() > Constants.SCRIPT_FMT_MAX_HEADER_LEN) {
				// prevent page heading/footer (with saved indentation) from going outside display area
				while(s.charAt(0) == ' ' &&
						s.length() > Constants.SCRIPT_FMT_MAX_HEADER_LEN) {
					s = s.substring(1);
				}
			}
			while(i >= 0 ) {
				line++;
				i = s.indexOf(Constants.SCRIPT_NEW_LINE_CHAR, i+1);
			}
			sl.setText(StringUtils.saveHtml(s));
		}
		return line;
	}

	/**
	 * Add our "change marker" -- an asterisk -- to the end of the string
	 * buffer, after adding enough blank padding to position the "*" at the
	 * right margin on the screen display.
	 *
	 * @param buff The current output text.
	 * @param type The TextElementType of the line being formatted; this is used
	 *            to determine the left margin of the text, and thereby compute
	 *            where the text will end, and how many blanks must be added to
	 *            reach the right margin.
	 * @return The updated output text containg an asterisk positioned at the
	 *         right margin.
	 */
	private static String addChangeMarker(String buff, TextElementType type) {
		int len = buff.length();
		int margin = Constants.SCRIPT_FMT_CHANGE_MARGIN - type.getLeftMargin() - 9;
		if (len < margin) {
			buff += BLANK_LINE.substring(0, margin-len);
		}
		buff += '*';
		return buff;
	}

	/**
	 * Fills 'lineList' with the data to be displayed when the underlying Script
	 * does NOT have 'pageData' and the display requested is by either page or
	 * shooting day. (No pageData is normal for Tagger, FDX, or .sex imports;
	 * PDF imports normally have pageData.)
	 */
	private void createTextListOther() {
		boolean foundChar = false;
		boolean hasLineNumbers = false;
		boolean output = false;
		boolean requested = true;
		int pageNumber = 0;
		int line = 0;
		lineList = new ArrayList<>(sceneList.size()*40);
		ScriptLine blank;
		int linesPerPage = Constants.SCRIPT_MAX_LINES_PER_PAGE;
		if (script.getLinesPerPage() > 40 && script.getLinesPerPage() < linesPerPage) {
			linesPerPage = script.getLinesPerPage();
		}
		if (startPage == firstPageNum) {
			if (getShowPageBreaks()) {
				insertPageBreak(firstPageNum, scriptPageNumbers.get(firstPageNum), line);
			}
			pageNumber = firstPageNum;
			line = 1;
		}
		sceneLoop:
		for (Scene scene : sceneList) {
			if (scene.getPageNumber() > pageNumber  && (! selectedGroup.equals(GROUP_DAY))) {
				pageNumber = scene.getPageNumber();
				if (getShowPageBreaks()) {
					insertPageBreak(pageNumber, scriptPageNumbers.get(pageNumber), line);
				}
				line = 1;
			}
			if (selectedGroup.equals(GROUP_SCENE)) {
				requested = (scene.getSequence() >= fromSceneSeq && scene.getSequence() <= toSceneSeq);
			}
			for (TextElement tx : scene.getTextElements()) {
				line++;
				if (tx.getLineNumber() > line) {
					hasLineNumbers = true;
					line = tx.getLineNumber();
				}
				if (! hasLineNumbers &&
						(line > linesPerPage ||
							(line >= linesPerPage-1 && tx.getType()==TextElementType.CHARACTER) ||
							(line >= linesPerPage-2 && tx.getType()==TextElementType.SCENE_HEADING) )) {
					pageNumber++;
					if (pageNumber <= endPage) {
						if (getShowPageBreaks()) {
							insertPageBreak(pageNumber, scriptPageNumbers.get(pageNumber), line);
						}
						line = 1;
					}
				}
				if (pageNumber > endPage) {
					pageNumber--; // end-output code needs real last page, not next page number.
					break sceneLoop;
				}
				output = (pageNumber >= startPage);
				foundChar = applyCss(tx, requested, foundChar, pageNumber);
				switch (tx.getType()) {
					case ACTION:
						blank = new ScriptLine(TextElementType.BLANK, " ");
						applyCss(blank, requested, pageNumber);
						if (output && showText) {
							lineList.add(blank);
							line++;
						}
						line = insertText(tx, Constants.SCRIPT_FMT_ACTION_WIDTH, line, output);
						break;
					case CHARACTER:
						blank = new ScriptLine(TextElementType.BLANK, " ");
						applyCss(blank, requested, pageNumber);
						if (output && showText) {
							lineList.add(blank);
							line++;
						}
						line = insertText(tx, Constants.SCRIPT_FMT_CHARACTER_WIDTH, line, output);
						break;
					case DIALOGUE:
						if (tx.getText().length() > 0) {
							line = insertText(tx, Constants.SCRIPT_FMT_DIALOGUE_WIDTH, line, output);
						}
						break;
					case TRANSITION:
					case OTHER:
					case BLANK:
						line = insertText(tx, Constants.SCRIPT_FMT_ACTION_WIDTH, line, output);
						break;
					case PARENTHETICAL:
						line = insertText(tx, Constants.SCRIPT_FMT_PAREN_WIDTH, line, output);
						break;
					case START_ACT:
					case END_ACT:
						line = insertText(tx, Constants.SCRIPT_FMT_DIALOGUE_WIDTH, line, output);
						break;
					case SCENE_HEADING:
						blank = new ScriptLine(TextElementType.BLANK, " ");
						applyCss(blank, requested, pageNumber);
						if (output) {
							lineList.add(blank);
							line++;
						}
						String head = ScriptReporter.formatHeading(scene,ScriptReporter.HEADING_SCREEN);
						TextElement hdr = new TextElement(scene, TextElementType.SCENE_HEADING, 1, head);
						foundChar = applyCss(hdr, requested, foundChar, pageNumber);
						if (output) {
							lineList.add(new ScriptLine(hdr));
							line++;
						}
						if (!showText) {
							ScriptLine sl = new ScriptLine(TextElementType.ACTION, NOT_AVAILABLE);
							applyCss(sl, requested, pageNumber);
							lineList.add(sl);
							line++;
						}
					case CONTINUATION: // not in XML files
					case MORE:		   // not in XML files
					case PAGE_HEADING: // not in XML files
					case PAGE_FOOTER:  // not in XML files
					default:
						break;
				}
			}
		}
		insertEndOfOutput(line, pageNumber); // insert final padding/end of output
	}

	private int insertText(TextElement tx, int width, int line, boolean output) {
		if (output && showText) {
			lineList.add(new ScriptLine(tx));
		}
		line += (tx.getText().length() / width);
		return line;
	}

	/**
	 * Adds TextElement's necessary to start a page, and, if necessary, to end the
	 * preceding page.
	 * @param pageNumber The physical number of the page to be started.
	 * @param pageNumStr The logical page number of the new page.
	 * @param line The current line number on the page; used to determine whether, and
	 * how many, lines to add to the end of the preceding page.
	 */
	private void insertPageBreak(int pageNumber, String pageNumStr, int line) {
		if (getShowPageBreaks() && pageNumber >= startPage && pageNumber <= endPage) {
			if (pageNumber > startPage && line > 1 ) {
				insertEndOfPage(line, pageNumber-1); // pad out the page if necessary
			}
			String str = MsgUtils.formatMessage("Script.PageHeader", pageNumStr);
			ScriptLine pageBreak = new ScriptLine(TextElementType.PAGE_TOP, str);
			applyCss(pageBreak, true, pageNumber);
			lineList.add(pageBreak);
		}
	}

	/**
	 * Insert blank lines to fill out the last page, and a final item with special
	 * CSS class to allow formatting the end of the last page.
	 * @param line
	 * @param pageNumber
	 */
	private void insertEndOfOutput(int line, int pageNumber) {
		insertEndOfPage(line, pageNumber); // pad out the page if necessary
		// Insert final output element
		if (! singleScene) {
			lineList.add(new ScriptLine(TextElementType.BLANK, " ", CSS_CLASS_FINAL));
		}
	}

	/**
	 * Insert blank lines to fill out the last page, and a final item with special
	 * CSS class to allow formatting the end of the last page.
	 * @param line
	 * @param pageNumber
	 */
	private void insertEndOfPage(int line, int pageNumber) {
		// insert final padding/end of output
		if (line > 1 && line < pageLength) {
			ScriptLine blank = new ScriptLine(TextElementType.BLANK, " ");
			applyCss(blank, true, pageNumber);
			while (line < pageLength) {
				lineList.add(blank);
				line++;
				if (singleScene) {
					// only add a single blank trailing line in single-scene mode
					break;
				}
			}
		}
	}

	/**
	 * Determine the appropriate CSS class for the given ScriptLine, based on
	 * the element type, and whether it is within the requested range of script
	 * data.  This is a simplified version of the applyCss method for TextElements,
	 * as it is only called for blank lines, page headings, and "text not
	 * available" message lines.
	 *
	 * @param text The ScriptLine whose CSS class is to be set.
	 * @param requested True if the ScriptLine was within the user's requested
	 *            range (e.g., page or scene range).
	 * @param pageNum The page number this text resides on, which is used to set
	 *            the background color for the TextElement.
	 */
	private void applyCss(ScriptLine text, boolean requested, int pageNum) {
		String cssClass = (requested ? CSS_CLASS_PREFIX : CSS_CLASS_PREFIX_NOT);
		cssClass += text.getType().name();
		cssClass += " " + CSS_COLOR_PREFIX;
		if (singleScene) {
			cssClass += ColorNameDAO.WHITE.getScriptRevision();
		}
		else {
			cssClass += pageColors.get(pageNum);
		}
		text.setCssClass(cssClass);
	}

	/** Same function as above, but for TextElement's instead of ScriptLine's. */
	private boolean applyCss(TextElement text, boolean requested, boolean foundChar, int pageNum) {
		String cssClass = (requested ? CSS_CLASS_PREFIX : CSS_CLASS_PREFIX_NOT);
		cssClass += text.getType().name();
		int pageRevColor = pageColors.get(0);
		if (singleScene) {
			pageRevColor = ColorNameDAO.WHITE.getScriptRevision();
		}
		else if (pageNum < pageColors.size()) {
			pageRevColor = pageColors.get(pageNum);
		}
		else {
			log.warn("page number exceeds revColor array size. page#=" + pageNum);
		}
		if (requested && highlight && (characterNames != null)) {
			if (text.getType() == TextElementType.CHARACTER) {
				String name = ScriptUtils.extractCharacterName(text.getText(), null);
				if (characterNames.contains(name)) {
					foundChar = true;
				}
				else {
					foundChar = false;
				}
			}
			else if (foundChar && (text.getType() == TextElementType.DIALOGUE)) {
				if (pageRevColor == 4 || pageRevColor == 14) {
					text.setHighLight(2);
				}
				else {
					text.setHighLight(1);
				}
			}
		}
		if (text.getChanged()) {
			cssClass += " pre"; // keeps the 'change markers' aligned on the right
		}
		text.setCssClass(cssClass + " " + CSS_COLOR_PREFIX + pageRevColor);
		return foundChar;
	}

	private void initDefaultOptions() {
		if (printStyle == null) {
			printStyle = PRINT_STYLE_FULL;
			watermark = (SessionUtils.getProduction().getWatermark() != WatermarkPreference.FORBIDDEN);
		}
		if (printSelection == null) {
			printSelection = PRINT_CURRENT;
		}
		if (colorStyle < 0) {
			colorStyle = PRINT_COLOR_ALL;
		}
		if (watermarkDate == null) {
			watermarkDate = PRINT_MARK_DATE;
		}
	}

	private void saveSettings() {
		SessionUtils.put(ATTR_SP_GROUP, selectedGroup);
		SessionUtils.put(ATTR_SP_FROM_PAGE, fromPageNumber);
		SessionUtils.put(ATTR_SP_TO_PAGE, toPageNumber);
		SessionUtils.put(ATTR_SP_FROM_DAY, fromDay);
		SessionUtils.put(ATTR_SP_TO_DAY, toDay);
		SessionUtils.put(ATTR_SP_FROM_SCENE, fromScene);
		SessionUtils.put(ATTR_SP_TO_SCENE, toScene);
		SessionUtils.put(ATTR_SP_HIGHLIGHT, (highlight?1:0));
	}

	public Integer getToPageNumber() {
		return toPageNumber;
	}
	public void setToPageNumber(Integer pagenum) {
		//log.debug("old=" + toPageNumber + ", new=" + pagenum + ", flag=" + override);
		if (! override && pagenum != null) {
			toPageNumber = pagenum;
		}
	}

	public Integer getFromPageNumber() {
		return fromPageNumber;
	}
	public void setFromPageNumber(Integer fromPage) {
		//log.debug(fromPage);
		if (! override && fromPage != null) {
			fromPageNumber = fromPage;
		}
	}

	public Integer getFromScene() {
		return fromScene;
	}
	public void setFromScene(Integer fromSceneNumber) {
		//log.debug(fromSceneNumber);
		if (! override && fromSceneNumber != null) {
			fromScene = fromSceneNumber;
		}
	}

	/** See {@link #toScene}. */
	public Integer getToScene() {
		return toScene;
	}
	/** See {@link #toScene}. */
	public void setToScene(Integer toSceneNumber) {
		//log.debug(toSceneNumber);
		if (! override && toSceneNumber != null) {
			toScene = toSceneNumber;
		}
	}

	/** See {@link #fromDay}. */
	public Integer getFromDay() {
		return fromDay;
	}
	/** See {@link #fromDay}. */
	public void setFromDay(Integer fromDay) {
		//log.debug(fromDay);
		if (! override && fromDay != null) {
			this.fromDay = fromDay;
		}
	}

	/** See {@link #toDay}. */
	public Integer getToDay() {
		return toDay;
	}
	/** See {@link #toDay}. */
	public void setToDay(Integer toDay) {
		//log.debug(toDay);
		if (! override && toDay != null) {
			this.toDay = toDay;
		}
	}

	public String getSelectedGroup() {
		return selectedGroup;
	}
	public void setSelectedGroup(String selectedGroup) {
		//log.debug(selectedGroup);
		if ( ! overrideGroup) {
			this.selectedGroup = selectedGroup;
		}
		overrideGroup = false;
	}

	/** Returns true if we are displaying page breaks in the script window */
	private boolean getShowPageBreaks() {
		// We don't show page breaks in single-scene mode (breakdown page).
		return ! singleScene; // ! selectedGroup.equals(GROUP_DAY);
	}

	public List<SelectItem> getSelectItems() {
		return selectItems;
	}
	public void setSelectItems(List<SelectItem> selectItems) {
		this.selectItems = selectItems;
	}

	public List<SelectItem> getPageNumbers() {
		return pageNumbers;
	}
	public void setPageNumbers(List<SelectItem> pageNumbers) {
		this.pageNumbers = pageNumbers;
	}

	public List<SelectItem> getSceneNumberList() {
		return sceneNumberList;
	}
	public void setSceneNumberList(List<SelectItem> sceneNumber) {
		sceneNumberList = sceneNumber;
	}

	/** See {@link #dayNumbers}. */
	public List<SelectItem> getDayNumbers() {
		return dayNumbers;
	}
	/** See {@link #dayNumbers}. */
	public void setDayNumbers(List<SelectItem> dayNumbers) {
		this.dayNumbers = dayNumbers;
	}

	/** See {@link #characterNames}. */
	public List<String> getCharacterNames() {
		return characterNames;
	}

	public List<Scene> getSceneList() {
		return sceneList;
	}
	public void setSceneList(List<Scene> sceneList) {
		this.sceneList = sceneList;
	}

	/** See {@link #sceneTable}. */
//	public HtmlDataTable getSceneTable() {
//		return sceneTable;
//	}
//	/** See {@link #sceneTable}. */
//	public void setSceneTable(HtmlDataTable sceneTable) {
//		this.sceneTable = sceneTable;
//	}

	/** See {@link #lineList}. */
	public List<ScriptLine> getLineList() {
		if (lineList == null) {
			createTextList();
		}
		return lineList;
	}
	/** See {@link #lineList}. */
	public void setLineList(List<ScriptLine> lineList) {
		this.lineList = lineList;
	}

	/** See {@link #showDays}. */
	public boolean getShowDays() {
		return showDays;
	}
	/** See {@link #showDays}. */
	public void setShowDays(boolean showDays) {
		this.showDays = showDays;
	}

	/** See {@link #showText}. */
	public boolean getShowText() {
		return showText;
	}
	/** See {@link #showText}. */
	public void setShowText(boolean b) {
		showText = b;
	}

	/** See {@link #highlight}. */
	public boolean getHighlight() {
		return highlight;
	}
	/** See {@link #highlight}. */
	public void setHighlight(boolean highlight) {
		this.highlight = highlight;
	}

	/** See {@link #showPrintOptions}. */
	public boolean getShowPrintOptions() {
		return showPrintOptions;
	}
	/** See {@link #showPrintOptions}. */
	public void setShowPrintOptions(boolean showPrintOptions) {
		this.showPrintOptions = showPrintOptions;
	}

	/** See {@link #doPrint}. */
	public boolean getDoPrint() {
		return doPrint;
	}
	/** See {@link #doPrint}. */
	public void setDoPrint(boolean doPrint) {
		this.doPrint = doPrint;
	}

	/** See {@link #printTitle}. */
	public String getPrintTitle() {
		return printTitle;
	}
	/** See {@link #printTitle}. */
	public void setPrintTitle(String printTitle) {
		this.printTitle = printTitle;
	}

	/** See {@link #printButtonOkLabel}. */
	public String getPrintButtonOkLabel() {
		return printButtonOkLabel;
	}
	/** See {@link #printButtonOkLabel}. */
	public void setPrintButtonOkLabel(String printButtonOkLabel) {
		this.printButtonOkLabel = printButtonOkLabel;
	}

	/** See {@link #printStyle}. */
	public String getPrintStyle() {
		return printStyle;
	}
	/** See {@link #printStyle}. */
	public void setPrintStyle(String printStyle) {
		this.printStyle = printStyle;
	}

	/** See {@link #sidesType}. */
	public int getSidesType() {
		return sidesType;
	}

	/** See {@link #sidesType}. */
	public void setSidesType(int sidesType) {
		this.sidesType = sidesType;
	}

	/** See {@link #colorStyle}. */
	public int getColorStyle() {
		return colorStyle;
	}
	/** See {@link #colorStyle}. */
	public void setColorStyle(int colorStyle) {
		this.colorStyle = colorStyle;
	}

	/** See {@link #colorStyleDL}. */
	public List<SelectItem> getColorStyleDL() {
		return colorStyleDL;
	}
	/** See {@link #colorStyleDL}. */
	public void setColorStyleDL(List<SelectItem> colorStyleDL) {
		this.colorStyleDL = colorStyleDL;
	}

	/** See {@link #printRevision}. */
	public int getPrintRevision() {
		return printRevision;
	}
	/** See {@link #printRevision}. */
	public void setPrintRevision(int printRevision) {
		this.printRevision = printRevision;
	}

	/** See {@link #revisionDL}. */
	public List<SelectItem> getRevisionDL() {
		if (revisionDL == null) {
			return new ArrayList<>();
		}
		return revisionDL;
	}
	/** See {@link #revisionDL}. */
	public void setRevisionDL(List<SelectItem> revisionDL) {
		this.revisionDL = revisionDL;
	}

	/** See {@link #watermarkDate}. */
	public String getWatermarkDate() {
		return watermarkDate;
	}
	/** See {@link #watermarkDate}. */
	public void setWatermarkDate(String watermarkDate) {
		this.watermarkDate = watermarkDate;
	}

	/** See {@link #watermark}. */
	public boolean getWatermark() {
		return watermark;
	}
	/** See {@link #watermark}. */
	public void setWatermark(boolean watermark) {
		if (!styleChanged) {
		this.watermark = watermark;
		}
		styleChanged = false;
	}

	/** See {@link #printHighlight}. */
	public boolean getPrintHighlight() {
		return printHighlight;
	}
	/** See {@link #printHighlight}. */
	public void setPrintHighlight(boolean printHighlight) {
		this.printHighlight = printHighlight;
	}

	/** See {@link #includeTitlePages}. */
	public boolean getIncludeTitlePages() {
		return includeTitlePages;
	}
	/** See {@link #includeTitlePages}. */
	public void setIncludeTitlePages(boolean printTitlePages) {
		includeTitlePages = printTitlePages;
	}

	/** See {@link #printSelection}. */
	public String getPrintSelection() {
		return printSelection;
	}
	/** See {@link #printSelection}. */
	public void setPrintSelection(String printSelection) {
		this.printSelection = printSelection;
	}

	/** See {@link #scenesToPrint}. */
	public String getScenesToPrint() {
		return scenesToPrint;
	}
	/** See {@link #scenesToPrint}. */
	public void setScenesToPrint(String scenesToPrint) {
		this.scenesToPrint = scenesToPrint;
	}

	/** See {@link #printRangeCurrent}. */
	public boolean getPrintRangeCurrent() {
		return printRangeCurrent;
	}
	/** See {@link #printRangeCurrent}. */
	public void setPrintRangeCurrent(boolean printRangeCurrent) {
		this.printRangeCurrent = printRangeCurrent;
	}

	/** See {@link #printRangeAll}. */
	public boolean getPrintRangeAll() {
		return printRangeAll;
	}
	/** See {@link #printRangeAll}. */
	public void setPrintRangeAll(boolean printRangeAll) {
		this.printRangeAll = printRangeAll;
	}

	/** See {@link #printRangeRevision}. */
	public boolean getPrintRangeRevision() {
		return printRangeRevision;
	}
	/** See {@link #printRangeRevision}. */
	public void setPrintRangeRevision(boolean printRangeRevision) {
		this.printRangeRevision = printRangeRevision;
	}

	/** See {@link #printRangeScenes}. */
	public boolean getPrintRangeScenes() {
		return printRangeScenes;
	}
	/** See {@link #printRangeScenes}. */
	public void setPrintRangeScenes(boolean printRangeScenes) {
		this.printRangeScenes = printRangeScenes;
	}

}
