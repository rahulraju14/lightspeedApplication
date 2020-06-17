package com.lightspeedeps.web.onboard;

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;
import org.icefaces.ace.model.table.RowStateMap;

import com.lightspeedeps.dao.SubjectsDAO;
import com.lightspeedeps.model.Subjects;

@ManagedBean(name = "subjectBean")
@ViewScoped
public class SubjectBean {
	private static final Logger log = Logger.getLogger(SubjectBean.class);
	private static List<Subjects> subjects;
	private int selectedTab = 1;
	private static RowStateMap stateMap = new RowStateMap();

	public SubjectBean() {

		subjects = fetchSubjects();
	}

	public static void storeStudentId() {
		for (Subjects s : subjects) {
			int value = s.getStudent().getId();
			s.setStud_Id(value);
		}

	}

	private List<Subjects> fetchSubjects() {

		List<Subjects> theSubjects = new ArrayList<Subjects>();
		theSubjects = SubjectsDAO.getInstance().getSubjects();
		log.info(theSubjects);

		return theSubjects;
	}

	public List<Subjects> getSubjects() {
		return subjects;
	}

	@SuppressWarnings("static-access")
	public void setSubjects(List<Subjects> subjects) {
		this.subjects = subjects;
	}

	@Override
	public String toString() {
		return "SubjectBean [subjects=" + subjects + "]";
	}

	public int getSelectedTab() {
		return selectedTab;
	}

	public void setSelectedTab(int selectedTab) {
		this.selectedTab = selectedTab;
	}

	public RowStateMap getStateMap() {
		return stateMap;
	}

	@SuppressWarnings("static-access")
	public void setStateMap(RowStateMap stateMap) {
		this.stateMap = stateMap;
	}

}
