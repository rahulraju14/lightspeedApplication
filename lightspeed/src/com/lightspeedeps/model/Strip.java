//	File Name:	Strip.java
package com.lightspeedeps.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.*;

import com.lightspeedeps.type.StripStatus;
import com.lightspeedeps.type.StripType;

/**
 * Strip entity. Corresponds to a strip on the stripboard as well as the data
 * presented on the Breakdown page. Each Stripboard has its own distinct set of
 * Strip`s.
 * <p>
 * Initially one Strip is created for every scene in a Script. Use of the
 * Breakdown page Merge command, however, allows one Strip to represent more
 * than one Scene within the Stripboard. Note that the relationship of Strip to
 * scene is by scene number -- the Strip is NOT linked (e.g., by a foreign key)
 * to a specific Scene object, but rather to whichever Scene object in the
 * current Script has the matching scene number. If a user switches the current
 * Script, then the Strips will be logically associated with different Scene
 * objects.
 * <p>
 * Strips associated with scenes are of StripType BREAKDOWN. There are also
 * Strips created via user actions that represent the end of a shooting day
 * (END_OF_DAY) or comments embedded in the Stripboard (BANNER). There is also a
 * special END_OF_STRIPBOARD Strip, created by the app, marking the end of each
 * Scheduled and Unscheduled list of Strips.
 * <p>
 * Strips are ordered in the Stripboard view based on their status and their
 * orderNumber.
 */
@Entity
@Table(name = "strip")
public class Strip extends PersistentObject<Strip> implements Cloneable {

	//private static final Log LOG = LogFactory.getLog(Strip.class);

	private static final long serialVersionUID = 1186400778225541083L;

	// Fields

	/** The Stripboard that "owns" this Strip. */
	private Stripboard stripboard;

	/** For a SCHEDULED Strip, the database id of the Unit with which the Strip
	 * is associated. This will be NULL for a Strip that is not SCHEDULED.  Note that
	 * in "exported" Strip`s, this field is the Unit number, not database id. */
	private Integer unitId;

//	private static final String DTYPE_BREAKDOWN = "BK";
//	private static final String DTYPE_STRIP = "ST";
//
//	/** This field was originally supposed to be used by Hibernate for sub-class differentiation,
//	 * but that plan was dropped.  It is not currently used. */
//	private String dtype = DTYPE_BREAKDOWN;

	/** The order in which this Strip appears in either the scheduled or unscheduled list. The order
	 * within the unscheduled list only affects the stripboard viewer and editor displays; the order
	 * within the scheduled list affects many other displays and reports, as it determines which
	 * scenes will be shot on a particular day. */
	private Integer orderNumber;

	/** The type of Strip - Breakdown, end-of-day, or banner. Breakdown strips represent Scenes to be
	 * shot; end-of-day strips separate one day's shooting from the next; and banner strips are
	 * simply a way to add comments to the stripboard. */
	private StripType type = StripType.BREAKDOWN;

	/** The status of the Strip: scheduled, unscheduled, or omitted. */
	private StripStatus status = StripStatus.UNSCHEDULED;

	/** This field is currently only used for Banner strips, and is the text displayed in the banner.*/
	private String comment;

	/** The breakdown page (sheet, strip) number, assigned (sequentially) when the Strip is created. */
	private Integer sheetNumber;

	/** A list of scene numbers, separated by commas, of the Scenes represented by this Strip. */
	private String sceneNumbers;

	/** The total page length (in eighth's) of all the Scenes represented by this Strip. */
	private Integer length;

//	/** A textual summary of the scene(s) for this Strip.  This field is filled in automatically
//	 * if a script is imported with its text included, by using the first portion of the "Action"
//	 * text from each Scene.  In any case, it may be entered and edited on the Breakdown Edit page. */
//	private String synopsis;

	/** A textual summary common to a group of Scenes or Strips that could be used to sort or
	 * otherwise group Strips.  UNUSED in V1 (4/2010). */
	private String sequence;

	/** The total planned time for shooting the Scene(s) represented by this Strip.  This
	 * value is only set by manual entering it on the Breakdown Edit page. */
	private Integer elapsedTime;

	/** A collection of Notes applicable to this Strip, entered on the Breakdown View or Edit pages. */
	private List<Note> notes = new ArrayList<>(0);

	//@Transient
	//private List<Note> noteList = null;

	/**
	 * The first (or only) Scene represented by this Strip. The connection is established based on
	 * the project's current Script. Having the Transient field improves performance by not having
	 * to recalculate this relation (which requires database access) as frequently.
	 */
	@Transient
	private Scene scene;

	/** A transient flag, for breakdown page, to track which Strip in the "scene list" is
	 * currently selected. */
	@Transient
	private boolean selected = false;

	// Constructors

	/** default constructor */
	public Strip() {
	}

	/** minimal constructor */
	public Strip(Integer orderNumber, StripType type, StripStatus status) {
		this.orderNumber = orderNumber;
		setType(type); // will also set dType
		this.status = status;
	}

	public Strip(Integer orderNumber, StripType type, StripStatus status, Stripboard board, Unit pUnit) {
		this(orderNumber, type, status, board);
		unitId = pUnit.getId();
	}

	/** common constructor */
	public Strip(Integer orderNumber, StripType type, StripStatus status, Stripboard board) {
		this(orderNumber, type, status);
		stripboard = board;
	}

	/** full constructor */
/*	public Strip(Stripboard stripboard, String dtype, Integer orderNumber,
			StripType type, StripStatus status, String comment, Integer sheetNumber,
			String sceneNumbers, Integer length, String synopsis,
			String sequence, Integer elapsedTime, Set<Note> notes) {
		this.stripboard = stripboard;
		this.dtype = dtype;
		this.orderNumber = orderNumber;
		this.type = type;
		this.status = status;
		this.comment = comment;
		this.sheetNumber = sheetNumber;
		this.sceneNumbers = sceneNumbers;
		this.length = length;
		this.synopsis = synopsis;
		this.sequence = sequence;
		this.elapsedTime = elapsedTime;
		this.notes = notes;
	}
*/
	// Property accessors

	/** See {@link #stripboard}*/
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Stripboard_Id")
	public Stripboard getStripboard() {
		return stripboard;
	}
	/** See {@link #stripboard}*/
	public void setStripboard(Stripboard stripboard) {
		this.stripboard = stripboard;
	}

	/** See {@link #unitId}. */
	@Column(name = "Unit_Id")
	public Integer getUnitId() {
		return unitId;
	}
	/** See {@link #unitId}. */
	public void setUnitId(Integer unitId) {
		this.unitId = unitId;
	}

	/** See {@link #orderNumber}*/
	@Column(name = "OrderNumber", nullable = false)
	public Integer getOrderNumber() {
		return orderNumber;
	}
	/** See {@link #orderNumber}*/
	public void setOrderNumber(Integer orderNumber) {
		this.orderNumber = orderNumber;
	}

	/** See {@link #type}*/
	@Enumerated(EnumType.STRING)
	@Column(name = "Type", nullable = false, length = 30)
	public StripType getType() {
		return type;
	}
	/** See {@link #type}*/
	public void setType(StripType t) {
		type = t;
	}

	/** See {@link #status}*/
	@Enumerated(EnumType.STRING)
	@Column(name = "Status", nullable = false, length = 30)
	public StripStatus getStatus() {
		return status;
	}
	/** See {@link #status}*/
	public void setStatus(StripStatus status) {
		this.status = status;
	}

	/** See {@link #comment}*/
	@Column(name = "Comment", length = 200)
	public String getComment() {
		return comment;
	}
	/** See {@link #comment}*/
	public void setComment(String comment) {
		this.comment = comment;
	}

	/** See {@link #sheetNumber}*/
	@Column(name = "Sheet_Number")
	public Integer getSheetNumber() {
		return sheetNumber;
	}
	/** See {@link #sheetNumber}*/
	public void setSheetNumber(Integer sheetNumber) {
		this.sheetNumber = sheetNumber;
	}

	/** See {@link #sceneNumbers}*/
	@Column(name = "Scene_Numbers", length = 100)
	public String getSceneNumbers() {
		return sceneNumbers;
	}
	/** See {@link #sceneNumbers}*/
	public void setSceneNumbers(String sceneNumbers) {
		this.sceneNumbers = sceneNumbers;
	}

	/** See {@link #length}*/
	@Column(name = "Length")
	public Integer getLength() {
		return length;
	}
	/** See {@link #length}*/
	public void setLength(Integer length) {
		this.length = length;
	}

//	/** See {@link #synopsis}*/
//	@Column(name = "Synopsis", length = 200)
//	public String getSynopsis() {
//		return this.synopsis;
//	}
//	/** See {@link #synopsis}*/
//	public void setSynopsis(String synopsis) {
//		this.synopsis = synopsis;
//	}

	/** See {@link #sequence}*/
	@Column(name = "Sequence", length = 100)
	public String getSequence() {
		return sequence;
	}

	/** See {@link #sequence}*/
	public void setSequence(String sequence) {
		this.sequence = sequence;
	}

	/** See {@link #elapsedTime}*/
	@Column(name = "Elapsed_Time")
	public Integer getElapsedTime() {
		return elapsedTime;
	}

	/** See {@link #elapsedTime}*/
	public void setElapsedTime(Integer elapsedTime) {
		this.elapsedTime = elapsedTime;
	}

	/** See {@link #notes}*/
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "strip")
	@OrderBy("time DESC")
	public List<Note> getNotes() { // TODO change to List, fetch=LAZY & add OrderBy to replace getNoteList()
		return notes;
	}
	/** See {@link #notes}*/
	public void setNotes(List<Note> notes) {
		this.notes = notes;
		//noteList = null; // force refresh of list
	}

	/**
	 * Returns a list of Notes sorted in DESCENDING time order.
	 */
	@Transient
	public List<Note> getNoteList() {
//		if (noteList == null) {
//			noteList = new ArrayList<Note>(notes);
//			Collections.sort(noteList, Collections.reverseOrder());
//		}
		return getNotes();
	}

	/**
	 * Return the number of Note`s for this Strip.  Used in JSP.
	 */
	@Transient
	public int getNoteCount() {
		Collection<Note> cnotes;
		if ((cnotes = getNotes()) != null) {
			return cnotes.size();
		}
		return 0;
	}

	/** See {@link #scene}*/
	@Transient
	public Scene getScene() {
		return scene;
	}
	/** See {@link #scene}*/
	public void setScene(Scene s) {
		scene = s;
	}

	@Transient
	public boolean getSelected() {
		return selected;
	}
	public void setSelected(boolean b) {
		selected = b;
	}

	@Transient
	public boolean getHasMultipleScenes() {
		return getSceneNumbers() != null && getSceneNumbers().indexOf(',') >= 0;
	}

	/**
	 * Get the scene numbers for this breakdown strip as a List<String>.
	 * Note that getSceneNumbers() returns the entire list of scene numbers
	 * as a single comma-delimited String.
	 * @return A List<String>, with each scene number trimmed.  Never returns
	 * null.  An empty list could be returned if getSceneNumbers() is empty,
	 * but that is not supposed to happen!
	 */
	@Transient
	public List<String> getScenes() {
		List<String> list = new ArrayList<>();
		String scenes = getSceneNumbers();
		if (scenes != null && scenes.trim().length() > 0) {
			String[] strings = scenes.split(",");
			for (int i=0; i < strings.length; i++) {
				list.add(strings[i].trim());
			}
		}
		return list;
	}

	/**
	 * Get the scene numbers for this breakdown strip as a List<String> by one increment
	 * Note that getSceneNumbers() returns the entire list of scene numbers
	 * as a single comma-delimited String.
	 * @return A List<String>, with each scene number trimmed.  Never returns
	 * null.  An empty list could be returned if getSceneNumbers() is empty,
	 * but that is not supposed to happen!
	 */
/*	@Transient
	public String getScenesIncremented() {
		String scenes = getSceneNumbers();
		if (scenes != null && scenes.trim().length() > 0) {
			StringBuffer sceneNumbers=new StringBuffer();
			String[] strings = scenes.split(",");
			for (int i=0; i < strings.length; i++) {
				String val=strings[i];
				int incrementedVal=Integer.parseInt(val)+1;
				if (i==strings.length-1) {
					sceneNumbers.append(incrementedVal);

				}else
				{
					sceneNumbers.append(incrementedVal+",");

				}

			}return sceneNumbers.toString();
		}
		return sceneNumbers.toString();
	}
*/
	@Override
	public Object clone() {
		try {
			Strip copy = (Strip)super.clone();
			copy.id = null; // it's a transient object
			copy.notes = new ArrayList<>(notes.size());
			for ( Note note : notes) {
				Note newnote = (Note)note.clone();
				newnote.setStrip(copy);
				copy.notes.add(newnote);
			}
			return copy;
		}
		catch (CloneNotSupportedException e) { // should not happen
			return null;
		}
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "id=" + (getId()==null ? "null" : id);
		s += ", sheet#=" + (getSheetNumber()==null ? "null" : getSheetNumber());
		s += "]";
		return s;
	}

}