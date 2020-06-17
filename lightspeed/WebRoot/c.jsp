<%--
A short-URL page (for email links) which immediately redirects to the Calendar View page.
--%>
<%
		String unit = request.getParameter("u");
    response.sendRedirect("jsp/schedule/calendarview.jsf?u=" + unit);
%>
