package com.lightspeedeps.web.payroll;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.lightspeedeps.dao.ContractRuleDAO;
import com.lightspeedeps.model.Contact;
import com.lightspeedeps.model.ContractRule;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.web.view.ListView;

/** This is the backing bean for the page "Contracts Rule" under Prod Admin
 * 	main tab and Others sub tab. It is also useful in showing the list of contract rules.
 * @author root
 */
@ManagedBean
@ViewScoped
public class ContractRulesBean extends ListView implements Serializable{

	private static final long serialVersionUID = 1874368369183617396L;

	/**
	 * The list of contract rules to be shown on the table
	 */
	private List<ContractRule> contractRuleList = null;

	/** True iff we have initialized ourselves. */
	private boolean initialized = false;

	/** Used to return the instance of the packet list bean . */
	public static ContractRulesBean getInstance() {
		return (ContractRulesBean)ServiceFinder.findBean("contractRulesBean");
	}

	public ContractRulesBean() {
		super(Contact.SORTKEY_NAME, "Contact.");
	}

	/**
	 * Handle most initialization of the bean. This is delayed until we are
	 * called via a JSP reference on the Approval Paths mini-tab, to avoid the
	 * initialization overhead when the mini-tab is not being rendered.
	 */
	private void init() {
		try {
			initialized = true;
			getContractRuleList();
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	@Override
	protected void setSelected(Object item, boolean b) {

	}

	/**
	 * See {@link #contractRuleList}. Note that this method is called by the
	 * ICEfaces framework code even when the table using it is NOT being
	 * rendered! So we avoid filling this table until the 'initialized' boolean
	 * is true by {@link #init()}.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List getItemList() {
		return getContractRuleList();
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected Comparator getComparator() {
		return null;
	}

	/**See {@link #contractRuleList}. */
	public List<ContractRule> getContractRuleList() {
		if (initialized && contractRuleList ==  null) {
			contractRuleList = ContractRuleDAO.getInstance().findAll();
		}
		return contractRuleList;
	}
	/**See {@link #contractRuleList}. */
	public void setContractRuleList(List<ContractRule> contractRuleList) {
		this.contractRuleList = contractRuleList;
	}

	/**
	 * Called due to hidden field value in our JSP page. Make sure we're
	 * initialized to show something. Note that this must be public as
	 * it is called from JSP.
	 */
	public boolean getSetUp() {
		try {
			if (! initialized) {
				init();
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return false;
	}

}
