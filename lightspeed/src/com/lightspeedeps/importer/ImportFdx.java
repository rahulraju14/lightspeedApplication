//	File Name:	ImportFdx.java
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
import com.lightspeedeps.util.app.Constants;

import static com.lightspeedeps.type.DayNightType.*;
import static com.lightspeedeps.type.IntExtType.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

/**
 * A class for importing scripts saved in Final Draft FDX format --
 * from Final Draft 8 (or later?).  This is an XML file containing
 * the entire document, plus (possibly) "Tagger" information specifying
 * the occurrence of elements within the text.
 */
@ManagedBean
@ViewScoped
public class ImportFdx extends ImportFinalDraft implements Serializable {
	/** */
	private static final long serialVersionUID = 3580474097180873166L;

	private static final Log log = LogFactory.getLog(ImportFdx.class);

	private final static Namespace taggerNs = Namespace.getNamespace("http://www.finaldraft.com/ns/Tagger");

	enum ParagraphType {
		GENERAL,
		ACTION,
		CHARACTER,
		DIALOGUE,
		PARENTHETICAL,
		TRANSITION,
		SCENE_HEADING,
		SHOT,
		CAST_LIST,
		NORMAL_TEXT,
		UNKNOWN
	}

	/** True iff we have found "Tagger" data embedded in the FDX document. */
	private boolean taggerData;

	public ImportFdx() {
	}

	/**
	 * Note that ImportFdx instances are re-used, so fields must be re-initialized
	 * for each import.
	 * @see com.lightspeedeps.importer.ImportFinalDraft#init()
	 */
	@Override
	protected void init() {
		super.init();
		taggerData = false;
	}

	@Override
	protected boolean doImport() {
		boolean bRet = false;

		init();

		try {
			/**
			 * We use the FdxFilter to replace a few selected single-byte or
			 * 3-byte extended characters -- in particular, the "smart quote"
			 * symbols that Final Draft uses by default. To some extent, this
			 * seems necessary because the FDX files have an XML header
			 * specifying "UTF-8", even though they may contain ISO-8859-1
			 * (Latin 1) characters. (rev 2.2.4525; 2.2.4891)
			 */
			bRet = loadDocument(file, FILTER_FDX);
		}
		catch (FileNotFoundException e) {
			userErrorMessage("ImportFile.ReadError");
		}

		if (bRet && document != null) {
			Element root = document.getRootElement();
			if (root != null) {
				bRet = processDocument(root);
				if (bRet) {
					//bRet = assignCastIds(project);
					finalDebug();
				}
			}
		}

		return bRet;
	}

	/**
	 * This takes a script that has been loaded into memory as a DOM document
	 * and creates our Script object and all related objects (Pages,
	 * TextElements, Scenes).
	 *
	 * @param root The root of the DOM tree representing the script.
	 *
	 * @return False iff a severe error, such as an Exception, was encountered
	 *         during the script import.
	 */
	private boolean processDocument(Element root) {
		boolean bRet = setup();
		if (!bRet)
			return false;

		//log("root = " + root.getName());

		// These define the script element category names and ids.
		// Currently we're using the hard-coded values initialized above.
		//List<Element> catList = getSubList(root, "CategoryList", "Category");

		// Get the definitions of what we call ScriptElements
		Element userRoot = root.getChild("UserDocumentData");
		if (userRoot != null) {
			userRoot = userRoot.getChild("TaggerDocumentData",taggerNs);
			if (userRoot != null) {
				taggerData = true;
				List<Element> elementList = getSubList(userRoot, "ElementList", "Element", taggerNs);
				bRet = processScriptElements(elementList, true);
			}
		}

		if (bRet) {
			// Get the subtree that contains the body of the script
			List<Element> paragraphList = getSubList(root, "Content", "Paragraph");

			startTransaction();
			try {
				createScript(ImportType.FDX); // initialize the Script object

				// Main task: turn all the paragraph nodes into our objects.
				bRet = processParagraphs(paragraphList);
				if (bRet) {
					updatePageLayout(root);
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
	 * The heart of the processing. The script itself is passed in as a series
	 * of "paragraphs" of various types (from the FDX file). Loop through all
	 * the paragraphs, building scenes and text elements from the paragraph
	 * data.
	 *
	 * @param parList The List of Element`s from the sub-tree of the script's
	 *            DOM tree that contains all the Paragraph tags comprising the
	 *            actual Script body.
	 * @return False if there was a severe error, such as an Exception thrown,
	 *         during the import process.
	 */
	private boolean processParagraphs(List<Element> parList) {
		boolean bRet = true;
		int sceneNumber = 0; // numeric part of scene number
		int paragraphId = 0;	// sequential numbering of paragraphs
		String sceneNumberString = ""; // entire scene number (may include alpha)
		String baseSceneNumber = ""; // for "shots", scene number without shot number
		int sceneSequence = SceneDAO.SEQUENCE_INCREMENT; // for maintaining scene sort order
		int lastPage = 0;
		IntExtType lastIE = INTERIOR; // INT or EXT from prior scene
		DayNightType lastDN = DAY; // Day/Night from prior scene
		TextElementType textType = TextElementType.OTHER;
		TextElement textElement = null;
		textElementSequence += TextElement.SEQUENCE_INCREMENT; // start above 0, we may insert page header later
		List<TextElement> textElements = null;
		Scene scene = null;
		String text = "";
		ScriptElement character = null; // SE for the last "Character" paragraph processed
		int suffixNumber = 0; // used for scene number conflicts
		boolean hadSuffix = false, hadPrefix = false;	// any suffix/prefix found across all scenes
		boolean shot;	// true if current paragraph element is a "SHOT" type
		int shotNumber = 0;	// in case of multiple shots in one scene

	parLoop:
		for (Element par : parList) {
			textElement = new TextElement();
			text = concatenateChildText(par);
			String parType = par.getAttributeValue("Type");
			ParagraphType type;
			String uParType = "?";
			if (parType == null) {
				log.debug("error: missing paragraph type; par #" + paragraphId + ": " + par.toString());
				log("Paragraph with MISSING type is not supported and was treated as Action text.");
				type = ParagraphType.UNKNOWN;
			}
			else {
				uParType = parType.replace(' ',	'_').toUpperCase();
				try {
					type = ParagraphType.valueOf(uParType);
				}
				catch (Exception e1) {
					log("Paragraph of type " + parType + " is not supported and was treated as Action text.");
					type = ParagraphType.UNKNOWN;
				}
			}
			log.debug("par #" + paragraphId + ": " + uParType);
			shot = false;

			switch (type) {
				case SHOT :
					shot = true; // set flag, then mostly treat as Scene heading...
				case SCENE_HEADING :
					// finish up prior scene
					if (scene != null) {
						if (includeText) {
							scene.setTextElements(textElements);
						}
						updateRevision(scene);
					}
					// start new scene
					textType = TextElementType.SCENE_HEADING;
					text = text.toUpperCase().trim();
					character = null;
					int sceneId = paragraphId;
					String prefix = "", suffix = "";
					sceneNumberString = par.getAttributeValue("Number");
					if (shot && sceneNumberString == null) {
						shotNumber++;
						if (shotNumber == 1) {
							if (scene != null) {
								baseSceneNumber = scene.getNumber();
							}
							else { // no scene? script started with "shot"!
								baseSceneNumber = "" + sceneNumber; // should be 0
							}
						}
						sceneNumberString = baseSceneNumber + "s" + shotNumber;
					}
					else {
						shotNumber = 0;
						sceneNumber++;
					}
					if (!shot && sceneNumberString != null && sceneNumberString.length() > 0) {
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
						sceneNumberString = prefix + sceneNumber + suffix;
					}
					else if (sceneNumberString == null) {
						sceneNumberString = "" + sceneNumber;
					}
					//log("Scene " + sceneNumberString + ": '" + text + "'");
					Scene lastScene = scene;
					scene = new Scene();
					scene.setScript(script);
					if (sceneNumbers.contains(sceneNumberString)) {
						log.warn("duplicate scene number=" + sceneNumberString);
						userInfoMessage("ImportFile.DuplicateSceneNumber", sceneNumberString);
						String sceneAlpha = (prefix.length()==0 ? "A" : prefix);
						sceneNumberString = sceneAlpha + sceneNumber;
						while ( sceneNumbers.contains(sceneNumberString) ) {
							sceneAlpha = SceneDAO.incrementAlpha(sceneAlpha);
							sceneNumberString = sceneAlpha + sceneNumber;
						}
					}
					sceneNumbers.add(sceneNumberString);
					scene.setNumber(sceneNumberString);
					scene.setSequence(sceneSequence);
					sceneSequence += SceneDAO.SEQUENCE_INCREMENT;
					scene.setLastRevised(script.getRevisionNumber());
					SceneInfo info = getSceneInfo(sceneId, scene, par);
					textElements = new ArrayList<TextElement>();
					if (shot && lastScene != null) {
						scene.setPageNumber(lastScene.getPageNumber());
						scene.setPageNumStr(lastScene.getPageNumStr());
						scene.setDnType(lastScene.getDnType());
						scene.setIeType(lastScene.getIeType());
						if (text.length() > Scene.HINT_MAX_LENGTH) {
							scene.setHint(text.substring(0,Scene.HINT_MAX_LENGTH));
						}
						else {
							scene.setHint(text);
						}
						scene.setScriptElement(lastScene.getScriptElement());
					}
					else {
						// validate & clean heading (Int/ext, day/night, etc.)
						info.heading = text;
						cleanHeading(info, lastIE, lastDN);
						lastIE = scene.getIeType();
						lastDN = scene.getDnType();
						scene.setScriptElement(getLocation(info.location));
					}
					int nextPage = scene.getPageNumber() + ((scene.getLength()+7)/8);
					if (nextPage > lastPage) {
						lastPage = nextPage;
					}
					// Get associated ScriptElements for this scene
					if (includeSceneElements) {
						scene.setScriptElements(getSceneElements(par));
					}
					//log(info.toString());
					if (! addScene(info)) { // add scene to script and database
						scene = null;	// false return is a major failure
						bRet = false;
						break parLoop;
					}
					if (preSceneText != null) {
						// There was text preceding the (first) scene header -- attach to Scene
						for (TextElement te : preSceneText) {
							te.setScene(scene);
							getTextElementDAO().save(te); // add textElement to db
							textElements.add(te);
						}
						preSceneText = null;
					}
					break;
				case ACTION :
				case NORMAL_TEXT:
					textType = TextElementType.ACTION;
					text = removeLineBreaks(text);
					addSceneElements(scene, par);
					break;
				case CHARACTER :
					textType = TextElementType.CHARACTER;
					ScriptElement se = null;
					text = text.trim().toUpperCase();
					if (taggerData) {
						se = characterMap.get(text);
					}
					if (se == null) { // no tagger data, or no match
						if (! taggerData && scene != null) {
							se = getCharacterElement(text, scene.getScriptElements());
						}
						else {
							se = getCharacterElement(text, null);
						}
					}
					addSceneElements(scene, par);
					if (se != null) {
						character = se;
					}
					break;
				case DIALOGUE :
					textType = TextElementType.DIALOGUE;
					text = removeLineBreaks(text);
					if (character != null) {
						final float linesize = 35f;
						int lines = (int)Math.ceil(text.length()/linesize);
						/*log.debug(character.getName()+": "+character.getLineCount()
								+ " + " + lines + " (" + text.length() + ") = "
								+ (character.getLineCount()+lines) );/**/
						character.setLineCount(character.getLineCount() + lines);
						//character.setLineCount(character.getLineCount()+1); // dialogue count
					}
					addSceneElements(scene, par);
					break;
				case PARENTHETICAL :
					textType = TextElementType.PARENTHETICAL;
					text = removeLineBreaks(text);
					addSceneElements(scene, par);
					break;
				case TRANSITION :
					textType = TextElementType.TRANSITION;
					text = text.toUpperCase();
					addSceneElements(scene, par);
					break;
				case GENERAL :
					text = removeLineBreaks(text);
					textType = TextElementType.ACTION;
					addSceneElements(scene, par);
					break;
				case CAST_LIST :
				case UNKNOWN :
					textType = TextElementType.OTHER;
					addSceneElements(scene, par);
					break;
				default :
					log.warn("Warning: Unexpected paragraph element type: " + type);
			}
			if (includeText) {
				textElement.setType(textType);
				textElement.setSequence(textElementSequence);
				textElementSequence += TextElement.SEQUENCE_INCREMENT;
				textElement.setText(text);
				if (scene == null) { // some text elements preceding first scene heading (General, Transition, etc)
					if (preSceneText == null) {
						preSceneText = new ArrayList<TextElement>();
					}
					preSceneText.add(textElement); // add to List for scene
				}
				else {
					textElement.setScene(scene);
					getTextElementDAO().save(textElement); // add textElement to db
					textElements.add(textElement);
				}
			}
			paragraphId++;
		}

		if (scene != null) { // finish up last scene
			scene.setTextElements(textElements);
			updateRevision(scene);
		}

		script.setLastPage(lastPage);

		if (hadSuffix && hadPrefix) {
			userInfoMessage("ImportScript.MixedSceneNumbers");
		}
		return bRet;
	}

	/**
	 * Get all the "Text" children of the given paragraph element, and
	 * concatenate them.
	 *
	 * @param par The paragraph Element to be used as the source.
	 * @return A String which is the concatenation of all the "Text" child
	 *         elements within the paragraph.
	 */
	@SuppressWarnings("unchecked")
	private String concatenateChildText(Element par) {
		List<Element> kids = (List<Element>)par.getChildren("Text");
		String text = "";
		for (Element el : kids) {
			String str = el.getText();
			if (str != null) {
				text += str;
			}
		}
		return text;
	}

	/**
	 * Remove all line breaks.  Each line break and any contiguous following
	 * blanks will be replace by a single blank.
	 * @param text The string with possible line breaks to be removed.
	 * @return The revised string with line breaks replaced by blanks.
	 */
	private String removeLineBreaks(String text) {
		text = text.replaceAll("\\n *", " ");
		return text;
	}

	/**
	 * Get the set of ScriptElements referenced by the given paragraph Element,
	 * and add them to the set of ScriptElements of the given Scene.
	 *
	 * @param scene The Scene to which any ScriptElements will be added.
	 * @param par A paragraph Element that may contain Tagger data indicating
	 *            which ScriptElements it references.
	 */
	private void addSceneElements(Scene scene, Element par) {
		if (includeSceneElements && scene != null) {
			scene.getScriptElements().addAll(getSceneElements(par));
		}
	}

	/**
	 * Extract the Tagger information within a paragraph that identifies the
	 * elements referenced by the text of the paragraph.
	 *
	 * @param par The paragraph Element in question.
	 * @return A Set of ScriptElements matching the list of
	 *         TaggerElementInstance entries within the given paragraph.
	 */
	@SuppressWarnings("unchecked")
	private Set<ScriptElement> getSceneElements(Element par) {
		Namespace ns = Namespace.getNamespace("http://www.finaldraft.com/ns/Tagger");
		Set<ScriptElement> seSet = new HashSet<ScriptElement>();
		Element el = par.getChild("UserParagraphData");
		if (el != null) {
			Element el2;
			el2 = el.getChild("TaggerParagraphData", ns);
			if (el2 != null) {
				taggerData = true;
				List<Element> kids = (List<Element>)el2.getChildren("TaggerElementInstance", ns);
				if (kids != null) {
					for (Element child : kids) {
						String elId = child.getAttributeValue("ElementID");
						if (elId != null) {
							ScriptElement se = elementMap.get(elId);
							if (se != null) {
								seSet.add(se);
							}
						}
					}
				}
			}
		}
		log.debug(seSet.size() + " SE's found");
		return seSet;
	}

	/**
	 * Creates a SceneInfo object to match the specified (Tagger) scene ID, and
	 * fills in the Scene's page-length and starting-page from the data in the
	 * SceneProperties child element of the paragraph.
	 *
	 * @param sceneId The id number assigned by Final Draft to this scene --
	 *            which is just the sequential number of the paragraph within
	 *            the Content element.
	 * @param scene The Scene associated with this paragraph.
	 * @param par The paragraph Element that is the scene header.
	 */
	private SceneInfo getSceneInfo(int sceneId, Scene scene, Element par) {
		SceneInfo info = new SceneInfo();
		info.sceneId = sceneId; // paragraph id from tagger file
		info.scene = scene;
		int length = 1;
		int pageNum = 1;
		if (par != null) {
			Element prop = par.getChild("SceneProperties");
			if (prop != null) {
				pageNum = getIntAttribute(prop, "Page", pageNum);
				String len = prop.getAttributeValue("Length");
				length = Scene.convertPageLength(len);
				if (length == 0) { // did not convert properly
					length = 1;
				}
			}
		}
		scene.setLength(length);
		scene.setPageNumber(pageNum);
		scene.setPageNumStr(""+pageNum);
		return info;
	}

	private void updatePageLayout(Element root) {
		Element el = root.getChild("PageLayout");
		if (el != null) {
			int top = getIntAttribute(el, "TopMargin", 18);
			//int header = getIntAttribute(el, "HeaderMargin", 36);
			int bottom = getIntAttribute(el, "BottomMargin", 54);
			//int footer = getIntAttribute(el, "FooterMargin", 36);
			int lines = ((int)(11*Constants.POINTS_PER_INCH)
					- (top+bottom)) / 12;
			script.setLinesPerPage(lines);
		}
	}

	private int getIntAttribute(Element el, String name, int def) {
		int value = def;
		String str = el.getAttributeValue(name);
		if (str != null) {
			try {
				value = Integer.parseInt(str);
			}
			catch (NumberFormatException e) {
				int i;
				if ((i=str.indexOf('-')) > 0) { // Final draft shows pages deleted, e.g., 32-50.
					str = str.substring(0,i);
					try {
						value = Integer.parseInt(str);
					}
					catch (NumberFormatException e2) {
					}
				}
			}
		}
		return value;
	}

}
