package com.lightspeedeps.dao;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.*;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.file.FileRepositoryUtils;
import com.lightspeedeps.web.approver.ApproverUtils;

/**
 * A data access object (DAO) providing persistence and search support for
 * ApprovalPath entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.ApprovalPath
 */

public class ApprovalPathDAO extends BaseTypeDAO<ApprovalPath> {

	private static final Log log = LogFactory.getLog(ApprovalPathDAO.class);

	public static ApprovalPathDAO getInstance() {
		return (ApprovalPathDAO)getInstance("ApprovalPathDAO");
	}

	@Transactional
	public Integer createApprovalPath(String name, Production currentProduction, Project project) {
		ApprovalPath newApprovalPath = new ApprovalPath();
		newApprovalPath.setName(name);
		newApprovalPath.setProduction(currentProduction);
		if (project != null) {
			newApprovalPath.setProject(project);
		}
		Integer newApprovalPathId = save(newApprovalPath);
		log.debug("path id saved " + newApprovalPathId +" and name " + name);
		return newApprovalPathId;
	}

	public ApprovalPath findApprovalPathByNameProject(String approvalPathName, Project project) {
		Map<String, Object> values = new HashMap<>();
		values.put("pathName", approvalPathName);
		//values.put("production", production);
		values.put("project", project);
		return findOneByNamedQuery(ApprovalPath.GET_APPROVAL_PATH_NAME_BY_PROJECT, values);
	}

	public ApprovalPath findApprovalPathByNameProduction(String approvalPathName, Production production) {
		Map<String, Object> values = new HashMap<>();
		values.put("pathName", approvalPathName);
		values.put("production", production);
		return findOneByNamedQuery(ApprovalPath.GET_APPROVAL_PATH_BY_NAME_AND_PRODUCTION, values);
	}

	/**
	 * Generate all the default approval paths for the given Production and
	 * project.
	 *
	 * @param selectedProduction The Production in which the ApprovalPath`s will
	 *            be created.
	 * @param project The Project the ApprovalPath`s will be assigned to; should
	 *            be null for a non-Commercial Production.
	 * @return false if an exception occurred creating any of the paths, otherwise true.
	 */
	@Transactional
	public boolean generateApprovalPaths(Production selectedProduction, Project project) {
		log.debug("");
		boolean ret = false;
		if (selectedProduction != null) {
			DocumentChainDAO documentChainDAO = DocumentChainDAO.getInstance();
			Folder onboardFolder = FileRepositoryUtils.findOnboardingFolder(selectedProduction);
			if (onboardFolder != null) {
				Map<String, Object> values = new HashMap<>();
				log.debug("......folderId " + onboardFolder.getId());
				values.put("folderId", onboardFolder.getId());
				ret = true;
				Set<DocumentChain> chainSet = new HashSet<>(documentChainDAO.findAllOnboardingDocumentChains(selectedProduction));
				if (chainSet != null) {
					ret &= createApprovalPathForStandardDocument(selectedProduction, chainSet,/*form.getDefaultPath(),*/ project);
				}
			}
		}
		return ret;
	}

	/**
	 * Method used to create ApprovalPath for the standard documents when
	 * Onboarding for the production is enabled or when a new Project is added
	 * to the commercial production. If an existing path is found, it will add
	 * the given chain to that path.
	 *
	 * @param prod Production for which Onboarding is enabled or a production to
	 *            which a new project is added.
	 * @param chain Standard document chain for which the approval path will be
	 *            created.
	 * @param pathName Name of the new path.
	 * @param project Project for which the approval path will be created, or
	 *            null for a non-Commercial Production.
	 * @return false if an exception occurred, otherwise true.
	 */
	@Transactional
	public boolean createApprovalPathForStandardDocument(Production prod, Set<DocumentChain> chainSet, /*String pathName,*/ Project project) {
		boolean bret = false;
		log.debug("");
		try {
			String pathName = Constants.DEFAULT_APPROVAL_PATH;
			if (prod != null) {
				ApprovalPath approvalPath = null;
				if (prod.getType().isAicp()) {
					log.debug("");
					approvalPath = findApprovalPathByNameProject(pathName, project);
				}
				else {
					log.debug("");
					approvalPath = findApprovalPathByNameProduction(pathName, prod);
				}
				if (approvalPath != null) {
					approvalPath.getDocumentChains().addAll(chainSet);
					attachDirty(approvalPath);
					log.debug("docChainSet size: " + approvalPath.getDocumentChains().size());
					bret = true;
				}
				else {
					log.debug("");
					approvalPath = new ApprovalPath();
					approvalPath.setName(pathName);
					approvalPath.setProduction(prod);
					if (project != null) {
						approvalPath.setProject(project);
					}
					approvalPath.setUsePool(false);
					if (prod.getType().isCanadaTalent()) { // LS-3661
						// Canadian productions always use pool
						approvalPath.setUsePool(true);
					}
					Set<DocumentChain> docChainSet = new HashSet<>();
					docChainSet.addAll(chainSet);
					approvalPath.setDocumentChains(docChainSet);
					Integer newApprovalPathId = save(approvalPath);
					log.debug("docChainSet size: " + approvalPath.getDocumentChains().size());
					log.debug("path id saved " + newApprovalPathId +" and name " + pathName);
					bret = true;
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError("ApprovalPathBean createApprovalPath failed Exception: ", e);
			MsgUtils.addGenericErrorMessage();
		}
		return bret;
	}

	/**
	 * Copies all the approval paths from the old Project to new Project of a Production.
	 *
	 * @param prod The Production in which the Project will be added.
	 * @param oldProject The source Project.
	 * @param newProject The target project.
	 * @return false if an exception occurred copying any of the paths, otherwise true.
	 */
	@Transactional
	public boolean copyApprovalPaths(Production prod, Project oldProject, Project newProject) {
		log.debug("");
		boolean ret = true;
		if (prod != null) {
			// Create new copies of Approver groups.
			Map<ApproverGroup, ApproverGroup> mapOfAppGroupIds = new HashMap<>();
			List<ApproverGroup> appGroupListOfProject = ApproverGroupDAO.getInstance().
					findByNamedQuery(ApproverGroup.GET_APPROVER_GROUP_BY_PROJECT, map("project", oldProject));
			for (ApproverGroup group : appGroupListOfProject) {
				log.debug("ApproverGroup = " + group.getGroupName());
				ApproverGroup newGroup = new ApproverGroup();
				newGroup.setProduction(group.getProduction());
				newGroup.setGroupName(group.getGroupName());
				newGroup.setUsePool(group.getUsePool());
				newGroup.setProject(newProject);
				if (newGroup.getUsePool()) {
					log.debug("Pool = " + group.getUsePool());
					// To avoid shared references to a collection exception
					Set<Contact> approvalPoolSet = new HashSet<>(group.getGroupApproverPool());
					newGroup.setGroupApproverPool(approvalPoolSet);
				}
				else {
					log.debug("Pool = " + group.getUsePool());
					Approver approver = null;
					approver = ApproverUtils.copyApproverChain(group);
					/*List<Approver> list = new ArrayList<>();
					list = ApproverUtils.getApproverChain(group.getApprover(), list);
					Integer savedApprover = null;
					for (int i = (list.size() - 1); i >= 0; i--) {
						Approver app = list.get(i);
						approver = new Approver();
						approver.setContact(app.getContact());
						log.debug("Approver contact = " + app.getContact().getDisplayName());
						if (savedApprover != null) {
							Approver priorApprover = ApproverDAO.getInstance().findById(savedApprover);
							if (priorApprover != null) {
								approver.setNextApprover(priorApprover);
							}
						}
						else {
							approver.setNextApprover(null);
						}
						approver.setShared(true);
						savedApprover = ApproverDAO.getInstance().save(approver);
					}*/
					if (approver != null) {
						newGroup.setApprover(approver);
					}
				}
				Integer newGroupId = save(newGroup);
				mapOfAppGroupIds.put(group, newGroup);
				ret &= (newGroupId != null);
				log.debug("Ret = " + ret);
			}

			// Create new copies of Approval paths.
			List<ApprovalPath> pathListOfProject = ApprovalPathDAO.getInstance().
					findByNamedQuery(ApprovalPath.GET_APPROVAL_PATH_BY_PROJECT, map("project", oldProject));
			for (ApprovalPath path : pathListOfProject) {
				log.debug("Path = " + path.getName());
				ApprovalPath newPath = new ApprovalPath();
				newPath.setName(path.getName());
				newPath.setUsePool(path.getUsePool());
				newPath.setProduction(path.getProduction());
				newPath.setProject(newProject);
				newPath.setUseFinalApprover(path.getUseFinalApprover());
				if (path.getUsePool()) {
					log.debug("Pool = " + path.getUsePool());
					// To avoid shared references to a collection exception
					Set<Contact> approvalPoolSet = new HashSet<>(path.getApproverPool());
					newPath.setApproverPool(approvalPoolSet);
					if (path.getFinalApprover() != null) {
						Approver finalApp = new Approver(path.getFinalApprover().getContact(), null, true);
						ApproverDAO.getInstance().save(finalApp);
						newPath.setFinalApprover(finalApp);
					}
				}
				else {
					log.debug("Pool = " + path.getUsePool());
					List<Approver> list = new ArrayList<>();
					list = ApproverUtils.getApproverChain(path.getApprover(), list);
					Approver approver = null;
					Integer savedApprover = null;
					for (int i = (list.size() - 1); i >= 0; i--) {
						Approver app = list.get(i);
						approver = new Approver();
						approver.setContact(app.getContact());
						log.debug("Approver contact = " + app.getContact().getDisplayName());
						if (savedApprover != null) {
							Approver priorApprover = ApproverDAO.getInstance().findById(savedApprover);
							if (priorApprover != null) {
								approver.setNextApprover(priorApprover);
							}
						}
						else {
							approver.setNextApprover(null);
						}
						approver.setShared(true);
						savedApprover = ApproverDAO.getInstance().save(approver);
					}
					newPath.setApprover(approver);
				}
				if (path.getEditors() != null && ! path.getEditors().isEmpty()) {
					Set<Contact> editorSet = new HashSet<>(path.getEditors());
					newPath.setEditors(editorSet);
				}
				if (path.getDocumentChains() != null && ! path.getDocumentChains().isEmpty()) {
					Set<DocumentChain> chainSet = new HashSet<>(path.getDocumentChains());
					newPath.setDocumentChains(chainSet);
				}
				Integer id = save(newPath);
				ret &= (id != null);
				log.debug("Ret = " + ret);

				// Create new copies of ApprovalPath Anchors.
				List<ApprovalPathAnchor> anchorList = ApprovalPathAnchorDAO.getInstance().findByProperty("approvalPath", path);
				if (anchorList != null) {
					for (ApprovalPathAnchor anchor : anchorList) {
						log.debug("anchor dept = " + anchor.getDepartment());
						ApprovalPathAnchor newAnchor = new ApprovalPathAnchor();
						newAnchor.setProduction(anchor.getProduction());
						newAnchor.setProject(newProject);
						newAnchor.setDepartment(anchor.getDepartment());
						newAnchor.setApprovalPath(newPath);
						//We are not using Anchor's approver
						newAnchor.setApprover(null);
						if (anchor.getApproverGroup() != null && mapOfAppGroupIds.get(anchor.getApproverGroup()) != null) {
							newAnchor.setApproverGroup(mapOfAppGroupIds.get(anchor.getApproverGroup()));
						}
						Integer newId = ApprovalPathAnchorDAO.getInstance().save(newAnchor);
						ret &= (newId != null);
						log.debug("Ret = " + ret);
					}
				}
			}
		}
		return ret;
	}
}
