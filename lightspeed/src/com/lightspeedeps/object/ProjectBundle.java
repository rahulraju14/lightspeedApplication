/**
 * File: ProjectBundle.java
 */
package com.lightspeedeps.object;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.*;

/**
 * The object used by Import and Export functions to move a Project
 * and related objects between systems.
 */
public class ProjectBundle implements Serializable {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(ProjectBundle.class);

	private static final long serialVersionUID = 7700085130836445175L;

	/** Timestamp when this bundle was created. */
	private Date created;

	/** The Contact who created this bundle. */
	private Contact contact;

	/** The Project being exported; this will include most of the exported information,
	 * including Script`s, Scene`s, Unit`s, Stripboard`s, ProjectSchedule`s, etc. */
	private Project project;

	/** All the ScriptElement`s associated with the exported Project.  These are kept
	 * as a separate collection since some of them may be orphans and not referred to
	 * by any object (e.g., Scene) included in the Project object. */
	private Collection<ScriptElement> scriptElements;

	/**
	 * Default constructor.
	 */
	public ProjectBundle() {
	}

	/** See {@link #created}. */
	public Date getCreated() {
		return created;
	}
	/** See {@link #created}. */
	public void setCreated(Date created) {
		this.created = created;
	}

	/** See {@link #contact}. */
	public Contact getContact() {
		return contact;
	}
	/** See {@link #contact}. */
	public void setContact(Contact contact) {
		this.contact = contact;
	}

	/** See {@link #project}. */
	public Project getProject() {
		return project;
	}
	/** See {@link #project}. */
	public void setProject(Project project) {
		this.project = project;
	}

	/** See {@link #scriptElements}. */
	public Collection<ScriptElement> getScriptElements() {
		return scriptElements;
	}
	/** See {@link #scriptElements}. */
	public void setScriptElements(Collection<ScriptElement> scriptElements) {
		this.scriptElements = scriptElements;
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "project=" + (getProject() == null ? "null" : getProject());
		s += "SEs=" + (getScriptElements() == null ? "null" : getScriptElements());
		s += "]";
		return s;
	}

//	private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
//		log.debug("");
//		in.defaultReadObject();
//		log.debug("done");
//	}
//	private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
//		log.debug("");
//		out.defaultWriteObject();
//		log.debug("done");
//	}

}
