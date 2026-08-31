package exception;

/**
 * Exception thrown when a requested backup job or resource cannot be found.
 */
public class BackupNotFoundException extends RuntimeException {

    public BackupNotFoundException(String message) {
        super(message);
    }
}
