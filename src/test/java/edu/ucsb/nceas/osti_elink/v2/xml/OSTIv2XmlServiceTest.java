package edu.ucsb.nceas.osti_elink.v2.xml;

import edu.ucsb.nceas.osti_elink.OSTIElinkException;
import edu.ucsb.nceas.osti_elink.OSTIElinkNotFoundException;
import edu.ucsb.nceas.osti_elink.OSTIElinkService;
import edu.ucsb.nceas.osti_elink.OSTIServiceV1Test;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import uk.org.webcompere.systemstubs.rules.EnvironmentVariablesRule;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
    public static final String testBaseURL = "https://review.osti.gov";
    public static final int MAX_ATTEMPTS = 200;
    private static final String FAKE_TOKEN = "fake_token";
    private OSTIv2XmlService service;
    private Properties props;
    @Rule
    public EnvironmentVariablesRule environmentVariablesRule =
        new EnvironmentVariablesRule("METACAT_OSTI_TOKEN", null);
    @Rule
    public EnvironmentVariablesRule environmentVariablesURLRule =
        new EnvironmentVariablesRule("METACAT_OSTI_BASE_URL", null);
    @Rule
    public EnvironmentVariablesRule environmentVariablesXmlContextRule =
        new EnvironmentVariablesRule("METACAT_OSTI_V2XML_CONTEXT", null);
    @Rule
    public EnvironmentVariablesRule environmentVariablesJsonContextRule =
        new EnvironmentVariablesRule("METACAT_OSTI_V2JSON_CONTEXT", null);

    @Before
    public void setUp() throws Exception {
        props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("test.properties")) {
            props.load(is);
        }
        //username and password are null (it uses token)
        service = new OSTIv2XmlService(null, null, testBaseURL, props);
    }

    /**
     * Test the loadToken method from the token path
     * @throws Exception
     */
    @Test
    public void testLoadTokenFromTokenPath() throws Exception {
        assertNull(System.getenv("METACAT_OSTI_TOKEN"));
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
        environmentVariablesRule.set("METACAT_OSTI_TOKEN", FAKE_TOKEN);
        assertEquals(FAKE_TOKEN, System.getenv("METACAT_OSTI_TOKEN"));
        service.loadToken();
        assertEquals(FAKE_TOKEN, service.token);
    }

    /**
     * Test to overwrite the base url and query url by the env variable setting
     * @throws Exception
     */
    @Test
    public void testOverwriteURLByEnv() throws Exception {
        assertEquals(testBaseURL + "/" + "elink2xml/upload", service.getBaseUrl());
        assertEquals(testBaseURL + "/" + "elink2api", service.getQueryURL());
        // Set the env variable
        environmentVariablesURLRule.set("METACAT_OSTI_BASE_URL", "https://foo.com");
        assertEquals("https://foo.com", System.getenv("METACAT_OSTI_BASE_URL"));
        service.constructURLs();
        assertEquals("https://foo.com" + "/" + "elink2xml/upload", service.getBaseUrl());
        assertEquals("https://foo.com" + "/" + "elink2api", service.getQueryURL());
        assertEquals("https://foo.com" + "/" + "elink2api/records", service.getV2RecordsURLURL());
    }

    /**
     * Test to overwrite the context names by the env variable setting
     * @throws Exception
     */
    @Test
    public void testOverwriteContextNamesByEnv() throws Exception {
        String xml = "testxml";
        String json = "testjson";
        assertEquals(testBaseURL + "/" + "elink2xml/upload", service.getBaseUrl());
        assertEquals(testBaseURL + "/" + "elink2api", service.getQueryURL());
        // Set the env variable
        environmentVariablesURLRule.set("METACAT_OSTI_BASE_URL", testBaseURL);
        assertEquals(testBaseURL, System.getenv("METACAT_OSTI_BASE_URL"));
        environmentVariablesXmlContextRule.set("METACAT_OSTI_V2XML_CONTEXT", xml);
        assertEquals(xml, System.getenv("METACAT_OSTI_V2XML_CONTEXT"));
        environmentVariablesJsonContextRule.set("METACAT_OSTI_V2JSON_CONTEXT", json);
        assertEquals(json, System.getenv("METACAT_OSTI_V2JSON_CONTEXT"));
        service.constructURLs();
        assertEquals(testBaseURL + "/" + xml+ "/upload", service.getBaseUrl());
        assertEquals(testBaseURL + "/" + json, service.getQueryURL());
        assertEquals(testBaseURL + "/" + json + "/records", service.getV2RecordsURLURL());
        //Reset the correct contexts
        environmentVariablesXmlContextRule.set("METACAT_OSTI_V2XML_CONTEXT", "elink2xml");
        environmentVariablesJsonContextRule.set("METACAT_OSTI_V2JSON_CONTEXT", "elink2api");
        environmentVariablesURLRule.set("METACAT_OSTI_BASE_URL", testBaseURL);
        service.constructURLs();
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
        environmentVariablesRule.set("METACAT_OSTI_TOKEN", FAKE_TOKEN);
        assertEquals(FAKE_TOKEN, System.getenv("METACAT_OSTI_TOKEN"));
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
        assertEquals("Saved", status);
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

    /**
     * Test the method of setMetadata
     * @throws Exception
     */
    @Test
    public void testSetMetadata() throws Exception {
        String identifier = service.mintIdentifier(null);
        assertTrue(identifier.startsWith("doi:10."));
        identifier = OSTIElinkService.removeDOI(identifier);
        int index = 0;
        String metadata = null;
        while (index <= MAX_ATTEMPTS) {
            try {
                metadata = service.getMetadata(identifier);
                break;
            } catch (Exception e) {
                Thread.sleep(200);
            }
            index++;
        }
        assertTrue(metadata.contains(identifier));
        assertTrue(metadata.contains("\"title\":\"unknown\""));

        try (InputStream is = getClass().getClassLoader()
            .getResourceAsStream("test-files/input-two-osti-id.xml")) {
            String newMetadata = IOUtils.toString(is, StandardCharsets.UTF_8);
            try {
                service.setMetadata(identifier, null, newMetadata);
                fail("Test can't reach here");
            } catch (Exception e) {
                assertTrue(e instanceof OSTIElinkException);
            }
            metadata = service.getMetadata(identifier);
            assertTrue(metadata.contains(identifier));
            assertTrue(metadata.contains("\"title\":\"unknown\""));
        }

        try (InputStream is = getClass().getClassLoader()
            .getResourceAsStream("test-files/input-no-osti-id.xml")) {
            String newMetadata = IOUtils.toString(is, StandardCharsets.UTF_8);
            service.setMetadata(identifier, null, newMetadata);
            index = 0;
            metadata = service.getMetadata(identifier);

            while (!metadata.contains("\"title\":\"0 - Data from Raczka et al., Interactions "
                                         + "between") && index < MAX_ATTEMPTS) {
                Thread.sleep(200);
                index++;
                metadata = service.getMetadata(identifier);
            }
            assertTrue(metadata.contains(identifier));
            assertTrue(
                metadata.contains("\"title\":\"0 - Data from Raczka et al., Interactions between"));
        }

        try (InputStream is = getClass().getClassLoader()
            .getResourceAsStream("test-files/input-one-osti-id.xml")) {
            String newMetadata = IOUtils.toString(is, StandardCharsets.UTF_8);
            service.setMetadata(identifier, null, newMetadata);
            metadata = service.getMetadata(identifier);
            index = 0;
            metadata = service.getMetadata(identifier);
            while (!metadata.contains("\"title\":\"1 - Data from Raczka et al., Interactions "
                                          + "between") && index < MAX_ATTEMPTS) {
                Thread.sleep(200);
                index++;
                metadata = service.getMetadata(identifier);

            }
            assertTrue(metadata.contains(identifier));
            assertTrue(
                metadata.contains("\"title\":\"1 - Data from Raczka et al., Interactions "
                                      + "between"));
        }
    }

    /**
     * Test publishIdentifier command, which is special xml document in the setMetadata method.
     * The client only
     */
    @Test
    public void testPublishIdentifierCommand() throws Exception {
        String orgIdentifier = service.mintIdentifier(null);
        String identifier = OSTIElinkService.removeDOI(orgIdentifier);
        int index = 0;
        String metadata = null;
        while (index <= MAX_ATTEMPTS) {
            try {
                metadata = service.getMetadata(identifier);
                break;
            } catch (Exception e) {
                Thread.sleep(200);
            }
            index++;
        }
        assertTrue(metadata.contains(identifier));
        String siteUrl = "https://data.ess-dive.lbl.gov/view/" + orgIdentifier;
        String command = OSTIServiceV1Test.generatePublishIdentifierCommandWithSiteURL(siteUrl);
        service.setMetadata(identifier, null, command);
        String status = service.getStatus(identifier);
        index = 0;
        while (index <= MAX_ATTEMPTS && !status.equals("R")) {
            Thread.sleep(200);
            status = service.getStatus(identifier);
            index++;
        }
        assertTrue(status.equals("R"));
        metadata = service.getMetadata(identifier);
        assertTrue(metadata.contains(siteUrl));
    }
}
