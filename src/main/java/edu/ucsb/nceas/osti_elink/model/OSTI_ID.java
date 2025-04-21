package edu.ucsb.nceas.osti_elink.model;

/**
 * Represents an OSTI ID (Office of Scientific and Technical Information Identifier) data type.
 * OSTI IDs are typically numeric identifiers assigned to scientific and technical documents.
 */
public class OSTI_ID {
    // The OSTI ID value
    private final String value;

    /**
     * Constructs an OSTI_ID with the given value.
     *
     * @param value The OSTI ID string (typically numeric, e.g., "1234567")
     * @throws IllegalArgumentException if the value is null, empty, or invalid
     */
    public OSTI_ID(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("OSTI ID value cannot be null or empty");
        }

        // Basic validation to ensure the ID contains only digits
        if (!value.matches("\\d+")) {
            throw new IllegalArgumentException("Invalid OSTI ID format. Expected numeric value");
        }

        this.value = value;
    }

    /**
     * Returns the OSTI ID value.
     *
     * @return The OSTI ID string
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the string representation of this OSTI ID.
     *
     * @return The OSTI ID string
     */
    @Override
    public String toString() {
        return value;
    }

    /**
     * Checks if this OSTI ID equals another object.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        OSTI_ID other = (OSTI_ID) obj;
        return value.equals(other.value);
    }

    /**
     * Returns a hash code for this OSTI ID.
     */
    @Override
    public int hashCode() {
        return value.hashCode();
    }
}