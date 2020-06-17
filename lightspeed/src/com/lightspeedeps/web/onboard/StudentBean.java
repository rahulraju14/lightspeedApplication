package com.lightspeedeps.web.onboard;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;

import org.apache.log4j.Logger;
import org.icefaces.ace.model.table.RowState;
import org.icefaces.ace.model.table.RowStateMap;

import com.lightspeedeps.dao.StudentDAO;
import com.lightspeedeps.dao.SubjectsDAO;
import com.lightspeedeps.model.Student;
import com.lightspeedeps.model.Subjects;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.opencsv.CSVWriter;

@ManagedBean(name = "studentBean")
@ViewScoped
public class StudentBean {

	private static final Logger log = Logger.getLogger(StudentBean.class);
	private boolean checkedForAll = false;
	private static List<Student> studentList;
	private List<Student> checkedStudentList;
	private List<Subjects> subjects;
	private List<Student> masterCheckedStudents;
	private List<Subjects> sub;
	private Integer studentid;
	private String data;
	private RowStateMap stateMap = new RowStateMap();
	private Country theCountry; 
	private Integer storeKey;
	
	
	public StudentBean() {
		studentList = fetchStudentList();
		subjects = fetchSubjects();
		showStoreKey();
	

	}
	
	public void showStoreKey()
	{
		log.info(getStoreKey());
	}
	
	public void popUp() {
		ZipCodeBean bean = ZipCodeBean.getInstance();
		bean.show(null, 0, "StudentBean.ZipCodePopup.Title", "Confirm.Enter", "Confirm.Close");
			
	}

	private List<Subjects> fetchSubjects() {

		List<Subjects> theSubjects = new ArrayList<Subjects>();
		theSubjects = SubjectsDAO.getInstance().getSubjects();
		log.info(theSubjects);

		return theSubjects;
	}

	public static void studentIdMethod() {
		for (Student stud : studentList) {
			int value = stud.getId();
			stud.setStId(value);
		}

	}

	public List<Student> fetchStudentList() {
		List<Student> theStudents = new ArrayList<Student>();
		theStudents = StudentDAO.getInstance().findAll();
		log.info("Students lists: " + theStudents);

		return theStudents;
	}

	/**
	 * This method is used to create status graph for the subjects.
	 */
	public static void createStatusGraph() {

		StudentStatusCount statusGraph = new StudentStatusCount();
		for (Student stud : studentList) {
			int id = stud.getId();
			Subjects sub = SubjectsDAO.getInstance().getSubjectById(id);
			if (sub.getEnglish().equals("pass") && sub.getMath().equals("pass") && sub.getChemistry().equals("pass")) {
				statusGraph.setGreenCount(100);
				statusGraph.setBlackCount(0);
				statusGraph.setRedCount(0);
				stud.setPercentageArray(StudentDocumentUtil.setStatusGraph(statusGraph));
			} else if (sub.getEnglish().equals("fail") && sub.getMath().equals("fail")
					&& sub.getChemistry().equals("fail")) {
				statusGraph.setRedCount(100);
				statusGraph.setGreenCount(0);
				statusGraph.setBlackCount(0);
				stud.setPercentageArray(StudentDocumentUtil.setStatusGraph(statusGraph));
			} else if (sub.getEnglish().equals("notgiven") && sub.getMath().equals("notgiven")
					&& sub.getChemistry().equals("notgiven")) {
				statusGraph.setBlackCount(100);
				statusGraph.setGreenCount(0);
				statusGraph.setRedCount(0);
				stud.setPercentageArray(StudentDocumentUtil.setStatusGraph(statusGraph));
			}

			if (sub.getEnglish().equals("pass") || sub.getMath().equals("pass") || sub.getChemistry().equals("pass")) {
				statusGraph.setGreenCount(40);

			}
			if (sub.getEnglish().equals("fail") || sub.getMath().equals("fail") || sub.getChemistry().equals("fail")) {
				statusGraph.setRedCount(40);
			}

			if (sub.getEnglish().equals("notgiven") || sub.getMath().equals("notgiven")
					|| sub.getChemistry().equals("notgiven")) {
				statusGraph.setBlackCount(20);
			}

			stud.setPercentageArray(StudentDocumentUtil.setStatusGraph(statusGraph));

		}
	}

	public void masterCheckBoxListener(ValueChangeEvent evt) {
		try {

			if (masterCheckedStudents == null) {
				masterCheckedStudents = new ArrayList<>();
			}
			if (getCheckedForAll()) {

				for (Student stud : studentList) {
					// log.info(stud.getId()+stud.getFirstName()+stud.getLastName()+stud.getMobileNo()+stud.getEmailId());
					stud.setChecked(true);
					masterCheckedStudents.add(stud);

				}
			}

			else {
				for (Student stud : studentList) {
					stud.setChecked(false);
					

				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}

	}

	public void listenSingleCheck(ValueChangeEvent evt) {
		try {
			Student stud = (Student) evt.getComponent().getAttributes().get("selectedRow");
			log.info(stud);
			if (stud != null) {
				if (checkedStudentList == null) {
					checkedStudentList = new ArrayList<>();
				}
				if (stud.isChecked()) {
					log.info(stud.isChecked());
					checkedStudentList.add(stud);

				} else if (checkedStudentList.contains(stud)) {

					checkedStudentList.remove(stud);
				}
				setCheckedForAll(false);
			}
		} catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	public void exportSelectedData() {
		log.info("Welcome to csv data");
		boolean k=getCheckedForAll();
		log.info(k);
		String path = "C:/csv/ExportSelectedData.csv";
		try {
			File f = new File(path);
			FileWriter fw = new FileWriter(f);
			CSVWriter cw = new CSVWriter(fw);
			List<String[]> data = new ArrayList<String[]>();
			data.add(new String[] { "StudentId", "FirstName", "LastName", "Email Id", "Mobile No" });

			if (checkedStudentList != null) {
				for (Student stud : checkedStudentList) {
					data.add(new String[] { stud.getId().toString(), stud.getFirstName(), stud.getLastName(),
							stud.getEmailId(), stud.getMobileNo() });
					stud.setChecked(false);
				}

				cw.writeAll(data);
				FacesContext facesContext = FacesContext.getCurrentInstance();
				FacesMessage facesMessage = new FacesMessage("Data Exported Sucessfully!!!!!");
				facesContext.addMessage("export", facesMessage);
				log.info("Data Exported Sucessfully!!!!");
				cw.close();
			} else {
				FacesContext facesContext = FacesContext.getCurrentInstance();
				FacesMessage facesMessage = new FacesMessage("No Selection Made");
				facesContext.addMessage("export", facesMessage);
				log.debug(facesMessage);
			}

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			log.info(e.getMessage());
		}

	}

	public void exportAllData() {
		if(!getCheckedForAll())
		{
			FacesContext facesContext = FacesContext.getCurrentInstance();
			FacesMessage facesMessage = new FacesMessage("Student Not Selected");
			facesContext.addMessage("exportall", facesMessage);
			log.debug(facesMessage);
		}
		String path = "C:/csv/ExportAllData.csv";
		try {
			File f = new File(path);
			FileWriter fw = new FileWriter(f);
			CSVWriter cw = new CSVWriter(fw);
			List<String[]> data = new ArrayList<String[]>();
			data.add(new String[] { "StudentId", "FirstName", "LastName", "Email Id", "Mobile No" });

			if (masterCheckedStudents != null) {
				for (Student stud : masterCheckedStudents) {
					data.add(new String[] { stud.getId().toString(), stud.getFirstName(), stud.getLastName(),
							stud.getEmailId(), stud.getMobileNo() });
					stud.setChecked(false);
				}
				setCheckedForAll(false);
				cw.writeAll(data);
				FacesContext facesContext = FacesContext.getCurrentInstance();
				FacesMessage facesMessage = new FacesMessage("Data Exported Sucessfully!!!!!");
				facesContext.addMessage("exportall", facesMessage);
				log.info("Data Exported Sucessfully!!!!");
				cw.close();
			}

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			log.info(e.getMessage());
		}

	}

	public void stateTransfer(ActionEvent event) {
		RowState state = new RowState();
		int key = (int) event.getComponent().getAttributes().get("data");
		log.info(key);
		for (Subjects sub : subjects) {
			if (sub.getId() == key) {
				log.info(sub.getId() == key);
				state.setSelected(true);
				stateMap.put(sub, state);
				SubjectBean sb = new SubjectBean();
				sb.setStateMap(stateMap);
				break;
			} else {
				state.setSelectable(false);
				stateMap.put(null, state);
			}
		}

	}

	public Integer getStudentid() {

		return studentid;
	}

	public void setStudentid(Integer studentid) {
		this.studentid = studentid;
	}

	public List<Student> getMasterCheckedStudents() {
		return masterCheckedStudents;
	}

	public void setMasterCheckedStudents(List<Student> masterCheckedStudents) {
		this.masterCheckedStudents = masterCheckedStudents;
	}

	public boolean getCheckedForAll() {
		return checkedForAll;
	}

	public void setCheckedForAll(boolean checkedForAll) {
		this.checkedForAll = checkedForAll;
	}

	public List<Student> getStudentList() {
		return studentList;
	}

	public void setStudentList(List<Student> studentList) {
		this.studentList = studentList;
	}

	public List<Student> getCheckedStudentList() {
		return checkedStudentList;
	}

	public void setCheckedStudentList(List<Student> checkedStudentList) {
		this.checkedStudentList = checkedStudentList;
	}

	public List<Subjects> getSubjects() {
		return subjects;
	}

	public void setSubjects(List<Subjects> subjects) {
		this.subjects = subjects;
	}

	public List<Subjects> getSub() {
		return sub;
	}

	public void setSub(List<Subjects> sub) {
		this.sub = sub;
	}

	public RowStateMap getStateMap() {
		return stateMap;
	}

	public void setStateMap(RowStateMap stateMap) {
		this.stateMap = stateMap;
	}

	public Country getTheCountry() {
		return theCountry;
	}

	public void setTheCountry(Country theCountry) {
		this.theCountry = theCountry;
	}

	public Integer getStoreKey() {
		return storeKey;
	}

	public void setStoreKey(Integer storeKey) {
		this.storeKey = storeKey;
	}


	
	
}
