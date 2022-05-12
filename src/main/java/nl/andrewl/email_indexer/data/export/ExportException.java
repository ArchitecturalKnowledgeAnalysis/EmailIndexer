package nl.andrewl.email_indexer.data.export;

public class ExportException extends RuntimeException {
    public ExportException(String message) {
        super(message);
    }

    public ExportException(String message, Throwable innerThrowable) {
        super(message, innerThrowable);
    }
}
