package com.lightspeedeps.type;

/**
 * Types of selector values used to limit Roles available for some departments,
 * based on Worker's Comp setting, or EOR (Employer-of-Record) values in the
 * current Project or Production.
 */
public enum RoleSelectType {
	WC_N ("Worker's Comp = No"),
	WC_Y ("Worker's Comp = Yes"),
	EOR_S ("EOR = S"),
	EOR_D ("EOR = D"),
	;

	private String label;

	RoleSelectType(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	@Override
	public String toString() {
		return label;
	}
}
