/**
 * BigDecimalZeroSerializer.java
 */
package com.lightspeedeps.util.payroll;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.lightspeedeps.model.PayJob;

/**
 * This class is used to override the normal JSON serialization of PayJob
 * instances in situations where we want to NOT serialize that information. In
 * particular, it is used when a production wants to transfer timecard
 * information to a payroll service without including the "job table" data.
 */
public class SkipPayJobSerializer extends StdSerializer<PayJob> {
	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(SkipPayJobSerializer.class);

	/**
	 * default constructor
	 */
	public SkipPayJobSerializer() {
		super(PayJob.class);
	}

	/**
	 * @see com.fasterxml.jackson.databind.ser.std.StdSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
	 */
	@Override
	public void serialize(PayJob value, JsonGenerator jgen, SerializerProvider provider)
			throws IOException, JsonGenerationException {
		// output nothing - the point of this serializer is to skip PayJob output
	}

}
