package com.lightspeedeps.object;

import java.io.Serializable;
import java.util.List;

import com.lightspeedeps.model.Scene;
import com.lightspeedeps.type.DayNightType;
import com.lightspeedeps.type.IntExtType;
import com.lightspeedeps.type.ScriptElementType;

/**
 * A representation of one Scene in a Script, used to build the
 * display lists for the Script Review and Script Final Review
 * pages.
 */
public class SceneListItem implements Serializable {

	private static final long serialVersionUID = -1976799086425486683L;

	/** Scene.sequence, which determines the order of the Scene in the Script. */
	private int orderNumber;

	/** Scene.scriptDay - the user-defined description of when this Scene occurs. */
	private String dayNumber;

	/** Strip.sheetNumber, a sequential number assigned to Strips which is no longer important. */
	private String sheetNumber;

	/** Scene.number, the alpha-numeric Scene number assigned by the user. */
	private String sceneNumbers;

	/** The length of the Scene, in 1/8ths. */
	private int length;

	/** The alphabetic portion of the scene number, either prefix,
	 * suffix, or both.  May be an empty String, but never  null*/
	private String sceneAlpha;

	/** The Scene.ieType field - the interior/exterior scene type */
	private IntExtType intEType;

	/** The Scene.dnType field - the Day/Night setting for the Scene. */
	private DayNightType dayNType;

	/** the id of the scene's location ScriptElement */
	private int locationId;

	/** the Name of the scene's location ScriptElement */
	private String title;

	/** */
	private int rowNumber;

	/** Scene.id of the corresponding Scene object */
	private int sceneId;

	/** True iff the Scene's ScriptElement data should be displayed, i.e., the
	 * user has clicked on the "expand" control for this Scene. */
	private boolean showData=false;

	/** */
	private int columnCount;

	/** The complete heading line for the Scene, including int/ext, set, and day/night
	 * information. */
	private String heading;

	/** The Scene object this item describes. */
	private Scene scene;

	/** True iff this row is in "edit mode", that is, the fields are displayed
	 * as input fields instead of output-only. */
	private boolean edit;

	/** */
	private final String[] columns = new String[ScriptElementType.ELEMENT_TABLE_SIZE];

	/** Array of Lists of ScriptElement.name values - the names of the ScriptElements
	 * associated with this scene, separated by ScriptElementType.  Each List corresponds
	 * to one type. */
	@SuppressWarnings("unchecked")
	private final List<String>[] arrays = new List[ScriptElementType.ELEMENT_TABLE_SIZE];

	/**
	 * Default constructor.
	 */
	public SceneListItem() {
	}

	/** See {@link #showData}. */
	public boolean getShowData() {
		return showData;
	}
	/** See {@link #showData}. */
	public void setShowData(boolean showData) {
		this.showData = showData;
	}

	/** See {@link #title}. */
	public String getTitle() {
		return title;
	}
	/** See {@link #title}. */
	public void setTitle(String title) {
		this.title = title;
	}

	/** See {@link #dayNType}. */
	public DayNightType getDayNType() {
		return dayNType;
	}
	/** See {@link #dayNType}. */
	public void setDayNType(DayNightType dayNType) {
		this.dayNType = dayNType;
	}

	/** See {@link #intEType}. */
	public IntExtType getIntEType() {
		return intEType;
	}
	/** See {@link #intEType}. */
	public void setIntEType(IntExtType intEType) {
		this.intEType = intEType;
	}

	/** Get the formatted scene length as "m n/8" */
	public String getPageLength() {
		return Scene.formatPageLength(getLength());
	}

	/**
	 * Set the scene length from a formatted value.
	 * @param s The formatted page length, as "m n/8".
	 */
	public void setPageLength(String s) {
		int n = Scene.convertPageLength(s);
		if (n > 0) {
			setLength(n);
		}
	}

	/** See {@link #orderNumber}. */
	public int getOrderNumber() {
		return orderNumber;
	}
	/** See {@link #orderNumber}. */
	public void setOrderNumber(int orderNumber) {
		this.orderNumber = orderNumber;
	}

	/** See {@link #dayNumber}. */
	public String getDayNumber() {
		return dayNumber;
	}
	/** See {@link #dayNumber}. */
	public void setDayNumber(String dayNumber) {
		this.dayNumber = dayNumber;
	}

	/** See {@link #sheetNumber}. */
	public String getSheetNumber() {
		return sheetNumber;
	}
	/** See {@link #sheetNumber}. */
	public void setSheetNumber(String sheetNumber) {
		this.sheetNumber = sheetNumber;
	}

	/** See {@link #sceneNumbers}. */
	public String getSceneNumbers() {
		return sceneNumbers;
	}
	/** See {@link #sceneNumbers}. */
	public void setSceneNumbers(String sceneNumbers) {
		this.sceneNumbers = sceneNumbers;
	}

	/** See {@link #length}. */
	public int getLength() {
		return length;
	}
	/** See {@link #length}. */
	public void setLength(int length) {
		this.length = length;
	}

	/** See {@link #rowNumber}. */
	public int getRowNumber() {
		return rowNumber;
	}
	/** See {@link #rowNumber}. */
	public void setRowNumber(int rowNumber) {
		this.rowNumber = rowNumber;
	}

	/** See {@link #columnCount}. */
	public int getColumnCount() {
		return columnCount;
	}
	/** See {@link #columnCount}. */
	public void setColumnCount(int columnCount) {
		this.columnCount = columnCount;
	}

	/** See {@link #sceneAlpha}. */
	public String getSceneAlpha() {
		return sceneAlpha;
	}
	/** See {@link #sceneAlpha}. */
	public void setSceneAlpha(String sceneAlpha) {
		this.sceneAlpha = sceneAlpha;
	}

	/** See {@link #sceneId}. */
	public int getSceneId() {
		return sceneId;
	}
	/** See {@link #sceneId}. */
	public void setSceneId(int sceneId) {
		this.sceneId = sceneId;
	}

	/** See {@link #columns}. */
	public String[] getColumns() {
		return columns;
	}

	/**
	 * @param index the column number to set
	 * @param s the value to set
	 */
	public void setColumn(int index, String s) {
		columns[index] = s;
	}

	public List<String>[] getArrays() {
		return arrays;
	}

	/**
	 * @param index the entry to set
	 * @param list the List of names to put in that entry
	 */
	public void setArray(int index, List<String> list) {
		arrays[index] = list;
	}

	/** See {@link #heading}. */
	public String getHeading() {
		return heading;
	}
	/** See {@link #heading}. */
	public void setHeading(String heading) {
		this.heading = heading;
	}

	/** See {@link #scene}. */
	public Scene getScene() {
		return scene;
	}
	/** See {@link #scene}. */
	public void setScene(Scene scene) {
		this.scene = scene;
	}

	/** See {@link #locationId}. */
	public int getLocationId() {
		return locationId;
	}
	/** See {@link #locationId}. */
	public void setLocationId(int locationId) {
		this.locationId = locationId;
	}

	/** See {@link #edit}. */
	public boolean getEdit() {
		return edit;
	}
	/** See {@link #edit}. */
	public void setEdit(boolean edit) {
		this.edit = edit;
	}

}
