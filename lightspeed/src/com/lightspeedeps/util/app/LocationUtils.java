package com.lightspeedeps.util.app;

import java.util.*;

import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.lightspeedeps.dao.PostalLocationDAO;
import com.lightspeedeps.model.Address;
import com.lightspeedeps.model.PostalLocation;
import com.lightspeedeps.object.AddressInformation;
import com.lightspeedeps.object.LatitudeLongitude;
import com.lightspeedeps.object.LoggedException;
import com.lightspeedeps.type.EventType;
import com.lightspeedeps.util.common.CalendarUtils;
import com.lightspeedeps.util.common.StringUtils;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;

/**
 * Utility methods related to geographic locations. Includes calculating sunrise
 * and sunset times from an Adress, and validating city+state pairs.
 */
public class LocationUtils {
	private static Log LOG = LogFactory.getLog(LocationUtils.class);

	private static String CITY_LOOKUP_PATH = null;
	private static String STATE_LOOKUP_PATH = null;
	private static final String CITY_LOOKUP_PARAM_PATTERN = "country/{0}/state/{1}/city/{2}";
	private static final String STATE_LOOKUP_PARAM_PATTERN = "country/{0}/state/{1}";
	private static final String ZIPCODE_LOOKUP_PARAM_PATTERN = "country/{0}/zip/{1}";

	/** A map of (recently) tested city/state/country values which were valid.
	 * The map may be purged periodically; it is not required, just improves performance.
	 * Maps from a String which is the city/state/country URI parameter constructed for
	 * the API call, to a Boolean, which is always True.  (If the city/state is not valid,
	 * it is not added to the map. LS-3482 */
	private static Map<String,Boolean> validCity = new HashMap<>();

	/** Number of 'hits' against the city/state map -- when the requested city/state value was
	 * already in the map, and therefore an API call was avoided. */
	private static int mapHits = 0;

	/** Number of 'misses' against the city/state map -- when the requested city/state value was
	 * not in the map, and therefore an API call was required. */
	private static int mapMisses = 0;

	/** Private default constructor prevents instantiation. */
	private LocationUtils() {
	}

	/**
	 * Verify if a city/state combination is valid (a known city) for the
	 * default country (currently US). An empty city or state value will always
	 * return false. LS-1936
	 *
	 * @param city City name to be tested.
	 * @param state State containing the city; this is a state code, i.e., the
	 *            2-character code for U.S. states.
	 * @return True if the city/state exists in the applications "default"
	 *         country.
	 */
	public static boolean validateCityState(String city, String state) {
		return validateCityState(city, state, Constants.DEFAULT_COUNTRY_CODE);
	}

	/**
	 * Verify if a city/state/country combination is valid (a known city). An
	 * empty city, state, or country value will always return false. LS-1936
	 *
	 * @param city City name to be tested.
	 * @param state State containing the city; this is a state code, i.e., the
	 *            2-character code for U.S. states. If this is one of the
	 *            "special" state codes OT, FO, or HM, then validation is
	 *            skipped, and a true result is returned.
	 * @param country Country (2-letter ISO code) containing the city/state. If
	 *            empty (null or blank), then the default country is assumed
	 *            (currently US).
	 * @return True if the city/state exists, or if the validation API throws an
	 *         exception, or if the state is HM/OT/FO.
	 */
	public static boolean validateCityState(String city, String state, String country) {

		LOG.debug("");
		if (StringUtils.isEmpty(country)) { // LS-2341: treat blank country as default
			country = Constants.DEFAULT_COUNTRY_CODE;
		}

		if (StringUtils.isEmpty(city) || StringUtils.isEmpty(state)) {
			return false;
		}

		if (city.contains("/") || state.contains("/") || country.contains("/")) {
			return false;
		}

		if (state.equals(Constants.FOREIGN_FO_STATE) || state.equals(Constants.FOREIGN_OT_STATE)
				|| state.equals(Constants.TOURS_HOME_STATE)) {
			 // LS-2341: if "special" state code, allow any city name
			return true;
		}

		boolean foundCity = false;
		try {
			String cityParams = MsgUtils.formatText(CITY_LOOKUP_PARAM_PATTERN, country, state, city);
			if (validCity.containsKey(cityParams)) { // LS-3482
				// the requested city/state pair was previously validated
				mapHits++;  // keep stats
				return true;
			}
			mapMisses++; // requested item NOT in the map ... API call will be done

			if (CITY_LOOKUP_PATH == null) {
				CITY_LOOKUP_PATH = ApplicationUtils.getInitParameterString(Constants.INIT_PARAM_API_SERVER_DOMAIN) + "lookup/cities/";
			}
			String apiUrl = CITY_LOOKUP_PATH + cityParams;

			JSONObject jsonObject = null;
			try {
				jsonObject = ApiUtils.callRestApi(apiUrl);
			}
			catch (Exception e) {
				EventUtils.logError(e);
				foundCity = true; // don't fail validation due to API exceptions
			}

			if (jsonObject != null) {
				String ctoken = null;
				try {
					ctoken = jsonObject.getString("city");
				}
				catch (Exception e) {
					LOG.error(e);
				}
				LOG.debug(ctoken);
				if (! StringUtils.isEmpty(ctoken)) {
					foundCity = true;
					if (validCity.size() > 20) { // Log what's happening
						int hitRatio = (100 * mapHits) / (mapHits + mapMisses);
						String description = "city/state map: # of items mapped=" + validCity.size() + "; hit ratio=" + hitRatio + "%";
						LOG.debug(description);
						if (validCity.size() > 100) { // Don't let map take up too much space
							EventUtils.logEvent(EventType.INFO, null, null, "System", "Clearing " + description);
							validCity.clear(); // Clear the map
							mapHits = 0;
							mapMisses = 1;
						}
					}
					validCity.put(cityParams, Boolean.TRUE); // LS-3482
				}
			}
			else { // unable to get response for some reason,
				foundCity = true; // don't fail validation due to external issue.
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}

		return foundCity;
	}

	/**
	 * Method to null/empty check for city/state and call to the
	 * validateCityState method. This will issue a FacesMessage if there is an
	 * error. If both the city and state values in the Address object are empty
	 * (null, zero-length, or whitespace), then True will be returned. This
	 * allows an address that has not been filled in to be treated as valid. -
	 * LS-1843
	 *
	 * @param address Address containing the city/state to be checked; the
	 *            country value is not used.
	 * @return True if the city/state combination is determined to be valid, or
	 *         if both the city and state are null/blank; false otherwise.
	 */
	public static boolean checkCityState(Address address) {
		boolean match = true;
		if (StringUtils.isEmpty(address.getCountry()) || address.getCountry().equalsIgnoreCase("US")) {
			if ((! StringUtils.isEmpty(address.getCity())) ||
					(! StringUtils.isEmpty(address.getState()))) {
				match = validateCityState(address.getCity(), address.getState());
				if (! match) {
					MsgUtils.addFacesMessage("Address.CityStateMisMatched", FacesMessage.SEVERITY_ERROR,
							address.getCity(), address.getState());
					match = false;
				}
			}
		}
		return match;
	}

	/**
	 * Get the specified date's sunrise time at the specified address; the
	 * current code relies on the zip code to determine the location.
	 *
	 * @param addr The Address object used to find the latitude and longitude of
	 *            the location.
	 * @param date The Date on which the sunrise should be found.
	 * @return A Calendar object with the sunrise time, or null if the
	 *         address cannot be converted to a latitude/longitude position.
	 */
	public static Calendar[] getSunriseSunset(Address addr, Date date, TimeZone tz) {
		Calendar cal = CalendarUtils.getInstance(date);
		cal.setTimeZone(tz);
		LatitudeLongitude latlong = getLatLong(addr);
		Calendar result[] = new Calendar[2];
		if (latlong != null) {
			// create location
			Location location = new Location(latlong.getLatitude(), latlong.getLongitude());
			// create calculator
			SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(location, tz);
			// call the method for the type of sunrise/sunset we need
			result[0] = calculator.getOfficialSunriseCalendarForDate(cal);
			result[1] = calculator.getOfficialSunsetCalendarForDate(cal);
			//log.debug(result[0].getTime().toString() + ", " + result[1].getTime().toString());
		}
		return result;
	}

//	/**
//	 * Get the specified date's sunset time at the specified address; the
//	 * current code relies on the zip code to determine the location.
//	 *
//	 * @param addr The Address object used to find the latitude and longitude of
//	 *            the location.
//	 * @param date The Date on which the sunset should be found.
//	 * @return A Calendar object with the sunset time, or null if the
//	 *         address cannot be converted to a latitude/longitude position.
//	 */
//	public static Calendar getSunset(Address addr, Date date, TimeZone tz) {
//		Calendar cal = CalendarUtils.getInstance(date);
//		cal.setTimeZone(tz);
//		LatitudeLongitude latlong = getLatLong(addr);
//		Calendar sunset = null;
//		if (latlong != null) {
//			// create location
//			Location location = new Location(latlong.getLatitude(), latlong.getLongitude());
//			// create calculator
//			SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(location, "America/New_York");
//			// call the method for the type of sunrise/sunset we need
//			sunset = calculator.getOfficialSunsetCalendarForDate(cal);
//			log.debug(sunset);
//		}
//		return sunset;
//	}

	/**
	 * Get latitude/longitude information from the information in an
	 * Address object.
	 *
	 * @param addr The Address to look up.
	 * @return The latitude/longitude information, or null if either the Address
	 *         object did not contain a zip code, or the zip code was not found
	 *         in our table. We currently only support looking up by zip code.
	 */
	private static LatitudeLongitude getLatLong(Address addr) {
		LOG.debug(addr.getCityStateZip());
		LatitudeLongitude latlong = null;
		latlong = LocationUtils.getLatLongFromZip(addr);
		if (latlong == null) {
			latlong = LocationUtils.getLatLongFromPlace(addr);
			LOG.debug("latlong=" + latlong + " from place: " + addr.getCityStateZip());
		}
		else {
			LOG.debug("latlong=" + latlong + " from zip: " + addr.getCityStateZip());
		}
		return latlong;
	}

	/**
	 * Get latitude/longitude information from the zip code information in an
	 * Address object.
	 *
	 * @param addr The Address containing the zip code to look up.
	 * @return The latitude/longitude information, or null if either the Address
	 *         object did not contain a zip code, or the zip code was not found
	 *         in our table.
	 */
	private static LatitudeLongitude getLatLongFromZip(Address addr) {
		LatitudeLongitude latlong = null;
		String country = addr.getCountry();
		if (country == null || country.length() == 0) {
			country = Constants.DEFAULT_COUNTRY_CODE;
		}
		String zip = addr.getZip();
		if (zip == null || zip.trim().length() == 0) {
			return latlong;
		}

		final PostalLocationDAO postalLocationDAO = PostalLocationDAO.getInstance();

		List<PostalLocation> list = postalLocationDAO.findByCountryAndPostalCode(country, zip);
		if (list.size() > 0) {
			PostalLocation pl = list.get(0);
			latlong = new LatitudeLongitude(pl.getLatitude(), pl.getLongitude());
		}

		return latlong;
	}

	/**
	 * Get latitude/longitude information from the city/state/etc. information in an
	 * Address object.
	 *
	 * @param addr The Address containing the place to look up.
	 * @return null - this is not yet implemented.
	 */
	private static LatitudeLongitude getLatLongFromPlace(Address addr) {
		LatitudeLongitude latlong = null;
		// TODO add support for finding latitude & longitude from city/state/country
		return latlong;
	}
	
	/** Method returns the list of cities for the corresponding state
	 * @param state state code ex: CA for which cities needs to be fetched from the look up API
	 * @return JSONObject as API response or null if any exception occurs
	 */
	public static JSONArray getCityByState(String state) {
		LOG.debug("fetching cities by state " + state);
		JSONArray jsonArray = null;
		if (StringUtils.isEmpty(state)) {
			return null;
		}

		try {
			String cityParams = MsgUtils.formatText(STATE_LOOKUP_PARAM_PATTERN, Constants.DEFAULT_COUNTRY_CODE, state);

			if (STATE_LOOKUP_PATH == null) {
				STATE_LOOKUP_PATH = ApplicationUtils.getInitParameterString(Constants.INIT_PARAM_API_SERVER_DOMAIN)
						+ "lookup/cities/";
			}
			String apiUrl = STATE_LOOKUP_PATH + cityParams;

			try {
				jsonArray = ApiUtils.callRestApiWithJSONArray(apiUrl);
			} catch (Exception e) {
				EventUtils.logError(e);
			}
		} catch (Exception e) {
			EventUtils.logError(e);
		}
		return jsonArray;
	}
	
	/** Method returns the list of cities for the corresponding state
	 * @param state state code ex: CA for which cities needs to be fetched from the look up API
	 * @return JSONObject as API response or null if any exception occurs
	 */
	public static  List<SelectItem> getCityListBystate(JSONArray cityByState) throws JSONException {
		List<SelectItem> citiesList = new ArrayList<>();
		for (int i = 0; i < cityByState.length(); i++) {
			JSONObject cityObject = cityByState.getJSONObject(i);
			if (cityObject != null) {
				String cityName = (String) cityObject.get("city");
				if (cityName != null)
					citiesList.add(new SelectItem(cityName));
			}
		}
		return citiesList;
	}
	
	/**
	 * Method to null/empty check for zip and call to the
	 * isMyAccountZipValid method. This will return false if zip is not valid.
	 * allows an address that has not been filled in to be treated as valid.
	 *
	 * @param address Address containing the Zip to be checked; the
	 *            country value is not used.
	 * @param addressType is used for give more accuracy to user by which address Zip is not valid
	 * @return True if the zip combination is determined to be valid, or
	 * if zip is null/blank; false otherwise.
	 */
	public static boolean checkAddress(Address address, String addressType) {
		boolean match = true;
		if (StringUtils.isEmpty(address.getCountry()) ||
				address.getCountry().equalsIgnoreCase("US")) {
			if (! StringUtils.isEmpty(address.getZip()) || StringUtils.isEmpty(address.getZip())) {
				match = address.isZipValidIgnoreState();
				if (! match) {
					MsgUtils.addFacesMessage("Address.ZipMisMatched", FacesMessage.SEVERITY_ERROR,
							addressType);
					return false;
				}
			}
		}
		//LS-4717,  LS-4825 check for only street address
		if (addressType != Constants.AGENCY_ADDRESS &&
				! StringUtils.isEmpty(address.getCityStateZip()) &&
				(StringUtils.isEmpty(address.getAddrLine1()))) {
			MsgUtils.addFacesMessage("Address.StreetAddress1", FacesMessage.SEVERITY_ERROR,
					addressType);
			return false;
		}
		return true;
	}

	/** Method returns the list of zip, cities and State  for the corresponding zip
	 * @param Zip code ex: 92765 for which cities and State needs to be fetched from the look up API
	 * @return JSONObject as API response or null if any exception occurs
	 */
	public static List<AddressInformation> getCityStateByZip(String zipCode) {
		LOG.debug("fetching cities by zipCode " + zipCode);
		JSONArray jsonArray = null;
		List<AddressInformation> list = new ArrayList<>();
		if (StringUtils.isEmpty(zipCode)) {
			return null;
		}

		try {
			String cityParams = MsgUtils.formatText(ZIPCODE_LOOKUP_PARAM_PATTERN,
					Constants.DEFAULT_COUNTRY_CODE, zipCode);
			if (CITY_LOOKUP_PATH == null) {
				CITY_LOOKUP_PATH = ApplicationUtils.getInitParameterString(
						Constants.INIT_PARAM_API_SERVER_DOMAIN) + "lookup/cities/";
			}
			String apiUrl = CITY_LOOKUP_PATH + cityParams;

			try {
				jsonArray = ApiUtils.callRestApiWithJSONArray(apiUrl);
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject cityObject = jsonArray.getJSONObject(i);
					if (cityObject != null) {
						String city = (String)cityObject.get("city");
						String stateCode = (String)cityObject.get("stateCode");
						list.add(new AddressInformation(zipCode, city, stateCode, Boolean.FALSE));
					}
				}

			}
			catch (Exception e) {
				EventUtils.logError(e);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		return list;
	}

	/*
	 * setCityStateByZipCode method is used for set city and state by zip
	 * method accept list of AddressInformation and sets the values based on selected object
	 * detail will set according to addressType for user.
	 */
	public static void setCityStateByZipCode(List<AddressInformation> list, Address address) {
		for (AddressInformation addInfo : list) {
			if (addInfo.getSelected()) {
				if (address != null) {
					address.setCity(addInfo.getCity());
					address.setState(addInfo.getStateCode());
				}

			}
		}
	}
}

