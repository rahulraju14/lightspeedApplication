package com.lightspeedeps.test.payroll;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.*;
import com.lightspeedeps.model.*;
import com.lightspeedeps.test.SpringTestCase;

/**
 * This code currently iterates over StartForms from one production and updates
 * them, setting the Social Security number to one that starts with a "9" (and
 * is therefore invalid).
 * <p>
 * See {@link com.lightspeedeps.test.RunSomethingTest} for an example of running
 * this class' methods as a JUnit test.
 */
public class StartFormUpdate extends SpringTestCase {
	private static final Log log = LogFactory.getLog(StartFormUpdate.class);

	private int totSf = 0;
	private int totTc = 0;

	/** Default constructor */
	public StartFormUpdate() {
		log.debug("");
	}

	/**
	 * Called from a JUnit test case. The method name (testExecute) is arbitrary.
	 */
	public void testExecute() {
		log.debug("");
		try {
			updateStartSsn();
			//updateTimecardSsn();
		}
		catch (Exception ex) {
			log.error("Exception: ", ex);
		}
	}

	private void updateStartSsn() {
		for (int p = 1; p < 1010; p++ ) {
			if (p != 32) {
				Production prod = ProductionDAO.getInstance().findById(p); // set to appropriate ID for test run
				if (prod != null) {
					updateStartSsnProd(prod);
					updateTimecardSsn(prod);
				}
			}
		}
		log.debug(totSf + " SF updates");
		log.debug(totTc + " TC updates");

	}

	private void updateStartSsnProd(Production prod) {
		StartFormDAO sfDAO = StartFormDAO.getInstance();
		List<StartForm> list = sfDAO.findByProduction(prod); //sfDAO.findAll();

		int i=0;
		for (StartForm sf : list) {
			if (sf.getSocialSecurity() != null) {
				String ss = sf.getSocialSecurity();
				if (ss.length() == 0) {
					continue;
				}
				if (ss.length() != 9) {
					log.debug(ss);
					ss = ss.replaceAll("-", "");
					log.debug(ss);
				}
				if (! ss.startsWith("9")) {
					ss = "9" + ss.substring(1);
					sf.setSocialSecurity(ss);
					sfDAO.attachDirty(sf);
					i++;
				}
			}
		}
		log.debug(i + " SF updates; p=" + prod.getTitle());
		totSf += i;
	}

	private void updateTimecardSsn(Production prod) {
		WeeklyTimecardDAO wtcDAO = WeeklyTimecardDAO.getInstance();
		//List<WeeklyTimecard> list = wtcDAO.findAll();
		List<WeeklyTimecard> list2 = wtcDAO.findByProduction(prod);

		int i=0;
		for (WeeklyTimecard wtc : list2) {
			if (wtc.getSocialSecurity() != null) {
				String ss = wtc.getSocialSecurity();
				if (ss.length() == 0) {
					continue;
				}
				if (ss.length() != 9) {
					log.debug(ss);
					ss = ss.replaceAll("-", "");
					log.debug(ss);
				}
				if (! ss.startsWith("9")) {
					ss = "9" + ss.substring(1);
					wtc.setSocialSecurity(ss);
					wtcDAO.attachDirty(wtc);
					i++;
				}
			}
		}
		log.debug(i + " TC updates");
		totTc += i;
	}

}
