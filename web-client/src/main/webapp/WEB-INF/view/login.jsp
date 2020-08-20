<%@ page import="lsfusion.base.ServerMessages" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page isELIgnored="false" %>

<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <title>${title}</title>
        <link rel="shortcut icon" href="${logicsIcon}" />
        <link rel="stylesheet" media="only screen and (min-device-width: 601px)" href="static/noauth/css/login.css"/>
        <link rel="stylesheet" media="only screen and (max-device-width: 600px)" href="static/noauth/css/mobile_login.css"/>
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css">
    </head>
    <body onload="document.loginForm.username.focus();">

        <table class="content-table">
            <tr></tr>
            <tr valign="bottom">
                <td>
                    <div class="text-center">
                        <div class="text-center">
                            <div class="desktop-link">${jnlpUrls}</div>
                        </div>
                    </div>
                </td>
            </tr>
            <tr>
                <td>
                    <div id="content">

                        <%
                            String query = request.getQueryString();
                            String queryString = query == null || query.isEmpty() ? "" : ("?" + query);
                        %>

                        <div class="image-center">
                            <img id="logo" class="logo" src="${logicsLogo}" alt="LSFusion">
                        </div>
                        
                        <form id="login-form"
                              name="loginForm"
                              method="POST"
                              action="login_check<%=queryString%>" >
                            <fieldset>
                                <p>
                                    <br/>
                                    <label for="username"><%= ServerMessages.getString(request, "login") %></label>
                                    <input type="text" id="username" name="username" class="round full-width-box"/>
                                </p>
                                <p>
                                    <label for="password"><%= ServerMessages.getString(request, "password") %></label>
                                    <input type="password" id="password" name="password" class="round full-width-box"/>
                                </p>
                                <input name="submit" type="submit" class="button round blue" value="<%= ServerMessages.getString(request, "sign.in") %>"/>
                                <c:if test="${not empty SPRING_SECURITY_LAST_EXCEPTION}">
                                    <%
                                        if (session.getAttribute("SPRING_SECURITY_LAST_EXCEPTION") instanceof Exception) {
                                            session.setAttribute("SPRING_SECURITY_LAST_EXCEPTION", ((Exception) session.getAttribute("SPRING_SECURITY_LAST_EXCEPTION")).getMessage());
                                        }
                                    %>
                                    <div class="errorblock round full-width-box">
                                        ${SPRING_SECURITY_LAST_EXCEPTION}
                                    </div>
                                    <c:remove var="SPRING_SECURITY_LAST_EXCEPTION" scope="session"/>
                                </c:if>
                            </fieldset>
                        </form>
                        <div class="reg-block">
                            <br>
                                <a class="registration" href="${registrationPage}"><%= ServerMessages.getString(request, "registration") %></a>
                                <a class="forgot-password" href="${forgotPasswordPage}"><%= ServerMessages.getString(request, "password.forgot") %></a>
                        </div>
                    </div>
                </td>
            </tr>
            <tr valign="top">
                <td>
                    <div class="text-center">
                        <c:forEach var="url" items="${urls}">
                            <a href="${url.value}" class="text-center fa fa-${url.key}"></a>
                        </c:forEach>
                    </div>
                </td>
            </tr>
            <tr></tr>
        </table>
    </body>
</html>