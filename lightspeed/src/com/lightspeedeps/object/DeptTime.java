package com.lightspeedeps.object;

import java.util.ArrayList;
import java.util.List;

import com.lightspeedeps.model.Department;
import com.lightspeedeps.model.ReportTime;

/**
 * DeptTimes entity. Used to group TimeCards or other time-related
 * data for display on the Daily Production Report (DPR).
 */
public class DeptTime implements java.io.Serializable {
	private static final long serialVersionUID = -8562987656287874563L;

	// Fields
	private Department department;
	private List<ReportTime> timeCards = new ArrayList<ReportTime>(0);

	// Constructors

	/** default constructor */
	public DeptTime(Department dept) {
		department = dept;
	}

	// Property accessors

	public Department getDepartment() {
		return this.department;
	}
	public void setDepartment(Department department) {
		this.department = department;
	}

	public List<ReportTime> getTimeCards() {
		return this.timeCards;
	}
	public void setTimeCards(List<ReportTime> crewCalls) {
		this.timeCards = crewCalls;
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += ", deptId=" + department.getId();
		if (timeCards != null) {
			s += ", crew calls:\n";
			for (ReportTime cc : timeCards) {
				s += cc.toString() + "\n";
			}
		}
		s += "]";
		return s;
	}

}