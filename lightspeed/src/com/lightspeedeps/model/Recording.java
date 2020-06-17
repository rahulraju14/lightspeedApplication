package com.lightspeedeps.model;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "recording")
public class Recording extends PersistentObject<Recording> {

	private static final long serialVersionUID = 1864700486326238057L;
	
	/** Recording Date. */
	private Date recordingDate;
	
	/** Recording Time. */
	private BigDecimal recordingTime;
	
	/** Recording Location. */
	private String recordingLocation;

	/** See {@link #recordingDate}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "Recording_Date", length = 10)	
	public Date getRecordingDate() {
		return recordingDate;
	}
	/** See {@link #recordingDate}. */
	public void setRecordingDate(Date recordingDate) {
		this.recordingDate = recordingDate;
	}

	/** See {@link #recordingTime}. */
	@Column(name = "Recording_Time", precision = 6)
	public BigDecimal getRecordingTime() {
		return recordingTime;
	}
	/** See {@link #recordingTime}. */
	public void setRecordingTime(BigDecimal recordingTime) {
		this.recordingTime = recordingTime;
	}

	/** See {@link #recordinglocation}. */
	@Column(name = "Recording_Location", length = 150)
	public String getRecordingLocation() {
		return recordingLocation;
	}
	/** See {@link #recordinglocation}. */
	public void setRecordingLocation(String recordingLocation) {
		this.recordingLocation = recordingLocation;
	}

}
