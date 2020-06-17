package com.lightspeedeps.model;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import com.lightspeedeps.type.FileAccessType;
import com.lightspeedeps.type.Permission;

/**
 * RoleGroup entity. A grouping of Roles, used primarily to manage
 * permissions.  Every Role belongs to a single RoleGroup.
 *
 */
@Entity
@Table(name = "role_group", uniqueConstraints = @UniqueConstraint(columnNames = "Name"))
public class RoleGroup extends PersistentObject<RoleGroup> {

	private static final long serialVersionUID = -7133581213310543737L;

	// Fields

	/** The unique name of the RoleGroup. Unused at this time. */
	private String name;

	/** A description of the RoleGroup. Unused at this time. */
//	private String description;

	/** The class of file access (within the repository) granted to all
	 * Roles in this RoleGroup. */
	private FileAccessType fileAccess;

	/** A 64-bit mask that defines which permissions are granted to any Role
	 * in this RoleGroup. */
	private long permissionMask;

	/* The Set of Role's belonging to this RoleGroup. This Hibernate-managed
	 * collection is not currently needed in the application. */
//	private Set<Role> roles = new HashSet<Role>(0);

	/* The Set of Permission objects, which were previously stored in a table. */
//	private Set<Permission> permissions = new HashSet<Permission>(0);

	/** The Set of Permission values granted to all Roles in this
	 * RoleGroup.  This Set is generated from the PermissionMask when
	 * needed. */
	@Transient
	private Set<Permission> permissions;

	// Constructors

	/** default constructor */
	public RoleGroup() {
	}

	// Property accessors

	@Column(name = "Name", unique = true, nullable = false, length = 50)
	public String getName() {
		return this.name;
	}
	public void setName(String name) {
		this.name = name;
	}

//	@Column(name = "Description", length = 1000)
//	public String getDescription() {
//		return this.description;
//	}
//	public void setDescription(String description) {
//		this.description = description;
//	}

	/** See {@link #permissionMask}. */
	@Column(name = "Permission_Mask", nullable = false)
	public long getPermissionMask() {
		return permissionMask;
	}
	/** See {@link #permissionMask}. */
	public void setPermissionMask(long permissionMask) {
		this.permissionMask = permissionMask;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "File_Access", nullable = false, length = 30)
	public FileAccessType getFileAccess() {
		return this.fileAccess;
	}
	public void setFileAccess(FileAccessType fileAccess) {
		this.fileAccess = fileAccess;
	}

	/** See {@link #permissions}. */
	@Transient
	public Set<Permission> getPermissions() {
		if (permissions == null) {
			permissions = Permission.createPermissionSet(getPermissionMask());
		}
		return permissions;
	}

	public boolean hasPermission(Permission permission) {
		return (getPermissionMask() & permission.getMask()) != 0;
	}

//	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "roleGroup")
//	public Set<Role> getRoles() {
//		return this.roles;
//	}
//	public void setRoles(Set<Role> roles) {
//		this.roles = roles;
//	}

//	@ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//	@JoinTable(name = "role_group_permission", joinColumns = { @JoinColumn(name = "Role_Group_Id", nullable = false, updatable = false) }, inverseJoinColumns = { @JoinColumn(name = "Permission_Id", nullable = false, updatable = false) })
//	public Set<Permission> getPermissions() {
//		return this.permissions;
//	}
//
//	public void setPermissions(Set<Permission> permissions) {
//		this.permissions = permissions;
//	}

}