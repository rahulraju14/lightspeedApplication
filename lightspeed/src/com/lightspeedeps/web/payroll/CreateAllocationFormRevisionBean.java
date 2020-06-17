/**
 * File: CreateAllocationRevisionBean.java
 */
package com.lightspeedeps.web.payroll;

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.TaxWageAllocationForm;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.web.popup.PopupBean;

/**
 *
 */
@ManagedBean
@ViewScoped
public class CreateAllocationFormRevisionBean extends PopupBean {
//	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(CreateAllocationFormRevisionBean.class);
	private static final long serialVersionUID = -1L;

	/** List of Revisions for this employee. Selecting on of these revisions would a new allocation revision to be created from the existing revision. */
	private List<TaxWageAllocationForm> revisions;
	/** Allocation Form id selected from the dropdown. Defaults to blank allocation form. */
	private int selectedFormId;
	/** First line of text for the popup message. */
	private String txt1;
	/** List of SelectItems representing the different revisions. */
	private List<SelectItem> revisionsDL;

	public static CreateAllocationFormRevisionBean getInstance() {
		return (CreateAllocationFormRevisionBean)ServiceFinder.findBean("createAllocationFormRevisionBean");
	}

	public CreateAllocationFormRevisionBean() {
		log.debug("");
	}

	/** See {@link #revisions}. */
	public List<TaxWageAllocationForm> getRevisions() {
		return revisions;
	}

	/** See {@link #revisions}. */
	public void setRevisions(List<TaxWageAllocationForm> revisions) {
		this.revisions = revisions;
	}

	/** See {@link #selectedFormId}. */
	public int getSelectedFormId() {
		return selectedFormId;
	}

	/** See {@link #selectedFormId}. */
	public void setSelectedFormId(int selectedFormId) {
		this.selectedFormId = selectedFormId;
	}

	/** See {@link #txt1}. */
	public String getTxt1() {
		return txt1;
	}

	/** See {@link #txt1}. */
	public void setTxt1(String txt1) {
		this.txt1 = txt1;
	}

	/**
	 * List of SelectItems used to populate the revisions dropdown list.
	 * @return
	 */
	public List<SelectItem> getRevisionsDL() {
		if(revisionsDL == null) {
			String revDate;
			revisionsDL = new ArrayList<>();

			if(revisions != null) {
				// Add blank form.
				revisionsDL.add(new SelectItem(new Integer(0),  "Blank Form"));
				for(TaxWageAllocationForm form : revisions) {
					revDate = CalendarUtils.formatDate(form.getRevisionDate(), "MM/dd/yyyy hh:mm aaa");
					revisionsDL.add(new SelectItem(new Integer(form.getId()), revDate));
				}
				selectedFormId = revisions.get(0).getId();
			}
		}
		return revisionsDL;

	}

	/** See {@link #revisionsDL}. */
	public void setRevisionsDL(List<SelectItem> revisionsDL) {
		this.revisionsDL = revisionsDL;
	}

	@Override
	public String actionOk() {
		TaxWageAllocationBean bean = (TaxWageAllocationBean)getConfirmationHolder();
		bean.setNewRevisionId(selectedFormId);
		return super.actionOk();
	}
}
