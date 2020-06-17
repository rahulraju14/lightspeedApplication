/**
 * File: TimeZoneUtils.java
 */
package com.lightspeedeps.util.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A class for various Time Zone utilities. It holds the array that
 * maintains a correspondence between the displayed choices for
 * time zone selection (taken from the Windows list) and selected
 * equivalent Java time zone ID strings.  See also the source
 * spreadsheet TimeZoneList.xls.
 */
@ManagedBean
@ApplicationScoped
public class TimeZoneUtils implements Serializable {
	/** */
	private static final long serialVersionUID = - 4795490884033984997L;

	static Log log = LogFactory.getLog(TimeZoneUtils.class);

	/** Pairs of displayed value vs Java time zone id.  This array is
	 * used to generated the SelectItem List for the time zone drop-downs. */
	private static final String pairs[][] = {
			{"(GMT-12:00) International Date Line West","Etc/GMT+12"},
			{"(GMT-11:00) Coordinated Universal Time-11","Etc/GMT+11"},
			{"(GMT-10:00) Hawaii","US/Hawaii"},
			{"(GMT-09:00) Alaska","US/Alaska"},
			{"(GMT-08:00) Baja California","Mexico/BajaNorte"},
			{"(GMT-08:00) Pacific Time (US & Canada)","PST"},
			{"(GMT-07:00) Arizona","US/Arizona"},
			{"(GMT-07:00) Chihuahua, La Paz, Mazatlan","Mexico/BajaSur"},
			{"(GMT-07:00) Mountain Time (US & Canada)","US/Mountain"},
			{"(GMT-06:00) Central America","America/Guatemala"},
			{"(GMT-06:00) Central Time (US & Canada)","US/Central"},
			{"(GMT-06:00) Guadalajara, Mexico City","America/Mexico_City"},
			{"(GMT-06:00) Saskatchewan","Canada/Saskatchewan"},
			{"(GMT-05:00) Bogota, Lima, Quito","America/Bogota"},
			{"(GMT-05:00) Eastern Time (US & Canada)","US/Eastern"},
			{"(GMT-05:00) Indiana (East)","US/East-Indiana"},
			{"(GMT-04:30) Caracas","America/Caracas"},
			{"(GMT-04:00) Asuncion","America/Asuncion"},
			{"(GMT-04:00) Atlantic Time (Canada)","Canada/Atlantic"},
			{"(GMT-04:00) Cuiaba","America/Cuiaba"},
			{"(GMT-04:00) Georgetown, La Paz, Manaus","America/Puerto_Rico"},
			{"(GMT-04:00) Santiago","America/Santiago"},
			{"(GMT-03:30) Newfoundland","Canada/Newfoundland"},
			{"(GMT-03:00) Brasilia","Brazil/East"},
			{"(GMT-03:00) Buenos Aires","America/Buenos_Aires"},
			{"(GMT-03:00) Cayenne, Fortaleza","America/Cayenne"},
			{"(GMT-03:00) Greenland","America/Godthab"},
			{"(GMT-03:00) Montevideo","America/Montevideo"},
			{"(GMT-03:00) Salvador","BET"},
			{"(GMT-02:00) Coordinated Universal Time-02","Atlantic/South_Georgia"},
			{"(GMT-02:00) Mid-Atlantic","Etc/GMT+2"},
			{"(GMT-01:00) Azores","Atlantic/Azores"},
			{"(GMT-01:00) Cape Verde Is.","Atlantic/Cape_Verde"},
			{"(GMT) Casablanca","Africa/Casablanca"},
			{"(GMT) Coordinated Universal Time","Etc/UCT"},
			{"(GMT) Dublin, Edinburgh, London","Europe/Dublin"},
			{"(GMT) Monrovia, Reykjavik","Atlantic/Reykjavik"},
			{"(GMT+01:00) Amster., Berlin, Bern, Rome","Europe/Amsterdam"},
			{"(GMT+01:00) Belgrade, Budapest, Prague","Europe/Belgrade"},
			{"(GMT+01:00) Copenhagen, Madrid, Paris","Europe/Brussels"},
			{"(GMT+01:00) Sarajevo, Skopje, Warsaw","Europe/Sarajevo"},
			{"(GMT+01:00) West Central Africa","Africa/Algiers"},
			{"(GMT+01:00) Windhoek","Africa/Windhoek"},
			{"(GMT+02:00) Amman","Asia/Amman"},
			{"(GMT+02:00) Athens, Bucharest","Europe/Athens"},
			{"(GMT+02:00) Beirut","Asia/Beirut"},
			{"(GMT+02:00) Cairo","Africa/Cairo"},
			{"(GMT+02:00) Damascus","Asia/Damascus"},
			{"(GMT+02:00) Harare, Pretoria","Africa/Harare"},
			{"(GMT+02:00) Helsinki, Kyiv, Tallinn, Vilnius","Europe/Helsinki"},
			{"(GMT+02:00) Istanbul","Asia/Istanbul"},
			{"(GMT+02:00) Jerusalem","Asia/Jerusalem"},
			{"(GMT+02:00) Nicosia","Europe/Nicosia"},
			{"(GMT+03:00) Baghdad","Asia/Baghdad"},
			{"(GMT+03:00) Kaliningrad, Minsk","Europe/Minsk"},
			{"(GMT+03:00) Kuwait, Riyadh","Asia/Kuwait"},
			{"(GMT+03:00) Nairobi","Africa/Nairobi"},
			{"(GMT+03:30) Tehran","Asia/Tehran"},
			{"(GMT+04:00) Abu Dhabi, Muscat","Asia/Dubai"},
			{"(GMT+04:00) Baku","Asia/Baku"},
			{"(GMT+04:00) Moscow, St. Petersburg","Europe/Moscow"},
			{"(GMT+04:00) Port Louis","Asia/Muscat"},
			{"(GMT+04:00) Tbilisi","Asia/Tbilisi"},
			{"(GMT+04:00) Yerevan","Asia/Yerevan"},
			{"(GMT+04:30) Kabul","Asia/Kabul"},
			{"(GMT+05:00) Islamabad, Karachi","Asia/Karachi"},
			{"(GMT+05:00) Tashkent","Asia/Tashkent"},
			{"(GMT+05:30) Kolkata, Mumbai, New Delhi","Asia/Kolkata"},
			{"(GMT+05:30) Sri Jayawardenepura","IST"},
			{"(GMT+05:45) Kathmandu","Asia/Kathmandu"},
			{"(GMT+06:00) Astana","Asia/Almaty"},
			{"(GMT+06:00) Dhaku","Asia/Dhaka"},
			{"(GMT+06:00) Ekaterinburg","Asia/Yekaterinburg"},
			{"(GMT+06:30) Yangon (Rangoon)","Asia/Rangoon"},
			{"(GMT+07:00) Bangkok, Hanoi, Jakarta","Asia/Bangkok"},
			{"(GMT+07:00) Novosibirsk","Asia/Jakarta"},
			{"(GMT+08:00) Beijing, Chongqing, Hong Kong","Asia/Chongqing"},
			{"(GMT+08:00) Krasnoyarsk","Asia/Krasnoyarsk"},
			{"(GMT+08:00) Kuala Lumpur, Singapore","Singapore"},
			{"(GMT+08:00) Perth","Australia/Perth"},
			{"(GMT+08:00) Taipei","Asia/Taipei"},
			{"(GMT+08:00) Ulaanbaatar","Asia/Ulaanbaatar"},
			{"(GMT+09:00) Irkutsk","Asia/Irkutsk"},
			{"(GMT+09:00) Osaka, Sapporo, Tokyo","Asia/Tokyo"},
			{"(GMT+09:00) Seoul","Asia/Seoul"},
			{"(GMT+09:30) Adelaide","Australia/Adelaide"},
			{"(GMT+09:30) Darwin","Australia/Darwin"},
			{"(GMT+10:00) Brisbane","Australia/Brisbane"},
			{"(GMT+10:00) Canberra, Melbourne, Sydney","Australia/Melbourne"},
			{"(GMT+10:00) Guam, Port Moresby","Pacific/Guam"},
			{"(GMT+10:00) Hobart","Australia/Hobart"},
			{"(GMT+10:00) Yakutsk","Asia/Yakutsk"},
			{"(GMT+11:00) Solomon Is., New Caledonia","Pacific/Noumea"},
			{"(GMT+11:00) Vladivostok","Asia/Vladivostok"},
			{"(GMT+12:00) Auckland, Wellington","Pacific/Auckland"},
			{"(GMT+12:00) Coordinated Universal Time+12","Etc/GMT-12"},
			{"(GMT+12:00) Fiji","NST"},
			{"(GMT+12:00) Magadan","Asia/Magadan"},
			{"(GMT+13:00) Nuku'alofa","Pacific/Tongatapu"},
			{"(GMT+13:00) Samoa","Pacific/Enderbury"},
			};

	/** The time zone drop-down list used on the Project and
	 * Prod Admin / Production pages.  */
	private static List<SelectItem> timeZoneDL;

	public TimeZoneUtils() {
		timeZoneDL = createTimeZoneDL();
	}

	/**
	 * Generate the drop-down selection list from the static array.
	 * @return The time zone list for user selection.
	 */
	private static List<SelectItem> createTimeZoneDL() {
		List<SelectItem> list = new ArrayList<SelectItem>();
		for (int i = 0; i < pairs.length; i++) {
			list.add(new SelectItem(pairs[i][1], pairs[i][0]));
		}
		return list;
	}

	/**
	 * Gets the presentation name, which matches the Windows list, for a
	 * particular Java time zone id.
	 *
	 * @param timeZoneId The Java time zone ID string.
	 * @return The equivalent Windows display value, which we display to the end
	 *         user.
	 */
	public static String getTimeZoneName(String timeZoneId) {
		String name = "";
		for (int i = 0; i < pairs.length; i++) {
			if (timeZoneId.equals(pairs[i][1])) {
				name = pairs[i][0];
			}
		}
		return name;
	}

	/** See {@link #timeZoneDL}. Since this method is called from
	 * JSF, it cannot be static. */
	public List<SelectItem> getTimeZoneDL() {
		return timeZoneDL;
	}

	/**
	 * Get the TimeZone object that matches a given name; the case of the given
	 * name is ignored.
	 * <p>
	 * Note that we do not use TimeZone.getTimeZone(), because it has a very
	 * strange behavior regarding case. For example, getTimeZone("pst") does not
	 * match, but getTimeZone("cst") matches and returns a TimeZone object with
	 * the ID set to "cst", not "CST".
	 *
	 * @param tzStr The name of the time zone to match; if this is null, then
	 *            null is returned.
	 * @return The matching TimeZone object, with the expected/standard case in
	 *         the ID field, or null if no match is found.
	 */
	public static TimeZone getTimeZone(String tzStr) {
		if (tzStr == null) {
			return null;
		}
		TimeZone tz = null;
		for (String s : TimeZone.getAvailableIDs()) {
			if (s.equalsIgnoreCase(tzStr)) {
				tz = TimeZone.getTimeZone(s);
				break;
			}
		}
		return tz;
	}

	/**
	 * A debugging function to dump all the Java time zone
	 * id strings to the log.
	 */
	@SuppressWarnings("unused")
	private static void printTimeZones() {
		TimeZone tz;
		for (String s : TimeZone.getAvailableIDs()) {
			tz = TimeZone.getTimeZone(s);
			int n = tz.getDSTSavings();
			long off = tz.getRawOffset() / 60000;
			log.debug((n==0?"0":"1") + "\t" + off + "\t" + s );
		}
	}

}
