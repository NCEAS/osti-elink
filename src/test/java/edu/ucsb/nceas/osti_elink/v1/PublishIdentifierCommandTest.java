package edu.ucsb.nceas.osti_elink.v1;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * The junit test for PublishIdentifierCommand
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

    /**
     * Test the parse method
     * @throws Exception
     */
    @Test
    public void testParse() {
        PublishIdentifierCommand command = new PublishIdentifierCommand();
        assertFalse(command.parse(command1));
        assertFalse(command.parse(command2));
        assertFalse(command.parse(null));
        assertFalse(command.parse(""));
    }
}
