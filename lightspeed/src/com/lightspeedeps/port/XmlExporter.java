/**
 * File: Exporter.java
 */
package com.lightspeedeps.port;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Stack;

import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.app.EventUtils;

/**
 * A class used to generate an export file of "human readable" fields, in
 * XML format.  In particular, the open() and close() methods provide
 * for nested fields and automatic generation of the XML tags.
 */
public class XmlExporter extends StringExporter implements TaggedExporter {

	/** Controls the generation of line breaks.  When true, line breaks are
	 * added after opening and closing tags. */
	private final static boolean pretty = true;

	/** A (LIFO) Stack used to keep track of the open tags. */
	private final Stack<String> stack = new Stack<String>();

	@SuppressWarnings("unused")
	private XmlExporter() {
		// hide the default constructor.
	}

	/**
	 * Construct a new XmlExporter which will output data to the stream given.
	 *
	 * @param stream
	 */
	public XmlExporter(OutputStream stream) {
		super(stream);
	}

	/**
	 * @see com.lightspeedeps.port.Exporter#next()
	 */
	@Override
	public void next() {
	}

	/**
	 * @see com.lightspeedeps.port.TaggedExporter#open(java.lang.String)
	 */
	@Override
	public void open(String name) {
		startTag(name);
		stack.push(name);
	}

	/**
	 * @see com.lightspeedeps.port.TaggedExporter#close()
	 */
	@Override
	public void close() {
		endTag(stack.pop());
	}

	/**
	 * @see com.lightspeedeps.port.Exporter#append(java.lang.String)
	 */
	@Override
	public void append(String s) {
		try {
			if (s != null) {
				getOut().write(s.getBytes());
			}
		}
		catch (IOException e) {
			EventUtils.logError(e);
		}
	}

	/**
	 * @see com.lightspeedeps.port.TaggedExporter#append(java.lang.String, java.lang.Object)
	 */
	@Override
	public void append(String name, Object value) {
		//startTag(name);
		append("<" + name + ">"); // avoid newline
		append(value);
		endTag(name);
	}

	@Override
	public void startTag(String name) {
		append("<" + name + ">");
		if (pretty) {
			try {
				getOut().write(Constants.NEW_LINE.getBytes());
			}
			catch (IOException e) {
			}
		}
	}

	@Override
	public void endTag(String name) {
		append("</" + name + ">");
		if (pretty) {
			try {
				getOut().write(Constants.NEW_LINE.getBytes());
			}
			catch (IOException e) {
			}
		}
	}

	/**
	 * Output an Object which is one of the Java built-in language
	 * types: String, BigDecimal, Integer, Short, Byte, Boolean, or Date.
	 */
	private void append(Object value) {
		if (value == null) {
			append("");
		}
		else if (value instanceof String) {
			append((String)value);
		}
		else if (value instanceof BigDecimal) {
			append((BigDecimal)value);
		}
		else if (value instanceof Integer) {
			append((Integer)value);
		}
		else if (value instanceof Short) {
			append((Short)value);
		}
		else if (value instanceof Byte) {
			append((Byte)value);
		}
		else if (value instanceof Boolean) {
			append((Boolean)value);
		}
		else if (value instanceof Date) {
			append((Date)value);
		}
		else {
			throw new IllegalArgumentException(value.toString());
		}
	}

	/* (non-Javadoc)
	 * @see com.lightspeedeps.port.TaggedExporter#openWithAttribute(java.lang.String)
	 */
	@Override
	public void openWithAttribute(String name) {
		startTagWithAttribute(name);
		stack.push(name);
	}

	/* (non-Javadoc)
	 * @see com.lightspeedeps.port.TaggedExporter#addAttribute(java.lang.String, java.lang.String)
	 */
	@Override
	public void addAttribute(String attributeName, String attributeValue) {
		append(" " + attributeName + "=" + "\""+ attributeValue + "\"");
	}

	/* (non-Javadoc)
	 * @see com.lightspeedeps.port.TaggedExporter#startTagWithAttribute(java.lang.String)
	 */
	@Override
	public void startTagWithAttribute(String name) {
		append("<" + name);
	}
}
