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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Test the OSTIElinkService class
 * @author tao
 *
 */
public class OSTIElinkServiceTest {
    private static OSTIElinkService ostiService = null;
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
        ostiService = new OSTIElinkService(username, password, BASEURL);
    }

    /**
     * Test the buildMinimalMetadata method
     * @throws Exception
     */
    @Test
    public void testBuildMinimalMetadata() throws Exception {
        String result = ostiService.buildMinimalMetadata("KNB");
        assertTrue(result.contains("<set_reserved/>"));
        assertTrue(result.contains("<site_input_code>KNB</site_input_code>"));
        
        //The default one will be ess-dive
        result = ostiService.buildMinimalMetadata(null);
        assertTrue(result.contains("<set_reserved/>"));
        assertTrue(result.contains("<site_input_code>ESS-DIVE</site_input_code>"));
        
        result = ostiService.buildMinimalMetadata("ESS-DIVE");
        assertTrue(result.contains("<set_reserved/>"));
        assertTrue(result.contains("<site_input_code>ESS-DIVE</site_input_code>"));
        
        result = ostiService.buildMinimalMetadata("KNB");
        assertTrue(result.contains("<set_reserved/>"));
        assertTrue(result.contains("<site_input_code>KNB</site_input_code>"));
    }
    
    /**
     * Test the mintIdentifier method
     * @throws Exception
     */
    @Test
    public void testMintIdentifier() throws Exception {
        String identifier = ostiService.mintIdentifier(null);
        assertTrue(identifier.startsWith("doi:10."));
        identifier = ostiService.removeDOI(identifier);
        String metadata = ostiService.getMetadata(identifier);
        assertTrue(metadata.contains(identifier));
        try {
            identifier = ostiService.mintIdentifier("KNB");
            fail("The test can't get here");
        } catch (Exception e) {
            assertTrue(e instanceof OSTIElinkException);
        }
        
    }
    
    /**
     * Test the getMetadata method
     * @throws Exception
     */
    @Test
    public void testGetMetadata() throws Exception {
        String doi = "10.15485/1523924";
        String metadata = ostiService.getMetadata(doi);
        assertTrue(metadata.contains(doi));
        
        String doiWithPrefix = "doi:" + doi;
        metadata = ostiService.getMetadata(doiWithPrefix);
        assertTrue(metadata.contains(doi));
        
        try {
            String doi2 = "11.15df485/1523924";
            metadata = ostiService.getMetadata(doi2);
            fail("The test can't get here");
        } catch (Exception e) {
            //e.printStackTrace();
            assertTrue(e instanceof OSTIElinkNotFoundException);
        }
        
        try {
            String doi3 = "doi";
            metadata = ostiService.getMetadata(doi3);
            fail("The test can't get here");
        } catch (Exception e) {
            //e.printStackTrace();
            assertTrue(e instanceof OSTIElinkNotFoundException);
        }
        
        try {
            String doi4 = "doi:";
            metadata = ostiService.getMetadata(doi4);
            fail("The test can't get here");
        } catch (Exception e) {
            //e.printStackTrace();
            assertTrue(e instanceof OSTIElinkNotFoundException);
        }
        
        try {
            String doi5 = "doi:11.15df485/1523924";
            metadata = ostiService.getMetadata(doi5);
            fail("The test can't get here");
        } catch (Exception e) {
            //e.printStackTrace();
            assertTrue(e instanceof OSTIElinkNotFoundException);
        }
        
        try {
            String doi6 = "1523924";
            metadata = ostiService.getMetadata(doi6);
            fail("The test can't get here");
        } catch (Exception e) {
            //e.printStackTrace();
            assertTrue(e instanceof OSTIElinkNotFoundException);
        }
    }
    
    /**
     * Test the getMetadataFromOstiId method
     * @throws Exception
     */
    @Test
    public void testGetMetadataFromOstiId() throws Exception {
        String ostiId = "1523924";
        String metadata = ostiService.getMetadataFromOstiId(ostiId);
        assertTrue(metadata.contains(ostiId));
            
        try {
            String ostiId2 = "15df4851523924";
            metadata = ostiService.getMetadataFromOstiId(ostiId2);
            fail("The test can't get here");
        } catch (Exception e) {
            //e.printStackTrace();
            assertTrue(e instanceof OSTIElinkNotFoundException);
        } 
        try {
            String doi5 = "doi:11.15df485/1523924";
            metadata = ostiService.getMetadataFromOstiId(doi5);
            fail("The test can't get here");
        } catch (Exception e) {
            //e.printStackTrace();
            assertTrue(e instanceof OSTIElinkNotFoundException);
        }
        try {
            String doi5 = "10.15485/1523924";
            metadata = ostiService.getMetadataFromOstiId(doi5);
            fail("The test can't get here");
        } catch (Exception e) {
            //e.printStackTrace();
            assertTrue(e instanceof OSTIElinkNotFoundException);
        }
    }
    
    /**
     * Test the getOstiId method
     * @throws Exception
     */
    @Test
    public void testGetOstiId() throws Exception {
        String prefix = "10.15485";
        String doi = "10.15485/1523924";
        String ostiId = ostiService.getOstiId(doi, prefix);
        assertTrue(ostiId.equals("1523924"));
        
        prefix = "10.15485";
        doi = "doi:10.15485/1523924";
        ostiId = ostiService.getOstiId(doi, prefix);
        assertTrue(ostiId.equals("1523924"));
        
        prefix = "10.15485/";
        doi = "10.15485/1523924";
        ostiId = ostiService.getOstiId(doi, prefix);
        assertTrue(ostiId.equals("1523924"));
        
        prefix = "10.15485/";
        doi = "doi:10.15485/1523924";
        ostiId = ostiService.getOstiId(doi, prefix);
        assertTrue(ostiId.equals("1523924"));
        
        prefix = "doi:10.15485";
        doi = "10.15485/1523924";
        ostiId = ostiService.getOstiId(doi, prefix);
        assertTrue(ostiId.equals("1523924"));
        
        prefix = "doi:10.15485";
        doi = "doi:10.15485/1523924";
        ostiId = ostiService.getOstiId(doi, prefix);
        assertTrue(ostiId.equals("1523924"));
        
        prefix = "doi:10.15485/";
        doi = "10.15485/1523924";
        ostiId = ostiService.getOstiId(doi, prefix);
        assertTrue(ostiId.equals("1523924"));
        
        prefix = "doi:10.15485/";
        doi = "10.15485/1523924";
        ostiId = ostiService.getOstiId(doi, prefix);
        assertTrue(ostiId.equals("1523924"));
        
        prefix = null;
        doi = "10.15485/1523924";
        ostiId = ostiService.getOstiId(doi, prefix);
        assertTrue(ostiId.equals("1523924"));
        
        doi = "doi:10.15485/1523924";
        ostiId = ostiService.getOstiId(doi, prefix);
        assertTrue(ostiId.equals("1523924"));
        
        doi = "11.15df485/1523924";
        try {
            ostiId = ostiService.getOstiId(doi, prefix);
            fail("The test can't get here");
        } catch (Exception e) {
            //e.printStackTrace();
            assertTrue(e instanceof OSTIElinkNotFoundException);
        }
        
        doi = "doi:11.15df485/1523924";
        try {
            ostiId = ostiService.getOstiId(doi, prefix);
            fail("The test can't get here");
        } catch (Exception e) {
            //e.printStackTrace();
            assertTrue(e instanceof OSTIElinkNotFoundException);
        }
        
        prefix = "doi:10.15485/";
        doi = "11.15df485/1523924";
        try {
            ostiId = ostiService.getOstiId(doi, prefix);
            fail("The test can't get here");
        } catch (Exception e) {
            //e.printStackTrace();
            assertTrue(e instanceof OSTIElinkNotFoundException);
        }
        
        doi = "doi:11.15df485/1523924";
        try {
            ostiId = ostiService.getOstiId(doi, prefix);
            fail("The test can't get here");
        } catch (Exception e) {
            //e.printStackTrace();
            assertTrue(e instanceof OSTIElinkNotFoundException);
        }
        
        prefix = "doi:11.15df485/";
        doi = "11.15df485/1523924";
        ostiId = ostiService.getOstiId(doi, prefix);
        assertTrue(ostiId.equals("1523924"));
        
        doi = "doi:11.15df485/1523924";
        ostiId = ostiService.getOstiId(doi, prefix);
        assertTrue(ostiId.equals("1523924"));
        
        doi = "10.15485/1523924";
        ostiId = ostiService.getOstiId(doi, prefix);
        assertTrue(ostiId.equals("1523924"));
        
        doi = "doi:10.15485/1523924";
        ostiId = ostiService.getOstiId(doi, prefix);
        assertTrue(ostiId.equals("1523924"));
    }
    
    /**
     * Test the setMetadata method
     * @throws Exception
     */
    @Test
    public void testSetMetadata() throws Exception {
        String identifier = ostiService.mintIdentifier(null);
        assertTrue(identifier.startsWith("doi:10."));
        identifier = ostiService.removeDOI(identifier);
        //System.out.println("the doi identifier is " + identifier);
        String metadata = ostiService.getMetadata(identifier);
        assertTrue(metadata.contains(identifier));
        assertTrue(metadata.contains("<title>unkown</title>"));
        
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("test-files/input-two-osti-id.xml")) {
            String newMetadata = toString(is);
            //System.out.println("the new metadata is " + newMetadata);
            try {
                ostiService.setMetadata(identifier, null, newMetadata);
                fail("Test can't reach here");
            } catch (Exception e) {
                //e.printStackTrace();
                assertTrue(e instanceof OSTIElinkException);
            }
            metadata = ostiService.getMetadata(identifier);
            assertTrue(metadata.contains(identifier));
            assertTrue(metadata.contains("<title>unkown</title>"));
        }
        
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("test-files/input-no-osti-id.xml")) {
            String newMetadata = toString(is);
            ostiService.setMetadata(identifier, null, newMetadata);
            metadata = ostiService.getMetadata(identifier);
            assertTrue(metadata.contains(identifier));
            assertTrue(metadata.contains("<title>0 - Data from Raczka et al., Interactions between"));
        }
        
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("test-files/input-one-osti-id.xml")) {
            String newMetadata = toString(is);
            ostiService.setMetadata(identifier, null, newMetadata);
            metadata = ostiService.getMetadata(identifier);
            assertTrue(metadata.contains(identifier));
            assertTrue(metadata.contains("<title>1 - Data from Raczka et al., Interactions between"));
        }
       
    }
    
    /**
     * Read a input stream object to a string
     * @param inputStream  the source of input
     * @return the string presentation of the input stream
     * @throws Exception
     */
    private String toString(InputStream inputStream) throws Exception {
        int bufferSize = 1024;
        char[] buffer = new char[bufferSize];
        StringBuilder textBuilder = new StringBuilder();
        try (Reader reader = new InputStreamReader(inputStream, Charset.forName(StandardCharsets.UTF_8.name()))) {
            for (int numRead; (numRead = reader.read(buffer, 0, buffer.length)) > 0; ) {
                textBuilder.append(buffer, 0, numRead);
            }
        }
        return textBuilder.toString();
    }

}
