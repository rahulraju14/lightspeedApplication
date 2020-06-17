/** File: ImageManage.java */
package com.lightspeedeps.web.image;

import com.lightspeedeps.model.Image;

/**
 * An interface which must be implemented by any class that uses the
 * ImageManager class to manage (e.g., upload) image files. See, for example,
 * FullTimecardBean.
 */
public interface ImageManage {

	/**
	 * A method called by ImageManager when an Image has been successfully
	 * stored in the database, and may need to be associated with an object
	 * managed by the holding class. No action is required.
	 *
	 * @param image The newly-stored Image object.
	 * @param filename The original filename supplied by the upload servlet.
	 */
	public void updateImage(Image image, String filename);

}
