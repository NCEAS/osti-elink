package edu.ucsb.nceas.osti_elink.v1;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Tao
 */
public class PublishIdentifierCommand extends edu.ucsb.nceas.osti_elink.PublishIdentifierCommand {
    private static Log log = LogFactory.getLog(PublishIdentifierCommand.class);
    /**
     * To the v1 api, this library doesn't need to do some special actions to handle the
     * publishIdentifier command in the setMetadata method. So it always return false,
     */
    @Override
    public boolean parse(String xml) {
        log.debug("This is in the v1 API and it always return false.");
        return false;
    }
}
