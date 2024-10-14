package edu.ucsb.nceas.osti_elink.v2.xml;

import edu.ucsb.nceas.osti_elink.OSTIElinkService;
import edu.ucsb.nceas.osti_elink.OSTIServiceFactory;
import edu.ucsb.nceas.osti_elink.exception.PropertyNotFound;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * @author Tao
 * This class represents the service for v2xml
 */
public class OSTIv2XmlService extends OSTIElinkService {
    public static final String OSTI_TOKEN = "OSTI_TOKEN";
    protected static String token;

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
}
