package com.biger.client.ws.react.websocket;

import com.biger.client.ws.react.domain.response.ExchangeResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ServiceLoader;
import java.util.function.Predicate;

public interface Client {

    void stop();

    Mono<ExchangeResponse> expectAck(String requestId);

    Mono<ExchangeResponse> requestSingle(String requestId, String requestMsg);

    Flux<ExchangeResponse> sub(String subId, String subRequestMsg, String unSubRequestMsg, Predicate<String> topicFilter);

    static ClientBuilder newBuilder() {
        return ServiceLoader.load(ClientBuilder.class).iterator().next();
    }
}
