package com.lightspeedeps.dto;

import java.io.Serializable;

/**
* Data Transfer object holds the ApplicationReleaseDetail information for an Application Release
*
* @author  Mohamed Abuthahir
* @version 1.0
* @since   2019-03-11
*/

public class ApplicationReleaseDetailDTO implements Serializable{

	private static final long serialVersionUID = -5940986719822766037L;
	private long releaseNoteId;
	private String releaseNote;

	public long getReleaseNoteId() {
		return releaseNoteId;
	}
	public void setReleaseNoteId(long releaseNoteId) {
		this.releaseNoteId = releaseNoteId;
	}
	public String getReleaseNote() {
		return releaseNote;
	}
	public void setReleaseNote(String releaseNote) {
		this.releaseNote = releaseNote;
	}
}
