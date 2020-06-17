/**
 * File: ImageManager.java
 */
package com.lightspeedeps.web.image;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Image;
import com.lightspeedeps.util.app.EventUtils;

/**
 * A class that helps to manage a set of images. In turn, it uses the
 * {@link ImagePaginatorBean} class to do deletes, and the {@link ImageAddBean}
 * class to load new image files. It is somewhat similar in responsibilities to
 * the {@link com.lightspeedeps.web.view.ListImageView ListImageView} class,
 * except that that class is embedded in the
 * {@link com.lightspeedeps.web.view.ListView ListView} type hierarchy, and its
 * functions are designed to be used by its subclasses.
 */
public class ImageManager implements ImageHolder, Serializable {
	/** */
	private static final long serialVersionUID = - 7333325106312128901L;

	private static final Log log = LogFactory.getLog(ImageManager.class);

	/** Usually the object that instantiated this instance.  It may be
	 * used for callbacks. */
	private final ImageManage managed;

	/** The name of the item associated with the image(s) being uploaded.
	 * This is usually substituted in one or more messages displayed. */
	private String elementName;

	/** Complete list of images being managed. */
	private List<Image> imageList;

	/** Images that have been added but not saved; managed by ImageAddBean. */
	protected Set<Image> addedImages;

	/** The backing bean for the Add Image dialog. */
	private ImageAddBean imageAddBean;

	/** The backing bean which handles deleting images. */
	private final ImagePaginatorBean imagePaginatorBean;

	/** The message id in our message resource file of the message to be
	 * generated when an image has been successfully uploaded. Subclasses may
	 * change this, or set it to null to suppress the upload message. */
	private String imageAddedMessageId = "Image.FileUploaded";

	public ImageManager(ImageManage managee) {
		log.debug("");
		managed = managee;
		imageList = new ArrayList<Image>();

		imagePaginatorBean = ImagePaginatorBean.getInstance();
		imagePaginatorBean.setImageHolder(this);
		imagePaginatorBean.setAutoCommit(false);
	}

	/**
	 * Release image-related resources -- we clear the 'addedImages' collection,
	 * and our imageAddBean reference.
	 */
	public void resetImages() {
		setAddedImages(null);
		setImageAddBean(null);
	}

	/**
	 * The method called to open the "add image" dialog.
	 *
	 * @param msgid The message id that will be used for the "prompt" message
	 *            within the Add Image dialog box.
	 * @return null navigation string
	 */
	public String actionOpenNewImage(String msgid, boolean allowPdf) {
		String res = null;
		try {
			if (imageAddBean == null) {
				imageAddBean = ImageAddBean.getInstance();
			}
			imageAddBean.setAutoCommit(false);
			imageAddBean.setForMap(false);
			imageAddBean.setAddedMessageId(imageAddedMessageId);
			imageAddBean.setPromptId(msgid);
			res = imageAddBean.actionOpenNewImage(this, getElementName(), allowPdf);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return res;
	}

	/**
	 * Callback from ImagePaginatorBean.
	 */
	@Override
	public void removeImage(Image image) {
		try {
			boolean rem = getImageList().remove(image);
			log.debug("image="+image+", removed="+rem);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Callback from ImageAddBean, when an image has been successfully
	 * uploaded and stored in the database.
	 *
	 * @see com.lightspeedeps.web.image.ImageHolder#updateImage(com.lightspeedeps.model.Image, java.lang.String)
	 */
	@Override
	public void updateImage(Image image, String filename) {
		log.debug("");
		try {
			if (image != null) {
				getImageList().add(image);
				if (managed != null) {
					managed.updateImage(image, filename);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	public void commitImages() {
		imagePaginatorBean.commit();
	}

	/**
	 * Standard activities for all subclasses when a Save completes. In
	 * particular, we release any image-related resources. This method should be
	 * called by our owner if Save/Cancel protocol is in use.
	 */
	public void actionSave() {
		resetImages();
	}

	/**
	 * The Action method for Cancel button while in Edit mode. This
	 * discards any images that might have been uploaded during the
	 * current edit cycle.  This method should be called by our
	 * owner if Save/Cancel protocol is in use.
	 * @return null navigation string
	 */
	public String actionCancel() {
		log.debug("");
		try {
			imagePaginatorBean.rollback();
			ImageAddBean.rollback(this);
			resetImages();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	/** Provide access to hasChanges() method of paginator bean. */
	public boolean hasImageChanges() {
		return imagePaginatorBean.hasChanges();
	}

	/** Provides implementation required by ImageHolder interface. */
	@Override
	public void addEndingJavascript() {
	}

	/** See {@link #elementName}. */
	public String getElementName() {
		return elementName;
	}
	/** See {@link #elementName}. */
	public void setElementName(String elementName) {
		this.elementName = elementName;
	}


	@Override
	public List<Image> getImageList() {
		return imageList;
	}
	public void setImageList(List<Image> list) {
		imageList = list;
	}

	/** See {@link #addedImages}. */
	@Override
	public Set<Image> getAddedImages() {
		return addedImages;
	}
	/** See {@link #addedImages}. */
	@Override
	public void setAddedImages(Set<Image> addedImages) {
		this.addedImages = addedImages;
	}

	/** See {@link #imageAddBean}. */
	public ImageAddBean getImageAddBean() {
		return imageAddBean;
	}
	/** See {@link #imageAddBean}. */
	public void setImageAddBean(ImageAddBean imageAddBean) {
		this.imageAddBean = imageAddBean;
	}

	/** See {@link #imageAddedMessageId}. */
	public String getImageAddedMessageId() {
		return imageAddedMessageId;
	}
	/** See {@link #imageAddedMessageId}. */
	public void setImageAddedMessageId(String imageAddedMessageId) {
		this.imageAddedMessageId = imageAddedMessageId;
	}

}
