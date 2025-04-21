package edu.ucsb.nceas.osti_elink;

import edu.ucsb.nceas.osti_elink.utils.HttpService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * OSTIElinkService provides access to the identifier service maintained by OSTI.
 * Please see the documentation at https://www.osti.gov/elink/241-6api.jsp
 */
public abstract class OSTIElinkService {
    private static final String DOI = "doi";
    private static final String OSTI_ID = "osti_id";

    private String apiToken;
    private String baseURL;
    protected static Log log = LogFactory.getLog(OSTIElinkService.class);

    // HttpService instance
    protected HttpService httpService;

    public OSTIElinkService(String apiToken, String baseURL) {
        this.apiToken = apiToken;

        if (baseURL != null && !baseURL.trim().isEmpty()) {
            this.baseURL = baseURL;
        }

        // Initialize HttpService
        this.httpService = new HttpService(apiToken);
    }

    /**
     * Get the base URL for the OSTI service
     * @return the base URL
     */
    public String getBaseURL() {
        return baseURL;
    }

    /**
     * Get the HTTP service used by this OSTI service
     * @return the HTTP service
     */
    public HttpService getHttpService() {
        return httpService;
    }

    /**
     * Parse the query response to the osti_id for the given doi
     * @param metadata  the query response
     * @param doi  the doi
     * @return the osti id for the doi
     * @throws OSTIElinkException
     */
    protected abstract String parseOSTIidFromResponse(String metadata, String doi)
            throws OSTIElinkException;

    /**
     * The method to handle the publishIdentifier action
     * @param ostiId the OSTI ID for the DOI
     * @param siteUrl the site URL where the DOI should resolve
     * @throws OSTIElinkException
     */
    protected abstract void handlePublishIdentifierCommand(String ostiId, String siteUrl)
            throws OSTIElinkException;
}