/**
 * File: AddressTest.java
 */
package com.lightspeedeps.test.script;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.AddressDAO;
import com.lightspeedeps.model.Address;
import com.lightspeedeps.test.SpringTestCase;

/**
 * This tests the functionality of the Address.getCityStateZip()
 * method.  Other tests related to the Address object could be added.
 */
public class AddressTest extends SpringTestCase {

	private static Log log = LogFactory.getLog(AddressTest.class);

	/**
	 * This tests the functionality of the Address.getCityStateZip()
	 * method.
	 */
	public void testAddressCityStateZip() throws Exception {

		final AddressDAO addressDAO = AddressDAO.getInstance();

		int done = 0;
		for (int i=1; i < 1000 && done < 100; i++) {
			Address address = addressDAO.findById(i);
			if (address != null) {
				done++;
				log.debug(address.getCityStateZip());
			}
		}

		assertTrue("No Address objects found.", done > 0);

	}

}
