package edu.ucsb.nceas.osti_elink;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

/**
 * Test if the OSTIElinkErrorAgent can catch exceptions correctly.
 * We use the simple string implementation class - StringElinkErrorAgent as the example
 * @author tao
 *
 */
public class OSTIElinkErrorAgentTest {
    private OSTIElinkClient client = null;
    private StringElinkErrorAgent agent = null;
    private static String username = "";
    private static String password = "";


    @Before
    public void setUp() throws Exception {
        Properties prop = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("test.properties")) {
            prop.load(is);
            username = prop.getProperty("username");
            password = prop.getProperty("password");
        }
        prop.setProperty(
            OSTIServiceFactory.OSTISERVICE_CLASS_NAME, "edu.ucsb.nceas.osti_elink.v1.OSTIService");
        OSTIElinkClient.setProperties(prop);
        agent = new StringElinkErrorAgent();
        client = new OSTIElinkClient(username, password, OSTIServiceV1Test.BASEURL, agent);
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
        //System.out.println("the doi identifier is " + identifier);
        String metadata = client.getMetadata(identifier);
        assertTrue(metadata.contains(identifier));
        assertTrue(metadata.contains("<title>unknown</title>"));
        //things should work and the error should be blank
        assertTrue(agent.getError().equals(""));
        
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("test-files/input-no-osti-id.xml")) {
            String newMetadata = OSTIServiceV1Test.toString(is);
            client.setMetadata(identifier, newMetadata);
            Thread.sleep(1000);
            metadata = client.getMetadata(identifier);
            assertTrue(metadata.contains(identifier));
            assertTrue(metadata.contains("<title>0 - Data from Raczka et al., Interactions between"));
            //things should work and the error should be blank
            assertTrue(agent.getError().equals(""));
        }
        
        //even though this request should fail in the server side, this test
        //still succeed since it is running on another thread.
        //however, the error agent should catch the message
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("test-files/input-two-osti-id.xml")) {
            String newMetadata = OSTIServiceV1Test.toString(is);
            //System.out.println("the new metadata is " + newMetadata);
            client.setMetadata(identifier,newMetadata);
            Thread.sleep(1000);
            metadata = client.getMetadata(identifier);
            assertTrue(metadata.contains(identifier));
            assertTrue(metadata.contains("<title>0 - Data from Raczka et al., Interactions between"));   
        }
        
        //even though this request should fail in the server side (the doi doesn't exist), this test
        //still succeed since it is running on another thread.
        //however, the error agent should catch the message
        String uuid = UUID.randomUUID().toString();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("test-files/input-one-osti-id.xml")) {
            String newMetadata = OSTIServiceV1Test.toString(is);
            String doi = "doi:" + uuid;
            //System.out.println("the doi is " + uuid);
            client.setMetadata(uuid, newMetadata);
            Thread.sleep(1000);
            metadata = client.getMetadata(identifier);
            assertTrue(metadata.contains(identifier));
            assertTrue(metadata.contains("<title>0 - Data from Raczka et al., Interactions between"));
        }
        
        final String KNB = "KNB";
        try {
            String newDOI = client.mintIdentifier(KNB);
            fail("Test can't reach here");
        } catch (Exception e) {
            assertTrue(e instanceof OSTIElinkException);
        }
        client.shutdown();
        //System.out.println("the error message from agent is " + agent.getError());
        assertTrue(agent.getError().contains("the metadata shouldn't have more than one osti id"));
        assertTrue(agent.getError().contains(uuid));
        assertTrue(agent.getError().contains(KNB));
    }

}
