package com.lightspeedeps.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.util.IceOutputResource;

import com.lightspeedeps.dood.ElementDood;
import com.lightspeedeps.dood.ProductionDood;
import com.lightspeedeps.object.ImageResource;
import com.lightspeedeps.type.ScriptElementType;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.StringUtils;

/**
 * RealWorldElement entity.
 *
 * @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "real_world_element")
public class RealWorldElement extends PersistentObject<RealWorldElement> implements Cloneable, Comparable<RealWorldElement> {
	private static final Log log = LogFactory.getLog(RealWorldElement.class);

	private static final long serialVersionUID = -2907872598218635945L;

	public static final int DIRECTIONS_MAX_LENGTH = 65535;

	public static final String SORTKEY_NAME = "name";
	public static final String SORTKEY_START = "start";
	public static final String SORTKEY_END = "end";

	// Fields

	/** The production that this POI belongs to. */
	private Production production;

	/** The address of this RWE - only used for Locations. */
	private Address address;

	/** The Contact that this RWE represents -- only used for Characters. */
	private Contact actor;

	/** The person responsible for controlling or managing this RWE. */
	private Contact manager;

//	/** Discriminator value, no longer used, to distinguish between "actors"
//	 * and non-actor RWE types. */
//	private String dtype;

	/** The type of element; both RWEs and ScriptElements share the same
	 * set of types. */
	private ScriptElementType type;

	/** The name of this RWE, which must be unique within its Production. */
	private String name;

	/** The cost of this RWE per "ratePer" period. */
	private BigDecimal rate;

	/** The "cost per" period, such as "weekly" or "daily", which may
	 * be any value entered by the user.*/
	private String ratePer;

	/** The description of this RWE. */
	private String description;

	/** An additional descriptive text field available for each RWE. */
	private String specialInstructions;

	/** A phone number associated with this RWE. */
	private String phone;

	/** Typically driving directions; this field is currently only available
	 * for "Location" RWE types. */
	private String directions;

	/** Intended to be a count of the number of permits needed for this RWE;
	 * this field is currently only available for "Location" RWE types. */
	private Integer permitsRequired;

	/** Intended to be a count of the number of permits acquired for this RWE;
	 * this field is currently only available for "Location" RWE types. */
	private Integer permitsObtained;

	/** Intended for any additional text regarding permits for this RWE;
	 * this field is currently only available for "Location" RWE types. */
	private String permitData;

	/** Typically car parking directions; this field is currently only available
	 * for "Location" RWE types. */
	private String parking; // added in rev 1690

	/** Intended to be a street map in the vicinity of this RWE;
	 * this field is currently only available for "Location" RWE types. */
	private Image map;

	/** The List of associated POIs for this RWE. */
	private List<PointOfInterest> pointOfInterests = new ArrayList<>(0);

	/** The List of images for this RWE. */
	private List<Image> images = new ArrayList<>(0);

//	/** The List of maps for this RWE. */
//	private List<Image> locationMaps = new ArrayList<Image>(0);

	/** The List of black-out date ranges for this RWE. */
	private List<DateRange> dateRanges = new ArrayList<>(0);

	/** The List of RealLinks for this RWE, which creates the many-to-many
	 * association with linked ScriptElement`s */
	private Set<RealLink> realLinks = new HashSet<>(0);

	/** A transient ordered List, containing the same entries as the
	 * realLinks Set, ordered by the RealLink.id field, so that
	 * they always appear in the same order from one view to the next. */
	@Transient
	private List<RealLink> realLinkList = null;

	/** A transient ordered List, containing the subset of the entries
	 * in 'realLinks' which link to ScriptElements belonging to the
	 * current (active) Project.  This List is ordered by the RealLink.id
	 * field. */
	@Transient
	private List<RealLink> realLinksCurrent = null;

	/** Used to track row selection on List page. */
	@Transient
	private boolean selected;

	/** The transient day-out-of-days information for this RWE, which is built
	 * on-demand, and refreshed when necessary. */
	@Transient
	private ElementDood elementDood;

	/** List of Image resource generated from the list of images Required for Iceface 4.x upgrade.*/
	@Transient
	private List<IceOutputResource> imageResources;

	// Constructors

	/** default constructor */
	public RealWorldElement() {
	}

	/** full constructor */
/*	public RealWorldElement(Address address, Contact actor,
			Contact manager, String dtype, ScriptElementType type,
			String name, BigDecimal rate, String ratePer, String description,
			String specialInstructions, String phone, String directions,
			Integer permitsRequired, Integer permitsObtained,
			String permitData, List<PointOfInterest> pointOfInterests,
			Set<Image> images, List<DateRange> dateRanges,
			Set<RealLink> realLinks) {
		this.address = address;
		this.actor = actor;
		this.manager = manager;
		this.dtype = dtype;
		this.type = type;
		this.name = name;
		this.rate = rate;
		this.ratePer = ratePer;
		this.description = description;
		this.specialInstructions = specialInstructions;
		this.phone = phone;
		this.directions = directions;
		this.permitsRequired = permitsRequired;
		this.permitsObtained = permitsObtained;
		this.permitData = permitData;
		this.pointOfInterests = pointOfInterests;
		this.images = images;
		this.dateRanges = dateRanges;
		this.realLinks = realLinks;
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

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Address_Id")
	public Address getAddress() {
		return address;
	}
	public void setAddress(Address address) {
		this.address = address;
	}

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Contact_Id")
	public Contact getActor() {
		return actor;
	}
	public void setActor(Contact actor) {
		this.actor = actor;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Management_Id")
	public Contact getManager() {
		return manager;
	}
	public void setManager(Contact manager) {
		this.manager = manager;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Type", nullable = false, length = 30)
	public ScriptElementType getType() {
		return type;
	}
	public void setType(ScriptElementType type) {
		this.type = type;
	}

	@Column(name = "Name", nullable = false, length = 50)
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "Rate", precision = 10, scale = 2)
	public BigDecimal getRate() {
		return rate;
	}
	public void setRate(BigDecimal rate) {
		this.rate = rate;
	}

	@Column(name = "Rate_Per", length = 10)
	public String getRatePer() {
		return ratePer;
	}
	public void setRatePer(String ratePer) {
		this.ratePer = ratePer;
	}

	@Column(name = "Description", length = 5000)
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	@Column(name = "Special_Instructions", length = 5000)
	public String getSpecialInstructions() {
		return specialInstructions;
	}
	public void setSpecialInstructions(String specialInstructions) {
		this.specialInstructions = specialInstructions;
	}

	@Column(name = "Phone", length = 25)
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}

	@Column(name = "Directions", length = 65535)
	public String getDirections() {
		return directions;
	}
	public void setDirections(String directions) {
		this.directions = directions;
	}

	@Column(name = "Permits_Required")
	public Integer getPermitsRequired() {
		return permitsRequired;
	}
	public void setPermitsRequired(Integer permitsRequired) {
		this.permitsRequired = permitsRequired;
	}

	@Column(name = "Permits_Obtained")
	public Integer getPermitsObtained() {
		return permitsObtained;
	}
	public void setPermitsObtained(Integer permitsObtained) {
		this.permitsObtained = permitsObtained;
	}

	@Column(name = "Permit_Data", length = 5000)
	public String getPermitData() {
		return permitData;
	}
	public void setPermitData(String permitData) {
		this.permitData = permitData;
	}

	@Column(name = "Parking", length = 1000)
	public String getParking() {
		return parking;
	}
	public void setParking(String parking) {
		this.parking = parking;
	}

	@ManyToMany(fetch = FetchType.LAZY)
	@OrderBy("type,name")
	@JoinTable(name = "location_interest", joinColumns = { @JoinColumn(name = "Location_Id", nullable = false, updatable = false) }, inverseJoinColumns = { @JoinColumn(name = "Interest_Id", nullable = false, updatable = false) })
	public List<PointOfInterest> getPointOfInterests() {
		return pointOfInterests;
	}
	public void setPointOfInterests(List<PointOfInterest> pointOfInterests) {
		this.pointOfInterests = pointOfInterests;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "realWorldElement") // chg'd to LAZY 8/29/10
	@OrderBy("date")
	public List<Image> getImages() {
		return images;
	}
	public void setImages(List<Image> images) {
		this.images = images;
	}

//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "locationElement")
//	@OrderBy("date")
//	public List<Image> getLocationMaps() {
//		return locationMaps;
//	}
//	public void setLocationMaps(List<Image> images) {
//		locationMaps = images;
//	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "realWorldElement")
	public List<DateRange> getDateRanges() {
		return dateRanges;
	}
	public void setDateRanges(List<DateRange> dateRanges) {
		this.dateRanges = dateRanges;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "realWorldElement") // chg'd to LAZY 8/29/10
	public Set<RealLink> getRealLinks() {
		return realLinks;
	}
	public void setRealLinks(Set<RealLink> realLinks) {
		this.realLinks = realLinks;
		realLinksCurrent = null; // force refresh of dependent transient values
		realLinkList = null;
	}

	@Transient
	public List<RealLink> getRealLinksCurrent() {
		if (realLinksCurrent == null) {
			realLinksCurrent = new ArrayList<>(getRealLinks().size());
			Project cp = SessionUtils.getCurrentProject();
			for (RealLink rl : getRealLinkList()) {
				if (rl.getScriptElement().getProject().equals(cp)) {
					realLinksCurrent.add(rl);
				}
			}
		}
		return realLinksCurrent;
	}

	@Transient
	public List<RealLink> getRealLinkList() {
		if (realLinkList == null) {
			realLinkList = new ArrayList<>();
			if (getRealLinks() != null) {
				realLinkList.addAll(getRealLinks());
				Collections.sort(realLinkList);
			}
		}
		return realLinkList;
	}

	/**
	 * @return The Image object which represents the map
	 * to this RealWorldElement's address.  Typically only
	 * used for type=Location.
	 */
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Map_Id")
	public Image getMap() {
		return map;
	}
	/**
	 * Set the Image object which represents the map
	 * to this RealWorldElement's address.  Typically only
	 * used for type=Location.
	 */
	public void setMap(Image map) {
		this.map = map;
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

	/**
	 * @return The ElementDood maintained by the ProductionDood singleton, based on the current
	 *         project id and any ScriptElement which we are linked to via RealLinks. Used on the
	 *         Real Element List jsp page.
	 */
	@Transient
	public ElementDood getElementDood() {
		if (elementDood == null) {
			elementDood = ProductionDood.getElementDood(this);
		}
		return elementDood;
	}

	/** See {@link #imageResources}. */
	@Transient
	public List<IceOutputResource> getImageResources() {
		if(imageResources == null) {
			imageResources = new ArrayList<>();
			ImageResource imageResource = null;

			for(Image image : images) {
				try {
					imageResource = new ImageResource(image.getId().toString(), image.getThumbnailB(), "image/png");
					imageResource.setId(image.getId());
					imageResource.setTitle(image.getTitle());
					imageResource.setImage(image);
				}
				catch (Exception e) {
					EventUtils.logError(e.getMessage());
				}
				if(imageResource != null) {
					imageResources.add(imageResource);
				}
			}
		}
		return imageResources;
	}

	/** See {@link #imageResources}. */
	@Transient
	public void setImageResources(List<IceOutputResource> imageResources) {
		this.imageResources = imageResources;
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
		RealWorldElement other;
		try {
			other = (RealWorldElement)obj;
		}
		catch (Exception e) {
			return false;
		}
		if (getId() != null && other.getId() != null) {
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
		result = prime * result + ((getType() == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public int compareTo(RealWorldElement arg0) {
		if (arg0 == null) {
			return 1;
		}
		int comp = getType().compareTo(arg0.getType());
		if (comp != 0) {
			return comp;
		}
		return StringUtils.compareIgnoreCase(getName(), arg0.getName());
	}

	@Override
	public RealWorldElement clone() {
		RealWorldElement newElem;
		try {
			newElem = (RealWorldElement)super.clone();
			newElem.id = null;
			newElem.actor = null;
			newElem.manager = null;
			newElem.map = null;
			newElem.images = null;
			newElem.dateRanges = null;
			newElem.elementDood = null;
			newElem.pointOfInterests = null;
			newElem.realLinks = null;
			newElem.realLinkList = null;
			newElem.realLinksCurrent = null;
			if (getAddress() != null) {
				newElem.setAddress(getAddress().clone());
			}
		}
		catch (CloneNotSupportedException e) {
			log.error("RealWorldElement clone error: ", e);
			return null;
		}
		return newElem;
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "id=" + (getId()==null ? "null" : id);
		s += ", name="+ (getName()==null ? "null" : name);
		s += ", mgr="+ (getManager()==null ? "null" : manager);
		s += "]";
		return s;
	}

}