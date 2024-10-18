package edu.ucsb.nceas.osti_elink.v2.xml;

import edu.ucsb.nceas.osti_elink.OSTIElinkException;
import edu.ucsb.nceas.osti_elink.OSTIElinkNotFoundException;
import edu.ucsb.nceas.osti_elink.OSTIElinkService;
import edu.ucsb.nceas.osti_elink.OSTIServiceFactory;
import edu.ucsb.nceas.osti_elink.exception.PropertyNotFound;
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
 * @author Tao
 * This class represents the service for v2xml
 */
public class OSTIv2XmlService extends OSTIElinkService {
    public static final String OSTI_TOKEN = "OSTI_TOKEN";
    protected static String token;
    protected static String queryURL;

    /**
     * Constructor
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
     */
    public OSTIv2XmlService(String username, String password, String baseURL, Properties properties)
        throws PropertyNotFound, IOException {
        super(username, password, baseURL);
        this.properties = properties;
        queryURL = properties.getProperty("ostiService.v2xml.queryURL");
        loadToken();
    }

    /**
     * This method loads the token from the environmental variable OSTI_TOKEN first.
     * If this environment variable is not defined, it will read the token file path from the
     * property file and load the token from that file.
     * @throws PropertyNotFound
     * @throws IOException
     */
    protected void loadToken() throws PropertyNotFound, IOException {
        token = System.getenv(OSTI_TOKEN);
        if (token == null) {
            String token_path = OSTIServiceFactory.getProperty("ostiService.tokenPath", properties);
            log.debug("Can't get the token from the environmental variable OSTI_TOKEN and will "
                          + "read it from a file " + token_path);
            token = FileUtils.readFileToString(new File(token_path), "UTF-8");
        } else {
            log.debug("It got the token from the environmental variable OSTI_TOKEN.");
        }
    }

    /**
     * Add the bearer token authentication method for the v2 requests
     * @param request  the request needs to be added headers
     */
    @Override
    protected void setHeaders(HttpUriRequest request) {
        request.addHeader("Accept", "application/xml");
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
                    "OSTIElinkService.getMetadata - couldn't encode the query url: "
                        + e.getMessage());
            }
            log.info("OSTIElinkService.getMetadata - the url sending to the service is " + url);
            byte[] response = sendRequest(GET, url);
            metadata = new String(response);
            log.debug("OSTIElinkService.getMetadata - the rseponse for id " + identifier + " is\n "
                          + metadata);
            if (metadata == null || metadata.trim().equals("")) {
                throw new OSTIElinkException("OSTIElinkService.getMetadata - the response is blank"
                                                 + ". It means the token is invalid for looking "
                                                 + identifier + ", which type is " + type);
            } else if (!metadata.contains(identifier)) {
                if (metadata.equals("[]")) {
                    throw new OSTIElinkNotFoundException(
                        "OSTIElinkService.getMetadata - OSTI can't find the identifier "
                            + identifier + ", which type is " + type + " since\n " + metadata);
                } else {
                    throw new OSTIElinkException(
                        "OSTIElinkService.getMetadata - can't get the metadata for id " + identifier
                            + " since\n " + metadata);
                }
            }
        } else {
            throw new OSTIElinkException(
                "OSTIElinkService.getMetadata - the given identifier can't be null or blank.");
        }
        return metadata;
    }

}
