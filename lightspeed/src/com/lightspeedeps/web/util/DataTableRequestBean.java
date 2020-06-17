package com.lightspeedeps.web.util;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.icefaces.ace.component.datatable.DataTable;

import com.lightspeedeps.util.app.ServiceFinder;

/**
 * The purpose of this bean is to provide access, through the binding=
 * attribute to an Ace DataTable.
 * <p>
 * There is a problem that the bound fields are not Serializable, and so
 * can't be members of a View scoped bean, which must be Serializable.
 * <p>
 * This needs to be a request-scoped bean -- the binding= forces a new
 * instantiation of the bean on each request, even if the bean were marked View
 * scope. So we bind the control to this bean, and our "real" View-scoped
 * backing bean simply finds this bean if it needs access to the bound control.
 * <p>
 * If we don't use this technique, the View-scoped bean gets instantiated
 * on every view cycle.
 */
@ManagedBean
@RequestScoped
public class DataTableRequestBean {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(DataTableRequestBean.class);

	/** The bound DataTable */
	private DataTable dataTable;

	public static DataTableRequestBean getInstance() {
		return (DataTableRequestBean)ServiceFinder.findBean("dataTableRequestBean");
	}

	public DataTableRequestBean() {

	}

	/** See {@link #dataTable}. */
	public DataTable getDataTable() {
		return dataTable;
	}

	/** See {@link #dataTable}. */
	public void setDataTable(DataTable dataTable) {
		this.dataTable = dataTable;
	}

}
