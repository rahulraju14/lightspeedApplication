//	File Name:	ImportFile.java
package com.lightspeedeps.importer;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
//import com.lightspeedeps.offline.Import;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.script.ScriptUtils;

/**
 * A superclass for all script import classes.  Much of the common functionality
 * for all imports is contained in this class; the subclasses call the appropriate
 * methods as necessary.
 * Each subclass must implement the abstract method doImport().  This is the
 * main entry point into the subclass to perform the import process.
 * See ImportTagger for Final Draft Tagger import; see ImportSex for generic
 * .sex file import.
 */
public abstract class ImportFileImpl implements ImportFile {
	private static final Log log = LogFactory.getLog(ImportFileImpl.class);

	//@PersistenceContext
	//private EntityManager entityManager;

	protected ColorNameDAO colorNameDAO = ColorNameDAO.getInstance();
	protected ProjectDAO projectDAO = ProjectDAO.getInstance();
	protected ProjectMemberDAO projectMemberDAO = ProjectMemberDAO.getInstance();
	protected SceneDAO sceneDAO = SceneDAO.getInstance();
	protected ScriptDAO scriptDAO = ScriptDAO.getInstance();
	protected ScriptElementDAO scriptElementDAO = ScriptElementDAO.getInstance();

	protected SessionFactory sessionFactory = null;
	protected Session batchSession = null;
	protected Transaction transaction = null;

	protected Project project = null;
	protected Contact locationManager = null;
	protected Script script = null;
	protected Script priorScript = null;

	// Saved input parameters
	protected String description = null;
	protected String file = null;
	protected String messageLog = "";
	protected boolean includeSceneElements = true;
	protected boolean includeText = true;
	protected ColorName colorName = null;

	// Processing counters, etc.
	protected int newScriptElements = 0;
	protected int newLocations = 0;
	protected int oldScriptElements = 0;
	protected int scenesAdded = 0;
	protected int noIntExt = 0;
	protected int noDayNight = 0;
	protected int breakdownNumber = 0;
	protected int pageCount;

	/** A Set of all the Scene Numbers found in the script, used to check
	 * for duplicate scene numbers. */
	protected Set<String> sceneNumbers;

	private Set<Integer> matchedElements;

	/** The database ids of any ScriptElements created during the current import. */
	private List<Integer> addedElements;

	/** This is a map of character names to matching ScriptElement objects; it is
	 * used for quick lookup of Characters, and for tracking line count when
	 * dialogue paragraphs are processed. */
	protected Map<String, ScriptElement> characterMap = new HashMap<String, ScriptElement>();

	protected Integer sceneSequence = new Integer(0);
	protected int textElementSequence = 0;

	/** Used to hold TextElement preceding the first scene header. */
	protected List<TextElement> preSceneText;

	protected boolean batchMode = false; // batch mode does NOT work!

	/**
	 * Pattern #1 used for parsing incoming scene headings:
	 * Has hyphen with either leading or following blank;
	 *     1st match group is first word; next-to-last match group (#9) follows hyphen,
	 *     and is expected to be the D/N indicator; excludes optional trailing period.
	 */
	protected static final Pattern pFullHeading = Pattern.compile("(((.+?) (.+))|(.+))((- )|( -))(.+?)(\\.)?");

	/**
	 * Pattern #2 used for parsing incoming scene headings:
	 * Has at least one embedded blank;
	 *     1st match group is first word: hope for INT/EXT indicator.
	 */
	protected static final Pattern pShortHeading = Pattern.compile("(.+?) (.+)");

	/**
	 * Pattern #3 used for parsing incoming scene headings:
	 * Has at least one embedded blank;
	 *     1st match group is all but last word, 2nd group is last word;
	 *     we try to match last word to a D/N value.
	 * Note that 2nd group excludes any trailing period.
	 */
	protected static final Pattern pLastWord = Pattern.compile("(.+) (.+?)(\\.)?");

	/** Pattern to parse scene number - get leading/trailing alphas */
	protected static final Pattern pSceneNumber = Pattern.compile("([a-zA-Z]*)([0-9]+)([a-zA-Z]*)");

	/** Regular expression for "Omitted" scene heading. */
	private static final String RE_OMITTED_SCENE = "(SCENE )?(OMITTED)";	// "omitted" tag

	/** Words or phrases that occur in scene header which indicate scene is the same D/N as prior scene.
	 * When these are recognized, the "missing D/N notation" message is not issued.
	 * These phrases are also included in the "NOTATIONS" list. */
	private final static String CONTINUED =
			"?SAME TIME?CONTINUED?CONTINUOUS?CONT?MOMENTS LATER?SECONDS LATER?MINUTES LATER?";

	/** Words or phrases that often occur in scene header which should NOT be taken as part of Set name.
	 * They are recognized if delimited from the rest of the Set name by a hyphen, or if they
	 * appear as the last part of a Set name. */
	private static String NOTATIONS = ""; // will be created from NOTATION_LIST
	private final static String[] NOTATION_LIST = {
			"SAME TIME",
			"CONTINUED",
			"CONTINUOUS",
			"A MOMENT LATER",
			"THAT MOMENT",
			"MOMENTS LATER",
			"A LITTLE LATER",
			"SOMETIME LATER",
			"MINUTES LATER",
			"HOURS LATER",
			"DAYS LATER",
			"MONTHS LATER",
			"YEARS LATER",
			"LATER",
			"INTERCUT",
			"INTERCUTTING"
		};
	{
		for (String s : NOTATION_LIST) {
			NOTATIONS += '.' + s + '.';
		}
	}

	/**
	 * The main import process, implemented by each subclass.
	 * Returns true if the import was successful; false if it failed.
	 */
	protected abstract boolean doImport();

	public ImportFileImpl() {
		log.debug("");
	}

	/**
	 * Batch entry point to import a script file.
	 */
	@Transactional
	public boolean batchImportFile(String filename,
			boolean includeText, boolean includeSceneElements) {
		log.debug ("ImportTagger.batchImportFile");
		//String desc = "batch import";
		//File file = new File(filename);
		batchMode = true;
		log.error("BATCH MODE IS NOT CURRENTLY SUPPORTED (broken during migration to Spring architecture)");
		return false;
		//return importFile(file, desc, includeText, includeSceneElements);
	}

	/**
	 * This is the "normal" importFile method, called from the web view bean
	 * ImportScript.  It specifies the file to be imported, a description to
	 * be assigned to the new script, which project the script belongs to,
	 * and flags for whether or not to include the script text (dialogue)
	 * and Script Elements.
	 *
	 * Returns true if the import was successful, false if not.
	 */
	@Transactional
	@Override
	public boolean importFile(String file,
			String desc, Project pProject, ColorName color, boolean includeText, boolean includeSceneElements) {
		log.debug("");
		project = pProject;
		return importFile(file, desc, color, includeText, includeSceneElements);
	}

	/**
	 * Import the specified file, assigning the script the given description,
	 * and using the given processing options.
	 * All the public "importFile" methods eventually get here.  The instance
	 * variable 'project' must already have been set.
	 * @return True iff the import was successful.
	 */
	private boolean importFile(String pfile,
			String pdesc, ColorName pcolor, boolean pincludeText, boolean pincludeSceneElements) {

		description = pdesc;
		file = pfile;
		includeText = pincludeText;
		includeSceneElements = pincludeSceneElements;

		init();
		colorName = pcolor;

		setProgress(1);

		//Import.setSQLiteSyncOff();
		boolean bRet = false;

		try {
			bRet = doImport(); // Does most of the processing - implemented by subclasses

			if (batchSession != null) {
				batchSession.close();
				batchSession = null;
			}
			log.debug("IMPORT FINISHED, return=" + bRet);
			if (bRet && getScenesAdded() == 0) {
				bRet = false;
				userErrorMessage("ImportScript.NoScenesFound");
			}

			if (! bRet && script != null) {
				try {
					/* Because the script could be in a "bad" state due to an exception during
					 * the load process, we need to get a clean copy of it from the database
					 * before we delete it -- otherwise the delete can fail due to the complex
					 * relationships between the Script, Page, Scene, and TextElement objects.
					 * To force the clean retrieval, we evict the current instance and get
					 * a fresh one.
					 */
					scriptDAO.evict(script);
					script = scriptDAO.refresh(script);
					scriptDAO.delete(script);
				}
				catch (Exception e) {
					EventUtils.logError(e);
				}
				script = null;
				if (project.getScript() == null) { // no current script
					try {
						scriptElementDAO.deleteAll(project); // remove all Script Elements
					}
					catch (Exception e) {
						log.error(e);
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		finally {
			//Import.setSQLiteSyncOn();
		}


		if (script != null) {
			SessionUtils.put(Constants.ATTR_IMPORT_SCRIPT_ID, script.getId());
		}

		if (noIntExt > 0) {
			userInfoMessage("ImportFile.MissingIEvalues", noIntExt);
		}
		if (noDayNight > 0) {
			userInfoMessage("ImportFile.MissingDNvalues", noDayNight);
		}

		//userInfoMessage("ImportFile.SceneCount", scenesAdded);
		if (script != null) {
			pageCount = script.getLastPage();
			//userInfoMessage("ImportFile.PageCount", pageCount);
		}
		//userInfoMessage("ImportFile.NewLocations", newLocations);
		//userInfoMessage("ImportFile.NewElements", newScriptElements);
		if (oldScriptElements != 0) {
			userInfoMessage("ImportFile.ElementsFound", oldScriptElements);
		}

		release(); // release most of our objects

		return bRet;
	}

	/**
	 * Create a Hibernate session if needed for
	 * the batch import process.
	 */
	protected boolean setup() {
		if (batchMode) {
			/*
			if (sessionFactory == null) { // running in batch
				log.debug("get sessionFactory...");
				sessionFactory = HibernateUtil.getSessionFactory();
			}
			batchSession = sessionFactory.openSession();
			*/
		}

		project = SessionUtils.getCurrentProject();
		if (project == null) {
			EventUtils.logError("ImportFileImpl: no project available");
		}
		log.debug("project=" + project);
		return (project != null);
	}

	/**
	 * Initialize in preparation for an import. Note that the import beans are
	 * re-used, so fields must be re-initialized each time.
	 */
	private void init() {
		script = null;
		priorScript = null;
		locationManager = null;
		matchedElements = new HashSet<Integer>();
		addedElements = new ArrayList<Integer>();
		characterMap = new HashMap<String, ScriptElement>(100);
		preSceneText = null;
		newScriptElements = 0;
		newLocations = 0;
		oldScriptElements = 0;
		scenesAdded = 0;
		sceneNumbers = new HashSet<String>(100);
		noIntExt = 0;
		noDayNight = 0;
		breakdownNumber = 0;
		messageLog = "";
		sceneSequence = new Integer(0);
		textElementSequence = 0;
	}

	/**
	 * Called at end of processing, to release objects that are no longer needed.
	 * Note that we can't just initialize everything, as our caller may still retrieve
	 * values such as counts of scenes and elements added.
	 */
	private void release() {
		project = null;
		locationManager = null;
		script = null;
		priorScript = null;
		addedElements = null;
		matchedElements = null;
	}

	/**
	 * Join/start a transaction. Only used in batch mode.
	 */
	protected void startTransaction() {
		if (batchSession != null) {
			transaction = batchSession.beginTransaction();
			log.debug("transaction joined");
		}
		return;
	}

	/**
	 * End (commit or rollback) the current transaction.
	 * Only used in batch mode.
	 */
	protected void endTransaction(boolean bRet) {
		if (transaction != null) {
			if (bRet) {
				transaction.commit();
				log.debug("transaction COMMITTED");
			}
			else {
				transaction.rollback();
				log.debug("transaction ROLLED BACK");
			}
		}
	}

	/**
	 * Finish up the import process:
	 *  - save the script to the database;
	 *  - do project object updates.
	 */
	protected void doFinalUpdates() {
		setProgress(90);
		log.debug("scene count = " + script.getSceneSize());
		if (script.getSceneSize() > 0) {
			scriptDAO.save(script);
			updateProject();
		}
		SessionUtils.put(Constants.ATTR_IMPORT_ADDED_ELEMENTS, addedElements);
	}

	/**
	 * Validate the location name against the db; create a new location
	 * entry if necessary, and return the location's ScriptElement.
	 */
	protected ScriptElement getLocation(String name) {
		ScriptElement loc = null;

		if (name != null && name.length() > 0) {
			name = name.toUpperCase();
			if (name.length() > Constants.MAX_ELEMENT_NAME_LENGTH) {
				String newname = name.substring(0, Constants.MAX_ELEMENT_NAME_LENGTH);
				userInfoMessage("ImportScript.SetNameTooLong",
						name, Constants.MAX_ELEMENT_NAME_LENGTH, newname);
				name = newname;
			}
			loc = scriptElementDAO.findByNameTypeProject(name, ScriptElementType.LOCATION, project);
			if (loc == null) {
				// TODO search other projects for possible match?
				loc = new ScriptElement();
				loc.setType(ScriptElementType.LOCATION);
				loc.setName(name);
				loc.setProject(project);
				loc.setRealElementRequired(true);
				if (locationManager == null) {
					locationManager = RoleDAO.findContactByRole(project.getMainUnit(), RoleDAO.LOCATION_MANAGER);
				}
				loc.setContact(locationManager);
				scriptElementDAO.save(loc);
				addedElements.add(loc.getId());
				newLocations++;
			}
		}
		return loc;
	}

	/**
	 * Assign element id's (cast id's) to those CHARACTER ScriptElements which
	 * do not have one set yet.  They are numbered in ascending order, from 1,
	 * based on the number of scenes in which they appear.  Any pre-existing
	 * cast id numbers are skipped when assigning the new ones.
	 */
	protected boolean assignCastIds(Project project) {
		boolean bRet = true;
		List<Object[]> pairList;

		pairList = scriptElementDAO.findUnassignedCastIds(project, script);
		int elementid = 0;
		for ( Object[] pair : pairList) {
			elementid = getAvailableElementId(project,elementid);
			Integer id = (Integer)pair[0];
			ScriptElement se;
			se = scriptElementDAO.findById(id);
			if (se != null) {	// it should never be null!
				se.setElementIds(""+elementid);
				scriptElementDAO.merge(se);
				log.debug("script element "+se.getId()+" (" + se.getName() + ") assigned cast id (element_id) "+elementid);
			}
		}
		log.debug("assignCastIds completed, ret="+bRet);
		return bRet;
	}

	@Override
	@Transactional
	public boolean assignCastIds() {
		setProgress(98);	// almost done!
		project = SessionUtils.getCurrentProject();
		Integer scriptId = SessionUtils.getInteger(Constants.ATTR_IMPORT_SCRIPT_ID);
		if (scriptId != null) {
			script = ScriptDAO.getInstance().findById(scriptId);
		}
		else {
			script = project.getScript();
		}
		boolean bRet = assignCastIds(project);
		return bRet;
	}

	/**
	 * Find the next unused ScriptElement.element_id for the given project which is greater
	 * than the supplied element_id.
	 */
	private int getAvailableElementId(Project project, int elementid) {
		ScriptElement se;
		do {
			elementid++;
			se = scriptElementDAO.findByElementIdProjectType(""+elementid, project, ScriptElementType.CHARACTER);
		} while( se != null );

		return elementid;
	}

	protected void createScript(ImportType type) {
		script = new Script();
		int rev = scriptDAO.findMaxScriptRevision(project);
		//log("prior max script revision= "+rev);

		priorScript = scriptDAO.findByRevisionAndProject(rev, project);
		log.debug("prior script="+priorScript);
		script.setRevisionNumber(++rev);
		script.setImportType(type);
		script.setColorName(colorName);

		Date date = new Date();
		script.setDate(date);
		script.setDescription(description);
		script.setProject(project);
		scriptDAO.save(script);
	}

	/**
	 * If the project does not have a current script,
	 * set those values to the just-created script.
	 */
	protected void updateProject() {
		if (project.getScript() == null) {
			projectDAO.setScript(project, script);
			project = projectDAO.merge(project);
		}
	}

	/**
	 * Add a scene to the current script and insert in the database.
	 */
	protected boolean addScene(SceneInfo info) {
		return addScene(info.scene, info.omitted);
	}

	/**
	 * Add a scene to the current script and insert in the database.
	 */
	protected boolean addScene(Scene scene, boolean omitted) {
		boolean bRet = true;

		scene.setScript(script);
		if (script.getScenes() == null) {
			script.setScenes(new ArrayList<Scene>());
		}

		scene.setLastRevised(script.getRevisionNumber());
		if (omitted) {
			scene.setOmitted(true);
		}
		// add scene to database
		try {
			sceneDAO.save(scene);
		}
		catch (Exception e) {
			userErrorMessage("ImportFile.AddSceneError", scene.getNumber());
			scene.setScript(null);
			userExceptionMessage("Exception", e);
			bRet = false;
		}
		script.getScenes().add(scene);
		scenesAdded++;
		return bRet;
	}

	/**
	 * Create a new Scene object, attach it to the current Script, and set the
	 * sequence and revision numbers.
	 */
	protected Scene createScene(String sceneNumber) {
		Scene scene = new Scene();
		scene.setScript(script);
		scene.setNumber(sceneNumber);
		sceneSequence += SceneDAO.SEQUENCE_INCREMENT;
		scene.setSequence(sceneSequence);
		scene.setLastRevised(script.getRevisionNumber());
		return scene;
	}

	/**
	 * Determine if a newly imported Scene has any revisions (changes) versus
	 * the matching Scene in the previous Script, if any.  If there is a prior
	 * Script, and a Scene is found in the prior Script with the same scene
	 * number as the given Scene, they are compared.  If they are identical,
	 * then the "revision number" of the given Scene will be updated to match
	 * the revision number of the prior Script's scene.
	 */
	protected void updateRevision(Scene scene) {
		boolean bRevised = true;
		Scene priorScene = null;
		if (priorScript != null) {
			priorScene = sceneDAO.findByScriptAndNumber(priorScript, scene.getNumber());
			if (priorScene != null) {
				//log("priorScene id="+priorScene.getId()+" - "+priorScene.getNumber());
				bRevised = priorScene.isRevised(scene);
			}
			else {
				log("New scene added -- number "+scene.getNumber());
			}
			if ( (! bRevised) && priorScene != null) {
				scene.setLastRevised(priorScene.getLastRevised());
				// if scene page position shifted due to earlier changes, our calculated
				// page length might be 1/8 different for various reasons.  We'll force
				// it the same, so it looks/compares equal in compare/transfer step.
				scene.setLength(priorScene.getLength());
				//sceneDAO.merge(scene);
			}
		}
		if (includeText) {
			scene.setSynopsis(SceneDAO.createSynopsis(scene));
		}
	}

	/**
	 * Find or create a ScriptElement of type CHARACTER that matches the name in
	 * the supplied text.
	 * <p>
	 * Some analysis of the text is done to eliminate possible mis-diagnosed
	 * Character lines, to remove annotations (such as V.O.), and handle cases
	 * where multiple character names appear on the line (delimited by either
	 * '&' or '/').
	 * <p>
	 * Once the Character name is isolated, we find the ScriptElement of type
	 * Character that matches the text. If not found in our existing map of
	 * Characters (from the current script), check the database. If found, add
	 * it to our map. If not, add it to the database and our character map. The
	 * local character map avoids issuing a database call for every Character
	 * line.
	 *
	 * @param name The String containing a Character name.
	 * @param scriptElements The Set of ScriptElement`s (if any) to which the
	 *            Character element will be added. If this parameter is null, it
	 *            is ignored.
	 * @return The ScriptElement matching the given name. This is only null if
	 *         the name String is blank (or consists solely of text that appears
	 *         to be an annotation such as (V.O.)).
	 */
	protected ScriptElement getCharacterElement(String name, Set<ScriptElement> scriptElements) {
		ScriptElement se = null;
		name = name.toUpperCase();
		se = characterMap.get(name); // try common case first - already mapped
		if (se == null) {
			name = ScriptUtils.extractCharacterName(name, null);
			se = characterMap.get(name); // try match again
			if (se == null && name.length() > 0) {
				// check for combination of names ('FRED & JOE', 'JACK/JILL')
				int ix = name.indexOf('/');
				if (ix < 0) {
					ix = name.indexOf('&');
				}
				if (ix > 0) { // check 1st part for name
					String name1 = name.substring(ix+1).trim();
					if (characterMap.get(name1) == null) {
						se = getScriptElement(name1, ScriptElementType.CHARACTER);
						if (se != null) {
							characterMap.put(name1, se);
							if (includeSceneElements && scriptElements != null) {
								scriptElements.add(se);
							}
						}
					}
					// pass 2nd part as name to be handled below
					name = name.substring(0, ix).trim();
					se = characterMap.get(name);
				}
				if (se == null) {
					// Find matching ScriptElement in db, or create a new one:
					se = getScriptElement(name, ScriptElementType.CHARACTER);
				}
				if (se != null) {
					characterMap.put(name, se);
				}
			}
		}
		if (includeSceneElements && se != null && scriptElements != null) {
			scriptElements.add(se);
		}
		return se;
	}

	/**
	 * Find or create a ScriptElement of the given type with the given name.
	 *
	 * @param name The name of the ScriptElement; this will be converted to all
	 *            uppercase, and will be truncated to the allowed maximum if
	 *            necessary.
	 * @param type The ScriptElementType of the desired element.
	 * @return A ScriptElement with the given name and type. If a matching entry
	 *         was not found in the database, a new one was created, saved in
	 *         the database and returned.
	 */
	protected ScriptElement getScriptElement(String name, ScriptElementType type) {
		ScriptElement se;
		if (name.length() > Constants.MAX_ELEMENT_NAME_LENGTH) {
			String newname = name.substring(0, Constants.MAX_ELEMENT_NAME_LENGTH);
			userInfoMessage("ImportScript.ElementNameTooLong",
					name, Constants.MAX_ELEMENT_NAME_LENGTH, newname);
			name = newname;
		}
		name = name.toUpperCase();
		se = scriptElementDAO.findByNameTypeProject( name, type, project);
		if (se == null) {
			if (includeSceneElements) {
				se = new ScriptElement();
				se.setName(name);
				se.setType(type);
				ScriptUtils.setDefaultValues(se, project);
				scriptElementDAO.save(se);
				newScriptElements++;
				addedElements.add(se.getId());
			}
		}
		else {
			if (! addedElements.contains(se.getId())) {
				boolean newid = matchedElements.add(se.getId());
				if (newid) {
					oldScriptElements++;
				}
			}
		}
		return se;
	}

	/**
	 * Checking scene heading for proper structure -- [INT./EXT.] <location> - [DAY/NIGHT/etc]
	 * lastIE and lastDN are the previous heading's Int/Ext and Day/Night values. These will
	 * be used for the current scene if the heading does not contain valid values.
	 * Updates
	 * 		info.scene.ieType,
	 * 		info.scene.dnType,
	 * 		info.location,
	 * 		info.heading, and
	 * 		info.omitted.
	 */
	protected void cleanHeading(SceneInfo info, IntExtType lastIE, DayNightType lastDN) {

		Scene scene = info.scene;
		scene.setIeType(lastIE);
		scene.setDnType(lastDN);
		int len = info.heading.length();
		if (len == 0) {
			info.heading = scene.getShortIeType() + ". ? - " + scene.getDnType();
			return;
		}

		// strip trailing "*" - probably "text changed" indicator from Final Draft
		if (info.heading.charAt(len - 1) == '*') {
			info.heading = info.heading.substring(0, len - 1).trim();
		}

		if (info.heading.matches(RE_OMITTED_SCENE)) {
			info.heading = info.heading.toUpperCase();
			info.omitted = true;
			return;
		}

		while( info.heading.indexOf("  ") >= 0 ) {
			// reduce any multiple contiguous blanks to a single blank
			info.heading = info.heading.replaceAll("  ", " ");
		}

		boolean goodDN = false, goodIE = false;
		String strDn = "";
		Matcher match = pFullHeading.matcher(info.heading);
		boolean bMatch = match.matches();
		String firstWord = "";
		try {
			String location = ""; // "set" location/name
			if (bMatch) {
				if (match.group(3) != null) {
					firstWord = match.group(3).trim();
					IntExtType ie = getIntExt(firstWord);
					if (ie != null) {
						goodIE = true;
						scene.setIeType(ie);
						firstWord = "";
					}
					else { // not valid I/E indicator
						firstWord += " ";
					}
				}
				if (match.group(4) != null) {
					location = firstWord + match.group(4).trim();
				}
				else if (match.group(5) != null) {
					location = match.group(5).trim();
				}
				if (match.group(9) != null) { // trailing text following "-"
					strDn = match.group(9).trim();
					DayNightType newDN = DayNightType.toValue(strDn);
					if (newDN != DayNightType.N_A) { // valid D/N type
						scene.setDnType(newDN);
						goodDN = true;
					}
					else { // not pure D/N, save as "hint"
						// see if field CONTAINS a value from DayNightType
						for (DayNightType t : DayNightType.values()) {
							if (strDn.indexOf(t.name()) >= 0) {
								// So use this, but leave in "hint", and still issue warning
								scene.setDnType(t);
								break;
							}
						}
						if (strDn.length() > Scene.HINT_MAX_LENGTH) {
							strDn = strDn.substring(0,Scene.HINT_MAX_LENGTH);
						}
						scene.setHint(strDn);
						if (CONTINUED.indexOf('?' + strDn + '?') >= 0) {
							// string is one of "standard" continuation terms, so don't issue message
							goodDN = true;
						}
					}
				}
				int ix;
				if ((ix=location.lastIndexOf('-')) > 0) { // look for other common annotations
					String note = location.substring(ix+1).trim();
					if (NOTATIONS.indexOf('.' + note + '.') >= 0) {
						if (scene.getHint() != null) {
							note += " - " + scene.getHint();
						}
						if (note.length() > Scene.HINT_MAX_LENGTH) {
							note = note.substring(0,Scene.HINT_MAX_LENGTH);
						}
						scene.setHint(note);
						location = location.substring(0, ix).trim();
					}
					else { // Maybe the D/N value was before the "hint"! ("DOCK - DAY - LATER")
						DayNightType newDN = DayNightType.toValue(note);
						if (newDN != DayNightType.N_A) { // valid D/N type
							scene.setDnType(newDN);
							goodDN = true;
							location = location.substring(0, ix).trim();
						}
					}
				}
			}
			else { // no hyphen
				//log("**NO MATCH** '"+scene.heading+"'");
				location = info.heading; // assume all goes to description
				match = pShortHeading.matcher(info.heading);
				boolean bMatchShort = match.matches();
				if (bMatchShort) {	// at least one blank; group1 = first word, group2 = remainder
					firstWord = match.group(1).trim();
					IntExtType ie = getIntExt(firstWord);
					if (ie != null) {
						goodIE = true;
						scene.setIeType(ie);
						location = match.group(2).trim();
						// going well -- let's see if last word is D/N value
						match = pLastWord.matcher(location);
						if (match.matches()) { // at least 2 words in string...
							String lastWord = match.group(2).trim();
							DayNightType dntype = DayNightType.toValue(lastWord);
							if (dntype != DayNightType.N_A) { // good D/N text!
								scene.setDnType(dntype);
								goodDN = true;
								// remove last word from description string
								location = match.group(1).trim();
							}
							else {
								log.debug("");
								for (DayNightType dnt : DayNightType.values() ) {
									if (location.endsWith("-" + dnt.name())) {
										scene.setDnType(dnt);
										goodDN = true;
										location = location.substring(0,location.lastIndexOf(dnt.name())-1).trim();
										break;
									}
								}
								if (! goodDN) {
									for (String s : NOTATION_LIST) {
										if ( location.endsWith(s)) {
											location = location.substring(0,location.lastIndexOf(s)).trim();
											scene.setHint(s);
										}
									}
								}
							}
						}
					}
				}
			}

			if ( ! goodIE ) {	// count Int/Ext errors
				noIntExt++;
				log.info("missing I/E indicator: " + info.heading);
				userInfoMessage("ImportScript.MissingIE", scene.getNumber(), info.heading);
			}
			if ( ! goodDN ) {	// count Day/Night errors
				noDayNight++;
				log.info("missing D/N indicator: " + info.heading);
				userInfoMessage("ImportScript.MissingDN", scene.getNumber(), info.heading, scene.getDnType().getLabel());
			}

			// If location has trailing "-", strip it; happens if "--" used as separator
			len = location.length();
			if (len > 1 && location.charAt(len - 1) == '-') {
				location = location.substring(0, len - 1).trim();
			}
			info.location = location;
			info.heading = scene.getShortIeType() + ". " + location + " - "
					+ scene.getDnType();
		}
		catch (Exception e) {
			if (bMatch) {
				EventUtils.logError("1=" + match.group(1) + "; " + "2=" + match.group(2) + "; "
						+ "3=" + match.group(3) + "; " + "4=" + match.group(4)
						+ "; " + "5=" + match.group(5) + "; " + "6="
						+ match.group(6) + ". ", e);
			}
			else {
				EventUtils.logError("*** no match ***", e);
			}
		}
	}

	/**
	 * Analyze the supplied text to try and match it with one of the standard
	 * Interior/Exterior abbreviations in scripts.
	 *
	 * @param word The text to match.
	 * @return An enumeration of type IntExtType that corresponds to the text.
	 *         Returns null if no match is found.
	 */
	protected IntExtType getIntExt(String word) {
		IntExtType ie = null;
		if (word.length() > 2 &&
				"INT. INT EXT. EXT I/E. I/E E/I. E/I INT/EXT. INT./EXT. INT/EXT EXT/INT. EXT./INT. EXT/INT ".indexOf(word+" ") >= 0) {
			if (word.contains("/")) {
				if (word.charAt(0) == 'I') {
					ie = IntExtType.INT_EXT;
				}
				else {
					ie = IntExtType.EXT_INT;
				}
			}
			else if (word.charAt(0) == 'I') {
				ie = IntExtType.INTERIOR;
			}
			else {
				ie = IntExtType.EXTERIOR;
			}
		}
		return ie;
	}

	protected void setProgress(int progress) {
//		if (progressBar != null) { // not used in batch
//			if (progress > progressBar.getProgress()) {
//				// only allow increase
//				progressBar.setProgress(progress);
//			}
//		}
	}

	protected void userInfoMessage(String msgid, Object... args) {
		String msg = MsgUtils.formatMessage(msgid, args);
		log.info(msg);
		log(msg);
	}

	protected void userErrorMessage(String msgid, Object... args) {
		String msg = MsgUtils.formatMessage(msgid, args);
		log.warn(msg);
		log(msg);
	}

	protected void userErrorMessage(String msgid) {
		String msg = MsgUtils.getMessage(msgid);
		log.warn(msg);
		log(msg);
	}

	protected void userExceptionMessage(String exceptionName, Exception ex) {
		EventUtils.logError(ex);
		userErrorMessage("ImportFile.ProcessError", exceptionName);
		if (ex.getLocalizedMessage() != null) {
			userErrorMessage("ImportFile.ExceptionMessage", ex.getLocalizedMessage());
		}
	}

	protected void log(String msg) {
		if (batchMode) {
			System.out.println(msg);
		}
		else {
			messageLog += msg + Constants.NEW_LINE;
		}
	}

	@Override
	public String getMessageLog() {
		return messageLog;
	}

	@Override
	public int getNewScriptElements() {
		return newScriptElements;
	}
	public void setNewScriptElements(int newScriptElements) {
		this.newScriptElements = newScriptElements;
	}

	@Override
	public int getNewLocations() {
		return newLocations;
	}
	public void setNewLocations(int newLocations) {
		this.newLocations = newLocations;
	}

	@Override
	public int getOldScriptElements() {
		return oldScriptElements;
	}
	public void setOldScriptElements(int oldScriptElements) {
		this.oldScriptElements = oldScriptElements;
	}

	@Override
	public int getScenesAdded() {
		return scenesAdded;
	}
	public void setScenesAdded(int scenesAdded) {
		this.scenesAdded = scenesAdded;
	}

	@Override
	public int getPageCount() {
		return pageCount;
	}
	public void setPageCount(int pageCount) {
		this.pageCount = pageCount;
	}

	static class SceneInfo {
		protected boolean omitted = false;
		protected int sceneId = 0;
		protected Scene scene = null;
		protected String heading = null;
		protected String location = null;
		protected float pageYpos;

		public SceneInfo() {
		}

		public SceneInfo(Scene scn) {
			scene = scn;
		}

		@Override
		public String toString() {
			return "sceneId=" + sceneId +
					", heading=" + heading + Constants.NEW_LINE + scene.toString();
		}
	}

}
