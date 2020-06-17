package com.lightspeedeps.service;

import java.util.*;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.message.DoNotification;
import com.lightspeedeps.model.*;
import com.lightspeedeps.type.*;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.file.FileRepositoryUtils;

public class OnboardService extends BaseService {

	private static final Log log = LogFactory.getLog(OnboardService.class);

	ContactDocumentDAO contactDocumentDAO = ContactDocumentDAO.getInstance();

	/**
	 * Default constructor.
	 */
	public OnboardService() {
		// default constructor
	}

	/**
	 * @return A new instance of OnboardService.
	 */
	public static OnboardService getInstance() {
		return new OnboardService();
	}

	/**
	 * Deliver a copy of each Document in the given Packet to each of the
	 * Employment entries specified.
	 *
	 * @param status The status of the ContactDocument to be set for each
	 *            distributed document.
	 * @param currContact The Contact of the currently logged-in user.
	 * @param distribute True if the user requested that the documents be
	 *            distributed immediately (i.e., marked as OPEN, not PENDING).
	 * @param packetName The name of the Packet associated with this delivery,
	 *            or null if a single document is being delivered.
	 * @param employmentList A Collection of Employment instances who are all to
	 *            receive the packet of documents.
	 * @param production The Production to which the Employments and the Packet
	 *            belong.
	 * @param project The Project in which the Packet is defined; only applies
	 *            to Commercial productions, and should be null for
	 *            non-Commercial productions.
	 */
	public List <String> deliverStarts(ApprovalStatus status, Contact currContact, boolean distribute,
			String packetName, Collection<Employment> employmentList, Production production, Project project) {
		Integer numOfDocs;
		List <String> distributedDocumentList = new ArrayList<>();
		List <String> errorMsgs = new ArrayList<>(0);
		List<Packet> packets;
		Packet packet = null;
		Map<String, Object> valueMap = new HashMap<>();
		valueMap.put("packetName", packetName);
		valueMap.put("production", production);
		//For Commercial
		if (project != null && production.getType().isAicp()) {
			valueMap.put("project", project);
			packets = PacketDAO.getInstance().findByNamedQuery(Packet.FIND_PACKET_NAME_BY_PRODUCTION_AND_PROJECT, valueMap);
		}
		else {
			packets = PacketDAO.getInstance().findByNamedQuery(Packet.FIND_PACKET_BY_NAME_AND_PRODUCTION, valueMap);
		}
		if(packets == null || packets.isEmpty()) {
			errorMsgs.add(MsgUtils.getMessage("OnboardService.Packet.NotFound"));

			return errorMsgs;
		}

		packet = packets.get(0);
		Collection<Document> documentList = packet.getDocumentList();
		if(documentList == null || documentList.isEmpty()) {
			errorMsgs.add(MsgUtils.getMessage("OnboardService.Packet.NoDocumentsFound"));

			return errorMsgs;
		}
		for (Employment emp : employmentList) {
			Contact empContact = ContactDAO.getInstance().refresh(emp.getContact());
			for (Document document : documentList) {
				distributedDocumentList = deliverStart(emp, document, status, distribute,
						packetName, empContact, distributedDocumentList);
			}
			numOfDocs = distributedDocumentList.size();
			if (numOfDocs > 0) {
				DoNotification.getInstance().documentDistributed(emp.getContact(), currContact, numOfDocs, distributedDocumentList);
			}
			distributedDocumentList.clear();
			emp.setChecked(false);
			emp.setPercentageArray(null);
		}

		return errorMsgs;
	}

	/**
	 * Deliver a single Start document to one Employment instance.
	 *
	 * @param emp The Employment to which the document is being delivered.
	 * @param document The Document to be delivered.
	 * @param status The status of the ContactDocument to be set.
	 * @param distribute True if the user requested that the documents be
	 *            distributed immediately (i.e., marked as OPEN, not PENDING).
	 * @param packetName The name of the Packet associated with this delivery,
	 *            or null if a single document is being delivered.
	 * @param empContact The Contact of the Employment record to which the
	 *            document is being delivered.
	 * @param distributedDocumentList
	 * @return List of names of distributed documents, used for notification.
	 */
	private List<String> deliverStart(Employment emp, Document document, ApprovalStatus status,
			boolean distribute, String packetName, Contact empContact, List<String> distributedDocumentList) {
		ApprovalStatus docStatus = status;
		PayrollFormType type = PayrollFormType.toValue(document.getName());
		if (type.isWtpa() && distribute) {
			docStatus = ApprovalStatus.PENDING;
		}

		// As of 1/19/2017, we allow duplicate I9s.
		Integer id = saveContactDocument(emp, empContact, document, packetName, docStatus, null);
		if (docStatus == ApprovalStatus.OPEN && id != null) {
			distributedDocumentList.add(document.getNormalizedName());
		}
		return distributedDocumentList;
	}

	/** Method used to save the ContactDocument instance to be created
	 * and also looks for document name to set its form type
	 * @param emp
	 * @param currentContact
	 * @param document document to be delivered
	 * @param packetName packet name if document belongs to a packet
	 * @param proj
	 * @return Integer id of the saved contact document.
	 */
	public Integer saveContactDocument(Employment emp, Contact currentContact, Document document, String packetName, ApprovalStatus status, Project proj) {
		Integer contactDocId = null;
		try {
			contactDocId = null;
			ContactDocument contactDoc = ContactDocumentDAO.create(currentContact, document, packetName, SessionUtils.getProduction().getType().isAicp(), emp, status, proj);
			if (document.getMimeType() == MimeType.LS_FORM) {
				boolean found = false;
				for (PayrollFormType formType : PayrollFormType.values()) {
					log.debug("PayrollFormType = " + formType);
					if (formType.getName() != null && formType.getName().equals(document.getName())) {
						found = true;
						log.debug("");
						contactDoc.setFormType(formType);
						if (formType == PayrollFormType.START) {
							// Then update StartForm if necessary
							Integer id = updateCreateStartForm(emp, currentContact);
							if (id == null) { // StartForm was not created...
								MsgUtils.addFacesMessage("StatusListBean.DeliverDocumentError", FacesMessage.SEVERITY_INFO, PayrollFormType.START.getName());
								return null; // do NOT create ContactDocument!
							}
							contactDoc.setRelatedFormId(id);
						}
					}
				}
				if (! found) {
					log.debug("");
					contactDoc.setFormType(PayrollFormType.OTHER);
				}
			}
			else {
				contactDoc.setFormType(PayrollFormType.OTHER);
			}
			contactDocId = contactDocumentDAO.save(contactDoc);
			if (contactDoc.getFormType() == PayrollFormType.OTHER && contactDoc.getStatus() == ApprovalStatus.OPEN) {
				// Custom docs only, & skip if PENDING.  If document has action=Receive, mark CD approved.
				if (document.getEmployeeAction() == DocumentAction.RCV) {
					contactDoc.setStatus(ApprovalStatus.APPROVED);
					contactDocumentDAO.attachDirty(contactDoc);
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return contactDocId;
	}


	/**
	 * Method used to create the start form instances for the
	 * employment who will be distributed with the payroll start.
	 *
	 * @param emp Employment to get the document
	 * @param empContact Employment's contact
	 * @return Id of newly created StartForm, or null if no StartForm was
	 *         created.
	 */
	private Integer updateCreateStartForm(Employment emp, Contact empContact) {
		// Either add StartForm, or do nothing.
		StartFormDAO startFormDAO = StartFormDAO.getInstance();
//		ContactDocumentDAO contactDocumentDAO = ContactDocumentDAO.getInstance();
		StartForm priorStartForm = null;
		StartForm newStartForm = null;
		//Project project = SessionUtils.getCurrentProject();
		Integer id = null;
		Date today = CalendarUtils.todaysDate();

		boolean oldValid = contactDocumentDAO.existsNonVoidByEmployment(emp);
		if (! oldValid) {
			// Create a "NEW" start form, with effective-start date of today.
			newStartForm = StartFormService.createStartForm(StartForm.FORM_TYPE_NEW, null, null, empContact,
					emp, emp.getProject(), today, today, null, false, true);

			//LS-2159, set default work country for the startform
			if (newStartForm.getWorkCountry() == null) {
				PayrollPreference payrollPreference = StartFormService.getSFPayrollPreference(newStartForm);
				if (payrollPreference != null) {
					newStartForm.setWorkCountry(payrollPreference.getWorkCountry());
					newStartForm.setWorkState(payrollPreference.getWorkState());
				}
			}
			if (newStartForm != null) {
				//newStartForm.setStatus(ApprovalStatus.OPEN);
				startFormDAO.attachDirty(newStartForm);
				id = newStartForm.getId();
			}
		}
		else {
			Map<String, Object> valueMap = new HashMap<>();
			valueMap.put("employment", emp);
			priorStartForm = startFormDAO.findOneByNamedQuery(
					StartForm.GET_START_FORM_NON_VOID_BY_EMPLOYMENT_DESCENDING_ORDER, valueMap);
			if (priorStartForm != null) {
				// Remove check for "duplicate" Start Forms; rev 3.2.6.9114
//				if (priorStartForm.getEffectiveStartDate() == null) {
//					return null; // indicates StartForm was not created.
//				}
				String type = StartForm.FORM_TYPE_CHANGE; // Default form type
				if (priorStartForm.getEarliestEndDate() != null && priorStartForm.getEarliestEndDate().before(today)) {
					// prior (non-VOID) form has expired, so create the new one as "re-hire"
					type = StartForm.FORM_TYPE_REHIRE;
				}
				// Note: leave Effective Start Date null
				newStartForm = StartFormService.createStartForm(type, priorStartForm, null, empContact,
						emp, emp.getProject(), today, null, null, false, true);
				newStartForm.setPriorFormId(priorStartForm.getId());
				startFormDAO.attachDirty(newStartForm);
				id = newStartForm.getId();
			}
		}
		return id;
	}

	public DocumentStatusType getDocumentsStatus(List<ContactDocument> cds, List<String> msgs) {
		// Go through the document list. If we find a document that has not been submitted,
		// break out of loop and return pending status;
		for (ContactDocument doc : cds) {
			if (! doc.getSubmitted()) {
				return DocumentStatusType.STATUS_PENDING;
			}
		}
		// All docs have been submitted (or voided)
		return DocumentStatusType.STATUS_COMPLETE;
	}

	/** Method used to generate all the other Objects required to enable the Onboarding feature.
	 * @param selectedProduction
	 * @return
	 */
	public Production enableProductionOnboarding(Production selectedProduction) {
		selectedProduction.setAllowOnboarding(true);
		selectedProduction = ProductionDAO.getInstance().merge(selectedProduction);
		generateOnboard(selectedProduction);
		generateContactDocuments(selectedProduction);
		generateApprovalPaths(selectedProduction);
		// Need to give the standard Admin user onboarding permissions...
		User pUser = UserDAO.getInstance().findSingleUser( // get user, using email in web.xml
				ApplicationUtils.getInitParameterString(Constants.INIT_PARAM_PROD_ADMIN));
		if (pUser != null) {
			// Find contact for pUser within production being enabled
			Contact adminContact = ContactDAO.getInstance().findByUserProduction(pUser, selectedProduction);
			if (adminContact != null) {
				// find employment for adminContact with "LS eps Admin" role;
				for (Employment emp : adminContact.getEmployments()) {
					if (emp.getRole().isLsAdmin()) {
						long masks = Permission.MANAGE_START_DOCS.getMask()
								+ Permission.MANAGE_START_DOC_APPROVERS.getMask() + Permission.VIEW_ALL_DISTRIBUTED_FORMS.getMask();
						emp.setPermissionMask(emp.getPermissionMask() | masks);
						EmploymentDAO.getInstance().attachDirty(emp);
						break;
					}
				}
			}
		}

		// Need to give the View Admin user onboarding permissions...
		User user = UserDAO.getInstance().findSingleUser( // get user, using email in web.xml
				ApplicationUtils.getInitParameterString(Constants.INIT_PARAM_PROD_ADMIN_VIEW));
		if (user != null) {
			// Find contact for user within production being enabled
			Contact viewContact = ContactDAO.getInstance().findByUserProduction(user, selectedProduction);
			if (viewContact != null) {
				// find employment for viewContact view admin role;
				for (Employment emp : viewContact.getEmployments()) {
					if (emp.getRole().isViewAdmin()) {
						emp.setPermissionMask(emp.getPermissionMask() | Permission.VIEW_ALL_DISTRIBUTED_FORMS.getMask());
						EmploymentDAO.getInstance().attachDirty(emp);
						break;
					}
				}
			}
		}
		return selectedProduction;
	}

	/** Method to generate Onboarding stuffs(Onboard folder, Standard documents and chains) when onboarding is enabled.
	 * @param selectedProduction, production for which Onboarding is enabled.
	 */
	private void generateOnboard(Production selectedProduction) {
		Folder repository = selectedProduction.getRootFolder();
		User currentUser = SessionUtils.getCurrentUser();
		Folder onboardFolder = FileRepositoryUtils.findOnboardingFolder(selectedProduction);
		if (onboardFolder == null) {
			onboardFolder = new Folder("Onboarding", currentUser, repository, false, new Date());
			repository.getFolders().add(onboardFolder);
			FolderDAO.getInstance().save(onboardFolder);
			onboardFolder = FileRepositoryUtils.findOnboardingFolder(selectedProduction);
		}
		if (onboardFolder != null) {
			boolean isCanadian = selectedProduction.getType().isCanadaTalent();

			/**LS-2150 isTeamPayroll to check, if this is a TEAM payroll client*/
			boolean isTeamPayroll = false;
			selectedProduction = ProductionDAO.getInstance().refresh(selectedProduction);
			PayrollService payrollService = selectedProduction.getPayrollPref().getPayrollService();
			if (payrollService != null) {
				isTeamPayroll = payrollService.getTeamPayroll();
			}

			DocumentDAO documentDAO = DocumentDAO.getInstance();
			for (PayrollFormType form : PayrollFormType.values()) {
				if ((isCanadian && form.isAutoAddedCanada()) || // LS-3965 Canada gets ONLY canadian forms
						(! isCanadian &&
							(form.isAutoAdded() || (isTeamPayroll && form.isAutoAddedTeam())))) {
					log.debug("Create document and document chain for builtin form : " + form.getName());
					documentDAO.saveStandardDocumentDocumentChain(onboardFolder, form);
				}
			}
		}
	}

	/** Method to generate missing ContactDocuments for StartForms when Onboarding feature for a production is enabled.
	 * @param selectedProduction production for which Onboarding is enabled.
	 */
	private void generateContactDocuments(Production selectedProduction) {
		List<StartForm> sfList = new ArrayList<>();
		Map<String, Object> valueMap = new HashMap<>();
		valueMap.put("production", selectedProduction);
		sfList = StartFormDAO.getInstance().findByNamedQuery(StartForm.GET_START_FORM_BY_PRODUCTION, valueMap);
		selectedProduction = ProductionDAO.getInstance().refresh(selectedProduction);
		Document payrollDocument = DocumentDAO.findPayrollDocument(selectedProduction);
		if (payrollDocument != null) { // null for production types w/o Payroll Starts, e.g., Canadian
			DocumentChain payrollChain = DocumentChainDAO.getInstance().findById(payrollDocument.getDocChainId());
			StartFormDAO startFormDAO = StartFormDAO.getInstance();

			List<ContactDocument> cdList = ContactDocumentDAO.getInstance().findByProperty("document", payrollDocument);
			Set<Integer> relatedFormIdList = new HashSet<>();
			for (ContactDocument c : cdList) {
				relatedFormIdList.add(c.getRelatedFormId());
			}

			for (StartForm sf : sfList) {
				if (! relatedFormIdList.contains(sf.getId())) {
					startFormDAO.createContactDocument(sf, payrollDocument, payrollChain, ApprovalStatus.PENDING);
				}
			}
		}
	}

	/** Method to generateApproval Paths for Standard Document Chains when Onboarding feature for a production is enabled.
	 * @param selectedProduction production for which Onboarding is enabled.
	 */
	private void generateApprovalPaths(Production selectedProduction) {
		try {
			log.debug("");
			if (selectedProduction != null) {
				boolean ret = true;
				Set<Project> projects = null;
				if (selectedProduction.getType().isAicp()) {
					projects = selectedProduction.getProjects();
					for (Project proj : projects) {
						ret &= ApprovalPathDAO.getInstance().generateApprovalPaths(selectedProduction, proj);
					}
				}
				else {
					ret = ApprovalPathDAO.getInstance().generateApprovalPaths(selectedProduction, null);
				}
				if (! ret) {
					// message 'creating one or more default Approval Paths failed due to a system error'
					MsgUtils.addFacesMessage("ApprovalPath.GenerateApprovalPathFailed", FacesMessage.SEVERITY_ERROR);
				}
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}


}
