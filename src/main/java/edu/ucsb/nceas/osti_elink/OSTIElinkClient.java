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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.ucsb.nceas.osti_elink.exception.ClassNotSupported;
import edu.ucsb.nceas.osti_elink.exception.PropertyNotFound;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.parsers.ParserConfigurationException;

/**
 * A simple client application for the OSTI Elink Service that allows calling applications
 * to set up a connection to the service and maintain that connection across a series of
 * service invocations. Service call requests are maintained in a queue and submitted
 * to Elink asynchronously, allowing the rate of requests to the Elink service to be
 * controlled by the calling application.
 * @author tao
 */
public class OSTIElinkClient {
    public static final String USER_NAME_PROPERTY = "guid.doi.username";
    public static final String PASSWORD_PROPERTY = "guid.doi.password";
    public static final String BASE_URL_PROPERTY = "guid.doi.baseurl";
    private OSTIElinkErrorAgent errorAgent = null;
    private OSTIElinkService service = null;
    private ExecutorService executor = null;
    private static Properties properties = null;

    protected static Log log = LogFactory.getLog(OSTIElinkClient.class);

    /**
     * Constructor
     * @param username  the username of an OSTIElink account
     * @param password  the password of the OSTIElink account
     * @param baseURL  the base url of the OSTIElink service
     * @param errorAgent  the class used to send error message to administers. It can be null.
     *                    If it is null, the error messages will only be logged in the error level.
     * 
     */
    public OSTIElinkClient(
        String username, String password, String baseURL, OSTIElinkErrorAgent errorAgent) {
        if (properties == null)  {
            loadDefaultPropertyFile();
        }
//        if (username != null) {
//            properties.setProperty(USER_NAME_PROPERTY, username);
//        }
//        if (password != null) {
//            properties.setProperty(PASSWORD_PROPERTY, password);
//        }
//        properties.setProperty(BASE_URL_PROPERTY, baseURL);
//        try {
//            service = OSTIServiceFactory.getOSTIElinkService(properties);
//        } catch (PropertyNotFound | ClassNotFoundException | ClassNotSupported | IOException |
//                 ParserConfigurationException | OSTIElinkException e) {
//            log.error("Can't generate the OSTIElinkService instance since " + e.getMessage(), e);
//            throw new RuntimeException(e);
//        }
        this.errorAgent = errorAgent;
        startExecutorLoop();
    }

    private void loadDefaultPropertyFile() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("osti.properties")) {
            properties = new Properties();
            properties.load(is);
        } catch (IOException e) {
            log.error("Can't load the default property file into properties " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Set the given properties to the class. This method is for testing only
     * @param properties1  the properties will be used to create the client.
     */
//    public static void setProperties(Properties properties1) {
//        properties = properties1;
//    }

    /**
     * Set the meta data for a given identifier. The identifier should already exist in the elink service.
     * The method will run the commands in another thread.
     * We always use the query method to figure out internal OSTI id (not the prefix comparison).
     * @param identifier  the identifier of object which will be set a new metadata
     * @param metadata  the new metadata which will be used
     */
    public void setMetadata(String identifier, String metadata) throws InterruptedException {
        OSTIElinkServiceRequest request =
                new OSTIElinkServiceRequest(service, OSTIElinkServiceRequest.SETMETADATA, identifier, errorAgent, metadata);
        executor.execute(request);
    }
    
    /**
     * Ask the elink service to generate a doi for the given siteCode.
     * The thread blocks until the identifier is returned
     * @param siteCode  the siteCode will be used. If it is null, the default one, ess-dive, will be used.
     * @return  the newly generated doi
     * @throws OSTIElinkException
     */
    public String mintIdentifier(String siteCode) throws OSTIElinkException {
        String identifier = null;
        try {
            identifier = service.mintIdentifier(siteCode);
        } catch (OSTIElinkException e) {
            if (errorAgent != null) {
                errorAgent.notify(e.getMessage());
            }
            throw e;
        }
        return identifier;
    }
    
    /**
     * Get the associated metadata for the given identifier.
     * The thread blocks until the identifier is returned
     * @param identifier  for which metadata should be returned
     * @return  the metadata associated with the identifier
     * @throws OSTIElinkException
     */
    public String getMetadata(String identifier) throws OSTIElinkException {
        return service.getMetadata(identifier);
    }
    
    
    /**
     * Get the status for the given identifier. If there are multiple records 
     * associate with the identifier, the status of the first one will be returned.
     * The thread blocks until the status is returned
     * @param identifier  id to identify whose status should be returned
     * @return  the metadata associated with the identifier
     * @throws OSTIElinkException
     */
    public String getStatus(String identifier) throws OSTIElinkException {
        return service.getStatus(identifier);
    }
    
    private void startExecutorLoop() {
        // Query the runtime to see how many CPUs are available, and configure that many threads
        Runtime runtime = Runtime.getRuntime();        
        int numCores = runtime.availableProcessors();
        log.debug("OSTIElinkClient.startExecutorLoop - Number of cores available: " + numCores);
        executor = Executors.newFixedThreadPool(numCores);
    }
    
    /**
     * Shut down the excutor loop until all submitted tasks are completed.
     */
    public void shutdown() {
        log.debug("Shutting down executor...");
        // Stop the executor from accepting new requests and finishing existing Runnables
        executor.shutdown();
        // Wait until all Runnables are finished
        while (!executor.isTerminated()) {
            //log.debug("OSTIElinkClient.shutdown....");
        }
    }

    /**
     * Get the OSTIElinkService object associated with the client
     * @return the OSTIElinkService object
     */
    public OSTIElinkService getService() {
        return this.service;
    }

}
