package com.lightspeedeps.web.onboard;

import java.io.Serializable;
import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.event.SelectEvent;
import org.icefaces.ace.event.UnselectEvent;
import org.icefaces.ace.model.table.RowState;
import org.icefaces.ace.model.table.RowStateMap;

import com.lightspeedeps.dao.DocumentDAO;
import com.lightspeedeps.dao.PacketDAO;
import com.lightspeedeps.model.*;
import com.lightspeedeps.object.DocRowItem;
import com.lightspeedeps.type.PayrollFormType;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupInputBean;
import com.lightspeedeps.web.view.ListView;

/**
 * This class is the backing bean for the Packets mini tab,
 * under Onboarding sub-main tab.
 */
@ManagedBean
@ViewScoped
public class PacketListBean extends ListView implements Serializable {

	private static final long serialVersionUID = -6487791336112396029L;

	private static final Log log = LogFactory.getLog(PacketListBean.class);

	/** "Create" action code for popup confirmation/prompt dialog. */
	private static final int ACT_CREATE = 10;

	/** "Delete" action code for popup confirmation/prompt dialog. */
	private static final int ACT_DELETE = 11;

	/** "Rename" action code for popup confirmation/prompt dialog. */
	private static final int ACT_RENAME = 12;

	/** The filename entered (in the input field) by the user. */
	private String userPacketName;

	/** The packet dao instance used to use the dao layer methods. */
	private transient PacketDAO packetDAO;

	/** The packet instance . */
	private Packet packet;

	/** The list of packets to be shown on the UI. */
	private List<Packet> packetList;

	private RowStateMap stateMap = new RowStateMap();

	/** The list of (not deleted) documents in the currently selected packet. Used
	 * by JSP to display the list. */
	private List<Document> documentList = new ArrayList<>();

	/** Boolean field used for keeping track of master check box check uncheck event */
	private boolean checkedForAll = false;

	/** The document instance used to know the checked document*/
	private Document checkedDocument;

	/** The documents to be removed from the packet */
	private Document documentToRemove;

	/** Used to store the selected packet name and show it on its document list.*/
	private String clickedPacketName = null;

	/** True iff the bottom ("start packet ") portion of the Start Packets page
	 * should be displayed. */
	private boolean showDetail = false;

	/** The default constructor . */
	public PacketListBean() {
		super(Packet.SORT_KEY_NAME, "Packet.");
		Integer id = SessionUtils.getInteger(Constants.ATTR_PACKET_ID);
		setupSelectedItem(id);
		showDetail = SessionUtils.getBoolean(Constants.ATTR_ONBOARDING_SHOW_DETAIL, true);
	}

	/** Used to return the instance of the packet list bean . */
	public static PacketListBean getInstance() {
		return (PacketListBean)ServiceFinder.findBean("packetListBean");
	}

	/** Listener used for row selection, sets the packet with the currently selected row object . */
	@Override
	public void listenRowClicked (SelectEvent event) {
		packet = (Packet)event.getObject();
		setupSelectedItem(packet.getId());
		setDocumentList(getDocumentListByPacket());
		setClickedPacketName(packet.getName());
	}

	/**
	 * Method creates the list of packet documents within the currently selected
	 * packet that are not deleted
	 *
	 * @return Current packet's document list (without deleted docs)
	 */
	private List<Document> getDocumentListByPacket() {
		List<Document> packetDocumentList = new ArrayList<>();
		if (packet != null && packet.getDocumentList() != null) {
			for (Document doc : packet.getDocumentList()) {
				if (! doc.getDeleted()) {
					packetDocumentList.add(doc);
				}
			}
		}
		return packetDocumentList;
	}

	/** Listener used for row de-selection, it sets the packet instance to null . */
	public void listenRowUnClicked (UnselectEvent event) {
		packet = null;
		setupSelectedItem(null);
		setDocumentList(null);
	}

//	/** Value change listener for individual checkbox's checked / unchecked event
//	 * @param evt
//	 */
//	public void listenSingleCheck (ValueChangeEvent evt) {
//		DocRowItem docRowItem = (DocRowItem) evt.getComponent().getAttributes().get("selectedRow");
//		if (packet != null) {
//			if (docRowItem.getChecked()) {
//				if (docRowItem.getFolder() == null) {
//					checkBoxSelectedItems.add(docRowItem);
//				}
//			}
//			else if (checkBoxSelectedItems.contains(docRowItem)) {
//				checkBoxSelectedItems.remove(docRowItem);
//			}
//		}
//		else {
//			MsgUtils.addFacesMessage("PacketListBean.SelectPacket", FacesMessage.SEVERITY_INFO);
//		}
//	}

	/**
	 * Set the 'selected' flag on the given item in our list, which should be a
	 * Department.
	 *
	 * @see com.lightspeedeps.web.view.ListView#setSelected(java.lang.Object,
	 *      boolean)
	 */
	@Override
	protected void setSelected(Object item, boolean b) {
		try {
			((Packet)item).setSelected(b);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * @see com.lightspeedeps.web.view.ListView#setupSelectedItem(java.lang.Integer)
	 */
	@Override
	protected void setupSelectedItem(Integer id) {
		log.debug("id=" + id);
		if (packet != null) {
			packet.setSelected(false);
		}
		if (id == null) {
			id = findDefaultId();
		}
		if (id == null) {
			SessionUtils.put(Constants.ATTR_PACKET_ID, null);
			packet = null;
			editMode = false;
			newEntry = false;
		}
		else if ( ! id.equals(NEW_ID)) {
			packet = getPacketDAO().findById(id);
			if (packet != null) {
				if (getItemList().indexOf(packet) < 0) {
					// Can happen if switched projects in Commercial production.
					packet = null; // Force use of default entry.
				}
			}
			if (packet == null) {
				id = findDefaultId();
				if (id != null) {
					packet = getPacketDAO().findById(id);
				}
			}
			SessionUtils.put(Constants.ATTR_PACKET_ID, id);
			if (packet != null) {
				//createMembersList();
			}
		}
		else {
			log.debug("new packet");
			SessionUtils.put(Constants.ATTR_PACKET_ID, null); // erase "new" flag
			packet = new Packet();
		}
		if (packet != null) {
			RowState state = new RowState();
			state.setSelected(true);
			stateMap.put(packet, state);
			setDocumentList(getDocumentListByPacket());
			setClickedPacketName(packet.getName());
			forceLazyInit();
		}
	}

	private void forceLazyInit() {
		packet.getDocumentCount();
	}

	private Integer findDefaultId() {
		return null;
	}

	/** Action method for creating a new packet
	 * @return null navigation string.
	 */
	public String actionCreate() {
		PopupInputBean inputBean = PopupInputBean.getInstance();
		inputBean.show(this, ACT_CREATE, "PacketListBean.CreatePacket.");
		inputBean.setInput("New Packet");
		inputBean.setMaxLength(Folder.MAX_NAME_LENGTH);
	return null;
	}

	/** Action method for deleting a new packet
	 * @return null navigation string.
	 */
	@Override
	public String actionDelete() {
		if (packet != null) {
			PopupBean bean = PopupBean.getInstance();
			bean.show(this, ACT_DELETE, "PacketListBean.DeletePacket.");
			bean.setMessage(MsgUtils.formatMessage("PacketListBean.DeletePacket.Text", packet.getName()));
		}
		else {
			MsgUtils.addFacesMessage("PacketListBean.SelectPacket", FacesMessage.SEVERITY_INFO);
		}
		return null;
	}

	/** Action method for renaming a new packet
	 * @return null navigation string.
	 */
	public String actionRename() {
		if (packet != null) {
			PopupInputBean inputBean = PopupInputBean.getInstance();
			inputBean.show(this, ACT_RENAME, "PacketListBean.RenamePacket.");
			inputBean.setInput(packet.getName());
		}
		else {
			MsgUtils.addFacesMessage("PacketListBean.SelectPacket", FacesMessage.SEVERITY_INFO);
		}
		return null;
	}

	/** Action method used to move the documents into the selected packet.
	 * @return null
	 */
	public String actionMoveDocumentToPacket() {
		if (packet != null) {
			List<Document> packetDocs = new ArrayList<>();
			if (getCheckBoxSelectedItems().size() > 0) {
				packet = getPacketDAO().refresh(packet);
				packetDocs = packet.getDocumentList();
				Integer docInPacket = 0;
				Integer docCounter = 0;
				for (DocRowItem item : getCheckBoxSelectedItems()) {
					docCounter++;
					if (item.getDocument() != null) {
						if (packetDocs != null) {
							if (! packetDocs.contains(item.getDocument())) {
								packetDocs.add(item.getDocument());
								item.setChecked(false);
							}
							else {
								docInPacket++;
								item.setChecked(false);
							}
						}
					}
					else if (item.getDocumentChain() != null) {
						if (packetDocs != null) {
							Document documentOfHighRevision = DocumentDAO.getInstance().findDocumentWithHighestRevision(item.getDocumentChain());
							//Remove ACTRA Intent from the list of documents that are to be delivered.)
							if (! PayrollFormType.ACTRA_INTENT.getName().equals(documentOfHighRevision.getName())) {
								if (! packetDocs.contains(documentOfHighRevision)) {
									packetDocs.add(documentOfHighRevision);
									item.setChecked(false);
								}
								else {
									docInPacket++;
									item.setChecked(false);
								}
							}
							else {
								MsgUtils.addFacesMessage("PacketListBean.ActraIntentForm", FacesMessage.SEVERITY_INFO);
								log.debug("ACTRA INTENT FORM");
								item.setChecked(false);
							}
						}
					}
				}
				if (docInPacket != 0) {
					if (docInPacket == 1) {
						MsgUtils.addFacesMessage("PacketListBean.DocumentAlreadyPresent", FacesMessage.SEVERITY_INFO);
					}
					else {
						MsgUtils.addFacesMessage("PacketListBean.DocumentsAreAlreadyPresent", FacesMessage.SEVERITY_INFO, docCounter, docInPacket);
					}
				}
				if (packetDocs != null) {
					packet.setDocumentList(packetDocs);
					packet.setDocumentCount(packetDocs.size());
				}
				PacketDAO.getInstance().attachDirty(packet);
				refreshPacketList(); // forces getting sorted document list from JPA. LS-4600
				getCheckBoxSelectedItems().clear();
				checkedForAll = false;
				forceLazyInit();
			}
			else {
				MsgUtils.addFacesMessage("PacketListBean.SelectDocument", FacesMessage.SEVERITY_INFO);
			}
		}
		else {
			MsgUtils.addFacesMessage("PacketListBean.SelectPacket", FacesMessage.SEVERITY_INFO);
		}
		return null;
	}

	/** Action method used to remove the documents from the selected packet.
	 *  executed when the cross image is clicked on the in the packet's document list
	 * @return null
	 */
	public String actionRemoveDocument() {
		if (packet != null) {
			if (documentToRemove != null) {
				documentList.remove(documentToRemove);
				packet = getPacketDAO().refresh(packet);
				packet.setDocumentList(documentList);
				getPacketDAO().attachDirty(packet);
				setDocumentList(packet.getDocumentList());
			}
		}
		else {
			MsgUtils.addFacesMessage("PacketListBean.SelectPacket", FacesMessage.SEVERITY_INFO);
		}
		return null;
	}

	public void refreshPacketList() {
		packet = getPacketDAO().refresh(packet);
		if (packet != null) {
			getPacketDAO().evict(packet);
			packet = getPacketDAO().findById(packet.getId());
			log.debug("packet selected >>>>>>>>>>> "+packet.getName());
			log.debug("packet document list size >>>>>>> "+packet.getDocumentList().size());
			setDocumentList(getDocumentListByPacket());
		}
	}

	/**
	 * Action method to toggle the display of the "start packets" section of
	 * the Start Packet page.
	 *
	 * @return null navigation string
	 */
	public String actionToggleDetail() {
		showDetail = ! showDetail;
		SessionUtils.put(Constants.ATTR_ONBOARDING_SHOW_DETAIL, showDetail);
		return null;
	}

	@Override
	public String confirmOk(int action) {
		String res = null;
		switch(action) {
			case ACT_CREATE:
				userPacketName = PopupInputBean.getInstance().getInput().trim();
				res = actionCreatePacketOk();
				break;
			case ACT_DELETE:
				res = actionDeleteOk();
				break;
			case ACT_RENAME:
				userPacketName = PopupInputBean.getInstance().getInput().trim();
				res = actionRenameOk();
				break;
		}
		return res;
	}

	@Override
	public String confirmCancel(int action) {
		return super.confirmCancel(action);
	}

	/**
	 * Called when the user clicks OK on the "create packet" pop-up dialog.
	 */
	public String actionCreatePacketOk() {
		try {
			if (userPacketName != null) {
				Production production = SessionUtils.getProduction();
				Packet newPacket = null;
				Map<String,Object> values = new HashMap<>();
				values.put("packetName", userPacketName);
				values.put("production", production);
				if (production != null) {
					if (production.getType().isAicp()) {
						Project currentProject = SessionUtils.getCurrentProject();
						values.put("project", currentProject);
						newPacket = getPacketDAO().findOneByNamedQuery(Packet.FIND_PACKET_NAME_BY_PRODUCTION_AND_PROJECT, values);
						log.debug(">>>>>>>>>>> packet name" + newPacket);
					}
					else {
						newPacket = getPacketDAO().findOneByNamedQuery(Packet.FIND_PACKET_BY_NAME_AND_PRODUCTION, values);
						log.debug(">>>>>>>>>>> packet name" + newPacket);
					}
					if (newPacket != null) {
						log.debug(">>>>>>>>>>> packet name" + newPacket);
						PopupInputBean inputBean = PopupInputBean.getInstance();
						inputBean.displayError(MsgUtils.getMessage("PacketListBean.DuplicatePacket"));
					}
					else {
						getPacketDAO().create(userPacketName, null);
						packetList = null;
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError("PacketListBean createPacket failed Exception: ", e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Called when the user clicks OK on the "delete packet" pop-up dialog.
	 */
	@Override
	public String actionDeleteOk() {
		try {
			if (packet != null) {
				packet = getPacketDAO().refresh(packet);
				getPacketDAO().delete(packet);
				packet = null;
				packetList = null;
				documentList = null;
				setClickedPacketName(null);
			}
		}
		catch (Exception e) {
			EventUtils.logError("PacketListBean delete packet failed Exception: ", e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/**
	 * Called when the user clicks OK on the "rename packet" pop-up dialog.
	 */
	public String actionRenameOk() {
		try {
			if (userPacketName != null) {
				Production production = SessionUtils.getProduction();
				Map<String,Object> values = new HashMap<>();
				values.put("packetName", userPacketName);
				values.put("production", production);
				Packet newPacket = null;
				if (production != null) {
					if (production.getType().isAicp()) {
						Project currentProject = SessionUtils.getCurrentProject();
						values.put("project", currentProject);
						newPacket = getPacketDAO().findOneByNamedQuery(Packet.FIND_PACKET_NAME_BY_PRODUCTION_AND_PROJECT, values);
						log.debug(">>>>>>>>>>> packet name" + newPacket);
					}
					else {
						newPacket = getPacketDAO().findOneByNamedQuery(Packet.FIND_PACKET_BY_NAME_AND_PRODUCTION, values);
						//Packet newPacket = getPacketDAO().findPacketByNameAndProduction(userPacketName, SessionUtils.getProductionId());
						log.debug(">>>>>>>>>>> packet name" + newPacket);
					}

					if (newPacket != null) {
						PopupInputBean inputBean = PopupInputBean.getInstance();
						inputBean.displayError(MsgUtils.getMessage("PacketListBean.DuplicatePacket"));
					}
					else {
						packet = getPacketDAO().refresh(packet);
						packet.setName(userPacketName);
						getPacketDAO().attachDirty(packet);
						packetList = null;
						setClickedPacketName(userPacketName);
					}
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError("PacketListBean rename packet failed Exception: ", e);
			MsgUtils.addGenericErrorMessage();
		}
		return null;
	}

	/** This method returns the PacketDAO instance
	 * @return folderDAO
	 */
	private PacketDAO getPacketDAO() {
		if (packetDAO == null) {
			packetDAO = PacketDAO.getInstance();
		}
		return packetDAO;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public List getItemList() {
		return getPacketList();
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected Comparator getComparator() {
		return null;
	}

	/**See {@link #packetList}. */
	public List<Packet> getPacketList() {
		if (packetList == null) {
			packetList = new ArrayList<>();
			Production production = SessionUtils.getProduction();
			if (production != null) {
				if (production.getType().isAicp()) {
					Map<String, Object> values = new HashMap<>();
					values.put("production", production);
					values.put("project", SessionUtils.getCurrentProject());
					log.debug("<><><><><>");
					packetList = getPacketDAO().findByNamedQuery(Packet.GET_PACKET_LIST_BY_PRODUCTION_PROJECT, values);
				}
				else {
					log.debug("<><><><><>");
					packetList = getPacketDAO().findByProperty("production", production);
				}
			}
		}
		return packetList;
	}
	/**See {@link #packetList}. */
	public void setPacketList(List<Packet> packetList) {
		this.packetList = packetList;
	}

	/**See {@link #stateMap}. */
	@Override
	public RowStateMap getStateMap() {
		return stateMap;
	}
	/**See {@link #stateMap}. */
	@Override
	public void setStateMap(RowStateMap stateMap) {
		this.stateMap = stateMap;
	}

	/**See {@link #documentList}. */
	public List<Document> getDocumentList() {
		if (documentList == null) {
			documentList = getDocumentListByPacket();
		}
		return documentList;
	}
	/**See {@link #documentList}. */
	public void setDocumentList(List<Document> documentList) {
		this.documentList = documentList;
	}

	/**See {@link #checkedForAll}. */
	public boolean isCheckedForAll() {
		return checkedForAll;
	}
	/**See {@link #checkedForAll}. */
	public void setCheckedForAll(boolean checkedForAll) {
		this.checkedForAll = checkedForAll;
	}

	/**See {@link #checkedDocument}. */
	public Document getCheckedDocument() {
		return checkedDocument;
	}
	/**See {@link #checkedDocument}. */
	public void setCheckedDocument(Document checkedDocument) {
		this.checkedDocument = checkedDocument;
	}

	/**See {@link #documentToRemove}. */
	public Document getDocumentToRemove() {
		return documentToRemove;
	}
	/**See {@link #documentToRemove}. */
	public void setDocumentToRemove(Document documentToRemove) {
		this.documentToRemove = documentToRemove;
	}

	/**See {@link #clickedPacketName}. */
	public String getClickedPacketName() {
		return clickedPacketName;
	}
	/**See {@link #clickedPacketName}. */
	public void setClickedPacketName(String clickedPacketName) {
		this.clickedPacketName = clickedPacketName;
	}

	/**See {@link #showDetail}. */
	public boolean getShowDetail() {
		return showDetail;
	}
	/**See {@link #showDetail}. */
	public void setShowDetail(boolean showDetail) {
		this.showDetail = showDetail;
	}

	/** Returns the List of selected DocRowItems from the DocumentListBean. */
	public List<DocRowItem> getCheckBoxSelectedItems() {
		return DocumentListBean.getInstance().getCheckBoxSelectedItems();
	}

	public Packet getPacket() {
		return packet;
	}

	public void setPacket(Packet packet) {
		this.packet = packet;
	}

}
