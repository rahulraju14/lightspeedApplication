//	File Name:	Stripboard.java
package com.lightspeedeps.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.*;

/**
 * Stripboard entity.
 *
 */
@Entity
@Table(name = "stripboard", uniqueConstraints = @UniqueConstraint(columnNames = {
		"Project_Id", "Revision" }))
public class Stripboard extends PersistentObject<Stripboard> implements Cloneable {

	//private static final Log LOG = LogFactory.getLog(Stripboard.class);

	// Fields

	private static final long serialVersionUID = -7785211272182214251L;


	/** The project that owns this Stripboard. */
	private Project project;

	/** The User that last updated this Stripboard. */
	private User user;

	/**
	 * The internal revision number assigned to this Stripboard. This should be
	 * unique within a given Project. Revision numbers start at 1 and increment
	 * by 1. There can be a gap if a Stripboard has been deleted. New
	 * Stripboard`s are always assigned a number one higher than the current
	 * highest revision number within the same Project.
	 */
	private Integer revision;

	/** The name (title) of the Stripboard. */
	private String description;

	/** Timestamp of the last change made to this Stripboard. */
	private Date lastSaved;

	/** not needed */
//	private Set<Change> changes = new HashSet<Change>(0);

	/** The Set of all Strip`s in this Stripboard, including both scheduled
	 * and unscheduled, across all Units. */
	private Set<Strip> strips = new HashSet<>(0);

	/** The Set of UnitStripboard`s for this Stripboard, consisting of
	 * one for each Unit in this Stripboard's Project. */
	private List<UnitStripboard> unitSbs = new ArrayList<>(0);

	@Transient
	private boolean selected;

	// Constructors

	/** default constructor */
	public Stripboard() {
	}

	/** full constructor */
/*	public Stripboard(Project project, User user, Integer revision,
			String description, Integer shootingDays, Date lastSaved,
			Set<Changes> changeses, Set<Strip> strips,
			Set<Callsheet> callsheets, Set<Project> projects) {
		this.project = project;
		this.user = user;
		this.revision = revision;
		this.description = description;
		this.shootingDays = shootingDays;
		this.lastSaved = lastSaved;
		this.changes = changeses;
		this.strips = strips;
		//this.callsheets = callsheets;
		//this.projects = projects;
	}
*/
	// Property accessors

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Project_Id")
	public Project getProject() {
		return project;
	}
	public void setProject(Project project) {
		this.project = project;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Saved_By_Id")
	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}

	@Column(name = "Revision", nullable = false)
	public Integer getRevision() {
		return revision;
	}
	public void setRevision(Integer revision) {
		this.revision = revision;
	}

	@Column(name = "Description", length = 100)
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	/** Use getShootingDays(Unit u) instead */
//	@Transient
//	@Deprecated
//	public Integer getShootingDays() {
//		return getUnitSbs().iterator().next().getShootingDays();
//	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Last_Saved", nullable = false, length = 0)
	public Date getLastSaved() {
		return lastSaved;
	}
	public void setLastSaved(Date lastSaved) {
		this.lastSaved = lastSaved;
	}

//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "stripboard")
//	public Set<Change> getChanges() {
//		return this.changes;
//	}
//	public void setChanges(Set<Change> changeses) {
//		this.changes = changeses;
//	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "stripboard") // GnG had EAGER
	public Set<Strip> getStrips() {
		return strips;
	}
	public void setStrips(Set<Strip> strips) {
		this.strips = strips;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "stripboard")
	@OrderBy("id")
	public List<UnitStripboard> getUnitSbs() {
		return unitSbs;
	}
	public void setUnitSbs(List<UnitStripboard> unitsbs) {
		unitSbs = unitsbs;
	}

	@Transient
	public Integer getShootingDays(Unit unit) {
		int n = 0;
		UnitStripboard uSb = getUnitSb(unit);
		if (uSb != null) {
			n = uSb.getShootingDays();
		}
		return n;
	}

	@Transient
	public UnitStripboard getUnitSb(Unit unit) {
		//log.debug("sb=" + getId() + ", unit=" + unit);
		UnitStripboard uSb = null;
		for (UnitStripboard u : getUnitSbs()) {
			//log.debug(u.getId());
			if (u.getUnit().equals(unit)) {
				uSb = u;
				//log.debug("matched");
			}
		}
		return uSb;
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

	@Override
	public Object clone() {
		try {
			Stripboard copy = (Stripboard)super.clone();
			copy.id = null; // it's a transient object

			// Create a new set of strips, identical to the existing set
			Set<Strip> copyStrips = new HashSet<>(getStrips().size());
			for (Strip strip : getStrips() ) {
				Strip newone = (Strip)strip.clone(); // this will also create clones of any Notes
				newone.setStripboard(copy);
				copyStrips.add(newone);
			}
			copy.setStrips(copyStrips);

			// Create a new set of UnitStripboards
			List<UnitStripboard> usbs = new ArrayList<>();
			for (UnitStripboard usb : getUnitSbs()) {
				UnitStripboard newone = (UnitStripboard)usb.clone();
				newone.setStripboard(copy);
				usbs.add(newone);
			}
			copy.setUnitSbs(usbs);

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
		s += ", rev=" + (getRevision()==null ? "null" : getRevision());
		s += ", desc=" + (getDescription()==null ? "null" : getDescription());
		s += "]";
		return s;
	}

}