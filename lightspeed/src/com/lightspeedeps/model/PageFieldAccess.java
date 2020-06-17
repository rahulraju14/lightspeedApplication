package com.lightspeedeps.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import com.lightspeedeps.type.AllowedMode;

/**
 * PageFieldAccess entity. Each record associates a particular Permission with a
 * page-field-access key. So the user's set of Permissions turns into a set of
 * page-field-access (PFA) keys; and the JSP code looks for particular PFA keys
 * to determine whether particular screen widgets (buttons, inputs, etc.) should
 * be displayed for the current user.
 * <p>
 * Note that the set of PFAs given to a user may also be controlled (filtered)
 * by the production type, or attributes of the production, such as "Onboarding
 * enabled".
 */
@Entity
@Table(name = "page_field_access")
public class PageFieldAccess extends PersistentObject<PageFieldAccess> {
	private static final long serialVersionUID = - 8440845101586940522L;

	// Fields
	/** The Permission value that enables this particular pageField to be
	 * included for a user. */
	private Integer permissionId;
	/** The page-field key, such as "2.1,edit" which is used by the JSP code
	 * to validate access to a particular field, or set of fields, on a page. */
	private String pageField;
	/** Specifies in which modes (e.g., Onboarding, Tours) this access applies. */
	private AllowedMode allowedMode;

	// Constructors

	/** default constructor */
	public PageFieldAccess() {
	}

	/** full constructor */
/*	public PageFieldAccess(Permission permission, String page, String field) {
		this.permission = permission;
		this.page = page;
		this.field = field;
	}
*/
	// Property accessors

//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "Permission_Id", nullable = false)
//	public Permission getPermission() {
//		return this.permission;
//	}
//	public void setPermission(Permission permission) {
//		this.permission = permission;
//	}

	/** See {@link #permissionId}. */
	@Column(name = "Permission_Id", nullable = false)
	public Integer getPermissionId() {
		return permissionId;
	}
	/** See {@link #permissionId}. */
	public void setPermissionId(Integer permissionId) {
		this.permissionId = permissionId;
	}

	@Column(name = "Page_field", nullable = false, length = 30)
	public String getPageField() {
		return this.pageField;
	}
	public void setPageField(String page) {
		this.pageField = page;
	}

	/** See {@link #allowedMode}*/
	@Enumerated(EnumType.STRING)
	@Column(name = "Allowed_Mode", nullable = false, length = 30)
	public AllowedMode getAllowedMode() {
		return this.allowedMode;
	}
	/** See {@link #allowedMode}*/
	public void setAllowedMode(AllowedMode t) {
		this.allowedMode = t;
	}

}