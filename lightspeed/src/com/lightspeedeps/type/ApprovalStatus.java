//	File Name:	WeeklyStatus.java
package com.lightspeedeps.type;

import com.lightspeedeps.util.common.NumberUtils;

/**
 * An enumeration used in a {@link com.lightspeedeps.model.WeeklyTimecard
 * WeeklyTimecard} to indicate the current status of that time card. Also used
 * as a "cumulative" status value when assigned to a
 * {@link com.lightspeedeps.model.WeeklyBatch WeeklyBatch} object.
 * <p>
 * Note that the order of declaration of the values controls the sort order on
 * the Timecard Review page when sorting by the Status field.
 */
public enum ApprovalStatus {

	/** A "SUBMITTED" document that is in the current viewer's queue.
	 * This status is only used on Approval dashboard. */
	APPROVED_READY  	(10, "Approved-Ready", "Ready for Review",
			Group.YELLOW, "Submitted, Ready For Your Review",
			ApprovableStatusFilter.SUBMITTED.mask + ApprovableStatusFilter.READY.mask),

	/** A "REJECTED" document that is in the current viewer's queue.
	 * This status is only used on Approval dashboard. */
	REJECTED_READY		(20, "Rejected-Ready", "Ready for Review",
			Group.YELLOW, "Rejected, Ready For Your Review",
			ApprovableStatusFilter.SUBMITTED.mask +
			ApprovableStatusFilter.REJECTED_RECALLED.mask + ApprovableStatusFilter.READY.mask),

	/** For documents that were rejected at least once, then submitted or approved,
	 * and in the current user's queue. This status is only used on Approval dashboard. */
	RESUBMITTED_READY	(30, "Resubmitted-Ready", "Ready for Review",
			Group.YELLOW, "Resubmitted, Ready for Your Review",
			ApprovableStatusFilter.SUBMITTED.mask + ApprovableStatusFilter.READY.mask),

	/** Recalled (by a former approver), and not yet approved again, and in the
	 * current user's queue. This status is only used on Approval dashboard. */
	RECALLED_READY		(40, "Recalled-Ready", "Ready for Review",
			Group.YELLOW, "Recalled, Ready For Your Review",
			ApprovableStatusFilter.SUBMITTED.mask +
			ApprovableStatusFilter.REJECTED_RECALLED.mask + ApprovableStatusFilter.READY.mask),

	/** Recalled (by a former approver), and not yet approved again. */
	RECALLED			(50, "Recalled", "Recalled",
			Group.YELLOW, "Recalled",
			ApprovableStatusFilter.SUBMITTED.mask +
			ApprovableStatusFilter.REJECTED_RECALLED.mask + ApprovableStatusFilter.READY.mask),

	/** Rejected, and not yet submitted or approved again. */
	REJECTED			(60, "Rejected", "Rejected",
			Group.YELLOW, "Rejected",
			ApprovableStatusFilter.SUBMITTED.mask +
			ApprovableStatusFilter.NOT_SUBMITTED.mask +
			ApprovableStatusFilter.REJECTED_RECALLED.mask + ApprovableStatusFilter.READY.mask),

		/** Available for editing by accountant/HR; not yet visible to employee. */
	PENDING 			(0, "Pending", "Pending",
			Group.RED, "Pending",
			ApprovableStatusFilter.NOT_SUBMITTED.mask),

	/** Not yet submitted; available for editing by employee. */
	OPEN 				(0, "Not Submitted", "Not Submitted",
			Group.RED, "Not yet submitted",
			ApprovableStatusFilter.NOT_SUBMITTED.mask),

	/** Submitted by employee; may have had some (but not final) approvals.
	 * On the dashboard, this displays as "waiting for next approver". */
	SUBMITTED			(70, "Submitted", "Submitted, Pending",
			Group.YELLOW, "Submitted, Awaiting Initial Approval(s)",
			ApprovableStatusFilter.SUBMITTED.mask + ApprovableStatusFilter.READY.mask),

	/** Submitted by employee; may have had some (but not final) approvals. It's
	 * prior ApprovalPath has been deleted, or the document type was removed
	 * from its prior ApprovalPath, so it cannot be processed until it is added
	 * to another ApprovalPath. */
	SUBMITTED_NO_APPROVERS	(75, "Submitted, No Approver", "Submitted, No Approver",
			Group.YELLOW, "Submitted, waiting for assignment of approver(s)",
			ApprovableStatusFilter.SUBMITTED.mask),

	/** For documents that were rejected at least once, then submitted or approved. */
	RESUBMITTED			(80, "Resubmitted", "Resubmitted, Pending",
			Group.YELLOW, "Resubmitted, Awaiting Initial Approval(s)",
			ApprovableStatusFilter.SUBMITTED.mask + ApprovableStatusFilter.READY.mask),

	/** A "REJECTED" or "RESUBMITTED" document that has already been approved by the
	 * current viewer.  This status is only used on Approval dashboard. */
	REJECTED_PAST		(90, "Rejected-Past", "Resubmitted, Pending",
			Group.YELLOW, "Resubmitted, Final Approval Pending",
			ApprovableStatusFilter.SUBMITTED.mask + ApprovableStatusFilter.REJECTED_RECALLED.mask),

	/** A "SUBMITTED" document that has already been approved by the current viewer.
	 * This status is only used on Approval dashboard. */
	APPROVED_PAST		(100, "Approved-Past", "Submitted, Pending",
			Group.YELLOW, "Submitted, Final Approval Pending",
			ApprovableStatusFilter.SUBMITTED.mask),

	/** Final approval has been given. */
	APPROVED			(200, "Approved", "Approved",
			Group.GREEN, "Approved",
			ApprovableStatusFilter.APPROVED.mask),

	/** Locked (occurs sometime after final approval). */
	LOCKED				(300, "Approved-Locked", "Locked, Approved",
			Group.GREEN, "Locked, Approved",
			ApprovableStatusFilter.APPROVED.mask),

	/** Void. Occurs only as a result of a Void operation. */
	VOID				(400, "Void", "Void",
			Group.YELLOW, "Void",
			ApprovableStatusFilter.VOID.mask),
	/** for timecard instances created for UDA HTG Calculator */
	UDA_HTG				(400, "UDAHTG_CREATED", "UDA HTG",
			Group.YELLOW, "For UDA HTG",
			ApprovableStatusFilter.VOID.mask),
	;

	public class Group { // necessary, to use statics "prior to definition"
		private final static int RED = 0;
		private final static int YELLOW = 1;
		private final static int GREEN = 2;
	}

	/** The highest/last status value that may be assigned to a weekly
	 * batch of timecards. */
	public final static ApprovalStatus BATCH_FINAL_STATUS = LOCKED;
	/** The initial status value assigned to a weekly batch of timecards. */
	public final static ApprovalStatus BATCH_INITIAL_STATUS = OPEN;

	/** The usual mixed-case heading to display for this value. */
	private final String heading;

	/** The mixed-case heading to display for this value on mobile timecard pages. */
	private final String mobileStatus;

	/** The text to be used for the "title" attribute on JSP pages. */
	private final String iconTitle;

	/** Indicates which background group this status falls into, for controlling
	 * the background color or other UI highlighting. */
	private int background;

	/** The ordering of these values when considered within a WeeklyBatch.  The status
	 * of the WeeklyBatch will be the status with the lowest batchOrder value of all
	 * the WeeklyTimecard statuses. */
	private final int batchOrder;

	/** A combination of the mask values from the values of the ApprovableStatusFilter
	 * enums which should include documents with this ApprovalStatus value. For example,
	 * the ApprovalStatus value SUBMITTED is defined with a filterMask of: </br>
	 *   ApprovableStatusFilter.SUBMITTED.mask + ApprovableStatusFilter.READY.mask </br>
	 * because documents with SUBMITTED status will match the ApprovableStatusFilter.SUBMITTED
	 * value and may match the ApprovableStatusFilter.READY value. */
	private final int filtersMask;

	private ApprovalStatus(int batchOrd, String head, String mobile, int back, String iconText, int filters) {
		heading = head;
		mobileStatus = mobile;
		background = back;
		iconTitle = iconText;
		batchOrder = batchOrd;
		filtersMask = filters;
	}

	/**
	 * @return The "pretty" mixed-case version of the enumerated value.
	 * This could be enhanced to use the current locale setting for i18n purposes.
	 * <p>
	 * NOTE: This overrides the default toString(), which returns
	 * the same value as name(), which is the exact text of the enum name.
	 */
	@Override
	public String toString() {
		return heading;
	}

	/**
	 * @return The "pretty" printable version of this enumerated type. It is the same as toString,
	 * but can be used from jsp pages since it follows the bean accessor (getter) naming convention.
	 */
	public String getLabel() {
		return toString();
	}

	/** See {@link #mobileStatus}. */
	public String getMobileStatus() {
		return mobileStatus;
	}

	/** See {@link #iconTitle}. */
	public String getIconTitle() {
		return iconTitle;
	}

	/** See {@link #background}. */
	public int getBackground() {
		return background;
	}

	/**See {@link #batchOrder}. */
	public int getBatchOrder() {
		return batchOrder;
	}

	/** See {@link #filtersMask}. */
	public int getFiltersMask() {
		return filtersMask;
	}

	/**
	 * @return True iff a document with this status is waiting to be approved.
	 *         That is, it has been submitted, and it has not received its final
	 *         approval.
	 */
	public boolean isPendingApproval() {
		return this == SUBMITTED ||
				this == SUBMITTED_NO_APPROVERS ||
				this == RESUBMITTED ||
				this == REJECTED ||
				this == RECALLED;
	}

	/**
	 * @return True iff the status indicates the document is in the current user's
	 *         queue, i.e., ready for their approval or rejection.
	 */
	public boolean isReady() {
		return this == APPROVED_READY ||
				this == RESUBMITTED_READY ||
				this == RECALLED_READY ||
				this == REJECTED_READY;
	}

	/**
	 * @return True iff the status indicates the document is not in the current
	 *         approver's queue, and has not "past" them yet in the approval
	 *         process. I.e., it has either not been approved by anyone
	 *         (including not even submitted), or only approved by persons
	 *         earlier in the chain than the current approver.
	 */
	public boolean isPrior() {
		return this == RECALLED ||
				this == REJECTED ||
				this == OPEN ||
				this == SUBMITTED ||
				this == RESUBMITTED;
	}

	/**
	 * @return True iff the status indicates the document has already been
	 *         approved by the current user.
	 */
	public boolean isPast() {
		return this == REJECTED_PAST ||
				this == APPROVED_PAST;
	}

	/**
	 * @return True iff the status indicates the document has gotten final
	 *         approval. Note: in JSP, use "finalized" property.
	 */
	public boolean isFinal() {
		return this == APPROVED ||
				this == LOCKED;
	}

	/**
	 * @return True iff the status indicates the document has gotten final
	 *         approval.  This is for use in JSP, where using "var.final"
	 *         throws an error. :(
	 */
	public boolean isFinalized() {
		return isFinal();
	}

	/**
	 * @return True iff the status indicates the document can no longer be changed.
	 */
	public boolean isSealed() {
		return this == VOID || this == LOCKED;
	}

	/**
	 * @return true iff a document with this status may be Voided. If the status
	 *         is Open, Locked, or Void, it may NOT be voided; in any other case
	 *         it is allowed to be voided.
	 */
	public boolean isVoidable() {
		return ! (this == OPEN || this == LOCKED || this == VOID || this == PENDING);
	}

	/**
	 * @return True iff documents with this status should be included by the
	 *         given filter.
	 */
	public boolean isIncludedInFilter(ApprovableStatusFilter filter) {
		if (filter.mask == 0) {
			return true;
		}
		return (filtersMask & filter.mask) > 0;
	}

	/**
	 * Compare this status with another status when the status values relate to
	 * a WeeklyBatch object. This method is used to sort WeeklyBatch objects by
	 * their status such that they are ordered by the value of the batchOrder
	 * field of WeeklyStatus.
	 *
	 * @param other The WeeklyStatus to be compared against.
	 * @return Standard -1/0/1 comparison values.
	 */
	public int compareBatch(ApprovalStatus other) {
		return NumberUtils.compare(getBatchOrder(), other.getBatchOrder());
	}

}

