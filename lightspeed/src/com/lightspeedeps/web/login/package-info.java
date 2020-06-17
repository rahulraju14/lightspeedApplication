/**
 * Contains classes which are related to logging in, authorization,
 * and passwords.  It includes the backing beans for the login
 * page and other pages that are accessed before logging
 * in, such as "Password Reset".  The
 * {@link com.lightspeedeps.web.login.AuthorizationBean} is
 * also here; it is a session-scoped bean used during login
 * and throughout many of our pages to manage access to
 * data based on the user's permissions (privileges).
 */
package com.lightspeedeps.web.login;
