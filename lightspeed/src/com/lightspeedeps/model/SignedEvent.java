package com.lightspeedeps.model;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.port.Exporter;

/**
 * SignedEvent entity. Each instance represents a time-stamped, and possibly
 * signed, event related to another object. It enhances {@link TimedEvent} by
 * adding a universally-unique Id (UUID). This class is currently extended by
 * {@link ContactDocEvent} for on-boarding document events and by
 * {@link TimecardEvent} for WeeklyTimecard events.
 */
@SuppressWarnings("rawtypes")
@MappedSuperclass
public abstract class SignedEvent<T extends PersistentObject> extends TimedEvent<T> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(SignedEvent.class);

	private static final long serialVersionUID = 1L;

	// Fields

	/** The  universally unique identifier associated with this event. This
	 * field is automatically kept in sync with the transient field uuid. */
	private byte[] uuidBytes;

	@Transient
	/** The  universally unique identifier associated with this event. This
	 * transient field is automatically kept in sync with the database field
	 * uuidBytes. */
	private UUID uuid;

	// Constructors

	/** default constructor */
	public SignedEvent() {
	}

	/** minimal constructor */
	public SignedEvent(Date date) {
		super(date);
	}

	/** See {@link #uuidBytes}. */
	@Column(name = "Uuid_Bytes", length = 16)
	public byte[] getUuidBytes() {
		return uuidBytes;
	}
	/** See {@link #uuidBytes}. */
	public void setUuidBytes(byte[] bytes) {
		uuidBytes = bytes;
		uuid = null;
//		if (getUuid() != null) {
//			log.debug(uuid.getLeastSignificantBits() + " " + uuid.getMostSignificantBits() + " " + uuid.toString());
//		}
	}

	/** See {@link #uuid}. */
	@Transient
	public UUID getUuid() {
		if (uuid == null && getUuidBytes() != null) {
			ByteBuffer bb = ByteBuffer.wrap(getUuidBytes());
			long lMsb = bb.getLong();
			long lLsb = bb.getLong();
			uuid = new UUID(lMsb, lLsb);
		}
		return uuid;
	}
	/** See {@link #uuid}. */
	public void setUuid(UUID id) {
		uuid = id;
		ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
		bb.putLong(uuid.getMostSignificantBits());
		bb.putLong(uuid.getLeastSignificantBits());
		uuidBytes = bb.array();
	}

	@Transient
	public List<String> getSignature() {
		List<String> signatureDetails = new ArrayList<>(3);
		signatureDetails.add(getDisplay());
		String line2 = "Electronically Signed by: " + getName() + " "
				+ getDisplayTime();
		signatureDetails.add(line2);
		signatureDetails.add(getUuid().toString());
		return signatureDetails;
	}

	@Transient
	public List<String> get2LineSignature() {
		List<String> signatureDetails = new ArrayList<>(2);
		String line = "Signed by: " + getName() + " "
				+ getDisplayTime();
		signatureDetails.add(line);
		signatureDetails.add(getUuid().toString());
		return signatureDetails;
	}

	/**
	 * Export data in this instance via an Exporter. This is currently used to
	 * turn this object into a flat record. This is used for the document
	 * transfer function.
	 *
	 * @param ex The Exporter to which each field should be passed.
	 */
	public void exportFlat(Exporter ex) {
		ex.appendDateTime(getDate());
		if (getType() != null) {
			ex.append(getType().name());
		}
		else {
			ex.append("");
		}
		ex.append(getLastName());
		ex.append(getFirstName());
	}

	/**
	 * Export data in this instance via an Exporter. This is currently used to
	 * turn this object into a flat record. The timecard data can then be loaded
	 * into other products, such as Crew Cards.
	 *
	 * @param ex The Exporter to which each field should be passed.
	 */
	public void flatten(Exporter ex) {
//		ex.append(getId());
//		ex.appendDateTime(getDate());
		ex.appendDateTime(null);	// do not export time-stamps
		if (getType() != null) {
			ex.append(getType().name());
		}
		else {
			ex.append("");
		}
		ex.append(getLastName());
		ex.append(getFirstName());
//		if (getUuid() != null) {
//			ex.append(getUuid().toString());
//		}
//		else {
			ex.append(""); // do not export UUIDs
//		}
	}

}
