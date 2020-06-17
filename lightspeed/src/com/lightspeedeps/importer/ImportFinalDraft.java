//	File Name:	ImportFinalDraft.java
package com.lightspeedeps.importer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.*;
import org.jdom.input.*;

import com.lightspeedeps.dao.TextElementDAO;
import com.lightspeedeps.model.ScriptElement;
import com.lightspeedeps.type.ScriptElementType;

import java.io.*;
import java.util.*;


/**
 * A superclass for importing scripts saved in various Final Draft formats.
 * It contains fields and methods common to the FDX and Tagger import classes.
 */
public abstract class ImportFinalDraft extends ImportFileImpl {
	private static final Log log = LogFactory.getLog(ImportFinalDraft.class);

	protected final static Namespace emptyNs = Namespace.NO_NAMESPACE;

	protected final static int FILTER_NONE = 0;
	protected final static int FILTER_TAGGER = 1;
	protected final static int FILTER_FDX = 2;

	protected transient TextElementDAO textElementDAO;

	/**
	 * The XML document, as loaded by the SAX (JDOM) parser.
	 */
	protected org.jdom.Document document = null;

	/**
	 * This is a mapping from Tagger's ElementID values to our ScriptElement
	 * objects.  We create it when we process the "ElementList" tag within
	 * the Tagger file.
	 */
	protected Map<String, ScriptElement> elementMap = new HashMap<String, ScriptElement>();

	// default values for category IDs in Tagger file <Element> items
	// These are the same in Tagger v7 and v8.
	protected static final int id_cast = 0;
	protected static final int id_extra = 1;
	protected static final int id_stunt = 2;
	protected static final int id_vehicle = 3;
	protected static final int id_prop = 4;
	protected static final int id_sfx = 5;
	protected static final int id_costume = 6;
	protected static final int id_makeup = 7;
	protected static final int id_animal = 8;
	protected static final int id_handler = 9;
	protected static final int id_music = 10;
	protected static final int id_sound = 11;
	protected static final int id_dressing = 12;
	protected static final int id_greenery = 13;
	protected static final int id_equipment = 14;
	protected static final int id_security = 15;
	protected static final int id_labor = 16;
	protected static final int id_optical_fx = 17;
	protected static final int id_mech_fx = 18;
	protected static final int id_misc = 19;
	protected static final int id_notes = 20;

	public ImportFinalDraft() {
	}

	/**
	 * Note that the import beans are re-used, so fields must be re-initialized
	 * for each import.
	 */
	protected void init() {
	}

	/**
	 * Process the Tagger file "ElementList" tag, which is (essentially) a list
	 * of objects we call ScriptElements. This creates a map of Tagger
	 * elementId's to ScriptElements, and creates new ScriptElements as
	 * necessary.
	 * <p>
	 * See getSceneElements() for how this map is later used.
	 * <p>
	 * Some sample scripts have had elements which are identical except for
	 * leading or trailing blanks. We trim the names, so these end up as
	 * identical for our purposes. In this case, there will be 2 map entries,
	 * one for each tagger id, referencing the same ScriptElement object. (Not a
	 * problem, just noteworthy.)
	 *
	 * @param elemList The List of (JDOM) Element nodes containing all the
	 *            ScriptElements referred to in the Tagger or FDX file.
	 * @param fdx True if the file is .fdx format, false if it is Tagger (XML)
	 *            format. The structure of the Element nodes is slightly
	 *            different between the two files!
	 *
	 * @return true. Also, instance fields {@link #elementMap} and
	 *         {@link #characterMap} have been updated.
	 */
	protected boolean processScriptElements(List<Element> elemList, boolean fdx) {
		String name = null;
		String strId = null;
		String strCat = null;
		for (Element el : elemList) {
			if (fdx) {	// In FDX, the Text, ElementId, and Category are attributes
				name = el.getAttributeValue("Text");
				if (name != null) {
					strId = el.getAttributeValue("ElementID"); // name is different than Tagger
					strCat = el.getAttributeValue("Category");
				}
			}
			else {	// In Tagger, the Text, Id, and Category are child nodes
				name = el.getChildText("Text");
				if (name != null) {
					strId = el.getChildTextTrim("ID"); // name is different than fdx
					strCat = el.getChildTextTrim("Category");
				}
			}
			if (name != null) {
				name = name.trim().replace("  ", " ");	// doubled spaces seen in some import files
				ScriptElementType eType = convertCatToElemType(strCat);
				if (name.length() > 0 && eType != ScriptElementType.N_A) {
					// (some samples had elements with empty names - ignore these.)
					// Find matching ScriptElement in db, or create a new one:
					name = name.toUpperCase();
					ScriptElement se = getScriptElement(name, eType);
					// Save ScriptElement for adding to Scene's when those are processed...
					if (se != null) {
						elementMap.put(strId, se);
						if (eType == ScriptElementType.CHARACTER) {
							characterMap.put(name, se);
						}
					}
				}
			}
		}
		return true;
	}

	protected List<Element> getSubList(Element root, String topName, String subName) {
		return getSubList(root, topName, subName, emptyNs);
	}

	/**
	 * Retrieves all the child elements of the "topname" node which are
	 * of type "subName".  In the Tagger files, this is generally all the
	 * children of the node: the file is essentially a set of several lists of
	 * various element types.
	 */
	@SuppressWarnings("unchecked")
	protected List<Element> getSubList(Element root, String topName, String subName, Namespace ns) {

		List<Element> elemList = (List<Element>)root.getChildren(topName, ns);
		//log("List size = " + (elemList == null ? "null" : "" + elemList.size()));
		if (elemList != null && elemList.size() >= 1) {
			Element el = (Element) elemList.get(0);
			elemList = (List<Element>)el.getChildren(subName,ns);
			//log(subName + " item count = " + (elemList==null ? "null" : elemList.size()));
		}
		return elemList;
	}

	/**
	 * Load the XML file "filename" into our document using SAXBuilder.
	 */
	protected boolean loadDocument(String file, int filter) throws FileNotFoundException {
		boolean expandEntities = true;
		boolean bRet = false;
//		String saxDriverClass = null;
		FileInputStream stream = null;

		try {
			SAXBuilder builder = null;
//			if (saxDriverClass == null) {
				builder = new SAXBuilder();
//			}
//			else {
//				builder = new SAXBuilder(saxDriverClass);
//			}
			builder.setExpandEntities(expandEntities);
			//builder.setIgnoringBoundaryWhitespace(true);

			stream = new FileInputStream(file);
			if (filter == FILTER_FDX) {
				TaggerFilter fileFilter = new FdxFilter(stream);
				document = builder.build(fileFilter);
			}
			else if (filter == FILTER_TAGGER) {
				TaggerFilter fileFilter = new TaggerFilter(stream);
				document = builder.build(fileFilter);
			}
			else {
				document = builder.build(stream);
			}
			bRet = true;
			/*
			 XMLOutputter output = new XMLOutputter(Format.getPrettyFormat());
			 output.output(doc, System.out);
			 */
		}
		catch (JDOMParseException e) {
			userExceptionMessage("JDOMParseException", e);
		}
		catch (JDOMException e) {
			userExceptionMessage("JDOMException", e);
		}
		catch (FileNotFoundException e) {
			log.debug("file not found exception for file="+file);
			throw e;
		}
		catch (IOException e) {
			userExceptionMessage("IOException", e);
		}
		catch (Exception e) {
			userExceptionMessage("Exception", e);
		}
		finally {
			if (stream != null) {
				try {
					stream.close();
				}
				catch (IOException e) {}
			}
		}

		return bRet;
	}

	/**
	 * Convert a Tagger category type to an LS ScriptElementType.
	 */
	private ScriptElementType convertCatToElemType(String strCat) {
		int cat = -1;
		ScriptElementType eType = ScriptElementType.N_A;
		try {
			cat = Integer.parseInt(strCat);
		}
		catch (Exception e) {
		};
		switch (cat) {
			case id_cast :
				eType = ScriptElementType.CHARACTER;
				break;
			case id_extra :
				eType = ScriptElementType.EXTRA;
				break;
			case id_stunt :
				eType = ScriptElementType.STUNT;
				break;
			case id_vehicle :
				eType = ScriptElementType.VEHICLE;
				break;
			case id_prop :
				eType = ScriptElementType.PROP;
				break;
			case id_costume :
				eType = ScriptElementType.WARDROBE;
				break;
			case id_makeup :
				eType = ScriptElementType.MAKEUP_HAIR;
				break;
			case id_animal :
				eType = ScriptElementType.LIVESTOCK;
				break;
			case id_handler : // Animal handlers
				eType = ScriptElementType.ANIMAL;
				break;
			case id_equipment :
				eType = ScriptElementType.EQUIPMENT;
				break;
			case id_dressing :
				eType = ScriptElementType.SET_DECORATION;
				break;
			case id_greenery :
				eType = ScriptElementType.GREENERY;
				break;
			case id_music :
				eType = ScriptElementType.MUSIC;
				break;
			case id_sound :
				eType = ScriptElementType.SOUND;
				break;
			case id_sfx :
				eType = ScriptElementType.SPECIAL_EFFECT;
				break;
			case id_optical_fx :
				eType = ScriptElementType.OPTICAL_FX;
				break;
			case id_mech_fx :
				eType = ScriptElementType.MECHANICAL_FX;
				break;
			case id_labor :
				eType = ScriptElementType.ADDITIONAL_LABOR;
				break;
			case id_security :
				eType = ScriptElementType.SECURITY;
				break;
			case id_notes :
			case id_misc :
				eType = ScriptElementType.MISC;
				break;
			default :
				eType = ScriptElementType.MISC;
				//eType = ScriptElementType.N_A;
				break;
		}
		return eType;
	}

	/**
	 * Output any final debugging info, typically corresponding to the overall
	 * import process, but unique to the Tagger import.
	 */
	protected void finalDebug() {
		if (log.isDebugEnabled()) {
			log.debug("Character line counts:");
			for (String s : characterMap.keySet()) {
				ScriptElement se = characterMap.get(s);
				if (se != null) {
					log.debug(s + ":  " + se.getLineCount());
				}
			}
		}
	}

	/** See {@link #textElementDAO}. */
	public TextElementDAO getTextElementDAO() {
		if (textElementDAO == null) {
			textElementDAO = TextElementDAO.getInstance();
		}
		return textElementDAO;
	}

}
