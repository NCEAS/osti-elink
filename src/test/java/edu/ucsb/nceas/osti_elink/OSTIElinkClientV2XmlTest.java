package edu.ucsb.nceas.osti_elink;

import edu.ucsb.nceas.osti_elink.v2.xml.OSTIv2XmlService;
import edu.ucsb.nceas.osti_elink.v2.xml.OSTIv2XmlServiceTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test the OSTIElinkClient class connecting a v2xml api
 * @author Tao
 */
public class OSTIElinkClientV2XmlTest {
    private static final String v2ClassName = "edu.ucsb.nceas.osti_elink.v2.xml.OSTIv2XmlService";
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
        client = new OSTIElinkClient(null, null, OSTIv2XmlServiceTest.testBaseURL, agent);
    }

    @After
    public void tearDown() {
        client.shutdown();
    }

    /**
     * Test the getService method
     */
    @Test
    public void testGetService() {
        assertTrue(client.getService() instanceof OSTIv2XmlService);
    }

    /**
     * Test the mintIdentifier method
     * @throws Exception
     */
    @Test
    public void testMintIdentifier() throws Exception {
        String doi = client.mintIdentifier(null);
        assertTrue(doi.startsWith("doi:"));
        doi = client.mintIdentifier("ESS-DIVE");
        assertTrue(doi.startsWith("doi:"));
        try {
            doi = client.mintIdentifier("KNB");
            fail("test shouldn't get here");
        } catch (Exception e) {
            assertTrue(e instanceof OSTIElinkException);
        }
    }

    /**
     * Test the set/get metadata and publish methods
     * @throws Exception
     */
    @Test
    public void testSetAndGetMetadataAndPublish() throws Exception {
        String identifier = client.mintIdentifier(null);
        assertTrue(identifier.startsWith("doi:10."));
        identifier = OSTIElinkService.removeDOI(identifier);
        int index = 0;
        String metadata = null;
        while (index <= OSTIv2XmlServiceTest.MAX_ATTEMPTS) {
            try {
                metadata = client.getMetadata(identifier);
                break;
            } catch (Exception e) {
                Thread.sleep(200);
            }
            index++;
        }
        assertTrue(metadata.contains(identifier));
        assertTrue(metadata.contains("\"title\":\"unknown\""));
        String status = client.getStatus(identifier);
        assertEquals("Saved", status);

        try (InputStream is = getClass().getClassLoader()
            .getResourceAsStream("test-files/input-two-osti-id.xml")) {
            String newMetadata = OSTIServiceV1Test.toString(is);
            //even though this request should fail in the server side, this test
            //still succeed since it is running on another thread.
            client.setMetadata(identifier,newMetadata);
            Thread.sleep(1000);
            metadata = client.getMetadata(identifier);
            assertTrue(metadata.contains(identifier));
            assertTrue(metadata.contains("\"title\":\"unknown\""));
            status = client.getStatus(identifier);
            assertEquals("Saved", status);
        }

        try (InputStream is = getClass().getClassLoader()
            .getResourceAsStream("test-files/input-no-osti-id.xml")) {
            String newMetadata = OSTIServiceV1Test.toString(is);
            client.setMetadata(identifier, newMetadata);
            index = 0;
            metadata = client.getMetadata(identifier);
            while (!metadata.contains(
                "\"title\":\"0 - Data from Raczka et al., Interactions " + "between")
                && index < OSTIv2XmlServiceTest.MAX_ATTEMPTS) {
                Thread.sleep(200);
                index++;
                metadata = client.getMetadata(identifier);
            }
            assertTrue(metadata.contains(identifier));
            assertTrue(
                metadata.contains("\"title\":\"0 - Data from Raczka et al., Interactions between"));
            status = client.getStatus(identifier);
            assertNotEquals("Saved", status);
        }

        try (InputStream is = getClass().getClassLoader()
            .getResourceAsStream("test-files/input-one-osti-id.xml")) {
            String newMetadata = OSTIServiceV1Test.toString(is);
            client.setMetadata(identifier, newMetadata);
            metadata = client.getMetadata(identifier);
            index = 0;
            metadata = client.getMetadata(identifier);
            while (!metadata.contains(
                "\"title\":\"1 - Data from Raczka et al., Interactions between")
                && index < OSTIv2XmlServiceTest.MAX_ATTEMPTS) {
                Thread.sleep(200);
                index++;
                metadata = client.getMetadata(identifier);
            }
            assertTrue(metadata.contains(identifier));
            assertTrue(
                metadata.contains("\"title\":\"1 - Data from Raczka et al., Interactions "
                                      + "between"));
            status = client.getStatus(identifier);
            assertNotEquals("Saved", status);
        }
        String siteUrl = "https://data.ess-dive.lbl.gov/view/" + identifier;
        String command = OSTIServiceV1Test.generatePublishIdentifierCommandWithSiteURL(siteUrl);
        client.setMetadata(identifier, command);
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
            metadata.contains("\"title\":\"1 - Data from Raczka et al., Interactions "
                                  + "between"));
        status = client.getStatus(identifier);
        assertEquals("R", status);
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
            .getResourceAsStream("test-files/input-no-osti-id-without-site-url.xml")) {
            String newMetadata = OSTIServiceV1Test.toString(is);
            client.setMetadata(identifier, newMetadata);
            index = 0;
            metadata = client.getMetadata(identifier);
            while (!metadata.contains(
                "\"title\":\"0 - Data from Raczka et al., Interactions " + "between")
                && index < OSTIv2XmlServiceTest.MAX_ATTEMPTS) {
                Thread.sleep(200);
                index++;
                metadata = client.getMetadata(identifier);
            }
            assertTrue(metadata.contains(identifier));
            assertTrue(
                metadata.contains("\"title\":\"0 - Data from Raczka et al., Interactions between"));
            status = client.getStatus(identifier);
            Thread.sleep(2000);
            assertEquals("Saved", status);
        }

        // Publish the object
        String siteUrl =
            "https://knb.ecoinformatics.org/view/urn%3Auuid%3A1651eeb1-e050-4c78-8410-ec2389ca2363";
        String publish = OSTIServiceV1Test.generatePublishIdentifierCommandWithSiteURL(siteUrl);
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
            metadata.contains("\"title\":\"0 - Data from Raczka et al., Interactions between"));
        Thread.sleep(2000);
        status = client.getStatus(identifier);
        assertEquals("R", status);

        // Reset a new URL by the publish command
        String newSiteUrl =
            "https://knb.ecoinformatics.org/view/urn%3Auuid%3A90dfc355-2f29-4eb5-be9a-b742df13b323";
        publish = OSTIServiceV1Test.generatePublishIdentifierCommandWithSiteURL(newSiteUrl);
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
            metadata.contains("\"title\":\"0 - Data from Raczka et al., Interactions between"));
        Thread.sleep(2000);
        status = client.getStatus(identifier);
        assertEquals("R", status);

        //Set a new metadata with the site url
        try (InputStream is = getClass().getClassLoader()
            .getResourceAsStream("test-files/input-one-osti-id.xml")) {
            String newMetadata = OSTIServiceV1Test.toString(is);
            client.setMetadata(identifier, newMetadata);
            index = 0;
            metadata = client.getMetadata(identifier);
            while (
                !metadata.contains("\"title\":\"1 - Data from Raczka et al., Interactions between")
                    && index < OSTIv2XmlServiceTest.MAX_ATTEMPTS) {
                Thread.sleep(200);
                index++;
                metadata = client.getMetadata(identifier);
            }
            assertFalse(metadata.contains(newSiteUrl));
            assertFalse(metadata.contains(siteUrl));
            assertTrue(metadata.contains("https://data.ess-dive.lbl.gov/view/doi:10.15485/1829502"));
            assertTrue(metadata.contains(identifier));
            assertTrue(metadata.contains(
                "\"title\":\"1 - Data from Raczka et al., Interactions " + "between"));
            index = 0;
            status = client.getStatus(identifier);
            while (!status.equals("R") && index < OSTIv2XmlServiceTest.MAX_ATTEMPTS) {
                Thread.sleep(200);
                index++;
                status = client.getStatus(identifier);
            }
            Thread.sleep(2000);
            assertEquals("R", status);
        }

        //Set a new metadata with the set_reserved. The status will change to "Saved".
        try (InputStream is = getClass().getClassLoader()
            .getResourceAsStream("test-files/input-no-osti-id-without-site-url.xml")) {
            String newMetadata = OSTIServiceV1Test.toString(is);
            client.setMetadata(identifier, newMetadata);
            index = 0;
            metadata = client.getMetadata(identifier);
            while (
                !metadata.contains("\"title\":\"0 - Data from Raczka et al., Interactions between")
                    && index < OSTIv2XmlServiceTest.MAX_ATTEMPTS) {
                Thread.sleep(200);
                index++;
                metadata = client.getMetadata(identifier);
            }
            System.out.println("metadata\n" + metadata);
            assertFalse(metadata.contains(newSiteUrl));
            assertFalse(metadata.contains(siteUrl));
            assertTrue(metadata.contains("https://data.ess-dive.lbl.gov/view/doi:10"
                                             + ".15485/1829502"));
            assertTrue(metadata.contains(identifier));
            assertTrue(metadata.contains(
                "\"title\":\"0 - Data from Raczka et al., Interactions " + "between"));
            index = 0;
            status = client.getStatus(identifier);
            while (!status.equals("Saved") && index < OSTIv2XmlServiceTest.MAX_ATTEMPTS) {
                Thread.sleep(200);
                index++;
                status = client.getStatus(identifier);
            }
            assertEquals("Saved", status);
        }
    }

}
