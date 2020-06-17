/**
 * SelectContactsHolder.java
 */
package com.lightspeedeps.web.popup;

import java.util.Collection;

import com.lightspeedeps.model.Contact;

/**
 * Implemented by callers of SelectContactsBean.
 */
public interface SelectContactsHolder {

	public void contactsSelected(int action, Collection<Contact> list);

}
