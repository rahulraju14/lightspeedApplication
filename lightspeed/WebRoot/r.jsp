<%--
A short-URL page (for email links) which immediately redirects to the reset-password-return page.
--%>
<%
		String key = request.getParameter("key");
    response.sendRedirect("jsp/login/resetpwreturn.jsf?key=" + key);
%>
