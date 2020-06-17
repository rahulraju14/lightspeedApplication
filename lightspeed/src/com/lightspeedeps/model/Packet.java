package com.lightspeedeps.model;

import java.util.Date;
import java.util.List;

import javax.persistence.*;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import com.lightspeedeps.dao.PacketDAO;
import com.lightspeedeps.type.PacketStatus;

/** The Packet entity is basically used to group the Document into one object
 * and then giving the user the ability to distribute multiple Documents at a time.
 * <p>
 * Packets are specific to a Production/Project accordingly.
 * @author root
 *
 */
@NamedQueries ({
	@NamedQuery (name = Packet.GET_PACKET_LIST_BY_PRODUCTION_PROJECT , query = "from Packet p where p.production =:production and p.project =:project"),
	@NamedQuery (name = Packet.FIND_PACKET_BY_NAME_AND_PRODUCTION, query = "from Packet p where p.name =:packetName and p.production =:production"),
	@NamedQuery (name = Packet.FIND_PACKET_NAME_BY_PRODUCTION_AND_PROJECT, query = "from Packet p where p.name =:packetName and p.production =:production and p.project =:project")
})
@Entity
@Table(name = "packet" ,uniqueConstraints = @UniqueConstraint(columnNames = {
		"Name", "Production_Id", "Project_Id"}))
public class Packet extends PersistentObject<Packet> implements Comparable<Packet> {

	private static final long serialVersionUID = 440885346615882701L;

	public static final String GET_PACKET_LIST_BY_PRODUCTION_PROJECT = "getPacketListByProductionProject";
	public static final String FIND_PACKET_BY_NAME_AND_PRODUCTION = "findPacketByNameAndProduction";
	public static final String FIND_PACKET_NAME_BY_PRODUCTION_AND_PROJECT = "findPacketNameByProductionAndProject";

	public static final String SORT_KEY_NAME = "name";

	/** The displayed name of the Packet. */
	private String name;

	/** The description of the Packet. */
	private String description;

	/** The time when Packet was created. */
	private Date created;

	/** The time when Packet was last modified. */
	private Date lastModified;

	/** The status of the Packet, default is ACTIVE */
	private PacketStatus status;

	/** The recipient count of the packet*/
	private Integer recipientCount;

	/** The production this packet is defined for*/
	private Production production;

	/** The project this packet is defined for*/
	private Project project;

	/** The list of documents */
	private List<Document> documentList;

	/** Used in the row selection from UI, true if one of the packet row is selected*/
	@Transient
	private boolean selected;

	/** The count of documents in the packet*/
	@Transient
	private Integer documentCount;

	/**the default constructor */
	public Packet () {

	}

	/** See {@link #name}. */
	@Column(name = "Name", nullable = false, length = 50)
	public String getName() {
		return name;
	}
	/** See {@link #name}. */
	public void setName(String name) {
		this.name = name;
	}

	/** See {@link #description}. */
	@Column(name = "Description" , length = 1000)
	public String getDescription() {
		return description;
	}
	/** See {@link #description}. */
	public void setDescription(String description) {
		this.description = description;
	}

	/** See {@link #created}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Created", length = 0)
	public Date getCreated() {
		return created;
	}
	/** See {@link #created}. */
	public void setCreated(Date created) {
		this.created = created;
	}

	/** See {@link #lastModified}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "LastModified", length = 0)
	public Date getLastModified() {
		return lastModified;
	}
	/** See {@link #lastModified}. */
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	/** See {@link #status}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Status" , nullable = false, length = 30)
	public PacketStatus getStatus() {
		return status;
	}
	/** See {@link #status}. */
	public void setStatus(PacketStatus status) {
		this.status = status;
	}

	/** See {@link #recipientCount}. */
	@Column(name = "RecipientCount")
	public Integer getRecipientCount() {
		return recipientCount;
	}
	/** See {@link #recipientCount}. */
	public void setRecipientCount(Integer recipientCount) {
		this.recipientCount = recipientCount;
	}

	/** See {@link #production}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Production_Id")
	public Production getProduction() {
		return production;
	}
	/** See {@link #production}. */
	public void setProduction(Production production) {
		this.production = production;
	}

	/** See {@link #project}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Project_Id")
	public Project getProject() {
		return project;
	}
	/** See {@link #project}. */
	public void setProject(Project project) {
		this.project = project;
	}

	/** See {@link #documentList}. */
	@ManyToMany(
			targetEntity=Document.class,
			cascade={CascadeType.PERSIST, CascadeType.MERGE}
		)
	@JoinTable( name = "packet_document",
			joinColumns = {@JoinColumn(name = "Packet_Id")},
			inverseJoinColumns = {@JoinColumn(name = "Document_Id")}
			)
	@OrderBy("listOrder") // LS-4600
	public List<Document> getDocumentList() {
		return documentList;
	}
	/** See {@link #documentList}. */
	public void setDocumentList(List<Document> documentList) {
		this.documentList = documentList;
	}

	/** See {@link #selected}. */
	@Transient
	public boolean isSelected() {
		return selected;
	}
	/** See {@link #selected}. */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/** See {@link #documentCount}. */
	@Transient
	public Integer getDocumentCount() {
		return PacketDAO.getInstance().findDocumentCountByPacketId(id);
	}
	/** See {@link #documentCount}. */
	public void setDocumentCount(Integer documentCount) {
		this.documentCount = documentCount;
	}

	/**
	 * The default comparison for packet, which uses the
	 * packet name.
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Packet packet) {
		int ret = 1;
		if (packet != null) {
			return getName().compareToIgnoreCase(packet.getName());
		}
		return ret;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
		return result;
	}

	/**
	 * Compares Department objects based on their database id and/or name.
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		Packet other = null;
		try {
			other = (Packet)obj;
		}
		catch (Exception e) {
			return false;
		}
		if (getId() == null) {
			if (other.getId() != null)
				return false;
		}
		return getId().equals(other.getId());
	}


}
