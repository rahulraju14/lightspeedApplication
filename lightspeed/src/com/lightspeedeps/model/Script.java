//	File Name:	Script.java
package com.lightspeedeps.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.TextElementDAO;
import com.lightspeedeps.type.ImportType;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.common.NumberUtils;

/**
 * Script entity.
 *
 * @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "script", uniqueConstraints = @UniqueConstraint(columnNames = {
		"Project_Id", "Revision_Number" }))
public class Script extends PersistentObject<Script> implements Comparable<Script>, Cloneable {
	/** for serialization */
	private static final long serialVersionUID = -3733351062773603500L;

	private static final Log log = LogFactory.getLog(Script.class);

	public static final int MAX_TITLE_LENGTH = 50;

	public static final String SORT_REV = "rev";
	public static final String SORT_DATE = "date";
	public static final String SORT_NAME = "name";

	// Fields

	private ColorName colorName;
	private Project project;
	private String copyright;

	/** The sequential revision number of this script within this project. This
	 * is assigned by LS during the import process. */
	private Integer revisionNumber;

	/** The date & time the script was loaded/created in lightSpeed. */
	private Date date;

	/** The name or title assigned to the script by the user when the script
	 * is first imported. */
	private String description;

	/** The number of physical pages in the script. */
	private Integer lastPage = 0;

	/** Estimated lines per page of script from import process. */
	private Integer linesPerPage = Constants.SCRIPT_MAX_LINES_PER_PAGE;

	/** A string of the logical page numbers in the (PDF) text, concatenated
	 * with "|" separating the entries. */
	private String pageNumbers = "";

	/** The type of file or import method that created this script. */
	private ImportType importType = ImportType.MANUAL;

	/** An ordered List of all the Scene's comprising the script, ordered
	 * by Scene.sequence. */
	private List<Scene> scenes = new ArrayList<>(0);

	/** An ordered List of all the Page's comprising the script. */
	private List<Page> pages = new ArrayList<>(0);

	//DH private Set<Project> projects = new HashSet<Project>(0); // not used
	//DH private Set<Contact> contacts = new HashSet<Contact>(0);	// Script writers relation
	//DH private Set<Contact> contacts_1 = new HashSet<Contact>(0); // not used

	/**
	 * A transient boolean used on the Script Drafts web page for selection within the list of
	 * scripts.
	 */
	@Transient
	private Boolean selected = false;

	@Transient
	private Boolean sceneText;

	// Constructors

	/** default constructor */
	public Script() {
	}

	/** full constructor */
/*	public Script(ColorName colorName, Project project, String copyright,
			Integer revisionNumber, Date date, String description,
			Integer lastPage, List<Scene> scenes, Set<Project> projects ) {
		this.colorName = colorName;
		this.project = project;
		this.copyright = copyright;
		this.revisionNumber = revisionNumber;
		this.date = date;
		this.description = description;
		this.lastPage = lastPage;
		this.scenes = scenes;
	//	this.projects = projects;
	//	this.contacts_1 = contacts_1;
	}
*/
	// Property accessors

	/** See {@link #colorName}*/
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Color_Id")
	public ColorName getColorName() {
		return colorName;
	}
	/** See {@link #colorName}*/
	public void setColorName(ColorName colorName) {
		this.colorName = colorName;
	}

	/** See {@link #project}*/
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Project_Id", nullable = false)
	public Project getProject() {
		return project;
	}
	/** See {@link #project}*/
	public void setProject(Project project) {
		this.project = project;
	}

	/** See {@link #copyright}*/
	@Column(name = "Copyright", length = 100)
	public String getCopyright() {
		return copyright;
	}
	/** See {@link #copyright}*/
	public void setCopyright(String copyright) {
		this.copyright = copyright;
	}

	/** See {@link #revisionNumber}*/
	@Column(name = "Revision_Number", nullable = false)
	public Integer getRevisionNumber() {
		return revisionNumber;
	}
	/** See {@link #revisionNumber}*/
	public void setRevisionNumber(Integer revisionNumber) {
		this.revisionNumber = revisionNumber;
	}

	/** See {@link #date}*/
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Date", nullable = false, length = 0)
	public Date getDate() {
		return date;
	}
	/** See {@link #date}*/
	public void setDate(Date date) {
		this.date = date;
	}

	/** See {@link #description}*/
	@Column(name = "Description", length = 1000)
	public String getDescription() {
		return description;
	}
	/** See {@link #description}*/
	public void setDescription(String description) {
		this.description = description;
	}

	/** See {@link #lastPage}*/
	@Column(name = "Last_Page")
	public Integer getLastPage() {
		return lastPage;
	}
	/** See {@link #lastPage}*/
	public void setLastPage(Integer lastPage) {
		this.lastPage = lastPage;
	}

	/** See {@link #pageNumbers}. */
	@Column(name = "Page_Numbers", nullable = false, length = 1000)
	public String getPageNumbers() {
		return pageNumbers;
	}
	/** See {@link #pageNumbers}. */
	public void setPageNumbers(String pageNumbers) {
		this.pageNumbers = pageNumbers;
	}

	/** See {@link #linesPerPage}. */
	@Column(name = "Lines_per_Page")
	public Integer getLinesPerPage() {
		return linesPerPage;
	}
	/** See {@link #linesPerPage}. */
	public void setLinesPerPage(Integer linesPerPage) {
		this.linesPerPage = linesPerPage;
	}

	/** See {@link #importType}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Import_Type", nullable = false, length = 30)
	public ImportType getImportType() {
		return importType;
	}
	/** See {@link #importType}. */
	public void setImportType(ImportType importType) {
		this.importType = importType;
	}

	// Note: with List<> & OrderBy, FetchType EAGER is not allowed, gets Hibernate
	// error: "cannot simultaneously fetch multiple bags"
	/** See {@link #scenes}*/
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "script")
	@OrderBy("sequence")
	public List<Scene> getScenes() {
		return scenes;
	}

	public void setScenes(List<Scene> scenes) {
		this.scenes = scenes;
	}

/*	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinTable(name = "script_writer", joinColumns = { @JoinColumn(name = "Script_Id", nullable = false, updatable = false) }, inverseJoinColumns = { @JoinColumn(name = "Writer_Id", nullable = false, updatable = false) })
	public Set<Contact> getContacts() {
		return this.contacts;
	}
	public void setContacts(Set<Contact> contacts) {
		this.contacts = contacts;
	}
*/

	/** See {@link #pages}. */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "script")
	@OrderBy("number")
	public List<Page> getPages() {
		return pages;
	}
	/** See {@link #pages}. */
	public void setPages(List<Page> pages) {
		this.pages = pages;
	}

	/**
	 * Return the number of scenes.  Useful in JSP, where only "get"
	 * methods can be used.
	 */
	@Transient
	public Integer getSceneSize() {
		Integer i = getScenes().size();
		return i;
	}

	/**
	 * Indicates whether or not the script contains TextElement objects, that is, if it has the
	 * script text loaded.
	 *
	 * @return True if any scene contains one or more TextElements, false otherwise.
	 */
	@Transient
	public Boolean getSceneText() {
		//log.debug("text=" +sceneText + ", scr=" + ((Object)this).hashCode());
		if (sceneText == null) {
			sceneText = TextElementDAO.getInstance().hasText(this);
		}
		return sceneText;
	}
	public void setSceneText(Boolean b) {
		sceneText = b;
	}

	/** See {@link #selected}*/
	@Transient
	public Boolean getSelected() {
		return selected;
	}
	/** See {@link #selected}*/
	public void setSelected(Boolean select) {
		selected = select;
	}

	/**
	 * @return True if this Script has Page objects (which describe the page-by-page
	 * text arrangement); false otherwise.
	 */
	@Transient()
	public boolean hasPageData() {
		return (getPages() != null && getPages().size() >= getLastPage());
	}

	/**
	 * All we care about for ordering scripts is when we list them to have
	 * the most recent first.  So our compare just gives the reverse of
	 * comparing the Date fields.
	 */
	@Override
	public int compareTo(Script s) {
		if (s == null)
			return 1;
		return - getDate().compareTo(s.getDate());
	}

	public int compareTo(Script other, String sortField, boolean ascending) {
		int ret = 0;
		if (sortField == null || sortField.equals(SORT_NAME)) {
			// will sort by name at end
		}
		else if (sortField.equals(SORT_REV)) {
			ret = NumberUtils.compare(getRevisionNumber(), other.getRevisionNumber());
		}
		else if (sortField.equals(SORT_DATE)) {
			ret = getDate().compareTo(other.getDate());
		}
		else {
			log.error("unexpected sort field=" + sortField);
		}
		if (ret == 0) {
			ret = getDescription().compareToIgnoreCase(other.getDescription());
		}
		if ( ! ascending ) {
			ret = - ret;	// swap 1 and -1 return values
		}
		return ret;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		//log.debug("hash="+result);
		return result;
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
		Script other;
		try {
			other = (Script)obj;
		}
		catch (Exception e) {
			return false;
		}
		if (getDate() == null) {
			if (other.getDate() != null)
				return false;
		}
		else if (!getDate().equals(other.getDate()))
			return false;
		if (id == null) {
			return true; // could be unsaved copy
//			if (other.id != null)
//				return false;
		}
		else if (!getId().equals(other.getId()))
			return false;
		return true;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "id=" + (getId()==null ? "null" : id);
		s += ", rev="+ (getRevisionNumber()==null ? "null" : getRevisionNumber());
		s += ", scenes="+ (getSceneSize()==null ? "null" : getSceneSize());
		s += ", pages="+ (getLastPage()==null ? "null" : getLastPage());
		s += ", type="+ getImportType();
		s += "]";
		return s;
	}

	/**
	 * Used in Project Import, to merge the given imported Script with this
	 * existing Script. This (currently) only uses the color and description
	 * from the import, and leaves everything else about the existing Script
	 * unchanged.
	 *
	 * @param script The Script object loaded from the import file.
	 */
	public void merge(Script script) {
		setColorName(script.colorName);
//		setDate(script.date);
		setDescription(script.description);
//		setImportType(script.importType);
//		setLastPage(script.lastPage);
//		setLinesPerPage(script.linesPerPage);
//		setPageNumbers(script.pageNumbers);
//		setRevisionNumber(script.revisionNumber);
//		setPages(script.pages);
//		setScenes(script.scenes);
	}

	/**
	 * This method is called by the Java Serialization framework when a Script
	 * object is being de-serialized. The TextElement`s of the Script object are
	 * serialized without their Page references, because having those sometimes
	 * caused stack overflows during exports or imports. Here we restore the
	 * Page references, based on the page number values saved in the TextElement
	 * objects.
	 * <p>
	 * See also {@link TextElement#writeObject}.
	 *
	 * @param in The ObjectInputStream that is the source for the
	 *            de-serialization in progress.
	 * @throws java.io.IOException Iff in.defaultReadObject() throws it. See
	 *             {@link java.io.ObjectInputStream#defaultReadObject}.
	 * @throws ClassNotFoundException Iff in.defaultReadObject() throws it. See
	 *             {@link java.io.ObjectInputStream#defaultReadObject}.
	 */
	private void readObject(java.io.ObjectInputStream in)
			throws java.io.IOException, ClassNotFoundException {
		log.debug("");
		in.defaultReadObject();
		log.debug("done with Script; fix TextElement page-refs");
		Map<Integer,Page> pgMap = new HashMap<>();
		try {
			for (Scene scene : scenes) {
				for (TextElement te : scene.getTextElements()) {
					if (te.getPageNumber() != null) {
						te.setPage(findPage(pgMap, te.getPageNumber()));
					}
				}
			}
		}
		catch (Exception e) {
			log.error("Error restoring TextElement values: " + e.getLocalizedMessage());
		}
		log.debug("textElements done");
	}

	/**
	 * Lookup the given pageNumber in the give Map, and return the Page if
	 * found. If not found in the Map, then search this Script's Page objects to
	 * find the page with the matching number, add that to the Map, and return
	 * it.
	 *
	 * @param pgMap A Map to keep track of the pages found so far.
	 * @param pageNumber The physical number of the page we're looking for.
	 * @return The Page object with the given physical page number; the Map may
	 *         be updated if this is the first time we were called for that
	 *         pageNumber.
	 */
	private Page findPage(Map<Integer,Page> pgMap, Integer pageNumber) {
		Page page = pgMap.get(pageNumber);
		if (page == null) {
			for (Page p : pages) {
				if (p.getNumber().equals(pageNumber)) {
					page = p;
					pgMap.put(pageNumber, page);
					break;
				}
			}
		}
		return page;
	}

//	private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
//		log.debug("");
//		out.defaultWriteObject();
//		log.debug("done");
//	}

}