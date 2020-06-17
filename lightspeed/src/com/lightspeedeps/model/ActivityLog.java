// File: ActivityLog.java
package com.lightspeedeps.model;

import java.util.Date;

import javax.persistence.*;

/**
 * ActivityLog instances track activity within TTCO that is monitored by
 * outside processes, typically to update/synchronize other systems/databases.
 */
@Entity
@Table(name = "activity_log")
public class ActivityLog  extends PersistentObject<ActivityLog> {

	/** serial version id */
	private static final long serialVersionUID = 1005637155331747567L;

	/** Current version of records created by TTCO. */
	private static final byte CURRENT_VERSION = 1;

	/** Personal Address change type of activity.  Once there's more than one or two,
	 * we might make this an enum. LS-4574 */
	public static final String TYPE_PERSONAL_ADDRESS_CHANGE = "PERSONAL_ADDR_CHANGE";

	/** Loan-out (Corp) Address change type of activity. LS-4574 */
	public static final String TYPE_LOANOUT_ADDRESS_CHANGE = "LOANOUT_ADDR_CHANGE";

	// Fields

	/** Version of this entity. */
	private byte version;

	/** The type of activity */
	private String type;

	/** The date/time this activity occurred. */
	private Date created;

	/** The date/time this activity instance was processed (by outside
	 * process).  Not used by TTCO. */
	private Date processed;

	/** The date/time this activity should be re-processed; not used by TTCO. */
	private Date nextRun;

	/** flag available for external processes */
	private boolean skip;

	/** The User.accountNumber of the user related to this activity. */
	private String userAcct;

	/** the "permanent" (or resident) address changed. LS-4574 */
	private boolean permanentAddrChanged;

	/** the Mailing address changed. LS-4574 */
	private boolean mailingAddrChanged;

	// Constructors

	/** default constructor */
	public ActivityLog() {
		version = CURRENT_VERSION;
	}

	/** minimal (non-null) constructor */
	public ActivityLog(String type, Date created) {
		this();
		this.type = type;
		this.created = created;
	}

	@Column(name = "version", nullable = false)
	public byte getVersion() {
		return this.version;
	}
	public void setVersion(byte version) {
		this.version = version;
	}

	@Column(name = "type", nullable = false, length = 20)
	public String getType() {
		return this.type;
	}
	public void setType(String type) {
		this.type = type;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created", nullable = false, length = 19)
	public Date getCreated() {
		return this.created;
	}
	public void setCreated(Date created) {
		this.created = created;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "processed", length = 19)
	public Date getProcessed() {
		return this.processed;
	}
	public void setProcessed(Date processed) {
		this.processed = processed;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "next_run", length = 19)
	public Date getNextRun() {
		return this.nextRun;
	}
	public void setNextRun(Date nextRun) {
		this.nextRun = nextRun;
	}

	/** See {@link #skip}. */
	@Column(name = "skip", nullable = false)
	public boolean getSkip() {
		return skip;
	}
	/** See {@link #skip}. */
	public void setSkip(boolean skip) {
		this.skip = skip;
	}

	@Column(name = "user_acct", length = 20)
	public String getUserAcct() {
		return this.userAcct;
	}
	public void setUserAcct(String userAcct) {
		this.userAcct = userAcct;
	}

	/** See {@link #permanentAddrChanged}. */
	@Column(name = "permanent_addr_changed", nullable = false)
	public boolean getPermanentAddrChanged() {
		return permanentAddrChanged;
	}
	/** See {@link #permanentAddrChanged}. */
	public void setPermanentAddrChanged(boolean permanentAddrChanged) {
		this.permanentAddrChanged = permanentAddrChanged;
	}

	/** See {@link #mailingAddrChanged}. */
	@Column(name = "mailing_addr_changed", nullable = false)
	public boolean getMailingAddrChanged() {
		return mailingAddrChanged;
	}
	/** See {@link #mailingAddrChanged}. */
	public void setMailingAddrChanged(boolean mailingAddrChanged) {
		this.mailingAddrChanged = mailingAddrChanged;
	}

}
