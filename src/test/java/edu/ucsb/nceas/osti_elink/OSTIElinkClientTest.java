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

import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class OSTIElinkClientTest {
    private static OSTIElinkClient client = null;
    private static String username = "";
    private static String password = "";
    
    public static final String BASEURL = "https://www.osti.gov/elinktest/2416api";
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
        client = new OSTIElinkClient(username, password, BASEURL);
    }
    
    @After
    public void tearDown() {
        client.shutdown();
        //System.out.println("shut down.");
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
        assertTrue(metadata.contains("<title>unkown</title>"));
        
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("test-files/input-two-osti-id.xml")) {
            String newMetadata = OSTIElinkServiceTest.toString(is);
            //System.out.println("the new metadata is " + newMetadata);
            //even though this request should fail in the server side, this test
            //still succeed since it is running on another thread.
            client.setMetadata(identifier,newMetadata);
            metadata = client.getMetadata(identifier);
            assertTrue(metadata.contains(identifier));
            assertTrue(metadata.contains("<title>unkown</title>"));
        }
        
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("test-files/input-no-osti-id.xml")) {
            String newMetadata = OSTIElinkServiceTest.toString(is);
            client.setMetadata(identifier, newMetadata);
            metadata = client.getMetadata(identifier);
            assertTrue(metadata.contains(identifier));
            assertTrue(metadata.contains("<title>0 - Data from Raczka et al., Interactions between"));
        }
        
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("test-files/input-one-osti-id.xml")) {
            String newMetadata = OSTIElinkServiceTest.toString(is);
            client.setMetadata(identifier, newMetadata);
            metadata = client.getMetadata(identifier);
            assertTrue(metadata.contains(identifier));
            assertTrue(metadata.contains("<title>1 - Data from Raczka et al., Interactions between"));
        }
    }

}
