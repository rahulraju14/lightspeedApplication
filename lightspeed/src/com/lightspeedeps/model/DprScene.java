package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * DprScene entity. This holds the information for one line in the "scene table"
 * of the DPR. The table entries are created based on the information in the
 * corresponding Call Sheet report for the same date.
 */
@Entity
@Table(name = "dpr_scene")
public class DprScene extends PersistentObject<DprScene> {
	private static final long serialVersionUID = - 3746096740796646717L;

	// Fields

	/** A sequential number used for sorting the list of scene lines */
	private Integer lineNumber;

	/** The DPR that this instance is associated with. */
	private Dpr dpr;

	/** The Episode number or other project code for this scene. */
	private String episode;

	/** The Scene number (alphanumeric) */
	private String sceneNumber;

	/** The scene title (header line) */
	private String title;

	/** Where the scene was shot. */
	private String location;

	/** Distance from studio or other starting point. */
	private String mileage;

	// Constructors

	/** default constructor */
	public DprScene() {
	}

	/** full constructor */
/*	public DprScene(Dpr dpr, String sceneNumber, String title, String location,
			String mileage) {
		this.dpr = dpr;
		this.sceneNumber = sceneNumber;
		this.title = title;
		this.location = location;
		this.mileage = mileage;
	}
*/
	// Property accessors

	/** See {@link #lineNumber}. */
	@Column(name = "Line_Number")
	public Integer getLineNumber() {
		return lineNumber;
	}
	/** See {@link #lineNumber}. */
	public void setLineNumber(Integer lineNumber) {
		this.lineNumber = lineNumber;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "DPR_Id", nullable = false)
	public Dpr getDpr() {
		return dpr;
	}
	public void setDpr(Dpr dpr) {
		this.dpr = dpr;
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

	@Column(name = "Scene_Number", length = 10)
	public String getSceneNumber() {
		return sceneNumber;
	}
	public void setSceneNumber(String sceneNumber) {
		this.sceneNumber = sceneNumber;
	}

	@Column(name = "Title", length = 200)
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}

	@Column(name = "Location", length = 1000)
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}

	@Column(name = "Mileage", length = 100)
	public String getMileage() {
		return mileage;
	}
	public void setMileage(String mileage) {
		this.mileage = mileage;
	}

}