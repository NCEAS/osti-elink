package edu.ucsb.nceas.osti_elink.v2.json;

import edu.ucsb.nceas.osti_elink.OSTIElinkException;
import edu.ucsb.nceas.osti_elink.OSTIElinkService;
import edu.ucsb.nceas.osti_elink.OSTIServiceFactory;
import edu.ucsb.nceas.osti_elink.exception.PropertyNotFound;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class OSTIv2JsonService extends OSTIElinkService {

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


    protected void setHeaders(HttpUriRequest request, String url){
        return;
    }

    protected void setGetHeaders(HttpUriRequest request){
        return;
    }

    protected String parseOSTIidFromResponse(String metadata, String doi)
            throws OSTIElinkException{
        return "";
    }

    protected void handlePublishIdentifierCommand(String ostiId, String siteUrl)
            throws OSTIElinkException{
        return;
    }

}
