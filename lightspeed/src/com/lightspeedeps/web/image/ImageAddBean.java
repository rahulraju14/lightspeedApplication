package com.lightspeedeps.web.image;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.AjaxBehaviorEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.component.fileentry.FileEntryResults.FileInfo;
import org.icefaces.ace.component.fileentry.FileEntryStatus;
import org.icefaces.ace.component.fileentry.FileEntryStatuses;

import com.lightspeedeps.dao.ImageDAO;
import com.lightspeedeps.model.Image;
import com.lightspeedeps.type.ImageType;
import com.lightspeedeps.type.UploadSuccessStatus;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.file.ImageUtils;
import com.lightspeedeps.web.popup.FileLoadBean;
import com.lightspeedeps.web.view.ListView;

/**
 * Backing bean for the "Add Image" dialog box.
 */
@ManagedBean
@ViewScoped
public class ImageAddBean extends FileLoadBean implements Serializable {
	/** */
	private static final long serialVersionUID = 5834413392247579834L;

	private static final Log log = LogFactory.getLog(ImageAddBean.class);

	/** Our 'holder' - the instance that is using this bean to load
	 * some image files into the database. */
	private ImageHolder imageHolder = null;

	/** The name to be substituted into prompting messages.  This usually indicates
	 * the item with which the image will be associated. */
	private String name;

	/** The message id of the message to display as the prompt in the
	 * dialog box. */
	private String promptId = "Image.AddPrompt.Default";

	/** If false, images are not saved here directly; they will be saved (or discarded)
	 * by our "holder" based on the owning element (such as a ScriptElement); the save
	 * is typically handled automatically via the hibernate cascade option. */
	private boolean autoCommit = true;

	/** The flag that controls the display of the pop-up dialog. */
	private boolean showNewImage = false;

	/** If true, pop-up is for a Map image (renders different dialog text). */
	private boolean forMap;

	/** If true, a PDF may be uploaded instead of an image file. */
	private boolean allowPdf;

	/* Constructor */
	public ImageAddBean() {
		setAddedMessageId("Image.FileUploaded");
	}

	public static ImageAddBean getInstance() {
		return (ImageAddBean)ServiceFinder.findBean("imageAddBean");
	}

	/**
	 * Display the "Add Image" dialog box.
	 *
	 * @param pHolder
	 *            A reference used for callbacks when an upload completes.
	 * @param pName
	 *            The name of the object with which the image will be
	 *            associated; may be used in messages in the dialog box.
	 * @return null navigation string
	 */
	public String actionOpenNewImage(ImageHolder pHolder, String pName) {
		return actionOpenNewImage(pHolder, pName, false);
	}

	/**
	 * Display the "Add Image" dialog box.
	 *
	 * @param pHolder
	 *            A reference used for callbacks when an upload completes.
	 * @param pName
	 *            The name of the object with which the image will be
	 *            associated; may be used in messages in the dialog box.
	 * @param pdfOk
	 *            True iff we should allow a PDF file to be uploaded; if false,
	 *            only true image files are allowed (e.g., jpeg, png, etc.).
	 * @return null navigation string
	 */
	public String actionOpenNewImage(ImageHolder pHolder, String pName, boolean pdfOk) {
		showNewImage = true;
		allowPdf = pdfOk;
		cancelled = false;
		setImageHolder(pHolder);
		setName(pName);
		setErrorMessage("");
		setAddedMessageId("Image.FileUploaded");
		setMessage(MsgUtils.formatMessage(promptId, name));
		//log.debug(pName + ", " + autoCommit);
//		ListView.addClientResize();
		return null;
	}

	/**
	 * Action method for the Cancel button on the dialog box. This just closes
	 * the dialog box.
	 *
	 * @return null navigation string
	 */
	public String actionCancelNewImage() {
		showNewImage = false;
		if (imageHolder != null) {
			imageHolder.addEndingJavascript();
		}
		imageHolder = null;
		setAddedMessageId(null);
		super.actionCancel();
		return null;
	}

	/**
	 * Method called by "X" icon built-in to ace:dialog header.
	 * Called due to ace:ajax tag in pop-ups.
	 * @param evt Ajax event from the framework.
	 */
	@Override
	public void actionClose(AjaxBehaviorEvent evt) {
		log.debug("");
		actionCancelNewImage();
	}

	/**
	 * Called by our "holder" to remove any images that may have been saved in
	 * the database during the current edit cycle. Typically called if the user
	 * Cancels the current edit session.
	 *
	 * @param holder The ImageHolder, which should have a list of the images
	 *            added.
	 */
	public static void rollback(ImageHolder holder) {
		if (holder.getAddedImages() != null) {
			ImageDAO imageDAO = ImageDAO.getInstance();
			for (Image img : holder.getAddedImages()) {
				imageDAO.delete(img);
			}
			holder.setAddedImages(null);
		}
	}

	/**
	 * Called from our superclass to process the uploaded file. Verify the
	 * content is visual - either an image, or (optionally) a PDF. If valid,
	 * then store the file in the database and notify our 'holder' that a file
	 * was loaded.
	 *
	 * @see com.lightspeedeps.web.popup.FileLoadBean#processFile(org.icefaces.ace.component.fileentry.FileEntryResults.FileInfo)
	 */
	@Override
	protected String processFile(FileInfo fileInfo) {
		String messageId = null;
		showPopupErrors();
		if ( (! getFileContentType().contains("image")) &&
				((! allowPdf) || (! getFileContentType().contains("pdf")))) {
			if (allowPdf) {
				messageId = "Image.FileErrorContentPdf";
			}
			else {
				messageId = "Image.FileErrorContent";
			}
			FileEntryStatus newStatus = FileEntryStatuses.INVALID_CONTENT_TYPE;
			fileInfo.updateStatus(newStatus, false, true);
			// this deletes the uploaded file; normally this would generate a message,
			// but our popup JSF code will display our message and not the JSF message.
		}
		else {
			fileInfo.updateStatus(new UploadSuccessStatus(getAddedMessageId()), false);
			if (storeImageFile(getUploadFile())) {
				showNewImage = false; // close pop-up dialog
				log.debug("saved");
				if (imageHolder != null) {
					imageHolder.addEndingJavascript();
				}
			}
			else {
				messageId = "Image.StoreFailed";
				//setErrorMessage(MsgUtils.formatMessage("Image.StoreFailed", getDisplayFilename()));
				FileEntryStatus newStatus = FileEntryStatuses.INVALID;
				fileInfo.updateStatus(newStatus, false, true);
			}
		}
		return messageId;
	}

	/**
	 * Save the image file into the Image table of the database.
	 *
	 * @param iFile The existing file (on server) whose contents are to be
	 *            saved.
	 * @return True iff the file was successfully stored in the Image table.
	 */
	private boolean storeImageFile(File iFile) {
		log.debug("");
		boolean bRet = false;
		Image image = createImageFromFile(iFile);
		if (image != null) {
			image.setTruncatedTitle(getDisplayFilename());
			if (getFileContentType().contains("image")) {
				image.setType(ImageType.IMAGE);
			}
			else if (getFileContentType().contains("pdf")) {
				image.setType(ImageType.PDF);
			}
			else {
				image.setType(ImageType.OTHER);
			}
			if (imageHolder != null) {
				imageHolder.updateImage(image, getDisplayFilename());
				getAddedImages().add(image);
			}
			if (getAutoCommit()) {
				ImageDAO.getInstance().save(image);
			}
			bRet = true;
		}
		return bRet;
	}

	/**
	 * Create an Image instance from the contents of the given file.
	 *
	 * @param iFile The existing file (on the server).
	 * @return The Image instance containing the contents of the given file.
	 */
	private static Image createImageFromFile(File iFile) {
		log.debug("");
		Image image = null;
		byte[] content = null;
		try {
			content = getBytesFromFile(iFile);
			if (content != null) {
				image = new Image();
				image.setDate(new Date());
				image.setContent(content);
				image.setArtist(SessionUtils.getCurrentUser().getFirstNameLastName());
				log.debug("image loaded, size="+content.length);
				ImageUtils.resizeImage(image); // create thumbnail & shrink image if necessary
			}
			else { // File was too large
				log.debug("file too large (over 2GB)");
			}
		}
		catch (IOException e) {
			log.debug(e);
			e.printStackTrace();
		}
		return image;
	}

	/**
	 * Read the given file into a byte array.
	 *
	 * @param file The file, which already resides on the server, to be loaded.
	 * @return A new byte array containing the file's contents. Returns null if
	 *         the file's length is greater than Integer.MAX_VALUE.
	 * @throws IOException
	 */
	private static byte[] getBytesFromFile(File file) throws IOException {
		// Get the size of the file
		long length = file.length();

		if (length > Integer.MAX_VALUE) {
			// File is too large
			return null;
		}

		InputStream is = new FileInputStream(file);

		// Create the byte array to hold the data
		byte[] bytes = new byte[(int)length];

		// Read in the bytes
		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length
			   && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
			offset += numRead;
		}

		// Ensure all the bytes have been read in
		if (offset < bytes.length) {
			is.close();
			throw new IOException("Could not completely read file "+file.getName());
		}

		// Close the input stream and return bytes
		is.close();
		return bytes;
	}

	/**
	 * Turn on the display of error and/or informational messages in
	 * the dialog box.
	 */
	private void showPopupErrors() {
		ListView.addJavascript("showPopErrors();");
	}

	/** See {@link #name}. */
	public String getName() {
		return name;
	}
	/** See {@link #name}. */
	public void setName(String name) {
		this.name = name;
	}

	/** See {@link #promptId}. */
	public String getPromptId() {
		return promptId;
	}
	/** See {@link #promptId}. */
	public void setPromptId(String promptId) {
		this.promptId = promptId;
	}

	/** See {@link #showNewImage}. */
	public boolean getShowNewImage() {
		return showNewImage;
	}
	/** See {@link #showNewImage}. */
	public void setShowNewImage(boolean showNewImage) {
		this.showNewImage = showNewImage;
	}

	/** See {@link #forMap}. */
	public boolean getForMap() {
		return forMap;
	}
	/** See {@link #forMap}. */
	public void setForMap(boolean isMap) {
		forMap = isMap;
		if (forMap) {
			promptId = "Image.AddPrompt.Map";
		}
		else {
			promptId = "Image.AddPrompt.Default";
		}
	}

	/** See {@link #imageHolder}. */
	public ImageHolder getImageHolder() {
		return imageHolder;
	}
	/** See {@link #imageHolder}. */
	public void setImageHolder(ImageHolder holder) {
		imageHolder = holder;
	}

	/** @return the Set of Images maintained by our "holder". */
	private Set<Image> getAddedImages() {
		if (imageHolder.getAddedImages() == null) {
			imageHolder.setAddedImages(new HashSet<Image>());
		}
		return imageHolder.getAddedImages();
	}

	/** See {@link #autoCommit}. */
	public boolean getAutoCommit() {
		return autoCommit;
	}
	/** See {@link #autoCommit}. */
	public void setAutoCommit(boolean b) {
		autoCommit = b;
	}

}
