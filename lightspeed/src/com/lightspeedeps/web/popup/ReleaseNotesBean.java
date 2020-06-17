/**
 * File: ReleaseNotesBean.java
 */
package com.lightspeedeps.web.popup;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lightspeedeps.dto.ApplicationReleaseDTO;
import com.lightspeedeps.object.LoggedException;
import com.lightspeedeps.util.app.*;
import com.lightspeedeps.web.util.ApplicationScopeBean;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 * Backing bean for displaying release notes in popup
 *
 * LS-3097
 */
@ManagedBean
@SessionScoped
public class ReleaseNotesBean extends PopupBean {
	private static final long serialVersionUID = - 6545614184148668916L;

	private static final String RELEASE_API_URL = ApplicationUtils.getInitParameterString("RELEASE_API_URL", false);
	private static final String USER_LOGIN_COUNT_URL = "/release/user/login/count?user_name=";
	private static final String ALL_RELEASE_DETAILS_URL = "/release/all";
	public static final String LATEST_RELEASE_DETAILS_URL = "/release/latest/TTCO";

	List<ApplicationReleaseDTO> releases = null;

	public static ReleaseNotesBean getInstance() {
		return (ReleaseNotesBean)ServiceFinder.findBean("releaseNotesBean");
	}

	/** True iff user is logging in first time post release */
	private boolean showUpdateMsg;

	public ReleaseNotesBean() {
	}


	/**
	 * Action Method for the 'Updates' link on application home page. It
	 * displays all the release information in pop up
	 *
	 */
	public String actionShowReleaseNotes() {
		try {
			releases = getAllReleaseDetails(RELEASE_API_URL + ALL_RELEASE_DETAILS_URL);
			setVisible(true);
			setShowUpdateMsg(false);
		}
		catch (Exception e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
		return null;
	}

	/**
	 * Method to check whether to display release update banner on home page
	 * based on release date and user login count
	 *
	 * @param emailAddress to pass in API to get logged in count
	 */
	public void displayBannerMessage(String emailAddress) {
		try {
			String releaseDate = ApplicationScopeBean.getInstance().getLatestReleaseDate();
			int loginCount = getLoginCount(emailAddress, releaseDate);
			if (loginCount <= 1) {
				setShowUpdateMsg(true);
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
	}

	/**
	 * method to get user logged in count post release
	 *
	 * @param emailAddress user's email address, first parameter to pass in API
	 * @param date latest release date, second parameter to pass in API
	 * @return user logged in count post release
	 */
	private int getLoginCount(String emailAddress, String date) {
		int count = 0;
		try {
			String url = RELEASE_API_URL + USER_LOGIN_COUNT_URL + encodeValue(emailAddress) + "&start_time=" +
					encodeValue(date);
			ClientResponse clientResponse = getClientResponse(url);
			String response = clientResponse.getEntity(String.class);
			count = Integer.parseInt(response);
		}
		catch (URISyntaxException e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return count;
	}

	/**
	 * Method to get all the release information for a given application Release
	 * information are fetched from Release Management API
	 *
	 * @param apiUrl hostname, port information read from application config
	 *            file
	 * @return releaseDetails, a list of release information
	 */
	public List<ApplicationReleaseDTO> getAllReleaseDetails(String apiUrl) {
		try {
			ClientResponse clientResponse = null;
			clientResponse = getClientResponse(apiUrl);
			List<ApplicationReleaseDTO> releaseDetails =
					new ObjectMapper().readValue(clientResponse.getEntity(String.class),
							new TypeReference<List<ApplicationReleaseDTO>>() {
					});
			return releaseDetails;
		}
		catch (Exception e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
	}

	/**
	 * method to build URI and call release info APIs
	 *
	 * @param apiUrl
	 * @return
	 * @throws URISyntaxException
	 */
	private ClientResponse getClientResponse(String apiUrl) throws URISyntaxException {
		final URI uri = new URI(apiUrl);
		final WebResource webResource = ApiUtils.buildWebResource(uri);
		final ClientResponse clientResponse =
				webResource.type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);
		return clientResponse;
	}

	/**
	 * method to encode the parameters to pass in API url
	 *
	 * @param value
	 * @return
	 */
	private String encodeValue(String value) {
		try {
			value = URLEncoder.encode(value, "UTF-8").replaceAll("\\+", "%20");
		}
		catch (UnsupportedEncodingException e) {
			EventUtils.logError(e);
			MsgUtils.addGenericErrorMessage();
		}
		return value;
	}

	/**
	 * @return the releases
	 */
	public List<ApplicationReleaseDTO> getReleases() {
		return releases;
	}

	/**
	 * @param releases the releases to set
	 */
	public void setReleases(List<ApplicationReleaseDTO> releases) {
		this.releases = releases;
	}


	/** See {@link #showUpdateMsg}. */
	public boolean getShowUpdateMsg() {
		return showUpdateMsg;
	}

	/** See {@link #showUpdateMsg}. */
	public void setShowUpdateMsg(boolean showUpdateMsg) {
		this.showUpdateMsg = showUpdateMsg;
	}

}
