package com.lightspeedeps.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.lightspeedeps.util.common.StringUtils;

/**
 * Agent entity. @author MyEclipse Persistence Tools
 * 
 * Person who is representing a performer for a particular job.
 * 
 */
@Entity
@Table(name = "agent")
public class Agent extends PersistentObject<Agent> {
	private static final long serialVersionUID = - 8088353961217883067L;
	
	// Fields    
	/** Agent first name */
	private String firstName;
	/** Agent last name */
	private String lastName;
	/** combination of the first and last names for display purposed. */
	private String displayName;
	/** Agent's email address */
	private String emailAddress;
	/** Agent's office phone # */
	private String officePhone;
	/** Name of the agency the agent is working for. */
	private String agencyName;
	/** Address of the agency */
	private Address agencyAddress;
	/** Collection of users associated with this agent */
	private List<User> usersList;
	/* Whether or not this is the currently selected agent 
	 *  The selected agent will be used to prefill the 
	 *  performer contract agent fields
	 */
	private boolean selected;
	
	// Constructors

	/** default constructor */
	public Agent() {
		agencyAddress = new Address();
		usersList = new ArrayList<>();
	}

	// Property accessors
	
	/** See {@link #firstName}. */
	@Column(name = "first_name", length = 30)
	public String getFirstName() {
		return this.firstName;
	}

	/** See {@link #firstName}. */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	/** See {@link #lastName}. */
	@Column(name = "last_name", length = 30)
	public String getLastName() {
		return this.lastName;
	}

	/** See {@link #lastName}. */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	/** See {@link #displayName}. */
	@Column(name = "display_name", length = 62)
	public String getDisplayName() {
		return this.displayName;
	}

	/** See {@link #displayName}. */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/** See {@link #emailAddress}. */
	@Column(name = "email_address", length = 100)
	public String getEmailAddress() {
		return this.emailAddress;
	}

	/** See {@link #emailAddress}. */
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	/** See {@link #officePhone}. */
	@Column(name = "office_phone", length = 25)
	public String getOfficePhone() {
		return this.officePhone;
	}

	/** See {@link #officePhone}. */
	public void setOfficePhone(String officePhone) {
		this.officePhone = officePhone;
	}

	/** See {@link #agencyName}. */
	@Column(name = "agency_name", length = 50)
	public String getAgencyName() {
		return this.agencyName;
	}

	/** See {@link #agencyName}. */
	public void setAgencyName(String agencyName) {
		this.agencyName = agencyName;
	}

	/** See {@link #agencyAddress}. */
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Agency_Address_Id")	public Address getAgencyAddress() {
		return this.agencyAddress;
	}

	/** See {@link #agencyAddress}. */
	public void setAgencyAddress(Address agencyAddress) {
		this.agencyAddress = agencyAddress;
	}

	@ManyToMany(
			targetEntity=User.class,
			cascade={CascadeType.PERSIST, CascadeType.MERGE}
		)
	@JoinTable( name = "agent_user",
			joinColumns = {@JoinColumn(name = "Agent_Id")},
			inverseJoinColumns = {@JoinColumn(name = "User_Id")}
			)
	/** See {@link #usersList}. */
	public List<User> getUsersList() {
		return usersList;
	}

	/** See {@link #usersList}. */
	public void setUsersList(List<User> usersList) {
		this.usersList = usersList;
	}

	/** See {@link #selected}. */
	@Column(name = "Selected")
	public boolean getSelected() {
		return selected;
	}

	/** See {@link #selected}. */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
		return result;
	}

	/**
	 * Compares Agent objects based on their database id and/or name.
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		Agent other = null;
		try {
			other = (Agent)obj;
		}
		catch (Exception e) {
			return false;
		}
		if (getId() == null) {
			if (other.getId() != null) {
				return false;
			}
			else { // neither one is persisted, compare names
				return StringUtils.compare(displayName, other.displayName) == 0;
			}
		}
		return getId().equals(other.getId());
	}

}
