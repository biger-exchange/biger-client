package com.biger.client.httpops;

public class BigerResponse<T> {
    public String result;
    public int code;
    public String msg;
    public T data;
    public String requestTraceId;

    public T data() {
        return data;
    }

    @Override
    public String toString() {
        return "BigerResponse{" +
                "result='" + result + '\'' +
                ", code=" + code +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                ", requestTraceId='" + requestTraceId + '\'' +
                '}';
    }
}
