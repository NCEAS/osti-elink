
package edu.ucsb.nceas.osti_elink;

/**
 * An exception that encapsulates authentication and authorization errors
 * from the OSTI Elink service (HTTP 401/403 responses).
 */
public class OSTIElinkAuthenticationException extends OSTIElinkException {

    private final int statusCode;

    public OSTIElinkAuthenticationException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * Get the HTTP status code that caused this authentication exception.
     * @return HTTP status code (typically 401 or 403)
     */
    public int getStatusCode() {
        return statusCode;
    }
}
