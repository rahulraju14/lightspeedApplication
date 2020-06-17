package com.lightspeedeps.web.script;

import com.lightspeedeps.model.Scene;

/**
 * Defines the interface implemented by classes that use the LocationSelector class.
 *
 */
public interface LocationHolder {

	/**
	 * Get the current scene -- the one that the location is being changed upon.
	 */
	public Scene getScene();

	/**
	 * Update the current scene object.  This is done in case the LocationSelector
	 * needs to refresh the object from the database.
	 */
	public void updateScene(Scene scene);

	/**
	 * Called when the LocationSelector has updated the location in the
	 * current Scene.  The holder may need to update the screen presentation.
	 */
	public void locationUpdated();

	/**
	 * Called just before the LocationSelector is going to present
	 * the "New Location" dialog.  If the user cancels this dialog, the
	 * page is refreshed, and any pending changes are lost.  So this call
	 * gives the holder a chance to save any pending changes.
	 */
	public void saveChanges();

}
