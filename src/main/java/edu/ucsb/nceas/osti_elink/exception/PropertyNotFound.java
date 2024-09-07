package edu.ucsb.nceas.osti_elink.exception;

/**
 * An exception indicate a property can't be found in the property file.
 * @author Tao
 */
public class PropertyNotFound extends Exception {

    /**
     * Constructor
     * @param message the message of the exception
     */
    public PropertyNotFound(String message) {
        super(message);
    }
}
