package edu.ucsb.nceas.osti_elink.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import edu.ucsb.nceas.osti_elink.OSTIElinkException;

/**
 * HttpService provides HTTP client functionality for communicating with web services.
 * It handles GET, PUT, POST, and DELETE requests with proper header management.
 */
public class HttpService implements AutoCloseable {

    /**
     * Enum representing HTTP request methods
     */
    public enum RequestMethod {
        GET, PUT, POST, DELETE
    }

    private static final int CONNECTIONS_PER_ROUTE = 8;
    private static final int MAX_TOTAL_CONNECTIONS = 20;

    protected final CloseableHttpClient httpClient;
    protected static final Log log = LogFactory.getLog(HttpService.class);

    /**
     * Constructor
     */
    public HttpService() {
        httpClient = createThreadSafeClient();
    }

    /**
     * Generate an HTTP Client for communicating with web services that is
     * thread safe and can be used in the context of a multi-threaded application.
     * @return CloseableHttpClient
     */
    private static CloseableHttpClient createThreadSafeClient() {
        PoolingHttpClientConnectionManager poolingConnManager = new PoolingHttpClientConnectionManager();
        poolingConnManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
        poolingConnManager.setDefaultMaxPerRoute(CONNECTIONS_PER_ROUTE);
        return HttpClients.custom().setConnectionManager(poolingConnManager).build();
    }

    /**
     * Send an HTTP request without a request body.
     * @param requestMethod the type of the HTTP method
     * @param uri endpoint to be accessed in the request
     * @return byte[] containing the response body
     * @throws OSTIElinkException if there's an error during the request
     */
    protected byte[] sendRequest(RequestMethod requestMethod, String uri) throws OSTIElinkException {
        return sendRequest(requestMethod, uri, null);
    }

    /**
     * Send an HTTP request with a request body (for POST and PUT requests).
     * @param requestMethod the type of the HTTP method
     * @param uri endpoint to be accessed in the request
     * @param requestBody the String body to be encoded into the body of the request
     * @return byte[] containing the response body
     * @throws OSTIElinkException if there's an error during the request
     */
    protected byte[] sendRequest(RequestMethod requestMethod, String uri, String requestBody) throws OSTIElinkException {
        HttpUriRequest request;
        log.debug("HttpService.sendRequest - Trying uri: " + uri);

        switch (requestMethod) {
            case GET:
                request = new HttpGet(uri);
                setGetHeaders(request);
                break;
            case PUT:
                request = new HttpPut(uri);
                if (requestBody != null && !requestBody.isEmpty()) {
                    StringEntity myEntity = new StringEntity(requestBody, StandardCharsets.UTF_8);
                    ((HttpPut) request).setEntity(myEntity);
                }
                setHeaders(request, uri);
                break;
            case POST:
                request = new HttpPost(uri);
                if (requestBody != null && !requestBody.isEmpty()) {
                    StringEntity myEntity = new StringEntity(requestBody, StandardCharsets.UTF_8);
                    ((HttpPost) request).setEntity(myEntity);
                }
                setHeaders(request, uri);
                break;
            case DELETE:
                request = new HttpDelete(uri);
                setHeaders(request, uri);
                break;
            default:
                throw new OSTIElinkException("Unrecognized HTTP method requested.");
        }

        try {
            HttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                return EntityUtils.toByteArray(entity);
            }
            return new byte[0];
        } catch (ClientProtocolException e) {
            throw new OSTIElinkException("Protocol error: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new OSTIElinkException("I/O error: " + e.getMessage(), e);
        }
    }

    /**
     * This method will add headers for HTTP requests.
     * Subclasses should override this method to add specific headers.
     * @param request the request will be modified.
     * @param url the url will be sent
     */
    protected void setHeaders(HttpUriRequest request, String url) {
        // Default implementation - subclasses should override
        request.setHeader("Content-Type", "application/xml");
        request.setHeader("Accept", "application/xml");
    }

    /**
     * This method will add headers for HTTP GET requests.
     * Subclasses should override this method to add specific headers.
     * @param request the request will be modified.
     */
    protected void setGetHeaders(HttpUriRequest request) {
        // Default implementation - subclasses should override
        request.setHeader("Accept", "application/xml");
    }

    /**
     * Close the HTTP client to release resources
     */
    @Override
    public void close() {
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException e) {
                log.error("Error closing HTTP client: " + e.getMessage(), e);
            }
        }
    }
}