package edu.ucsb.nceas.osti_elink;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

import edu.ucsb.nceas.osti_elink.v2.json.OSTIv2JsonServiceTest;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import uk.org.webcompere.systemstubs.rules.EnvironmentVariablesRule;

import java.nio.charset.StandardCharsets;

/**
 * Test if the OSTIElinkErrorAgent can catch exceptions correctly for v2json service.
 * We use the simple string implementation class - StringElinkErrorAgent as the example
 * @author tao
 */
public class OSTIElinkErrorAgentTest {
    private OSTIElinkClient client = null;
    private StringElinkErrorAgent agent = null;

    public static final int MAX_ATTEMPTS = 5;
    public static final String testBaseURL = "https://review.osti.gov/";

    @Rule
    public EnvironmentVariablesRule environmentVariablesRule =
            new EnvironmentVariablesRule("METACAT_OSTI_TOKEN", null);

    @Rule
    public EnvironmentVariablesRule environmentVariablesURLRule =
            new EnvironmentVariablesRule("METACAT_OSTI_BASE_URL", "https://review.osti.gov");

    @Rule
    public EnvironmentVariablesRule environmentVariablesJsonContextRule =
            new EnvironmentVariablesRule("METACAT_OSTI_V2JSON_CONTEXT", "elink2api");

    @Rule
    public EnvironmentVariablesRule environmentVariablesMinimalMetadataFileRule =
            new EnvironmentVariablesRule("METACAT_OSTI_MINIMAL_METADATA_FILE", "test-files/minimal-osti-test.json");

    @Before
    public void setUp() throws Exception {
        Properties prop = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("test.properties")) {
            prop.load(is);
        }

        // Configure for v2json service
        prop.setProperty(
                OSTIServiceFactory.OSTISERVICE_CLASSNAME_PROPERTY,
                "edu.ucsb.nceas.osti_elink.v2.json.OSTIv2JsonService");

        OSTIElinkClient.setProperties(prop);
        agent = new StringElinkErrorAgent();

        // v2json uses token auth, not username/password
        client = new OSTIElinkClient(null, null, testBaseURL, agent);
    }

    /**
     * Test the notify method
     * @throws Exception
     */
    @Test
    public void testNotify() throws Exception {
        String identifier = client.mintIdentifier(null);
        assertTrue(identifier.startsWith("doi:10."));
        identifier = OSTIElinkService.removeDOI(identifier);

        // Wait for the DOI to become searchable (like in testPublishIdentifierCommand)
        int index = 0;
        String metadata = null;
        while (index <= MAX_ATTEMPTS) {
            try {
                metadata = client.getMetadata(identifier);
                break;
            } catch (OSTIElinkNotFoundException e) {
                Thread.sleep(1000); // Wait longer than 200ms
                index++;
            }
        }

        if (metadata == null) {
            fail("DOI " + identifier + " never became searchable after " + MAX_ATTEMPTS + " attempts");
        }

        assertTrue(metadata.contains(identifier));
        assertTrue(metadata.contains("\"title\":\"unknown\""));

        // Things should work and the error should be blank
        assertTrue(agent.getError().equals(""));

        // Test 1: JSON without osti_id (should work)
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("test-files/input-no-osti-id.json")) {
            String newMetadata = IOUtils.toString(is, StandardCharsets.UTF_8);
            client.setMetadata(identifier, newMetadata);

            index = 0;
            int delay = 1000;
            metadata = null;
            while (index <= MAX_ATTEMPTS) {
                try {
                    metadata = client.getMetadata(identifier);
                    if (metadata.contains("\"title\":\"0 - Data from Raczka et al., Interactions between"))
                        break;
                } catch (OSTIElinkNotFoundException e) {
                    Thread.sleep(delay); // Wait longer than 200ms
                    delay *= 2;
                    index++;
                }
            }

            assertTrue(metadata.contains(identifier));
            // Check for JSON format
            assertTrue(metadata.contains("\"title\":\"0 - Data from Raczka et al., Interactions between"));
            // Things should work and the error should be blank
            assertTrue(agent.getError().equals(""));
        }

        // Test 2: JSON with two osti_ids (should work)
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("test-files/input-two-osti-id.json")) {
            String newMetadata = IOUtils.toString(is, StandardCharsets.UTF_8);
            client.setMetadata(identifier, newMetadata);

            index = 0;
            int delay = 1000;
            metadata = null;
            while (index <= MAX_ATTEMPTS) {
                try {
                    metadata = client.getMetadata(identifier);
                    if (metadata.contains("\"title\":\"2 - Data from Raczka et al., Interactions between"))
                        break;
                } catch (OSTIElinkNotFoundException e) {
                    Thread.sleep(delay); // Wait longer than 200ms
                    delay *= 2;
                    index++;
                }
            }

            assertTrue(metadata.contains(identifier));
            assertTrue(metadata.contains("\"title\":\"2 - Data from Raczka et al., Interactions between"));
            // Things should work and the error should be blank
            assertTrue(agent.getError().equals(""));
        }

        // Test 3: Invalid DOI (set metadata request should throw an exception)
        String uuid = UUID.randomUUID().toString();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("test-files/input-one-osti-id.json")) {
            String newMetadata = IOUtils.toString(is, StandardCharsets.UTF_8);
            client.setMetadata(uuid, newMetadata);

//            index = 0;
//            int delay = 1000;
//            metadata = null;
//            while (index <= MAX_ATTEMPTS) {
//                try {
//                    metadata = client.getMetadata(identifier);
//                    if (metadata.contains("\"title\":\"2 - Data from Raczka et al., Interactions between"))
//                        break;
//                } catch (OSTIElinkNotFoundException e) {
//                    Thread.sleep(delay);
//                    delay *= 2;
//                    index++;
//                }
            }

//            assertTrue(metadata.contains(identifier));
//            assertTrue(metadata.contains("\"title\":\"2 - Data from Raczka et al., Interactions between"));
//            assertTrue(agent.getError().contains(uuid));
//        }

        // Test 4: Invalid site code (should fail immediately)
        final String KNB = "KNB";
        try {
            String newDOI = client.mintIdentifier(KNB);
            fail("Test can't reach here");
        } catch (Exception e) {
            assertTrue(e instanceof OSTIElinkException);
        }

        client.shutdown();

        // Verify error agent caught the expected errors
        String errorMessage = agent.getError();
        System.out.println("Error message from agent: " + errorMessage);

        assertTrue("Should contain the UUID that failed", errorMessage.contains(uuid));
        assertTrue("Should contain 403 error about KNB site code", errorMessage.contains("{\"errors\":[{\"status\":\"403\",\"detail\":\"Permission to create new record denied.\"}]}"));
        assertTrue("Requests should fail with errors due to invalid doi and site_ownership_code", !agent.getError().isEmpty());
    }
}