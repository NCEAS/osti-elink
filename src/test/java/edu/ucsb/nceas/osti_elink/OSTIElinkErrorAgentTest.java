/**
 * This work was created by the National Center for Ecological Analysis and Synthesis
 * at the University of California Santa Barbara (UCSB).
 *
 *   Copyright 2021 Regents of the University of California
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.ucsb.nceas.osti_elink;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    protected static Log log = LogFactory.getLog(OSTIElinkServiceTest.class);


    @Before
    public void setUp() throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("test.properties")) {
            Properties prop = new Properties();
            prop.load(is);
            username = prop.getProperty("username");
            password = prop.getProperty("password");
            //System.out.println("the user name is " + username + " and password is " + password);
        }
        agent = new StringElinkErrorAgent();
        client = new OSTIElinkClient(username, password, OSTIElinkServiceTest.BASEURL, agent);
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
            String newMetadata = OSTIElinkServiceTest.toString(is);
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
            String newMetadata = OSTIElinkServiceTest.toString(is);
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
            String newMetadata = OSTIElinkServiceTest.toString(is);
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
