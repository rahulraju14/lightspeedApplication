package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.FormModelRelease;

/**
 * A data access object (DAO) providing persistence and search support for
 * FormA1Contract entities. Transaction control of the save(), update() and
 * delete() operations can directly support Spring container-managed
 * transactions or they can be augmented to handle user-managed Spring
 * transactions. Each of these methods provides additional information for how
 * to configure it for the desired type of transaction control.
 *
 * @see com.lightspeedeps.model.FormModelRelease
 * @author MyEclipse Persistence Tools
 */
public class FormModelReleaseDAO extends BaseTypeDAO<FormModelRelease> {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(FormModelReleaseDAO.class);


	public static FormModelReleaseDAO getInstance() {
		return (FormModelReleaseDAO) getInstance("FormModelReleaseDAO");
	}

}
