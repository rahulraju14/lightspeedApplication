//	File Name:	TextElementType.java
package com.lightspeedeps.type;

/**
 * The type of TextElement. Each TextElement represents one or more lines from a
 * Script, and its type is generally determined during the import process.
 * <p>
 * NOTE: Code in ImportPdf depends on the order of the enumeration below!!
 * <p>
 * Do not change the order or add (or delete) values without reviewing that
 * code, in particular the action/state transition table(s). If types are added
 * or removed, the state-change arrays in ImportPdf.java MUST be updated
 * accordingly.
 */
public enum TextElementType {
	ACTION			(8,		true), /*  8 * 7 =  56px in CSS */
	DIALOGUE		(18,	true), /* 18 * 7 = 126px in CSS */
	CHARACTER		(28,	true), /* 28 * 7 = 196px in CSS */
	PARENTHETICAL	(23,	true), /* 23 * 7 = 161px in CSS */
	SCENE_HEADING	(0,		false),/*  0 * 7 =   0px in CSS */
	TRANSITION		(38,	true), /* 38 * 7 = 266px in CSS */
	OTHER			(8,		true), /*  8 * 7 =  56px in CSS */
	PAGE_HEADING	(0,		false), /* 0 * 7 =   0px in CSS */
	START_ACT		(28,	false), /*28 * 7 = 196px in CSS */
	END_ACT			(28,	false), /*28 * 7 = 196px in CSS */
	CONTINUATION	(38,	false), /*38 * 7 = 266px in CSS */
	BLANK			(0,		false), /* 0 * 7 =   0px in CSS */
	MORE			(28,	false), /* ? -  centered in CSS */
	PAGE_FOOTER		(0,		false), /* 0 * 7 =   0px in CSS */
	PAGE_TOP		(1,		false), // only used in online output
	IGNORE			(0,		false);
	// ** If adding new types, the state-change arrays in ImportPdf.java MUST
	// be updated!! **

	/**
	 * The left margin values assigned to each type should correlate with the
	 * "padding-left" values specified in CSS. The value is used to compute the
	 * character padding needed to position the 'change indicator', "*", at the
	 * right margin of the online script display. These values indicate the
	 * number of characters, not pixels.
	 */
	private int leftMargin;

	/**
	 * Indicates if this type of entry is considered "content" when comparing
	 * two scenes to see how similar they are between two revisions of a script.
	 * Basically we're concerned with action and dialogue, and trying to ignore
	 * anything that might change due to page breaks moving.
	 */
	private boolean isSceneContent;

	TextElementType(int leftMar, boolean content) {
		leftMargin = leftMar;
		isSceneContent = content;
	}

	/** See {@link #leftMargin}. */
	public int getLeftMargin() {
		return leftMargin;
	}

	/** See {@link #isSceneContent}. */
	public boolean isSceneContent() {
		return isSceneContent;
	}

	/**
	 * @return True iff this element type is part of a Character's dialogue,
	 * meaning it must be either a Dialogue or Parenthetical type.
	 */
	public boolean isDialogue() {
		return this == DIALOGUE || this == PARENTHETICAL;
	}

}
