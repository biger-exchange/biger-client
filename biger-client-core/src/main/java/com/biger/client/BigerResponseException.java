package com.biger.client;

public class BigerResponseException extends BigerException {
    public int statusCode;
    public String respBody;

    public BigerResponseException(String message, int statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        respBody = responseBody;
    }
}
