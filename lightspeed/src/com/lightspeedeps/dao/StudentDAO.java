package com.lightspeedeps.dao;

import java.util.List;

import com.lightspeedeps.model.Student;

public class StudentDAO extends BaseTypeDAO<Student> {
	public static StudentDAO getInstance() {
		return (StudentDAO) getInstance("StudentDAO");
	}

	
	public List<Student> getStudents()
	{
		List<Student>theStudents=getHibernateTemplate().loadAll(Student.class);
		
		return theStudents;
	}
	
	
}
