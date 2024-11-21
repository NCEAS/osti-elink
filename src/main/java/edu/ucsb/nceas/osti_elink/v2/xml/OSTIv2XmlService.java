package edu.ucsb.nceas.osti_elink.v2.xml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.ucsb.nceas.osti_elink.OSTIElinkException;
import edu.ucsb.nceas.osti_elink.OSTIElinkNotFoundException;
import edu.ucsb.nceas.osti_elink.OSTIElinkService;
import edu.ucsb.nceas.osti_elink.OSTIServiceFactory;
import edu.ucsb.nceas.osti_elink.exception.PropertyNotFound;
import edu.ucsb.nceas.osti_elink.v2.response.JsonResponseHandler;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * This class represents the service for v2xml
 * @author Tao
 */
public class OSTIv2XmlService extends OSTIElinkService {
    public static final String WORKFLOW_STATUS = "workflow_status";
    public static final String SITE_URL = "site_url";
    public static final String OSTI_TOKEN_ENV_NAME = "METACAT_OSTI_TOKEN";
    public static final String BASE_URL_ENV_NAME = "METACAT_OSTI_BASE_URL";
    public static final String TOKEN_PATH_PROP_NAME = "ostiService.v2.tokenFilePath";
    public static final String V2XML_CONTEXT_ENV_NAME = "METACAT_OSTI_V2XML_CONTEXT";
    public static final String V2JSON_CONTEXT_ENV_NAME = "METACAT_OSTI_V2JSON_CONTEXT";
    public static final String DOI_QUERY_MAX_ATTEMPTS_ENV_NAME =
        "METACAT_OSTI_DOI_QUERY_MAX_ATTEMPTS";
    private static final String UPLOAD = "/upload";
    private static String v2XmlContext = "elink2xml";
    protected static String v2JsonContext = "elink2api";
    protected static final String SUBMIT_SUFFIX = "submit";
    protected static final String RECORDS = "records";
    protected static String token;
    protected static String queryURL;
    protected static String v2RecordsURL;
    protected static int maxAttempts = 40; //

    /**
     * Constructor. This one will NOT be used.
     *
     * @param username the username of the account which can access the OSTI service
     * @param password the password of the account which can access the OSTI service
     * @param baseURL  the url which specifies the location of the OSTI service
     */
    public OSTIv2XmlService(String username, String password, String baseURL) {
        super(username, password, baseURL);
    }

    /**
     * Constructor
     *
     * @param username the username of the account which can access the OSTI service
     * @param password the password of the account which can access the OSTI service
     * @param baseURL  the url which specifies the location of the OSTI service
     * @param properties  the properties will be used in the OSTI service
     * @throws PropertyNotFound
     * @throws IOException
     * @throws OSTIElinkException
     */
    public OSTIv2XmlService(String username, String password, String baseURL, Properties properties)
        throws PropertyNotFound, IOException, OSTIElinkException {
        super(username, password, baseURL);
        this.properties = properties;
        constructURLs();
        loadToken();
        String maxAttemptsStr = System.getenv(DOI_QUERY_MAX_ATTEMPTS_ENV_NAME);
        if (maxAttemptsStr != null && !maxAttemptsStr.trim().equals("")) {
            log.info("The max query attempts from env variable " + DOI_QUERY_MAX_ATTEMPTS_ENV_NAME
                         + " is " + maxAttemptsStr);
            try {
                maxAttempts = Integer.parseInt(maxAttemptsStr);
            } catch (NumberFormatException e) {
                log.warn("The max query attempt value specified in the env variable "
                             + DOI_QUERY_MAX_ATTEMPTS_ENV_NAME + " is not a integer: "
                             + e.getMessage() + ". So we still use the default value 40.");
                maxAttempts = 40;
            }

        }
    }

    /**
     * Get the status of a DOI. If there are multiple records for a DOI, the status of
     * the first one will be returned
     * @param doi  the doi to identify the record
     * @return  the status of the doi
     * @throws OSTIElinkException
     */
    @Override
    public String getStatus(String doi) throws OSTIElinkException {
        String status;
        String metadata = null;
        long start = System.currentTimeMillis();
        for (int i = 0; i <= maxAttempts; i++ ) {
            try {
                metadata = getMetadata(doi);
                break;
            } catch (OSTIElinkNotFoundException e) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                    log.warn("The thread waiting for the DOI searchable in the getStatus method "
                                 + "was interrupted " + ex.getMessage());
                }
            }
            if (i == maxAttempts) {
                throw new OSTIElinkNotFoundException("The library tried " + maxAttempts + " times"
                                                         + " to query the status of " + doi + " "
                                                         + "from the OSTI service. However OSTI "
                                                         + "service still can't find it");
            }
        }
        long end = System.currentTimeMillis();
        log.warn("It waited " + (end - start)/1000 + " seconds for doi " + doi + " to be "
                     + "searchable after minting it.");
        try {
            status = JsonResponseHandler.getPathValue(metadata, WORKFLOW_STATUS);
        } catch (JsonProcessingException e) {
            throw new OSTIElinkException(e.getMessage());
        }
        if (status == null) {
            throw new OSTIElinkException("There is no workflow_status for " + doi +" in the query"
                                             + " result:\n" + metadata);
        }
        // Our metacat checks the status of Saved. However, from v1 to v2, the status of Saved
        // changed to SA. In order not to change Metacat's code, we need to map SA to Saved
        if (status.equals("SA")) {
            status = OSTIElinkService.SAVED;
        }
        log.debug("The status of " + doi + " is " + status);
        return status;
    }

    /**
     * This method loads the token from the environmental variable OSTI_TOKEN first.
     * If this environment variable is not defined, it will read the token file path from the
     * property file and load the token from that file.
     * @throws PropertyNotFound
     * @throws IOException
     */
    protected void loadToken() throws PropertyNotFound, IOException {
        token = System.getenv(OSTI_TOKEN_ENV_NAME);
        if (token == null) {
            String token_path = OSTIServiceFactory.getProperty(TOKEN_PATH_PROP_NAME, properties);
            log.info("Can't get the token from the environmental variable OSTI_TOKEN and will "
                          + "read it from a file " + token_path);
            token = FileUtils.readFileToString(new File(token_path), "UTF-8");
        } else {
            log.info("It got the token from the environmental variable - " + OSTI_TOKEN_ENV_NAME);
        }
    }

    /**
     * Construct the base, query and records url. The query and records are v2 api url. They both
     * construct from the host name. The host name can be read either from the environmental
     * variable guid.doi.baseurl or a property with the same name. The environmental surpasses
     * the property one.
     * @throws OSTIElinkException
     */
    protected void constructURLs() throws OSTIElinkException {
        log.info("The base URL from the property file is " + baseURL);
        String url = System.getenv(BASE_URL_ENV_NAME);
        if (url != null && !url.trim().equals("")) {
            log.info("The base URL from the env variable " + BASE_URL_ENV_NAME
                        + " is " + url + " and the value overwrites the one from the property file");
            baseURL = url;
        }
        if (baseURL == null) {
            throw new OSTIElinkException("The base URL for the osti service is null");
        }
        if (!baseURL.endsWith("/")) {
            baseURL = baseURL + "/";
        }
        String xmlContext = System.getenv(V2XML_CONTEXT_ENV_NAME);
        if (xmlContext != null && !xmlContext.trim().equals("")) {
            log.info(
                "The v2XmlContext was overwritten by the env variable " + V2XML_CONTEXT_ENV_NAME
                    + " with value " + xmlContext);
            v2XmlContext = xmlContext;
        }
        String jsonContext = System.getenv(V2JSON_CONTEXT_ENV_NAME);
        if (jsonContext != null && !jsonContext.trim().equals("")) {
            log.info(
                "The v2JsonContext was overwritten by the env variable " + V2JSON_CONTEXT_ENV_NAME
                    + " with value " + jsonContext);
            v2JsonContext = jsonContext;
        }
        queryURL = baseURL + v2JsonContext;
        v2RecordsURL = queryURL + "/" + RECORDS;
        baseURL = baseURL + v2XmlContext + UPLOAD;
        log.info("The OSTI base url is " + baseURL + " , the query url is " + queryURL +" and the"
                     + " records url is " + v2RecordsURL);
    }

    /**
     * Add the bearer token authentication method for the v2 requests
     * @param request  the request needs to be added headers
     * @param url  the url which will be sent
     */
    @Override
    protected void setHeaders(HttpUriRequest request, String url) {
        if (url.contains(v2XmlContext + UPLOAD)) {
            log.debug(url + "is a v2xml request so it set be application/xml ");
            request.addHeader("Accept", "application/xml");
        } else {
            log.debug(url + "is a v2api json request so it set be application/json ");
            request.addHeader("Accept", "application/json");
            request.addHeader("Content-Type", "application/json");
        }
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    @Override
    protected  void setGetHeaders(HttpUriRequest request) {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }


    /**
     * Get the metadata associated with the given identifier. An OSTIElinkNotFoundException
     * will be thrown if the identifier can't be found.
     * @param identifier  the identifier for which the metadata should be returned
     * @param  type  the type of the identifier, which can be doi or OSTIId
     * @return  the metadata in the xml format
     * @throws OSTIElinkException
     */
    @Override
    protected String getMetadata(String identifier, String type) throws OSTIElinkException {
        String metadata = null;
        if (identifier != null && !identifier.trim().equals("")) {
            //we need to remove the doi prefix
            identifier = removeDOI(identifier);
            String url = null;
            try {
                url = queryURL + "/records?" + type + "=" + URLEncoder.encode(
                    "\"" + identifier + "\"", StandardCharsets.UTF_8.toString());
            } catch (UnsupportedEncodingException e) {
                throw new OSTIElinkException(
                    "OSTIv2XmlService.getMetadata - couldn't encode the query url: "
                        + e.getMessage());
            }
            log.info("The url sending to the service is " + url);
            byte[] response = sendRequest(GET, url);
            metadata = new String(response);
            log.info("The response for id " + identifier + " is\n " + metadata);
            if (metadata == null || metadata.trim().equals("")) {
                throw new OSTIElinkException("OSTIv2XmlService.getMetadata - the response is blank"
                                                 + ". It means the token is invalid for looking "
                                                 + identifier + ", which type is " + type);
            } else if (metadata.equals("[]")) {
                throw new OSTIElinkNotFoundException(
                    "OSTIv2XmlService.getMetadata - OSTI can't find the identifier "
                        + identifier + ", which type is " + type + " since\n " + metadata);
            } else {
                try {
                    // Check if it is an error response
                    JsonResponseHandler.isResponseWithError(metadata);
                } catch (OSTIElinkException ee) {
                    throw new OSTIElinkException(
                        "OSTIv2XmlService.getMetadata - can't get the metadata for id " + identifier
                            + " since\n " + metadata);
                }
            }
        } else {
            throw new OSTIElinkException(
                "OSTIv2XmlService.getMetadata - the given identifier can't be null or blank.");
        }
        return metadata;
    }

    @Override
    protected String parseOSTIidFromResponse(String metadata, String doi)
        throws OSTIElinkException {
        if (metadata == null || metadata.trim().equals("")) {
            throw new OSTIElinkException("The service can't parse the blank response to get the "
                                             + "OSTI id for the DOI " + doi);
        } else {
            try {
                return JsonResponseHandler.getPathValue(metadata, OSTI_ID);
            } catch (JsonProcessingException e) {
                throw new OSTIElinkException(e.getMessage());
            }
        }
    }

    protected String getBaseUrl() {
        return baseURL;
    }

    protected String getQueryURL() {
        return queryURL;
    }

    protected String getV2RecordsURLURL() {
        return v2RecordsURL;
    }

    protected int getMaxAttempts() {
        return maxAttempts;
    }

    @Override
    protected void handlePublishIdentifierCommand(String ostiId, String siteUrl)
        throws OSTIElinkException {
        //First, query the ostiId from the service
        String jsonMetadata = getMetadataFromOstiId(ostiId);
        log.debug("The metadata for osti_id " + ostiId + " is\n" + jsonMetadata);
        // Extract the record from the metadata (it is a json array)
        try {
            ObjectNode record = JsonResponseHandler.getFirstNodeInArray(jsonMetadata);
            //Manipulate the record - removing the workflow_status and adding the site url
            record.remove(WORKFLOW_STATUS);
            record.put(SITE_URL, siteUrl);
            // Send the modified record back
            String newMetadata = record.toString();
            log.debug("The modified metadata (removing workflow_status and adding site_url is\n"
                          + newMetadata);
            setJsonMetadata(ostiId, newMetadata);
        } catch (JsonProcessingException e) {
            throw new OSTIElinkException(e.getMessage());
        }
    }

    /**
     * Set a new version of metadata (json format) to the given osti id
     * @param osti_id  the identifier's metadata which will be replaced
     * @param jsonMetadata  the new metadata in json format
     * @throws OSTIElinkException
     */
    protected void setJsonMetadata(String osti_id, String jsonMetadata) throws OSTIElinkException {
        try {
            String url = v2RecordsURL + "/" + osti_id + "/" +SUBMIT_SUFFIX;
            byte[] response = sendRequest(PUT, url, jsonMetadata);
            String responseStr = new String(response);
            log.debug("The response from the OSTI service to set metadata for osti_id " + osti_id
                          + " is:\n " + responseStr);
            // Parse the response to determine if the request succeeded or failed. If it failed, an
            // exception will be thrown.
            JsonResponseHandler.isResponseWithError(responseStr);
        } catch (OSTIElinkException e) {
            throw new OSTIElinkException("Can't set the json metadata for osti_id " + osti_id +
                                             " since " + e.getMessage());
        }

    }
}
