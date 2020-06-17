package com.lightspeedeps.test.pdf;

import java.awt.Color;

import com.lightspeedeps.object.WaterMark;
import com.lightspeedeps.test.SpringMultiTestCase;
import com.lightspeedeps.type.TextDirection;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.file.PdfUtils;

public class PdfUtilsTest extends SpringMultiTestCase {

	private int success = 0;

	private final static String PATH =
			".\\data\\"; // use this for JUnit batch testing - relative to {project}/junit/
//			".\\junit\\data\\"; // use this for Eclipse JUnit testing - relative to {project}
	private final static String FILENAME = "test_script";
	private final static String FILETYPE = ".pdf";

	private final static Color BLACK = new Color(0);
	//private final static Color RED = new Color(0xff0000);
	private final static int DEF_WIDTH = (int)(8.5f * Constants.POINTS_PER_INCH);
	private final static int DEF_HEIGHT = (int)(11f * Constants.POINTS_PER_INCH);

	private int filenum = 1;

	private TextDirection dir = TextDirection.HORIZONTAL;

	public void testExtractPages() throws Exception {

		// tests for PdfUtils.extractPages()
		String inFile = FILENAME;
		String outFile = inFile + "Ex";

		testOneEx( inFile, outFile, 22, 22); /**/

		System.out.println(success + " successful tests completed.");
	}

	public void testWatermark() throws Exception {

		// tests for PdfUtils.addWatermark()
		String inFile = FILENAME;
		String outFile = inFile + "W";

		dir = TextDirection.HORIZONTAL;
		testOneWm( inFile, outFile, 72, DEF_HEIGHT/2,
				54, dir, BLACK, 30, true, "30% black 54pt "+dir); /**/

		dir = TextDirection.BOTTOMLEFT_TOPRIGHT;
		testOneWm( inFile, outFile, 144, 144,
				54, dir, BLACK, 30, true, "30% black 54pt "+dir); /**/

		dir = TextDirection.VERTICAL;
		testOneWm( inFile, outFile, DEF_WIDTH/2, 72,
				54, dir, BLACK, 30, true, "30% black 54pt "+dir); /**/

		testOneWmDef( inFile, outFile, "default");
		testOneWmDef( inFile, outFile, "default watermark");
		testOneWmDef( inFile, outFile, "default watermark settings");

		System.out.println(success + " successful tests completed.");
	}

	public void testExtractPagesWatermark() throws Exception {

		// tests for PdfUtils.extractPage() with watermark added
		String inFile = FILENAME;
		String outFile = inFile + "ExW";

		dir = TextDirection.BOTTOMLEFT_TOPRIGHT;
		testOneExWm( inFile, outFile, 2, 5, 144, 144,
				54, dir, BLACK, 30, true, "pages 2-5 only "); /**/

		System.out.println(success + " successful tests completed.");
	}

	private void testOneEx(
			String inputFile, String outputFile,
			int fromPage, int toPage ) {

		outputFile = PATH + outputFile + (filenum++) + FILETYPE;
		inputFile = PATH + inputFile + FILETYPE;

		assertEquals("range=" + fromPage + " to " + toPage, true,
				PdfUtils.extractPages(inputFile, outputFile, fromPage, toPage) );
		success++;
	}

	private void testOneWm(
			String inputFile, String outputFile,
			int hpos, int vpos,
			int fontsize, TextDirection dir,
			Color color,
			int opacity,
			boolean outline,
			String text) throws Exception {

		outputFile = PATH + outputFile + (filenum++) + FILETYPE;
		inputFile = PATH + inputFile + FILETYPE;

		WaterMark wm = new WaterMark(hpos, vpos, fontsize, dir, color, opacity, outline, text);
		assertEquals("input=`" + text + "` ", true,
				PdfUtils.addWatermark(inputFile, outputFile, wm, false));
		success++;
	}

	private void testOneWmDef(
			String inputFile, String outputFile,
			String text) throws Exception {

		outputFile = PATH + outputFile + (filenum++) + FILETYPE;
		inputFile = PATH + inputFile + FILETYPE;

		WaterMark wm = new WaterMark(text);
		assertEquals("input=`" + text + "` ", true,
				PdfUtils.addWatermark(inputFile, outputFile, wm, false) );
		success++;
	}

	private void testOneExWm(
			String inputFile, String outputFile,
			int fromPage, int toPage,
			int hpos, int vpos,
			int fontsize, TextDirection dir,
			Color color,
			int opacity,
			boolean outline,
			String text) throws Exception {

		outputFile = PATH + outputFile + (filenum++) + FILETYPE;
		inputFile = PATH + inputFile + FILETYPE;

		assertEquals("input=`" + text + "` ", true,
				PdfUtils.extractPages(inputFile, outputFile, fromPage, toPage,
						hpos, vpos, fontsize,
						dir, color, opacity, outline, text) );
		success++;
	}


}
