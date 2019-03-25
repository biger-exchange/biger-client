package com.biger.client.ws.react;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BigerMarketRequestType {

    PRICER_QUERY("price.query"),
    LOGIN("user.login")
    ;

    private String method;



}
