//	File Name:	TaggerFilter.java
package com.lightspeedeps.importer;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A class originally used for filtering the XML file from a Final Draft Tagger
 * export, which frequently contains illegal UTF-8 characters, such as x'00',
 * and "extended" prime and quote characters.
 * <p>
 * The "smart quotes" characters (both single-byte and 2-byte versions) are
 * replaced with equivalent "dumb quote" symbols. In addition, most 2-byte UTF
 * foreign symbols are replaced with the (more-or-less equivalent) English
 * characters.
 * <p>
 * At some point, this seemed to be necessary to successfully import the Tagger
 * export XML files. However, testing for rev 2.2.4891 seemed to indicate this
 * was NOT necessary, so Tagger files now use the more limited filtering of the
 * FdxFilter.
 */
/* package-private */ class TaggerFilter extends FilterInputStream {
	private static final Log log = LogFactory.getLog(TaggerFilter.class);

	public TaggerFilter(InputStream arg0) {
		super(arg0);
	}

	/**
	 * This method allows our subclasses to read directly from the stream, bypassing our read() method.
	 * @see java.io.FilterInputStream#read()
	 * @throws IOException
	 */
	protected int superRead() throws IOException {
		return super.read();
	}

	/**
	 * Overrides the normal read() method to perform filtering/replacement of
	 * non-English characters.
	 *
	 * @see java.io.FilterInputStream#read()
	 */
	@Override
	public int read() throws IOException {
		int ch = super.read();
		while ( ch == 0 ) {	// nulls we just throw away
			log.debug("null discarded");
			ch = super.read();
		}
		if (ch == 0x91 || ch == 0x92) {  // these two change to primes
			//log.debug("prime replaced");
			ch = '\'';
		}
		else if (ch == 0x93 || ch == 0x94) { // these two change to quotes
			//log.debug("quote replaced");
			ch = '"';
		}
		// Added a bunch of cases for UTF characters, for "fdx" import,
		// then found that we could just run the load WITHOUT any filter at all.
		else if (ch >= 0xe0 ) { // 3-character UTF-8 unicode
			// e28098 - left single quote mark
			// e28099 - right single quote mark
			// e2809e - double low-9 quote mark
			int ch1 = ch;
			int ch2 = super.read();
			int ch3 = super.read();
			ch = '?';
			if (ch1 == 0xe2) {
				if (ch2 == 0x80) {
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
					}
				}
			}
		}
		else if (ch >= 0xc0 ) { // 2-character UTF-8 unicode
			int ch1 = ch;
			int ch2 = super.read();
			ch = '?';
			if (ch1 == 0xc2) {
				switch(ch2) {
				case 0x8f: /* 'single shift 3' */
					ch = '#';
					break;
				case 0xba: /* +/- */
					ch = '+';
					break;
				}
			}
			else if (ch1 == 0xc3) {
				switch(ch2) {
				case 0x80: /* A grave */
				case 0x81: /* A acute */
				case 0x82: /* A circumflex */
				case 0x83: /* A tilde */
				case 0x84: /* A umlaut/diaeresis (double-dot) */
				case 0x85: /* A ring */
				case 0x86: /* AE */
					ch = 'A';
					break;
				case 0x87: /* C cedilla */
					ch = 'C';
					break;
				case 0x88: /* E grave */
				case 0x89: /* E acute */
				case 0x8A: /* E circumflex */
				case 0x8B: /* E umlaut/diaeresis (double-dot) */
					ch = 'E';
					break;
				case 0x8c: /* I grave */
				case 0x8d: /* I acute */
				case 0x8e: /* I circumflex */
				case 0x8f: /* I umlaut/diaeresis (double-dot) */
					ch = 'I';
					break;
				case 0x90: /* Latin "ETH" */
					ch = 'D';
					break;
				case 0x91: /* N tilde */
					ch = 'N';
					break;
				case 0x92: /* O grave */
				case 0x93: /* O acute */
				case 0x94: /* O circumflex */
				case 0x95: /* O tilde */
				case 0x96: /* O umlaut/diaeresis (double-dot) */
				case 0x98: /* O slash */
					ch = 'O';
					break;
				case 0x99: /* U grave */
				case 0x9a: /* U acute */
				case 0x9b: /* U circumflex */
				case 0x9c: /* U umlaut/diaeresis (double-dot) */
					ch = 'U';
					break;
				case 0x9d: /* Y acute */
					ch = 'Y';
					break;
				case 0x9e: /* cap "thorn" */
					ch = 'P';
					break;
				case 0x9f: /* small "sharp s" */
					ch = 's';
					break;
				case 0xa0: /* a grave*/
				case 0xa1: /* a acute */
				case 0xa2: /* a circumflex */
				case 0xa3: /* a tilde */
				case 0xa4: /* a umlaut/diaeresis (double-dot) */
				case 0xa5: /* a ring */
				case 0xa6: /* ae */
					ch = 'a';
					break;
				case 0xa7: /* c cedilla */
					ch = 'c';
					break;
				case 0xa8: /* e grave */
				case 0xa9: /* e acute */
				case 0xaa: /* e circumflex */
				case 0xab: /* e umlaut/diaeresis (double-dot) */
					ch = 'e';
					break;
				case 0xac: /* i grave */
				case 0xad: /* i acute */
				case 0xae: /* i circumflex */
				case 0xaf: /* i umlaut/diaeresis (double-dot) */
					ch = 'i';
					break;
				case 0xb0: /* small Latin "ETH" */
					ch = 'd';
					break;
				case 0xb1: /* n tilde */
					ch = 'n';
					break;
				case 0xb2: /* o grave */
				case 0xb3: /* o acute */
				case 0xb4: /* o circumflex */
				case 0xb5: /* o umlaut/diaeresis (double-dot) */
				case 0xb6: /* o umlaut/diaeresis (double-dot) */
				case 0xb8: /* o slash */
					ch = 'o';
					break;
				case 0xb9: /* u grave */
				case 0xba: /* u acute */
				case 0xbb: /* u circumflex */
				case 0xbc: /* u umlaut/diaeresis (double-dot) */
					ch = 'u';
					break;
				case 0xbd: /* y acute */
					ch = 'y';
					break;
				case 0xbe: /* small "thorn" */
					ch = 'p';
					break;
				case 0xbf: /* small y umlaut/diaeresis (double-dot) */
					ch = 'y';
					break;
				}
			}
		}

		return ch;
	}

	/**
	 * @see java.io.FilterInputStream#read(byte[], int, int)
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int i;
		int j = 0;
		int ch = 0;
		for (i = off; j < len; i++, j++ ) {
			ch = read();
			if (ch == -1)	// end of file
				break;
			b[i] = (byte)ch;
		}
		if (j == 0 && ch == -1) { // end of file & no data read
			j = -1;				// return EOF indication
		}
		//log.debug("read1, off="+off+", len="+len+", ret="+j);
		return j;
	}

	/**
	 * @see java.io.FilterInputStream#read(byte[])
	 */
	@Override
	public int read(byte[] b) throws IOException {
		//log.debug("read2");
		return read(b, 0, b.length);
	}

}

