package com.lightspeedeps.dao;

import java.util.List;

import com.lightspeedeps.model.Subjects;
import com.lightspeedeps.util.app.EventUtils;

public class SubjectsDAO extends BaseTypeDAO<Subjects> {
	public static SubjectsDAO getInstance() {
		return (SubjectsDAO) getInstance("SubjectsDAO");
	}

	public List<Subjects> getSubjects() {
		List<Subjects> theSubjects = getHibernateTemplate().loadAll(Subjects.class);

		return theSubjects;
	}

	public Subjects getSubjectById(int id) {
		Subjects theSubjects = null;
		try {
			String query = "from Subjects where stud_Id=" + id;
			theSubjects = findOne(query, null);
		} catch (Exception re) {
			// TODO: handle exception
			EventUtils.logError(re);
			throw re;
		}

		return theSubjects;

	}

}
