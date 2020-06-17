package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.type.RealLinkStatus;

/**
 * RealLink entity. Each one indicates a pairing of a production
 * element ({@link RealWorldElement}) and the {@link ScriptElement} that it will
 * represent in the filmed scene.
 */
@Entity
@Table(name = "real_link", uniqueConstraints = @UniqueConstraint(columnNames = {
		"Script_Element_Id", "Real_Element_Id" }))
public class RealLink extends PersistentObject<RealLink> implements Comparable<RealLink> {
	private static final Log log = LogFactory.getLog(RealLink.class);

	/**	Serializable id */
	private static final long serialVersionUID = 5454772699516590850L;

	/** The {@link ScriptElement} being linked. */
	private ScriptElement scriptElement;
	/** The {@link RealWorldElement} - Production Element - being linked. */
	private RealWorldElement realWorldElement;
	/** The status of the relation, indicating if the Production element has
	 * been definitely selected, rejected, or still under consideration. */
	private RealLinkStatus status;

	/** Indicates the entry has been selected in the UI.  Not persisted. */
	@Transient
	private boolean selected;

	// Constructors

	/** default constructor */
	public RealLink() {
	}

	/** full constructor */
/*	public RealLink(ScriptElement scriptElement,
			RealWorldElement realWorldElement, RealLinkStatus status) {
		this.scriptElement = scriptElement;
		this.realWorldElement = realWorldElement;
		this.status = status;
	}
*/
	public RealLink(ScriptElement scriptElement,
			RealWorldElement realWorldElement) {
		this.scriptElement = scriptElement;
		this.realWorldElement = realWorldElement;
		status = RealLinkStatus.SELECTED;
	}

	// Property accessors

	@ManyToOne(fetch = FetchType.LAZY) // Changed to Lazy rev 3.0.4801
	@JoinColumn(name = "Script_Element_Id", nullable = false)
	public ScriptElement getScriptElement() {
		return scriptElement;
	}

	public void setScriptElement(ScriptElement scriptElement) {
		this.scriptElement = scriptElement;
	}

	@ManyToOne(fetch = FetchType.LAZY) // Changed to Lazy rev 3.0.4801
	@JoinColumn(name = "Real_Element_Id", nullable = false)
	public RealWorldElement getRealWorldElement() {
		return realWorldElement;
	}

	public void setRealWorldElement(RealWorldElement realWorldElement) {
		this.realWorldElement = realWorldElement;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Status", nullable = false, length = 30)
	public RealLinkStatus getStatus() {
		return status;
	}

	public void setStatus(RealLinkStatus status) {
		this.status = status;
	}

	/**See {@link #selected}. */
	@Transient
	public boolean getSelected() {
		return selected;
	}
	/**See {@link #selected}. */
	public void setSelected(boolean selected) {
		this.selected = selected;
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

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		boolean b = false;
		try {
			RealLink r = (RealLink)obj;
			b = getId().equals(r.getId());
		}
		catch (Exception e) {
		}
		log.debug("ret=" + b);
		return b;
	}

	@Override
	public int compareTo(RealLink arg0) {
		int comp;
		if (arg0 == null) {
			comp = 1;
		}
		else if (getId() == null) {
			if (arg0.getId() == null) {
				comp = 0;
			}
			else {
				comp = -1;
			}
		}
		else {
			if (arg0.getId() == null) {
				comp = 1;
			}
			else {
				comp = getId().compareTo(arg0.getId());
			}
		}
		return comp;
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += '[';
		s += "id=" + (getId()==null ? "null" : getId());
		s += ']';
		return s;
	}

}