<%--
A page which immediately redirects to the ESS My Profile view.
Note that the redirect here has no effect; the redirect is done in PageAuthenticatePhaseListener.
But this page must exist, as it is the URL specified in faces-config, and the JSF navigation will ignore
the URL if the page does not exist.
--%>
<%
	response.sendRedirect("http://google.com/");
%>
