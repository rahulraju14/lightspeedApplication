package com.lightspeedeps.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import com.lightspeedeps.type.ProductionType;
import com.lightspeedeps.type.ReportStatus;

/**
 * ExhibitG entity.
 *
 * @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "exhibit_g", uniqueConstraints = @UniqueConstraint(columnNames = {
		"Date", "Project_Id", "Unit_Number" }))
public class ExhibitG extends PersistentObject<ExhibitG> {
	private static final long serialVersionUID = 1513190474111567277L;

	// Fields
	private Project project;
	private ReportStatus status;
	private Date date;
	private Date updated;

	/** The Unit number associated with this ExhibitG. */
	private Integer unitNumber = 1;

	/** The type of production: TV series, movie, documentary, etc.
	 * @see ProductionType
	*/
	private ProductionType type = ProductionType.FEATURE_FILM;

	private String title;
	private String company;
	private String location;
	private String productionNumber;
	private String contactName;
	private String contactPhone;
	private Boolean dayOff = false;

	private List<ReportTime> timeCards = new ArrayList<ReportTime>(0);

	// Constructors

	/** default constructor */
	public ExhibitG() {
	}

	/** minimal constructor */
	public ExhibitG(Project project, ReportStatus status, Date date, Boolean dayOff) {
		this.project = project;
		this.status = status;
		this.date = date;
		this.dayOff = dayOff;
	}

	/** full constructor */
/*	public ExhibitG(Project project, ReportStatus status, Date date, String title,
			String company, String location, String productionNumber,
			String contactName, String contactPhone, Boolean dayOff) {
		this.project = project;
		this.status = status;
		this.date = date;
		this.title = title;
		this.company = company;
		this.location = location;
		this.productionNumber = productionNumber;
		this.contactName = contactName;
		this.contactPhone = contactPhone;
		this.dayOff = dayOff;
	}
*/
	// Property accessors

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Project_Id", nullable = false)
	public Project getProject() {
		return this.project;
	}
	public void setProject(Project project) {
		this.project = project;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Status", nullable = false)
	public ReportStatus getStatus() {
		return this.status;
	}
	public void setStatus(ReportStatus status) {
		this.status = status;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Date", nullable = false, length = 0)
	public Date getDate() {
		return this.date;
	}
	public void setDate(Date date) {
		this.date = date;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Updated", length = 0)
	public Date getUpdated() {
		return this.updated;
	}
	public void setUpdated(Date updated) {
		this.updated = updated;
	}

	@Column(name = "Unit_Number", nullable = false)
	/** See {@link #unitNumber}. */
	public Integer getUnitNumber() {
		return this.unitNumber;
	}
	/** See {@link #unitNumber}. */
	public void setUnitNumber(Integer number) {
		this.unitNumber = number;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Type", nullable = false, length = 30)
	public ProductionType getType() {
		return this.type;
	}
	public void setType(ProductionType type) {
		this.type = type;
	}

	@Column(name = "Title", length = 100)
	public String getTitle() {
		return this.title;
	}
	public void setTitle(String title) {
		this.title = title;
	}

	@Column(name = "Company", length = 100)
	public String getCompany() {
		return this.company;
	}
	public void setCompany(String company) {
		this.company = company;
	}

	@Column(name = "Location", length = 100)
	public String getLocation() {
		return this.location;
	}
	public void setLocation(String location) {
		this.location = location;
	}

	@Column(name = "Production_Number", length = 25)
	public String getProductionNumber() {
		return this.productionNumber;
	}
	public void setProductionNumber(String productionNumber) {
		this.productionNumber = productionNumber;
	}

	@Column(name = "Contact_Name", length = 50)
	public String getContactName() {
		return this.contactName;
	}
	public void setContactName(String contactName) {
		this.contactName = contactName;
	}

	@Column(name = "Contact_Phone", length = 25)
	public String getContactPhone() {
		return this.contactPhone;
	}
	public void setContactPhone(String contactPhone) {
		this.contactPhone = contactPhone;
	}

	@Column(name = "Day_Off", nullable = false)
	public Boolean getDayOff() {
		return this.dayOff;
	}
	public void setDayOff(Boolean dayOff) {
		this.dayOff = dayOff;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "exhibitG")
	@OrderBy("castId")
	public List<ReportTime> getTimeCards() {
		return this.timeCards;
	}
	public void setTimeCards(List<ReportTime> timeCards) {
		this.timeCards = timeCards;
	}

}