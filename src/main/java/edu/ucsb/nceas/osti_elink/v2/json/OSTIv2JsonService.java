package edu.ucsb.nceas.osti_elink.v2.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ucsb.nceas.osti_elink.OSTIElinkException;
import edu.ucsb.nceas.osti_elink.OSTIElinkAuthenticationException;
import edu.ucsb.nceas.osti_elink.OSTIElinkNotFoundException;
import edu.ucsb.nceas.osti_elink.OSTIElinkService;
import edu.ucsb.nceas.osti_elink.OSTIServiceFactory;
import edu.ucsb.nceas.osti_elink.PublishIdentifierCommandFactory;
import edu.ucsb.nceas.osti_elink.PublishIdentifierCommand;
import edu.ucsb.nceas.osti_elink.exception.PropertyNotFound;
import edu.ucsb.nceas.osti_elink.v2.response.JsonResponseHandler;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpUriRequest;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.UnsupportedEncodingException;



public class OSTIv2JsonService extends OSTIElinkService {

    // The baseURL specifies the default endpoint for the OSTI v2 JSON API.
    // It can be overridden by the environment variable METACAT_OSTI_BASE_URL.
    protected String BASE_URL = "https://www.osti.gov";

    // The name of the environment variable used to specify the base URL for the OSTI service.
    // This variable can override the default base URL provided in the application configuration.
    public static final String BASE_URL_ENV_NAME = "METACAT_OSTI_BASE_URL";

    // OSTI_TOKEN_ENV_NAME specifies the environment variable name for the OSTI service token.
    public static final String OSTI_TOKEN_ENV_NAME = "METACAT_OSTI_TOKEN";
    // TOKEN_PATH_PROP_NAME specifies the property name for the file path containing the OSTI service token.
    public static final String TOKEN_PATH_PROP_NAME = "ostiService.v2.tokenFilePath";
    protected static String token;

    // Specifies the default context path for the OSTI v2 JSON API.
    // This value can be overridden by the environment variable METACAT_OSTI_V2JSON_CONTEXT.
    protected static final String v2JsonContext = "elink2api";
    public static final String V2JSON_CONTEXT_ENV_NAME = "METACAT_OSTI_V2JSON_CONTEXT";
    // Defines the constant for the "records" endpoint used to access all information about DOI records at OSTI.
    protected static final String DOI_RECORDS_ENDPOINT = "records";
    // Parameter for the "save" operation in the records endpoint, used for minting and updating DOI records.
    protected static final String DOI_RECORDS_ENDPONT_SAVE_PARAMETER = "save";
    // Parameter for the "submit" operation in the records endpoint, used for publishing DOI records.
    protected static final String DOI_RECORDS_ENDPONT_SUBMIT_PARAMETER = "submit";

    // Holds the constructed query URL for the OSTI v2 JSON API, initialized later based on the base URL and context path.
    // Example value: "https://www.osti.gov/elink2api"
    protected String QUERY_URL = null;
    // Defines the full URL to access the "records" endpoint in the OSTI API.
    // Example value: "https://www.osti.gov/elink2api/records"
    protected static String FULL_RECORDS_ENDPOINT_URL = null;
    protected static String MINT_DOI_ENDPOINT_URL = null;
    protected static String GET_METADATA_ENDPOINT_URL = null;
    // Constructed by concatenating  FULL_RECORDS_ENDPOINT_URL with /{id}/submit
    protected static String SET_METADATA_ENDPOINT_URL = null;
    // Constructed by concatenating  FULL_RECORDS_ENDPOINT_URL with /{id}/save
    protected static String UPDATE_METADATA_ENDPOINT_URL = null;
    // Constructed by concatenating  FULL_RECORDS_ENDPOINT_URL with /{id}/submit
    protected static String PUBLISH_DOI_ENDPOINT_URL = null;

    public static final String DOI_QUERY_MAX_ATTEMPTS_ENV_NAME =
            "METACAT_OSTI_DOI_QUERY_MAX_ATTEMPTS";
    protected static int maxAttempts = 40;

    public static final String WORKFLOW_STATUS = "workflow_status";
    public static final String SITE_URL = "site_url";
    private JsonNode minimalMetadataNode = null;
    private String originalDefaultSiteCode = null;
    private String currentDefaultSiteCode = null;
//    protected static final String minimalMetadataFileJson = "minimal-osti.json";
    protected static final String DEFAULT_MINIMAL_METADATA_FILE_JSON = "minimal-osti.json";
    public static final String MINIMAL_METADATA_FILE_ENV_NAME = "METACAT_OSTI_MINIMAL_METADATA_FILE";



    /**
     * Constructor. This one will NOT be used.
     *
     * @param username the username of the account which can access the OSTI service
     * @param password the password of the account which can access the OSTI service
     * @param BASE_URL  the url which specifies the location of the OSTI service
     */
    public OSTIv2JsonService(String username, String password, String BASE_URL) {
        super(username, password, BASE_URL);
    }

    /**
     * Constructor
     *
     * @param username the username of the account which can access the OSTI service
     * @param password the password of the account which can access the OSTI service
     * @param BASE_URL  the url which specifies the location of the OSTI service
     * @param properties  the properties will be used in the OSTI service
     * @throws PropertyNotFound
     * @throws IOException
     * @throws OSTIElinkException
     */
    public OSTIv2JsonService(String username, String password, String BASE_URL, Properties properties)
            throws PropertyNotFound, IOException, OSTIElinkException {
        super(username, password, BASE_URL);
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

        // fetch metadata for given doi
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

        // process response to check status
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

        // url for GET metadata request
        String getMetadataUrl = null;

        // check identifier for errors
        if (identifier != null && !identifier.trim().equals("")) {

            // Remove the DOI prefix from the identifier to extract the actual DOI
            String extractedIdentifier = removeDOI(identifier);

            // build encoded getMetadata query url
            try {
                getMetadataUrl = FULL_RECORDS_ENDPOINT_URL + "?" + type + "=" + URLEncoder.encode(
                        "\"" + extractedIdentifier + "\"", StandardCharsets.UTF_8.toString());
            } catch (UnsupportedEncodingException e) {
                throw new OSTIElinkException(
                        "OSTIv2JsonService.getMetadata - couldn't encode the getMetadataUrl: "
                                + e.getMessage());
            }
            log.info("The query sent to get metadata is " + getMetadataUrl);

            // execute the query with authentication error handling
            try {
                byte[] response = sendRequest(GET, getMetadataUrl);
                metadata = new String(response);
                log.info("OSTIv2JsonService.getMetadata: Successfully retrieved metadata for " + extractedIdentifier);

            } catch (OSTIElinkAuthenticationException e) {
                // Handle authentication errors with more context
                String contextMsg = "OSTIv2JsonService.getMetadata - Failed to retrieve metadata for identifier '"
                        + identifier + "' (" + type + "): " + e.getMessage()
                        + ". Please check your OSTI token configuration.";
                log.error(contextMsg);
                throw new OSTIElinkAuthenticationException(e.getStatusCode(), contextMsg);
            }
            // process query response
            // check for errors; return response if none found
            if (metadata == null || metadata.trim().equals("")) {
                throw new OSTIElinkException("OSTIv2JsonService.getMetadata - the response is blank"
                        + ". It means the token is invalid for looking "
                        + extractedIdentifier + ", which type is " + type);
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

    /**
     * Set new metadata for the given DOI.
     * @param doi The DOI identifier to update
     * @param doiPrefix A shortcut to determine OSTI_id (can be null for safety)
     * @param metadataJson The new metadata in JSON format
     * @throws OSTIElinkException
     */
    @Override
    public void setMetadata(String doi, String doiPrefix, String metadataJson) throws OSTIElinkException {
        // Get the OSTI ID associated with this DOI
        String ostiId = getOstiId(doi, doiPrefix);

        log.debug("OSTIv2JsonService.setMetadata - Processing metadata update for DOI " + doi +
                " with OSTI ID " + ostiId + ". Metadata:\n" + metadataJson);

        // Check if this is a publish command
        PublishIdentifierCommand command = PublishIdentifierCommandFactory.getInstance(this);
        if (command.parse(metadataJson)) {
            log.info("OSTIv2JsonService.setMetadata - Detected publish identifier command. " +
                    "Will handle via specialized route.");

            // Use the specialized publication handler which handles the workflow status to site_url conversion
            handlePublishIdentifierCommand(command.getOstiId(), command.getUrl());
        } else {
            log.debug("OSTIv2JsonService.setMetadata - Standard metadata update (not a publish command)");

            // For standard updates, use the /records/{id}/save endpoint
            String updateUrl = UPDATE_METADATA_ENDPOINT_URL + "/" + ostiId + "/" + DOI_RECORDS_ENDPONT_SAVE_PARAMETER;

            log.debug("OSTIv2JsonService.setMetadata - Sending metadata update to: " + updateUrl);
            byte[] response = sendRequest(PUT, updateUrl, metadataJson);
            String responseStr = new String(response);

            log.debug("OSTIv2JsonService.setMetadata - Response from OSTI service: " + responseStr);

            // Validate the response
            try {
                // This will throw an exception if there's an error in the response
                JsonNode responseNode = JsonResponseHandler.isResponseWithError(responseStr);

                // Check if response indicates success
                if (responseNode == null || !responseNode.has(WORKFLOW_STATUS)) {
                    throw new OSTIElinkException("OSTIv2JsonService.setMetadata - Invalid or incomplete response");
                }

                log.info("OSTIv2JsonService.setMetadata - Successfully updated metadata for DOI " +
                        doi + " (OSTI ID: " + ostiId + "). New status: " +
                        responseNode.get(WORKFLOW_STATUS).asText());

            } catch (OSTIElinkException e) {
                log.error("OSTIv2JsonService.setMetadata - Error updating metadata: " + e.getMessage());
                throw new OSTIElinkException("OSTIv2JsonService.setMetadata - Error:\n" + responseStr);
            }
        }
    }


    @Override
    public String mintIdentifier(String siteCode) throws OSTIElinkException {
        // mintIdentifier is used to mint a new DOI for the given siteCode
        String DoiIdentifier = null;

        String minimalMetadata = buildMinimalMetadata(siteCode);
        log.debug("the minimal metadata is " + minimalMetadata);
        log.debug("the MINT_DOI_ENDPOINT_URL is " + MINT_DOI_ENDPOINT_URL);
        byte[] response = sendRequest(POST, MINT_DOI_ENDPOINT_URL, minimalMetadata);
        log.debug("OSTIv2JsonService.mintIdentifier - the response from the OSTI service is:\n "
                + new String(response));

        // validate that the response is valid json
//        try {
//            final ObjectMapper mapper = new ObjectMapper();
//            mapper.readTree(response);
//        } catch (Exception e) {
//            throw new OSTIElinkException("OSTIv2JsonService.mintIdentifier - Error:  " + new String(response));
//        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);

            // Check that doi record has "status = SA" (saved) and access the "doi" field
            if (rootNode.has(WORKFLOW_STATUS)) {
                String status = rootNode.get(WORKFLOW_STATUS).asText();
                String doi = rootNode.get(DOI).asText();
                if (status != null && status.equalsIgnoreCase(SAVED_STATUS) && doi != null && !doi.trim().equals("")) {
                    DoiIdentifier = DOI + ":" + doi;
                }
            } else {
                System.out.println("OSTIv2JsonService.mintIdentifier - ERROR: Status field not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new OSTIElinkException("OSTIv2JsonService.mintIdentifier - Error:  " + new String(response));

        }

        log.debug("OSTIv2JsonService.mintIdentifier(): DoiIdentifier = " + DoiIdentifier);

        return DoiIdentifier;
    }

    @Override
    protected String buildMinimalMetadata(String siteCode) throws OSTIElinkException {
        String metadataStr = null;
        ObjectMapper mapper = new ObjectMapper();

        // Load minimal metadata if not already loaded
        if (minimalMetadataNode == null) {
            // Check environment variable first, then fall back to default
            String metadataFileName = System.getenv(MINIMAL_METADATA_FILE_ENV_NAME);
            if (metadataFileName == null || metadataFileName.trim().isEmpty()) {
                metadataFileName = DEFAULT_MINIMAL_METADATA_FILE_JSON;
            }
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(metadataFileName)) {
                if (is == null) {
                    throw new IOException("Resource not found: " + metadataFileName);
                }

                // Read JSON from input stream into JsonNode
                minimalMetadataNode = mapper.readTree(is);

                // Store the original site code
                if (minimalMetadataNode.has("site_ownership_code")) {
                    originalDefaultSiteCode = minimalMetadataNode.get("site_ownership_code").asText();
                    currentDefaultSiteCode = originalDefaultSiteCode;
                    log.debug("OSTIElink.buildMinimalMetadata - Original site code: " + originalDefaultSiteCode);
                }

            } catch (IOException e) {
                throw new OSTIElinkException("OSTIElink.buildMinimalMetadata - Error loading file: " + e.getMessage());
            }
        }

        // Modify site code based on parameter
        if (siteCode != null && !siteCode.trim().equals("")) {
            modifySiteCode(siteCode);
        } else if (!originalDefaultSiteCode.equals(currentDefaultSiteCode)) {
            // Reset to original site code if no specific code requested
            // but current code has been modified by previous call
            modifySiteCode(originalDefaultSiteCode);
        }

        // Convert JsonNode to string
        try {
            metadataStr = mapper.writeValueAsString(minimalMetadataNode);
            log.debug("OSTIElink.buildMinimalMetadata - Final metadata: " + metadataStr);
        } catch (JsonProcessingException e) {
            throw new OSTIElinkException("OSTIElink.buildMinimalMetadata - Error serializing JSON: " + e.getMessage());
        }

        return metadataStr;
    }

    /**
     * Modify the site ownership code in the minimal metadata JSON
     * @param siteCode the new site code to set
     * @throws OSTIElinkException if the modification fails
     */
    private void modifySiteCode(String siteCode) throws OSTIElinkException {
        if (minimalMetadataNode == null) {
            throw new OSTIElinkException("OSTIElink.modifySiteCode - Minimal metadata not loaded");
        }

        if (siteCode == null || siteCode.trim().equals("")) {
            throw new OSTIElinkException("OSTIElink.modifySiteCode - Site code cannot be null or empty");
        }

        try {
            // Convert to ObjectNode to allow modifications
            ObjectNode objectNode = (ObjectNode) minimalMetadataNode;
            objectNode.put("site_ownership_code", siteCode);
            currentDefaultSiteCode = siteCode;
            log.debug("OSTIElink.modifySiteCode - Updated site_ownership_code to: " + siteCode);
        } catch (Exception e) {
            throw new OSTIElinkException("OSTIElink.modifySiteCode - Error modifying site code: " + e.getMessage());
        }
    }

    public String parseOSTIidFromResponse(String metadata, String doi)
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
//        log.debug("1. OSTIv2JsonService.loadToken(): token: " + token);
        if (token == null) {
            String token_path = OSTIServiceFactory.getProperty(TOKEN_PATH_PROP_NAME, properties);
            log.info("OSTIv2JsonService.loadToken(): Can't get the token from the environmental variable OSTI_TOKEN and will "
                    + "read it from a file " + token_path);
            token = FileUtils.readFileToString(new File(token_path), "UTF-8");
        } else {
            log.info("OSTIv2JsonService.loadToken(): It got the token from the environmental variable - " + OSTI_TOKEN_ENV_NAME);
        }
//        log.debug("2. OSTIv2JsonService.loadToken(): token: " + token);
    }

//    todo rename method to setPostHEaders?
//    todo review ifelse clause, simplify
    protected void setHeaders(HttpUriRequest request, String url){
        if(url.contains("elink2api")) {
            log.debug("OSTIv2JsonService.setHeaders():" + url + "is a v2 elink2api json request, so application/json has been added as the header");
            request.addHeader("Accept", "application/json");
            request.addHeader("Content-Type", "application/json");
        }
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
//        log.debug("OSTIv2JsonService.setHeaders(): request:" + request + " token: " + request.getFirstHeader("Authorization"));
    }

    protected void setGetHeaders(HttpUriRequest request){
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        request.addHeader("Accept", "application/json");
    }


    /**
     * Handle publication of a DOI by sending to the submit endpoint
     *
     * @param ostiId The OSTI ID to publish
     * @param siteUrl The site URL for the published record
     * @throws OSTIElinkException
     */
    protected void handlePublishIdentifierCommand(String ostiId, String siteUrl)
            throws OSTIElinkException {

        // 1. Get the metadata for the given osti id
        String jsonMetadata = getMetadataFromOstiId(ostiId);
        log.debug("OSTIv2JsonService.handlePublishIdentifierCommand(): The metadata for osti_id " + ostiId + " is\n" + jsonMetadata);

        try {
            // 2. Parse the metadata to get the record
            ObjectNode record = JsonResponseHandler.getFirstNodeInArray(jsonMetadata);

            // 3. Prepare metadata for publishing - by replacing workflow_status with site_url
            record.remove(WORKFLOW_STATUS);
            record.put(SITE_URL, siteUrl);

            // 4. Call the publish endpoint directly
            String publishUrl = PUBLISH_DOI_ENDPOINT_URL + "/" + ostiId + "/" + DOI_RECORDS_ENDPONT_SUBMIT_PARAMETER;
            String newMetadata = record.toString();

            log.debug("OSTIv2JsonService.handlePublishIdentifierCommand(): Sending to publish endpoint: " + publishUrl +
                    "\nThe modified metadata (removing workflow_status and adding site_url) is:\n" + newMetadata);

            byte[] response = sendRequest(PUT, publishUrl, newMetadata);
            String responseStr = new String(response);

            log.debug("OSTIv2JsonService.handlePublishIdentifierCommand(): Response from OSTI service: " + responseStr);

            // Verify the response
            JsonResponseHandler.isResponseWithError(responseStr);

            log.info("OSTIv2JsonService.handlePublishIdentifierCommand(): Successfully published OSTI ID " + ostiId);

        } catch (JsonProcessingException e) {
            throw new OSTIElinkException("Error processing metadata for OSTI ID " + ostiId + ": " + e.getMessage());
        }
    }

    protected void constructURLs() throws OSTIElinkException {
        // get the base URL from the property file
        log.info("OSTIv2JsonService.constructURLs(): The base URL from the property file is " + BASE_URL);

        // get the base URL from the environment variable
        // overwrites the one from the property file
        String url = System.getenv(BASE_URL_ENV_NAME);
        log.debug("OSTIv2JsonService.constructURLs(): url is " + url);
        if (url != null && !url.trim().equals("")) {
            log.info("OSTIv2JsonService.constructURLs(): The base URL from the env variable " + BASE_URL_ENV_NAME
                    + " is " + url + " and the value overwrites the one from the property file");
            BASE_URL = url;

        }

        // error checking for baseURL
        if (BASE_URL == null) {
            throw new OSTIElinkException("OSTIv2JsonService.constructURLs(): The base URL for the osti service is null");
        }

        // baseURL should end with a "/"
        // baseURL will be "https://www.osti.gov/" in production or "https://review.osti.gov/" in test environments.
        if (!BASE_URL.endsWith("/")) {
            BASE_URL = BASE_URL + "/";
        }

        // example value for QUERY_URL: www.osti.gov/elink2api
        QUERY_URL = BASE_URL + v2JsonContext;
        // Defines the constant for the full URL to access the "records" endpoint in the OSTI API.
        // example value for FULL_RECORDS_ENDPOINT_URL: www.osti.gov/elink2api/records
        FULL_RECORDS_ENDPOINT_URL = QUERY_URL + "/" + DOI_RECORDS_ENDPOINT;
        // example value for MINT_DOI_ENDPOINT_URL: www.osti.gov/elink2api/records/save
        MINT_DOI_ENDPOINT_URL = FULL_RECORDS_ENDPOINT_URL + "/" + DOI_RECORDS_ENDPONT_SAVE_PARAMETER;
        GET_METADATA_ENDPOINT_URL = FULL_RECORDS_ENDPOINT_URL;
        SET_METADATA_ENDPOINT_URL = FULL_RECORDS_ENDPOINT_URL; // append osti_id and save parameters to construct complete url /records/{id}/save
        UPDATE_METADATA_ENDPOINT_URL = FULL_RECORDS_ENDPOINT_URL; // append osti_id and save parameters to construct complete url /records/{id}/save
        PUBLISH_DOI_ENDPOINT_URL = FULL_RECORDS_ENDPOINT_URL; // append osti_id and submit parameters to construct complete url /records/{id}/submit

    }

    // methods to access the endpoints
    protected String getBaseUrl() {
        return BASE_URL;
    }

    protected String getQueryUrl() {
        return QUERY_URL;
    }

    protected String getRecordsEndpointURL() {
        return FULL_RECORDS_ENDPOINT_URL;
    }

    protected int getMaxAttempts() {
        return maxAttempts;
    }

}
