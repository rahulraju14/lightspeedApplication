//	File Name:	NotificationMethod.java
package com.lightspeedeps.type;

/**
 * An enumeration used in Message, indicating how the message
 * is supposed to be delivered.
 */
public enum NotificationMethod {
	/** The user can view it in the Lightspeed Message Center. */
	WEB,			// in "Message Center" on LightSpeed app
	/** Message will be sent via email. */
	EMAIL,
	/** Message will be sent via SMS. */
	TEXT_MESSAGE,
	/** Message will be sent via an instant-messaging service. */
	INSTANT_MESSAGE,
	/** Message will be converted to audio and sent via
	 * a phone call. */
	PHONE_CALL
}
