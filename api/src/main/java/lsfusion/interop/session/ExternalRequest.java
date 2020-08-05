package lsfusion.interop.session;

import java.io.Serializable;

public class ExternalRequest implements Serializable {

    private static final long serialVersionUID = -1029191636149626916L;
    public final String[] returnNames;
    public Object[] params;

    public final String charsetName;
    public final String[] headerNames;
    public final String[] headerValues;
    public final String[] cookieNames;
    public final String[] cookieValues;

    public final String appHost;
    public final Integer appPort;
    public final String exportName;

    public final String scheme;
    public final String webHost;
    public final Integer webPort;
    public final String contextPath;
    public final String servletPath;
    public final String query;

    public ExternalRequest() {
        this(new Object[0]);
    }

    public ExternalRequest(Object[] params) {
        this(new String[0], params);    
    }

    public ExternalRequest(String[] returnNames, Object[] params) {
        this(returnNames, params, "utf-8");
    }

    public ExternalRequest(String[] returnNames, Object[] params, String charsetName) {
        this(returnNames, params, charsetName, null, new String[0], new String[0], null,
                null, null, null, null, null, null, null, null, null);
    }

    public ExternalRequest(String query) {
        this(new String[0], new Object[0], "utf-8", query, new String[0], new String[0], null,
                null, null, null, null, null, null, null, null, null);
    }

    public ExternalRequest(String[] returnNames, Object[] params, String charsetName, String query,
                           String[] headerNames, String[] headerValues, String[] cookieNames, String[] cookieValues,
                           String appHost, Integer appPort, String exportName, String scheme, String webHost,
                           Integer webPort, String contextPath, String servletPath) {
        this.returnNames = returnNames;
        this.params = params;
        this.charsetName = charsetName;
        this.headerNames = headerNames;
        this.headerValues = headerValues;
        this.cookieNames = cookieNames;
        this.cookieValues = cookieValues;
        this.query = query;
        this.appHost = appHost;
        this.appPort = appPort;
        this.exportName = exportName;
        this.scheme = scheme;
        this.webHost = webHost;
        this.webPort = webPort;
        this.contextPath = contextPath;
        this.servletPath = servletPath;
    }
}
