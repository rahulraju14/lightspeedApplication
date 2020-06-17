/**
 * File: Item.java
 */
package com.lightspeedeps.object;

import java.io.Serializable;

import com.lightspeedeps.util.common.StringUtils;

/**
 * A class to hold a minimal database object, or reference to such
 * an object. (This is not a persisted object -- there is no SQL
 * table backing it.)
 */
public class Item implements Comparable<Item>, Serializable {
	/** */
	private static final long serialVersionUID = 7775956595515037266L;

	/** The unique database id for the item. */
	private Integer id;

	/** A name typically used when displaying the item. */
	private String name;

	/** The key to use for sorting; if null, the name field is used. */
	private String key;

	/** A flag typically used when the item is in a list, to indicate
	 * its selection state. */
	private boolean selected;

	public Item() {
	}

	public Item(Integer pid) {
		id = pid;
	}

	public Item(Integer pid, String pname) {
		this(pid);
		name = pname;
	}

	/** See {@link #id}. */
	public Integer getId() {
		return id;
	}
	/** See {@link #id}. */
	public void setId(Integer id) {
		this.id = id;
	}

	/** See {@link #name}. */
	public String getName() {
		return name;
	}
	/** See {@link #name}. */
	public void setName(String name) {
		this.name = name;
	}

	/** See {@link #key}. */
	public void setKey(String key) {
		this.key = key;
	}
	/** See {@link #key}. */
	public String getKey() {
		return key;
	}

	/** See {@link #selected}. */
	public boolean getSelected() {
		return selected;
	}
	/** See {@link #selected}. */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "id=" + (getId()==null ? "null" : id);
		s += ", name="+ (getName()==null ? "null" : name);
		s += "]";
		return s;
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Item o) {
		if (o == null) {
			return 1;
		}
		String k1 = (key != null ? key : name);
		String k2 = (o.key != null ? o.key : o.name);
		return StringUtils.compareIgnoreCase(k1,k2);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Item other = (Item)obj;
		if (id == null) {
			if (other.id != null)
				return false;
		}
		else if ( ! id.equals(other.id))
			return false;
		return true;
	}

}
