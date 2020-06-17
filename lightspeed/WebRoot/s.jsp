<%--
A short-URL page (for email links) which immediately redirects to the Callsheet View page,
optionally passing a Unit parameter.
--%>
<%
		String unit = request.getParameter("u");
    response.sendRedirect("jsp/callsheet/view.jsf?u=" + unit);
%>
