//	File Name:	FdxFilter.java
package com.lightspeedeps.importer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Queue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A class for filtering the XML file from a Final Draft (.fdx) file, which
 * frequently contains illegal UTF-8 characters, such as x'00', and "extended"
 * prime and quote characters.
 * <p>
 * This class provides significantly LESS filtering than the TaggerFilter. This
 * one only replaces the "smart quote" type characters, and leaves the rest
 * intact, to be interpreted by whatever loader is called from the SAXBuilder's
 * build method. This results in the preservation of all/most foreign character
 * sets.
 */
/* package-private */ class FdxFilter extends TaggerFilter {
	private static final Log log = LogFactory.getLog(FdxFilter.class);

	private int skip = 0;

	Queue<Integer> hold = new ArrayDeque<>();

	public FdxFilter(InputStream arg0) {
		super(arg0);
	}

	@Override
	public int read() throws IOException {
		int ch;
		if (hold.size() > 0) {
			ch = hold.remove();
		}
		else {
			ch = superRead();
		}
		while ( ch == 0 ) {	// nulls we just throw away
			log.debug("null discarded");
			ch = superRead();
		}
		if (skip == 0) {
			if (ch == 0x91 || ch == 0x92) {  // these two change to primes
				//log.debug("prime replaced");
				ch = '\'';
			}
			else if (ch == 0x93 || ch == 0x94) { // these two change to quotes
				//log.debug("quote replaced");
				ch = '"';
			}
			else if (ch == 0xe2 ) { // replace some 3-character UTF-8 unicode
				int ch2 = superRead();
				if (ch2 == 0x80) {
					int ch3 = superRead();
					switch(ch3) {
					case 0x98: /* left single quote */
					case 0x99: /* right single quote */
					case 0x9a: /* single low quote */
					case 0x9b: /* single high reverse quote */
						ch = '\'';
						break;
					case 0x9c: /* left double quote */
					case 0x9d: /* right double quote */
					case 0x9e: /* double low-9 quote */
					case 0x9f: /* double high reverse quote */
						ch = '\''; // problem returning " inside quoted string!
						break;
					default:
						skip = 2;
						hold.add(ch2);
						hold.add(ch3);
					}
				}
				else {
					skip = 1;
					hold.add(ch2);
				}
			}
			else if (ch > 0xe0 ) { // 3-character UTF-8 unicode
				skip = 2;
			}
			else if (ch >= 0xc0 ) { // 2-character UTF-8 unicode
				skip = 1;
			}
		}
		else {
			skip--;
		}

		return ch;
	}

}

