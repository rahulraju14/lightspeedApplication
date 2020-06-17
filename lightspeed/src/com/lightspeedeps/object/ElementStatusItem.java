/**
 * File: ElementStatus.java
 */
package com.lightspeedeps.object;

import java.io.Serializable;

import com.lightspeedeps.type.ScriptElementType;

/**
 * Used to hold the status of one type of ScriptElement for
 * presentation on the Home page.  (See HomePageBean.java)
 */
public class ElementStatusItem implements Comparable<ElementStatusItem>, Serializable {
	/** */
	private static final long serialVersionUID = - 4600376185073779850L;

	/** The type whose status this instance describes. */
	private ScriptElementType type;
	/** How many of the ScriptElement`s of this type have associated Production Elements. */
	private int selected = 0;
	/** How many ScriptElement`s of this type require Production Elements. */
	private int required = 0;

	public ElementStatusItem(ScriptElementType pType) {
		type = pType;
	}

	/** See {@link #type}. */
	public ScriptElementType getType() {
		return type;
	}
	/** See {@link #type}. */
	public void setType(ScriptElementType type) {
		this.type = type;
	}

	/** True if all the required occurrences of this element type
	 * have been satisfied. */
	public boolean getSatisfied() {
		return (selected >= required);
	}
	/** no-op */
	public void setSatisfied(boolean satisfied) {
		//this.satisfied = satisfied;
	}

	/** See {@link #selected}. */
	public int getSelected() {
		return selected;
	}
	/** See {@link #selected}. */
	public void setSelected(int selected) {
		this.selected = selected;
	}

	/** See {@link #required}. */
	public int getRequired() {
		return required;
	}
	/** See {@link #required}. */
	public void setRequired(int required) {
		this.required = required;
	}

	@Override
	public int compareTo(ElementStatusItem other) {
		return getType().toString().compareTo(other.getType().toString());
	}

}
