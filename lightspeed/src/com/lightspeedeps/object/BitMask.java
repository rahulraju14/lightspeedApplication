/**
 * File: BitMask.java
 */
package com.lightspeedeps.object;

import java.util.BitSet;

/**
 * A class to hold a 64-bit mask, stored as a 'long'. For longer mask
 * requirements, the built-in BitSet class could be used directly, or this class
 * could be extended, depending on requirements.
 */
public class BitMask extends BitSet implements java.io.Serializable {
	/** */
	private static final long serialVersionUID = 1L;

	/** Default constructor. All mask bits are set to zero. */
	public BitMask() {
	}

	/** A "clone" constructor, which can take either a BitSet or
	 * a BitMask. */
	public BitMask(BitSet b) {
		or(b);
	}

	/** Construct a 64-bit BitMask from a long. */
	public BitMask(long mask) {
		this(BitSet.valueOf( new long[] {mask}) );
	}

	/** Set a 64-bit mask from a long. */
	public void set(long mask) {
		clear();
		or(BitSet.valueOf( new long[] {mask}) );
	}

	/** Return our 64-bit mask as a long. */
	public long getLong() {
		long[] results = toLongArray();
		if (results.length > 0) {
			return results[0];
		}
		return 0;
	}

	/**
	 * Provide a pretty-print version of the mask, which displays it in 10-bit
	 * sections.
	 *
	 * @see java.util.BitSet#toString()
	 */
	@Override
	public String toString() {
		String s = super.toString();
		s += "[";
		s += toBinaryString();
		s += "]";
		return s;
	}

	/**
	 * Convert the bit mask into a 64-character String, where each character is
	 * a '1' or '0', representing the value of the corresponding bit in the
	 * original long. The result is split into 10-bit sections for readability.
	 * Used for debugging purposes.
	 *
	 * @return The 64-character printable representation of the mask.
	 */
	public String toBinaryString() {
		String s = Long.toBinaryString(getLong());
		s = "0000000000000000000000000000000000000000000000000000000000000000".substring(0, 64-s.length()) + s;
		s = s.substring(0,4) + " " + s.substring(4,14) + " " + s.substring(14,24) + " " +
				s.substring(24,34) + " " + s.substring(34,44) + " " + s.substring(44,54)+ " " + s.substring(54,64);
		return s;
	}

}
