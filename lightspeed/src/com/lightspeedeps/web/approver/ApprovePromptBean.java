/**
 * File: ApproverPromptBean.java
 */
package com.lightspeedeps.web.approver;

import java.io.Serializable;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.WeeklyTimecard;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.payroll.TimecardUtils;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.popup.RejectPromptBean;

/**
 * This class extends the {@link RejectPromptBean} (which accepts and validates a
 * password and PIN, and displays a list of reject-to candidates) by changing
 * the supplied list to be that of candidate approvers, and handling the case
 * where the timecards being approved are destined for multiple approvers.
 */
@ManagedBean
@ViewScoped
public class ApprovePromptBean extends RejectPromptBean implements Serializable {
	/** */
	private static final long serialVersionUID = - 3146178618540528469L;

	private static final Log log = LogFactory.getLog(ApprovePromptBean.class);

	/** True iff the selection list of possible approvers should be displayed.
	 * When false, the 'approverName' is displayed as an output field. */
	private boolean showSelect;

	/** True if the approval process is being applied to multiple timecards, and
	 * not all the timecards have the same "next approver".  When true, neither
	 * the approverName nor the drop-down select list are displayed; instead, a
	 * message is displayed telling the user that multiple next-approvers exist. */
	private boolean multipleApprovers;

	/** The name of the next approver in the normal chain. */
	private String approverName;

	/** The Contact.id value of the selected item in the drop-down list of
	 * possible approvers. */
	private Integer approverContactId;

	/** A List of SelectItem`s of possible approver Contacts.  The value of
	 * each SelectItem is a Contact.id, and the label is the contact's name. */
	private List<SelectItem> approverContactDL;

	/** If non-null, additional message text to be displayed in the Approval
	 * pop-up dialog box. */
	private String alertMessage;

	public ApprovePromptBean() {
	}

	public static ApprovePromptBean getInstance() {
		return (ApprovePromptBean)ServiceFinder.findBean("approvePromptBean");
	}

	@Override
	public void showPin(PopupHolder holder, int act, String prefix,
			WeeklyTimecard wtc) {
		showSelect = false;
		approverContactId = null;
		alertMessage = null;
		approverName = "";
		super.showPin(holder, act, prefix, wtc);
		if (getApproverContactDL().size() > 0) {
			approverName = getApproverContactDL().get(0).getLabel();
		}
	}

	/**
	 *
	 * @see com.lightspeedeps.web.popup.RejectPromptBean#actionOk()
	 */
	@Override
	public String actionOk() {
		setContactId(approverContactId);
		log.debug("approver contact id=" + approverContactId);
		return super.actionOk();
	}

	private void createApproverContactDL() {
		Production prod = TimecardUtils.findProduction(getWeeklyTimecard());
		approverContactDL = ApproverUtils.createApproverContactList(prod);
		if (approverContactDL.size() > 0) {
			approverContactId = (Integer)approverContactDL.get(0).getValue();
		}
	}

	/** See {@link #showSelect}. */
	public boolean getShowSelect() {
		return showSelect;
	}
	/** See {@link #showSelect}. */
	public void setShowSelect(boolean showSelect) {
		this.showSelect = showSelect;
	}

	/** See {@link #multipleApprovers}. */
	public boolean getMultipleApprovers() {
		return multipleApprovers;
	}
	/** See {@link #multipleApprovers}. */
	public void setMultipleApprovers(boolean multiple) {
		multipleApprovers = multiple;
		if (multipleApprovers) {
			approverName = null;
			approverContactId = null;
		}
	}

	/** See {@link #approverName}. */
	public String getApproverName() {
		return approverName;
	}
	/** See {@link #approverName}. */
	public void setApproverName(String approverName) {
		this.approverName = approverName;
	}

	/** See {@link #approverContactId}. */
	public Integer getApproverContactId() {
		return approverContactId;
	}
	/** See {@link #approverContactId}. */
	public void setApproverContactId(Integer id) {
		approverContactId = id;
		if (id == null) {
			approverName = "(Final approval)";
			if (approverContactDL != null && approverContactDL.size() > 0) {
				if (approverContactDL.get(0).getValue() != null) {
					approverContactDL.add(0,new SelectItem(null, approverName));
				}
			}
		}
		else {
			if (approverContactDL != null) {
				for (SelectItem si : approverContactDL) {
					if (approverContactId.equals(si.getValue())) {
						approverName = si.getLabel();
						break;
					}
				}
			}
		}
	}

	/** See {@link #approverContactDL}. */
	public List<SelectItem> getApproverContactDL() {
		if (approverContactDL == null) {
			createApproverContactDL();
		}
		return approverContactDL;
	}
	/** See {@link #approverContactDL}. */
	public void setApproverContactDL(List<SelectItem> approverContactDL) {
		this.approverContactDL = approverContactDL;
	}

	/**See {@link #alertMessage}. */
	public String getAlertMessage() {
		return alertMessage;
	}
	/**See {@link #alertMessage}. */
	public void setAlertMessage(String alertMessage) {
		this.alertMessage = alertMessage;
	}

}
