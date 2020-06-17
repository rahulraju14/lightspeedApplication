//	File Name:	PdfDumpText.java
package com.lightspeedeps.test.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.PDFScanStrategy;
import org.apache.pdfbox.util.PDFTextScanner;

import com.lightspeedeps.type.TextElementType;
import com.lightspeedeps.util.app.EventUtils;

import junit.framework.TestCase;

/**
 * A class for reading PDF documents and processing the text lines extracted
 * from it.
 * <p/>
 * This particular implementation is designed to extract pay rates from a PDF
 * containing lines of rates associated with occupation codes and schedule
 * codes.
 */
public class PdfDumpRates extends TestCase implements PDFScanStrategy {

	private static final Log log = LogFactory.getLog(PdfDumpRates.class);

	// Known schedule codes
	private static final String scheds = "00 01 40 41 42 43 44 45 48 49 54 55 56 57 64 70 ";

	/** The scanner reads the PDF and decodes it for us. */
	private PDFTextScanner scanner;

	/** This is the PDF document. */
	private PDDocument document;
	private Writer output;

	/**
	 * The last TextLine that was adding to a TextElement.  This is used by addTextElement()
	 * to determine if the next TextLine can be merged into the existing TextElement.
	 */
	private TextLine lastLine = null;

	TextLine firstLine = null; // possible page header line w/o page number

	/** The physical page number within the PDF, passed to us by the PDF
	 * management routines.  See startPage(). */
	private int pdfPageNum;

	/** The last logical page number encountered -- extracted from page headings. */
//	private String scriptPageNum;

	/** A running count of the lines passed to writeLine() on the current page. */
	private int lineNumber;

	/** The lowest value for "startY" we encounter on any page, i.e., the smallest
	 * value of Y coordinate of any line.  This is at the top of the page, and
	 * is in inches. */
	@SuppressWarnings("unused")
	private float smallestY;

	/** The highest value for "startY" we encounter on any page, i.e., the largest
	 * value of Y coordinate of any line.  This is at the bottom of the page, and
	 * is in inches. */
	@SuppressWarnings("unused")
	private float largestY;

	/** This is set to true by the "startPage()" call-back from the PDF scanner, and set to
	 * false once writeLine() has been called back with a non-blank line on the page.  It is
	 * used to track if we are looking for a page heading line.  */
	@SuppressWarnings("unused")
	private boolean newPage;

	/** A list of all the lines in the script, created during the initial
	 * scan of the PDF, and processed multiple times during the import process. */
	private List<TextLine> lines;

	TextElementType textType = TextElementType.OTHER;

	/** first PDF page number to process */
	private int startPage;

	/** last PDF page number to process */
	private int endPage;

	public PdfDumpRates() {
		log.debug("");
	}

	protected void init() {
		lines = new ArrayList<TextLine>(5000);
		pdfPageNum = 0;
		newPage = false;
		lineNumber = 0;
		smallestY = 72000; // a thousand inches! Any large value works here.
		largestY = -1;

		startPage = 495;	// Set first & last page of PDF to be processed or dumped.
		endPage = 496;
	}

	/**
	 * Will be called by test supervisor.
	 */
	public void testPdfDump() {
		boolean bRet = false;
		init();

		String path;
		path = "D:\\Dev\\Studio\\timecards-payroll-etc\\pay master\\";
		path = "D:\\Dev\\Studio\\samples\\scripts\\";
		path = "C:\\Dev\\Studio\\eStarts-onBoarding\\";

		String file;
		file = "I-9 2017"; // "30 page script LS eps-DH";

		file = path + file + ".pdf";

		try {
			document = PDDocument.load(file, true);
		}
		catch (IOException e) {
			log.error("*** Document load failed. ***");
			return;
		}

		if (document.isEncrypted()) {
			// TODO what about encrypted PDF?
		}
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		output = new OutputStreamWriter( byteOut );
			  //  new FileOutputStream( "c:\\tmp\\temp.txt" ));

		String encoding = "ISO-8859-1"; // "UTF-8" "ISO-8859-1"
			// with "UTF-8", a special prime (x92) came through as "?" (on 30page.pdf)
		try {
			scanner = new PDFTextScanner(encoding);
			scanner.setSortByPosition(true);
			scanner.setPadding(true);
			scanner.setStrategy(this);
			bRet = processDocument();
		}
		catch (IOException e) {
		}
		finally {
			// 'document' will be null if processDocument() closed in normally.
			if (document != null) {
				try {
					document.close();
				}
				catch(Exception e) {
					EventUtils.logError("Exception in final document close.", e);
				}
			}
		}

		if (bRet) {
		}
		log.debug("output stream (len="+ byteOut.toString().length() + ")=`"  + byteOut.toString() + "`");
	}

	/**
	 * This manages the majority of the PDF import process.  It:
	 * <ul>
	 * <li> creates a Script object
	 * <li> calls the PDF scanner (which returns lines via callbacks)
	 * <li> closes the PDF
	 * <li> calls processLines() to analyze and load the data
	 * <li> calls doFinalUpdates() to wrap the process
	 * </ul>
	 * If running in batch mode, the database transaction is started
	 * and ended within this method.
	 * @return False only if a major error, typically a runtime exception, occurs;
	 * true otherwise.
	 * Data errors, such as missing D/N values, scene numbers out of sequence, or
	 * any other foreseen and avoidable/correctable problems, do NOT cause a False
	 * return.
	 */
	private boolean processDocument() {
		boolean bRet = true;

		try {
			// writeText will call us back using the PDFScanStrategy-defined methods:
			// writeLine(), startPage(), endPage(), startArticle(), endArticle().
			scanner.writeText(document, output);

			// At this point, all the document data is in the "lines" List of TextLines.

			try {	// catch close errors separately, so they won't stop the import.
				document.close();	// close the PDF file
				document = null;	// indicate it was closed successfully.
			}
			catch (Exception e) {
			}

			bRet = processLines(); // use this to extract occ-code and rate information from lines
//			bRet = printLines(); // use this to jump dump all lines to sysout
		}
		catch (Exception e) {
			bRet = false;
		}

		return bRet;
	}

	/**
	 * The PDF input processing starts here.  This method is called from the scanner
	 * for each line in the PDF.
	 * <p>
	 * Here we accumulate all the individual lines as TextLine objects, and do
	 * some preliminary analysis -- counting all lines at each indent point,
	 * and looking for page headings to set script page numbers.  Only the first
	 * two lines of each page are considered possible page headers.
	 * <p>
	 * Also, non-ASCII hyphens, quotes & primes are replaced with their ASCII
	 * equivalents.
	 */
	@Override
	public void writeLine(String text, float startX, float startY, float averageCharWidth) {

		if (pdfPageNum < startPage) { // ********************** FIRST PAGE OF OCCUPATION CODE DATA ************
			return;
		}
		if (pdfPageNum > endPage) { // ********************** LAST PAGE OF OCCUPATION CODE DATA ************
			return;
		}
		TextLine line = new TextLine();
		int indent = (int)(startX/averageCharWidth);
		line.startX = (int)startX;
		//log.debug("x=" + startX + ", width=" + averageCharWidth + ", indent=" + indent);
		if (text.equals("*")) { // empty "changed" line
			text = "";
		}
		else {
			if (text.length() > 0 && text.charAt(0) == ' ') {
				int i = 0;
				for (; i < text.length(); i++) {
					if (text.charAt(i) != ' ') {
						break;
					}
				}
				indent += i;
			}
			text = text.trim() // replace non-ASCII punctuation with ASCII equivalents
					.replace('‘', '\'')
					.replace('’', '\'')
					.replace('“', '"')
					.replace('”', '"')
					.replace('–', '-'); // x'96'
			text = text.replace("***", " ");
		}
		line.text = text;
		line.pdfPageNumber = pdfPageNum; // pdfPageNum is maintained by the startPage() callback.
		lineNumber++;		// count lines passed to us from current page (zeroed in startPage()).
		line.lineNumber = lineNumber;
		line.yPos = startY;

		if (text.length()==0) {
			line.type = TextElementType.BLANK;
		}
		else {
			line.indent = indent < 0 ? 0 : indent;
		}

		lines.add(line);
		lastLine = line;
	}

	/**
	 * This is a major processing section of the PDF import. It makes multiple passes
	 * over the List of text lines.
	 * <p>
	 * The first pass tries to identify line types by matching them to patterns
	 * (regular expressions) -- see checkPatterns().
	 * <p>
	 * The next pass calculates line numbers from the PDF y-position values.
	 * <p>
	 * The last pass is a state-driven process, controlled by the state
	 * transition tables ('transitions') defined near the top of this class.
	 * During the second pass we also attempt to identify line types by
	 * indentation (for lines not identified by patterns).
	 *
	 * @return True in all cases (so far).
	 */
//	@SuppressWarnings("unused")
	private boolean processLines() {

		boolean occ = false;
		int occPage = 0;
		short count = 0;
		short lineCount = 0;

		System.out.println("Seq\tPage\tLine\tCode 1\tSchedule\tCode 2\tJob title\tRates");

		for (TextLine line : lines) {
			checkPatterns(line); // Determine line type by analyzing text layout; count scenes
			if (line.type == TextElementType.ACTION) { // ACTION = line to keep
				occ = true;
				occPage = line.pdfPageNumber;
				count = 1;
			}
			else {
				count++;
				if (count <= 3 && line.pdfPageNumber == occPage) {
					// keep next 2 lines if on same page, may be continuation text
					line.text  = stripPay(line.text, line);
				}
				else {
					occ = false;
				}
			}
			if (occ && line.text.length() > 1) {
				lineCount++;
				String rateList = "";
				for (int i=0; i<4; i++) {
					rateList += line.rates[i] + "\t";
				}

				System.out.println(
						lineCount
						+ "\t" + line.pdfPageNumber
						+ "\t" + line.lineNumber
						+ "\t" + line.occCode
						+ "\t" + line.schedule
						+ "\t" + line.occCode2
						+ "\t" + line.text
						+ "\t" + rateList
						);
			}
		}

		return true;
	}

	private static final String RE_OCCUPATION = "((\\d{2}-\\d{3} )|(\\d{4,5}[A-Za-z]?( {1,2}\\d{2})? ))(.*)";
	private static final Pattern P_OCCUPATION = Pattern.compile(RE_OCCUPATION);
	private static final int OCC_TEXT_GROUP = 5;

	private static final String RE_OCCUPATION2 = "(\\d{4,5} )(.*)";
	private static final Pattern P_OCCUPATION2 = Pattern.compile(RE_OCCUPATION2);

	private static final String RE_PAY = "(.*?)((\\$?[0-9,]*\\d+\\.\\d{2,4}( |\\(|\\*|\\+)).*)";
	private static final Pattern P_PAY = Pattern.compile(RE_PAY);

	private static final String RE_RATE = "\\$?([0-9,]*\\d+\\.\\d{2,4})[+A]?";
	private static final Pattern P_RATE = Pattern.compile(RE_RATE);

	/**
	 * Attempt to determine line type by comparing to various patterns
	 * (typically regular expressions).
	 * @param line The TextLine to be analyzed.
	 */
	private void checkPatterns(TextLine line) {
		TextElementType type = line.type;
		boolean scheduleOk;

		if (type == TextElementType.OTHER &&
				line.indent < 100) {
			// test for various possible Scene Heading patterns
			Matcher m = P_OCCUPATION.matcher(line.text);
			if (m.matches()) {
				line.occCode = m.group(1).trim();
				scheduleOk = splitSchedule(line);
				if (scheduleOk) {
					type = TextElementType.ACTION;
					String right = m.group(OCC_TEXT_GROUP).trim();
					right = stripPay(right, line);
					m = P_OCCUPATION2.matcher(right);
					if (m.matches()) {
						line.occCode2 = m.group(1).trim();
						right = m.group(2).trim();
					}
					line.text = right;
					splitRates(line);
				}
			}
		}
		line.type = type;
	}

	/**
	 * @param line
	 */
	private void splitRates(TextLine line) {
		if (line.rateText.length() > 1) {
			String rates = line.rateText.trim();
			String rate;
			int len;
			int ix = 0;
			while(rates.length() > 0) {
				//log.debug(rates);
				if ((len=rates.indexOf(' ')) > 0) {
					rate = rates.substring(0, len);
					rates = rates.substring(len+1).trim();
				}
				else {
					rate = rates;
					rates = "";
				}
				// note: our RegEx will NOT match values in parens!
				Matcher m = P_RATE.matcher(rate);
				if (m.matches()) {
					rate = m.group(1);
					line.rates[ix++] = rate;
					if (ix > 3) {
						break;
					}
				}
			}
		}
	}

	/**
	 * @param line
	 */
	private boolean splitSchedule(TextLine line) {
		if (line.occCode.contains(" ")) {
			String occ = line.occCode;
			String sched = occ.substring(occ.indexOf(' ')).trim();
			if (sched.length() == 2) {
				if (scheds.indexOf(sched) >= 0) {
					line.schedule = sched;
					occ = occ.substring(0, occ.indexOf(' '));
					line.occCode = occ;
				}
				else {
					log.debug("schedule? =" + sched + " pg=" + line.pdfPageNumber);
				}
			}
		}
		return true;
	}

	private String stripPay(String input, TextLine line) {
		input += ' ';
		if (input.indexOf('.') > 0) {
			input = input.replace(" 7.0 ", " ");
			input = input.replace(" 8.0 ", " ");
			input = input.replace(" 9.0 ", " ");
			input = input.replace(" 40.0 ", " ");
			input = input.replace(" 43.2 ", " ");
			input = input.replace(" 45.0 ", " ");
			input = input.replace(" 48.6 ", " ");
			input = input.replace(" 54.0 ", " ");
			input = input.replace(" 56.0 ", " ");
			input = input.replace(" 60.0 ", " ");
			input = input.replace(" 64.0 ", " ");
		}
		if (input.indexOf("egotiat") > 0) {
			input = input.replace(" Subject to negotiation ", " ");
			input = input.replace(" Subject to Individual Negotiation ", " ");
			input = input.replace(" As negotiated under Local Agreements ", " ");
			input = input.replace(" As negotiated under Basic Agreement ", " ");
			input = input.replace(" As Negotiated ", " ");
			input = input.replace(" Negotiated ", " ");
			input = input.replace(" negotiated ", " ");
			input = input.replace(" Negotiable ", " ");
			input = input.replace(" Negotiate ", " ");
		}
		if (input.indexOf("see") > 0) {
			input = input.replaceAll(" \\(.?ee pg\\.?  ?2 ?- ?32\\)", " ");
			//  (see pg. 2 - 32)
		}
		input = input.replace(" NA ", " ");
		input = input.replace(" NA* ", " ");
		input = input.replace("** ", " ");
		input = input.replace("++ ", " ");
		input = input.replace(" ---- ", " ");

		input = input.replace('%', '@');
		Matcher pay = P_PAY.matcher(input);
		if (pay.matches()) {
			input = pay.group(1);
			line.rateText = pay.group(2);
		}
		input = input.replace('@', '%');
		input = input.trim();
		if (input.endsWith("*")) {
			input = input.substring(0, input.length()-1);
		}
		else if (input.endsWith("+")) {
			input = input.substring(0, input.length()-1);
		}
		return input;
	}

	@SuppressWarnings("unused")
	private boolean printLines() {

		for (TextLine line : lines) {
			System.out.println(line.text);
		}

		return true;
	}


	// * * * PDFScanStrategy implementation methods

	@Override
	public void startPage(PDPage page, int pageNumber) {
		if (lastLine != null) {
			lastLine.endOfPage = true;
		}
		newPage = true;
		lineNumber = 0;
		pdfPageNum = pageNumber;
		firstLine = null;
	}

	@Override
	public void endPage(PDPage page) {
		// not used
	}

	@Override
	public void startArticle(boolean isltr) {
		// not used for scripts
	}

	@Override
	public void endArticle() {
		// not used for scripts
	}

	private static class TextLine {

		public TextElementType type = TextElementType.OTHER;
		public String text = null;
		/** Starting x (horizontal) position of this line of text. */
		@SuppressWarnings("unused")
		public int startX = 0;
		/** calculated indent, based on average character width passed to writeLine */
		public int indent = 0;
		/** physical page number, from the PDF process */
		public int pdfPageNumber;
		/** Line number computed based on "Y" coordinate of the text. */
		public int lineNumber;
		/** The y (vertical) position of this line, as reported by the PDF scanner. */
		public float yPos;
		/** Set to true if this is the last line on a page. */
		public boolean endOfPage;

		public String occCode = " ";
		public String schedule = " ";
		public String occCode2 = " ";
		public String rateText = " ";
		public String[] rates = new String[4];

		public TextLine() {
			for (int i=0; i<4; i++) {
				rates[i] = " ";
			}
		}

		@Override
		public String toString() {
			String s;
			s = String.format("%3d(%s)- %3d(%3.0f) ", pdfPageNumber,lineNumber, indent, yPos);
			s += " '" + text + "' ";
			s += "[" + type + "] ";
			s += (endOfPage ? "(endPage) " : "");
			return s;
		}

	}

}
