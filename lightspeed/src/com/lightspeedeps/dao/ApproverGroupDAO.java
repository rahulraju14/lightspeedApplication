package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.ApproverGroup;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;

public class ApproverGroupDAO extends BaseTypeDAO<ApproverGroup> {

	private static final Log log = LogFactory.getLog(ApproverGroupDAO.class);

	public static ApproverGroupDAO getInstance() {
		return (ApproverGroupDAO)getInstance("ApproverGroupDAO");
	}

	@Transactional
	public Integer createApproverGroup(String groupName, Production currentProduction, Project project, boolean usePool) {
		ApproverGroup newApproverGroup = new ApproverGroup();
		newApproverGroup.setGroupName(groupName);
		newApproverGroup.setProduction(currentProduction);
		if (project != null) {
			newApproverGroup.setProject(project);
		}
		newApproverGroup.setUsePool(usePool);
		newApproverGroup.setApprover(null);
		Integer newApproverGroupId = save(newApproverGroup);
		log.debug("Group id saved " + newApproverGroupId +" and name " + groupName);
		return newApproverGroupId;
	}

}
