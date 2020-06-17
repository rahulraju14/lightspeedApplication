package com.lightspeedeps.model;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.log4j.Logger;

import com.lightspeedeps.web.onboard.StudentBean;

@Entity
@Table(name = "student")
public class Student extends PersistentObject<Student> implements Cloneable {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Logger.getLogger(Student.class);
	private String firstName;
	private String lastName;
	private String emailId;
	private String mobileNo;
	@Transient
	private boolean checked;
	@Transient
	private Integer[] percentageArray;
	@Transient
	private Integer stId;

	public Student() {
		super();
		// TODO Auto-generated constructor stub
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

	public String getEmailId() {
		return emailId;
	}

	public void setEmailId(String emailId) {
		this.emailId = emailId;
	}

	public String getMobileNo() {
		return mobileNo;
	}

	public void setMobileNo(String mobileNo) {
		this.mobileNo = mobileNo;
	}

	@Override
	public String toString() {
		return "Student [firstName=" + firstName + ", lastName=" + lastName + ", emailId=" + emailId + ", mobileNo="
				+ mobileNo + "]";
	}

	@Transient
	public boolean isChecked() {
		return checked;
	}

	public void setChecked(boolean checked) {
		this.checked = checked;

	}

	@Transient
	public Integer[] getPercentageArray() {
		if (percentageArray == null) {
			StudentBean.createStatusGraph();
		}
		return percentageArray;
	}

	public void setPercentageArray(Integer[] percentageArray) {
		this.percentageArray = percentageArray;
	}

	@Transient
	public Integer getStId() {
		if (stId == null) {
			StudentBean.studentIdMethod();
			log.info(stId);
		}
		return stId;
	}

	public void setStId(Integer stId) {
		this.stId = stId;
	}

}
