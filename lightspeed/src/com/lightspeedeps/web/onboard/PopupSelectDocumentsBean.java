package com.lightspeedeps.web.onboard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ContactDocumentDAO;
import com.lightspeedeps.object.ContactDocumentInfo;
import com.lightspeedeps.object.TransferDocItem;
import com.lightspeedeps.type.PayrollFormType;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupHolder;

/**
 * This class is the backing bean for the Select Documents popup window,
 * of transfer mini tab under Onboarding sub-main tab.
 */
@ManagedBean
@ViewScoped
public class PopupSelectDocumentsBean  extends PopupBean implements Serializable {

	private static final long serialVersionUID = 2291375198595390721L;

	private static final Log log = LogFactory.getLog(PopupSelectDocumentsBean.class);

	private Map<ContactDocumentInfo, Boolean> contactDocumentInfoMap;

	private TransferDocItem transferDocItem;

	private PayrollFormType selectedFormType;

	private Map<PayrollFormType, Boolean> formTypeMap;

	/** True, if the master check box is checked otherwise false */
	private Boolean checkedForAll = false;

	/** True for "Include Docs" popup. */
	private boolean isInclude = false;

	public PopupSelectDocumentsBean() {
		log.debug("");
	}

	public static PopupSelectDocumentsBean getInstance(){
		return (PopupSelectDocumentsBean)ServiceFinder.findBean("popupSelectDocumentsBean");
	}

	/**
	 * @param holder
	 * @param act
	 * @param prefix
	 * @param item
	 * @param formType
	 */
	public void show(PopupHolder holder, int act, String prefix, TransferDocItem item, PayrollFormType formType) {
		contactDocumentInfoMap = new HashMap<>();
		transferDocItem = null;
		selectedFormType = null;
		isInclude = false;
		if (item != null && item.getDocInfoList() != null) {
			setTransferDocItem(item);
			setSelectedFormType(formType);
			for (ContactDocumentInfo cd : item.getDocInfoList()) {
				contactDocumentInfoMap.put(cd, cd.getSelected());
			}
		}
		super.show(holder, act, prefix);
	}

	/* (non-Javadoc)
	 * @see com.lightspeedeps.web.popup.PopupBean#show(com.lightspeedeps.web.popup.PopupHolder, int, java.lang.String)
	 */
	public void show(PopupHolder holder, int act, String prefix, List<PayrollFormType> formTypes, List<PayrollFormType> availableFormTypeList) {
		formTypeMap = new TreeMap<>();
		isInclude = true;
		boolean check;
		if (formTypes == null) {
			formTypes = new ArrayList<>();
		}
		if (availableFormTypeList != null) {
			for (PayrollFormType form : availableFormTypeList) {
				if (form.getName() != null) {
					check = formTypes.contains(form);
					log.debug("Form = " + form);
					formTypeMap.put(form, check);
				}
			} 
		}
		super.show(holder, act, prefix);
	}

	/** Value change listener for master-checkbox's checked / unchecked event
	 * @param evt
	 */
	public void listenCheckedForAll(ValueChangeEvent evt) {
		try {
			if (getCheckedForAll() && contactDocumentInfoMap != null) {
				for (ContactDocumentInfo cd : contactDocumentInfoMap.keySet()) {
					if (! cd.getDisabled()) {
						contactDocumentInfoMap.put(cd, true);
					}
				}
			}
			else {
				checkedForAll = false;
				for (ContactDocumentInfo cd : contactDocumentInfoMap.keySet()) {
					contactDocumentInfoMap.put(cd, false);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Value change listener for individual checkbox's checked / unchecked event
	 * @param evt
	 */
	public void listenSingleCheck (ValueChangeEvent evt) {
		try {
			ContactDocumentInfo cd = (ContactDocumentInfo) evt.getComponent().getAttributes().get("selectedRow");
			cd.setSelected((boolean) evt.getNewValue());
			if (contactDocumentInfoMap.get(cd)) {
				log.debug("");
				boolean allSelected = true;
				for (ContactDocumentInfo info : contactDocumentInfoMap.keySet()) {
					if ((! info.getDisabled()) && (! contactDocumentInfoMap.get(info))) {
						allSelected = false;
						break;
					}
				}
				if (allSelected) {
					setCheckedForAll(true);
				}
				else {
					setCheckedForAll(false);
				}
			}
			else {
				setCheckedForAll(false);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	public List<ContactDocumentInfo> getContactDocumentList() {
		return (new ArrayList<>(getContactDocumentInfoMap().keySet()));
	}

	public ArrayList<PayrollFormType> getFormTypeList() {
		return (new ArrayList<>(getFormTypeMap().keySet()));
	}

	/** See {@link #contactDocumentInfoMap}. */
	public Map<ContactDocumentInfo, Boolean> getContactDocumentInfoMap() {
		if (contactDocumentInfoMap == null) {
			contactDocumentInfoMap = new HashMap<>();
		}
		return contactDocumentInfoMap;
	}
	/** See {@link #contactDocumentInfoMap}. */
	public void setContactDocumentInfoMap(Map<ContactDocumentInfo, Boolean> contactDocumentInfoMap) {
		this.contactDocumentInfoMap = contactDocumentInfoMap;
	}

	/** See {@link #checkedForAll}. */
	public Boolean getCheckedForAll() {
		return checkedForAll;
	}
	/** See {@link #checkedForAll}. */
	public void setCheckedForAll(Boolean checkedForAll) {
		this.checkedForAll = checkedForAll;
	}

	/** See {@link #transferDocItem}. */
	public TransferDocItem getTransferDocItem() {
		return transferDocItem;
	}
	/** See {@link #transferDocItem}. */
	public void setTransferDocItem(TransferDocItem transferDocItem) {
		this.transferDocItem = transferDocItem;
	}

	/** See {@link #selectedFormType}. */
	public PayrollFormType getSelectedFormType() {
		return selectedFormType;
	}
	/** See {@link #selectedFormType}. */
	public void setSelectedFormType(PayrollFormType selectedFormType) {
		this.selectedFormType = selectedFormType;
	}

	/** See {@link #formTypeMap}. */
	public Map<PayrollFormType, Boolean> getFormTypeMap() {
		if (formTypeMap == null) {
			formTypeMap = new HashMap<>();
		}
		return formTypeMap;
	}
	/** See {@link #formTypeMap}. */
	public void setFormTypeMap(Map<PayrollFormType, Boolean> formTypeMap) {
		this.formTypeMap = formTypeMap;
	}

	/** See {@link #isInclude}. */
	public boolean getIsInclude() {
		return isInclude;
	}
	/** See {@link #isInclude}. */
	public void setIsInclude(boolean isInclude) {
		this.isInclude = isInclude;
	}



}
