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

/**
 * An OSTIElinkServiceRequest request represents the data needed for a single request
 * to the OSTI Elink Service as a Callable task that can be executed
 * within a thread pool, typically provided by an Executor service.  The request  
 * is used within a queue to temporarily store requests before they are processed 
 * by the OSTIElink service. EZIDServiceRequests are created only by the OSTIElinkClient,
 * which provides methods for external applications to generate requests.
 * @author tao
 *
 */
public class OSTIElinkServiceRequest implements Runnable {
    public static final int SETMETADATA = 1;
    
    private OSTIElinkService service = null;
    private OSTIElinkErrorAgent errorAgent = null;
    private int method = 0;
    private String identifier = null;
    private String metadata = null;
    
    protected static Log log = LogFactory.getLog(OSTIElinkServiceRequest.class);
    
    /**
     * Constructor
     * @param service  the OSTIElinkService object will run the request
     * @param method  the method which the request will handle
     * @param identifier  the identifier associated with request
     * @param errorAgent  the class used to send error message to administers. It can be null.
     *                    If it is null, the error messages will only be logged in the error level.
     */
    protected OSTIElinkServiceRequest(OSTIElinkService service, int method, String identifier, OSTIElinkErrorAgent errorAgent) {
        if (service == null) {
            throw new IllegalArgumentException("EZIDService argument must not be null.");
        }
        if (method < 1 || method > 4) {
            throw new IllegalArgumentException("Service must be an interger value between 1 and 4.");
        }
        if (identifier == null) {
            throw new IllegalArgumentException("Identifier must not be null.");
        }
        this.service = service;
        this.method = method;
        this.identifier = identifier;
        this.errorAgent = errorAgent;
    }
    
    /**
     * Constructor
     * @param service  the OSTIElinkService object will run the request
     * @param method  the method which the request will handle
     * @param identifier  the identifier associated with the request
     * @param errorAgent  the class used to send error message to administers. It can be null.
     *                    If it is null, the error messages will only be logged in the error level.
     * @param metadata  the metadata associated with the request
     */
    protected OSTIElinkServiceRequest(OSTIElinkService service, int method, String identifier, OSTIElinkErrorAgent errorAgent, String metadata) {
        this(service, method, identifier, errorAgent);
        this.metadata = metadata;
    }
    
    public void run() {
        log.debug("OSTIElinkServiceRequest - Service to execute: " + method + "/" + identifier + "/" + metadata);
        try {
            switch (method) {
                case SETMETADATA:
                    String prefix = null;
                    service.setMetadata(identifier, prefix, metadata);
                    log.debug("Completed CREATE request for: " + identifier);
                    break;
                default:
                    log.warn("OSTIElinkServiceRequest - the request doesn't support this methdo: " + method);
                    break;
            }
        } catch (Exception e) {
            log.error("OSTIElinkServiceRequest - Failed request " + method + " for " + identifier + " since: " + e.getMessage());
            if (errorAgent != null) {
                errorAgent.notify(e);
            }
        }
    }
}
