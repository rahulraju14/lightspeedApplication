/**
 * TaggedExporter.java
 */
package com.lightspeedeps.port;

/**
 * An interface that extends the Exporter interface to include those methods
 * needed to output a "tagged" data file, typically XML.
 */
public interface TaggedExporter extends Exporter {

	/**
	 * Add a value with tag 'name' to the current output record. For delimited
	 * files, the field will be preceded by this Exporter's delimiter character,
	 * unless this is the first field of a record.
	 *
	 * @param name The name of the field to be output.
	 * @param value The value of the field to be output.
	 */
	public void append(String name, Object value);

	/**
	 * Start an XML tag. This outputs the opening tag, and saves
	 * the tag name for use by the matching {@link #close()} call.
	 *
	 * @param name The tag name to generate.
	 */
	public void open(String name);

	/**
	 * Start an XML tag. This outputs the opening of a tag which needs to be
	 * closed explicitly after adding any no. of attributes to it and saves
	 * the tag name for use by the matching {@link #close()} call.
	 *
	 * @param name The tag name to generate.
	 */
	public void openWithAttribute(String name);

	/**
	 * Add an attribute with name 'attributeName' and value 'attributeValue' in a tag.
	 *
	 * @param attributeName The name of the attribute to be output.
	 * @param attributeValue The value of the attribute to be output.
	 */
	public void addAttribute(String attributeName, String attributeValue);

	/**
	 * Close the last-opened tag which has not yet been closed.
	 * The {@link #open(String)} and close() methods may be
	 * used in (nested) pairs to easily manage nested XML fields.
	 */
	public void close();

	/**
	 * Output an opening XML tag.
	 * @param name The tag name to output.
	 */
	public void startTag(String name);

	/**
	 * Output an opening XML tag that can accept attributes.
	 * @param name The tag name to output.
	 */
	public void startTagWithAttribute(String name);

	/**
	 * Output a closing XML tag.
	 * @param name The tag name to output.
	 */
	public void endTag(String name);

}
