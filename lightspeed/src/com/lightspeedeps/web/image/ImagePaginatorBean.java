package com.lightspeedeps.web.image;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.DataModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.component.datatable.DataTable;

import com.lightspeedeps.dao.ImageDAO;
import com.lightspeedeps.model.Image;
import com.lightspeedeps.object.ImageResource;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.util.DataTableRequestBean;

/**
 * This class manages {@link Image} deletion for all the entity classes that
 * support associated images, such as
 * {@link com.lightspeedeps.model.ScriptElement ScriptElement} and
 * {@link com.lightspeedeps.model.PointOfInterest PointOfInterest}. When a page
 * is presented that allows image deletion, the backing bean for that page, such
 * as {@link com.lightspeedeps.web.element.ScriptElementBean ScriptElementBean},
 * will set itself as the "imageHolder" of the current instance of this class.
 * (This is typically done in one of the shared superclasses, such as
 * {@link com.lightspeedeps.web.view.ListImageView ListImageView}.)
 */
@ManagedBean
@ViewScoped
public class ImagePaginatorBean implements PopupHolder, Serializable {
	/** */
	private static final long serialVersionUID = - 8652621200943500050L;

	private static final Log log = LogFactory.getLog(ImagePaginatorBean.class);

	private static final int ACT_DELETE_IMAGE = 1;

	/** If false, images are not deleted immediately, but held until our commit()
	 * method is called. */
	private boolean autoCommit = false;

	private Set<Integer> deletedImageIds;

	private Object paginator = null;
	private ImageHolder imageHolder = null;

	public static ImagePaginatorBean getInstance() {
		return (ImagePaginatorBean)ServiceFinder.findBean("imagePaginatorBean");
	}

	/**
	 * Delete a contact.. This is an Action method called by the Delete button. Here we just put up
	 * the confirmation dialog.
	 */
	public String actionDeleteImage() {
		PopupBean.getInstance().show(
			this, ACT_DELETE_IMAGE, "Image.Delete.");
//		ListView.addClientResize();
		return null;
	}

	/**
	 * Called when user presses "Delete" button to delete the
	 * currently displayed image.  Removes the image from the
	 * database, and from this PointOfInterest's image set.
	 * @return null navigation string
	 */
	@SuppressWarnings("rawtypes")
	private String actionDeleteImageOk() {
		DataTableRequestBean dtb = DataTableRequestBean.getInstance();
		DataTable dataTable = dtb.getDataTable();
		DataModel model = dataTable.getModel();

		// Use the current page number of the data table to fetch
		// the correct image from the data model. LS-2272
		int page = dataTable.getPage();
		model.setRowIndex(page - 1);
		ImageResource imageResource = (ImageResource)model.getRowData();
		Image image = imageResource.getImage();
		if (image != null) {
			imageHolder.removeImage(image);
			if (isAutoCommit()) {
				ImageDAO.getInstance().delete(image);
			}
			else {
				if (deletedImageIds == null) {
					deletedImageIds = new HashSet<>();
				}
				deletedImageIds.add(image.getId());
			}
		}
		else {
			log.error("null image returned");
		}
		return null;
	}

	/**
	 * Called by our holder to commit any image delete requests to the database.
	 */
	public void commit() {
		log.debug("");
		if (deletedImageIds != null) {
			log.debug(deletedImageIds.size());
			Image fresh;
			for (Integer id : deletedImageIds) {
				if (id != null) {
					fresh = ImageDAO.getInstance().findById(id);
					if (fresh != null) {
						ImageDAO.getInstance().delete(fresh);
					}
				}
			}
			deletedImageIds = null;
		}
	}

	public void rollback() {
		deletedImageIds = null;
	}

	public boolean hasChanges() {
		return (deletedImageIds != null && deletedImageIds.size() > 0);
	}

	/**
	 * Find the image currently displayed on the screen, based on information
	 * from the object bound to the paginator control.
	 * @return the current Image.
	 */
//	private Image findCurrentImage() {
//		Image image = null;
//		if (imageHolder != null) {
//			List<Image> imageList = imageHolder.getImageList();
//			if (imageList.size() == 1) {
//				// handles easy case, plus Production Logo display, which has no paginator!
//				image = imageList.get(0);
//			}
//			else if (imageList.size() > 0 && getPaginator() != null) {
//				int index = 0; //getPaginator().getFirstRow();
//				log.debug("ix=" + index);
//				if (index >= 0 && index < imageList.size()) {
//					image = imageList.get(index);
//				}
//			}
//		}
//		return image;
//	}

	// Component binding: get access to the image paginator in the web page.
	public Object getPaginator() {
		//log.debug("pg="+paginator);
		// TODO maybe eliminate following code; it seemed to work ok without this code during
		// testing for bug #129
		if (paginator == null) {
			// this seemed necessary to avoid an "error writing" failure from JSF
			paginator = new Object();
		}
		return this.paginator;
	}
	public void setPaginator(Object p) {
		this.paginator = p;
		//String s = "id="+paginator.getId() + " cnt=" + paginator.getRowCount() +
		//		" fr=" + paginator.getFirstRow() + " fvar=" + paginator.getFirstRowIndexVar();
		//log.debug(s);
	}

	public ImageHolder getImageHolder() {
		return imageHolder;
	}
	public void setImageHolder(ImageHolder imageHolder) {
		this.imageHolder = imageHolder;
		deletedImageIds = null;
	}

	/** See {@link #autoCommit}. */
	public boolean isAutoCommit() {
		return autoCommit;
	}
	/** See {@link #autoCommit}. */
	public void setAutoCommit(boolean b) {
		this.autoCommit = b;
	}

	/**
	 * @see com.lightspeedeps.web.popup.PopupHolder#confirmCancel(int)
	 */
	@Override
	public String confirmCancel(int action) {
//		ListView.addClientResize();
		return null;
	}

	/**
	 * @see com.lightspeedeps.web.popup.PopupHolder#confirmOk(int)
	 */
	@Override
	public String confirmOk(int action) {
		String res = null;
		try {
			switch(action) {
				case ACT_DELETE_IMAGE:
					res = actionDeleteImageOk();
					break;
			}
//			ListView.addClientResize();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return res;
	}

}
