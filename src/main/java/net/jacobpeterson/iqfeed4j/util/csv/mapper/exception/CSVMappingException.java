package net.jacobpeterson.iqfeed4j.util.csv.mapper.exception;

/**
 * {@link CSVMappingException} is a {@link RuntimeException} for CSV mapping errors.
 */
public class CSVMappingException extends RuntimeException {

    /**
     * Instantiates a new {@link CSVMappingException}.
     */
    public CSVMappingException() {}

    /**
     * Instantiates a new {@link CSVMappingException}.
     *
     * @param message the message
     */
    public CSVMappingException(String message) {
        super(message);
    }

    /**
     * Instantiates a new {@link CSVMappingException}.
     *
     * @param message the message
     * @param cause   the cause
     */
    public CSVMappingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new {@link CSVMappingException}
     *
     * @param cause the cause
     */
    public CSVMappingException(Throwable cause) {
        super(cause);
    }

    /**
     * Instantiates a new {@link CSVMappingException}
     *
     * @param csvIndex the CSV index
     * @param cause    the cause
     */
    public CSVMappingException(int csvIndex, Throwable cause) {
        super(String.format("Error mapping at index %d!", csvIndex), cause);
    }

    /**
     * Instantiates a new {@link CSVMappingException}
     *
     * @param csvIndex the CSV index
     * @param offset   the offset index
     * @param cause    the cause
     */
    public CSVMappingException(int csvIndex, int offset, Throwable cause) {
        super(String.format("Error mapping at index %d with offset %d!", csvIndex, offset), cause);
    }
}
