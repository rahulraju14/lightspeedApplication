package com.lightspeedeps.test.pdf;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.lightspeedeps.object.TextMarkup;
import com.lightspeedeps.object.TextStyle;
import com.lightspeedeps.test.SpringMultiTestCase;
import com.lightspeedeps.type.TextDirection;
import com.lightspeedeps.util.app.Constants;
import com.lightspeedeps.util.file.PdfUtils;

public class PdfSignatureTest extends SpringMultiTestCase {

	private int success = 0;

	private final static String PATH =
//			"/home/ist-126/lightspeed/LightSpeedLocal/lightspeed29/junit/data/";
//			".\\data\\"; // use this for JUnit batch testing - relative to {project}/junit/
			"./junit/data/"; // use this for Eclipse JUnit testing - relative to {project}

	private final static String FILENAME = "test_script";
	private final static String FILETYPE = ".pdf";

	private final static Color BLACK = new Color(0);
//	private final static int DEF_WIDTH = (int)(8.5f * Constants.POINTS_PER_INCH);
//	private final static int DEF_HEIGHT = (int)(11f * Constants.POINTS_PER_INCH);

	private int filenum = 1;

	private TextDirection dir = TextDirection.HORIZONTAL;

	public void testSignature() throws Exception {

		// tests for PdfUtils.addWatermark()
		String inFile = FILENAME;
		String outFile = inFile + "W";

		/*dir = TextDirection.HORIZONTAL;
		testSignature( inFile, outFile, 80, DEF_HEIGHT/2,
				16, dir, BLACK, 30, true, "30% black 54pt "+dir);

		dir = TextDirection.BOTTOMLEFT_TOPRIGHT;
		testSignature( inFile, outFile, 144, 144,
				16, dir, BLACK, 30, true, "30% black 54pt "+dir);

		dir = TextDirection.VERTICAL;
		testSignature( inFile, outFile, DEF_WIDTH/2, 72,
				16, dir, BLACK, 30, true, "30% black 54pt "+dir); /**/

		dir = TextDirection.HORIZONTAL;
		testSignature( inFile, outFile,
				Constants.POINTS_PER_INCH, // 1 inch from left edge
				Constants.POINTS_PER_INCH, // 1 inch from bottom edge
				12, dir, BLACK, 100, false, "100% black 12pt "+dir);

		System.out.println(success + " successful tests completed.");
	}

	private void testSignature(
			String inputFile, String outputFile,
			int hpos, int vpos,
			int fontsize, TextDirection dir,
			Color color,
			int opacity,
			boolean outline,
			String text) throws Exception {

		outputFile = PATH + outputFile + (filenum++) + FILETYPE;
		inputFile = PATH + inputFile + FILETYPE;
		List<TextMarkup> textMarkupList = new ArrayList<>();
		UUID u = UUID.randomUUID();
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

		for (int pageNum = 1; pageNum <= 3; pageNum++ ) {
			List<String> ptext = new ArrayList<>();
			ptext.add(Constants.SIGNED_BY + "Me on pg " + pageNum + " " + sdf.format(new Date()));
			ptext.add(u.toString());
			TextMarkup textMarkup = new TextMarkup(hpos, vpos, pageNum, ptext);
			textMarkupList.add(textMarkup);
		}

		List<String> ptext = new ArrayList<>();
		ptext.add("Initialed here: by me");
		textMarkupList.add(new TextMarkup(400, vpos, 0, ptext));

		TextStyle textStyle = new TextStyle(fontsize, dir, color, opacity, outline);
		assertEquals("input=`" + text + "` ", true,
				PdfUtils.addMarkup(inputFile, outputFile, textStyle, textMarkupList));
		success++;
	}
}
