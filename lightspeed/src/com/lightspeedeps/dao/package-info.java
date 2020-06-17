/**
 * Contains the DAO -- Data Access Objects -- used to access
 * all the Hibernate-managed tables. The DAO methods are where the Hibernate
 * transactional boundary occurs.  Entire classes are not marked with the
 * "@Transactional" annotation; instead, only those methods that update the
 * database are so marked.  All the read-only methods are left unmarked.
 * <p>
 * While the original code for the DAOs was generated automatically, most of
 * the common functions (save, delete, etc.)
 * were moved into a superclass ({@link com.lightspeedeps.dao.BaseDAO}). In
 * addition, methods that were common across numerous DAOs except for the
 * returned type were placed in a generic subclass of BaseDAO,
 * {@link com.lightspeedeps.dao.BaseTypeDAO}, which is now the parent class
 * for all the actual concrete DAO classes.  This substantially reduces
 * the amount of (mostly redundant) code required in each of the
 * concrete classes.
 * <p>
 * In addition to the common generated methods found in BaseDAO, there are
 * a number of flavors of "find" which were added to simplify the coding necessary
 * in the concrete classes, e.g., findByProperty, findOne, findCount.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way
 * they should be accessed.
 * <p>
 * There are quite a few methods within the DAO classes that incorporate
 * business logic.  This is typically where the work being performed needs
 * to be enclosed within a transactional environment.
 * <p>
 * Note that there are a few tables that exist which have no DAO (or model)
 * class.  These are tables that are used by Hibernate to manage
 * many-to-many relationships, and are never directly accessed by the
 * application code. (For example, the location_interest table, which
 * handles many-to-many relations between the point_of_interest table
 * and the real_world_element table.)
 *
 * @see com.lightspeedeps.model
 */
package com.lightspeedeps.dao;
