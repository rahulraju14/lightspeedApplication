package com.lightspeedeps.web.onboard;

import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.event.ValueChangeEvent;

import org.apache.log4j.Logger;

import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.web.popup.PopupBean;
import com.opencsv.CSVWriter;

@ManagedBean(name = "zipCodeBean")
@ViewScoped
public class ZipCodeBean extends PopupBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(ZipCodeBean.class);
	private List<Country> theCountry;
	private Integer zipCode;
	private String city;
	private Country country;

	public ZipCodeBean() {
		theCountry = getCountry();

	}

	@Override
	public void actionClose(AjaxBehaviorEvent evt) {
		actionCancel();
		super.actionClose(evt);
	}

	@Override
	public String actionCancel() {
		try {
			super.actionCancel();
			country.clearData();
		} catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	@Override
	public String actionOk() {
		return super.actionOk();
	}

	public void listenSelectedRowChange(ValueChangeEvent event) {
		boolean newValue = (boolean) event.getNewValue();
		if (newValue) {
			log.info(newValue);
			Country country = (Country) event.getComponent().getAttributes().get("currentRow");
			country.setSelected(true);
			for (Country con : theCountry) {
				if (!(country.getCity().equals(con.getCity()))) {
					con.setSelected(false);
				}
			}
		}

	}

	/**
	 * This method returns a list odf countries which is hardcoded for temporary
	 * purpose
	 * 
	 * @return
	 */
	public List<Country> getCountry() {
		List<Country> con = new ArrayList<Country>();
		con.add(new Country(400089, "Mumbai"));
		con.add(new Country(400089, "Banglore"));

		con.add(new Country(400089, "kolkata"));
		con.add(new Country(400089, "Pune"));
		con.add(new Country(400089, "Jaipur"));
		/*
		 * con.add(new Country(400089, "Jaipur")); con.add(new Country(400089,
		 * "Jaipur")); con.add(new Country(400089, "Jaipur")); con.add(new
		 * Country(400089, "Jaipur")); con.add(new Country(400089, "Jaipur"));
		 * con.add(new Country(400089, "Jaipur")); con.add(new Country(400089,
		 * "Jaipur")); con.add(new Country(400089, "Jaipur")); con.add(new
		 * Country(400089, "Jaipur")); con.add(new Country(400089, "Jaipur"));
		 * con.add(new Country(400089, "Jaipur")); con.add(new Country(400089,
		 * "Jaipur")); con.add(new Country(400089, "Jaipur")); con.add(new
		 * Country(400089, "Jaipur")); con.add(new Country(400089, "Jaipur"));
		 * con.add(new Country(400089, "Jaipur")); con.add(new Country(400089,
		 * "Jaipur")); con.add(new Country(400089, "Jaipur")); con.add(new
		 * Country(400089, "Jaipur")); con.add(new Country(400089, "Jaipur"));
		 * con.add(new Country(400089, "Jaipur")); con.add(new Country(400089,
		 * "Jaipur")); con.add(new Country(400089, "Jaipur")); con.add(new
		 * Country(400089, "Jaipur")); con.add(new Country(400089, "Jaipur"));
		 * con.add(new Country(400089, "Jaipur")); con.add(new Country(400089,
		 * "Jaipur")); con.add(new Country(400089, "Jaipur")); con.add(new
		 * Country(400089, "Jaipur")); con.add(new Country(400089, "Jaipur"));
		 * con.add(new Country(400089, "Jaipur")); con.add(new Country(400089,
		 * "Jaipur")); con.add(new Country(400089, "Jaipur")); con.add(new
		 * Country(400089, "Jaipur")); con.add(new Country(400089, "Jaipur"));
		 * con.add(new Country(400089, "Jaipur")); con.add(new Country(400089,
		 * "Jaipur")); con.add(new Country(400089, "Jaipur")); con.add(new
		 * Country(400089, "Jaipur")); con.add(new Country(400089, "Jaipur"));
		 * con.add(new Country(400089, "Jaipur")); con.add(new Country(400089,
		 * "Jaipur")); con.add(new Country(400089, "Jaipur")); con.add(new
		 * Country(400089, "Jaipur")); con.add(new Country(400089, "Jaipur"));
		 * con.add(new Country(400089, "Jaipur")); con.add(new Country(400089,
		 * "Jaipur"));
		 */

		return con;

	}

	/**
	 * This method is basically responsible for exporting data of selected radio
	 * button where it collects that object and saves it in csv file
	 * 
	 * @param theCountry
	 */
	public void exportSelectedData(Country theCountry) {
		log.info("Welcome to csv data");
		String path = "C:/csv/ExportSelectedcity.csv";
		try {
			File f = new File(path);
			FileWriter fw = new FileWriter(f);
			CSVWriter cw = new CSVWriter(fw);
			List<String[]> data = new ArrayList<String[]>();
			if (theCountry != null) {
				data.add(new String[] { "ZipCode", "City" });
				data.add(new String[] { theCountry.getZipCode().toString(), theCountry.getCity() });

				cw.writeAll(data);
				FacesContext facesContext = FacesContext.getCurrentInstance();
				FacesMessage facesMessage = new FacesMessage("Data Exported Sucessfully!!!!!");
				facesContext.addMessage("msg", facesMessage);
				log.info("Data Exported Sucessfully!!!!");
				cw.close();
			}

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			log.info(e.getMessage());
		}
	}

	public static ZipCodeBean getInstance() {
		return (ZipCodeBean) ServiceFinder.findBean("zipCodeBean");
	}

	public List<Country> getTheCountry() {
		return theCountry;
	}

	public void setTheCountry(List<Country> theCountry) {
		this.theCountry = theCountry;
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

	public void setCountry(Country country) {
		this.country = country;
	}


}
