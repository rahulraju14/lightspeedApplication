package com.lightspeedeps.object;

import java.io.Serializable;
import java.util.List;

import com.lightspeedeps.model.ContactDocument;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.type.PayrollFormType;

/** Wrapper Object holds the occupation and employment wise contact document list
 * @author root
 *
 */
public class EmploymentWrapper implements Serializable{

	private static final long serialVersionUID = -6394642032286312140L;

	/** Occupation of the contact */
	private String occupationName;

	/** Employment wise contact document list */
	private List<ContactDocument> contactDocument;

	/** Project of the employment */
	private Project project;

	public EmploymentWrapper() {

	}

	public EmploymentWrapper(String occupationName, List<ContactDocument> contactDocList, Project project) {
		super();
		this.occupationName = occupationName;
		this.contactDocument = contactDocList;
		this.project = project;
	}

	/** Method to return payroll start contact document, if exists.
	 * @return payroll start contact document
	 */
	public ContactDocument getPayrollStartForOccupation () {
		for (ContactDocument cd : getContactDocument()) {
			if (cd.getFormType() == PayrollFormType.START) {
				return cd;
			}
		}
		return null;
	}

	/** See {@link #occupationName}. */
	public String getOccupationName() {
		return occupationName;
	}
	/** See {@link #occupationName}. */
	public void setOccupationName(String occupationName) {
		this.occupationName = occupationName;
	}

	/** See {@link #contactDocument}. */
	public List<ContactDocument> getContactDocument() {
		return contactDocument;
	}
	/** See {@link #contactDocument}. */
	public void setContactDocument(List<ContactDocument> contactDocument) {
		this.contactDocument = contactDocument;
	}

	/** See {@link #project}. */
	public Project getProject() {
		return project;
	}
	/** See {@link #project}. */
	public void setProject(Project project) {
		this.project = project;
	}

}