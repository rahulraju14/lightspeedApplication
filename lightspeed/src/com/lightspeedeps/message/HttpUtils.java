//	File Name:	SmsUtils.java
package com.lightspeedeps.message;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.lightspeedeps.type.PayrollStatus;
import com.lightspeedeps.util.app.ApplicationUtils;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;

/**
 * A class for HTTP utility methods.
 */
public abstract class HttpUtils {
	private static final Log log = LogFactory.getLog(HttpUtils.class);

	/** Default SFTP port if not specified in web.xml. */
	private final static int SFTP_PORT = 22;

	private final static TrustManager[] trustAllCerts;
	private final static HostnameVerifier hostVerifier;

	/** Both Read timeout and Connect timeout will be set to this number
	 * of seconds. */
	private static final int TIMEOUT_SECONDS = 15;

	static {
	    // Create a trust manager that does not validate certificate chains
	    trustAllCerts = new TrustManager[] { new X509TrustManager() {
	        @Override
	        public void checkClientTrusted( final X509Certificate[] chain, final String authType ) {
	        }
	        @Override
	        public void checkServerTrusted( final X509Certificate[] chain, final String authType ) {
	        }
	        @Override
	        public X509Certificate[] getAcceptedIssuers() {
	            return null;
	        }
	    } };

	    hostVerifier = new HostnameVerifier() {
		     @Override
			public boolean verify(String urlHostName, SSLSession session) {
		    	 // Dummy verifier to suppress validation
		         //log.debug("Warning: URL Host: " + urlHostName + " vs. " + session.getPeerHost());
		         return true;
		     }
		 };


		 HttpsURLConnection.setDefaultHostnameVerifier(hostVerifier);

	}

	private HttpUtils() {
	}

	/**
	 * Retrieve an authorization token by sending a basic authorization request
	 * to the given URL.
	 *
	 * @param url The URL to which the request should be sent.
	 * @param name The name to provide in the request.
	 * @param password The password to provide in the request.
	 * @return The response string from the service provider.
	 */
	public static String getAuthToken(String url, String name, String password) {
		String response = "";
		HttpURLConnection conn = null;
		try {
			String auth = name + ":" + password;
			String encodedStr = new String(Base64.encodeBase64(auth.getBytes()));

//			byte[] dec = Base64.decodeBase64(encodedAuth);
//			log.debug(new String(dec));

		    conn = (HttpURLConnection)new URL(url).openConnection();
		    conn.setConnectTimeout(TIMEOUT_SECONDS*1000);
		    conn.setReadTimeout(TIMEOUT_SECONDS*1000);

		    // Tell the URL connection object to use our socket factory which bypasses security checks
		    ( (HttpsURLConnection) conn ).setSSLSocketFactory( getSslSocketFactory() );

		    conn.addRequestProperty("Authorization", "Basic " + encodedStr);

		    final InputStream input = conn.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(input));
			String line;
			while ((line = rd.readLine()) != null) {
				//log.debug(line);
				response += line;
			}
			log.debug("read complete; response: " + response);
			rd.close();
		}
		catch ( final Exception e ) {
			response = null;
		    log.error("", e);
		    if (conn != null) {
				try {
					int rc = conn.getResponseCode();
					String responseMsg = conn.getResponseMessage();
					log.debug("rc=" + rc + ", msg=" + responseMsg);
				}
				catch (Exception e1) {
					// ignore any errors during recovery
				}
		    }
		}
		finally {
			try {
				if (conn != null) {
					conn.disconnect();
				}
			}
			catch (Exception e) {
				// ignore disconnect errors
			}
		}

		if (response != null && response.length() > 0) {
			if (response.startsWith("\"")) {
				response = response.substring(1);
			}
			if (response.endsWith("\"")) {
				response = response.substring(0, response.length()-1);
			}
		}

		return response;
	}

	/**
	 * Send a set of timecards to the given URL, accompanied by the given
	 * authorization token using HTTP(s) POST.
	 *
	 * @param url The URL to which the timecards will be sent.
	 * @param authToken The authorization token which will be included as a
	 *            request parameter.
	 * @param body The timecards, normally a JSON string.
	 * @return The text response received from the server in response to our
	 *         POST request.
	 */
	public static String sendTimecards(String url, String authToken, String body) {
		//log.debug("URL=" + url);
		//log.debug("body=" + body);
		String responseMsg = null;
		HttpURLConnection conn = null;
		try {
			conn = writeToUrl(url, authToken, body);
			int rc = conn.getResponseCode();
			responseMsg = conn.getResponseMessage();
			if (rc == 200) {
				log.debug("rc=" + rc + ", msg=" + responseMsg);
				responseMsg = null;
			}
			else {
				log.error("rc=" + rc + ", msg=" + responseMsg);
				log.error("URL=" + url);
				log.error("body=" + body);
			}
		}
		catch (Exception e) {
			log.error("", e);
			if (conn != null) {
				Map<String, List<String>> parms = conn.getRequestProperties();
				log.debug(formatParams(parms));
			}
		}
		finally {
			try {
				if (conn != null) {
					conn.disconnect();
				}
			}
			catch (Exception e) {
				// ignore disconnect errors
			}
		}
		return responseMsg;
	}

	/**
	 * Send a set of timecards to the given URL, accompanied by the given
	 * authorization token using HTTP(s) POST.
	 *
	 * @param url The URL to which the timecards will be sent.
	 * @param authToken The authorization token which will be included as a
	 *            request parameter.
	 * @return The text response received from the server in response to our
	 *         POST request.
	 */
	public static PayrollStatus sendBatchQuery(String url, String authToken) {
		log.debug("URL=" + url);
		String responseMsg = null;
		HttpURLConnection conn = null;
		PayrollStatus status = null;
		try {
			conn = writeToUrl(url, authToken, "");
			int rc = conn.getResponseCode();
			responseMsg = conn.getResponseMessage();
			log.debug("rc=" + rc + ", msg=" + responseMsg);
			if (rc == 200) {
				int stat = -1;
				int ix = responseMsg.indexOf("IP");
				if (ix >= 0) {
					String s = responseMsg.substring(ix+2, ix+3);
					try {
						stat = Integer.parseInt(s);
					}
					catch (Exception e) {
					}
				}
				for (PayrollStatus st : PayrollStatus.values()) {
					if (st.getPayCode() == stat) {
						status = st;
						break;
					}
				}
				if (status == null) {
					EventUtils.logError("Unrecognized Batch Status response: " + responseMsg);
				}
			}
			else if (rc == 401) { // authorization error
				log.error("rc=" + rc + ", msg=" + responseMsg);
				status = PayrollStatus.UNAVAILABLE;
			}
			else if (rc == 500) {
				log.debug("rc=" + rc + ", msg=" + responseMsg);
				String rcStr = responseMsg.substring(0, 2);
				int msgrc = -1;
				try {
					msgrc = Integer.parseInt(rcStr);
				}
				catch (NumberFormatException e) {
					EventUtils.logError(e);
				}
				if (msgrc == 23) {
					status = PayrollStatus.UNKNOWN;
				}
				else {
					EventUtils.logError("Unrecognized Batch Status response: " + responseMsg + " (rc=500)");
				}
			}
			else {
				EventUtils.logError("Unexpected Batch Status return code: " + rc + ", msg: " + responseMsg);
				status = PayrollStatus.UNAVAILABLE;
			}
		}
		catch (Exception e) {
			log.error("", e);
			if (conn != null) {
				Map<String, List<String>> parms = conn.getRequestProperties();
				log.debug(formatParams(parms));
			}
		}
		finally {
			try {
				if (conn != null) {
					conn.disconnect();
				}
			}
			catch (Exception e) {
				// ignore disconnect errors
			}
		}
		return status;
	}

	/**
	 * Transfer a file using SFTP to a specified domain and directory using port
	 * 22, or the specified port, or the port in the application's web.xml file.
	 *
	 * @param file The File to be transferred.
	 * @param filename The filename that the file will be stored as on the
	 *            receiving system.
	 * @param domain The server domain name, e.g., "www.somedomain.com".
	 * @param port The port to use for the FTP connection; if null, check the
	 *            application initialization parameter; if that's also null, use
	 *            the default (22).
	 * @param directory The directory where the file will be stored, e.g.,
	 *            "/home/mystuff".
	 * @param userName The user name used to login to the SFTP server.
	 * @param password The password used to login to the server.
	 * @return True iff the file was successfully stored on the server.
	 */
	public static boolean sendFile(File file, String filename, String domain, Integer port, String directory, String userName,
			String password) {
		boolean bRet = false;

		log.debug("file: " + directory + "/" + filename + " (" + file.getName() + ")" );

		Session session = null;
		Channel channel = null;
		ChannelSftp channelSftp = null;

		try {
			if (port == null) {
				port = ApplicationUtils.getInitParameterInt(Constants.INIT_PARAM_SFTP_PORT);
			}
			if (port <= 0) {
				port = SFTP_PORT; // use default
			}
			log.debug("domain or IP=" + domain + ", port=" + port);
			// Create the JSch object with appropriate properties...
			JSch jsch = new JSch();
			session = jsch.getSession(userName, domain, port);
			session.setPassword(password);

			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);

			// Open the connection and get an SFTP channel...
			session.connect(); // this will fail if domain, username, or password is invalid

			channel = session.openChannel("sftp");
			channel.connect();
			channelSftp = (ChannelSftp)channel;

			// Try to create the directory; will fail if already exists - that's ok.
			try {
				channelSftp.mkdir(directory);
			}
			catch (SftpException e) {
				// ignore error
				log.debug("mkdir failed; directory may already exist; error: " + e.getLocalizedMessage());
			}

			// Switch to the specified directory
			try {
				channelSftp.cd(directory);	// this will fail if the directory doesn't exist
				bRet = true;
			}
			catch (SftpException e) {
				EventUtils.logError("SFTP directory error: ", e);
			}

			if (bRet) {
				bRet = false;
				// Since trying to replace a file fails with "permission denied", we
				// first try to delete the target file, ignoring any error:
				try {
					channelSftp.rm(filename);
				}
				catch (SftpException e) {
					// ignore error
					//log.debug("rm failed");
				}
				// Send the file:
				channelSftp.put(new FileInputStream(file), filename, ChannelSftp.OVERWRITE);
				log.debug("File sent successfully: " + filename + " (" + file.getName() + ")");
				bRet = true; // we made it without throwing aceptions./*-*/
			}
		}
		catch (JSchException e) {
			EventUtils.logError("SFTP connection error: ", e);
		}
		catch (SftpException e) {
			EventUtils.logError("SFTP send error: ", e);
		}
		catch (FileNotFoundException e) {
			EventUtils.logError("SFTP -- file to be sent (" + filename + ") not found: ", e);
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		finally {
			if (channel != null && channel.isConnected()) {
				try {
					channel.disconnect();
				}
				catch (Exception e) {
					log.debug(e.getLocalizedMessage());
					// ignore disconnect errors
				}
			}
			if (session != null && session.isConnected()) {
				try {
					session.disconnect();
				}
				catch (Exception e) {
					log.debug(e.getLocalizedMessage());
					// ignore disconnect errors
				}
			}
		}

		return bRet;
	}

	/**
	 * POST some text to a given URL, including the supplied authorization
	 * token.
	 *
	 * @param url The URL to receive the text.
	 * @param authToken The Security Token (authorization) to be included in the
	 *            header of the POST.
	 * @param body The String of text which will be the body of the POST.
	 * @return The HttpURLConnection that was established to send the data. The
	 *         caller should check the response code and message to see if the
	 *         send was successful.
	 */
	private static HttpURLConnection writeToUrl(String url, String authToken, String body) {
		HttpURLConnection conn = null;
		try {
			URL wsURL;
			if (url.indexOf("https:") > -1) {
				wsURL = new URL(null, url, new sun.net.www.protocol.https.Handler());
				conn = (HttpURLConnection)wsURL.openConnection();
				// Tell the URL connection object to use our socket factory which bypasses security checks
			    ((HttpsURLConnection)conn).setSSLSocketFactory( getSslSocketFactory() );
			}
			else {
				wsURL = new URL(url);
				conn = (HttpURLConnection)wsURL.openConnection();
			}
			log.debug("conn=" + conn);

		    conn.setConnectTimeout(TIMEOUT_SECONDS*1000);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
			conn.addRequestProperty("Security-Token", authToken);

			conn.setDoOutput(true);
			OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
			wr.write(body);
			wr.flush();
			wr.close();
			log.debug("write complete");
		}
		catch (Exception e) {
			log.error("", e);
			if (conn != null) {
				Map<String, List<String>> parms = conn.getRequestProperties();
				log.debug(formatParams(parms));
			}
		}
		return conn;
	}

	private static SSLSocketFactory getSslSocketFactory() {

	    // Create an SSL socket factory with our all-trusting manager
		SSLSocketFactory sslSocketFactory = null;
		try {
			// Install the all-trusting trust manager
			final SSLContext sslContext = SSLContext.getInstance( "SSL" );
			sslContext.init( null, trustAllCerts, new java.security.SecureRandom() );
			sslSocketFactory = sslContext.getSocketFactory();
		}
		catch (KeyManagementException e) {
			log.error(e);
		}
		catch (NoSuchAlgorithmException e) {
			//EventUtils.logError(e);
			log.error(e);
		}

	    return sslSocketFactory;
	}

	/**
	 * Created a printable representation of all the parameters passed as part
	 * of the given request.
	 *
	 * @param request The HttpServletRequest whose parameters are to be
	 *            formatted.
	 * @return A non-null String containing the names and values of all the
	 *         parameters in the given request, in ascending alphabetical order
	 *         of the parameter names.
	 */
	public static String formatParams(HttpServletRequest request) {
		String msg = "";
		Map<String, String[]> parms = request.getParameterMap();
		List<String> keys = new ArrayList<>(parms.keySet());
		Collections.sort(keys);
		for (String s : keys) {
			msg += "param: " + s + ", value(s): ";
			String[] values = parms.get(s);
			for (String v : values) {
				msg += v + "; ";
			}
			msg += Constants.NEW_LINE;
		}
		return msg;
	}


	/**
	 * Created a printable representation of all the parameters in the given
	 * Map.
	 *
	 * @param parms A Map< String, List< String >> which may be null or empty.
	 * @return A non-null String containing the names and values of all the
	 *         parameters, in ascending alphabetical order of the parameter
	 *         names.
	 */
	private static String formatParams(Map<String, List<String>> parms) {
		String msg = "";
		if (parms == null) {
			return "null";
		}
		List<String> keys = new ArrayList<>(parms.keySet());
		Collections.sort(keys);
		for (String s : keys) {
			msg += "param: " + s + ", value(s): ";
			List<String> values = parms.get(s);
			for (String v : values) {
				msg += v + "; ";
			}
			msg += Constants.NEW_LINE;
		}
		return msg;
	}

}
