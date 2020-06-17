<%--
The "logout" page -- invalidate the existing session, then redirect to the login page.
--%>
<%
	Boolean origin = (Boolean)session.getAttribute("com.lightspeedeps.OriginESS");
	session.invalidate();
	String jump = (String)request.getParameter("jump");
	if (jump != null) {
		// redirect elsewhere. Used for ESS logout. LS-3758
		response.sendRedirect(jump);
	}
	else {
		// if we were passed a "source" parameter, pass it along
		String source = (String)request.getParameter("source");
		if (source != null) {
			response.sendRedirect("login.jsf?source=" + source + "&or=" + origin);
		}
		else {
			String ref = (String)request.getParameter("ref");
			// if we were passed a "ref" parameter, pass it along
			if (ref != null) {
				response.sendRedirect("login.jsf?ref=" + ref);
			}
			else {
				response.sendRedirect("login.jsf");
			}
		}
	}
%>
