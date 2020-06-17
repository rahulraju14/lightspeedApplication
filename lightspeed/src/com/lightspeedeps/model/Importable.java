/**
 * File: Importable.java
 */
package com.lightspeedeps.model;

import java.util.Date;

import com.lightspeedeps.type.ActionType;

/**
 * An interface extracted from commonalities of ContactImport (used in Project /
 * Import), and CreateUserContact (used in the Lightspeed Web Service "Account"
 * access point).
 */
public interface Importable {

	/** The action to perform with this entry: Add, Replace, or Delete. */
	public ActionType getAction();

	/** Required -- Employee's email address. This is the unique identifier for
	 * the User account to be created, or the existing User who will be added to
	 * the given Production, or have a role added to the production. It must be
	 * a syntactically valid email address. */
	public String getEmailAddress();

	/** The User.accountNumber of the person who uploaded this data
	 * into the ContactImport table. */
//	public String getUserAccount();

	/** Production to which this entry will be added. */
	public Production getProduction();

	public String getEpisodeCode();

	public String getEpisodeTitle();

	public String getJobId();

	/** Employee's first (given) name. */
	public String getFirstName();

	/** Employee's last (family) name. */
	public String getLastName();

	/** The Department to which the employee will be assigned. This is used
	 * when creating the Employment object.  A custom Department will be created
	 * if necessary. */
	public String getDepartment();

	/** Employee's occupation. This will be used to create the Employment object
	 * as well as the StartForm. A custom Role will be created to match this
	 * if necessary. */
	public String getOccupation();

	public String getWorkCity();

	public String getWorkState();

	public String getLastNameFirstName();

	public Date getProjectStartDate();

	public Date getProjectedEndDate();

	public String getPhone();


}
