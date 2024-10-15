package edu.ucsb.nceas.osti_elink.v1;

import edu.ucsb.nceas.osti_elink.OSTIElinkService;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpUriRequest;

/**
 * The OSTI service supports the v1 API. It extends from the OSTIElinkService class.
 * @author Tao
 */
public class OSTIService extends OSTIElinkService {

    /**
     * Constructor
     * @param username  the username of the account which can access the OSTI service
     * @param password  the password of the account which can access the OSTI service
     * @param baseURL  the url which specifies the location of the OSTI service
     */
    public OSTIService(String username, String password, String baseURL) {
        super(username, password, baseURL);
    }

    /**
     * Add the basic authentication method for the v1 requests
     * @param request  the request needs to be added headers
     */
    @Override
    protected void setHeaders(HttpUriRequest request) {
        request.addHeader("Accept", "application/xml");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + new String(encodedAuthStr));
    }
}
