package com.lightspeedeps.object;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ContactDocumentDAO;
import com.lightspeedeps.model.ContactDocument;
import com.lightspeedeps.type.ApprovalStatus;
import com.lightspeedeps.type.PayrollFormType;

/**
 * This object stores all the information for one row on the Transfer Documents
 * page. It represents the data regarding one occupation for one employee. (An
 * employee with multiple occupations will have a row displayed for each
 * occupation.)
 *
 * @see com.lightspeedeps.web.onboard.TransferDocBean
 */
public class EmploymentDocuments implements Serializable {

	private static final long serialVersionUID = -7592057170577867072L;

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(EmploymentDocuments.class);

	private Integer employmentId;

	private String empName;

	/** Occupation of the Employment */
	private String occupation;

	private String department;

	/** This Map relates a form type (which effectively selects a column) to a
	 * TransferDocItem, which holds all the information for one 'cell' (intersection
	 * of row & column) on the transfer page. */
	private Map<String, TransferDocItem> mapOfDocumentItems;

	private boolean selected;

//LS-3067	private TransferDocBean transferDocBean;

	private transient ContactDocumentDAO contactDocumentDAO;

	/** The backing field for the employment checkbox, which acts
	 * as a "master" select/unselect/partially selected for all checkboxes in the
	 * employment's row. */
	private TriStateCheckboxExt empMasterCheck = new TriStateCheckboxExt();


	public EmploymentDocuments(Integer employmentId, String empName, String occupation, String department) {
		super();
		this.employmentId = employmentId;
		this.empName = empName;
		this.occupation = occupation;
		this.department = department;
	}

	/**
	 * Method controls the visibility of one cell's checkbox, and the
	 * enabled/disabled status for each contactDocument, according to their
	 * approval and transfer status. Note that one cell may represent multiple
	 * documents. The checkbox should be visible if any one of the CD's is
	 * eligible to be transferred.
	 *
	 * @param type The PayrollFormType of the documents being analyzed; this
	 *            will be used to select the cell (column) within our map of
	 *            cells.
	 * @param allowSent The setting of the checkbox (in the UI) -- if true, we
	 *            should allow documents that have already been transferred to
	 *            be selected again for transfer.
	 */
	public void setBoxVisibilityByType(PayrollFormType type, boolean allowSent) {
		//log.debug("");
		boolean visible = false; // assume checkbox is NOT visible
		if (mapOfDocumentItems != null &&  mapOfDocumentItems.get(type.name()) != null) {
			List<ContactDocumentInfo> docs = mapOfDocumentItems.get(type.name()).getDocInfoList();
			if (docs != null && (! docs.isEmpty())) {
				for (ContactDocumentInfo cd : docs) {
					if (cd.getStatus() == ApprovalStatus.APPROVED ||
							cd.getStatus() == ApprovalStatus.LOCKED || cd.getStatus() == ApprovalStatus.VOID) {
						ContactDocument contactDoc = getContactDocumentDAO().findById(cd.getContactDocumentId());
						// We should be doing this check regardless of the allowSent flag. LS-1643
						if (contactDoc.getRelatedFormId() == null) {
							cd.setDisabled(true);
						}
						else { // Related form exists.  Following section rewritten for LS-2353.
							if (cd.getTimeSent() == null && cd.getStatus() != ApprovalStatus.VOID) {
								visible = true; // not yet transferred, and not Void
								cd.setDisabled(false);
							}
							else if (cd.getTimeSent() != null && cd.getLastUpdated() != null && cd.getLastUpdated().after(cd.getTimeSent())) {
								visible = true; // Document changed (or voided) since it was last transferred
								cd.setDisabled(false);
							}
							else {
								cd.setDisabled(true);
							}
							if (allowSent && cd.getTimeSent() != null) {
								// allow any transferred document to be transferred again
								visible = true;
								cd.setDisabled(false);
							}
							if (type.isAutoAddedCanada()) {

								mapOfDocumentItems.get(type.name())
										.setSentPerformer(contactDoc.isSentToPerformer());
								mapOfDocumentItems.get(type.name())
										.setSentUnion(contactDoc.isSentToUnion());
								mapOfDocumentItems.get(type.name())
										.setSentTPS(contactDoc.isSentToTPS());
								cd.setSentPerformer(contactDoc.isSentToPerformer());
								cd.setSentUnion(contactDoc.isSentToUnion());
								cd.setSentTPS(contactDoc.isSentToTPS());
								if (! contactDoc.isSentToPerformer() ||
										! contactDoc.isSentToUnion() ||
										! contactDoc.isSentToTPS()) {
									visible = true;
									cd.setDisabled(false);
									cd.setTimeSent(null);
								}
							}
						}
					}
					else { // not transferable status
						cd.setDisabled(true);
					}
				}
			}
			mapOfDocumentItems.get(type.name()).setCheckType(visible);
		}
	}

	/** Method sets the check value for the inner tri-state checkboxes.
	 * @param type
	 * @param value
	 */
	public void selectCheckBoxByType(PayrollFormType type, Byte value) {
		if (getMapOfDocumentItems().get(type.name()) != null) {
			getMapOfDocumentItems().get(type.name()).getCheckBox().setCheckValue(value);
		}
	}

	/** See {@link #employmentId}. */
	public Integer getEmploymentId() {
		return employmentId;
	}
	/** See {@link #employmentId}. */
	public void setEmploymentId(Integer employmentId) {
		this.employmentId = employmentId;
	}

	/** See {@link #empName}. */
	public String getEmpName() {
		return empName;
	}
	/** See {@link #empName}. */
	public void setEmpName(String empName) {
		this.empName = empName;
	}

	/** See {@link #occupation}. */
	public String getOccupation() {
		return occupation;
	}
	/** See {@link #occupation}. */
	public void setOccupation(String occupation) {
		this.occupation = occupation;
	}

	/** See {@link #department}. */
	public String getDepartment() {
		return department;
	}
	/** See {@link #department}. */
	public void setDepartment(String department) {
		this.department = department;
	}

	/** See {@link #selected}. */
	public boolean getSelected() {
		return selected;
	}
	/** See {@link #selected}. */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	// LS-3067 - remove - unused
//	public TransferDocBean getTransferDocBean() {
//		if (transferDocBean == null) {
//			transferDocBean = TransferDocBean.getInstance();
//		}
//		return transferDocBean;
//	}

	/** See {@link #empMasterCheck}. */
	public TriStateCheckboxExt getEmpMasterCheck() {
		return empMasterCheck;
	}
	/** See {@link #empMasterCheck}. */
	public void setEmpMasterCheck(TriStateCheckboxExt empMasterCheck) {
		this.empMasterCheck = empMasterCheck;
	}

	/** See {@link #mapOfDocumentItems}. */
	public Map<String, TransferDocItem> getMapOfDocumentItems() {
		if (mapOfDocumentItems == null) {
			mapOfDocumentItems = new HashMap<>();
		}
		return mapOfDocumentItems;
	}
	/** See {@link #mapOfDocumentItems}. */
	public void setMapOfDocumentItems(Map<String, TransferDocItem> mapOfDocumentItems) {
		this.mapOfDocumentItems = mapOfDocumentItems;
	}

	/** See {@link #contactDocumentDAO}. */
	private ContactDocumentDAO getContactDocumentDAO() {
		if(contactDocumentDAO == null) {
			contactDocumentDAO = ContactDocumentDAO.getInstance();
		}
		return contactDocumentDAO;
	}

}
