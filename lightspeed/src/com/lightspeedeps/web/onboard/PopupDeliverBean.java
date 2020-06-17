package com.lightspeedeps.web.onboard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.PacketDAO;
import com.lightspeedeps.model.Packet;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.util.app.ServiceFinder;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.popup.PopupHolder;

/**
 * This class is the backing bean for the Deliver popup window,
 * of Start Status mini tab under Onboarding sub-main tab.
 */
@ManagedBean
@ViewScoped
public class PopupDeliverBean extends PopupBean implements Serializable {

	private static final long serialVersionUID = -3340123455854694853L;

	private static final Log log = LogFactory.getLog(PopupDeliverBean.class);

	/** Backing field for radio buttons -- indicates the type document,
	 * or start packet is selected by the user */
	private String selectType = START_PACKET_TYPE;
	public static final String START_PACKET_TYPE = "p";
	public static final String DOCUMENT_TYPE = "d";

	/** the list of packets in the current production to be shown on the select menu */
	private List<SelectItem> packetList;

	/** the list of document in the current production to be shown on the select menu */
	private List<SelectItem> documentList;

	/** The current production. */
	private Production production = SessionUtils.getProduction();

	/** Object variable used to hold the instance of currently selected packet from the select item list */
	private Object selectedPacket;

	/** Object variable used to hold the instance of currently selected document from the select item list */
	private Object selectedDocument;

	/** if true then the document will be distributed immediately to the employee.
	 * If false, then the status of the document will be Pending.*/
	private boolean isPending;
	/**  replace the Deliver to Create*/
	private static final String CREATE_STARTS_PREFIX= "StatusListBean.CreateStarts.";

	public PopupDeliverBean() {
		log.debug("");
	}

	public static PopupDeliverBean getInstance(){
		return (PopupDeliverBean)ServiceFinder.findBean("popupDeliverBean");
	}

	@Override
	public void show(PopupHolder holder, int act, String prefix) {
		log.debug("");
		String newPrefix;
		selectType = START_PACKET_TYPE;
		if (getPacketList().size() == 0) {
			selectType = DOCUMENT_TYPE;
		}
		//LS-2190 Change button label from "Deliver" to "Create" on the Issues Starts popup box
		if (production.getType().isCanadaTalent()) {

			newPrefix = CREATE_STARTS_PREFIX;
		}
		else {
			newPrefix = prefix;
		}
		super.show(holder, act, prefix + "Title",
				prefix + "Text", newPrefix + "Ok", "Confirm.Cancel");
	}

	/** The method used to create the Packet list
	 * @return {@link packetList}
	 */
	private List<SelectItem> createPacketList() {
		List<Packet> list = new ArrayList<>();
		if (production != null) {
			if (production.getType().isAicp()) {
				Map<String, Object> values = new HashMap<>();
				values.put("production", production);
				values.put("project", SessionUtils.getCurrentProject());
				list = PacketDAO.getInstance().findByNamedQuery(Packet.GET_PACKET_LIST_BY_PRODUCTION_PROJECT, values);
			}
			else {
				list = PacketDAO.getInstance().findByProperty("production", production);

			}
		}
		if (list != null) {
			packetList = new ArrayList<>();
			for (Packet packet : list) {
				if (packet.getDocumentList() != null && (! packet.getDocumentList().isEmpty())) {
					packetList.add(new SelectItem(packet.getId(), packet.getName()));
				}
			}
		}
		return packetList;
	}

	/** The method used to create the Document list
	 * @return {@link documentList}
	 */
	private List<SelectItem> createDocumentList() {
		documentList = StatusListBean.createDocumentList();
		return documentList;
	}

	public void listenSelectType(ValueChangeEvent event) {
		selectType = (String) event.getNewValue();
		switch (selectType) {
		case START_PACKET_TYPE:
			selectedDocument = null;
			break;
		case DOCUMENT_TYPE:
			selectedPacket = null;
			break;
		default:
			break;
		}
	}

	/**See {@link #selectType}. */
	public String getSelectType() {
		return selectType;
	}
	/**See {@link #selectType}. */
	public void setSelectType(String selectType) {
		this.selectType = selectType;
	}

	/**See {@link #packetList}. */
	public List<SelectItem> getPacketList() {
		if (packetList == null) {
			packetList = createPacketList();
		}
		return packetList;
	}
	/**See {@link #packetList}. */
	public void setPacketList(List<SelectItem> packetList) {
		this.packetList = packetList;
	}

	/**See {@link #documentList}. */
	public List<SelectItem> getDocumentList() {
		if (documentList == null) {
			documentList = createDocumentList();
		}
		return documentList;
	}
	/**See {@link #documentList}. */
	public void setDocumentList(List<SelectItem> documentList) {
		this.documentList = documentList;
	}

	/**See {@link #selectedPacket}. */
	public Object getSelectedPacket() {
		return selectedPacket;
	}
	/**See {@link #selectedPacket}. */
	public void setSelectedPacket(Object selectedPacket) {
		this.selectedPacket = selectedPacket;
	}

	/**See {@link #selectedDocument}. */
	public Object getSelectedDocument() {
		return selectedDocument;
	}
	/**See {@link #selectedDocument}. */
	public void setSelectedDocument(Object selectedDocument) {
		this.selectedDocument = selectedDocument;
	}

	/**See {@link #isPending}. */
	public boolean getPending() {
		return isPending;
	}
	/**See {@link #isPending}. */
	public void setPending(boolean isPending) {
		this.isPending = isPending;
	}
}
