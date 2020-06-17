package com.lightspeedeps.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.*;

import org.icefaces.ace.util.IceOutputResource;

import com.lightspeedeps.dao.ProjectDAO;
import com.lightspeedeps.dood.ElementDood;
import com.lightspeedeps.dood.ProductionDood;
import com.lightspeedeps.port.TaggedExporter;
import com.lightspeedeps.type.ScriptElementType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.common.ImageUtils;
import com.lightspeedeps.util.common.NumberUtils;

/**
 * ScriptElement entity. This represents a "virtual" object that appears within
 * a Scene in a Script. It can be a character, a prop, a vehicle, etc. The
 * 'type' field, of class ScriptElementType, describes the type of object it
 * represents. A ScriptElement belongs to a particular Project, but is shared
 * across all Scenes and Scripts within that Project. Within a project, the
 * combination of (type + name) must be unique.
 */
@Entity
@Table(name = "script_element", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "Project_Id", "Type", "Name" }),
		@UniqueConstraint(columnNames = { "Project_Id", "Type", "Element_Id" }) })
public class ScriptElement extends PersistentObject<ScriptElement> implements Comparable<ScriptElement>, Cloneable {

	//private static final Log LOG = LogFactory.getLog(ScriptElement.class);

	private static final long serialVersionUID = -1398149361633147458L;

	private static final String SORTKEY_NAME = "Name";
	public static final String SORTKEY_ID = "ID";
	private static final String SORTKEY_STATUS = "status";

	/** The numeric part of a cast-id is multiplied by this when creating the
	 * elementId value. */
	public final static int CASTID_NUMBER_FACTOR = 1000;
	/** The converted alpha prefix part of a cast-id is multiplied by this when
	 * creating the elementId value. */
	public final static int CASTID_PREFIX_FACTOR = 10000000;

	/** The validation pattern for element_id (cast-id) values. */
	private static final Pattern ID_PATTERN = Pattern.compile("([a-z]?)([0-9]{1,4})([a-z]{0,2})");

	// Fields

	/** This is the "responsible party" for acquiring or otherwise dealing with the script element. */
	private Contact contact;

	/** The Project that this element belongs to. May not be null. */
	private Project project;

	/** This is the primary identifier of this object from the user's
	 * perspective. It must be unique within this element's Project. By industry
	 * convention, these names are displayed in all upper case. */
	private String name;

	/** This is an identifying string, primarily used for Character types,
	 * usually referred to as "cast id", with lower values indicating higher
	 * importance and/or increased frequency of appearance (and speaking) in the
	 * script. It is typically all numeric, but in some cases a single leading
	 * or trailing alphabetic character appears. It may be null. If not null, it
	 * must be unique across all ScriptElement`s within a Project. */
	private String elementIdStr;

	/** This is a unique number generated from the {@link #elementIdStr}, used for sorting the
	 * elements. It is null iff elementIdStr is null. */
	private Integer elementId;

	/** A user-supplied description of the element; may be null. */
	private String description;

	/** The element's type.  May not be null. */
	private ScriptElementType type = ScriptElementType.CHARACTER;

	/** True if there should be a RealWorldElement associated with this ScriptElement
	 * (via a RealLink).  This is set by the user based on script/business requirements. */
	private Boolean realElementRequired = false;

	/** True if {@link #realElementRequired} is true, and there exists a RealLink for
	 * this ScriptElement where the RealLink's status value is SELECTED.  This field
	 * is maintained by the application, in response to the user adding or removing
	 * "played-by" objects associated with this element, or changing the status of
	 * those associations. */
	private Boolean requirementSatisfied = false;

	/** If true, this ScriptElement is included on all Callsheets, even when the
	 * element is not associated with any scene being shot that day. */
	private Boolean includeOnCallsheet = false;

	/** True if this element uses Drop/Pickup/Hold settings for DooD. */
	private Boolean dropPickupAllowed = false;

	/** Minimum number of days off (held) before element may be dropped. */
	private Integer daysHeldBeforeDrop = Constants.DEFAULT_HELD_BEFORE_DROP;

	/** The starting date of the "held" (off) period during which this element
	 *  will be dropped instead. */
	private Date dropToUse;

	/** The Set of associations between this ScriptElement and zero or more
	 * RealWorldElement`s that will be used in the production to represent
	 * this element. */
	private Set<RealLink> realLinks = new HashSet<>(0);

	/** A transient List, generated from {@link #realLinks}, sorted in an order
	 * appropriate for presentation. */
	private List<RealLink> realLinkList = null;

	/** */
	private List<Image> images = new ArrayList<>(0);

	/** The Set of Scene`s which include this ScriptElement.  This does NOT
	 * include Scene`s using this element as a Set (if this is a LOCATION). */
	private Set<Scene> scenes = new HashSet<>(0);

	/** The Set of Scene`s which use this ScriptElement as a Set.  This
	 * will be null or empty if the type of this element is not LOCATION. */
	private Set<Scene> scenesForLocation = new HashSet<>(0);

	/** A transient List which is generated (if needed) from either the
	 * scenes or scenesForLocation Set, as appropriate, and then sorted. */
	private List<Scene> sceneList = null;

	/** The ScriptElement`s linked to this one that will get automatically
	 * added to a Scene whenever this element is added. */
	private Set<ScriptElement> childElements;

	/** Used to track row selection on List page. */
	@Transient
	private boolean selected;

	/** Used for controlling the "Delete" button on the Script Element list
	 * page.  True for non-Location elements, or for Location elements that
	 * are NOT referenced by any scene in ANY script in the related project.
	 * In other words, False for Location elements that ARE referenced by
	 * any scene in any script in the related project. */
	@Transient
	private Boolean okToDelete;

	/** List of Image resource generated from the list of images Required for Iceface 4.x upgrade.*/
	@Transient
	private transient List<IceOutputResource> imageResources;

	// Constructors

	/** default constructor */
	public ScriptElement() {
	}

	/** full constructor */
/*	public ScriptElement(Contact contact, Project project, String name,
			Integer elementId, String description, ScriptElementType type,
			Boolean realElementRequired, Boolean requirementSatisfied,
			Set<RealLink> realLinks, Set<Image> images, Set<Scene> scenes,
			Set<Scene> scenes_1) {
		this.contact = contact;
		this.project = project;
		this.name = name;
		this.elementId = elementId;
		this.description = description;
		this.type = type;
		this.realElementRequired = realElementRequired;
		this.requirementSatisfied = requirementSatisfied;
		this.realLinks = realLinks;
		this.images = images;
		this.scenes = scenes;
		//DH this.scenesForLocation = scenes_1;
	}
*/
	// Property accessors

	/** See {@link #contact}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Responsible_Party_Id")
	public Contact getContact() {
		//log.debug(contact);
		return contact;
	}
	/** See {@link #contact}. */
	public void setContact(Contact contact) {
		//log.debug(contact);
		this.contact = contact;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Project_Id", nullable = false)
	public Project getProject() {
		return project;
	}
	public void setProject(Project project) {
		this.project = project;
	}

	@Column(name = "Name", nullable = false, length = 100)
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	/** See {@link #elementIdStr}. */
	@Column(name = "Element_id_str", length = 10)
	public String getElementIdStr() {
		return elementIdStr;
	}
	/** See {@link #elementIdStr}. */
	public void setElementIdStr(String elementIdStr) {
		this.elementIdStr = elementIdStr;
	}

	/** See {@link #elementId}. */
	@Column(name = "Element_Id")
	public Integer getElementId() {
		return elementId;
	}
	/** See {@link #elementId}. */
	public void setElementId(Integer elementId) {
		this.elementId = elementId;
	}

	@Column(name = "Description", length = 1000)
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Type", nullable = false, length = 30)
	public ScriptElementType getType() {
		return type;
	}
	public void setType(ScriptElementType type) {
		this.type = type;
	}

	@Column(name = "Real_Element_Required", nullable = false)
	public Boolean getRealElementRequired() {
		return realElementRequired;
	}
	public void setRealElementRequired(Boolean realElementRequired) {
		this.realElementRequired = realElementRequired;
	}

	@Column(name = "Requirement_Satisfied", nullable = false)
	public Boolean getRequirementSatisfied() {
		return requirementSatisfied;
	}
	public void setRequirementSatisfied(Boolean requirementSatisfied) {
		this.requirementSatisfied = requirementSatisfied;
	}

	/**See {@link #includeOnCallsheet}. */
	@Column(name = "Include_On_Callsheet", nullable = false)
	public Boolean getIncludeOnCallsheet() {
		return includeOnCallsheet;
	}
	/**See {@link #includeOnCallsheet}. */
	public void setIncludeOnCallsheet(Boolean includeOnCallsheet) {
		this.includeOnCallsheet = includeOnCallsheet;
	}

	/** See {@link #dropPickupAllowed}. */
	@Column(name = "Drop_Pickup_Allowed", nullable = false)
	public Boolean getDropPickupAllowed() {
		return dropPickupAllowed;
	}
	/** See {@link #dropPickupAllowed}. */
	public void setDropPickupAllowed(Boolean dropPickupAllowed) {
		this.dropPickupAllowed = dropPickupAllowed;
	}

	/** See {@link #dropToUse}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "Drop_To_Use")
	public Date getDropToUse() {
		return dropToUse;
	}
	/** See {@link #dropToUse}. */
	public void setDropToUse(Date dropToUse) {
		this.dropToUse = dropToUse;
	}

	/** See {@link #daysHeldBeforeDrop}. */
	@Column(name = "Days_Held_Before_Drop")
	public Integer getDaysHeldBeforeDrop() {
		return daysHeldBeforeDrop;
	}
	/** See {@link #daysHeldBeforeDrop}. */
	public void setDaysHeldBeforeDrop(Integer daysHeldBeforeDrop) {
		this.daysHeldBeforeDrop = daysHeldBeforeDrop;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "scriptElement")
	public Set<RealLink> getRealLinks() { // changed to Lazy 2/26/11 rev 1682
		return realLinks;
	}
	public void setRealLinks(Set<RealLink> realLinks) {
		realLinkList = null;
		this.realLinks = realLinks;
	}

	@Transient
	public List<RealLink> getRealLinkList() {
		if (realLinkList == null) {
			realLinkList = new ArrayList<>(getRealLinks());
			Collections.sort(realLinkList);
		}
		return realLinkList;
	}
	/** See {@link #realLinkList}. */
	public void setRealLinkList(List<RealLink> realLinkList) {
		this.realLinkList = realLinkList;
	}

	// Changed to LAZY - 8/25/10 rev 878
	/** See {@link #images}. */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "scriptElement")
	@OrderBy("date")
	public List<Image> getImages() {
		return images;
	}
	/** See {@link #images}. */
	public void setImages(List<Image> images) {
		this.images = images;
	}

	/** See {@link #scenes}. */
	@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY,
		mappedBy = "scriptElements", // changed to Lazy 2/26/11 rev 1682
		targetEntity = Scene.class)
	public Set<Scene> getScenes() {
		return scenes;
	}
	/** See {@link #scenes}. */
	public void setScenes(Set<Scene> scenes) {
		this.scenes = scenes;
		sceneList = null;	// force refresh
	}

	/**
	 * Returns a sorted list of Scenes that use this ScriptElement, either as a
	 * referenced element, or as its "set", if this is a LOCATION element.
	 */
	@Transient
	public List<Scene> getSceneList() {
		if (sceneList == null) {
			if (getType() == ScriptElementType.LOCATION) {
				sceneList = new ArrayList<>(getScenesForLocation());
			}
			else {
				sceneList = new ArrayList<>(getScenes());
			}
			Collections.sort(sceneList);
		}
		return sceneList;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "scriptElement") // changed to Lazy 2/26/11 rev 1682
	public Set<Scene> getScenesForLocation() {
		return scenesForLocation;
	}
	public void setScenesForLocation(Set<Scene> scenes) {
		scenesForLocation = scenes;
	}

	/** See {@link #childElements}. */
	@ManyToMany( targetEntity=ScriptElement.class,
			cascade={CascadeType.PERSIST, CascadeType.MERGE} )
	@JoinTable( name = "child_element",
			joinColumns = {@JoinColumn(name = "PARENT_ID")},
			inverseJoinColumns = {@JoinColumn(name = "CHILD_ID")} )
	public Set<ScriptElement> getChildElements() {
		return childElements;
	}
	/** See {@link #childElements}. */
	public void setChildElements(Set<ScriptElement> childElements) {
		this.childElements = childElements;
	}

	@Transient
	public List<ScriptElement> getChildElementList() {
		if (getChildElements() == null) {
			return new ArrayList<>();
		}
		return new ArrayList<>(getChildElements());
	}

	/**
	 * @return The ElementDood maintained by the ProductionDood singleton,
	 * based on our project id and scriptElement id.
	 * Used on the Script Element List jsp page.
	 */
	@Transient
	public ElementDood getElementDood() {
		project = ProjectDAO.getInstance().refresh(project);
		return ProductionDood.getElementDood(project, id);
	}

	@Transient
	public ElementDood getElementDood(Unit unit) {
		return ProductionDood.getElementDood(unit, id);
	}

	@Transient
	/** Used by JSP input/output. */
	public String getElementIds() {
		return getElementIdStr();
	}
	/** Used to set BOTH the numeric (sort-key) value and the
	 * textual element-id (cast-id) value for this ScriptElement. */
	public void setElementIds(String str) {
		setElementIdStr(str);
		setElementId(convertIdString(str));
		if (getElementId() == null) {
			setElementIdStr(null);
		}
		else {
			setElementIdStr(str.trim());
		}
	}

	private static Integer convertIdString(String idStr) {
		if (idStr == null) {
			return null;
		}
		idStr = idStr.trim().toLowerCase();
		if (idStr.length() == 0) {
			return null;
		}
		int n = -1;
		Matcher match = ID_PATTERN.matcher(idStr);
		boolean bMatch = match.matches();
		if (bMatch) {
			String prefix = match.group(1);
			String numberToConvert = match.group(2);
			String suffix = match.group(3);
			try {
				n = Integer.parseInt(numberToConvert) * CASTID_NUMBER_FACTOR;
			}
			catch(Exception e) {}
			n += (convertAlpha(prefix) * CASTID_PREFIX_FACTOR);
			n += convertAlpha(suffix);
		}
		return new Integer(n);
	}

	public static boolean isValidIdString(String idStr) {
		boolean bMatch = false;
		if (idStr != null && idStr.length() > 0) {
			idStr = idStr.trim().toLowerCase();
			Matcher match = ID_PATTERN.matcher(idStr);
			bMatch = match.matches();
		}
		return bMatch;
	}

	private final static int alphaBase = Character.getNumericValue('a');

	private static int convertAlpha(String str) {
		int n = 0;
		if (str.length() > 0) {
			n = Character.getNumericValue(str.charAt(0)) - alphaBase + 1;
			if (str.length() > 1) {
				n *= 26;
				n += Character.getNumericValue(str.charAt(1)) - alphaBase + 1;
			}
		}
		return n;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		ScriptElement other;
		try {
			other = (ScriptElement)obj;
		}
		catch (Exception e) {
			return false;
		}
		if ( getId() != null && other.getId() != null ) {
			return getId().equals(other.getId());
		}
		// one of them is transient, use type/name comparison
		return (compareTo(other) == 0);
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getId() == null) ? 0 : id.hashCode());
		result = prime * result + ((getName() == null) ? 0 : name.hashCode());
		result = prime * result + ((getProject() == null) ? 0 :
				(project.getId() == null ? 0 : project.getId().hashCode()) );
		result = prime * result + ((getType() == null) ? 0 : type.hashCode());
		return result;
	}

	/**
	 * Compare two ScriptElement's based on type, name, and project.  The primary
	 * ordering is by type, which is based on the ScriptElementType ordinal value; the
	 * secondary ordering is by name (compared with 'ignore case'); if those are
	 * both equal, the ordering is by Project name.
	 * @see java.lang.Comparable#compareTo(Object)
	 */
	@Override
	public int compareTo(ScriptElement arg0) {
		//log.debug("this: "+ this + ", arg0: " + arg0);
		if (arg0 == null) {
			return 1;
		}
		int comp = getType().compareTo(arg0.getType());
		if (comp != 0) {
			return comp;
		}
		if (getName() == null) {
			if (arg0.getName() == null) {
				return 0;
			}
			else {
				return -1;
			}
		}
		comp = getName().compareToIgnoreCase(arg0.getName());
		if (comp == 0) {
			if (getProject() != null) {
				comp = getProject().compareTo(arg0.getProject());
			}
		}
		return comp;
	}

	public int compareTo(ScriptElement other, String sortColumnName, boolean ascending) {
		boolean sortByName = true;
		int ret = NumberUtils.compare(getType().ordinal(), other.getType().ordinal());
		if (ret == 0) {
			if (sortColumnName == null) {
				// fall thru to default name sort
			}
			else if (sortColumnName.equals(SORTKEY_ID)) {
				if (getElementId() == null) {
					ret = other.getElementId() == null ? 0 : -1;
				}
				else if (other.getElementId() == null) {
					ret = 1;
				}
				else {
					ret = getElementId().compareTo(other.getElementId());
				}
			}
			else if (sortColumnName.equals(SORTKEY_NAME)) {
				ret = getName().compareToIgnoreCase(other.getName());
				sortByName = false;
			}
			else if (sortColumnName.equals(SORTKEY_STATUS)) {
				int b1 = (getRealElementRequired() ? (getRequirementSatisfied() ? 0 : 1) : 2);
				int b2 = (other.getRealElementRequired() ? (other.getRequirementSatisfied() ? 0 : 1) : 2);
				ret = NumberUtils.compare(b1,b2);
			}
			if (ret == 0 && sortByName) {
				ret = getName().compareToIgnoreCase(other.getName());
			}
			if ( ! ascending ) {
				ret = 0 - ret;	// swap 1 and -1 return values
				// Note that we do NOT invert non-equal Type compares
			}
		}
		return ret;
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "id=" + (getId()==null ? "null" : id);
		s += ", name="+ (getName()==null ? "null" : name);
		s += "]";
		return s;
	}

	private int lineCount;
	@Transient
	public int getLineCount() {
		return lineCount;
	}
	public void setLineCount(int lineCount) {
		this.lineCount = lineCount;
	}

	/** See {@link #selected}. */
	@Transient
	public boolean getSelected() {
		return selected;
	}
	/** See {@link #selected}. */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/** See {@link #okToDelete}. */
	@Transient
	public Boolean getOkToDelete() {
		if (getType() != ScriptElementType.LOCATION) {
			return true;
		}
		return okToDelete;
	}
	/** See {@link #okToDelete}. */
	public void setOkToDelete(Boolean okToDelete) {
		this.okToDelete = okToDelete;
	}

	/** See {@link #imageResources}. */
	@Transient
	public List<IceOutputResource> getImageResources() {
		if (imageResources == null) {
			imageResources = ImageUtils.createImageResources(images);
		}
		return imageResources;
	}
	/** See {@link #imageResources}. */
	@Transient
	public void setImageResources(List<IceOutputResource> imageResources) {
		this.imageResources = imageResources;
	}

	/**
	 * Merge the contents of the provided ScriptElement into this object.
	 * Basically we just handle primitive fields here. Collections are either
	 * ignored, or handled by our caller. This is used in the Project Import
	 * facility.
	 *
	 * @param source The source object, whose fields will be copied into this
	 *            object's corresponding fields.
	 */
	public void merge(ScriptElement source) {
		setDescription(source.description);
		setDaysHeldBeforeDrop(source.daysHeldBeforeDrop);
		setDropPickupAllowed(source.dropPickupAllowed);
		setDropToUse(source.dropToUse);
		setElementId(source.elementId);
		setElementIdStr(source.elementIdStr);
//		setImages(se.images); // we don't export images, so don't try to import them
		setLineCount(source.lineCount);
		setRealElementRequired(source.realElementRequired);
//		setScenes(se.scenes); // handled by Project.load
//		setScenesForLocation(se.scenesForLocation); // handled by Project.load
		setSelected(source.selected);
	}

	/**
	 * Export data in this instance via a TaggedExporter. This is currently used to
	 * output this object into an XML file.
	 *
	 * @param ex The TaggedExporter to which each field should be passed.
	 */
	public void flatten(TaggedExporter ex) {
		ex.append("ID", getId());
		ex.append("Description", getName());
		ex.append("Category", getType().getLabel());
	}

//	private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
//		log.debug("");
//		in.defaultReadObject();
//		log.debug("done: " + name);
//	}
//	private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
//		log.debug("start " + name);
//		out.defaultWriteObject();
//		log.debug("done: " + name);
//	}

}
