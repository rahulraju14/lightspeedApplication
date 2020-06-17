<%--
The "timedOut" page, which invalidates the session, then 
redirects to the login page with 'ex=1' to trigger the "session expired" message.
--%>
<%
	String ref = (String)session.getAttribute("com.lightspeedeps.brandServiceRef");
	session.invalidate();
	// if session has a "ref" parameter (for branding), pass it along
	if (ref != null) {
	    response.sendRedirect("../../login.jsf?ex=1&ref=" + ref);
	}
	else {
	    response.sendRedirect("../../login.jsf?ex=1");
	}
%>
