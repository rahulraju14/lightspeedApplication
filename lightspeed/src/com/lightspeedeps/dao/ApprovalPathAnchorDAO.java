package com.lightspeedeps.dao;

import com.lightspeedeps.model.ApprovalPathAnchor;

public class ApprovalPathAnchorDAO extends BaseTypeDAO<ApprovalPathAnchor>{

	public static ApprovalPathAnchorDAO getInstance() {
		return (ApprovalPathAnchorDAO)getInstance("ApprovalPathAnchorDAO");
	}

}
