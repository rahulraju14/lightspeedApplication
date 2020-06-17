package com.lightspeedeps.web.image;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.AjaxBehaviorEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ImageDAO;
import com.lightspeedeps.model.Image;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.web.view.ListView;

/**
 * The backing bean for the "large image" pop-up window, used for displaying
 * full-size versions of images from contacts, elements, etc.
 */
@ManagedBean
@ViewScoped
public class ImageViewerBean implements Serializable {
	/** */
	private static final long serialVersionUID = - 1747335197116221778L;

	private static final Log log = LogFactory.getLog(ImageViewerBean.class);

	/** True if the image viewer pop-up should be displayed. */
	private boolean showImage;

	/** The database id of the Image object to be displayed. */
	private Integer imageId;

	private Image image;

	/** Title for the pop-up dialog box. */
	private String title;

	public ImageViewerBean() {
		log.debug("");
	}

	public static ImageViewerBean getInstance() {
		return (ImageViewerBean)ServiceFinder.findBean("imageViewerBean");
	}

	public String actionShowImage() {
		showImage = true;
		if (imageId != null) {
			ImageDAO imageDAO = ImageDAO.getInstance();
			image = imageDAO.findById(imageId);
		}
		ListView.addClientResizeScroll();
		return null;
	}

	/**
	 * Method called by the Close button
	 * @return null navigation string
	 */
	public String actionClose() {
		showImage = false;
		image = null; // release object
		//ListView.addClientResizeScroll();
		return null;
	}

	/**
	 * Method called by "X" icon built-in to ace:dialog header.
	 * Called due to ace:ajax tag in pop-ups.
	 * @param evt Ajax event from the framework.
	 */
	public void actionClose(AjaxBehaviorEvent evt) {
		log.debug("");
		actionClose();
	}

	/** See {@link #showImage}. */
	public boolean getShowImage() {
		return showImage;
	}
	/** See {@link #showImage}. */
	public void setShowImage(boolean showImage) {
		this.showImage = showImage;
	}

	/** See {@link #imageId}. */
	public Integer getImageId() {
		return imageId;
	}
	/** See {@link #imageId}. */
	public void setImageId(Integer imageId) {
		this.imageId = imageId;
	}

	/** See {@link #image}. */
	public Image getImage() {
		return image;
	}
	/** See {@link #image}. */
	public void setImage(Image image) {
		this.image = image;
	}

	/** See {@link #title}. */
	public String getTitle() {
		return title;
	}
	/** See {@link #title}. */
	public void setTitle(String title) {
		this.title = title;
	}

}
