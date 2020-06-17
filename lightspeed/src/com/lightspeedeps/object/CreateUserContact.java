package com.lightspeedeps.object;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.lightspeedeps.model.Importable;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.type.ActionType;

/**
 * This class contains the attributes for creating a new Lightspeed User account
 * and adding that User to a Production (and Project) with a given occupation in
 * a given Department. The data is set through unmarshalling of the JSON string
 * sent by a caller of the web service.
 */
@XmlRootElement
@XmlAccessorType(value=XmlAccessType.FIELD)
public class CreateUserContact implements Importable {

	/** Required. This is the unique identifier for the User account to be created.
	 * It must be a syntactically valid email address. If a Lightspeed account already
	 * exists for the given email address, processing will continue and add
	 * this user to the specified Production and Project. */
	private	String email;

	/** Required. The person's last (family) name. */
	private String lastName;

	/** Required. The person's first (given) name. */
	private String firstName;

	/** Optional. The street address of the person. */
	private String address;

	/** Optional. The city name of the person's address. */
	private String city;

	/** Optional. The state abbreviation (2 characters) of the person's
	 * address. No validation is done. */
	private String state;

	/** Optional. The zip code of the person's address. No validation is done. */
	private String zip;

	/** Optional. The 2-letter ISO code for the country.  For United States, use 'US'
	 * (if specified). */
	private String country;

	/** Optional. The person's birth date, in the format 'yyyy-mm-dd'. */
	private String birthday;

	/** Optional. The person's phone number.  No validation is done. */
	private String phone;

	/** Optional. The name of an emergency contact.  No validation
	 * is done. */
	private String emergencyContact;

	/** Optional. The phone number of an emergency contact.
	 * No validation is done. */
	private String emergencyPhone;

	/** Optional. The relationship of the given emergency contact.
	 * No validation is done. */
	private String emergencyRelationship;

	/** Required. The alphanumeric identifier of the Lightspeed Production
	 * to which the user will be added.  The identifier is typically one
	 * or two alphabetic characters concatenated with a 2-to-5 digit number,
	 * e.g., 'PB104' or 'P12345'. */
	private String productionId;

	/** Required for multi-project productions; that is, it is only optional for
	 * 'Feature-Film' productions. It is the name of the Project ('job'), within
	 * the Lightspeed production, to which the new user will be added. If the
	 * given Project does not exist, it will be created.
	 */
	private String project;

	/** Required. The name of the Department, within the Lightspeed
	 * production, to which the new user will be added. If the given Department
	 * does not exist, it will be created. */
	private String department;

	/** Required.  The occupation to be given to the person within
	 * the specified Production, Project, and Department. If the given
	 * occupation does not exist, it will be created. */
	private String occupation;

	/** Optional.  The name of the Document Packet which contains those
	 * on-boarding documents to be immediately distributed to the newly
	 * created user.  If this is blank or null, no documents will be
	 * distributed to the user.  If the specified packet does not exist,
	 * an error will be returned, but the user account will still be
	 * created. */
	private String packet;

	// The following are fields which are not part of the webService parameter string.
	// These are all marked with JsonIgnore annotation.

	/** Production to which this entry will be added. */
	@XmlTransient
	private Production production;

	public CreateUserContact() {
	}

	/** See {@link #email}. */
	public String getEmail() {
		return email;
	}
	/** See {@link #email}. */
	public void setEmail(String email) {
		this.email = email;
	}

	/** See {@link #lastName}. */
	@Override
	public String getLastName() {
		return lastName;
	}
	/** See {@link #lastName}. */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	/** See {@link #firstName}. */
	@Override
	public String getFirstName() {
		return firstName;
	}
	/** See {@link #firstName}. */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	/** See {@link #address}. */
	public String getAddress() {
		return address;
	}
	/** See {@link #address}. */
	public void setAddress(String address) {
		this.address = address;
	}

	/** See {@link #city}. */
	public String getCity() {
		return city;
	}
	/** See {@link #city}. */
	public void setCity(String city) {
		this.city = city;
	}

	/** See {@link #state}. */
	public String getState() {
		return state;
	}
	/** See {@link #state}. */
	public void setState(String state) {
		this.state = state;
	}

	/** See {@link #zip}. */
	public String getZip() {
		return zip;
	}
	/** See {@link #zip}. */
	public void setZip(String zip) {
		this.zip = zip;
	}

	/** See {@link #country}. */
	public String getCountry() {
		return country;
	}
	/** See {@link #country}. */
	public void setCountry(String country) {
		this.country = country;
	}

	/** See {@link #birthday}. */
	public String getBirthday() {
		return birthday;
	}
	/** See {@link #birthday}. */
	public void setBirthday(String birthday) {
		this.birthday = birthday;
	}

	/** See {@link #phone}. */
	@Override
	public String getPhone() {
		return phone;
	}
	/** See {@link #phone}. */
	public void setPhone(String phone) {
		this.phone = phone;
	}

	/** See {@link #emergencyContact}. */
	public String getEmergencyContact() {
		return emergencyContact;
	}
	/** See {@link #emergencyContact}. */
	public void setEmergencyContact(String emergencyContact) {
		this.emergencyContact = emergencyContact;
	}

	/** See {@link #emergencyPhone}. */
	public String getEmergencyPhone() {
		return emergencyPhone;
	}
	/** See {@link #emergencyPhone}. */
	public void setEmergencyPhone(String emergencyPhone) {
		this.emergencyPhone = emergencyPhone;
	}

	/** See {@link #emergencyRelationship}. */
	public String getEmergencyRelationship() {
		return emergencyRelationship;
	}
	/** See {@link #emergencyRelationship}. */
	public void setEmergencyRelationship(String emergencyRelationship) {
		this.emergencyRelationship = emergencyRelationship;
	}

	/** See {@link #productionId}. */
	public String getProductionId() {
		return productionId;
	}
	/** See {@link #productionId}. */
	public void setProductionId(String productionId) {
		this.productionId = productionId;
	}

	/** See {@link #project}. */
	public String getProject() {
		return project;
	}

	/** See {@link #project}. */
	public void setProject(String project) {
		this.project = project;
	}

	/** See {@link #department}. */
	@Override
	public String getDepartment() {
		return department;
	}
	/** See {@link #department}. */
	public void setDepartment(String department) {
		this.department = department;
	}

	/** See {@link #occupation}. */
	@Override
	public String getOccupation() {
		return occupation;
	}
	/** See {@link #occupation}. */
	public void setOccupation(String occupation) {
		this.occupation = occupation;
	}

	/** See {@link #packet}. */
	public String getPacket() {
		return packet;
	}
	/** See {@link #packet}. */
	public void setPacket(String packet) {
		this.packet = packet;
	}

	/** See {@link #production}. */
	@Override
	public Production getProduction() {
		return production;
	}
	/** See {@link #production}. */
	public void setProduction(Production production) {
		this.production = production;
	}

	/**
	 * @see com.lightspeedeps.model.Importable#getAction()
	 */
	@Override
	public ActionType getAction() {
		return ActionType.CREATE;
	}

	/**
	 * @see com.lightspeedeps.model.Importable#getEmailAddress()
	 */
	@Override
	public String getEmailAddress() {
		return getEmail();
	}


	/**
	 * @see com.lightspeedeps.model.Importable#getEpisodeCode()
	 */
	@Override
	public String getEpisodeCode() {
		return getProject();
	}

	/**
	 * @see com.lightspeedeps.model.Importable#getEpisodeTitle()
	 */
	@Override
	public String getEpisodeTitle() {
		return getProject();
	}

	/**
	 * @see com.lightspeedeps.model.Importable#getJobId()
	 */
	@Override
	public String getJobId() {
		return getProject();
	}

	/**
	 * @see com.lightspeedeps.model.Importable#getWorkCity()
	 */
	@Override
	public String getWorkCity() {
		return null;
	}

	/**
	 * @see com.lightspeedeps.model.Importable#getWorkState()
	 */
	@Override
	public String getWorkState() {
		return null;
	}

	/**
	 * @see com.lightspeedeps.model.Importable#getLastNameFirstName()
	 */
	@Override
	public String getLastNameFirstName() {
		String s = "";
		if (getLastName() != null) {
			s = getLastName();
		}
		if (getFirstName() != null) {
			s += ", " + getFirstName();
		}
		return s;
	}

	/**
	 * @see com.lightspeedeps.model.Importable#getProjectStartDate()
	 */
	@Override
	public Date getProjectStartDate() {
		return new Date();
	}

	/**
	 * @see com.lightspeedeps.model.Importable#getProjectedEndDate()
	 */
	@Override
	public Date getProjectedEndDate() {
		return null;
	}

}
