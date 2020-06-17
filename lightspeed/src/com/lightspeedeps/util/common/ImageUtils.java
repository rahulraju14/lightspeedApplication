/**
 * File: ImageUtils.java
 */
package com.lightspeedeps.util.common;

import java.util.ArrayList;
import java.util.List;

import org.icefaces.ace.util.IceOutputResource;

import com.lightspeedeps.model.Image;
import com.lightspeedeps.object.ImageResource;
import com.lightspeedeps.util.app.EventUtils;

/**
 * A class for static image utilities.
 */
public class ImageUtils {

	private ImageUtils() {
		// private constructor to prevent instantiation.
	}

	/**
	 * Create a List of ImageResource's from a list of Image objects.
	 * @param images existing list of Image objects
	 * @return List of ImageResource's, one per Image
	 */
	public static List<IceOutputResource> createImageResources(List<Image> images) {
		List<IceOutputResource> imageResources = new ArrayList<>();
		ImageResource imageResource = null;

		for (Image image : images) {
			try {
				String idstr;
				if (image.getId() == null) {
					idstr = "" + (0-(imageResources.size()+1));
				}
				else {
					idstr = image.getId().toString();
				}
				imageResource = new ImageResource(idstr, image.getThumbnailB(), "image/png");
				imageResource.setId(image.getId());
				imageResource.setTitle(image.getTitle());
				imageResource.setImage(image);
			}
			catch (Exception e) {
				EventUtils.logError(e);
			}
			if (imageResource != null) {
				imageResources.add(imageResource);
				imageResource = null;
			}
		}
		return imageResources;
	}

}
