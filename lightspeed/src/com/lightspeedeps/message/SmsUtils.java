//	File Name:	SmsUtils.java
package com.lightspeedeps.message;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A class for SMS (text message) utility methods.
 */
public abstract class SmsUtils {
	private static final Log log = LogFactory.getLog(SmsUtils.class);

	// Note: Using EventUtils for logging ran into "findBean resolution failed for ProductionDAO"

	public final static int RC_MESSAGE_NOT_SENT_EMPTY = -1;
	public final static int RC_MESSAGE_NOT_SENT_TOO_LONG = -2;
	public final static int RC_MESSAGE_NOT_SENT_INVALID_PHONE_NUMBER = -3;
	public final static int RC_MESSAGE_NOT_SENT_POST_ERROR = -4;
	public final static int RC_INTERNAL_ERROR = -5;
	public final static int RC_MESSAGE_SENT_OK = 0;

	private final static int MAX_SMS_MESSAGE_LENGTH = 160;

	private SmsUtils() {
	}

	/**
	 * Send the given text as an SMS message to the specified phone number.
	 *
	 * @param text The text to send; the maximum length for an SMS message is
	 *            160 characters. If the text is longer than 160, the send will
	 *            not be attempted, and a negative return code will be returned.
	 * @param phone The phone number; format is flexible, recommended is 11
	 *            digits (leading "1" for US numbers, without any punctuation.
	 *            (Punctuation, if present, is normally stripped by service
	 *            provider.) If 'phone' is less than 10 characters, the send
	 *            will not be attempted, and a negative return code will be
	 *            returned.
	 * @return A return code:
	 *         <ul>
	 *         <li>< 0: Code did not attempt to send the message due to invalid
	 *         data
	 *         <li>0 : Message was sent and a normal response was received.
	 *         <li>> 0: Message was sent and this response result was received.
	 *         UWN typically sets a code of "10001" for any error.
	 */
	public static int sendMsg(String text, String phone) {
		log.debug("Sending `" + text + "` to: " + phone);
		int code = RC_INTERNAL_ERROR;
		try {
			if (phone == null || phone.length() < 10) {
				log.error("phone number null or too short; #=`" + phone + "`");
				return RC_MESSAGE_NOT_SENT_INVALID_PHONE_NUMBER;
			}
			if (text == null || text.trim().length() == 0 ) {
				//EventUtils.logEvent(EventType.DATA_ERROR, "NULL or empty SMS message");
				log.error("NULL or empty SMS message");
				return RC_MESSAGE_NOT_SENT_EMPTY;
			}

			text = text.trim();
			if ( text.length() > MAX_SMS_MESSAGE_LENGTH) {
				//EventUtils.logEvent(EventType.DATA_ERROR, "Invalid SMS message, length=" + text.length());
				log.error("Invalid SMS message, longer than " + MAX_SMS_MESSAGE_LENGTH + ", length=" + text.length());
				//text = text.substring(0, MAX_SMS_MESSAGE_LENGTH);
				return RC_MESSAGE_NOT_SENT_TOO_LONG;
			}

			if ((phone.length() == 10 && phone.substring(3, 6).equals("555")) ||
					(phone.length() == 12 && phone.substring(4, 7).equals("555"))) {
				log.warn("SMS SKIPPED (555 phone#) to: " + phone + ", text=" + text);
				System.out.println("SMS SKIPPED (555 phone#) to: " + phone);
				return 0;
			}
//			if (false) {
//				log.warn("FUNCTION DISABLED TEMPORARILY: SMS Send to: " + phone + ", text=" + text);
//				System.out.println("FUNCTION DISABLED TEMPORARILY: SMS Send to: " + phone);
//				return 0;
//			}

			//code = UnwiredNationUtils.send(text, phone);
			code = TwilioUtils.send(text, phone);

		}
		catch (Exception e) {
			log.error(e);
		}

		return code;
	}

	/**
	 * Send the given text as an SMS message to the specified phone number.
	 * @param text The text to send; the maximum length for an SMS message is 160 characters.
	 * @param phone The phone number; format is flexible, recommended is 10 digits (leading "1"
	 * for US numbers, without any punctuation. (Punctuation, if present, is normally stripped by
	 * service provider.)
	 * @return The response string from the service provider, as an XML string.
	 */
/*	public static String sendMsgB(String text, String phone) {
		String response = "";
		String xml = SMS_XML_A + phone + SMS_XML_B + text + SMS_XML_C;

		try {
			// Send data
			msgCount++;
			URL url = new URL(SMS_URL);
			URLConnection conn = url.openConnection();
			conn.setDoOutput(true);
			OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
			wr.write(xml);
			log.debug("sending: "+xml);
			wr.flush();
			wr.close();
			log.debug("write complete");
			// Get the response
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			while ((line = rd.readLine()) != null) {
				response += line;
			}
			rd.close();
			log.debug("read complete");
		}
		catch (Exception e) {
			log.error(e);
		}

		log.debug("response: "+response);

		String statusMessage = response.substring(response.toString().toLowerCase().indexOf(statusMsgStart.toLowerCase())+statusMsgStart.length(),
				response.toString().toLowerCase().indexOf(statusMsgEnd.toLowerCase()));

		String resultCode = response.substring(response.toString().toLowerCase().indexOf(resultCodeStart.toLowerCase())+resultCodeStart.length(),
				response.toString().toLowerCase().indexOf(resultCodeEnd.toLowerCase()));

		log.debug("statusMessage === " + statusMessage + ", resultCode=" + resultCode);
//		if ((!statusMessage.equalsIgnoreCase("success") || !resultCode.equalsIgnoreCase("10000")) && msgCount < 3) {
//			log.debug("Resending Message");
//			log.info("Resending Message");
//			sendMsg(text, phone);
//			msgCount++;
//		}
		return response;
	}
*/

/*	public String listen(ServletRequest request) throws IOException {
		String response = "";
		ServletInputStream instream = request.getInputStream();
		byte[] tmpbuffer = new byte[512];
		int length = 0;
		String inputLine = null;
		while (true) {
			length = instream.readLine(tmpbuffer, 0, tmpbuffer.length);
			if (length < 0) {
				break;
			}
			inputLine = new String(tmpbuffer, 0, length);
			if (inputLine != null && inputLine.length() > 0) {
				log.debug("input=" + inputLine);
				response += inputLine;
			}
		}
		return response;
	}
*/
}
