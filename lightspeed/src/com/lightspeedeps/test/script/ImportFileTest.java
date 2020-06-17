//	File Name:	ImportFileTest.java
package com.lightspeedeps.test.script;

import com.lightspeedeps.importer.ImportFileImpl;
import com.lightspeedeps.importer.ImportSex;
import com.lightspeedeps.importer.ImportTagger;

/**
 * A class for testing file import in batch (command line) mode.
 */
public class ImportFileTest {

	public static void main(String[] args) {
		boolean includeText = true;
		boolean includeScriptElements = true;
		if ((args.length < 1) || (args.length > 3)) {
			System.out.println("Usage: java ImportFileTest "
					+ "[XML document filename] ([includeText] [includeScriptElements])");
			return;
		}

		// Load filename and options
		String filename = args[0];
		if (args.length > 1) {
			if (args[1].equalsIgnoreCase("false")) {
				includeText = false;
			}
			if (args.length > 2) {
				if (args[1].equalsIgnoreCase("false")) {
					includeScriptElements = false;
				}
			}
		}
		ImportFileImpl importer;
		if (filename.toLowerCase().indexOf(".sex") > 0) {
			importer = new ImportSex();
		}
		else {
			importer = new ImportTagger();
		}

		importer.batchImportFile(filename,
				includeText, /* include text */
				includeScriptElements /* include script elements */);
	}

}
