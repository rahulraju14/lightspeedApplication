package com.lightspeedeps.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;


/**
 * Folder entity.
 *
 * @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "folder", uniqueConstraints = @UniqueConstraint(columnNames = {
		"Parent_Id", "Name" }))
public class Folder extends PersistentObject<Folder> {

	private static final long serialVersionUID = 7376339666663203338L;

	public static final int MAX_NAME_LENGTH = 100;

	// Fields

	private Folder folder;
	private User user;
	private String name;
	private Boolean privateB;
	private Date created;
	private List<Document> documents = new ArrayList<Document>(0);
	private List<Folder> folders = new ArrayList<Folder>(0);
	private List<DocumentChain> documentChains = new ArrayList<DocumentChain>(0);
	//private Set<Production> productions = new HashSet<Production>(0);

	// Constructors

	/** default constructor */
	public Folder() {
	}

	public Folder(String nam, User usr, Folder parent, boolean privat, Date create) {
		name = nam;
		user = usr;
		folder = parent;
		privateB = privat;
		created = create;
	}

	// Property accessors

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Parent_Id")
	public Folder getFolder() {
		return folder;
	}
	public void setFolder(Folder folder) {
		this.folder = folder;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Owner_Id")
	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}

	@Column(name = "Name", nullable = false, length = 100)
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "Private", nullable = false)
	public Boolean getPrivate() {
		return privateB;
	}
	public void setPrivate(Boolean bool) {
		privateB = bool;
	}

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "folder")
	@LazyCollection(LazyCollectionOption.FALSE)
	@OrderBy("name")
	public List<Document> getDocuments() {
		return documents;
	}
	public void setDocuments(List<Document> documents) {
		this.documents = documents;
	}

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "folder")
	@LazyCollection(LazyCollectionOption.FALSE)
	@OrderBy("name")
	public List<Folder> getFolders() {
		return folders;
	}
	public void setFolders(List<Folder> folders) {
		this.folders = folders;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created", length = 0)
	public Date getCreated() {
		return created;
	}
	public void setCreated(Date created) {
		this.created = created;
	}

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "folder")
	@LazyCollection(LazyCollectionOption.FALSE)
	@OrderBy("name")
	public List<DocumentChain> getDocumentChains() {
		return documentChains;
	}

	public void setDocumentChains(List<DocumentChain> documentChains) {
		this.documentChains = documentChains;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += "name=" + (getName()==null ? "null" : getName());
		s += "]";
		return s;
	}

}