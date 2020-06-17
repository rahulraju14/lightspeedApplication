package com.lightspeedeps.web.onboard;

import com.lightspeedeps.util.app.ServiceFinder;

public class Country {
	private Integer zipCode;
	private String city;
	private boolean selected;

	public Country() {

	}

	public static Country getInstance() {
		return (Country) ServiceFinder.findBean("country");
	}

	public Country(Integer zipCode, String city) {
		super();
		this.zipCode = zipCode;
		this.city = city;
	}

	public Integer getZipCode() {
		return zipCode;
	}

	public void setZipCode(Integer zipCode) {
		this.zipCode = zipCode;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	@Override
	public String toString() {
		return "Country [zipCode=" + zipCode + ", city=" + city + "]";
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public void clearData() {
		setCity(null);
		setZipCode(null);
	}
}
