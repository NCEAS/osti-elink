package edu.ucsb.nceas.osti_elink;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import edu.ucsb.nceas.osti_elink.v1.OSTIService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class OSTIElinkClientV1Test {
    private OSTIElinkClient client = null;
    private OSTIElinkErrorAgent agent = null;
    private static String username = "";
    private static String password = "";


    @Before
    public void setUp() throws Exception {
        Properties prop = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("test.properties")) {
            prop.load(is);
            String passwordFile = prop.getProperty(OSTIServiceV1Test.PASSWORD_FILE_PROP_NAME);
            try (InputStream passwordStream = new FileInputStream(new File(passwordFile))) {
                Properties passwordProp = new Properties();
                passwordProp.load(passwordStream);
                username = passwordProp.getProperty(OSTIElinkClient.USER_NAME_PROPERTY);
                password = passwordProp.getProperty(OSTIElinkClient.PASSWORD_PROPERTY);
            }
        }
        prop.setProperty(
            OSTIServiceFactory.OSTISERVICE_CLASSNAME_PROPERTY,
            "edu.ucsb.nceas.osti_elink.v1.OSTIService");
        OSTIElinkClient.setProperties(prop);
        client = new OSTIElinkClient(username, password, OSTIServiceV1Test.BASEURL, agent);
    }
    
    @After
    public void tearDown() {
        client.shutdown();
    }

    /**
     * Test the getService method
     */
    @Test
    public void testGetService() throws Exception {
        assertTrue(client.getService() instanceof OSTIService);
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
        //System.out.println("the doi identifier is " + identifier);
        String metadata = client.getMetadata(identifier);
        assertTrue(metadata.contains(identifier));
        assertTrue(metadata.contains("<title>unknown</title>"));
        String status = client.getStatus(identifier);
        assertTrue(status.equals("Saved"));

        try (InputStream is = getClass().getClassLoader()
            .getResourceAsStream("test-files/input-two-osti-id.xml")) {
            String newMetadata = OSTIServiceV1Test.toString(is);
            //System.out.println("the new metadata is " + newMetadata);
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

        try (InputStream is = getClass().getClassLoader()
            .getResourceAsStream("test-files/input-no-osti-id.xml")) {
            String newMetadata = OSTIServiceV1Test.toString(is);
            client.setMetadata(identifier, newMetadata);
            Thread.sleep(1000);
            metadata = client.getMetadata(identifier);
            assertTrue(metadata.contains(identifier));
            assertTrue(metadata.contains("<title>0 - Data from Raczka et al., Interactions between"));
            status = client.getStatus(identifier);
            assertTrue(status.equals("Pending"));
        }

        try (InputStream is = getClass().getClassLoader()
            .getResourceAsStream("test-files/input-one-osti-id.xml")) {
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
