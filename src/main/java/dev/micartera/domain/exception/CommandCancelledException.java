package dev.micartera.domain.exception;

public class CommandCancelledException extends RuntimeException {
    public CommandCancelledException(String message) {
        super(message);
    }
}