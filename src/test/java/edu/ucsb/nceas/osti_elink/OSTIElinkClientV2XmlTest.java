package edu.ucsb.nceas.osti_elink;

import edu.ucsb.nceas.osti_elink.v2.xml.OSTIv2XmlService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Properties;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test the OSTIElinkClient class connecting a v2xml api
 * @author Tao
 */
public class OSTIElinkClientV2XmlTest {

    private OSTIElinkClient client = null;
    private OSTIElinkErrorAgent agent = null;


    @Before
    public void setUp() throws Exception {
        Properties prop = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("test.properties")) {
            prop.load(is);
        }
        prop.setProperty(
            OSTIServiceFactory.OSTISERVICE_CLASS_NAME,
            "edu.ucsb.nceas.osti_elink.v2.OSTIv2XmlService");
        OSTIElinkClient.setProperties(prop);
        String baseURL = prop.getProperty("ostiService.v2.baseURL");
        //Username and password are null
        client = new OSTIElinkClient(null, null, baseURL, agent);
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
        String metadata = client.getMetadata(identifier);
        assertTrue(metadata.contains(identifier));
        assertTrue(metadata.contains("<title>unknown</title>"));
        String status = client.getStatus(identifier);
        assertTrue(status.equals("Saved"));

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("test-files/input-two-osti-id.xml")) {
            String newMetadata = OSTIServiceV1Test.toString(is);
            //even though this request should fail in the server side, this test
            //still succeed since it is running on another thread.
            client.setMetadata(identifier,newMetadata);
            Thread.sleep(1000);
            metadata = client.getMetadata(identifier);
            assertTrue(metadata.contains(identifier));
            assertTrue(metadata.contains("<title>unknown</title>"));
            status = client.getStatus(identifier);
            assertTrue(status.equals("Saved"));
        }

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("test-files/input-no-osti-id.xml")) {
            String newMetadata = OSTIServiceV1Test.toString(is);
            client.setMetadata(identifier, newMetadata);
            Thread.sleep(1000);
            metadata = client.getMetadata(identifier);
            assertTrue(metadata.contains(identifier));
            assertTrue(metadata.contains("<title>0 - Data from Raczka et al., Interactions between"));
            status = client.getStatus(identifier);
            assertTrue(status.equals("Pending"));
        }

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("test-files/input-one-osti-id.xml")) {
            String newMetadata = OSTIServiceV1Test.toString(is);
            client.setMetadata(identifier, newMetadata);
            Thread.sleep(5000);
            metadata = client.getMetadata(identifier);
            assertTrue(metadata.contains(identifier));
            assertTrue(metadata.contains("<title>1 - Data from Raczka et al., Interactions between"));
            status = client.getStatus(identifier);
            assertTrue(status.equals("Pending"));
        }
    }

}
