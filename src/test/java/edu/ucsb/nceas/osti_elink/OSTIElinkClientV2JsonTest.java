package edu.ucsb.nceas.osti_elink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.ucsb.nceas.osti_elink.v2.json.OSTIv2JsonServiceTest;
import edu.ucsb.nceas.osti_elink.v2.xml.OSTIv2XmlServiceTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test the client which connects a osti v2 Json service
 * @author Tao
 */
public class OSTIElinkClientV2JsonTest {
    private static final String v2ClassName = "edu.ucsb.nceas.osti_elink.v2.json.OSTIv2JsonService";
    private OSTIElinkClient client = null;
    private OSTIElinkErrorAgent agent = null;


    @Before
    public void setUp() throws Exception {
        Properties prop = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("test.properties")) {
            prop.load(is);
        }
        prop.setProperty(
            OSTIServiceFactory.OSTISERVICE_CLASSNAME_PROPERTY, v2ClassName);
        OSTIElinkClient.setProperties(prop);
        //Username and password are null
        client = new OSTIElinkClient(null, null, OSTIv2JsonServiceTest.testBaseURL, agent);
    }

    @After
    public void tearDown() {
        client.shutdown();
    }

    /**
     * Test the mint, set the rich metadata, publish identifier and update the metadata processes
     * @throws Exception
     */
    @Test
    public void testFullCycle() throws Exception {
        //Mint the doi
        String identifier = client.mintIdentifier(null);
        assertTrue(identifier.startsWith("doi:10."));
        identifier = OSTIElinkService.removeDOI(identifier);
        int index = 0;

        String status = client.getStatus(identifier);
        assertEquals("Saved", status);
        String metadata = client.getMetadata(identifier);
        assertTrue(metadata.contains(identifier));
        assertTrue(metadata.contains("\"title\":\"unknown\""));


        // Set the rich metadata without the site url
        try (InputStream is = getClass().getClassLoader()
            .getResourceAsStream("test-files/input-no-osti-id-without-site-url.json")) {
            String newMetadata = OSTIv2XmlServiceTest.toString(is);
            client.setMetadata(identifier, newMetadata);
            index = 0;
            metadata = client.getMetadata(identifier);
            while (!metadata.contains(
                "\"title\":\"Specific conductivity")
                && index < OSTIv2XmlServiceTest.MAX_ATTEMPTS) {
                Thread.sleep(200);
                index++;
                metadata = client.getMetadata(identifier);
            }
            assertTrue(metadata.contains(identifier));
            assertTrue(
                metadata.contains("\"title\":\"Specific conductivity"));
            status = client.getStatus(identifier);
            Thread.sleep(2000);
            assertEquals("Saved", status);
        }

        // Publish the object
        String siteUrl =
            "https://knb.ecoinformatics.org/view/urn%3Auuid%3A1651eeb1-e050-4c78-8410-ec2389ca2363";
        String publish = generatePublishIdentifierCommandWithSiteURL(siteUrl);
        client.setMetadata(identifier, publish);
        index = 0;
        status = client.getStatus(identifier);
        while (!status.equals("R") && index < OSTIv2XmlServiceTest.MAX_ATTEMPTS) {
            Thread.sleep(200);
            index++;
            status = client.getStatus(identifier);
        }
        metadata = client.getMetadata(identifier);
        assertTrue(metadata.contains(identifier));
        assertTrue(metadata.contains(siteUrl));
        assertTrue(
            metadata.contains("\"title\":\"Specific conductivity"));
        Thread.sleep(2000);
        status = client.getStatus(identifier);
        assertEquals("R", status);

        // Reset a new URL by the publish command
        String newSiteUrl =
            "https://knb.ecoinformatics.org/view/urn%3Auuid%3A90dfc355-2f29-4eb5-be9a-b742df13b323";
        publish = generatePublishIdentifierCommandWithSiteURL(newSiteUrl);
        client.setMetadata(identifier, publish);
        index = 0;
        metadata = client.getMetadata(identifier);
        while (!metadata.contains(newSiteUrl) && index < OSTIv2XmlServiceTest.MAX_ATTEMPTS) {
            Thread.sleep(200);
            index++;
            metadata = client.getMetadata(identifier);
        }
        index = 0;
        status = client.getStatus(identifier);
        while (!status.equals("R") && index < OSTIv2XmlServiceTest.MAX_ATTEMPTS) {
            Thread.sleep(200);
            index++;
            status = client.getStatus(identifier);
        }
        metadata = client.getMetadata(identifier);
        assertTrue(metadata.contains(identifier));
        // The new media replaced the old one
        assertTrue(metadata.contains(newSiteUrl));
        assertFalse(metadata.contains(siteUrl));
        assertTrue(
            metadata.contains("\"title\":\"Specific conductivity"));
        Thread.sleep(2000);
        status = client.getStatus(identifier);
        assertEquals("R", status);


        //Set a new metadata without the site url. The status will change to "Saved".
        try (InputStream is = getClass().getClassLoader()
            .getResourceAsStream("test-files/input-no-osti-id-without-site-url-2.json")) {
            String newMetadata = OSTIv2XmlServiceTest.toString(is);
            client.setMetadata(identifier, newMetadata);
            index = 0;
            status = client.getStatus(identifier);
            while (!status.equals("Saved") && index < OSTIv2XmlServiceTest.MAX_ATTEMPTS) {
                Thread.sleep(200);
                index++;
                status = client.getStatus(identifier);
            }
            assertEquals("Saved", status);
            metadata = client.getMetadata(identifier);
            assertTrue(metadata.contains(newSiteUrl));
            assertFalse(metadata.contains(siteUrl));
            assertTrue(metadata.contains(identifier));
            assertTrue(metadata.contains("\"title\":\"2. Specific conductivity"));
        }

        // Set the metadata with the site url. It should be R again
        try (InputStream is = getClass().getClassLoader()
            .getResourceAsStream("test-files/input-no-osti-id-without-site-url.json")) {
            String anotherSiteUrl = "https://knb.ecoinformatics.org/view/doi%3A10.5063%2FF1D50KFR";
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(is);
            ObjectNode objectNode = (ObjectNode) rootNode;
            objectNode.put(edu.ucsb.nceas.osti_elink.v2.json.PublishIdentifierCommand.SITE_URL,
                           anotherSiteUrl); // Add a new name/value pair
            // Convert the modified JsonNode back to a JSON string
            String modifiedJsonString = objectMapper.writeValueAsString(objectNode);
            client.setMetadata(identifier, modifiedJsonString);
            index = 0;
            status = client.getStatus(identifier);
            while (!status.equals("R") && index < OSTIv2XmlServiceTest.MAX_ATTEMPTS) {
                Thread.sleep(200);
                index++;
                status = client.getStatus(identifier);
            }
            assertEquals("R", status);
            metadata = client.getMetadata(identifier);
            assertTrue(metadata.contains(identifier));
            assertFalse(metadata.contains(newSiteUrl));
            assertFalse(metadata.contains(siteUrl));
            assertTrue(metadata.contains(anotherSiteUrl));
            assertTrue(metadata.contains("\"title\":\"Specific conductivity"));

        }

    }

    /**
     * Generate the publishing Json metadata
     * @param siteURL the given site url used in the metadata
     * @return the metadata to publish a doi
     * @throws JsonProcessingException
     */
    public static String generatePublishIdentifierCommandWithSiteURL(String siteURL)
        throws JsonProcessingException {
        Map<String, Object> params = new HashMap<>();
        params.put(edu.ucsb.nceas.osti_elink.v2.json.PublishIdentifierCommand.SITE_URL, siteURL);
        String payload = new ObjectMapper().writeValueAsString(params);
        return payload;
    }
}
