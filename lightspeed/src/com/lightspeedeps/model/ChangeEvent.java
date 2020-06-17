package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import com.lightspeedeps.type.TimedEventType;

/**
 * ChangeEvent is an abstract class that adds "old" and "new" values to a
 * TimedEvent, for the purpose of tracking changes in field values. Subclasses
 * will extend this further for tracking changes to particular objects.
 */
@MappedSuperclass // required so this class' fields are persisted when a subclass instance is saved.
public abstract class ChangeEvent extends TimedEvent<ChangeEvent> {

	/** */
	private static final long serialVersionUID = 3095266888404462377L;

	private String newValue;

	private String oldValue;

	public ChangeEvent() {
		super();
	}

	public ChangeEvent(User user, TimedEventType eventType) {
		super(user, eventType);
	}

	@Column(name = "Old_Value", length = 100)
	public String getOldValue() {
		return oldValue;
	}
	public void setOldValue(String oldValue) {
		this.oldValue = oldValue;
	}

	@Column(name = "New_Value", length = 100)
	public String getNewValue() {
		return newValue;
	}
	public void setNewValue(String newValue) {
		this.newValue = newValue;
	}

}
