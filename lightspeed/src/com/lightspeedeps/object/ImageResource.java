package com.lightspeedeps.object;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.util.IceOutputResource;

import com.lightspeedeps.model.Image;

/**
 * Wrapper class for using the IceOutputResource object to display images.
 * IceOutputResource takes the byte[] of the image to create the resource.
 * This class is used for the upgrade to Icefaces 4.x. It fixes the problem
 * when clicking on an object that display an image and then clicking on another
 * of the same object and not having the image change.
 */
public class ImageResource extends IceOutputResource {
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(ImageResource.class);

	/** The id of the associated image */
	private Integer id;

	/** Title of the image */
	private String title;

	/** The image this wrapper is holding */
	private Image image;

	/**
	 * @param name
	 * @param image
	 * @param mimeType
	 */
	public ImageResource(String name, Object image, String mimeType) {
		super(name, image, mimeType);
	}

	/** See {@link #id}. */
	public Integer getId() {
		return id;
	}

	/** See {@link #id}. */
	public void setId(Integer id) {
		this.id = id;
	}

	/** See {@link #title}. */
	public String getTitle() {
		return title;
	}

	/** See {@link #title}. */
	public void setTitle(String title) {
		this.title = title;
	}

	/** See {@link #image}. */
	public Image getImage() {
		return image;
	}

	/** See {@link #image}. */
	public void setImage(Image image) {
		this.image = image;
	}

}
