package edu.ucsb.nceas.osti_elink.v2.xml;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import uk.org.webcompere.systemstubs.rules.EnvironmentVariablesRule;

import java.io.InputStream;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

/**
 * The junit test class of OSTIv2XmlService.
 * @author Tao
 */
public class OSTIv2XmlServiceTest {
    private static final String FAKE_TOKEN = "fake_token";
    private OSTIv2XmlService service;
    private String baseUrl;
    private Properties props;
    @Rule
    public EnvironmentVariablesRule environmentVariablesRule =
        new EnvironmentVariablesRule("OSTI_TOKEN", null);

    @Before
    public void setUp() throws Exception {
        props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("test.properties")) {
            props.load(is);
        }
        baseUrl = props.getProperty("ostiService.v2.baseURL");
    }

    /**
     * Test the loadToken method from the token path
     * @throws Exception
     */
    @Test
    public void testLoadTokenFromTokenPath() throws Exception {
        assertNull(System.getenv("OSTI_TOKEN"));
        //username and password are null (it uses token)
        service = new OSTIv2XmlService(null, null, baseUrl, props);
        service.loadToken();
        assertNotEquals(FAKE_TOKEN, service.token);
    }

    /**
     * Test the loadToken method from the token path
     * @throws Exception
     */
    @Test
    public void testLoadTokenFromEnv() throws Exception {
        // Set the env variable
        environmentVariablesRule.set("OSTI_TOKEN", FAKE_TOKEN);
        assertEquals(FAKE_TOKEN, System.getenv("OSTI_TOKEN"));
        //username and password are null (it uses token)
        service = new OSTIv2XmlService(null, null, baseUrl, props);
        service.loadToken();
        assertEquals(FAKE_TOKEN, service.token);
    }
}
