package edu.ucsb.nceas.osti_elink;

import edu.ucsb.nceas.osti_elink.exception.ClassNotSupported;
import edu.ucsb.nceas.osti_elink.exception.PropertyNotFound;
import edu.ucsb.nceas.osti_elink.v1.OSTIService;
import edu.ucsb.nceas.osti_elink.v2.json.OSTIv2JsonService;
import edu.ucsb.nceas.osti_elink.v2.xml.OSTIv2XmlService;
import org.junit.Rule;
import org.junit.Test;
import uk.org.webcompere.systemstubs.rules.EnvironmentVariablesRule;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The test class for OSTIServiceFactory
 * @author Tao
 */
public class OSTIServiceFactoryTest {
    private static final String v2JsonClassName = "edu.ucsb.nceas.osti_elink.v2.json.OSTIv2JsonService";
    private static final String v2XmlClassName = "edu.ucsb.nceas.osti_elink.v2.xml.OSTIv2XmlService";
    @Rule
    public EnvironmentVariablesRule environmentVariablesRule =
        new EnvironmentVariablesRule("METACAT_OSTI_SERVICE_CLASS_NAME", null);

    /**
     * Test the getProperty method
     * @throws Exception
     */
    @Test
    public void testGetProperty() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("password", "value");
        String propValue = OSTIServiceFactory.getProperty("password", properties);
        assertEquals("value", propValue);
        try {
            propValue = OSTIServiceFactory.getProperty("newPassword", properties);
            fail("The test shouldn't be here since the property doesn't exist");
        } catch (Exception e) {
            assertTrue(e instanceof PropertyNotFound);
        }
        properties = new Properties();
        try {
            propValue = OSTIServiceFactory.getProperty("newPassword", properties);
            fail("The test shouldn't be here since the properties is empty");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
        properties = null;
        try {
            propValue = OSTIServiceFactory.getProperty("newPassword", properties);
            fail("The test shouldn't be here since the properties is empty");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    /**
     * Test to get a V2Json OSTIService
     * @throws Exception
     */
    @Test
    public void testGetOSTIV2JsonService() throws Exception {
        Properties properties = new Properties();
        properties.setProperty(OSTIElinkClient.USER_NAME_PROPERTY, "name");
        properties.setProperty(OSTIElinkClient.PASSWORD_PROPERTY, "password");
        properties.setProperty(OSTIElinkClient.BASE_URL_PROPERTY, "https://foo.com");
        properties.setProperty(OSTIServiceFactory.OSTISERVICE_CLASSNAME_PROPERTY, v2JsonClassName);
        properties.setProperty(OSTIv2JsonService.TOKEN_PATH_PROP_NAME, "./token");
        OSTIElinkService service = OSTIServiceFactory.getOSTIElinkService(properties);
        assertTrue(service instanceof OSTIv2JsonService);
        properties.remove(OSTIv2JsonService.TOKEN_PATH_PROP_NAME);
        try {
            service = OSTIServiceFactory.getOSTIElinkService(properties);
            fail("Test can't get there since the TOKEN_PATH_PROP_NAME property is not set.");
        } catch (Exception e) {
            assertTrue(e instanceof PropertyNotFound);
        }
    }

    /**
     * Test to get a V2Json OSTIService when the env is set to be v2json while the property is set to v2xml
     * @throws Exception
     */
    @Test
    public void testGetOSTIV2JsonServiceFromEnv() throws Exception {
        // env set the variable v1
        environmentVariablesRule.set("METACAT_OSTI_SERVICE_CLASS_NAME", v2JsonClassName);
        Properties properties = new Properties();
        properties.setProperty(OSTIElinkClient.USER_NAME_PROPERTY, "name");
        properties.setProperty(OSTIElinkClient.PASSWORD_PROPERTY, "password");
        properties.setProperty(OSTIElinkClient.BASE_URL_PROPERTY, "https://foo.com");
        properties.setProperty(OSTIv2JsonService.TOKEN_PATH_PROP_NAME, "./token");
        // Properties set it to v2xml
        properties.setProperty(OSTIServiceFactory.OSTISERVICE_CLASSNAME_PROPERTY, v2XmlClassName);
        OSTIElinkService service = OSTIServiceFactory.getOSTIElinkService(properties);
        assertTrue(service instanceof OSTIv2JsonService);
        properties.remove(OSTIv2JsonService.TOKEN_PATH_PROP_NAME);
        try {
            service = OSTIServiceFactory.getOSTIElinkService(properties);
            fail("Test can't get there since TOKEN_PATH_PROP_NAME property is not set.");
        } catch (Exception e) {
            assertTrue(e instanceof PropertyNotFound);
        }
    }

    /**
     * Test to get a V2Xml OSTIService
     * @throws Exception
     */
    @Test
    public void testGetOSTIV2XmlService() throws Exception {
        Properties properties = new Properties();
        properties.setProperty(OSTIElinkClient.BASE_URL_PROPERTY, "https://foo.com");
        properties.setProperty(OSTIServiceFactory.OSTISERVICE_CLASSNAME_PROPERTY, v2XmlClassName);
        properties.setProperty(OSTIv2XmlService.TOKEN_PATH_PROP_NAME, "./token");
        OSTIElinkService service = OSTIServiceFactory.getOSTIElinkService(properties);
        assertTrue(service instanceof OSTIv2XmlService);
        properties.remove(OSTIv2XmlService.TOKEN_PATH_PROP_NAME);
        try {
            service = OSTIServiceFactory.getOSTIElinkService(properties);
            fail("Test can't get there since the username property is not set.");
        } catch (Exception e) {
            assertTrue(e instanceof PropertyNotFound);
        }
    }

    /**
     * Test to get a V2Xml OSTIService when the env variable set it v2xml while properties set it v1
     * @throws Exception
     */
    @Test
    public void testGetOSTIV2XmlServiceFromEnv() throws Exception {
        // env set the variable v2xml
        environmentVariablesRule.set(OSTIServiceFactory.OSTISERVICE_CLASSNAME_ENV_NAME, v2XmlClassName);
        Properties properties = new Properties();
        properties.setProperty(OSTIElinkClient.BASE_URL_PROPERTY, "https://foo.com");
        // properties set the variable v1
        properties.setProperty(OSTIServiceFactory.OSTISERVICE_CLASSNAME_PROPERTY, v2JsonClassName);
        properties.setProperty(OSTIv2XmlService.TOKEN_PATH_PROP_NAME, "./token");
        OSTIElinkService service = OSTIServiceFactory.getOSTIElinkService(properties);
        assertTrue(service instanceof OSTIv2XmlService);
        properties.remove(OSTIv2XmlService.TOKEN_PATH_PROP_NAME);
        try {
            service = OSTIServiceFactory.getOSTIElinkService(properties);
            fail("Test can't get there since the username property is not set.");
        } catch (Exception e) {
            assertTrue(e instanceof PropertyNotFound);
        }
    }

    /**
     * Test the method of getOSTIService with the unsupported class
     * @throws Exception
     */
    @Test
    public void testNotSupportedOSTIService() throws Exception {
        Properties properties = new Properties();
        properties.setProperty(OSTIElinkClient.USER_NAME_PROPERTY, "name");
        properties.setProperty(OSTIElinkClient.PASSWORD_PROPERTY, "password");
        properties.setProperty(OSTIElinkClient.BASE_URL_PROPERTY, "https://foo.com");
        properties.setProperty(OSTIServiceFactory.OSTISERVICE_CLASSNAME_PROPERTY,
                               "edu.ucsb.nceas.osti_elink.foo");
        try {
            OSTIElinkService service = OSTIServiceFactory.getOSTIElinkService(properties);
            fail("Test can't get there since the class is not supported.");
        } catch (Exception e) {
            assertTrue(e instanceof ClassNotSupported);
        }
    }

    /**
     * If we can't find the class name in neither the environmental variable nor the properties
     * file, the default class - v2json will be used
     * @throws Exception
     */
    @Test
    public void testCreateDefaultClass() throws Exception {
        environmentVariablesRule.set("METACAT_OSTI_TOKEN", "token");
        Properties properties = new Properties();
        properties.setProperty(OSTIElinkClient.USER_NAME_PROPERTY, "name");
        properties.setProperty(OSTIElinkClient.PASSWORD_PROPERTY, "password");
        properties.setProperty(OSTIElinkClient.BASE_URL_PROPERTY, "https://foo.com");
        OSTIElinkService service = OSTIServiceFactory.getOSTIElinkService(properties);
        assertTrue(service instanceof OSTIv2JsonService);
    }
}
