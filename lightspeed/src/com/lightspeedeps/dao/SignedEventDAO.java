package com.lightspeedeps.dao;

import java.util.UUID;

import com.lightspeedeps.model.SignedEvent;
import com.lightspeedeps.model.User;
import com.lightspeedeps.util.app.SessionUtils;

public class SignedEventDAO<T> extends BaseTypeDAO<T> {

	@SuppressWarnings("rawtypes")
	public void initEvent(SignedEvent ev) {
		//TimecardEvent ev = new TimecardEvent(wtc, new Date());
		User user = SessionUtils.getCurrentUser();
		ev.setUserAccount(user.getAccountNumber());
		ev.setFirstName(user.getFirstName());
		ev.setLastName(user.getLastName());
		UUID u = UUID.randomUUID();
//		log.debug(u.getLeastSignificantBits() + " " + u.getMostSignificantBits() + " " + u.toString());
		ev.setUuid(u);
		//return ev;
	}

}
