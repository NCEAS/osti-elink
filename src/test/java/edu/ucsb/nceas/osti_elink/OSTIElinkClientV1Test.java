package edu.ucsb.nceas.osti_elink;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import edu.ucsb.nceas.osti_elink.v1.OSTIService;
import edu.ucsb.nceas.osti_elink.v2.xml.OSTIv2XmlServiceTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

    /**
     * Test the mint, set the rich metadata, publish identifier and update the metadata processes
     * @throws Exception
     */
    @Test
    public void testFullCycle() throws Exception {
        //Mint a doi
        String identifier = client.mintIdentifier(null);
        assertTrue(identifier.startsWith("doi:10."));
        identifier = OSTIElinkService.removeDOI(identifier);
        //System.out.println("the doi identifier is " + identifier);
        String metadata = client.getMetadata(identifier);
        assertTrue(metadata.contains(identifier));
        assertTrue(metadata.contains("<title>unknown</title>"));
        String status = client.getStatus(identifier);
        assertTrue(status.equals("Saved"));

        // Set more rich metadata to the doi
        try (InputStream is = getClass().getClassLoader()
            .getResourceAsStream("test-files/input-no-osti-id-without-site-url.xml")) {
            String newMetadata = OSTIServiceV1Test.toString(is);
            client.setMetadata(identifier, newMetadata);
            int index = 0;
            metadata = client.getMetadata(identifier);
            while (!metadata.contains("<title>0 - Data from Raczka et al., Interactions between")
                && index < OSTIv2XmlServiceTest.MAX_ATTEMPTS) {
                Thread.sleep(200);
                index++;
                metadata = client.getMetadata(identifier);
            }
            metadata = client.getMetadata(identifier);
            assertTrue(metadata.contains(identifier));
            assertTrue(
                metadata.contains("<title>0 - Data from Raczka et al., Interactions between"));
            Thread.sleep(4000);
            status = client.getStatus(identifier);
            assertTrue(status.equals("Saved"));
        }

        //Generate the special command to publish the DOI
        String siteUrl = "https://knb.ecoinformatics/view/" + identifier;
        String publish = OSTIServiceV1Test.generatePublishIdentifierCommandWithSiteURL(siteUrl);
        client.setMetadata(identifier, publish);
        int index = 0;
        status = client.getStatus(identifier);
        while (!status.equals("Pending") && index < OSTIv2XmlServiceTest.MAX_ATTEMPTS) {
            Thread.sleep(200);
            index++;
            status = client.getStatus(identifier);
        }
        metadata = client.getMetadata(identifier);
        assertTrue(metadata.contains(identifier));
        assertTrue(metadata.contains(siteUrl));
        assertTrue(metadata.contains("<title>0 - Data from Raczka et al., Interactions between"));
        status = client.getStatus(identifier);
        assertEquals("Pending", status);

        // Reset a new site URL
        String newSiteUrl = "https://data.ess-dive.doe.gov/view/" + identifier;
        publish = OSTIServiceV1Test.generatePublishIdentifierCommandWithSiteURL(newSiteUrl);
        client.setMetadata(identifier, publish);
        index = 0;
        status = client.getStatus(identifier);
        metadata = client.getMetadata(identifier);
        while ((!metadata.contains(newSiteUrl) || !status.equals("Pending"))
            && index < OSTIv2XmlServiceTest.MAX_ATTEMPTS) {
            Thread.sleep(200);
            index++;
            status = client.getStatus(identifier);
            metadata = client.getMetadata(identifier);
        }
        assertTrue(metadata.contains(identifier));
        // The new site url replaced the old one
        assertTrue(metadata.contains(newSiteUrl));
        assertFalse(metadata.contains(siteUrl));
        assertTrue(metadata.contains("<title>0 - Data from Raczka et al., Interactions between"));
        Thread.sleep(2000);
        status = client.getStatus(identifier);
        assertEquals("Pending", status);

        //Set a new metadata with the site url
        try (InputStream is = getClass().getClassLoader()
            .getResourceAsStream("test-files/input-one-osti-id.xml")) {
            String newMetadata = OSTIServiceV1Test.toString(is);
            client.setMetadata(identifier, newMetadata);
            index = 0;
            metadata = client.getMetadata(identifier);
            while (
                !metadata.contains("<title>1 - Data from Raczka et al., Interactions between")
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
                "<title>1 - Data from Raczka et al., Interactions between"));
            index = 0;
            status = client.getStatus(identifier);
            while (!status.equals("Pending") && index < OSTIv2XmlServiceTest.MAX_ATTEMPTS) {
                Thread.sleep(200);
                index++;
                status = client.getStatus(identifier);
            }
            Thread.sleep(2000);
            assertEquals("Pending", status);
        }

        //Set a new metadata with the set_reserved. The status will change to "Saved".
        try (InputStream is = getClass().getClassLoader()
            .getResourceAsStream("test-files/input-no-osti-id-without-site-url.xml")) {
            String newMetadata = OSTIServiceV1Test.toString(is);
            client.setMetadata(identifier, newMetadata);
            index = 0;
            metadata = client.getMetadata(identifier);
            while (
                !metadata.contains("<title>0 - Data from Raczka et al., Interactions between")
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
                "<title>0 - Data from Raczka et al., Interactions between"));
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
