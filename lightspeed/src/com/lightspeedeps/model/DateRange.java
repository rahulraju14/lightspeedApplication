package com.lightspeedeps.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * DateRange entity.
 *
 * @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "date_range")
public class DateRange extends PersistentObject<DateRange> implements Comparable<DateRange> {

	private static final long serialVersionUID = - 7041631745411336188L;

	// Fields
	private RealWorldElement realWorldElement;
	private Date startDate;
	private Date endDate;
	private String description;

	// Constructors

	/** default constructor */
	public DateRange() {
	}

	/** full constructor */
/*	public DateRange(RealWorldElement realWorldElement, Date startDate,
			Date endDate, String description) {
		this.realWorldElement = realWorldElement;
		this.startDate = startDate;
		this.endDate = endDate;
		this.description = description;
	}
*/
	// Property accessors

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Real_World_Element_Id")
	public RealWorldElement getRealWorldElement() {
		return this.realWorldElement;
	}

	public void setRealWorldElement(RealWorldElement realWorldElement) {
		this.realWorldElement = realWorldElement;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "Start_Date", nullable = false, length = 0)
	public Date getStartDate() {
		return this.startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "End_Date", length = 0)
	public Date getEndDate() {
		return this.endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	@Column(name = "Description", length = 1000)
	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
		result = prime * result + ((getStartDate() == null) ? 0 : getStartDate().hashCode());
		result = prime * result + ((getEndDate() == null) ? 0 : getEndDate().hashCode());
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		DateRange other;
		try {
			other = (DateRange)obj;
		}
		catch (Exception e) {
			return false;
		}
		if ( getId() != null && getId().equals(other.getId()) ) {
			return true;
		}
		return (compareTo(other) == 0);
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(DateRange other) {
		if (other == null) {
			return 1;
		}
		int comp = getStartDate().compareTo(other.getStartDate());
		if (comp == 0) {
			comp = getEndDate().compareTo(other.getEndDate());
		}
		return comp;
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += '[';
		s += "id=" + (getId()==null ? "null" : getId());
		if (getStartDate() != null) {
			s += ", start=" + getStartDate();
		}
		if (getEndDate() != null) {
			s += ", end=" + getEndDate();
		}
		s += ']';
		return s;
	}

}
