package edu.ucsb.nceas.osti_elink.v2.json;


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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The junit test class of OSTIv2JsonService.
 * @author Shalki Shrivastava
 */
public class OSTIv2JsonServiceTest {
    public static final String testBaseURL = "https://review.osti.gov";
    public static final int MAX_ATTEMPTS = 200;
    private static final String FAKE_TOKEN = "fake_token";
    private OSTIv2JsonService service;
    private Properties props;
    @Rule
    public EnvironmentVariablesRule environmentVariablesRule =
        new EnvironmentVariablesRule("METACAT_OSTI_TOKEN", null);
    @Rule
    public EnvironmentVariablesRule environmentVariablesURLRule =
        new EnvironmentVariablesRule("METACAT_OSTI_BASE_URL", null);
   @Rule
    public EnvironmentVariablesRule environmentVariablesJsonContextRule =
        new EnvironmentVariablesRule("METACAT_OSTI_V2JSON_CONTEXT", null);

    @Rule
    public EnvironmentVariablesRule environmentVariablesMaxQueryAttemptRule =
        new EnvironmentVariablesRule("METACAT_OSTI_DOI_QUERY_MAX_ATTEMPTS", null);

    @Before
    public void setUp() throws Exception {
        props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("test.properties")) {
            props.load(is);
        }
        //username and password are null (it uses token)
        service = new OSTIv2JsonService(null, null, testBaseURL, props);
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
        assertEquals(testBaseURL + "/" + "elink2api", service.getQueryUrl());
        // Set the env variable
        environmentVariablesURLRule.set("METACAT_OSTI_BASE_URL", "https://foo.com");
        assertEquals("https://foo.com", System.getenv("METACAT_OSTI_BASE_URL"));
        service.constructURLs();
        assertEquals("https://foo.com" + "/" + "elink2xml/upload", service.getBaseUrl());
        assertEquals("https://foo.com" + "/" + "elink2api", service.getQueryUrl());
        assertEquals("https://foo.com" + "/" + "elink2api/records", service.getRecordsEndpointURL());
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
        assertEquals(testBaseURL + "/" + "elink2api", service.getQueryUrl());
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
     * Test the mint method with the invalid token
     * @throws Exception
     */
    @Test
    public void testMintingWithInvalidToken() throws Exception {
        // Set the env variable
        environmentVariablesRule.set("METACAT_OSTI_TOKEN", FAKE_TOKEN);
        assertEquals(FAKE_TOKEN, System.getenv("METACAT_OSTI_TOKEN"));
        service.loadToken();
        assertEquals(FAKE_TOKEN, service.token);
        try {
            service.mintIdentifier(null);
            fail("Test shouldn't get here since the minting should fail because of the invalid "
                     + "token");
        } catch (Exception e) {
            assertTrue(e instanceof OSTIElinkException);
        }
        try {
            service.mintIdentifier("ESS-DIVE");
            fail("Test shouldn't get here since the minting should fail because of the invalid "
                     + "token");
        } catch (Exception e) {
            assertTrue(e instanceof OSTIElinkException);
        }
        try {
            service.mintIdentifier("KNB");
            fail("Test shouldn't get here since the minting should fail because of the invalid "
                     + "token");
        } catch (Exception e) {
            assertTrue(e instanceof OSTIElinkException);
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
        String status = service.getStatus(identifier);
        assertEquals("Saved", status); // todo what should the status be?
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
     * Test the method of setMetadata with the valid token
     * @throws Exception
     */
    @Test
    public void testSetMetadataWithInvalidToken() throws Exception {
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
            .getResourceAsStream("test-files/input-no-osti-id.xml")) { // todo update for the v2json workflow
            String newMetadata = IOUtils.toString(is, StandardCharsets.UTF_8);
            // Set the env variable
            environmentVariablesRule.set("METACAT_OSTI_TOKEN", FAKE_TOKEN);
            assertEquals(FAKE_TOKEN, System.getenv("METACAT_OSTI_TOKEN"));
            service.loadToken();
            assertEquals(FAKE_TOKEN, service.token);
            try {
                service.setMetadata(identifier, null, newMetadata);
                fail("Test can't reach here");
            } catch (Exception e) {
                assertTrue(e instanceof OSTIElinkException);
            }
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
            .getResourceAsStream("test-files/input-two-osti-id.xml")) { // todo update for the v2json workflow
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
        assertTrue(metadata.contains(identifier)); // todo update for the v2json workflow
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
        // Try another site url to test the new site url will replace the old one.
        String newSiteUrl = "https://knb.ecoinformatics/view/" + orgIdentifier;
        command = OSTIServiceV1Test.generatePublishIdentifierCommandWithSiteURL(newSiteUrl);
        service.setMetadata(identifier, null, command);
        metadata = service.getMetadata(identifier);
        index = 0;
        while (index <= MAX_ATTEMPTS && !metadata.contains(newSiteUrl)) {
            Thread.sleep(200);
            status = service.getStatus(identifier);
            index++;
        }
        assertTrue(status.equals("R"));
        metadata = service.getMetadata(identifier);
        // Old site url should be gone
        assertFalse(metadata.contains(siteUrl));
        // New site url should be there
        assertTrue(metadata.contains(newSiteUrl));
    }

    /**
     * Test the default max query attempts
     * @throws Exception
     */
    @Test
    public void testDefaultMaxQueryAttempts() throws Exception {
        assertEquals(40, service.getMaxAttempts());
    }

    /**
     * Test the scenario to get the max query attempts from the env variable
     * @throws Exception
     */
    @Test
    public void testGetMaxQueryAttemptsFromEnv() throws Exception {
        // An empty string on the env variable doesn't change the default value
        environmentVariablesMaxQueryAttemptRule.set("METACAT_OSTI_DOI_QUERY_MAX_ATTEMPTS", "");
        OSTIv2JsonService service2 = new OSTIv2JsonService(null, null, testBaseURL, props);
        assertEquals(40, service2.getMaxAttempts());
        // A non-number string on the env variable doesn't change the default value
        environmentVariablesMaxQueryAttemptRule.set("METACAT_OSTI_DOI_QUERY_MAX_ATTEMPTS", "wer");
        OSTIv2JsonService service3 = new OSTIv2JsonService(null, null, testBaseURL, props);
        assertEquals(40, service3.getMaxAttempts());
        // A number string on the env variable changes the default value
        environmentVariablesMaxQueryAttemptRule.set("METACAT_OSTI_DOI_QUERY_MAX_ATTEMPTS", "100");
        OSTIv2JsonService service4 = new OSTIv2JsonService(null, null, testBaseURL, props);
        assertEquals(100, service4.getMaxAttempts());
    }
}
