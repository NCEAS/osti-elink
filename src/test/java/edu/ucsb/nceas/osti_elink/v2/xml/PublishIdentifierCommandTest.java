package edu.ucsb.nceas.osti_elink.v2.xml;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Tao
 */
public class PublishIdentifierCommandTest {
    private static final String command1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<records>\n" + "<record>\n" + "<osti_id>2304990</osti_id>\n"
        + "<site_url>https://valley.duckdns.org/metacatui/view/doi:10.15485/2304990</site_url>\n"
        + "</record>\n" + "</records>";
    private static final String command2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<records>\n" + "<record>\n" + "<osti_id>2304990</osti_id>\n"
        + "<doi>10.15485/2304990</doi>"
        + "<site_url>https://valley.duckdns.org/metacatui/view/doi:10.15485/2304990</site_url>\n"
        + "</record>\n" + "</records>";
    private static final String command3 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<records>\n" + "<record>\n" + "<osti_id>2304990</osti_id>\n"
        + "<site_url>https://valley.duckdns.org/metacatui/view/doi:10.15485/2304990</site_url>\n"
        + "</record>\n"
        +  "<record>\n" + "<osti_id>2304990</osti_id>\n"
        + "<site_url>https://valley.duckdns.org/metacatui/view/doi:10.15485/2304990</site_url>\n"
        + "</record>\n"
        + "</records>";
    private static final String command4 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<records>\n" + "<record>\n" + "<osti_id>2304990<url>url</url></osti_id>\n"
        + "<site_url>https://valley.duckdns.org/metacatui/view/doi:10.15485/2304990</site_url>\n"
        + "</record>\n" + "</records>";
    private static final String command5 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<records>\n" + "<record>\n" + "<osti_id>2304990</osti_id>\n"
        + "<site_url>https://valley.duckdns.org/metacatui/view/doi:10.1<url>url</url></site_url>\n"
        + "</record>\n" + "</records>";
    private static final String command6 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<records>\n" + "<record>\n" + "<osti_id>2304990</osti_id>" + "<osti_id>2304990</osti_id>"
        + "<site_url>https://valley.duckdns.org/metacatui/view/doi:10.15485/2304990</site_url>\n"
        + "</record>\n" + "</records>";
    private static final String command7 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<records>\n" + "<record>\n" + "<osti_id>2304990</osti_id>"
        + "<site_url>https://valley.duckdns.org/metacatui/view/doi:10.15485/2304990</site_url>\n"
        + "<site_url>https://valley.duckdns.org/metacatui/view/doi:10.15485/2304990</site_url>\n"
        + "</record>\n" + "</records>";

    /**
     * Test the parse method
     * @throws Exception
     */
    @Test
    public void testParse() throws Exception {
        PublishIdentifierCommand command = new PublishIdentifierCommand();
        assertTrue(command.parse(command1));
        assertEquals("2304990", command.getOstiId());
        assertEquals(
            "https://valley.duckdns.org/metacatui/view/doi:10.15485/2304990", command.getUrl());
        assertFalse(command.parse(command2));
        assertFalse(command.parse(command3));
        assertFalse(command.parse(command4));
        assertFalse(command.parse(command5));
        assertFalse(command.parse(command6));
        assertFalse(command.parse(command7));
    }
}
