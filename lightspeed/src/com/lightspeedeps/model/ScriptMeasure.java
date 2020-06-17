package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * ScriptMeasure entity.
 */
@Entity
@Table(name = "script_measure")
public class ScriptMeasure extends PersistentObject<ScriptMeasure> implements Cloneable {
	private static final long serialVersionUID = 4484075542625365517L;

	// Fields

	private Integer scenes = 0;

	private Integer pages = 0;

	/** Actually the number of SECONDS, not minutes, planned, shot or remaining. */
	private Integer minutes = 0;

	private Integer setups = 0;

	// Constructors

	/** default constructor */
	public ScriptMeasure() {
	}

	/** full constructor */
/*	public ScriptMeasure(Integer scenes, Integer pages, Integer minutes,
			Integer setups) {
		this.scenes = scenes;
		this.pages = pages;
		this.minutes = minutes;
		this.setups = setups;
	}
*/
	// Property accessors

	@Column(name = "Scenes")
	public Integer getScenes() {
		return scenes;
	}
	public void setScenes(Integer scene) {
		if (scene == null) {
			scene = 0;
		}
		scenes = scene;
	}

	@Column(name = "Pages")
	public Integer getPages() {
		return pages;
	}
	public void setPages(Integer pages) {
		this.pages = pages;
	}

	@Column(name = "Minutes")
	public Integer getMinutes() {
		return minutes;
	}
	public void setMinutes(Integer minutes) {
		this.minutes = minutes;
	}

	@Column(name = "Setups")
	public Integer getSetups() {
		return setups;
	}
	public void setSetups(Integer setup) {
		if (setup == null) {
			setup = 0;
		}
		setups = setup;
	}

/*	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "scriptMeasureByScriptRemainingId")
	public Dpr getDprsForScriptRemainingId() {
		return this.dprsForScriptRemainingId;
	}

	public void setDprsForScriptRemainingId(Dpr dprsForScriptRemainingId) {
		this.dprsForScriptRemainingId = dprsForScriptRemainingId;
	}

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "scriptMeasureByScriptShotToDateId")
	public Dpr getDprsForScriptShotToDateId() {
		return this.dprsForScriptShotToDateId;
	}

	public void setDprsForScriptShotToDateId(Dpr dprsForScriptShotToDateId) {
		this.dprsForScriptShotToDateId = dprsForScriptShotToDateId;
	}

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "scriptMeasureByScriptDeletedId")
	public Dpr getDprsForScriptDeletedId() {
		return this.dprsForScriptDeletedId;
	}

	public void setDprsForScriptDeletedId(Dpr dprsForScriptDeletedId) {
		this.dprsForScriptDeletedId = dprsForScriptDeletedId;
	}

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "scriptMeasureByScriptShotId")
	public Dpr getDprsForScriptShotId() {
		return this.dprsForScriptShotId;
	}

	public void setDprsForScriptShotId(Dpr dprsForScriptShotId) {
		this.dprsForScriptShotId = dprsForScriptShotId;
	}

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "scriptMeasureByScriptTotalId")
	public Dpr getDprsForScriptTotalId() {
		return this.dprsForScriptTotalId;
	}

	public void setDprsForScriptTotalId(Dpr dprsForScriptTotalId) {
		this.dprsForScriptTotalId = dprsForScriptTotalId;
	}

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "scriptMeasureByScriptShotPriorId")
	public Dpr getDprsForScriptShotPriorId() {
		return this.dprsForScriptShotPriorId;
	}

	public void setDprsForScriptShotPriorId(Dpr dprsForScriptShotPriorId) {
		this.dprsForScriptShotPriorId = dprsForScriptShotPriorId;
	}

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "scriptMeasureByScriptPriorTotalId")
	public Dpr getDprsForScriptPriorTotalId() {
		return this.dprsForScriptPriorTotalId;
	}

	public void setDprsForScriptPriorTotalId(Dpr dprsForScriptPriorTotalId) {
		this.dprsForScriptPriorTotalId = dprsForScriptPriorTotalId;
	}

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "scriptMeasureByScriptAddedId")
	public Dpr getDprsForScriptAddedId() {
		return this.dprsForScriptAddedId;
	}

	public void setDprsForScriptAddedId(Dpr dprsForScriptAddedId) {
		this.dprsForScriptAddedId = dprsForScriptAddedId;
	}

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "scriptMeasureByScriptPlannedId")
	public Dpr getDprsForScriptPlannedId() {
		return this.dprsForScriptPlannedId;
	}

	public void setDprsForScriptPlannedId(Dpr dprsForScriptPlannedId) {
		this.dprsForScriptPlannedId = dprsForScriptPlannedId;
	}
*/
	@Override
	public ScriptMeasure clone() {
		try {
			ScriptMeasure copy = (ScriptMeasure)super.clone();
			copy.id = null; // it's a transient object
			return copy;
		}
		catch (CloneNotSupportedException e) { // should not happen
			return null;
		}
	}

}