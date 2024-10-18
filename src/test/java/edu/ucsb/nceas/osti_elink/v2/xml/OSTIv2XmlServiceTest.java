package edu.ucsb.nceas.osti_elink.v2.xml;

import edu.ucsb.nceas.osti_elink.OSTIElinkException;
import edu.ucsb.nceas.osti_elink.OSTIElinkNotFoundException;
import edu.ucsb.nceas.osti_elink.OSTIElinkService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import uk.org.webcompere.systemstubs.rules.EnvironmentVariablesRule;

import java.io.InputStream;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The junit test class of OSTIv2XmlService.
 * @author Tao
 */
public class OSTIv2XmlServiceTest {
    public static final int MAX_ATTEMPTS = 200;
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
        //username and password are null (it uses token)
        service = new OSTIv2XmlService(null, null, baseUrl, props);
    }

    /**
     * Test the loadToken method from the token path
     * @throws Exception
     */
    @Test
    public void testLoadTokenFromTokenPath() throws Exception {
        assertNull(System.getenv("OSTI_TOKEN"));
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
        service.loadToken();
        assertEquals(FAKE_TOKEN, service.token);
    }

    /**
     * Test the mintIdentifier method
     * @throws Exception
     */
    @Test
    public void testMintIdentifier() throws Exception {
        String identifier = service.mintIdentifier(null);
        assertTrue(identifier.startsWith("doi:10."));
        identifier = service.mintIdentifier("ESS-DIVE");
        assertTrue(identifier.startsWith("doi:"));
        try {
            identifier = service.mintIdentifier("KNB");
            fail("The test can't get here");
        } catch (Exception e) {
            assertTrue(e instanceof OSTIElinkException);
        }
    }

    /**
     * Test the getMetadata method
     * @throws Exception
     */
    @Test
    public void testGetMetadata() throws Exception {
        String identifier = "doi:10.15485/2304391";
        String ostiId = "2304391";
        assertTrue(identifier.startsWith("doi"));
        String metadata = service.getMetadata(identifier, OSTIElinkService.DOI);
        assertTrue(metadata.contains("10.15485/2304391"));
        assertTrue(identifier.endsWith(ostiId));
        metadata = service.getMetadata(ostiId, OSTIElinkService.OSTI_ID);
        assertTrue(metadata.contains("10.15485/2304391"));
        try {
            identifier = "doi:10.15125/2304391";
            metadata = service.getMetadata(identifier, OSTIElinkService.DOI);
            fail("Test shouldn't get here since the doi doesn't exist");
        } catch (Exception e) {
            assertTrue(e instanceof OSTIElinkNotFoundException);
        }
        try {
            ostiId = "1000000";
            metadata = service.getMetadata(ostiId, OSTIElinkService.OSTI_ID);
            fail("Test shouldn't get here since the osti_id doesn't exist");
        } catch (Exception e) {
            assertTrue(e instanceof OSTIElinkNotFoundException);
        }
    }

    /**
     * Test the getMetadata method with the invalid token
     * @throws Exception
     */
    @Test
    public void testGetMetadataWithInvalidToken() throws Exception {
        // Set the env variable
        environmentVariablesRule.set("OSTI_TOKEN", FAKE_TOKEN);
        assertEquals(FAKE_TOKEN, System.getenv("OSTI_TOKEN"));
        service.loadToken();
        assertEquals(FAKE_TOKEN, service.token);
        String identifier = "doi:10.15485/2304391";
        String ostiId = "2304391";
        try {
            String metadata = service.getMetadata(identifier, OSTIElinkService.DOI);
            fail("Test shouldn't get here since the doi doesn't exist");
        } catch (Exception e) {
            assertTrue(e instanceof OSTIElinkException);
            assertTrue(e.getMessage().contains("token"));
        }
        try {
            String metadata = service.getMetadata(ostiId, OSTIElinkService.OSTI_ID);
            fail("Test shouldn't get here since the osti_id doesn't exist");
        } catch (Exception e) {
            assertTrue(e instanceof OSTIElinkException);
            assertTrue(e.getMessage().contains("token"));
        }
    }

    /**
     * Test the method of getStatus
     * @throws Exception
     */
    @Test
    public void testGetStatus() throws Exception {
        String identifier = service.mintIdentifier(null);
        assertTrue(identifier.startsWith("doi:10."));
        String status = null;
        int index = 0;
        //a new minted doi can't be querable immediately. We should give them some time.
        while (status == null && index < MAX_ATTEMPTS) {
            index++;
            try {
               status = service.getStatus(identifier);
            } catch (Exception e) {
                Thread.sleep(100);
            }
        }
        assertEquals("SA", status);
        identifier = "doi:10.15485/2304391";
        status = service.getStatus(identifier);
        assertEquals("R", status);
        try {
            identifier = "doi:10.15125/2304391";
            status = service.getStatus(identifier);
            fail("Test shouldn't get here since the doi doesn't exist");
        } catch (Exception e) {
            assertTrue(e instanceof OSTIElinkNotFoundException);
        }
    }
}
