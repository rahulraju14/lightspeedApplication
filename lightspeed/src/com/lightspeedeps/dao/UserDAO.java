package com.lightspeedeps.dao;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.*;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;

import com.lightspeedeps.model.*;
import com.lightspeedeps.object.LoggedException;
import com.lightspeedeps.type.ActionType;
import com.lightspeedeps.type.ChangeType;
import com.lightspeedeps.type.UserStatus;
import com.lightspeedeps.util.app.*;

/**
 * A data access object (DAO) providing persistence and search support for User
 * entities. Transaction control of the save(), update() and delete() operations
 * can directly support Spring container-managed transactions or they can be
 * augmented to handle user-managed Spring transactions. Each of these methods
 * provides additional information for how to configure it for the desired type
 * of transaction control.
 *
 * @see com.lightspeedeps.model.User
 */
public class UserDAO extends BaseTypeDAO<User> {
	private static final Log log = LogFactory.getLog(UserDAO.class);
	// property constants
	public static final String EMAIL_ADDRESS = "emailAddress";
	public static final String ACCOUNT_NUMBER = "accountNumber";
	public static final String STATUS = "status";
	public static final String GST_NUMBER = "gstNumber";
	public static final String QST_NUMBER = "qstNumber";
	public static final String FULL_MEMBER_NUM = "fullMemberNum";
//	private static final String DISPLAY_NAME = "displayName";

	//	private static final String PREFIX = "prefix";
//	private static final String FIRST_NAME = "firstName";
//	private static final String MIDDLE_NAME = "middleName";
//	private static final String LAST_NAME = "lastName";
//	private static final String SAG_MEMBER = "sagMember";
//	private static final String DGA_MEMBER = "dgaMember";
//	private static final String AFTRA_MEMBER = "aftraMember";
//	private static final String IATSE_MEMBER = "iatseMember";
//	private static final String TEAMSTERS_MEMBER = "teamstersMember";
//	private static final String HOME_PHONE = "homePhone";
//	private static final String BUSINESS_PHONE = "businessPhone";
//	private static final String CELL_PHONE = "cellPhone";
//	private static final String PRIMARY_PHONE_INDEX = "primaryPhoneIndex";
//	private static final String IMDB_LINK = "imdbLink";
//	private static final String IM_SERVICE = "imService";
//	private static final String IM_ADDRESS = "imAddress";
//	private static final String PSEUDONYM = "pseudonym";
//	private static final String MINOR = "minor";

//	private static final String LOGIN_ALLOWED = "loginAllowed";
//	private static final String LOCKED_OUT = "lockedOut";
//	private static final String NEW_PASSWORD_REQUIRED = "newPasswordRequired";
//	private static final String FAILED_LOGON_COUNT = "failedLogonCount";
//	private static final String FILE_ACCESS = "fileAccess";
//	private static final String ENCRYPTED_PASSWORD = "encryptedPassword";

	public static UserDAO getInstance() {
		return (UserDAO)getInstance("UserDAO");
	}

	@SuppressWarnings("unchecked")
	public List<User> findAllActive() {
		String queryString = "from User where status <> '" + UserStatus.DELETED.name() + "'";
		return find(queryString);
	}

	public List<User> findByGSTNumber(Object gstNumber) {
		return findByProperty(GST_NUMBER, gstNumber);
	}

	public List<User> findByQSTNumber(Object qstNumber) {
		return findByProperty(QST_NUMBER, qstNumber);
	}

	public List<User> findByFullMemberNum(Object fullMemberNum) {
		return findByProperty(FULL_MEMBER_NUM, fullMemberNum);
	}

	/**
	 * Find the User(s) that match a given email address; there should be
	 * either zero or one!
	 * <p>
	 * This method seems to be the one most plagued by the transient SQL
	 * CommunicationsException errors. One reason is that the errors seem to
	 * happen after the system has been unused for a while, so the first use is
	 * a login, and this is the first database access during login processing.
	 *
	 * So we've built a recovery loop into this method, to retry the SQL query
	 * up to 4 times in case of a failure.
	 *
	 * @param emailAddr
	 * @return A List of User objects which should contain either a single User
	 *         (if there was a match), or no entries (if there was not a match).
	 *         May return null if there was a database access error.
	 */
	public List<User> findByEmailAddress(String emailAddr) {
		List<User> usrlist = null;
		for (int i=0; i < 4 && usrlist == null; i++) {
			try {
				log.debug("i=" + i);
				usrlist = findByUserNameTest(emailAddr);
			}
			catch (DataAccessException d) {
				log.error("data exception: ", d);
				usrlist = null;
			}
			catch (org.jasypt.exceptions.EncryptionOperationNotPossibleException ce) {
				log.error("decryption error?: ", ce);
				usrlist = null;
				break; // do not retry this case, it can "succeed" but with encrypted fields corrupted
			}
			catch (Exception e) {
				log.error("exception: ", e);
				usrlist = null;
			}
		}
		return usrlist;
	}

	@SuppressWarnings("unchecked")
	private List<User> findByUserNameTest(String userName) {
//		if (Math.random() < 0.3) { // for testing
//			System.out.println("throwing fake error!!");
//			throw new DataAccessResourceFailureException("test exception handling");
//		}
		String queryString = "from User where " + EMAIL_ADDRESS + "= ?";
		// do NOT user superclass find(), as a failure there will interfere with our recovery here.
		// -- so go direct to Hibernate
		return (List<User>)getHibernateTemplate().find(queryString, userName);
	}

	public static UserDAO getFromApplicationContext(ApplicationContext ctx) {
		return (UserDAO) ctx.getBean("UserDAO");
	}

//	/**
//	 * Find the total of all the "failedLogonCount" values for all users in the database.
//	 * @return The total (may be zero)
//	 */
//	public long findTotalFailedLogonCount() {
//		long count = 0;
//		try {
//			count = (Long)find("select sum(u.failedLogonCount)from User u").get(0);
//		}
//		catch (DataAccessException d) {
//			log.error("data exception: ", d);
//		}
//		catch (Exception e) {
//			EventUtils.logError(e);
//		}
//		return count;
//	}

	/**
	 * Find the count of users in the database who are currently locked out.
	 * @return The total (may be zero)
	 */
	public long findLockedOutCount() {
		return findCount("select count(id) from User u where lockedOut = true", null);
	}

//	/**
//	 * Find the count of users in the database who were locked out within a
//	 * given time frame.
//	 *
//	 * @return The number of users whose "lock out" timestamp is within the
//	 *         range of the begin and ending timestamp parameters (may be zero).
//	 */
//	public long findLockedOutInRange(Date begin, Date end) {
//
//		String queryString = "select count(id) from User u "
//				+ " where lockedOut = true and "
//				+ " lock_out_time >= ? and "
//				+ " lock_out_time <= ? ";
//
//		Object[] values = {begin, end};
//
//		return findCount(queryString, values);
//	}

	/**
	 * Determine if an email address is already assigned to a User in the
	 * system.
	 *
	 * @param email The email address to check.
	 * @return True iff the given email address is already assigned to some User
	 *         in the system.
	 */
	@SuppressWarnings("unchecked")
	public boolean existsEmailAddress(String email) {
		boolean bRet = false;
		log.debug("name=" + email);
		String queryString = "select count(id) from User where " + EMAIL_ADDRESS + " = ? " ;
		List<Long> list = find(queryString, email);
		if (list.size() > 0 && list.get(0).longValue() > 0) {
			bRet = true;
		}
		log.debug("ret=" + bRet);
		return bRet;
	}

	/**
	 * Find the one User instance that matches the given email address.
	 *
	 * @return The matching User, or null if the email address is not found.
	 */
	public User findSingleUser(String emailAddr) {
		User obj = null;
		List<User> objs = findByEmailAddress(emailAddr);
		if ((objs != null) && (objs.size() > 0)) {
			obj = objs.get(0);
		}
		return obj;
	}

	/**
	 * Find all User`s who either: (a) have a Role in the given Production where
	 * that Role is in one of the given Department`s (specified by their
	 * database id values); or (b) have a Start Form specifying one of the given
	 * departments.
	 *
	 * @param idList A Collection of Department.id values.
	 * @param prod The Production of interest.
	 * @param project
	 * @return A non-null, but possibly empty, List of distinct User`s, where
	 *         each User either has a Role in the given Production that is
	 *         associated with one of the Department`s identified in the given
	 *         list of ids, or has a Start Form associated with one of those
	 *         Department`s.
	 */
	@SuppressWarnings("unchecked")
	public List<User> findByDepartments(Collection<Integer> idList, Production prod, Project project) {
		try {
			if (idList == null || idList.size() == 0) {
				return new ArrayList<>();
			}
			String deptIdList = "(";
			for (Integer id : idList) {
				deptIdList += id + ",";
			}
			deptIdList = deptIdList.substring(0,deptIdList.length()-1) + ")";

			String query;
			List<User> users;
			Object[] values = {prod, project};
			// Get the list of users based on project roles
			if (project == null) {
				query = "select distinct u from ProjectMember pm, User u " +
						"where pm.employment.role.department.id in " +
						deptIdList +
						" and pm.employment.contact.user = u " +
						" and pm.employment.contact.production = ? ";
				users = find(query, prod);
			}
			else {
				query = "select distinct u from ProjectMember pm, User u, Unit unit " +
						"where pm.employment.role.department.id in " +
						deptIdList +
						" and pm.employment.contact.user = u " +
						" and ( (pm.unit is null and pm.employment.contact.production = ?) " +
						"   or (pm.unit = unit and unit.project = ?) )";
				users = find(query, values);
			}

			// Then get the list based on StartForm entries
			query = "select distinct u from User u, StartForm sf " +
					"where sf.employment.role.department.id in " +
					deptIdList +
					" and sf.contact.user = u " +
					" and sf.contact.production = ? ";
			List<User> sfUsers;
			if (project == null) {
				sfUsers = find(query, prod);
			}
			else {
				query += " and sf.project = ? ";
				sfUsers = find(query, values);
			}

			// Add any users not already in the first list
			for (User user : sfUsers) {
				if (! users.contains(user)) {
					users.add(user);
				}
			}

			return users;
		}
		catch (RuntimeException re) {
			EventUtils.logError(re);
			throw re;
		}
	}

	/**
	 * @param prod The Production of interest.
	 * @return List of account numbers for all Users who are members of the
	 *         specified Production.
	 */
	public List<String> findAcctsByProduction(Production prod) {
		String query = "select u." + ACCOUNT_NUMBER + " from User u, Contact c where " +
				" c." + ContactDAO.USER + " = u and " +
				" c." + ContactDAO.PRODUCTION + " = ? ";

		@SuppressWarnings("unchecked")
		List<String> list = find(query, prod);
		return list;
	}

	/**
	 * Add or update the supplied User, after first updating the Image objects so
	 * they are associated with the given element.
	 *
	 * @param user The User to be saved or updated.
	 * @param images The Collection of images to associate with the element.
	 * @return The updated User.
	 */
	@Transactional
	public User update(User user, Collection<Image> images) {
		if (images != null) {
			for (Image image : images) {
				image.setUser(user);
			}
		}
		if (user.getId() == null) {
			user.setCreated(new Date());
			user.setCreatedBy(SessionUtils.getCurrentUser().getAccountNumber());
			user.setAccountNumber("NEW" + (int)(Math.random()*10000.)); // temporary
			save(user);
			user.setAccountNumber(ApplicationUtils.createAccountNumber(user.getId()));
			attachDirty(user);
		}
		else {
			user = merge(user);
		}
		return user;
	}

	/**
	 * Save the new User object given, after setting various fields
	 * appropriately. This is designed for a person  who is creating a
	 * new user on the behalf of that user. Currently this is being called
	 * from a web service.
	 * <p>
	 * A new Account number is assigned to the User at this point.
	 *
	 * @param user The User to be added to the database. The email address and
	 *            password fields should be set already.
	 * @param createdBy - The user who is creating the new user
	 * @return The updated User object.
	 */
	@Transactional
	public User createUser(User user, User createdBy) {
		if (user.getId() == null) {
			user.setCreated(new Date());
			user.setCreatedBy(createdBy.getAccountNumber());
			user.setAccountNumber("NEW" + (int)(Math.random()*10000.)); // temporary
			save(user);
			user.setAccountNumber(ApplicationUtils.createAccountNumber(user.getId()));
			attachDirty(user);
		}
		else {
			attachDirty(user);
		}
		return user;
	}

	/**
	 * Save the new User object given, after setting various fields
	 * appropriately. This is designed for a person registering themselves, so
	 * the status is set to Registered, not Pending.
	 * <p>
	 * A new Account number is assigned to the User at this point.
	 *
	 * @param user The User to be added to the database. The email address and
	 *            password fields should be set already.
	 * @return The updated User object.
	 */

	@Transactional
	public User createUser(User user) {
		user.setStatus(UserStatus.REGISTERED);
		user.setNewPasswordRequired(false);
		user.setAccountNumber("NEW" + (int)(Math.random()*10000.)); // temporary
		user.setCreated(new Date());
		user.setCreatedBy("self"); // temporary
		save(user);
		user.setAccountNumber(ApplicationUtils.createAccountNumber(user.getId()));
		user.setCreatedBy(user.getAccountNumber());
		attachDirty(user);
		ChangeUtils.logChange(ChangeType.USER, ActionType.CREATE,
				user, "New User registration");
		return user;
	}

	/**
	 * Remove a User from availability by marking its
	 * status "Deleted".
	 *
	 * @param user The User to be removed.
	 */
	@Transactional
	public void remove(User user) {
		user = findById(user.getId()); // refresh
		user.setStatus(UserStatus.DELETED);
		user.setEmailAddress('@' + user.getEmailAddress());
		attachDirty(user);
		ChangeUtils.logChange(ChangeType.USER, ActionType.DELETE,
				user, "User deleted");
	}


//	/**
//	 * Convert encrypted fields from v2.1 to v2.2. (rev 4439)
//	 */
//	@Transactional
//	public void convertUsers() {
//		int converted = 0;
//		int already = 0;
//
//		List<User> users = findAll();
//		for (User user : users) {
//			//log.debug(user.getId());
//			if (user.getEncryptedPassword() == null) {
//				if (user.getOldPassword() != null || user.getOldPin() != null) {
//					user.setEncryptedPassword(user.getOldPassword());
//					user.setPin(user.getOldPin());
//					converted++;
//				}
//			}
//			else {
//				log.debug("#" + user.getId() + " password already converted");
//				already++;
//			}
//		}
//
//		log.info("" + users.size() + " Users input for conversion from 2.1 to 2.2.");
//		log.info("" + already + " already had new password" );
//		log.info("" + converted + " Users converted");
//	}

	/** Method creates the filtered list of user according to the parameters passed
	 * @param firstName firstName property
	 * @param firstNameValue firstName value
	 * @param lastName lastName property
	 * @param lastNameValue lastName value
	 * @param emailAddress emailAddress property
	 * @param emailAddressValue emailAddress value
	 * @param status user status (pending,registered,deleted)
	 * @param pageSize no of users to fetch at a time
	 * @param first index of the user from where the list will start
	 * @return list of filtered users
	 */
	@SuppressWarnings("unchecked")
	public List<User> findByFilter(String firstName, String firstNameValue, String lastName, String lastNameValue, String emailAddress, String emailAddressValue, UserStatus status, int pageSize, int first) {
		try {
			Criteria criteria = createFilterCritera(firstName, firstNameValue, lastName,
					lastNameValue, emailAddress, emailAddressValue, status);
			criteria.setFirstResult(first);
			criteria.setMaxResults(pageSize);
			criteria.addOrder(Order.asc(lastName)).addOrder(Order.asc(firstName));
			List<User> list = criteria.list();
			log.debug("Count=" + list.size() + ", String to be matched =" + firstNameValue +"-"+ lastNameValue+"-"+"-"+ emailAddressValue+"-"+status);
			return list;
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw new LoggedException();
		}
	}

	/** Method used to get the count of the filtered user list.
	 * @param firstName firstName property
	 * @param firstNameValue firstName value
	 * @param lastName lastName property
	 * @param lastNameValue lastName value
	 * @param emailAddress emailAddress property
	 * @param emailAddressValue emailAddress value
	 * @param status user status (pending,registered,deleted)
	 * @return count of user
	 */
	public long findCountByFilter(String firstName, String firstNameValue, String lastName,
			String lastNameValue, String emailAddress, String emailAddressValue, UserStatus status) {
		long count = 0L;
		try {
			Criteria criteria = createFilterCritera(firstName, firstNameValue, lastName,
					lastNameValue, emailAddress, emailAddressValue, status);
			Projection pj = Projections.rowCount();
			criteria.setProjection(pj);
			@SuppressWarnings("unchecked")
			List<Long> list = criteria.list();
			if (list.size() > 0) {
				Object entry = list.get(0);
				if (entry instanceof Long) { // standard for count(x)
					count = (Long)entry;
				}
			}
		}
		catch (RuntimeException e) {
			EventUtils.logError(e);
			throw new LoggedException();
		}
		log.debug("count=" + count);
		return count;
	}

	/** Method creates the criteria for getting the filtered list of users
	 * @param firstName firstName property
	 * @param firstNameValue firstName value
	 * @param lastName lastName property
	 * @param lastNameValue lastName value
	 * @param emailAddress emailAddress property
	 * @param emailAddressValue emailAddress value
	 * @param status user status (pending,registered,deleted)
	 * @return criteria
	 */
	public Criteria createFilterCritera(String firstName, String firstNameValue, String lastName, String lastNameValue, String emailAddress, String emailAddressValue, UserStatus status) {
		Criteria criteria = getHibernateSession().createCriteria(User.class);

		if (firstNameValue != null) {
			firstNameValue = "%"+firstNameValue+"%";
			criteria.add(Restrictions.like(firstName, firstNameValue));
		}
		if (lastNameValue != null) {
			lastNameValue = "%"+lastNameValue+"%";
			criteria.add(Restrictions.like(lastName, lastNameValue));
		}
		if (emailAddressValue != null) {
			emailAddressValue = "%"+emailAddressValue+"%";
			criteria.add(Restrictions.like(emailAddress, emailAddressValue));
		}
		if (status.equals(UserStatus.DELETED)) {
			criteria.add((Restrictions.ne("status", status)));
		}
		else {
			criteria.add(Restrictions.eq("status", status));
		}
		return criteria;
	}

}
