package com.lightspeedeps.model;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.lightspeedeps.web.onboard.SubjectBean;

@Entity
@Table(name = "subjects")
public class Subjects extends PersistentObject<Subjects> implements Cloneable {

	private static final long serialVersionUID = 1L;
	private String english;
	private String math;
	private String chemistry;
	private Student student;
	@Transient
	private Integer stud_Id;

	public Subjects() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Subjects(String english, String math, String chemistry, Student student) {
		super();
		this.english = english;
		this.math = math;
		this.chemistry = chemistry;
		this.student = student;
	}

	public String getEnglish() {
		return english;
	}

	public void setEnglish(String english) {
		this.english = english;
	}

	public String getMath() {
		return math;
	}

	public void setMath(String math) {
		this.math = math;
	}

	public String getChemistry() {
		return chemistry;
	}

	public void setChemistry(String chemistry) {
		this.chemistry = chemistry;
	}

	@OneToOne
	@JoinColumn(name = "stud_Id")
	public Student getStudent() {
		return student;
	}

	public void setStudent(Student student) {
		this.student = student;
	}

	@Override
	public String toString() {
		return "Subjects [english=" + english + ", math=" + math + ", chemistry=" + chemistry + ", student=" + student
				+ "]";

	}
	@Transient
	public Integer getStud_Id() {
		if (stud_Id == null) {
			SubjectBean.storeStudentId();
		}
		return stud_Id;
	}

	public void setStud_Id(Integer stud_Id) {
		this.stud_Id = stud_Id;
	}

}
