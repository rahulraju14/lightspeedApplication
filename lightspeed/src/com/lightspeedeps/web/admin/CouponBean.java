package com.lightspeedeps.web.admin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.CouponDAO;
import com.lightspeedeps.model.Coupon;
import com.lightspeedeps.model.CouponType;
import com.lightspeedeps.type.DiscountType;
import com.lightspeedeps.util.app.ApplicationUtils;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;
import com.lightspeedeps.util.app.MsgUtils;
import com.lightspeedeps.util.app.SessionUtils;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.common.NumberUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.lightspeedeps.web.popup.PopupBean;
import com.lightspeedeps.web.view.View;

/**
 * Backing bean for the Admin "Coupon" page, which is used to generate discount
 * coupon codes that are stored in the database.
 */
@ManagedBean
@ViewScoped
public class CouponBean extends View implements Serializable {
	/** */
	private static final long serialVersionUID = 6256483727646243011L;

	private static final Log log = LogFactory.getLog(CouponBean.class);

	private final static int ACT_GENERATE = 1;

	// Fields

	/** The currently displayed List of Coupon`s */
	private List<Coupon> couponList;

	/** The available DiscountType values. */
	private List<SelectItem> couponTypes;

	/** List of the names of the selected DiscountType values. */
	private List<String> selectedDiscountTypes = new ArrayList<>();

	/** The ONE selected name for the generated DiscountType value. */
	private String generatedDiscountType = DiscountType.PERCENT_OFF.name();

	/** The currently selected (Detail tab) Coupon. */
	private Coupon coupon;

	/** Starting date (inclusive) for range selected by the user to be displayed. */
	private Date startDate = null;

	/** Ending date (inclusive) for range selected by the user to be displayed. */
	private Date endDate = null;

	/** Generator input: number of coupons to generate. */
	private int quantity;
	/** Generator input: prefix for code; random generated numbers follow this. */
	private String prefix;
	/** Generator input: single SKU or regular expression to match SKUs. */
	private String skuPattern;

	/** Generator input: maximum number of users for production purchased. */
	private Integer numUsers = 0;
	/** Generator input: enable SMS messaging on production purchased. */
	private boolean smsOk;

	/** Generator input: Description of coupon (for LS/internal use) */
	private String description;
	/** Generator input: message displayed to user after redeeming the coupon. */
	private String message;
	/** Generator input: expiration date; null if none set. */
	private Date expiration;
	/** Generator input: how many times each coupon may be used. */
	private Integer useCount;
	/** Generator input: The dollar, percentage, or number of months related
	 * to this coupon's offer.  (The meaning of this field depends on the coupon type.) */
	private BigDecimal amount;

	private transient CouponDAO couponDAO;

	/* Constructor */
	public CouponBean() {
		super("Coupon.");
		log.debug("");
		try {
			skuPattern = ".*"; // matches any SKU
			useCount = 1;
			message = "Discount: ??$%?? off. Your monthly subscription is now $???? per month.";
			endDate = SessionUtils.getDate(Constants.ATTR_COUPON_END);
			if (endDate == null) {
				endDate = new Date();
			}
			startDate = SessionUtils.getDate(Constants.ATTR_COUPON_START);
			if (startDate == null || startDate.after(endDate)) {
				Calendar date = new GregorianCalendar();
				date.setTime(endDate);
				date.add(Calendar.DAY_OF_MONTH, -1);
				startDate = date.getTime();
			}

			couponTypes = new ArrayList<>();
			for (DiscountType type : DiscountType.values()) {
				couponTypes.add(new SelectItem(type.name(),type.toString()));
			}

			doQuery();
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		try {
			Integer id = (Integer)SessionUtils.getHttpSession().getAttribute("rowId");
			if (id != null) {
				coupon = getCouponDAO().findById(id);
			}
			if (coupon == null) {
				if (couponList.size() > 0) {
					coupon = couponList.get(0);
				}
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * Action method for the "refresh" button.
	 * @return null navigation string
	 */
	public String actionSearch() {
		try {
			doQuery();
			if (couponList.size() > 0) {
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return null;
	}

	/**
	 * Action method for the "Generate" button. We validate all the parameters,
	 * and if they're OK we put up a confirmation dialog box.  Control will
	 * return via the confirmOk() method.
	 *
	 * @return null navigation string
	 */
	public String actionGenerate() {
		if (quantity <= 0) {
			MsgUtils.addFacesMessage("Coupon.NoQuantity", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		if (useCount == null || useCount <= 0) {
			MsgUtils.addFacesMessage("Coupon.NoUseCount", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		if (numUsers == null) {
			numUsers = 0;
		}
		if (numUsers < 0 || numUsers > 10000) {
			MsgUtils.addFacesMessage("Coupon.BadUserCount", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		if (amount == null || amount.signum() <= 0) {
			MsgUtils.addFacesMessage("Coupon.NoAmount", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		if (StringUtils.isEmpty(prefix)) {
			MsgUtils.addFacesMessage("Coupon.NoPrefix", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		if (StringUtils.isEmpty(skuPattern)) {
			MsgUtils.addFacesMessage("Coupon.NoSku", FacesMessage.SEVERITY_ERROR);
			return null;
		}
		if (StringUtils.isEmpty(description)) {
			MsgUtils.addFacesMessage("Coupon.NoMessage", FacesMessage.SEVERITY_ERROR);
			return null;
		}

		prefix = prefix.toUpperCase().trim();
		if (! prefix.endsWith("-")) {
			prefix += "-";
		}
		DiscountType type = DiscountType.valueOf(generatedDiscountType);
		PopupBean bean = PopupBean.getInstance();
		bean.show( this, ACT_GENERATE, "Coupon.GeneratePrompt.");
		String msg = MsgUtils.formatMessage("Coupon.GeneratePrompt.Text",
				quantity, type, prefix, useCount,
				createAmountString(type,amount),
				skuPattern, message,
				(numUsers==0 ? "not specified" : ""+numUsers),
				(smsOk ? "allowed" : "not allowed"));
		bean.setMessage(msg);
		return null;
	}

	/**
	 * ActionListener method when the user clicks on a particular detail entry.
	 * NOT CURRENTLY IMPLEMENTED
	 * @param evt
	 */
	public void actionCouponView(ActionEvent evt) {
//		try {
//			setCoupon((Coupon)evt.getComponent().getAttributes().get("currentRow"));
//			setSelectedTab(TAB_DETAIL);
//			user = null;
//			if (coupon != null) {
//				String acct = coupon.getRedeemerAcct();
//				user = UserDAO.getInstance().findOneByProperty(UserDAO.ACCOUNT_NUMBER, acct);
//			}
//		}
//		catch (Exception e) {
//			EventUtils.logError(e);
//		}
	}

	/**
	 * Action method when "OK" button is pressed on a confirmation
	 * pop-up dialog.  Currently the only such dialog used in this
	 * bean is for the Generate prompt.
	 *
	 * @see com.lightspeedeps.web.popup.PopupHolder#confirmOk(int)
	 */
	@Override
	public String confirmOk(int action) {
		String ret = generateOk();
		return ret;
	}

	/**
	 * Generate the requested coupons.
	 * @return null navigation string
	 */
	private String generateOk() {
		String codeList = generateCoupons();
		export(codeList);
		MsgUtils.addFacesMessage("Coupon.Generated", FacesMessage.SEVERITY_INFO);
		doQuery();	// refresh search results, will probably show generated items
		return null;
	}

	/**
	 * Generates a set of coupons based on the member fields' values.
	 *
	 * @return A list of the coupon codes generated, separated by new-line
	 *         characters. This may be used to generate an export file for the
	 *         user.
	 */
	private String generateCoupons() {
		Coupon cp;
		String code;
		String codeList = "";
		Calendar today = Calendar.getInstance(ApplicationUtils.getTimeZoneStatic());
		Date created = today.getTime();
		String codePrefix = prefix.replaceAll("[- ]", ""); // store code without hypens or blanks
		DiscountType type = DiscountType.valueOf(generatedDiscountType);

		CouponType couponType = new CouponType(type, amount, skuPattern,
				numUsers.shortValue(), smsOk,
				useCount.shortValue(), created, description, message, expiration);
		getCouponDAO().save(couponType);

		for (int i = 1; i <= quantity; i++) {
			long num = (long)Math.floor(Math.random() * 90000.0);
			num += 10000; // guarantees it has 6 digits with leading digit non-zero.
			String strNumber = "" + num;
			int digit = NumberUtils.calculateCheckDigit(strNumber); // 0-9
			strNumber = strNumber.charAt(0) + (""+digit) + strNumber.substring(1);
			code = codePrefix + strNumber;
			if (! getCouponDAO().existsProperty(CouponDAO.CODE, code)) {
				cp = new Coupon(code, couponType);
				getCouponDAO().save(cp);
				codeList += prefix + strNumber + Constants.NEW_LINE;
			}
			else {
				i--;
			}
		}
		return codeList;
	}

	/**
	 * Create an export file containing the current code-generation parameters
	 * and the supplied list of coupon codes. Sends a JavaScript command to the
	 * browser to open the exported file.
	 *
	 * @param codes The list of coupon codes to include in the export file.
	 * @return The generated filename of the file created.
	 */
	private String export(String codes) {
		String fileLocation = null;
		try {
			DateFormat df = new SimpleDateFormat("yyMMdd");
			String timestamp = df.format(new Date());
			String reportFileName = "coupons"+ "_" + timestamp + ".txt";
			log.debug(reportFileName);
			fileLocation = Constants.REPORT_FOLDER + "/" + reportFileName;
			String reportPath = SessionUtils.getRealReportPath();
			reportFileName = reportPath + reportFileName;
			OutputStream outputStream = new FileOutputStream(new File(reportFileName));
			String expires = "none";
			if (expiration != null) {
				expires = new SimpleDateFormat("MMMM d, yyyy").format(expiration);
			}
			String msg = "{0} coupons\nDescription: {8}\nPrefix: {2}\nType: {1}\n" +
					"Value: {4}\nMax users: {9}\nSMS enabled: {10}\n" +
					"SKU pattern: ''{5}''\nExpiration date: {7}\n" +
					"Message: ''{6}''\nEach coupon may be used {3} times\n\n";
			msg = MsgUtils.formatText(msg, quantity, generatedDiscountType, prefix, useCount,
					createAmountString(DiscountType.valueOf(generatedDiscountType), amount),
					skuPattern, message, expires, description,
					(numUsers==0 ? "use SKU setting" : ""+numUsers),
					(smsOk ? "Yes" : "No") );
			outputStream.write(msg.getBytes());
			outputStream.write(codes.getBytes());
			outputStream.close();
			// open in "same window" ('_self'), since user should get prompt to save as file
			String javascriptCode = "window.open('../../" + fileLocation
					+ "','LS_coupons');";
			addJavascript(javascriptCode);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			fileLocation = null;
		}

		return fileLocation;
	}

	private static String createAmountString(DiscountType type, BigDecimal amount) {
		String text = "?";
		switch (type) {
		case AMOUNT_OFF:
			text = "$" + amount + " off";
			break;
		case PRICE:
			text = "$" + amount;
			break;
		case PERCENT_OFF:
			text = "" + amount + "% off";
			break;
		case FREE_MONTHS:
			text = "" + amount + " months free";
			break;
		case PRE_PURCHASE:
			text = "pre-purchase";
			break;
		}
		return text;
	}
	/**
	 * Force lazy-loading of fields to be displayed in JSP.
	 */
	private void forceLazyInit() {
		if (coupon != null) {
			coupon = CouponDAO.getInstance().refresh(coupon);
		}
		for (Coupon cpn : couponList) {
			initCoupon(cpn);
		}
		if (coupon != null) {
			initCoupon(coupon);
		}
	}

	/**
	 * Force lazy-loading of fields to be displayed in JSP.
	 * @param coupon The Coupon whose fields should be pre-loaded.
	 */
	private void initCoupon(Coupon cpn) {
		try {
			cpn.getCouponType().getCreated();
		}
		catch (Exception e) { // catch errors if referenced object has been deleted.
		}
		return;
	}

	/**
	 * Create a query matching the user's selections, and execute it.
	 */
	@SuppressWarnings("unchecked")
	private void doQuery() {
		SessionUtils.put(Constants.ATTR_EVENT_END, endDate);
		SessionUtils.put(Constants.ATTR_EVENT_START, startDate);

		List<Object> values = new ArrayList<>();
		String query = createQuery(values);
		query = "from Coupon c where " + query;
		couponList = getCouponDAO().find(query, values.toArray());
		log.debug("# of results: " + couponList.size());

		forceLazyInit();
	}

	private String createQuery(List<Object> values) {
		String q = "";

		q = addQuery( q, "c.couponType.created >= ? " );
		startDate = CalendarUtils.setTime(startDate, 0, 0);
		values.add(startDate);

		q = addQuery( q, "c.couponType.created < ? " );
		Calendar date = new GregorianCalendar();
		endDate = CalendarUtils.setTime(endDate, 0, 0);
		date.setTime(endDate);
		date.add(Calendar.DAY_OF_MONTH, 1);
		values.add(date.getTime());

		if (selectedDiscountTypes.size() > 0) {
			q = addQuery( q, createListQuery(selectedDiscountTypes, "c.couponType.discountType") );
		}

		q += " order by c.couponType.created desc, c.id desc";

		return q;
	}

	private String createListQuery(List<String> list, String field) {
		String s = "";
		if (list.size() > 0) {
			for (String item : list) {
				s = s + ", '" + item + "'";
			}
			s = s.substring(1);
			s = field + " in (" + s + ")";
		}
		return s;
	}

	private String addQuery(String q1, String q2) {
		if ( q1.length() > 0) {
			if (q2.length() > 0) {
				q1 += " and " + q2;
			}
			return q1;
		}
		return q2;
	}

	public void setCouponList(List<Coupon> couponList) {
		this.couponList = couponList;
	}
	public List<Coupon> getCouponList() {
		return couponList;
	}

	public Coupon getCoupon() {
		return coupon;
	}
	public void setCoupon(Coupon coupon) {
		this.coupon = coupon;
	}

	public Date getStartDate() {
		return startDate;
	}
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	/** See {@link #quantity}. */
	public int getQuantity() {
		return quantity;
	}
	/** See {@link #quantity}. */
	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	/** See {@link #prefix}. */
	public String getPrefix() {
		return prefix;
	}
	/** See {@link #prefix}. */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/** See {@link #skuPattern}. */
	public String getSkuPattern() {
		return skuPattern;
	}
	/** See {@link #skuPattern}. */
	public void setSkuPattern(String skuPattern) {
		this.skuPattern = skuPattern;
	}

	/** See {@link #numUsers}. */
	public Integer getNumUsers() {
		return numUsers;
	}
	/** See {@link #numUsers}. */
	public void setNumUsers(Integer numUsers) {
		this.numUsers = numUsers;
	}

	/** See {@link #smsOk}. */
	public boolean getSmsOk() {
		return smsOk;
	}
	/** See {@link #smsOk}. */
	public void setSmsOk(boolean smsOk) {
		this.smsOk = smsOk;
	}

	/** See {@link #description}. */
	public String getDescription() {
		return description;
	}
	/** See {@link #description}. */
	public void setDescription(String description) {
		this.description = description;
	}

	/** See {@link #message}. */
	public String getMessage() {
		return message;
	}

	/** See {@link #message}. */
	public void setMessage(String message) {
		this.message = message;
	}

	/** See {@link #expiration}. */
	public Date getExpiration() {
		return expiration;
	}
	/** See {@link #expiration}. */
	public void setExpiration(Date expiration) {
		this.expiration = expiration;
	}

	/** See {@link #useCount}. */
	public Integer getUseCount() {
		return useCount;
	}
	/** See {@link #useCount}. */
	public void setUseCount(Integer useCount) {
		this.useCount = useCount;
	}

	/** See {@link #amount}. */
	public BigDecimal getAmount() {
		return amount;
	}
	/** See {@link #amount}. */
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public List<SelectItem> getDiscountTypes() {
		return couponTypes;
	}
	public void setDiscountTypes(List<SelectItem> couponTypes) {
		this.couponTypes = couponTypes;
	}

	/** See {@link #generatedDiscountType}. */
	public String getGeneratedDiscountType() {
		return generatedDiscountType;
	}
	/** See {@link #generatedDiscountType}. */
	public void setGeneratedDiscountType(String type) {
		generatedDiscountType = type;
	}

	public List<String> getSelectedDiscountTypes() {
		return selectedDiscountTypes;
	}
	public void setSelectedDiscountTypes(List<String> selectedDiscountTypes) {
		this.selectedDiscountTypes = selectedDiscountTypes;
	}

	private CouponDAO getCouponDAO() {
		if (couponDAO == null) {
			couponDAO = CouponDAO.getInstance();
		}
		return couponDAO;
	}

}
