package com.github.ysl3000.quantum.api.receiver;

public final class ValueNotChangedException extends Exception {
    /**
     * Constructs a new exception with {@code null} as its detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     */
    public ValueNotChangedException() {
        super("Value hasn't changed.");
    }
}