//	File Name:	ImportSex.java
package com.lightspeedeps.importer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Scene;
import com.lightspeedeps.model.ScriptElement;
import com.lightspeedeps.type.DayNightType;
import com.lightspeedeps.type.ImportType;
import com.lightspeedeps.type.IntExtType;
import com.lightspeedeps.type.ScriptElementType;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import static com.lightspeedeps.type.DayNightType.*;
import static com.lightspeedeps.type.IntExtType.*;

/**
 * A class for importing scripts saved in Movie Magic ".sex" format.
 */
@ManagedBean
@ViewScoped
public class ImportSex extends ImportFileImpl implements Serializable {
	/** */
	private static final long serialVersionUID = - 8842806056078934495L;

	private static final Log log = LogFactory.getLog(ImportSex.class);

	Map<String, ScriptElement> elementMap = new HashMap<String, ScriptElement>();

	// values for types of records (2-byte field in input records)
	private static final int TYPE_CATEGORIES 	= 0; 	// list of categories -- not used
	private static final int TYPE_SCENE 		= 1;	// scene heading
	private static final int TYPE_ELEMENT 		= 2;	// script element
	private static final int TYPE_SCENE_LENGTH 	= 3;	// end of scene - has scene length

	private static final int SUBTYPE_CHARACTER 	= 0; 	// character (from dialogue entries)
	private static final int SUBTYPE_EXTRAS		= 1; 	// extras
	private static final int SUBTYPE_STUNTS 	= 2; 	// stunts
	private static final int SUBTYPE_VEHICLES 	= 3; 	// vehicles
	private static final int SUBTYPE_PROP 		= 4; 	// Props
	private static final int SUBTYPE_SFX 		= 5; 	// special effects
	private static final int SUBTYPE_WARDROBE 	= 6; 	// Costumes
	private static final int SUBTYPE_MAKEUP 	= 7; 	// makeup
	private static final int SUBTYPE_LIVESTOCK 	= 8; 	// Livestock
	private static final int SUBTYPE_HANDLER 	= 9; 	// Animal handler
	private static final int SUBTYPE_MUSIC 		= 10; 	// Music
	private static final int SUBTYPE_SOUND 		= 11; 	// Sound
	private static final int SUBTYPE_DRESSING 	= 12; 	// Set dressing
	private static final int SUBTYPE_GREENERY 	= 13; 	// Greenery
	private static final int SUBTYPE_EQUIPMENT 	= 14; 	// Special Equipment
	private static final int SUBTYPE_SECURITY 	= 15; 	// Security
	private static final int SUBTYPE_LABOR 		= 16; 	// Additional Labor
	private static final int SUBTYPE_OPT_FX 	= 17; 	// Optical Effects
	private static final int SUBTYPE_MECH_FX 	= 18; 	// Mechanical Effects
	private static final int SUBTYPE_MISC 		= 19; 	// Miscellaneous
	private static final int SUBTYPE_NOTES 		= 20; 	// Notes

	private static final char TAB_CHARACTER = '\t';

	private DataInputStream dstream = null;

	public ImportSex() {
	}

	/**
	 * Called from superclass to import the file.  All parameters have been stored
	 * in member variables.
	 */
	@Override
	protected boolean doImport() {
		boolean bRet = false;

		try {
			dstream = new DataInputStream(new FileInputStream(file));
			bRet = true;
		}
		catch (FileNotFoundException ex) {
			userExceptionMessage(".sex import: File "+file+" could not be found.", ex);
		}

		if (bRet)
			bRet = processDocument();

		if (bRet) {
			//bRet = assignCastIds(project);
		}

		return bRet;
	}

	/**
	 * High-level processing routine:
	 *   - validate the file header
	 *   - call super to setup the session info
	 *   - call super to start/join a transaction
	 *   - create new script & stripboard objects
	 *   - process all the data in the file
	 *   - call super to finish up the script objects
	 *   - call super to finish transaction processing
	 */
	private boolean processDocument() {
		boolean bRet;
		bRet = checkFileHeader();

		if (!bRet) {
			log.warn("invalid header in .sex import file: "+file);
			userErrorMessage("ImportScript.InvalidFileType");
		}
		else {
			bRet = setup();
		}

		if (bRet) {
			startTransaction(); // only needed in batch
			try {
				createScript(ImportType.SEX);

				bRet = processRecords();
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
	 * Main processing logic for the file.
	 * Loop reading records from the file until eof is reached.  The file consists of
	 * a series of scenes.  Each scene begins with a scene-start record, then zero or more
	 * character records (taken from any dialogue in the scene), followed by an scene-end
	 * record.
	 */
	private boolean processRecords() {
		boolean bRet = true;
		SexRecord sex = null;
		Scene scene = null;
		Set<ScriptElement> scriptElements = new HashSet<ScriptElement>();
		SceneInfo info = null;
		int lastPage = 1;
		int sceneNumber = 0; // numeric part of scene number
		int suffixNumber = 0; // used for scene number conflicts
		int totalSceneLength = 0;
		String sceneNumberString = ""; // entire scene number (may include alpha)
		IntExtType lastIE = INTERIOR; // INT or EXT from prior scene
		DayNightType lastDN = DAY; // Day/Night from prior scene
		boolean hadSuffix = false, hadPrefix = false;	// any suffix/prefix found across all scenes

		do {
			sex = getRecord();
			switch (sex.type) {
				case TYPE_CATEGORIES:
					//log("categories (ignored): "+sex.text);
					break;
				case TYPE_ELEMENT:
					//log("character: "+sex.text);
					processElement(scriptElements, sex.text, sex.subtype);
					break;
				case TYPE_SCENE:
					//log("start of scene "+sex.sceneNumber);
					if (info != null) { // need to finish prior scene (no scene-length record)
						finishScene(info, scene, 1, scriptElements);
						totalSceneLength++; // treat scene as 1/8-page long
					}
					scriptElements = new HashSet<ScriptElement>();
					scene = createScene("");
					info = getSceneInfo(scene, sex.text, totalSceneLength);
					String prefix = "";
					String suffix = "";
					sceneNumber++;
					sceneNumberString = scene.getNumber();
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
										sceneNumberString, sceneNumber+"", prefix +(--sceneNumber)+suffix);
							}
						}
						catch (Exception e) {
						}
					}
					else {
						suffixNumber = 0;	// reset since we're ok
					}
					sceneNumberString = prefix + sceneNumber + suffix;
					scene.setNumber(sceneNumberString);
					//log("Scene " + sceneNumberString + ": '" + info.heading + "'");
					// validate & clean heading (Int/ext, day/night, etc.)
					cleanHeading(info, lastIE, lastDN);
					lastIE = scene.getIeType();
					lastDN = scene.getDnType();
					info.scene.setScriptElement(getLocation(info.location));
					if (scene.getPageNumber() != null && scene.getPageNumber() > lastPage) {
						// new last page #
						lastPage = scene.getPageNumber();
					}
					break;
				case TYPE_SCENE_LENGTH:
					if (info != null) {
						finishScene(info, scene, sex.sceneLength, scriptElements);
						totalSceneLength += sex.sceneLength;
					}
					info = null; // indicate we finished scene
					break;
				default:
					break;
			}
		} while(!sex.eof);

		if (info != null) { // last scene needs to be pushed
			finishScene(info, scene, 0, scriptElements);
		}
		if (lastPage == 1 && totalSceneLength > 8) {
			// probably no page numbers in file, but had scene length records
			lastPage = totalSceneLength / 8; // calculate approximate last page from scene lengths
		}

		try {
			dstream.close();
		}
		catch (IOException ex) {
			userExceptionMessage("IOException", ex);
			bRet = false;
		}

		script.setLastPage(lastPage);

		return bRet;
	}

	/**
	 * Find the ScriptElement matching 'name', or create a new one if necessary.
	 * Add it the to running set of script elements for the current scene.
	 *
	 * @param subtype The element type, as defined in the sex file.
	 */
	private boolean processElement(Set<ScriptElement> set, String name, ScriptElementType subtype) {
		if (name == null)
			return false;
		if (subtype == ScriptElementType.CHARACTER) {
			getCharacterElement(name, set);
		}
		else {
			name = name.toUpperCase().trim();
			if (name.length() > 0) {
				ScriptElement se = getScriptElement(name, subtype);
				if (se != null) {
					set.add(se);
				}
			}
		}
		return true;
	}

	/**
	 * Create a SceneInfo object for the scene, where "text" is the contents of
	 * the "start scene" record in the .sex file. The typical format of the text
	 * is "<page number>\t<scene number>\t<scene header>", although some files
	 * do not have page numbers in this text string.
	 * @param totalSceneLen
	 */
	private SceneInfo getSceneInfo(Scene scene, String text, int totalSceneLen) {
		SceneInfo info = new SceneInfo();
		info.scene = scene;
		int page = 1;
		text += TAB_CHARACTER; // fixes case where 'text' is just the page number, e.g., "1"
		int i = text.indexOf(TAB_CHARACTER); // look for first tab
		if (i >= 0) {
			String s = text.substring(0, i).trim();
			text = text.substring(i+1);
			try {
				page = Integer.parseInt(s);
			}
			catch (NumberFormatException ex) {
			}
		}
		else if (totalSceneLen > 0) {
			page = (totalSceneLen / 8) + 1;
		}
		if (page > 0) {
			scene.setPageNumber( page );
			scene.setPageNumStr(""+page);
		}

		i = text.indexOf(TAB_CHARACTER); // look for second tab
		if (i >= 0) {
			String sceneNum = text.substring(0, i).trim();
			text = text.substring(i+1);
			if (sceneNum.length() > 0) {
				scene.setNumber(sceneNum);
			}
		}
		info.heading = text.trim();
		return info;
	}

	/**
	 * Finish up the current scene & add to database.
	 *
	 * @param info The SceneInfo object previously created for this scene
	 * @param scene The Scene to be finished.
	 * @param sceneLen The scene length (in 1/8s of a page).
	 * @param scriptElements Any ScriptElement`s found for this Scene.
	 */
	private void finishScene(SceneInfo info, Scene scene, int sceneLen, Set<ScriptElement> scriptElements) {
		if (includeSceneElements) {
			scene.setScriptElements(scriptElements);
		}
		if (sceneLen < 1) {
			scene.setLength(1);
		}
		else {
			scene.setLength(sceneLen);
		}
		addScene(info);
	}

	/**
	 * Validate that the file begins with "SSI*", which appears to be the
	 * standard for .sex files.
	 */
	private boolean checkFileHeader() {
		String header = "";
		try {
			for (int i=0; i<4; i++) {
				byte c = dstream.readByte();
				header += (char)c;
			}
		}
		catch (IOException ex) {
		}
		//log("header = "+header);

		return header.equals("SSI*");
	}

	/**
	 * Read the next record from the current file.
	 */
	private SexRecord getRecord() {
		SexRecord sex = new SexRecord();
		byte[] bytes = new byte[1000];

		try {
			short s = dstream.readShort();
			if (s == 35) {
				sex.eof = false;
				sex.length = dstream.readInt();
				sex.type = dstream.readShort();
				sex.sceneCounter = dstream.readShort();

				switch (sex.type) {
					case TYPE_CATEGORIES:
						dstream.read(bytes, 0, sex.length-4);
						sex.text = new String(bytes, 0, sex.length-4);
						break;
					case TYPE_ELEMENT:
						short sub = dstream.readShort();
						dstream.read(bytes, 0, sex.length-6);
						sex.text = new String(bytes, 0, sex.length-6);
						sex.text = sex.text.trim();
						switch (sub) {
							case SUBTYPE_CHARACTER:
								sex.subtype = ScriptElementType.CHARACTER;
								break;
							case SUBTYPE_EXTRAS:
								sex.subtype = ScriptElementType.EXTRA;
								break;
							case SUBTYPE_STUNTS:
								sex.subtype = ScriptElementType.STUNT;
								break;
							case SUBTYPE_VEHICLES:
								sex.subtype = ScriptElementType.VEHICLE;
								break;
							case SUBTYPE_PROP:
								sex.subtype = ScriptElementType.PROP;
								break;
							case SUBTYPE_SFX:
								sex.subtype = ScriptElementType.SPECIAL_EFFECT;
								break;
							case SUBTYPE_WARDROBE:
								sex.subtype = ScriptElementType.WARDROBE;
								break;
							case SUBTYPE_MAKEUP:
								sex.subtype = ScriptElementType.MAKEUP_HAIR;
								break;
							case SUBTYPE_LIVESTOCK:
								sex.subtype = ScriptElementType.LIVESTOCK;
								break;
							case SUBTYPE_HANDLER:
								sex.subtype = ScriptElementType.ANIMAL;
								break;
							case SUBTYPE_MUSIC:
								sex.subtype = ScriptElementType.MUSIC;
								break;
							case SUBTYPE_SOUND:
								sex.subtype = ScriptElementType.SOUND;
								break;
							case SUBTYPE_DRESSING:
								sex.subtype = ScriptElementType.SET_DECORATION;
								break;
							case SUBTYPE_GREENERY:
								sex.subtype = ScriptElementType.GREENERY;
								break;
							case SUBTYPE_EQUIPMENT:
								sex.subtype = ScriptElementType.EQUIPMENT;
								break;
							case SUBTYPE_SECURITY:
								sex.subtype = ScriptElementType.SECURITY;
								break;
							case SUBTYPE_LABOR:
								sex.subtype = ScriptElementType.ADDITIONAL_LABOR;
								break;
							case SUBTYPE_OPT_FX:
								sex.subtype = ScriptElementType.OPTICAL_FX;
								break;
							case SUBTYPE_MECH_FX:
								sex.subtype = ScriptElementType.MECHANICAL_FX;
								break;
							case SUBTYPE_MISC:
								sex.subtype = ScriptElementType.MISC;
								break;
							case SUBTYPE_NOTES:
								sex.subtype = ScriptElementType.MISC;
								break;
							default:
								log.warn("unknown element subtype=" + sub + "=" + sex.text);
								sex.subtype = ScriptElementType.MISC;
								break;
						}
						break;
					case TYPE_SCENE:
						dstream.read(bytes, 0, sex.length-4);
						sex.text = new String(bytes, 0, sex.length-4);
						break;
					case TYPE_SCENE_LENGTH:
						sex.sceneLength = dstream.readShort()*8 + dstream.readShort();
						break;
					default:
						log.warn(".sex import: unexpected record type "+sex.type);
						userInfoMessage("ImportSex.UnknownRecordType",sex.type);
						dstream.read(bytes, 0, sex.length-4);
						sex.text = new String(bytes, 0, sex.length-4);
						log.warn("Record data: "+sex.toString());
						break;
				}
			}
			else if (s == 0x7fff) {
				sex.eof = true;
				//log("Normal end of file reached.");
			}
			else {
				sex.eof = true;
				log.warn(".sex import data error: unexpected record header: "+s);
				userInfoMessage("ImportSex.UnknownRecordHeader", s);
			}
		}
		catch (EOFException ex) {
			userInfoMessage("ImportSex.MissingEOF");
			sex.eof = true;
		}
		catch (IOException ex) {
			userExceptionMessage("IOException reading import file " + file, ex);
			sex.eof = true;
		}

		return sex;
	}

	/**
	 * Holds contents of one "record" from .sex file.  Note that not all fields
	 * apply to all record types.
	 */
	class SexRecord {
		protected boolean eof = false;
		protected int type = -1;	// scene, element, end-of-scene
		protected ScriptElementType subtype = null;	// character, prop, costume, etc.
		protected int length = 0;	// length of record (following length bytes)
		protected int sceneCounter = 0;	// sequential scene counter, starts at 1
		protected int sceneLength = 0;	// length of scene in 1/8ths
		protected String text = null;	// any text in record

		@Override
		public String toString() {
			String s;
			if (eof) {
				s = "End of file";
			}
			else if (type<0) {
				s = "Uninitialized";
			}
			else {
				s = "Type: "+type +
						", len: " + length +
						", scn#: " + sceneCounter +
						", scnLen: " + sceneLength +
						", text: '" + text + "'";
			}
			return s;
		}
	}

}
