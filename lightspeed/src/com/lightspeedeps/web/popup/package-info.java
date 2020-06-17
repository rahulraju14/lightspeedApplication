/**
 * Contains classes and interfaces providing several varieties
 * of pop-up dialog boxes. All of the classes extend
 * {@link com.lightspeedeps.web.popup.PopupBean}.
 * <p>
 * The users of these classes will generally implement
 * either {@link com.lightspeedeps.web.popup.PopupHolder} or
 * {@link com.lightspeedeps.web.popup.SelectContactsHolder}.
 * However, since {@link com.lightspeedeps.web.view.View}
 * implements {@link com.lightspeedeps.web.popup.PopupHolder},
 * and many of our beans are subclasses of {@link com.lightspeedeps.web.view.View View}, they
 * don't need to explicit specify "extends PopupHolder"
 * even though they make use of the interface.
 */
package com.lightspeedeps.web.popup;
