package com.lightspeedeps.model;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lightspeedeps.type.ApprovalStatus;

@MappedSuperclass
public abstract class Approvable extends PersistentObject<Approvable> {

	private static final long serialVersionUID = -1544185451842443888L;
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(Approvable.class);

	/**
	 * The database id of the Approver identifying the next person to approve
	 * this timecard. This will be null if (a) the timecard has not been
	 * submitted, (b) was rejected back to the employee, (c) has gotten final
	 * approval, (d) the approvalPath for the document was deleted while the item
	 * was waiting for approval, or (e) the document type was removed from the
	 * ApprovalPath while the item was waiting for approval.  If this value
	 * is negative, it is the negation of the ApprovalPath.id of the path this
	 * Approvable is assigned to, and the item is waiting for any "pool"
	 * approver within that path. */
	protected Integer approverId;

	/** Database id of the Contact who is the next person to approve
	 * this timecard. null if not yet set (due to transient nature),
	 * or if approverId is null. */
	@Transient
	protected Integer approverContactId;

	protected ApprovalStatus status;

	/** The database id of the User object who has this timecard locked
	 * for editing. */
	protected Integer lockedBy;


	/** iff true, indicates that the negative {@link #approverId}
	 *  is the negation of the ApproverGroup.id instead of ApprovalPath.id. */
	protected boolean isDeptPool;

	/** See {@link #approverId}. */
	@JsonIgnore
	@Column(name = "Approver_Id")
	public Integer getApproverId() {
		return approverId;
	}
	/** See {@link #approverId}. */
	public void setApproverId(Integer approverContactId) {
		approverId = approverContactId;
		setApproverContactId(null); // force refresh
	}

	/** See {@link #approverContactId}. */
	@JsonIgnore
	@Transient
	public Integer getApproverContactId() {
		return approverContactId;
	}
	/** See {@link #approverContactId}. */
	public void setApproverContactId(Integer approverContactId) {
		this.approverContactId = approverContactId;
	}

	/** See {@link #status}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Status" , nullable = false, length = 30)
	public ApprovalStatus getStatus() {
		return status;
	}
	/** See {@link #status}. */
	public void setStatus(ApprovalStatus status) {
		this.status = status;
	}

	/** See {@link #lockedBy}. */
	@JsonIgnore
	@Column(name = "Locked_By")
	public Integer getLockedBy() {
		return lockedBy;
	}
	/** See {@link #lockedBy}. */
	public void setLockedBy(Integer lockedBy) {
		this.lockedBy = lockedBy;
	}

	/** See {@link #isDeptPool}. */
	@Column(name = "Is_Dept_Pool", nullable = false)
	public boolean getIsDeptPool() {
		return isDeptPool;
	}
	/** See {@link #isDeptPool}. */
	public void setIsDeptPool(boolean isDeptPool) {
		this.isDeptPool = isDeptPool;
	}

	/** @return True iff this Timecard / ContactDocument is in a state that allows
	 * a SUBMIT operation. */
	@JsonIgnore
	@Transient
	public boolean getSubmitable() {
		return getStatus() == ApprovalStatus.OPEN ||
				(getApproverId() == null &&
					(getStatus() == ApprovalStatus.REJECTED || getStatus() == ApprovalStatus.RECALLED));
	}

	/**
	 * @return True iff this Timecard / ContactDocument has been submitted, including one that has
	 *         reached final approval. Returns false if a document was
	 *         submitted, but has been returned to the employee due to either a
	 *         rejection or recall action. Also returns false for VOID status.
	 */
	@JsonIgnore
	@Transient
	public boolean getSubmitted() {
		if (getStatus() == ApprovalStatus.OPEN || getStatus() == ApprovalStatus.PENDING) {
			return false;
		}
		if (status.isFinal()) {
			return true;
		}
		if (status.isPendingApproval() && getApproverId() != null) {
			return true;
		}
		return false; // rejected or recalled back to employee; or VOID
	}

	@SuppressWarnings("rawtypes")
	@Transient
	public abstract List<? extends SignedEvent> getEvents();

	/** Overridden by WeeklyTimecard. (May be overridden by other subclasses.) */
	@Transient
	public boolean getUnderContract() {
		return false;
	}

	@Transient
	public abstract Production getProduction();

	@Transient
	public abstract Department getDepartment();

}