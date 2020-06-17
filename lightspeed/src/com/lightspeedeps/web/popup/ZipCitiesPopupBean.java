package com.lightspeedeps.web.popup;

import java.io.Serializable;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Address;
import com.lightspeedeps.object.AddressInformation;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.LocationUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;

/**
 * This class is the backing bean for the City selection pop up,
 * invoked when multiple cities are found for a ZIP code. 
 * It is used in My Account, W4 and W9 forms
 */
@ManagedBean
@ViewScoped
public class ZipCitiesPopupBean  extends PopupBean implements Serializable {

	private static final long serialVersionUID = 2291375198595390721L;

	private static final Log log = LogFactory.getLog(ZipCitiesPopupBean.class);

	private List<AddressInformation> cityStateList;

	private Address address;
	
	/** boolean used to render error message on popup itself user clicks OK without selecting any city */
	private boolean errormsg = false;

	public ZipCitiesPopupBean() {
		log.debug("");
	}
	
	public static ZipCitiesPopupBean getInstance(){
		return (ZipCitiesPopupBean)ServiceFinder.findBean("zipCitiesPopupBean");
	}

	/*
	 * @see com.lightspeedeps.web.popup.PopupBean#actionOk()
	 */
	@Override
	public String actionOk() {
		if (validateZipCites()) {
			LocationUtils.setCityStateByZipCode(getCityStateList(), address);
		}
		else {
			setErrormsg(true);
			return null;
		}
		return super.actionOk();
	}


	/*
	 * @see com.lightspeedeps.web.popup.PopupBean#actionCancel()
	 */
	@Override
	public String actionCancel() {
		try {
			super.actionCancel();
			address.clearCityStateZip();
			setErrormsg(false);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/*
	 * @see com.lightspeedeps.web.popup.PopupBean#actionClose(javax.faces.event.AjaxBehaviorEvent)
	 */
	@Override
	public void actionClose(AjaxBehaviorEvent evt) {
		actionCancel();
		super.actionClose(evt);
	}

	/*
	 * listenSelectedRowChange is used for get current row set true other row set false
	 */
	public void listenSelectedRowChange(ValueChangeEvent event) {
		boolean newValue = (boolean) event.getNewValue();
		if (newValue) {
			AddressInformation addInfo = (AddressInformation) event.getComponent().getAttributes().get("currentRow");
			setErrormsg(false);
			addInfo.setSelected(true);
			for (AddressInformation item : getCityStateList()) {
				if (!addInfo.getCity().equals(item.getCity())) {
					item.setSelected(false);
				}
			}
		}
	}

	/**
	 * @return true if from popup selected zip and city else return false
	 */
	private boolean validateZipCites() {
		for (AddressInformation addrInfo : getCityStateList()) {
			if (addrInfo.getSelected()) {
				return true;
			}
		}
		return false;
	}
	
	/** See {@link #cityStateList. */
	public List<AddressInformation> getCityStateList() {
		return cityStateList;
	}

	/** See {@link #cityStateList. */
	public void setCityStateList(List<AddressInformation> cityStateList) {
		this.cityStateList = cityStateList;
	}
	
	/** See {@link #address */
	public Address getAddress() {
		return address;
	}

	/** See {@link #address */
	public void setAddress(Address address) {
		this.address = address;
	}

	/** See {@link #errormsg */
	public boolean isErrormsg() {
		return errormsg;
	}

	/** See {@link #errormsg */
	public void setErrormsg(boolean errormsg) {
		this.errormsg = errormsg;
	}

}
