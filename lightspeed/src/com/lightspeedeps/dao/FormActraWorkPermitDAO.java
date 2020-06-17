package com.lightspeedeps.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.FormActraWorkPermit;
import com.lightspeedeps.util.app.ServiceFinder;

/**
 * A data access object (DAO) providing persistence and search support for
 * FormActraWorkPermit entities.
 * <p>
 * Note that most of the common functions (save, delete, findById, etc.) are in
 * one of the superclasses {@link com.lightspeedeps.dao.BaseDAO} or
 * {@link com.lightspeedeps.dao.BaseTypeDAO}.
 * <p>
 * Every DAO class implements a getInstance() method, which is the way they
 * should be accessed.
 *
 * @see com.lightspeedeps.model.FormActraWorkPermit
 */
public class FormActraWorkPermitDAO extends BaseTypeDAO<FormActraWorkPermit> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(FormActraWorkPermitDAO.class);

	//property constants
//	public static final String PROFESSIONAL_NAME = "professionalName";
//	public static final String CITIZENSHIP = "citizenship";
//	public static final String LEGAL_NAME = "legalName";
//	public static final String HOME_PHONE = "homePhone";
//	public static final String CELL_PHONE = "cellPhone";
//	public static final String TALENT_EMAIL_ADDRESS = "talentEmailAddress";
//	public static final String TALENT_ADDRESS_ID = "talentAddressId";
//	public static final String TALENT_AGENCY_NAME = "talentAgencyName";
//	public static final String SAG_AFTRA_MEMBER = "sagAftraMember";
//	public static final String EQUITY_MEMBER = "equityMember";
//	public static final String APPRENTICE_MEMBER = "apprenticeMember";
//	public static final String APPRENTICE_NUM = "apprenticeNum";
//	public static final String MEMBERS_AUDITIONED_NUM = "membersAuditionedNum";
//	public static final String MEMBERS_AUDITIONED_NAMES = "membersAuditionedNames";
//	public static final String COMMERCIAL_NAME = "commercialName";
//	public static final String CHAR_NAME_DESC = "charNameDesc";
//	public static final String NUM_COM = "numCom";
//	public static final String COM_ID = "comId";
//	public static final String COM_TYPE_TV = "comTypeTv";
//	public static final String COM_TYPE_RADIO = "comTypeRadio";
//	public static final String COM_TYPE_DIGITAL_MEDIA = "comTypeDigitalMedia";
//	public static final String PERFORMANCE_CATEGORY = "performanceCategory";
//	public static final String COM_LOCATION = "comLocation";
//	public static final String APPLICANT_SIGN_ID = "applicantSignId";
//	public static final String WORK_PERMIT_FEE = "workPermitFee";
//	public static final String WORK_PERMIT_NUM = "workPermitNum";
//	public static final String PAID_BY = "paidBy";
//	public static final String PAYMENT_METHOD = "paymentMethod";
//	public static final String RECEIPT_BY_EMAIL = "receiptByEmail";
//	public static final String RECEIPT_TO = "receiptTo";
//	public static final String CC_HOLDER_NAME = "ccHolderName";
//	public static final String CC_HOLDER_SIGN_ID = "ccHolderSignId";
//	public static final String CC_NUM = "ccNum";
//	public static final String ENGAGER_ID = "engagerId";
//	public static final String APPROVED_DENIED_BY = "approvedDeniedBy";
//	public static final String QUALIFYING = "qualifying";
//	public static final String DENIAL_REASON = "denialReason";

	public static FormActraWorkPermitDAO getInstance() {
		return (FormActraWorkPermitDAO) ServiceFinder.findBean("FormActraWorkPermitDAO");
	}

}
