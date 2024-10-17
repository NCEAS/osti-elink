package edu.ucsb.nceas.osti_elink.v2.response;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
}
