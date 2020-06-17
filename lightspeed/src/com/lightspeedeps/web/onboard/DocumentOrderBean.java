/**
 * DocumentOrderBean.java
 */
package com.lightspeedeps.web.onboard;

import java.util.*;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.DocumentDAO;
import com.lightspeedeps.model.Document;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupHolder;
import com.lightspeedeps.web.view.SelectableList;
import com.lightspeedeps.web.view.SelectableListHolder;
import com.lightspeedeps.web.view.SortableList;

/**
 * This is the backing bean for the "Order Documents" dialog box. This dialog
 * allows the user to control the display order of documents within a production,
 * such as on the Documents & Packets page. LS-4600
 */
@ManagedBean
@ViewScoped
public class DocumentOrderBean extends PopupBean implements SelectableListHolder {
	private static final Log log = LogFactory.getLog(DocumentOrderBean.class);

	/** */
	private static final long serialVersionUID = 1L;

	/** The list of documents used in the "change order" popup. */
	private final List<Document> documentOrderList = new ArrayList<>();

	/** The 'automatically' sorted list, accessed by the JSP. The SortableListImpl instance will call
	 * us back at our sort(List, SortableList) method to actually do a sort when necessary. */
	private final SelectableList selectDocList =
			new SelectableList(this, documentOrderList, "none", true);

	private transient DocumentDAO documentDAO;

	/**
	 * Default constructor.
	 */
	public DocumentOrderBean() {
		log.debug("");
	}

	/**
	 * @return the current instance of this bean.
	 */
	public static DocumentOrderBean getInstance() {
		return (DocumentOrderBean)ServiceFinder.findBean("documentOrderBean");
	}

	/**
	 * Display our dialog box.
	 *
	 * @param holder Typically our caller, used for call-backs.
	 * @param act An integer passed in call-backs to discriminate among various
	 *            actions the caller might manage.
	 * @param titleId The message-id to use to look up the dialog box title.
	 * @param buttonOkId The message-id to use to look up the "Ok" button text.
	 * @param buttonCancelId The message-id to use to look up the "Cancel"
	 *            button text.
	 * @param docOrderList The list of documents to display.
	 */
	public void show(PopupHolder holder, int act, String titleId, String buttonOkId, String buttonCancelId,
			List<Document> docOrderList) {
		documentOrderList.clear();
		documentOrderList.addAll(docOrderList);
		Collections.sort(documentOrderList, orderCompare);
		super.show(holder, act, titleId, buttonOkId, buttonCancelId);
	}

	/** Comparator for sorting Documents by their list priority. */
	private static Comparator<Document> orderCompare = new Comparator<Document>() {
		@Override
		public int compare(Document one, Document two) {
			return NumberUtils.compare(one.getListOrder(), two.getListOrder());
		}
	};

	/**
	 * @see com.lightspeedeps.web.popup.PopupBean#actionOk()
	 */
	@Override
	public String actionOk() {
		unselectCurrent();
		if (actionSaveChangeOrder()) {
			return super.actionOk();
		}
		return null;
	}

	/**
	 * @see com.lightspeedeps.web.popup.PopupBean#actionCancel()
	 */
	@Override
	public String actionCancel() {
		unselectCurrent();
		return super.actionCancel();
	}

	/**
	 * Action method for the "up arrow" control. This moves the currently
	 * selected Document name up one position in the list. If the Document
	 * is already first in the list, no change (or error) occurs.
	 *
	 * @return null navigation string
	 */
	public String actionMoveDocUp() {
		int ix = findOrderIndex();
		if (ix > 0) {
			Document doc = documentOrderList.remove(ix);
			documentOrderList.add(ix-1, doc);
			getSelectDocList().setSelectedRow(ix-1);
			showSelected(doc);
		}
		return null;
	}

	/**
	 * Action method for the "down arrow" control. This moves the currently
	 * selected Document name down one position in the list. If the Document
	 * is already last in the list, no change (or error) occurs.
	 *
	 * @return null navigation string
	 */
	public String actionMoveDocDown() {
		int ix = findOrderIndex();
		if (ix >= 0 && ix < documentOrderList.size()-1) {
			Document doc = documentOrderList.remove(ix);
			documentOrderList.add(ix+1, doc);
			getSelectDocList().setSelectedRow(ix+1);
			showSelected(doc);
		}
		return null;
	}

	/**
	 * Find the index of the currently selected Document (on the
	 * "change order" pop-up dialog) within the selection list.
	 *
	 * @return The zero-origin index of the selected Document, or -1 if it is
	 *         not found (which should not happen!).
	 */
	private int findOrderIndex() {
		int ix = getSelectDocList().getSelectedRow();
		if (ix > documentOrderList.size()) {
			ix = -1;
		}
		return ix;
	}

	/**
	 * Method to save changes from on the "change order" pop-up dialog.
	 * Re-assign the document list priorities to match the user's selected
	 * order. Note that this requires creating custom Document objects if the
	 * user's ordering is such that the "list priority" of the system Document
	 * does not match the priority specified by the user.
	 *
	 * @return True iff no exception occurred during save processing.
	 */
	public boolean actionSaveChangeOrder() {
		boolean ret = false;
		try {
			int order = 1;
			for (Document doc : documentOrderList) {
				doc = getDocumentDAO().refresh(doc);
				if (doc.getListOrder() != order) { // Order changed
					doc.setListOrder(order);
					getDocumentDAO().attachDirty(doc);
				}
				order++;
			}
			ret = true;
			// TODO UN-lock the document list for this production!
		}
		catch (Exception e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return ret;
	}

	/**
	 * @see com.lightspeedeps.web.view.SortHolder#sort(java.util.List, com.lightspeedeps.web.view.SortableList)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public void sort(List list, SortableList sortableList) {
		// we don't support sorting in this popup
	}

	/**
	 * @see com.lightspeedeps.web.view.SortHolder#isSortableDefaultAscending(java.lang.String)
	 */
	@Override
	public boolean isSortableDefaultAscending(String sortColumn) {
		return true;
	}

	/**
	 * @see com.lightspeedeps.web.view.SelectableListHolder#selectableRowChanged()
	 */
	@Override
	public void selectableRowChanged() {
	}

	/**
	 * @see com.lightspeedeps.web.view.SelectableListHolder#setSelectableSelected(java.lang.Object, boolean)
	 */
	@Override
	public void setSelectableSelected(Object item, boolean b) {
		try {
			Document doc = (Document)item;
			doc.setSelected(b);
		}
		catch (Exception e) {
		}
	}

	/**
	 * Turn off the "select" flag in the currently selected document. We do
	 * this before we exit, since the "select" field in the Document is also
	 * used by the main page in the left-hand list.
	 */
	private void unselectCurrent() {
		int n = getSelectDocList().getSelectedRow();
		if (n > 0) {
			Document doc = getDocumentOrderList().get(n);
			setSelectableSelected(doc, false);
		}
	}

	/**
	 * Make the document show up as selected using ace rowStateMap.
	 * @param doc
	 */
	private void showSelected(Document doc) {
		getSelectDocList().getStateMap().clear();
		getSelectDocList().selectRowState(doc);
	}

	/**
	 * @see com.lightspeedeps.web.view.SelectableListHolder#setupSelectableItem(java.lang.Integer)
	 */
	@Override
	public void setupSelectableItem(Integer id) {
		// Not used at this time.
	}

	/** See {@link #selectDocList}. */
	public SelectableList getSelectDocList() {
		return selectDocList;
	}

	/** See {@link #documentOrderList}. */
	public List<Document> getDocumentOrderList() {
		return documentOrderList;
	}

	/**
	 * @return An instance of DocumentDAO.
	 */
	private DocumentDAO getDocumentDAO() {
		if (documentDAO == null) {
			documentDAO = DocumentDAO.getInstance();
		}
		return documentDAO;
	}

}
