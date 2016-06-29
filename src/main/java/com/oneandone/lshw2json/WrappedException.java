package com.oneandone.lshw2json;

/**
 * A runtime exception used for wrapping checked exceptions.
 * Useful in the environment of functional programming.
 * @author Stephan Fuhrmann
 */
public class WrappedException extends RuntimeException {

    public WrappedException(Exception cause) {
        super(cause);
    }
}
