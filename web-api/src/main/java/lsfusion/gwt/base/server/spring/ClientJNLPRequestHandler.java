package lsfusion.gwt.base.server.spring;

import lsfusion.base.IOUtils;
import lsfusion.interop.VMOptions;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;

import static lsfusion.base.BaseUtils.isRedundantString;

public class ClientJNLPRequestHandler implements HttpRequestHandler {
    protected final static Logger logger = Logger.getLogger(ClientJNLPRequestHandler.class);

    private static final PropertyPlaceholderHelper stringResolver = new PropertyPlaceholderHelper("${", "}", ":", true);
    private static final String DEFAULT_INIT_HEAP_SIZE = "32m";
    private static final String DEFAULT_MAX_HEAP_SIZE = "800m";
    private static final String DEFAULT_MAX_HEAP_FREE_RATIO = "70";

    @Autowired
    private BusinessLogicsProvider blProvider;

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.debug("Handling jnlp request");

        try {
            StringBuffer requestURL = request.getRequestURL();
            String codebaseUrl = requestURL.substring(0, requestURL.lastIndexOf("/"));
            VMOptions clientVMOptions;
            try {
                clientVMOptions = blProvider.getLogics().getClientVMOptions();
            } catch (Exception e) {
                //use default
                clientVMOptions = new VMOptions(null, null, null);
            }

            handleJNLPRequest(request, response, codebaseUrl, "client.jnlp", "lsFusion Client", request.getServerName(),
                              blProvider.getRegistryPort(), blProvider.getExportName(), blProvider.getLogics().isSingleInstance(),
                              clientVMOptions.getInitHeapSize(), clientVMOptions.getMaxHeapSize(), clientVMOptions.getMaxHeapFreeRatio());
        } catch (RemoteException e) {
            blProvider.invalidate();
            logger.debug("Error handling jnlp request: ", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error generating jnlp. Please, try again.");
        } catch (Exception e) {
            logger.debug("Error handling jnlp request: ", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Can't generate jnlp.");
        }
    }

    protected void handleJNLPRequest(HttpServletRequest request,
                                     HttpServletResponse response,
                                     String codebaseUrl,
                                     String jnlpUrl,
                                     String appName,
                                     String registryHost,
                                     int registryPort,
                                     String exportName) throws ServletException, IOException {
        handleJNLPRequest(request, response, codebaseUrl, jnlpUrl, appName, registryHost, registryPort, exportName, false, null, null, null);
    }

    protected void handleJNLPRequest(HttpServletRequest request,
                                     HttpServletResponse response,
                                     String codebaseUrl,
                                     String jnlpUrl,
                                     String appName,
                                     String registryHost,
                                     int registryPort,
                                     String exportName,
                                     boolean singleInstance,
                                     String initHeapSize,
                                     String maxHeapSize,
                                     String maxHeapFreeRatio) throws ServletException, IOException {
        logger.debug("Generating jnlp response.");

        try {

            Map<String, String> queryParams = getQueryParams(request);
            String jnlpMaxHeapSize = queryParams.get("maxHeapSize");

            Properties properties = new Properties();
            properties.put("jnlp.codebase", codebaseUrl);
            properties.put("jnlp.url", jnlpUrl);
            properties.put("jnlp.appName", appName);
            properties.put("jnlp.registryHost", registryHost);
            properties.put("jnlp.registryPort", String.valueOf(registryPort));
            properties.put("jnlp.exportName", exportName);
            properties.put("jnlp.singleInstance", String.valueOf(singleInstance));
            properties.put("jnlp.initHeapSize", !isRedundantString(initHeapSize) ? initHeapSize : DEFAULT_INIT_HEAP_SIZE);
            properties.put("jnlp.maxHeapSize", jnlpMaxHeapSize != null ? jnlpMaxHeapSize : (!isRedundantString(maxHeapSize) ? maxHeapSize : DEFAULT_MAX_HEAP_SIZE));
            properties.put("jnlp.maxHeapFreeRatio", !isRedundantString(maxHeapFreeRatio) ? String.valueOf(maxHeapFreeRatio) : DEFAULT_MAX_HEAP_FREE_RATIO);

            String content = stringResolver.replacePlaceholders(
                    IOUtils.readStreamToString(getClass().getResourceAsStream("/client.jnlp")), properties
            );

            response.setContentType("application/x-java-jnlp-file");
            response.getOutputStream().write(content.getBytes());
        } catch (Exception e) {
            logger.debug("Error handling jnlp request: ", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Can't generate jnlp.");
        }
    }

    private Map<String, String> getQueryParams(HttpServletRequest request) {
        final Map<String, String> queryParams = new HashMap<>();
        String queryString = request.getQueryString();
        if(queryString != null) {
            final String[] pairs = queryString.split("&");
            for (String pair : pairs) {
                if (pair.contains("=")) {
                    final int idx = pair.indexOf("=");
                    queryParams.put(pair.substring(0, idx), pair.length() > idx + 1 ? pair.substring(idx + 1) : null);
                }
            }
        }
        return queryParams;
    }
}
