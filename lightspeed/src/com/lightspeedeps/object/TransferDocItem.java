/**
 * File: TransferDocItem.java
 */
package com.lightspeedeps.object;

import java.util.ArrayList;
import java.util.List;

/*import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;*/

/**
 * This holds all the information needed to maintain and display
 * one "cell" on the Transfer Documents page in Onboarding.
 */
public class TransferDocItem {

	/** List of the ContactDocumentInfo instances for the documents in this cell. */
	private List<ContactDocumentInfo> docInfoList;

	/** count of documents */
	private Integer docCount;

	private TriStateCheckboxExt checkBox;

	/** boolean indicating if item's checkbox is visible or not.
	 * Accessed from JSP. */
	private Boolean checkType;

	// TODO Is this needed so that click() can find the EmploymentDocuments row?
	private Integer employmentId;

	private boolean sentPerformer = false;
	private boolean sentUnion = false;
	private boolean sentTPS = false;

	//private static final Log log = LogFactory.getLog(TransferDocItem.class);

	public TransferDocItem(Integer employmentId) {
		super();
		this.employmentId = employmentId;
		this.docInfoList = new ArrayList<>();
		checkBox = new TriStateCheckboxExt();
		checkBox.setCheckValue(TriStateCheckboxExt.CHECK_OFF);
		checkBox.setId(this);
	}

	public TransferDocItem(List<ContactDocumentInfo> docInfoList, Integer docCount, Integer employmentId) {
		super();
		this.docInfoList = docInfoList;
		this.docCount = docCount;
		this.employmentId = employmentId;
		checkBox = new TriStateCheckboxExt();
		checkBox.setCheckValue(TriStateCheckboxExt.CHECK_OFF);
		checkBox.setId(this);
	}

	/** If this cell represents a single document, this will return that doc's info;
	 * otherwise it returns null. */
	public ContactDocumentInfo getDocInfo() {
		ContactDocumentInfo docInfo = null;
		if (docInfoList != null && docInfoList.size() == 1) {
			docInfo = docInfoList.get(0);
		}
		return docInfo;
	}
//	/** See {@link #docInfo}. */
//	public void setDocInfo(ContactDocumentInfo docInfo) {
//		this.docInfo = docInfo;
//	}

	/** See {@link #docInfoList}. */
	public List<ContactDocumentInfo> getDocInfoList() {
		return docInfoList;
	}
	/** See {@link #docInfoList}. */
	public void setDocInfoList(List<ContactDocumentInfo> docInfoList) {
		this.docInfoList = docInfoList;
	}

	/** See {@link #docCount}. */
	public Integer getDocCount() {
		if (getDocInfo() == null) {
			return getDocInfoList().size();
		}
		return docCount;
	}
	/** See {@link #docCount}. */
	public void setDocCount(Integer docCount) {
		this.docCount = docCount;
	}

	/** See {@link #checkBox}. */
	public TriStateCheckboxExt getCheckBox() {
		return checkBox;
	}
	/** See {@link #checkBox}. */
	public void setCheckBox(TriStateCheckboxExt checkBox) {
		this.checkBox = checkBox;
	}

	/** See {@link #checkType}. */
	public Boolean getCheckType() {
		/*checkType = false;
		if (docInfoList != null && (! docInfoList.isEmpty())) {
			for (ContactDocumentInfo cd : docInfoList) {
				if (! cd.getDisabled()) {
					log.debug("");
					checkType = true;
					break;
				}
			}
		}*/
		return checkType;
	}
	/** See {@link #checkType}. */
	public void setCheckType(Boolean checkType) {
		this.checkType = checkType;
	}

	/** See {@link #employmentId}. */
	public Integer getEmploymentId() {
		return employmentId;
	}
	/** See {@link #employmentId}. */
	public void setEmploymentId(Integer employmentId) {
		this.employmentId = employmentId;
	}

	/** See {@link #sentPerformer. */
	public boolean isSentPerformer() {
		return sentPerformer;
	}

	/** See {@link #sentPerformer. */
	public void setSentPerformer(boolean sentPerformer) {
		this.sentPerformer = sentPerformer;
	}

	/** See {@link #sentUnion. */
	public boolean isSentUnion() {
		return sentUnion;
	}

	/** See {@link #sentUnion. */
	public void setSentUnion(boolean sentUnion) {
		this.sentUnion = sentUnion;
	}

	/** See {@link #sentTPS. */
	public boolean isSentTPS() {
		return sentTPS;
	}

	/** See {@link #sentTPS. */
	public void setSentTPS(boolean sentTPS) {
		this.sentTPS = sentTPS;
	}

}
