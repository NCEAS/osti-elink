package edu.ucsb.nceas.osti_elink;

import edu.ucsb.nceas.osti_elink.exception.ClassNotSupported;
import edu.ucsb.nceas.osti_elink.exception.PropertyNotFound;
import edu.ucsb.nceas.osti_elink.v1.OSTIService;
import edu.ucsb.nceas.osti_elink.v2.xml.OSTIv2XmlService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Properties;

/**
 * This is the factory to generate the OSTIElinkService instances.
 * @author Tao
 */
public class OSTIServiceFactory {
    public static final String OSTISERVICE_CLASSNAME_PROPERTY = "ostiService.className";
    public static final String OSTISERVICE_CLASSNAME_ENV_NAME = "METACAT_OSTI_SERVICE_CLASS_NAME";
    private static final Log log = LogFactory.getLog(OSTIServiceFactory.class);

    /**
     * Get the OSTIElinService instance based on the environment variables and given properties.
     * The environmental variable of METACAT_OSTI_SERVICE_CLASS_NAME overwrites the property in the file.
     * @param properties  the configuration determining the class instances
     * @return an OSTIElinkService instance
     * @throws IllegalArgumentException
     * @throws PropertyNotFound
     * @throws ClassNotFoundException
     * @throws OSTIElinkException
     */
    public static OSTIElinkService getOSTIElinkService(Properties properties)
        throws IllegalArgumentException, PropertyNotFound, ClassNotFoundException,
        ClassNotSupported, IOException, OSTIElinkException, ParserConfigurationException {
        OSTIElinkService service;
        // Get className from the environment variable first. If we can't, read it from the
        // property file.
//        String className = System.getenv(OSTISERVICE_CLASSNAME_ENV_NAME);
//        if (className == null) {
//            try {
//                className = getProperty(OSTISERVICE_CLASSNAME_PROPERTY, properties);
//                log.info("The class name " + className + " is set by the property file ");
//            } catch (PropertyNotFound e) {
//                log.info("We cannot find the OSTI service name from neither the evn variable "
//                             + OSTISERVICE_CLASSNAME_ENV_NAME
//                             + "nor the properties file. So we will use the default one: "
//                             + "edu.ucsb.nceas.osti_elink.v1.OSTIService");
//                className = "edu.ucsb.nceas.osti_elink.v1.OSTIService";
//            }
//        } else {
//            log.info("The class name " + className + " is set by the environmental variable "
//                         + OSTISERVICE_CLASSNAME_ENV_NAME);
//        }
        String baseURL = getProperty(OSTIElinkClient.BASE_URL_PROPERTY, properties);
        if (className.equals("edu.ucsb.nceas.osti_elink.v1.OSTIService")) {
            // v1 service
            String userName = getProperty(OSTIElinkClient.USER_NAME_PROPERTY, properties);
            String password = getProperty(OSTIElinkClient.PASSWORD_PROPERTY, properties);

            service = new OSTIService(userName, password, baseURL);
            log.info("The OSTI service class is v1: " + service.getClass().getName());
        } else if (className.equals("edu.ucsb.nceas.osti_elink.v2.xml.OSTIv2XmlService")) {
            service = new OSTIv2XmlService(null, null, baseURL, properties);
            log.info("The OSTI service class is v2xml: " + service.getClass().getName());
        } else {
            throw new ClassNotSupported("OSTIService does not support the class " + className);
        }
        return service;
    }

    /**
     * Get the value from the given properties with the given property name
     * @param propertyName  the name of property
     * @param properties  the property will be looked at
     * @return the value of the property
     * @throws IllegalArgumentException
     * @throws PropertyNotFound
     */
    public static String getProperty(String propertyName, Properties properties)
        throws IllegalArgumentException, PropertyNotFound {
        if (propertyName == null || propertyName.trim().equals("")) {
            throw new IllegalArgumentException("The propertyName should not be null or blank in "
                                                   + "the getProperty method.");
        }
        if (properties == null || properties.isEmpty()) {
            throw new IllegalArgumentException("The parameter properties should not be null or "
                                                   + "blank in the getProperty method.");
        }
        String value = properties.getProperty(propertyName);
        if (value == null || value.trim().equals("")) {
            throw new PropertyNotFound("The poperty of " + propertyName + " can't found");
        }
        return value;
    }
}
