package com.lightspeedeps.model;

import java.util.Date;

import javax.persistence.*;

/**
 * SceneCall entity. This holds the information for one line in either the
 * "Scenes" or "Advanced Scenes" table in the Callsheet. Note that a number of
 * the text fields, while populated with the information matching the field
 * description (e.g., number of pages in the 'pages' field), the user can edit
 * the contents of the fields and enter any information they want. In addition,
 * in the Scenes table the application creates 3 SceneCall records for every
 * Scene, only the first of which has the data matching the field names. In the
 * second record the Scene's synopsis is placed in the 'heading' field, and the
 * third record is blank.
 */
@Entity
@Table(name = "scene_call")
public class SceneCall extends PersistentObject<SceneCall> {

	private static final long serialVersionUID = - 3647532871455813296L;

	// Fields

	/** Foreign key for the Advance Scene table records */
	private Callsheet callsheetByAdvanceId;

	/** Foreign key for the Scene table records */
	private Callsheet callsheetByCallsheetId;

	/** The episode number this scene is from. */
	private String episode;

	/** For Advanced Scene records, which shooting day number this line applies to. */
	private Integer dayNumber;

	/** The date that corresponds to this Advance Scene entry's shooting day number. */
	private Date date;

	/** The display line number of this record, origin 0, within its table. */
	private Integer lineNumber;

	/** The scene heading, or the synopsis. */
	private String heading;

	/** The scene number (as text). */
	private String number;

	/** The list of Cast IDs appearing in this Scene. */
	private String castIds;

	/** The Day/Night value for this Scene. */
	private String dayNight;

	/** The length of the scene in eighth's of a page. */
	private Integer pageLength;

	/** The number of script pages (formatted as "n m/8") for this Scene. */
	private String pages;

	/** The Location (from a RealWorldElement) where the Scene will be shot. */
	private String location;

	/** Transient field holding the database id of the RealWorldElement Location
	 * that matches the location named in this SceneCall. */
	private transient Integer locationId;

	/** True if we have already attempted to find the locationId value. */
	private transient boolean locationChecked = false;

	// Constructors

	/** default constructor */
	public SceneCall() {
	}

	// Property accessors

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Advance_Id")
	public Callsheet getCallsheetByAdvanceId() {
		return callsheetByAdvanceId;
	}
	public void setCallsheetByAdvanceId(Callsheet callsheetByAdvanceId) {
		this.callsheetByAdvanceId = callsheetByAdvanceId;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Callsheet_Id")
	public Callsheet getCallsheetByCallsheetId() {
		return callsheetByCallsheetId;
	}
	public void setCallsheetByCallsheetId(Callsheet callsheetByCallsheetId) {
		this.callsheetByCallsheetId = callsheetByCallsheetId;
	}

	/**See {@link #episode}. */
	@Column(name = "Episode", length = 10)
	public String getEpisode() {
		return episode;
	}
	/**See {@link #episode}. */
	public void setEpisode(String episode) {
		this.episode = episode;
	}

	@Column(name = "Day_Number")
	public Integer getDayNumber() {
		return dayNumber;
	}
	public void setDayNumber(Integer dayNumber) {
		this.dayNumber = dayNumber;
	}

	/** See {@link #date}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Date", length = 0)
	public Date getDate() {
		return date;
	}
	/** See {@link #date}. */
	public void setDate(Date date) {
		this.date = date;
	}

	@Column(name = "Line_Number", nullable = false)
	public Integer getLineNumber() {
		return lineNumber;
	}
	public void setLineNumber(Integer lineNumber) {
		this.lineNumber = lineNumber;
	}

	@Column(name = "Heading", length = 200)
	public String getHeading() {
		return heading;
	}
	public void setHeading(String heading) {
		this.heading = heading;
	}

	@Column(name = "Number", length = 10)
	public String getNumber() {
		return number;
	}
	public void setNumber(String number) {
		this.number = number;
	}

	@Column(name = "Cast_Ids", length = 100)
	public String getCastIds() {
		return castIds;
	}
	public void setCastIds(String castIds) {
		this.castIds = castIds;
	}

	public static final int MAX_LENGTH_DAY_NIGHT = 10;
	@Column(name = "Day_Night", length = 10)
	public String getDayNight() {
		return dayNight;
	}
	public void setDayNight(String dayNight) {
		this.dayNight = dayNight;
	}

	/**See {@link #pageLength}. */
	@Column(name = "Page_Length")
	public Integer getPageLength() {
		return pageLength;
	}
	/**See {@link #pageLength}. */
	public void setPageLength(Integer pageLength) {
		this.pageLength = pageLength;
	}

	@Column(name = "Pages", length = 10)
	public String getPages() {
		return pages;
	}
	public void setPages(String pages) {
		this.pages = pages;
	}

	@Column(name = "Location", length = 100)
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}

	/** See {@link #locationId}. */
	@Transient
	public Integer getLocationId() {
		return locationId;
	}
	/** See {@link #locationId}. */
	public void setLocationId(Integer locationId) {
		this.locationId = locationId;
	}

	/** See {@link #locationChecked}. */
	@Transient
	public boolean getLocationChecked() {
		return locationChecked;
	}
	/** See {@link #locationChecked}. */
	public void setLocationChecked(boolean locationChecked) {
		this.locationChecked = locationChecked;
	}

	/**
	 * Compare two SceneCall objects based on their date and line
	 * number values. Note that both date and lineNumber are assumed
	 * to be non-null.
	 *
	 * @param other The SceneCall being compared to this one.
	 * @return Standard 1/0/-1 compareTo response.
	 */
	public int compareDate(SceneCall other) {
		int ret = 0;
		if (other == null) {
			return 1;
		}
		ret = getDate().compareTo(other.getDate());
		if (ret == 0) {
			ret = getLineNumber().compareTo(other.getLineNumber());
		}
		return ret;
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "id=" + id;
		s += ", line=" + lineNumber;
		s += ", day#=" + dayNumber;
		s += ", scene=" + number;
		s += ", head=" + heading;
		s += ", cast=" + castIds;
		s += ", d/n=" + dayNight;
		s += ", pg=" + pages;
		s += ", loc=" + location;
		s += "]";
		return s;
	}

}