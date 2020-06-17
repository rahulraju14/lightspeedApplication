/**
 * Contains classes and interfaces related to Script management functions.
 * This includes backing beans for the Breakdown page, the Script Revisions
 * page, and all the Script Import pages (which are stepped through during
 * a Script import process).
 * <p>
 * Included is the code that formats portions of a Script for on-screen
 * display ({@link com.lightspeedeps.web.script.ScriptPageBean}) on both the
 * Breakdown and Script Revisions pages, plus the code that reformats
 * a Script which was imported from an FDX (Final Draft) file
 * ({@link com.lightspeedeps.web.script.ScriptFormatter}).  Note, however,
 *  that the code which formats a Script for printing to a PDF is not
 *  here, but in {@link com.lightspeedeps.util.report.ScriptReporter}.
 */
package com.lightspeedeps.web.script;
