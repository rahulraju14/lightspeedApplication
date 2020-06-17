package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import static javax.persistence.GenerationType.IDENTITY;

import javax.persistence.Id;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lightspeedeps.type.ObjectType;

/**
 * This class may be used as a superclass for any Hibernate-persisted model class.
 * It defines the unique database id field ('id'), an equals() method that checks
 * for equal id values, and a hashCode() method which uses the 'id' value.
 */
@SuppressWarnings("rawtypes")
@MappedSuperclass
public abstract class PersistentObject<T extends PersistentObject> implements java.io.Serializable {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(PersistentObject.class);

	private static final long serialVersionUID = 1L;

	// Fields

	/** The unique database id of this instance. */
	protected Integer id;

	// Constructors

	/** default constructor */
	public PersistentObject() {
	}

	// Property accessors
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "Id", unique = true, nullable = false)
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @return This object's type, for purposes of AuditEvent logging.
	 */
	@JsonIgnore
	@Transient
	public ObjectType getObjectType() {
		return null;
	}

	/**
	 * Determine if 'this' object is equal to the given object based on business
	 * data contained within the objects. This is called if the equals() method
	 * was called and one or both of the two instances being compared had a null
	 * 'id'.
	 * <p>
	 * By default, we return false. Typically in the Lightspeed application, we
	 * do not deal with transient instances -- ones that do not have an 'id'
	 * value.
	 * <p>
	 * If a particular class needs to do a business-data comparison in this
	 * situation, that class can override this method.
	 *
	 * @param obj The instance to compare to the 'this' instance.
	 * @return True if the two instances are equal.
	 */
	protected boolean equalsData(T obj) {
		return false;
	}

	/**
	 * Besides doing the typical checks for instance equivalence or a null
	 * argument, this method also verifies that the given object is of the
	 * correct class, and if the 'id' (database identifier) values are equal or
	 * not. Instances with equal (non-null) 'id' values are considered equal
	 * without any further checking.
	 * <p>
	 * In the case where one or both 'id' values are null, the
	 * {@link #equalsData(PersistentObject)} method is called and its return
	 * value is returned to our caller.
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		T other;
		try {
			other = (T)obj;
			if (id != null && other.getId() != null) {
				return id.equals(other.getId());
			}
		}
		catch (ClassCastException e) {
			return false;
		}
		return equalsData(other);
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
		return result;
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "id=" + (getId()==null ? "null" : id);
		s += "]";
		return s;
	}

}
