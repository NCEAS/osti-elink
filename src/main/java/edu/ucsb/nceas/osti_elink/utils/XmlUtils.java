package edu.ucsb.nceas.osti_elink.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import edu.ucsb.nceas.osti_elink.OSTIElinkException;

/**
 * Utility class for XML document operations used in the OSTI E-Link integration.
 * Provides methods for parsing XML content, extracting values from elements and attributes,
 * and serializing DOM documents back to XML strings.
 */
public class XmlUtils {

    private static final Log log = LogFactory.getLog(XmlUtils.class);

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private XmlUtils() {
        // Utility class should not be instantiated
    }

    /**
     * Generates a DOM document from XML content provided as a byte array.
     *
     * @param bytes The XML content as a byte array
     * @return A DOM Document object representing the parsed XML
     * @throws OSTIElinkException If any error occurs during XML parsing
     */
    public static Document generateDOM(byte[] bytes) throws OSTIElinkException {
        if (bytes == null || bytes.length == 0) {
            throw new OSTIElinkException("Cannot parse empty or null XML content");
        }

        Document doc;
        try {
            ByteArrayInputStream inputData = new ByteArrayInputStream(bytes);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(inputData);
            doc.getDocumentElement().normalize(); // Normalize the document structure
        } catch (ParserConfigurationException e) {
            throw new OSTIElinkException("XmlUtils.generateDOM - Error: " + e.getMessage());
        } catch (SAXException e) {
            throw new OSTIElinkException("XmlUtils.generateDOM - Error: " + e.getMessage());
        } catch (IOException e) {
            throw new OSTIElinkException("XmlUtils.generateDOM - Error: " + e.getMessage());
        }
        return doc;
    }

    /**
     * Extracts the text value from the first occurrence of a specified XML element.
     *
     * <p>This method searches for elements with the given name and returns the text content
     * of the first matching element. If no matching element is found, or if the element
     * has no text content, null is returned.</p>
     *
     * @param doc The DOM document to search within
     * @param elementName The name of the XML element to find
     * @return The text content of the first matching element, or null if not found
     */
    public static String getElementValue(Document doc, String elementName) {
        String value = null;
        if (doc != null && elementName != null && !elementName.trim().isEmpty()) {
            NodeList nodes = doc.getElementsByTagName(elementName);
            if (nodes.getLength() > 0) {
                Node node = nodes.item(0);
                NodeList children = node.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);
                    if (child.getNodeType() == Node.TEXT_NODE) {
                        value = child.getNodeValue().trim();
                        log.debug("XmlUtils.getElementValue - the value of the element " + elementName + " is " + value);
                        break;
                    }
                }
            }
        }
        return value;
    }

    /**
     * Retrieves the value of a specific attribute from the first occurrence of a specified element.
     *
     * <p>This method searches for elements with the given name and returns the value of the
     * specified attribute from the first matching element. If no matching element is found,
     * or if the attribute doesn't exist, null is returned.</p>
     *
     * @param doc The DOM document to search within
     * @param elementName The name of the XML element to find
     * @param attributeName The name of the attribute whose value to retrieve
     * @return The value of the specified attribute, or null if not found
     */
    public static String getAttributeValue(Document doc, String elementName, String attributeName) {
        String value = null;
        if (doc != null && elementName != null && !elementName.trim().isEmpty() &&
                attributeName != null && !attributeName.trim().isEmpty()) {
            NodeList nodes = doc.getElementsByTagName(elementName);
            if (nodes.getLength() > 0) {
                Node node = nodes.item(0);
                Element element = (Element) node;
                value = element.getAttribute(attributeName).trim();
                if (value.isEmpty()) {
                    value = null;
                }
            }
        }
        log.debug("XmlUtils.getAttributeValue - the value of the attribute " + attributeName +
                " on the element " + elementName + " is " + value);
        return value;
    }

    /**
     * Serializes a DOM Document object to an XML string.
     *
     * <p>This method converts the hierarchical DOM structure back into a properly formatted
     * XML string representation using UTF-8 encoding.</p>
     *
     * @param doc The DOM document to serialize
     * @return A string containing the XML representation of the document
     * @throws OSTIElinkException If serialization fails
     */
    public static String serialize(Document doc) throws OSTIElinkException {
        if (doc == null) {
            throw new OSTIElinkException("Cannot serialize null document");
        }

        try {
            DOMImplementationLS domImplementation = (DOMImplementationLS) doc.getImplementation();
            LSSerializer serializer = domImplementation.createLSSerializer();
            LSOutput output = domImplementation.createLSOutput();
            output.setEncoding("UTF-8");
            Writer stringWriter = new StringWriter();
            output.setCharacterStream(stringWriter);
            serializer.write(doc, output);
            return stringWriter.toString();
        } catch (Exception e) {
            throw new OSTIElinkException("Failed to serialize XML document: " + e.getMessage());
        }
    }
}