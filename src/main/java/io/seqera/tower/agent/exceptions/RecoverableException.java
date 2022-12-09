package io.seqera.tower.agent.exceptions;

/**
 * A recoverable exception is an exception that Tower Agent will log as
 * an error, but it will keep running and retrying to connect.
 */
public class RecoverableException extends RuntimeException {

    public RecoverableException() {
    }

    public RecoverableException(String message) {
        super(message);
    }

    public RecoverableException(String message, Throwable cause) {
        super(message, cause);
    }
}
