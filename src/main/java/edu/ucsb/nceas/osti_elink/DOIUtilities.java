package edu.ucsb.nceas.osti_elink;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import edu.ucsb.nceas.osti_elink.utils.XmlUtils;

/**
 * DOIUtilities provides supporting utilities for DOI operations.
 */
public class DOIUtilities {
    private static final String DOI = "doi";
    private static final String OSTI_ID = "osti_id";

    protected static Log log = LogFactory.getLog(DOIUtilities.class);

    private Document minimalMetadataDoc = null;
    private String minimalMetadataFile;
    private String originalDefaultSiteCode = null;
    private String currentDefaultSiteCode = "test";

    /**
     * Build a minimal metadata for the given siteCode in order to
     * mint a DOI from the OSTI Elink service. If the siteCode is null or blank, the default ESS-DIVE code will be used.
     * @param siteCode  the site code (determining the prefix of the DOI) will be used in the metadata
     * @return  the minimal metadata will be used to mint a DOI
     * @throws OSTIElinkException
     */
    public String buildMinimalMetadata(String siteCode) throws OSTIElinkException {
        String metadataStr = null;
        if (minimalMetadataDoc == null) {
            //we need to read the minimal metadata file and parse it into a document
            try(InputStream input = getClass().getClassLoader().getResourceAsStream("osti.properties")) {
                java.util.Properties props = new java.util.Properties();
                props.load(input);
                minimalMetadataFile = props.getProperty("minimal.metadata.file");

                try (InputStream is = getClass().getClassLoader().getResourceAsStream(minimalMetadataFile)) {
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder;
                    try {
                        dBuilder = dbFactory.newDocumentBuilder();
                        minimalMetadataDoc = dBuilder.parse(is);
                        originalDefaultSiteCode = XmlUtils.getElementValue(minimalMetadataDoc, "site_input_code");
                        log.debug("DOIUtilities.buildMinimalMetadata - the original site code in the minimal metadata is " + originalDefaultSiteCode);
                    } catch (ParserConfigurationException e) {
                        throw new OSTIElinkException("DOIUtilities.buildMinimalMetadata - Error: " + e.getMessage());
                    } catch (SAXException e) {
                        throw new OSTIElinkException("DOIUtilities.buildMinimalMetadata - Error: " + e.getMessage());
                    } catch (IOException e) {
                        throw new OSTIElinkException("DOIUtilities.buildMinimalMetadata - Error: " + e.getMessage());
                    }
                } catch (IOException ee) {
                    throw new OSTIElinkException("DOIUtilities.buildMinimalMetadata - Error to read the file: " + ee.getMessage());
                }
            } catch (IOException e) {
                throw new OSTIElinkException("DOIUtilities.buildMinimalMetadata - Error: " + e.getMessage());
            }
        }

        if (siteCode != null && !siteCode.trim().equals("")) {
            modifySiteCode(siteCode);
        } else if (!originalDefaultSiteCode.equals(currentDefaultSiteCode)) {
            //now the user ask the default site code. But the site map value has been updated by another call.
            //we need to change back to the original code.
            modifySiteCode(originalDefaultSiteCode);
        }

        metadataStr = XmlUtils.serialize(minimalMetadataDoc);
        return metadataStr;
    }

    /**
     * Modify the value of the site code element to the given value
     * @param siteCode  the value will be assigned as the new value
     * @throws OSTIElinkException
     */
    private void modifySiteCode(String siteCode) throws OSTIElinkException {
        synchronized (minimalMetadataDoc) {
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
                        currentDefaultSiteCode = siteCode;
                        break;
                    }
                }
            } else {
                throw new OSTIElinkException("DOIUtilities.modifySiteCode - the minimal metadata should have the site_input_code element.");
            }
        }
    }

    /**
     * If a identifier starts with "doi:", this method will remove "doi:".
     * Otherwise it will return the original one back
     * @param identifier  the identifier which will be removed "doi:" if it has one
     * @return  the string with the "doi:" part if it has; otherwise return the identifier itself
     */
    public static String removeDOI(String identifier) {
        log.debug("DOIUtilities.removeDOI - the original identifier is " + identifier);
        String doiPrefix = DOI + ":";
        if (identifier!= null) {
            //we need to remove the doi prefix
            if (identifier.startsWith(doiPrefix)) {
                identifier = identifier.substring(identifier.indexOf(doiPrefix) + doiPrefix.length());
            }
        }
        log.debug("DOIUtilities.removeDOI - the identifier after removing doi is " + identifier);
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
     * @param metadataManager the metadata manager to use for retrieving metadata
     * @param ostiService the OSTI service implementation
     * @return the osti id associates with the doi
     * @throws OSTIElinkException
     */
    public String getOstiId(String doi, String prefix, MetadataManager metadataManager, OSTIElinkService ostiService) throws OSTIElinkException {
        String ostiId = null;
        if (doi == null || doi.trim().equals("")) {
            throw new OSTIElinkException("DOIUtilities.getOstiId - the given doi shouldn't be null or blank when it figures out the OSTI id for a DOI.");
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
                log.debug("DOIUtilities.getOstiId - tried to use prefix " + prefix + " to get the osti id " + ostiId +
                        " from the doi identifier " + doi + " without querying the services");
            }
        }
        if (ostiId == null || ostiId.trim().equals("")) {
            //we can't get the osti id from doi itself. We have to query the service.
            String metadata = metadataManager.getMetadata(doi);
            ostiId = ostiService.parseOSTIidFromResponse(metadata, doi);
            log.debug("DOIUtilities.getOstiId - tried to query the service to get the osti id " + ostiId +
                    " from the doi identifier " + doi);
        }
        log.debug("DOIUtilities.getOstiId - the osti id of the doi identifier " + doi + " is " + ostiId);
        return ostiId;
    }

    /**
     * Add the osti id element to the metadata as the first child if the metadata doesn't have one;otherwise, it will
     * replace with the new value
     * @param ostiId  the value of the ostiId element will be added or replaced
     * @param metadataXML  the metadata xml which will be modified
     * @return the xml string presentation of the new metadata document with the given osti id
     * @throws OSTIElinkException
     */
    public String addOrReplaceOstiIdToXMLMetadata(String ostiId, String metadataXML) throws OSTIElinkException {
        if (metadataXML == null || metadataXML.trim().equals("")) {
            throw new OSTIElinkException("DOIUtilities.addOrReplaceOstiIdToXMLMetadata - the metadata part mustn't be null or blank.");
        }
        Document doc = null;
        try {
            doc = XmlUtils.generateDOM(metadataXML.getBytes());
        } catch (Exception e) {
            throw new OSTIElinkException("DOIUtilities.addOrReplaceOstiIdToXMLMetadata - the metadata part must be a valid xml string. But the string is " +
                    metadataXML + " And it can't be processed because " + e.getMessage());
        }
        NodeList osti_id_nodes = doc.getElementsByTagName(OSTI_ID);
        if (osti_id_nodes.getLength() == 0) {
            //it doesn't have an osti id, we need to append one
            NodeList records = doc.getElementsByTagName("record");
            if (records.getLength() !=1 ) {
                throw new OSTIElinkException("DOIUtilities.addOrReplaceOstiIdToXMLMetadata - the metadata must only one record.");
            } else {
                Node record = records.item(0);
                Node ostiNode = doc.createElement("osti_id");
                Text newText = doc.createTextNode(ostiId);
                ostiNode.appendChild(newText);
                record.insertBefore(ostiNode, record.getFirstChild());
            }
        } else if (osti_id_nodes.getLength() == 1) {
            //The osti id already exists, we need to replace it.
            Node osti_id_node = osti_id_nodes.item(0);
            NodeList children = osti_id_node.getChildNodes();
            for (int i=0; i<children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.TEXT_NODE) {
                    Text newText = doc.createTextNode(ostiId);
                    child.getParentNode().replaceChild(newText, child);
                    break;
                }
            }
        } else {
            throw new OSTIElinkException("DOIUtilities.addOrReplaceOstiIdToXMLMetadata - the metadata shouldn't have more than one osti id.");
        }
        return XmlUtils.serialize(doc);
    }
}