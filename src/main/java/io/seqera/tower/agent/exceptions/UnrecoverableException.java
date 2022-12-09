package io.seqera.tower.agent.exceptions;

/**
 * An unrecoverable exception is an exception that Tower Agent will log as
 * an error and cause it to exit with an exit code error.
 */
public class UnrecoverableException extends RuntimeException {

    public UnrecoverableException() {
    }

    public UnrecoverableException(String message) {
        super(message);
    }

    public UnrecoverableException(String message, Throwable cause) {
        super(message, cause);
    }
}
