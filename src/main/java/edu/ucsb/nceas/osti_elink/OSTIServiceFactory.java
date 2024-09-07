package edu.ucsb.nceas.osti_elink;

import edu.ucsb.nceas.osti_elink.exception.PropertyNotFound;
import edu.ucsb.nceas.osti_elink.v1.OSTIService;

import java.util.Properties;

/**
 * This is the factory to generate the OSTIElinkService instances.
 * @author Tao
 */
public class OSTIServiceFactory {

    /**
     * Get the OSTIElinService instance based on the given properties
     * @param properties  the configuration determining the class instances
     * @return an OSTIElinkService instance
     * @throws IllegalArgumentException
     * @throws PropertyNotFound
     */
    public static OSTIElinkService getOSTIElinkService(Properties properties)
        throws IllegalArgumentException, PropertyNotFound {
        OSTIElinkService service;
        String className = getProperty("ostiService.className", properties);
        if (className.equals("edu.ucsb.nceas.osti_elink.v1.OSTIService")) {
            // v1 service
            String userName = getProperty(OSTIElinkClient.USER_NAME_PROPERTY, properties);
            String password = getProperty(OSTIElinkClient.PASSWORD_PROPERTY, properties);
            String baseURL = getProperty(OSTIElinkClient.BASE_URL_PROPERTY, properties);
            service = new OSTIService(userName, password, baseURL);

        } else {
            throw new ClassNotFoundException("OSTIService does not find the class " + className);
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
