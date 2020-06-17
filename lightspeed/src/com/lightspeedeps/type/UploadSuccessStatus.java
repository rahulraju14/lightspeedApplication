/**
 * UploadSuccessStatus.java
 */
package com.lightspeedeps.type;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.icefaces.ace.component.fileentry.FileEntryResults;
import org.icefaces.ace.component.fileentry.FileEntryStatus;

import com.lightspeedeps.util.app.MsgUtils;

/**
 * Defines a successful file upload status. This version allows the
 * "success" message to be specified, as a message resource id.
 * (The ICEfaces version does not support overriding the message.)
 */
public class UploadSuccessStatus implements FileEntryStatus {
	/** */
	private static final long serialVersionUID = 1L;

	String messageId;
	public UploadSuccessStatus(String msgId) {
		messageId = msgId;
	}

	@Override
	public boolean isSuccess() {
		return true;
	}
	@Override
	public FacesMessage getFacesMessage(FacesContext facesContext,
			UIComponent uiComponent, FileEntryResults.FileInfo fileInfo) {
		String msg = MsgUtils.formatMessage(messageId, getParameters(fileInfo));
		return new FacesMessage(FacesMessage.SEVERITY_INFO, msg, "");
	}

    /**
     * When formatting the MessageFormat patterns that comes from the
     * ResourceBundles, the following parameters are provided:
     *
     * param[0] : fileName     (The original file name, on user's computer)
     * param[1] : contentType  (MIME type of uploaded file)
     * param[2] : size         (Size of the uploaded file)
     */
    private static Object[] getParameters(FileEntryResults.FileInfo fi) {
        Object[] params = new Object[] {
            fi.getFileName(),
            fi.getContentType(),
            fi.getSize(),
        };
        return params;
    }

}
