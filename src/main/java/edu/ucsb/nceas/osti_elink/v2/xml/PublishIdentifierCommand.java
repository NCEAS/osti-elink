package edu.ucsb.nceas.osti_elink.v2.xml;

import edu.ucsb.nceas.osti_elink.OSTIElinkException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * @author Tao
 */
public class PublishIdentifierCommand extends edu.ucsb.nceas.osti_elink.PublishIdentifierCommand {
    private static final Log log = LogFactory.getLog(PublishIdentifierCommand.class);
    private DocumentBuilder dBuilder;

    /**
     * Constructor
     * @throws ParserConfigurationException
     */
    public PublishIdentifierCommand() throws ParserConfigurationException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dBuilder = dbFactory.newDocumentBuilder();
    }
    /**
     * If the xml is a publishIdentifier command, we should use different routine. This method
     * can determine if it is. The publishIdentifier command looks like:
     * <?xml version="1.0" encoding="UTF-8"?>
     * <records>
     *   <record>
     *     <osti_id>2304990</osti_id>
     *     <site_url>https://valley.duckdns.org/metacatui/view/doi:10.15485/2304990</site_url>
     *   </record>
     * </records>
     */
    @Override
    public boolean parse(String xml) throws OSTIElinkException {
        ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes());
        try {
            Document doc = dBuilder.parse(is);
            Element rootElement = doc.getDocumentElement();
            if (!rootElement.getNodeName().equals("records")) {
                return false;
            }
            NodeList childNodes = rootElement.getChildNodes();
            int numberOfRecord = 0;
            int numberOfOsitId = 0;
            int numberOfUrl = 0;
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node childNode = childNodes.item(i);
                // Check if the node is an element
                if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element childElement = (Element) childNode;
                    //handle the record element
                    if (!childElement.getNodeName().equals("record")) {
                        log.debug("It has a child whose name is " + childElement.getNodeName()
                                      + "rather than record");
                        return false;
                    } else {
                        numberOfRecord++;
                        NodeList grandChildNodes = childElement.getChildNodes();
                        for (int j=0; j < grandChildNodes.getLength(); j++) {
                            Node grandChildNode = grandChildNodes.item(j);
                            if (grandChildNode.getNodeType() == Node.ELEMENT_NODE) {
                                if (grandChildNode.getNodeName().equals("osti_id")) {
                                    //handle handle osti_id element
                                    numberOfOsitId++;
                                    NodeList ostiChildNodes = grandChildNode.getChildNodes();
                                    for (int m=0; m < ostiChildNodes.getLength(); m++) {
                                        Node ostiChildNode = ostiChildNodes.item(m);
                                        if (ostiChildNode.getNodeType() == Node.TEXT_NODE) {
                                            ostiId = ostiChildNode.getTextContent();
                                            log.debug("Set ostId " + ostiId);
                                        } else {
                                            log.debug("The ostid_id element has other child nodes "
                                                          + "rather the text node");
                                            return false;
                                        }
                                    }
                                } else if (grandChildNode.getNodeName().equals("site_url")) {
                                    // handle the url element
                                    numberOfUrl++;
                                    NodeList urlChildNodes = grandChildNode.getChildNodes();
                                    for (int n=0; n < urlChildNodes.getLength(); n++) {
                                        Node urlChildNode = urlChildNodes.item(n);
                                        if (urlChildNode.getNodeType() == Node.TEXT_NODE) {
                                            url = urlChildNode.getTextContent();
                                            log.debug("Set url " + url);
                                        } else {
                                            log.debug("The url element has other child nodes "
                                                          + "rather the text node");
                                            return false;
                                        }
                                    }
                                } else {
                                    log.debug("It has a child whose name is "
                                                  + grandChildNode.getNodeName()
                                                  + "rather than osti_id or url");
                                    return false;
                                }
                            }
                            if (numberOfOsitId > 1) {
                                log.debug(
                                    "The number of osti_id is greater than 1 : " + numberOfOsitId);
                                return false;
                            }
                            if (numberOfUrl > 1) {
                                log.debug(
                                    "The number of url is greater than 1 : " + numberOfUrl);
                                return false;
                            }
                        }
                    }
                }
                if (numberOfRecord > 1) {
                    log.debug("The number of record is greater than 1 : " + numberOfRecord);
                    return false;
                }
            }
            if (numberOfRecord != 1) {
                log.debug("The number of record does not equal 1 : " + numberOfRecord);
                return false;
            }
            if (numberOfOsitId != 1) {
                log.debug("The number of osti_id does not equal 1 : " + numberOfOsitId);
                return false;
            }
            if (numberOfUrl != 1) {
                log.debug("The number of url does not equal 1 : " + numberOfUrl);
                return false;
            }
        } catch (SAXException | IOException e) {
            throw new OSTIElinkException(e.getMessage());
        }
        return true;
    }
}
