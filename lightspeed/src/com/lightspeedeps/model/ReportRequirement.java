package com.lightspeedeps.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import com.lightspeedeps.type.ReportType;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import com.lightspeedeps.type.ReportFrequency;

/**
 * ReportRequirement entity.
 *
 * @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "report_requirement", uniqueConstraints = @UniqueConstraint(columnNames = {
		"Project_Id", "Type", "Unit_Number" }))
public class ReportRequirement extends PersistentObject<ReportRequirement> {
	// private static final Log log = LogFactory.getLog(ReportRequirement.class);

	private static final long serialVersionUID = -7780347198317201949L;

	// Fields

	private ReportType type;
	/** The Unit number associated with this ReportRequirement. */
	private Integer unitNumber = 1;
//	private Role role;
	private Contact contact;
	private Project project;
	private String description;
	private ReportFrequency frequency;
	private Date firstDate;

	// Constructors

	/** default constructor */
	public ReportRequirement() {
	}

	/** full constructor */
	public ReportRequirement(Role role, Contact contact, Project project, ReportType type,
			String description, ReportFrequency frequency, Date firstDate) {
//		this.role = role;
		this.contact = contact;
		this.project = project;
		this.type = type;
		this.description = description;
		this.frequency = frequency;
		this.firstDate = firstDate;
	}

	// Property accessors

	@Column(name = "Unit_Number", nullable = false)
	/** See {@link #unitNumber}. */
	public Integer getUnitNumber() {
		return unitNumber;
	}
	/** See {@link #unitNumber}. */
	public void setUnitNumber(Integer number) {
		unitNumber = number;
	}

	//@ManyToOne(fetch = FetchType.LAZY) // Changed to Lazy rev 3.0.4801
	//@JoinColumn(name = "Responsible_Role_Id")
	@Transient // no longer supported
	public Role getRole() {
		return null;
		//return role;
	}
	public void setRole(Role role) {
		//this.role = role;
		// log.debug("this=" + getId() + ", role="+role);
	}

	@ManyToOne(fetch = FetchType.LAZY) // Changed to Lazy rev 3.0.4801
	@JoinColumn(name = "Responsible_Party_Id")
	public Contact getContact() {
		return contact;
	}
	public void setContact(Contact contact) {
		this.contact = contact;
		// log.debug("this=" + getId() + ", contact="+contact);
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Project_Id", nullable = false)
	public Project getProject() {
		return project;
	}
	public void setProject(Project project) {
		this.project = project;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Type", nullable = false, length = 30)
	public ReportType getType() {
		return type;
	}
	public void setType(ReportType type) {
		this.type = type;
	}

	@Column(name = "Description", length = 100)
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Frequency", nullable = false, length = 30)
	public ReportFrequency getFrequency() {
		return frequency;
	}
	public void setFrequency(ReportFrequency frequency) {
		this.frequency = frequency;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "First_Date", length = 0)
	public Date getFirstDate() {
		return firstDate;
	}
	public void setFirstDate(Date firstDate) {
		this.firstDate = firstDate;
	}

	@Transient
	public boolean isActive() {
		return getContact() != null;
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += " [" + getType() + ", " + getFrequency();
//		if (getRole() != null) {
//			s += ", " + getRole().getName();
//		}
		if (getContact() != null) {
			s += ", contact=" + getContact().getId();
		}
		s += "]";
		return s;
	}

}
