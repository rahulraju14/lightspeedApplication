package com.lightspeedeps.web.onboard.form;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.DepartmentDAO;
import com.lightspeedeps.dao.EmploymentDAO;
import com.lightspeedeps.dao.ProductionBatchDAO;
import com.lightspeedeps.dao.RoleDAO;
import com.lightspeedeps.model.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.payroll.ContractUtils;
import com.lightspeedeps.util.project.DepartmentUtils;
import com.lightspeedeps.web.onboard.ContactFormBean;
import com.lightspeedeps.web.payroll.BatchSetupBean;
import com.lightspeedeps.web.util.ApplicationScopeBean;

/**
 * Backing bean for the Employment's Summary Sheet on the Payroll/Start Forms
 * page. Also used to persist the employment records and to show the employment
 * wise summary sheet on the UI.
 */
@ManagedBean
@ViewScoped
public class EmploymentBean extends StandardFormBean<Employment> implements Serializable {

	private static final long serialVersionUID = 1144496550307654098L;

	private static final Log log = LogFactory.getLog(EmploymentBean.class);

	/** Id value used in SelectItem`s indicating a real value has not been selected yet. */
	private static final int SELECT_ID = -1;

	/**	Employment instance, initially set to the employment record from the session variable */
	private Employment employment;

	/** List of SelectItem's which populates the drop-down lists on the Positions
	 * mini-tab.   The values are department.id and labels are department.name. */
	private List<SelectItem> departmentDL = null;

	/** The database id of the currently selected ProductionBatch object. */
	private Integer batchId;

	/** The database id of the currently selected Department object. */
	private Integer deptId;

	/** List of Production batch items that may be selected from for this
	 * StartForm.  The value field is the ProductionBatch.id. */
	private List<SelectItem> prodBatchList;

	/** Drop-down for choosing which state wage laws to apply. */
	private List<SelectItem> wageRuleStateDL;

	/** Whether or not the production is using the Video Tape Agreement */
	private Boolean usesVideoTape;

	/** The current Production. */
	private Production production;

	/** The current Project. */
	private Project project;

	public EmploymentBean() {
		super("EmploymentBean.");
		setUpEmployment();
		setUpDeptAndBatch();
		production = SessionUtils.getCurrentOrViewedProduction();
		if (production != null && production.getType().hasPayrollByProject()) {
			project = SessionUtils.getCurrentOrViewedProject();
		}
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#getFormById(Integer)
	 */
	@Override
	public Employment getFormById(Integer id) {
		return EmploymentDAO.getInstance().findById(id);
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#refreshForm()
	 */
	@Override
	public void refreshForm() {
		if (form != null) {
			form = EmploymentDAO.getInstance().refresh(form);
		}
	}

	/**
	 * @see com.lightspeedeps.web.onboard.form.StandardFormBean#getBlankForm()
	 */
	@Override
	public Employment getBlankForm() {
		return new Employment();
	}

	/**
	 * Utility method used to set the employment for the current contact document's employment
	 */
	private void setUpEmployment() {
		Integer id = SessionUtils.getInteger(Constants.ATTR_ONBOARDING_EMPLOYMENT_ID);
		if (id != null) {
			employment = EmploymentDAO.getInstance().findById(id);
		}
		else {
			employment = new Employment();
		}
	}

	public static EmploymentBean getInstance() {
		return (EmploymentBean) ServiceFinder.findBean("employmentBean");
	}

	/**
	 * Action method used to save the employment record into the database for the current contact
	 */
	@Override
	public String actionSave() {
		log.debug("save of summary sheet");
		if (employment.getStartForm() != null && employment.getDepartment() != null &&
				getDeptId() != null && (! employment.getDepartmentId().equals(getDeptId()))) {
			Department newDept  = DepartmentDAO.getInstance().findById(getDeptId());
			// TODO occupation may have been edited! Won't match 'random' role selected below.
			Role newRole = RoleDAO.getInstance().findOneByProperty("department", newDept);
			employment.setRole(newRole);
		}
		employment.setLastUpdated(new Date());
		EmploymentDAO.getInstance().attachDirty(employment);
		departmentDL = null;
		return super.actionSave();
	}

	/**
	 * Set up method used to refresh the employment object to refresh the view on the summary sheet
	 * when the user clicks on the summary sheet column, It also disable's edit mode
	 */
	public void setUpSelectedItem(boolean isJump) {
		if (isJump) {
			setEditMode(false);
			ContactFormBean.getInstance().setEditMode(false);
		}
		employment = EmploymentDAO.getInstance().findById(SessionUtils.getInteger(Constants.ATTR_ONBOARDING_EMPLOYMENT_ID));
		setUpDeptAndBatch();
	}

	/**
	 * Utility method used to set the employment's department and production batch in the drop down
	 */
	public void setUpDeptAndBatch() {
		if (employment != null) {
			deptId = null;
			batchId = null;
			employment = EmploymentDAO.getInstance().refresh(employment);
			if (employment.getDepartment() != null) {
				deptId = employment.getDepartment().getId();
				employment.getDepartment().getName(); // prevent LIE
			}
			if (employment.getProductionBatch() != null) {
				batchId = employment.getProductionBatch().getId();
			}
			if (employment.getStartForm() != null) {
				employment.getStartForm().getUnionLocalNum();
				employment.getStartForms().size(); // force Hibernate initialization
			}
			if (employment.getContact() != null) {
				employment.getContact().getEmailAddress();
			}
		}
	}

	/**
	 * ValueChangeListener for Batch drop-down list
	 * @param event contains old and new values
	 */
	public void listenBatchChange(ValueChangeEvent event) {
		try {
			log.debug("new val = " + event.getNewValue());
			Integer id = (Integer)event.getNewValue();
			if (id != null) {
				batchId = id;
				ProductionBatch batch = ProductionBatchDAO.getInstance().findById(batchId);
				employment.setProductionBatch(batch);
			}
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
		}
	}

	public String listenAccount() {
		return null;
	}

	/**
	 * ValueChangeListener for Department drop-down list
	 * @param event contains old and new values
	 */
	public void listenDeptChange(ValueChangeEvent event) {
		try {
			log.debug("new val = " + event.getNewValue());
			Integer id = (Integer) event.getNewValue();
			if (id != null) {
				deptId = id;
//				employment.setDepartmentId(deptId); // added in rev 6696 9/27.
			}
		}
		catch(Exception e) {
			EventUtils.logError("Error: ", e);
		}
	}

	@Override
	public String actionCancel() {
		setUpEmployment();
		return super.actionCancel();
	}

	@Override
	public void rowClicked(ContactDocument contactDocument) {
		log.debug("in employment bean");
		SessionUtils.put(Constants.ATTR_ONBOARDING_EMPLOYMENT_ID, contactDocument.getEmployment().getId());
		setUpSelectedItem(true);
		//initScrollPos(0);
	}

	@Override
	public String actionEdit() {
		try {
			//calculateEditFlags(false);
			return super.actionEdit();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	public List<SelectItem> getDepartmentDL() {
		if (departmentDL == null) {
			// get the full department list including "Cast" (if it is active). LS-2365
			departmentDL = DepartmentUtils.getDepartmentCastCrewDL();
		}
		return departmentDL;
	}

	/** See {@link #employment}. */
	public Employment getEmployment() {
		return employment;
	}
	/** See {@link #employment}. */
	public void setEmployment(Employment employment) {
		this.employment = employment;
	}

	/**See {@link #batchId}. */
	public Integer getBatchId() {
		return batchId;
	}
	/**See {@link #batchId}. */
	public void setBatchId(Integer batchId) {
		this.batchId = batchId;
	}

	/**See {@link #deptId}. */
	public Integer getDeptId() {
		return deptId;
	}
	/**See {@link #deptId}. */
	public void setDeptId(Integer deptId) {
		this.deptId = deptId;
	}

	/** See {@link #wageRuleStateDL}. */
	public List<SelectItem> getWageRuleStateDL() {
		if (wageRuleStateDL == null) {
			wageRuleStateDL = ApplicationScopeBean.getInstance().getStateCodeWorkedDL(SessionUtils.getProduction());
		}
		return wageRuleStateDL;
	}

	/**
	 * @return {@link #prodBatchList}, creating it first
	 * if necessary.*/
	public List<SelectItem> getProdBatchList() {
		if (prodBatchList == null) {
			prodBatchList = BatchSetupBean.createProdBatchList(production, project);
			prodBatchList.add(0, new SelectItem(SELECT_ID, "(no batch selected)"));
		}
		return prodBatchList;
	}
	/**See {@link #prodBatchList}. */
	public void setProdBatchList(List<SelectItem> prodBatchList) {
		this.prodBatchList = prodBatchList;
	}

	/**See {@link #usesVideoTape}. */
	public Boolean getUsesVideoTape() {
		if(usesVideoTape == null) {
			usesVideoTape = ContractUtils.calculateUsesVideoTape(production);
		}
		return usesVideoTape;
	}

	public boolean getIsUnion() {
		if (employment.getStartForm() != null && employment.getStartForm().getUnionKey() != null) {
			return ! employment.getStartForm().getUnionKey().equals(Unions.NON_UNION);
		}
		return false;
	}

	/**See {@link #production}. */
	@Override
	public Production getProduction() {
		return production;
	}
	/**See {@link #production}. */
	public void setProduction(Production production) {
		this.production = production;
	}

	/**See {@link #project}. */
	public Project getProject() {
		return project;
	}
	/**See {@link #project}. */
	public void setProject(Project project) {
		this.project = project;
	}

}
