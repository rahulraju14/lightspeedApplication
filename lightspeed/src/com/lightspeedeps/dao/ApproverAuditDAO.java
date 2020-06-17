package com.lightspeedeps.dao;

import com.lightspeedeps.model.ApproverAudit;

public class ApproverAuditDAO extends BaseTypeDAO<ApproverAudit>{

	public static ApproverAuditDAO getInstance() {
		return (ApproverAuditDAO)getInstance("ApproverAuditDAO");
	}

}
