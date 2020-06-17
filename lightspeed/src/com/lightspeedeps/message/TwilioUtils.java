/**
 * File: TwilioUtils.java
 */
package com.lightspeedeps.message;

import static com.lightspeedeps.message.SmsUtils.*;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.util.app.EventUtils;
import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.SmsFactory;
import com.twilio.sdk.resource.instance.Sms;

/**
 *
 */
public class TwilioUtils {
	private static final Log log = LogFactory.getLog(TwilioUtils.class);

	/* Twilio REST API version */
	private static final String ACCOUNTSID = "ACda9d32071e6043cdabd9eea56b01fb70"; // twilio@lseps
	private static final String AUTHTOKEN = "35b6d4505f23adb75cca24f2e1c58a19"; // twilio@lseps
	private static final String FROM_PHONE = "13238004455"; // Lightspeed SMS number

	//private static final String ACCOUNTSID = "AC8473b11d0b7846b7acdc1b3ed9d988ff"; // dh trial
	//private static final String AUTHTOKEN = "ee93fc83400d62042ce0ea9e639ad250"; // dh trial
	//private static final String FROM_PHONE = "415-599-2671"; // Twilio sandbox number

	public static int send(String text, String phone) {
		int code = RC_MESSAGE_SENT_OK;

		try {
		    /* Instantiate a new Twilio Rest Client */
			TwilioRestClient client = new TwilioRestClient(ACCOUNTSID, AUTHTOKEN);
			// call factory class
			SmsFactory smsFactory = client.getAccount().getSmsFactory();

			// build Map of message parameters
			Map<String, String> params = new HashMap<String, String>();
			params.put("From", FROM_PHONE);
			params.put("To", phone);
			params.put("Body", text);

			try {
				// send the SMS message
				Sms sms = smsFactory.create(params);
				String status = "FAILED";
				String sid = "FAILED";
				try {
					status = sms.getStatus();
					sid = sms.getSid();
				}
				catch (Exception e) {
					// getStatus or getSid may fail if the SMS did not get queued
					log.error("getStatus/getSid failed: ", e);
					code = RC_MESSAGE_NOT_SENT_POST_ERROR;
					try {
						Object o = sms.getObject("status");
						if (o != null) log.debug(o.getClass() +", " + o.toString());
					}
					catch (Exception e2) {
						log.error("getObject failed: ", e2);
					}
				}
				log.debug("Sent SMS, sid=" + sid + ", status=" + status);
			}
			catch (TwilioRestException e) {
				int rc = e.getErrorCode();
				log.error("Twilio error code: " + rc + ", exception: ", e);
				code = RC_MESSAGE_NOT_SENT_POST_ERROR;
			}
		}
		catch (ClassCircularityError e) {
			// This was thrown at one point; possibly due to a mismatch of twilio jar
			// level with supporting library jars
			EventUtils.logError(e);
			code = RC_INTERNAL_ERROR;
		}
		catch (Exception e) {
			EventUtils.logError(e);
			code = RC_INTERNAL_ERROR;
		}
		return code;
	}

}
