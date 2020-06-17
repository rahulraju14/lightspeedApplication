package com.lightspeedeps.model;

import java.math.BigDecimal;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.lightspeedeps.util.common.NumberUtils;

/**
 * PayExpense entity.
 * This represents one line in the "Expenses/Reimbursement" table of
 * a WeeklyTimecard.
 */
@Entity
@Table(name = "pay_expense")
public class PayExpense extends PayExpenseMapped implements Cloneable {

	private static final long serialVersionUID = - 301645342736904666L;

	/** default constructor */
	public PayExpense() {
	}

	/** minimal constructor */
	public PayExpense(WeeklyTimecard wtc, byte lineNumber) {
		super(wtc, lineNumber);
	}

	/** Calculate and set the total of this expense line item. */
	public BigDecimal calculateTotal() {
		BigDecimal tot = NumberUtils.scaleTo2Places(
				NumberUtils.safeMultiply(getQuantity(), getRate()));
		if (tot == null) {
			setTotal(BigDecimal.ZERO);
		}
		else {
			setTotal(tot);
		}
		return tot;
	}

	/**
	 * @see java.lang.Object#clone()
	 */
	@Override
	public PayExpense clone() {
		PayExpense pexp;
		pexp = (PayExpense)super.clone();
		return pexp;
	}

}
