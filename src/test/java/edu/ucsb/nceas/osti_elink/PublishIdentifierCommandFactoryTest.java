package edu.ucsb.nceas.osti_elink;

import edu.ucsb.nceas.osti_elink.v1.OSTIService;
import edu.ucsb.nceas.osti_elink.v2.xml.OSTIv2XmlService;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test the PublishIdentifierCommandFactory class
 * @author Tao
 */

public class PublishIdentifierCommandFactoryTest {

    /**
     * Test the getInstance method
     * @throws Exception
     */
    @Test
    public void testGetInstance() throws Exception {
        OSTIElinkService service = new OSTIService(null, null, null);
        PublishIdentifierCommand command = PublishIdentifierCommandFactory.getInstance(service);
        assertTrue( command instanceof edu.ucsb.nceas.osti_elink.v1.PublishIdentifierCommand);
        service = new OSTIv2XmlService(null, null, null);
        command = PublishIdentifierCommandFactory.getInstance(service);
        assertTrue( command instanceof edu.ucsb.nceas.osti_elink.v2.xml.PublishIdentifierCommand);
        service = null;
        try {
            command = PublishIdentifierCommandFactory.getInstance(service);
            fail("Test can't get here");
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException);
        }
    }
}
