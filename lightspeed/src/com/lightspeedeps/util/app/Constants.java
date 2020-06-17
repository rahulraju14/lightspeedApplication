//	File Name:	Constants.java
package com.lightspeedeps.util.app;

import java.awt.Color;
import java.math.BigDecimal;
import java.util.*;

import javax.faces.model.SelectItem;

import com.lightspeedeps.object.BitMask;
import com.lightspeedeps.type.StudioType;
import com.lightspeedeps.type.TextDirection;
import com.lightspeedeps.util.common.CalendarUtils;

/**
 * A class for defining a variety of literal or system-dependent values used throughout
 * the LightSPEED applications.
 */
public final class Constants {

	/** list of short week-day names; used for labels for the x axis of the Hours Worked chart
	 * on the Approver Dashboard / Timecard Review page. */
	public final static List<String> WEEKDAY_NAMES;

	/** list of long week-day names */
	public final static List<String> WEEKDAY_LONG_NAMES;

	/** Drop-down selection list of long week-day names; used for the Production week start
	 * on the Payroll Preferences | Setup page. */
	public final static List<SelectItem> WEEKDAY_NAMES_DL;

	/** Drop-down selection list of long week-day names with addition of initial
	 * blank entry; used where no default is wanted, such as on the WTPA forms. */
	public final static List<SelectItem> WEEKDAY_NAMES_PLUS_BLANK_DL;

	/** Our default Locale. */
	public final static Locale LOCALE_US = new Locale("en", "US");
	public final static Locale LOCALE_FRENCH_CANADA = new Locale("fr", "CA");

	static {
		WEEKDAY_NAMES = new ArrayList<>();
		WEEKDAY_LONG_NAMES = new ArrayList<>();
		WEEKDAY_NAMES_DL = new ArrayList<>();
		Calendar cal = Calendar.getInstance(LOCALE_US);
		cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
		for (int i=1; i <= 7; i++) { // 7 days in the week!
			WEEKDAY_NAMES.add(cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, LOCALE_US));
			String day = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, LOCALE_US);
			WEEKDAY_LONG_NAMES.add(day);
			WEEKDAY_NAMES_DL.add(new SelectItem(cal.get(Calendar.DAY_OF_WEEK), day));
			cal.add(Calendar.DAY_OF_WEEK, 1);
		}
		WEEKDAY_NAMES_PLUS_BLANK_DL = new ArrayList<>(WEEKDAY_NAMES_DL);
		WEEKDAY_NAMES_PLUS_BLANK_DL.add(0, new SelectItem(0, " "));
	}

	/**
	 * Default Constructor
	 */
	private Constants() {
	}

	// * * * Pure constants that should never change * * *

	// True & False for MySQL Byte fields
	/** Byte value for 0 for boolean comparisons */
	public static final Byte FALSE = Byte.valueOf((byte)0);
	/** Byte value for 1 for boolean comparisons */
	public static final Byte TRUE = Byte.valueOf((byte)1);

	/** Byte value of zero for numeric comparisons. */
	public static final Byte BYTE_ZERO = Byte.valueOf((byte)0);

	/** Integer 0, may be used in SelectItems, etc. */
	public static final Integer INTEGER_ZERO = Integer.valueOf(0);

	/** Decimal 0.00 for initializing monetary values (to display nicely). */
	public static final BigDecimal DECIMAL_ZERO = BigDecimal.ZERO.setScale(2);
	/** Decimal 0.2 for some payroll calculations (= 1/5) */
	public static final BigDecimal DECIMAL_POINT_TWO = new BigDecimal("0.2");
	/** Decimal 0.4 for some payroll calculations (= 2/5) */
	public static final BigDecimal DECIMAL_POINT_FOUR = new BigDecimal("0.4");
	/** Decimal 1.5 for time-and-a-half calculations. */
	public static final BigDecimal DECIMAL_ONE_FIVE = new BigDecimal(1.5);
	/** Decimal 2.0 for double-time calculations. */
	public static final BigDecimal DECIMAL_TWO = new BigDecimal(2.0);
	/** Decimal 3.0 for 1/3 calculations. */
	public static final BigDecimal DECIMAL_THREE = new BigDecimal(3.0);
	/** Decimal 4.0 for minimum time or other uses. */
	public static final BigDecimal DECIMAL_FOUR = new BigDecimal(4.0);
	/** Decimal 5.0; for dividing 5-day rate into daily, or other. */
	public static final BigDecimal DECIMAL_FIVE = new BigDecimal(5.0);
	/** Decimal 5.5 for minimum time or other uses. */
	public static final BigDecimal DECIMAL_FIVE_FIVE = new BigDecimal(5.5);
	/** Decimal 6.0; for dividing 6-day rate into daily, or other. */
	public static final BigDecimal DECIMAL_SIX = new BigDecimal(6.0);
	/** Decimal 7.0 for minimum time or other uses. */
	public static final BigDecimal DECIMAL_SEVEN = new BigDecimal(7.0);
	/** Decimal 8.0 for minimum time or other uses. */
	public static final BigDecimal DECIMAL_EIGHT = new BigDecimal(8.0);
	/** Decimal 10 for fields that we validate to 1 digit (9.9 max). */
	public static final BigDecimal DECIMAL_10 = new BigDecimal(10);
	/** Decimal 12.0 for minimum time or other uses. */
	public static final BigDecimal DECIMAL_12 = new BigDecimal(12.0);
	/** Decimal 100 for fields that we validate to 2 digits (99 max). */
	public static final BigDecimal DECIMAL_100 = new BigDecimal(100);
	/** Decimal 1,000 for fields that we validate to 3 digits (999 max). */
	public static final BigDecimal DECIMAL_1K = new BigDecimal(1000);
	/** Decimal 10,000 for fields that we validate to 4 digits (9,999 max). */
	public static final BigDecimal DECIMAL_10K = new BigDecimal(10000);
	/** Decimal 100,000 for fields that we validate to 5 digits (99,999 max). */
	public static final BigDecimal DECIMAL_100K = new BigDecimal(100000);
	/** Decimal 1,000,000 for fields that we validate to 6 digits (999,999 max). */
	public static final BigDecimal DECIMAL_1_MILLION = new BigDecimal(1000000);
	/** Decimal 100,000,000 for fields that we validate to 8 digits (99,999,999 max). */
	public static final BigDecimal DECIMAL_100_MILLION = new BigDecimal(100000000);

	/** Decimal -1,000 for fields that we validate to 3 digits (-999 min). */
	public static final BigDecimal DECIMAL_NEG_1K = new BigDecimal(-1000);
	/** Decimal -10,000 for fields that we validate to 4 digits (-9,999 min). */
	public static final BigDecimal DECIMAL_NEG_10K = new BigDecimal(-10000);
	/** Decimal -100,000 for fields that we validate to 5 digits (-99,999 min). */
	public static final BigDecimal DECIMAL_NEG_100K = new BigDecimal(-100000);
	/** Decimal -1,000,000 for fields that we validate to 6 digits (-999,999 min). */
	public static final BigDecimal DECIMAL_NEG_1_MILLION = new BigDecimal(-1000000);
	/** Decimal -100,000,000 for fields that we validate to 8 digits (-99,999,999 min). */
	public static final BigDecimal DECIMAL_NEG_100_MILLION = new BigDecimal(-100000000);

	// Night Premium Multipliers
	/** Night Premium 10 hours 1 */
	public static final BigDecimal HOURS_10_NP1 = new BigDecimal(1.1);
	/** Night Premium 10 hours 2 */
	public static final BigDecimal HOURS_10_NP2 = new BigDecimal(1.2);
	/** Night Premium 15 hours 1 */
	public static final BigDecimal HOURS_15_NP1 = new BigDecimal(1.65);
	/** Night Premium 15 hours 2 */
	public static final BigDecimal HOURS_15_NP2 = new BigDecimal(1.8);

	/** Generic String return value indicating an error condition; should not be
	 * equal to any JSP navigation string! */
	public static final String ERROR_RETURN = "_ERROR_";

	// * * * Constants defined by ICEfaces -- used in Request Parameters * * *

	/** suffix for the index in a list of the last selected row */
	public static final String ICEFACES_SELECTED_ROW = "_instantSelectedRowIndexes";

	/** suffix for a drag-and-drop operation within a single list */
	public static final String ICEFACES_REORDER_PARAM = "_reorderings";

	/** suffix for a drag-and-drop operation from one list to another list */
	public static final String ICEFACES_IMMIGRATION_PARAM = "_immigration";

	// * * * Constants that may change as the application changes * * *

	/** Maximum value allowed for per-mile reimbursement on timecards. */
	public static final BigDecimal MAX_MILEAGE_RATE = new BigDecimal(5);

	/** List of valid LightSPEED reseller codes. */
	public static final List<String> RESELLER_CODES = Arrays.asList("MDSV");

//	/** String specified in URL as ?ref=... to indicate ShowBiz branded site. */
//	public static final String SHOW_BIZ_REFER = "l1";
//	/** String contained in referring URL which indicates ShowBiz branded site. */
//	public static final String SHOW_BIZ_URL_TEXT = "showbiz";
//	/** Database id for ShowBiz (Media Services) payroll service. */
//	public static final int SHOW_BIZ_ID = 3;

	/** The name assigned to the first Unit of a Project. */
	public static final String DEFAULT_UNIT_NAME = "Main";

	/** The maximum number of Unit`s a user can create within a single Project. */
	public static final int MAX_UNITS = 8;

	/** Default email prefix for notification messages. */
	public static final String DEFAULT_EMAIL_SENDER = "notifications";
	public static final String EMAIL_FROM_PREFIX = "do_not_reply_";
	/* Number of times to attempt sending an email. */
	public static final int MAIL_RETRY_COUNT = 3;

	/** Standard html email header to set font, etc. */
	public static final String EMAIL_STD_HEADER = "<div style='font-family:\"Arial\",\"sans-serif\";font-size:11pt'>";
	/** Standard html email trailer to close opening div or other tag(s). */
	public static final String EMAIL_STD_TRAILER = "</div>";

	/** Date style typically used in email body text or other long messages. */
	public static final String LONG_DATE_FORMAT = "EEEE, MMMM d, yyyy";

	/** Date style used in various places */
	public static final String MEDIUM_DATE_FORMAT = "MMM d, yyyy";

	/** Short (numeric) style used in various places */
	public static final String SHORT_DATE_FORMAT = "MM/dd/yyyy";

	/** Short (numeric) date style used in various places, in Canadian format (e.g., for ACTRA) */
	public static final String SHORT_DATE_FORMAT_CANADA = "yyyy-MM-dd";

	/** Date style typically used in timecards for "week-ending date". */
	public static final String WEEK_END_DATE_FORMAT = "MM/dd/yyyy";

	/** Date style typically used to represent a "week-ending date" when embedded
	 * as part of a file name. */
	public static final String WEEK_END_DATE_FILE_FORMAT = "MMddyy";

	/** The date format pattern that should be used in SQL queries. */
	public static final String SQL_DATE_PATTERN = "yyyy-MM-dd";

	/** The date format used when creating a "label" for a StartForm, typically used in
	 * drop-down lists of StartForms. */
	public static final String START_FORM_DATE_FORMAT = "MM/dd/yy";

	/** The date format used used in canada */
	public static final String DATE_FORMAT_CANADA = "dd/MM/yyyy";

	/** The date & time format used for the "tool tip" in the 'Sent' column
	 * on the document and timecard transfer pages. */
	public static final String TRANSFER_TOOLTIP_SENT_DATE_FORMAT = "M/d/yy h:mm a";

	public static final Date JAN_1_2000 = CalendarUtils.parseDate("01012000", "MMddyyyy");

	public static final Date JAN_8_2000 = CalendarUtils.parseDate("01082000", "MMddyyyy");

	public static final Date JAN_1_0100 = CalendarUtils.parseDate("01010100", "MMddyyyy");

	/** An unlikely date, to use as the "All" entry in various date drop-down lists.
	 * *** NOTE *** This MUST BE a date that falls on a SATURDAY!! *** */
	public static final Date SELECT_ALL_DATE = Constants.JAN_1_2000; // ALL_DATE must be a SATURDAY

	/** An unlikely date, to use as the "Prior" entry in timecard date drop-down lists.
	 * *** NOTE *** This MUST BE a date that falls on a SATURDAY!! ***  */
	public static final Date SELECT_PRIOR_DATE = Constants.JAN_8_2000; // PRIOR_DATE must be a SATURDAY

	/** Earliest date allowed for Hire dates, work dates, etc. */
	public static final Date EARLIEST_PAYROLL_DATE = CalendarUtils.parseDate("01011980", "MMddyyyy");

//	public static final DateFormat LOG_DATE_TIME = new SimpleDateFormat("MM/dd/yy HH:mm:ss");

	public static final String SELECT_HEAD_BASIS = "(Select Basis)";
	public static final String SELECT_HEAD_DEPARTMENT = "(Select Department)";
	public static final String SELECT_HEAD_ROLE = "(Select Role)";
	public static final String SELECT_HEAD_NEW_ROLE = "(Create New Role)";
	public static final String SELECT_HEAD_NO_TASK_OWNER = "(No person selected)";
	public static final String SELECT_HEAD_NONE = "(None selected)";
	public static final String SELECT_HEAD_STATE = "(Select State)";
	// Substitute Province for State for Canada
	public static final String SELECT_HEAD_PROVINCE = "(Select Province)";
	public static final String SELECT_BATCH_NONE = "(Not in a batch)";
	public static final Integer SELECT_BATCH_NONE_ID = -1;

	/** Empty SelectItem used for top entry in lists, to indicate no 'real' item selected. */
	public static final SelectItem EMPTY_SELECT_ITEM = new SelectItem(null, "--");

	public static final SelectItem BLANK_STATE_ITEM = new SelectItem("  ", "--");

	/** Other SelectItem used for city names lists */
	public static final SelectItem SELECT_OTHER_ITEM = new SelectItem("Other");

	/** State code used for touring timesheets & hybrid (touring) time cards indicating "use home" state. LS-1492, LS-1981 */
	public static final String TOURS_HOME_STATE = "HM";

	/** State list item used for "home state" in touring timecards. LS-1491 */
	public static final SelectItem TOURS_HOME_STATE_ITEM = new SelectItem(TOURS_HOME_STATE, TOURS_HOME_STATE);

	/** Original LS code for state value that indicated foreign country. */
	public static final String FOREIGN_FO_STATE = "FO";

	/** Selection item for LS foreign state ("FO"). */
	public static final SelectItem FOREIGN_FO_STATE_ITEM = new SelectItem(FOREIGN_FO_STATE, "(Foreign)");

	/** TEAM State code used for timesheets and hybrid (touring) time cards for out of country. LS-2166 */
	public static final String FOREIGN_OT_STATE = "OT";

	/** Selection item for Team/tours/hybrid foreign state ("OT"). */
	public static final SelectItem FOREIGN_OT_STATE_ITEM = new SelectItem(Constants.FOREIGN_OT_STATE, Constants.FOREIGN_OT_STATE);

	/** Canadian "state" value used in Form I-9. LS-1937 */
	public static final String CANADA_STATE = "CAN";

	/** Mexican "state" value used in Form I-9. LS-1937 */
	public static final String MEXICO_STATE = "MEX";


	public static final String CATEGORY_ALL = "ALL";
	private static final String CATEGORY_ALL_LABEL = "All";
	public static final SelectItem GENERIC_ALL_ITEM = new SelectItem(CATEGORY_ALL, CATEGORY_ALL_LABEL);

	/** New York City name, used for Hot Costs */
	public static final String NEW_YORK_CITY = "new york city";
	/** New York State code, used for Hot Costs */
	public static final String NEW_YORK_STATE = "NY";
	/** Canada State code, used for tax wage allocations */
	public static final String CANADA_STATE_CODE = "Canada";
	/** Washington DC code, used for hot costs */
	public static final String DC_STATE = "DC";

	/** A SelectItem used for the "All" (week-ending dates) choice in a drop-down list. */
	public static final SelectItem SELECT_ALL_DATES = new SelectItem(SELECT_ALL_DATE, CATEGORY_ALL_LABEL);

	/** A SelectItem used for the "All" choice in a drop-down list using ID values. */
	public static final SelectItem SELECT_ALL_IDS = new SelectItem(0, CATEGORY_ALL_LABEL);

	/** A SelectItem used for the "All" choice in a drop-down list using ID values. */
	public static final SelectItem SELECT_NOT_BATCHED = new SelectItem(SELECT_BATCH_NONE_ID, SELECT_BATCH_NONE);

	public static final String CAT_ACTIVE = "Active";
	private static final String CAT_INACTIVE = "Inactive";

	/** Selection list for drop-down lists on pages where the user may filter the type
	 * of Productions shown based on their active/inactive status. */
	public static final List<SelectItem> PRODUCTION_STATUS_DL = Arrays.asList(
			GENERIC_ALL_ITEM,
			new SelectItem(CAT_ACTIVE, CAT_ACTIVE),
			new SelectItem(CAT_INACTIVE, CAT_INACTIVE));

	/** U/A/O  (U=US), (A=RES. ALIEN), or (O=OTHER) */
	public static final SelectItem[] CITIZEN_STATUS_ITEMS = {
		new SelectItem("", "---"),
		new SelectItem("c", "Citizen of US"),
		new SelectItem("n", "Noncitizen National of US"),
		new SelectItem("p", "Lawful Permanent Resident"),
		new SelectItem("a", "Alien Authorized to work") };

	/** C/A/O  (C=CANADA), (A=RES. ALIEN), or (O=OTHER) */
	public static final SelectItem[] CANADA_CITIZEN_STATUS_ITEMS = {
		new SelectItem("", "---"),
		new SelectItem("C", "Canada"),
		new SelectItem("A", "Perm. Resident"),
		new SelectItem("O", "Other") };

	/** The selection list, used for radio button generation, for the contract type
	 * for Commercial productions.  The selection gets mapped to the same field as the
	 * "major/independent" StudioType choice for non-Commercial productions. */
	public static final List<SelectItem> AICP_CONTRACT_DL = Arrays.asList(
			new SelectItem(StudioType.MJ, "CPA - Commercial Production Agreement"),
			new SelectItem(StudioType.IN, "Independent"));

	public static final List<String> AICP_CONTRACT_LIST = Arrays.asList(
			"CPA - Commercial Production Agreement",
			"Independent");

	public static final String NEW_LINE = System.getProperty("line.separator");
	public static final char SCRIPT_NEW_LINE_CHAR = '\n'; // the newline used in TextElement records
	public static final String SCRIPT_NEW_LINE = ""+SCRIPT_NEW_LINE_CHAR; // ...same, as a String
	public static final String HTML_BREAK = "<br/>";

	public static final String BLANK_LINE = // 300 blanks (100 is enough for all 'normal' cases!)
			"                                                                                                    " +
			"                                                                                                    " +
			"                                                                                                    ";

	public static final int POINTS_PER_INCH = 72;

	// * * * Stripboard * * *

	public static final String DEFAULT_STRIPBOARD_NAME = "Strip board #1";

	public static final String SHEET_COLUMN_NAME = "Page";
	public static final String SCENE_COLUMN_NAME = "Scn";
	public static final String DN_COLUMN_NAME = "D/N";
	public static final String IE_COLUMN_NAME = "I/E";
	public static final String SCRIPT_DAY_COLUMN_NAME = "S/d";
	public static final String LOCATION_COLUMN_NAME = "Location / Synopsis";
	public static final String PAGES_COLUMN_NAME = "Pages";
	public static final String ID_COLUMN_NAME = "Ids";
	public static final String DRAG_PANEL_MESSAGE = "Drag and drop to add new \"Banner\"";
	public static final String DRAG_END_OF_DAY_MESSAGE = "Drag and drop to add new \"End Of Day\"";

	public static final String EMPTY_BANNER_TEXT = " ";
	public static final String END_OF_DAY_TEXT = "End of Day # ";

	/** UserPrefBean key for user's preferred value for Stripboard layout style. */
	public final static String UPREF_STB_LAYOUT = "SBL";

	// Import Script data
	public static final String NO_SET_NAME = "(No set location given.)";
	public static final String NO_SCRIPT_ELEMENTS = "No script elements are assigned to this scene.";

	public static final int SYNOPSIS_MAX_CREATE_LENGTH = 190;
	public static final int SYNOPSIS_MIN_CREATE_LENGTH = 30;

	public static final int MAX_ELEMENT_NAME_LENGTH = 45;
	public static final int MAX_USER_NAME_LENGTH = 50;
	public static final int MAX_EMAIL_ADDR_LENGTH = 100;

	/** This is a dummy ScriptElement.id value used for all Crew members for DooD purposes. */
	public static final Integer CREW_SCRIPT_ELEMENT_ID = -1;

	// * * * * * * * Various indents, widths, and margins for formatting Script output * * * * * * *

	public static final int SCRIPT_MAX_LINES_PER_PAGE = 58;

	/**
	 *  Left margin, applicable to all output lines.  All "INDENT" values are relative
	 * to this value, so they don't need to be adjusted to shift the whole page left or right.
	 */
	public static final int SCRIPT_FMT_LEFT_MARGIN = 8;

	/** Indent of "Action" text from the left margin.  This is also used for the position of
	 * the Scene heading (where the "INT"/"EXT" text begins). */
	public static final int SCRIPT_FMT_ACTION_INDENT = 8;
	/** The maximum width of an action line. */
	public static final int SCRIPT_FMT_ACTION_WIDTH = 61;

	/** Indent of "Dialogue" text from the left margin. */
	public static final int SCRIPT_FMT_DIALOGUE_INDENT = 18;
	/** The maximum width of a dialogue line. */
	public static final int SCRIPT_FMT_DIALOGUE_WIDTH = 41;

	/** Indent of "Parenthetical" text from the left margin. */
	public static final int SCRIPT_FMT_PAREN_INDENT = 22;
	/** The maximum width of a parenthetical text line. */
	public static final int SCRIPT_FMT_PAREN_WIDTH = 26;

	/** Indent of a "Character" name from the left margin.*/
	public static final int SCRIPT_FMT_CHARACTER_INDENT = 28;
	/** The maximum width of an Character line. */
	public static final int SCRIPT_FMT_CHARACTER_WIDTH = 40;

	/** The right margin (maximum width of a line, including the left margin). */
	public static final int SCRIPT_FMT_RIGHT_MARGIN = SCRIPT_FMT_LEFT_MARGIN + SCRIPT_FMT_ACTION_INDENT + SCRIPT_FMT_ACTION_WIDTH;

	/** The right-alignment point for "transition" text. */
	public static final int SCRIPT_FMT_TRANSITION_MARGIN = SCRIPT_FMT_RIGHT_MARGIN;

	/** The right-alignment point for the "*" indicating changed text. */
	public static final int SCRIPT_FMT_CHANGE_MARGIN = SCRIPT_FMT_RIGHT_MARGIN + 3;

	/** Maximum length the page header can be and still fit within display area. */
	public static final int SCRIPT_FMT_MAX_HEADER_LEN = SCRIPT_FMT_CHANGE_MARGIN - SCRIPT_FMT_LEFT_MARGIN;


	// * * * * * * *  Keys for "enumerations" stored in the Enumeration table  * * * * * * *

	/** Key for the TV Production Type enumeration, e.g., Pilot, MOW, ... */
	public static final String TV_PRODUCTION_TYPE_ENUM_KEY = "TV-PT";

	/** Key for the TV Production Season enumeration, e.g., 1st, 2nd, ... */
	public static final String TV_PRODUCTION_SEASON_ENUM_KEY = "TV-SS";

	/** Key for the TV Production Start enumeration which are date ranges. */
	public static final String TV_PRODUCTION_START_ENUM_KEY = "TV-SD";


	// * * * * * * *  Session attribute names (keys)  * * * * * * *
	/* !! IMPORTANT !!
	 * A number of these attributes are specified as LITERAL values (not using the field
	 * names) within JSF code.  If a change is made to the literal value, you must
	 * do a text search of all the xhtml files and change the corresponding values there.
	 */

	/** standard prefix on all attribute strings, to identify them as 'ours'. If this value
	 * is changed, all the xhtml files that use attribute names must be updated to match! */
	public static final String ATTR_PREFIX = "com.lightspeedeps.";

	/** Session attribute key for database id of the payroll service whose branding
	 * should be displayed. */
	public static final String ATTR_BRAND_SERVICE_ID = ATTR_PREFIX + "brandServiceId";

	/** Session attribute key for referral string for the payroll service whose branding
	 * should be displayed. */
	public static final String ATTR_BRAND_SERVICE_REFER = ATTR_PREFIX + "brandServiceRef";

	/** Session attribute key to determine if the user is entering from the ttconline domain. */
	public static final String ATTR_IS_TTCONLINE_DOMAIN =  ATTR_PREFIX + "isTTCOnlineDomain";  // LS-1691

	/** Session attribute key to determine if the user is currently in a Team payroll client production. */
	public static final String ATTR_IS_TTC_PROD =  ATTR_PREFIX + "isTTCProduction";  // LS-1763

	/** Session attribute key to determine if the user is currently in a Team payroll client production. */
	public static final String ATTR_USER_SHOWN_ESS_MENU =  ATTR_PREFIX + "isShownEssMenu";  // LS-3758

	/** Session attribute key for the selected contact/production on production selection
	 * page */
	public static final String ATTR_SELECT_CONTACT_ID = ATTR_PREFIX + "selectContactId";

	/** Session attribute for the selected allocation form contact */
	public static final String ATTR_SELECTED_ALLOC_CONTACT_ID = ATTR_PREFIX + "selectedAllocContactId";

	/** Session attribute key for the filter string on the production selection page. */
	public static final String ATTR_PROD_FILTER = ATTR_PREFIX + "productionListFilter";

	/** Session attribute key for the database id of the current Contact object */
	public static final String ATTR_CURRENT_CONTACT = ATTR_PREFIX + "CurrentContactId";
	/** Session attribute key for the database id of the current User object */
	public static final String ATTR_CURRENT_USER = ATTR_PREFIX + "CurrentUserId";
	/** Session attribute key for the current user's user-name (for Reset password) */
	public static final String ATTR_CURRENT_USER_NAME = ATTR_PREFIX + "CurrentUserName";

	/** Session attribute key for the user's smart-phone-device status: true or false. */
	public static final String ATTR_USER_PHONE = ATTR_PREFIX + "UserOnPhone";
	/** Session attribute key for the user's tablet-device status: true or false. */
	public static final String ATTR_USER_TABLET = ATTR_PREFIX + "UserOnTablet";

	/** Session attribute key for the password-reset key; only used for mobile devices. */
	public static final String ATTR_PW_RESET_KEY = ATTR_PREFIX + "PwResetKey";

	/** Session attribute key used to indicate a user has just clicked on an entry
	 * in My Productions and is being sent "into" the production.  Any non-null value
	 * indicates this state; it is set back to null by PageAuthenticationPhaseListener
	 * after it allows the apparent "outer page/inner state" incident to pass. */
	public static final String ATTR_ENTERING_PROD = ATTR_PREFIX + "EnteringProduction";

	/** Session attribute key for boolean indicating session is logged in already: true or false. */
	public static final String ATTR_LOGGED_IN = ATTR_PREFIX + "UserIsLoggedIn";

	/** Session attribute key for boolean indicating user has come from, or gone to, another app (e.g., ESS): true or false. */
	public static final String ATTR_CROSS_APP = ATTR_PREFIX + "CrossApplicationLogin";

	/** Session attribute key for boolean indicating user first logged into ESS: true or false. */
	public static final String ATTR_ORIGIN_ESS = ATTR_PREFIX + "OriginESS";

	/** Authorization (OAuth) token used for service requests. */
	public static final String ATTR_ACCESS_TOKEN = ATTR_PREFIX + "UserAccessToken";

	/** Authorization-refresh token which may be used to get a fresh access token. */
	public static final String ATTR_REFRESH_TOKEN = ATTR_PREFIX + "UserRefreshToken";

	/** Session attribute key for boolean indicating the user's session expired (='1'). */
	public static final String ATTR_SESSION_EXPIRED = ATTR_PREFIX + "SessionExpired";

	/** Session attribute key for the user's IP address. */
	public static final String ATTR_IP_ADDR = ATTR_PREFIX + "IpAddress";

	/** Full domain (including any sub-domain(s)) of the http request */
	public static final String ATTR_HTTP_REQUEST_DOMAIN =  ATTR_PREFIX + "RequestDomain";

	/** Top domain extracted from the http request; this will be used as the "@< domain >" part of
	 * the "sender's" address for outbound emails. */
	public static final String ATTR_EMAIL_SENDING_DOMAIN =  ATTR_PREFIX + "EmailSendingDomain";

	/** Session attribute key for the user's browser's 'user-agent' string. */
	public static final String ATTR_USER_AGENT = ATTR_PREFIX + "UserAgent";

	/** Session attribute key which Apache Tomcat will search for, and display the value
	 * of, on the Tomcat Manager page.  We put the email address here as a reasonable
	 * way of identifying currently logged-in users from the Tomcat console.*/
	public static final String TOMCAT_USER_KEY = "Login";

	/** Session attribute key for the referred-by string which may be passed to the
	 * login page. */
	public static final String ATTR_REFERRED_BY = ATTR_PREFIX + "ReferredBy";

	/** Session attribute key for a URI entered before the user has logged in. */
	public static final String ATTR_SAVED_PAGE_INFO = ATTR_PREFIX + "SavedURI";
	/** Session attribute key for saving the Unit id parameter (?u=<n>) included on a
	 * URL entered before the user has logged in. */
	public static final String ATTR_SAVED_PAGE_UNIT = ATTR_PREFIX + "SavedUnitId";
	/** The name (parameter key) used in a URL to specify the Unit database id. E.g., a
	 * calendar URL might be "<our domain>/ls2/c.jsp?u=23" */
	public static final String UNIT_ID_URL_KEY = "u";

	/** Session attribute key for saving scroll position on various pages. */
	public static final String ATTR_SCROLL_POS = ATTR_PREFIX + "ScrollPos";

	/** Session attribute key for the current Project.id value */
	public static final String ATTR_CURRENT_PROJECT = ATTR_PREFIX + "CurrentProject";
	/** Session attribute key for the current Production.id value */
	public static final String ATTR_PRODUCTION = ATTR_PREFIX + "CurrentProduction";

	/** Session attribute key for the Production-type product selected by the user for purchase. */
	public static final String ATTR_SELECTED_PRODUCT_ID = ATTR_PREFIX + "SelectedProductId";
	/** Session attribute key for the database id of the production being upgraded/resubscribed. */
	public static final String ATTR_UPGRADE_PRODUCTION_ID = ATTR_PREFIX + "UpgradeProductionId";

	/** Session attribute key for which header tab to display. */
	public static final String ATTR_HEADER_TAB_ID = ATTR_PREFIX + "headerTabId";

	/** Session attribute key for the index of the previously displayed tab; used
	 * for "Return" key processing. */
	public static final String ATTR_PRIOR_TAB_IX = ATTR_PREFIX + "priorTabIx";

	/** Session attribute key for the index of the "mini-tab" currently selected. Used mostly
	 * to re-select the mini-tab automatically if the user does a browser refresh. */
	public static final String ATTR_CURRENT_MINI_TAB = ATTR_PREFIX + "CurrentMiniTab";

	/** Session attribute key for the UserCallInfo for this user. */
	public static final String ATTR_USER_CALL_INFO = ATTR_PREFIX + "userCallInfo";

	/** Session attribute key for the id of the just-imported script */
	public static final String ATTR_IMPORT_SCRIPT_ID = ATTR_PREFIX + "ImportScriptId";

	/** Session attribute key for the List of database ids of ScriptElements created by an import. */
	public static final String ATTR_IMPORT_ADDED_ELEMENTS = ATTR_PREFIX + "ImportAddedElements";

	/** Session attribute key for the id of the currently selected Script
	 * on the Script Revisions page. */
	public static final String ATTR_SCRIPT_LIST_ID = ATTR_PREFIX + "scriptListId";

	// Most of these are used to pass database id values to the various view pages,
	// to indicate which item should be selected upon entry.

	/** Session attribute key for currently selected contact on Cast&Crew List/View page */
	public static final String ATTR_CONTACT_ID = ATTR_PREFIX + "contactid";
	/** Session attribute key for currently selected department on Department List/View page */
	public static final String ATTR_DEPARTMENT_ID = ATTR_PREFIX + "departmentid";
	/** Session attribute key to position User List/View (admin) page to selected account number. */
	public static final String ATTR_USER_LIST_ACCOUNT = ATTR_PREFIX + "userListAccount";
	/** Session attribute key for id of currently selected User on User List/View (admin) page */
	public static final String ATTR_USER_LIST_ID = ATTR_PREFIX + "userListId";
	/** Session attribute key for currently selected Material on Material View page*/
	public static final String ATTR_MATERIAL_ID = ATTR_PREFIX + "MaterialId";
	/** Session attribute key for currently selected Project on Project View page*/
	public static final String ATTR_PROJECT_VIEW_ID = ATTR_PREFIX + "ProjectViewId";
	/** Session attribute key for currently selected PointOfInterest on PointOfInterest View page*/
	public static final String ATTR_POINT_OF_INTEREST_ID = ATTR_PREFIX + "PointOfInterestId";
	/** Session attribute key for currently selected RealWorldElement on Product Element View page*/
	public static final String ATTR_RW_ELEMENT_ID = ATTR_PREFIX + "RealWorldElementId";
	/** Session attribute key for currently selected ScriptElement on ScriptElement View page*/
	public static final String ATTR_SE_ELEMENT_ID = ATTR_PREFIX + "ScriptElementId";
	/** Session attribute key for selecting a Production on Production List page by prod_id (key). */
	public static final String ATTR_PRODUCTION_KEY = ATTR_PREFIX + "ProductionViewKey";
	/** Session attribute key for id of currently selected Production on Production List (admin) page. */
	public static final String ATTR_PRODUCTION_ID = ATTR_PREFIX + "ProductionViewId";
	/** Session attribute key for the currently displayed/edited stripboard */
	public static final String ATTR_STRIPBOARD_ID = ATTR_PREFIX + "stripboardId";
	/** Session attribute key for the Unit being view/edited on the stripboard page. */
	public static final String ATTR_STRIPBOARD_UNIT_ID = ATTR_PREFIX + "stripboardUnitId";
	/** Session attribute key for the payroll service being view/edited on the admin/payroll page. */
	public static final String ATTR_PAYROLL_LIST_ID = ATTR_PREFIX + "payrollServiceId";
//	/** String attribute key for the Week Ending date being view/edited on the Payroll/Employee Timesheet page. */
//	public static final String ATTR_PAYROLL_SELECTED_WEEK_END_DATE = ATTR_PREFIX + "timesheetWeekEndDate";

	// Attributes for various "Onboarding" selections
	/** Session attribute key for currently selected packet on Start Packets Cast n Crew/Onboarding page */
	public static final String ATTR_PACKET_ID = ATTR_PREFIX + "packettid";
	/** Session attribute key for currently selected contact on Start Status Cast n Crew/Onboarding page */
	public static final String ATTR_CONTACTS_ID = ATTR_PREFIX + "obcontactid";
	/** Session attribute key for currently selected department on Start Status Cast n Crew/Onboarding page
	 * department filter */
	public static final String ATTR_ONBOARDING_DEPT = ATTR_PREFIX + "onboardDept";
	/** Session attribute key for currently selected status on Start Status Cast n Crew/Onboarding page
	 * status filter */
	public static final String ATTR_ONBOARDING_STATUS = ATTR_PREFIX + "onboardStatus";
	/** Session attribute key for the employee account number for the Employee filter drop-down on
	 * Start Status Cast n Crew/Onboarding page */
	public static final String ATTR_ONBOARDING_EMPLOYEE = ATTR_PREFIX + "onboardEmployeeAcct";
	public static final String ATTR_ONBOARDING_VIEW_STATUS = ATTR_PREFIX + "onboardViewStatus";
	/** Session attribute key for currently selected packet on Start Packets Cast n Crew/Onboarding page */
	public static final String ATTR_ONBOARDING_CONTACT_DOCUMENT_ID = ATTR_PREFIX + "onboardDocument";
	/** Session attribute key for currently uploaded document on Documents Cast n Crew/Onboarding page */
	public static final String ATTR_ONBOARDING_UPLOADED_DOCUMENT_ID = ATTR_PREFIX + "uploadedDocumentId";
	/** Session attribute key for currently uploaded document on Documents Cast n Crew/Onboarding page */
	public static final String ATTR_ONBOARDING_UPLOADED_ATTACHMENT_ID = ATTR_PREFIX + "uploadedAttachmentId";
//	/** Session attribute key for currently selected contact on start status Cast n Crew/Onboarding page */
//	public static final String ATTR_ONBOARDING_SELECTED_CONTACT = ATTR_PREFIX + "contact";
//	/** Session attribute key for currently selected contact on Start Status Cast n Crew/Onboarding page */
//	public static final String ATTR_EMPLOYMENT_ID = ATTR_PREFIX + "employmentSelectedId";
	/** Session attribute key for currently selected contactdocument on Start form Payroll/Start Form page */
	public static final String ATTR_PAYROLL_CONTACT_DOCUMENT_ID = ATTR_PREFIX + "contactDocumentId";
	/** Session attribute key for currently selected Attachment on Start form Payroll/Start Form/Attachments page */
	public static final String ATTR_PAYROLL_ATTACHMENT_ID = ATTR_PREFIX + "attachmentId";
	/** Session attribute key for currently selected Attachment on Tiimecard/Attachments page */
	public static final String ATTR_TIMECARD_ATTACHMENT_ID = ATTR_PREFIX + "tcAttachmentId";
	/** Session attribute key for currently selected view for the Timecard(basic/full view). */
	public static final String ATTR_TIMECARD_VIEW = ATTR_PREFIX + "tcView";
	/** Maximum number of Attachments a WeeklyTimecard may have. */
	public static final int MAX_TC_ATTACHMENT = 100;

	/** Indices of the Timecard/Startforms tab. Used to clear the attachment list for Timecard/ContactDocument. *//*
	public static final int MY_TIMECARDS_TAB_IX = 29;
	public static final int MY_STARTS_TAB_IX = 30;
	public static final int TIMECARD_TAB_IX = 7;
	public static final int STARTFORMS_TAB_IX = 13;*/

	/** Session attribute key for the flag as to whether or not to show the detail section
	 * (start packet) on the Onboarding - Start Packets. */
	public static final String ATTR_ONBOARDING_SHOW_DETAIL = ATTR_PREFIX + "obShowDetail";
	/** String Literal, stores the default message to be shown on the approval path list under Approval Paths mini-tab */
	public static final String SELECT_OR_CREATE_APPROVAL_PATH = "(Select or Create an Approval Path)";
	/** String Literal, stores the default message to be shown on the approver group list under Approval Paths mini-tab */
	public static final String SELECT_OR_CREATE_APPROVER_GROUP = "(Select or Create an Approver Group)";
	/** String Literal, stores the the approval path under Approval Paths mini-tab */
	public static final String ATTR_ONBOARDING_APPROVAL_PATH_ID = ATTR_PREFIX + "obApprovalPath";
	/** String Literal, stores the current employment record instance for each contact */
	public static final String ATTR_ONBOARDING_EMPLOYMENT_ID = ATTR_PREFIX + "employmentId";
	/** String Literal, stores the current database id of the document to be previewed */
	//public static final String ATTR_ONBOARDING_SELECTED_DOCUMENT_ID = ATTR_PREFIX + "documentId";
	/** String Literal, stores the current database id of document chain of the document to be previewed */
	public static final String ATTR_ONBOARDING_SELECTED_DOCUMENT_CHAIN_ID = ATTR_PREFIX + "documentChainId";
	/** String Literal, stores the contact document id of the selected Form I9 */
	public static final String ATTR_ONBOARDING_SELECTED_FORM_I9_ID = ATTR_PREFIX + "formI9Id";
	/** String Literal, stores the contact document id of the clicked contact document on Review & Approval tab*/
	public static final String ATTR_ONBOARDING_CONTACTDOC_ID = ATTR_PREFIX + "contactDocumentId";
	/** String Literal, stores the the selected Document id from the filter under Distribution and Review mini-tab */
	public static final String ATTR_ONBOARDING_SELECTED_DOCUMENT_ID = ATTR_PREFIX + "selectedDocumentId";
	/** String Literal, stores the name of onboard folder. */
	public static final String ONBOARDING_FOLDER = "Onboarding";
	/** String Literal, stores the name of Primary folder, this folder stores replacements of documents. */
	public static final String PRIMARY_FOLDER = "Primary";
	/** String Literal, stores the prefix for Employee's signature button(s). */
	public static final String BUTTON_EMP_SIGN = "BtnEmpSign";
	/** String Literal, stores the prefix for Employee's Initial button(s). */
	public static final String BUTTON_EMP_INIT = "BtnEmpInit";
	/** String Literal, stores the prefix for Approver's signature button(s). */
	public static final String BUTTON_APP_SIGN = "BtnAppSign";
	/** String Literal, stores the prefix for Employee's signature value field. */
	public static final String EMP_SIGN_VALUE_FIELD = "EmployeeSignature";
	/** String Literal, stores the prefix for Employee's Initial value field. */
	public static final String EMP_INIT_VALUE_FIELD = "EmployeeInitial";
	/** String Literal, stores the prefix for Employee's signature's date value field. */
	public static final String EMP_DATE_VALUE_FIELD = "EmployeeSignatureDate";
	/** String Literal, stores the prefix for employee's initial's date field. */
	public static final String EMP_INIT_DATE_VALUE_FIELD = "EmployeeInitialDate";
	/** String Literal, stores the prefix for Approver's signature value field. */
	public static final String APP_SIGN_VALUE_FIELD = "ApproverSignature";
	/** String Literal, stores the prefix for Approver's signature's date value field. */
	public static final String APP_DATE_VALUE_FIELD = "ApproverSignatureDate";
	/** String Literal, stores the correct suffix format for the last name Junior. */
	public static final String LAST_NAME_SUFFIX_JUNIOR = "Jr";
	/** String Literal, stores the correct suffix format for the last name Senior. */
	public static final String LAST_NAME_SUFFIX_SENIOR = "Sr";

	/** Session attribute key to hold the value of the checkbox for the auto-fill popup,
	 *  as to whether to show the prompt for auto-fill next time for the user or not.  */
	public static final String ATTR_AUTO_FILL_PROMPT = ATTR_PREFIX + "showPrompt";
	/** Session attribute key to hold the value of the checkbox for the update account popup,
	 *  as to whether to show the prompt for  update account next time for the user or not.  */
	public static final String ATTR_UPDATE_ACCT_PROMPT = ATTR_PREFIX + "showUpdateAccountPrompt";
	/** Session attribute key to hold the value of the radio button selected for the instruction
	 *  view on the Approval Path tab, as to whether to show the tab with full instructions or
	 *  with short instructions to nexthe user for the next time.  */
	public static final String ATTR_INSTR_VIEW_PREF = ATTR_PREFIX + "instructionView";
	/** Session attribute key for the contact document whose attachment icon has been selected on StartForms page. */
	public static final String ATTR_ATTACHMENT_CONTACT_DOCUMENT = ATTR_PREFIX + "contactDocId";
	/** String Literal, stores the name of the generated default approval path. */
	public static final String DEFAULT_APPROVAL_PATH = "All Documents";
	/** This is a dummy Document.DocChainId value used for the replacement documents. */
	public static final Integer REPLACEMENT_DOCUMENT_CHAIN = -1;

	// Attributes for various pages "category" selections

	/** Session attribute key for current category selection on Point of Interest page. */
	public static final String ATTR_POI_CATEGORY = ATTR_PREFIX + "PointOfInterestListCategory";
	/** Session attribute key for current category selection on User list (admin) page. */
	public static final String ATTR_USER_CATEGORY = ATTR_PREFIX + "UserListCategory";
	/** Session attribute key for current category selection on My Productions List page.*/
	public static final String ATTR_MYPROD_CATEGORY = ATTR_PREFIX + "MyProductionListCategory";
	/** Session attribute key for current category selection on Productions Admin List page.*/
	public static final String ATTR_PROD_CATEGORY = ATTR_PREFIX + "ProductionListCategory";
	/** Session attribute key for the Script Element page Category selected. */
	public static final String ATTR_SE_CATEGORY = ATTR_PREFIX + "SeListCategory";
	/** Session attribute key for the Production Element page Category selected. */
	public static final String ATTR_RW_CATEGORY = ATTR_PREFIX + "RwListCategory";

	// Attributes for Stripboard viewer

	public static final String ATTR_SB_VIEW_SCENE_NUM = ATTR_PREFIX + "viewSceneNumber";
	public static final String ATTR_SB_VIEW_DAY_NUMBER = ATTR_PREFIX + "stripboardViewDay";

	// Attributes for Script Pages screen

	/** Session attribute key for which radio button is selected on the Scripts page. */
	public static final String ATTR_SP_GROUP = ATTR_PREFIX + "ScriptPagesGroup";
	/** Session attribute key for the starting page index on the Scripts page. */
	public static final String ATTR_SP_FROM_PAGE = ATTR_PREFIX + "ScriptPagesFromPage";
	/** Session attribute key for the ending page index on the Scripts page. */
	public static final String ATTR_SP_TO_PAGE = ATTR_PREFIX + "ScriptPagesToPage";
	/** Session attribute key for the starting scene index on the Scripts page. */
	public static final String ATTR_SP_FROM_SCENE = ATTR_PREFIX + "ScriptPagesFromScene";
	/** Session attribute key for the ending scene index on the Scripts page. */
	public static final String ATTR_SP_TO_SCENE = ATTR_PREFIX + "ScriptPagesToScene";
	/** Session attribute key for the starting shooting day index on the Scripts page. */
	public static final String ATTR_SP_FROM_DAY = ATTR_PREFIX + "ScriptPagesFromDay";
	/** Session attribute key for the ending shooting day index on the Scripts page. */
	public static final String ATTR_SP_TO_DAY = ATTR_PREFIX + "ScriptPagesToDay";
	/** Session attribute key for the value of the "highlight lines" checkbox on the Scripts page. */
	public static final String ATTR_SP_HIGHLIGHT = ATTR_PREFIX + "ScriptPagesHighlight";

	// Attributes for Script Compare page

	public static final String ATTR_SCRIPT_COMPARE_IMPORT = ATTR_PREFIX + "ScriptCompareImport";
	/** Session attribute key for the database id of the left-hand script on the Script Comparison page. */
	public static final String ATTR_SCRIPT_COMPARE_LEFT_ID = ATTR_PREFIX + "ScriptCompareLeftId";
	/** Session attribute key for the database id of the right-hand script on the Script Comparison page. */
	public static final String ATTR_SCRIPT_COMPARE_RIGHT_ID = ATTR_PREFIX + "ScriptCompareRightId";

	// Attributes for various Start Form related information

	/** Session attribute key for currently selected Start Form page. This is also used to track
	 * the currently selected Start Form on the Approver Dashboard - Start Forms mini-tab.*/
	public static final String ATTR_START_FORM_ID = ATTR_PREFIX + "StartFormId";

	/**  Session attribute key for the ProductionBatch to be displayed in the Start Forms page. */
	public static final String ATTR_SF_BATCH_ID = ATTR_PREFIX + "startFormBatchId";

	/**  Session attribute key for the ProductionBatch to be displayed in the Batch Setup page. */
	public static final String ATTR_SETUP_BATCH_ID = ATTR_PREFIX + "setupBatchId";

	// Attributes for various Timecard beans and pages

	/** Session attribute key for the HourRoundingType that specifies how to round decimal times
	 * entered by the user. */
	public static final String ATTR_TIME_ROUNDING_TYPE = ATTR_PREFIX + "timeRoundingType";

	/** Session attribute key for Boolean indicating if times entered in decimal format
	 * should be rounded. */
	public static final String ATTR_ROUND_DECIMAL_TIME = ATTR_PREFIX + "roundDecimalTime";

	/** Session attribute key for the last-used FilterType enum. */
	public static final String ATTR_FILTER_BY = ATTR_PREFIX + "filterBy";

	/** Session attribute key for the database id of the WeeklyTimecard to display. */
	public static final String ATTR_TIMECARD_ID = ATTR_PREFIX + "timecardId";

	/** Session attribute key for the flag as to whether or not to show all Projects of a
	 * Commercial production.  This tracks the "show all projects" check-box setting. */
	public static final String ATTR_FULL_TC_SHOW_ALL = ATTR_PREFIX + "fullTimecardShowAll";

	/** Session attribute key for the database id of the User whose timecards are
	 * currently on display. */
	public static final String ATTR_TC_TCUSER_ID = ATTR_PREFIX + "tcTimecardUserId";

	/** Session attribute key for the preference to expand or collapse the Pay Jobs table. */
	public static final String ATTR_TC_EXPAND_JOBS = ATTR_PREFIX + "tcExpandJobs";

	/** Session attribute key for the preference to expand or collapse the Job Splits table. */
	public static final String ATTR_TC_EXPAND_SPLIT = ATTR_PREFIX + "tcExpandSplit";

	/** Session attribute key for the list of timecard ids that could not be cloned. */
	public static final String ATTR_TC_CLONE_ERRORS = ATTR_PREFIX + "tcCloneErrors";

	/** Session attribute key for the count of successfully cloned timecards. */
	public static final String ATTR_TC_CLONE_COUNT = ATTR_PREFIX + "tcCloneCount";

	/** Session attribute key for the Production to select on the My Timecards page. */
	public static final String ATTR_VIEW_PRODUCTION_ID = ATTR_PREFIX +"viewProductionId";

	/** Session attribute key for the database id of the WeeklyBatch to display on
	 * the Transfer to Payroll page. */
	public static final String ATTR_BATCH_ID = ATTR_PREFIX + "batchId";

	/**  Session attribute key for the database id of the WeeklyBatch to be used for
	 * filtering in the dashboard pages. */
	public static final String ATTR_FILTER_BATCH_ID = ATTR_PREFIX + "filterBatchId";

	/** Session attribute key for the navigation string to be used for the
	 * "Return" button on the "Start Form" timecard page. (This is necessary
	 * because the user may get to the page from either the dashboard
	 * "Start Form List" or the Cast & Crew page.) */
	public static final String ATTR_START_FORM_BACK_PAGE = ATTR_PREFIX + "startFormBackPage";

	/** Session attribute key for the integer value that specifies the last day of
	 * the timecard week; may vary by production and project. 1=Sunday, 7=Saturday. */
	public static final String ATTR_TC_WEEK_END_DAY = "tcWeekEndDay";

	/** Session attribute key for the week-ending date for the timecard pages to display. */
	public static final String ATTR_WEEKEND_DATE = ATTR_PREFIX + "weekEndDate";

	/** Session attribute key for the week-ending date selected on the Approver dashboard
	 *  page week-ending drop-down and on the Tours Timesheet page week-ending drop-down. */
	public static final String ATTR_APPROVER_DATE = ATTR_PREFIX + "approverDate";

	/** Session attribute key for the department name selected from the Department filter
	 * drop-down on the Approver Dashboard page. */
	public static final String ATTR_APPROVER_DEPT = ATTR_PREFIX + "approverDept";

	/** Session attribute key for the employee account number for the Employee filter drop-down on
	 * the Approver Dashboard page. */
	public static final String ATTR_APPROVER_EMPLOYEE = ATTR_PREFIX + "approverEmployeeAcct";

	/** Session attribute key for the Status filter drop-down setting on the
	 * Approver Dashboard page. */
	public static final String ATTR_APPROVER_STATUS = ATTR_PREFIX + "approverStatus";

	/** Session attribute key for the flag as to whether or not to show the detail section
	 * (PR compare and chart) on the Approver Dashboard - Timecard Review and Gross Payroll. */
	public static final String ATTR_APPROVER_SHOW_DETAIL = ATTR_PREFIX + "approverShowDetail";

	/** Session attribute key for the flag as to whether or not to show all Projects of a
	 * Commercial production.  This tracks the "show all projects" check-box setting. */
	public static final String ATTR_APPROVER_SHOW_ALL = ATTR_PREFIX + "approverShowAll";

	/** Session attribute key for the database id of the user's last-entered Production; used in
	 * Mobile Timecard for finding the 'current' timecard, and as the default Production selected
	 * when logging into LS, on the My Productions page. */
	public static final String ATTR_LAST_PROD_ID = ATTR_PREFIX + "mtcProdId";

	/** Session attribute key for the database id of the user's last-entered Project; used in
	 * Mobile Timecard for finding the 'current' timecard, and as the default Project selected
	 * when logging into LS, on the My Productions page. Applies only to Commercial productions. */
	public static final String ATTR_LAST_PROJECT_ID = ATTR_PREFIX + "mtcProjectId";

	/** String Literal, stores the attachment string. */
	public static final String ATTACHMENT = "ATTACHMENTS";


	// Attributes for various Mobile Timecard beans and pages

	/** Session attribute key for the navigation string of the page to go to after
	 * successfully creating or changing the user's PIN. */
	public static final String ATTR_PIN_NEXT_PAGE = ATTR_PREFIX + "pinNextPage";

	/** Session attribute key for the navigation string to be used for the "Back"
	 * button on the "Timecard List" mobile timecard page.  This is necessary because
	 * the user may return either to the My Productions page or the My Projects page. */
	public static final String ATTR_TCS_BACK_PAGE = ATTR_PREFIX + "tcsBackPage";

	/** Session attribute key for the navigation string to be used for the "Back"
	 * button on the "hours entry" mobile timecard page.  (This is necessary because
	 * the user may get to the hours page from either the Timecard Review page for
	 * approvers, or from the Weekly Payroll page for one's own timecard.) */
	public static final String ATTR_HOURS_BACK_PAGE = ATTR_PREFIX + "hoursBackPage";

	/** Session attribute key for the navigation string to be used for the "Back" and
	 * "Cancel" buttons on the "Submit" and "Submit and Approve" mobile timecard pages.
	 * (This is necessary because the user may get to the page from either the
	 * Timecard Review page for approvers, or from the Weekly Payroll page for
	 * one's own timecard.) */
	public static final String ATTR_SUBMIT_BACK_PAGE = ATTR_PREFIX + "submitBackPage";

	/** Session attribute key for the navigation string to be used for the "Back" and
	 * "Cancel" buttons on the "Approve" and "Reject" mobile timecard pages. (This is
	 * necessary because the user may get to the page from either the Timecard
	 * Review page or the Approver Dashboard.) */
	public static final String ATTR_APPROVE_BACK_PAGE = ATTR_PREFIX + "approveBackPage";

	/** Session attribute key for the database id of the current StartForm in Mobile Timecard. */
	public static final String ATTR_MTC_START_FORM_ID = ATTR_PREFIX + "mtcStartFormId";

	/** Session attribute key for the current week-ending date selected or displayed in Mobile Timecard. */
	public static final String ATTR_MTC_DATE = ATTR_PREFIX + "mtcDate";

	/** Session attribute key for the database id of the current mileageLine (within the mileageLines
	 * List of the weeklyTimecard.mileage object) displayed on the Weekly Mileage (Mobile) page. */
	public static final String ATTR_MTC_MILEAGE_ID = ATTR_PREFIX + "mtcMileageId";

	/** Session attribute key for the database id of the current payExpense (within the expenseLines
	 * List of the weeklyTimecard object) displayed on the Weekly Expenses (Mobile) page. */
	public static final String ATTR_MTC_EXPENSE_ID = ATTR_PREFIX + "mtcExpenseId";

	/** Session attribute key for boolean indicating whether to show the timecard
	 * section of the Weekly Payroll page in Mobile Timecard. */
	public static final String ATTR_MTC_SHOW_TC = ATTR_PREFIX + "mtcShowTimecard";

	/** Session attribute key for boolean indicating whether to show the Box Rental
	 * section of the Weekly Payroll page in Mobile Timecard. */
	public static final String ATTR_MTC_SHOW_BOX = ATTR_PREFIX + "mtcShowBoxRental";

	/** Session attribute key for boolean indicating whether to show the Mileage
	 * section of the Weekly Payroll page in Mobile Timecard. */
	public static final String ATTR_MTC_SHOW_MILES = ATTR_PREFIX + "mtcShowMileage";

	/** Session attribute key for boolean indicating whether to show the Expenses
	 * section of the Weekly Payroll page in Mobile Timecard. */
	public static final String ATTR_MTC_SHOW_EXPENSES = ATTR_PREFIX + "mtcShowExpenses";

	/** Session attribute key for boolean indicating whether to show the Mileage
	 * section of the Weekly Payroll page in Mobile Timecard. */
	public static final String ATTR_MTC_SHOW_MORE_DAILY = ATTR_PREFIX + "mtcShowMoreDaily";

	/** Session attribute key for boolean indicating whether to show the Start Information
	 * section of the Weekly Payroll page in Mobile Timecard. */
	public static final String ATTR_MTC_SHOW_START_INFO = ATTR_PREFIX + "mtcShowStartInfo";


	// Attributes for Payroll Contracts page(s)

	/** Session attribute key for option to hide the Occupation Code list. */
	public static final String ATTR_CT_HIDE_OCCLIST =  ATTR_PREFIX + "hideOccList";

	/** Session attribute key for option to hide the Schedule list. */
	public static final String ATTR_CT_HIDE_SCHEDULE =  ATTR_PREFIX + "hideSchedule";


	// Attributes for reports

	/** Session attribute key for the callsheet.id value of the callsheet being displayed. */
	public static final String ATTR_CALL_SHEET_ID = ATTR_PREFIX + "callsheetid";
	/** Session attribute key for the date of the callsheet being displayed */
	public static final String ATTR_CALL_SHEET_DATE = ATTR_PREFIX + "callsheetdate";
	/** Session attribute key for the unit number of a report being displayed. */
	public static final String ATTR_UNIT_NUMBER = ATTR_PREFIX + "unitnumber";
	/** Session attribute key for the date of a report to display. */
	public static final String ATTR_REPORT_DATE = ATTR_PREFIX + "reportDate";

	// Attributes for System Event Log page

	/** Session attribute key for the starting date of the range of events to display. */
	public static final String ATTR_EVENT_START = ATTR_PREFIX + "EventStartDate";
	/** Session attribute key for the ending date of the range of events to display. */
	public static final String ATTR_EVENT_END = ATTR_PREFIX + "EventEndDate";

	// Attributes for Coupon generation & display page

	/** Session attribute key for Coupon generation - coupon start date. */
	public static final String ATTR_COUPON_START = ATTR_PREFIX + "CouponStartDate";
	/** Session attribute key for Coupon generation - coupon end date.  */
	public static final String ATTR_COUPON_END = ATTR_PREFIX + "CouponEndDate";
	/** Session attribute key for expense claim without attachment LS-2643 */
	public static final String ATTR_EXPENSE_WITHOUT_ATTACHMENT = ATTR_PREFIX + "expenseWithoutAttachment";
	/** For time converter - used on callsheet and possibly other reports */
	public static final String ON_CALL = "O/C";

	// Standard success/failure result values for JSP

	public static final String SUCCESS = "success";
	public static final String FAILURE = "failure";

	// Application default values

	/** The default ISO Country code for all addresses, etc. */
	public static final String DEFAULT_COUNTRY_CODE = "US";

	/** Canada ISO code */
	public static final String COUNTRY_CODE_CANADA = "CA";

	/** Mexico ISO code LS-1937 */
	public static final String COUNTRY_CODE_MEXICO = "MX";

	// 	* * * * * * * WATER MARK DEFAULTS * * * * * * *

	/** Date style for date added to watermark on printed scripts or other reports. */
	public static final String WATERMARK_DATE = "MMM d, yyyy";

	public static final Color BLACK = new Color(0);
	public static final Color WM_DEF_COLOR = BLACK;

	// hpos & vpos = -1 makes the watermark utility center the text on the page
	public static final int WM_DEF_HPOS = -1; //(int)(1.5f * POINTS_PER_INCH);
	public static final int WM_DEF_VPOS = -1; //(int)(1.5f * POINTS_PER_INCH);
	public static final int WM_DEF_FONTSIZE = 54; // 54pt = about 3/4 inch high
	public static final int WM_DEF_OPACITY = 30;  // percent opacity, 0-100.
	public static final TextDirection WM_DEF_DIRECTION = TextDirection.BOTTOMLEFT_TOPRIGHT;;

	// 	* * * * * * * Hold/Drop settings * * * * * * *

	public static final boolean DEFAULT_HOLD_ALLOWED = true;
	public static final int DEFAULT_HELD_BEFORE_DROP = 10;

	// 	* * * * * * * Image settings * * * * * * *

	public static final long MAX_IMAGE_FILESIZE = (6*1024*1024); // 6MB max image upload; change msg if changed!
	public static final long TARGET_IMAGE_FILESIZE = (300*1024); // Shrink uploaded files to this

	public static final int MAX_THUMBNAIL_SIZE = 200;	// max width or height of thumbnails

	// * * * * * * LOGIN / PASSWORD / LOCKOUT SETTINGS * * * * * *

	/** This many failed logins allowed per 24 hours; 1 more will lock out the user. */
	public static final int LOGIN_DAILY_FAILURE_COUNT = 4;

	public static final int MIN_PASSWORD_LENGTH = 8;
	public static final int MAX_PASSWORD_LENGTH = 20;

	/** How many hours an emailed link (for reset password or 3rd party
	 * user invitation) will be valid, i.e., before it expires. */
	public static final int TIME_EMAIL_LINK_VALID = 48;

	public static final int MIN_PIN_LENGTH = 4;

	// * * * * * Product / Production creation/purchase/expiration * * * * *

	/** The maximum number of DataAdmin accounts an expired Production may have.
	 * If an account has more than this number, logon access for the additional ones
	 * will be turned off when the production expires. */
	public static final int MAX_EXPIRED_ADMINS = 5;
	/** The maximum number of read/write projects an expired Production may have.
	 * If an account has more projects than this, the additional projects are marked
	 * read-only. */
	public static final int MAX_EXPIRED_WRITE_PROJECTS = 5;

	/** Maximum number of free (trial) productions one user account may have. */
	public static final int MAX_FREE_PRODUCTIONS = 1;

	/** Maximum number of student productions one user account may have. */
	public static final int MAX_STUDENT_PRODUCTIONS = 3;

	/** The SKU that a Free production may upgrade to. */
	public static final String UPGRADE_SKU = "F-IN-01";

	// * * * * * * * Constants for report due & overdue tests * * * * * * *

	/** How frequently (how many hours apart) we should issue "overdue" messages for reports. */
	public static final int REPORT_OVERDUE_FREQUENCY_HOURS = 4;

	/** The maximum number of minutes "old" a UserCallInfo evaluation can
	 * be before we recalculate it (to determine the "current" callsheet). */
	public static final int MAX_EVAL_CALLSHEET_MINUTES = 5;

	/** How many hours past a call sheet's Crew Call time is it still considered "current"? */
	public static final int CS_PREFERRED_HOURS_USED = 4;

	/** The minimum number of minutes a Callsheet will be considered "current" after
	 * its call time. */
	public static final int CS_MINIMUM_MINUTES_USED = 15;

	/** The number of hours between the current time and a future callsheet's call
	 * time at which time that callsheet will become "current", overriding the
	 * CS_PREFERRED_HOURS_USED value of an earlier callsheet. */
	public static final int CS_WITHIN_HOURS_PREFERRED = 9;

	/** When a call sheet is published, or changed after publishing, send
	 * notifications and the PDF to those user's with appropriate preferences if
	 * the current time is no more than this many hours after the call sheet's
	 * crew call time. */
	public static final int CS_WITHIN_HOURS_DISTRIBUTE = 24;

	/** First callsheet is due this many hours before 12:00am of the project start date. */
	public static final int CS_DUE_HOURS_BEFORE_PROJECT_START = 36;
	/** First callsheet is OVER-due this many hours before 12:00am of the project start date. */
	public static final int CS_OVERDUE_HOURS_BEFORE_PROJECT_START = 12;
	/** The number of hours after a production day's call time when the next call sheet is due. */
	public static final int CS_DUE_HOURS_AFTER_CALLTIME = 6;
	/** The number of hours after a production day's call time when the next call sheet is OVER-due. */
	public static final int CS_OVERDUE_HOURS_AFTER_CALLTIME = 14;

	/** The number of hours after a production day's call time when the DPR is due. */
	public static final int DPR_DUE_HOURS_AFTER_CALLTIME = 14;
	/** The number of hours after a production day's call time when the DPR is OVER-due. */
	public static final int DPR_OVERDUE_HOURS_AFTER_CALLTIME = 24;

	/** The number of hours after a production day's call time when the time sheet is due. */
	//public static final int TS_DUE_HOURS_AFTER_CALLTIME = 13;
	/** The number of hours after a production day's call time when the time sheet is OVER-due. */
	//public static final int TS_OVERDUE_HOURS_AFTER_CALLTIME = 24;

	/** The number of hours after a production day's call time when the ExhibitG is due. */
	//public static final int EXHIBITG_DUE_HOURS_AFTER_CALLTIME = 16;
	/** The number of hours after a production day's call time when the ExhibitG is OVER-due. */
	//public static final int EXHIBITG_OVERDUE_HOURS_AFTER_CALLTIME = 48;


	// * * * * * * * * Constants for Report Page * * * * * * *

	public static final String REPORT_TYPE_PEOPLE = "Cast & Crew";
	public static final String REPORT_TYPE_BREAKDOWN = "Scene Breakdown / Shooting Schedule";
	public static final String REPORT_TYPE_DOOD = "DooD / Elements";
	public static final String REPORT_TYPE_STRIPBOARD = "Strip board";
	public static final String REPORT_TYPE_CALLSHEET = "Call Sheet";
	public static final String REPORT_TYPE_EXHIBITG = "SAG Exhibit G";
	public static final String REPORT_TYPE_DPR = "Production Report";


	// * * * Section numbers within Call Sheet for specific types of messages * * *

	public static final int CN_SECTION_TOP_MESSAGE1 = 0; // appears below title, above "Crew call"
	public static final int CN_SECTION_TOP_MESSAGE2 = 1; // appears below "Shooting call", above Scene info
	public static final int CN_SECTION_CONTACT_MESSAGE = 21; // appears at bottom of front page
	public static final int CN_SECTION_IN_USE[] = {0,1,2,3,4,5,6,7,8,9,10,21,22,23,24,30};

	// * * * Strings for Initialization Parameters in web.xml: com.lightspeedeps.model.xxxx * * *

	public static final String INIT_PARAM_TIME_ZONE = "timeZone";
	public static final String INIT_PARAM_DST = "DST";
	public static final String INIT_PARAM_ACCOUNT_PREFIX = "accountPrefix";
	public static final String INIT_PARAM_PRODUCTION_PREFIX = "productionPrefix";
	public static final String INIT_PARAM_PROD_ADMIN = "prodAdmin";
	public static final String INIT_PARAM_PROD_ADMIN_VIEW = "prodAdminView";
	public static final String INIT_PARAM_EMAIL_AUDIT = "emailAuditTrail";
	public static final String INIT_PARAM_SEND_MAIL_FROM = "sendEmailFrom";
	public static final String INIT_PARAM_SECURE_BASE_URL = "secureBaseURL";
	public static final String INIT_PARAM_IS_OFFLINE = "isOffline";

	// Authorization parameters
	/**  Initialization parameter name for authorization client name (ID) */
	public static final String INIT_PARAM_AUTH_CLIENT_ID = "authClientId";
	/**  Initialization parameter name for authorization client password (secret) */
	public static final String INIT_PARAM_AUTH_CLIENT_PW = "authClientPassword";
	/**  Initialization parameter name for authorization standard user name (ID) */
	public static final String INIT_PARAM_AUTH_STANDARD_USER_ID = "authStandardUserId";
	/**  Initialization parameter name for authorization for standard user's password */
	public static final String INIT_PARAM_AUTH_STANDARD_USER_PW = "authStandardUserPassword";

	/**  Initialization parameter name for root URL (protocol+domain) for API services. */
	public static final String INIT_PARAM_API_SERVER_DOMAIN = "apiServerDomain";

	/**  Initialization parameter name for port number used for outbound SFTP connections,
	 * e.g., payroll data transfers. */
	public static final String INIT_PARAM_SFTP_PORT = "sftpPort";

	/** Initialization parameter name for boolean indicating if using Production (true) or
	 * Test (false) mode of authorize.net. */
	public static final String INIT_PARAM_AUTHORIZE_PRODUCTION = "authorizeProduction";

	// * * * * * *  URLs, partial pathnames, etc. * * * * * *

	/** URL for the "My Productions" home page. */
	public static final String HOME_PATH = "jsp/user/myprod.xhtml";
	/** URL for the Mobile user home page. */
	public static final String HOME_PATH_MOBILE = "m/u/home.xhtml";
	/** relative path from usual jsp pages to our image directory */
	public static final String IMAGE_PATH = "../../i/";


	// * * * Checkout URLs

	/** URL passed to Authorize.net which the user can click after paying for
	 * a subscription, to return to our application. */
	public static final String CHECKOUT_PAID_LINK = "jsp/product/paid.jsf";
	/** URL passed to Authorize.net which the user can click after paying for
	 * a subscription, to return to our application. */
	public static final String CHECKOUT_RETURN_LINK = "jsp/user/myprod.jsf";
	/** URL passed to Authorize.net which the user can click if they decide not
	 * to pay for a subscription (after reaching the Authorize.net checkout page),
	 * to return to our application. */
	public static final String CHECKOUT_CANCEL_LINK = "jsp/product/list.jsf";

	// * * * Email'd URLs

	/** URL used for the password-reset-return link in email. */
	public static final String RESET_PW_PATH = "r.jsp";
	/** URL used for the Calendar View link in email. */
	public static final String CALENDAR_PATH = "c.jsp";
	/** URL used for the Callsheet View link in email. */
	public static final String CALLSHEET_PATH = "s.jsp";
	/** URL used for the New User (account) link in email. */
	public static final String NEW_USER_PATH = "jsp/login/resetnew.jsf";
	/** URL used for the New User (account) link in email which redirects to the ESS application. */
	public static final String NEW_USER_PATH_ESS = "ttcosignup";
	/** URL used for the DPR View link in email, to DPR Approver. */
	public static final String DPR_PATH = "jsp/dpr/view.jsf";
	/** URL used to direct to My Account page. */
	public static final String MY_ACCT_PATH = "jsp/user/account.jsf";

	// * * * API URLs

	/** The first part of the URL used for invoking the Onboarding API micro-service. */
	public final static String ONBOARDING_URL_PREFIX = "/form/";

	/** The first part of the URL used for invoking the Onboarding API micro-service. */
	public final static String ONBOARDING_PRINT_URL = "/form/print/id/";

	// * * * Server directory paths

	/** The relative path to the directory for our generated report files. */
	public static final String REPORT_FOLDER = "report";

	// * * * * * *   Special page-field-keys   * * * * * * *

	/** Special page-field-key indicating a user is logged in, but has not selected a production */
	public static final String PGKEY_LOGGED_IN_ONLY = "0.1,account";

	/** Page-field-key assigned via normal (spreadsheet) permissions which gives access
	 * to approval dashboard, among other things. */
	public static final String PGKEY_TC_APPROVAL = "0.2,tc_approval";

	/** Page-field-key assigned via normal (spreadsheet) permissions which gives access
	 * to the Payroll / Start Forms tab. This permission may be removed by AuthorizationBean
	 * if a Production is not allowed to use Onboarding. */
	public static final String PGKEY_PAYROLL_TAB = "0.2,payroll_tab";

	/** Special page-field-key assigned to any logged-in user (at all times) */
	public static final String PGKEY_ONLINE = "0.0,online";

	/** Special page-field-key assigned (at all times) to a logged-in user who has the
	 * user.showUS flag set to true.  Used for tabs or fields that are hidden from
	 * Canadian-only users. */
	public static final String PGKEY_ONLINE_US = "0.0,online_us";

	/** Special page-field-key assigned (at all times) to a logged-in user who has the
	 * user.showCanada flag set to true.  Used for tabs or fields that are only shown
	 * to Canadian users. */
	public static final String PGKEY_ONLINE_CA = "0.0,online_canada";

	/** Special page-field-key allowing display of the ESS menu/link. LS-3758 */
	public static final String PGKEY_ESS = "0.0,ess";

	/** Special page-field-key assigned to a user who has entered a production. */
	public static final String PGKEY_PRODUCTION = "0.0,production";

	/** Special page-field-key assigned when the user's current production (whether "entered"
	 * or not) is writable -- i.e., not read-only due to being archived, expired, etc.  The
	 * presence of this key is logically equivalent to the Production.isWritable value being
	 * true; however, in some cases (such as in PageAuthenticatePhaseListener), checking the
	 * page-key is simpler. */
	public static final String PGKEY_WRITABLE_PRODUCTION = "0.0,writable_prod";

	/** Special page-field-key assigned if the current production allows On-Boarding features.
	 * This is based on the Production.allowOnboarding flag. */
	public static final String PGKEY_ALLOW_ONBOARDING = "0.0,onboarding";

	/** Page-field-key (assigned via normal permissions) which is only assigned to LS EPS ADMIN
	 * users -- this is the page-field-key associated with the "super user" role. */
	public static final String PGKEY_WRITE_ANY = "2.0,write_any";

	/** Page-field-key (assigned via normal permissions) allowing a user to create
	 * a custom role; usually associated with Edit_Contacts_Crew permission. */
	public static final String PGKEY_CUSTOM_ROLE = "3.1.1,custom_role";

	/** Page-field-key (assigned via normal permissions) usually associated with View_Call_Sheet_List
	 * permission. */
	public static final String PGKEY_VIEW_CS_LIST ="7.2,view_call_sheet_list";

	/** Page-field-key (assigned via normal permissions) usually associated with View_Contacts_Cast
	 * permission. */
	public static final String PGKEY_VIEW_CAST = "7.2,view_contacts_cast";

	/** Page-field-key (assigned via normal permissions) usually associated with View_Contacts_Crew
	 * permission. */
	public static final String PGKEY_VIEW_CREW = "7.2,view_contacts_crew";

	/** Page-field-key (assigned via normal permissions) usually associated with View_Exhibit_G
	 * permission. */
	public static final String PGKEY_VIEW_EX_G = "7.2,view_exhibit_g";

	/** Page-field-key (assigned via normal permissions) usually associated with View_Production_Report
	 * permission. */
	public static final String PGKEY_VIEW_DPR = "7.2,view_production_report";

	/** Page-field-key (assigned via normal permissions) usually associated with Edit_HTG
	 * permission. */
	public static final String PGKEY_EDIT_HTG = "9.1,edit";

	/** Page-field-key (assigned via normal permissions) usually associated with View_HTG
	 * permission. */
	public static final String PGKEY_VIEW_HTG = "9.1,view_htg";

	/** Page-field-key assigned via VIEW_ALL_PROJECTS permission, which is only given
	 * to the Financial Data Admin role. */
	public static final String PGKEY_ALL_PROJECTS = "9.2,all_projects";

	/** Page-field-key (assigned via normal permissions) allowing a user to view Start Forms;
	 * usually associated with View_HTG permission. */
	public static final String PGKEY_VIEW_STARTS = "9.3,view";

	/** Page-field-key (assigned via normal permissions) allowing a user to select ALL
	 * timecards in a production (or project for Commercial) to print or run HTG on. */
	public static final String PGKEY_ALL_TIMECARDS = "9.4,all_timecards";

	/** Page-field-key (assigned via normal permissions), usually associated with View_HTG
	 * permission, allowing access to the Export ABS payroll feature. */
	public static final String PGKEY_EXPORT_ABS = "9.4,export_abs";

	/** Page-field-key (assigned via normal permissions), usually associated with View_HTG
	 * permission, allowing access to the Export HotBudget feature. */
	public static final String PGKEY_EXPORT_HB = "9.4,export_hb";

	/** Page-field-key (assigned via normal permissions), usually associated with View_HTG
	 * permission, allowing access to the Export Showbiz Budgeting feature. */
	public static final String PGKEY_EXPORT_SBB = "9.4,export_sbb";

	/** Page-field-key (assigned via normal permissions), usually associated with View_HTG
	 * permission, allowing access to the Export JSON feature. */
	public static final String PGKEY_EXPORT_JSON = "9.4,export_json";

	/** Page-field-key (assigned via normal permissions), usually associated with View_HTG
	 * permission, allowing access to the Export payroll (as CSV) feature. */
	public static final String PGKEY_EXPORT_PAYROLL = "9.4,export_payroll";

	/** Page-field-key (assigned via normal permissions), usually associated with View_HTG
	 * permission, allowing access to the TEAM Export file layout feature. */
	public static final String PGKEY_EXPORT_TEAM = "9.4,export_team";

	/** Page-field-key (assigned via normal permissions), giving the user the
	 * authority to view the payroll "Summary Sheets". */
	public static final String PGKEY_VIEW_SUMMARY = "10.5,view_summary";

	// * * * * * *  Import/Export constants * * * * * *

	public static final String EXPORT_TYPE = ".lse";
	public static final String EXPORT_FOLDER = "files";
	public static final String DOWNLOAD_PREFIX = "download.";
	public static final String UPLOAD_PREFIX = "upload.";

	/** Filename suffix used for CrewCards download files from payroll.
	 * These are tab-delimited. */
	public static final String TIMECARD_SUFFIX = ".tab"; //
	/** Filename suffix used for comma-separated download files from payroll. */
	public static final String COMMA_SUFFIX = ".csv"; //
	/** Filename suffix used for JSON files in payroll transfer. */
	public static final String JSON_SUFFIX = ".json"; //

	// * * * * * *  Archive constants * * * * * *

	public static final String ARCHIVE_ROOT = "Archives";
	public static final String ARCHIVE_PROJECT = ARCHIVE_ROOT + "/Project{0}";
	public static final String ARCHIVE_CALLSHEET = ARCHIVE_PROJECT + "/Unit{1}/Callsheet";
	public static final String ARCHIVE_RETRIEVAL_FOLDER = "files";

	// * * * * * *  Other file-related constants * * * * * *

	public static final String QUICK_START_GUIDE_FILENAME = "Lightspeed Quick Start Guide.pdf";

	/** A "directory" used to create a filename indicating that the file data will be retrieved from
	 * the database by our servlet (LsFacesServlet) when the file is requested by a browser. */
	public static final String IMAGE_PSEUDO_DIRECTORY = "_db_image";

	/** A "directory" used to create a filename indicating that the file data will be retrieved from
	 * the database by our servlet (LsFacesServlet) when the file is requested by a browser. */
	public static final String DOCUMENT_PSEUDO_DIRECTORY = "_db_document";

	/** A "directory" used to create a filename indicating that the file data will be retrieved from
	 * the database by our servlet (LsFacesServlet) when the file is requested by a browser. */
	public static final String ATTACHMENT_PSEUDO_DIRECTORY = "_db_attachment";

	/** A "directory" used to create a filename indicating that the file data will be retrieved from
	 * the database by our servlet (LsFacesServlet) when the file is requested by a browser. */
	public static final String DOCUMENT_XFDF_PSEUDO_DIRECTORY = "_db_annotations";

	/** The suffix added to a filename so that, when the request is delivered to our servlet, the
	 * data returned will be the XOD component of the document. */
	public static final String DOCUMENT_XOD_SUFFIX = ".xod";

	// * * * * * * * * * DATABASE ID VALUES * * * * * * * * *

	/** Database id of the "SYSTEM" Production used before a user enters a
	 * specific production. */
	public static final int SYSTEM_PRODUCTION_ID = 1;

	// * * * Role IDs for specific Roles or RoleGroups.

	/** Database id for the Role of "LS eps Administrator" - the 'super user'; this
	 * is the only role that is in RoleGroup 1. */
	public static final int ROLE_ID_LS_ADMIN = 2;

	/** Database id for the Role of "LS Viewer" in the "LS Admin" dept.  This is
	 * designed to give read-only access to all data within a production. */
	public static final int ROLE_ID_LS_ADMIN_VIEW = 3;

	/** Database id for the Role of LS Coordinator in the "LS Admin" dept. Has most of the
	 *  permissions LS eps Admin except for "Write Any".  This is also equivalent to
	 *  Financial Data Admin + Prod Data Admin + "manage start form approvers". Will be
	 *  used for Team account managers. */
	public static final int ROLE_ID_LS_COORDINATOR = 4;

	/** Database id for the Role of "VIP" in the LS Admin department. */
	public static final int ROLE_ID_LS_VIP = 99;

	/** Database id for the Role of Prod Data Admin */
	public static final int ROLE_ID_DATA_ADMIN = 422;

	/** Database id for the Role of Production Financial Admin */
	public static final int ROLE_ID_FINANCIAL_ADMIN = 423;

	/** Database id for the Role of Business Affairs Manager */
	public static final int ROLE_ID_ACTRA_BUSINESS_AFFAIRS_MANAGER = 1198;

	/** Database id for the Role of Agency Producer */
	public static final int ROLE_ID_ACTRA_AGENCY_PRODUCER = 1199;

	/** The Role_Group ID for the LS Admin group */
	public static final int ROLE_GROUP_ID_ADMIN = 1;

	/** The Role_Group ID for the LS Data Admin group */
	public static final int ROLE_GROUP_ID_DATA_ADMIN = 2;

	/** The Role_Group ID assigned to custom Role`s; should be the "General Crew" role group. */
	public static final int ROLE_GROUP_ID_CUSTOM_ROLES = 12;

	// * * * * * * Specific Department IDs  * * * * * *

	/** The ACTRA "Production" department value is used to determine certain actions
	 * for members (within a Canadian production) of that department, such as automatically
	 * adding them to the approval path.   LS-3661*/
	public static final int DEPARTMENT_ID_ACTRA_PRODUCTION = 151;

	/** The Cast department value is used for filtering Contact lists.  Most
	 * users, for example, are not allowed to view Contacts who have roles in
	 * the Cast department -- i.e., talent/actors.  */
	public static final int DEPARTMENT_ID_CAST = 124;

	/** The Stunts department value is used for filtering Contact lists.  Used in
	 * generating reports and in determining which persons in Cast&Crew list need
	 * to have associated Production (Real World) Elements. */
	public static final int DEPARTMENT_ID_STUNTS = 125;

	/** The LS Admin department value is used for filtering Contact lists.  Most
	 * users, for example, are not allowed to view Contacts who have roles in
	 * the LS Admin department. */
	public static final int DEPARTMENT_ID_LS_ADMIN = 128;

	/** The Data Admin department value is used for filtering Contact lists.  Most
	 * users, for example, are not allowed to view Contacts who have roles in
	 * the Data Admin department. */
	public static final int DEPARTMENT_ID_DATA_ADMIN = 129;

	/** The "Vendor" department is used for "non-production" contacts -- contacts
	 * who normally would not have a "Role" in the project; e.g., someone who
	 * manages a Location being used for the project. */
	public static final int DEPARTMENT_ID_VENDOR = 122;

	/** The Catering department section of the DPR back page is where LS adds
	 * catering/vendor/meal count information. */
	public static final int DEPARTMENT_ID_CATERING = 115;

	/** The default department is usually only used if something odd happens such that
	 * the code has a missing (null) department value. */
	public static final int DEFAULT_DEPARTMENT_ID = 101;	// "Set/production" department

	/** The list of department ids used in Touring and Hybrid productions, which have a
	 * restricted set of Roles to be filtered at run-time. */
	public static final Collection<Integer> RESTRICTED_ROLES_DEPARTMENT_IDS =
			new ArrayList<>(Arrays.asList(145,152,153,154,155,156,157,158));

	/** The default mask for new Production`s and Project`s, which includes all
	 * standard Department`s including Cast (whose mask is '00...1', the right-most, low-order, bit).
	 * This includes all masks up through LS Data Admin (mask position 43), but does NOT include
	 * the special Talent, Canada, Touring, and Hybrid departments. */
	public static final BitMask ALL_DEPARTMENTS_MASK = new BitMask(8796093022207L);  //      =x'00007FFFFFFFFFF'

	/** The default mask for Tours Production`s (and Project`s?), which is a
	 * relatively small subset of the standard Department`s. */
	public static final BitMask TOURS_DEPARTMENTS_MASK = new BitMask(139618185518252032L); // =x'1F0060000000000'
	// x'000020000000000' = 42 = LS Admin
	// x'000040000000000' = 43 = LS Data Admin
	// x'010000000000000' = 53 = Band
	// x'020000000000000' = 54 = Crew
	// x'040000000000000' = 55 = Dancers
	// x'080000000000000' = 56 = Drivers
	// x'100000000000000' = 57 = Officer

	/** The default mask for Tours Production`s (and Project`s?), which is a
	 * relatively small subset of the standard Department`s. */
	public static final BitMask HYBRID_DEPARTMENTS_MASK = new BitMask(468398550502342656L); // =x'680160000000000'
	// x'000020000000000' = 42 = LS Admin
	// x'000040000000000' = 43 = LS Data Admin
	// x'000100000000000' = 45 = Stationary
	// x'080000000000000' = 56 = Drivers
	// x'200000000000000' = 58 = Domestic
	// x'400000000000000' = 59 = Catering/Live Event

	/** The default mask for Canada Productions. This includes ACTRA cast department and a
	 * Actra Production department. Cast for US SAG/AFTRA talent may be added at a different
	 * time.
	 */
	public static final BitMask CANADA_DEPARTMENTS_MASK = new BitMask(1156305801397141504L); // =x'100C060000000000'
	// x'000020000000000' = LS Admin
	// x'000040000000000' = LS Data Admin
	// x'008000000000000' = Production - ACTRA
	// x'004000000000000' = Cast - ACTRA
	//x'1000000000000000' = Cast - UDA

	/** Department mask for US Talent includes separate cast departments for
	 * SAG-AFTRA Principals (A1) and Extras (A2) */
	public static final BitMask US_TALENT_DEPARTMENTS_MASK = new BitMask(112150186033154L); // =x'000660000000002'
	// x'000020000000000' = LS Admin
	// x'000040000000000' = LS Data Admin
	// x'000000000000002' = Production
	// x'000200000000000' = 46 = Cast - Principals (A1)
	// x'000400000000000' = 47 = Cast - Extras (A2)

	/** Department mask for Team Signatory production. This is used for Team employees who are working on set
	 * for a client.
	 */
	public static final BitMask SIGNATORY_DEPARTMENTS_MASK = new BitMask(576467349373190144L);
	// x'000020000000000' = LS Admin
	// x'000040000000000' = LS Data Admin
	// x'800000000000000' = Signatory

	// * * * * * * * TIME CARDS / START FORMS * * * * * * *

	/** The default for the first day of the week on a weekly timecard. */
	public static final int DEFAULT_WEEK_FIRST_DAY = Calendar.SUNDAY;

	/** The default for the last day of the week on a weekly timecard. */
	public static final int DEFAULT_WEEK_END_DAY = Calendar.SATURDAY;

	/** Maximum allowed MPV (meal penalty violations) per day. */
	public static final int MAX_DAILY_MPV = 40;

	/** The number of hours to assume for a "worked" day (when no hours are submitted)
	 * for a Union timecard (12). */
	public static final BigDecimal WORKED_HOURS_UNION = new BigDecimal(12).setScale(2);

	/** The number of hours to assume for a "worked" day (when no hours are submitted)
	 * for a Non-Union timecard (8). */
	public static final BigDecimal WORKED_HOURS_NON_UNION = new BigDecimal(8).setScale(2);

	/** The number of hours assumed to be paid as straight time for a "Daily" employee.  This
	 * may be used for export calculations.  HTG should use contract rule information! */
	public static final BigDecimal DAILY_STRAIGHT_HOURS = new BigDecimal(8).setScale(2);

	/** Number of hours in a day = 24. */
	public static final BigDecimal HOURS_IN_A_DAY = new BigDecimal(24);

	/** Number of hours to add to switch from AM to PM = 12. */
	public static final BigDecimal HOURS_FROM_AM_TO_PM = new BigDecimal(12);

	/** Number of hours in a week = 7*24 = 168. */
	public static final BigDecimal HOURS_IN_A_WEEK = new BigDecimal(7*24);

	/** Max number of hours allowed in a single PayJob (Job table) "cell". May be more
	 * than 24 in some cases (forced call). */
	public static final BigDecimal MAX_PAY_HOURS_IN_A_DAY = new BigDecimal(48);

	/** Number of minutes in an hour, as BigDecimal. */
	public static final BigDecimal MINUTES_PER_HOUR = new BigDecimal(60);

	/** A valid call time should be less than 24.0. */
	public static final BigDecimal LATEST_CALL_TIME_24 = HOURS_IN_A_DAY;

	/** A valid wrap time, or any meal time, on a day should be less than 72 (or even 48?). */
	public static final BigDecimal LATEST_WRAP_TIME_72 = new BigDecimal(72);

	/** The "guaranteed hours" for a weekly employee must be LESS than this amount. */
	public static final BigDecimal MAX_WEEKLY_GUAR_HOURS = DECIMAL_100;

	/** True: use the union contract letter codes for schedules. False: use numeric schedules. */
	public static final boolean USE_CONTRACT_SCHEDULES = true;

	/** The minimum number of decimal points (scale) to keep or display for hourly rates. */
	public static final int HOURLY_RATE_SCALE_MIN = 2;

	/** The maximum number of decimal points (scale) to keep or display for hourly rates. */
	public static final int HOURLY_RATE_SCALE_MAX = 4;

	/** The maximum length of text (usually an occupation name) placed onto a mobile time card button. */
	public static final int MAX_MOBILE_BUTTON_TEXT_LENGTH = 50;

	/** The Region_code in the Occupation table for low-budget production in L.A. and production cities. */
	public static final String PRODUCTION_CITY_REGION = "L";

	/** The Region_code in the Occupation table for low-budget production in non-production cities. */
	public static final String NON_PRODUCTION_CITY_REGION = "K";

	/** For Start Form "use rules from state" drop-down list -- code for "state worked". */
	public static final String STATE_WORKED_CODE = "X1";

	/** For Start Form "use rules from state" drop-down list -- text for "state worked". */
	public static final String STATE_WORKED_LABEL = "State Worked";

	/** String for electronic signature display */
	public static final String SIGNED_BY = "Signed by: ";

	/** Label prefix for Pay Breakdown "extended day" hours, used by DGA. */
	public static final String EXTENDED_DAY_PREFIX = "Extended Day @";

	// * * * * * * * START DOCUMENT export constants * * * * * * * *

	/** Number of signature event entries in the export file */
	public static final int DOC_EXPORT_NUMBER_SIGNATURES = 6;

	/** Transfer status value for a New document (first time transferred). */
	public static final String DOC_EXPORT_STATUS_NEW = "N";

	/** Transfer status value for an updated document (changed since last transferred). */
	public static final String DOC_EXPORT_STATUS_UPDATED = "U";

	// * * * * * * * CREW CARDS (export/import) constants * * * * * * * *

	/** The number of Jobs in a Crew Card timecard export file. */
	public static final int CREW_CARDS_NUMBER_JOBS = 7;

	/** The number of Pay Breakdown detail lines in a Crew Card timecard export file. */
	public static final int CREW_CARDS_NUMBER_PAYLINES = 30;

	/** The number of Pay Breakdown detail lines in a (Team) Tabbed timecard export file. */
	public static final int TABBED_NUMBER_PAYLINES = 60;

	/** The number of Expense table detail lines in a Crew Card timecard export file. */
	public static final int CREW_CARDS_NUMBER_EXPENSELINES = 10;

	/** The number of splits per day in a Crew Card timecard export file. */
	public static final int CREW_CARDS_NUMBER_SPLITS = 3;

	/** The number of Signature entries in a Crew Card timecard export file. */
	public static final int CREW_CARDS_NUMBER_SIGNATURES = 5;

	/** The number of Signature entries in a Crew Card timecard export file. */
	public static final int CREW_CARDS_NUMBER_MILEAGE_LINES = 7;

	/** TTC Online domain part*/
	public static final String TTC_ONLINE_DOMAIN_REFER_PART = "ttconline"; // LS-1691

	/** TTC Online domain */
	public static final String TTC_ONLINE_DOMAIN = "ttconline.com";

	/** TTC Online BETA domain -- used only for generating links in emails */
	public static final String TTC_ONLINE_BETA_DOMAIN = "qa.ttconline.com";

	/** Lightspeed domain */
//	public static final String LS_DOMAIN = "lightspeedeps.com";

	/** Lightspeed domain specific suffix for pulling out correct message text from Message.properties. */
//	public static final String LS_MESSAGE_SUFFIX = "LS";

	/** TTC Online domain specific suffix for pulling out correct message text from Message.properties. */
//	public static final String TEAM_MESSAGE_SUFFIX = "TEAM";


	/** TTC Online company name */
	public static final String TTC_ONLINE_COMPANY_NAME = "TTC Online";

	/** LS company name */
	public static final String LS_COMPANY_NAME = "LightSPEED eps";

	/** TTC Online support email */
	public static final String TTC_ONLINE_SUPPORT_EMAIL = "servicedesk@theteamcompanies.com";

	/** TTC Online support Phone # */
	public static final String TTC_ONLINE_SUPPORT_PHONE = "(747) 200-3391";

	/** LS support email */
	public static final String LS_SUPPORT_EMAIL = "support@lightspeedeps.com";

	/** LS support Phone # */
	public static final String LS_SUPPORT_PHONE = "(323) 851-2000";

	/** LS support extension */
	public static final String LS_SUPPORT_EXT = "ext 2";

	/** LS marketing extension */
	public static final String LS_MARKETING_EXT = "ext 1";

	/** TTC Online support extension */
	public static final String TTC_ONLINE_SUPPORT_EXT = "Ext 7777";

	/** Team home site */
	public static final String TEAM_HOME_URL = "theteamcompanies.com";

	/** LS home site */
	public static final String LS_HOME_URL = "lightspeedeps.com";

	/** User object's 'source system' when created by Team ESS (Employee Self-Service) */
	public static final String SOURCE_SYSTEM_ESS = "ESS"; // LS-3758

	/** Check User and set constant accordingly to HeaderText	 */
	public static final String HEADER_TEXT_FOR_US = "My Productions";
	public static final String HEADER_TEXT_FOR_CANADA = "My Agencies";

	public static final String PROJECT_TEXT_CANADA = "Production";
	public static final String PROJECT_S_TEXT_CANADA = "Productions";
	public static final String PROJECT_TEXT_US = "Project";
	public static final String PROJECT_S_TEXT_US = "Projects";

	public static final String CANADA_PRODUCTION_TEXT = "Agency";
	public static final String STANDARD_PRODUCTION_TEXT = "Production";
	public static final String CANADA_PRODUCTION_COMPANY_TEXT = "AGENCY";
	public static final String STANDARD_PRODUCTION_COMPANY_TEXT = "PRODUCTION COMPANY";
	public static final String PROJECTJOB_TEXT = "Project/Job";
	
	
	/** used to the AddressType and as Param for error messages while validations. LS-4478 to LS-4482  */
	public final static String HOME_ADDRESS="Resident Address";
	public final static String LOANOUT_ADDRESS="Corporation Address";
	public final static String LOANOUT_MAILING_ADDRESS="Corporation Mailing Address";
	public final static String AGENCY_ADDRESS="Agency Address";
	public final static String MAILING_ADDRESS="Mailing Address";
}
