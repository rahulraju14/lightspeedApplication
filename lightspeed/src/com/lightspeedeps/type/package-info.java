/**
 * Contains all the enumerated types (enum classes) used in the application.
 * <p>
 * <b>IMPORTANT:</b> Where enum values are referenced in model classes (tables), they
 * are usually stored in the database as their name value.  For this reason,
 * great care must be taken before renaming any enum values.
 * <p>
 * Many of the types provide constructors that include a
 * mixed-case equivalent name for each value in the enum.
 * These names are made available via an override of the toString()
 * method in those classes.  In addition, a getLabel() method
 * is frequently provided as an equivalent to toString() that
 * can be accessed from JSP or JSF code.
 * <p>
 * In those classes where groups of enum values may share
 * some underlying quality or trait, an "isXxxx()" method may
 * be provided.  This avoids replicating checks for multiple
 * enum values in various places in the code, and makes it much
 * simpler to change when enum values are added, removed, or
 * their traits are changed for business reasons.  See, for example,
 * {@link com.lightspeedeps.type.AccessStatus#isBillable()}.
 * <p>
 * More complex enum classes may include additional fields
 * to support such options as different sort orders, or various
 * abbreviations. See, for example,
 * {@link com.lightspeedeps.type.DayNightType}.
 */
package com.lightspeedeps.type;
