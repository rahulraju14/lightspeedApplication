package com.lightspeedeps.web.onboard.form;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.service.DocumentTransferService;
import com.lightspeedeps.type.OfficeType;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.web.report.ReportBean;

/**
 * Backing bean for the FormActraPermit form on the Payroll / Start Forms page.
 *
 * @see com.lightspeedeps.model.FormActraIntent
 * @see StandardFormBean
 */
@ManagedBean
@ViewScoped
public class FormActraIntentBean extends StandardFormBean<FormActraIntent> implements Serializable  {

	private static final long serialVersionUID = 7503045267054895325L;

	private static final Log LOG = LogFactory.getLog(FormActraIntentBean.class);

	private transient FormActraIntentDAO formActraIntentDAO;

	/** Boolean array used for the selection of Multi-Branch radio buttons. */
	private Boolean multiBranch[] = new Boolean[2];

	/** Boolean array used for the selection of Minor radio buttons. */
	private Boolean minor[] = new Boolean[2];

	/** Boolean array used for the selection of Stunts radio buttons. */
	private Boolean stunts[] = new Boolean[2];

	/** Boolean array used for the selection of Exterior Scenes radio buttons. */
	private Boolean extScenes[] = new Boolean[2];

	/** Boolean array used for the selection of Location Shoot - beyond 40 km radius radio buttons. */
	private Boolean locationShoot40Radius[] = new Boolean[2];

	/** Boolean array used for the selection of Weather Permitting radio buttons. */
	private Boolean weatherPermitting[] = new Boolean[2];

	/** Boolean array used for the selection of Weekend or Night Production radio buttons. */
	private Boolean weekendNight[] = new Boolean[2];

	/** Boolean array used for the selection of Nude Scenes radio buttons. */
	private Boolean nudeScenes[] = new Boolean[2];

	private static final Integer INITIAL_ROW_COUNT = 8;

	private Integer rowCount = INITIAL_ROW_COUNT;

	private List<Talent> talentList = null;

	private static final Integer ADD_ROW_COUNT = 50;

	private Integer talentToRemoveIndex;

	private Project selectedProject;

	private String transferStatus;

	/** Transfer status of an unsent document/Intent form. */
	private final static String TRANSFER_STATUS_1 = "Form has not been transferred.";

	/** Transfer status of a sent document/Intent form. */
	private final static String TRANSFER_STATUS_2 = "Form was last transferred at ";

	public FormActraIntentBean() {
		super("FormActraIntentBean.");
		LOG.debug("");
		resetBooleans();
		getTransferStatus();
	}

	public static FormActraIntentBean getInstance() {
		return (FormActraIntentBean) ServiceFinder.findBean("formActraIntentBean");
	}

	/**
	 * Set all our backing boolean arrays to all False.
	 */
	private void resetBooleans() {
		Arrays.fill(multiBranch, null);
		Arrays.fill(minor, null);
		Arrays.fill(stunts, null);
		Arrays.fill(extScenes, null);
		Arrays.fill(locationShoot40Radius, null);
		Arrays.fill(weatherPermitting, null);
		Arrays.fill(weekendNight, null);
		Arrays.fill(nudeScenes, null);
	}

	/** Method used to fetch the saved data for the selected form and to set that data in the Form instance. */
	public void setUpForm() {
		try {
			Integer relatedFormId = contactDoc.getRelatedFormId();
			if(relatedFormId == null) {
				LOG.debug("");
				setForm(getBlankForm());
			}
			else {
				LOG.debug("");
				setForm(getFormActraIntentDAO().findById(relatedFormId));
				forceLazyInit();
			}
			setTalentList(null);
			setUpBooleans();
			getTransferStatus();
			if (form.getCommercial1() == null) {
				form.setCommercial1(new Commercial());
			}
			if (form.getCommercial2() == null) {
				form.setCommercial2(new Commercial());
			}
			if (form.getCommercial3() == null) {
				form.setCommercial3(new Commercial());
			}
			if (relatedFormId == null) {
				getFormActraIntentDAO().save(form);
				contactDoc.setRelatedFormId(form.getId());
				ContactDocumentDAO.getInstance().merge(contactDoc);
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
	}

	private void forceLazyInit() {
		Commercial com1 = form.getCommercial1();
		if (com1 != null) {
			com1.getName();
		}
		Commercial com2 = form.getCommercial2();
		if (com2 != null) {
			com2.getName();
		}
		Commercial com3 = form.getCommercial3();
		if (com3 != null) {
			com3.getName();
		}
	}

	/** Fetches the stored values of the radio buttons for the form. */
	private void setUpBooleans() {
		if (form != null) {
			fillArray(multiBranch, form.getMultiBranch());
			fillArray(minor, form.getMinor());
			fillArray(stunts, form.getStunts());
			fillArray(extScenes, form.getExtScenes());
			fillArray(locationShoot40Radius, form.getLocationShoot40Radius());
			fillArray(weatherPermitting, form.getWeatherPermitting());
			fillArray(weekendNight, form.getWeekendNight());
			fillArray(nudeScenes, form.getNudeScenes());
		}
	}

	/** Utility method used to set the value of radio button for the form field.
	 * @param booleanArray Array of Boolean used for the Form field.
	 * @param formField
	 */
	private void fillArray(Boolean[] booleanArray, Boolean formField) {
		if (formField == null) {
			booleanArray[0] = false;
			booleanArray[1] = false;
		}
		else if (formField) {
			booleanArray[0] = true;
			booleanArray[1] = false;
		}
		else if (! formField) {
			booleanArray[0] = false;
			booleanArray[1] = true;
		}
	}

	@Override
	public String actionEdit() {
		if (form.getCommercial1() == null) {
			form.setCommercial1(new Commercial());
		}
		if (form.getCommercial2() == null) {
			form.setCommercial2(new Commercial());
		}
		if (form.getCommercial3() == null) {
			form.setCommercial3(new Commercial());
		}
		super.actionEdit();
		return null;
	}

	@Override
	public String actionSave() {
		form.setMultiBranch(setChosenValue(multiBranch));
		form.setMinor(setChosenValue(minor));
		form.setStunts(setChosenValue(stunts));
		form.setExtScenes(setChosenValue(extScenes));
		form.setLocationShoot40Radius(setChosenValue(locationShoot40Radius));
		form.setWeatherPermitting(setChosenValue(weatherPermitting));
		form.setWeekendNight(setChosenValue(weekendNight));
		form.setNudeScenes(setChosenValue(nudeScenes));

		if (form.getTalent() != null && (! form.getTalent().isEmpty())) {
			Iterator<Talent> itr = form.getTalent().iterator();
			while (itr.hasNext()) {
				Talent oldTalent = itr.next();
				if (getTalentList().contains(oldTalent)) {
					continue;
				}
				else {
					LOG.debug("Removed Talent = " + oldTalent.getName());
					itr.remove();
					TalentDAO.getInstance().delete(oldTalent);
				}
			}
		}

		List<Talent> talList = form.getTalent();
		for (Talent newTalent : getTalentList()) {
			if (talList != null && (! talList.contains(newTalent))) {
				LOG.debug("Talent = " + newTalent.getName());
				form.getTalent().add(newTalent);
			}
		}
		form = getFormActraIntentDAO().merge(form);
		setEditMode(false);
		return null;
	}

	@Override
	public String actionCancel() {
		LOG.debug("");
		setEditMode(false);
		setRowCount(INITIAL_ROW_COUNT);
		refreshForm();
		setTalentList(null);
		getTalentList();
		setUpBooleans();
		return null;
	}

	/** Sets the value of boolean fields chosen from their given radio buttons on the form.
	 * @param radioButton Boolean Array used for the field.
	 * @return
	 */
	private Boolean setChosenValue(Boolean[] radioButton) {
		if (radioButton != null) {
			if (radioButton[0]) {
				return true;
			}
			else if (radioButton[1]) {
				return false;
			}
		}
		// If none of them is selected.
		return null;
	}

	private FormActraIntentDAO getFormActraIntentDAO() {
		if (formActraIntentDAO == null) {
			formActraIntentDAO = FormActraIntentDAO.getInstance();
		}
		return formActraIntentDAO;
	}

	@Override
	public void rowClicked(ContactDocument contactDocument) {
		LOG.debug("FormActraIntentBean rowClicked");
		setContactDoc(contactDocument);
		setUpForm();
	}

	@Override
	public FormActraIntent getFormById(Integer id) {
		return getFormActraIntentDAO().findById(id);
	}

	@Override
	public FormActraIntent getBlankForm() {
		form = new FormActraIntent();
		return form;
	}

	@Override
	public void refreshForm() {
		if (form != null) {
			form = getFormActraIntentDAO().refresh(form);
			forceLazyInit(); // Fix LIE. DH 4.14.0
			LOG.debug("");
		}
	}

	public String actionAddMoreLines() {
		try {
			setRowCount(rowCount + ADD_ROW_COUNT);
			addRemoveTalentEntries(getRowCount(), false);
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	public String actionRemoveLines() {
		try {
			LOG.debug("Talent To Remove Index = " + getTalentToRemoveIndex());
			if (getTalentToRemoveIndex() != null) {
				LOG.debug("");
				int i = 0;
				Iterator<Talent> itr = getTalentList().iterator();
				while (itr.hasNext() && i <= getTalentList().size()) {
					itr.next();
					if (i == getTalentToRemoveIndex()) {
						itr.remove();
						break;
					}
					i++;
				}
				setRowCount(rowCount-1);
			}
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/*@Override
	public String confirmOk(int action) {
		switch (action) {
		case ACT_ADD:
			actionAddMoreLinesOk();
			break;
		case ACT_REMOVE:
			actionRemoveLinesOk();
			break;
		default:
			super.confirmOk(action);
		}
		return super.confirmOk(action);
	}

	public String actionAddMoreLinesOk() {
		try {
			PopupInputBean bean = PopupInputBean.getInstance();
			Integer newCount = Integer.parseInt(bean.getInput());
			LOG.debug("newCount = " + newCount);
			setRowCount(getRowCount() + newCount);
			addRemoveTalentEntries(newCount, false);
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	public String actionRemoveLinesOk() {
		try {
			PopupInputBean bean = PopupInputBean.getInstance();
			Integer newCount = Integer.parseInt(bean.getInput());
			LOG.debug("newCount = " + newCount);
			//setRowCount(getRowCount() - newCount);
			addRemoveTalentEntries(newCount, true);
		}
		catch(Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}*/

	private List<Talent> createTalentList() {
		if (getForm() != null) {
			LOG.debug("");
			talentList = new ArrayList<>();
			addRemoveTalentEntries(getRowCount(), false);
			refreshForm();
			if (getForm().getTalent() != null && (! getForm().getTalent().isEmpty())) {
				int i = 0;
				for (Talent t : getForm().getTalent()) {
					if (i >= getTalentList().size()) {
						talentList.add(t);
						setRowCount(getRowCount() + 1);
					}
					else {
						talentList.set(i, t);
					}
					i++;
				}
			}
		}
		return talentList;
	}

	/** Method used to add blank talent entries and
	 * to remove the selected talent entries from the talent table.
	 * @param count Number of rows to add/remove
	 * @param isRemove true, if the method is called for the remove action.
	 */
	private void addRemoveTalentEntries(Integer count, boolean isRemove) {
		if (isRemove) {
			LOG.debug("Row Count = " + getTalentList().size());
			Integer removeAfterIndex = (getTalentList().size() - count);
			LOG.debug("Remove After Index = " + removeAfterIndex);
			Iterator<Talent> itr = getTalentList().iterator();
			int i = 1;
			while (itr.hasNext()) {
				itr.next();
				if (i > removeAfterIndex) {
					LOG.debug("Deleted row " + i);
					itr.remove();
				}
				i++;
			}
			setRowCount(rowCount-count);
		}
		else {
			Talent talent;
			for (int i = 0; i < count; i++) {
				talent = new Talent();
				talent.setFormActraIntent(getForm());
				talentList.add(talent);
			}
		}
	}

	/**
	 * Action method for the "Print" button on the Projects mini-tab.
	 *
	 * @return null navigation string
	 */
	@Override
	public String actionPrint() {
		try {
			if (contactDoc != null) {
				LOG.debug("");
				Integer id = contactDoc.getRelatedFormId();
				FormActraIntent formActraIntent = null;
				if (id != null) {
					formActraIntent = FormActraIntentDAO.getInstance().findById(id);
				}
				else {
					formActraIntent = new FormActraIntent();
					formActraIntent.setId(0); // this retrieves blank Indemnification record in database!
				}
				if (formActraIntent != null) {
					ReportBean report = ReportBean.getInstance();
					// Report was not opening in the new tab after transfer action.
					report.setOpenReportPopupWindow(true);
					formActraIntent = FormActraIntentDAO.getInstance().refresh(formActraIntent);
					report.generateFormActraIntent(formActraIntent, null);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	// No longer used for category - LS-1987
	/**
	 * @return List of Talent Categories.
	 */
	/*public List<SelectItem> getTalentCategoryList() {
		List<SelectItem> categoryList = new ArrayList<>();
		for (TalentCategoryType type : TalentCategoryType.values()) {
			categoryList.add(new SelectItem(null, ""));
			categoryList.add(new SelectItem(type, type.toString()));
		}
		return categoryList;
	}*/

	/**
	 * @return SelectItem list of ACTRA Offices.
	 */
	public List<SelectItem> getActraOfficeList() {
		List<SelectItem> officeList = OfficeDAO.getInstance().createOfficeSelectList(OfficeType.ACTRA);
		if (officeList == null) {
			officeList = new ArrayList<>();
		}
		return officeList;
	}

	/**
	 * Set the selected office.LS-3052
	 *
	 * @param event
	 */
	public void listenOfficeSelectChange(ValueChangeEvent event) {
		Integer officeId = (Integer)event.getNewValue();
		form.setOfficeId(officeId);
		formActraIntentDAO.attachDirty(form);
	}

	/**
	 * Method called for "Send" button on Intent to Produce form. Transfers
	 * printed copy (PDF) of the form to the currently selected office.
	 *
	 * @return null navigation string
	 */
	public String actionTransferToActraOffice() {
		LOG.debug("");
		if (form.getOfficeId() != null && form.getOfficeId() != 0) {
			ContactDocument cd = getContactDocumentDAO().refresh(contactDoc);
			if (cd != null) {
				DocumentTransferService.transferDocumentToOffice(cd, form.getOfficeId(),
						cd.getProject());
				cd.setLastSent(new Date());
				getContactDocumentDAO().merge(cd);
				setContactDoc(cd);
			}
		}
		// LS-2985
		else {
			MsgUtils.addFacesMessage("FormActraIntent.NoOfficeSelected.Error",
					FacesMessage.SEVERITY_ERROR);
		}
		return null;
	}

	/**
	 * Method to clear all fields from intent to produce form LS-2070
	 *
	 * @return
	 */
	public String actionClear() {
		FormActraIntent form = getForm();
		if (form != null) {
			form.clearFields();
			resetBooleans();
			setTalentList(null);
		}
		return null;
	}

	/**See {@link #multiBranch}. */
	public Boolean[] getMultiBranch() {
		return multiBranch;
	}
	/**See {@link #multiBranch}. */
	public void setMultiBranch(Boolean[] multiBranch) {
		this.multiBranch = multiBranch;
	}

	/**See {@link #minor}. */
	public Boolean[] getMinor() {
		return minor;
	}
	/**See {@link #minor}. */
	public void setMinor(Boolean[] minor) {
		this.minor = minor;
	}

	/**See {@link #stunts}. */
	public Boolean[] getStunts() {
		return stunts;
	}
	/**See {@link #stunts}. */
	public void setStunts(Boolean[] stunts) {
		this.stunts = stunts;
	}

	/**See {@link #extScenes}. */
	public Boolean[] getExtScenes() {
		return extScenes;
	}
	/**See {@link #extScenes}. */
	public void setExtScenes(Boolean[] extScenes) {
		this.extScenes = extScenes;
	}

	/**See {@link #locationShoot40Radius}. */
	public Boolean[] getLocationShoot40Radius() {
		return locationShoot40Radius;
	}
	/**See {@link #locationShoot40Radius}. */
	public void setLocationShoot40Radius(Boolean[] locationShoot40Radius) {
		this.locationShoot40Radius = locationShoot40Radius;
	}

	/**See {@link #weatherPermitting}. */
	public Boolean[] getWeatherPermitting() {
		return weatherPermitting;
	}
	/**See {@link #weatherPermitting}. */
	public void setWeatherPermitting(Boolean[] weatherPermitting) {
		this.weatherPermitting = weatherPermitting;
	}

	/**See {@link #weekendNight}. */
	public Boolean[] getWeekendNight() {
		return weekendNight;
	}
	/**See {@link #weekendNight}. */
	public void setWeekendNight(Boolean[] weekendNight) {
		this.weekendNight = weekendNight;
	}

	/**See {@link #nudeScenes}. */
	public Boolean[] getNudeScenes() {
		return nudeScenes;
	}
	/**See {@link #nudeScenes}. */
	public void setNudeScenes(Boolean[] nudeScenes) {
		this.nudeScenes = nudeScenes;
	}

	/**See {@link #rowCount}. */
	public Integer getRowCount() {
		return rowCount;
	}
	/**See {@link #rowCount}. */
	public void setRowCount(Integer rowCount) {
		this.rowCount = rowCount;
	}

	/**See {@link #talentList}. */
	public List<Talent> getTalentList() {
		if (talentList == null) {
			LOG.debug("");
			talentList = createTalentList();
		}
		LOG.debug("");
		return talentList;
	}
	/**See {@link #talentList}. */
	public void setTalentList(List<Talent> talentList) {
		this.talentList = talentList;
	}

	/**See {@link #talentToRemoveIndex}. */
	public Integer getTalentToRemoveIndex() {
		return talentToRemoveIndex;
	}
	/**See {@link #talentToRemoveIndex}. */
	public void setTalentToRemoveIndex(Integer talentToRemoveIndex) {
		this.talentToRemoveIndex = talentToRemoveIndex;
	}
	/**See {@link #selectedProject}. */
	public Project getSelectedProject() {
		return selectedProject;
	}
	/**See {@link #selectedProject}. */
	public void setSelectedProject(Project selectedProject) {
		this.selectedProject = selectedProject;
	}

	/**See {@link #transferStatus}. */
	public String getTransferStatus() {
		transferStatus = TRANSFER_STATUS_1;
		if (contactDoc != null && contactDoc.getLastSent() != null) {
			DateFormat df = new SimpleDateFormat("h:mm a 'on' M/d/yyyy");
			String timestamp = df.format(contactDoc.getLastSent());
			transferStatus = TRANSFER_STATUS_2 + timestamp;
		}
		return transferStatus;
	}

}
