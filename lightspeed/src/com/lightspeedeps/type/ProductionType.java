//	File Name:	ProductionType.java
package com.lightspeedeps.type;

/**
 * An enumeration used in the Production class to specify the particular type of
 * production, such as Feature Film, TV Series, or Tours. Agency is Commercial type
 * dealing with mostly talent. In particular, only certain production types allow
 * multiple Project`s within one Production.
 */
public enum ProductionType {
	FEATURE_FILM		("Feature Film"),
	TELEVISION_SERIES	("Television Series"),
	TELEVISION_MOVIE	("Television Movie"),
	TV_COMMERCIALS		("TV Commercials"),
	TOURS				("Tours"),
	INDUSTRIAL			("Industrial"),
	US_TALENT			("US Talent"), 
	CANADA_TALENT		("Canadian Talent"),
	OTHER				("Other");

	private final String heading;

	private ProductionType(String head) {
		heading = head;
	}

	/**
	 * @return True iff this type allows multiple Projects (episodes) within one
	 *         Production.
	 */
	public boolean getEpisodic() {
		return (this == TELEVISION_SERIES || this == TV_COMMERCIALS || this == US_TALENT || this == CANADA_TALENT);
	}

	/**
	 * @return True for production types that are considered feature films. This
	 *         is just a "best guess" based on the type of production chosen
	 *         when the production was created. The Production.mediumType field
	 *         is a more reliable setting if the production is using timecard
	 *         facilities, especially HTG.
	 */
	public boolean getFeature() {
		return (this == FEATURE_FILM || this == TOURS);
	}

	/**
	 * @return True iff this type indicates it is a television production, as
	 *         opposed to a theatrical/film production. This also applies to
	 *         Commercial productions geared toward talent. This is just a
	 *         "best guess" based on the type of production chosen when the
	 *         production was created. The Production.mediumType field is a more
	 *         reliable setting if the production is using timecard facilities,
	 *         especially HTG.
	 */
	public boolean getForTelevision() {
		return (this == TELEVISION_SERIES || this == TV_COMMERCIALS || this == TELEVISION_MOVIE
					|| this == US_TALENT || this == CANADA_TALENT);
	}

	/**
	 * @return True iff this type is "cross-project", meaning that Callsheets
	 *         and PRs will contain all active (shooting) projects, and Units
	 *         are automatically maintained identical across all Projects.
	 */
	public boolean getCrossProject() {
		return (this == TELEVISION_SERIES);
	}

	/** True if timecards and Start documents are related to a specific Project,
	 * instead of just the Production. */
	public boolean hasPayrollByProject() {
		return (this == TV_COMMERCIALS || this == US_TALENT || this == CANADA_TALENT);
	}

	/** True if this is a Commercial project. */
	public boolean isAicp() {
		return (this == TV_COMMERCIALS || this == US_TALENT || this == CANADA_TALENT);
	}

	/** True if this is a Agency/Commercial, i.e., US Talent project. */
	public boolean isUsTalent() {
		return (this == US_TALENT);
	}

	/** True if TOURS type production. */
	public boolean isTours() {
		return (this == TOURS);
	}

	/** True if CANADA_TALENT production type. */
	public boolean isCanadaTalent() {
		return (this == CANADA_TALENT);
	}

	/** True if either US Talent or Canadian Talent production. */
	public boolean isTalent() {
		return (this == CANADA_TALENT || this == US_TALENT);
	}
	/**
	 * Returns a "pretty" mixed-case version of the enumerated value.
	 * This could be enhanced to use the current locale setting for i18n purposes.
	 * <p>
	 * NOTE: This overrides the default toString(), which returns
	 * the same value as name(), which is the exact text of the enum name.
	 */
	@Override
	public String toString() {
		return heading;
	}

	/**
	 * Returns the "pretty" printable version of this enumerated type. It is the same as toString,
	 * but can be used from jsp pages since it follows the bean accessor (getter) naming convention.
	 *
	 * @return The value of the enumerated type as mixed-case text.
	 */
	public String getLabel() {
		return toString();
	}
	
	/**
	 * @return True for adding only mentioned production type in My Timecards production
	 * drop-down
	 */
	public boolean getMyTimeCardProdList() {
		return (this == ProductionType.FEATURE_FILM || this == TELEVISION_SERIES || this == TELEVISION_MOVIE || this == TV_COMMERCIALS);
	}
}
