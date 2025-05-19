package edu.ucsb.nceas.osti_elink.v2.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ucsb.nceas.osti_elink.OSTIElinkException;
import edu.ucsb.nceas.osti_elink.OSTIElinkNotFoundException;
import edu.ucsb.nceas.osti_elink.OSTIElinkService;
import edu.ucsb.nceas.osti_elink.OSTIServiceFactory;
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
    protected String BASE_URL = "https://www.osti.gov/elink2api/";

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

            // execute the query
            byte[] response = sendRequest(GET, getMetadataUrl);
            metadata = new String(response);
            log.info("The response for id " + extractedIdentifier + " is\n " + metadata);

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
     * Set a new version of metadata (json format) to the given osti id
     * @param osti_id  the identifier's metadata which will be replaced
     * @param jsonMetadata  the new metadata in json format
     * @throws OSTIElinkException
     */
    protected void setMetadata(String osti_id, String jsonMetadata) throws OSTIElinkException {
        String setMetadataUrl = null;
        try {
            setMetadataUrl = SET_METADATA_ENDPOINT_URL + "/" + osti_id + "/" + DOI_RECORDS_ENDPONT_SUBMIT_PARAMETER;
            byte[] response = sendRequest(PUT, setMetadataUrl, jsonMetadata);
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


    @Override
    public String mintIdentifier(String siteCode) throws OSTIElinkException {
        // mintIdentifier is used to mint a new DOI for the given siteCode
        String DoiIdentifier = null;

        String minimalMetadata = buildMinimalMetadata(siteCode);
        log.debug("the minmal metadata is " + minimalMetadata);
        log.debug("the base url is " + MINT_DOI_ENDPOINT_URL);
        byte[] response = sendRequest(POST, MINT_DOI_ENDPOINT_URL, minimalMetadata);
        log.debug("OSTIv2JsonService.mintIdentifier - the response from the OSTI service is:\n "
                + new String(response));

        // validate that the response is valid json
        try {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.readTree(response);
        } catch (Exception e) {
            throw new OSTIElinkException("OSTIv2JsonService.mintIdentifier - Error:  " + new String(response));
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);

            // Check that doi record has "status = SA" (saved) and access the "doi" field
            if (rootNode.has(STATUS)) {
                String status = rootNode.get(STATUS).asText();
                String doi = rootNode.get(DOI).asText();
                if (status != null && status.equalsIgnoreCase(SAVED_STATUS) && doi != null && !doi.trim().equals("")) {
                    DoiIdentifier = DOI + ":" + doi;
                }
            } else {
                System.out.println("Status field not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return DoiIdentifier;
    }

    @Override
    protected String buildMinimalMetadata(String siteCode) throws OSTIElinkException {
        // todo is minimal metadata needed for v2json?

        String metadataStr = null;

        ObjectMapper mapper = new ObjectMapper();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(minimalMetadataFileJson)) {
            if (is == null) {
                throw new IOException("Resource not found: " + minimalMetadataFileJson);
            }

            // Read JSON from input stream into JsonNode
            JsonNode jsonNode = mapper.readTree(is);
            // Convert JsonNode back to string and return the response
            metadataStr = mapper.writeValueAsString(jsonNode);
        }  catch (IOException e) {
            throw new OSTIElinkException("OSTIElink.buildMinimalMetadata - Error: " + e.getMessage());
        }

//        if (minimalMetadataDoc == null) {
//            try (InputStream is = getClass().getClassLoader().getResourceAsStream(minimalMetadataFileJson)) {
//                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
//                DocumentBuilder dBuilder;
//                try {
//                    dBuilder = dbFactory.newDocumentBuilder();
//                    minimalMetadataDoc = dBuilder.parse(is);
//                    originalDefaultSiteCode = getElementValue(minimalMetadataDoc, "site_input_code");
//                    log.debug("DOIService.buildMinimalMetadata - the original site code in the minimal metadata is " + originalDefaultSiteCode);
//                } catch (ParserConfigurationException e) {
//                    throw new OSTIElinkException("OSTIElink.buildMinimalMetadata - Error: " + e.getMessage());
//                } catch (SAXException e) {
//                    throw new OSTIElinkException("OSTIElink.buildMinimalMetadata - Error: " + e.getMessage());
//                } catch (IOException e) {
//                    throw new OSTIElinkException("OSTIElink.buildMinimalMetadata - Error: " + e.getMessage());
//                }
//            } catch (IOException ee) {
//                throw new OSTIElinkException("OSTIElink.buildMinimalMetadata - Error to read the file: " + ee.getMessage());
//            }
//        }
//        if (siteCode != null && !siteCode.trim().equals("")) {
//            modifySiteCode(siteCode);
//        } else if (!originalDefaultSiteCode.equals(currentDefaultSiteCode)) {
//            //now the user ask the default site code. But the site map value has been updated by another call.
//            //we need to change back to the original code.
//            modifySiteCode(originalDefaultSiteCode);
//        }
//        metadataStr = serialize(minimalMetadataDoc);
        return metadataStr;
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
        if(url.contains("elink2api")){
            log.debug(url + "is a v2 elink2api json request, so application/json has been added as the header");
            request.addHeader("Accept", "application/json");
        } else {
            log.debug(url + "is a v2xml request, so application/xml has been added as the header");
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

            // 3. Prepare metadata for publishing - by replacing workflow_status with site_url
            record.remove(WORKFLOW_STATUS);
            record.put(SITE_URL, siteUrl);

            // 4. Call the publish endpoint to send
            // publish endpoint expects entire metadata with the request
            String newMetadata = record.toString();
            log.debug("The modified metadata (removing workflow_status and adding site_url is\n"
                    + newMetadata);
            setMetadata(ostiId, newMetadata);
        } catch (JsonProcessingException e) {
            throw new OSTIElinkException(e.getMessage());
        }
    }

    protected void constructURLs() throws OSTIElinkException {
        // get the base URL from the property file
        log.info("The base URL from the property file is " + BASE_URL);

        // get the base URL from the environment variable
        // overwrites the one from the property file
        String url = System.getenv(BASE_URL_ENV_NAME);
        if (url != null && !url.trim().equals("")) {
            log.info("The base URL from the env variable " + BASE_URL_ENV_NAME
                    + " is " + url + " and the value overwrites the one from the property file");
            BASE_URL = url;
        }

        // error checking for baseURL
        if (BASE_URL == null) {
            throw new OSTIElinkException("The base URL for the osti service is null");
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
