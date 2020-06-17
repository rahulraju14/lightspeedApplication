<%--
The "expired" page, which redirects to the login page with 'ex=1' to trigger the "session expired" message.
--%>
<%
	session.invalidate();
	response.sendRedirect("../../login.jsf?ex=1");
%>
