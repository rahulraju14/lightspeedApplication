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

/**
 * Note entity. Holds user-entered notes for Strips.
 *
 * @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "note")
public class Note extends PersistentObject<Note> implements Comparable<Object>, Cloneable {

	private static final long serialVersionUID = 1L;

	// Fields
	private User user;
	private Strip strip;
	private Date time;
	private String text;

	// Constructors

	/** default constructor */
	public Note() {
	}

	/** full constructor */
/*	public Note(User user, Strip strip, Date time, String text) {
		this.user = user;
		this.strip = strip;
		this.time = time;
		this.text = text;
	}
*/
	// Property accessors

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Author_Id")
	public User getUser() {
		return this.user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Strip_Id", nullable = false)
	public Strip getStrip() {
		return this.strip;
	}

	public void setStrip(Strip strip) {
		this.strip = strip;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Time", nullable = false, length = 0)
	public Date getTime() {
		return this.time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	@Column(name = "Text", length = 1000)
	public String getText() {
		return this.text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@Override
	public int compareTo(Object obj) {
		if (obj instanceof Note) {
			return getTime().compareTo(((Note) obj).getTime());
		}
		return 1;
	}

	@Override
	public Object clone() {
		try {
			Note copy = (Note)super.clone();
			copy.id = null; // it's a transient object
			return copy;
		}
		catch (CloneNotSupportedException e) { // should not happen
			return null;
		}
	}

}