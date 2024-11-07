package edu.ucsb.nceas.osti_elink.v2.response;

import com.fasterxml.jackson.databind.JsonNode;
import edu.ucsb.nceas.osti_elink.OSTIElinkException;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Junit test class for JsonResponseHandler
 * @author Tao
 */
public class JsonResponseHandlerTest {

    /**
     * Test the method of getPath
     * @throws Exception
     */
    @Test
    public void testGetPath() throws Exception {
        try (InputStream is = getClass().getClassLoader()
            .getResourceAsStream("test-files/search-osti-id-response.json")) {
            String json = IOUtils.toString(is, StandardCharsets.UTF_8);
            String value = JsonResponseHandler.getPathValue(json, "doi");
            assertEquals("10.15485/2304331", value);
            value = JsonResponseHandler.getPathValue(json, "workflow_status");
            assertEquals("R", value);
            value = JsonResponseHandler.getPathValue(json, "foo");
            assertNull(value);
            value = JsonResponseHandler.getPathValue(json, "osti_id");
            assertEquals("2304331", value);
        }
        try (InputStream is = getClass().getClassLoader()
            .getResourceAsStream("test-files/search-doi-response.json")) {
            String json = IOUtils.toString(is, StandardCharsets.UTF_8);
            String value = JsonResponseHandler.getPathValue(json, "doi");
            assertEquals("10.15485/2304391", value);
            value = JsonResponseHandler.getPathValue(json, "workflow_status");
            assertEquals("R", value);
            value = JsonResponseHandler.getPathValue(json, "foo");
            assertNull(value);
            value = JsonResponseHandler.getPathValue(json, "osti_id");
            assertEquals("2304391", value);
        }
        String json = "[]";
        String value = JsonResponseHandler.getPathValue(json, "doi");
        assertNull(value);
    }

    /**
     * Test the getFirstNodeInArray method
     * @throws Exception
     */
    @Test
    public void testGetFirstNodeInArray() throws Exception {
        try (InputStream is = getClass().getClassLoader()
            .getResourceAsStream("test-files/search-osti-id-response.json")) {
            String json = IOUtils.toString(is, StandardCharsets.UTF_8);
            JsonNode node = JsonResponseHandler.getFirstNodeInArray(json);
            assertEquals("10.15485/2304331", node.get("doi").textValue());
            assertEquals("R", node.get("workflow_status").textValue());
            assertNull(node.get("foo"));
            assertEquals(2304331, node.get("osti_id").intValue());
        }
        try (InputStream is = getClass().getClassLoader()
            .getResourceAsStream("test-files/search-doi-response.json")) {
            String json = IOUtils.toString(is, StandardCharsets.UTF_8);
            JsonNode node = JsonResponseHandler.getFirstNodeInArray(json);
            assertEquals("10.15485/2304391", node.get("doi").textValue());
            assertEquals("R", node.get("workflow_status").textValue());
            assertNull(node.get("foo"));
            assertEquals(2304391, node.get("osti_id").intValue());
        }
        String json = "[]";
        JsonNode node = JsonResponseHandler.getFirstNodeInArray(json);
        assertNull(node);
        json = null;
        try {
            node = JsonResponseHandler.getFirstNodeInArray(json);
            fail("Test can't reach here");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
        json = "";
        node = JsonResponseHandler.getFirstNodeInArray(json);
        assertNull(node);
    }

    /**
     * Test the parsePutAndPostResponse method
     * @throws Exception
     */
    @Test
    public void testParsePutAndPostResponse() throws Exception {
        try (InputStream is = getClass().getClassLoader()
            .getResourceAsStream("test-files/put-success-response.json")) {
            String json = IOUtils.toString(is, StandardCharsets.UTF_8);
            //The method should work without any exceptions
            JsonResponseHandler.isResponseWithError(json);
        }
        try (InputStream is = getClass().getClassLoader()
            .getResourceAsStream("test-files/put-error-response.json")) {
            String json = IOUtils.toString(is, StandardCharsets.UTF_8);
            try {
                JsonResponseHandler.isResponseWithError(json);
                fail("Test can't get there since the parsePutAndPostResponse should throw an "
                         + "exception");
            } catch (Exception e) {
                assertTrue(e instanceof OSTIElinkException);
            }
        }
        String invalidJson = "{\"name: \"John\"}";
        try {
            JsonResponseHandler.isResponseWithError(invalidJson);
            fail("Test can't get there since the parsePutAndPostResponse should throw an "
                     + "exception");
        } catch (Exception e) {
            assertTrue(e instanceof OSTIElinkException);
        }
        try {
            JsonResponseHandler.isResponseWithError(null);
            fail("Test can't get there since the parsePutAndPostResponse should throw an "
                     + "exception");
        } catch (Exception e) {
            assertTrue(e instanceof OSTIElinkException);
        }
    }

}
