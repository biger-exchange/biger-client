package com.biger.client.ws.react.exceptions;

import lombok.Getter;

@Getter
public class BigerExchangeException extends RuntimeException {

    final private Object data;

    public BigerExchangeException(String message, Throwable cause, Object data) {
        super(message, cause);
        this.data = data;
    }

    public BigerExchangeException(String message, Object data) {
        super(message);
        this.data = data;
    }
}
