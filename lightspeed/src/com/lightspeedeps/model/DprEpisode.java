package com.lightspeedeps.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

/**
 * This holds all the episode-specific information for one DPR. A DPR may have
 * multiple instances of DprEpisode if it is a "cross-project" production. That
 * is, a production in which more than one episode can appear on a call sheet
 * and DPR.  DPRs for feature film and commercial productions will only have a
 * single instance of DprEpisode.
 */
@Entity
@Table(name = "dpr_episode", uniqueConstraints = @UniqueConstraint(columnNames = {"Dpr_Id", "Project_Id" }))
public class DprEpisode extends PersistentObject<DprEpisode> {

	private static final long serialVersionUID = 8943477653209876001L;

	// Fields

	/** */
	private Dpr dpr;

	/** */
	private Project project;

	/** */
	private ScriptMeasure scriptMeasurePriorTotal;

	/** */
	private ScriptMeasure scriptMeasureRevisedTotal;

	/** */
	private ScriptMeasure scriptMeasureShotPrior;

	/** */
	private ScriptMeasure scriptMeasureShot;

	/** */
	private String completedScenes;

	/** */
	private String partialScenes;

	/** */
	private String scheduledNotShot;

	/** */
	private Integer soundUsedTotal = 0;

	/** */
	private Integer soundUsedPrior = 0;

	/** */
	private Integer soundUsedToday = 0;

//	/** */
//	private List<DprScene> dprScenes = new ArrayList<DprScene>(0);
//
//	/** */
//	private List<TimeCard> castTimeCards = new ArrayList<TimeCard>(0);

	// Constructors

	/** default constructor */
	public DprEpisode() {
	}

	// Property accessors

	/**See {@link #dpr}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Dpr_Id", nullable = false)
	public Dpr getDpr() {
		return dpr;
	}
	/**See {@link #dpr}. */
	public void setDpr(Dpr dpr) {
		this.dpr = dpr;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Project_Id", nullable = false)
	public Project getProject() {
		return project;
	}
	public void setProject(Project project) {
		this.project = project;
	}

	@Column(name = "Completed_Scenes", length = 100)
	public String getCompletedScenes() {
		return completedScenes;
	}
	public void setCompletedScenes(String completedScenes) {
		this.completedScenes = completedScenes;
	}

	@Column(name = "Partial_Scenes", length = 100)
	public String getPartialScenes() {
		return partialScenes;
	}
	public void setPartialScenes(String partialScenes) {
		this.partialScenes = partialScenes;
	}

	@Column(name = "Scheduled_Not_Shot", length = 100)
	public String getScheduledNotShot() {
		return scheduledNotShot;
	}
	public void setScheduledNotShot(String scheduledNotShot) {
		this.scheduledNotShot = scheduledNotShot;
	}

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Script_Prior_Total_Id")
	public ScriptMeasure getScriptMeasurePriorTotal() {
		return scriptMeasurePriorTotal;
	}
	public void setScriptMeasurePriorTotal(ScriptMeasure sm) {
		scriptMeasurePriorTotal = sm;
	}

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Script_Total_Id")
	public ScriptMeasure getScriptMeasureRevisedTotal() {
		return scriptMeasureRevisedTotal;
	}
	public void setScriptMeasureRevisedTotal(ScriptMeasure sm) {
		scriptMeasureRevisedTotal = sm;
	}

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Script_Shot_Prior_Id")
	public ScriptMeasure getScriptMeasureShotPrior() {
		return scriptMeasureShotPrior;
	}
	public void setScriptMeasureShotPrior(ScriptMeasure sm) {
		scriptMeasureShotPrior = sm;
	}

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "Script_Shot_Id")
	public ScriptMeasure getScriptMeasureShot() {
		return scriptMeasureShot;
	}
	public void setScriptMeasureShot(
			ScriptMeasure scriptMeasureByScriptShotId) {
		scriptMeasureShot = scriptMeasureByScriptShotId;
	}

//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "dpr")
//	@OrderBy("lineNumber")
//	public List<DprScene> getDprScenes() {
//		return dprScenes;
//	}
//	public void setDprScenes(List<DprScene> dprScenes) {
//		this.dprScenes = dprScenes;
//	}
//
//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "dprCast")
//	@OrderBy("castId")
//	public List<TimeCard> getCastTimeCards() {
//		return castTimeCards;
//	}
//	public void setCastTimeCards(List<TimeCard> timeCards) {
//		castTimeCards = timeCards;
//	}

	@Transient
	public ScriptMeasure getScriptMeasureShotToDate() {
		return DprEpisode.getScriptMeasureShotToDate(this);
	}
	public void setScriptMeasureShotToDate(ScriptMeasure sm) {
		//this.scriptMeasureShotToDate = sm;
	}

	@Transient
	public ScriptMeasure getScriptMeasureRemaining() {
		return DprEpisode.getScriptMeasureRemaining(this);
	}
	public void setScriptMeasureRemaining(ScriptMeasure sm) {
		//this.scriptMeasureRemaining = sm;
	}

	@Column(name = "Sound_Used_Prior")
	public Integer getSoundUsedPrior() {
		return soundUsedPrior;
	}
	public void setSoundUsedPrior(Integer used) {
		if (used == null) {
			used = 0;
		}
		soundUsedPrior = used;
	}

	@Column(name = "Sound_Used_Today")
	public Integer getSoundUsedToday() {
		return soundUsedToday;
	}
	public void setSoundUsedToday(Integer used) {
		if (used == null) {
			used = 0;
		}
		soundUsedToday = used;
	}

	@Column(name = "Sound_Used_Total")
	public Integer getSoundUsedTotal() {
		return soundUsedTotal;
	}
	public void setSoundUsedTotal(Integer used) {
		if (used == null) {
			used = 0;
		}
		soundUsedTotal = used;
	}

	/**
	 * Calculate a "transient" ScriptMeasure of information on scenes shot
	 * to date.
	 * @param episode The DPR containing the previous and todays shooting values.
	 * @return A new ScriptMeasure with shot-to-date values.
	 */
	public static ScriptMeasure getScriptMeasureShotToDate(DprEpisode episode) {
		ScriptMeasure sm = new ScriptMeasure();
		ScriptMeasure tempSm;
		int scenes=0, pages=0, minutes=0, setups=0;

		if ((tempSm = episode.getScriptMeasureShotPrior()) != null) {
			scenes = tempSm.getScenes();
			pages = tempSm.getPages();
			minutes = tempSm.getMinutes();
			setups = tempSm.getSetups();
		}
		if ((tempSm = episode.getScriptMeasureShot()) != null) {
			scenes += tempSm.getScenes();
			pages += tempSm.getPages();
			minutes += tempSm.getMinutes();
			setups += tempSm.getSetups();
		}

		sm.setScenes(scenes);
		sm.setPages(pages);
		sm.setMinutes(minutes);
		sm.setSetups(setups);
		return sm;
	}

	/**
	 * Calculate a "transient" ScriptMeasure of information on scenes
	 * remaining to be shot.
	 * @param episode The DPR containing the known shooting values.
	 * @return A new ScriptMeasure with "remaining" values.
	 */
	public static ScriptMeasure getScriptMeasureRemaining(DprEpisode episode) {
		ScriptMeasure remaining = new ScriptMeasure();
		int scenes=0, pages=0, minutes=0, setups=0;

		ScriptMeasure total = episode.getScriptMeasureRevisedTotal();
		if (total != null) {
			scenes = total.getScenes();
			pages = total.getPages();
			minutes = total.getMinutes();
			setups = total.getSetups();
		}

		ScriptMeasure toDate = episode.getScriptMeasureShotToDate();
		if (toDate != null) {
			scenes -= toDate.getScenes();
			pages -= toDate.getPages();
			minutes -= toDate.getMinutes();
			setups -= toDate.getSetups();
		}

		remaining.setScenes(scenes);
		remaining.setPages(pages);
		remaining.setMinutes(minutes);
		remaining.setSetups(setups);
		return remaining;
	}

}