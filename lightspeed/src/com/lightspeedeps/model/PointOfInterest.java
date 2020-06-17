package com.lightspeedeps.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.util.IceOutputResource;

import com.lightspeedeps.type.PointOfInterestType;
import com.lightspeedeps.util.common.ImageUtils;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.common.StringUtils;

/**
 * PointOfInterest entity.
 *
 * @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "point_of_interest")
public class PointOfInterest extends PersistentObject<PointOfInterest> implements Comparable<PointOfInterest>  {
	private static final Log log = LogFactory.getLog(PointOfInterest.class);

	private static final long serialVersionUID = 2785145084366920530L;

	public static final String SORTKEY_NAME = "name";
	private static final String SORTKEY_PHONE = "phone";

	// Fields

	/** The production that this POI belongs to. */
	private Production production;

	/** The address of this POI. */
	private Address address;

	/** The Type of POI, such as Restaurant or Hospital. */
	private PointOfInterestType type = PointOfInterestType.OTHER;

	/** The name of this POI, which must be unique within the Production. */
	private String name;

	/** The description of this POI; currently unused. */
	private String description;

	/** A phone number related to this POI. */
	private String phone;

	/** The Set of RW elements associated (linked) to this POI. */
	private Set<RealWorldElement> realWorldElements = new HashSet<>(0);

	/** The List of Images associated with this POI. */
	private List<Image> images = new ArrayList<>(0);

	/** Used to track row selection on List page. */
	@Transient
	private boolean selected;

	/** List of Image resource generated from the list of images Required for Iceface 4.x upgrade.*/
	@Transient
	private transient List<IceOutputResource> imageResources;

	// Constructors

	/** default constructor */
	public PointOfInterest() {
	}

	/** full constructor */
/*	public PointOfInterest(Address address, PointOfInterestType type, String name,
			String description, String phone,
			Set<RealWorldElement> realWorldElements, Set<Image> images) {
		this.address = address;
		this.type = type;
		this.name = name;
		this.description = description;
		this.phone = phone;
		this.realWorldElements = realWorldElements;
		this.images = images;
	}
*/
	// Property accessors

	/** See {@link #production}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Production_Id", nullable = false)
	public Production getProduction() {
		return production;
	}
	/** See {@link #production}. */
	public void setProduction(Production production) {
		this.production = production;
	}

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Address_Id")
	public Address getAddress() {
		return address;
	}
	public void setAddress(Address address) {
		this.address = address;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Type", nullable = false, length = 30)
	public PointOfInterestType getType() {
		return type;
	}
	public void setType(PointOfInterestType type) {
		this.type = type;
	}

	@Column(name = "Name", length = 50)
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "Description", length = 1000)
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	@Column(name = "Phone", length = 25)
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}

	@ManyToMany(fetch = FetchType.LAZY, mappedBy = "pointOfInterests")
	public Set<RealWorldElement> getRealWorldElements() {
		return realWorldElements;
	}
	public void setRealWorldElements(Set<RealWorldElement> realWorldElements) {
		this.realWorldElements = realWorldElements;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "pointOfInterest")
	@OrderBy("date")
	public List<Image> getImages() {
		return images;
	}
	public void setImages(List<Image> images) {
		this.images = images;
		//imageList = null; // force refresh
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

	/** See {@link #imageResources}. */
	@Transient
	public List<IceOutputResource> getImageResources() {
		if (imageResources == null) {
			imageResources = ImageUtils.createImageResources(getImages());
		}
		return imageResources;
	}

	/** See {@link #imageResources}. */
	@Transient
	public void setImageResources(List<IceOutputResource> imageResources) {
		this.imageResources = imageResources;
	}

	public int compareTo(PointOfInterest other, String sortField, boolean ascending) {
		int ret = NumberUtils.compare(getType().ordinal(), other.getType().ordinal());
		if (ret == 0) {
			if (sortField == null || sortField.equals(SORTKEY_NAME)) {
				// will sort by name at end
			}
			else if (sortField.equals(SORTKEY_PHONE)) {
				ret = StringUtils.compare(getPhone(), other.getPhone());
			}
			else {
				log.error("unexpected sort field=" + sortField);
			}
			if (ret == 0) {
				ret = getName().compareToIgnoreCase(other.getName());
			}
			if ( ! ascending ) {
				ret = 0 - ret;	// swap 1 and -1 return values
				// Note that we do NOT invert non-equal Type compares
			}
		}
		return ret;
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(PointOfInterest other) {
		int ret = 1;
		if (other != null) {
			ret = compareTo(other, null, true);
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
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		if (getClass() != obj.getClass())
			return false;
		PointOfInterest other = (PointOfInterest)obj;
		// id check modified so new (transient) instance may compare equal to saved instance
		if (id != null && other.id != null)
			return id.equals(other.id);

		// if either id is null, we will return true as long as type & name are equal.
		if (type == null) {
			if (other.type != null)
				return false;
		}
		else if ( ! type.equals(other.type))
			return false;

		if (name == null) {
			if (other.name != null)
				return false;
		}
		else if ( ! name.equals(other.name))
			return false;

		return true;
	}

}