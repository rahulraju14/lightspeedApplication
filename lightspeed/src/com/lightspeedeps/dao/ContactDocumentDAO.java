package com.lightspeedeps.dao;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.*;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.DocumentStatusCount;
import com.lightspeedeps.object.LoggedException;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.util.payroll.TimecardUtils;
import com.lightspeedeps.web.approver.ApproverUtils;
import com.lightspeedeps.web.onboard.StatusListBean;

public class ContactDocumentDAO extends ApprovableDAO<ContactDocument> {

	private static final Log log = LogFactory.getLog(ContactDocumentDAO.class);

	/** List of "LS Admin" and "LS Data Admin" Role Id's used for filtering ContactDocument`s */
	private static List<Integer> adminRoleIdList = new ArrayList<>();
	static {
		adminRoleIdList.add(Constants.ROLE_ID_LS_ADMIN);
		adminRoleIdList.add(Constants.ROLE_ID_LS_ADMIN_VIEW);
		adminRoleIdList.add(Constants.ROLE_ID_LS_VIP);
		adminRoleIdList.add(Constants.ROLE_ID_DATA_ADMIN);
		adminRoleIdList.add(Constants.ROLE_ID_FINANCIAL_ADMIN);
	}

	public static ContactDocumentDAO getInstance() {
		return (ContactDocumentDAO)getInstance("ContactDocumentDAO");
	}

	/**
	 * Override standard attachDirty so we can catch occurrence of
	 * ContactDocument being stored with "APPROVED_PAST" status.
	 *
	 * @see com.lightspeedeps.dao.BaseDAO#attachDirty(java.lang.Object)
	 */
	@Override
	@Transactional
	public void attachDirty(Object instance) {
		if (instance instanceof ContactDocument) {
			ContactDocument cd = (ContactDocument)instance;
			if (cd.getStatus() == ApprovalStatus.APPROVED_PAST) {
				EventUtils.logError(new IllegalArgumentException(
						"invalid status for persisted ContactDocument = " + cd.getStatus().name()));
				cd.setStatus(ApprovalStatus.SUBMITTED);
			}
			else if (cd.getStatus() == ApprovalStatus.REJECTED_PAST) {
				EventUtils.logError(new IllegalArgumentException(
						"invalid status for persisted ContactDocument = " + cd.getStatus().name()));
				cd.setStatus(ApprovalStatus.RESUBMITTED);
			}
		}
		super.attachDirty(instance);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<Document> findDocByEmploymentId(Integer id) {
		return (List)findByNamedQuery(ContactDocument.GET_DOC_LIST_BY_EMPLOYMENT_ID, map("employmentId", id));
	}

	public Integer findDocCountByEmploymentId(Integer id) {
		return findDocCountByEmploymentId(id, StatusListBean.getInstance().getViewProject());
	}

	public Integer findDocCountByEmploymentId(Integer id, Project proj) {
		Map<String, Object> values = new HashMap<>();
		values.put("employmentId", id);
//		int voidDocCount =  findDocCountOfVoidStatus(id, proj);
		int totalDocCount = 0;
		if (proj != null) {
			values.put("project", proj);
			totalDocCount =  findCountByNamedQuery(ContactDocument.GET_DOC_COUNT_NON_VOID_BY_EMPLOYMENT_PROJECT, values).intValue();
		}
		else {
			totalDocCount = findCountByNamedQuery(ContactDocument.GET_DOC_COUNT_NON_VOID_BY_EMPLOYMENT, values).intValue();
		}
//		totalDocCount = totalDocCount - voidDocCount;
		return totalDocCount;
	}

	public Long findContactCountByDocChainId(Integer id) {
		Production production = SessionUtils.getProduction();
		Map<String, Object> values = new HashMap<>();
		values.put("documentChainId", id);
		Project proj = StatusListBean.getInstance().getViewProject();
		if (proj != null) {
			values.put("project", proj);
			return findCountByNamedQuery(ContactDocument.GET_CONTACT_COUNT_BY_DOCUMENT_CHAIN_PROJECT, values);
		}
		else {
			values.put("production", production);
			return findCountByNamedQuery(ContactDocument.GET_CONTACT_COUNT_BY_DOCUMENT_CHAIN, values);
		}
	}

	public Integer findDocCountByEmploymentIdAndStatus(Integer id) {
		return findDocCountByEmploymentIdAndStatus(id, StatusListBean.getInstance().getViewProject());
	}

	public Integer findDocCountByEmploymentIdAndStatus(Integer id, Project proj) {
		Map<String, Object> values = new HashMap<>();
		values.put("employmentId", id);
		if (proj != null) {
			values.put("project", proj);
			return findCountByNamedQuery(ContactDocument.GET_DOC_COUNT_BY_EMPLOYMENT_AND_FINAL_PROJECT, values).intValue();
		}
		else {
			return findCountByNamedQuery(ContactDocument.GET_DOC_COUNT_BY_EMPLOYMENT_AND_FINAL, values).intValue();
		}
	}

	public Long findContactCountByDocChainIdAndStatus(Integer id) {
		Production production = SessionUtils.getProduction();
		Map<String, Object> values = new HashMap<>();
		values.put("documentChainId", id);
		Project proj = StatusListBean.getInstance().getViewProject();
		if (proj != null) {
			values.put("project", proj);
			return findCountByNamedQuery(ContactDocument.GET_CONTACT_COUNT_BY_DOCUMENT_CHAIN_ID_AND_STATUS_PROJECT, values);
		}
		else {
			values.put("production", production);
			return findCountByNamedQuery(ContactDocument.GET_CONTACT_COUNT_BY_DOCUMENT_CHAIN_ID_AND_STATUS, values);
		}
	}

	public Long findRemainingDocCountByEmploymentId(Integer id) {
		Map<String, Object> values = new HashMap<>();
		values.put("employmentId", id);
		Project proj= StatusListBean.getInstance().getViewProject();
		if (proj != null) {
			values.put("project", proj);
			return findCountByNamedQuery(ContactDocument.GET_REMAINING_DOC_COUNT_BY_EMPLOYMENT_ID_PROJECT, values);
		}
		else {
			return findCountByNamedQuery(ContactDocument.GET_REMAINING_DOC_COUNT_BY_EMPLOYMENT_ID, values);
		}
	}

	public Integer findDocCountOfOpenStatus(Integer id, Project proj) {
		Map<String, Object> values = new HashMap<>();
		values.put("employmentId", id);
		if (proj != null) {
			values.put("project", proj);
			return findCountByNamedQuery(ContactDocument.GET_DOC_COUNT_OF_OPEN_STATUS_BY_EMPLOYMENT_PROJECT, values).intValue();
		}
		else {
			return findCountByNamedQuery(ContactDocument.GET_DOC_COUNT_OF_OPEN_STATUS_BY_EMPLOYMENT, values).intValue();
		}
	}

	@SuppressWarnings("unchecked")
	public List<ContactDocument> findFilteredContactListByStatus(Integer documentChainId, String sqlStatus) {
		String query = "from ContactDocument where Document_Chain_Id = ? and Status in ("+sqlStatus+")";
		List<Object> valueList = new ArrayList<>();
		valueList.add(documentChainId);
		return find(query, valueList.toArray());
	}

	@SuppressWarnings("unchecked")
	public List<ContactDocument> findContactDocumentList (List<Contact> contactList) {
		List<Integer> ids = new ArrayList<>();
		for (Contact contact : contactList) {
			ids.add(contact.getId());
		}
		String contactIds = StringUtils.getStringFromList(ids);
		String query = "from ContactDocument where Contact_Id in ("+contactIds+")";
		return find(query);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ApprovalStatus findWorseStatus(Integer contactId, Integer documentChainId) {
		Map<String, Object> values = new HashMap<>();
		values.put("contactId", contactId);
		values.put("documentChainId", documentChainId);
		List<ApprovalStatus> statusList = (List)findByNamedQuery(ContactDocument.GET_CONTACT_DOCUMENT_STATUS_BY_CONTACT_AND_DOCUMENT_CHAIN, values);
		ApprovalStatus status = ApprovalStatus.BATCH_FINAL_STATUS;
		for (ApprovalStatus contactStatus : statusList) {
			if (contactStatus.compareBatch(status) < 0) {
				status = contactStatus;
			}
		}
		return status;
	}

	/** Method used to find the counts of Contact documents status wise for an employment.
	 * @param employmentId
	 * @param prod
	 * @param project
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public DocumentStatusCount findDocStatusGraphForEmployment(Integer employmentId/*, Production prod, Project project*/) {
		String queryString = null;
		DocumentStatusCount statusCount = null;
		List<Object> valueList = new ArrayList<>();
		valueList.add(employmentId);
//		if (prod != null) {
//			valueList.add(prod);
//			if (prod.getType().isAicp() && project != null) {
//				log.debug("Commercial Production");
//				queryString = "select new com.lightspeedeps.object.DocumentStatusCount( " +
//						"sum(case when cd.status='OPEN' then 1 else 0 end), " +
//						"sum(case when cd.status not in ('APPROVED','LOCKED','PENDING','OPEN','VOID') then 1 else 0 end), " +
//						"sum(case when cd.status in ('APPROVED','LOCKED') then 1 else 0 end), " +
//						"sum(case when cd.status='PENDING' then 1 else 0 end)) " +
//						"from ContactDocument cd where cd.status <> 'VOID' and " +
//						"cd.employment.id = ? and cd.project = ? " ;
//				valueList.add(project);
//			}
//			else {
//				log.debug("Non Commercial Production");
				queryString = "select new com.lightspeedeps.object.DocumentStatusCount( " +
						"sum(case when cd.status='OPEN' then 1 else 0 end), " +
						"sum(case when cd.status not in ('APPROVED','LOCKED','PENDING','OPEN','VOID') then 1 else 0 end), " +
						"sum(case when cd.status in ('APPROVED','LOCKED') then 1 else 0 end), " +
						"sum(case when cd.status='PENDING' then 1 else 0 end)) " +
						"from ContactDocument cd where cd.status <> 'VOID' and " +
						"cd.employment.id = ? " ;
//			}
//		}
//		if (queryString != null) {
			getHibernateTemplate().setMaxResults(1);
			List list = getHibernateTemplate().find(queryString, valueList.toArray());
			//getHibernateTemplate().setMaxResults(0);
			if (list.size() > 0) {
				statusCount = (DocumentStatusCount) list.get(0);
			}
//		}
		return statusCount;
	}

	/** Method used to find the counts of Contact documents status wise for an employment.
	 * @param employmentId
	 * @param prod
	 * @param project
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<DocumentStatusCount> findDocStatusGraphForAllEmployments(Production prod, Project project) {
		List<DocumentStatusCount> empStatusGraph = new ArrayList<>();
		try {
			String queryString = null;
			List<Object> valueList = new ArrayList<>();
			if (prod != null) {
				valueList.add(prod);
				if (prod.getType().isAicp() && project != null) {
					log.debug("Commercial Production");
					queryString = "select new com.lightspeedeps.object.DocumentStatusCount( " +
							"cd.employment.id, "+
							"sum(case when cd.status='OPEN' then 1 else 0 end), " +
							"sum(case when cd.status not in ('APPROVED','LOCKED','PENDING','OPEN','VOID') then 1 else 0 end), " +
							"sum(case when cd.status in ('APPROVED','LOCKED') then 1 else 0 end), " +
							"sum(case when cd.status='PENDING' then 1 else 0 end)) " +
							"from ContactDocument cd where cd.status <> 'VOID' " +
							"and cd.contact.production = ? and cd.project = ? " +
							"group by cd.employment.id" ;
					valueList.add(project);
				}
				else {
					log.debug("Non Commercial Production");
					queryString = "select new com.lightspeedeps.object.DocumentStatusCount( " +
							"cd.employment.id, "+
							"sum(case when cd.status='OPEN' then 1 else 0 end), " +
							"sum(case when cd.status not in ('APPROVED','LOCKED','PENDING','OPEN','VOID') then 1 else 0 end), " +
							"sum(case when cd.status in ('APPROVED','LOCKED') then 1 else 0 end), " +
							"sum(case when cd.status='PENDING' then 1 else 0 end)) " +
							"from ContactDocument cd where cd.status <> 'VOID' " +
							"and cd.contact.production = ? group by cd.employment.id" ;
				}
			}
			if (queryString != null) {
				List list = getHibernateTemplate().find(queryString, valueList.toArray());
				empStatusGraph.addAll(list);
				log.debug("count=" + list.size());
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return empStatusGraph;
	}

	/** Method used to find the counts of Contact documents status wise for an employment.
	 * @param empList List of Employment Ids.
	 * @return
	 */
	@SuppressWarnings({"unchecked"})
	public List<DocumentStatusCount> findDocStatusGraphForSelectedEmployments(List<Integer> empList) {
		List<DocumentStatusCount> empStatusGraph = new ArrayList<>();
		try {
			String queryString = null;
			if (empList != null) {
				log.debug("Commercial Production");
				queryString = "select new com.lightspeedeps.object.DocumentStatusCount( " +
						"cd.employment.id, "+
						"sum(case when cd.status='OPEN' then 1 else 0 end), " +
						"sum(case when cd.status not in ('APPROVED','LOCKED','PENDING','OPEN','VOID') then 1 else 0 end), " +
						"sum(case when cd.status in ('APPROVED','LOCKED') then 1 else 0 end), " +
						"sum(case when cd.status='PENDING' then 1 else 0 end)) " +
						"from ContactDocument cd where cd.status <> 'VOID' " +
						"and cd.employment.id in (:empList) " +
						"group by cd.employment.id" ;
				empStatusGraph = (List<DocumentStatusCount>)getHibernateTemplate().findByNamedParam(queryString, "empList", empList);
				log.debug("count=" + empStatusGraph.size());
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return empStatusGraph;
	}

	/**
	 * Method returns a filtered list of contact documents according to the
	 * parameters passed.
	 *
	 * @param status If "1", restrict results to documents that have a status of
	 *            either APPROVED or LOCKED. If "2", restrict results to
	 *            documents that are NOT either APPROVED or LOCKED. If null, or
	 *            any other value, do not restrict results based on the status.
	 * @param departmentValue If null, ignore. If non-null, restrict results to
	 *            documents for departments whose name contains the given
	 *            string, i.e., match a SQL query of '%'+departmentValue+'%'.
	 * @param contactName If null, ignore. If non-null, restrict results to
	 *            documents for people whose first OR last name contains the
	 *            given string, i.e., either name matches a SQL query of
	 *            '%'+contactName+'%'.
	 * @param occupation If null, ignore. If non-null, restrict results to
	 *            documents for occupations (Role.name) that contain the given
	 *            string, i.e., match a SQL query of '%'+occupation+'%'.
	 * @param docChainName If null, ignore. If non-null, restrict results to
	 *            documents whose name (actually, docChainName) contains the
	 *            given string, i.e., match a SQL query of '%'+docChainName+'%'.
	 * @param meCheckBox If true, restrict results to documents that are waiting
	 *            for the currently logged-in user.
	 * @param sentStatus If "1", restrict results to documents that have already
	 *            been transferred. If "2", restrict results to documents that
	 *            have NOT yet been transferred. If null, or any other value, do
	 *            not restrict results based on the transfer status.
	 * @param project Restrict the results to documents for a specific Project.
	 *            If null, include documents from all projects. Only applicable
	 *            to Commercial productions; should be null for non-Commercial
	 *            productions.
	 * @param sortMap A mapping from a key (i.e., field) name to boolean true or
	 *            false. The existence of an entry indicates the query should be
	 *            sorted by the given field (key). If the boolean is true, the
	 *            sort is ascending; if false, then descending. If null, then no
	 *            sorting is done.
	 * @return A non-null, but possibly empty, List of ContactDocument that
	 *         match the given criteria.
	 */
	@SuppressWarnings("unchecked")
	public List<ContactDocument> findByFilter(String status, String departmentName, String contactName, String occupation, int pageSize, int first, String docChainName,
			boolean meCheckBox, String sentStatus, Boolean project, Map<String, Boolean> sortMap) {
		try {
			Criteria criteria = createFilterCritera(status, departmentName, contactName, occupation, docChainName, meCheckBox, sentStatus, project, sortMap);
			criteria.setFirstResult(first);
			criteria.setMaxResults(pageSize);
			List<ContactDocument> list = criteria.list();

			// To remove pool documents that are not for current contact.
			if (meCheckBox) {
				Contact currContact = SessionUtils.getCurrentContact();
				// map approval path id to boolean=(is contact in pool?)
				Map<Integer,Boolean> contactInPath = new HashMap<>();
				Iterator<ContactDocument> itr = list.iterator();
				Boolean inPool;
				while (itr.hasNext()) {
					ContactDocument contactDoc = itr.next();
					if (contactDoc.getApproverId() != null && contactDoc.getApproverId() < 0) {
						Set<Contact> contactPool = new HashSet<>();
						ApprovalPath path = null;
						//ApproverGroup Changes
						Integer approvalPathId = -(contactDoc.getApproverId());
						if (approvalPathId != null) {
							//Check for Department pool
							if (contactDoc.getIsDeptPool()) { // Department pool, Group Id
								log.debug("Department Pool");
								// Currently approvalPathId is Approver Group Id, because contact document is in dept pool.
								ApproverGroup approverGroup = ApproverGroupDAO.getInstance().findById(approvalPathId);
								if (approverGroup != null) {
									contactPool = approverGroup.getGroupApproverPool();
								}
								path = ContactDocumentDAO.getCurrentApprovalPath(contactDoc, null, null);
								if (path != null) {
									// Replace Approver group id with Approval Path Id.
									approvalPathId = path.getId();
								}
							}
							inPool = contactInPath.get(approvalPathId);
							if (inPool == null) { // haven't seen this path before, query database...
								if (! contactDoc.getIsDeptPool()) {
									// Production Pool
									path = ApprovalPathDAO.getInstance().findById(approvalPathId);
									contactPool = path.getApproverPool();
								}
								//ApprovalPath path = ApprovalPathDAO.getInstance().findById(approvalPathId);
								if (path != null) {
									if (contactPool != null && (! contactPool.contains(currContact))) {
										 itr.remove();
										 contactInPath.put(approvalPathId, false);
									}
									else {
										 contactInPath.put(approvalPathId, true);
									}
								}
							}
							else if (! inPool) { // found pool previously, & contact was not in pool
								itr.remove();
							}
						}
					}
				}
			}

			log.debug("Count=" + list.size() + ", String to be matched =" + departmentName +"-"+ contactName+"-"+"-"+ occupation+"--"+docChainName);
			return list;
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw new LoggedException();
		}
	}

	/**
	 * Method returns the count of the rows to be shown on the ace dataTable after the filtration
	 *
	 * @param status If "1", restrict results to documents that have a status of
	 *            either APPROVED or LOCKED. If "2", restrict results to
	 *            documents that are NOT either APPROVED or LOCKED. If null, or
	 *            any other value, do not restrict results based on the status.
	 * @param departmentValue If null, ignore. If non-null, restrict results to
	 *            documents for departments whose name contains the given
	 *            string, i.e., match a SQL query of '%'+departmentValue+'%'.
	 * @param contactName If null, ignore. If non-null, restrict results to
	 *            documents for people whose first OR last name contains the
	 *            given string, i.e., either name matches a SQL query of
	 *            '%'+contactName+'%'.
	 * @param occupation If null, ignore. If non-null, restrict results to
	 *            documents for occupations (Role.name) that contain the given
	 *            string, i.e., match a SQL query of '%'+occupation+'%'.
	 * @param docChainName If null, ignore. If non-null, restrict results to
	 *            documents whose name (actually, docChainName) contains the
	 *            given string, i.e., match a SQL query of '%'+docChainName+'%'.
	 * @param meCheckBox If true, restrict results to documents that are waiting
	 *            for the currently logged-in user.
	 * @param sentStatus If "1", restrict results to documents that have already
	 *            been transferred. If "2", restrict results to documents that
	 *            have NOT yet been transferred. If null, or any other value, do
	 *            not restrict results based on the transfer status.
	 * @param project Restrict the results to documents for a specific Project.
	 *            If null, include documents from all projects. Only applicable
	 *            to Commercial productions; should be null for non-Commercial
	 *            productions.
	 * @return A count of the number of ContactDocument that match the given criteria.
	 */
	public long findCountByFilter(String status, String departmentValue, String contactName, String occupation,
			String docChainName, boolean meCheckBox, String sentStatus, Boolean project) {
		long count = 0L;
		try {
			Criteria criteria = createFilterCritera(status, departmentValue, contactName, occupation, docChainName, meCheckBox, sentStatus, project, null);
			Projection pj = Projections.rowCount();
			criteria.setProjection(pj);
			@SuppressWarnings("unchecked")
			List<Long> list = criteria.list();
			if (list.size() > 0) {
				Object entry = list.get(0);
				if (entry instanceof Long) { // standard for count(x)
					count = (Long)entry;
				}
			}
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw new LoggedException();
		}
		log.debug("count=" + count);
		return count;
	}

	/**
	 * Method creates the criteria for the list of contact documents according
	 * to the parameters passed
	 *
	 * @param status If "1", restrict results to documents that have a status of
	 *            either APPROVED or LOCKED. If "2", restrict results to
	 *            documents that are NOT either APPROVED or LOCKED. If null, or
	 *            any other value, do not restrict results based on the status.
	 * @param departmentName If null, ignore. If non-null, restrict results to
	 *            documents for departments whose name contains the given
	 *            string, i.e., match a SQL query of '%'+departmentName+'%'.
	 * @param contactName If null, ignore. If non-null, restrict results to
	 *            documents for people whose first OR last name contains the
	 *            given string, i.e., either name matches a SQL query of
	 *            '%'+contactName+'%'.
	 * @param occupation If null, ignore. If non-null, restrict results to
	 *            documents for occupations (Role.name) that contain the given
	 *            string, i.e., match a SQL query of '%'+occupation+'%'.
	 * @param docChainName If null, ignore. If non-null, restrict results to
	 *            documents whose name (actually, docChainName) contains the
	 *            given string, i.e., match a SQL query of '%'+docChainName+'%'.
	 * @param meCheckBox If true, restrict results to documents that are waiting
	 *            for the currently logged-in user.
	 * @param sentStatus If "1", restrict results to documents that have already
	 *            been transferred. If "2", restrict results to documents that
	 *            have NOT yet been transferred. If null, or any other value, do
	 *            not restrict results based on the transfer status.
	 * @param project Restrict the results to documents for a specific Project.
	 *            If null, include documents from all projects. Only applicable
	 *            to Commercial productions; should be null for non-Commercial
	 *            productions.
	 * @param sortMap A mapping from a key (i.e., field) name to boolean true or
	 *            false. The existence of an entry indicates the query should be
	 *            sorted by the given field (key). If the boolean is true, the
	 *            sort is ascending; if false, then descending. If null, then no
	 *            sorting is done.
	 * @return A Criteria object to be used with a named query that encompasses
	 *         all the supplied parameters as described above.
	 */
	private Criteria createFilterCritera(String status, String departmentName, String contactName, String occupation,
					String docChainName, boolean meCheckBox, String sentStatus, Boolean project, Map<String, Boolean> sortMap) {
		Criteria criteria = getHibernateSession().createCriteria(ContactDocument.class, "contactDoc");
		criteria.createAlias("contactDoc.contact", "contact");
		criteria.createAlias("contactDoc.employment", "employment");
		criteria.createAlias("employment.role", "role");
		criteria.createAlias("contact.user", "user");
		criteria.createAlias("role.department", "department");
		criteria.createAlias("contactDoc.documentChain", "documentChain");

		criteria.add(Restrictions.like("contact.production", SessionUtils.getProduction()));
		if (project != null && ! project) {
			criteria.add(Restrictions.like("contactDoc.project", SessionUtils.getCurrentProject()));
		}
		// Add default sort for name
		if (sortMap != null && sortMap.isEmpty()) {
			sortMap.put("name", true);
		}

		// Exclude CD belonging to users with "admin" roles:
		criteria.add(Restrictions.not(Restrictions.in("role.id", adminRoleIdList)));

		if (status != null) {
			if (status.equals("1")) {
				criteria.add(Restrictions.or(
						Restrictions.like("contactDoc.status", ApprovalStatus.APPROVED),
						Restrictions.like("contactDoc.status", ApprovalStatus.LOCKED)));
				//criteria.add(Restrictions.like("contactDoc.status", ApprovalStatus.APPROVED));
			}
			else if (status.equals("2")) {
				criteria.add(Restrictions.not( Restrictions.or(
						Restrictions.like("contactDoc.status", ApprovalStatus.APPROVED),
						Restrictions.like("contactDoc.status", ApprovalStatus.LOCKED))));
			}
		}
		if (departmentName != null) {
			departmentName = "%"+departmentName+"%";
			criteria.add(Restrictions.like("department.name", departmentName));
//			criteria.addOrder(Order.asc("department.name"));
		}
		if (contactName != null) {
			contactName = "%"+contactName+"%";
			//criteria.createAlias("contact.user", "user");
			criteria.add(Restrictions.or(
					Restrictions.like("user.firstName", contactName),
					Restrictions.like("user.lastName", contactName)));
//			criteria.addOrder(Order.asc("user.lastName"));
//			criteria.addOrder(Order.asc("user.firstName"));
		}
		if (occupation != null) {
			occupation = "%"+occupation+"%";
			criteria.add(Restrictions.like("role.name", occupation));
			criteria.addOrder(Order.asc("role.name"));
		}
		if (docChainName != null) {
			docChainName = "%"+docChainName+"%";
			criteria.add(Restrictions.like("documentChain.name", docChainName));
//			criteria.addOrder(Order.asc("documentChain.name"));
		}
		if (meCheckBox) {
			List<Approver> approverIdList = ApproverDAO.getInstance().
					findByNamedQuery(Approver.GET_APPROVER_IDS_BY_CONTACT, map("contact", SessionUtils.getCurrentContact()));
			if (approverIdList != null) {
				// To add all the pool documents also.
				criteria.add(Restrictions.or(
						(Restrictions.in("contactDoc.approverId", approverIdList)),
						(Restrictions.lt("contactDoc.approverId", 0))));
				//criteria.add(Restrictions.in("contactDoc.approverId", approverIdList));
			}
		}
		if (sentStatus != null) {
			if (sentStatus.equals("1")) {
				criteria.add(Restrictions.isNotNull("contactDoc.timeSent"));
			}
			else if (sentStatus.equals("2")) {
				criteria.add(Restrictions.isNull("contactDoc.timeSent"));
			}
		}
		if (sortMap != null && (! sortMap.isEmpty())) {
			for (String sortKey : sortMap.keySet()) {
				if (sortKey.equals("department")) {
					if (sortMap.get(sortKey)) {
						criteria.addOrder(Order.asc("department.name"));
					}
					else {
						criteria.addOrder(Order.desc("department.name"));
					}
				}
				else if (sortKey.equals("name")) {
					if (sortMap.get(sortKey)) {
						criteria.addOrder(Order.asc("user.lastName"));
						criteria.addOrder(Order.asc("user.firstName"));
					}
					else {
						criteria.addOrder(Order.desc("user.lastName"));
						criteria.addOrder(Order.desc("user.firstName"));
					}
				}
				else if (sortKey.equals("occupation")) {
					if (sortMap.get(sortKey)) {
						criteria.addOrder(Order.asc("role.name"));
					}
					else {
						criteria.addOrder(Order.desc("role.name"));
					}
				}
				else if (sortKey.equals("document")) {
					if (sortMap.get(sortKey)) {
						criteria.addOrder(Order.asc("documentChain.name"));
					}
					else {
						criteria.addOrder(Order.desc("documentChain.name"));
					}
				}
				else if (sortKey.equals(ContactDocument.SORTKEY_STATUS)) {
					if (sortMap.get(sortKey)) {
						criteria.addOrder(Order.asc("status"));
					}
					else {
						criteria.addOrder(Order.desc("status"));
					}
				}
			}
		}
		return criteria;
	}

	/**
	 * Get approved- and unapproved-document counts for all the Employment
	 * records associated with the current Production.
	 *
	 * @return A (possibly empty) List of EmpDocInfo objects; each one has either
	 *       the approved-document count (if EmpDocInfo.approved>0) or the
	 *         un-approved document count (if EmpDocInfo.approved<=0) for a
	 *         specific Employment instance identified by its database id
	 *         (EmpDocInfo.empId = Employee.id). Note that if an Employment
	 *         instance has either no approved documents or no un-approved
	 *         documents, the corresponding entry will not appear at all in the
	 *         returned list. That is, all entries will have EmpDocInfo.count > 0.
	 */
	@SuppressWarnings("unchecked")
	public List<EmpDocInfo> findEmploymentDocStatus() {

		String queryString = "select employment_id as empId, " +
				" case cd.status " +
				"  when 'APPROVED' then 1 " +
				"  when 'LOCKED' then 1 " +
				"  else 0 end as approved, " +
				" count(*) " +
				"from contact_document cd, contact c " + // Changed to lower case because of case sensitivity in ubuntu.
				"where c.id = cd.contact_id and c.production_id = :prodId " +
				" and cd.employment_id is not null and cd.status <> 'VOID' " +
				"group by empId, approved";

		List<EmpDocInfo> empList = new ArrayList<>();
		Integer prodId = SessionUtils.getProductionId();
		if (prodId == null) { // unusual - session logged out then did render?
			return empList;
		}
		Query query = getHibernateSession().createSQLQuery(queryString)
				.setInteger("prodId", prodId);
		log.debug("");
		List<Object[]> list = query.list();
		log.debug("count=" + list.size());

		for (Object[] row: list) {
			EmpDocInfo e = new EmpDocInfo((Integer)row[0], (Integer)row[1], (Number)row[2]);
			empList.add(e);
			//log.debug(e);
		}

		return empList;
	}

	/**
	 * Mark the given ContactDocument approved, and set the Approver.id value of
	 * the next approver in the chain. This will be null if the ContactDocument
	 * has received its final approval. An approval event is added to the
	 * history of the ContactDocument. The updated document is saved.
	 *
	 * @param cd The ContactDocument to be updated.
	 * @param approverId If not null, the Contact.id value of the person
	 *            who should be the next approver for this ContactDocument. If
	 *            null, this method determines the next approver based on the
	 *            current approval chain for the given ContactDocument.
	 * @return A (possibly refreshed) reference to the ContactDocument object.
	 */
	@Transactional
	public ContactDocument approve(ContactDocument cd, Integer approverId) {
		cd = (ContactDocument)super.approve(cd, approverId, true);
		if (cd != null && cd.getStatus() == ApprovalStatus.APPROVED
				&& cd.getFormType() == PayrollFormType.START) {
			// Update employment after startForm is approved
			Employment emp = cd.getEmployment();
			if (emp != null && cd.getRelatedFormId() != null) {
				StartForm sf = StartFormDAO.getInstance().findById(cd.getRelatedFormId());
				emp.setOffProduction(sf.getOffProduction());
				if (emp.getAccount() != null) {
					emp.getAccount().copyFrom(sf.getAccount());
				}
				emp.setEndDate(sf.getWorkEndDate());
				emp.setAdditionalStaff(sf.getAdditionalStaff());
				attachDirty(emp);
			}
		}
		return cd;
	}

	/**
	 * Mark the given ContactDocument approved, and set the Approver.id value of
	 * the next approver in the chain. This will be null if the ContactDocument
	 * has received its final approval. An approval event is added to the
	 * history of the ContactDocument. The updated document is saved.
	 *
	 * @param cd The ContactDocument to be updated.
	 * @param approverId If not null, the Contact.id value of the person
	 *            who should be the next approver for this ContactDocument. If
	 *            null, this method determines the next approver based on the
	 *            current approval chain for the given ContactDocument.
	 * @param toFinalApprover
	 */
/*	private void approveSub(ContactDocument cd, Integer approverId) {
		Contact contact = null; // Contact of new approver
		Integer approvalPathId = null;
		// Determine next approver
		if (cd.getApproverId() < 0) {
			approvalPathId = -(cd.getApproverId());
		}
		else {
			approvalPathId = getCurrentApprovalPathId(cd, approvalPathId);
		}
		Approver nextApprover = TimecardUtils.findNextApprover(cd, approvalPathId);
		if (approverId == null) { // use normal chain
			if (nextApprover != null) { // not at end of chain
				if (nextApprover.getId() < 0) { // negative id means dummy approver and throw that in the pool
					log.debug(">>>>>>>>>>>>>>>>>>>>>>>> "+nextApprover.getId());
					approverId = nextApprover.getId();
				}
				else {
					approverId = nextApprover.getId(); // this is Approver.id of next one
					contact = nextApprover.getContact();
				}
			}
		}
		else if (nextApprover != null && nextApprover.getContact() != null &&
				nextApprover.getContact().getId().equals(approverId)) {
			approverId = nextApprover.getId(); // use next one in chain
			contact = nextApprover.getContact();
		}
		else { // out-of-line approval: create new Approver instance
			contact = ContactDAO.getInstance().findById(approverId);
			Approver app = new Approver(contact, nextApprover, false);
			save(app);
			approverId = app.getId();
		}

		// NOW approverId is an Approver.id value!
		if (approverId == null) { // no more approvers in the chain ... has final approval!
			cd.setStatus(ApprovalStatus.APPROVED);
		}
		else if (cd.getStatus() == ApprovalStatus.REJECTED ||
				cd.getStatus() == ApprovalStatus.RECALLED) {
			cd.setStatus(ApprovalStatus.RESUBMITTED);
		}
		Integer priorApproverId = cd.getApproverId();
		cd.setApproverId(approverId);

		// Create event
		ContactDocEventDAO.getInstance().createEvent(cd, TimedEventType.APPROVE);
		//cd.setMarkedForApproval(false);
		attachDirty(cd);

		// If the prior Approver was "out of line" (un-shared), then delete it.
		if (priorApproverId != null) {
			Approver app = ApproverDAO.getInstance().findById(priorApproverId);
			if (app != null && ! app.getShared()) {
				log.debug("deleting non-shared Approver, id=" + app.getId());
				delete(app);
			}
		}

		if (approverId != null && approverId > 0) {
			notifyReady(cd);
		}
		//WeeklyBatchUtils.updateBatchStatus(cd);
		log.info("TC approved, id=" + cd.getId() + ", next appr=" + approverId);
	}
*/

	@Override
	protected void createEvent(Approvable doc, Contact contact, Integer priorApproverId,
			byte outOfLine) {
		ContactDocEventDAO.getInstance().createEvent((ContactDocument)doc, TimedEventType.APPROVE);
	}

	/**
	 * @param doc
	 */
	@Override
	protected void notifyReady(Approvable doc) {
		//ContactDocument cd = (ContactDocument)doc;
		//notifyReady(cd, cd.getContact());
	}

	/**
	 * @param cd
	 * @param approvalPathId
	 * @return
	 */
	@Override
	protected Integer findCurrentApprovalPathId(Approvable cd) {
		return getCurrentApprovalPathId((ContactDocument)cd);
	}

	/**
	 * Utility method to find the database id of the ApprovalPath of a given
	 * ContactDocument.
	 *
	 * @param cd The ContactDocument of interest.
	 * @return The id of the path used to approve the given ContactDocument.
	 */
	public static Integer getCurrentApprovalPathId(ContactDocument cd) {
		ApprovalPath path = getCurrentApprovalPath(cd, null, null);
		if (path == null) {
			return null;
		}
		return path.getId();
	}

	/**
	 * Utility method to find the approval path of a given ContactDocument.
	 *
	 * @param cd The ContactDocument of interest.
	 * @return The ApprovalPath path used to approve the given ContactDocument;
	 *         returns null if no such path exists.
	 */
	public static ApprovalPath getCurrentApprovalPath(ContactDocument cd, DocumentChain docChain, Project project) {
		ApprovalPath approvalPath = null;
		try {
			Project cdProject = null;
			DocumentChain documentChain = null;
			if (cd != null) {
				cdProject = cd.getProject();
				documentChain = cd.getDocumentChain();
			}
			else if (docChain != null) {
				cdProject = project;
				documentChain = docChain;
			}
			documentChain = DocumentChainDAO.getInstance().refresh(documentChain);
			if (documentChain != null && documentChain.getApprovalPath() != null) {
				Set<ApprovalPath> pathSet = documentChain.getApprovalPath();
				Production prod = SessionUtils.getNonSystemProduction();
				if (prod == null) { // Get 'Viewed' production in My Starts environment
					Integer id = SessionUtils.getInteger(Constants.ATTR_VIEW_PRODUCTION_ID);
					if (id != null) {
						prod = ProductionDAO.getInstance().findById(id);
					}
				}
				if (prod != null && prod.getType().isAicp()) {
					Map<String, Object> values = new HashMap<>();
					values.put("project", cdProject);
					List<ApprovalPath> pathListOfProject = ApprovalPathDAO.getInstance().
							findByNamedQuery(ApprovalPath.GET_APPROVAL_PATH_BY_PROJECT, values);
					Iterator<ApprovalPath> itr = pathSet.iterator();
					while (itr.hasNext()) {
						ApprovalPath path = itr.next();
						for (ApprovalPath pathToCompare : pathListOfProject) {
							if (pathToCompare.equals(path)) {
								approvalPath = pathToCompare;
								break;
							}
						}
					}
				}
				else {
					Iterator<ApprovalPath> itr = pathSet.iterator();
					if (itr.hasNext()) {
						approvalPath = itr.next();
					}
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return approvalPath;
	}

	/**
	 * Issue a notification (if appropriate) to the next approver of the
	 * document.
	 *
	 * @param appr The approvable whose approver should be notified.
	 * @param employee The employee whose document we are processing.
	 */
	/*private void notifyReady(Approvable appr, Contact employee) {
		Integer appId = appr.getApproverId();
		if (appId != null) {
			Approver app = ApproverDAO.getInstance().findById(appId);
			if (app != null && app.getContact() != null) {
				Contact c = app.getContact();
				if (! c.equals(SessionUtils.getCurrentContact())) {
					DoNotification.getInstance().documentReady(employee, c);
				}
			}
		}
	}
*/
	/**
	 * Reject a single ContactDocument. A Reject event is added to the document's
	 * history; the document's status is changed to REJECTED; the document's
	 * "next approver" is changed to refer to the Contact specified; and
	 * notifications are sent to the recipient of the rejected document and any
	 * approvers who will need to re-approve the document.
	 *
	 * @param cd The ContactDocument to be rejected.
	 * @param rejector The Contact of the person doing the Reject operation.
	 * @param sendToId The Contact.id value of the Contact who should be the
	 *            "next approver" for the document. If null, the document is set
	 *            to have no "next approver", which means that it is in the
	 *            employee's possession, waiting to be (re)submitted.
	 * @param approverId The Approver.id value of the Approver which will point
	 *            to the "next approver" for the document. May be null.
	 * @param comment The reason for the rejection. May be null. If null or
	 *            blank, no change is made to the document's Comment field.
	 * @return A refreshed reference for the ContactDocument, or null if an
	 *         Exception is thrown during processing.
	 */
	@Transactional
	public ContactDocument reject(ContactDocument cd, Contact rejector, Integer sendToId,
			Integer approverId, String comment) {
		try {
			cd = refresh(cd);
			// Add the comment text to the document's Comment field.
			addComment(cd, comment);
			// and update document...
			cd.setStatus(ApprovalStatus.REJECTED);
			Integer oldApproverId = cd.getApproverId();
			ApprovalPath approvalPath = null;
			approvalPath = getCurrentApprovalPath(cd, null, null);
			if (oldApproverId != null && oldApproverId < 0 && approvalPath != null &&
					cd.getIsDeptPool() && (! oldApproverId.equals((-1)*(approvalPath.getId())))) {
				log.debug("");
				cd.setIsDeptPool(false);
			}

			//ApproverGroup Changes
			//New change.
			if (approvalPath != null && approvalPath.getUsePool() &&
					approvalPath.getUseFinalApprover() && oldApproverId != null &&
					oldApproverId.equals(approvalPath.getFinalApprover().getContact().getId()) &&
					approvalPath.getApproverPool() != null) {
				for (Contact con : approvalPath.getApproverPool()) {
					if (con.getId().equals(sendToId)) {
						approverId = (-1)*(approvalPath.getId());
					}
				}
			}
			log.debug("approverId = " + approverId);
			Contact sendTo = changeApprover(cd, sendToId, approverId, TimedEventType.REJECT, approvalPath);
			// Send notification to recipient & other approvers;
			// 'sendTo' is null if going to employee - buildReject understands this!
			Collection<Contact> approvers = TimecardUtils.createRejectNotify(cd, sendTo, oldApproverId, approvalPath);
			if (sendTo == null) { // target is employee; get Contact value for notification
				sendTo = cd.getContact();
			}
			approvers.add(sendTo);
			String documentName = null;
			if (cd.getFormType() != null) {
				documentName = cd.getFormType().getName();
				if (cd.getFormType() == PayrollFormType.OTHER && cd.getDocument() != null) {
					documentName = cd.getDocument().getName();
				}
			}
			DoNotification no = DoNotification.getInstance();
			no.documentRejected(cd.getContact().getUser().getFirstName(), cd.getContact().getUser().getLastName(),
					rejector, sendTo, approvers, comment, documentName);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			cd = null;
		}

		return cd;
	}

	/** Method used to call the method of ApprovableDAO in ApprovalPathBean.
	 * @param app First Approver.
	 * @param contactId contact id to search for.
	 * @return The matching Approver object if found; null if the end of the chain is reached without a match.
	 */
	public Approver findApproverFromContact(Approver app, Integer contactId) {
		return searchChainContact(app, contactId);
	}

	/** Method to find the number of contact documents with Pending status for an employment.
	 * @param proj
	 * @param id, Employment id for which count will be calculated.
	 * @return Number of documents.
	 */
	public Integer findDocCountOfPendingStatus(Integer id, Project proj) {
		Map<String, Object> values = new HashMap<>();
		values.put("employmentId", id);
		if (proj != null) {
			values.put("project", proj);
			return findCountByNamedQuery(ContactDocument.GET_DOC_COUNT_OF_PENDING_STATUS_BY_EMPLOYMENT_PROJECT, values).intValue();
		}
		else {
			return findCountByNamedQuery(ContactDocument.GET_DOC_COUNT_OF_PENDING_STATUS_BY_EMPLOYMENT, values).intValue();
		}
	}

	/** Method to find the number of contact documents with Void status for an employment.
	 * @param proj
	 * @param id, Employment id for which count will be calculated.
	 * @return Number of documents.
	 */
	public Integer findDocCountOfVoidStatus(Integer id, Project proj) {
		Map<String, Object> values = new HashMap<>();
		values.put("employmentId", id);
		if (proj != null) {
			values.put("project", proj);
			return findCountByNamedQuery(ContactDocument.GET_DOC_COUNT_OF_VOID_STATUS_BY_EMPLOYMENT_PROJECT, values).intValue();
		}
		else {
			return findCountByNamedQuery(ContactDocument.GET_DOC_COUNT_OF_VOID_STATUS_BY_EMPLOYMENT, values).intValue();
		}
	}

	/**
	 * Void a single Document. A Void event is added to the document's history; the document's
	 * status is changed to VOID; the document's "next approver" is set to null; and notifications
	 * are sent to everyone on the approval chain for the document, and the employee.
	 *
	 * @param cd The contactDocument to be voided.
	 * @param comment The reason for the void. May be null. If null or blank, no change is made to
	 *            the document's Comment field.
	 * @return A refreshed reference for the ContactDocument, or null if an Exception is thrown
	 *         during processing.
	 */
	@Transactional
	public ContactDocument voidStatus(ContactDocument cd, String comment) {
		try {
			cd = refresh(cd);
			// Add the comment text to the document's Comment field.
			addComment(cd, comment);
			// Create event
			ContactDocEventDAO.getInstance().createEvent(cd, TimedEventType.VOID);
			cd.setStatus(ApprovalStatus.VOID);
			cd.setApproverId(null);
			cd.setApproverContactId(null);
			cd.setLockedBy(null);
			cd.setLastUpdated(new Date());
			attachDirty(cd);

			// Send notification to approvers & employee.
			// The 'sendTo' (2nd param) and 'oldApproverId' (3rd param) are null
			// so list includes all approvers.
			Integer approvalPathId = getCurrentApprovalPathId(cd);
			ApprovalPath path = ApprovalPathDAO.getInstance().findById(approvalPathId);
			Collection<Contact> approvers = TimecardUtils.createRejectNotify(cd, null, null, path);
			// For logs only
			for (Contact c : approvers) {
				log.debug("Contact = " + c.getDisplayName());
			}
			//Production prod = TimecardUtils.findProduction(cd);
			// Get contact for employee
			Contact sendTo = cd.getContact();
			approvers.add(sendTo); // add employee to notification list
			Contact doer = SessionUtils.getCurrentContact(); // Get contact for person doing the Void
			DoNotification no = DoNotification.getInstance();
			User user = cd.getContact().getUser();
			String docName = cd.getFormType().getName();
			if (cd.getFormType() == PayrollFormType.OTHER && cd.getDocument() != null) {
				docName = cd.getDocument().getNormalizedName();
			}
			no.documentVoided(user.getFirstName(), user.getLastName(), doer, approvers, comment, docName);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			cd = null;
		}
		return cd;
	}

	/**
	 * Add the given comment to the document's Comment field, along with the
	 * user's name, and the current date & time, with appropriate HTML
	 * formatting codes included.
	 *
	 * @param cd The ContactDocument to be updated.
	 * @param comment The comment to be added; if this is null, or an empty or
	 *            all-blank String, the document is not changed.
	 */
	public static void addComment(ContactDocument cd, String comment) {
		comment = TimecardUtils.formatComment(comment);
		if (comment != null) {
			if (cd.getComments() != null) {
				comment = cd.getComments() + comment;
			}
			cd.setComments(comment);
		}
	}

	/**
	 * Recall a single ContactDocument. A Recall event is added to the document's history; the
	 * document's status is changed to RECALLED; the document's "next approver" is changed to refer
	 * to the specified Contact; and notifications are sent to any approvers who will need to
	 * re-approve the document (not including the person doing the recall).
	 *
	 * @param cd The ContactDocument to be recalled.
	 * @param recallerId The Contact.id value of the Contact who is doing the recall, and who should
	 *            be made the "next approver" for the document.
	 * @return A refreshed reference for the ContactDocument, or null if there is an Exception during
	 *         processing.
	 */
	@Transactional
	public ContactDocument recall(ContactDocument cd, Integer recallerId) {
		try {
			cd = refresh(cd);
			cd.setStatus(ApprovalStatus.RECALLED);
			cd.setLockedBy(null);
			Integer oldApproverId = cd.getApproverId();
			ApprovalPath approvalPath = getCurrentApprovalPath(cd, null, null);
			log.debug("Old Approver Id = " + oldApproverId );
			if (oldApproverId != null && oldApproverId < 0 && approvalPath != null &&
					cd.getIsDeptPool() && (! oldApproverId.equals((-1)*(approvalPath.getId())))) {
				log.debug("");
				cd.setIsDeptPool(false);
			}

			//ApproverGroup Changes
			//New change.
			Integer recallerApproverId = null;
			if (approvalPath != null && approvalPath.getUsePool() &&
					approvalPath.getUseFinalApprover() && oldApproverId != null &&
					oldApproverId.equals(approvalPath.getFinalApprover().getContact().getId()) &&
					approvalPath.getApproverPool() != null) {
				for (Contact con : approvalPath.getApproverPool()) {
					if (con.getId().equals(recallerId)) {
						recallerApproverId = (-1)*(approvalPath.getId());
					}
				}
			}
			// Do the recall, even if no old approver id
			Contact recaller = changeApprover(cd, recallerId, recallerApproverId, TimedEventType.RECALL, approvalPath);
			if (oldApproverId != null) {
				// Send notification to other approvers
				List<Contact> approvers;
				if (recaller == null) { // employee did "recall"
					//Production prod = TimecardUtils.findProduction(cd);
					recaller = SessionUtils.getCurrentContact();
					//recaller = ContactDAO.getInstance().findByAccountNumAndProduction(wtc.getUserAccount(), prod);
					approvers = new ArrayList<>(); // only one approver to notify -
													// the current one:
					if (oldApproverId < 0 && approvalPath != null) {
						for (Contact con : approvalPath.getApproverPool()) {
							approvers.add(con);
						}
					}
					else {
						Approver oldApprover = ApproverDAO.getInstance().findById(oldApproverId);
						if (oldApprover != null) {
							approvers.add(oldApprover.getContact());
						}
					}
				}
				else {
					approvers = TimecardUtils.createRecallNotify(cd, recaller, oldApproverId, approvalPath);
				}
				if (approvers.size() > 0) {
					Project proj = SessionUtils.getCurrentProject();
					if (proj == null) {
						Production prod = TimecardUtils.findProduction(cd);
						proj = TimecardUtils.findProject(prod, cd); // project if
																		// Commercial
						if (proj == null) {
							proj = prod.getDefaultProject(); // project if
																// TV/Feature
						}
					}
					DoNotification no = DoNotification.getInstance();
					User user = cd.getContact().getUser();
					String docName = cd.getFormType().getName();
					if (cd.getFormType() == PayrollFormType.OTHER && cd.getDocument() != null) {
						docName = cd.getDocument().getNormalizedName();
					}
					no.documentRecalled(proj, user.getFirstName(),  user.getLastName(), docName, recaller, approvers);
				}
			} else { // special case for model release since with the new flow approverId is always NULL for Model release
				if (cd.getFormType() == PayrollFormType.MODEL_RELEASE) {
					log.debug("For model release notifying employee about the recall action");
					List<Contact> approvers = new ArrayList<Contact>();
					User user = cd.getContact().getUser();
					String docName = cd.getFormType().getName();
					approvers.add(cd.getEmployment().getContact());
					DoNotification.getInstance().documentRecalled(SessionUtils.getCurrentProject(), user.getFirstName(),
							user.getLastName(), docName, cd.getContact(), approvers);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
		return cd;
	}

	/**
	 * Change the current approver for the given document to reference the Contact identified by
	 * 'sendToId', a Contact.id value.  A ContactDocEvent is also added to record the change.
	 * The updated ContactDocument is persisted in the database.
	 *
	 * @param cd The ContactDocument to be updated.
	 * @param sendToId The Contact.id value of the person to be made the approver for the document.
	 *            If this is null, the contact document is being returned to the employee.
	 * @param approverId The Approver.id value of the Approver object that references the Contact
	 *            matching sendToId. If null, use the first (lowest) approver in the hierarchy that
	 *            matches the given sendToId. This parameter is used by the Reject and Pull
	 *            functions so that if the same Contact appears multiple times in the approval
	 *            hierarchy (e.g., as both a departmental and production approver), the rejector can
	 *            select the appropriate one in the list and we will return the document to the
	 *            appropriate point in the approval chain, not just to the first occurrence of the
	 *            given Contact.
	 * @param eventType The type of TimedEventType to create for this action.
	 * @param approvalPath The approval path of the contact document.
	 * @return The Contact object corresponding to the sendToId parameter.
	 */
	private Contact changeApprover(ContactDocument cd, Integer sendToId, Integer approverId,
			TimedEventType eventType, ApprovalPath approvalPath) {
		Contact sendTo = null;
		//byte outOfLine = 0;
		Integer priorApproverId = cd.getApproverId(); // save existing
														// Approver.id
		if (sendToId == null) { // return to employee
			cd.setApproverId(null);
		}
		else {
			Production prod = TimecardUtils.findProduction(cd);
			Project project = TimecardUtils.findProject(prod, cd);
			Approver firstApp = ApproverUtils.findFirstApprover(prod, project, cd.getEmployment().getDepartment(), approvalPath.getId());
			Approver app = firstApp;
			//Contact poolContact = null; // If a pool contact recalls document from final approver.
			//ApproverGroup Changes
			Contact sendToContact = ContactDAO.getInstance().findById(sendToId);
			//For Department Pool's recall/reject case
			if (firstApp != null && firstApp.getId() < 0) {
				Integer groupId = -(firstApp.getId());
				ApproverGroup group = ApproverGroupDAO.getInstance().findById(groupId);
				if (group != null && group.getGroupApproverPool() != null && group.getGroupApproverPool().contains(sendToContact)) {
					sendTo = sendToContact;
					//app.setContact(sendToContact);
					cd.setApproverId((-1)*(group.getId()));
					cd.setIsDeptPool(true);
				}
			}
			if (approverId != null && approverId > 0) {
				/*if (app == null) {
					app = searchChain(approvalPath.getApprover(), approverId);
				}*/
				// first search the approval chain using the approverId value
				app = searchChain(app, approverId);
				if (app == null) {
					app = searchChain(approvalPath.getApprover(), approverId);
				}
			}
			if (approverId != null && approverId < 0 && approvalPath.getUsePool() && approvalPath.getApproverPool() != null &&
					approverId.equals((-1)*(approvalPath.getId()))) {
				if (approvalPath.getApproverPool().contains(sendToContact)) {
					sendTo = sendToContact;
					cd.setApproverId((-1)*(approvalPath.getId()));
				}
			}
			if (app == null || approverId == null) {
				// No approverId given, or not found by approverId; look by
				// contact id.
				// ApproverId will be null if the target is an out-of-line
				// approver; if not null,
				// the above search should only fail if someone modified the
				// approval hierarchy while the Reject
				// was in progress (after user got Reject pop-up, but before
				// they clicked Ok).

				// Search by contact Id.
				app = searchChainContact(firstApp, sendToId);
				if (app == null) {
					app = searchChainContact(approvalPath.getApprover(), sendToId);
				}
				if (app == null && approvalPath.getUsePool()) {
					//Contact sendToContact = ContactDAO.getInstance().findById(sendToId);
					// Pull case (Final approver pulls)
					if (approvalPath.getUseFinalApprover() && sendToId.equals(approvalPath.getFinalApprover().getContact().getId())) {
						app = approvalPath.getFinalApprover();
					}
					// If a contact from pool recalls document from final approver
					/*else if (approvalPath.getUseFinalApprover() && priorApproverId != null &&
								priorApproverId.equals(approvalPath.getFinalApprover().getContact().getId())) {
						if (approvalPath.getApproverPool().contains(sendToContact)) {
							poolContact = sendToContact;
						}
					}
					// If pool contact pulls
					else if (approvalPath.getApprover() == null) {
						if (approvalPath.getApproverPool().contains(sendToContact)) {
							poolContact = sendToContact;
						}
					}*/
				}
			}
			/*if (app == null && approvalPath.getUsePool() && poolContact != null) {
				sendTo = poolContact;
				cd.setApproverId((-1) * approvalPath.getId());
			}*/
			if (app != null && app.getId() > 0) {
				sendTo = app.getContact();
				cd.setApproverId(app.getId());
			}
		}
		if (cd.getApproverId() != null) {
			ApprovalLevel approvalLevel = ApproverUtils.isProductionOrDeptApprover(approverId, approvalPath.getId());
			if (approvalLevel != null) {
				cd.setApprovalLevel(approvalLevel);
			}
		}
		ContactDocEventDAO.getInstance().createEvent(cd, eventType);
		attachDirty(cd);
		// If the prior Approver was "out of line" (un-shared), then delete it.
		if (priorApproverId != null) {
			Approver app = ApproverDAO.getInstance().findById(priorApproverId);
			if (app != null && ! app.getShared()) {
				log.debug("deleting non-shared Approver, id=" + app.getId());
				delete(app);
			}
		}
		return sendTo;
	}

	/**
	 * Pull a single ContactDocument -- that is, move it UP in the approval queue, bypassing the
	 * current approver, and (possibly) others in the chain up to the "puller". A Pull event is
	 * added to the document's history; the document's status remains unchanged; the document's
	 * "next approver" is changed to refer to the specified Contact; and notifications are sent to
	 * those approvers who were bypassed.
	 *
	 * @param cd The ContactDocument to be recalled.
	 * @param pullerId The Contact.id value of the Contact who is doing the pull, and who should be
	 *            made the "next approver" for the document.
	 * @return A refreshed reference for the ContactDocument, or null if there is an Exception during
	 *         processing.
	 */
	@Transactional
	public ContactDocument pull(ContactDocument cd, Integer pullerId) {
		try {
			log.debug("");
			cd = refresh(cd);
			cd.setLockedBy(null);
			// Save existing approver id
			Integer oldApproverId = cd.getApproverId();
			Integer pullerApproverId = null;
			ApprovalPath approvalPath = getCurrentApprovalPath(cd, null, null);
			if (approvalPath != null) {
				// Set the dept pool false if previously it was true.
				if (oldApproverId != null && oldApproverId < 0 &&
						approvalPath != null &&  cd.getIsDeptPool() &&
						(! oldApproverId.equals((-1)*approvalPath.getId()))) {
					log.debug("");
					cd.setIsDeptPool(false);
				}

				// Find the Approver entry for the given Contact (pullerId).
				// we want the "latest" one, so check the production approver chain
				// first.
				Production prod = TimecardUtils.findProduction(cd);
				Approver app = searchChainContact(approvalPath.getApprover(), pullerId);
				if (app != null) {
					pullerApproverId = app.getId();
				}

				//ApproverGroup Changes
				//New change.
				else if (app == null && approvalPath.getUsePool() && approvalPath.getApproverPool() != null) {
					for (Contact con : approvalPath.getApproverPool()) {
						if (con.getId().equals(pullerId)) {
							pullerApproverId = (-1)*(approvalPath.getId());
						}
					}
				}
				log.debug("Puller Approver Id = " + pullerApproverId);
				// If approver not found yet, changeApprover will find the department approver anyway,
				// no need for us to search for it.

				Contact puller = changeApprover(cd, pullerId, pullerApproverId, TimedEventType.PULL, approvalPath);
				// Send notification to other approvers
				if (puller == null) { // shouldn't happen (employee can't do "pull")
					puller = cd.getContact();
				}
				log.debug("Puller = " + puller.getDisplayName());
				Project tcProj = TimecardUtils.findProject(prod, cd);
				List<Contact> approvers = ApproverUtils.createPullNotify(puller, tcProj, oldApproverId, approvalPath);
				if (approvers.size() > 0) {
					app = ApproverDAO.getInstance().findById(oldApproverId);
					String approverName = app.getContact().getUser().getFirstNameLastName();
					DoNotification no = DoNotification.getInstance();
					User user = cd.getContact().getUser();
					String docName = cd.getFormType().getName();
					if (cd.getFormType() == PayrollFormType.OTHER && cd.getDocument() != null) {
						docName = cd.getDocument().getNormalizedName();
					}
					no.documentPulled(user.getFirstName(), user.getLastName(), docName, puller, approverName, approvers);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			cd = null;
		}
		return cd;
	}

	/**
	 * Determine if there are any StartForm`s for the given Employment which do
	 * NOT have a VOID status.
	 *
	 * @param emp The Employment of interest.
	 * @return True iff at least one StartForm with a non-VOID status exists
	 *         which is related to the given Employment.
	 */
	public boolean existsNonVoidByEmployment(Employment emp) {
		return findCountByNamedQuery(ContactDocument.GET_COUNT_START_FORM_NON_VOID_BY_EMPLOYMENT, map("employment", emp)) > 0;
	}

	/**
	 * Unlock all ContactDocument's that are currently locked. Called during application
	 * startup, to clear any locks leftover from a prior execution, in case of a
	 * "hard" crash in which our normal methods did not clear the locks during
	 * shutdown (e.g., via ContactFormBean.dispose()).
	 */
	@SuppressWarnings("unchecked")
	@Transactional
	public void unlockAllLocked() {
		try {
			String query = "from ContactDocument where lockedBy is not null";
			List<ContactDocument> locked = find(query);
			for (ContactDocument cd : locked) {
				unlock(cd, null);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/** Returns the list of distributed form types in the Production.
	 * @param prod
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<PayrollFormType> findProductionFormTypes(Production prod, Project project) {
		String query = "select distinct cd.formType from Production p, ContactDocument cd, Contact c where cd.contact.id = c.id " +
				" and c.production.id = p.id and p.id = ?" ;
		List<Object> valueList = new ArrayList<>();
		valueList.add(prod.getId());
		if (project != null) {
			query += " and cd.project.id = ? ";
			valueList.add(project.getId());
		}
		return find(query, valueList.toArray());
	}

	/** Used to check whether there exists a ContactDocument of ACTRA Intent form
	 * for the given Project or not. And returns that ContactDocument if exists.
	 *
	 * @param project Project that the current user is viewing on "Project/Projects" tab currently.
	 * @return Returns the ContactDocument of ACTRA Intent form for the given Project.
	 */
	public ContactDocument findIntentContactDocumentForProject(Project project) {
		if (project != null) {
			return findOneByNamedQuery(ContactDocument.GET_INTENT_CONTACT_DOCUMENT_BY_PROJECT, map("project", project));
		}
		return null;
	}

	/** Parameterized constructor that takes the following parameters
	 * @param currentContact the contact in which the documents will be distributed
	 * @param document the document that will be distributed to the contact
	 * @param packetName the packet with which the document was distributed
	 * @param aicp
	 * @param proj Project that user is currently viewing on the Project/Projects tab.
	 */
	public static ContactDocument create(Contact currentContact, Document document, String packetName, boolean aicp, Employment employment, ApprovalStatus status, Project proj) {
		ContactDocument cd = new ContactDocument();
		cd.setContact(currentContact);
		cd.setDocument(DocumentDAO.getInstance().refresh(document));
		cd.setDelivered(new Date());
		cd.setPacketName(packetName);
		cd.setDocumentChain(DocumentChainDAO.getInstance().findById(document.getDocChainId()));
		cd.setStatus(status);
		if (aicp) {
			if (proj != null) {
				cd.setProject(proj);
			}
			else if (employment != null) {
				cd.setProject(employment.getProject());
			}
			else {
				cd.setProject(SessionUtils.getCurrentProject());
			}
		}
		cd.setEmployment(employment);
		return cd;
	}

	/**
	 * A class to briefly hold the information about a set of Employment instances and the count
	 * of documents associated with each which are either in "approved" or "not approved" status.
	 */
	public class EmpDocInfo {

		/** The Employment.id value for the relevant Employment instance. */
		Integer empId;

		/** A flag indicating the meaning of the {@link #count} field: 1 for
		 * approved or locked documents, 0 for all other documents. */
		Integer approved;

		/** The number of ContactDocuments associated with this Employment
		 * instance which are either (a) in APPROVED or LOCKED status, or (b)
		 * NOT in that status. If {@link #approved} is 1, it is the (a) value,
		 * otherwise it is the (b) value. */
		Integer count;

		public EmpDocInfo(Integer id, Integer app, Number cnt) {
			empId = id;
			approved = app;
			count = cnt.intValue();
		}

		/** See {@link #empId}. */
		public Integer getEmpId() {
			return empId;
		}
		/** See {@link #empId}. */
		public void setEmpId(Integer empId) {
			this.empId = empId;
		}

		/** See {@link #approved}. */
		public Integer getApproved() {
			return approved;
		}
		/** See {@link #approved}. */
		public void setApproved(Integer approved) {
			this.approved = approved;
		}

		/** See {@link #count}. */
		public Integer getCount() {
			return count;
		}
		/** See {@link #count}. */
		public void setCount(Integer count) {
			this.count = count;
		}

		@Override
		public String toString() {
			return "[id=" + empId +
					", app=" + approved +
					", cnt=" + count + "]";
		}
	}

}
