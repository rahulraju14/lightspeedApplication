package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * VehicleLog entity.
 *
 * @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "vehicle_log")
public class VehicleLog extends PersistentObject<VehicleLog> {
	private static final long serialVersionUID = 7563192235180177268L;

	// Fields
	private Dpr dpr;
	private String vehicleName;
	private String driver;
	private String passengers;
	private String vehicleId;
	private String note;

	// Constructors

	/** default constructor */
	public VehicleLog() {
	}

	/** full constructor */
/*	public VehicleLog(Dpr dpr, String vehicleName, String driver,
			String passengers, String vehicleId, String note) {
		this.dpr = dpr;
		this.vehicleName = vehicleName;
		this.driver = driver;
		this.passengers = passengers;
		this.vehicleId = vehicleId;
		this.note = note;
	}
*/
	// Property accessors

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "DPR_Id", nullable = false)
	public Dpr getDpr() {
		return this.dpr;
	}

	public void setDpr(Dpr dpr) {
		this.dpr = dpr;
	}

	@Column(name = "Vehicle_Name", length = 50)
	public String getVehicleName() {
		return this.vehicleName;
	}

	public void setVehicleName(String vehicleName) {
		this.vehicleName = vehicleName;
	}

	@Column(name = "Driver", length = 50)
	public String getDriver() {
		return this.driver;
	}

	public void setDriver(String driver) {
		this.driver = driver;
	}

	@Column(name = "Passengers", length = 1000)
	public String getPassengers() {
		return this.passengers;
	}

	public void setPassengers(String passengers) {
		this.passengers = passengers;
	}

	@Column(name = "Vehicle_Id", length = 50)
	public String getVehicleId() {
		return this.vehicleId;
	}

	public void setVehicleId(String vehicleId) {
		this.vehicleId = vehicleId;
	}

	@Column(name = "Note", length = 1000)
	public String getNote() {
		return this.note;
	}

	public void setNote(String note) {
		this.note = note;
	}

}