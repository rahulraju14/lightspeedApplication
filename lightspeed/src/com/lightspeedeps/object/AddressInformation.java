package com.lightspeedeps.object;

/** The wrapper class AddressInformation is used to hold the Zip, City name,
 * State and selected status
 */
public class AddressInformation {

	private String zipCode;
	private String city;
	private String stateCode;
	private boolean selected;

	/**
	 * 
	 */
	public AddressInformation() {
		// TODO Auto-generated constructor stub
	}
	/**
	 * @param zipCode
	 * @param city
	 * @param stateCode
	 * @param selected
	 */
	public AddressInformation(String zipCode, String city, String stateCode, boolean selected) {
		super();
		this.zipCode = zipCode;
		this.city = city;
		this.stateCode = stateCode;
		this.selected = selected;
	}

	/** See {@link #zipCode. */
	public String getZipCode() {
		return zipCode;
	}

	/** See {@link #zipCode. */
	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}

	/** See {@link #city. */
	public String getCity() {
		return city;
	}

	/** See {@link #city. */
	public void setCity(String city) {
		this.city = city;
	}

	/** See {@link #stateCode. */
	public String getStateCode() {
		return stateCode;
	}

	/** See {@link #stateCode. */
	public void setStateCode(String stateCode) {
		this.stateCode = stateCode;
	}

	/** See {@link #selected}. */
	public boolean getSelected() {
		return selected;
	}

	/** See {@link #selected}. */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}
}
