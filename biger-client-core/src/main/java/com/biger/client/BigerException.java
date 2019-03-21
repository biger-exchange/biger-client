package com.biger.client;

public class BigerException extends RuntimeException {
    public BigerException() {
    }

    public BigerException(String message) {
        super(message);
    }

    public BigerException(String message, Throwable cause) {
        super(message, cause);
    }

    public BigerException(Throwable cause) {
        super(cause);
    }

    public BigerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
