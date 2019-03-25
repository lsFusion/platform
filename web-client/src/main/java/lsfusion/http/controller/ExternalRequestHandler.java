package lsfusion.http.controller;

import lsfusion.interop.session.ExternalUtils;
import lsfusion.interop.logics.LogicsRunnable;
import lsfusion.interop.logics.LogicsSessionObject;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.json.JSONObject;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.rmi.RemoteException;

import static lsfusion.base.ServerMessages.getString;

public class ExternalRequestHandler extends LogicsRequestHandler implements HttpRequestHandler {

    protected void handleRequest(LogicsSessionObject sessionObject, HttpServletRequest request, HttpServletResponse response) throws RemoteException {
    }

    @Override
    public void handleRequest(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        try {
            runRequest(request, new LogicsRunnable<Object>() {
                @Override
                public Object run(LogicsSessionObject sessionObject) throws RemoteException {
                    handleRequest(sessionObject, request, response);
                    return null;
                }
            });
        } catch (RemoteException e) { // will suppress that error, because we rethrowed it when handling request (see above)
        }
    }

    protected void sendOKResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        sendResponse(request, response, getString(request, "executed.successfully"), Charset.forName("UTF-8"), false, false);
    }

    protected void sendErrorResponse(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        JSONObject messageObject = new JSONObject();
        messageObject.put("message", message);
        sendResponse(request, response, message, Charset.forName("UTF-8"),  true, true);
    }

    protected void sendResponse(HttpServletRequest request, HttpServletResponse response, String message, Charset charset, boolean error, boolean accessControl) throws IOException {
        sendResponse(request, response, new ExternalUtils.ExternalResponse(new StringEntity(message, charset), null, null, null, null, null), error, accessControl);
    }

    // copy of ExternalHTTPServer.sendResponse
    protected void sendResponse(HttpServletRequest request, HttpServletResponse response, ExternalUtils.ExternalResponse responseHttpEntity, boolean error, boolean accessControl) throws IOException {
        HttpEntity responseEntity = responseHttpEntity.response;
        Header contentType = responseEntity.getContentType();
        String contentDisposition = responseHttpEntity.contentDisposition;
        String[] headerNames = responseHttpEntity.headerNames;
        String[] headerValues = responseHttpEntity.headerValues;
        String[] cookieNames = responseHttpEntity.cookieNames;
        String[] cookieValues = responseHttpEntity.cookieValues;

        boolean hasContentType = false; 
        boolean hasContentDisposition = false;
        if(headerNames != null) {
            for (int i = 0; i < headerNames.length; i++) {
                String headerName = headerNames[i];
                if (headerName.equals("Content-Type")) {
                    hasContentType = true;
                    response.setContentType(headerValues[i]);
                } else {
                    response.addHeader(headerName, headerValues[i]);
                }
                hasContentDisposition = hasContentDisposition || headerName.equals("Content-Disposition");
            }
        }

        if(cookieNames != null) {
            for (int i = 0; i < cookieNames.length; i++) {
                response.addCookie(new Cookie(cookieNames[i], cookieValues[i]));
            }
        }

        if(contentType != null && !hasContentType)
            response.setContentType(contentType.getValue());
        if(contentDisposition != null && !hasContentDisposition)
            response.addHeader("Content-Disposition", contentDisposition);        
        response.setStatus(error ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : HttpServletResponse.SC_OK);
        if(accessControl) {
            //marks response as successful for js request
            response.addHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
            //allows to use cookies for js request
            response.addHeader("Access-Control-Allow-Credentials", "true");
        }
        responseEntity.writeTo(response.getOutputStream());
    }
}