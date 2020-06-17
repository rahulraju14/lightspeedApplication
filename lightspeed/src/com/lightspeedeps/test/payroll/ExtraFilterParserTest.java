package com.lightspeedeps.test.payroll;

import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.object.RuleFilter;
import com.lightspeedeps.object.RuleFilterImpl;
import com.lightspeedeps.service.RuleService;
import com.lightspeedeps.type.RuleField;
import com.lightspeedeps.type.RuleOperator;

import junit.framework.TestCase;

/**
 * This class tests the RuleService code that parses the
 * "additional restrictions" (extra filters) text column in our contract rule
 * spreadsheets.
 *
 * @see RuleFilterImpl
 * @see RuleService
 */
public class ExtraFilterParserTest extends TestCase {
	private static final Log log = LogFactory.getLog(ExtraFilterParserTest.class);

	// tests for RuleService code that parses "additional restrictions" (extra filters)
	// in our contract rule spreadsheets.

	// TEST Methods
	// (Methods beginning with "test" are automatically run by JUnit)

	public void testFP1() throws Exception {
		RuleFilter rf[] = {
				new RuleFilterImpl(RuleField.CL, RuleOperator.GT, new BigDecimal(8)) };
		filterTestOne( "CL>8", rf );
	}

	public void testFP2() throws Exception {
		RuleFilter rf[] = {
				new RuleFilterImpl(RuleField.CL, RuleOperator.EQ, new BigDecimal(8)),
				new RuleFilterImpl(RuleField.FR, RuleOperator.NE, new Boolean(true)) };
		filterTestOne( "CL=8;FR!=Y", rf );
	}

	public void testFP3() throws Exception {
		RuleFilter rf[] = {
				new RuleFilterImpl(RuleField.CL, RuleOperator.NL, Boolean.TRUE),
				new RuleFilterImpl(RuleField.DT, RuleOperator.NE, "WORK"),
				new RuleFilterImpl(RuleField.EH, RuleOperator.GT, new BigDecimal(8)),
				new RuleFilterImpl(RuleField.EX, RuleOperator.NE, Boolean.FALSE),
				new RuleFilterImpl(RuleField.EXHR, RuleOperator.LE, new BigDecimal(0.5)),
				new RuleFilterImpl(RuleField.FR, RuleOperator.EQ, Boolean.TRUE),
				new RuleFilterImpl(RuleField.FT, RuleOperator.NE, "F"),
				new RuleFilterImpl(RuleField.LO, RuleOperator.EQ, "S"),
				new RuleFilterImpl(RuleField.MJ, RuleOperator.EQ, Boolean.TRUE),
				new RuleFilterImpl(RuleField.MXW, RuleOperator.NE, Boolean.TRUE),
				new RuleFilterImpl(RuleField.NXH, RuleOperator.EQ, Boolean.FALSE),
				new RuleFilterImpl(RuleField.PHW, RuleOperator.GE, new BigDecimal(12.5)),
				new RuleFilterImpl(RuleField.PH, RuleOperator.EQ, "W"),
				new RuleFilterImpl(RuleField.PWDP, RuleOperator.GT, new Integer(1)),
				new RuleFilterImpl(RuleField.SH, RuleOperator.EQ, Boolean.FALSE),
				new RuleFilterImpl(RuleField.TO, RuleOperator.EQ, new BigDecimal(0)),
				new RuleFilterImpl(RuleField.TP, RuleOperator.ME, "(P,1)"),
				new RuleFilterImpl(RuleField.WD, RuleOperator.CO, "W"),
				new RuleFilterImpl(RuleField.WDN, RuleOperator.NL, Boolean.FALSE),
				new RuleFilterImpl(RuleField.WR, RuleOperator.GE, new BigDecimal(33)),
				new RuleFilterImpl(RuleField.WRKD, RuleOperator.LT, new Integer(5)),
				};

		filterTestOne( "CL !!true;" +
				"DT!=work;" +
				"EH>8;" +
				"EX!=false;" +
				"EXHR<=0.5;" +
				"FR = y;" +
				"FT !=F;" +
				"LO=S;" +
				"MJ=TRUE;" +
				"MXW != True;" +
				"NXH=false;" +
				"PHW>= 12.5;" +
				"PH=W;" +
				"PWDP>1;" +
				"SH = FALSE;" +
				"TO = 0.0;" +
				"TP : (P,1);" +
				"WD<>w;" +
				"WDN !! false;" +
				"WR >= 33.0;" +
				"WRKD <5", rf );
	}


	// SUPPORTING METHODS -- NOT automatically run by JUnit

	/**
	 * Parse the filter text "filter", and see if the resulting List matches the
	 * supplied array of RuleFilter`s.
	 *
	 * @param filterText The textual filter string to be parsed.
	 * @param rf The array of RuleFilter`s that should match the List returned
	 *            by our parser.
	 */
	private void filterTestOne(String filterText, RuleFilter[] rf) {

		List<RuleFilter> rules = RuleService.testFilterParsing(filterText);

		assertEquals("Number of parsed rules", rf.length, rules.size());

		int ix = 0;
		for (RuleFilter filter : rules) {
			try {
				assertEquals("field in rule " + ix, rf[ix].getField(), filter.getField());
				assertEquals("operator in rule " + ix, rf[ix].getOperator(), filter.getOperator());

				if (rf[ix].getValue() instanceof BigDecimal) {
					assertTrue(
							"value in rule ",
							((BigDecimal)rf[ix].getValue()).compareTo((BigDecimal)filter.getValue()) == 0);
				}
				else {
					assertEquals("value in rule " + ix, rf[ix].getValue(), filter.getValue());
				}
				ix++;
			}
			catch (Exception e) {
				log.error("ix=" + ix);
				log.error("filter=" + filter);
			}
		}
	}

}
