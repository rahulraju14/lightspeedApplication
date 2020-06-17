package com.lightspeedeps.web.project;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ContactImportDAO;
import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.dao.ProjectDAO;
import com.lightspeedeps.model.ContactImport;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.Project;
import com.lightspeedeps.service.ImportService;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.popup.FileLoadBean;
import com.lightspeedeps.web.util.HeaderViewBean;
import com.lightspeedeps.web.view.View;

/**
 * Backing bean for the Import mini-tab of the Projects page. This supports
 * uploading and importing a tab-delimited file containing a list of projects
 * and personnel to be added to a Production.
 */
@ManagedBean
@ViewScoped
public class ImportContactsBean extends View implements Serializable {
	/** */
	private static final long serialVersionUID = 4634957178851974000L;

	private static final Log log = LogFactory.getLog(ImportContactsBean.class);

	/** The action key for the import step that creates Projects and Contacts. */
	private static final int ACT_CREATE = 14;

	/** The action key for the import step that uploads the file to the server. */
	private static final int ACT_IMPORT = 18;

	/** The tab number (origin zero) of the Import mini-tab. */
	@SuppressWarnings("unused")
	private static final int TAB_IMPORT = 2;

	// Fields
	/** The current Production. */
	private Production production;

	/** The currently selected project (in the list), whose data will
	 * be displayed in the right-hand panels. This project will be used
	 * as the source of preferences to be copied to any new Projects created. */
	private Project project;

	/** The list of ContactImport instances to be displayed. */
	private List<ContactImport> contactList = null;

	/** The name of the file being uploaded. */
	private String importFileName;

	/** True if the "import data" dialog box is to be displayed */
	private boolean showImport;

	/** True if added Contacts should be given production access. */
	private boolean allowAccess;

	/** True if invitation emails should be sent to new Contacts. */
	private boolean sendInvitation;

	private final transient ContactImportDAO contactImportDAO = ContactImportDAO.getInstance();

	private transient ProjectDAO projectDAO;

	/* Constructor */
	public ImportContactsBean() {
		super("Project.");
		log.debug("");
		production = SessionUtils.getNonSystemProduction();
		try {
			Integer id = SessionUtils.getInteger(Constants.ATTR_PROJECT_VIEW_ID);
			if (id == null) {
//				project = ProjectViewBean.getInstance().getProject();
				if (project == null) {
					project = SessionUtils.getCurrentProject();
				}
				if (project != null) {
					id = project.getId();
				}
			}
			setupSelectedItem(id);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	public static ImportContactsBean getInstance() {
		return (ImportContactsBean)ServiceFinder.findBean("importContactsBean");
	}

	/**
	 * Determine which element we are supposed to view. If the id given is null or invalid, we try
	 * to display the "default" element.
	 *
	 * @param id
	 */
	protected void setupSelectedItem(Integer id) {
		log.debug("id="+id);
		if (id == null) {
			SessionUtils.put(Constants.ATTR_PROJECT_VIEW_ID, null);
			project = null;
		}
		else {
			project = getProjectDAO().findById(id);
		}
	}

	/**
	 * Called when user clicks "Ok" (or equivalent) on a standard confirmation
	 * dialog.
	 *
	 * @see com.lightspeedeps.web.popup.PopupHolder#confirmOk(int)
	 *
	 * @return null navigation string (since none of these actions navigate to a
	 *         new page).
	 */
	@Override
	public String confirmOk(int action) {
		log.debug(action);
		String res = null;
		switch(action) {
			case ACT_IMPORT:
				res = actionImportOk();
				break;
			case ACT_CREATE:
				res = actionCreateOk();
				break;
			default:
				res = super.confirmOk(action);
		}
		return res;
	}

	/**
	 * Action method for the "import" button. This runs the file upload process,
	 * to upload the user's data file to our server. Control will return to us
	 * at {@link #actionImportOk()} via the usual confirmOk() path.
	 *
	 * @return null navigation string
	 */
	public String actionImport() {
		FileLoadBean bean = FileLoadBean.getInstance();
		bean.setAddedMessageId("Contact.Import.Upload.Success");
		bean.show(this, ACT_IMPORT, "Contact.Import.Upload.");
		return null;
	}

	/**
	 * Callback method invoked when the user's file has been successfully
	 * uploaded to the server. The upload bean (FileLoadBean) must be queried to
	 * determine the actual filename created on the server.
	 *
	 * @return null navigation string
	 */
	private String actionImportOk() {
		try {
			importFileName = null; // "D:\\Dev\\Studio\\samples\\timecards_P25_17155340.tab";
			FileLoadBean bean = FileLoadBean.getInstance();
			importFileName = bean.getServerFilename();
			log.debug("file=" + importFileName);
			loadImportTable(); // load the file into our ContactImport table
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Load the uploaded file specified by {@link #importFileName} (an absolute
	 * path) into the 'temporary' ContactImport table.
	 *
	 * @return null navigation string
	 */
	private String loadImportTable() {
		if (importFileName != null) {
			deleteData(); // purge existing data before loading new data
			createImportTable(importFileName);
			importFileName = null; // prevents accidental re-load if user double-clicks
		}
		return null;
	}

	/**
	 * Prompt user for "create" options. Called when the user clicks the
	 * "Create" button. Buttons on the popup will call us back at either
	 * {@link #actionCreateOk()} or {@link #actionCreateCancel()}.
	 *
	 * @return null navigation string
	 */
	public String actionCreate() {
		if (getContactList().size() == 0) {
			MsgUtils.addFacesMessage("Contact.Import.NoData", FacesMessage.SEVERITY_ERROR);
		}
		else {
			showImport = true;
		}
		return null;
	}

	/**
	 * User clicked "Create" (or equivalent 'ok') button on the prompt for the
	 * import options. Close the popup and proceed with processing the data.
	 *
	 * @return null navigation string
	 */
	public String actionCreateOk() {
		showImport = false;
		return processImportTable();
	}

	/**
	 * User clicked "Cancel" on the prompt for import options. Close the popup
	 * and we're done.
	 *
	 * @return null navigation string
	 */
	public String actionCreateCancel() {
		showImport = false;
		return null;
	}

	/**
	 * Load the data in the ContactImport table into the system, creating new
	 * Projects as necessary, and creating new Contact (crew) instances, and
	 * adding them to the given Project. Called via confirmO(), when the user
	 * clicks the "OK" (or equivalent) button on the prompt dialog.
	 *
	 * @return null navigation string
	 */
	private String processImportTable() {
		try {
			production = ProductionDAO.getInstance().refresh(production);
			project = getProjectDAO().refresh(project);
			ImportService importer = new ImportService();
			List<String> msgs = new ArrayList<>();
			try {
				importer.createFromTable(allowAccess, sendInvitation, msgs);
			}
			catch (Exception e) {
				EventUtils.logError(e);
				MsgUtils.addGenericErrorMessage();
			}
			project = getProjectDAO().refresh(project);
			contactList = null;
			if (msgs.size() == 0) {
				MsgUtils.addFacesMessage("Contact.Import.CreateOk", FacesMessage.SEVERITY_INFO);
			}
			else {
				for (String msg : msgs) {
					MsgUtils.addFacesMessageText(msg, FacesMessage.SEVERITY_ERROR);
				}
				MsgUtils.addFacesMessage("Contact.Import.CreateFailed", FacesMessage.SEVERITY_ERROR,
						project.getTitle());
			}
			ProjectViewBean.getInstance().refreshList();
			HeaderViewBean.reset();	// refresh project list in page header
			if (project != null) {
				setupSelectedItem(project.getId());
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Remove existing data from the ContactImport table that's
	 * associated with this Production.
	 */
	private void deleteData() {
		production = ProductionDAO.getInstance().refresh(production);
		contactImportDAO.deleteByProduction(production);
		contactList = null;
	}

	/**
	 * Load an import file into our temporary ContactImport table.
	 *
	 * @param filename The absolute filename of the file (previously uploaded to
	 *            the server) to be loaded into our table.
	 * @return The current Project, possibly refreshed.
	 */
	private void createImportTable(String filename) {
		log.debug("");
		ImportService importer = new ImportService(filename);
		List<String> msgs = new ArrayList<String>();
		boolean ret = importer.load(msgs);
		contactList = null; // force refresh of table display
		if (ret) {
//			MsgUtils.addFacesMessage("Contact.ImportOk", FacesMessage.SEVERITY_INFO,
//					project.getTitle());
		}
		else {
			for (String msg : msgs) {
				MsgUtils.addFacesMessageText(msg, FacesMessage.SEVERITY_ERROR);
			}
			MsgUtils.addFacesMessage("Contact.ImportFailed", FacesMessage.SEVERITY_ERROR);
		}
	}

	public Project getProject() {
		return project;
	}
	public void setProject(Project project) {
		this.project = project;
	}
	public Project getElement() {
		return getProject();
	}

	/** See {@link #production}. */
	public Production getProduction() {
		return production;
	}
	/** See {@link #production}. */
	public void setProduction(Production production) {
		this.production = production;
	}

	/**See {@link #contactList}. */
	public List<ContactImport> getContactList() {
		if (contactList ==  null) {
			contactList = ContactImportDAO.getInstance().findByProperty(ContactImportDAO.PRODUCTION, production);
		}
		return contactList;
	}
	/**See {@link #contactList}. */
	public void setContactList(List<ContactImport> list) {
		contactList = list;
	}

	/** See {@link #showImport}. */
	public boolean getShowImport() {
		return showImport;
	}
	/** See {@link #showImport}. */
	public void setShowImport(boolean showImport) {
		this.showImport = showImport;
	}

	/** See {@link #allowAccess}. */
	public boolean getAllowAccess() {
		return allowAccess;
	}
	/** See {@link #allowAccess}. */
	public void setAllowAccess(boolean allowAccess) {
		this.allowAccess = allowAccess;
	}

	/** See {@link #sendInvitation}. */
	public boolean getSendInvitation() {
		return sendInvitation;
	}
	/** See {@link #sendInvitation}. */
	public void setSendInvitation(boolean sendInvitation) {
		this.sendInvitation = sendInvitation;
	}

	private ProjectDAO getProjectDAO() {
		if (projectDAO == null) {
			projectDAO = ProjectDAO.getInstance();
		}
		return projectDAO;
	}

}
