package com.biger.client.ws.react.websocket.httpclient;

import com.biger.client.ws.react.domain.response.ExchangeResponse;
import com.biger.client.ws.react.websocket.Client;
import com.biger.client.ws.react.websocket.ClientBuilder;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class BigerWebSocketClientBuilder implements ClientBuilder {
    URI uri;
    Function<String, ExchangeResponse> text2Response;
    Function<ExchangeResponse, String>  response2SubId;

    @Override
    public ClientBuilder uri(URI uri) {
        this.uri = uri;
        return this;
    }

    @Override
    public ClientBuilder text2Response(Function<String, ExchangeResponse> text2Response) {
        this.text2Response = text2Response;
        return this;
    }

    @Override
    public ClientBuilder response2SubId(Function<ExchangeResponse, String> response2SubId) {
        this.response2SubId = response2SubId;
        return this;
    }

    @Override
    public Client build() {
        return BigerWebSocketClient.build(uri, text2Response, response2SubId);
    }
}

