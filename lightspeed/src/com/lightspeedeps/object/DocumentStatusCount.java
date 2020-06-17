package com.lightspeedeps.object;


/**
 * This object holds count of ContactDocuments according to their status values for an Employment.
 */
public class DocumentStatusCount {

	/** Id of the Employment to whom the ContactDocuments belongs to. */
	private Integer employmentId;

	/** Count of ContactDocuments with status OPEN. */
	private Long redCount;

	/** Count of ContactDocuments with status other than OPEN, APPROVED, LOCKED, PENDING or VOID. */
	private Long yellowCount;

	/** Count of ContactDocuments having status APPROVED or LOCKED. */
	private Long greenCount;

	/** Count of ContactDocuments with status PENDING. */
	private Long blackCount;

	public DocumentStatusCount() {
	}

	public DocumentStatusCount(Long redCount, Long yellowCount, Long greenCount, Long blackCount) {
		this.redCount = redCount;
		this.yellowCount = yellowCount;
		this.greenCount = greenCount;
		this.blackCount = blackCount;
	}

	public DocumentStatusCount(Integer employmentId, Long redCount, Long yellowCount, Long greenCount, Long blackCount) {
		this.employmentId = employmentId;
		this.redCount = redCount;
		this.yellowCount = yellowCount;
		this.greenCount = greenCount;
		this.blackCount = blackCount;
	}

	/** See {@link #employmentId}. */
	public Integer getEmploymentId() {
		return employmentId;
	}
	/** See {@link #employmentId}. */
	public void setEmploymentId(Integer employmentId) {
		this.employmentId = employmentId;
	}

	/** See {@link #redCount}. */
	public Long getRedCount() {
		return redCount;
	}
	/** See {@link #redCount}. */
	public void setRedCount(Long redCount) {
		this.redCount = redCount;
	}

	/** See {@link #yellowCount}. */
	public Long getYellowCount() {
		return yellowCount;
	}
	/** See {@link #yellowCount}. */
	public void setYellowCount(Long yellowCount) {
		this.yellowCount = yellowCount;
	}

	/** See {@link #greenCount}. */
	public Long getGreenCount() {
		return greenCount;
	}
	/** See {@link #greenCount}. */
	public void setGreenCount(Long greenCount) {
		this.greenCount = greenCount;
	}

	/** See {@link #blackCount}. */
	public Long getBlackCount() {
		return blackCount;
	}
	/** See {@link #blackCount}. */
	public void setBlackCount(Long blackCount) {
		this.blackCount = blackCount;
	}

}
