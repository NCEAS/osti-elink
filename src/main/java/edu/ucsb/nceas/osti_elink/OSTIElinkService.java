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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

/**
 * OSTIElinkService provides access to the identifier service maintained by OSTI.
 * Please see the documentation at https://www.osti.gov/elink/241-6api.jsp 
 * @author tao
 *
 */
public class OSTIElinkService {
    public static final String DOI = "doi";
    
    private static final int GET = 1;
    private static final int PUT = 2;
    private static final int POST = 3;
    private static final int DELETE = 4;
    private static final int CONNECTIONS_PER_ROUTE = 8;
    private static final String minimalMetadataFile = "minimal-osti.xml";
    private static final String STATUS = "status";
    private static final String SUCCESS = "SUCCESS";
    
    private String username = null;
    private String password = null;
    private String baseURL = "https://www.osti.gov/elink/2416api";
    private CloseableHttpClient httpClient = null;
    private  byte[] encodedAuthStr = null;
    private Document minimalMetadataDoc = null;
    protected static Log log = LogFactory.getLog(OSTIElinkService.class);
    
    /**
     * Constructor
     * @param username  the username of the account which can access the OSTI service
     * @param password  the password of the account which can access the OSTI service
     * @param baseURL  the url which specifies the location of the OSTI service
     */
    public OSTIElinkService(String username, String password, String baseURL) {
        this.username = username;
        this.password = password;
        if (baseURL != null && !baseURL.trim().equals("")) {
            this.baseURL = baseURL;
        }
        httpClient = createThreadSafeClient();
        String authentication = username + ":" + password;
        encodedAuthStr = Base64.encodeBase64(authentication.getBytes(Charset.forName("ISO-8859-1")));
    }
    
    /**
     * Create a new, unique, opaque identifier by requesting the OSTI elink service
     * @param siteCode  a pre-dinfined site code which associates doi prefixes.If it is null, the default
     * ess-dive value will be used.
     * @return  the identifier generated by OSTI for this site code
     * @throws OSTIElinkException 
     */
    public String mintIdentifier(String siteCode) throws OSTIElinkException {
        String identifier = null;
        String minimalMetadata = buildMinimalMetadata(siteCode);
        byte[] reponse = sendRequest(POST, baseURL, minimalMetadata);
        log.debug("OSTIElinkService.mintIdentifier - the response from the OSTI service is:\n " + new String(reponse));
        Document doc = generateDOM(reponse);
        String status = getElementValue(doc, STATUS);
        String id = getElementValue(doc, DOI);
        if (status != null && status.equalsIgnoreCase(SUCCESS) && id != null && !id.trim().equals("")) {
            identifier = DOI + ":" + id;
        } else {
            throw new OSTIElinkException("OSTIElinkService.mintIdentifier - Error:  " + new String(reponse));
        }
        log.debug("OSTIElinkService.mintIdentifier - the generated identifier is " + identifier);
        return identifier;
    }
    
    /**
     * Get the metadata associated with the given identifier, which should be a doi.
     * @param doi  the identifier for which the metadata should be returned
     * @return  the metadata in the xml format
     * @throws OSTIElinkException 
     */
    public String getMetadata(String doi) throws OSTIElinkException {
        return getMetadata(doi, DOI);
    }
    
    /**
     * Get the metadata associated with the osti id
     * @param ostiId  the osti id for which the metadata should be returned
     * @return  the metadata in the xml format
     * @throws OSTIElinkException
     */
    String getMetadataFromOstiId(String ostiId) throws OSTIElinkException {
        return getMetadata(ostiId, "osti_id");
    }
    
    /**
     * Get the metadata associated with the given identifier.
     * @param identifier  the identifier for which the metadata should be returned
     * @param  type  the type of the identifier, which can be doi or OSTIId
     * @return  the metadata in the xml format
     * @throws OSTIElinkException 
     */
    private String getMetadata(String identifier, String type) throws OSTIElinkException {
        String metadata = null;
        if (identifier != null && !identifier.trim().equals("")) {
            //we need to remove the doi prefix
            identifier = removeDOI(identifier);
            String url = null;
            try {
                url = baseURL + "?" + type + "=" + URLEncoder.encode(identifier, StandardCharsets.UTF_8.toString());
            } catch (UnsupportedEncodingException e) {
                throw new OSTIElinkException("OSTIElinkService.getMetadata - couldn't encode the query url: " + e.getMessage());
            }
            log.debug("OSTIElinkService.getMetadata - the url sending the service is " + url);
            byte[] response = sendRequest(GET, url);
            metadata = new String(response);
            log.debug("OSTIElinkService.getMetadata - the reponse is " + metadata);
            if (metadata == null || metadata.trim().equals("")) {
                throw new OSTIElinkNotFoundException("OSTIElinkService.getMetadata - the reponse is blank. So we can't find the identifier " + 
                                                      identifier + ", which type is " + type);
            } else if (!metadata.contains(identifier)) {
                Document doc = generateDOM(response);
                String numFound = getAttributeValue(doc, "records", "numfound");
                if (numFound.equals("0")) {
                    throw new OSTIElinkNotFoundException("OSTIElinkService.getMetadata - OSTI can't find the identifier " + 
                                                            identifier + ", which type is " + type + " since " + metadata);
                } else {
                    throw new OSTIElinkException(metadata);
                }
            }
        }
        return metadata;
    }
    
    /**
     * Set new metadata to the given doi. 
     * The OSTI Elink service uses the OSTI id, rather than DOI, to update the metadata of a record.
     * So first we need to query OSTI service with the doi to figure out the OSTI id associated with the doi.
     * Then, we can update the metadata by specifying the OSTI id.
     * Sometimes the OSTI id is the part of DOI by removing the prefix. So we can use the prefix to skip
     * the service query if the prefix is not null and the given DOI starts with the prefix.
     * @param doi  the identifier of the object which will be set the new metadata
     * @param doiPrefix  a shortcut to determine if we can get OSTI_id (replace the query) by string comparing. The
     * safest way is pass null there (but it costs a query to the service).
     * @param metadataXML  the new metadata in xml format
     */
    public void setMetadata(String doi, String doiPrefix, String metadataXML) {
        
    }
    
    /**
     * Generate an HTTP Client for communicating with web services that is
     * thread safe and can be used in the context of a multi-threaded application.
     * @return DefaultHttpClient
     */
    private static CloseableHttpClient createThreadSafeClient()  {
        PoolingHttpClientConnectionManager poolingConnManager = new PoolingHttpClientConnectionManager();
        CloseableHttpClient client = HttpClients.custom().setConnectionManager(poolingConnManager).build();
        poolingConnManager.setMaxTotal(5);
        poolingConnManager.setDefaultMaxPerRoute(CONNECTIONS_PER_ROUTE);
        return client;
    }
    
    /**
     * Send an HTTP request to the OSTI Elink service without a request body.
     * @param requestType the type of the service as an integer
     * @param uri endpoint to be accessed in the request
     * @return byte[] containing the response body
     */
    private byte[] sendRequest(int requestType, String uri) throws OSTIElinkException {
        return sendRequest(requestType, uri, null);
    }
    
    /**
     * Send an HTTP request to the OSTI Elink service with a request body (for POST and PUT requests).
     * @param requestType the type of the service as an integer
     * @param uri endpoint to be accessed in the request
     * @param requestBody the String body to be encoded into the body of the request
     * @return byte[] containing the response body
     */
    private byte[] sendRequest(int requestType, String uri, String requestBody) throws OSTIElinkException {
        HttpUriRequest request = null;
        log.debug("OSTIElinkService.sendRequest - Trying uri: " + uri);
        switch (requestType) {
        case GET:
            request = new HttpGet(uri);
            break;
        case PUT:
            request = new HttpPut(uri);
            if (requestBody != null && requestBody.length() > 0) {
                StringEntity myEntity = new StringEntity(requestBody, "UTF-8");
                ((HttpPut) request).setEntity(myEntity);
            }
            break;
        case POST:
            request = new HttpPost(uri);
            if (requestBody != null && requestBody.length() > 0) {
                StringEntity myEntity = new StringEntity(requestBody, "UTF-8");
                ((HttpPost) request).setEntity(myEntity);
            }
            break;
        case DELETE:
            request = new HttpDelete(uri);
            break;
        default:
            throw new OSTIElinkException("Unrecognized HTTP method requested.");
        }
        request.addHeader("Accept", "application/xml");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + new String(encodedAuthStr));
        byte[] body = null;
        try {
            HttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                body = EntityUtils.toByteArray(entity);
            }
        } catch (ClientProtocolException e) {
            throw new OSTIElinkException(e.getMessage());
        } catch (IOException e) {
            throw new OSTIElinkException(e.getMessage());
        }
        return body;
    }
    
    /**
     * Build a minimal metadata for the given siteCode in order to
     * mint a DOI from the OSTI Elink service. If the siteCode is null or blank, the default ESS-DIVE code will be used.
     * @param siteCode  the site code (determining the prefix of the DOI) will be used in the metadata
     * @return  the minimal metadata will be used to mint a DOI
     * @throws ParserConfigurationException 
     * @throws IOException 
     * @throws SAXException 
     * @throws OSTIElinkException 
     */
    String buildMinimalMetadata(String siteCode) throws OSTIElinkException {
        String metadataStr = null;
        if (minimalMetadataDoc == null) {
            InputStream is = getClass().getClassLoader().getResourceAsStream(minimalMetadataFile);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;
            try {
                dBuilder = dbFactory.newDocumentBuilder();
                minimalMetadataDoc = dBuilder.parse(is);
            } catch (ParserConfigurationException e) {
                throw new OSTIElinkException("OSTIElink.buildMinimalMetadata - Error: " + e.getMessage());
            } catch (SAXException e) {
                throw new OSTIElinkException("OSTIElink.buildMinimalMetadata - Error: " + e.getMessage());
            } catch (IOException e) {
                throw new OSTIElinkException("OSTIElink.buildMinimalMetadata - Error: " + e.getMessage());
            }
            
        }
        if (siteCode != null && !siteCode.trim().equals("")) {
            NodeList nodes = minimalMetadataDoc.getElementsByTagName("site_input_code");
            if (nodes.getLength() > 0) {
                //Only change the first one
                Node node = nodes.item(0);
                NodeList children = node.getChildNodes();
                for (int i=0; i<children.getLength(); i++) {
                    Node child = children.item(i);
                    if (child.getNodeType() == Node.TEXT_NODE) {
                        Text newText = minimalMetadataDoc.createTextNode(siteCode);
                        child.getParentNode().replaceChild(newText, child);
                        break;
                    }
                }
            } else {
                throw new OSTIElinkException("DOIService.buildMinimalMetadata - the minimal metadata should have the site_input_code element.");
            }
        }
        DOMImplementationLS domImplementation = (DOMImplementationLS) minimalMetadataDoc.getImplementation();
        LSSerializer lsSerializer = domImplementation.createLSSerializer();
        metadataStr = lsSerializer.writeToString(minimalMetadataDoc);
        return metadataStr;
    }
    
    /**
     * If a identifier starts with "doi:", this method will remove "doi:".
     * Otherwise it will return the original one back
     * @param identifier  the identifier which will be removed "doi:" if it has one
     * @return  the string with the "doi:" part if it has; otherwise return the identifier itself
     */
    static String removeDOI(String identifier) {
        log.debug("OSTIElinkService.removeDOI - the origial identifier is " + identifier);
        String doiPrefix = DOI + ":";
        if (identifier!= null) {
            //we need to remove the doi prefix
            if (identifier.startsWith(doiPrefix)) {
                identifier = identifier.substring(identifier.indexOf(doiPrefix) + doiPrefix.length());
            }
        }
        log.debug("OSTIElinkService.removeDOI - the identifier after removing doi is " + identifier);
        return identifier;
    }
    
    /**
     * Figure out the osti id for the given doi. If the doi prefix is null, we will figure it out
     * by querying the service; otherwise, we will use string comparing to get the last part of doi 
     * as the osti id. For example, if the doi is 10.15485/1523924 and prefix is doi:10.15485, we will
     * think 1523924 is the osti id. If you are not sure the pattern is correct, just pass null as the 
     * prefix, which is safer.
     * @param doi  the doi associates with the osti id. It can contain "doi:" or not.
     * @param prefix  the prefix will used to figure out osti id by string comparing. It can contain "doi:" or not. It is safer to pass it as null.
     * @return the osti id associates with the doi
     * @throws OSTIElinkException
     */
    String getOstiId(String doi, String prefix) throws OSTIElinkException {
        String ostiId = null;
        if (doi == null || doi.trim().equals("")) {
            throw new OSTIElinkException("DOIService.getOstiId - the given doi shouldn't be null or blank when it figures out the OSTI id for a DOI.");
        }
        if (prefix != null && !prefix.trim().equals("")) {
            //This is a short cut. We will figure out the osti id from the doi itself.
            //the last part (1523924) is the OSTI id for 10.15485/1523924
            prefix = removeDOI(prefix);
            doi = removeDOI(doi);
            if (doi.contains(prefix)) {
                if (!prefix.endsWith("/")) {
                    prefix = prefix + "/";
                }
                ostiId = doi.substring(doi.indexOf(prefix) + prefix.length());
                log.debug("OSTIElinkService.getOstiId - tried to use prefix " + prefix + " to get the osti id " + ostiId +
                        " from the doi identifier " + doi + " without querying the services");
            }
        }
        if ( ostiId == null || ostiId.trim().equals("")) {
            //we can't get the osti id from doi itself. We have to query the service.
           String metadata = getMetadata(doi);
           Document doc = generateDOM(metadata.getBytes());
           ostiId = getElementValue(doc, "osti_id");
           log.debug("OSTIElinkService.getOstiId - tried to query the service to get the osti id " + ostiId +
                   " from the doi idetnifier " + doi);
        }
        log.debug("OSTIElinkService.getOstiId - the osti id of the doi identifier " + doi + " is " + ostiId);
        return ostiId;
    }
    
    /**
     * Generate a dom document for the given byte array
     * @param bytes  the content will be used for the dom document
     * @return  the generated dom document
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    private Document generateDOM(byte[] bytes) throws OSTIElinkException{
        Document doc = null;
        try {
            ByteArrayInputStream is = new ByteArrayInputStream(bytes);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(is);
        } catch (ParserConfigurationException e) {
            throw new OSTIElinkException("OSTIElink.generateDOM - Error: " + e.getMessage());
        } catch (SAXException e) {
            throw new OSTIElinkException("OSTIElink.generateDOM - Error: " + e.getMessage());
        } catch (IOException e) {
            throw new OSTIElinkException("OSTIElink.generateDOM - Error: " + e.getMessage());
        }
        return doc;
    }
    
    /**
     * Get the value of the first element of the given element.
     * If we can't find it, null will be returned.
     * @param doc  the DOM tree model will be looked
     * @param elementName  the name of the element
     * @return  the value of the element
     */
    private String getElementValue(Document doc, String elementName) {
        String value = null;
        if (doc != null && elementName != null && !elementName.trim().equals("")) {
            NodeList nodes = doc.getElementsByTagName(elementName);
            if (nodes.getLength() > 0) {
                Node node = nodes.item(0);
                NodeList children = node.getChildNodes();
                for (int i=0; i<children.getLength(); i++) {
                    Node child = children.item(i);
                    if (child.getNodeType() == Node.TEXT_NODE) {
                        value = child.getNodeValue().trim();
                        log.debug("OSTIElinkService.getElementValue - the value of the element " + elementName + " is " + value);
                        break;
                    }
                }
            }
        }
        return value;
    }
    
    /**
     * Get the value of the given attribute on first element of the given element.
     * If we can't find it, null will be returned.
     * @param doc  the DOM tree model will be looked
     * @param elementName  the name of the element
     * @param attributeName  the name of the attribute
     * @return the value of the attribute
     */
    private String getAttributeValue(Document doc, String elementName, String attributeName) {
        String value = null;
        if (doc != null && elementName != null && !elementName.trim().equals("") && 
                            attributeName != null && !attributeName.trim().equals("")) {
            NodeList nodes = doc.getElementsByTagName(elementName);
            if (nodes.getLength() > 0) {
                Node node = nodes.item(0);
                Element e = (Element)node;
                value = e.getAttribute(attributeName).trim();
            }
        }
        log.debug("OSTIElinkService.getAttributeValue - the value of the attribute " + attributeName + 
                                                       " on the element " + elementName + " is " + value);
        return value;
    }

}
