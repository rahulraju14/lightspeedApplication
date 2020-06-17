package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import com.lightspeedeps.type.SignatureType;

/**
 * SignatureBox is used to define the signature information that can be placed
 * on a particular document by one or more Users. It specifies the size and
 * location of the signature area on a particular page of the PDF document.
 */
@Entity
@Table(name = "signature_box")
public class SignatureBox extends PersistentObject<SignatureBox> {

	private static final long serialVersionUID = 6403557139545234854L;

	private Integer documentId;

	private Integer pageNumber;

	private Integer x1;

	private Integer x2;

	private Integer y1;

	private Integer y2;

	private Integer maximumSignatures;

	private SignatureType signatureType;

	public SignatureBox() {
		super();
	}

	public SignatureBox(Integer pageNumber, Integer x1, Integer x2, Integer y1,
			Integer y2, Integer maximumSignatures) {
		super();
		this.pageNumber = pageNumber;
		this.x1 = x1;
		this.x2 = x2;
		this.y1 = y1;
		this.y2 = y2;
		this.maximumSignatures = maximumSignatures;
	}

	@Column(name = "Document_Id" , nullable = false)
	public Integer getDocumentId() {
		return documentId;
	}
	public void setDocumentId(Integer documentId) {
		this.documentId = documentId;
	}

	@Column(name = "Page_Number", nullable = false)
	public Integer getPageNumber() {
		return pageNumber;
	}
	public void setPageNumber(Integer pageNumber) {
		this.pageNumber = pageNumber;
	}

	@Column(name = "X1", nullable = false)
	public Integer getX1() {
		return x1;
	}
	public void setX1(Integer x1) {
		this.x1 = x1;
	}

	@Column(name = "X2", nullable = false)
	public Integer getX2() {
		return x2;
	}
	public void setX2(Integer x2) {
		this.x2 = x2;
	}

	@Column(name = "Y1", nullable = false)
	public Integer getY1() {
		return y1;
	}
	public void setY1(Integer y1) {
		this.y1 = y1;
	}

	@Column(name = "Y2", nullable = false)
	public Integer getY2() {
		return y2;
	}
	public void setY2(Integer y2) {
		this.y2 = y2;
	}

	@Column(name = "Maximum_Signatures", nullable = false)
	public Integer getMaximumSignatures() {
		return maximumSignatures;
	}
	public void setMaximumSignatures(Integer maximumSignatures) {
		this.maximumSignatures = maximumSignatures;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "Signature_Type" , nullable = false, length = 30)
	public SignatureType getSignatureType() {
		return signatureType;
	}
	public void setSignatureType(SignatureType signatureType) {
		this.signatureType = signatureType;
	}

}
