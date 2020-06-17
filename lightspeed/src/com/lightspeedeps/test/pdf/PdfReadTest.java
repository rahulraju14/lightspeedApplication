package com.lightspeedeps.test.pdf;

import java.util.*;

import com.lowagie.text.pdf.*;

import junit.framework.TestCase;

public class PdfReadTest extends TestCase {

	private final static String PATH =  "D:\\Dev\\Studio\\samples\\scripts\\"; // "c:\\tmp\\";
	private final static String FILETYPE = ".pdf";

	private int indent = 0;

	public void testPdfReader() throws Exception {

		String inFile = "30 page script LS eps-DH";

		PdfReader rdr = new PdfReader(PATH + inFile + FILETYPE);
		dumpRdr(rdr);
	}

	private void dumpRdr(PdfReader rdr) throws Exception {
		int pn = rdr.getNumberOfPages();
		log(pn + " pages.");
		dumpPage(rdr, 5);
	}

	private void dumpPage(PdfReader rdr, int pagenum) throws Exception {
		byte[] b = rdr.getPageContent(pagenum);
		log("page " + pagenum + "; len=" + b.length );
		PdfDictionary d = rdr.getPageN(pagenum);
		dumpDictionary(d, rdr);
	}

	@SuppressWarnings("rawtypes")
	private void dumpDictionary(PdfDictionary dict, PdfReader rdr) throws Exception {
		Set keys = dict.getKeys();
		log("dictionary:");
		for (Object o : keys) {
			PdfObject po = dict.get((PdfName)o);
			log("key: " + o + ", data=" + po.getClass().getName() + ":" + po);
			dumpObject(po, rdr);
		}
	}

	private void dumpObject(PdfObject po, PdfReader rdr) throws Exception {
		indent++;
		if ( po instanceof PRIndirectReference) {
			PRIndirectReference pi = (PRIndirectReference)po;
			PdfObject o = PdfReader.getPdfObject(pi);
			log("==>"+ o.getClass().getName() + ":" + o);
			dumpObject(o, rdr);
		}
		if ( po instanceof PdfDictionary) {
			dumpDictionary((PdfDictionary)po, rdr);
			if (po instanceof PRStream) {
				PRStream pr = (PRStream)po;
				log("len=" + pr.getLength());
				System.out.println(pr.getBytes());
			}
		}
		indent--;
	}

	private void log(Object o) {
		for (int i=indent; i > 0; i--) {
			System.out.print("  ");
		}
		System.out.println(o);
	}

}
