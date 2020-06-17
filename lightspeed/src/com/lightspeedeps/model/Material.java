package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Material entity.
 *
 * @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "material", uniqueConstraints = @UniqueConstraint(columnNames = "Type"))
public class Material extends PersistentObject<Material> implements Comparable<Material> {
	private static final Log log = LogFactory.getLog(Material.class);
	private static final long serialVersionUID = - 7126570577820348616L;

	public static final String SORT_ID = "id";
	public static final String SORT_NAME = "name";

	// Fields

	/** The Production to which this Project belongs.  This is never null. */
	private Production production;

	/** The name/type of Material -- must be unique within a Production. */
	private String type;

	/** True if this Material is actively in use, and should be
	 * included on the DPR. (Set by end user.) */
	private Boolean inUse = true;

	private String description;
	//private Set<FilmStock> filmStocks = new HashSet<FilmStock>(0);

	/** Used to track row selection on Materials page list. */
	@Transient
	private boolean selected;

	// Constructors

	/** default constructor */
	public Material() {
	}

	// Property accessors

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Production_Id", nullable = false)
	public Production getProduction() {
		return this.production;
	}
	public void setProduction(Production production) {
		this.production = production;
	}

	@Column(name = "Type", unique = true, nullable = false, length = 25)
	public String getType() {
		return this.type;
	}
	public void setType(String type) {
		this.type = type;
	}

	/** See {@link #inUse}. */
	@Column(name = "In_Use", nullable = false)
	public Boolean getInUse() {
		return inUse;
	}
	/** See {@link #inUse}. */
	public void setInUse(Boolean inUse) {
		this.inUse = inUse;
	}

	@Column(name = "Description", length = 1000)
	public String getDescription() {
		return this.description;
	}
	public void setDescription(String description) {
		this.description = description;
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
	public int compareTo(Material s) {
		if (s == null)
			return 1;
		return getType().compareTo(s.getType());
	}

	public int compareTo(Material other, String sortField, boolean ascending) {
		int ret = 0;
		if (sortField == null || sortField.equals(SORT_NAME)) {
			// will sort by name at end
		}
		else if (sortField.equals(SORT_ID)) {
			ret = getId().compareTo(other.getId());
		}
		else {
			log.error("unexpected sort field=" + sortField);
		}
		if (ret == 0) {
			ret = getType().compareToIgnoreCase(other.getType());
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
		result = prime * result + ((getId() == null) ? 0 : id.hashCode());
		result = prime * result + ((getType() == null) ? 0 : type.hashCode());
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
		Material other = null;
		try {
			other = (Material)obj;
		}
		catch (Exception e) {
			return false;
		}
		if (getId() != null && other.getId() != null) {
			return getId().equals(other.getId());
		}
		if (getType() == null) {
			return (other.getType() == null);
		}
		return getType().equals(other.getType());
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "id=" + (getId()==null ? "null" : id);
		s += ", type=" + (getType()==null ? "null" : getType());
		s += "]";
		return s;
	}

//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "material")
//	public Set<FilmStock> getFilmStocks() {
//		return this.filmStocks;
//	}
//	public void setFilmStocks(Set<FilmStock> filmStocks) {
//		this.filmStocks = filmStocks;
//	}

}