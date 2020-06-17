package com.lightspeedeps.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.port.Exporter;

/**
 * This object maintains the four account codes typically associated with a
 * given rate, e.g., hourly-studio rate. This class is extended by various
 * classes that add rate amounts, guaranteed hours, or other related fields.
 * Those classes typically define the information for one row of some payroll
 * table such as pay rates, allowances, or expenses.
 * <p>
 * This class is @Embeddable because its instances are persisted as part of
 * the StartForm object.  It is also @MappedSuperclass since a number of its
 * subclasses are @Embeddable.
 */
@Embeddable
@MappedSuperclass
public class AccountCodes implements Serializable, Cloneable {
	private static final Log log = LogFactory.getLog(AccountCodes.class);

	private static final long serialVersionUID = 1L;

	// Fields

	/** Location account code */
	private String aloc;

	/** Major account code, also referred to as Prod/Episode; 4 character max.
	 * In commercial productions this is used as the "Prep/Wrap" account code. */
	private String major;

	/** Detail account code. In commercial productions, this is used
	 * as the "Shoot" account code. */
	private String dtl;

	/** Sub account code */
	private String sub;

	/** Set account code */
	private String set;

	/** Free account code */
	private String free;

	/** Second Free account code */
	private String free2;

	// Constructors

	/** default constructor */
	public AccountCodes() {
	}

	// Property accessors

	/** See {@link #aloc}. */
	@Column(name = "Acct_Loc", length = 10)
	public String getAloc() {
		return aloc;
	}
	/** See {@link #aloc}. */
	public void setAloc(String loc) {
		aloc = loc;
	}

	/**See {@link #major}. */
	@Column(name = "Acct_Major", length = 10)
	public String getMajor() {
		return major;
	}
	/**See {@link #major}. */
	public void setMajor(String major) {
		this.major = major;
	}

	/**See {@link #dtl}. */
	@Column(name = "Acct_Dtl", length = 10)
	public String getDtl() {
		return dtl;
	}
	/**See {@link #dtl}. */
	public void setDtl(String dtl) {
		this.dtl = dtl;
	}

	/** See {@link #sub}. */
	@Column(name = "Acct_Sub", length = 10)
	public String getSub() {
		return sub;
	}
	/** See {@link #sub}. */
	public void setSub(String sub) {
		this.sub = sub;
	}

	/**See {@link #set}. */
	@Column(name = "Acct_Set", length = 10)
	public String getSet() {
		return set;
	}
	/**See {@link #set}. */
	public void setSet(String set) {
		this.set = set;
	}

	/**See {@link #free}. */
	@Column(name = "Acct_Free", length = 10)
	public String getFree() {
		return free;
	}
	/**See {@link #free}. */
	public void setFree(String free) {
		this.free = free;
	}

	/**See {@link #free2}. */
	@Column(name = "Acct_Free2", length = 10)
	public String getFree2() {
		return free2;
	}
	/**See {@link #free}. */
	public void setFree2(String free) {
		free2 = free;
	}

	public void copyFrom(AccountCodes ac) {
		setAloc(ac.getAloc());
		setMajor(ac.getMajor());
		setDtl(ac.getDtl());
		setSub(ac.getSub());
		setSet(ac.getSet());
		setFree(ac.getFree());
		setFree2(ac.getFree2());
	}

	/**
	 * Export data in this instance via an Exporter. This is currently used to
	 * turn this object into a flat record to be loaded into Crew Cards.
	 *
	 * @param ex The Exporter to which each field should be passed.
	 */
	public void exportCrewcards(Exporter ex) {
//		ex.append(getAloc());
		ex.append(getMajor());
		ex.append(getDtl());
//		ex.append(getSub());
		ex.append(getSet());
		ex.append(getFree());
//		ex.append(getAccountFree2());
	}

	/**
	 * Export data in this instance via an Exporter. This is currently used to
	 * turn this object into a flat record. The timecard data can then be loaded
	 * into other products.
	 *
	 * @param ex The Exporter to which each field should be passed.
	 */
	public void exportTabbed(Exporter ex) {
		ex.append(getAloc());
		ex.append(getMajor());
		ex.append(getDtl());
		ex.append(getSub());
		ex.append(getSet());
		ex.append(getFree());
		ex.append(getFree2());
	}

	@Override
	public AccountCodes clone() throws CloneNotSupportedException {
		AccountCodes newObj;
		try {
			newObj = (AccountCodes)super.clone();
		}
		catch (CloneNotSupportedException e) {
			log.error("AccountGroup clone error: ", e);
			throw e;
		}
		return newObj;
	}

}