package com.lightspeedeps.test.util;

import com.lightspeedeps.model.User;
import com.lightspeedeps.test.SpringTestCase;
import com.lightspeedeps.util.app.ApiUtils;
import com.lightspeedeps.util.app.ApplicationUtils;

public class UserTokenTest extends SpringTestCase {

	private int success = 0;

	public void testEmailValidator() throws Exception {

		ApplicationUtils.setInitializationParameter("authClientId", "ttcguest");

		oneTest( "L60005", true ); // trailing hyphen in domain label

		System.out.println(success + " successful tests completed.");
	}

	private void oneTest(String str, boolean result) {

		User user = new User();
		user.setAccountNumber(str);
		user.setId(0);

		String token = ApiUtils.getToken(user);

		User foundUser = ApiUtils.validateToken(token);
		assertEquals("input=`"+str+"` ", str, foundUser.getAccountNumber());

		boolean valid = ApiUtils.validateToken(user, token);

		assertEquals("input=`"+str+"` ", result, valid);
		success++;
	}

}
