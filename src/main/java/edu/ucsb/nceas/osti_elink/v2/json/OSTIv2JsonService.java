package edu.ucsb.nceas.osti_elink.v2.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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

public class OSTIv2JsonService extends OSTIElinkService {

    public static final String WORKFLOW_STATUS = "workflow_status";
    public static final String SITE_URL = "site_url";
    public static final String DOI_QUERY_MAX_ATTEMPTS_ENV_NAME =
            "METACAT_OSTI_DOI_QUERY_MAX_ATTEMPTS";
    public static final String OSTI_TOKEN_ENV_NAME = "METACAT_OSTI_TOKEN";
    public static final String TOKEN_PATH_PROP_NAME = "ostiService.v2.tokenFilePath";
    protected static int maxAttempts = 40;
    protected static String token;

    /**
     * Constructor. This one will NOT be used.
     *
     * @param username the username of the account which can access the OSTI service
     * @param password the password of the account which can access the OSTI service
     * @param baseURL  the url which specifies the location of the OSTI service
     */
    public OSTIv2JsonService(String username, String password, String baseURL) {
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
    public OSTIv2JsonService(String username, String password, String baseURL, Properties properties)
            throws PropertyNotFound, IOException, OSTIElinkException {
        super(username, password, baseURL);
        this.properties = properties;

        String queryEndpoint = "";
        String uploadEndpoint = "";

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
            if (i >= maxAttempts) {
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

            String url = "";
            String encodedQuery = null;
            // build encoded query
            try {
                encodedQuery = url + type + "=" + URLEncoder.encode(
                        "\"" + identifier + "\"", StandardCharsets.UTF_8.toString());
            } catch (UnsupportedEncodingException e) {
                throw new OSTIElinkException(
                        "OSTIv2JsonService.getMetadata - couldn't encode the query url: "
                                + e.getMessage());
            }
            log.info("The query sent to get metadata is " + encodedQuery);

            // execute the query
            byte[] response = sendRequest(GET, encodedQuery);
            metadata = new String(response);
            log.info("The response for id " + identifier + " is\n " + metadata);

            // process query response
            // check for errors; return response if none found
            if (metadata == null || metadata.trim().equals("")) {
                throw new OSTIElinkException("OSTIv2JsonService.getMetadata - the response is blank"
                        + ". It means the token is invalid for looking "
                        + identifier + ", which type is " + type);
            } else {
                JsonNode node;
                try {
                    // Check if it is an error response
                    node = JsonResponseHandler.isResponseWithError(metadata);
                } catch (OSTIElinkException ee) {
                    throw new OSTIElinkException(
                            "OSTIv2JsonService.getMetadata - can't get the metadata for id " + identifier
                                    + " since\n " + metadata);
                }
                // Am empty array return means not-found
                if (JsonResponseHandler.isEmptyArray(node)) {
                    throw new OSTIElinkNotFoundException(
                            "OSTIv2JsonService.getMetadata - OSTI can't find the identifier "
                                    + identifier + ", which type is " + type + " since\n " + metadata);
                }
            }
        } else {
            throw new OSTIElinkException(
                    "OSTIv2JsonService.getMetadata - the given identifier can't be null or blank.");
        }

        // no errors found; return response
        return metadata;
    }

    protected String parseOSTIidFromResponse(String metadata, String doi)
            throws OSTIElinkException{
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

//    todo rename method to setPostHEaders?
//    todo review ifelse clause, simplify
    protected void setHeaders(HttpUriRequest request, String url){
        if(url.contains("elink2apijson")){
            log.debug("");
            request.addHeader("Accept", "application/json");
        } else {
            log.debug("");
            request.addHeader("Accept", "application/xml");
            request.addHeader("Content-Type", "application/xml");
        }
        request.addHeader("Authorization", "Bearer " + token);
    }

    protected void setGetHeaders(HttpUriRequest request){
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }


    protected void handlePublishIdentifierCommand(String ostiId, String siteUrl)
            throws OSTIElinkException {

        // 1. Get the metadata for the given osti id
        String jsonMetadata = getMetadataFromOstiId(ostiId);
        log.debug("The metadata for osti_id " + ostiId + " is\n" + jsonMetadata);


        try {
            // 2. Parse the metadata to get the record
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
            String queryUrl = "";
            byte[] response = sendRequest(PUT, queryUrl, jsonMetadata);
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
