package edu.ucsb.nceas.osti_elink.v1;

import edu.ucsb.nceas.osti_elink.OSTIElinkException;
import edu.ucsb.nceas.osti_elink.OSTIElinkService;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpUriRequest;
import org.w3c.dom.Document;

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
     * @param url the url which will be sent
     */
    @Override
    protected void setHeaders(HttpUriRequest request, String url) {
        request.addHeader("Accept", "application/xml");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + new String(encodedAuthStr));
    }

    @Override
    protected void setGetHeaders(HttpUriRequest request) {
        setHeaders(request, null);
    }

    @Override
    protected String parseOSTIidFromResponse(String metadata, String doi)
        throws OSTIElinkException {
        if (metadata == null || metadata.trim().equals("")) {
            throw new OSTIElinkException("The service can't parse the blank response to get the "
                                             + "OSTI id for the DOI " + doi);
        } else {
            Document doc = generateDOM(metadata.getBytes());
            return getElementValue(doc, OSTI_ID);
        }
    }

    @Override
    protected void handlePublishIdentifierCommand(String ostiId, String siteUrl) {
        // Do nothing.
        log.debug("V1 service does nothing in the handlePublishIdentifierCommand since it should "
                      + "never be called");
    }

}
