//	File Name:	PdfDumpText.java
package com.lightspeedeps.test.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

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
 * A class for reading PDF documents and dumping the text portion to system
 * output.
 * <p/>
 * See 'PdfDumpRates' for a version designed to do more processing of the input
 * lines.
 */
public class PdfDumpText extends TestCase implements PDFScanStrategy {

	private static final Log log = LogFactory.getLog(PdfDumpText.class);

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

	public PdfDumpText() {
		log.debug("");
	}

	protected void init() {
		lines = new ArrayList<TextLine>(5000);
		pdfPageNum = 0;
		newPage = false;
		lineNumber = 0;
		smallestY = 72000; // a thousand inches! Any large value works here.
		largestY = -1;

		startPage = 0;	// Set first & last page of PDF to be processed or dumped.
		endPage = 999;
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
		path = "C:\\Dev\\Studio\\eStarts-onBoarding\\I-9 2017\\";

		String file;
		file = "i-9instr"; // "30 page script LS eps-DH";

		file = path + file + ".pdf";

		try {
			document = PDDocument.load(file, true);
		}
		catch (IOException e) {
			log.error("*** Document load failed. ***");
			return;
		}

		if (document.isEncrypted()) {
			// what about encrypted PDF?
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

			bRet = printLines(); // use this to jump dump all lines to sysout
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

		public TextLine() {
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
