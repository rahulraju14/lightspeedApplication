package com.lightspeedeps.web.image;

import java.util.List;
import java.util.Set;

import com.lightspeedeps.model.Image;

/**
 * An interface which must be implemented by any class that uses the
 * {@link ImageAddBean} or {@link ImagePaginatorBean} classes to manage (e.g.,
 * upload) image files. See, for example, {@link com.lightspeedeps.web.view.ListImageView ListImageView}.
 */
public interface ImageHolder {

	/** A method called by ImageAddBean when an Image has been successfully
	 * stored in the database, and may need to be associated with an object
	 * (e.g., ScriptElement) managed by the holding class. No action is
	 * required.
	 * @param image The newly-stored Image object.
	 * @param filename The original filename supplied by the upload servlet.
	 */
	public void updateImage(Image image, String filename);

	/** A method called by ImageAddBean when an image is being removed
	 * by the user.  This gives the holder an opportunity to update the
	 * object that contains the set of images, such as a Contact or ScriptElement.
	 * No action is required. */
	public void removeImage(Image image);

	/** This method must return the holder's current List of associated Image objects. */
	public List<Image> getImageList();

	/** This method must return the holder's set of Image objects that have been added
	 * during the current edit cycle. This set is maintained by ImageAddBean, and
	 * will be initialized by a call to setAddedImages(). */
	public Set<Image> getAddedImages();

	/**
	 * This method is called by ImageAddBean to provide the holder with the
	 * Set of Images added during the current edit cycle.  The Set must be
	 * 'remembered' by the holder, and returned when getAddedImages() is called.
	 * @param addedImages A non-null, but possibly empty, Set of Image objects.
	 */
	public void setAddedImages(Set<Image> addedImages);

	/**
	 * A method to add whatever JavaScript is required to run after the Add
	 * Image dialog box is closed. This typically calls the
	 * {@link com.lightspeedeps.web.view.ListView ListView} addClientResize or
	 * addClientResizeScroll methods.
	 */
	public void addEndingJavascript();

}
