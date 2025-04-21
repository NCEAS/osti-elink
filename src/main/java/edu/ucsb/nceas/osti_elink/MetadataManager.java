package edu.ucsb.nceas.osti_elink;

import edu.ucsb.nceas.osti_elink.utils.HttpService;
import edu.ucsb.nceas.osti_elink.utils.XmlUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

/**
 * MetadataManager handles operations related to retrieving and updating metadata
 * for DOIs and OSTI IDs.
 */
public class MetadataManager {
    private static final String STATUS = "SUCCESS";
    private static final String SUCCESS = "SUCCESS";
    private static final String DOI = "doi";
    private static final String OSTI_ID = "osti_id";

    private String baseURL;
    private HttpService httpService;
    protected static Log log = LogFactory.getLog(MetadataManager.class);

    public MetadataManager(String baseURL, HttpService httpService) {
        this.baseURL = baseURL;
        this.httpService = httpService;
    }

    /**
     * Get the metadata associated with the given identifier, which should be a doi. An OSTIElinkNotFoundException
     * will be thrown if the identifier can't be found. It may contains multiple records.
     * @param doi  the identifier for which the metadata should be returned
     * @return  the metadata in the xml format
     * @throws OSTIElinkException
     */
    public String getMetadata(String doi) throws OSTIElinkException {
        return getMetadata(doi, DOI);
    }

    /**
     * Get the metadata associated with the osti id. An OSTIElinkNotFoundException
     * will be thrown if the identifier can't be found.
     * @param ostiId  the osti id for which the metadata should be returned
     * @return  the metadata in the xml format
     * @throws OSTIElinkException
     */
    public String getMetadataFromOstiId(String ostiId) throws OSTIElinkException {
        return getMetadata(ostiId, OSTI_ID);
    }

    /**
     * Get the metadata associated with the given identifier. An OSTIElinkNotFoundException
     * will be thrown if the identifier can't be found.
     * @param identifier  the identifier for which the metadata should be returned
     * @param  type  the type of the identifier, which can be doi or OSTIId
     * @return  the metadata in the xml format
     * @throws OSTIElinkException
     */
    public String getMetadata(String identifier, String type) throws OSTIElinkException {
        String metadata = null;
        if (identifier != null && !identifier.trim().equals("")) {
            identifier = DOIUtilities.removeDOI(identifier);
            String url = null;
            try {
                url = baseURL + "?" + type + "=" + URLEncoder.encode(identifier, StandardCharsets.UTF_8.toString());
            } catch (UnsupportedEncodingException e) {
                throw new OSTIElinkException("MetadataManager.getMetadata - couldn't encode the query url: " + e.getMessage());
            }
            log.debug("MetadataManager.getMetadata - the url sending to the service is " + url);
            byte[] response = httpService.sendRequest(HttpService.GET, url);
            metadata = new String(response);
            log.debug("MetadataManager.getMetadata - the reponse for id " + identifier + " is\n " + metadata);
            if (metadata == null || metadata.trim().equals("")) {
                throw new OSTIElinkNotFoundException("MetadataManager.getMetadata - the reponse is blank. So we can't find the identifier " +
                        identifier + ", which type is " + type);
            } else if (!metadata.contains(identifier)) {
                Document doc = null;
                try {
                    doc = XmlUtils.generateDOM(response);
                } catch (Exception e) {
                    //The response is not a xml document. Just throw the exception
                    throw new OSTIElinkException("MetadataManager.getMetadata - can't get the metadata for id " +
                            identifier + " since\n " + metadata);
                }
                String numFound = XmlUtils.getAttributeValue(doc, "records", "numfound");
                if (numFound.equals("0")) {
                    throw new OSTIElinkNotFoundException("MetadataManager.getMetadata - OSTI can't find the identifier " +
                            identifier + ", which type is " + type + " since\n " + metadata);
                } else {
                    throw new OSTIElinkException("MetadataManager.getMetadata - can't get the metadata for id " +
                            identifier + " since\n " + metadata);
                }
            }
        } else {
            throw new OSTIElinkException("MetadataManager.getMetadata - the given identifier can't be null or blank.");
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
     * @param ostiService  the OSTIElinkService instance to use for operations
     * @throws OSTIElinkException
     */
    public void setMetadata(String doi, String doiPrefix, String metadataXML, OSTIElinkService ostiService) throws OSTIElinkException {
        DOIUtilities doiUtilities = new DOIUtilities();
        String ostiId = doiUtilities.getOstiId(doi, doiPrefix, this, ostiService);
        String newMetadataXML = doiUtilities.addOrReplaceOstiIdToXMLMetadata(ostiId, metadataXML);
        log.debug("MetadataManager.setMetadata - the new xml metadata with the osti id " + ostiId +
                " for the doi identifier " + doi + " is:\n" + newMetadataXML);
        PublishIdentifierCommand command = PublishIdentifierCommandFactory.getInstance(ostiService);
        if (command.parse(newMetadataXML)) {
            log.info(newMetadataXML + " is a publishIdentifier command and it should be handled "
                    + "by a different route.");
            ostiService.handlePublishIdentifierCommand(command.getOstiId(), command.getUrl());
        } else {
            log.debug("The metadata in the setMetadata method is NOT a publishIdentifier command "
                    + "and the method just use the regular route.");
            byte[] response = httpService.sendRequest(HttpService.POST, baseURL, newMetadataXML);
            log.debug("MetadataManager.setMetadata - the response from the OSTI service to set "
                    + "metadata for id " + doi + " is:\n " + new String(response));
            Document doc = null;
            String status = null;
            try {
                doc = XmlUtils.generateDOM(response);
                status = XmlUtils.getElementValue(doc, STATUS);
            } catch (Exception e) {
                log.info("MetadataManager.setMetadata - can't get the status of the response:\n" +
                        new String(response) + " since the response is not an XML string.");
            }
            if (status == null || !status.equalsIgnoreCase(SUCCESS)) {
                throw new OSTIElinkException("MetadataManager.setMetadata - Error:\n"
                        + new String(response));
            }
        }
    }
}