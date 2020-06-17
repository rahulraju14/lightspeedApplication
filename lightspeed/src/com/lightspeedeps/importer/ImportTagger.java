//	File Name:	ImportTagger.java
package com.lightspeedeps.importer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.*;

import com.lightspeedeps.dao.SceneDAO;
import com.lightspeedeps.model.Scene;
import com.lightspeedeps.model.ScriptElement;
import com.lightspeedeps.model.TextElement;
import com.lightspeedeps.type.DayNightType;
import com.lightspeedeps.type.ImportType;
import com.lightspeedeps.type.IntExtType;
import com.lightspeedeps.type.TextElementType;
import com.lightspeedeps.util.app.EventUtils;

import static com.lightspeedeps.type.DayNightType.*;
import static com.lightspeedeps.type.IntExtType.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;


/**
 * A class for importing scripts saved in Final Draft Tagger format. This is not
 * used very often since more recent Final Draft versions have the "tag"
 * information in the native FDX file, which we import directly.
 */
@ManagedBean
@ViewScoped
public class ImportTagger extends ImportFinalDraft implements Serializable {
	/** */
	private static final long serialVersionUID = - 2757257547515419946L;

	private static final Log log = LogFactory.getLog(ImportTagger.class);

	/**
	 * This is a list of "SceneInfo" nodes from the "SceneInfoList" tag of the
	 * Tagger file, mapped by the "sceneId", a numeric value assigned to every
	 * scene-header type of Paragraph element in the file. The SceneInfoList has
	 * an entry for each scene, defining its length and starting page number.
	 * This List is used during paragraph processing by getSceneInfo() to help
	 * populate our SceneInfo objects.
	 */
	private Map<Integer,Element> sceneInfoMap = null;

	/**
	 * This is a list of "ElementScene" nodes from the "ElementSceneList" tag
	 * of the Tagger file.
	 * The ElementSceneList has an entry for each ScriptElement that occurs
	 * in each scene.  Each entry has a SceneID and ElementID.
	 * This List is used by getSceneElements() to create each Scene's set of
	 * ScriptElements when we process a "scene" paragraph type.
	 */
	private List<Element> elementSceneList = null;

	// values for element types of paragraphs
	// Note that paragraph type is also specified, but appears to be superfluous
	private static final int type_general = 1; // paragraphType = 0
	private static final int type_scene = 2; // paragraphType = 9 - scene heading
	private static final int type_action = 3; // paragraphType = 10
	private static final int type_character = 4; // paragraphType = 11
	private static final int type_paren = 5; // paragraphType = 13
	private static final int type_dialog = 6; // paragraphType = 12
	private static final int type_transition = 7; // paragraphType = 8

	public ImportTagger() {
	}

	@Override
	protected boolean doImport() {
		boolean bRet = false;

		try {
			init();
			// 2.2.4891 - use the FDX filter, which preserves most foreign-language characters,
			// instead of the old Tagger filter, which replaced most of them.
			bRet = loadDocument(file, FILTER_FDX);
		}
		catch (FileNotFoundException e) {
			userErrorMessage("ImportFile.ReadError");
		}

		try {
			if (bRet && document != null) {
				Element root = document.getRootElement();
				if (root != null) {
					bRet = processDocument(root);
					if (bRet) {
						//bRet = assignCastIds(project);
						finalDebug();
					}
				}
				else {
					log("Tagger Document has no root element; invalid XML format.");
					bRet = false;
				}
			}
			else {
				log("Tagger Document could not be parsed; invalid XML format.");
				bRet = false;
			}
		}
		catch (Exception e) {
			bRet = false;
			EventUtils.logError(e);
		}

		return bRet;
	}

	private boolean processDocument(Element root) {
		boolean bRet = setup();
		if (!bRet)
			return false;

		//log("root = " + root.getName());

		// These define the script element category names and ids.
		// Currently we're using the hard-coded values initialized above.
		//List<Element> catList = getSubList(root, "CategoryList", "Category");

		// These define what we call scriptElements
		List<Element> elementList = getSubList(root, "ElementList", "Element");
		bRet = processScriptElements(elementList, false);

		if (bRet) {
			// These define paragraph types and margins; not currently used.
			// Currently we're using the hard-coded values initialized above.
			//List<Element> scriptElementList = getSubList(root, "ScriptElementList", "ScriptElement");

			// These define the body of the script
			List<Element> paragraphList = getSubList(root, "ParagraphList", "Paragraph");

			// The ElementInstanceList is not currently used. It contains an entry
			// for every instance (occurrence) of every ScriptElement.
			//List<Element> elementInstanceList = getSubList(root, "ElementInstanceList", "ElementInstance");

			// The ElementSceneList has an entry for each ScriptElement that occurs
			// in each scene.  We'll use that to create each Scene's set of ScriptElements.
			elementSceneList = getSubList(root, "ElementSceneList", "ElementScene");

			// The SceneInfoList has an entry for each scene, defining its length
			// and starting page number.  We'll extract that during the scene
			// (paragraph) processing.
			createSceneInfoMap(getSubList(root, "SceneInfoList", "SceneInfo"));

			startTransaction();
			try {
				createScript(ImportType.TAGGER);

				bRet = processParagraphs(paragraphList);
				if (bRet) {
					doFinalUpdates();
				}

			}
			catch (Exception e) {
				userExceptionMessage("Exception", e);
				bRet = false;
			}
			endTransaction(bRet);
		}
		return bRet;
	}

	/**
	 * The heart of the processing.
	 * The script itself is stored as a series of "paragraphs" of various types.
	 * Loop through all the paragraphs, building scenes and text elements
	 * from the paragraph data.
	 */
	private boolean processParagraphs(List<Element> parList) {
		int sceneNumber = 0; // numeric part of scene number
		String sceneNumberString = ""; // entire scene number (may include alpha)
		int sceneSequence = SceneDAO.SEQUENCE_INCREMENT; // for maintaining scene sort order
		int lastPage = 0;
		IntExtType lastIE = INTERIOR; // INT or EXT from prior scene
		DayNightType lastDN = DAY; // Day/Night from prior scene
		TextElementType textType = TextElementType.OTHER;
		TextElement textElement = null;
		List<TextElement> textElements = null;
		Scene scene = null;
		String text = "";
		ScriptElement character = null; // SE for the last "Character" paragraph processed
		int suffixNumber = 0; // used for scene number conflicts
		boolean hadSuffix = false, hadPrefix = false;	// any suffix/prefix found across all scenes

		for (Element par : parList) {
			textElement = new TextElement();
			text = par.getChildTextTrim("Text");
			int type = Integer.parseInt(par.getAttributeValue("ElementType"));

			switch (type) {
				case type_scene :
					// finish up prior scene
					if (scene != null) {
						if (includeText) {
							scene.setTextElements(textElements);
						}
						updateRevision(scene);
					}
					// start new scene
					textType = TextElementType.SCENE_HEADING;
					character = null;
					int sceneId = 0;
					String sceneIdStr = par.getAttributeValue("ID");
					try {
						sceneId = Integer.parseInt(sceneIdStr);
					}
					catch (Exception e) {
					}
					String prefix = "", suffix = "";
					sceneNumber++;
					sceneNumberString = par.getChildTextTrim("ElementNumber");
					if (sceneNumberString != null && sceneNumberString.length() > 0) {
						Matcher match = pSceneNumber.matcher(sceneNumberString);
						boolean bMatch = match.matches();
						String numberToConvert = sceneNumberString;
						if (bMatch) {
							prefix = match.group(1).trim();
							numberToConvert = match.group(2).trim();
							suffix = match.group(3).trim();
							hadPrefix = hadPrefix || prefix.length() > 0;
							hadSuffix = hadSuffix || suffix.length() > 0;
						}
						int num = 0;
						try {
							num = Integer.parseInt(numberToConvert);
							if (num >= sceneNumber) {
								sceneNumber = num;
								suffixNumber = 0;	// reset since we're ok
							}
							else if ( (prefix+suffix).length() > 0 && num == (sceneNumber-1)) {
								// probably OK.  e.g., had "47" then "47A"
								sceneNumber = num;	// keep lower number
							}
							else {
								suffixNumber++;
								suffix = "X" + suffixNumber;
								userInfoMessage("ImportScript.WarnSceneNumber",
										sceneNumberString, sceneNumber+"", (--sceneNumber)+suffix);
							}
						}
						catch (Exception e) {
						}
					}
					else {
						suffixNumber = 0;	// reset since we're ok
					}
					sceneNumberString = prefix + sceneNumber + suffix;
					//log("Scene " + sceneNumberString + ": '" + text + "'");
					scene = new Scene();
					scene.setScript(script);
					scene.setNumber(sceneNumberString);
					scene.setSequence(sceneSequence);
					sceneSequence += SceneDAO.SEQUENCE_INCREMENT;
					scene.setLastRevised(script.getRevisionNumber());
					SceneInfo info = getSceneInfo(sceneId, scene);
					if (scene.getPageNumber() >= lastPage) { // (maybe) new last page #
						lastPage = scene.getPageNumber() + ((scene.getLength()+7)/8);
					}
					textElements = new ArrayList<>();

					// validate & clean heading (Int/ext, day/night, etc.)
					info.heading = text;
					cleanHeading(info, lastIE, lastDN);
					lastIE = scene.getIeType();
					lastDN = scene.getDnType();

					info.scene.setScriptElement(getLocation(info.location));

					// Get associated ScriptElements for this scene
					if (includeSceneElements) {
						scene.setScriptElements(getSceneElements(sceneIdStr));
					}
					//log(info.toString());

					// Create a Breakdown Sheet to match the Scene,
					// and add scene & breakdown sheet to database
					addScene(info);

					break;
				case type_action :
					textType = TextElementType.ACTION;
					break;
				case type_character :
					textType = TextElementType.CHARACTER;
					// xTODO remove annotations like (V.O.) [skip - infrequent Tagger usage]
					ScriptElement se = characterMap.get(text);
					if (se != null) {
						character = se;
					}
					break;
				case type_dialog :
					textType = TextElementType.DIALOGUE;
					if (character != null) {
						final float linesize = 35f;
						int lines = (int)Math.ceil(text.length()/linesize);
						/*log.debug(character.getName()+": "+character.getLineCount()
								+ " + " + lines + " (" + text.length() + ") = "
								+ (character.getLineCount()+lines) );/**/
						character.setLineCount(character.getLineCount() + lines);
						//character.setLineCount(character.getLineCount()+1); // dialogue count
					}
					break;
				case type_paren :
					textType = TextElementType.PARENTHETICAL;
					break;
				case type_transition :
					textType = TextElementType.TRANSITION;
					break;
				case type_general :
					textType = TextElementType.OTHER;
					break;
				default :
					log.warn("Warning: Unexpected paragraph element type: " + type);
			}
			if (scene == null) { // happens if leading text elements (general)
				scene = new Scene();
				scene.setNumber("0");
				scene.setSequence(sceneSequence);
				sceneSequence += SceneDAO.SEQUENCE_INCREMENT;
				scene.setDnType(DayNightType.N_A);
				scene.setIeType(IntExtType.N_A);
				textElements = new ArrayList<>();
				// Tagger file may have info as "sceneId=0"
				SceneInfo info = getSceneInfo(0, scene);
				scene.setPageNumber(1); // some Tagger files had last page as value
				addScene(info);
			}
			if (includeText && textElements != null) {
				textElement.setScene(scene);
				textElement.setType(textType);
				textElement.setSequence(textElementSequence);
				textElementSequence += TextElement.SEQUENCE_INCREMENT;
				textElement.setText(text);
				// add textElement to db
				getTextElementDAO().save(textElement);
				textElements.add(textElement);
			}
		}

		if (scene != null) { // finish up last scene
			scene.setTextElements(textElements);
			updateRevision(scene);
		}

		script.setLastPage(lastPage);

		if (hadSuffix && hadPrefix) {
			userInfoMessage("ImportScript.MixedSceneNumbers");
		}
		return true;
	}

	/**
	 * Return the set of ScriptElements referenced by a particular scene, identified
	 * by its Tagger 'sceneId'.  This info is retrieved from the Tagger ElementSceneList
	 * tag.  It uses the map of (tagger) elementId to ScriptElement created
	 * during the import from the Tagger file's ElementList tag.
	 */
	private Set<ScriptElement> getSceneElements(String sceneId) {
		Set<ScriptElement> s = new HashSet<ScriptElement>();
		if (includeSceneElements) {
			for (Element el : elementSceneList) {
				String id = el.getAttributeValue("SceneID");
				if (sceneId.equals(id)) {
					String elemId = el.getAttributeValue("ElementID");
					ScriptElement sEl = elementMap.get(elemId);
					if (sEl != null) {
						s.add(sEl);
					}
				}
			}
		}
		return s;
	}

	/**
	 * Creates the sceneInfoMap by taking each element in the SceneInfoList
	 * (passed as a parameter) and adding it to the map with the sceneId as the
	 * key.
	 *
	 * @param sceneInfoList The list of Elements from the Tagger file that are
	 *            in the SceneInfoList tag.
	 */
	private void createSceneInfoMap(List<Element> sceneInfoList) {
		sceneInfoMap = new HashMap<Integer,Element>();
		for (Element el : sceneInfoList) {
			String id = el.getAttributeValue("SceneID");
			int idNum = 0;
			try {
				idNum = Integer.parseInt(id);
			}
			catch (NumberFormatException e) {
			}
			sceneInfoMap.put(idNum, el);
		}
	}

	/**
	 * Creates a SceneInfo object to match the specified (Tagger) scene ID, and
	 * fills in the Scene's page-length and starting-page from the matching entry in the
	 * Tagger SceneInfoList tag.
	 */
	private SceneInfo getSceneInfo(int sceneId, Scene scene) {
		SceneInfo info = new SceneInfo();
		info.sceneId = sceneId; // paragraph id from tagger file
		info.scene = scene;
		scene.setLength(1);		// set defaults
		scene.setPageNumber(1);
		Element el = sceneInfoMap.get(sceneId);
		if (el != null) {
			try {
				int len = Integer.parseInt(el.getAttributeValue("Pages"))*8 +
						Integer.parseInt(el.getAttributeValue("Eighths"));
				if (len > 0) {
					scene.setLength( len );
				}
			}
			catch (NumberFormatException e1) {
			}
			try {
				scene.setPageNumStr(el.getAttributeValue("Start"));
				scene.setPageNumber( Integer.parseInt(scene.getPageNumStr()) );
			}
			catch (NumberFormatException e) {
			}
			// log("got scene: " + info.toString());
		}
		return info;
	}

}
