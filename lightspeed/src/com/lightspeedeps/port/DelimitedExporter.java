/**
 * DelimitedExporter.java
 */
package com.lightspeedeps.port;

/**
 * An interface that extends the Exporter interface to include those methods
 * needed to output a "delimited" data file, typically either a comma-delimited
 * or tab-delimited file.
 */
public interface DelimitedExporter extends Exporter {

	/**
	 * @return the delimiter in use for separating individual data fields.
	 */
	public byte getDelimiter();

	/**
	 * Set the delimiter property.
	 *
	 * @param delimiter the delimiter to be used for separating individual data
	 *            fields.
	 */
	public void setDelimiter(byte delimiter);

}
