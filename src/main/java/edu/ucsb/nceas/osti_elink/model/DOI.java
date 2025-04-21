package edu.ucsb.nceas.osti_elink.model;

/**
 * Represents a Digital Object Identifier (DOI) data type.
 */
public class DOI {
    // The complete DOI string
    private final String value;

    /**
     * Constructs a DOI with the given value.
     *
     * @param value The DOI string (e.g., "10.1000/182")
     * @throws IllegalArgumentException if the value is null or not a valid DOI format
     */
    public DOI(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("DOI value cannot be null or empty");
        }

        // Basic validation that the DOI has the expected format with at least one '/'
        if (!value.contains("/")) {
            throw new IllegalArgumentException("Invalid DOI format. Expected 'prefix/suffix'");
        }

        this.value = value;
    }

    /**
     * Returns the DOI value.
     *
     * @return The DOI string
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the string representation of this DOI.
     *
     * @return The DOI string
     */
    @Override
    public String toString() {
        return value;
    }

    /**
     * Checks if this DOI equals another object.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        DOI other = (DOI) obj;
        return value.equals(other.value);
    }

    /**
     * Returns a hash code for this DOI.
     */
    @Override
    public int hashCode() {
        return value.hashCode();
    }
}