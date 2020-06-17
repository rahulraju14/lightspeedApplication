package com.lightspeedeps.web.onboard;

public class StudentStatusCount {

	private Integer greenCount;
	private Integer redCount;
	private Integer blackCount;

	public StudentStatusCount() {
		super();
		// TODO Auto-generated constructor stub
	}

	public StudentStatusCount(Integer greenCount, Integer redCount, Integer blackCount) {
		super();
		
		this.greenCount = greenCount;
		this.redCount = redCount;
		this.blackCount = blackCount;
	}

	public Integer getGreenCount() {
		return greenCount;
	}

	public void setGreenCount(Integer greenCount) {
		this.greenCount = greenCount;
	}

	public Integer getRedCount() {
		return redCount;
	}

	public void setRedCount(Integer redCount) {
		this.redCount = redCount;
	}

	public Integer getBlackCount() {
		return blackCount;
	}

	public void setBlackCount(Integer blackCount) {
		this.blackCount = blackCount;
	}

}
