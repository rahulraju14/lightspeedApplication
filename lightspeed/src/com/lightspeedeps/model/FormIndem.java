package com.lightspeedeps.model;

import javax.persistence.*;

import org.hibernate.annotations.Type;

/** This entity contains all the fields of the Indemnification form.
 * It is used to maintain the user wise record of the Indemnification entries.
 *
 */
@Entity
@Table(name = "form_indem")
public class FormIndem extends Form<FormIndem> {

	private static final long serialVersionUID = 6280468390897627419L;

	private boolean companyType;

	private String companyName;

	private String workerName;

	private String fullCompanyName;

	private String corpOfficerName;

	private String fedidNumber;

	private String stateOfReg;

	private Address address;

	private String telephone;

	private String email;

	@Column(name = "Company_Type" , nullable = false)
	public boolean getCompanyType() {
		return companyType;
	}
	public void setCompanyType(boolean companyType) {
		this.companyType = companyType;
	}

	@Column(name = "Company_Name", nullable = true, length = 35)
	public String getCompanyName() {
		return companyName;
	}
	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	@Column(name = "Worker_Name", nullable = true, length = 60)
	public String getWorkerName() {
		return workerName;
	}
	public void setWorkerName(String workerName) {
		this.workerName = workerName;
	}

	@Column(name = "Full_Company_Name", nullable = true, length = 100)
	public String getFullCompanyName() {
		return fullCompanyName;
	}
	public void setFullCompanyName(String fullCompanyName) {
		this.fullCompanyName = fullCompanyName;
	}

	@Column(name = "Corp_Officer_Name", nullable = true, length = 60)
	public String getCorpOfficerName() {
		return corpOfficerName;
	}
	public void setCorpOfficerName(String corpOfficerName) {
		this.corpOfficerName = corpOfficerName;
	}

	@Type(type="encryptedString") // defined in User.java
	@Column(name = "Fedid_Number", nullable = true, length = 1000)
	public String getFedidNumber() {
		return fedidNumber;
	}
	public void setFedidNumber(String fedidNumber) {
		this.fedidNumber = fedidNumber;
	}

	@Column(name = "State_Of_Reg", nullable = true, length = 10)
	public String getStateOfReg() {
		return stateOfReg;
	}
	public void setStateOfReg(String stateOfReg) {
		this.stateOfReg = stateOfReg;
	}

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "Address_Id")
	public Address getAddress() {
		return address;
	}
	public void setAddress(Address address) {
		this.address = address;
	}

	@Column(name = "Telephone", nullable = true, length = 25)
	public String getTelephone() {
		return telephone;
	}
	public void setTelephone(String telephone) {
		this.telephone = telephone;
	}

	@Column(name = "Email", nullable = true, length = 100)
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	public void trim() {
		companyName = trim(companyName);
		workerName = trim(workerName);
		fullCompanyName = trim(fullCompanyName);
		corpOfficerName = trim(corpOfficerName);
		fedidNumber = trim(fedidNumber);
		stateOfReg = trim(stateOfReg);
		telephone = trim(telephone);
		email = trim(email);
	}

}
