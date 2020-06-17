/**
 * Contains classes which are POJOs representing each of the persisted objects in the database.
 * Basically there is one class here for each table in the database, and each class contains one field for
 * each column in the corresponding table.
 * There is also a corresponding DAO class for each model class.
 * <p>
 * Each class typically contains accessors (get... methods) and mutators (set... methods) for each field.
 * They also frequently have equals, hashCode, and compareTo methods.  A number of them have variations on the
 * compareTo method which allow the caller to specify a sort key.  This is typical for objects which appear
 * in prominent lists within the UI, where the user may choose to sort them by more than one property (column).
 * <p>
 * Note that in a very few cases, there are database tables which do NOT have corresponding classes in this package,
 * or any package.  Those tables are for tracking many-to-many relations, and are referenced in the
 * hibernate annotations of the model files for the classes that are being related.  In v2.0,
 * there are three of these tables: child_element, location_interest, and scene_script_element.
 * <p>
 * Simple objects (POJOs) that are NOT persisted in the database are in the {@link com.lightspeedeps.object} package.
 *
 * @see com.lightspeedeps.dao
 * @see com.lightspeedeps.object
 */
package com.lightspeedeps.model;
