/**
 * Contains utility classes related to report generation.  This includes reports
 * that are generated directly to PDFs (e.g., Crew lists or stripboard
 * reports) as well as reports that have an online form (e.g., Callsheet, DPR).
 * It also includes classes that support generating Script printouts.
 * <p>
 * Note that the String constants in {@link com.lightspeedeps.util.report.ReportsConstants},
 * and the methods in {@link com.lightspeedeps.util.report.ReportScriptlet},
 * are not (generally) referenced by our Java code, but are used
 * by the Jasper report template files.
 */
package com.lightspeedeps.util.report;