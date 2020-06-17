/**
 * ExportType.java
 */
package com.lightspeedeps.type;

import javax.faces.model.SelectItem;

/**
 * Enumeration of the types of export of timecards available.
 * The labels defined here are used in the drop-down of choices
 * in the Export dialog box.
 */
public enum ExportType {
		/** ABS Payroll export, CSV */
		ABS			("ABS Payroll export", 			true,	".csv"),
		/** Showbiz Crew Cards export, tabbed */
		CREW_CARDS	("Showbiz Crew Cards export", 	false,	".tab"),
		/** Hot Budget export, CSV */
		HOT_BUDGET	("Hot Budget export", 			true,	".hb"),
		/** Showbiz Budgeting export, tabbed */
		SHOWBIZ_BUDGET	("Showbiz Budgeting export",false,	".ubx"),
		/** JSON export */
		JSON		("JSON export", 				false,	".json"),
		/** Gross Payroll summary export (CSV) */
		PAYROLL		("Gross Payroll Summary (CSV)",	true, 	".csv"),
		/** Complete timecard (tabbed), for TEAM use */
		FULL_TAB	("TEAM Payroll export",			false, 	".tab"),
		/** */
		NONE		("None", 						false, 	""),
		;

		/** Displayed value, e.g., in drop-down lists */
		private String label;

		/** File extension to use */
		private String extension;

		/** True iff this format uses comma as delimiter. */
		private boolean usesComma;

		ExportType(String lab, boolean comma, String ext) {
			label = lab;
			usesComma = comma;
			extension = ext;
		}

		/** See {@link #label}. */
		public String getLabel() {
			return label;
		}

		/** See {@link #extension}. */
		public String getExtension() {
			return extension;
		}

		/** See {@link #usesComma}. */
		public boolean getUsesComma() {
			return usesComma;
		}

		/**
		 * @return a SelectItem instance containing this enum value and its standard
		 *         label.
		 */
		public SelectItem getSelectItem() {
			return new SelectItem(name(), getLabel());
		}

}
