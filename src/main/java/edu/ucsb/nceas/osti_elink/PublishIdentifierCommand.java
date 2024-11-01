package edu.ucsb.nceas.osti_elink;

/**
 * This class will determine if the given xml is a publishIdentifier command from Metacat
 * @author Tao
 */
public abstract class PublishIdentifierCommand {
    protected String ostiId;
    protected String url;

    /**
     * Get the osti id associated with the command
     * @return the osti id
     */
    public String getOstiId() {
        return ostiId;
    }

    /**
     * Get the url associated with the osit id in the command
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * This method will parse the given xml to determine if it is a publishIdentifier command
     * from the Metacat instance
     * The publishIdentifier command looks like this:
     * <?xml version="1.0" encoding="UTF-8"?>
     * <records>
     *   <record>
     *     <osti_id>2304990</osti_id>
     *     <site_url>https://valley.duckdns.org/metacatui/view/doi:10.15485/2304990</site_url>
     *   </record>
     * </records>
     * It also will extract the osti_id and url values from the given xml if it is the command
     * @param xml  the xml string which needs to be parsed
     * @return true if it is a publishIdentifier command; otherwise false.
     */
    public abstract boolean parse(String xml);
}
