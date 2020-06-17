package com.lightspeedeps.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.RoleGroup;
import com.lightspeedeps.type.Permission;

import junit.framework.TestCase;

public class PermissionTest extends TestCase {

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(PermissionTest.class);

	public void testPermissionClass() throws Exception {

		Permission type1 = Permission.COMPARE_REVISIONS;
		Permission type2 = Permission.EDIT_CONTACTS_CAST;
		Permission type3 = Permission.APPROVE_PRODUCTION_REPORT;

		RoleGroup rg = new RoleGroup();

		long mask = type1.getMask() + type2.getMask();

		System.out.println(mask);
		rg.setPermissionMask(mask);

		assertTrue(rg.hasPermission(type1));
		assertTrue(rg.hasPermission(type2));
		assertTrue(! rg.hasPermission(type3));

		System.out.println("rg has: ");
		for (Permission p : rg.getPermissions()) {
			System.out.println(p.getName());
		}

		// role group 7 mask formula from spreadsheet:
		mask = (1<<40)+(1<<41)+(1<<28)+(1<<4)+(1<<27)+(1<<43)+(1<<44)+(1<<46)+(1<<0)+(1<<48)+(1<<45)+(1<<26)+(1<<47)+(1<<59)+(1<<42)+(1<<25)+0;

		rg.setPermissionMask(mask);

		System.out.println(mask);
		System.out.println("rg has: ");
		for (Permission p : Permission.values()) {
			if (rg.hasPermission(p)) {
				System.out.println(p.getName());
			}
		}
	}

}
