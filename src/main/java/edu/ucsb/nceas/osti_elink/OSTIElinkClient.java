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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A simple client application for the OSTI Elink Service that allows calling applications
 * to set up a connection to the service and maintain that connection across a series of
 * service invocations. Service call requests are maintained in a queue and submitted
 * to Elink asynchronously, allowing the rate of requests to the Elink service to be
 * controlled by the calling application.
 * @author tao
 */
public class OSTIElinkClient {
    private String USERNAME = "";
    private String PASSWORD = "";
    private OSTIElinkService service = null;
    private ExecutorService executor = null;

    protected static Log log = LogFactory.getLog(OSTIElinkClient.class);

    /**
     * Constructor
     * @param username  the user name of an OSTIElink account
     * @param password  the password of the OSTIElink account
     * @param baseURL  the base url of the OSTIElink service
     */
    public OSTIElinkClient(String username, String password, String baseURL) {
        service = new OSTIElinkService(username, password, baseURL);
        startExecutorLoop();
    }
    
    /**
     * Set the meta data for a given identifier. The identifier should alreay exist in the elink service.
     * We always use the query method to figure out internal OSTI id (not the prefix comparison).
     * @param identifier  the identifier of object which will be set a new metadata
     * @param metadata  the new metadata which will be used
     */
    public void setMetadata(String identifier, String metadata) throws InterruptedException {
        OSTIElinkServiceRequest request = new OSTIElinkServiceRequest(service, OSTIElinkServiceRequest.SETMETADATA, identifier, metadata);
        executor.execute(request);
    }
    
    /**
     * Ask the elink service to generate a doi for the given siteCode.
     * @param siteCode  the siteCode will be used. If it is null, the default one, ess-dive, will be used.
     * @return  the newly generated doi
     * @throws OSTIElinkException
     */
    public String mintIdentifier(String siteCode) throws OSTIElinkException {
        String identifier = service.mintIdentifier(siteCode);
        return identifier;
    }
    
    /**
     * Get the associated metadata for the given identifier
     * @param identifier  for which metadata should be returned
     * @return  the metadata associated with the identifier
     * @throws OSTIElinkException
     */
    public String getMetadata(String identifier) throws OSTIElinkException {
        String metadata = service.getMetadata(identifier);
        return metadata;
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

}
