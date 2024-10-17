package edu.ucsb.nceas.osti_elink.v2.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Tao
 * This class parse the json response from a query against the v2 api
 */
public class JsonResponseHandler {

    /**
     * Get the first non-null value of a json string with the given path
     * @param json  the json string will be looked up
     * @param path  the path will be looked
     * @return  the value of element. Null will be returned if it cannot be found
     * @throws JsonProcessingException
     */
    public static String getPathValue(String json, String path) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode arrayNode = mapper.readTree(json);
        if (arrayNode != null) {
            if (arrayNode.isArray()) {
                for (JsonNode node : arrayNode) {
                    JsonNode element = node.get(path);
                    if (element != null && element.asText() != null) {
                        //find the first non-null value
                        return element.asText();
                    }
                }
            }
        }
        return null;
    }

}
