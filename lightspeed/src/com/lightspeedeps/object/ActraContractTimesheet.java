package com.lightspeedeps.object;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.model.SelectItem;

import com.lightspeedeps.type.ActraCallType;

public class ActraContractTimesheet implements Serializable {

	private static final long serialVersionUID = 8610267955478222121L;

	private static final List<SelectItem> callTypeList;
	private static final List<SelectItem> callTypeListOther;

	static {
		callTypeList = new ArrayList<>();
		for (ActraCallType type : ActraCallType.values()) {
			if (type.getShow()) {
				callTypeList.add(new SelectItem(type, type.toString()));
			}
		}
		callTypeListOther = new ArrayList<>();
		for (ActraCallType type : ActraCallType.values()) {
			callTypeListOther.add(new SelectItem(type, type.toString()));
		}
	}

	private Date date1;

	private BigDecimal travelFrom1;

	private BigDecimal travelTo1;

	private BigDecimal callTime1;

	private BigDecimal m1Out;

	private BigDecimal m1In;

	private BigDecimal finishTime1;

	private BigDecimal travelFrom2;

	private BigDecimal travelTo2;

	private Date date2;

	private BigDecimal callTime2;

	private BigDecimal finishTime2;

	private Boolean addOther;

	private ActraCallType callType;

	public ActraContractTimesheet(Boolean addOther) {
		super();
		this.addOther = addOther;
		getCallTypeList();
	}

	public ActraContractTimesheet(Date date1, BigDecimal travelFrom1, BigDecimal travelTo1, BigDecimal callTime1,
			BigDecimal m1Out, BigDecimal m1In, BigDecimal finishTime1, BigDecimal travelFrom2, BigDecimal travelTo2,
			Date date2, BigDecimal callTime2, BigDecimal finishTime2, ActraCallType callType, int count) {
		super();
		this.date1 = date1;
		this.travelFrom1 = travelFrom1;
		this.travelTo1 = travelTo1;
		this.callTime1 = callTime1;
		this.m1Out = m1Out;
		this.m1In = m1In;
		this.finishTime1 = finishTime1;
		this.travelFrom2 = travelFrom2;
		this.travelTo2 = travelTo2;
		this.date2 = date2;
		this.callTime2 = callTime2;
		this.finishTime2 = finishTime2;
		this.callType = callType;
		if (count > 2) {
			this.addOther = true;
		}
		else {
			this.addOther = false;
		}
	}

	public Date getDate1() {
		return date1;
	}

	public void setDate1(Date date1) {
		this.date1 = date1;
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

	public BigDecimal getTravelFrom2() {
		return travelFrom2;
	}

	public void setTravelFrom2(BigDecimal travelFrom2) {
		this.travelFrom2 = travelFrom2;
	}

	public BigDecimal getTravelTo2() {
		return travelTo2;
	}

	public void setTravelTo2(BigDecimal travelTo2) {
		this.travelTo2 = travelTo2;
	}

	public Date getDate2() {
		return date2;
	}

	public void setDate2(Date date2) {
		this.date2 = date2;
	}

	public BigDecimal getCallTime2() {
		return callTime2;
	}

	public void setCallTime2(BigDecimal callTime2) {
		this.callTime2 = callTime2;
	}

	public BigDecimal getFinishTime2() {
		return finishTime2;
	}

	public void setFinishTime2(BigDecimal finishTime2) {
		this.finishTime2 = finishTime2;
	}

	public Boolean getAddOther() {
		return addOther;
	}

	public void setAddOther(Boolean addOther) {
		this.addOther = addOther;
	}

	public ActraCallType getCallType() {
		return callType;
	}

	public void setCallType(ActraCallType callType) {
		this.callType = callType;
	}

	public List<SelectItem> getCallTypeList() {
		if (addOther) {
			return callTypeListOther;
		}
		else {
			return callTypeList;
		}
	}

}
