package com.lightspeedeps.type;

/**
 * An enumeration used in UserPreferences for callsheet version .
 */
public enum CallSheetVersion {
		FULL,
		BRIEF,
		CUSTOM;

		private final static String[] headings =
				{ "Full", "Brief", "Custom" };

		@Override
		public String toString() {
			String s = headings[this.ordinal()];
			return s;
		}

}