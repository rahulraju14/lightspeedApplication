package com.lightspeedeps.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

/**
* Data Transfer object holds the ApplicationRelease information like version number, release date etc
*
* @author  Mohamed Abuthahir
* @version 1.0
* @since   2019-03-11
*/

public class ApplicationReleaseDTO implements Serializable {

	private static final long serialVersionUID = 8981734092388240449L;


	private long releaseDetailsId;


    private String applicationName;


    private String releaseVersion;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
	private LocalDateTime releaseDate;


    private String releaseTitle;


    private List<ApplicationReleaseDetailDTO> applicationReleaseDetails;


	public long getReleaseDetailsId() {
		return releaseDetailsId;
	}


	public void setReleaseDetailsId(long releaseDetailsId) {
		this.releaseDetailsId = releaseDetailsId;
	}


	public String getApplicationName() {
		return applicationName;
	}


	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}


	public String getReleaseVersion() {
		return releaseVersion;
	}


	public void setReleaseVersion(String releaseVersion) {
		this.releaseVersion = releaseVersion;
	}


	public LocalDateTime getReleaseDate() {
		return releaseDate;
	}

	public void setReleaseDate(LocalDateTime releaseDate) {
		this.releaseDate = releaseDate;
	}

	/**
	 * LS-3259
	 * Returns the formatted string for the LocalDateTime object. To be used on
	 * the release notes popup.
	 * 
	 * @return
	 */
	public String getFormattedReleaseDate() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		return releaseDate.format(formatter);
	}
	
	public String getReleaseTitle() {
		return releaseTitle;
	}


	public void setReleaseTitle(String releaseTitle) {
		this.releaseTitle = releaseTitle;
	}


	public List<ApplicationReleaseDetailDTO> getApplicationReleaseDetails() {
		return applicationReleaseDetails;
	}


	public void setApplicationReleaseDetails(List<ApplicationReleaseDetailDTO> applicationReleaseDetails) {
		this.applicationReleaseDetails = applicationReleaseDetails;
	}

}
