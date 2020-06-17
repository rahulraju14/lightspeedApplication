package com.lightspeedeps.web.onboard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.DocumentChainDAO;
import com.lightspeedeps.dao.DocumentDAO;
import com.lightspeedeps.dao.FolderDAO;
import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.model.Document;
import com.lightspeedeps.model.DocumentChain;
import com.lightspeedeps.model.Folder;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.User;
import com.lightspeedeps.type.MimeType;
import com.lightspeedeps.type.PayrollFormType;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.file.FileRepositoryUtils;
import com.lightspeedeps.web.view.View;

/**
 * Backing bean for the Copy Documents mini tab under OnBoarding tab.
 */
@ManagedBean
@ViewScoped
public class CopyDocumentBean extends View implements Serializable  {

	private static final long serialVersionUID = 3502111624348462607L;

	private static final Log log = LogFactory.getLog(CopyDocumentBean.class);

	/** The documentList, list of all the documents in the Onboarding folder of current production.	 */
	private List<DocumentChain> documentChainList;

	/** The documents checked to copy in other productions. */
	private List<DocumentChain> checkedDocChainList;

	/** True, if the master check box is checked otherwise false */
	private Boolean checkedForAllDocChains = false;

	/** The productionList, list of all the productions.	 */
	private List<Production> productionList;

	/** The target productions checked to copy documents. */
	private List<Production> checkedProductionList;

	/** True, if the master check box is checked otherwise false */
	private Boolean checkedForAllProductions = false;

	/** The production to be removed from the list of selected Productions. */
	private Production prodToUncheck;

	private transient DocumentChainDAO documentChainDAO;

	private transient ProductionDAO productionDAO;

	public CopyDocumentBean() {
		super("CopyDocumentBean.");
		checkedDocChainList = new ArrayList<>();
	}

	public static CopyDocumentBean getInstance() {
		return (CopyDocumentBean)ServiceFinder.findBean("copyDocumentBean");
	}

	/** The method used to create the Document list
	 * @return {@link documentList}
	 */
	private List<DocumentChain> createDocumentChainList() {
		documentChainList = new ArrayList<>();
		documentChainList = getDocumentChainDAO().findAllOnboardingDocumentChains(SessionUtils.getNonSystemProduction());
		/*if (documentList != null) {
			if (AuthorizationBean.getInstance().isAdmin()) { // if Ls Admin add the standard I9 to the list for self distribution
				log.debug("is admin");
				// Add all NON-standard docs (e.g., uploaded ones) that are in the SYSTEM Onboarding folder.
				//Folder sysFolder = ApplicationUtils.getSystemProduction().getRootFolder();
				//sysFolder = FileRepositoryUtils.findFolder(Constants.ONBOARDING_FOLDER, sysFolder);
				Folder sysFolder = FileRepositoryUtils.findOnboardingFolder(ApplicationUtils.getSystemProduction());
				Map<String, Object> valueMap = new HashMap<>();
				valueMap.put("folderId", sysFolder.getId());
				List<Document> sysDocs = DocumentDAO.getInstance().
						findByNamedQuery(Document.GET_NON_STANDARD_DOCUMENT_BY_FOLDER_ID, valueMap);
				documentList.addAll(sysDocs);
			}
		}*/
		return documentChainList;
	}

	/** The method used to create the Production list, Productions with
	 *  Status active and enabled Onboarding.
	 * @return {@link documentList}
	 */
	private List<Production> createProductionList() {
		productionList = new ArrayList<>();
		productionList = getProductionDAO().findByNamedQuery(Production.GET_PRODUCTION_LIST_BY_ACTIVE_STATUS_AND_ALLOW_ONBOARDING);
		if (productionList != null && ! productionList.isEmpty()) {
			productionList.remove(SessionUtils.getNonSystemProduction());
		}
		return productionList;
	}

	/** Value change listener for the Checkboxes in DocumentChain list/table,
	 * listens individual checkbox's checked / unchecked event.
	 * @param evt
	 */
	public void listenSingleDocumentCheck(ValueChangeEvent evt) {
		try {
			DocumentChain docChain = (DocumentChain) evt.getComponent().getAttributes().get("selectedRow");
			if (checkedDocChainList == null) {
				checkedDocChainList = new ArrayList<>();
			}
			if (docChain.getChecked()) {
				log.debug("Selected Chain = " + docChain.getName());
				if (! checkedDocChainList.contains(docChain)) {
					checkedDocChainList.add(docChain);
					log.debug("Checked Document chains:" + (checkedDocChainList.isEmpty() ? "Empty List" : checkedDocChainList.size()));
				}
			}
			else {
				checkedDocChainList.remove(docChain);
				log.debug("Checked Document chains:" + (checkedDocChainList.isEmpty() ? "Empty List" : checkedDocChainList.size()));
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Value change listener for the Master Checkbox in DocumentChain list/table,
	 * listens individual checkbox's checked / unchecked event.
	 * @param evt
	 */
	public void listenCheckedForAllDocChains(ValueChangeEvent evt) {
		log.debug("");
		if (checkedDocChainList == null) {
			checkedDocChainList = new ArrayList<>();
		}
		if (getCheckedForAllDocChains()) {
			for (DocumentChain docChain : documentChainList) {
				docChain.setChecked(true);
				if (! checkedDocChainList.contains(docChain)) {
					checkedDocChainList.add(docChain);
				}
			}
		}
		else {
			uncheckDocChainList();
		}
		log.debug("Checked Document chains:" + (checkedDocChainList.isEmpty() ? "Empty List" : checkedDocChainList.size()));
	}

	private void uncheckDocChainList() {
		checkedDocChainList.clear();
		for (DocumentChain chain : documentChainList) {
			chain.setChecked(false);
		}
	}

	/** Value change listener for the Checkboxes in Production list/table,
	 * listens individual checkbox's checked / unchecked event.
	 * @param evt
	 */
	public void listenSingleProductionCheck(ValueChangeEvent evt) {
		try {
			Production prod = (Production) evt.getComponent().getAttributes().get("selectedRow");
			if (checkedProductionList == null) {
				checkedProductionList = new ArrayList<>();
			}
			if (prod.getSelected()) {
				log.debug("Selected Production = " + prod.getTitle());
				if (! checkedProductionList.contains(prod)) {
					checkedProductionList.add(prod);
					log.debug("Checked Productions = " + (checkedProductionList.isEmpty() ? "Empty List" : checkedProductionList.size()));
				}
			}
			else {
				checkedProductionList.remove(prod);
				log.debug("Checked Productions = " + (checkedProductionList.isEmpty() ? "Empty List" : checkedProductionList.size()));
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** Value change listener for the Master Checkbox in Production list/table,
	 * listens individual checkbox's checked / unchecked event.
	 * @param evt
	 */
	public void listenCheckedForAllProductions(ValueChangeEvent evt) {
		log.debug("");
		if (checkedProductionList == null) {
			checkedProductionList = new ArrayList<>();
		}
		if (getCheckedForAllProductions()) {
			for (Production prod : productionList) {
				prod.setSelected(true);
				if (! checkedProductionList.contains(prod)) {
					checkedProductionList.add(prod);
				}
			}
		}
		else {
			uncheckProductionList();
		}
		log.debug("Checked Productions = " + (checkedProductionList.isEmpty() ? "Empty List" : checkedProductionList.size()));
	}

	private void uncheckProductionList() {
		checkedProductionList.clear();
		for (Production prod : productionList) {
			prod.setSelected(false);
		}
	}

	/** Method to remove a selected Production from the list of selected Productions.
	 * @return null navigation string.
	 */
	public String actionRemoveSelectedProd() {
		try {
			if (prodToUncheck != null) {
				log.debug("prodToUncheck: " + prodToUncheck.getTitle());
				if (checkedProductionList.contains(prodToUncheck)) {
					prodToUncheck.setSelected(false);
					checkedProductionList.remove(prodToUncheck);
					log.debug("checkedProductionList size : " + checkedProductionList.size());
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** Method to copy the selected Doucuments to the selected Productions.
	 */
	public void actionCopyDocuments() {
		try {
			if (checkedDocChainList != null && checkedProductionList != null &&
					(! checkedDocChainList.isEmpty()) && (! checkedProductionList.isEmpty())) {
				log.debug("");
				int countSuccess = 0;
				int countProd = 0;

				Folder sourceOnboardFolder = FileRepositoryUtils.findOnboardingFolder(SessionUtils.getNonSystemProduction());
				User currentUser = SessionUtils.getCurrentUser();
				DocumentDAO documentDAO = DocumentDAO.getInstance();
				for (Production prod : checkedProductionList) {
					log.debug(" Production = " + prod.getId() + ", " + prod.getTitle());
					prod = getProductionDAO().refresh(prod);
					Folder repository = prod.getRootFolder();
					Folder onboardFolder = FileRepositoryUtils.findOnboardingFolder(prod);
					if (onboardFolder == null) {
						onboardFolder = new Folder("Onboarding", currentUser, repository, false, new Date());
						repository.getFolders().add(onboardFolder);
						FolderDAO.getInstance().save(onboardFolder);
						log.debug("New ONBOARD Folder = " + onboardFolder.getId());
					}
					log.debug("Existing ONBOARD Folder = " + onboardFolder.getId());
					if (onboardFolder != null) {
						List<DocumentChain> prodChainList = getDocumentChainDAO().findAllOnboardingDocumentChains(prod);
						List<String> prodChainNameList = new ArrayList<>();
						for (DocumentChain chain : prodChainList) {
							prodChainNameList.add(chain.getDisplayName());
						}
						for (DocumentChain originalDocChain : checkedDocChainList) {
							log.debug("DOCUMENT CHAIN: " + originalDocChain.getName());
							originalDocChain = getDocumentChainDAO().refresh(originalDocChain);

							if (! prodChainNameList.contains(originalDocChain.getDisplayName())) {
								Map<String, Object> values = new HashMap<> ();
								values.put("name", originalDocChain.getName());
								values.put("folderId", sourceOnboardFolder.getId());
								Document originalDoc = DocumentDAO.getInstance().findOneByNamedQuery(Document.GET_DOCUMENT_BY_NAME_FOLDER_ID, values);
								if (originalDocChain.getType() == MimeType.LS_FORM) {
									log.debug("SAVE DOCUMENT CHAIN FOR : " + originalDocChain.getName());
									documentDAO.saveStandardDocumentDocumentChain(onboardFolder, PayrollFormType.toValue(originalDocChain.getName()));
									countSuccess++;
								}
								else {
									log.debug("CUSTOM DOCUMENT");
									DocumentChain documentChain = new DocumentChain();
									documentChain.copyFrom(originalDocChain);
									documentChain.setFolder(onboardFolder);
									Integer docChainId = getDocumentChainDAO().save(documentChain);
									Document document = new Document();
									if (originalDoc.getOriginalDocumentId() != null) {
										log.debug("Fetching original document");
										originalDoc = DocumentDAO.getInstance().findById(originalDoc.getOriginalDocumentId());
									}
									document.copyFrom(originalDoc);
									document.setDocChainId(docChainId);
									document.setFolder(onboardFolder);
									document.setOriginalDocumentId(originalDoc.getId());
									DocumentDAO.getInstance().save(document);
									countSuccess++;
								}
							}
							else {
								log.debug("><><>< DUPLICATE DOCUMENT ><><><");
								countProd++;
								//MsgUtils.addFacesMessageText("DUPLICATE DOCUMENT", FacesMessage.SEVERITY_INFO);
							}
						}
					}
				}
				if (countProd == 0 && countSuccess > 0) {
					MsgUtils.addFacesMessage("CopyDocumentBean.Success", FacesMessage.SEVERITY_INFO);
				}
				else if (countProd > 0 && countSuccess > 0) {
					MsgUtils.addFacesMessage("CopyDocumentBean.PartialSuccess", FacesMessage.SEVERITY_INFO, countProd);
				}
				else {
					MsgUtils.addFacesMessage("CopyDocumentBean.Failure", FacesMessage.SEVERITY_ERROR);
				}
				setCheckedForAllDocChains(false);
				setCheckedForAllProductions(false);
				uncheckDocChainList();
				uncheckProductionList();
			}
			else {
				MsgUtils.addFacesMessage("CopyDocumentBean.SelectDocumentProduction", FacesMessage.SEVERITY_ERROR);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	/** See {@link #documentChainList}. */
	public List<DocumentChain> getDocumentChainList() {
		if (documentChainList == null) {
			documentChainList = createDocumentChainList();
		}
		return documentChainList;
	}
	/** See {@link #documentChainList}. */
	public void setDocumentChainList(List<DocumentChain> documentChainList) {
		this.documentChainList = documentChainList;
	}

	/** See {@link #checkedDocChainList}. */
	public List<DocumentChain> getCheckedDocChainList() {
		return checkedDocChainList;
	}
	/** See {@link #checkedDocChainList}. */
	public void setCheckedDocChainList(List<DocumentChain> checkedDocChainList) {
		this.checkedDocChainList = checkedDocChainList;
	}

	/** See {@link #checkedForAllDocChains}. */
	public Boolean getCheckedForAllDocChains() {
		return checkedForAllDocChains;
	}
	/** See {@link #checkedForAllDocChains}. */
	public void setCheckedForAllDocChains(Boolean checkedForAllDocChains) {
		this.checkedForAllDocChains = checkedForAllDocChains;
	}

	/** See {@link #productionList}. */
	public List<Production> getProductionList() {
		if (productionList == null) {
			productionList = createProductionList();
		}
		return productionList;
	}
	/** See {@link #productionList}. */
	public void setProductionList(List<Production> productionList) {
		this.productionList = productionList;
	}

	/** See {@link #checkedProductionList}. */
	public List<Production> getCheckedProductionList() {
		return checkedProductionList;
	}
	/** See {@link #checkedProductionList}. */
	public void setCheckedProductionList(List<Production> checkedProductionList) {
		this.checkedProductionList = checkedProductionList;
	}

	/** See {@link #checkedForAllProductions}. */
	public Boolean getCheckedForAllProductions() {
		return checkedForAllProductions;
	}
	/** See {@link #checkedForAllProductions}. */
	public void setCheckedForAllProductions(Boolean checkedForAllProductions) {
		this.checkedForAllProductions = checkedForAllProductions;
	}

	/** See {@link #prodToUncheck}. */
	public Production getProdToUncheck() {
		return prodToUncheck;
	}
	/** See {@link #prodToUncheck}. */
	public void setProdToUncheck(Production prodToUncheck) {
		this.prodToUncheck = prodToUncheck;
	}

	/** See {@link #documentChainDAO}. */
	public DocumentChainDAO getDocumentChainDAO() {
		if (documentChainDAO == null) {
			documentChainDAO = DocumentChainDAO.getInstance();
		}
		return documentChainDAO;
	}

	/** See {@link #productionDAO}. */
	public ProductionDAO getProductionDAO() {
		if (productionDAO == null) {
			productionDAO = ProductionDAO.getInstance();
		}
		return productionDAO;
	}

}
