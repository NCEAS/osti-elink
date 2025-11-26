package edu.ucsb.nceas.osti_elink.v2.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.nceas.osti_elink.OSTIElinkException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * JSON implementation of PublishIdentifierCommand for v2json OSTI service
 * This class determines if metadata represents a "publish" request by checking
 * if it contains site_url and has workflow_status = "R"
 *
 * @author Tao
 */
public class PublishIdentifierCommand extends edu.ucsb.nceas.osti_elink.PublishIdentifierCommand {
    private static final Log log = LogFactory.getLog(PublishIdentifierCommand.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    public static final String WORKFLOW_STATUS = "workflow_status";
    public static final String SITE_URL = "site_url";
    private static final String OSTI_ID = "osti_id";
    public static final String RELEASED_STATUS = "R"; // Released status

    /**
     * Constructor
     */
    public PublishIdentifierCommand() {
    }

    /**
     * Parse metadata to determine if it represents a publish command.
     * A publish command is determined by: The metadata contains site_url
     * @param json The JSON metadata string to parse
     * @return true if this metadata represents a publish request, false otherwise
     * @throws OSTIElinkException if JSON parsing fails
     */
    @Override
    public boolean parse(String json) throws OSTIElinkException {
        if (json == null || json.trim().isEmpty()) {
            log.debug("v2.json.PublishIdentifierCommand: Received empty JSON string");
            return false;
        }

        try {
            // Parse the JSON string
            JsonNode rootNode = mapper.readTree(json);

            // Check if parsing multiple records or single record
            JsonNode recordNode;
            if (rootNode.isArray() && rootNode.size() > 0) {
                // If it's an array, use the first record
                recordNode = rootNode.get(0);
                log.debug("v2.json.PublishIdentifierCommand: Processing first record from array of records");
            } else {
                // Otherwise use the root as the record
                recordNode = rootNode;
            }


            if (!recordNode.has(SITE_URL)) {
                log.debug("v2.json.PublishIdentifierCommand: JSON doesn't have a 'site_url' field");
                return false;
            }

            // Extract the required fields
            if (rootNode.get(OSTI_ID) != null) {
                this.ostiId = recordNode.get(OSTI_ID).asText();
            }
            this.url = recordNode.get(SITE_URL).asText();

            log.debug("v2.json.PublishIdentifierCommand: Successfully parsed publish request with osti_id: " +
                    this.ostiId + " and site_url: " + this.url);

            return true;

        } catch (JsonProcessingException e) {
            log.debug("v2.json.PublishIdentifierCommand: Failed to parse JSON: " + e.getMessage());
            throw new OSTIElinkException("v2.json.PublishIdentifierCommand: Failed to parse JSON metadata: " + e.getMessage());
        }
    }
}