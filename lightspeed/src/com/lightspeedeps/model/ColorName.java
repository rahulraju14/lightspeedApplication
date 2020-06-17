package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * ColorName entity. This table consists of all the colors used for
 * script revisions.
 */
@Entity
@Table(name = "color_name", uniqueConstraints = @UniqueConstraint(columnNames = "Name"))
public class ColorName extends PersistentObject<ColorName> {

	private static final long serialVersionUID = - 3963963100037340247L;

	// Fields
	private String name;
	private Integer rgbValue;
	private Integer scriptRevision;
	//private Set<Script> scripts = new HashSet<Script>(0);

	// Constructors

	/** default constructor */
	public ColorName() {
	}

	/** full constructor */
	public ColorName(String name, Integer rgbValue, Integer scriptRevision) {
		this.name = name;
		this.rgbValue = rgbValue;
		this.scriptRevision = scriptRevision;
	}

	// Property accessors

	@Column(name = "Name", unique = true, nullable = false, length = 30)
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "RGB_Value", nullable = false)
	public Integer getRgbValue() {
		return rgbValue;
	}
	public void setRgbValue(Integer rgbValue) {
		this.rgbValue = rgbValue;
	}

	@Column(name = "Script_Revision")
	public Integer getScriptRevision() {
		return scriptRevision;
	}
	public void setScriptRevision(Integer scriptRevision) {
		this.scriptRevision = scriptRevision;
	}

//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "colorName")
//	public Set<Script> getScripts() {
//		return this.scripts;
//	}
//	public void setScripts(Set<Script> scripts) {
//		this.scripts = scripts;
//	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "color=" + getName();
		s += "(" + getRgbValue() + ")";
		s += "]";
		return s;
	}

}
