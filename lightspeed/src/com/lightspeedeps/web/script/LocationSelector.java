//	File Name:	LocationSelector.java
package com.lightspeedeps.web.script;

import java.io.Serializable;
import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.SceneDAO;
import com.lightspeedeps.dao.ScriptElementDAO;
import com.lightspeedeps.dood.ProductionDood;
import com.lightspeedeps.model.*;
import com.lightspeedeps.type.ScriptElementType;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;

/**
 * This class manages a Location ("Set") drop-down list.  It allows the
 * user to create new Locations, or select from a list of existing locations.
 * It is used by the Breakdown Page Edit screen, and may be used in the future by the
 * Script Review screen(s) in the script import process.
 */
@ManagedBean
@ViewScoped
public class LocationSelector implements Serializable {
	/** */
	private static final long serialVersionUID = - 1429335523735767155L;

	private static final Log log = LogFactory.getLog(LocationSelector.class);

	private final Project project;

	/** If true, location changes are saved immediately; if false, the change is made to
	 * the scene object, but it is not saved to the database. */
	private boolean autosave = true;

	/**
	 * The reference to our "owner", used to call back for
	 * certain operations. See the LocationHolder interface.
	 */
	private LocationHolder locationHolder;

	/** If true, display the "New Location" pop-up dialog box. */
	private boolean showNewLocation = false;

	private String newLocationName;

	/**
	 * The location Id is the controlling value (input and output) for
	 * the location drop-down list.
	 */
	private Integer locationId;

	/**
	 * The contents of the location ("Set") drop-down list.
	 */
	private List<SelectItem> locationList;


	public LocationSelector() {
		project = SessionUtils.getCurrentProject();
		//setLocationList(createLocationList());
	}

/*	public LocationSelector(LocationHolder lh, ScriptElementDAO seDAO, SceneDAO pSceneDAO) {
		log.debug(""+this.hashCode());
		locationHolder = lh;
		scriptElementDAO = seDAO;
		sceneDAO = pSceneDAO;
		project = UtilHelper.getCurrentProject();
		setLocationList(createLocationList());
	}
*/
	/**
	 * Called when a new location (Set) value has been selected from the drop-down
	 * list, on the Script Review page or Breakdown Edit page.
	 * @param event The ValueChangeEvent created by JSF.  The new value it
	 * contains is the "value" (not displayed "label") of the SelectItem chosen
	 * by the user.  In our case, this is the ScriptElement.id value.
	 */
	public void actionLocationSelected(ValueChangeEvent event) {
		if (event.getNewValue()==null) {
			return;
		}
		String location = event.getNewValue().toString().toUpperCase().trim();
		log.debug("location id=`"+location+"`");
		int locationId = -1;
		try {
			locationId = Integer.parseInt(location);
		}
		catch (NumberFormatException e) {
		}
		if (locationId > 0 ) {
			ScriptElement locationElement = ScriptElementDAO.getInstance().findById(locationId);
			if (locationElement != null) {
				if (getScene() != null) {
					getScene().setScriptElement(locationElement);
					if (autosave) {
						SceneDAO.getInstance().attachDirty(getScene());
					}
					else {
						//getScene().setDirty(true);
					}
					log.debug("loc id="+locationElement.getId()+", scene id="+getScene().getId());
					if (locationHolder != null) {
						locationHolder.locationUpdated();
					}
					// Mark DooD info dirty, but do not update ScriptElement drop/hold info
					ProductionDood.markProjectDirty(SessionUtils.getCurrentProject(), false);
				}
			}
		}
		else if (locationId == 0) { // "New location" selected
			saveUpdates(); // save any pending edits (else would be lost if user cancels New Location dialog)
			openNewLocation();
			// sceneView.setLocationId(Integer.parseInt(event.getOldValue().toString()));
		}
	}

	/**
	 * Build a List of SelectItems from the Location type ScriptElements in this project.
	 * @return the List as described above.
	 */
	private List<SelectItem> createLocationList() {
		return createLocationList(ScriptElementDAO.getInstance(), project);
	}

	/**
	 * Build a List of SelectItems from the Location type ScriptElements in this project.
	 * @return non-null List
	 */
	public static List<SelectItem> createLocationList(ScriptElementDAO scriptElementDAO, Project project) {
		SelectItem locItem;
		List<ScriptElement> scriptElements = scriptElementDAO.findByTypeAndProject(ScriptElementType.LOCATION, project);
		List<SelectItem> locations = new ArrayList<SelectItem>();

		locItem = new SelectItem(new Integer(-1), MsgUtils.getMessage("ImportScript.SetLocationPrompt"));
		locations.add(locItem);
		locItem = new SelectItem(new Integer(0), MsgUtils.getMessage("ImportScript.NewLocationPrompt"));
		locations.add(locItem);

		for (ScriptElement element : scriptElements) {
			locItem = new SelectItem(element.getId(), element.getName().toUpperCase());
			locations.add(locItem);
		}
		log.debug("list size: "+locations.size());
		return locations;
	}

	public void enterNewLocation(ActionEvent event) {
		log.debug("");
		saveNewLocation();
	}

	/**
	 * Called by the "Save" button on the "New Location" pop-up dialog.
	 * @return null navigation string
	 */
	public String saveNewLocation() {
		log.debug("scene id=" + getScene().getId());
		if (newLocationName == null) {
			newLocationName = "";
		}
		newLocationName = newLocationName.trim().toUpperCase();
		if (newLocationName.length()==0) {
			MsgUtils.addFacesMessage("ImportScript.BlankLocation",
					FacesMessage.SEVERITY_ERROR);
		}
		else {
			ScriptElementDAO scriptElementDAO = ScriptElementDAO.getInstance();
			ScriptElement loc = scriptElementDAO.findByNameTypeProject(newLocationName,
					ScriptElementType.LOCATION, project);
			if (loc != null) {
				MsgUtils.addFacesMessage("ImportScript.DuplicateLocation",
						FacesMessage.SEVERITY_ERROR);
			}
			else {
				loc = new ScriptElement();
				loc.setType(ScriptElementType.LOCATION);
				loc.setName(newLocationName);
				loc.setProject(SessionUtils.getCurrentProject());
				loc.setRealElementRequired(true);
				scriptElementDAO.save(loc);
				setLocationList(createLocationList()); // update drop-down list with new entry
				setLocationId(loc.getId());	// make new entry current one to display

				if (autosave) {
					SceneDAO sceneDAO = SceneDAO.getInstance();
					setScene( sceneDAO.findById(getScene().getId()) ); // get current one from db
					getScene().setScriptElement(loc);
					sceneDAO.merge(getScene());
				}
				else {
					getScene().setScriptElement(loc);
				}
				log.debug("loc added, id="+getScene().getScriptElement().getId()+", loc="+newLocationName);

				closeNewLocation();
			}
		}
		return null;
	}

	/**
	 * Called when user clicks "Cancel" on the "New Location" dialog.
	 * @return null navigation string
	 */
	public String cancelNewLocation() {
		setLocationId(-1);	// "select a location" prompt
		closeNewLocation();
		return null;
	}

	private void closeNewLocation() {
		setShowNewLocation(false);
		setNewLocationName("");
		if (locationHolder != null) {
			locationHolder.locationUpdated();
		}
//		ListView.addClientResize();
	}

	public void openNewLocation() {
		showNewLocation = true;
	}

	public boolean getShowNewLocation() {
		return showNewLocation;
	}
	public void setShowNewLocation(boolean newLocation) {
		showNewLocation = newLocation;
	}

	protected void saveUpdates() {
		log.debug(""+hashCode());
		if (locationHolder != null) {
			locationHolder.saveChanges();
		}
		if (autosave && getScene() != null && getScene().getId() != null) {
			SceneDAO.getInstance().attachDirty(getScene());
		}
	}

	// * * * Accessors & Mutators

	public List<SelectItem> getLocationList() {
		if (locationList == null) {
			locationList = createLocationList();
		}
		return locationList;
	}
	public void setLocationList(List<SelectItem> locationList) {
		this.locationList = locationList;
	}

	public Integer getLocationId() {
		return locationId;
	}
	public void setLocationId(Integer locationId) {
		this.locationId = locationId;
	}

	public String getNewLocationName() {
		return newLocationName;
	}
	public void setNewLocationName(String newLocationName) {
		log.debug("loc="+newLocationName);
		this.newLocationName = newLocationName;
	}

	public LocationHolder getLocationHolder() {
		return locationHolder;
	}
	public void setLocationHolder(LocationHolder locationHolder) {
		this.locationHolder = locationHolder;
	}

	public Scene getScene() {
		if (locationHolder != null) {
			return locationHolder.getScene();
		}
		return null;
	}
	public void setScene(Scene scene) {
		if (locationHolder != null) {
			locationHolder.updateScene(scene);
		}
	}

	/** See {@link #autosave}. */
	public boolean getAutosave() {
		return autosave;
	}
	/** See {@link #autosave}. */
	public void setAutosave(boolean autosave) {
		this.autosave = autosave;
	}

}
