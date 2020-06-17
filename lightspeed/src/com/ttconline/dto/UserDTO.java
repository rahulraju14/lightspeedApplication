package com.ttconline.dto;

import java.util.Date;

import com.lightspeedeps.model.User;

/**
* Data Transfer object holds the user information
*
* @author  Mohamed Abuthahir
* @version 1.0
* @since   2018-07-31
*/

public class UserDTO {

	private int userId;
	private String email;
	private String password;
	private String firstName;
	private String lastName;
	private String zipCode;
	private String ssn;
	private Date dateOfBirth;
	private String dob;
	private String confirmPassword;
	private String address;
	private String token;
	private boolean agreeToTerms;
	private String status;
	private String clientIPAddress;
	private long registrationCount;
	private String lastUpdatedTimeStamp;
	private String lastUpdatedBy;
	/** User account number */
	private String userName;
	private boolean isTokenValid;


	/**
	 * Default constructor, required for JSON transfer.
	 */
	public UserDTO() {
	}

	/**
	 * Construct a UserDTO object from a (TTCO) User model instance.
	 * @param user The User instance.
	 */
	public UserDTO(User user) {
		setUserId(user.getId());
		setUserName(user.getAccountNumber());
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * @return the lastUpdatedBy
	 */
	public String getLastUpdatedBy() {
		return lastUpdatedBy;
	}

	/**
	 * @param lastUpdatedBy the lastUpdatedBy to set
	 */
	public void setLastUpdatedBy(String lastUpdatedBy) {
		this.lastUpdatedBy = lastUpdatedBy;
	}

	public String getLastUpdatedTimeStamp() {
		return lastUpdatedTimeStamp;
	}

	public void setLastUpdatedTimeStamp(String lastUpdatedTimeStamp) {
		this.lastUpdatedTimeStamp = lastUpdatedTimeStamp;
	}

	/**
	 * @return the registrationCount
	 */
	public long getRegistrationCount() {
		return registrationCount;
	}

	/**
	 * @param registrationCount the registrationCount to set
	 */
	public void setRegistrationCount(long registrationCount) {
		this.registrationCount = registrationCount;
	}

	/**
	 * @return the clientIPAddress
	 */
	public String getClientIPAddress() {
		return clientIPAddress;
	}

	/**
	 * @param clientIPAddress the clientIPAddress to set
	 */
	public void setClientIPAddress(String clientIPAddress) {
		this.clientIPAddress = clientIPAddress;
	}


	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getDob() {
		return dob;
	}

	public void setDob(String dob) {
		this.dob = dob;
	}

	public Date getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getZipCode() {
		return zipCode;
	}

	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}

	public String getSsn() {
		return ssn;
	}

	public void setSsn(String ssn) {
		this.ssn = ssn;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getConfirmPassword() {
		return confirmPassword;
	}

	public void setConfirmPassword(String confirmPassword) {
		this.confirmPassword = confirmPassword;
	}

	public boolean isAgreeToTerms() {
		return agreeToTerms;
	}

	public void setAgreeToTerms(boolean agreeToTerms) {
		this.agreeToTerms = agreeToTerms;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public boolean isTokenValid() {
		return isTokenValid;
	}

	public void setTokenValid(boolean isTokenValid) {
		this.isTokenValid = isTokenValid;
	}

}
