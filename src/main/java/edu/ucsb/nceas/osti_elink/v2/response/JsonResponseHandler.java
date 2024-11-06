package edu.ucsb.nceas.osti_elink.v2.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.ucsb.nceas.osti_elink.OSTIElinkException;

/**
 * @author Tao
 * This class parse the json response from a query against the v2 api
 */
public class JsonResponseHandler {

    /**
     * Get the first non-null value of a json string with the given path (first level in the
     * array element)
     * For example: if the json string is:
     * "[{"name": "John", "age": 30}, {"name": "Mary", "age": 25}]" and path is "name", the
     * method will return the value of "John".
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

    /**
     * Get the first json node from the given json string (it is an array).
     * For example: if the json string is:
     * "[{"name": "John", "age": 30}, {"name": "Mary", "age": 25}]"
     * It will return {"name": "John", "age": 30}
     * @param json  the json string
     * @return the first json array node. It may return null if it can't find it.
     */
    public static ObjectNode getFirstNodeInArray(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode arrayNode = mapper.readTree(json);
        if (arrayNode != null) {
            if (arrayNode.isArray()) {
                return (ObjectNode) arrayNode.get(0);
            }
        }
        return null;
    }

    /**
     * Parse the response json string to the put or post requests. If the response has the error
     * attribute, this will throw an exception
     * method will
     * @param response the response string (the json format) of the put or post requests
     */
    public static void parsePutAndPostResponse(String response) throws OSTIElinkException {
        if (response == null || response.trim().equals("")) {
            throw new OSTIElinkException("The response for the request is blank");
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode arrayNode = mapper.readTree(response);
            if (arrayNode.get("errors") != null) {
                throw new OSTIElinkException("The request failed since " + response);
            }
        } catch (JsonProcessingException e) {
            throw new OSTIElinkException(
                "The response for the request is not a valid json string: " + response);
        }
    }

}
