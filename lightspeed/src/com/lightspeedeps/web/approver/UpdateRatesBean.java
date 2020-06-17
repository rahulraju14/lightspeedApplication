package com.lightspeedeps.web.approver;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.EmploymentDAO;
import com.lightspeedeps.model.Employment;
import com.lightspeedeps.model.StartForm;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupInputBean;

@ManagedBean
@ViewScoped
public class UpdateRatesBean extends StartFormListBean implements Serializable{

	/**
	 *
	 */
	private static final long serialVersionUID = -7899423893158643719L;

	/** The logger  */
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(UpdateRatesBean.class);

	public static final int ACT_FILL_RATES = 10;

	public static final int ACT_COMMIT_RATES = 11;

	private BigDecimal newRateValue;

	private String rate;

	public UpdateRatesBean() {
		super();
		getFilterBean().register(this, FilterBean.TAB_UPDATE_RATES);
	}

	/** Used to return the instance of the UpdateRatesBean */
	public static UpdateRatesBean getInstance() {
		return (UpdateRatesBean)ServiceFinder.findBean("updateRatesBean");
	}

	public String actionFillRates() {
		PopupInputBean bean = PopupInputBean.getInstance();
		bean.show(this, ACT_FILL_RATES, "UpdateRates.FillRates.");
		return null;
	}

	public String actionCommitRates() {
		PopupBean bean = PopupBean.getInstance();
		bean.show(this, ACT_COMMIT_RATES, "UpdateRates.CommitRates.");
		return null;
	}

	@Override
	public String confirmOk(int action) {
		String res = null;
		rate = PopupInputBean.getInstance().getInput().trim();
		switch(action) {
			case ACT_FILL_RATES:
				res = actionFillRatesOk();
				break;
			case ACT_COMMIT_RATES:
				res = actionCommitRatesOk();
				break;
		}
		return res;
	}

	public String actionFillRatesOk () {
		if (! rate.equals("")) {
			newRateValue = new BigDecimal(rate);
			for (Employment emp : getEmploymentList()) {
				if (emp.getStartForm() != null && emp.getStartForm().getRate() != null) {
					emp.getStartForm().setNewRate(calculateRate(newRateValue, emp.getStartForm().getRate()));
				}
			}
		}
		else {
			MsgUtils.addFacesMessage("DocumentListBean.MissingRename", FacesMessage.SEVERITY_INFO);
		}
		return null;
	}

	public String actionCommitRatesOk () {
		for (Employment emp : getEmploymentList()) {
			if (emp.getStartForm() != null && emp.getStartForm().getNewRate() != null &&
					(! emp.getStartForm().getNewRate().equals(emp.getStartForm().getRate()))) {
				Employment employment = emp.clone();
				employment.getStartForm().setPriorFormId(emp.getId());
				employment.getStartForm().setFormType(StartForm.FORM_TYPE_CHANGE);
				employment.getStartForm().setRate(emp.getStartForm().getNewRate());
				EmploymentDAO.getInstance().save(employment);
			}
		}
		actionRefresh();
		return null;
	}

	public BigDecimal calculateRate (BigDecimal newRate, BigDecimal oldRate) {
		if (oldRate != null) {
			BigDecimal calculatedRate = oldRate.multiply(new BigDecimal(1).add(newRate.divide(new BigDecimal(100))));
			return calculatedRate.setScale(4, RoundingMode.HALF_UP);
		}
		return null;
	}

}
