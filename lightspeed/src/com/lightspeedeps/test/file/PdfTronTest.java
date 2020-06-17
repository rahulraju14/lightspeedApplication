package com.lightspeedeps.test.file;

import java.io.ByteArrayOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.ContentDAO;
import com.lightspeedeps.model.Content;
import com.lightspeedeps.test.SpringMultiTestCase;
import com.pdftron.common.PDFNetException;
import com.pdftron.fdf.FDFDoc;
import com.pdftron.fdf.FDFField;
import com.pdftron.fdf.FDFFieldIterator;
import com.pdftron.filters.Filter;
import com.pdftron.filters.FilterReader;
import com.pdftron.pdf.Convert;
import com.pdftron.pdf.ConvertPrinter;
import com.pdftron.pdf.Field;
import com.pdftron.pdf.FieldIterator;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFNet;
import com.pdftron.sdf.SDFDoc;

public class PdfTronTest extends SpringMultiTestCase{

	private static Log log = LogFactory.getLog(PdfTronTest.class);

	private class TestFile {
		public String inputFile;
		public String outputFile;
		@SuppressWarnings("unused")
		public boolean requiresWindowsPlatform;

		public TestFile(String inputFile, String outputFile,
				boolean requiresWindowsPlatform) {
			this.inputFile = inputFile;
			this.outputFile = outputFile;
			this.requiresWindowsPlatform = requiresWindowsPlatform;
		}
	}

public static boolean printerInstalled;

	// Relative path to the folder containing test files.
	public static String input_path = "./junit/data/TestFiles/";
	public static String output_path = "./junit/data/TestFiles/Output/";

	public TestFile[] testFiles = {
			new TestFile("simple-powerpoint_2007.pptx",
					"simple-powerpoint_2007.xod", true),
			new TestFile("simple-word_2007.docx", "simple-word_2007.xod", true),
			new TestFile("butterfly.png", "butterfly.xod", false),
			new TestFile("numbered.pdf", "numbered.xod", false),
			new TestFile("dice.jpg", "dice.xod", false),
			new TestFile("simple-xps.xps", "xps2pdf.xod", false) };

	public void bulkConvertRandomFilesToXod() {
		int err = 0;

		try {
			// See if the alternative printer is installed, the PDFNet printer
			// is installed, or if not try to install a printer
			if (ConvertPrinter.isInstalled("PDFTron PDFNet")) {
				ConvertPrinter.setPrinterName("PDFTron PDFNet");
				printerInstalled = true;
				System.out
						.println("PDFTron PDFNet Printer is already installed");
			} else if (ConvertPrinter.isInstalled()) {
				printerInstalled = true;
				System.out
						.println("PDFTron PDFNet Printer is already installed");
			} else {
				System.out
						.println("Installing printer (requires administrator and Windows platform)");
				// This will fail if not run as administrator. Harmless if
				// PDFNet
				// printer already installed
				ConvertPrinter.install();
				System.out.println("Installed printer "
						+ ConvertPrinter.getPrinterName());
				printerInstalled = true;
			}
		} catch (PDFNetException e) {
			System.out.println("Unable to install printer, error:");
			System.out.println(e);
		}
		for (int i=0; i<testFiles.length; ++i) {
			TestFile file=testFiles[i];
			try {
				if (Convert.requiresPrinter(file.inputFile)) {
					String osName = System.getProperty("os.name");
					if (osName.indexOf("Windows",0)==-1) {
						continue;
					}
					System.out.println("Using PDFNet printer to convert file "
							+ file.inputFile);
				}
				Convert.toXod(input_path + file.inputFile, output_path + file.outputFile);
				System.out.println("Converted file: " + file.inputFile
						+ " to :" + file.outputFile);

			} catch (PDFNetException e) {
				System.out.println("Unable to convert file: " + file.inputFile);
				System.out.println(e.toString());
				err = 1;
			}
		}
		if (err == 1) {
			System.out.println("ConvertFile failed");
		} else {
			System.out.println("ConvertFile succeeded");
		}

		// Uninstall the printer
		if (printerInstalled) {
			try {
				System.out
						.println("Uninstalling printer (requires administrator)");
				ConvertPrinter.uninstall();
				System.out.println("Uninstalled printer "
						+ ConvertPrinter.getPrinterName());
			} catch (PDFNetException e) {
				System.out.println("Unable to uninstall printer, error:");
				System.out.println(e);
			}
		}
	}

	public void teConvertToXOD() throws Exception {
		PDFNet.initialize();

		printerInstalled = false;

		System.out.println("-------------------------------------------------");

		try {

			Content ourDoc = null; // need to get Content from database (from Document)
			ourDoc = ContentDAO.getInstance().findByDocId(1, null); // (one of my docs' content)
			//log.debug(ourDoc.getContent().length);

			// Create PDFDoc ... either this way:
//			InputStream stream = new ByteArrayInputStream(ourDoc.getContent());
//			PDFDoc pdfDoc2 = new PDFDoc(stream);

			// or this way (shorter ... seems to work fine.)
			PDFDoc pdfDoc = new PDFDoc(ourDoc.getContent());

			Filter filter = Convert.toXod(pdfDoc);
			FilterReader filterRdr = new FilterReader(filter);

			// There might be a better way to do this ... read all bytes from filterRdr into a byte array ....
	        ByteArrayOutputStream bos = new ByteArrayOutputStream();
	        byte[] buf = new byte[1024];
	        try {
	            for (long readNum; (readNum = filterRdr.read(buf)) > 0;) {
	                // Write bytes from the byte array starting at offset 0 to the byte array output stream.
	                bos.write(buf, 0, (int)readNum);
	            }
	        } catch (PDFNetException ex) {
	        	// ?
	        }
	        byte[] bytes = bos.toByteArray();
	        log.debug(bytes.length);

	        // (Save bytes as XOD content)

/*
			// Sample 1:
			// Directly convert from PDF to XOD.
			Convert.toXod(input_path + "newsletter.pdf", output_path
					+ "from_pdf.xod");

			// Sample 2:
			// Directly convert from generic XPS to XOD.
			Convert.toXod(input_path + "simple-xps.xps", output_path
					+ "from_xps.xod");

			// Sample 3:
			// Convert from MS Office (does not require printer driver for
			// Office 2007+)
			// and other document formats to XOD.
			PdfTronTest test = new PdfTronTest();
			test.bulkConvertRandomFilesToXod();
*/
		} catch (PDFNetException e) {
			System.out
					.println("Unable to convert file document to XOD, error:");
			System.out.println(e);
		}

		PDFNet.terminate();
		System.out.println("Done.");
	}

	public void teConvertToXFDF() {

		PDFNet.initialize();

		// Example 1)
		// Iterate over all form fields in the document. Display all field names.
		try
		{
			PDFDoc doc = new PDFDoc((input_path + "form1.pdf"));
			doc.initSecurityHandler();

			for(FieldIterator itr = doc.getFieldIterator(); itr.hasNext();)
			{
				Field current=(Field)(itr.next());
				System.out.println("Field name: 169" + current.getName());
				System.out.println("Field partial name: 170 " + current.getPartialName());

				System.out.print("Field type: ");
				int type = current.getType();
				switch(type)
				{
				case Field.e_button: System.out.println("Button 176"); break;
				case Field.e_text: System.out.println("Text 177"); break;
				case Field.e_choice: System.out.println("Choice 178"); break;
				case Field.e_signature: System.out.println("Signature 179"); break;
				}

				System.out.println("------------------------------ 182");
			}

			doc.close();
			System.out.println("Done.");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		// Example 2) Import XFDF into FDF, then merge data from FDF into PDF
		try
		{
			// XFDF to FDF
			// form fields
			System.out.println("Import form field data from XFDF to FDF.");

			FDFDoc fdf_doc1 = FDFDoc.createFromXFDF((input_path + "form1_data.xfdf"));
			fdf_doc1.save((output_path + "form1_data.fdf"));

			// annotations
			System.out.println("Import annotations from XFDF to FDF.");

			FDFDoc fdf_doc2 = FDFDoc.createFromXFDF((input_path + "form1_annots.xfdf"));
			fdf_doc2.save((output_path + "form1_annots.fdf"));

			// FDF to PDF
			// form fields
			System.out.println("Merge form field data from FDF.");

			PDFDoc doc=new PDFDoc((input_path + "form1.pdf"));
			doc.initSecurityHandler();
			doc.fdfMerge(fdf_doc1);

			// To use PDFNet form field appearance generation instead of relying on
			// Acrobat, uncomment the following two lines:
			//doc.refreshFieldAppearances();
			//doc.getAcroForm().putBool("NeedAppearances", false);

			doc.save((output_path + "form1_filled.pdf"), SDFDoc.e_linearized, null);

			// annotations
			System.out.println("Merge annotations from FDF.");

			doc.fdfMerge(fdf_doc2);
			doc.save((output_path + "form1_filled_with_annots.pdf"), SDFDoc.e_linearized, null);
			doc.close();
			System.out.println("Done.");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		// Example 3) Extract data from PDF to FDF, then export FDF as XFDF
		try
		{
			// PDF to FDF
			PDFDoc in_doc = new PDFDoc((output_path + "form1_filled_with_annots.pdf"));
			in_doc.initSecurityHandler();

			// form fields only
			System.out.println("Extract form fields data to FDF.");

			FDFDoc doc_fields = in_doc.fdfExtract(PDFDoc.e_forms_only);
			doc_fields.setPDFFileName("../form1_filled_with_annots.pdf");
			doc_fields.save((output_path + "form1_filled_data.fdf"));

			// annotations only
			System.out.println("Extract annotations from FDF.");

			FDFDoc doc_annots = in_doc.fdfExtract(PDFDoc.e_annots_only);
			doc_annots.setPDFFileName("../form1_filled_with_annots.pdf");
			doc_annots.save((output_path + "form1_filled_annot.fdf"));

			// both form fields and annotations
			System.out.println("Extract both form fields and annotations to FDF.");

			FDFDoc doc_both = in_doc.fdfExtract(PDFDoc.e_both);
			doc_both.setPDFFileName("../form1_filled_with_annots.pdf");
			doc_both.save((output_path + "form1_filled_both.fdf"));

			// FDF to XFDF
			// form fields
			System.out.println("Export form field data from FDF to XFDF.");

			doc_fields.saveAsXFDF((output_path + "form1_filled_data.xfdf"));

			// annotations
			System.out.println("Export annotations from FDF to XFDF.");

			doc_annots.saveAsXFDF((output_path + "form1_filled_annot.xfdf"));

			// both form fields and annotations
			System.out.println("Export both form fields and annotations from FDF to XFDF.");

			doc_both.saveAsXFDF((output_path + "form1_filled_both.xfdf"));

			in_doc.close();
			System.out.println("Done.");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		// Example 4) Merge/Extract XFDF into/from PDF
		try
		{
			// Merge XFDF from string
			PDFDoc in_doc = new PDFDoc((input_path + "numbered.pdf"));
			in_doc.initSecurityHandler();

			System.out.println("Merge XFDF string into PDF.");

			String str = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><xfdf xmlns=\"http://ns.adobe.com/xfdf\" xml:space=\"preserve\"><square subject=\"Rectangle\" page=\"0\" name=\"cf4d2e58-e9c5-2a58-5b4d-9b4b1a330e45\" title=\"user\" creationdate=\"D:20120827112326-07'00'\" date=\"D:20120827112326-07'00'\" rect=\"227.7814207650273,597.6174863387978,437.07103825136608,705.0491803278688\" color=\"#000000\" interior-color=\"#FFFF00\" flags=\"print\" width=\"1\"><popup flags=\"print,nozoom,norotate\" open=\"no\" page=\"0\" rect=\"0,792,0,792\" /></square></xfdf>";

			FDFDoc fdoc = FDFDoc.createFromXFDF(str);
			in_doc.fdfMerge(fdoc);
			in_doc.save((output_path + "numbered_modified.pdf"), SDFDoc.e_linearized, null);
			System.out.println("Merge complete.");

			// Extract XFDF as string
			System.out.println("Extract XFDF as a string.");

			FDFDoc fdoc_new = in_doc.fdfExtract(PDFDoc.e_both);
			String XFDF_str = fdoc_new.saveAsXFDF();
			System.out.println("Extracted XFDF: ");
			System.out.println(XFDF_str);
			in_doc.close();
			System.out.println("Extract complete.");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		// Example 5) Read FDF files directly
		try
		{
			FDFDoc doc=new FDFDoc((output_path + "form1_filled_data.fdf"));

			for(FDFFieldIterator itr = doc.getFieldIterator(); itr.hasNext();)
			{
				FDFField current=(FDFField)(itr.next());
				System.out.println("Field name: 328" + current.getName());
				System.out.println("Field partial name: 329" + current.getPartialName());

				System.out.println("------------------------------");
			}
			doc.close();
			System.out.println("Done.");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		// Example 6) Direct generation of FDF.
		try
		{
			FDFDoc doc=new FDFDoc();
			// Create new fields (i.e. key/value pairs).
			doc.fieldCreate("Company", Field.e_text, "PDFTron Systems");
			doc.fieldCreate("First Name", Field.e_text, "John");
			doc.fieldCreate("Last Name", Field.e_text,  "Doe");
			// ...

			// doc.setPdfFileName("mydoc.pdf");

			doc.save((output_path + "sample_output.fdf"));
			doc.close();
			System.out.println("Done.");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		PDFNet.terminate();
	}

	public void testMergeXFDFIntoPDF() {
		PDFNet.initialize();
		try {
			Content content = ContentDAO.getInstance().findByDocId(1, null);
			PDFDoc in_doc = new PDFDoc(content.getContent());
			in_doc.initSecurityHandler();

			/*XfdfContent xfdfString = XfdfContentDAO.getInstance().findByContactDocId(1);
			FDFDoc fdoc = FDFDoc.createFromXFDF(xfdfString.getContent());
			in_doc.fdfMerge(fdoc);
			byte [] bytes = in_doc.save(SDFDoc.e_linearized, null);*/
			// bytes can be stored into mongo
			//System.out.println("Merge complete."+bytes.length);
			/*for (PageIterator pi = in_doc.getPageIterator(); pi.hasNext();) {
				Page page = (Page) pi.next();
				Obj annots = page.getAnnots();
				if (annots != null) {
					Field field = new Field(annots);
					field.getValueAsString();
					field.setFlag(Field.e_read_only, true);
				}
			}*/
			//PDFDoc out_doc = new PDFDoc(bytes);
			FieldIterator pdfitr = in_doc.getFieldIterator();
			/*FDFFieldIterator itr = fdoc.getFieldIterator();
			while(itr.hasNext()) {
				Field current = (Field) itr.next();
				if (current.getValueAsString().equals(FormFieldType.USER_FIRST_NAME.name())) {
					current.setFlag(Field.e_read_only, true);
					current.setValue("ANSHAJ");
				}
			}*/
			while (pdfitr.hasNext()) {
				Field current = (Field)pdfitr.next();
				System.out.println("field names "+current.getPartialName());
				if (current.getPartialName().equals("f1_1[0]")) {
					System.out.println(">>>>>>>>>>>>>>>>>>>>>");
					current.setFlag(Field.e_read_only, true);
					current.setValue("DWIGHT");
				}
			}
			byte[] bytes = in_doc.save(SDFDoc.e_linearized, null);
			in_doc.save((output_path + "sample_flatten_output.pdf"), 0, null);
			in_doc.close();
			System.out.println("xdfdf ");
			System.out.println("bytes length "+bytes.length);
			FDFDoc fdoc = new FDFDoc(bytes);
			String flatten = fdoc.saveAsXFDF();
			System.out.println("xdfdf "+flatten);
		} catch (PDFNetException e) {
			e.printStackTrace();
		}
		PDFNet.terminate();
	}
}
