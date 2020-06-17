package com.lightspeedeps.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.Document;
import com.lightspeedeps.model.Packet;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.type.PacketStatus;
import com.lightspeedeps.util.app.SessionUtils;

public class PacketDAO extends BaseTypeDAO<Packet> {
	private static final Log log = LogFactory.getLog(PacketDAO.class);

	public static PacketDAO getInstance() {
		return (PacketDAO)getInstance("PacketDAO");
	}

	/** Save the given data into a new Packet
	 * @param name
	 * @param description
	 * @param created
	 * @param lastModified
	 * @param status
	 * @param recipientCount
	 * @return
	 */
	@Transactional
	public Integer create(String name, String description) {
		Packet packet = new Packet();
		packet.setName(name);
		packet.setDescription(description);
		Date date = new Date();
		packet.setCreated(date);
		packet.setLastModified(date);
		packet.setStatus(PacketStatus.ACTIVE);
		packet.setRecipientCount(1);
		Production production = SessionUtils.getProduction();
		packet.setProduction(production);
		if (production != null) {
			if (production.getType().isAicp()) {
				packet.setProject(SessionUtils.getCurrentProject());
			}
			else {
				packet.setProject(null);
			}
		}
		Integer packetId = save(packet);
		log.debug("id returned "+packetId);
		return packetId;
	}

	/** Delete the given Packet
	 * @param packet the packet to be deleted
	 */
	@Transactional
	public void delete(Packet packet) {
		super.delete(packet);
	}

	/*public Packet findPacketByNameAndProduction(String userPacketName,Integer id) {
		String query = "from Packet where Name = ? and Production_Id = ?";
		List<Object> valueList = new ArrayList<Object>();
		valueList.add(userPacketName);
		valueList.add(id);
		return findOne(query, valueList.toArray());
	}*/

	public Integer findDocumentCountByPacketId(Integer id) {
		Packet packet = findById(id);
		Integer count = 0;
		if (packet.getDocumentList() != null) {
			List<Document> packetDocuments = new ArrayList<>();
			for (Document doc: packet.getDocumentList()) {
				if (!doc.getDeleted()) {
					packetDocuments.add(doc);
				}
			}
			count = packetDocuments.size();
		}
		return count;
	}
}
