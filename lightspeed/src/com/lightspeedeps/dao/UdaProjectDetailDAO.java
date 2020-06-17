package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.UdaProjectDetail;
import com.lightspeedeps.util.app.ServiceFinder;

/**
 * A data access object (DAO) providing persistence and search support for
 * UdaProjectDetail entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.UdaProjectDetail
 */
public class UdaProjectDetailDAO extends BaseTypeDAO<UdaProjectDetail> {
	@SuppressWarnings("unused")
	private static final Log LOG = LogFactory.getLog(UdaProjectDetailDAO.class);

	// property constants
//	public static final String PRODUCER_NAME = "producerName";
//	public static final String PRODUCER_ADDRESS = "producerAddress";
//	public static final String PRODUCER_PHONE = "producerPhone";
//	public static final String PRODUCER_EMAIL = "producerEmail";
//	public static final String RESPONSIBLE_NAME = "responsibleName";
//	public static final String NO_UDA_MEMBER = "noUDAMember";
//	public static final String ADVERTISER_NAME = "advertiserName";
//	public static final String COMMERCIAL_TITLE = "commercialTitle";
//	public static final String COMMERCIAL_DESCRIPTION = "commercialDescription";
//	public static final String COMMERCIAL_VERSION = "commercialVersion";
//	public static final String PRODUCT_NAME = "productName";

	public static UdaProjectDetailDAO getInstance() {
		return (UdaProjectDetailDAO) ServiceFinder.findBean("UdaProjectDetailDAO");
	}

}
