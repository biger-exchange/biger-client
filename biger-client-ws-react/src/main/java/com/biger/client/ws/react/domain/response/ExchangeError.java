package com.biger.client.ws.react.domain.response;

import lombok.Data;

@Data
public class ExchangeError {

    private int code;

    private String message;
}
