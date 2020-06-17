/**
 * File: ImageView.java
 */
package com.lightspeedeps.web.view;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.Image;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.web.image.ImageAddBean;
import com.lightspeedeps.web.image.ImageHolder;
import com.lightspeedeps.web.image.ImagePaginatorBean;

/**
 * A superclass for classes that have a detail view, and includes support for a
 * set of images associated with the current element. (Classes which also
 * support displaying a list of elements should subclass
 * {@link ListImageView} or {@link ListView} instead of this class.)
 * <p>
 * Much of the image management functionality is a result of this class having a
 * {@link ImagePaginatorBean}, and implementing the {@link ImageHolder}
 * interface.
 * <p>
 * Note that a lot of this code duplicates code in the {@link ListImageView}
 * class; I didn't see any good way around that.
 */
public abstract class ImageView extends View implements ImageHolder {
	/** */
	private static final long serialVersionUID = - 7632120907784495333L;

	private static final Log log = LogFactory.getLog(ImageView.class);

	/** Images that have been added but not saved; managed by {@link ImageAddBean}. */
	protected Set<Image> addedImages;

	/** The backing bean for the Add Image dialog. */
	private ImageAddBean imageAddBean;

	/** The backing bean which handles deleting images. */
	private final ImagePaginatorBean imagePaginatorBean;

	public abstract String getElementName();

	public ImageView(String prefix) {
		super(prefix);
		log.debug("");

		imagePaginatorBean = ImagePaginatorBean.getInstance();
		imagePaginatorBean.setImageHolder(this);
		imagePaginatorBean.setAutoCommit(false);
	}

	/**
	 * Release image-related resources -- we clear the 'addedImages' collection,
	 * and our imageAddBean reference.
	 */
	protected void resetImages() {
		setAddedImages(null);
		setImageAddBean(null);
	}

	/**
	 * Action method for the "Add Image" or "New Image" button; opens
	 * the "Add Image" dialog box.
	 *
	 * @return null navigation string
	 */
	public String actionOpenNewImage() {
		String res = null;
		try {
			if (imageAddBean == null) {
				imageAddBean = ImageAddBean.getInstance();
			}
			imageAddBean.setAutoCommit(false);
			imageAddBean.setForMap(false);
			res = imageAddBean.actionOpenNewImage(this, getElementName());
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

	@Override
	public void updateImage(Image image, String filename) {
		log.debug("");
		try {
			if (image != null) {
				getImageList().add(image);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	protected void commitImages() {
		imagePaginatorBean.commit();
	}

	/**
	 * Standard activities for all subclasses when a Save completes.
	 * In particular, we release any image-related resources.
	 * @return superclass' return value
	 */
	@Override
	public String actionSave() {
		resetImages();
		return super.actionSave();
	}

	/**
	 * The Action method for Cancel button while in Edit mode. Cleans up the
	 * state of any images added or removed, and calls our superclass'
	 * actionCancel() method.
	 *
	 * @return null navigation string
	 */
	@Override
	public String actionCancel() {
		log.debug("");
		try {
			super.actionCancel();
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
		ListView.addClientResizeScroll();
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

}
