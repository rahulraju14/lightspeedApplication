/**
 * PermissionRequestBean.java
 */
package com.lightspeedeps.web.project;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.component.html.HtmlDataTable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.ServiceFinder;

/**
 * The purpose of this bean is to provide access, through the binding=
 * attribute, to editing areas (dataTables) of the Permissions page. This needs
 * to be a request-scoped bean, because the bound fields are not Serializable,
 * and so can't be members of a View or Session scoped bean, which must be
 * Serializable.
 * <p>
 * So we bind the controls to this bean, and our "real" backing bean,
 * PermissionBean, simply finds this bean when access is needed to the bound
 * controls.
 */
@ManagedBean
@RequestScoped
public class PermissionRequestBean {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(PermissionRequestBean.class);

	/** The JSP control that displays all of the permissions data. */
	private HtmlDataTable permTable;

	public PermissionRequestBean() {
	}

	public static PermissionRequestBean getInstance() {
		return (PermissionRequestBean)ServiceFinder.findBean("permissionRequestBean");
	}

	/** See {@link #permTable}. */
	public HtmlDataTable getPermTable() {
		return permTable;
	}
	/** See {@link #permTable}. */
	public void setPermTable(HtmlDataTable permTable) {
		this.permTable = permTable;
	}

}
