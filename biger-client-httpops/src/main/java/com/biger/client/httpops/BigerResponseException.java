package com.biger.client.httpops;

public class BigerResponseException extends RuntimeException {
    final public int statusCode;
    final public String respBody;

    public BigerResponseException(String message, int statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        respBody = responseBody;
    }

    public BigerResponseException(String message, Throwable cause) {
        super(message, cause);
        statusCode = 0;
        respBody = "";
    }
}
