/**
 * File: TimecardCreatorTest.java
 */
package com.lightspeedeps.test.payroll;

import com.lightspeedeps.batch.FormI9Check;
import com.lightspeedeps.test.SpringTestCase;
import com.lightspeedeps.util.app.Constants;


/**
 * Test the "FormI9Check" class, which notifies approvers of I-9's awaiting
 * approval. That class is normally run as a scheduled task, e.g., on a daily
 * basis, to inform all approvers about the pending forms.
 */
public class FormI9CheckTest extends SpringTestCase {

	FormI9Check bean;

	public void testFormI9Check() throws Exception {

		bean = (FormI9Check)ctx.getBean("formI9Check");

		String result = bean.execute();

		assertEquals("test failed", Constants.SUCCESS, result);

	}

}
