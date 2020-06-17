/**
 * Contains classes and interfaces which are superclasses of many
 * of our bean classes.
 * <p>
 * {@link com.lightspeedeps.web.view.View} is the superclass of almost all
 * of our backing beans (for our web pages).  The backing beans most commonly
 * extend one of its subclasses:
 *  {@link com.lightspeedeps.web.view.ListImageView} for pages presenting
 *  lists of items which may have related images (such as Cast members), or
 *  {@link com.lightspeedeps.web.view.ListView} for pages presenting
 *  lists of items that do NOT have related images (such as Projects).
 *  <p>
 *  Beans backing pages that do not have a list, but pertain to only a single
 *  item or facility (like Permissions) directly extend either {@link com.lightspeedeps.web.view.View}
 *  or {@link com.lightspeedeps.web.view.ImageView} -- the latter in the
 *  case where the item may have related images (such as the My Account page).
 *  <p>
 *  Two somewhat unusual beans, {@link com.lightspeedeps.web.schedule.StripBoardViewBean}
 *  and {@link com.lightspeedeps.web.schedule.StripBoardEditBean}, extend
 *  the {@link com.lightspeedeps.web.view.SortableList} class, and manage their
 *  lists through its methods.  Most of the other beans manage their lists via methods
 *  in the {@link com.lightspeedeps.web.view.ListView} class.
 */
package com.lightspeedeps.web.view;
