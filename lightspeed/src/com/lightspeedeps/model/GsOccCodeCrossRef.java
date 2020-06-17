/**
 * File: GsOccCodeCrossRef.java
 */
package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * An entity representing a cross-reference between Lightspeed occupation
 * codes and "Green screen" (GS), i.e., AS/400, occupation codes.
 */
@Entity
@Table(name = "gs_occ_code_cross_ref")
public class GsOccCodeCrossRef extends PersistentObject<GsOccCodeCrossRef> {

	/** serial id */
	private static final long serialVersionUID = - 3713911956682692658L;

	private Integer clientId;
	private String occCode;
	private String gsOccCode;
	private String union;

	public GsOccCodeCrossRef() {
	}

	@Column(name = "client_id", nullable = false)
	public Integer getClientId() {
		return clientId;
	}
	public void setClientId(Integer clientId) {
		this.clientId = clientId;
	}

	@Column(name = "occ_code", length = 100)
	public String getOccCode() {
		return occCode;
	}
	public void setOccCode(String occCode) {
		this.occCode = occCode;
	}

	@Column(name = "gs_occ_code", length = 12)
	public String getGsOccCode() {
		return gsOccCode;
	}
	public void setGsOccCode(String gsOccCode) {
		this.gsOccCode = gsOccCode;
	}

	@Column(name = "local_union", length = 50)
	public String getUnion() {
		return union;
	}
	public void setUnion(String union) {
		this.union = union;
	}

}
