package edu.ucsb.nceas.osti_elink;

import edu.ucsb.nceas.osti_elink.v2.xml.OSTIv2XmlService;
import edu.ucsb.nceas.osti_elink.v2.xml.OSTIv2XmlServiceTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
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
            OSTIServiceFactory.OSTISERVICE_CLASS_NAME, v2ClassName);
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
     * Test the mintIdentifier method
     * @throws Exception
     */
    @Test
    public void testSetAndGetMetadata() throws Exception {
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
    }

}
