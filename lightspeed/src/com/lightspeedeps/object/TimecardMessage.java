package com.lightspeedeps.object;

/**
 * A class to hold messages generated during timecard HTG
 * processing.
 */
public class TimecardMessage {

	private String text;


	public TimecardMessage(String msg) {
		text = msg;
	}


	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
}
