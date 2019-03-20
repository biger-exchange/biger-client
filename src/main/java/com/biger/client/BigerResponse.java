package com.biger.client;

public class BigerResponse<T> {
    public String result;
    public int code;
    public String msg;
    public T data;
    public String requestTraceId;

    public T data() {
        return data;
    }
}
