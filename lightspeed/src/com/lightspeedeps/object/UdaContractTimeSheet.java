package com.lightspeedeps.object;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.model.SelectItem;

import com.lightspeedeps.type.ActraCallType;

public class UdaContractTimeSheet implements Serializable {

	private static final long serialVersionUID = 8610267955478222121L;


	private Date dateOfRecording;
	
	private BigDecimal timeOfArrival;

	private BigDecimal callTime1;
	
	private BigDecimal travelFrom1;

	private BigDecimal travelTo1;

	private BigDecimal m1Out;

	private BigDecimal m1In;
	
	private BigDecimal finishTime1;

	

	public UdaContractTimeSheet(Boolean addOther) {
		super();
	}

	public UdaContractTimeSheet(Date dateOfRecording, BigDecimal timeOfArrival, BigDecimal callTime1, BigDecimal travelFrom1,
			BigDecimal travelTo1, BigDecimal m1Out, BigDecimal m1In, BigDecimal finishTime1) {
		super();
		this.dateOfRecording = dateOfRecording;
		this.timeOfArrival=timeOfArrival;
		this.travelFrom1 = travelFrom1;
		this.travelTo1 = travelTo1;
		this.callTime1 = callTime1;
		this.m1Out = m1Out;
		this.m1In = m1In;
		this.finishTime1 = finishTime1;
	}


	public Date getDateOfRecording() {
		return dateOfRecording;
	}

	public void setDateOfRecording(Date dateOfRecording) {
		this.dateOfRecording = dateOfRecording;
	}

	public BigDecimal getTimeOfArrival() {
		return timeOfArrival;
	}

	public void setTimeOfArrival(BigDecimal timeOfArrival) {
		this.timeOfArrival = timeOfArrival;
	}
	public BigDecimal getTravelFrom1() {
		return travelFrom1;
	}

	public void setTravelFrom1(BigDecimal travelFrom1) {
		this.travelFrom1 = travelFrom1;
	}

	public BigDecimal getTravelTo1() {
		return travelTo1;
	}

	public void setTravelTo1(BigDecimal travelTo1) {
		this.travelTo1 = travelTo1;
	}

	public BigDecimal getCallTime1() {
		return callTime1;
	}

	public void setCallTime1(BigDecimal callTime1) {
		this.callTime1 = callTime1;
	}

	public BigDecimal getM1Out() {
		return m1Out;
	}

	public void setM1Out(BigDecimal m1Out) {
		this.m1Out = m1Out;
	}

	public BigDecimal getM1In() {
		return m1In;
	}

	public void setM1In(BigDecimal m1In) {
		this.m1In = m1In;
	}

	public BigDecimal getFinishTime1() {
		return finishTime1;
	}

	public void setFinishTime1(BigDecimal finishTime1) {
		this.finishTime1 = finishTime1;
	}
}
