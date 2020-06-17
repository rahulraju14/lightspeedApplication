package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.lightspeedeps.type.DayNightType;
import com.lightspeedeps.type.IntExtType;

/**
 * StripColor entity.
 */
@Entity
@Table(name = "strip_color", uniqueConstraints = @UniqueConstraint(columnNames = {
		"IE_Type", "DN_Type" }))
public class StripColor extends PersistentObject<StripColor> implements java.io.Serializable {

	// Fields
	private static final long serialVersionUID = -3637586253854113992L;

	private IntExtType ieType;
	private DayNightType dnType;
	private Integer backgroundRgb;
	private Integer textRgb;

	// Constructors

	/** default constructor */
	public StripColor() {
	}

	/** full constructor */
	public StripColor(IntExtType ieType, DayNightType dnType, Integer backgroundRgb,
			Integer textRgb) {
		this.ieType = ieType;
		this.dnType = dnType;
		this.backgroundRgb = backgroundRgb;
		this.textRgb = textRgb;
	}

	// Property accessors

	@Enumerated(EnumType.STRING)
	@Column(name = "IE_Type", nullable = false, length = 30)
	public IntExtType getIeType() {
		return ieType;
	}

	public void setIeType(IntExtType ieType) {
		this.ieType = ieType;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "DN_Type", nullable = false, length = 30)
	public DayNightType getDnType() {
		return dnType;
	}

	public void setDnType(DayNightType dnType) {
		this.dnType = dnType;
	}

	@Column(name = "Background_RGB", nullable = false)
	public Integer getBackgroundRgb() {
		return backgroundRgb;
	}

	public void setBackgroundRgb(Integer backgroundRgb) {
		this.backgroundRgb = backgroundRgb;
	}

	@Column(name = "Text_RGB", nullable = false)
	public Integer getTextRgb() {
		return textRgb;
	}

	public void setTextRgb(Integer textRgb) {
		this.textRgb = textRgb;
	}

}