package com.biger.client.ws.react.websocket;

import com.biger.client.ws.react.domain.response.ExchangeResponse;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface ClientBuilder {
    ClientBuilder uri(URI uri);
    ClientBuilder text2Response(Function<String, ExchangeResponse> text2Response);
    ClientBuilder response2SubId(Function<ExchangeResponse, String> response2SubId);

    CompletableFuture<? extends Client> build();
}