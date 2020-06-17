package com.lightspeedeps.web.onboard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.DocChangeEventDAO;
import com.lightspeedeps.model.DocChangeEvent;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.view.View;

@ManagedBean
@ViewScoped
public class DocumentEventsBean extends View implements Serializable {

	private static final long serialVersionUID = 1575356411080669524L;

	private static final Log log = LogFactory.getLog(DocumentEventsBean.class);

	/** The list of Document Events of currently selected Form I9. */
	List<DocChangeEvent> documentEventList = null;

	public DocumentEventsBean() {
		super("DocumentEventsBean.");
	}

	public static DocumentEventsBean getInstance() {
		return (DocumentEventsBean) ServiceFinder.findBean("documentEventsBean");
	}

	public void createDocumentEventList() {
		log.debug("!!!!!!!!!!!!!!!!");
		documentEventList = new ArrayList<>();
		Integer contactDocumentId = SessionUtils.getInteger(Constants.ATTR_ONBOARDING_SELECTED_FORM_I9_ID);

		if (contactDocumentId != null) {
			log.debug(">>>>>> contact doc id of form_i9 " + contactDocumentId);
			documentEventList = DocChangeEventDAO.getInstance().findByNamedQuery(DocChangeEvent.GET_DOCUMENT_EVENT_LIST_BY_CONTACT_DOCUMENT_ID, map("contactDocumentId", contactDocumentId));
		}
	}

	/**See {@link #documentEventList}. */
	public List<DocChangeEvent> getDocumentEventList() {
		if (documentEventList == null) {
			createDocumentEventList();
		}
		return documentEventList;
	}
	/**See {@link #documentEventList}. */
	public void setDocumentEventList(List<DocChangeEvent> documentEventList) {
		this.documentEventList = documentEventList;
	}
}
