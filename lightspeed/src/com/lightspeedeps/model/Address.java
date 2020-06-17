package com.lightspeedeps.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.port.Exporter;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.common.StringUtils;

/**
 * Address entity.
 *
 * @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "address")
public class Address extends PersistentObject<Address> implements Cloneable {
	private static final Log log = LogFactory.getLog(Address.class);
	private static final long serialVersionUID = - 3439679023391809372L;

	private static final String DUMMY_ZIP = "00000";

	private static final String ZIP_REGEX = "\\d{5}(-\\d{4})?";
	private static final Pattern ZIP_PATTERN = Pattern.compile(ZIP_REGEX);

	//LS-1937
	private static final String CA_ZIP_REGEX = "^(?!.*[DFIOQU])[A-VXY][0-9][A-Z] ?[0-9][A-Z][0-9]$";
	private static final Pattern CA_ZIP_PATTERN = Pattern.compile(CA_ZIP_REGEX);
	private static final String MEX_ZIP_REGEX = "[0-9]{5}";
	private static final Pattern MEX_ZIP_PATTERN = Pattern.compile(MEX_ZIP_REGEX);

	// Fields
	private String addrLine1;
	private String addrLine2;
	private String city;
	private String state;
	private String zip;
	private String country = Constants.DEFAULT_COUNTRY_CODE;
	/** LS-3997 Adding county field. */
	private String county;
	private String timezone;
	private Boolean mapStart = false;
	private String mapLink;
	//private Set<PointOfInterest> pointOfInterests = new HashSet<PointOfInterest>(0);
	//private Set<RealWorldElement> realWorldElements = new HashSet<RealWorldElement>(0);
	//private Set<Contact> contactsForBusinessAddressId = new HashSet<Contact>(0);
	//private Set<Production> productions = new HashSet<Production>(0);
	//private Set<Client> clients = new HashSet<Client>(0);
	//private Set<Contact> contactsForHomeAddressId = new HashSet<Contact>(0);

	// Constructors

	/** default constructor */
	public Address() {
	}

	public Address(boolean isCanada) {
		this();
		if (isCanada) {
			country = Constants.COUNTRY_CODE_CANADA;
		}
	}

	/** full constructor */
/*	public Address(String addrLine1, String addrLine2, String city,
			String state, String zip, String country, String timezone,
			Boolean mapStart, String mapLink) {
		this.addrLine1 = addrLine1;
		this.addrLine2 = addrLine2;
		this.city = city;
		this.state = state;
		this.zip = zip;
		this.country = country;
		this.timezone = timezone;
		this.mapStart = mapStart;
		this.mapLink = mapLink;
	}
*/
	// Property accessors
	@Column(name = "Addr_Line1", length = 100)
	public String getAddrLine1() {
		return addrLine1;
	}
	public void setAddrLine1(String addrLine1) {
		this.addrLine1 = addrLine1;
	}

	@Column(name = "Addr_Line2", length = 100)
	public String getAddrLine2() {
		return addrLine2;
	}
	public void setAddrLine2(String addrLine2) {
		this.addrLine2 = addrLine2;
	}

	@Column(name = "City", length = 100)
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}

	@Column(name = "State", length = 50)
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}

	@Column(name = "Zip", length = 10)
	public String getZip() {
		return zip;
	}
	public void setZip(String zip) {
		this.zip = zip;
	}

	@Column(name = "Country", length = 2)
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}

	@Column(name = "Timezone", length = 10)
	public String getTimezone() {
		return timezone;
	}
	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	@Column(name = "Map_Start", nullable = false)
	public Boolean getMapStart() {
		return mapStart;
	}
	public void setMapStart(Boolean mapStart) {
		this.mapStart = mapStart;
	}

	@Column(name = "Map_Link", length = 200)
	public String getMapLink() {
		return mapLink;
	}
	public void setMapLink(String mapLink) {
		this.mapLink = mapLink;
	}

/** See {@link #county}. */
	@Column(name = "County", length = 100)
	public String getCounty() {
		return county;
	}

	/** See {@link #county}. */
	public void setCounty(String county) {
		this.county = county;
	}

	/*	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "address")
	public Set<PointOfInterest> getPointOfInterests() {
		return this.pointOfInterests;
	}

	public void setPointOfInterests(Set<PointOfInterest> pointOfInterests) {
		this.pointOfInterests = pointOfInterests;
	}
*/
/*	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "address")
	public Set<RealWorldElement> getRealWorldElements() {
		return this.realWorldElements;
	}

	public void setRealWorldElements(Set<RealWorldElement> realWorldElements) {
		this.realWorldElements = realWorldElements;
	}
*/
/*	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "businessAddress")
	public Set<Contact> getContactsForBusinessAddressId() {
		return this.contactsForBusinessAddressId;
	}

	public void setContactsForBusinessAddressId(
			Set<Contact> contactsForBusinessAddressId) {
		this.contactsForBusinessAddressId = contactsForBusinessAddressId;
	}
*/
/*	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "address")
	public Set<Production> getProductions() {
		return this.productions;
	}

	public void setProductions(Set<Production> productions) {
		this.productions = productions;
	}
*/
/*	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "address")
	public Set<Client> getClients() {
		return this.clients;
	}

	public void setClients(Set<Client> clients) {
		this.clients = clients;
	}
*/
/*	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "homeAddress")
	public Set<Contact> getContactsForHomeAddressId() {
		return this.contactsForHomeAddressId;
	}

	public void setContactsForHomeAddressId(
			Set<Contact> contactsForHomeAddressId) {
		this.contactsForHomeAddressId = contactsForHomeAddressId;
	}
*/
	@Transient
	public String getCityStateZip() {
		String result = "";
		if (getCity() != null && getCity().length() > 0)
			result = getCity() + ", ";
		if (getState() != null)
			result += getState() + "  ";
		if (getZip() != null)
			result += getZip();
		return result;
	}

	/**
	 * @return city, state, zip and country
	 */
	@Transient
	public String getCityStateZipCountry() {
		String result = "";
		if (getCity() != null && getCity().length() > 0)
			result = getCity() + ", ";
		if (getState() != null)
			result += getState() + "  ";
		if (getZip() != null)
			result += getZip() + "  ";
		if (getCountry() != null)
			result += getCountry();
		return result;
	}

	/** @return the street address - the line1 and line2 fields - as a single String. */
	@Transient
	public String getAddrLine1Line2() {
		String result = "";
		if (getAddrLine1() != null && getAddrLine1().length() > 0) {
			result += getAddrLine1();
		}
		if (getAddrLine2() != null && getAddrLine2().length() > 0) {
			result += ", " + getAddrLine2();
		}
		return result;
	}

	/** @return the street address (both lines, if non-blank), and the
	 * city, state, and zip, all as a single String. */
	@Transient
	public String getCompleteAddress() {
		String result = "";
		if (getAddrLine1() != null && getAddrLine1().length() > 0) {
			result += getAddrLine1() + ", ";
		}
		if (getAddrLine2() != null && getAddrLine2().length() > 0) {
			result += getAddrLine2() + ", ";
		}
		result += getCityStateZip();
		return result;
	}

	/** @return the street address (both lines, if non-blank), and the
	 * city, state, zip and country, all as a single String. */
	@Transient
	public String getCompleteAddressCountry() {
		String result = getCompleteAddress();

		if(getCountry() != null) {
			result += " " + getCountry();
		}
		return result;
	}

	/**
	 * @return the street address (both lines, if non-blank), and the city,
	 * state, and zip, all as a 2-line address String. This is the same as
	 * {@link #getCompleteAddress()} except that an HTML line-break is inserted
	 * after the first address line. Currently used for the DPR.
	 */
	@Transient
	public String getTwoLineAddress() {
		String result = "";
		if (getAddrLine1() != null && getAddrLine1().length() > 0) {
			result += getAddrLine1() + ", ";
		}
		result += "<br/>"; // the only difference between this & "getCompleteAddress".
		if (getAddrLine2() != null && getAddrLine2().length() > 0) {
			result += getAddrLine2() + ", ";
		}
		result += getCityStateZip();
		return result;
	}

	/**
	 * Clear all the fields in this Address instance.
	 */
	public void clear() {
		setAddrLine1(null);
		setAddrLine2(null);
		setCity(null);
		setState(null);
		setZip(null);
		setCounty(null);
		setCountry(Constants.DEFAULT_COUNTRY_CODE);
		setTimezone(null);
		setMapStart(false);
		setMapLink(null);
	}

	/**
	 * Copy all the data fields from the 'other' Address into
	 * this Address.
	 * @param other The source object for the fields to be copied.
	 */
	@Transient
	public void copyFrom(Address other) {
		setAddrLine1(other.getAddrLine1());
		setAddrLine2(other.getAddrLine2());
		setCity(other.getCity());
		setState(other.getState());
		setCounty(other.getCounty());
		setCountry(other.getCountry());
		setZip(other.getZip());
		setTimezone(other.getTimezone());
		setMapStart(other.getMapStart());
		setMapLink(other.getMapLink());
	}

	@Transient
	public boolean isEmpty() {
		return (getCompleteAddress().trim().length() == 0);
	}

	/**
	 * This method "trims" all non-null fields in the Address; any fields that
	 * are empty (length of zero) are set to null.
	 *
	 * @return True if all fields are null (either initially or as a result of
	 *         the trim operation).
	 */
	public boolean trimIsEmpty() {
		boolean empty = true;
		if (getAddrLine1() != null) {
			if (getAddrLine1().trim().length() == 0) {
				setAddrLine1(null);
			}
			else {
				setAddrLine1(getAddrLine1().trim());
				empty = false;
			}
		}
		if (getAddrLine2() != null) {
			if (getAddrLine2().trim().length() == 0) {
				setAddrLine2(null);
			}
			else {
				setAddrLine2(getAddrLine2().trim());
				empty = false;
			}
		}
		if (getCity() != null) {
			if (getCity().trim().length() == 0) {
				setCity(null);
			}
			else {
				setCity(getCity().trim());
				empty = false;
			}
		}
		if (getState() != null) {
			if (getState().trim().length() == 0) {
				setState(null);
			}
			else {
				setState(getState().trim());
				empty = false;
			}
		}
		if (getCounty() != null) {
			if (getCounty().trim().length() == 0) {
				setCounty(null);
			}
			else {
				setCounty(getCounty().trim());
				empty = false;
			}
		}
		if (getZip() != null) {
			if (getZip().trim().length() == 0) {
				setZip(null);
			}
			else {
				setZip(getZip().trim());
				empty = false;
			}
		}
		return empty;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getAddrLine1() == null) ? 0 : addrLine1.hashCode());
		result = prime * result + ((getAddrLine2() == null) ? 0 : addrLine2.hashCode());
		result = prime * result + ((getCity() == null) ? 0 : city.hashCode());
		result = prime * result + ((getCountry() == null) ? 0 : country.hashCode());
		result = prime * result + ((getId() == null) ? 0 : id.hashCode());
		result = prime * result + ((getState() == null) ? 0 : state.hashCode());
		result = prime * result + ((getZip() == null) ? 0 : zip.hashCode());
		return result;
	}

	/**
	 * Compare two Address objects.  Note that this will return True
	 * if the two objects have the same database id, even if the other
	 * fields are different (which shouldn't happen)!
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;

		Address other;
		try {
			other = (Address)obj;
		}
		catch (Exception e) {
			return false;
		}
		if ( getId() != null && other.getId() != null) {
			return getId().equals(other.getId());
		}

		return equalsAddress(other);
	}

	/**
	 * Compare the address fields, but ignore the database id value. This allows
	 * code to compare two persisted Address objects to see if they refer to the
	 * same location, even though they have different id's.
	 *
	 * @param other The Address object to compare to this object.
	 * @return True if all the address fields in this object are equal to the
	 *         corresponding fields in 'other'.
	 */
	@Transient
	public boolean equalsAddress(Address other) {
		if (other == null) {
			return false;
		}
		if (getAddrLine1() == null) {
			if (other.getAddrLine1() != null)
				return false;
		}
		else if (!getAddrLine1().equals(other.getAddrLine1()))
			return false;

		if (getAddrLine2() == null) {
			if (other.getAddrLine2() != null)
				return false;
		}
		else if (!getAddrLine2().equals(other.getAddrLine2()))
			return false;

		if (getCity() == null) {
			if (other.getCity() != null)
				return false;
		}
		else if (!getCity().equals(other.getCity()))
			return false;

		if (getCountry() == null) {
			if (other.getCountry() != null)
				return false;
		}
		else if (!getCountry().equals(other.getCountry()))
			return false;

		if (getState() == null) {
			if (other.getState() != null)
				return false;
		}
		else if (!getState().equals(other.getState()))
			return false;

		if (getCounty() == null) {
			if (other.getCounty() != null)
				return false;
		}
		else if (!getCounty().equals(other.getCounty()))
			return false;

		if (getZip() == null) {
			if (other.getZip() != null)
				return false;
		}
		else if (!getZip().equals(other.getZip()))
			return false;

		return true;
	}

	/**
	 * Compare the address fields, but ignore the database id value. This allows
	 * code to compare two persisted Address objects to see if they refer to the
	 * same location, even though they have different id's.  In addition, if this
	 * instance is 'empty', and the 'other' is null, true is returned.
	 *
	 * @param other The Address object to compare to this object.
	 * @return True if all the address fields in this object are equal to the
	 *         corresponding fields in 'other'; or if 'other' is null and this
	 *         instance is "empty" (isEmpty() returns true).
	 */
	@Transient
	public boolean equalsAddressOrNull(Address other) {
		if (other == null && isEmpty()) {
			return true;
		}
		return equalsAddress(other);
	}

	@Override
	public Address clone() {
		Address a;
		try {
			a = (Address)super.clone();
			a.id = null;
		}
		catch (CloneNotSupportedException e) {
			log.error(e);
			return null;
		}
		return a;
	}

	/**
	 * Standard export method for US address. The country code is not exported.
	 *
	 * @param ex The Exporter to use for exporting the fields.
	 */
	public void exportFlat(Exporter ex) {
		ex.append(getAddrLine1());
		ex.append(getAddrLine2());
		ex.append(getCity());
		ex.append(getState());
		exportZip(ex);
	}

	/**
	 * Export method for US address which combines the first and second lines of
	 * street address into a single export field. The country code is not
	 * exported.
	 *
	 * @param ex The Exporter to use for exporting the fields.
	 */
	public void exportFlatShort(Exporter ex) {
		String addr = getAddrLine1();
		if (! StringUtils.isEmpty(getAddrLine2())) {
			addr += ", " + getAddrLine2();
		}
		ex.append(addr);
		ex.append(getCity());
		ex.append(getState());
		exportZip(ex);
	}

	/**
	 * Export the zip code. If the address is not empty, and the zip code is
	 * empty or invalid, the string "00000" is exported instead.
	 *
	 * @param ex The Exporter to be used.
	 */
	private void exportZip(Exporter ex) {
		String azip = getZip();
		if (getCountry() == null || getCountry().equals("US")) {
			// only validate for U.S. addresses
			if (getState() == null || ! getState().equals(Constants.FOREIGN_FO_STATE)) {
				// only validate if state is NOT 'FO' (Foreign)
				if (! StringUtils.isEmpty(getAddrLine1()) ||
						! StringUtils.isEmpty(getAddrLine2()) || ! StringUtils.isEmpty(getCity()) ||
						! StringUtils.isEmpty(getState())) {
					// non-blank Adress; we need to export a non-blank zip
					if (StringUtils.isEmpty(azip)) {
						azip = DUMMY_ZIP;
					}
					else {
						Matcher matcher = ZIP_PATTERN.matcher(azip);
						if (! matcher.matches()) {
							azip = DUMMY_ZIP;
						}
					}
				}
			}
		}
		ex.append(azip);
	}

	@Transient
	public boolean isZipValidOrEmpty() {
		if (getZip() == null || StringUtils.isEmpty(getZip())) {
			return true;
		}
		return isZipValid();
	}

	@Transient
	public boolean isZipValid() {
		boolean b = true;
		if ((getCountry() == null || getCountry().equals("US")) &&
				getState() != null && ! getState().equals(Constants.FOREIGN_FO_STATE)) {
			// Should be US address
			if (getZip() == null) {
				b = false;
			}
			else {
				Matcher matcher = ZIP_PATTERN.matcher(getZip());
				b = matcher.matches();
			}
		}
		if ((getCountry().equals(Constants.COUNTRY_CODE_MEXICO) ||
				getCountry().equals(Constants.COUNTRY_CODE_CANADA)) &&
				getState().equals(Constants.MEXICO_STATE) &&
				! getState().equals(Constants.CANADA_STATE)) {
			b = validateZipCodes();
		}

		log.debug("b = " + b);
		return b;
	}

	//LS-1937 zip code validation
	private boolean validateZipCodes() {
		boolean valid = true;
		if ((getCountry().equals(Constants.COUNTRY_CODE_CANADA)) && getState() != null &&
				! getState().equals(Constants.CANADA_STATE)) {
			if (getZip() == null) {
				valid = false;
			}
			else {
				Matcher matcher = CA_ZIP_PATTERN.matcher(getZip());
				valid = matcher.matches();
			}
		}

		else if ((getCountry().equals(Constants.COUNTRY_CODE_MEXICO)) && getState() != null &&
				! getState().equals(Constants.MEXICO_STATE)) {
			if (getZip() == null) {
				valid = false;
			}
			else {
				Matcher matcher = MEX_ZIP_PATTERN.matcher(getZip());
				valid = matcher.matches();
			}
		}
		return valid;
	}
	/*
	 * Zip code validation for MyAccount
	 */
	@Transient
	public boolean isZipValidIgnoreState() {
		boolean b = true;
		if ((getCountry() == null || getCountry().equals("US"))) {
			// Should be US address
			if (getZip() == null) {
				b = false;
			}
			else {
				Matcher matcher = ZIP_PATTERN.matcher(getZip());
				b = matcher.matches();
			}
		}
		if ((getCountry().equals(Constants.COUNTRY_CODE_MEXICO) ||
				getCountry().equals(Constants.COUNTRY_CODE_CANADA))) {
			b = validateZipCodes();
		}

		log.debug("b = " + b);
		return b;
	}

	/*
	 * clearCityStateZip is used to clear city state zipCode if Zip is not valid LS-4478
	 */
	@Transient
	public void clearCityStateZip() {
		setCity(null);
		setState(null);
		setZip(null);
	}

}