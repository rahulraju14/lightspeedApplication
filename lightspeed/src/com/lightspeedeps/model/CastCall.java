package com.lightspeedeps.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import com.lightspeedeps.util.common.StringUtils;

/**
 * CastCall entity.
 * Each object represents one line in the "Cast" section of a Call Sheet report.
 *
 */
@Entity
@Table(name = "cast_call")
public class CastCall extends PersistentObject<CastCall> implements Comparable<CastCall> {
	private static final long serialVersionUID = 1923317370319840709L;

	// Fields
	private Callsheet callsheet;

	/** Element id (cast id) text string for this Character. */
	private String actorIdStr;

	/** Element id (cast id), as numeric sort key, for this Character. */
	private Integer actorId;

	/** database id of the original Contact represented; NOT defined as
	 * a foreign key. */
	private Integer contactId;

	/** The name of the Character (Script Element). */
	private String characterName;

	/** The name of the cast member assigned to this Character. */
	private String name;

	/** The work status for this cast member on this date. */
	private String status;

	/** Time for cast member to be picked up. */
	private Date pickup;

	/** Time for cast member to report to Make-up. */
	private Date makeup;

	/**Time for cast member to report On-Set. */
	private Date rehearse;

	/** Time for cast member to report On-Set. */
	private Date onSet;

	/** Free text field for the user */
	private String note;

	/** Used during call sheet creation to track which entries have been udpated
	 * as each project is processed. */
	@Transient
	private boolean updated;

	// Constructors

	/** default constructor */
	public CastCall() {
	}

	/** full constructor */
/*	public CastCall(Callsheet callsheet, Integer actorId, String characterName,
			String name, String status, Date pickup, Date makeup,
			Date rehearse, Date onSet, String note) {
		this.callsheet = callsheet;
		this.actorId = actorId;
		this.characterName = characterName;
		this.name = name;
		this.status = status;
		this.pickup = pickup;
		this.makeup = makeup;
		this.rehearse = rehearse;
		this.onSet = onSet;
		this.note = note;
	}
*/
	// Property accessors

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Callsheet_Id", nullable = false)
	public Callsheet getCallsheet() {
		return callsheet;
	}

	public void setCallsheet(Callsheet callsheet) {
		this.callsheet = callsheet;
	}

	/** See {@link #contactId}. */
	@Column(name = "Contact_Id")
	public Integer getContactId() {
		return contactId;
	}
	/** See {@link #contactId}. */
	public void setContactId(Integer contactId) {
		this.contactId = contactId;
	}

	/** See {@link #actorIdStr}. */
	@Column(name = "Actor_Id_Str", length = 10)
	public String getActorIdStr() {
		return actorIdStr;
	}
	/** See {@link #actorIdStr}. */
	public void setActorIdStr(String actorId) {
		actorIdStr = actorId;
	}

	/** See {@link #actorId}. */
	@Column(name = "Actor_Id")
	public Integer getActorId() {
		return actorId;
	}
	/** See {@link #actorId}. */
	public void setActorId(Integer actorId) {
		this.actorId = actorId;
	}

	/**See {@link #characterName}. */
	@Column(name = "Character_Name", length = 100)
	public String getCharacterName() {
		return characterName;
	}
	/**See {@link #characterName}. */
	public void setCharacterName(String characterName) {
		this.characterName = characterName;
	}

	/**See {@link #name}. */
	@Column(name = "Name", length = 50)
	public String getName() {
		return name;
	}
	/**See {@link #name}. */
	public void setName(String name) {
		this.name = name;
	}

	/**See {@link #status}. */
	@Column(name = "Status", nullable = false, length = 30)
	public String getStatus() {
		return status;
	}
	/**See {@link #status}. */
	public void setStatus(String status) {
		this.status = status;
	}

	/**See {@link #pickup}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Pickup", length = 0)
	public Date getPickup() {
		return pickup;
	}
	/**See {@link #pickup}. */
	public void setPickup(Date pickup) {
		this.pickup = pickup;
	}

	/**See {@link #makeup}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Makeup", length = 0)
	public Date getMakeup() {
		return makeup;
	}
	/**See {@link #makeup}. */
	public void setMakeup(Date makeup) {
		this.makeup = makeup;
	}

	/**See {@link #rehearse}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Rehearse", length = 0)
	public Date getRehearse() {
		return rehearse;
	}
	/**See {@link #rehearse}. */
	public void setRehearse(Date rehearse) {
		this.rehearse = rehearse;
	}

	/**See {@link #onSet}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "OnSet", length = 0)
	public Date getOnSet() {
		return onSet;
	}
	/**See {@link #onSet}. */
	public void setOnSet(Date onSet) {
		this.onSet = onSet;
	}

	/**See {@link #note}. */
	@Column(name = "Note", length = 1000)
	public String getNote() {
		return note;
	}
	/**See {@link #note}. */
	public void setNote(String note) {
		this.note = note;
	}

	/**See {@link #updated}. */
	@Transient
	public boolean getUpdated() {
		return updated;
	}
	/**See {@link #updated}. */
	public void setUpdated(boolean updated) {
		this.updated = updated;
	}

	@Override
	public int compareTo(CastCall other) {
		//log.debug("this: "+ this + ", other: " + other);
		int ret = 0;
		if (other == null) {
			return 1;
		}

		if (getActorId() == null) {
			if (other.getActorId() == null) {
				ret = 0;
			}
			else {
				ret = 1;
			}
		}
		else {
			if (other.getActorId() == null) {
				ret = -1;
			}
			else {
				ret = getActorId().compareTo(other.getActorId());
			}
		}
		if (ret == 0) {
			ret = StringUtils.compare(getName(), other.getName());
		}
		return ret;
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "actor=" + actorId;
		s += ", st=" + status;
		s += ", char=" + characterName;
		s += ", name=" + name;
		s += ", p/u=" + pickup;
		s += ", m/u=" + makeup;
		s += ", reh=" + rehearse;
		s += ", set=" + onSet;
		s += "]";
		return s;
	}


}