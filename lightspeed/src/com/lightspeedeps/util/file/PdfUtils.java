package com.lightspeedeps.util.file;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.object.*;
import com.lightspeedeps.type.TextDirection;
import com.lightspeedeps.util.app.EventUtils;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

/**
 * A class providing utility functions that work on PDF files.
 */
public final class PdfUtils {
	/** Logger */
	private static final Log LOG = LogFactory.getLog(PdfUtils.class);

	/** Standard font shared for all PDF functions. */
	private static BaseFont FONT = null;
	/** Standard Bold font shared for all PDF functions. */
	private static BaseFont FONT_BOLD = null; // shared across entire application

	static {
		try {
			FONT = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.EMBEDDED);
			FONT_BOLD = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.EMBEDDED);
		}
		catch(Exception e) {
			LOG.error("Failed creating font: ", e);
		}
	}

	private PdfUtils() {
	}

	/**
	 * Adds a textual watermark to an existing PDF document, creating a new
	 * document in the process. Uses the default settings (see WaterMark class)
	 * for the watermark's position, direction, font-size, color, and opacity.
	 *
	 * @param inputFileName The fully-qualified filename (including path) of the
	 *            input PDF.
	 * @param outputFileName The fully-qualified filename (including path) of
	 *            the output PDF. This file will be created by the process.
	 * @param text The message to be displayed as the watermark.
	 * @return True if the process was successful.
	 * @throws Exception
	 * @see WaterMark
	 */
	public static boolean addWatermark(String inputFileName, String outputFileName, String text) {

		WaterMark waterMark = new WaterMark(text);
		boolean bRet = addWatermark(inputFileName, outputFileName, waterMark, false);

		return bRet;
	}

	/**
	 * Adds a textual water-mark to an existing PDF document, creating a new
	 * document in the process. The watermark's position, rotation, color, etc.,
	 * are defined in the given WaterMark object. The font CANNOT be set via the
	 * parameters.
	 *
	 * @param inputFileName - The existing PDF to which a water-mark will be
	 *            added; specifies the complete path.
	 * @param outputFileName - The filename used to create the new PDF;
	 *            specifies the complete path.
	 * @param waterMark Watermark to be printed.
	 * @param markIsUnder If true, the watermark should print underneath the
	 *            pdf's text; if false, print the watermark above (on top of)
	 *            the pdf's content text.
	 * @throws Exception
	 */
	public static boolean addWatermark(String inputFileName, String outputFileName, WaterMark waterMark, boolean markIsUnder) {
		int hpos = waterMark.getHorizontalPos();
		int vpos = waterMark.getVerticalPos();
		int fontsize = waterMark.getFontSize();
		int opacity = waterMark.getOpacity();
		Color color = waterMark.getColor();
		TextDirection dir = waterMark.getDirection();
		boolean outlineonly = waterMark.getOutlineOnly();
		String text = waterMark.getText();

		int angle = 0;
		float fOpacity = opacity/100.0f;
		boolean res = false;
		try {
			// Create a reader for the given document
			PdfReader reader = new PdfReader(inputFileName);
			int numPages = reader.getNumberOfPages();
			Rectangle pagesize = reader.getPageSize(1);
			int hCenter = (int)(pagesize.getWidth()/2);
			int vCenter = (int)(pagesize.getHeight()/2);
			if (waterMark.getTwoUp()) { // 2-up and landscape ("sides" script style)
				hCenter = (int)(pagesize.getHeight()/4);
				vCenter = (int)(pagesize.getWidth()/2);
				angle = dir.getAngle(pagesize.getHeight()/2, pagesize.getWidth());
			}
			else {
				angle = dir.getAngle(pagesize.getWidth(), pagesize.getHeight());
			}

//			int rtMargin = (int)(pagesize.getWidth() - (POINTS_PER_INCH * 0.4));
			int align = Element.ALIGN_LEFT;
			if (hpos < 0) {
				hpos = hCenter;
				align = Element.ALIGN_CENTER;
			}
			if (vpos < 0) {
				vpos = vCenter - (fontsize/2);
			}

			// we create a stamper that will copy the document to a new file
			PdfStamper stamp = new PdfStamper(reader, new FileOutputStream(outputFileName));

			// adding content to each page
			PdfContentByte under;
			PdfSpotColor pColor = new PdfSpotColor("some color", 1.0f, color);

			for (int pageNum = 1; pageNum <= numPages; pageNum++ ) {
				if (markIsUnder) {
					under = stamp.getUnderContent(pageNum); // under, to be under text on page
				}
				else {
					under = stamp.getOverContent(pageNum); // over, to be above text on page
				}
				under.beginText();
				under.setFontAndSize(FONT_BOLD, fontsize);
				under.setColorStroke(pColor, fOpacity);
				under.setColorFill(pColor, fOpacity);
				if (outlineonly) {
					under.setTextRenderingMode(PdfContentByte.TEXT_RENDER_MODE_STROKE);
				}
				under.showTextAligned(align, text, hpos, vpos, angle);
				under.endText();

				if (waterMark.getTwoUp()) {
					under.beginText();
					// font-size, opacity, outline, etc., are all the same & don't need to be re-set
					under.showTextAligned(align, text, hpos+(hCenter*2), vpos, angle);
					under.endText();
				}
			}

			// closing PdfStamper will generate the new PDF file
			stamp.close();
			res = true;
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		LOG.debug("ret=" + res + ", msg=" + text + ", in=" + inputFileName + ", out=" + outputFileName);
		return res;
	}

	/**
	 * Create a new PDF from an existing PDF by extracting a specific range of
	 * pages.
	 *
	 * @param inputFileName Filename of the input PDF.
	 * @param outputFileName Filename of the output PDF, to be created by this
	 *            method.
	 * @param fromPage First page (origin 1) to be included in the extracted
	 *            output file.
	 * @param toPage Last page (origin 1) to be included in the extracted output
	 *            file.
	 * @return True iff the extract was successful.
	 */
	public static boolean extractPages( String inputFileName, String outputFileName, int fromPage, int toPage) {
		LOG.debug("extract--from "+fromPage+" to "+toPage);
		boolean res = false;
		try {
			// create a reader for a certain document
			PdfReader reader = new PdfReader(inputFileName);
			// create a stamper that will copy the document to a new file
			PdfStamper stamp = new PdfStamper(reader, new FileOutputStream(outputFileName));
			// Then select the desired page range
			stamp.getReader().selectPages(fromPage + "-" + toPage);
			// closing PdfStamper will generate the new PDF file
			stamp.close();
			res = true;
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}

		return res;
	}

	/**
	 * TEST ONLY - This code is not currently used in production Lightspeed.
	 *
	 * Create a new PDF by extracting a given set of pages from a supplied PDF and simultaneously
	 * applying a watermark to each of the extracted page.
	 *
	 * @param inputFileName Filename of the input PDF.
	 * @param outputFileName Filename of the output PDF, to be created by this
	 *            method.
	 * @param fromPage First page (origin 1) to be included in the extracted
	 *            output file.
	 * @param toPage Last page (origin 1) to be included in the extracted output
	 *            file.
	 * @param hpos
	 * @param vpos
	 * @param fontsize
	 * @param dir
	 * @param color
	 * @param opacity
	 * @param outlineonly
	 * @param text
	 * @return
	 */
	public static boolean extractPages(String inputFileName, String outputFileName, int fromPage, int toPage,
			int hpos, int vpos, int fontsize, TextDirection dir,
			Color color, int opacity, boolean outlineonly, String text) {
		LOG.debug("extract+Wm--from "+fromPage+" to "+toPage);
		int angle = 0;
		float fOpacity = opacity/100.0f;
		boolean res = false;
		try {
			// we create a reader for a certain document
			PdfReader reader = new PdfReader(inputFileName);
			int numPages = reader.getNumberOfPages();
			LOG.debug(numPages);
			Rectangle pagesize = reader.getPageSize(1);
			angle = dir.getAngle(pagesize.getWidth(), pagesize.getHeight());

			// we create a stamper that will copy the document to a new file
			PdfStamper stamp = new PdfStamper(reader, new FileOutputStream(outputFileName));

			stamp.getReader().selectPages(fromPage + "-" + toPage);
			LOG.debug(numPages = reader.getNumberOfPages());

			// adding content to each page
			PdfContentByte under;
			BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.EMBEDDED);
			PdfSpotColor pColor = new PdfSpotColor("junk", 1.0f, color);

			for (int pageNum = 1; pageNum <= numPages; pageNum++ ) {
				// add text under the existing page
				under = stamp.getUnderContent(pageNum);
				under.beginText();
				under.setFontAndSize(bf, fontsize);
				under.setColorStroke(pColor, fOpacity);
				under.setColorFill(pColor, fOpacity);
				if (outlineonly) {
					under.setTextRenderingMode(PdfContentByte.TEXT_RENDER_MODE_STROKE);
				}
				under.showTextAligned(Element.ALIGN_LEFT, text, hpos, vpos, angle);
				under.endText();
			}
			// closing PdfStamper will generate the new PDF file
			stamp.close();
			res = true;
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}

		return res;
	}

	/**
	 * Adds a textual signature to an existing PDF document The Signature's
	 * position, rotation, color, etc., can be set via the input parameters. The
	 * font CANNOT be set via the parameters.
	 *
	 * @param infile - The existing PDF to which a water-mark will be added;
	 *            specifies the complete path.
	 * @param outfile - The filename used to create the new PDF; specifies the
	 *            complete path. This must be a different file than the input
	 *            file.
	 * @param textStyle Defines the style (font size, direction, opacity, color)
	 *            of the text to be added.
	 * @param textMarkupList A List of TextMarkup objects defining the location
	 *            (page, x, y) and text to be added.
	 * @return False if an exception was encountered, otherwise true.
	 * @throws Exception
	 */
	public static boolean addMarkup(String infile, String outfile, TextStyle textStyle, List<TextMarkup> textMarkupList) {
		int fontsize = textStyle.getFontSize();
		int opacity = textStyle.getOpacity();
		Color color = textStyle.getColor();
		TextDirection dir = textStyle.getDirection();
		boolean outlineonly = textStyle.getOutlineOnly();

		int angle = 0;
		float fOpacity = opacity/100.0f;
		boolean res = false;
		try {
			// Create a reader for the given document
			PdfReader reader = new PdfReader(infile);
			int numPages = reader.getNumberOfPages();
			Rectangle pagesize = reader.getPageSize(1);
			int hCenter = (int)(pagesize.getWidth()/2);
			int vCenter = (int)(pagesize.getHeight()/2);

			angle = dir.getAngle(pagesize.getWidth(), pagesize.getHeight());

			// we create a stamper that will copy the document to a new file
			PdfStamper stamp = new PdfStamper(reader, new FileOutputStream(outfile));

			// adding content to each page
			PdfContentByte under;
			PdfSpotColor pColor = new PdfSpotColor("some color", 1.0f, color);

			for (int pageNum = 1; pageNum <= numPages; pageNum++ ) {

				// 'underContent' is invisible for a "graphic" PDF, e.g., when created by scanning. Ok for printed PDF.
				// under = stamp.getUnderContent(pageNum); // under, to allow existing text to show better.

				// 'overContent' works on both graphic and printed PDFs.
				under = stamp.getOverContent(pageNum); // over, to be above scanned images

				under.beginText();
				under.setFontAndSize(FONT, fontsize);
				under.setColorStroke(pColor, fOpacity);
				under.setColorFill(pColor, fOpacity);
				if (outlineonly) {
					under.setTextRenderingMode(PdfContentByte.TEXT_RENDER_MODE_STROKE);
				}
				for (TextMarkup textMarkup : textMarkupList) {
					if (textMarkup.getPageNumber() == pageNum ||
							textMarkup.getPageNumber() == 0) { // "0" means print on all pages
						int hpos = textMarkup.getHorizontalPos();
						int vpos = textMarkup.getVerticalPos();
						int align = Element.ALIGN_LEFT;
						if (hpos < 0) {
							hpos = hCenter;
							align = Element.ALIGN_CENTER;
						}
						if (vpos < 0) {
							vpos = vCenter;
							align = Element.ALIGN_CENTER;
						}
						for (String text : textMarkup.getText()) {
							vpos -= fontsize;
							under.showTextAligned(align, text, hpos, vpos, angle);
						}
					}
				}
				under.endText();
			}
			// closing PdfStamper will generate the new PDF file
			stamp.close();
			res = true;
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		LOG.debug("ret=" + res + ", in=" + infile + ", out=" + outfile);
		return res;
	}

	/**
	 * TEST CODE ONLY
	 * Adds a textual water-mark as a signature to an existing PDF document, creating a new
	 * document in the process. The watermark's position, rotation, color, etc.,
	 * can be set via the input parameters. The font CANNOT be set via the
	 * parameters.
	 *
	 * @param inputFileName - The existing PDF to which a water-mark will be
	 *            added; specifies the complete path.
	 * @param outputFileName - The filename used to create the new PDF;
	 *            specifies the complete path.
	 * @param signature - The signature of the user which last updated the PDF.
	 * @throws Exception
	 */
	public static boolean addText(String inputFileName, String outputFileName, WaterMark waterMark, List<String> signature) {

		int hpos = waterMark.getHorizontalPos();
		int vpos = waterMark.getVerticalPos();
		int fontsize = waterMark.getFontSize();
		int opacity = waterMark.getOpacity();
		Color color = waterMark.getColor();
		TextDirection dir = waterMark.getDirection();
		boolean outlineonly = waterMark.getOutlineOnly();
		//String text = waterMark.getText();

		int angle = 0;
		float fOpacity = opacity/100.0f;
		boolean res = false;
		try {
			// Create a reader for the given document
			PdfReader reader = new PdfReader(inputFileName);
			int numPages = reader.getNumberOfPages();
			Rectangle pagesize = reader.getPageSize(1);
			int hCenter = (int)(pagesize.getWidth()/2);
			int vCenter = (int)(pagesize.getHeight()/2);
			angle = dir.getAngle(pagesize.getWidth(), pagesize.getHeight());

//			int rtMargin = (int)(pagesize.getWidth() - (POINTS_PER_INCH * 0.4));
			int align = Element.ALIGN_LEFT;
			if (hpos < 0) {
				hpos = hCenter;
				align = Element.ALIGN_CENTER;
			}
			if (vpos < 0) {
				vpos = vCenter;
				align = Element.ALIGN_CENTER;
			}

			// we create a stamper that will copy the document to a new file
			PdfStamper stamp = new PdfStamper(reader, new FileOutputStream(outputFileName));

			// adding content to each page
			PdfContentByte under;
			//Image img = Image.getInstance("watermark.jpg");
			//img.setAbsolutePosition(200, 400);
			PdfSpotColor pColor = new PdfSpotColor("some color", 1.0f, color);

			for (int pageNum = 1; pageNum <= numPages; pageNum++ ) {
				// watermark image under the existing page
				// under = stamp.getUnderContent(i);
				// under.addImage(img);
				// text under the existing page
				//under = stamp.getUnderContent(pageNum);
				under = stamp.getUnderContent(pageNum); // over, to be above colored pages
				under.beginText();
				under.setFontAndSize(FONT, fontsize);
				under.setColorStroke(pColor, fOpacity);
				under.setColorFill(pColor, fOpacity);
				if (outlineonly) {
					under.setTextRenderingMode(PdfContentByte.TEXT_RENDER_MODE_STROKE);
				}
				//under.showTextAligned(align, text, hpos, vpos, angle);
				for (String text : signature) {
					under.showTextAligned(align, text, hpos, vpos, angle);
					vpos -= fontsize;
				}
				under.endText();

				// test code to put lightspeed name in right margin
//				under.setFontAndSize(FONT, 10);
//				under.setTextRenderingMode(PdfContentByte.TEXT_RENDER_MODE_FILL);
//				under.showTextAligned(Element.ALIGN_CENTER, "LightSPEED eps", rtMargin, vCenter, 90);
			}

			/*
			// example of adding an extra page
			stamp.insertPage(1, PageSize.A4);
			over = stamp.getOverContent(1);
			over.beginText();
			over.setFontAndSize(bf, 18);
			over.showTextAligned(Element.ALIGN_LEFT,
				"DUPLICATE OF AN EXISTING PDF DOCUMENT", 30, 600, 0);
			over.endText();
			*/

			// closing PdfStamper will generate the new PDF file
			stamp.close();
			res = true;
		}
		catch (Exception e) {
			EventUtils.logError(e);
		}
		LOG.debug("ret=" + res + ", msg=" + signature.get(0) + ", in=" + inputFileName + ", out=" + outputFileName);
		return res;
	}

	/**
	 * This method merges multiple PDF files, which may contain editable
	 * (fillable-form) fields, into a single PDF file.
	 *
	 * @param inputFileNames List of filenames of the PDF files to be merged.
	 * @param outputFileName The filename to be used for the newly-created
	 *            output file.
	 * @return Zero if no exception occurred during the merge process. A
	 *         positive value indicates the number of files (typically
	 *         attachments) which could NOT be merged and were skipped. A
	 *         negative value indicates that the output file could not be
	 *         created, typically because all the input files failed in some
	 *         fashion. LS-2889
	 */
	public static int combinePdfs(List<String> inputFileNames, String outputFileName) {
		LOG.debug("output=" + outputFileName);
		int failedItems = 0; // LS-2889
		try {
			Document document = new Document();
			PdfCopy copy = new PdfCopy(document, new FileOutputStream(outputFileName));
			document.open();
			int cnt = 1;
			for (String file : inputFileNames) {
				ByteArrayOutputStream baos = null;
				PdfReader reader = null;
				try {
					baos = renameFields(file, cnt);
				}
				catch (LoggedException e) {
					 // Pass the LoggedException, so we don't create a 2nd Event entry. LS-2889
					EventUtils.logError("Error combining PDFs, skipping file=" + file, e);
					failedItems++; // LS-2889
					continue; // allow process to continue, we'll try the next file
				}
				cnt++;
				if (baos != null) {
					reader = new PdfReader(baos.toByteArray());
					for (int i = 1; i <= reader.getNumberOfPages(); i++) {
						copy.addPage(copy.getImportedPage(reader, i));
					}
					copy.copyAcroForm(reader);
					copy.freeReader(reader);
					reader.close();
					baos.close();
				}
			}
			try {
				document.close();
			}
			catch (Exception e) {
				// May fail with "document has no pages" if attachments are 'bad' (e.g., password protected) LS-2889
				EventUtils.logError("Error combining PDFs (document.close), expected output file=" + outputFileName);
				failedItems = -1;
			}
			try {
				copy.close();
			}
			catch (Exception e) {
				// May fail with "document has no pages" if attachments are 'bad' (e.g., password protected) LS-2889
				if (failedItems >= 0) {
					EventUtils.logError("Error combining PDFs (copy.close), expected output file=" + outputFileName);
				}
				else { // Event was already logged for document.close().
					LOG.error("Error combining PDFs (copy.close), expected output file=" + outputFileName);
				}
				failedItems = -1;
			}
		}
		catch (Exception e) {
			EventUtils.logError(e);
			throw new LoggedException(e);
		}
		return failedItems;
	}

	/**
	 * Rename the embedded form fields within a PDF. This is to eliminate
	 * conflicts between the fields when merging multiple PDFs into one.
	 *
	 * @param filename The name of the PDF file to process.
	 * @param counter An int value which should be unique across the set of PDFs being
	 *            merged. This uniqueness is what guarantees that field names
	 *            will not conflict between merged files.
	 * @return A byte array containing the updated PDF.
	 * @throws IOException
	 * @throws DocumentException
	 */
	@SuppressWarnings("unchecked")
	private static ByteArrayOutputStream renameFields(String filename, int counter)
			throws IOException, DocumentException {
		ByteArrayOutputStream baos;
		try {
			baos = new ByteArrayOutputStream();
			PdfReader reader = new PdfReader(filename);
			PdfStamper stamper = new PdfStamper(reader, baos);
			AcroFields form = stamper.getAcroFields();
			Set<String> keys = new HashSet<>(form.getFields().keySet());
			for (String key : keys) {
				form.renameField(key, String.format("%s_%d", key, counter));
			}
			stamper.close();
			reader.close();
		}
		catch (Exception e) {
			EventUtils.logError(e);
			LOG.error("file=" + filename + " counter=" + counter);
			throw new LoggedException(e);
		}
		return baos;
	}

}
