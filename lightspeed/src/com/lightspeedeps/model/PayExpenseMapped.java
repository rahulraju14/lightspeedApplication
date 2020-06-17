package com.lightspeedeps.model;

import java.math.BigDecimal;

import javax.persistence.*;

import org.apache.commons.logging.*;

import com.fasterxml.jackson.annotation.*;
import com.lightspeedeps.port.Exporter;
import com.lightspeedeps.type.PayCategory;
import com.lightspeedeps.util.app.Constants;

/**
 * This is equivalent to the {@link PayExpense} entity, but as a @MappedSuperclass.
 * Hibernate does not allow a single class to be both an @Entity and a @MappedSuperclass,
 * so we basically need two identical classes, one of which is a @MappedSuperclass, and the
 * other which is an @Entity that extends the first one.
 * <p>
 * (We want an extendable (@MappedSuperclass) version of the PayExpense class so that
 * {@link PayBreakdown} can extend it.)
 */
@MappedSuperclass
@JsonIgnoreProperties({"id","weeklyTimecard"}) // used by generateJsonSchema()
public class PayExpenseMapped extends PersistentObject<PayExpenseMapped> implements Cloneable {
	private static final Log log = LogFactory.getLog(PayExpenseMapped.class);

	private static final long serialVersionUID = - 301645342736904666L;

	/** Suffix that appears on PayCategory values when a "Premium rate" is
	 * specified in a Pay Job table.  This suffix is removed during the export. */
	private final static String PREMIUM_SUFFIX = "_PREM";

	// Fields

	/** The WeeklyTimecard that contains this PayExpense line item. */
	private WeeklyTimecard weeklyTimecard;

	/** The database id of the associated weeklyTimecard. Used in ESS. */
	private long weeklyId;

	/** The PayExpense items within one WeeklyTimecard are presented in the
	 * order of this lineNumber. The values are not necessarily contiguous. */
	private byte lineNumber;

	/** Account codes for the expense or pay breakdown line item. */
	private AccountCodes account;

	/** The expense category, as a 'pretty' String, not an enum or enum name. The
	 * equivalent enum value can be retrieved using {@link #getCategoryType()};
	 * the name of the enum can be accessed with {@link #getCategoryEnum()}. */
	private String category;
	/** Number of hours (or other quantity). */
	private BigDecimal quantity;
	/** Rate (dollars) per some unit. */
	private BigDecimal rate;
	/** Calculated as hours times rate */
	private BigDecimal total = BigDecimal.ZERO;

	// Transient fields -- used primarily during export (transfer)

	/** True iff this timecard is for a "Cast" department person.  This affects some
	 * aspects of the exported data, to support AS400 processing. (Added for refactoring of
	 * code in LS-4664.) */
	@Transient
	private boolean isCast;

	/** True iff this timecard is associated with a FormModelReleasePrint instance.
	 * Affects some pay codes exported (for particular PayCategory values, for AS400
	 * processing.  LS-4664 */
	@Transient
	private boolean isModelRelease;

	// Constructors

	/** default constructor */
	public PayExpenseMapped() {
	}

	/** minimal constructor */
	public PayExpenseMapped(WeeklyTimecard weeklyTimeCard, byte lineNumber) {
		weeklyTimecard = weeklyTimeCard;
		this.lineNumber = lineNumber;
	}

	// Property accessors

	/** See {@link #weeklyTimecard}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Weekly_Id", nullable = false)
	public WeeklyTimecard getWeeklyTimecard() {
		return weeklyTimecard;
	}
	/** See {@link #weeklyTimecard}. */
	public void setWeeklyTimecard(WeeklyTimecard wtc) {
		weeklyTimecard = wtc;
	}

	/** See {@link #weeklyId}. */
	@Column(name = "Weekly_Id", updatable = false, insertable = false)
	public long getWeeklyId() {
		return weeklyId;
	}
	/** See {@link #weeklyId}. */
	public void setWeeklyId(long weeklyId) {
		this.weeklyId = weeklyId;
	}

	/** See {@link #lineNumber}. */
	@Column(name = "Line_Number", nullable = false)
	public byte getLineNumber() {
		return lineNumber;
	}
	/** See {@link #lineNumber}. */
	public void setLineNumber(byte lineNumber) {
		this.lineNumber = lineNumber;
	}

	/** See {@link #account}. */
	@Embedded
	@AttributeOverrides( {
		@AttributeOverride(name="aloc", 	column = @Column(name = "Account_Loc", length = 10) ),
		@AttributeOverride(name="major", 	column = @Column(name = "Account_Major", length = 10) ),
		@AttributeOverride(name="dtl", 		column = @Column(name = "Account_Dtl", length = 10) ),
		@AttributeOverride(name="sub", 		column = @Column(name = "Account_Sub", length = 10) ),
		@AttributeOverride(name="set", 		column = @Column(name = "Account_Set", length = 10) ),
		@AttributeOverride(name="free", 	column = @Column(name = "Free", length = 10) ),
		@AttributeOverride(name="free2", 	column = @Column(name = "Free2", length = 10) ),
	} )
	public AccountCodes getAccount() {
		if (account == null) {
			account = new AccountCodes();
		}
		return account;
	}

	/**
	 * See {@link #account}.
	 * <p>
	 * Note: do NOT use this if you want to copy all the account fields from one
	 * place to another! Use {@link #setAccountCodes(AccountCodes)} instead.
	 */
	public void setAccount(AccountCodes account) {
		this.account = account;
	}

	/** surrogate for account.aloc
	 * @see AccountCodes#getAloc() */
	@JsonIgnore
	@Transient
	public String getAccountLoc() {
		return getAccount().getAloc();
	}
	public void setAccountLoc(String code) {
		getAccount().setAloc(code);
	}

	/** surrogate for account.major
	 * @see AccountCodes#getMajor() */
	@JsonIgnore
	@Transient
	public String getAccountMajor() {
		return getAccount().getMajor();
	}
	public void setAccountMajor(String accountMajor) {
		getAccount().setMajor(accountMajor);
	}

	/** surrogate for account.dtl
	 * @see AccountCodes#getDtl() */
	@JsonIgnore
	@Transient
	public String getAccountDtl() {
		return getAccount().getDtl();
	}
	public void setAccountDtl(String accountDtl) {
		getAccount().setDtl(accountDtl);
	}

	/** surrogate for account.Sub
	 * @see AccountCodes#getSub() */
	@JsonIgnore
	@Transient
	public String getAccountSub() {
		return getAccount().getSub();
	}
	public void setAccountSub(String accountSub) {
		getAccount().setSub(accountSub);
	}

	/** surrogate for account.set
	 * @see AccountCodes#getSet() */
	@JsonIgnore
	@Transient
	public String getAccountSet() {
		return getAccount().getSet();
	}
	public void setAccountSet(String accountSet) {
		getAccount().setSet(accountSet);
	}

	/** surrogate for account.free
	 * @see AccountCodes#getFree() */
	@JsonIgnore
	@Transient
	public String getAccountFree() {
		return getAccount().getFree();
	}
	public void setAccountFree(String free) {
		getAccount().setFree(free);
	}

	/** surrogate for account.free2
	 * @see AccountCodes#getFree2() */
	@JsonIgnore
	@Transient
	public String getAccountFree2() {
		return getAccount().getFree2();
	}
	public void setAccountFree2(String free) {
		getAccount().setFree2(free);
	}

	/**
	 * Copy all of the account code fields from the given source into this
	 * object's corresponding account fields.
	 *
	 * @param ac The source of the account codes.
	 */
	@JsonIgnore
	@Transient
	public void setAccountCodes(AccountCodes ac) {
		getAccount().copyFrom(ac);
	}

	/** See {@link #category} */
	@Column(name = "Category", length = 30)
	public String getCategory() {
		return category;
	}
	/** See {@link #category} */
	public void setCategory(String category) {
		this.category = category;
	}

	/**
	 * Used for the JSON file, this returns the name of the Enum value that
	 * matches the "label" text in the 'category' field.
	 *
	 * @return The name() of the PayCategory that matches the category text, or
	 *         PayCategory.CUSTOM if no match is found.
	 */
	@Transient
	public String getCategoryEnum() {
		return getCategoryType().name();
	}

	/**
	 * This returns the Enum value that matches the "label" text in the
	 * 'category' field.
	 *
	 * @return The PayCategory that matches the category text, or
	 *         PayCategory.CUSTOM if no match is found.
	 */
	@JsonIgnore
	@Transient
	public PayCategory getCategoryType() {
		PayCategory cat = PayCategory.CUSTOM;
		if (getCategory() != null) {
			cat = PayCategory.toValue(getCategory());
		}
		return cat;
	}

	/** See {@link #quantity}. */
	@Column(name = "Quantity", precision = 6, scale = 2)
	public BigDecimal getQuantity() {
		return quantity;
	}
	/** See {@link #quantity}. */
	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}

	/** See {@link #rate}. */
	@Column(name = "Rate", precision = 9, scale = 4)
	public BigDecimal getRate() {
		return rate;
	}
	/** See {@link #rate}. */
	public void setRate(BigDecimal rate) {
		this.rate = rate;
	}

	/** See {@link #total}. */
	@Column(name = "Total", precision = 12, scale = 4)
	public BigDecimal getTotal() {
		return total;
	}
	/** See {@link #total}. */
	public void setTotal(BigDecimal total) {
		this.total = total;
	}

	/** See {@link #isCast}. */
	@Transient
	public boolean getIsCast() {
		return isCast;
	}
	/** See {@link #isCast}. */
	public void setIsCast(boolean isCast) {
		this.isCast = isCast;
	}

	/** See {@link #isModelRelease}. */
	@Transient
	public boolean getIsModelRelease() {
		return isModelRelease;
	}
	/** See {@link #isModelRelease}. */
	public void setIsModelRelease(boolean isModelRelease) {
		this.isModelRelease = isModelRelease;
	}

	/**
	 * @see java.lang.Object#clone()
	 */
	@Override
	public PayExpenseMapped clone() {
		PayExpenseMapped pexp;
		try {
			pexp = (PayExpenseMapped)super.clone();
			pexp.id = null;
			pexp.weeklyTimecard = null;
}
		catch (CloneNotSupportedException e) {
			log.error("PayExpense clone error: ", e);
			return null;
		}
		return pexp;
	}

	/**
	 * Export data in this instance via an Exporter. This is currently used to
	 * turn this object into a flat record. The timecard data can then be loaded
	 * into other products, such as Crew Cards.
	 *
	 * @param ex The Exporter to which each field should be passed.
	 */
	public void exportCrewcards(Exporter ex) {
//		ex.append(getId());
//		ex.append(getLineNumber());

		getAccount().exportCrewcards(ex);	// export account codes

		ex.append(getCategory());
		ex.append(getQuantity());
		ex.append(getRate(), 4);
//		ex.append(getTotal()); // not included in export
	}

	/**
	 * Export data in this instance via an Exporter. This is currently used to
	 * turn this object into a flat record. The timecard data can then be loaded
	 * into other products.
	 *
	 * @param ex The Exporter to which each field should be passed.
	 */
	public void exportTabbed(Exporter ex) {
//		ex.append(getId());
//		ex.append(getLineNumber());

		getAccount().exportTabbed(ex);	// export account codes
		exportCategoryEnum(ex, null);
		PayCategory type = getCategoryType();
		if (type == PayCategory.SAL_ADVANCE_NONTAX ||
				type == PayCategory.SAL_ADVANCE_TAX ||
				type == PayCategory.LODGING_ADVANCE ||
				type == PayCategory.PER_DIEM_ADVANCE) {
			// LS-1588 "Advance" pay categories should always export as positive
			ex.append(getQuantity().abs());
			if (getRate() != null) { // LS-1688
				ex.append(getRate().abs(), 4); // LS-1588
			}
			else {
				ex.append(getRate(), 4);
			}
		}
		else {
			ex.append(getQuantity());
			ex.append(getRate(), 4);
		}
//		ex.append(getTotal()); // not included in export
	}

	/**
	 * Export the category as an enum name, if possible.
	 * @param ex The Exporter to be used for the output.
	 * @param multiplier Multiplier value or null; affects export category.
	 */
	protected void exportCategoryEnum(Exporter ex, BigDecimal multiplier) {
		if (getCategory() == null) {
			ex.append((String)null);
		}
		else {
			String cat = getExportName(multiplier); //LS-2011 Change export pay codes for Travel Day Type added multiplier as argument
			ex.append(cat);
		}
	}

	/**
	 * @return The name used for this entry when exported. This is currently
	 *         customized for the TEAM export. This method is called from JSP.
	 */
	@Transient
	public String getExportName() {
		return getExportName(BigDecimal.ZERO);
	}

	/**
	 * Determine the export name for this instance. LS-4664
	 *
	 * @param modelRelease True if this is for a model-release associated
	 *            timecard.
	 * @return The name used for this entry when exported. This is currently
	 *         customized for the TEAM export. This method is called from JSP.
	 */
	@Transient
	public String getExportName(boolean modelRelease) {
		isModelRelease = modelRelease;
		return getExportName(BigDecimal.ZERO);
	}

	/**
	 * @param multiplier Multiplier value or null; affects export category.
	 * @return The name used for this entry when exported. This is currently
	 *         customized for the TEAM export.
	 */
	@Transient
	public String getExportName(BigDecimal multiplier) {
		if (getCategory() == null) {
			return "";
		}
		PayCategory type = getCategoryType();
		String cat = type.name();
		if (isModelRelease && type.getForModelRelease()) { // Model release reimbursement catgory
			cat = type.getModelReleaseCode(); // uses specific pay codes for AS400. LS-4664
		}
		if (type == PayCategory.CUSTOM) { // category didn't match any enum
			if (getCategory().startsWith(Constants.EXTENDED_DAY_PREFIX)) {
				cat = getCategory().substring(Constants.EXTENDED_DAY_PREFIX.length());
				cat = "EXT_DAY_X" + cat;
			}
			else if (getCategory().startsWith("Travel@")) {
				cat = "TRAVEL"; // drop multiplier suffix
				// LS-2011 use multiplier to change export pay codes for Travel Day Type
				if (getCategory().contains("1.5x")) {
					cat = "150";
				}
				else if (getCategory().contains("2.0x")) {
					cat = "200";
				}
			}
			else if (getCategory().startsWith(PayCategory.HOLIDAY_WKD.getLabel())) {
				cat = PayCategory.HOLIDAY_WKD.name(); // drop multiplier suffix
			}
			else if (getCategory().startsWith(PayCategory.SICK_PAY.getLabel())) {
				//SD-2928 AS400 is expecting the value without the multiplier
				cat = PayCategory.SICK_PAY.name(); //  drop multiplier suffix
			}
			else {
				cat = getCategory();	// so export (user-entered?) custom text
			}
		}
		else if (type.name().startsWith(PayCategory.HOLIDAY_WKD.name())) {
			cat = PayCategory.HOLIDAY_WKD.name(); // drop multiplier suffix
		}
		else if (cat.endsWith(PREMIUM_SUFFIX)) { // LS-1426
			// strip PREM suffix for labor codes
			int i = cat.indexOf(PREMIUM_SUFFIX);
			cat = cat.substring(0, i);
		}
		else if (isCast && type == PayCategory.X10_HOURS) {
			// LS-1140: use special pay code for straight time for "Cast" employees
			cat = WeeklyTimecard.PB_MAPPED_STRAIGHT_TIME_CODE;
		}
		else if (type.name().startsWith("TRAVEL") && multiplier != null) {
			// LS-2011 use multiplier to change export pay codes for Travel Day Type
			if (PayCategory.X10_HOURS.getMultiplier().compareTo(multiplier) == 0) {
				cat = "TRAVEL";
			}
			if (PayCategory.X15_HOURS.getMultiplier().compareTo(multiplier) == 0) {
				cat = "150";
			}
			if (PayCategory.X15_HOURS.getMultiplier().compareTo(multiplier) == 0) {
				cat = "200";
			}
		}
		return cat;
	}

}
