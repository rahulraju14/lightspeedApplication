package com.lightspeedeps.dao;

import com.lightspeedeps.model.SignatureBox;

public class SignatureBoxDAO extends BaseTypeDAO<SignatureBox> {

	public static SignatureBoxDAO getInstance() {
		return (SignatureBoxDAO)getInstance("SignatureBoxDAO");
	}
}
